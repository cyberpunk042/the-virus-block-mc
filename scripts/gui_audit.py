#!/usr/bin/env python3
"""
GUI audit helper for the Field Customizer.

What it does:
- Inventories config/the-virus-block/field_* folders (fragments) and reports counts + sample names.
- Inventories config/the-virus-block/field_presets (category folders + loose files).
- Flags missing/empty categories and presence of only Default/Custom.

Run:
  python scripts/gui_audit.py
"""
from __future__ import annotations

import json
import pathlib
from collections import defaultdict

ROOT = pathlib.Path("config/the-virus-block")

# Fragment categories we expect
FRAG_CATEGORIES = [
    "field_shapes",
    "field_fills",
    "field_masks",
    "field_arrangements",
    "field_animations",
    "field_beams",
    "field_follows",
    "field_appearances",
    "field_layers",
    "field_links",
    "field_orbits",
    "field_predictions",
    "field_primitives",
    "field_transforms",
]


def list_json(path: pathlib.Path) -> list[pathlib.Path]:
    return sorted(path.glob("*.json"))


def summarize_fragments():
    print("=== Fragment categories (config/the-virus-block) ===")
    missing = []
    only_default = []
    for cat in FRAG_CATEGORIES:
        p = ROOT / cat
        if not p.exists():
            missing.append(cat)
            print(f"[MISSING] {cat}")
            continue
        files = list_json(p)
        names = [f.stem for f in files]
        print(f"{cat:20s} count={len(names)} samples={names[:5]}")
        if set(names).issubset({"default", "custom", "Default", "Custom"}):
            only_default.append(cat)
    if missing:
        print("\nMissing categories:", ", ".join(missing))
    if only_default:
        print("\nOnly Default/Custom found in:", ", ".join(only_default))
    print()


def summarize_presets():
    presets_root = ROOT / "field_presets"
    print("=== Presets (config/the-virus-block/field_presets) ===")
    if not presets_root.exists():
        print("Presets folder is missing.")
        return
    categories = defaultdict(list)
    for sub in presets_root.iterdir():
        if sub.is_dir():
            for f in list_json(sub):
                categories[sub.name].append(f.name)
    loose = [f.name for f in list_json(presets_root)]
    for cat, files in sorted(categories.items()):
        print(f"preset/{cat:12s} count={len(files)} samples={files[:5]}")
    if loose:
        print(f"preset/ (loose)  count={len(loose)} samples={loose[:5]}")
    if not categories and not loose:
        print("No presets found.")
    print()


def main():
    summarize_fragments()
    summarize_presets()


if __name__ == "__main__":
    main()




