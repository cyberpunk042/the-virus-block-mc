#!/usr/bin/env python3
"""
UBO Audit Script - Phase 0 Classification Helper

Parses GLSL uniform blocks and extracts parameters for domain classification.
Outputs a markdown table ready for human classification.

Reuses preprocessing logic from 10_validate_shader.py for #include handling.

Usage:
    python3 ubo_audit.py <glsl_file> [-o output.md]
"""

from __future__ import annotations
import re
import sys
import os
import argparse
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional, List

# ANSI Colors
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"
RESET = "\033[0m"

@dataclass
class Parameter:
    """Represents a single UBO parameter"""
    slot: int
    name: str
    glsl_type: str
    section: str
    comment: str = ""
    
    def to_row(self) -> str:
        """Convert to markdown table row with classification columns"""
        domain = self._guess_domain()
        update_freq = self._guess_update_freq()
        
        return f"| {self.slot} | `{self.name}` | {self.section} | {domain} | {update_freq} | {self.comment} |"
    
    def _guess_domain(self) -> str:
        """Heuristic guess at target domain"""
        name_lower = self.name.lower()
        
        # Camera-related
        if any(x in name_lower for x in ['camerax', 'cameray', 'cameraz', 'forwardx', 'forwardy', 'forwardz', 
                                           'upx', 'upy', 'upz', 'fov', 'aspectratio', 'nearplane', 'farplane', 
                                           'viewproj', 'invviewproj']):
            return "**Camera**"
        
        # Frame-related
        if name_lower == 'time':
            return "**Frame**"
        
        # Position/Instance
        if any(x in name_lower for x in ['centerx', 'centery', 'centerz', 'radius']):
            return "EffectRuntime?"
        
        # Debug
        if 'debug' in name_lower or 'reserved' in name_lower:
            return "Debug/Reserved"
        
        # Flying flag could be either
        if 'flying' in name_lower:
            return "**Camera**"
        
        return "EffectConfig"
    
    def _guess_update_freq(self) -> str:
        """Heuristic guess at update frequency"""
        name_lower = self.name.lower()
        
        if name_lower == 'time':
            return "per-frame"
        if any(x in name_lower for x in ['camera', 'forward', 'up', 'viewproj', 'invviewproj']):
            return "per-frame"
        if any(x in name_lower for x in ['centerx', 'centery', 'centerz', 'radius']):
            return "per-frame?"
        if 'reserved' in name_lower:
            return "never"
        return "on-change"


def preprocess_shader(file_path: str, included_files: Optional[set] = None, quiet: bool = True) -> str:
    """
    Recursively preprocess a shader file, expanding #include directives.
    Adapted from 10_validate_shader.py.
    """
    if included_files is None:
        included_files = set()
    
    abs_path = os.path.abspath(file_path)
    if abs_path in included_files:
        return f"// SKIPPED CIRCULAR INCLUDE: {file_path}\n"
    
    included_files.add(abs_path)
    base_dir = os.path.dirname(file_path)
    processed_lines = []
    
    if not quiet:
        print(f"{CYAN}Processing: {os.path.basename(file_path)}{RESET}")
    
    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()
    except FileNotFoundError:
        print(f"{RED}Error: Include file not found: {file_path}{RESET}")
        return f"// ERROR: FILE NOT FOUND {file_path}\n"

    for line in lines:
        match = re.match(r'^\s*#include\s+"([^"]+)"', line)
        if match:
            include_rel_path = match.group(1)
            include_full_path = os.path.join(base_dir, include_rel_path)
            processed_lines.append(f"// >>> INCLUDE START: {include_rel_path}\n")
            processed_lines.append(preprocess_shader(include_full_path, included_files, quiet))
            processed_lines.append(f"// <<< INCLUDE END: {include_rel_path}\n")
        else:
            processed_lines.append(line)
            
    return "".join(processed_lines)


