#!/usr/bin/env python3
"""
config_profile_lint.py

Lint and validate Field "profiles" JSON files.

It can:
  - Validate profiles against your generated JSON Schema
    (e.g. field_profiles.schema.json from config_schema_extractor.py).
  - Check that all "$group/id" references point to actual profiles under
    profiles-root/<group>/*.json.
  - Report unused profiles (defined but never referenced).
  - Summarize errors and warnings, and exit non-zero if there are hard errors.

----------------------------------------------------------------------
Typical usage
----------------------------------------------------------------------

# 1) With profiles rooted at profiles/<group>/<id>.json
#    and a JSON Schema at agent-tools/config-schema/field_profiles.schema.json:
python3 scripts/config_profile_lint.py \
  --profiles-root profiles \
  --schema agent-tools/config-schema/field_profiles.schema.json

# 2) Restrict to specific files / directories:
python3 scripts/config_profile_lint.py \
  --profiles-root profiles \
  --paths profiles/shapes profiles/effects/fade_in.json \
  --schema agent-tools/config-schema/field_profiles.schema.json

If you don't provide --schema, it will skip JSON Schema validation and only:
  - build a profile index from profiles-root
  - scan all JSON files for "$group/id" references
  - validate that each reference points to an existing profile
  - report unused profiles.

----------------------------------------------------------------------
Exit codes
----------------------------------------------------------------------

0  => all good (no schema errors, no invalid references)
1  => some schema validation errors and/or invalid references
2  => script misconfiguration (e.g. profiles-root not found)
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field as dc_field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    import jsonschema  # type: ignore
except ImportError:
    jsonschema = None


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class ProfileRef:
    file: Path
    json_path: str  # JSON Pointer-ish path, e.g. "/effects/0/shape"
    raw: str
    group: str
    ident: str


@dataclass
class LintResult:
    file: Path
    schema_errors: List[str] = dc_field(default_factory=list)
    invalid_refs: List[ProfileRef] = dc_field(default_factory=list)
    warnings: List[str] = dc_field(default_factory=list)


# ---------------------------------------------------------------------------
# Constants / regex
# ---------------------------------------------------------------------------

# Same convention as the extractor: "$group/id"
REF_RE = re.compile(r'^\$(?P<group>[A-Za-z0-9_\-]+)/(?P<id>[A-Za-z0-9_\-./]+)$')


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Lint Field profiles JSON files against schema and profile references.",
    )
    parser.add_argument(
        "--profiles-root",
        type=Path,
        required=True,
        help="Root directory containing groups: profiles-root/<group>/*.json",
    )
    parser.add_argument(
        "--paths",
        nargs="*",
        type=Path,
        help=(
            "Optional specific profile files/directories to lint. "
            "If omitted, all *.json under profiles-root are used."
        ),
    )
    parser.add_argument(
        "--schema",
        type=Path,
        help="Optional JSON Schema file (e.g. field_profiles.schema.json).",
    )
    parser.add_argument(
        "--allow-missing-schema",
        action="store_true",
        help="Do not treat missing jsonschema library as an error; skip schema validation.",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Print per-file details even when there are no errors.",
    )
    return parser.parse_args()


def load_schema(schema_path: Optional[Path]) -> Optional[Dict[str, Any]]:
    if not schema_path:
        return None
    if not schema_path.exists():
        print(f"[err] Schema file does not exist: {schema_path}", file=sys.stderr)
        return None
    try:
        with schema_path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"[err] Failed to load schema {schema_path}: {e}", file=sys.stderr)
        return None


def build_profile_index(profiles_root: Path) -> Dict[str, List[str]]:
    """
    Build index: group -> [id, id, ...]

    Convention:
        profiles_root/<group>/*.json
        id = filename without .json
    """
    index: Dict[str, List[str]] = {}
    if not profiles_root.exists() or not profiles_root.is_dir():
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
    return index


def find_profile_files(
    profiles_root: Path,
    paths: Optional[List[Path]],
) -> List[Path]:
    """
    Determine which *.json files to lint.

    - If paths is provided:
        * For each file: if .json, use it.
        * For each directory: recursively glob *.json.
    - Else:
        * recursively glob *.json under profiles_root.
    """
    files: List[Path] = []

    if paths:
        for p in paths:
            if p.is_file() and p.suffix.lower() == ".json":
                files.append(p)
            elif p.is_dir():
                files.extend(sorted(p.rglob("*.json")))
            else:
                # ignore missing / non-json files
                pass
    else:
        files.extend(sorted(profiles_root.rglob("*.json")))

    # Deduplicate
    seen = set()
    uniq_files: List[Path] = []
    for f in files:
        if f not in seen:
            uniq_files.append(f)
            seen.add(f)
    return uniq_files


def parse_ref(value: Any) -> Optional[Tuple[str, str, str]]:
    """
    If value is a "$group/id" string, return (raw, group, id).
    Otherwise None.
    """
    if not isinstance(value, str):
        return None
    m = REF_RE.match(value.strip())
    if not m:
        return None
    return value, m.group("group"), m.group("id")


def walk_json_for_refs(
    obj: Any,
    path_prefix: str = "",
) -> List[ProfileRef]:
    """
    Walk a JSON object and collect all "$group/id" references.

    path_prefix is a JSON Pointer-ish string like "/effects/0/shape".
    """
    refs: List[ProfileRef] = []

    def _walk(o: Any, path: str):
        if isinstance(o, dict):
            for k, v in o.items():
                child_path = f"{path}/{k}"
                _walk(v, child_path)
        elif isinstance(o, list):
            for idx, v in enumerate(o):
                child_path = f"{path}/{idx}"
                _walk(v, child_path)
        else:
            parsed = parse_ref(o)
            if parsed:
                raw, group, ident = parsed
                # file will be filled later; we store placeholders
                refs.append(ProfileRef(file=Path(), json_path=path, raw=raw, group=group, ident=ident))

    _walk(obj, path_prefix or "")
    return refs


def validate_with_schema(
    schema: Dict[str, Any],
    data: Any,
    file: Path,
) -> List[str]:
    """
    Validate data against given JSON Schema using jsonschema.

    Returns list of error messages (empty if valid or if jsonschema is missing).
    """
    if jsonschema is None:
        # Caller should decide whether this is an error or just a warning
        return []

    validator = jsonschema.Draft7Validator(schema)
    errors = []
    for e in validator.iter_errors(data):
        loc = "/".join(str(p) for p in e.path)
        if loc:
            msg = f"{file}: at /{loc} - {e.message}"
        else:
            msg = f"{file}: {e.message}"
        errors.append(msg)
    return errors


# ---------------------------------------------------------------------------
# Lint logic
# ---------------------------------------------------------------------------

def lint_profile_file(
    file: Path,
    schema: Optional[Dict[str, Any]],
    profile_index: Dict[str, List[str]],
    allow_schema_missing: bool,
) -> LintResult:
    res = LintResult(file=file)

    # Load JSON
    try:
        with file.open("r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        res.schema_errors.append(f"Failed to parse JSON: {e}")
        return res

    # Schema validation
    if schema is not None:
        if jsonschema is None and not allow_schema_missing:
            res.schema_errors.append(
                "jsonschema library not available; cannot validate against schema. "
                "Install via `pip install jsonschema` or use --allow-missing-schema to ignore."
            )
        elif jsonschema is not None:
            errs = validate_with_schema(schema, data, file)
            res.schema_errors.extend(errs)

    # Reference validation
    refs = walk_json_for_refs(data, "")
    for r in refs:
        # fill file path
        r.file = file

        groups = profile_index.get(r.group)
        if groups is None:
            res.invalid_refs.append(r)
            continue
        if r.ident not in groups:
            res.invalid_refs.append(r)

    return res


def main() -> None:
    args = parse_args()

    profiles_root = args.profiles_root
    if not profiles_root.exists() or not profiles_root.is_dir():
        print(f"[err] profiles-root must be an existing directory: {profiles_root}", file=sys.stderr)
        sys.exit(2)

    # Load schema if provided
    schema = load_schema(args.schema)
    if args.schema and schema is None:
        # schema path specified but failed to load
        sys.exit(2)

    if schema is None and jsonschema is None and not args.allow_missing_schema and args.schema:
        print(
            "[err] jsonschema not installed and --allow-missing-schema not set; "
            "cannot perform schema validation.",
            file=sys.stderr,
        )
        sys.exit(2)

    # Build profile index (group -> [id])
    profile_index = build_profile_index(profiles_root)
    if not profile_index:
        print(f"[warn] No profile groups found under {profiles_root} (or directory is empty).", file=sys.stderr)

    # Determine which files to lint
    profile_files = find_profile_files(profiles_root, args.paths)
    if not profile_files:
        print("[warn] No profile JSON files found to lint.", file=sys.stderr)
        sys.exit(0)

    print(f"[info] Profiles root: {profiles_root}")
    print(f"[info] Files to lint: {len(profile_files)}")
    if args.schema:
        print(f"[info] Using schema: {args.schema}")
    else:
        print("[info] No schema specified; skipping JSON Schema validation.")

    all_results: List[LintResult] = []
    used_profiles: Dict[Tuple[str, str], List[ProfileRef]] = {}

    # Lint all files
    for f in profile_files:
        res = lint_profile_file(f, schema, profile_index, args.allow_missing_schema)
        all_results.append(res)

        # accumulate used profiles
        for ref in res.invalid_refs:
            # even invalid ones record usage, but group/id may not exist
            key = (ref.group, ref.ident)
            used_profiles.setdefault(key, []).append(ref)

        # we also need valid refs:
        try:
            with f.open("r", encoding="utf-8") as fp:
                data = json.load(fp)
        except Exception:
            # already counted as schema error; skip
            continue
        valid_refs = [
            r for r in walk_json_for_refs(data, "")
            if (r.group, r.ident) in {(g, i) for g, ids in profile_index.items() for i in ids}
        ]
        for r in valid_refs:
            r.file = f
            key = (r.group, r.ident)
            used_profiles.setdefault(key, []).append(r)

    # Report per-file
    hard_errors = 0
    soft_warnings = 0

    for res in all_results:
        has_err = bool(res.schema_errors or res.invalid_refs)
        has_warn = bool(res.warnings)

        if has_err or args.verbose:
            print("=" * 70)
            print(f"File: {res.file}")

        if res.schema_errors:
            print("  Schema errors:")
            for e in res.schema_errors:
                print(f"    - {e}")
            hard_errors += len(res.schema_errors)

        if res.invalid_refs:
            print("  Invalid references:")
            for r in res.invalid_refs:
                # Determine why it's invalid
                if r.group not in profile_index:
                    reason = f"unknown group '{r.group}'"
                else:
                    reason = f"unknown id '{r.ident}' in group '{r.group}'"
                print(f"    - {r.raw} at {r.json_path}: {reason}")
            hard_errors += len(res.invalid_refs)

        if res.warnings:
            print("  Warnings:")
            for w in res.warnings:
                print(f"    - {w}")
            soft_warnings += len(res.warnings)

    # Unused profiles
    print("=" * 70)
    print("Unused profiles:")
    unused_count = 0
    for group, ids in sorted(profile_index.items()):
        for ident in ids:
            key = (group, ident)
            if key not in used_profiles:
                unused_count += 1
                path = profiles_root / group / (ident + ".json")
                print(f"  - {group}/{ident} ({path})")

    if unused_count == 0:
        print("  (none)")

    # Summary
    print("=" * 70)
    print("Summary:")
    print(f"  Files checked: {len(profile_files)}")
    print(f"  Schema errors: {hard_errors}")
    print(f"  Invalid refs : {sum(len(r.invalid_refs) for r in all_results)}")
    print(f"  Warnings     : {soft_warnings}")
    print(f"  Unused profs : {unused_count}")
    print("=" * 70)

    if hard_errors > 0:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
