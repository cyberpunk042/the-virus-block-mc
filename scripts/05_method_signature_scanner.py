#!/usr/bin/env python3
"""
method_signature_scanner.py

Scan .class files in jars and list methods matching desired signatures.

Use it to answer questions like:
    - "Show me all methods that return boolean."
    - "Show me all methods that take BlockPos."
    - "Show me all methods named tick that return void."
    - "Show me all public static methods whose name contains 'heal'."

Features:
    - Recursively scans a directory for .jar files (or a single .jar).
    - Parses .class files directly (no javap required).
    - Extracts:
        * class name
        * method name
        * descriptor
        * access flags (for public/static filters)
    - Filters:
        * --name-contains: substring in method name
        * --return-type: human or descriptor form (e.g. boolean, void, int, net.minecraft.util.math.BlockPos)
        * --param-type: same as return type, can be specified multiple times
        * --only-public
        * --only-static
        * --limit: cap total results
    - Output:
        ClassName#methodName descriptor [flags]
    - --save: write full output to agent-tools/signatures/ (or a custom base dir)

Examples:
    # All methods whose name contains 'tick'
    python3 scripts/method_signature_scanner.py .gradle/loom-cache --name-contains tick

    # All methods returning boolean
    python3 scripts/method_signature_scanner.py /mnt/f/minecraftNativeJars --return-type boolean

    # All methods taking BlockPos
    python3 scripts/method_signature_scanner.py /mnt/f/minecraftNativeJars --param-type net.minecraft.util.math.BlockPos

    # All public static methods named create
    python3 scripts/method_signature_scanner.py build/libs --name-contains create --only-public --only-static

    # Save output
    python3 scripts/method_signature_scanner.py /mnt/f/minecraftNativeJars --return-type boolean --save

"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from zipfile import ZipFile, BadZipFile


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class MethodInfo:
    class_name: str
    name: str
    descriptor: str
    access_flags: int


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
# Minimal .class parser (constant pool + methods)
# ---------------------------------------------------------------------------

class ClassFileParser:
    """
    Minimal parser for Java .class files to extract:
      - this class name
      - method name / descriptor / access flags

    Only parses enough of the structure to reach method_info.
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

    # basic readers

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

    # parser

    def parse(self) -> Tuple[str, List[MethodInfo]]:
        # magic
        magic = self.read_u4()
        if magic != 0xCAFEBABE:
            raise ValueError("Not a valid .class file (bad magic)")

        _minor = self.read_u2()
        _major = self.read_u2()

        cp_count = self.read_u2()
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
                _ = self.read_u4()
                self.constant_pool[i] = ("Num", None)

            elif tag in (self.TAG_LONG, self.TAG_DOUBLE):
                _high = self.read_u4()
                _low = self.read_u4()
                self.constant_pool[i] = ("Num2", None)
                i += 1  # long/double occupy 2 entries

            elif tag == self.TAG_CLASS:
                name_index = self.read_u2()
                self.constant_pool[i] = ("Class", name_index)

            elif tag == self.TAG_STRING:
                string_index = self.read_u2()
                self.constant_pool[i] = ("String", string_index)

            elif tag in (self.TAG_FIELDREF, self.TAG_METHODREF, self.TAG_INTERFACE_METHODREF):
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
                raise ValueError(f"Unknown constant pool tag: {tag}")

            i += 1

        # access_flags, this_class, super_class, interfaces
        _access_flags = self.read_u2()
        this_class_index = self.read_u2()
        _super_class_index = self.read_u2()
        interfaces_count = self.read_u2()
        for _ in range(interfaces_count):
            _ = self.read_u2()

        # fields_count + skip fields
        fields_count = self.read_u2()
        for _ in range(fields_count):
            self._skip_member_info()

        # methods_count + parse methods
        methods_count = self.read_u2()
        methods: List[MethodInfo] = []
        class_name = self._resolve_class_name(this_class_index)

        for _ in range(methods_count):
            access_flags = self.read_u2()
            name_index = self.read_u2()
            desc_index = self.read_u2()
            attributes_count = self.read_u2()

            name = self._resolve_utf8(name_index)
            desc = self._resolve_utf8(desc_index)

            # skip attributes
            for _ in range(attributes_count):
                _attr_name_index = self.read_u2()
                attr_length = self.read_u4()
                self.read_bytes(attr_length)

            methods.append(MethodInfo(
                class_name=class_name,
                name=name,
                descriptor=desc,
                access_flags=access_flags,
            ))

        return class_name, methods

    def _skip_member_info(self) -> None:
        """
        Skip a field_info or method_info structure (used for fields).
        """
        _access_flags = self.read_u2()
        _name_index = self.read_u2()
        _desc_index = self.read_u2()
        attributes_count = self.read_u2()
        for _ in range(attributes_count):
            _attr_name_index = self.read_u2()
            attr_length = self.read_u4()
            self.read_bytes(attr_length)

    def _resolve_utf8(self, index: int) -> str:
        entry = self.constant_pool.get(index)
        if not entry:
            return f"<utf8_{index}_missing>"
        if entry[0] != "Utf8":
            return f"<utf8_{index}_wrong_type>"
        return entry[1]

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


