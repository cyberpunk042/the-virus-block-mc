#!/usr/bin/env python3
"""
Audit Script: Find all manual JSON ↔ Field mapping patterns

Looks for:
1. json.addProperty("fieldName", value)  - manual serialization
2. json.has("fieldName") ... getAsXxx()  - manual deserialization
3. state.setXxx(json.get(...))          - manual application

Goal: Identify all places that would break if we add a new field to FieldEditState
"""

import os
import re
from pathlib import Path
from collections import defaultdict

# Patterns to search for
PATTERNS = {
    'addProperty': re.compile(r'json\.addProperty\s*\(\s*"(\w+)"'),
    'json_has': re.compile(r'json\.has\s*\(\s*"(\w+)"\s*\)'),
    'getAs': re.compile(r'json\.get\s*\(\s*"(\w+)"\s*\)\s*\.getAs(\w+)\(\)'),
    'state_set_from_json': re.compile(r'state\.set(\w+)\s*\(\s*json\.get'),
}

def scan_file(filepath):
    """Scan a single file for patterns."""
    results = defaultdict(list)
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
    except Exception as e:
        return results
    
    for i, line in enumerate(lines, 1):
        for pattern_name, pattern in PATTERNS.items():
            matches = pattern.findall(line)
            if matches:
                results[pattern_name].append({
                    'line': i,
                    'matches': matches,
                    'content': line.strip()[:100]
                })
    
    return results

def scan_directory(root_dir, extensions=('.java',)):
    """Recursively scan directory for matching files."""
    all_results = {}
    
    for root, dirs, files in os.walk(root_dir):
        # Skip build directories
        dirs[:] = [d for d in dirs if d not in ('build', '.gradle', 'out')]
        
        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                filepath = Path(root) / file
                results = scan_file(filepath)
                if any(results.values()):
                    all_results[str(filepath)] = results
    
    return all_results

def print_report(results):
    """Print audit report."""
    print("=" * 80)
    print("JSON ↔ FIELD MAPPING AUDIT REPORT")
    print("=" * 80)
    print()
    
    summary = defaultdict(int)
    
    for filepath, patterns in sorted(results.items()):
        # Make path relative and readable
        short_path = filepath.split('src')[-1] if 'src' in filepath else filepath
        
        total_in_file = sum(len(v) for v in patterns.values())
        if total_in_file == 0:
            continue
            
        print(f"\n📁 {short_path}")
        print("-" * 60)
        
        for pattern_name, matches in patterns.items():
            if matches:
                print(f"  [{pattern_name}] - {len(matches)} occurrences")
                summary[pattern_name] += len(matches)
                
                # Show first few examples
                for match in matches[:3]:
                    fields = match['matches']
                    print(f"    Line {match['line']}: {fields}")
                if len(matches) > 3:
                    print(f"    ... and {len(matches) - 3} more")
    
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    for pattern, count in sorted(summary.items(), key=lambda x: -x[1]):
        risk = "🔴 HIGH" if count > 20 else "🟡 MEDIUM" if count > 5 else "🟢 LOW"
        print(f"  {risk} {pattern}: {count} occurrences")
    
    total = sum(summary.values())
    print(f"\n  TOTAL: {total} manual field mappings found")
    print()
    print("These are places that could break if you add a new field to FieldEditState")
    print("and forget to update the JSON mapping.")

def find_setter_field_mismatches(root_dir):
    """Find cases where JSON key != setter name (the prefix problem)."""
    print("\n" + "=" * 80)
    print("SETTER NAME MISMATCH ANALYSIS")
    print("=" * 80)
    
    # Pattern: state.setXxx(json.get("yyy"))
    pattern = re.compile(r'state\.set(\w+)\s*\(\s*json\.get\s*\(\s*"(\w+)"\s*\)')
    
    mismatches = []
    
    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in ('build', '.gradle', 'out')]
        
        for file in files:
            if file.endswith('.java'):
                filepath = Path(root) / file
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        for i, line in enumerate(f, 1):
                            matches = pattern.findall(line)
                            for setter_name, json_key in matches:
                                # Check if they match (case-insensitive)
                                if setter_name.lower() != json_key.lower():
                                    mismatches.append({
                                        'file': str(filepath).split('src')[-1],
                                        'line': i,
                                        'setter': f'set{setter_name}',
                                        'json_key': json_key
                                    })
                except:
                    pass
    
    if mismatches:
        print(f"\nFound {len(mismatches)} cases where JSON key ≠ setter name:\n")
        for m in mismatches[:20]:
            print(f"  {m['file']}:{m['line']}")
            print(f"    JSON: \"{m['json_key']}\" → Setter: {m['setter']}()")
        if len(mismatches) > 20:
            print(f"\n  ... and {len(mismatches) - 20} more")
    else:
        print("\n  No mismatches found (all JSON keys match setter names)")

def main():
    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    src_dir = project_root / 'src'
    
    if not src_dir.exists():
        print(f"ERROR: src directory not found at {src_dir}")
        return
    
    print(f"Scanning: {src_dir}")
    print()
    
    results = scan_directory(src_dir)
    print_report(results)
    find_setter_field_mismatches(src_dir)
    
    print("\n" + "=" * 80)
    print("RECOMMENDATIONS")
    print("=" * 80)
    print("""
1. HIGH PRIORITY: FieldEditState.toStateJson() / fromStateJson()
   - Replace manual addProperty/has calls with Gson reflection
   - Single source of truth: field definitions = serialized fields

2. MEDIUM PRIORITY: FragmentRegistry.applyXxxPreset() methods
   - The JSON key → setter name mismatch is by design (context-based naming)
   - Could create a mapping table or use annotations
   - Or: Change JSON to use full names (sphereLatSteps instead of latSteps)

3. Consider: Create a shared utility class for JSON ↔ Object mapping
   - JsonStateApplicator.apply(json, state, prefix)
   - Would handle the prefix mapping automatically
""")

if __name__ == '__main__':
    main()

