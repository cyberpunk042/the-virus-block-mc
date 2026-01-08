#!/usr/bin/env python3
"""
Phase 1: Dry-Run Audit for JSON Serialization Refactor

This script analyzes the codebase and produces a detailed report of:
1. All classes with manual toJson/fromJson patterns
2. Field → JSON key mappings (including mismatches)
3. Proposed annotations for each field
4. Estimated changes per file
5. Risk assessment

NO CHANGES ARE MADE - this is audit only.

Run: python3 scripts/audit_json_refactor_plan.py
"""

import os
import re
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Set
from collections import defaultdict
import json

# ═══════════════════════════════════════════════════════════════════════════════
# DATA STRUCTURES
# ═══════════════════════════════════════════════════════════════════════════════

@dataclass
class FieldMapping:
    """A single field and its JSON mapping(s)."""
    field_name: str
    field_type: str
    json_keys: Set[str] = field(default_factory=set)  # All JSON keys that map to this field
    setter_name: Optional[str] = None
    getter_name: Optional[str] = None
    default_value: Optional[str] = None
    needs_alias: bool = False  # True if json_key != field_name
    
    def primary_json_key(self) -> str:
        """The main JSON key (usually matches field name)."""
        if self.field_name in self.json_keys:
            return self.field_name
        return next(iter(self.json_keys)) if self.json_keys else self.field_name
    
    def aliases(self) -> Set[str]:
        """JSON keys that don't match field name."""
        return {k for k in self.json_keys if k != self.field_name}

@dataclass
class ClassAnalysis:
    """Analysis of a single Java class."""
    file_path: str
    class_name: str
    package: str
    fields: List[FieldMapping] = field(default_factory=list)
    has_to_json: bool = False
    has_from_json: bool = False
    manual_add_property_count: int = 0
    manual_json_has_count: int = 0
    is_record: bool = False
    extends: Optional[str] = None
    risk_level: str = "LOW"  # LOW, MEDIUM, HIGH
    notes: List[str] = field(default_factory=list)

# ═══════════════════════════════════════════════════════════════════════════════
# PATTERNS
# ═══════════════════════════════════════════════════════════════════════════════

# Field declaration: private float radius = 3.0f;
FIELD_PATTERN = re.compile(
    r'private\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*(?:=\s*([^;]+))?;'
)

# json.addProperty("key", value)
ADD_PROPERTY_PATTERN = re.compile(r'json\.addProperty\s*\(\s*"(\w+)"')

# json.has("key")
JSON_HAS_PATTERN = re.compile(r'json\.has\s*\(\s*"(\w+)"\s*\)')

# json.get("key").getAsXxx()
JSON_GET_PATTERN = re.compile(r'json\.get\s*\(\s*"(\w+)"\s*\)\.getAs(\w+)\s*\(\s*\)')

# state.setXxx(json.get("key"))
STATE_SET_PATTERN = re.compile(r'state\.set(\w+)\s*\(\s*json\.get\s*\(\s*"(\w+)"\s*\)')

# Setter: public void setRadius(float r)
SETTER_PATTERN = re.compile(r'public\s+void\s+set(\w+)\s*\(')

# Getter: public float getRadius()
GETTER_PATTERN = re.compile(r'public\s+(\w+)\s+(?:get|is)(\w+)\s*\(\s*\)')

# Class declaration
CLASS_PATTERN = re.compile(r'public\s+(?:final\s+)?(?:class|record)\s+(\w+)(?:\s+extends\s+(\w+))?')

# Package
PACKAGE_PATTERN = re.compile(r'package\s+([\w.]+);')

# toJson method
TO_JSON_PATTERN = re.compile(r'public\s+(?:static\s+)?JsonObject\s+toJson\s*\(')

# fromJson method
FROM_JSON_PATTERN = re.compile(r'public\s+static\s+\w+\s+fromJson\s*\(')

# ═══════════════════════════════════════════════════════════════════════════════
# ANALYSIS
# ═══════════════════════════════════════════════════════════════════════════════