def extract_methods_from_jar(jar_path: Path, out) -> List[MethodInfo]:
    methods: List[MethodInfo] = []
    try:
        with ZipFile(jar_path) as jar:
            for entry in jar.infolist():
                if entry.is_dir() or not entry.filename.endswith(".class"):
                    continue
                try:
                    data = jar.read(entry)
                    parser = ClassFileParser(data)
                    _class_name, m_list = parser.parse()
                    methods.extend(m_list)
                except Exception:
                    # ignore broken / unexpected class structures
                    continue
    except BadZipFile:
        out(f"[warn] Skipping unreadable jar: {jar_path}")
    return methods


# ---------------------------------------------------------------------------
# Descriptor helpers
# ---------------------------------------------------------------------------

PRIMITIVE_MAP = {
    "void": "V",
    "boolean": "Z",
    "byte": "B",
    "char": "C",
    "short": "S",
    "int": "I",
    "long": "J",
    "float": "F",
    "double": "D",
}

def type_to_descriptor(type_str: str) -> str:
    """
    Convert a human-friendly type to a JVM descriptor fragment.

    Examples:
        boolean                 -> Z
        int                     -> I
        void                    -> V
        net.minecraft.util.Foo  -> Lnet/minecraft/util/Foo;
        net.minecraft.util.Foo[] -> [Lnet/minecraft/util/Foo;
    """
    t = type_str.strip()
    # primitive
    if t in PRIMITIVE_MAP:
        return PRIMITIVE_MAP[t]

    # array handling: count [] suffixes
    array_dim = 0
    while t.endswith("[]"):
        array_dim += 1
        t = t[:-2].strip()

    # class name: assume dotted, convert to slashes
    internal = t.replace(".", "/")
    base = f"L{internal};"
    return "[" * array_dim + base


def split_descriptor(desc: str) -> Tuple[str, str]:
    """
    Split a JVM method descriptor into (param_part, return_part).

    Example:
        (IILnet/minecraft/Foo;)Z  -> ("IILnet/minecraft/Foo;", "Z")
    """
    if not desc or desc[0] != "(":
        return "", ""
    try:
        close = desc.index(")")
    except ValueError:
        return "", ""
    return desc[1:close], desc[close + 1:]


def access_is_public(flags: int) -> bool:
    return bool(flags & 0x0001)


def access_is_static(flags: int) -> bool:
    return bool(flags & 0x0008)


# ---------------------------------------------------------------------------
# Filtering
# ---------------------------------------------------------------------------

def method_matches(
    m: MethodInfo,
    name_contains: Optional[str],
    return_desc: Optional[str],
    param_descs: List[str],
    only_public: bool,
    only_static: bool,
) -> bool:
    if only_public and not access_is_public(m.access_flags):
        return False
    if only_static and not access_is_static(m.access_flags):
        return False

    if name_contains:
        if name_contains.lower() not in m.name.lower():
            return False

    params_part, ret_part = split_descriptor(m.descriptor)

    if return_desc:
        if return_desc not in ret_part:
            return False

    for p in param_descs:
        if p not in params_part:
            return False

    return True


def format_flags(flags: int) -> str:
    parts = []
    if access_is_public(flags):
        parts.append("public")
    if flags & 0x0002:
        parts.append("private")
    if flags & 0x0004:
        parts.append("protected")
    if access_is_static(flags):
        parts.append("static")
    if flags & 0x0010:
        parts.append("final")
    if flags & 0x0020:
        parts.append("synchronized")
    if flags & 0x0100:
        parts.append("native")
    if flags & 0x0400:
        parts.append("abstract")
    if not parts:
        return "-"
    return " ".join(parts)


