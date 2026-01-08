"""Phase 3: Display transformations with clear diffs."""

import os
from .models import LoopWithLogs, Transformation


# ANSI colors
class C:
    BOLD = '\033[1m'
    DIM = '\033[2m'
    END = '\033[0m'
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    MAGENTA = '\033[95m'


def clr(text, *colors):
    return ''.join(colors) + str(text) + C.END


def show_summary(loops: list, base_path: str):
    """Show scan summary."""
    print(clr("═" * 74, C.MAGENTA))
    print(clr("  LogScope Migration Tool", C.BOLD))
    print(clr("═" * 74, C.MAGENTA))
    
    total_calls = sum(len(l.log_calls) for l in loops)
    batchable = sum(len(l.batchable_calls) for l in loops)
    immediate = total_calls - batchable
    nested = [l for l in loops if l.depth > 1]
    
    print(f"\n  Loops with logging:  {clr(len(loops), C.BOLD)}")
    print(f"  Total log calls:     {total_calls}")
    print(f"    {clr('Batchable', C.GREEN)} (trace/debug/info): {batchable}")
    print(f"    {clr('Keep', C.YELLOW)} (warn/error):          {immediate}")
    
    if nested:
        print(clr(f"\n  ⚠ {len(nested)} NESTED LOOPS - will be processed FIRST!", C.RED + C.BOLD))
        for loop in nested[:5]:  # Show first 5
            rel = os.path.relpath(loop.file_path, base_path)
            print(clr(f"    depth={loop.depth}: {rel}:{loop.start_line} ({loop.loop_type})", C.RED))
        if len(nested) > 5:
            print(clr(f"    ... and {len(nested) - 5} more", C.DIM))
    
    # Check for loops in same file that might overlap
    by_file = {}
    for loop in loops:
        by_file.setdefault(loop.file_path, []).append(loop)
    
    overlapping = []
    for fpath, file_loops in by_file.items():
        if len(file_loops) > 1:
            # Sort by start line
            sorted_loops = sorted(file_loops, key=lambda l: l.start_line)
            for i in range(len(sorted_loops) - 1):
                outer = sorted_loops[i]
                inner = sorted_loops[i + 1]
                if inner.start_line < outer.end_line:
                    overlapping.append((outer, inner))
    
    if overlapping:
        print(clr(f"\n  ⚠ {len(overlapping)} OVERLAPPING loop pairs detected:", C.YELLOW))
        for outer, inner in overlapping[:3]:
            rel = os.path.relpath(outer.file_path, base_path)
            print(clr(f"    {rel}: L{outer.start_line}-{outer.end_line} contains L{inner.start_line}-{inner.end_line}", C.YELLOW))


