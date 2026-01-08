#!/usr/bin/env python3
"""
Audit field fragments and identify missing base definitions.

This script:
1. Scans existing fragment folders
2. Identifies what pattern types exist (quad, segment, sector, edge)
3. Shows gaps in coverage
4. Proposes missing base fragments
"""

import json
import os
from pathlib import Path
from collections import defaultdict

# Paths
CONFIG_ROOT = Path("config/the-virus-block")
DATA_ROOT = Path("src/main/resources/data/the-virus-block")

# Fragment categories and expected bases
FRAGMENT_CATEGORIES = {
    "field_shapes": {
        "description": "Shape geometry definitions",
        "pattern_types": ["sphere", "ring", "disc", "prism", "cylinder", "polyhedron", "cage"],
        "expected_variants": ["default", "lowpoly", "highpoly", "thin", "thick", "dense", "sparse"]
    },
    "field_fills": {
        "description": "Fill mode configurations",
        "expected": ["default", "solid", "wireframe", "thin_wire", "thick_wire", "cage_sparse", "cage_dense", "points"]
    },
    "field_masks": {
        "description": "Visibility mask patterns",
        "expected": ["full", "bands", "stripes", "checker", "radial", "gradient", "half", "quarter"]
    },
    "field_arrangements": {
        "description": "Pattern arrangements (quad, segment, sector, edge)",
        "pattern_types": ["quad", "segment", "sector", "edge"],
        "expected_per_type": ["default", "alternating", "sparse", "dense"]
    },
    "field_animations": {
        "description": "Animation presets",
        "expected": ["static", "spin_slow", "spin_medium", "spin_fast", "pulse_soft", "pulse_strong", "breathe", "wave"]
    },
    "field_beams": {
        "description": "Debug beam configurations",
        "expected": ["default", "thin", "thick", "tall", "short"]
    },
    "field_follow": {
        "description": "Follow mode presets",
        "expected": ["snap", "smooth", "glide", "static"]
    }
}

# Pattern type to shape type mapping
PATTERN_SHAPE_MAP = {
    "quad": ["sphere", "prism", "polyhedron", "cube"],
    "segment": ["ring"],
    "sector": ["disc"],
    "edge": ["cage", "wireframe"]
}

def scan_folder(folder: Path) -> dict:
    """Scan a fragment folder and return file info."""
    result = {
        "exists": folder.exists(),
        "files": [],
        "names": []
    }
    
    if folder.exists():
        for f in folder.glob("*.json"):
            try:
                with open(f, 'r') as fp:
                    data = json.load(fp)
                    name = data.get("name", f.stem)
                    result["files"].append(f.name)
                    result["names"].append(name)
            except:
                result["files"].append(f.name)
                result["names"].append(f.stem)
    
    return result

def scan_field_definitions() -> dict:
    """Scan field_definitions to identify pattern usage."""
    defs_path = DATA_ROOT / "field_definitions"
    pattern_usage = defaultdict(list)
    
    if defs_path.exists():
        for f in defs_path.glob("*.json"):
            name = f.stem
            # Classify by prefix
            if name.startswith("quad_"):
                pattern_usage["quad"].append(name)
            elif name.startswith("segment_"):
                pattern_usage["segment"].append(name)
            elif name.startswith("sector_"):
                pattern_usage["sector"].append(name)
            elif name.startswith("edge_"):
                pattern_usage["edge"].append(name)
            else:
                pattern_usage["other"].append(name)
    
    return dict(pattern_usage)

def audit():
    """Run the full audit."""
    print("=" * 70)
    print("FIELD FRAGMENT AUDIT")
    print("=" * 70)
    
    # 1. Scan existing fragments
    print("\n1. EXISTING FRAGMENTS")
    print("-" * 50)
    
    for category, info in FRAGMENT_CATEGORIES.items():
        folder = CONFIG_ROOT / category
        result = scan_folder(folder)
        
        status = "✅" if result["exists"] else "❌"
        count = len(result["files"])
        print(f"\n{status} {category}/ ({count} files)")
        print(f"   {info['description']}")
        
        if result["files"]:
            for f in sorted(result["files"])[:10]:  # Show first 10
                print(f"      - {f}")
            if len(result["files"]) > 10:
                print(f"      ... and {len(result['files']) - 10} more")
    
    # 2. Field definitions by pattern type
    print("\n\n2. FIELD DEFINITIONS BY PATTERN TYPE")
    print("-" * 50)
    
    pattern_usage = scan_field_definitions()
    for pattern, defs in sorted(pattern_usage.items()):
        print(f"\n{pattern.upper()} ({len(defs)} definitions)")
        for d in sorted(defs)[:5]:
            print(f"   - {d}")
        if len(defs) > 5:
            print(f"   ... and {len(defs) - 5} more")
    
    # 3. Gap analysis
    print("\n\n3. GAP ANALYSIS")
    print("-" * 50)
    
    # Check shapes per type
    shapes_folder = CONFIG_ROOT / "field_shapes"
    existing_shapes = scan_folder(shapes_folder)["files"]
    existing_shape_types = set()
    for f in existing_shapes:
        # Extract shape type from filename
        for st in ["sphere", "ring", "disc", "prism", "cylinder"]:
            if st in f.lower():
                existing_shape_types.add(st)
    
    print("\n3a. Shape types coverage:")
    for shape_type in ["sphere", "ring", "disc", "prism", "cylinder", "polyhedron"]:
        has_default = any(f"default_{shape_type}" in f or f"{shape_type}" in f 
                         for f in existing_shapes)
        status = "✅" if has_default else "❌ MISSING"
        print(f"   {status} {shape_type}")
    
    # Check arrangements per pattern type
    print("\n3b. Arrangement patterns:")
    arrangements_folder = CONFIG_ROOT / "field_arrangements"
    existing_arr = scan_folder(arrangements_folder)["files"]
    for pattern_type in ["quad", "segment", "sector", "edge"]:
        has_pattern = any(pattern_type in f.lower() for f in existing_arr)
        # Also check the JSON content for pattern references
        status = "⚠️ CHECK" if not has_pattern else "✅"
        print(f"   {status} {pattern_type} pattern arrangements")
    
    # 4. Recommendations
    print("\n\n4. RECOMMENDATIONS")
    print("-" * 50)
    
    print("""
MISSING BASE FRAGMENTS TO CREATE:

A. Shape Fragments (per shape type):
   - Each shape type needs: default, lowpoly (non-complex), highpoly
   
B. Arrangement Fragments (per pattern type):
   - quad_default.json, quad_alternating.json, quad_sparse.json
   - segment_default.json, segment_alternating.json, segment_sparse.json
   - sector_default.json, sector_alternating.json, sector_sparse.json
   - edge_default.json, edge_alternating.json, edge_sparse.json

C. Fill Mode Fragments (ensure coverage):
   - Per-shape-type fill configs (sphere_solid, ring_wireframe, etc.)

D. Performance Variants:
   - _lowpoly / _simple variants with low complexity for each shape
""")
    
    return {
        "existing_fragments": {cat: scan_folder(CONFIG_ROOT / cat) 
                               for cat in FRAGMENT_CATEGORIES},
        "pattern_usage": pattern_usage,
    }

if __name__ == "__main__":
    audit()