def analyze_file(filepath: Path) -> Optional[ClassAnalysis]:
    """Analyze a single Java file."""
    try:
        content = filepath.read_text(encoding='utf-8')
    except Exception as e:
        return None
    
    # Basic info
    package_match = PACKAGE_PATTERN.search(content)
    class_match = CLASS_PATTERN.search(content)
    
    if not class_match:
        return None
    
    analysis = ClassAnalysis(
        file_path=str(filepath),
        class_name=class_match.group(1),
        package=package_match.group(1) if package_match else "",
        extends=class_match.group(2),
        is_record='record' in content[:content.find(class_match.group(0)) + 100]
    )
    
    # Check for toJson/fromJson
    analysis.has_to_json = bool(TO_JSON_PATTERN.search(content))
    analysis.has_from_json = bool(FROM_JSON_PATTERN.search(content))
    
    # Count manual mappings
    analysis.manual_add_property_count = len(ADD_PROPERTY_PATTERN.findall(content))
    analysis.manual_json_has_count = len(JSON_HAS_PATTERN.findall(content))
    
    # Skip if no JSON operations
    if analysis.manual_add_property_count == 0 and analysis.manual_json_has_count == 0:
        return None
    
    # Extract fields
    fields_dict: Dict[str, FieldMapping] = {}
    
    for match in FIELD_PATTERN.finditer(content):
        field_type, field_name, default_val = match.groups()
        fields_dict[field_name] = FieldMapping(
            field_name=field_name,
            field_type=field_type,
            default_value=default_val.strip() if default_val else None
        )
    
    # Map JSON keys to fields via addProperty
    for json_key in ADD_PROPERTY_PATTERN.findall(content):
        # Try to find matching field
        if json_key in fields_dict:
            fields_dict[json_key].json_keys.add(json_key)
        else:
            # JSON key doesn't match any field name - might be camelCase mismatch
            # Look for field that matches when lowercased
            for fname, fmap in fields_dict.items():
                if fname.lower() == json_key.lower():
                    fmap.json_keys.add(json_key)
                    fmap.needs_alias = True
                    break
    
    # Map JSON keys from json.has patterns
    for json_key in JSON_HAS_PATTERN.findall(content):
        if json_key in fields_dict:
            fields_dict[json_key].json_keys.add(json_key)
        else:
            for fname, fmap in fields_dict.items():
                if fname.lower() == json_key.lower():
                    fmap.json_keys.add(json_key)
                    fmap.needs_alias = True
                    break
    
    # Check state.setXxx patterns (for FragmentRegistry etc)
    for setter_name, json_key in STATE_SET_PATTERN.findall(content):
        # setter_name is like "SphereLatSteps", json_key is like "latSteps"
        field_name = setter_name[0].lower() + setter_name[1:]  # sphereLatSteps
        if field_name in fields_dict:
            fields_dict[field_name].json_keys.add(json_key)
            if json_key != field_name:
                fields_dict[field_name].needs_alias = True
    
    # Find setters/getters
    for match in SETTER_PATTERN.finditer(content):
        prop_name = match.group(1)
        field_name = prop_name[0].lower() + prop_name[1:]
        if field_name in fields_dict:
            fields_dict[field_name].setter_name = f"set{prop_name}"
    
    for match in GETTER_PATTERN.finditer(content):
        ret_type, prop_name = match.groups()
        field_name = prop_name[0].lower() + prop_name[1:]
        if field_name in fields_dict:
            fields_dict[field_name].getter_name = f"get{prop_name}" if ret_type != 'boolean' else f"is{prop_name}"
    
    analysis.fields = list(fields_dict.values())
    
    # Risk assessment
    if analysis.manual_add_property_count > 20 or analysis.manual_json_has_count > 20:
        analysis.risk_level = "HIGH"
    elif analysis.manual_add_property_count > 5 or analysis.manual_json_has_count > 5:
        analysis.risk_level = "MEDIUM"
    
    # Notes
    if any(f.needs_alias for f in analysis.fields):
        analysis.notes.append(f"Has {sum(1 for f in analysis.fields if f.needs_alias)} fields needing aliases")
    
    if analysis.is_record:
        analysis.notes.append("Is a record - may need special handling")
    
    return analysis

def scan_directory(root: Path) -> List[ClassAnalysis]:
    """Scan all Java files in directory."""
    results = []
    
    for java_file in root.rglob("*.java"):
        # Skip build directories
        if 'build' in java_file.parts or '.gradle' in java_file.parts:
            continue
        
        analysis = analyze_file(java_file)
        if analysis:
            results.append(analysis)
    
    return results

