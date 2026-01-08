#!/usr/bin/env python3
"""
mod_interface_map.py

Analyze your mod source tree and map how it interfaces with Minecraft internals.

It tries to answer:
    - Which net.minecraft.* classes are imported where?
    - Which Mixin targets are used?
    - Which files are "hot" (touch lots of Minecraft internals)?

It focuses on source-level analysis (Java/Kotlin), not bytecode.

Signals detected:
    1) Imports:
        import net.minecraft.foo.Bar;
        import net.minecraft.*;   (wildcards noted)
    2) Mixin targets:
        @Mixin(Bar.class)
        @Mixin(net.minecraft.foo.Bar.class)
    3) Optional: generic "net.minecraft." mentions

Features:
    - Scans specified paths (default: src).
    - Uses ripgrep (rg) if available; falls back to grep -R.
    - Groups results by Minecraft class / symbol and by file.
    - Prints:
        - Per-Minecraft-class usage locations
        - Per-file summary (how much Minecraft it touches)
    - Supports:
        --paths
        --mc-prefix              (default: net.minecraft.)
        --mod-prefix             (for filtering / highlighting your own package)
        --max-locations-per-sym
        --max-syms-per-file
        --save (default base: agent-tools)

Examples:
    # Basic interface map over src/
    python3 scripts/mod_interface_map.py

    # Multiple roots
    python3 scripts/mod_interface_map.py --paths src/main/java src/main/kotlin

    # Restrict to your mod package for display
    python3 scripts/mod_interface_map.py --mod-prefix net.cyberpunk042

    # Save to agent-tools/interface-map/
    python3 scripts/mod_interface_map.py --save

    # Save to custom base directory
    python3 scripts/mod_interface_map.py --save my-agent-tools
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
from typing import Dict, List, Optional, Sequence, Tuple


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class Location:
    file: Path
    line: int
    text: str


@dataclass
class SymbolUsage:
    symbol: str
    locations: List[Location]


# ---------------------------------------------------------------------------
# Output collector
# ---------------------------------------------------------------------------

def make_output_collector():
    buffer: List[str] = []

    def out(*args):
        msg = " ".join(str(a) for a in args)
        buffer.append(msg)
        print(msg)

    return out, buffer


# ---------------------------------------------------------------------------
# ripgrep / grep helpers
# ---------------------------------------------------------------------------

def has_rg() -> bool:
    return shutil.which("rg") is not None


def run_rg(pattern: str, paths: Sequence[Path], globs: Sequence[str]) -> List[Location]:
    cmd = ["rg", "--json"]
    for g in globs:
        cmd.extend(["-g", g])
    cmd.append(pattern)
    cmd.extend(str(p) for p in paths)

    result = subprocess.run(cmd, capture_output=True, text=True)
    locs: List[Location] = []

    if not result.stdout:
        return locs

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
        locs.append(
            Location(
                file=Path(path_text),
                line=int(line_number),
                text=line_text,
            )
        )
    return locs


def run_grep(pattern: str, paths: Sequence[Path]) -> List[Location]:
    cmd = ["grep", "-RIn", pattern]
    cmd.extend(str(p) for p in paths)
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError:
        return []

    locs: List[Location] = []
    stdout = result.stdout or ""
    for line in stdout.splitlines():
        parts = line.split(":", 2)
        if len(parts) < 3:
            continue
        file_str, line_str, text = parts
        try:
            line_no = int(line_str)
        except ValueError:
            continue
        locs.append(
            Location(
                file=Path(file_str),
                line=line_no,
                text=text.rstrip("\n"),
            )
        )
    return locs


# ---------------------------------------------------------------------------
# Parsing helpers (imports, mixins)
# ---------------------------------------------------------------------------

def parse_import_symbol(line: str, mc_prefix: str) -> Optional[str]:
    """
    Parse a single Java/Kotlin import line for net.minecraft.* symbols.
    Returns fully-qualified class name if found, otherwise None.

    Examples:
        import net.minecraft.entity.LivingEntity;
        import net.minecraft.world.*;
    """
    stripped = line.strip()
    if not stripped.startswith("import "):
        return None
    # remove 'import' and trailing ';'
    body = stripped[len("import "):].strip()
    if body.endswith(";"):
        body = body[:-1].strip()

    if not body.startswith(mc_prefix):
        return None

    # wildcards are tracked as-is
    return body


def parse_mixin_target(line: str, mc_prefix: str) -> Optional[str]:
    """
    Parse a line containing @Mixin(...) to extract potential target class.

    Examples:
        @Mixin(LivingEntity.class)
        @Mixin(net.minecraft.entity.LivingEntity.class)
    """
    stripped = line.strip()
    if "@Mixin" not in stripped:
        return None

    # naive parse: look for parentheses content
    start = stripped.find("(")
    end = stripped.find(")", start + 1)
    if start == -1 or end == -1 or end <= start + 1:
        return None

    inside = stripped[start + 1:end].strip()

    # common patterns:
    #   Foo.class
    #   net.minecraft.entity.Foo.class
    #   { Foo.class, Bar.class }   (we ignore multiple for now; just take first)
    if "," in inside:
        inside = inside.split(",", 1)[0].strip()

    if inside.endswith(".class"):
        inside = inside[:-len(".class")].strip()

    # If it already looks fully qualified and matches mc_prefix, we return as-is.
    if inside.startswith(mc_prefix):
        return inside

    # Otherwise, we only keep fully qualified Minecraft names here.
    # (Short names could be resolved via imports, but that's extra complexity.)
    return None


def collect_imports_and_mixins(
    locs_imports: List[Location],
    locs_mixins: List[Location],
    mc_prefix: str,
) -> Tuple[Dict[str, List[Location]], Dict[str, List[Location]]]:
    imports_by_symbol: Dict[str, List[Location]] = defaultdict(list)
    mixins_by_symbol: Dict[str, List[Location]] = defaultdict(list)

    for loc in locs_imports:
        sym = parse_import_symbol(loc.text, mc_prefix)
        if sym:
            imports_by_symbol[sym].append(loc)

    for loc in locs_mixins:
        sym = parse_mixin_target(loc.text, mc_prefix)
        if sym:
            mixins_by_symbol[sym].append(loc)

    return imports_by_symbol, mixins_by_symbol


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(base_dir: Path, label: str, buffer: List[str], out) -> None:
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "interface-map"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    lab = label.replace(" ", "_").replace("/", "_").replace(".", "_")
    filename = f"{lab}_{timestamp}.txt"
    path = sub / filename

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Interface map written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Analyze how your mod interfaces with Minecraft internals."
    )
    parser.add_argument(
        "--paths",
        nargs="+",
        type=Path,
        default=[Path("src")],
        help="Root paths to search (default: src).",
    )
    parser.add_argument(
        "--mc-prefix",
        default="net.minecraft.",
        help="Prefix used to detect Minecraft classes (default: net.minecraft.).",
    )
    parser.add_argument(
        "--mod-prefix",
        help="Your mod's base package prefix (used only for display filtering / highlighting).",
    )
    parser.add_argument(
        "--max-locations-per-sym",
        type=int,
        default=30,
        help="Max locations to show per Minecraft symbol (0 = unlimited).",
    )
    parser.add_argument(
        "--max-syms-per-file",
        type=int,
        default=50,
        help="Max unique Minecraft symbols to list per file (0 = unlimited).",
    )
    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save output to this base directory (default: agent-tools).",
    )
    parser.add_argument(
        "--no-globs",
        action="store_true",
        help="Disable default source file globs (search all files).",
    )
    parser.add_argument(
        "--glob",
        action="append",
        default=None,
        help="Extra globs for ripgrep (e.g. --glob '*.java').",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    out, buffer = make_output_collector()

    paths = [p for p in args.paths if p.exists()]
    if not paths:
        out("[err] None of the specified paths exist.")
        if args.save:
            save_output(Path(args.save), "mod_interface_map_error", buffer, out)
        sys.exit(1)

    # globs
    if args.no_globs:
        globs: List[str] = []
    else:
        globs = [
            "*.java",
            "*.kt",
            "*.groovy",
        ]
    if args.glob:
        globs.extend(args.glob)

    mc_prefix = args.mc_prefix
    mod_prefix = args.mod_prefix

    out(f"[info] Paths: {', '.join(str(p) for p in paths)}")
    out(f"[info] mc-prefix: {mc_prefix}")
    out(f"[info] mod-prefix: {mod_prefix if mod_prefix else '(none)'}")
    out(f"[info] using ripgrep: {has_rg()}")

    # Search patterns:
    # 1) imports: "import net.minecraft."
    # 2) mixin annotations: "@Mixin(" (later filtered to mc-prefix)
    if has_rg():
        out("[info] Searching for imports...")
        locs_imports = run_rg("import " + mc_prefix, paths, globs)
        out(f"[info] Found {len(locs_imports)} import line(s) mentioning {mc_prefix}.")

        out("[info] Searching for @Mixin...")
        locs_mixins = run_rg("@Mixin", paths, globs)
        out(f"[info] Found {len(locs_mixins)} line(s) with @Mixin.")
    else:
        out("[warn] ripgrep not found. Using grep fallback.")
        locs_imports = run_grep("import " + mc_prefix, paths)
        out(f"[info] Found {len(locs_imports)} import line(s) mentioning {mc_prefix}.")

        locs_mixins = run_grep("@Mixin", paths)
        out(f"[info] Found {len(locs_mixins)} line(s) with @Mixin.")

    imports_by_symbol, mixins_by_symbol = collect_imports_and_mixins(
        locs_imports,
        locs_mixins,
        mc_prefix,
    )

    # Merge into unified symbol usage
    usage_by_symbol: Dict[str, List[Location]] = defaultdict(list)
    for sym, locs in imports_by_symbol.items():
        usage_by_symbol[sym].extend(locs)
    for sym, locs in mixins_by_symbol.items():
        usage_by_symbol[sym].extend(locs)

    if not usage_by_symbol:
        out("[info] No Minecraft symbols detected via imports/mixins.")
        if args.save:
            save_output(Path(args.save), "mod_interface_map_empty", buffer, out)
        return

    # Deduplicate locations per symbol (file,line)
    for sym, locs in usage_by_symbol.items():
        seen = set()
        unique_locs: List[Location] = []
        for loc in locs:
            key = (loc.file, loc.line)
            if key in seen:
                continue
            seen.add(key)
            unique_locs.append(loc)
        usage_by_symbol[sym] = unique_locs

    out("")
    out("[info] Per-symbol Minecraft usage:")
    out("----------------------------------")

    # Sort symbols for deterministic output
    sym_list = sorted(usage_by_symbol.keys())

    for sym in sym_list:
        locs = sorted(usage_by_symbol[sym], key=lambda l: (str(l.file), l.line))
        total = len(locs)
        limited = locs
        if args.max_locations_per_sym and total > args.max_locations_per_sym:
            limited = locs[: args.max_locations_per_sym]
            omitted = total - len(limited)
        else:
            omitted = 0

        out("")
        out(f"SYMBOL: {sym}")
        out(f"  total locations: {total}")
        for loc in limited:
            text = loc.text.replace("\t", "    ").strip()
            prefix_mark = ""
            if mod_prefix and str(loc.file).replace("\\", "/").find(mod_prefix.replace(".", "/")) != -1:
                prefix_mark = "[MOD]"
            out(f"    {prefix_mark} {loc.file}:L{loc.line}: {text}")
        if omitted > 0:
            out(f"    ... {omitted} more location(s) omitted (increase --max-locations-per-sym to see more)")

    # Build per-file summary
    out("")
    out("[info] Per-file Minecraft symbol summary:")
    out("-----------------------------------------")

    symbols_per_file: Dict[Path, List[str]] = defaultdict(list)
    for sym, locs in usage_by_symbol.items():
        for loc in locs:
            if sym not in symbols_per_file[loc.file]:
                symbols_per_file[loc.file].append(sym)

    for file_path in sorted(symbols_per_file.keys(), key=lambda p: str(p)):
        syms = sorted(symbols_per_file[file_path])
        total = len(syms)
        limited = syms
        if args.max_syms_per_file and total > args.max_syms_per_file:
            limited = syms[: args.max_syms_per_file]
            omitted = total - len(limited)
        else:
            omitted = 0

        mark = ""
        if mod_prefix and str(file_path).replace("\\", "/").find(mod_prefix.replace(".", "/")) != -1:
            mark = "[MOD]"
        out("")
        out(f"FILE: {mark} {file_path}")
        out(f"  total Minecraft symbols: {total}")
        for s in limited:
            out(f"    - {s}")
        if omitted > 0:
            out(f"    ... {omitted} more symbol(s) omitted in this file (increase --max-syms-per-file to see more)")

    out("")
    out(f"[summary] Unique Minecraft symbols: {len(sym_list)}")
    out(f"[summary] Files touching Minecraft: {len(symbols_per_file)}")

    if args.save:
        label = "mod_interface_map"
        if mod_prefix:
            label += "_" + mod_prefix.replace(".", "_")
        save_output(Path(args.save), label, buffer, out)


if __name__ == "__main__":
    main()
