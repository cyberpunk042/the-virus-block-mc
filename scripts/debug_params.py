#!/usr/bin/env python3
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).parent))
from lib.java_parser import scan_project

classes = scan_project(Path(__file__).parent.parent)
for c in classes:
    if c.name in ['ValidationHelper', 'JsonParseUtils', 'ReferenceResolver']:
        print(f'{c.name}:')
        for m in c.methods[:4]:
            print(f'  {m.name}({m.parameters}) -> {m.return_type}')