# ═══════════════════════════════════════════════════════════════════════════════
# REPORT GENERATION
# ═══════════════════════════════════════════════════════════════════════════════

def generate_report(analyses: List[ClassAnalysis]) -> str:
    """Generate detailed audit report."""
    lines = []
    
    lines.append("=" * 80)
    lines.append("JSON SERIALIZATION REFACTOR - DRY RUN AUDIT")
    lines.append("=" * 80)
    lines.append("")
    
    # Summary
    total_files = len(analyses)
    total_add_property = sum(a.manual_add_property_count for a in analyses)
    total_json_has = sum(a.manual_json_has_count for a in analyses)
    high_risk = sum(1 for a in analyses if a.risk_level == "HIGH")
    medium_risk = sum(1 for a in analyses if a.risk_level == "MEDIUM")
    needs_aliases = sum(1 for a in analyses if any(f.needs_alias for f in a.fields))
    
    lines.append("SUMMARY")
    lines.append("-" * 40)
    lines.append(f"  Files with manual JSON mapping: {total_files}")
    lines.append(f"  Total addProperty() calls: {total_add_property}")
    lines.append(f"  Total json.has() calls: {total_json_has}")
    lines.append(f"  Files needing aliases: {needs_aliases}")
    lines.append(f"  Risk: {high_risk} HIGH, {medium_risk} MEDIUM, {total_files - high_risk - medium_risk} LOW")
    lines.append("")
    
    # Categorize files
    categories = {
        "GUI State (FieldEditState)": [],
        "GUI Fragment/Preset Registries": [],
        "Config Classes (visual/animation/etc)": [],
        "Shape Classes": [],
        "Field System": [],
        "Network Handlers": [],
        "Other": []
    }
    
    for a in analyses:
        if "FieldEditState" in a.class_name:
            categories["GUI State (FieldEditState)"].append(a)
        elif "Registry" in a.class_name or "Handlers" in a.class_name:
            if "Fragment" in a.class_name or "Preset" in a.class_name:
                categories["GUI Fragment/Preset Registries"].append(a)
            elif "Client" in a.file_path:
                categories["Network Handlers"].append(a)
            else:
                categories["Other"].append(a)
        elif "Shape" in a.class_name:
            categories["Shape Classes"].append(a)
        elif "Config" in a.class_name or "visual" in a.file_path or "animation" in a.file_path:
            categories["Config Classes (visual/animation/etc)"].append(a)
        elif "field" in a.file_path.lower():
            categories["Field System"].append(a)
        else:
            categories["Other"].append(a)
    
    # Detailed breakdown by category
    lines.append("=" * 80)
    lines.append("DETAILED BREAKDOWN BY CATEGORY")
    lines.append("=" * 80)
    
    for category, items in categories.items():
        if not items:
            continue
        
        lines.append("")
        lines.append(f"### {category} ({len(items)} files)")
        lines.append("")
        
        for a in sorted(items, key=lambda x: -x.manual_add_property_count - x.manual_json_has_count):
            short_path = a.file_path.split('src')[-1] if 'src' in a.file_path else a.file_path
            risk_icon = "🔴" if a.risk_level == "HIGH" else "🟡" if a.risk_level == "MEDIUM" else "🟢"
            
            lines.append(f"  {risk_icon} {a.class_name}")
            lines.append(f"     Path: {short_path}")
            lines.append(f"     Manual mappings: {a.manual_add_property_count} write, {a.manual_json_has_count} read")
            
            # Fields needing aliases
            alias_fields = [f for f in a.fields if f.needs_alias]
            if alias_fields:
                lines.append(f"     Needs aliases ({len(alias_fields)}):")
                for f in alias_fields[:5]:
                    aliases = f.aliases()
                    lines.append(f"       - {f.field_name} ← JSON: {aliases}")
                if len(alias_fields) > 5:
                    lines.append(f"       ... and {len(alias_fields) - 5} more")
            
            if a.notes:
                for note in a.notes:
                    lines.append(f"     ⚠️  {note}")
            
            lines.append("")
    
    # Proposed solution
    lines.append("=" * 80)
    lines.append("PROPOSED REFACTOR STRATEGY")
    lines.append("=" * 80)
    lines.append("")
    lines.append("PHASE 1: Create JsonSerializer utility")
    lines.append("  - Generic toJson(Object) using Gson reflection")
    lines.append("  - Generic fromJson(JsonObject, Class) using Gson")
    lines.append("  - @JsonField annotation for aliases")
    lines.append("")
    lines.append("PHASE 2: Refactor simple config classes (LOW risk)")
    lines.append("  - Replace manual toJson/fromJson with utility calls")
    lines.append("  - Estimated: 30+ files")
    lines.append("")
    lines.append("PHASE 3: Refactor FieldEditState (MEDIUM risk)")
    lines.append("  - Add @JsonField annotations where needed")
    lines.append("  - Replace toStateJson/fromStateJson")
    lines.append("")
    lines.append("PHASE 4: Refactor FragmentRegistry/PresetRegistry (MEDIUM risk)")
    lines.append("  - These apply JSON to FieldEditState with prefix mapping")
    lines.append("  - Need context-aware application (shape type → prefix)")
    lines.append("")
    
    # What would change - example
    lines.append("=" * 80)
    lines.append("EXAMPLE: What SpinConfig.java would look like")
    lines.append("=" * 80)
    lines.append("")
    lines.append("BEFORE:")
    lines.append("```java")
    lines.append("public JsonObject toJson() {")
    lines.append("    JsonObject json = new JsonObject();")
    lines.append('    json.addProperty("axis", axis.name());')
    lines.append('    json.addProperty("speed", speed);')
    lines.append('    json.addProperty("oscillate", oscillate);')
    lines.append("    return json;")
    lines.append("}")
    lines.append("```")
    lines.append("")
    lines.append("AFTER:")
    lines.append("```java")
    lines.append("public JsonObject toJson() {")
    lines.append("    return JsonSerializer.toJson(this);")
    lines.append("}")
    lines.append("```")
    lines.append("")
    
    # Risk matrix
    lines.append("=" * 80)
    lines.append("RISK MATRIX")
    lines.append("=" * 80)
    lines.append("")
    lines.append("| Risk | Description | Mitigation |")
    lines.append("|------|-------------|------------|")
    lines.append("| Enum serialization | Enums need .name() | Gson handles this automatically |")
    lines.append("| Nested objects | JsonObject within object | Custom TypeAdapter if needed |")
    lines.append("| Default values | Need to preserve defaults | Gson skips null, keeps defaults |")
    lines.append("| Backward compat | Old JSON files still load | Aliases handle key changes |")
    lines.append("| Performance | Reflection overhead | Negligible, cached after first use |")
    lines.append("")
    
    return "\n".join(lines)

