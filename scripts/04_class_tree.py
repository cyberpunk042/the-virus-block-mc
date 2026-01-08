#!/usr/bin/env python3
"""
class_tree.py

Build a class inheritance tree from .class files inside Minecraft / mod jars.

Features:
    - Recursively scans a directory for .jar files (or accepts a single .jar).
    - Parses .class files directly (no javap needed).
    - Extracts: this_class, super_class, and implemented interfaces.
    - Builds a class hierarchy:
        - class -> super
        - super -> children
    - Optional --root-class to show a subtree only.
    - Optional --prefix to filter printed classes.
    - Optional --save to write output to agent-tools/hierarchy/ by default.

Examples:
    # Build tree from all jars under .gradle/loom-cache
    python3 scripts/class_tree.py .gradle/loom-cache

    # Show only subtree under net.minecraft.entity.LivingEntity
    python3 scripts/class_tree.py .gradle/loom-cache --root-class net.minecraft.entity.LivingEntity

    # Restrict printed classes to your mod package
    python3 scripts/class_tree.py build/libs --prefix net.cyberpunk042.mymod

    # Save output to agent-tools/hierarchy/
    python3 scripts/class_tree.py /mnt/f/minecraftNativeJars --save

    # Save output to a custom base directory
    python3 scripts/class_tree.py /mnt/f/minecraftNativeJars --save my-agent-tools
"""

from __future__ import annotations

import argparse
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from zipfile import ZipFile, BadZipFile


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class ClassInfo:
    name: str
    super_name: Optional[str]
    interfaces: List[str]


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
# Class file parsing (just enough for name / super / interfaces)
# ---------------------------------------------------------------------------

class ClassFileParser:
    """
    Minimal parser for Java .class files to extract:
      - this class name
      - super class name
      - implemented interfaces

    It only reads up to the interfaces table and constant pool relevant to names.
    """

    TAG_UTF8 = 1
    TAG_INTEGER = 3
    TAG_FLOAT = 4
    TAG_LONG = 5
    TAG_DOUBLE = 6
    TAG_CLASS = 7
    TAG_STRING = 8
    TAG_FIELDREF = 9
    TAG_METHODREF = 10
    TAG_INTERFACE_METHODREF = 11
    TAG_NAME_AND_TYPE = 12
    TAG_METHOD_HANDLE = 15
    TAG_METHOD_TYPE = 16
    TAG_INVOKE_DYNAMIC = 18

    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0
        self.constant_pool: Dict[int, Tuple[str, object]] = {}

    def read_u1(self) -> int:
        val = self.data[self.pos]
        self.pos += 1
        return val

    def read_u2(self) -> int:
        val = int.from_bytes(self.data[self.pos:self.pos + 2], "big")
        self.pos += 2
        return val

    def read_u4(self) -> int:
        val = int.from_bytes(self.data[self.pos:self.pos + 4], "big")
        self.pos += 4
        return val

    def read_bytes(self, n: int) -> bytes:
        chunk = self.data[self.pos:self.pos + n]
        self.pos += n
        return chunk

    def parse(self) -> ClassInfo:
        # magic
        magic = self.read_u4()
        if magic != 0xCAFEBABE:
            raise ValueError("Not a valid .class file (bad magic)")

        # minor_version, major_version
        _minor = self.read_u2()
        _major = self.read_u2()

        # constant_pool_count
        cp_count = self.read_u2()
        # constant pool index goes 1..cp_count-1
        self.constant_pool = {}

        i = 1
        while i < cp_count:
            tag = self.read_u1()

            if tag == self.TAG_UTF8:
                length = self.read_u2()
                bytes_ = self.read_bytes(length)
                try:
                    text = bytes_.decode("utf-8")
                except UnicodeDecodeError:
                    text = ""
                self.constant_pool[i] = ("Utf8", text)

            elif tag in (self.TAG_INTEGER, self.TAG_FLOAT):
                # 4 bytes
                _ = self.read_u4()
                self.constant_pool[i] = ("Num", None)

            elif tag in (self.TAG_LONG, self.TAG_DOUBLE):
                # 8 bytes, occupies two entries
                _high = self.read_u4()
                _low = self.read_u4()
                self.constant_pool[i] = ("Num2", None)
                i += 1  # extra slot (per JVM spec)

            elif tag == self.TAG_CLASS:
                name_index = self.read_u2()
                self.constant_pool[i] = ("Class", name_index)

            elif tag == self.TAG_STRING:
                string_index = self.read_u2()
                self.constant_pool[i] = ("String", string_index)

            elif tag in (self.TAG_FIELDREF, self.TAG_METHODREF, self.TAG_INTERFACE_METHODREF):
                # class_index, name_and_type_index
                _class_index = self.read_u2()
                _name_type_index = self.read_u2()
                self.constant_pool[i] = ("Ref", None)

            elif tag == self.TAG_NAME_AND_TYPE:
                _name_index = self.read_u2()
                _desc_index = self.read_u2()
                self.constant_pool[i] = ("NameType", None)

            elif tag == self.TAG_METHOD_HANDLE:
                _ref_kind = self.read_u1()
                _ref_index = self.read_u2()
                self.constant_pool[i] = ("MethodHandle", None)

            elif tag == self.TAG_METHOD_TYPE:
                _desc_index = self.read_u2()
                self.constant_pool[i] = ("MethodType", None)

            elif tag == self.TAG_INVOKE_DYNAMIC:
                _bsm_index = self.read_u2()
                _name_type_index = self.read_u2()
                self.constant_pool[i] = ("InvokeDynamic", None)

            else:
                # Unknown tag; this should not happen in valid class files
                # but we defensively bail
                raise ValueError(f"Unknown constant pool tag: {tag}")

            i += 1

        # access_flags
        _access_flags = self.read_u2()
        this_class_index = self.read_u2()
        super_class_index = self.read_u2()
        interfaces_count = self.read_u2()
        interfaces_indices = [self.read_u2() for _ in range(interfaces_count)]

        this_name = self._resolve_class_name(this_class_index)
        super_name = self._resolve_class_name(super_class_index) if super_class_index != 0 else None
        interfaces = [self._resolve_class_name(idx) for idx in interfaces_indices if idx != 0]

        return ClassInfo(
            name=this_name,
            super_name=super_name,
            interfaces=interfaces,
        )

    def _resolve_class_name(self, class_index: int) -> str:
        entry = self.constant_pool.get(class_index)
        if not entry:
            return f"<unknown_{class_index}>"
        tag, val = entry
        if tag != "Class":
            return f"<nonclass_{class_index}>"
        name_index = val
        utf_entry = self.constant_pool.get(name_index)
        if not utf_entry or utf_entry[0] != "Utf8":
            return f"<nonutf8_{name_index}>"
        name = utf_entry[1]
        # Convert internal name (pkg/Name) to dotted form
        return name.replace("/", ".")


