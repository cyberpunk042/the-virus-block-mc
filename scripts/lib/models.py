"""Data models for the migration tool."""

from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class LogCall:
    """A single logging call found inside a loop."""
    line_num: int              # Absolute line number in file
    line_offset: int           # Offset from loop start (0-based)
    end_line: int              # End line (for multi-line calls)
    original_text: str         # Exact original text (may be multi-line)
    channel: str               # e.g., "FIELD", "GUI"
    topic: Optional[str]       # e.g., "render" or None
    level: str                 # trace/debug/info/warn/error
    format_string: str         # The format string
    arguments: List[str]       # The arguments
    indent: str                # Leading whitespace
    
    @property
    def is_batchable(self) -> bool:
        return self.level in ('trace', 'debug', 'info')


@dataclass
class LoopWithLogs:
    """A loop containing one or more logging calls."""
    file_path: str
    start_line: int            # First line of loop
    end_line: int              # Last line of loop (closing brace)
    loop_type: str             # for-each, for-i, while
    loop_var: Optional[str]    # Iteration variable name
    loop_collection: Optional[str]  # What's being iterated
    indent: str                # Whitespace before loop keyword
    depth: int                 # Nesting depth (1 = not nested)
    original_lines: List[str]  # Exact original lines (with newlines stripped)
    log_calls: List[LogCall] = field(default_factory=list)
    
    @property
    def batchable_calls(self) -> List[LogCall]:
        return [c for c in self.log_calls if c.is_batchable]
    
    @property
    def immediate_calls(self) -> List[LogCall]:
        return [c for c in self.log_calls if not c.is_batchable]
    
    @property
    def has_batchable(self) -> bool:
        return len(self.batchable_calls) > 0


@dataclass
class Transformation:
    """A pending transformation for a loop."""
    loop: LoopWithLogs
    transformed_lines: List[str]  # The new code
    scope_line: str               # The try (LogScope...) line
    replacements: List[tuple]     # [(original, replacement), ...]