def parse_glsl_uniform_block(content: str, block_name: str = None) -> List[Parameter]:
    """
    Parse GLSL content and extract parameters from uniform block.
    
    Args:
        content: Full GLSL source (after preprocessing)
        block_name: Optional specific block name to parse (default: first found)
    """
    parameters = []
    current_section = "Unknown"
    current_slot = 0
    in_uniform_block = False
    target_block = block_name
    
    lines = content.split('\n')
    
    for i, line in enumerate(lines):
        stripped = line.strip()
        
        # Check for uniform block start
        if not in_uniform_block:
            match = re.match(r'layout\s*\(\s*std140\s*\)\s*uniform\s+(\w+)\s*\{', stripped)
            if match:
                found_block = match.group(1)
                if target_block is None or found_block == target_block:
                    in_uniform_block = True
                    target_block = found_block
                    print(f"{GREEN}Found uniform block: {found_block}{RESET}")
                continue
        
        if in_uniform_block:
            # Check for block end
            if stripped.startswith('}'):
                break
            
            # Check for section header (═══ pattern)
            if '═══' in line:
                # Look at next line for section name
                if i + 1 < len(lines):
                    next_line = lines[i + 1].strip()
                    if next_line.startswith('//'):
                        section_text = next_line.lstrip('/').strip()
                        if section_text and '═' not in section_text:
                            current_section = section_text
                            print(f"  Section: {section_text}")
            
            # Check for slot comment (// vec4 N: ...)
            slot_match = re.match(r'//\s*vec4\s+(\d+):', stripped)
            if slot_match:
                current_slot = int(slot_match.group(1))
            
            # Check for mat4 slot comment (// mat4 (vec4 N-M): ...)
            mat_slot_match = re.match(r'//\s*mat4\s*\(vec4\s*(\d+)-(\d+)\):', stripped)
            if mat_slot_match:
                current_slot = int(mat_slot_match.group(1))
            
            # Check for float declaration
            float_match = re.match(r'float\s+(\w+)\s*;\s*(?://\s*(.*))?', stripped)
            if float_match:
                name = float_match.group(1)
                comment = (float_match.group(2) or "").strip()
                parameters.append(Parameter(
                    slot=current_slot,
                    name=name,
                    glsl_type="float",
                    section=current_section,
                    comment=comment
                ))
            
            # Check for mat4 declaration
            mat_match = re.match(r'mat4\s+(\w+)\s*;\s*(?://\s*(.*))?', stripped)
            if mat_match:
                name = mat_match.group(1)
                comment = (mat_match.group(2) or "").strip()
                parameters.append(Parameter(
                    slot=current_slot,
                    name=name,
                    glsl_type="mat4",
                    section=current_section,
                    comment=f"(4 slots) {comment}"
                ))
                current_slot += 4
            
            # Check for vec2/vec3/vec4 declarations (if any)
            vec_match = re.match(r'(vec[234])\s+(\w+)\s*;\s*(?://\s*(.*))?', stripped)
            if vec_match:
                vec_type = vec_match.group(1)
                name = vec_match.group(2)
                comment = (vec_match.group(3) or "").strip()
                parameters.append(Parameter(
                    slot=current_slot,
                    name=name,
                    glsl_type=vec_type,
                    section=current_section,
                    comment=comment
                ))
    
    return parameters


