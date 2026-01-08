#!/usr/bin/env python3
"""
config_schema_extractor.py

Unified tool to:

1) Extract configuration metadata from annotated Java config classes
   into a JSON "doc" (for agents / tooling).

2) Generate a JSON Schema (draft-07) from that metadata so that
   VS Code (and other JSON-schema-aware tools) can provide:
     - autocomplete
     - validation
     - default value hints
     - enum suggestions for profile references like "$shapes/smooth_sphere".

It is tuned for patterns like in your `field` project, e.g.:

    public record LifecycleConfig(
        @Range(ValueRange.STEPS) int fadeIn,
        @Range(ValueRange.STEPS) int fadeOut,
        @Range(ValueRange.STEPS) int scaleIn,
        @Range(ValueRange.STEPS) int scaleOut,
        DecayConfig decay
    ) { ... }

    public static class Builder {
        private @Range(ValueRange.STEPS) int fadeIn = 20;
        private @Range(ValueRange.STEPS) int fadeOut = 40;
        ...
    }

    public static LifecycleConfig fromJson(JsonObject json) {
        if (json == null) return null;

        int fadeIn   = json.has("fadeIn")   ? json.get("fadeIn").getAsInt()   : 20;
        int fadeOut  = json.has("fadeOut")  ? json.get("fadeOut").getAsInt()  : 40;
        ...
        return new LifecycleConfig(fadeIn, fadeOut, scaleIn, scaleOut, decay);
    }

References like "$shapes/smooth_sphere" are recognized and represented as:

    "ref": {
      "raw": "$shapes/smooth_sphere",
      "group": "shapes",
      "id": "smooth_sphere"
    }

You can map these to existing profile JSONs to get enum-based autocomplete.

----------------------------------------------------------------------
Usage examples
----------------------------------------------------------------------

# Full pipeline (extract + schema) from restricted roots
python3 scripts/config_schema_extractor.py \
  --paths \
    src/main/java/net/cyberpunk042/field \
    src/client/java/net/cyberpunk042/client/field

# Broader paths, but restricted by package prefixes
python3 scripts/config_schema_extractor.py \
  --paths src \
  --package-prefix net.cyberpunk042.field \
  --package-prefix net.cyberpunk042.client.field \
  --class-name-contains Config

# Specify where to write the internal JSON doc and VS Code schema
python3 scripts/config_schema_extractor.py \
    --json-doc-out agent-tools/config-schema/config_schema.json \
    --schema-out   agent-tools/config-schema/field_profiles.schema.json

# Use a specific top-level class for profiles.json (e.g., LifecycleConfig)
python3 scripts/config_schema_extractor.py \
    --top-class net.cyberpunk042.field.influence.LifecycleConfig

# Scan profile folders to derive enums for $group/id refs
# (assumes structure: profiles_root/<group>/*.json; id = filename without .json)
python3 scripts/config_schema_extractor.py \
    --profiles-root profiles

# Also write a human-readable log
python3 scripts/config_schema_extractor.py --save-log agent-tools

After generating `field_profiles.schema.json`, wire it into VS Code:

In .vscode/settings.json:

{
  "json.schemas": [
    {
      "fileMatch": [
        "profiles.json",
        "profiles/*.json"
      ],
      "url": "./agent-tools/config-schema/field_profiles.schema.json"
    }
  ]
}

VS Code will then provide autocomplete/validation based on your extracted config.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass, field as dc_field
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class FieldMeta:
    name: str
    type: str
    annotations: Dict[str, Dict[str, Any]] = dc_field(default_factory=dict)
    default_field: Any = None   # default from field initializer
    json_key: Optional[str] = None
    json_default: Any = None
    file: str = ""
    line: int = 0

    # Reference metadata for "$group/id" style values
    ref_raw: Optional[str] = None
    ref_group: Optional[str] = None
    ref_id: Optional[str] = None


@dataclass
class ClassMeta:
    name: str  # fully-qualified
    fields: Dict[str, FieldMeta] = dc_field(default_factory=dict)


# ---------------------------------------------------------------------------
# Output collector (stdout + buffer)
# ---------------------------------------------------------------------------

def make_output_collector():
    buffer: List[str] = []

    def out(*args):
        msg = " ".join(str(a) for a in args)
        buffer.append(msg)
        print(msg)

    return out, buffer


# ---------------------------------------------------------------------------
# Regex patterns
# ---------------------------------------------------------------------------

PACKAGE_RE = re.compile(r"^\s*package\s+([a-zA-Z0-9_.]+)\s*;", re.MULTILINE)

CLASS_LINE_RE = re.compile(
    r"^\s*(?:public\s+|protected\s+|private\s+)?"
    r"(?:final\s+|abstract\s+)?"
    r"(class|record|interface|enum)\s+([A-Za-z0-9_]+)"
)

ANNOTATION_RE = re.compile(
    r"^\s*@(?P<name>[A-Za-z0-9_$.]+)\s*(\((?P<args>.*)\))?\s*$"
)

FIELD_RE = re.compile(
    r"""
    ^\s*
    (?:(?:public|protected|private|static|final|transient|volatile|synchronized)\s+)*  # modifiers
    (?P<type>[A-Za-z0-9_$.<>?\[\]]+)                                                 # type
    \s+
    (?P<name>[A-Za-z0-9_]+)                                                          # field name
    \s*
    (?P<rest>=.*)?;                                                                  # optional initializer until semicolon
    """,
    re.VERBOSE,
)

# Multi-line record header
RECORD_RE = re.compile(
    r"(?:^|\s)(public\s+)?record\s+([A-Za-z0-9_]+)\s*\((?P<params>.*?)\)\s*\{",
    re.DOTALL,
)

# fromJson(...) assignments:
#   TYPE var = json.has("key") ? json.get("key").getAsXxx() : DEFAULT;
FROM_JSON_ASSIGN_RE = re.compile(
    r"""
    ^\s*
    (?P<type>[A-Za-z0-9_$.<>?\[\]]+)        # type
    \s+
    (?P<var>[A-Za-z0-9_]+)                 # variable name
    \s*=\s*
    json\.has\("(?P<key>[^"]+)"\)          # json.has("key")
    \s*\?\s*
    json\.get\("\3"\)\.getAs[A-Za-z0-9_]+\(\)
    \s*:\s*
    (?P<default>[^;]+)
    ;
    """,
    re.VERBOSE,
)

# ref strings: "$group/id"
REF_RE = re.compile(r'^\$(?P<group>[A-Za-z0-9_\-]+)/(?P<id>[A-Za-z0-9_\-./]+)$')


# ---------------------------------------------------------------------------
# Utility parsers
# ---------------------------------------------------------------------------

def parse_annotation_line(line: str) -> Optional[Tuple[str, str]]:
    """
    Parse a single-line annotation like:

        @Range(ValueRange.STEPS)
        @MyAnno
        @pkg.MyAnno(arg = "value")

    Returns (name, args_string) or None.
    """
    m = ANNOTATION_RE.match(line.strip())
    if not m:
        return None
    name = m.group("name")
    args = m.group("args") or ""
    return name, args.strip()


def split_args(args: str) -> List[str]:
    """
    Split an argument list on commas, respecting parentheses and quotes.

    Example:
        "min = 0, max = 10" -> ["min = 0", "max = 10"]
    """
    if not args:
        return []

    parts: List[str] = []
    buf: List[str] = []
    depth = 0
    in_single = False
    in_double = False

    for ch in args:
        if ch == "'" and not in_double:
            in_single = not in_single
        elif ch == '"' and not in_single:
            in_double = not in_double
        elif ch == "(" and not in_single and not in_double:
            depth += 1
        elif ch == ")" and not in_single and not in_double and depth > 0:
            depth -= 1

        if ch == "," and depth == 0 and not in_single and not in_double:
            part = "".join(buf).strip()
            if part:
                parts.append(part)
            buf = []
        else:
            buf.append(ch)

    if buf:
        part = "".join(buf).strip()
        if part:
            parts.append(part)

    return parts


def parse_value_literal(text: str) -> Any:
    """
    Parse a very simple literal:
        - integers
        - floats
        - booleans
        - strings ("..." or '...')
        - enum constants / identifiers

    For complex expressions, returns the raw string.
    """
    s = text.strip()

    # String literal
    if (len(s) >= 2) and ((s[0] == s[-1] == '"') or (s[0] == s[-1] == "'")):
        return s[1:-1]

    # Boolean
    if s == "true":
        return True
    if s == "false":
        return False

    # Integer
    if re.fullmatch(r"[+-]?\d+", s):
        try:
            return int(s)
        except ValueError:
            pass

    # Float
    if re.fullmatch(r"[+-]?(\d+\.\d*|\.\d+)([eE][+-]?\d+)?", s) or re.fullmatch(
        r"[+-]?\d+[eE][+-]?\d+", s
    ):
        try:
            return float(s)
        except ValueError:
            pass

    # Enum / identifier-like: keep as string
    return s


def parse_annotation_args(args: str) -> Dict[str, Any]:
    """
    Parse annotation args into {key: value} mapping.

    Supports:
        @Anno(123)
        @Anno("foo")
        @Anno(min = 0, max = 10)
        @Anno(value = MyEnum.FOO)

    Returns a dict; for unnamed single arguments, uses key "value".
    """
    args = args.strip()
    if not args:
        return {}

    parts = split_args(args)
    if not parts:
        return {}

    parsed: Dict[str, Any] = {}

    # If there's exactly one arg and no '=' in it, treat as 'value'
    if len(parts) == 1 and "=" not in parts[0]:
        parsed["value"] = parse_value_literal(parts[0])
        return parsed

    for part in parts:
        if "=" in part:
            key, val = part.split("=", 1)
            key = key.strip()
            val = val.strip()
            parsed[key] = parse_value_literal(val)
        else:
            k = f"arg_{len(parsed)}"
            parsed[k] = parse_value_literal(part)

    return parsed


def parse_field_line(line: str) -> Optional[Tuple[str, str, Optional[str]]]:
    """
    Parse a field declaration line like:

        public int foo = 5;
        private final List<String> stuff = new ArrayList<>();
        protected MyEnum value;

    Returns (type, name, default_string_or_None).
    """
    m = FIELD_RE.match(line)
    if not m:
        return None
    t = m.group("type")
    name = m.group("name")
    rest = m.group("rest")
    default_str = None
    if rest:
        default_str = rest.lstrip().lstrip("=").strip()
        default_str = default_str.rstrip(";").strip()
    return t, name, default_str


def parse_default_literal(default_str: Optional[str]) -> Any:
    if default_str is None:
        return None
    return parse_value_literal(default_str)


def parse_ref_string(value: Any) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    """
    If value is a string like "$shapes/smooth_sphere",
    return (raw, group, id). Otherwise (None, None, None).
    """
    if not isinstance(value, str):
        return None, None, None
    m = REF_RE.match(value.strip())
    if not m:
        return None, None, None
    group = m.group("group")
    ident = m.group("id")
    return value, group, ident


# ---------------------------------------------------------------------------
# Record component parsing
# ---------------------------------------------------------------------------

def parse_record_components(
    params_text: str,
    file_path: Path,
    file_text: str,
    record_start_index: int,
) -> List[Tuple[str, FieldMeta]]:
    """
    Parse record components inside the (...) of a record declaration.

    params_text: the literal text between '(' and ')'
    Returns a list of (fieldName, FieldMeta).
    """

    parts = split_args(params_text)
    results: List[Tuple[str, FieldMeta]] = []

    for part in parts:
        comp = part.strip()
        if not comp:
            continue

        # Extract inline annotations first
        annotations: Dict[str, Dict[str, Any]] = {}
        rest = comp

        while rest.lstrip().startswith("@"):
            m = re.match(
                r'^\s*(@[A-Za-z0-9_$.]+(?:\([^()]*\))?)\s*(.*)$',
                rest,
            )
            if not m:
                break
            ann_str, rest = m.group(1), m.group(2)
            ann_parsed = parse_annotation_line(ann_str)
            if ann_parsed:
                ann_name, ann_args = ann_parsed
                params = parse_annotation_args(ann_args)
                short_ann = ann_name.split(".")[-1]
                if short_ann not in annotations:
                    annotations[short_ann] = {}
                annotations[short_ann].update(params)

        rest = rest.strip()
        if not rest:
            continue

        # Now parse "type name"
        m2 = re.match(
            r'^([A-Za-z0-9_$.<>?\[\]]+)\s+([A-Za-z0-9_]+)$',
            rest,
        )
        if not m2:
            continue

        f_type = m2.group(1)
        f_name = m2.group(2)

        # Approximate line via text position
        part_index = file_text.find(part, record_start_index)
        line = file_text.count("\n", 0, part_index) + 1 if part_index != -1 else 1

        fm = FieldMeta(
            name=f_name,
            type=f_type,
            annotations=annotations,
            default_field=None,
            json_key=None,
            json_default=None,
            file=str(file_path),
            line=line,
        )
        results.append((f_name, fm))

    return results


# ---------------------------------------------------------------------------
# fromJson parsing
# ---------------------------------------------------------------------------

def scan_fromjson_defaults(text: str) -> Dict[str, Tuple[str, Any]]:
    """
    Scan for assignments of the form:

        TYPE var = json.has("key") ? json.get("key").getAsXxx() : DEFAULT;

    Returns mapping:
        varName -> (jsonKey, defaultValue)
    """
    defaults: Dict[str, Tuple[str, Any]] = {}
    for line in text.splitlines():
        m = FROM_JSON_ASSIGN_RE.match(line)
        if not m:
            continue
        _type = m.group("type")
        var = m.group("var")
        key = m.group("key")
        default_str = m.group("default")
        default_val = parse_value_literal(default_str)
        defaults[var] = (key, default_val)
    return defaults


# ---------------------------------------------------------------------------
# Scanning logic (Java -> ClassMeta / FieldMeta)
# ---------------------------------------------------------------------------

def scan_file(
    path: Path,
    class_name_filter: Optional[str],
    package_prefixes: Optional[List[str]],
    out,
) -> List[ClassMeta]:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        out(f"[warn] Skipping non-UTF8 or unreadable file: {path}")
        return []

    pkg_match = PACKAGE_RE.search(text)
    package = pkg_match.group(1) if pkg_match else ""

    # Package-prefix filter: if prefixes are provided, skip files whose
    # package does not start with any of them.
    if package_prefixes:
        if package and not any(package.startswith(p) for p in package_prefixes):
            out(f"[info] Skipping file (package not in prefixes): {path} ({package})")
            return []

    classes: Dict[str, ClassMeta] = {}

    # 1) Record declarations
    for m in RECORD_RE.finditer(text):
        short_name = m.group(2)
        params_text = m.group("params")
        fqcn = f"{package}.{short_name}" if package else short_name
        cm = classes.setdefault(fqcn, ClassMeta(name=fqcn))
        comps = parse_record_components(params_text, path, text, m.start())
        for fname, fm in comps:
            existing = cm.fields.get(fname)
            if existing:
                for k, v in fm.annotations.items():
                    if k not in existing.annotations:
                        existing.annotations[k] = {}
                    existing.annotations[k].update(v)
            else:
                cm.fields[fname] = fm

    # 2) Line-based scan for classes, fields, annotations
    lines = text.splitlines()
    current_class: Optional[str] = None
    pending_annotations: List[Tuple[str, Dict[str, Any], int]] = []

    for idx, raw_line in enumerate(lines, start=1):
        line = raw_line.rstrip("\n")

        # Class / record / interface / enum
        m_cls = CLASS_LINE_RE.match(line)
        if m_cls:
            kind = m_cls.group(1)
            short_name = m_cls.group(2)
            fqcn = f"{package}.{short_name}" if package else short_name
            current_class = fqcn
            classes.setdefault(fqcn, ClassMeta(name=fqcn))
            pending_annotations.clear()
            continue

        if current_class is None:
            continue

        stripped = line.strip()
        if not stripped:
            pending_annotations.clear()
            continue

        # Annotation line
        if stripped.startswith("@"):
            parsed = parse_annotation_line(stripped)
            if parsed:
                ann_name, ann_args = parsed
                ann_params = parse_annotation_args(ann_args)
                pending_annotations.append((ann_name, ann_params, idx))
            continue

        # Field line
        field_parsed = parse_field_line(line)
        if field_parsed:
            f_type, f_name, default_str = field_parsed
            default_val = parse_default_literal(default_str)
            fqcn = current_class
            cm = classes.setdefault(fqcn, ClassMeta(name=fqcn))

            # Annotations above
            ann_map: Dict[str, Dict[str, Any]] = {}
            for ann_name, ann_params, _ann_line in pending_annotations:
                short_ann = ann_name.split(".")[-1]
                if short_ann not in ann_map:
                    ann_map[short_ann] = {}
                ann_map[short_ann].update(ann_params)

            # Inline annotations (rough)
            inline_ann_map: Dict[str, Dict[str, Any]] = {}
            if "@" in line:
                left = line.split(f_name, 1)[0]
                tokens = left.split()
                for tok in tokens:
                    if tok.startswith("@"):
                        ann_parsed = parse_annotation_line(tok)
                        if ann_parsed:
                            ann_name, ann_args = ann_parsed
                            params = parse_annotation_args(ann_args)
                            short_ann = ann_name.split(".")[-1]
                            if short_ann not in inline_ann_map:
                                inline_ann_map[short_ann] = {}
                            inline_ann_map[short_ann].update(params)

            for k, v in inline_ann_map.items():
                if k not in ann_map:
                    ann_map[k] = {}
                ann_map[k].update(v)

            existing = cm.fields.get(f_name)
            if existing:
                if existing.type == "" or existing.type is None:
                    existing.type = f_type
                if default_val is not None:
                    existing.default_field = default_val
                for ak, av in ann_map.items():
                    if ak not in existing.annotations:
                        existing.annotations[ak] = {}
                    existing.annotations[ak].update(av)
                existing.file = existing.file or str(path)
                if existing.line == 0:
                    existing.line = idx
            else:
                fm = FieldMeta(
                    name=f_name,
                    type=f_type,
                    annotations=ann_map,
                    default_field=default_val,
                    json_key=None,
                    json_default=None,
                    file=str(path),
                    line=idx,
                )
                cm.fields[f_name] = fm

            pending_annotations.clear()
            continue

        pending_annotations.clear()

    # 3) fromJson defaults (file-level)
    json_defaults = scan_fromjson_defaults(text)

    for cm in classes.values():
        for var_name, (json_key, json_def) in json_defaults.items():
            fm = cm.fields.get(var_name)
            if not fm:
                continue
            fm.json_key = json_key
            fm.json_default = json_def

    # 4) classify reference-like fields
    for cm in classes.values():
        for fm in cm.fields.values():
            ref_raw, ref_group, ref_id = parse_ref_string(fm.json_default)
            if ref_raw is None:
                ref_raw, ref_group, ref_id = parse_ref_string(fm.default_field)
            fm.ref_raw = ref_raw
            fm.ref_group = ref_group
            fm.ref_id = ref_id

    # Apply class name filter
    if class_name_filter:
        filtered: List[ClassMeta] = []
        for fqcn, meta in classes.items():
            if class_name_filter in fqcn or any(
                class_name_filter in f for f in meta.fields.keys()
            ):
                filtered.append(meta)
        return filtered

    return list(classes.values())


def scan_paths(
    paths: List[Path],
    class_name_filter: Optional[str],
    package_prefixes: Optional[List[str]],
    out,
) -> Dict[str, ClassMeta]:
    all_classes: Dict[str, ClassMeta] = {}
    for root in paths:
        if root.is_file():
            if root.suffix == ".java":
                out(f"[info] Scanning file: {root}")
                metas = scan_file(root, class_name_filter, package_prefixes, out)
                for cm in metas:
                    if cm.name in all_classes:
                        all_classes[cm.name].fields.update(cm.fields)
                    else:
                        all_classes[cm.name] = cm
        else:
            for path in root.rglob("*.java"):
                out(f"[info] Scanning file: {path}")
                metas = scan_file(path, class_name_filter, package_prefixes, out)
                for cm in metas:
                    if cm.name in all_classes:
                        all_classes[cm.name].fields.update(cm.fields)
                    else:
                        all_classes[cm.name] = cm
    return all_classes


# ---------------------------------------------------------------------------
# JSON Doc building (internal metadata)
# ---------------------------------------------------------------------------

def build_internal_doc(classes: Dict[str, ClassMeta]) -> Dict[str, Any]:
    doc: Dict[str, Any] = {"classes": {}}
    for fqcn, cm in sorted(classes.items(), key=lambda kv: kv[0]):
        cls_entry: Dict[str, Any] = {"fields": {}}
        for fname, fm in sorted(cm.fields.items(), key=lambda kv: kv[0]):
            cls_entry["fields"][fname] = {
                "type": fm.type,
                "annotations": fm.annotations,
                "defaultField": fm.default_field,
                "jsonKey": fm.json_key,
                "jsonDefault": fm.json_default,
                "ref": {
                    "raw": fm.ref_raw,
                    "group": fm.ref_group,
                    "id": fm.ref_id,
                },
                "location": {
                    "file": fm.file,
                    "line": fm.line,
                },
            }
        doc["classes"][fqcn] = cls_entry
    return doc


# ---------------------------------------------------------------------------
# Profile index (for $group/id enums)
# ---------------------------------------------------------------------------

def scan_profiles_root(profiles_root: Optional[Path], out) -> Dict[str, List[str]]:
    """
    Scan a profiles root directory to discover group/id pairs.

    Convention:
        profiles_root/<group>/*.json
        where id = filename without extension.

    Example:
        profiles/shapes/smooth_sphere.json -> group: "shapes", id: "smooth_sphere"
    """
    index: Dict[str, List[str]] = {}
    if profiles_root is None:
        return index
    if not profiles_root.exists():
        out(f"[warn] profiles-root does not exist: {profiles_root}")
        return index
    if not profiles_root.is_dir():
        out(f"[warn] profiles-root is not a directory: {profiles_root}")
        return index

    for group_dir in profiles_root.iterdir():
        if not group_dir.is_dir():
            continue
        group = group_dir.name
        ids: List[str] = []
        for json_file in group_dir.glob("*.json"):
            ids.append(json_file.stem)
        if ids:
            ids.sort()
            index[group] = ids

    out(f"[info] Profile groups discovered: {', '.join(sorted(index.keys())) or '(none)'}")
    return index


# ---------------------------------------------------------------------------
# JSON Schema generation (VS Code)
# ---------------------------------------------------------------------------

def java_type_to_schema(
    type_str: str,
    fqcn_context: str,
    known_classes: Dict[str, ClassMeta],
) -> Dict[str, Any]:
    """
    Map a Java type string to a JSON Schema type or $ref.
    Uses FQCNs as definition keys.
    """
    t = type_str.strip()
    # Basic generics removal and detection
    # arrays
    if t.endswith("[]"):
        elem_type = t[:-2].strip()
        return {
            "type": "array",
            "items": java_type_to_schema(elem_type, fqcn_context, known_classes),
        }

    # List / Set / Collection
    m_coll = re.match(r'^(List|Set|Collection)<(.+)>$', t)
    if m_coll:
        elem_type = m_coll.group(2).strip()
        return {
            "type": "array",
            "items": java_type_to_schema(elem_type, fqcn_context, known_classes),
        }

    # Primitive mappings
    primitive_map = {
        "byte": "integer",
        "short": "integer",
        "int": "integer",
        "long": "integer",
        "float": "number",
        "double": "number",
        "boolean": "boolean",
        "char": "string",
    }
    if t in primitive_map:
        return {"type": primitive_map[t]}

    # Common Java reference types
    if t in ("String", "java.lang.String"):
        return {"type": "string"}

    # FQCN detection: if this matches any known class, reference it by FQCN
    simple_to_fqcn: Dict[str, str] = {}
    for fqcn in known_classes.keys():
        simple = fqcn.split(".")[-1]
        simple_to_fqcn.setdefault(simple, fqcn)

    # If type_str is fully-qualified and known
    if t in known_classes:
        return {"$ref": f"#/definitions/{t}"}

    # If type_str is a simple name mapping to a known fqcn
    simple_fqcn = simple_to_fqcn.get(t)
    if simple_fqcn:
        return {"$ref": f"#/definitions/{simple_fqcn}"}

    # Fallback: string
    return {"type": "string"}


def apply_range_constraints(schema_prop: Dict[str, Any], annotations: Dict[str, Dict[str, Any]]) -> None:
    """
    If a field has a @Range annotation with numeric min/max, encode them.
    If not numeric, attach a descriptive hint.
    """
    range_ann = annotations.get("Range")
    if not range_ann:
        return

    description_parts: List[str] = []
    if "min" in range_ann and isinstance(range_ann["min"], (int, float)):
        schema_prop["minimum"] = range_ann["min"]
    elif "min" in range_ann:
        description_parts.append(f"min: {range_ann['min']}")

    if "max" in range_ann and isinstance(range_ann["max"], (int, float)):
        schema_prop["maximum"] = range_ann["max"]
    elif "max" in range_ann:
        description_parts.append(f"max: {range_ann['max']}")

    if "value" in range_ann:
        description_parts.append(f"valueRange: {range_ann['value']}")

    if description_parts:
        existing = schema_prop.get("description")
        extra = "Range(" + ", ".join(description_parts) + ")"
        schema_prop["description"] = (existing + " " + extra).strip() if existing else extra


def build_json_schema(
    classes: Dict[str, ClassMeta],
    profile_index: Dict[str, List[str]],
    top_class: Optional[str],
    out,
) -> Dict[str, Any]:
    """
    Build a draft-07 JSON Schema from extracted ClassMeta/FieldMeta.

    - Definitions per class: key = FQCN.
    - Each field:
        * type, default, range from annotations, ref enum/pattern.
    - Top-level:
        * If top_class is provided and exists:
              schema describes that class directly.
        * Otherwise:
              schema is a generic "any object" with definitions only.
    """
    schema: Dict[str, Any] = {
        "$schema": "https://json-schema.org/draft-07/schema#",
        "title": "Generated config schema",
        "type": "object",
        "definitions": {},
    }

    # Build definitions
    for fqcn, cm in sorted(classes.items(), key=lambda kv: kv[0]):
        props: Dict[str, Any] = {}
        required: List[str] = []

        for fname, fm in sorted(cm.fields.items(), key=lambda kv: kv[0]):
            prop_schema = java_type_to_schema(fm.type, fqcn, classes)

            # Default value: prefer JSON default, else field default
            default_val = fm.json_default if fm.json_default is not None else fm.default_field
            if default_val is not None:
                prop_schema["default"] = default_val

            # Range annotation
            apply_range_constraints(prop_schema, fm.annotations)

            # Reference-like fields (e.g. "$shapes/smooth_sphere")
            if fm.ref_group:
                group = fm.ref_group
                ids = profile_index.get(group, [])
                if ids:
                    # enum of full "$group/id" values
                    prop_schema["type"] = "string"
                    prop_schema["enum"] = [f"${group}/{ident}" for ident in ids]
                else:
                    # generic pattern
                    prop_schema["type"] = "string"
                    prop_schema["pattern"] = rf"^\${re.escape(group)}/.+$"

            # Optional: treat absence of default as "required" or not; for now, we don't enforce.
            # You can later decide to mark fields as required based on annotations.

            props[fname] = prop_schema

        schema["definitions"][fqcn] = {
            "type": "object",
            "properties": props,
            "additionalProperties": False,
        }
        if required:
            schema["definitions"][fqcn]["required"] = required

    # Top-level structure
    if top_class:
        # Accept both FQCN and simple name
        target_fqcn = None
        if top_class in classes:
            target_fqcn = top_class
        else:
            # look up by simple name or suffix
            for fqcn in classes:
                simple = fqcn.split(".")[-1]
                if fqcn == top_class or fqcn.endswith("." + top_class) or simple == top_class:
                    target_fqcn = fqcn
                    break

        if target_fqcn:
            out(f"[info] Using top-class: {target_fqcn}")
            schema["$ref"] = f"#/definitions/{target_fqcn}"
        else:
            out(f"[warn] top-class not found among extracted classes: {top_class}")
            out("[warn] Top-level schema left as generic object with definitions only.")
    else:
        out("[info] No top-class specified; top-level is a generic object with definitions only.")

    return schema


# ---------------------------------------------------------------------------
# Log saving
# ---------------------------------------------------------------------------

def save_log(base_dir: Path, label: str, buffer: List[str], out) -> None:
    base_dir.mkdir(parents=True, exist_ok=True)
    sub = base_dir / "config-schema"
    sub.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    lab = label.replace(" ", "_").replace("/", "_").replace(".", "_")
    filename = f"{lab}_{timestamp}.log"
    path = sub / filename

    with path.open("w", encoding="utf-8") as f:
        f.write("\n".join(buffer))

    out(f"[saved] Human-readable log written to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extract config metadata from Java and generate JSON Schema for VS Code.",
    )
    parser.add_argument(
        "--paths",
        nargs="+",
        type=Path,
        default=[Path("src")],
        help="Java source paths to scan (default: src).",
    )
    parser.add_argument(
        "--class-name-contains",
        help="Filter to classes whose name (or FQCN) contains this substring (e.g. Config, Lifecycle).",
    )
    parser.add_argument(
        "--package-prefix",
        action="append",
        help=(
            "Only include classes whose package starts with this prefix. "
            "Can be specified multiple times, e.g. "
            "--package-prefix net.cyberpunk042.field "
            "--package-prefix net.cyberpunk042.client.field"
        ),
    )
    parser.add_argument(
        "--json-doc-out",
        type=Path,
        default=Path("agent-tools/config-schema/config_schema.json"),
        help="Output JSON doc file (default: agent-tools/config-schema/config_schema.json).",
    )
    parser.add_argument(
        "--schema-out",
        type=Path,
        default=Path("agent-tools/config-schema/field_profiles.schema.json"),
        help="Output JSON Schema file (default: agent-tools/config-schema/field_profiles.schema.json).",
    )
    parser.add_argument(
        "--profiles-root",
        type=Path,
        help="Optional profiles root (profiles_root/<group>/*.json) for $group/id enums.",
    )
    parser.add_argument(
        "--top-class",
        help="Optional top-level class (FQCN or simple name) for the root of the JSON Schema.",
    )
    parser.add_argument(
        "--save-log",
        nargs="?",
        const="agent-tools",
        help="Also write a human-readable log under this base directory (default: agent-tools).",
    )
    parser.add_argument(
        "--skip-schema",
        action="store_true",
        help="Only extract and write the internal JSON doc; skip JSON Schema generation.",
    )
    parser.add_argument(
        "--skip-doc",
        action="store_true",
        help="Skip writing the internal JSON doc (still generates JSON Schema).",
    )
    return parser.parse_args()


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    args = parse_args()
    out, buffer = make_output_collector()

    paths = [p for p in args.paths if p.exists()]
    if not paths:
        out("[err] None of the specified paths exist.")
        if args.save_log:
            save_log(Path(args.save_log), "config_schema_error", buffer, out)
        return

    out("[info] Starting config schema extraction.")
    out(f"[info] Paths: {', '.join(str(p) for p in paths)}")
    out(f"[info] Class name filter: {args.class_name_contains if args.class_name_contains else '(none)'}")
    out(f"[info] Package prefixes: {', '.join(args.package_prefix) if args.package_prefix else '(none)'}")
    out(f"[info] JSON doc output: {args.json_doc_out}")
    out(f"[info] JSON schema output: {args.schema_out}")
    out(f"[info] Profiles root: {args.profiles_root if args.profiles_root else '(none)'}")
    out(f"[info] Top class: {args.top_class if args.top_class else '(none)'}")

    # 1) Scan Java sources -> ClassMeta / FieldMeta
    classes = scan_paths(paths, args.class_name_contains, args.package_prefix, out)

    if not classes:
        out("[info] No classes / fields found matching criteria.")
        doc = {"classes": {}}
        if not args.skip_doc:
            args.json_doc_out.parent.mkdir(parents=True, exist_ok=True)
            args.json_doc_out.write_text(json.dumps(doc, indent=2), encoding="utf-8")
            out("[info] Wrote empty schema doc JSON.")
        if not args.skip_schema:
            empty_schema = {
                "$schema": "https://json-schema.org/draft-07/schema#",
                "title": "Empty schema (no classes found)",
                "type": "object",
                "definitions": {},
            }
            args.schema_out.parent.mkdir(parents=True, exist_ok=True)
            args.schema_out.write_text(json.dumps(empty_schema, indent=2), encoding="utf-8")
            out("[info] Wrote empty JSON Schema.")
        if args.save_log:
            save_log(Path(args.save_log), "config_schema_empty", buffer, out)
        return

    # 2) Build internal doc
    doc = build_internal_doc(classes)

    if not args.skip_doc:
        args.json_doc_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_doc_out.write_text(json.dumps(doc, indent=2, sort_keys=True), encoding="utf-8")
        out(f"[info] Wrote internal doc JSON to: {args.json_doc_out}")

    # 3) Profile index (for enums on refs)
    profile_index = scan_profiles_root(args.profiles_root, out)

    # 4) JSON Schema
    if not args.skip_schema:
        schema = build_json_schema(classes, profile_index, args.top_class, out)
        args.schema_out.parent.mkdir(parents=True, exist_ok=True)
        args.schema_out.write_text(json.dumps(schema, indent=2, sort_keys=True), encoding="utf-8")
        out(f"[info] Wrote JSON Schema to: {args.schema_out}")

    # Summary
    total_classes = len(classes)
    total_fields = sum(len(c.fields) for c in classes.values())
    out(f"[summary] Classes: {total_classes}, fields: {total_fields}")

    if args.save_log:
        label = "config_schema"
        if args.class_name_contains:
            label += "_" + args.class_name_contains
        save_log(Path(args.save_log), label, buffer, out)


if __name__ == "__main__":
    main()