# ---------------------------------------------------------------------------
# Jar / class scanning
# ---------------------------------------------------------------------------

def find_jars(root: Path):
    """Yield all .jar paths under root (or root itself if it's a .jar)."""
    if root.is_file():
        if root.suffix == ".jar":
            yield root
        return
    for path in root.rglob("*.jar"):
        if path.is_file():
            yield path


def parse_classes_from_jar(jar_path: Path, out) -> Dict[str, ClassInfo]:
    """Parse all .class entries in a jar and return a mapping name -> ClassInfo."""
    mapping: Dict[str, ClassInfo] = {}
    try:
        with ZipFile(jar_path) as jar:
            for entry in jar.infolist():
                if entry.is_dir() or not entry.filename.endswith(".class"):
                    continue
                try:
                    data = jar.read(entry)
                    parser = ClassFileParser(data)
                    info = parser.parse()
                    mapping[info.name] = info
                except Exception:
                    # We can log or ignore; for now, ignore single-class failures
                    continue
    except BadZipFile:
        out(f"[warn] Skipping unreadable jar: {jar_path}")
    return mapping


# ---------------------------------------------------------------------------
# Build tree + printing
# ---------------------------------------------------------------------------

def build_hierarchy(class_map: Dict[str, ClassInfo]) -> Tuple[Dict[str, List[str]], List[str]]:
    """
    Build children mapping: parent -> [children],
    and compute "root" classes (those whose super is None or unknown).
    """
    children: Dict[str, List[str]] = defaultdict(list)
    known_classes = set(class_map.keys())

    for cls_name, info in class_map.items():
        if info.super_name and info.super_name in known_classes:
            children[info.super_name].append(cls_name)
        else:
            # no known super -> later considered root
            pass

    # roots: any class whose super is None or super not in known_classes
    roots: List[str] = []
    for cls_name, info in class_map.items():
        if not info.super_name or info.super_name not in known_classes:
            roots.append(cls_name)

    return children, roots