def generate_markdown_report(parameters: List[Parameter], source_file: str, block_name: str) -> str:
    """Generate markdown classification table"""
    
    lines = [
        "# UBO Parameter Classification - Phase 0 Audit",
        "",
        f"> **Source:** `{source_file}`",
        f"> **Uniform Block:** `{block_name}`",
        f"> **Total Parameters:** {len(parameters)}",
        "> ",
        "> **Instructions:** Review and confirm the `Domain Target` and `Update Freq` columns.",
        "> Values marked with `**bold**` or `?` are suggestions that need verification.",
        "",
        "---",
        "",
        "## Domain Legend",
        "",
        "| Domain | Description | Update Policy |",
        "|--------|-------------|---------------|",
        "| **Frame** | Global per-frame drivers | Every frame |",
        "| **Camera** | View definition and matrices | Every frame |",
        "| **Object** | Per-instance identity/transform | Per draw (future) |",
        "| **EffectConfig** | Preset/style parameters | On preset change |",
        "| **EffectRuntime** | Per-frame instance state | Per frame (if CPU-driven) |",
        "| **Debug** | Debug flags and values | Dev only |",
        "| **REMOVE** | Candidate for removal | N/A |",
        "",
        "---",
        "",
        "## Parameter Classification",
        "",
        "| Slot | Name | Section | Domain Target | Update Freq | Notes |",
        "|------|------|---------|---------------|-------------|-------|",
    ]
    
    # Group by slot
    current_slot = -1
    for param in parameters:
        if param.slot != current_slot:
            if current_slot != -1:
                lines.append("|---|---|---|---|---|---|")
            current_slot = param.slot
        lines.append(param.to_row())
    
    # Summary by domain
    lines.extend([
        "",
        "---",
        "",
        "## Summary by Suggested Domain",
        "",
    ])
    
    domain_counts = {}
    for p in parameters:
        domain = p._guess_domain()
        domain_counts[domain] = domain_counts.get(domain, 0) + 1
    
    for domain, count in sorted(domain_counts.items(), key=lambda x: -x[1]):
        lines.append(f"- {domain}: {count} parameters")
    
    # Next steps
    lines.extend([
        "",
        "---",
        "",
        "## Next Steps",
        "",
        "1. ✅ Review each parameter classification",
        "2. ✅ Confirm update frequencies",
        "3. ✅ Identify consolidation opportunities (e.g., X/Y/Z/W → vec4)",
        "4. ⬜ Group by target UBO",
        "5. ⬜ Create new UBO record definitions",
        "6. ⬜ Update shader uniform blocks",
    ])
    
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(
        description="Audit GLSL uniform blocks for UBO refactor classification"
    )
    parser.add_argument(
        "glsl_file",
        help="Path to GLSL file containing uniform block"
    )
    parser.add_argument(
        "-b", "--block",
        help="Specific uniform block name to parse (default: first found)"
    )
    parser.add_argument(
        "-o", "--output",
        help="Output markdown file (default: stdout)"
    )
    parser.add_argument(
        "--no-preprocess",
        action="store_true",
        help="Don't expand #include directives"
    )
    
    args = parser.parse_args()
    
    # Read input file
    glsl_path = Path(args.glsl_file)
    if not glsl_path.exists():
        print(f"{RED}Error: File not found: {glsl_path}{RESET}", file=sys.stderr)
        sys.exit(1)
    
    # Get content (with or without preprocessing)
    if args.no_preprocess:
        content = glsl_path.read_text()
    else:
        content = preprocess_shader(str(glsl_path), quiet=True)
    
    print(f"{CYAN}Parsing uniform blocks...{RESET}")
    
    # Parse parameters
    parameters = parse_glsl_uniform_block(content, args.block)
    
    if not parameters:
        print(f"{YELLOW}Warning: No parameters found in uniform block{RESET}", file=sys.stderr)
        print(f"{YELLOW}Try specifying --block <name> or check if file has std140 uniform blocks{RESET}")
    else:
        print(f"{GREEN}Found {len(parameters)} parameters{RESET}")
    
    # Determine block name
    block_name = args.block or "FieldVisualConfig"
    
    # Generate report
    report = generate_markdown_report(parameters, str(glsl_path), block_name)
    
    # Output
    if args.output:
        Path(args.output).write_text(report)
        print(f"{GREEN}Report written to: {args.output}{RESET}")
    else:
        print(report)


if __name__ == "__main__":
    main()
