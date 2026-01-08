#!/usr/bin/env python3
"""
AUDIT: Find all (ServerWorld) player.getWorld() casts and verify they're safe to remove.

Safe to remove IF:
- player is declared as ServerPlayerEntity (getWorld returns ServerWorld)

NOT safe to remove IF:
- player is declared as PlayerEntity (getWorld returns World, needs cast)
- player could be either client or server side
"""

import os
import re

BASE_PATH = "/mnt/c/Users/Jean/the-virus-block-template-1.21.6/src/main/java"

def find_java_files(root):
    for dirpath, _, filenames in os.walk(root):
        for f in filenames:
            if f.endswith('.java'):
                yield os.path.join(dirpath, f)

def analyze_file(filepath):
    """Analyze a file for ServerWorld casts."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        lines = content.split('\n')
    
    results = []
    
    # Find all (ServerWorld) *.getWorld() patterns
    pattern = r'\(ServerWorld\)\s*(\w+)\.getWorld\(\)'
    
    for i, line in enumerate(lines, 1):
        for match in re.finditer(pattern, line):
            var_name = match.group(1)
            full_match = match.group(0)
            
            # Search backwards for variable declaration
            var_type = find_variable_type(lines, i-1, var_name)
            
            # Determine if safe
            is_safe = var_type in ['ServerPlayerEntity', 'var'] if var_type else False
            
            # Check if it's in a context.player() situation (networking handler)
            is_networking = 'context.player()' in content or 'ServerPlayNetworking' in content
            
            results.append({
                'line': i,
                'code': line.strip(),
                'variable': var_name,
                'var_type': var_type or 'UNKNOWN',
                'safe_to_remove': is_safe,
                'is_networking': is_networking
            })
    
    return results

def find_variable_type(lines, current_line_idx, var_name):
    """Search backwards for variable declaration to find its type."""
    # Common patterns for variable declarations
    patterns = [
        rf'(\w+)\s+{var_name}\s*=',  # Type varName =
        rf'var\s+{var_name}\s*=',     # var varName =
        rf'\((\w+)\s+{var_name}\)',   # (Type varName) in lambda/method param
        rf'(\w+)\s+{var_name}\)',     # method param: Type varName)
    ]
    
    # Search up to 50 lines back
    start = max(0, current_line_idx - 50)
    for i in range(current_line_idx, start, -1):
        line = lines[i]
        
        # Check for ServerPlayerEntity specifically
        if f'ServerPlayerEntity {var_name}' in line:
            return 'ServerPlayerEntity'
        if f'ServerPlayerEntity) {var_name}' in line:
            return 'ServerPlayerEntity'
        
        # Check for PlayerEntity (needs cast)
        if f'PlayerEntity {var_name}' in line:
            return 'PlayerEntity'
        
        # Check for var (inferred - usually safe in networking context)
        if f'var {var_name}' in line:
            return 'var'
        
        # Check context.player() assignment
        if f'{var_name} = context.player()' in line:
            return 'ServerPlayerEntity'  # context.player() returns ServerPlayerEntity
    
    return None

def main():
    print("=" * 70)
    print("AUDIT: (ServerWorld) casts in codebase")
    print("=" * 70)
    
    safe_count = 0
    unsafe_count = 0
    unknown_count = 0
    
    all_results = []
    
    for filepath in find_java_files(BASE_PATH):
        results = analyze_file(filepath)
        if results:
            rel_path = os.path.relpath(filepath, BASE_PATH)
            for r in results:
                r['file'] = rel_path
                all_results.append(r)
    
    # Group by safety
    safe = [r for r in all_results if r['safe_to_remove']]
    unsafe = [r for r in all_results if not r['safe_to_remove'] and r['var_type'] != 'UNKNOWN']
    unknown = [r for r in all_results if r['var_type'] == 'UNKNOWN']
    
    print(f"\n{'='*70}")
    print(f"SAFE TO REMOVE ({len(safe)} instances)")
    print(f"{'='*70}")
    for r in safe:
        print(f"  {r['file']}:{r['line']}")
        print(f"    Variable: {r['variable']} (type: {r['var_type']})")
        print(f"    Code: {r['code'][:80]}")
        print()
    
    print(f"\n{'='*70}")
    print(f"NOT SAFE - PlayerEntity needs cast ({len(unsafe)} instances)")
    print(f"{'='*70}")
    for r in unsafe:
        print(f"  {r['file']}:{r['line']}")
        print(f"    Variable: {r['variable']} (type: {r['var_type']})")
        print(f"    Code: {r['code'][:80]}")
        print()
    
    print(f"\n{'='*70}")
    print(f"UNKNOWN - Manual review needed ({len(unknown)} instances)")
    print(f"{'='*70}")
    for r in unknown:
        print(f"  {r['file']}:{r['line']}")
        print(f"    Variable: {r['variable']}")
        print(f"    Code: {r['code'][:80]}")
        print()
    
    print(f"\n{'='*70}")
    print("SUMMARY")
    print(f"{'='*70}")
    print(f"  Safe to remove:    {len(safe)}")
    print(f"  NOT safe (keep):   {len(unsafe)}")
    print(f"  Unknown (review):  {len(unknown)}")
    print(f"  Total:             {len(all_results)}")
    
    if len(unsafe) > 0 or len(unknown) > 0:
        print(f"\n⚠️  Some casts should NOT be removed or need manual review!")
        print(f"   The fix script should only remove casts where player is ServerPlayerEntity.")

if __name__ == "__main__":
    main()














