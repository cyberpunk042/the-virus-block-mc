#!/usr/bin/env python3
"""
Audit the category system scripts and current codebase state.
Identifies what needs to be fixed before running the phase scripts.
"""

from pathlib import Path
import re

# =============================================================================
# PATHS
# =============================================================================

PROJECT_ROOT = Path(".")
MAIN_SRC = PROJECT_ROOT / "src/main/java/net/cyberpunk042"
CLIENT_SRC = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client"

# Where things SHOULD be (domain concepts in main)
CORRECT_CATEGORY_PKG = MAIN_SRC / "field/category"
CORRECT_PROFILE_PKG = MAIN_SRC / "field/profile"

# Where things might be WRONG (GUI packages)
WRONG_CATEGORY_PKG = CLIENT_SRC / "gui/category"
WRONG_PROFILE_PKG = CLIENT_SRC / "gui/profile"

# Scripts to check
SCRIPTS = [
    "scripts/impl_category_system_phase1.py",
    "scripts/impl_category_system_phase2.py",
    "scripts/impl_category_system_phase3.py",
]

# Files that might have been patched with wrong imports
FILES_TO_CHECK = [
    "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java",
    "src/client/java/net/cyberpunk042/client/gui/panel/ProfilesPanel.java",
    "src/client/java/net/cyberpunk042/client/gui/widget/BottomActionBar.java",
    "src/client/java/net/cyberpunk042/client/gui/util/PresetRegistry.java",
]

# =============================================================================
# AUDIT FUNCTIONS
# =============================================================================

def check_wrong_folders():
    """Check if wrong package folders exist."""
    print("\n=== WRONG FOLDER LOCATIONS ===")
    issues = []
    
    if WRONG_CATEGORY_PKG.exists():
        files = list(WRONG_CATEGORY_PKG.glob("*.java"))
        if files:
            issues.append(f"❌ Wrong location exists: {WRONG_CATEGORY_PKG}")
            for f in files:
                issues.append(f"   - {f.name}")
        else:
            issues.append(f"⚠️  Empty wrong folder: {WRONG_CATEGORY_PKG}")
    else:
        issues.append(f"✅ No wrong category folder at: {WRONG_CATEGORY_PKG}")
    
    if WRONG_PROFILE_PKG.exists():
        files = list(WRONG_PROFILE_PKG.glob("*.java"))
        if files:
            issues.append(f"❌ Wrong location exists: {WRONG_PROFILE_PKG}")
            for f in files:
                issues.append(f"   - {f.name}")
        else:
            issues.append(f"⚠️  Empty wrong folder: {WRONG_PROFILE_PKG}")
    else:
        issues.append(f"✅ No wrong profile folder at: {WRONG_PROFILE_PKG}")
    
    for issue in issues:
        print(issue)
    return issues


def check_correct_folders():
    """Check if correct package folders exist."""
    print("\n=== CORRECT FOLDER LOCATIONS ===")
    status = []
    
    if CORRECT_CATEGORY_PKG.exists():
        files = list(CORRECT_CATEGORY_PKG.glob("*.java"))
        if files:
            status.append(f"✅ Category package exists: {CORRECT_CATEGORY_PKG}")
            for f in files:
                status.append(f"   - {f.name}")
        else:
            status.append(f"⚠️  Empty category folder: {CORRECT_CATEGORY_PKG}")
    else:
        status.append(f"⏳ Category package missing: {CORRECT_CATEGORY_PKG} (will be created)")
    
    if CORRECT_PROFILE_PKG.exists():
        files = list(CORRECT_PROFILE_PKG.glob("*.java"))
        if files:
            status.append(f"✅ Profile package exists: {CORRECT_PROFILE_PKG}")
            for f in files:
                status.append(f"   - {f.name}")
        else:
            status.append(f"⚠️  Empty profile folder: {CORRECT_PROFILE_PKG}")
    else:
        status.append(f"⏳ Profile package missing: {CORRECT_PROFILE_PKG} (will be created)")
    
    for s in status:
        print(s)
    return status


def check_wrong_imports_in_files():
    """Check if any files have wrong imports."""
    print("\n=== WRONG IMPORTS IN FILES ===")
    issues = []
    
    wrong_patterns = [
        r"import net\.cyberpunk042\.client\.gui\.category\.",
        r"import net\.cyberpunk042\.client\.gui\.profile\.",
    ]
    
    for filepath in FILES_TO_CHECK:
        path = Path(filepath)
        if not path.exists():
            continue
        
        content = path.read_text(encoding='utf-8')
        file_issues = []
        
        for pattern in wrong_patterns:
            matches = re.findall(pattern + r"[A-Za-z]+;", content)
            for match in matches:
                file_issues.append(match)
        
        if file_issues:
            issues.append(f"❌ {filepath}")
            for imp in file_issues:
                issues.append(f"   - {imp}")
        else:
            issues.append(f"✅ {filepath} - no wrong imports")
    
    for issue in issues:
        print(issue)
    return issues


