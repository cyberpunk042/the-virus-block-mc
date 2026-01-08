"""Phase 1: Find all loops containing logging calls."""

import os
import re
from typing import List, Optional, Tuple
from .models import LogCall, LoopWithLogs


class Scanner:
    """Scans Java files for loops containing Logging calls."""
    
    # Patterns
    FOR_EACH = re.compile(r'for\s*\(\s*(?:final\s+)?([\w<>\[\],\s]+)\s+(\w+)\s*:\s*(.+?)\s*\)\s*\{?')
    FOR_I = re.compile(r'for\s*\(\s*(?:int|long)\s+(\w+)\s*=')
    WHILE = re.compile(r'while\s*\(')
    LOG_CALL = re.compile(r'Logging\.(\w+)(?:\.topic\("([^"]+)"\))?\.(\w+)\s*\(')
    
    def scan_directory(self, path: str) -> List[LoopWithLogs]:
        """Scan all Java files in directory."""
        results = []
        
        for root, dirs, files in os.walk(path):
            # Skip build directories
            dirs[:] = [d for d in dirs if d not in ('build', 'bin', '.gradle', 'out', 'target')]
            
            for fname in files:
                if fname.endswith('.java'):
                    fpath = os.path.join(root, fname)
                    results.extend(self.scan_file(fpath))
        
        return results
    
    def scan_file(self, file_path: str) -> List[LoopWithLogs]:
        """Scan a single Java file."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.read().split('\n')
        except Exception:
            return []
        
        loops = self._find_loops(lines)
        results = []
        
        for loop_info in loops:
            log_calls = self._find_log_calls(lines, loop_info)
            if log_calls:
                loop = LoopWithLogs(
                    file_path=file_path,
                    start_line=loop_info['start'],
                    end_line=loop_info['end'],
                    loop_type=loop_info['type'],
                    loop_var=loop_info['var'],
                    loop_collection=loop_info['collection'],
                    indent=loop_info['indent'],
                    depth=loop_info['depth'],
                    original_lines=lines[loop_info['start']-1 : loop_info['end']],
                    log_calls=log_calls
                )
                results.append(loop)
        
        return results
    
    def _find_loops(self, lines: List[str]) -> List[dict]:
        """Find all loops with their boundaries."""
        loops = []
        stack = []  # Active loops being tracked
        brace_depth = 0
        
        for i, line in enumerate(lines):
            line_num = i + 1
            stripped = line.strip()
            
            # Skip comments
            if stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
                brace_depth += line.count('{') - line.count('}')
                continue
            
            indent = line[:len(line) - len(line.lstrip())]
            
            # Check for loop start
            loop_info = self._parse_loop_start(line)
            if loop_info:
                loop_info['start'] = line_num
                loop_info['indent'] = indent
                loop_info['start_brace_depth'] = brace_depth + line.count('{')
                loop_info['depth'] = len(stack) + 1
                stack.append(loop_info)
            
            # Update brace depth
            brace_depth += line.count('{') - line.count('}')
            
            # Check if any loops closed
            while stack and brace_depth < stack[-1]['start_brace_depth']:
                finished = stack.pop()
                finished['end'] = line_num
                loops.append(finished)
        
        return loops
    
    def _parse_loop_start(self, line: str) -> Optional[dict]:
        """Parse a line to see if it starts a loop."""
        # for-each: for (Type var : collection)
        m = self.FOR_EACH.search(line)
        if m:
            return {
                'type': 'for-each',
                'var': m.group(2),
                'collection': m.group(3).strip()
            }
        
        # for-i: for (int i = ...)
        m = self.FOR_I.search(line)
        if m:
            return {
                'type': 'for-i',
                'var': m.group(1),
                'collection': None
            }
        
        # while
        if self.WHILE.search(line):
            return {
                'type': 'while',
                'var': None,
                'collection': None
            }
        
        return None
    
    def _find_log_calls(self, lines: List[str], loop_info: dict) -> List[LogCall]:
        """Find all logging calls within a loop."""
        calls = []
        start = loop_info['start'] - 1  # 0-indexed
        end = loop_info['end'] - 1
        
        i = start
        while i <= end:
            line = lines[i]
            match = self.LOG_CALL.search(line)
            
            if match:
                # Get complete call (may span multiple lines)
                full_text, call_end = self._get_complete_call(lines, i, match.start())
                fmt_str, args = self._parse_arguments(full_text)
                indent = line[:len(line) - len(line.lstrip())]
                
                calls.append(LogCall(
                    line_num=i + 1,
                    line_offset=i - start,
                    end_line=call_end + 1,
                    original_text=full_text,
                    channel=match.group(1),
                    topic=match.group(2),
                    level=match.group(3).lower(),
                    format_string=fmt_str,
                    arguments=args,
                    indent=indent
                ))
                
                i = call_end
            
            i += 1
        
        return calls
    
    def _get_complete_call(self, lines: List[str], start_idx: int, col: int) -> Tuple[str, int]:
        """Extract a complete log call that may span multiple lines."""
        result_lines = [lines[start_idx]]
        
        # Count parentheses from the start of the call
        text = lines[start_idx][col:]
        depth = text.count('(') - text.count(')')
        end_idx = start_idx
        
        while depth > 0 and end_idx < len(lines) - 1:
            end_idx += 1
            result_lines.append(lines[end_idx])
            depth += lines[end_idx].count('(') - lines[end_idx].count(')')
        
        return '\n'.join(result_lines), end_idx
    
    def _parse_arguments(self, full_call: str) -> Tuple[str, List[str]]:
        """Extract format string and arguments from a log call."""
        # Find the LOG LEVEL method call (trace/debug/info/warn/error)
        # This avoids matching .topic("...")
        level_match = re.search(r'\.(trace|debug|info|warn|error)\s*\(', full_call, re.IGNORECASE)
        if not level_match:
            return "", []
        
        # Now find the format string after the level method
        after_level = full_call[level_match.end():]
        fmt_match = re.search(r'\s*"((?:[^"\\]|\\.)*)"\s*([,)])', after_level, re.DOTALL)
        if not fmt_match:
            return "", []
        
        fmt_str = fmt_match.group(1)
        
        # No arguments if ends with )
        if fmt_match.group(2) == ')':
            return fmt_str, []
        
        # Parse arguments after the comma
        after = after_level[fmt_match.end()-1:]  # Start at comma
        return fmt_str, self._split_arguments(after[1:])  # Skip comma
    
    def _split_arguments(self, text: str) -> List[str]:
        """Split comma-separated arguments, respecting nested parens/brackets."""
        args = []
        current = []
        depth = 0
        in_string = False
        
        for i, ch in enumerate(text):
            # Track strings
            if ch == '"' and (i == 0 or text[i-1] != '\\'):
                in_string = not in_string
                current.append(ch)
                continue
            
            if in_string:
                current.append(ch)
                continue
            
            # Track nesting
            if ch in '([{':
                depth += 1
                current.append(ch)
            elif ch in ')]}':
                if depth > 0:
                    depth -= 1
                    current.append(ch)
                else:
                    # End of call
                    arg = ''.join(current).strip()
                    if arg:
                        args.append(arg)
                    return args
            elif ch == ',' and depth == 0:
                arg = ''.join(current).strip()
                if arg:
                    args.append(arg)
                current = []
            else:
                current.append(ch)
        
        return args

