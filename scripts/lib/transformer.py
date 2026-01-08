"""Phase 2: Generate transformed code for loops."""

import re
from typing import List, Tuple
from .models import LogCall, LoopWithLogs, Transformation


class Transformer:
    """Generates transformed code for loops with logging."""
    
    def transform(self, loop: LoopWithLogs) -> Transformation:
        """Generate the complete transformation for a loop."""
        if not loop.has_batchable:
            # No batchable calls - no transformation needed
            return Transformation(
                loop=loop,
                transformed_lines=loop.original_lines.copy(),
                scope_line="",
                replacements=[]
            )
        
        # Determine scope parameters
        scope_line = self._generate_scope_line(loop)
        
        # Build transformed lines
        transformed = []
        replacements = []
        
        # Add scope wrapper open
        transformed.append(scope_line)
        
        # Process each line in the loop
        line_idx = 0
        while line_idx < len(loop.original_lines):
            orig_line = loop.original_lines[line_idx]
            abs_line = loop.start_line + line_idx
            
            # Check if this line starts a log call
            matching_call = None
            for call in loop.log_calls:
                if call.line_num == abs_line:
                    matching_call = call
                    break
            
            if matching_call:
                if matching_call.is_batchable:
                    # Generate replacement - preserve original indent + add 4 spaces for scope
                    replacement = self._generate_branch_call(matching_call, loop)
                    transformed.append('    ' + replacement)  # Add 4 spaces for try block
                    replacements.append((matching_call.original_text, replacement))
                    
                    # Skip continuation lines of multi-line call
                    lines_in_call = matching_call.end_line - matching_call.line_num
                    line_idx += lines_in_call
                else:
                    # Keep warn/error as-is, just add 4 spaces for try block
                    transformed.append('    ' + orig_line)
            else:
                # Regular line - add 4 spaces for scope wrapper
                if orig_line.strip():
                    transformed.append('    ' + orig_line)
                else:
                    transformed.append('')
            
            line_idx += 1
        
        # Add scope wrapper close (same indent as loop was)
        transformed.append(loop.indent + '}')
        
        return Transformation(
            loop=loop,
            transformed_lines=transformed,
            scope_line=scope_line,
            replacements=replacements
        )
    
    def _generate_scope_line(self, loop: LoopWithLogs) -> str:
        """Generate the try (LogScope scope = ...) line."""
        # Use first batchable call for channel/topic
        first = loop.batchable_calls[0]
        
        # Determine scope name from collection
        scope_name = self._infer_scope_name(loop)
        
        # Determine level (highest among batchable calls)
        level_priority = {'trace': 0, 'debug': 1, 'info': 2}
        max_level = max(loop.batchable_calls, key=lambda c: level_priority.get(c.level, 0)).level
        
        # Build the line
        topic_part = f'.topic("{first.topic}")' if first.topic else ''
        
        return (f'{loop.indent}try (LogScope scope = Logging.{first.channel}'
                f'{topic_part}.scope("{scope_name}", LogLevel.{max_level.upper()})) {{')
    
    def _infer_scope_name(self, loop: LoopWithLogs) -> str:
        """Infer a good scope name from the loop."""
        if loop.loop_collection:
            # Clean up collection name
            col = loop.loop_collection
            # foo.getRecordComponents() -> recordComponents
            m = re.search(r'\.get(\w+)\(\)', col)
            if m:
                name = m.group(1)
                # Keep camelCase: RecordComponents -> recordComponents
                name = name[0].lower() + name[1:] if name else name
                return f"process-{name}"
            # foo.items() -> items
            if '.' in col:
                last = col.split('.')[-1].split('(')[0]
                return f"process-{last}"
            return f"process-{col}"
        elif loop.loop_var:
            return f"iterate-{loop.loop_var}s"
        else:
            return f"{loop.loop_type}-loop"
    
    def _generate_branch_call(self, call: LogCall, loop: LoopWithLogs) -> str:
        """Generate scope.branch(...).kv(...) replacement."""
        # Extract key-value pairs first
        kvs = self._extract_kvs(call)
        kv_keys = {k for k, v in kvs}
        
        # Branch name: prefer loop var, but not if it's also a kv key
        branch_name = loop.loop_var or "item"
        if branch_name in kv_keys:
            # Use a more generic name to avoid duplication
            branch_name = "entry"
        
        # Build the call
        parts = [f'{call.indent}scope.branch("{branch_name}")']
        
        # Add extracted kvs
        for key, val in kvs:
            parts.append(f'.kv("{key}", {val})')
        
        # Add status hint from message
        status = self._infer_status(call.format_string)
        if status:
            parts.append(f'.kv("_s", "{status}")')
        
        return ''.join(parts) + ';'
    
    def _extract_kvs(self, call: LogCall) -> List[Tuple[str, str]]:
        """Extract key-value pairs from format string and arguments."""
        kvs = []
        
        # Look for named placeholders: "name={}" or "name: {}"
        named = re.findall(r'(\w+)\s*[=:]\s*\{\}', call.format_string)
        
        # Count total {} placeholders
        placeholder_count = call.format_string.count('{}')
        
        if named and len(named) == placeholder_count and call.arguments:
            # All placeholders are named - use them
            for i, name in enumerate(named):
                if i < len(call.arguments):
                    kvs.append((name, call.arguments[i]))
        elif call.arguments:
            # Infer names from ALL arguments (not just first few)
            for i, arg in enumerate(call.arguments[:6]):  # Limit to 6
                name = self._infer_arg_name(arg)
                # Avoid duplicate keys by appending index if needed
                if any(k == name for k, v in kvs):
                    name = f"{name}{i+1}"
                kvs.append((name, arg))
        
        return kvs
    
    def _infer_arg_name(self, arg: str) -> str:
        """Infer a key name from an argument expression."""
        arg = arg.strip()
        
        # foo.getName() -> name (extract from getter)
        m = re.search(r'\.get(\w+)\(\)$', arg)
        if m:
            name = m.group(1)
            return name[0].lower() + name[1:] if name else 'val'
        
        # foo.bar() or foo.bar().baz() -> use LAST method name
        # config.effect() -> "effect"
        # sprite.getContents().getId() -> "id"
        if '.' in arg and '(' in arg:
            m = re.search(r'\.(\w+)\(\)$', arg)
            if m:
                return m.group(1).lower()
            # Fallback: last segment before ()
            parts = arg.split('.')
            last = parts[-1].split('(')[0]
            return last.lower() if last else 'val'
        
        # foo.bar -> bar
        if '.' in arg:
            return arg.split('.')[-1].lower()
        
        # Simple identifier
        if arg.replace('_', '').isalnum():
            return arg
        
        return 'val'
    
    def _infer_status(self, fmt: str) -> str:
        """Infer a status from the format string."""
        fmt_lower = fmt.lower()
        if any(w in fmt_lower for w in ('skip', 'ignor', 'bypass')):
            return 'skip'
        if any(w in fmt_lower for w in ('fail', 'error', 'cannot', 'unable')):
            return 'fail'
        if any(w in fmt_lower for w in ('success', 'complete', 'done', 'ready', 'loaded')):
            return 'ok'
        return ''

