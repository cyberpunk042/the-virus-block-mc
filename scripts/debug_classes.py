#!/usr/bin/env python3
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).parent))

from lib.java_parser import scan_project

project = Path(__file__).parent.parent
classes = scan_project(project)

print("\n=== Checking key classes for relationships ===\n")
for name in ['EffectProcessor', 'TriggerProcessor', 'BindingResolver', 'FieldInstance', 'TriggerConfig', 'BindingConfig']:
    for c in classes:
        if c.name == name:
            print(f'{c.name}:')
            print(f'  type: {c.class_type}')
            print(f'  extends: {c.extends}')
            print(f'  implements: {c.implements}')
            print(f'  fields ({len(c.fields)}): {[f.type for f in c.fields[:5]]}')
            print()
            break
