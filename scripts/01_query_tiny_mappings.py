#!/usr/bin/env python3
"""
Query Fabric tiny mappings for class/method info.
Now supports:
    --save [DIR]
        Saves printed output to a structured file.
        Default directory: agent-tools/
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Optional
from zipfile import BadZipFile, ZipFile
from datetime import datetime


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class MethodEntry:
    descriptor: str
    named: str
    official: str
    intermediary: str


@dataclass
class ClassEntry:
    named: str
    official: str
    intermediary: str
    methods: list[MethodEntry] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Tiny mappings parsing
# ---------------------------------------------------------------------------

def parse_tiny(path: Path) -> list[ClassEntry]:
    classes = []
    current = None

    with path.open("r", encoding="utf-8") as fh:
        for raw in fh:
            line = raw.rstrip("\n")
            if not line:
                continue

            stripped = line.lstrip("\t")
            if not stripped or stripped.startswith("#"):
                continue

            parts = stripped.split("\t")
            if len(parts) < 2:
                continue

            tag, payload = parts[0], parts[1:]

            if tag == "c":
                if len(payload) >= 3:
                    try:
                        current = ClassEntry(payload[0], payload[1], payload[2])
                        classes.append(current)
                    except Exception:
                        current = None
                        continue

            elif tag == "m" and current is not None:
                if len(payload) >= 4:
                    try:
                        current.methods.append(MethodEntry(
                            payload[0], payload[1], payload[2], payload[3]
                        ))
                    except Exception:
                        continue

    return classes


# ---------------------------------------------------------------------------
# Search helpers
# ---------------------------------------------------------------------------

def find_classes(classes: Iterable[ClassEntry], needle: str) -> list[ClassEntry]:
    needle_lower = needle.lower()
    return [
        c for c in classes
        if needle_lower in c.named.lower()
        or needle_lower in c.official.lower()
        or needle_lower in c.intermediary.lower()
    ]


def find_methods(methods: Iterable[MethodEntry], needle: Optional[str]) -> list[MethodEntry]:
    if not needle:
        return list(methods)

    needle_lower = needle.lower()
    return [
        m for m in methods
        if needle_lower in m.named.lower()
        or needle_lower in m.official.lower()
        or needle_lower in m.intermediary.lower()
    ]


def find_tiny_files(root: Path) -> list[Path]:
    if root.is_file() and root.suffix == ".tiny":
        return [root]
    if not root.exists():
        return []
    return sorted(root.rglob("*.tiny"))


def find_jars(root: Path) -> list[Path]:
    if root.is_file() and root.suffix == ".jar":
        return [root]
    if not root.exists():
        return []
    return sorted(root.rglob("*.jar"))


# ---------------------------------------------------------------------------
# Jar search + javap
# ---------------------------------------------------------------------------

def search_jars_for_class(jar_paths: list[Path], class_needle: str) -> Optional[tuple[Path, str]]:
    class_needle_lower = class_needle.lower()
    simple_name = class_needle.split(".")[-1].lower()

    exact = []
    partial = []

    for jar_path in jar_paths:
        try:
            with ZipFile(jar_path) as jar:
                for entry_name in jar.namelist():
                    if not entry_name.endswith(".class"):
                        continue

                    class_name = entry_name[:-6].replace("/", ".")
                    cl = class_name.lower()
                    sl = class_name.split(".")[-1].lower()

                    if cl == class_needle_lower or sl == simple_name:
                        exact.append((jar_path, class_name))
                    elif class_needle_lower in cl or simple_name in cl:
                        if "render" not in cl and "model" not in cl:
                            partial.append((jar_path, class_name))

        except BadZipFile:
            continue

    if exact:
        return exact[0]
    if partial:
        return partial[0]
    return None


def run_javap(jar_path: Path, class_name: str, method_name: Optional[str], out):
    cmd = ["javap", "-classpath", str(jar_path)]
    if method_name:
        cmd += ["-c", "-p"]
    else:
        cmd.append("-c")

    cmd.append(class_name)

    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
    except FileNotFoundError:
        out("[javap] Missing javap (install JDK).")
        return

    if not method_name:
        out(result.stdout)
        if result.stderr:
            out(result.stderr)
        return

    lines = result.stdout.splitlines()
    method_lower = method_name.lower()

    collected = []
    for i, line in enumerate(lines):
        s = line.strip()
        if (
            method_lower in s.lower()
            and (s.startswith("public") or s.startswith("private") or s.startswith("protected"))
        ):
            collected.append(line)
            for j in range(i + 1, min(i + 40, len(lines))):
                collected.append(lines[j])
                sj = lines[j].strip()
                if sj == "}" or (
                    (sj.startswith("public") or sj.startswith("private") or sj.startswith("protected"))
                    and j > i + 1
                ):
                    break
            break

    if collected:
        out("\n".join(collected))
    else:
        out(f"[javap] Could not isolate method '{method_name}'. Showing fallback:")
        for line in lines:
            if method_lower in line.lower():
                out(line)

    if result.stderr:
        out(result.stderr)


# ---------------------------------------------------------------------------
# CLI processing
# ---------------------------------------------------------------------------

def resolve_tiny_files(mapping_file: Optional[Path], mapping_dir: Path):
    if mapping_file:
        if not mapping_file.exists():
            raise SystemExit(f"[ERR] Mapping file not found: {mapping_file}")
        return [mapping_file]

    if mapping_dir.exists():
        files = find_tiny_files(mapping_dir)
        if files:
            return files

    fallback = Path(".gradle/loom-cache/source_mappings/077c1ca8a67f63c999b4b3c5adee19230b53302c.tiny")
    if fallback.exists():
        return [fallback]

    return []


def main():
    # -----------------------------------------------------------------------
    # Set up CLI
    # -----------------------------------------------------------------------

    parser = argparse.ArgumentParser(description="Query Fabric tiny mappings.")
    parser.add_argument("--mapping-file", type=Path)
    parser.add_argument("--mapping-dir", type=Path, default=Path(".gradle/loom-cache/source_mappings"))
    # Class can be positional OR --class flag for flexibility
    parser.add_argument("class_pattern", nargs="?", help="Class name to search (positional)")
    parser.add_argument("--class", dest="class_flag", help="Class name to search (flag form)")
    parser.add_argument("--method", dest="method_name")
    parser.add_argument("--max-methods", type=int, default=50)
    parser.add_argument("--search-jars", action="store_true")
    parser.add_argument("--jar-dir", type=Path)
    parser.add_argument("--javap", action="store_true")

    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save output to a file. Default directory: agent-tools/",
    )

    args = parser.parse_args()

    # Resolve class name from positional or flag
    args.class_name = args.class_pattern or args.class_flag
    if not args.class_name:
        parser.error("Class name required: provide as positional arg or --class flag\n"
                     "  Examples:\n"
                     "    python 01_query_tiny_mappings.py SliderWidget\n"
                     "    python 01_query_tiny_mappings.py --class SliderWidget")

    # Output capture
    output_buffer: list[str] = []

    def out(*a):
        msg = " ".join(str(x) for x in a)
        output_buffer.append(msg)
        print(msg)

    # -----------------------------------------------------------------------
    # Load mappings
    # -----------------------------------------------------------------------

    tiny_files = resolve_tiny_files(args.mapping_file, args.mapping_dir)

    classes_parsed = []
    for tf in tiny_files:
        try:
            classes_parsed.extend(parse_tiny(tf))
        except Exception as e:
            out(f"[WARN] Failed to parse {tf}: {e}")

    # -----------------------------------------------------------------------
    # Search class in mappings
    # -----------------------------------------------------------------------

    patterns = [
        args.class_name,
        args.class_name.split(".")[-1],
        f"net.minecraft.{args.class_name}" if "." not in args.class_name else None,
    ]
    patterns = [p for p in patterns if p]

    found_classes = []
    for pattern in patterns:
        res = find_classes(classes_parsed, pattern)
        if res:
            found_classes.extend(res)
            break

    # -----------------------------------------------------------------------
    # Fallback: search jars
    # -----------------------------------------------------------------------

    if not found_classes and args.search_jars:
        jar_dir = args.jar_dir or Path(".gradle/loom-cache")
        if not jar_dir.exists():
            jar_dir = Path("/mnt/f/minecraftNativeJars")

        if not jar_dir.exists():
            out(f"[ERR] Jar directory not found: {jar_dir}")
            sys.exit(1)

        jar_paths = find_jars(jar_dir)

        out(f"Searching {len(jar_paths)} jars for '{args.class_name}' ...")
        r = search_jars_for_class(jar_paths, args.class_name)
        if not r:
            out(f"[ERR] No match in jars for: {args.class_name}")
        else:
            jar_path, class_name = r
            out(f"Found class: {class_name}")
            out(f"In jar: {jar_path}")

            if args.javap:
                out("\n[javap]")
                run_javap(jar_path, class_name, args.method_name, out)

        # after jar search, save if needed
        if args.save:
            save_output(args, output_buffer, out)
        return

    if not found_classes:
        out(f"No mapping entry for: {args.class_name}")
        out(f"Tried: {', '.join(patterns)}")
        if not args.search_jars:
            out("(Tip: Use --search-jars)")
        if args.save:
            save_output(args, output_buffer, out)
        return

    # -----------------------------------------------------------------------
    # Print results
    # -----------------------------------------------------------------------

    for entry in found_classes:
        out(f"Class: {entry.named}")
        out(f"  Official:     {entry.official}")
        out(f"  Intermediary: {entry.intermediary}")

        methods_list = find_methods(entry.methods, args.method_name)

        if not methods_list:
            out("  (no methods matched)")
            out("")
            continue

        limit = args.max_methods
        for i, m in enumerate(methods_list):
            if limit and i >= limit:
                out(f"    ... {len(methods_list)-i} more methods")
                break

            out(f"    {m.named} {m.descriptor}")
            out(f"      Official:     {m.official}")
            out(f"      Intermediary: {m.intermediary}")

        out("")

        # -------------------------------------------------------------------
        # javap on mapped class
        # -------------------------------------------------------------------
        if args.javap:
            jar_dir = args.jar_dir or Path(".gradle/loom-cache")
            if not jar_dir.exists():
                jar_dir = Path("/mnt/f/minecraftNativeJars")

            jar_paths = find_jars(jar_dir)
            r = search_jars_for_class(jar_paths, entry.named)

            if r:
                jar_path, class_name = r
                out(f"[javap] {jar_path} :: {class_name}")
                run_javap(jar_path, class_name, args.method_name, out)
            else:
                out(f"[javap] Could not locate class in jars: {entry.named}")

    # -----------------------------------------------------------------------
    # Save output to file
    # -----------------------------------------------------------------------

    if args.save:
        save_output(args, output_buffer, out)


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(args, buffer, out):
    base = Path(args.save)
    base.mkdir(parents=True, exist_ok=True)

    sub = base / "mappings"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    cls = args.class_name.replace(".", "_")
    meth = args.method_name.replace(".", "_") if args.method_name else "all"

    path = sub / f"{cls}_{meth}_{timestamp}.txt"

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Output written to: {path}")


if __name__ == "__main__":
    main()
