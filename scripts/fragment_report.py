#!/usr/bin/env python3
"""
Fragment report: map config fragments to the folders used by FragmentRegistry.

Outputs:
- For each fragment category (folder): count and sample names.
- Shows the exact folder names FragmentRegistry uses.

Run:
  python scripts/fragment_report.py
"""

from __future__ import annotations
import pathlib

# Mirror FragmentRegistry folder constants
FOLDERS = {
    "shape": "field_shapes",
    "fill": "field_fills",
    "visibility": "field_masks",
    "arrangement": "field_arrangements",
    "animation": "field_animations",
    "beam": "field_beams",
    "follow": "field_follows",
    "appearance": "field_appearances",
    "layer": "field_layers",
    "link": "field_links",
    "orbit": "field_orbits",
    "prediction": "field_predictions",
    "primitive": "field_primitives",
    "transform": "field_transforms",
}

def list_json_names(folder: pathlib.Path):
    return sorted([p.stem for p in folder.glob("*.json")])

def main():
    root = pathlib.Path("config/the-virus-block")
    for label, folder in FOLDERS.items():
        path = root / folder
        if not path.exists():
            print(f"{label:12s}: MISSING ({folder})")
            continue
        names = list_json_names(path)
        print(f"{label:12s}: {len(names):3d} ({folder}) samples={names[:6]}")

if __name__ == "__main__":
    main()




