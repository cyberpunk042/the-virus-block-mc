import sys
import os
import subprocess
import re
import argparse

# ANSI Colors
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"
RESET = "\033[0m"

def preprocess_shader(file_path, included_files=None, indent_level=0):
    if included_files is None:
        included_files = set()
    
    abs_path = os.path.abspath(file_path)
    if abs_path in included_files:
        return f"// SKIPPED CIRCULAR INCLUDE: {file_path}\n"
    
    included_files.add(abs_path)
    base_dir = os.path.dirname(file_path)
    processed_lines = []
    
    indent = "  " * indent_level
    print(f"{indent}{CYAN}Processing: {os.path.basename(file_path)}{RESET}")
    
    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()
    except FileNotFoundError:
        print(f"{indent}{RED}Error: Include file not found: {file_path}{RESET}")
        return f"// ERROR: FILE NOT FOUND {file_path}\n"

    for line_idx, line in enumerate(lines):
        # Match #include "path/to/file"
        match = re.match(r'^\s*#include\s+"([^"]+)"', line)
        if match:
            include_rel_path = match.group(1)
            include_full_path = os.path.join(base_dir, include_rel_path)
            
            processed_lines.append(f"// >>> INCLUDE START: {include_rel_path}\n")
            processed_lines.append(preprocess_shader(include_full_path, included_files, indent_level + 1))
            processed_lines.append(f"// <<< INCLUDE END: {include_rel_path}\n")
        else:
            processed_lines.append(line)
            
    return "".join(processed_lines)

def extract_uniforms(source_code):
    uniforms = []
    # Regex for standard glsl uniforms (layout(...) uniform type name;)
    pattern = re.compile(r'^\s*(layout\s*\([^)]+\)\s+)?uniform\s+(\w+)\s+(\w+)\s*;', re.MULTILINE)
    
    # Also handle uniform blocks
    block_pattern = re.compile(r'^\s*(layout\s*\([^)]+\)\s+)?uniform\s+(\w+)\s*\{([^}]+)\};', re.MULTILINE)
    
    for match in pattern.finditer(source_code):
        uniforms.append({'type': match.group(2), 'name': match.group(3), 'block': False})
        
    for match in block_pattern.finditer(source_code):
        block_name = match.group(2)
        block_content = match.group(3)
        # Naive extraction of members
        for line in block_content.split(';'):
            line = line.strip()
            if not line or line.startswith('//'): continue
            parts = line.split()
            if len(parts) >= 2:
                uniforms.append({'type': parts[-2], 'name': parts[-1], 'block': True, 'container': block_name})
                
    return uniforms

def get_all_shaders():
    """Get all standalone .fsh shader files"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    shader_dir = os.path.join(project_root, "src", "main", "resources", "assets", "the-virus-block", "shaders", "post")
    
    shaders = []
    if os.path.exists(shader_dir):
        for f in sorted(os.listdir(shader_dir)):
            if f.endswith('.fsh') and f.startswith(''):
                shaders.append(os.path.join(shader_dir, f))
    return shaders

def validate_all():
    """Validate all standalone shaders"""
    shaders = get_all_shaders()
    if not shaders:
        print(f"{RED}No shaders found!{RESET}")
        return False
    
    print(f"{CYAN}════ VALIDATING {len(shaders)} SHADERS ════{RESET}\n")
    
    results = {}
    for shader in shaders:
        name = os.path.basename(shader)
        print(f"\n{CYAN}{'='*60}{RESET}")
        print(f"{CYAN}Validating: {name}{RESET}")
        print(f"{CYAN}{'='*60}{RESET}")
        
        success = validate(shader, quiet=False)
        results[name] = success
    
    # Summary
    print(f"\n{CYAN}{'='*60}{RESET}")
    print(f"{CYAN}SUMMARY{RESET}")
    print(f"{CYAN}{'='*60}{RESET}")
    
    passed = sum(1 for v in results.values() if v)
    failed = len(results) - passed
    
    for name, success in results.items():
        status = f"{GREEN}✅ PASS{RESET}" if success else f"{RED}❌ FAIL{RESET}"
        print(f"  {status}  {name}")
    
    print()
    if failed == 0:
        print(f"{GREEN}All {passed} shaders passed!{RESET}")
        return True
    else:
        print(f"{RED}{failed} shaders failed, {passed} passed{RESET}")
        return False

def validate(shader_path, quiet=False):
    """Returns True if validation succeeded, False otherwise"""
    if not os.path.exists(shader_path):
        print(f"{RED}Error: Main shader file not found: {shader_path}{RESET}")
        return False

    if not quiet:
        print(f"{CYAN}Processing: {os.path.basename(shader_path)}{RESET}")
    
    full_source = preprocess_shader(shader_path)
    
    # Save to temp file
    temp_file = "temp_validated_shader.frag"
    with open(temp_file, 'w') as f:
        f.write(full_source)
        
    # Analyze Uniforms (Static Analysis)
    if not quiet:
        print(f"\n{CYAN}--- Static Analysis ---{RESET}")
    uniforms = extract_uniforms(full_source)
    if not quiet:
        print(f"Found {len(uniforms)} active uniforms.")
    
    # Run validator
    if not quiet:
        print(f"\n{CYAN}--- GLSL Compilation Check ---{RESET}")
    
    success = False
    try:
        cmd = ['glslangValidator', '-S', 'frag', '-C', temp_file]
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        output_lines = result.stdout.splitlines()
        
        clean_output = []
        has_errors = result.returncode != 0
        
        for line in output_lines:
            if temp_file in line:
                line = line.replace(temp_file, os.path.basename(shader_path) + " (Preprocessed)")
            
            if "ERROR:" in line:
                clean_output.append(f"{RED}{line}{RESET}")
            elif "WARNING:" in line:
                clean_output.append(f"{YELLOW}{line}{RESET}")
            else:
                clean_output.append(line)

        # Print output
        for line in clean_output:
            print(line)

        if has_errors:
            print(f"\n{RED}❌ VALIDATION FAILED{RESET}")
            # Context printing
            for line in result.stdout.splitlines():
                if "ERROR:" in line:
                    parts = line.split(':')
                    if len(parts) >= 3:
                        try:
                            line_num = int(parts[2])
                            print(f"{YELLOW}   -> Context around line {line_num}:{RESET}")
                            source_lines = full_source.splitlines()
                            start = max(0, line_num - 3)
                            end = min(len(source_lines), line_num + 2)
                            for i in range(start, end):
                                marker = ">>" if i + 1 == line_num else "  "
                                color = RED if i + 1 == line_num else RESET
                                print(f"   {marker} {color}{i+1}: {source_lines[i]}{RESET}")
                        except ValueError:
                            pass
            success = False
        else:
            print(f"\n{GREEN}✅ VALIDATION SUCCEEDED{RESET}")
            success = True
            
    except FileNotFoundError:
        print(f"{RED}Error: glslangValidator not found.{RESET}")
        success = False
    finally:
        if os.path.exists(temp_file):
            os.remove(temp_file)
    
    return success

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Validate GLSL shaders with preprocessing')
    parser.add_argument('shader', nargs='?', help='Path to shader file to validate')
    parser.add_argument('--auto', action='store_true', help='Validate all standalone field_visual shaders')
    
    args = parser.parse_args()
    
    if args.auto:
        success = validate_all()
        sys.exit(0 if success else 1)
    elif args.shader:
        success = validate(args.shader)
        sys.exit(0 if success else 1)
    else:
        parser.print_help()
        sys.exit(1)
