#!/usr/bin/env python3
"""
GUI Unused Code Audit Script

This script audits the GUI codebase to find:
1. Backup files (.bak) that should be removed
2. Unused classes (not referenced anywhere)
3. Duplicate/parallel class hierarchies (v2/ vs sub/)
4. Dead code patterns (unused methods, imports)
5. Empty or stub implementations

Run:
  python scripts/audit_gui_unused.py

Output:
  - Console summary
  - Detailed JSON report: scripts/gui_unused_audit_report.json
"""

from __future__ import annotations

import json
import os
import re
from pathlib import Path
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Set, Optional, Tuple
from collections import defaultdict

PROJECT_ROOT = Path(__file__).parent.parent
GUI_DIR = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui"
CLIENT_SRC = PROJECT_ROOT / "src/client/java"
MAIN_SRC = PROJECT_ROOT / "src/main/java"


@dataclass
class BackupFile:
    """Represents a backup file that could be cleaned up"""
    path: str
    size_bytes: int
    original_exists: bool


@dataclass 
class UnusedClass:
    """Represents a class that appears to be unused"""
    name: str
    path: str
    line_count: int
    reason: str
    references_found: int = 0


@dataclass
class DuplicatePair:
    """Represents a potential duplicate class pair"""
    name: str
    path1: str
    path2: str
    lines1: int
    lines2: int
    similarity_reason: str


@dataclass
class DeadCode:
    """Represents dead code (unused methods, fields, etc.)"""
    file: str
    type: str  # 'method', 'field', 'import'
    name: str
    line: int
    reason: str


@dataclass
class AuditReport:
    """Complete audit report"""
    backup_files: List[BackupFile] = field(default_factory=list)
    unused_classes: List[UnusedClass] = field(default_factory=list)
    duplicate_pairs: List[DuplicatePair] = field(default_factory=list)
    dead_code: List[DeadCode] = field(default_factory=list)
    summary: Dict = field(default_factory=dict)
    
    def to_dict(self):
        return {
            'backup_files': [asdict(b) for b in self.backup_files],
            'unused_classes': [asdict(u) for u in self.unused_classes],
            'duplicate_pairs': [asdict(d) for d in self.duplicate_pairs],
            'dead_code': [asdict(d) for d in self.dead_code],
            'summary': self.summary
        }


def find_backup_files(directory: Path) -> List[BackupFile]:
    """Find all .bak and similar backup files"""
    backups = []
    backup_patterns = ['*.bak', '*.java.bak', '*.bak.original', '*~', '*.orig']
    
    for pattern in backup_patterns:
        for f in directory.rglob(pattern):
            original_path = str(f).replace('.bak.original', '').replace('.bak', '').replace('~', '').replace('.orig', '')
            original_exists = Path(original_path).exists() if original_path != str(f) else False
            
            backups.append(BackupFile(
                path=str(f.relative_to(PROJECT_ROOT)),
                size_bytes=f.stat().st_size,
                original_exists=original_exists
            ))
    
    return backups


def get_class_name_from_file(path: Path) -> Optional[str]:
    """Extract class name from Java file"""
    try:
        content = path.read_text(encoding='utf-8', errors='ignore')
        match = re.search(r'public\s+(?:final\s+)?(?:abstract\s+)?(?:class|interface|record|enum)\s+(\w+)', content)
        if match:
            return match.group(1)
    except:
        pass
    return path.stem


def count_lines(path: Path) -> int:
    """Count lines in a file"""
    try:
        return len(path.read_text(encoding='utf-8', errors='ignore').split('\n'))
    except:
        return 0


def find_class_references(class_name: str, search_dirs: List[Path]) -> int:
    """Count how many times a class is referenced in the codebase"""
    count = 0
    patterns = [
        rf'\b{class_name}\b',  # Direct reference
        rf'new\s+{class_name}\s*\(',  # Constructor
        rf'extends\s+{class_name}\b',  # Inheritance
        rf'implements\s+.*{class_name}',  # Implementation
        rf'import\s+.*\.{class_name}\s*;',  # Import
    ]
    
    combined_pattern = '|'.join(patterns)
    
    for search_dir in search_dirs:
        for java_file in search_dir.rglob('*.java'):
            if '.bak' in str(java_file):
                continue
            try:
                content = java_file.read_text(encoding='utf-8', errors='ignore')
                matches = re.findall(combined_pattern, content)
                count += len(matches)
            except:
                pass
    
    return count