def check_script_packages():
    """Check package declarations in the phase scripts."""
    print("\n=== SCRIPT PACKAGE DECLARATIONS ===")
    issues = []
    
    wrong_pkg = "net.cyberpunk042.client.gui"
    correct_pkg = "net.cyberpunk042.field"
    
    for script_path in SCRIPTS:
        path = Path(script_path)
        if not path.exists():
            issues.append(f"⚠️  Script missing: {script_path}")
            continue
        
        content = path.read_text(encoding='utf-8')
        
        # Check for wrong package in Java code strings
        wrong_count = content.count(f"package {wrong_pkg}.category")
        wrong_count += content.count(f"package {wrong_pkg}.profile")
        wrong_count += content.count(f"import {wrong_pkg}.category")
        wrong_count += content.count(f"import {wrong_pkg}.profile")
        
        correct_count = content.count(f"package {correct_pkg}.category")
        correct_count += content.count(f"package {correct_pkg}.profile")
        correct_count += content.count(f"import {correct_pkg}.category")
        correct_count += content.count(f"import {correct_pkg}.profile")
        
        if wrong_count > 0:
            issues.append(f"❌ {script_path}: {wrong_count} wrong package refs, {correct_count} correct")
        elif correct_count > 0:
            issues.append(f"✅ {script_path}: {correct_count} correct package refs")
        else:
            issues.append(f"⚠️  {script_path}: no category/profile package refs found")
    
    for issue in issues:
        print(issue)
    return issues


def check_script_paths():
    """Check Path definitions in scripts."""
    print("\n=== SCRIPT PATH DEFINITIONS ===")
    issues = []
    
    for script_path in SCRIPTS:
        path = Path(script_path)
        if not path.exists():
            continue
        
        content = path.read_text(encoding='utf-8')
        
        # Look for path definitions
        if 'GUI_PACKAGE = Path("src/client' in content:
            issues.append(f"⚠️  {script_path}: Uses GUI_PACKAGE (may be wrong)")
        
        if 'CATEGORY_PACKAGE = GUI_PACKAGE' in content:
            issues.append(f"❌ {script_path}: CATEGORY_PACKAGE under GUI (wrong!)")
        
        if 'PROFILE_PACKAGE = GUI_PACKAGE' in content:
            issues.append(f"❌ {script_path}: PROFILE_PACKAGE under GUI (wrong!)")
        
        if 'MAIN_PACKAGE = Path("src/main' in content:
            issues.append(f"✅ {script_path}: Uses MAIN_PACKAGE (correct)")
        
        if 'FIELD_PACKAGE = MAIN_PACKAGE / "field"' in content:
            issues.append(f"✅ {script_path}: Uses FIELD_PACKAGE (correct)")
        
        if 'CATEGORY_PACKAGE = FIELD_PACKAGE' in content or 'CATEGORY_PACKAGE = MAIN_PACKAGE / "field" / "category"' in content:
            issues.append(f"✅ {script_path}: CATEGORY_PACKAGE under field (correct)")
        
        if 'PROFILE_PACKAGE = FIELD_PACKAGE' in content:
            issues.append(f"✅ {script_path}: PROFILE_PACKAGE under field (correct)")
    
    for issue in issues:
        print(issue)
    return issues


# =============================================================================
# MAIN
# =============================================================================

def main():
    print("=" * 70)
    print("CATEGORY SYSTEM AUDIT")
    print("=" * 70)
    
    all_issues = []
    
    all_issues.extend(check_wrong_folders())
    all_issues.extend(check_correct_folders())
    all_issues.extend(check_wrong_imports_in_files())
    all_issues.extend(check_script_packages())
    all_issues.extend(check_script_paths())
    
    # Summary
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)
    
    errors = [i for i in all_issues if i.startswith("❌")]
    warnings = [i for i in all_issues if i.startswith("⚠️")]
    ok = [i for i in all_issues if i.startswith("✅")]
    
    print(f"\n❌ Errors: {len(errors)}")
    print(f"⚠️  Warnings: {len(warnings)}")
    print(f"✅ OK: {len(ok)}")
    
    if errors:
        print("\n--- ERRORS TO FIX ---")
        for e in errors:
            print(e)
    
    if warnings:
        print("\n--- WARNINGS ---")
        for w in warnings:
            print(w)
    
    print("\n" + "=" * 70)
    if errors:
        print("ACTION NEEDED: Run fix script to correct issues")
    else:
        print("ALL CLEAR: Scripts are ready to run")
    print("=" * 70)


if __name__ == "__main__":
    main()

