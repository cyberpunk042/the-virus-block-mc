#!/usr/bin/env python3
"""
Audit script: Check existing files before category system refactor.
Identifies potential breaking changes and missing dependencies.
"""

import re
from pathlib import Path

GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")

FILES_TO_CHECK = [
    # Files we're CREATING (should NOT exist)
    ("CREATE", "category/PresetCategory.java"),
    ("CREATE", "category/ProfileCategory.java"),
    ("CREATE", "category/ProfileSource.java"),
    ("CREATE", "profile/ProfileManager.java"),
    
    # Files we're REPLACING (should exist)
    ("REPLACE", "profile/Profile.java"),
    ("REPLACE", "util/PresetRegistry.java"),
    ("REPLACE", "widget/BottomActionBar.java"),
    ("REPLACE", "panel/ProfilesPanel.java"),
]

# Files that might import from the ones we're changing
DEPENDENT_FILES = [
    "screen/FieldCustomizerScreen.java",
    "panel/QuickPanel.java",
    "panel/AdvancedPanel.java",
    "panel/DebugPanel.java",
    "panel/LayerPanel.java",
    "panel/PrimitivePanel.java",
    "state/FieldEditState.java",
    "widget/PresetConfirmDialog.java",
    "widget/ConfirmDialog.java",
]


def extract_public_methods(content):
    """Extract public method signatures from Java code."""
    pattern = r'public\s+(?:static\s+)?(?:<[^>]+>\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
    matches = re.findall(pattern, content)
    return [f"{ret} {name}()" for ret, name in matches]


def extract_imports(content):
    """Extract import statements."""
    pattern = r'import\s+([\w.]+);'
    return re.findall(pattern, content)


def check_usages(filename, search_paths):
    """Check if a class is used in other files."""
    class_name = Path(filename).stem
    usages = []
    
    for dep in search_paths:
        dep_path = GUI_PACKAGE / dep
        if dep_path.exists():
            content = dep_path.read_text(encoding='utf-8')
            if class_name in content:
                usages.append(dep)
    
    return usages