def analyze_v2_vs_sub_panels() -> Tuple[List[DuplicatePair], List[UnusedClass]]:
    """Analyze panel/v2/ vs panel/sub/ for duplicates and unused"""
    duplicates = []
    unused = []
    
    sub_dir = GUI_DIR / "panel/sub"
    v2_dir = GUI_DIR / "panel/v2"
    
    if not sub_dir.exists() or not v2_dir.exists():
        return duplicates, unused
    
    sub_classes = {f.stem: f for f in sub_dir.glob('*.java') if not str(f).endswith('.bak')}
    v2_classes = {f.stem: f for f in v2_dir.glob('*.java') if not str(f).endswith('.bak')}
    
    # First, check if ANY v2 import exists in the codebase
    v2_import_pattern = r'import\s+net\.cyberpunk042\.client\.gui\.panel\.v2\.'
    v2_is_imported = False
    for java_file in CLIENT_SRC.rglob('*.java'):
        if '.bak' in str(java_file) or 'panel/v2/' in str(java_file):
            continue
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            if re.search(v2_import_pattern, content):
                v2_is_imported = True
                break
        except:
            pass
    
    # If v2 is never imported, ALL v2 classes are unused
    if not v2_is_imported:
        for v2_name, v2_path in v2_classes.items():
            unused.append(UnusedClass(
                name=v2_name,
                path=str(v2_path.relative_to(PROJECT_ROOT)),
                line_count=count_lines(v2_path),
                reason="ENTIRE v2/ directory is never imported - candidate for removal",
                references_found=0
            ))
    
    # Still record duplicates for documentation
    for sub_name, sub_path in sub_classes.items():
        base_name = sub_name.replace('SubPanel', 'Panel')
        if base_name in v2_classes:
            v2_path = v2_classes[base_name]
            duplicates.append(DuplicatePair(
                name=base_name,
                path1=str(sub_path.relative_to(PROJECT_ROOT)),
                path2=str(v2_path.relative_to(PROJECT_ROOT)),
                lines1=count_lines(sub_path),
                lines2=count_lines(v2_path),
                similarity_reason=f"v2/{base_name} duplicates sub/{sub_name} - sub/ is used, v2/ is unused"
            ))
    
    return duplicates, unused


def find_unused_widget_classes() -> List[UnusedClass]:
    """Find unused widget classes"""
    unused = []
    widget_dir = GUI_DIR / "widget"
    
    if not widget_dir.exists():
        return unused
    
    for widget_file in widget_dir.glob('*.java'):
        if '.bak' in str(widget_file):
            continue
        
        class_name = get_class_name_from_file(widget_file)
        if not class_name:
            continue
        
        refs = find_class_references(class_name, [GUI_DIR, CLIENT_SRC])
        external_refs = refs - 1  # Subtract self
        
        if external_refs <= 0:
            unused.append(UnusedClass(
                name=class_name,
                path=str(widget_file.relative_to(PROJECT_ROOT)),
                line_count=count_lines(widget_file),
                reason="Widget with no external references",
                references_found=external_refs
            ))
    
    return unused


def find_unused_util_classes() -> List[UnusedClass]:
    """Find unused utility classes"""
    unused = []
    util_dir = GUI_DIR / "util"
    
    if not util_dir.exists():
        return unused
    
    for util_file in util_dir.glob('*.java'):
        if '.bak' in str(util_file):
            continue
        
        class_name = get_class_name_from_file(util_file)
        if not class_name:
            continue
        
        refs = find_class_references(class_name, [GUI_DIR, CLIENT_SRC])
        external_refs = refs - 1
        
        if external_refs <= 1:  # Allow 1 for potential internal calls
            unused.append(UnusedClass(
                name=class_name,
                path=str(util_file.relative_to(PROJECT_ROOT)),
                line_count=count_lines(util_file),
                reason="Utility class with few/no references",
                references_found=external_refs
            ))
    
    return unused


def find_unused_component_classes() -> List[UnusedClass]:
    """Find unused component classes"""
    unused = []
    component_dir = GUI_DIR / "component"
    
    if not component_dir.exists():
        return unused
    
    for comp_file in component_dir.glob('*.java'):
        if '.bak' in str(comp_file):
            continue
        
        class_name = get_class_name_from_file(comp_file)
        if not class_name:
            continue
        
        refs = find_class_references(class_name, [GUI_DIR, CLIENT_SRC])
        external_refs = refs - 1
        
        if external_refs <= 0:
            unused.append(UnusedClass(
                name=class_name,
                path=str(comp_file.relative_to(PROJECT_ROOT)),
                line_count=count_lines(comp_file),
                reason="Component with no external references",
                references_found=external_refs
            ))
    
    return unused


