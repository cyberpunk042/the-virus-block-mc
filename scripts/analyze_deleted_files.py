#!/usr/bin/env python3
"""
Analyze deleted files to determine which were moved vs actually lost.

For each deleted file, check:
1. If it exists elsewhere with same name
2. If it was moved to a subdirectory
3. If it's truly lost
"""

import subprocess
import sys
from pathlib import Path
from collections import defaultdict

def get_deleted_files():
    """Get list of deleted files from git status."""
    result = subprocess.run(
        ["git", "status", "--short"],
        capture_output=True,
        text=True,
        check=True
    )
    
    deleted = []
    for line in result.stdout.strip().split("\n"):
        if line.startswith(" D "):
            filepath = line[3:].strip()
            deleted.append(filepath)
    
    return deleted

def find_file_by_name(filename):
    """Find all files with given name in src/."""
    result = subprocess.run(
        ["find", "src", "-name", filename, "-type", "f"],
        capture_output=True,
        text=True
    )
    
    if result.stdout.strip():
        return [l.strip() for l in result.stdout.strip().split("\n") if l.strip()]
    return []

def check_if_file_exists(filepath):
    """Check if file exists at path."""
    return Path(filepath).exists()

def analyze_deleted_files():
    """Analyze all deleted files."""
    deleted_files = get_deleted_files()
    
    moved = []  # (old_path, new_path)
    exists_current = []  # (deleted_path, actual_path)
    truly_lost = []
    
    for filepath in deleted_files:
        filename = Path(filepath).name
        
        # Check if file exists at current path (maybe restored?)
        if check_if_file_exists(filepath):
            exists_current.append((filepath, filepath))
            continue
        
        # Check if file exists elsewhere with same name
        found = find_file_by_name(filename)
        if found:
            # Filter out the deleted path itself
            other_locations = [f for f in found if f != filepath]
            if other_locations:
                # Check if it's in a subdirectory (likely moved)
                old_dir = str(Path(filepath).parent)
                for loc in other_locations:
                    new_dir = str(Path(loc).parent)
                    # If new dir contains old dir name or is deeper, likely moved
                    if old_dir in new_dir or len(new_dir) > len(old_dir):
                        moved.append((filepath, loc))
                        break
                else:
                    # Same name but different location
                    exists_current.append((filepath, other_locations[0]))
                continue
        
        # Truly lost
        truly_lost.append(filepath)
    
    return moved, exists_current, truly_lost

def main():
    print("=" * 70)
    print("ANALYZING DELETED FILES")
    print("=" * 70)
    print()
    
    moved, exists_current, truly_lost = analyze_deleted_files()
    
    print("=" * 70)
    print(f"MOVED FILES ({len(moved)})")
    print("=" * 70)
    for old, new in moved:
        print(f"  ✅ {old}")
        print(f"     → {new}")
    if not moved:
        print("  (none)")
    print()
    
    print("=" * 70)
    print(f"EXISTS ELSEWHERE ({len(exists_current)})")
    print("=" * 70)
    for deleted, actual in exists_current:
        if deleted != actual:
            print(f"  ⚠️  {deleted}")
            print(f"     → {actual}")
        else:
            print(f"  ✓   {deleted} (exists at same path)")
    if not exists_current:
        print("  (none)")
    print()
    
    print("=" * 70)
    print(f"TRULY LOST - NEED RESTORATION ({len(truly_lost)})")
    print("=" * 70)
    if truly_lost:
        for filepath in truly_lost:
            print(f"  ❌ {filepath}")
    else:
        print("  (none - all files accounted for!)")
    print()
    
    print("=" * 70)
    print(f"Summary: {len(moved)} moved, {len(exists_current)} elsewhere, {len(truly_lost)} lost")
    print("=" * 70)
    
    if truly_lost:
        print("\n⚠️  ACTION REQUIRED: Restore truly lost files from git history")
        return 1
    else:
        print("\n✅ All deleted files are accounted for (moved or exist elsewhere)")
        return 0

if __name__ == "__main__":
    sys.exit(main())