def show_transformation(idx: int, total: int, trans: Transformation, base_path: str):
    """Show a single transformation with before/after diff."""
    loop = trans.loop
    rel_path = os.path.relpath(loop.file_path, base_path)
    
    # Header
    print()
    print(clr("═" * 74, C.DIM))
    print(f"{clr(f'[{idx}/{total}]', C.CYAN)} {clr(rel_path, C.BOLD)}:{loop.start_line}-{loop.end_line}")
    print(clr("═" * 74, C.DIM))
    
    # Loop info
    depth_str = clr(f" ⚠ NESTED×{loop.depth}", C.RED + C.BOLD) if loop.depth > 1 else ""
    print(f"\n  📍 {clr(loop.loop_type, C.BLUE)} @ L{loop.start_line}{depth_str}")
    if loop.loop_var:
        col = loop.loop_collection[:40] + "..." if loop.loop_collection and len(loop.loop_collection) > 40 else loop.loop_collection
        print(f"     {clr(loop.loop_var, C.GREEN)} : {col or '...'}")
    
    # Log calls summary
    print(f"\n  {clr('LOG CALLS:', C.BOLD)}")
    level_colors = {'trace': C.DIM, 'debug': C.CYAN, 'info': C.GREEN, 'warn': C.YELLOW, 'error': C.RED}
    
    for call in loop.log_calls:
        lc = level_colors.get(call.level, C.END)
        action = clr("→ BATCH", C.GREEN) if call.is_batchable else clr("→ KEEP", C.YELLOW)
        print(f"    L{call.line_num}: {clr(call.level.upper(), lc)} {action}")
    
    # BEFORE
    print(f"\n{clr('BEFORE:', C.RED + C.BOLD)}")
    print(clr("─" * 70, C.DIM))
    
    for i, line in enumerate(loop.original_lines):
        line_num = loop.start_line + i
        # Check if this line has a log call
        is_log = any(c.line_num == line_num for c in loop.log_calls)
        marker = '→' if is_log else ' '
        col = C.RED if is_log else C.DIM
        # Truncate very long lines
        display = line if len(line) <= 100 else line[:97] + "..."
        print(clr(f"  {marker}{line_num:4}│ {display}", col))
    
    # AFTER
    if loop.has_batchable:
        print(f"\n{clr('AFTER:', C.GREEN + C.BOLD)}")
        print(clr("─" * 70, C.DIM))
        
        for i, line in enumerate(trans.transformed_lines):
            # Determine line type
            is_scope_open = 'LogScope scope' in line
            is_scope_close = line.strip() == '}' and i == len(trans.transformed_lines) - 1
            is_branch = 'scope.branch' in line
            
            if is_scope_open or is_scope_close:
                marker = '+'
                col = C.GREEN + C.BOLD
            elif is_branch:
                marker = '~'
                col = C.CYAN
            else:
                marker = ' '
                col = C.DIM
            
            # Don't truncate scope lines or branch lines - they're important
            if is_scope_open or is_branch:
                display = line
            else:
                display = line if len(line) <= 100 else line[:97] + "..."
            print(clr(f"  {marker}    │ {display}", col))
        
        if loop.immediate_calls:
            print(clr(f"\n  ⚠ {len(loop.immediate_calls)} warn/error calls kept immediate", C.YELLOW))
    else:
        print(clr("\n  (no batchable calls - nothing to change)", C.YELLOW))


def show_action_menu(has_batchable: bool):
    """Show action menu and get user choice."""
    print()
    print(clr("╭────────────────────────────────────────────────────╮", C.CYAN))
    if has_batchable:
        print(clr("│", C.CYAN) + f"  {clr('[A]pply', C.GREEN+C.BOLD)}  {clr('[S]kip', C.YELLOW)}  {clr('[Q]uit', C.RED)}  " + clr("│", C.CYAN))
    else:
        print(clr("│", C.CYAN) + f"  {clr('[N]ext', C.DIM)}  {clr('[Q]uit', C.RED)}  " + clr("│", C.CYAN))
    print(clr("╰────────────────────────────────────────────────────╯", C.CYAN))
    
    while True:
        try:
            choice = input(clr("  → ", C.BOLD)).strip().lower()
        except (KeyboardInterrupt, EOFError):
            return 'q'
        
        if choice in ('', 'n', 'next'):
            return 'n'
        if choice in ('a', 'apply') and has_batchable:
            return 'a'
        if choice in ('s', 'skip'):
            return 's'
        if choice in ('q', 'quit'):
            return 'q'
        
        print(clr("    ? ", C.DIM), end='')


def show_report(applied: list, skipped: list, base_path: str):
    """Show final report."""
    print()
    print(clr("═" * 74, C.MAGENTA))
    print(clr("  📋 MIGRATION REPORT", C.BOLD))
    print(clr("═" * 74, C.MAGENTA))
    
    print(f"\n  {clr('✓ Applied:', C.GREEN)} {len(applied)}")
    print(f"  {clr('⊘ Skipped:', C.YELLOW)} {len(skipped)}")
    
    if applied:
        print(clr("\n  APPLIED:", C.GREEN))
        for loop in applied:
            rel = os.path.relpath(loop.file_path, base_path)
            print(f"    ✓ {rel}:{loop.start_line} ({len(loop.batchable_calls)} calls)")
    
    if skipped:
        print(clr("\n  SKIPPED:", C.YELLOW))
        for loop, reason in skipped:
            rel = os.path.relpath(loop.file_path, base_path)
            r = f" → {reason}" if reason else ""
            print(f"    ⊘ {rel}:{loop.start_line}{clr(r, C.DIM)}")