def generate_json_report(analyses: List[ClassAnalysis]) -> dict:
    """Generate machine-readable JSON report."""
    return {
        "summary": {
            "total_files": len(analyses),
            "total_add_property": sum(a.manual_add_property_count for a in analyses),
            "total_json_has": sum(a.manual_json_has_count for a in analyses),
            "high_risk_files": sum(1 for a in analyses if a.risk_level == "HIGH"),
            "files_needing_aliases": sum(1 for a in analyses if any(f.needs_alias for f in a.fields)),
        },
        "files": [
            {
                "path": a.file_path.split('src')[-1] if 'src' in a.file_path else a.file_path,
                "class": a.class_name,
                "risk": a.risk_level,
                "add_property_count": a.manual_add_property_count,
                "json_has_count": a.manual_json_has_count,
                "fields_needing_aliases": [
                    {
                        "field": f.field_name,
                        "aliases": list(f.aliases())
                    }
                    for f in a.fields if f.needs_alias
                ],
                "notes": a.notes
            }
            for a in analyses
        ]
    }

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════

def main():
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    src_dir = project_root / 'src'
    
    if not src_dir.exists():
        print(f"ERROR: src directory not found at {src_dir}")
        return
    
    print(f"Scanning: {src_dir}")
    print("This is a DRY RUN - no files will be modified.")
    print()
    
    analyses = scan_directory(src_dir)
    
    # Generate text report
    report = generate_report(analyses)
    print(report)
    
    # Save JSON report for phase 2
    json_report = generate_json_report(analyses)
    report_path = script_dir / "json_refactor_audit.json"
    with open(report_path, 'w') as f:
        json.dump(json_report, f, indent=2)
    print(f"\nJSON report saved to: {report_path}")
    print("This will be used by the refactor script in Phase 2.")

if __name__ == '__main__':
    main()

