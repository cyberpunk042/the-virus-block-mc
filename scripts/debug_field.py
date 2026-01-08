#!/usr/bin/env python3
"""Detailed debug of field package classes."""
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).parent))

from lib.java_parser import scan_project

project = Path(__file__).parent.parent
classes = scan_project(project)

print("\n=== FIELD PACKAGE CLASSES ===\n")

# Filter to field package only
field_classes = [c for c in classes if 'field' in c.relative_package and 'client' not in c.relative_package]

for c in sorted(field_classes, key=lambda x: x.name):
    print(f"📦 {c.name} ({c.class_type})")
    print(f"   Package: {c.relative_package}")
    if c.extends:
        print(f"   ⬆️ Extends: {c.extends}")
    if c.implements:
        print(f"   🔌 Implements: {c.implements}")
    if c.fields:
        print(f"   📋 Fields ({len(c.fields)}):")
        for f in c.fields[:8]:
            print(f"      - {f.name}: {f.type}")
    else:
        print(f"   📋 Fields: NONE")
    print()
