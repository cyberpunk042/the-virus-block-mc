#!/usr/bin/env python3
"""
usage_graph.py

Search your mod source tree for occurrences of a symbol (class, method, field, etc.)
and present a grouped "usage graph" by file, with line numbers.

This is meant as a refactor helper: before changing something important,
see where it is referenced across your codebase.

Features:
    - Uses ripgrep (rg) if available, with JSON output parsing (robust).
    - Falls back to grep -RIn if rg is not available.
    - Groups results by file and shows line numbers + line content.
    - Supports case-insensitive search.
    - Supports limiting matches per file.
    - Supports saving full output to a file via --save (default: agent-tools/usage/).

Examples:
    # Basic search in src/
    python3 scripts/usage_graph.py LivingEntity

    # Search heal in both src/main/java and src/test/java
    python3 scripts/usage_graph.py heal --paths src/main/java src/test/java

    # Case-insensitive, limit to 50 results per file
    python3 scripts/usage_graph.py tick --ignore-case --max-per-file 50

    # Save output to agent-tools/usage/
    python3 scripts/usage_graph.py PlayerEntity --save

    # Save output to custom base directory
    python3 scripts/usage_graph.py onDeath --save my-agent-tools
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Sequence


@dataclass
class UsageMatch:
    file: Path
    line: int
    text: str


# ---------------------------------------------------------------------------
# Output capture helper
# ---------------------------------------------------------------------------

def make_output_collector():
    buffer: List[str] = []

    def out(*args):
        msg = " ".join(str(a) for a in args)
        buffer.append(msg)
        print(msg)

    return out, buffer


# ---------------------------------------------------------------------------
# Core search backends
# ---------------------------------------------------------------------------

def has_rg() -> bool:
    """Check if ripgrep (rg) is available."""
    return shutil.which("rg") is not None


def run_rg(symbol: str, paths: Sequence[Path], ignore_case: bool, globs: Sequence[str]) -> List[UsageMatch]:
    """
    Use ripgrep with --json to find matches and parse them into UsageMatch objects.
    """
    cmd = ["rg", "--json"]
    if ignore_case:
        cmd.append("-i")
    # add globs
    for g in globs:
        cmd.extend(["-g", g])
    cmd.append(symbol)
    cmd.extend(str(p) for p in paths)

    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError:
        return []

    matches: List[UsageMatch] = []

    if result.stdout:
        for line in result.stdout.splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue

            if obj.get("type") != "match":
                continue

            data = obj.get("data", {})
            path_text = data.get("path", {}).get("text")
            line_number = data.get("line_number")
            line_text = data.get("lines", {}).get("text", "").rstrip("\n")

            if path_text is None or line_number is None:
                continue

            matches.append(
                UsageMatch(
                    file=Path(path_text),
                    line=int(line_number),
                    text=line_text,
                )
            )
    return matches


def run_grep(symbol: str, paths: Sequence[Path], ignore_case: bool) -> List[UsageMatch]:
    """
    Fallback when rg is not available: use grep -RIn.
    We assume the format: file:line:content
    """
    cmd = ["grep", "-RIn"]
    if ignore_case:
        cmd.append("-i")
    cmd.append(symbol)
    cmd.extend(str(p) for p in paths)

    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError:
        return []

    matches: List[UsageMatch] = []

    stdout = result.stdout or ""
    for line in stdout.splitlines():
        # Expect "file:line:text"
        parts = line.split(":", 2)
        if len(parts) < 3:
            continue
        file_str, line_str, text = parts
        try:
            line_no = int(line_str)
        except ValueError:
            continue
        matches.append(
            UsageMatch(
                file=Path(file_str),
                line=line_no,
                text=text.rstrip("\n"),
            )
        )
    return matches


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(base_dir: Path, symbol: str, buffer: List[str], out) -> None:
    """
    Save buffered output to base_dir/usage/<symbol>_<timestamp>.txt
    """
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "usage"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    sym = symbol.replace(" ", "_").replace("/", "_").replace(".", "_")
    fname = f"{sym}_{timestamp}.txt"
    path = sub / fname

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Usage graph written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build a usage graph for a symbol across your source tree."
    )
    parser.add_argument(
        "symbol",
        help="Symbol to search for (e.g. class name, method name, field name).",
    )
    parser.add_argument(
        "--paths",
        nargs="+",
        type=Path,
        default=[Path("src")],
        help="Root paths to search (default: src). For example: src/main/java src/test/java",
    )
    parser.add_argument(
        "--ignore-case",
        action="store_true",
        help="Case-insensitive search.",
    )
    parser.add_argument(
        "--max-per-file",
        type=int,
        default=100,
        help="Maximum number of matches to display per file (0 = unlimited).",
    )
    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save output to files under this base directory (default: agent-tools).",
    )
    parser.add_argument(
        "--no-globs",
        action="store_true",
        help="Disable default file globs (search all files).",
    )
    parser.add_argument(
        "--glob",
        action="append",
        default=None,
        help="Additional glob(s) to pass to ripgrep (e.g. --glob '*.java'). Can be used multiple times.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    out, buffer = make_output_collector()

    symbol = args.symbol
    paths: List[Path] = [p for p in args.paths if p.exists()]
    if not paths:
        out("[err] None of the specified paths exist.")
        if args.save:
            save_output(Path(args.save), symbol, buffer, out)
        sys.exit(1)

    # Default globs if not disabled
    if args.no_globs:
        globs: List[str] = []
    else:
        # reasonable defaults for Minecraft mods
        globs = [
            "*.java",
            "*.kt",
            "*.groovy",
            "*.scala",
            "*.json",
            "*.mcmeta",
            "*.txt",
        ]

    if args.glob:
        globs.extend(args.glob)

    out(f"[info] Searching for symbol '{symbol}'")
    out(f"[info] Paths: {', '.join(str(p) for p in paths)}")
    out(f"[info] ignore-case: {args.ignore_case}")
    out(f"[info] max-per-file: {args.max_per_file if args.max_per_file else 'unlimited'}")
    out(f"[info] using ripgrep: {has_rg()}")

    matches: List[UsageMatch]
    if has_rg():
        matches = run_rg(symbol, paths, args.ignore_case, globs)
    else:
        out("[warn] ripgrep (rg) is not available. Falling back to grep.")
        matches = run_grep(symbol, paths, args.ignore_case)

    if not matches:
        out("[info] No matches found.")
        if args.save:
            save_output(Path(args.save), symbol, buffer, out)
        return

    # Group by file
    grouped: Dict[Path, List[UsageMatch]] = defaultdict(list)
    for m in matches:
        grouped[m.file].append(m)

    # Sort files and matches
    for file_path in sorted(grouped.keys(), key=lambda p: str(p)):
        file_matches = sorted(grouped[file_path], key=lambda m: m.line)

        # Respect max-per-file
        limited = file_matches
        if args.max_per_file and len(file_matches) > args.max_per_file:
            limited = file_matches[: args.max_per_file]
            omitted = len(file_matches) - len(limited)
        else:
            omitted = 0

        out("")
        out(f"FILE: {file_path}  (hits: {len(file_matches)})")
        for m in limited:
            # basic trimming of whitespace
            text = m.text.replace("\t", "    ")
            out(f"  L{m.line}: {text}")

        if omitted > 0:
            out(f"  ... {omitted} more match(es) omitted in this file (raise --max-per-file to see more)")

    out("")
    out(f"[summary] Total matches: {len(matches)} in {len(grouped)} file(s).")

    if args.save:
        save_output(Path(args.save), symbol, buffer, out)


if __name__ == "__main__":
    main()