def print_subtree(
    root: str,
    class_map: Dict[str, ClassInfo],
    children_map: Dict[str, List[str]],
    out,
    prefix_filter: Optional[str] = None,
    indent: str = "",
    visited: Optional[set] = None,
):
    """
    Recursively print a subtree starting at 'root'.
    """
    if visited is None:
        visited = set()
    if root in visited:
        out(indent + root + " (cycle?)")
        return
    visited.add(root)

    if prefix_filter and not root.startswith(prefix_filter):
        # Still traverse children because they might match,
        # but we don't print this class itself.
        pass
    else:
        info = class_map.get(root)
        if info:
            label = root
            if info.super_name:
                label += f"  [extends {info.super_name}]"
            if info.interfaces:
                label += "  [implements " + ", ".join(info.interfaces) + "]"
            out(indent + label)
        else:
            out(indent + root)

    for child in sorted(children_map.get(root, [])):
        print_subtree(
            child,
            class_map,
            children_map,
            out,
            prefix_filter=prefix_filter,
            indent=indent + "  ",
            visited=visited,
        )


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(base_dir: Path, label: str, buffer: List[str], out) -> None:
    """
    Save buffered output to base_dir/hierarchy/<label>_<timestamp>.txt
    """
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "hierarchy"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    lab = label.replace(" ", "_").replace("/", "_").replace(".", "_")
    filename = f"{lab}_{timestamp}.txt"
    path = sub / filename

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Class tree written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build and print a class inheritance tree from jars."
    )
    parser.add_argument(
        "root",
        type=Path,
        help="Directory containing jars, or a single jar file.",
    )
    parser.add_argument(
        "--root-class",
        help="Limit output to the subtree starting from this fully-qualified class name.",
    )
    parser.add_argument(
        "--prefix",
        help="Only print classes whose names start with this prefix (useful for your mod package).",
    )
    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save output to files under this base directory (default: agent-tools).",
    )
    parser.add_argument(
        "--limit-jars",
        type=int,
        help="Limit the number of jars processed (for quick tests).",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    out, buffer = make_output_collector()

    root: Path = args.root
    if not root.exists():
        out(f"[err] Root path does not exist: {root}")
        if args.save:
            save_output(Path(args.save), "class_tree_error", buffer, out)
        return

    jars = list(find_jars(root))
    if args.limit_jars is not None:
        jars = jars[: args.limit_jars]

    if not jars:
        out(f"[err] No .jar files found under: {root}")
        if args.save:
            save_output(Path(args.save), "class_tree_empty", buffer, out)
        return

    out(f"[info] Found {len(jars)} jar(s) under {root}")

    # Parse classes from jars
    class_map: Dict[str, ClassInfo] = {}
    for jar_path in jars:
        out(f"[info] Parsing classes from {jar_path}")
        parsed = parse_classes_from_jar(jar_path, out)
        class_map.update(parsed)

    out(f"[info] Parsed {len(class_map)} classes.")

    if not class_map:
        out("[info] No classes parsed. Nothing to do.")
        if args.save:
            save_output(Path(args.save), "class_tree_none", buffer, out)
        return

    children_map, roots = build_hierarchy(class_map)

    # If root-class is specified, focus on that subtree
    if args.root_class:
        root_class = args.root_class
        if root_class not in class_map:
            out(f"[err] Root class not found in parsed classes: {root_class}")
            out("[info] Available example classes (first 10):")
            for name in list(sorted(class_map.keys()))[:10]:
                out(f"  {name}")
        else:
            out(f"[info] Printing subtree starting at: {root_class}")
            print_subtree(
                root_class,
                class_map,
                children_map,
                out,
                prefix_filter=args.prefix,
            )
    else:
        out("[info] Printing full class tree (roots only, with subtrees):")
        # Sort roots for deterministic output
        for root_name in sorted(roots):
            print_subtree(
                root_name,
                class_map,
                children_map,
                out,
                prefix_filter=args.prefix,
            )

    if args.save:
        label = args.root_class or "full"
        save_output(Path(args.save), label, buffer, out)


if __name__ == "__main__":
    main()