def find_unused_layout_classes() -> List[UnusedClass]:
    """Find unused layout classes"""
    unused = []
    layout_dir = GUI_DIR / "layout"
    
    if not layout_dir.exists():
        return unused
    
    for layout_file in layout_dir.glob('*.java'):
        if '.bak' in str(layout_file):
            continue
        if layout_file.stem == 'package-info':
            continue
        
        class_name = get_class_name_from_file(layout_file)
        if not class_name:
            continue
        
        refs = find_class_references(class_name, [GUI_DIR, CLIENT_SRC])
        external_refs = refs - 1
        
        if external_refs <= 0:
            unused.append(UnusedClass(
                name=class_name,
                path=str(layout_file.relative_to(PROJECT_ROOT)),
                line_count=count_lines(layout_file),
                reason="Layout class with no external references",
                references_found=external_refs
            ))
    
    return unused


def find_dead_imports(java_files: List[Path]) -> List[DeadCode]:
    """Find unused imports (basic heuristic - import not used in file)"""
    dead_imports = []
    
    for java_file in java_files:
        if '.bak' in str(java_file):
            continue
        
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            lines = content.split('\n')
            
            # Find all imports
            for i, line in enumerate(lines, 1):
                import_match = re.match(r'\s*import\s+([\w.]+)\.(\w+)\s*;', line)
                if import_match:
                    imported_class = import_match.group(2)
                    
                    # Check if it's used in the file (simple heuristic)
                    # Remove the import line and check if class is referenced
                    content_without_import = '\n'.join(lines[:i-1] + lines[i:])
                    if not re.search(rf'\b{imported_class}\b', content_without_import):
                        dead_imports.append(DeadCode(
                            file=str(java_file.relative_to(PROJECT_ROOT)),
                            type='import',
                            name=f"{import_match.group(1)}.{imported_class}",
                            line=i,
                            reason=f"Class '{imported_class}' not used in file"
                        ))
        except:
            pass
    
    return dead_imports


def analyze_all_gui_files() -> List[Path]:
    """Get all Java files in GUI directory"""
    java_files = []
    for f in GUI_DIR.rglob('*.java'):
        if not '.bak' in str(f):
            java_files.append(f)
    return java_files


def find_empty_implementations() -> List[DeadCode]:
    """Find empty or stub method implementations"""
    empty_methods = []
    
    for java_file in GUI_DIR.rglob('*.java'):
        if '.bak' in str(java_file):
            continue
        
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            lines = content.split('\n')
            
            # Pattern for methods with empty bodies or just return statements
            i = 0
            while i < len(lines):
                line = lines[i]
                # Check for method declarations
                method_match = re.match(r'\s*(?:public|private|protected)\s+[\w<>\[\]]+\s+(\w+)\s*\([^)]*\)\s*\{\s*$', line)
                if method_match:
                    method_name = method_match.group(1)
                    # Check next non-empty line
                    j = i + 1
                    while j < len(lines) and lines[j].strip() == '':
                        j += 1
                    if j < len(lines):
                        next_line = lines[j].strip()
                        if next_line == '}' or next_line.startswith('return;') or next_line.startswith('return null;'):
                            # Skip common valid empty methods
                            if method_name not in ['close', 'tick', 'init', 'render', 'removed']:
                                empty_methods.append(DeadCode(
                                    file=str(java_file.relative_to(PROJECT_ROOT)),
                                    type='method',
                                    name=method_name,
                                    line=i + 1,
                                    reason="Empty or trivial implementation"
                                ))
                i += 1
        except:
            pass
    
    return empty_methods


def run_audit() -> AuditReport:
    """Run the complete audit"""
    report = AuditReport()
    
    print("🔍 Running GUI Unused Code Audit...\n")
    
    # 1. Find backup files
    print("  [1/7] Scanning for backup files...")
    report.backup_files = find_backup_files(GUI_DIR)
    print(f"        Found {len(report.backup_files)} backup files")
    
    # 2. Analyze v2 vs sub panels
    print("  [2/7] Analyzing panel/v2 vs panel/sub...")
    duplicates, v2_unused = analyze_v2_vs_sub_panels()
    report.duplicate_pairs.extend(duplicates)
    report.unused_classes.extend(v2_unused)
    print(f"        Found {len(duplicates)} duplicate pairs, {len(v2_unused)} unused v2 panels")
    
    # 3. Find unused widgets
    print("  [3/7] Scanning for unused widgets...")
    widget_unused = find_unused_widget_classes()
    report.unused_classes.extend(widget_unused)
    print(f"        Found {len(widget_unused)} potentially unused widgets")
    
    # 4. Find unused utils
    print("  [4/7] Scanning for unused utilities...")
    util_unused = find_unused_util_classes()
    report.unused_classes.extend(util_unused)
    print(f"        Found {len(util_unused)} potentially unused utilities")
    
    # 5. Find unused components
    print("  [5/7] Scanning for unused components...")
    component_unused = find_unused_component_classes()
    report.unused_classes.extend(component_unused)
    print(f"        Found {len(component_unused)} potentially unused components")
    
    # 6. Find unused layouts
    print("  [6/7] Scanning for unused layout classes...")
    layout_unused = find_unused_layout_classes()
    report.unused_classes.extend(layout_unused)
    print(f"        Found {len(layout_unused)} potentially unused layout classes")
    
    # 7. Find empty implementations  
    print("  [7/7] Scanning for empty implementations...")
    empty_impls = find_empty_implementations()
    report.dead_code.extend(empty_impls)
    print(f"        Found {len(empty_impls)} empty method implementations")
    
    # Calculate summary
    total_backup_size = sum(b.size_bytes for b in report.backup_files)
    total_unused_lines = sum(u.line_count for u in report.unused_classes)
    
    report.summary = {
        'backup_files_count': len(report.backup_files),
        'backup_files_size_kb': round(total_backup_size / 1024, 2),
        'unused_classes_count': len(report.unused_classes),
        'unused_classes_lines': total_unused_lines,
        'duplicate_pairs_count': len(report.duplicate_pairs),
        'dead_code_count': len(report.dead_code),
        'cleanup_potential_lines': total_unused_lines + len(report.dead_code)
    }
    
    return report