def audit():
    print("=" * 70)
    print("CATEGORY SYSTEM REFACTOR - PRE-IMPLEMENTATION AUDIT")
    print("=" * 70)
    
    issues = []
    warnings = []
    
    # ========================================
    # CHECK FILES TO CREATE/REPLACE
    # ========================================
    
    print("\n## 1. File Status Check\n")
    
    for action, relative_path in FILES_TO_CHECK:
        full_path = GUI_PACKAGE / relative_path
        exists = full_path.exists()
        
        if action == "CREATE":
            if exists:
                issues.append(f"CREATE {relative_path} - FILE ALREADY EXISTS!")
                print(f"  ❌ CREATE {relative_path} - EXISTS (will overwrite!)")
            else:
                print(f"  ✅ CREATE {relative_path} - OK (doesn't exist)")
        
        elif action == "REPLACE":
            if exists:
                print(f"  ✅ REPLACE {relative_path} - EXISTS (will backup needed)")
                # Check for method signatures we need to preserve
                content = full_path.read_text(encoding='utf-8')
                methods = extract_public_methods(content)
                if methods:
                    print(f"       Current public methods: {len(methods)}")
            else:
                warnings.append(f"REPLACE {relative_path} - FILE DOESN'T EXIST")
                print(f"  ⚠️  REPLACE {relative_path} - DOESN'T EXIST (creating new)")
    
    # ========================================
    # CHECK EXISTING FILE CONTENTS
    # ========================================
    
    print("\n## 2. Existing File Analysis\n")
    
    for action, relative_path in FILES_TO_CHECK:
        if action == "REPLACE":
            full_path = GUI_PACKAGE / relative_path
            if full_path.exists():
                content = full_path.read_text(encoding='utf-8')
                methods = extract_public_methods(content)
                
                print(f"### {relative_path}")
                print(f"    Size: {len(content)} bytes, {len(content.splitlines())} lines")
                print(f"    Public methods ({len(methods)}):")
                for m in methods[:10]:  # First 10
                    print(f"      - {m}")
                if len(methods) > 10:
                    print(f"      ... and {len(methods) - 10} more")
                print()
    
    # ========================================
    # CHECK DEPENDENCIES
    # ========================================
    
    print("\n## 3. Dependency Analysis\n")
    
    for action, relative_path in FILES_TO_CHECK:
        if action == "REPLACE":
            class_name = Path(relative_path).stem
            usages = check_usages(relative_path, DEPENDENT_FILES)
            
            if usages:
                print(f"  {class_name} is used by:")
                for u in usages:
                    print(f"    - {u}")
                warnings.append(f"{class_name} is used by {len(usages)} files - check API compatibility")
            else:
                print(f"  {class_name}: No direct dependencies found in panel/screen files")
    
    # ========================================
    # CHECK STATE CLASS COMPATIBILITY
    # ========================================
    
    print("\n## 4. FieldEditState Method Check\n")
    
    state_file = GUI_PACKAGE / "state/FieldEditState.java"
    if state_file.exists():
        content = state_file.read_text(encoding='utf-8')
        
        # Methods our new code expects
        expected_methods = [
            "isDirty",
            "markDirty",
            "getCurrentProfileName",
            "isCurrentProfileServerSourced",
            "getShapeType",
            "getFillMode",
            "isSpinEnabled",
            "setFillMode",
            "setWireThickness",
            "setGlow",
            "setAlpha",
            "setSpinEnabled",
            "setSpinSpeed",
            "setPulseEnabled",
            "setPrimaryColor",
            "setEmissive",
            "setSaturation",
        ]
        
        missing = []
        found = []
        for method in expected_methods:
            if method in content:
                found.append(method)
            else:
                missing.append(method)
        
        print(f"  Expected methods found: {len(found)}/{len(expected_methods)}")
        if missing:
            print(f"  ⚠️  Missing methods in FieldEditState:")
            for m in missing:
                print(f"      - {m}()")
            warnings.append(f"FieldEditState missing {len(missing)} methods")
    else:
        issues.append("FieldEditState.java not found!")
    
    # ========================================
    # CHECK EXISTING PRESETREGISTRY
    # ========================================
    
    print("\n## 5. Existing PresetRegistry Analysis\n")
    
    preset_file = GUI_PACKAGE / "util/PresetRegistry.java"
    if preset_file.exists():
        content = preset_file.read_text(encoding='utf-8')
        methods = extract_public_methods(content)
        
        print("  Current PresetRegistry methods:")
        for m in methods:
            print(f"    - {m}")
        
        # Check what the new version needs to support
        print("\n  New version will have:")
        print("    - loadAll()")
        print("    - getCategories()")
        print("    - getPresets(PresetCategory)")
        print("    - getPreset(String)")
        print("    - applyPreset(FieldEditState, String)")
        print("    - getAffectedCategories(String)")
        print("    - reload()")
    
    # ========================================
    # SUMMARY
    # ========================================
    
    print("\n" + "=" * 70)
    print("AUDIT SUMMARY")
    print("=" * 70)
    
    if issues:
        print(f"\n❌ ISSUES ({len(issues)}):")
        for issue in issues:
            print(f"   - {issue}")
    
    if warnings:
        print(f"\n⚠️  WARNINGS ({len(warnings)}):")
        for warning in warnings:
            print(f"   - {warning}")
    
    if not issues and not warnings:
        print("\n✅ All checks passed! Safe to proceed.")
    elif not issues:
        print("\n⚠️  Warnings found but no blocking issues. Review before proceeding.")
    else:
        print("\n❌ Issues found! Fix before proceeding.")
    
    print()
    return len(issues) == 0


if __name__ == "__main__":
    success = audit()
    exit(0 if success else 1)

