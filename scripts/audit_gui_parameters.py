#!/usr/bin/env python3
"""
Audit GUI Parameters: Compare GuiState fields vs actual GUI controls.

Generates a report showing:
1. What's in GuiState
2. What has GUI sliders/controls
3. What's missing from the GUI
"""

import os
import re
from pathlib import Path
from collections import defaultdict

PROJECT_ROOT = Path(__file__).parent.parent
GUI_STATE = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/state/GuiState.java"
SUB_PANELS = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/panel/sub"
PARAMS_DOC = PROJECT_ROOT / "docs/field-system/NEW_REFACTORING_NEW_PHASES/03_PARAMETERS.md"

def extract_guistate_fields():
    """Extract all fields from GuiState.java"""
    content = GUI_STATE.read_text(encoding='utf-8')
    
    # Match private fields: private Type name = ...
    pattern = r'private\s+([\w<>]+)\s+(\w+)\s*='
    
    fields = {}
    current_section = "General"
    
    for line in content.split('\n'):
        # Detect section comments
        if '// ' in line and any(x in line.upper() for x in ['SHAPE', 'SPHERE', 'RING', 'DISC', 'PRISM', 'CYLINDER', 'POLY', 'BEAM', 'ANIMATION', 'TRANSFORM', 'VISIBILITY', 'FILL', 'ARRANGEMENT', 'LINKING', 'PREDICTION', 'FOLLOW', 'LIFECYCLE', 'TRIGGER', 'APPEARANCE']):
            section_match = re.search(r'//\s*(.+)', line)
            if section_match:
                current_section = section_match.group(1).strip()
        
        match = re.search(pattern, line)
        if match:
            field_type = match.group(1)
            field_name = match.group(2)
            
            # Skip internal/UI fields
            if field_name in ['isDirty', 'debugUnlocked', 'undoStack', 'redoStack', 'profiles', 'layers']:
                continue
            if field_name.startswith('current') or field_name.startswith('selected'):
                continue
                
            fields[field_name] = {
                'type': field_type,
                'section': current_section,
                'has_getter': f'get{field_name[0].upper()}{field_name[1:]}' in content or f'is{field_name[0].upper()}{field_name[1:]}' in content,
                'has_setter': f'set{field_name[0].upper()}{field_name[1:]}' in content
            }
    
    return fields

def find_gui_controls():
    """Find which GuiState fields are referenced in sub-panels with actual controls."""
    controls = set()
    
    for panel_file in SUB_PANELS.glob("*.java"):
        content = panel_file.read_text(encoding='utf-8')
        
        # Look for state.setXxx or state.getXxx calls that indicate a control exists
        setter_pattern = r'state\.set(\w+)\('
        getter_pattern = r'state\.get(\w+)\('
        is_pattern = r'state\.is(\w+)\('
        
        for match in re.finditer(setter_pattern, content):
            field = match.group(1)
            field = field[0].lower() + field[1:]  # Convert to field name
            controls.add(field)
        
        for match in re.finditer(getter_pattern, content):
            field = match.group(1)
            field = field[0].lower() + field[1:]
            controls.add(field)
            
        for match in re.finditer(is_pattern, content):
            field = match.group(1)
            field = field[0].lower() + field[1:]
            controls.add(field)
    
    return controls

def categorize_fields(fields, controls):
    """Categorize fields into sections and mark which have controls."""
    categories = defaultdict(list)
    
    for field_name, info in fields.items():
        has_control = field_name in controls
        categories[info['section']].append({
            'name': field_name,
            'type': info['type'],
            'has_control': has_control
        })
    
    return categories

def main():
    print("=" * 80)
    print("GUI PARAMETER AUDIT")
    print("=" * 80)
    
    # Extract data
    fields = extract_guistate_fields()
    controls = find_gui_controls()
    categories = categorize_fields(fields, controls)
    
    # Stats
    total_fields = len(fields)
    with_controls = len([f for f in fields if f in controls])
    missing_controls = total_fields - with_controls
    
    print(f"\n📊 SUMMARY")
    print(f"   Total GuiState fields: {total_fields}")
    print(f"   With GUI controls:     {with_controls} ✅")
    print(f"   Missing GUI controls:  {missing_controls} ❌")
    print(f"   Coverage:              {with_controls/total_fields*100:.1f}%")
    
    print("\n" + "=" * 80)
    print("MISSING GUI CONTROLS BY CATEGORY")
    print("=" * 80)
    
    missing_by_category = defaultdict(list)
    
    for section, items in sorted(categories.items()):
        missing = [i for i in items if not i['has_control']]
        if missing:
            missing_by_category[section] = missing
    
    for section, items in sorted(missing_by_category.items()):
        print(f"\n🔸 {section}")
        for item in items:
            print(f"   ❌ {item['name']} ({item['type']})")
    
    print("\n" + "=" * 80)
    print("FIELDS WITH GUI CONTROLS (for reference)")
    print("=" * 80)
    
    for section, items in sorted(categories.items()):
        present = [i for i in items if i['has_control']]
        if present:
            print(f"\n✅ {section}: {len(present)} controls")
            for item in present[:5]:  # Show first 5
                print(f"   • {item['name']}")
            if len(present) > 5:
                print(f"   ... and {len(present)-5} more")
    
    # Generate TODO list
    print("\n" + "=" * 80)
    print("IMPLEMENTATION TODO")
    print("=" * 80)
    
    priority_sections = ['Sphere', 'Ring', 'Disc', 'Prism', 'Cylinder', 'Poly', 'Field modifiers']
    
    print("\n🎯 HIGH PRIORITY (Shape Parameters):")
    for section in priority_sections:
        if section in missing_by_category:
            items = missing_by_category[section]
            print(f"\n   {section}:")
            for item in items:
                print(f"      - Add {item['name']} slider")
    
    print("\n🟡 MEDIUM PRIORITY (Other missing):")
    for section, items in sorted(missing_by_category.items()):
        if section not in priority_sections:
            print(f"\n   {section}:")
            for item in items[:3]:
                print(f"      - Add {item['name']} control")
            if len(items) > 3:
                print(f"      ... and {len(items)-3} more")

if __name__ == "__main__":
    main()