def print_report(report: AuditReport):
    """Print a formatted report to console"""
    print("\n" + "=" * 80)
    print("📊 GUI UNUSED CODE AUDIT REPORT")
    print("=" * 80)
    
    # Backup files
    print("\n🗂️  BACKUP FILES TO REMOVE")
    print("-" * 40)
    if report.backup_files:
        for b in report.backup_files:
            status = "✓ orig exists" if b.original_exists else "⚠ orig missing"
            print(f"   {b.path} ({b.size_bytes:,} bytes) [{status}]")
    else:
        print("   ✅ No backup files found")
    
    # Unused classes
    print("\n🚫 POTENTIALLY UNUSED CLASSES")
    print("-" * 40)
    if report.unused_classes:
        # Group by reason
        by_reason = defaultdict(list)
        for u in report.unused_classes:
            by_reason[u.reason].append(u)
        
        for reason, classes in by_reason.items():
            print(f"\n   [{reason}]")
            for u in classes:
                print(f"      • {u.name} ({u.line_count} lines) - refs: {u.references_found}")
                print(f"        {u.path}")
    else:
        print("   ✅ No unused classes detected")
    
    # Duplicate pairs
    print("\n🔀 DUPLICATE/PARALLEL CLASSES")
    print("-" * 40)
    if report.duplicate_pairs:
        for d in report.duplicate_pairs:
            print(f"\n   {d.name}")
            print(f"      Path 1: {d.path1} ({d.lines1} lines)")
            print(f"      Path 2: {d.path2} ({d.lines2} lines)")
            print(f"      Reason: {d.similarity_reason}")
    else:
        print("   ✅ No duplicate pairs detected")
    
    # Dead code samples
    print("\n💀 DEAD CODE (first 10)")
    print("-" * 40)
    if report.dead_code:
        for d in report.dead_code[:10]:
            print(f"   {d.type}: {d.name} @ L{d.line}")
            print(f"      {d.file}")
            print(f"      Reason: {d.reason}")
        if len(report.dead_code) > 10:
            print(f"\n   ... and {len(report.dead_code) - 10} more")
    else:
        print("   ✅ No significant dead code detected")
    
    # Summary
    print("\n" + "=" * 80)
    print("📈 SUMMARY")
    print("=" * 80)
    s = report.summary
    print(f"""
   Backup files:        {s['backup_files_count']} ({s['backup_files_size_kb']:.1f} KB)
   Unused classes:      {s['unused_classes_count']} ({s['unused_classes_lines']} lines)
   Duplicate pairs:     {s['duplicate_pairs_count']}
   Dead code items:     {s['dead_code_count']}
   
   🧹 Cleanup potential: ~{s['cleanup_potential_lines']} lines
""")
    
    print("=" * 80)
    print("✅ Audit complete!")
    print("=" * 80)


def save_report(report: AuditReport, output_path: Path):
    """Save report to JSON file"""
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(report.to_dict(), f, indent=2)
    print(f"\n📁 Detailed report saved to: {output_path}")


def main():
    report = run_audit()
    print_report(report)
    
    # Save detailed JSON report
    output_path = PROJECT_ROOT / "scripts" / "gui_unused_audit_report.json"
    save_report(report, output_path)
    
    # Return exit code based on findings
    if report.summary['cleanup_potential_lines'] > 100:
        print("\n⚠️  Significant cleanup opportunity detected!")
        return 1
    return 0


if __name__ == "__main__":
    exit(main())
