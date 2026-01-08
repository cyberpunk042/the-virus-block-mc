"""Phase 4: Apply transformations to files."""

import os
import shutil
from typing import Optional, Tuple
from .models import Transformation


class Applier:
    """Applies transformations to files with backup support."""
    
    def __init__(self):
        self.last_backup: Optional[Tuple[str, str]] = None  # (backup_path, original_path)
    
    def apply(self, trans: Transformation) -> Tuple[bool, str]:
        """Apply a transformation to its file.
        
        Returns: (success, message)
        """
        loop = trans.loop
        
        if not loop.has_batchable:
            return False, "No batchable calls"
        
        try:
            # Read file
            with open(loop.file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            # Create backup
            backup_path = loop.file_path + '.bak'
            shutil.copy2(loop.file_path, backup_path)
            self.last_backup = (backup_path, loop.file_path)
            
            # Build new file content
            new_lines = []
            
            # Lines before the loop
            new_lines.extend(lines[:loop.start_line - 1])
            
            # Transformed loop
            for tline in trans.transformed_lines:
                new_lines.append(tline + '\n')
            
            # Lines after the loop
            new_lines.extend(lines[loop.end_line:])
            
            # Check/add imports
            new_lines = self._ensure_imports(new_lines)
            
            # Write file
            with open(loop.file_path, 'w', encoding='utf-8') as f:
                f.writelines(new_lines)
            
            return True, f"Applied (backup: {os.path.basename(backup_path)})"
            
        except Exception as e:
            return False, f"Error: {e}"
    
    def undo(self) -> Tuple[bool, str]:
        """Undo the last applied transformation."""
        if not self.last_backup:
            return False, "Nothing to undo"
        
        backup_path, original_path = self.last_backup
        
        try:
            shutil.copy2(backup_path, original_path)
            os.remove(backup_path)
            self.last_backup = None
            return True, "Undone"
        except Exception as e:
            return False, f"Undo failed: {e}"
    
    def _ensure_imports(self, lines: list) -> list:
        """Ensure LogScope and LogLevel imports exist."""
        content = ''.join(lines)
        
        needs_scope = 'import net.cyberpunk042.log.LogScope' not in content
        needs_level = 'import net.cyberpunk042.log.LogLevel' not in content
        
        if not needs_scope and not needs_level:
            return lines
        
        # Find last import line
        last_import_idx = 0
        for i, line in enumerate(lines):
            if line.strip().startswith('import '):
                last_import_idx = i
        
        # Insert new imports after last import
        new_lines = lines[:last_import_idx + 1]
        
        if needs_scope:
            new_lines.append('import net.cyberpunk042.log.LogScope;\n')
        if needs_level:
            new_lines.append('import net.cyberpunk042.log.LogLevel;\n')
        
        new_lines.extend(lines[last_import_idx + 1:])
        
        return new_lines