# ---------------------------------------------------------------------------
# Save support
# ---------------------------------------------------------------------------

def save_output(base_dir: Path, label: str, buffer: List[str], out) -> None:
    """
    Save buffered output to base_dir/signatures/<label>_<timestamp>.txt
    """
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "signatures"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    lab = label.replace(" ", "_").replace("/", "_").replace(".", "_")
    filename = f"{lab}_{timestamp}.txt"
    path = sub / filename

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Method signatures written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scan jars for methods matching names / return types / parameters."
    )
    parser.add_argument(
        "root",
        type=Path,
        help="Directory containing jars, or a single jar.",
    )
    parser.add_argument(
        "--name-contains",
        help="Substring to match in method names (case-insensitive).",
    )
    parser.add_argument(
        "--return-type",
        help="Required return type (human or descriptor). E.g. boolean, void, int, net.minecraft.util.Foo",
    )
    parser.add_argument(
        "--param-type",
        action="append",
        default=None,
        help="Required parameter type (can be repeated). Same format as --return-type.",
    )
    parser.add_argument(
        "--only-public",
        action="store_true",
        help="Only include public methods.",
    )
    parser.add_argument(
        "--only-static",
        action="store_true",
        help="Only include static methods.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=200,
        help="Maximum number of results to print (0 = unlimited).",
    )
    parser.add_argument(
        "--save",
        nargs="?",
        const="agent-tools",
        help="Save output under this base directory (default: agent-tools).",
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
            save_output(Path(args.save), "method_signatures_error", buffer, out)
        return

    jars = list(find_jars(root))
    if args.limit_jars is not None:
        jars = jars[: args.limit_jars]

    if not jars:
        out(f"[err] No .jar files found under: {root}")
        if args.save:
            save_output(Path(args.save), "method_signatures_empty", buffer, out)
        return

    out(f"[info] Found {len(jars)} jar(s) under {root}")

    # Convert type filters to descriptor fragments
    return_desc = type_to_descriptor(args.return_type) if args.return_type else None
    param_descs: List[str] = []
    if args.param_type:
        for t in args.param_type:
            param_descs.append(type_to_descriptor(t))

    name_contains = args.name_contains.lower() if args.name_contains else None

    out("[info] Filters:")
    out(f"  name_contains = {name_contains}")
    out(f"  return_type   = {args.return_type} -> {return_desc}" if args.return_type else "  return_type   = (none)")
    if param_descs:
        out("  param_types   =")
        for raw, d in zip(args.param_type, param_descs):
            out(f"    {raw} -> {d}")
    else:
        out("  param_types   = (none)")
    out(f"  only_public   = {args.only_public}")
    out(f"  only_static   = {args.only_static}")
    out(f"  limit         = {args.limit if args.limit else 'unlimited'}")

    all_methods: List[MethodInfo] = []
    for jar_path in jars:
        out(f"[info] Extracting methods from {jar_path}")
        methods = extract_methods_from_jar(jar_path, out)
        all_methods.extend(methods)

    out(f"[info] Parsed {len(all_methods)} methods total.")

    # Filter
    matched: List[MethodInfo] = []
    for m in all_methods:
        if method_matches(
            m,
            name_contains=name_contains,
            return_desc=return_desc,
            param_descs=param_descs,
            only_public=args.only_public,
            only_static=args.only_static,
        ):
            matched.append(m)

    if not matched:
        out("[info] No methods matched the given filters.")
        if args.save:
            label = "none"
            save_output(Path(args.save), label, buffer, out)
        return

    out("")
    out(f"[info] Matched {len(matched)} method(s).")

    # Sort results for deterministic output
    matched.sort(key=lambda m: (m.class_name, m.name, m.descriptor))

    count = 0
    for m in matched:
        flags_str = format_flags(m.access_flags)
        out(f"{m.class_name}#{m.name} {m.descriptor}  [{flags_str}]")

        count += 1
        if args.limit and count >= args.limit:
            remaining = len(matched) - count
            if remaining > 0:
                out(f"... {remaining} more result(s) omitted (raise --limit to see more)")
            break

    if args.save:
        label_parts = []
        if args.name_contains:
            label_parts.append(args.name_contains)
        if args.return_type:
            label_parts.append(args.return_type)
        if args.param_type:
            label_parts.extend(args.param_type)
        label = "_".join(label_parts) if label_parts else "all"
        save_output(Path(args.save), label, buffer, out)


if __name__ == "__main__":
    main()
