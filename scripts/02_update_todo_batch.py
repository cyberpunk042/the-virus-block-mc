#!/usr/bin/env python3
"""
Utility to batch update TODO_LIST.md
Usage: python3 scripts/update_todo_batch.py <batch_number> [--complete]

Examples:
  python3 scripts/update_todo_batch.py 16 --complete   # Mark batch 16 complete
  python3 scripts/update_todo_batch.py 17              # Show batch 17 status
"""

import re
import sys
import os

TODO_FILE = "docs/field-system/NEW_REFACTORING_NEW_PHASES/TODO_LIST.md"

def read_todo():
    with open(TODO_FILE, 'r') as f:
        return f.read()

def write_todo(content):
    with open(TODO_FILE, 'w') as f:
        f.write(content)

def mark_batch_complete(batch_num, notes=None):
    """Mark all tasks in a batch as complete."""
    content = read_todo()
    
    # Pattern to find task rows: | Fxxx | ... | ⬜ | ... |
    # We look for rows after "Batch {batch_num}:" header
    
    # Find batch section
    batch_pattern = rf"### Batch {batch_num}:.*?\n\n---"
    batch_match = re.search(batch_pattern, content, re.DOTALL)
    
    if not batch_match:
        print(f"❌ Batch {batch_num} not found")
        return False
    
    batch_section = batch_match.group(0)
    
    # Replace ⬜ with ✅ in task rows (not headers)
    updated_section = re.sub(r'\| ⬜ \|', '| ✅ |', batch_section)
    
    # Update content
    content = content.replace(batch_section, updated_section)
    
    # Update Quick Stats
    content = update_stats(content)
    
    write_todo(content)
    
    # Count changes
    count = batch_section.count('⬜') - updated_section.count('⬜')
    print(f"✅ Marked {count} tasks complete in Batch {batch_num}")
    return True

def mark_tasks_complete(task_ids, notes_map=None):
    """Mark specific tasks as complete with optional notes."""
    content = read_todo()
    notes_map = notes_map or {}
    
    for task_id in task_ids:
        # Find and update the task row
        # Pattern: | F123 | description | ⬜ | notes |
        pattern = rf'(\| {re.escape(task_id)} \|[^|]+\|) ⬜ (\|[^|]*\|)'
        
        note = notes_map.get(task_id, '-')
        replacement = rf'\1 ✅ | {note} |'
        
        new_content = re.sub(pattern, replacement, content)
        if new_content != content:
            content = new_content
            print(f"✅ {task_id}")
        else:
            print(f"⚠️ {task_id} not found or already complete")
    
    content = update_stats(content)
    write_todo(content)

def update_stats(content):
    """Update Quick Stats section."""
    # Count ✅ and ⬜ in the file
    done_count = content.count('| ✅ |')
    pending_count = content.count('| ⬜ |')
    in_progress = content.count('| 🔄 |')
    
    # Update stats table
    stats_pattern = r'\| ✅ Done \| ~?\d+ \|\n\| 🔄 In Progress \| \d+ \|\n\| ⬜ Pending \| ~?\d+ \|'
    stats_replacement = f'| ✅ Done | ~{done_count} |\n| 🔄 In Progress | {in_progress} |\n| ⬜ Pending | ~{pending_count} |'
    
    content = re.sub(stats_pattern, stats_replacement, content)
    return content

def show_batch_status(batch_num):
    """Show status of tasks in a batch."""
    content = read_todo()
    
    # Find batch section
    batch_pattern = rf"### Batch {batch_num}:.*?\n\n---"
    batch_match = re.search(batch_pattern, content, re.DOTALL)
    
    if not batch_match:
        print(f"❌ Batch {batch_num} not found")
        return
    
    batch_section = batch_match.group(0)
    
    done = batch_section.count('| ✅ |')
    pending = batch_section.count('| ⬜ |')
    in_progress = batch_section.count('| 🔄 |')
    
    print(f"Batch {batch_num} Status:")
    print(f"  ✅ Done: {done}")
    print(f"  🔄 In Progress: {in_progress}")
    print(f"  ⬜ Pending: {pending}")

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return
    
    batch_num = sys.argv[1]
    
    if '--complete' in sys.argv:
        mark_batch_complete(batch_num)
    else:
        show_batch_status(batch_num)

if __name__ == "__main__":
    main()

