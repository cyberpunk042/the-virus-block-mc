#!/usr/bin/env python3
"""
Search recursively through Minecraft native jar files for ASCII patterns.

Usage:
    python3 scripts/00_search_native_jars.py /mnt/f/minecraftNativeJars getReachDistance
    python3 scripts/00_search_native_jars.py --javap --javap-opts "-c -p" /path/to/jars somePattern
    python3 scripts/00_search_native_jars.py --save agent-tools /path/to/jars methodName

Notes:
    - Searches all .jar files under the given root (or a single jar if root is a file).
    - Looks for ASCII byte patterns in .class entries.
    - Can optionally run javap for matches or for a specific class.
    - With --save, writes all output to a structured text file as well as printing to screen.
"""

from __future__ import annotations

import argparse
import shlex
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Iterable, Sequence
from zipfile import BadZipFile, ZipFile


# ---------------------------------------------------------------------------
# Core helpers
# ---------------------------------------------------------------------------

def find_jars(root: Path) -> Iterable[Path]:
    """Yield all .jar paths under root (or root itself if it's a jar)."""
    if root.is_file():
        if root.suffix == ".jar":
            yield root
        return
    for path in root.rglob("*.jar"):
        if path.is_file():
            yield path


def search_jar(
    jar_path: Path,
    patterns: Sequence[bytes],
    *,
    case_sensitive: bool,
) -> list[tuple[str, str]]:
    """
    Search a single jar for patterns in .class entries.

    Returns a list of (entry.filename, needle_str).
    """
    matches: list[tuple[str, str]] = []
    try:
        with ZipFile(jar_path) as jar:
            for entry in jar.infolist():
                if entry.is_dir() or not entry.filename.endswith(".class"):
                    continue
                with jar.open(entry) as fp:
                    data = fp.read()
                hay = data if case_sensitive else data.lower()
                for needle in patterns:
                    ned = needle if case_sensitive else needle.lower()
                    if ned in hay:
                        matches.append((entry.filename, needle.decode("utf-8", "ignore")))
                        break
    except BadZipFile:
        # jar unreadable
        # (we log from caller via `out`, not here)
        pass
    return matches


def entry_to_class(entry_name: str) -> str:
    """Convert a .class entry path to a fully-qualified class name."""
    return entry_name[:-6].replace("/", ".")


def disassemble_with_javap(
    jar_path: Path,
    class_name: str,
    javap_opts: list[str],
    out,
) -> None:
    """Run javap for a given class inside a jar."""
    cmd = ["javap", "-classpath", str(jar_path)]
    if javap_opts:
        cmd.extend(javap_opts)
    cmd.append(class_name)
    out(f"[javap] {' '.join(cmd)}")
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
    except FileNotFoundError:
        out("[javap] javap command not found. Install a JDK or adjust PATH.")
        return

    if result.stdout:
        out(result.stdout.rstrip("\n"))
    if result.stderr:
        out(result.stderr.rstrip("\n"))


def class_exists_in_jar(jar_path: Path, class_name: str) -> bool:
    """Check if a given class exists in a jar."""
    entry = class_name.replace(".", "/") + ".class"
    try:
        with ZipFile(jar_path) as jar:
            return entry in jar.namelist()
    except BadZipFile:
        return False


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(
    base_dir: Path,
    root: Path,
    patterns: Sequence[str],
    buffer: list[str],
    out,
) -> None:
    """
    Save buffered output to a structured file under base_dir/native-search/.
    """
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "native-search"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    root_name = root.name or "root"
    # sanitize patterns for filename
    pat_part = "_".join(p.replace(" ", "_") for p in patterns)
    if not pat_part:
        pat_part = "patterns"

    fname = f"{root_name}_{pat_part}_{timestamp}.txt"
    path = sub / fname

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Output written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Search Minecraft native jars for byte (ASCII) patterns.",
    )
    parser.add_argument(
        "--javap",
        nargs="?",
        const="__MATCHES__",
        metavar="CLASS",
        help=(
            "Run javap. Without CLASS, dumps every matching class; "
            "with CLASS, dumps that specific type."
        ),
    )
    parser.add_argument(
        "--javap-opts",
        default="-c",
        help='Options to pass to javap (default: "-c"). Use empty string to disable options.',
    )
    parser.add_argument(
        "--case-sensitive",
        action="store_true",
        help="Treat patterns as case-sensitive (default: case-insensitive).",
    )
    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save full output to a file. Default directory: agent-tools/",
    )
    parser.add_argument(
        "root",
        type=Path,
        help="Directory or jar containing classes to scan.",
    )
    parser.add_argument(
        "patterns",
        nargs="+",
        help="ASCII patterns to search for (e.g. method names).",
    )

    args = parser.parse_args()

    # Output capture
    output_buffer: list[str] = []

    def out(*a):
        msg = " ".join(str(x) for x in a)
        output_buffer.append(msg)
        print(msg)

    root = args.root
    if not root.exists():
        out(f"[err] {root} does not exist")
        if args.save:
            save_output(Path(args.save), root, args.patterns, output_buffer, out)
        sys.exit(1)

    needles = [pat.encode("utf-8") for pat in args.patterns]
    javap_opts = shlex.split(args.javap_opts) if args.javap_opts else []
    per_match_disassembly = args.javap == "__MATCHES__"
    specific_class = None if not args.javap or per_match_disassembly else args.javap

    out(f"Searching {root} for {', '.join(args.patterns)}...")
    jar_paths = list(find_jars(root))
    if not jar_paths:
        out("No jar files found under the specified root.")
        if args.save:
            save_output(Path(args.save), root, args.patterns, output_buffer, out)
        sys.exit(1)

    jar_count = 0
    total = 0

    for jar_path in jar_paths:
        jar_count += 1
        hits = search_jar(jar_path, needles, case_sensitive=args.case_sensitive)
        if not hits:
            continue

        for entry_name, needle_str in hits:
            out(f"{jar_path}:{entry_name} -> {needle_str}")
            total += 1
            if per_match_disassembly:
                disassemble_with_javap(
                    jar_path,
                    entry_to_class(entry_name),
                    javap_opts,
                    out,
                )

    summary = f"Scanned {jar_count} jar(s). Matches: {total}."
    out(summary)
    if total == 0:
        joined = ", ".join(args.patterns)
        out(f"No matches found for: {joined}")

    # If a specific class was requested for javap, locate it in scanned jars.
    if specific_class:
        target_jar = None
        if len(jar_paths) == 1:
            target_jar = jar_paths[0]
        else:
            for jar_path in jar_paths:
                if class_exists_in_jar(jar_path, specific_class):
                    target_jar = jar_path
                    break
        if target_jar is None:
            out(f"[javap] Could not locate {specific_class} in scanned jars.")
        else:
            disassemble_with_javap(target_jar, specific_class, javap_opts, out)

    # Save output if requested
    if args.save:
        save_output(Path(args.save), root, args.patterns, output_buffer, out)


if __name__ == "__main__":
    main()
