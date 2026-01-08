#!/usr/bin/env python3
"""
Documentation Cleanup Script
============================
ONLY handles docs/ folder and .md files.
Does NOT touch agent-tools/ or any other directories.

Features:
- Dry-run mode by default (shows what would happen)
- Copies files to archive before deleting (safe)
- Generates fresh documentation from Java source

Usage:
    python scripts/docs_cleanup.py --dry-run    # Preview changes (default)
    python scripts/docs_cleanup.py --execute    # Actually do it
    python scripts/docs_cleanup.py --generate   # Just generate new docs
"""

import argparse
import os
import shutil
import re
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict, Optional

PROJECT_ROOT = Path(__file__).parent.parent
ARCHIVE_DIR = PROJECT_ROOT / "_docs_archive"
DOCS_DIR = PROJECT_ROOT / "docs"


class DryRunPrinter:
    """Handles dry-run vs execute mode output."""
    def __init__(self, dry_run: bool):
        self.dry_run = dry_run
        self.actions = []
    
    def mkdir(self, path: Path):
        self.actions.append(f"CREATE DIR: {path}")
        if not self.dry_run:
            path.mkdir(parents=True, exist_ok=True)
    
    def copy(self, src: Path, dst: Path):
        self.actions.append(f"COPY: {src.relative_to(PROJECT_ROOT)} -> {dst.relative_to(PROJECT_ROOT)}")
        if not self.dry_run:
            dst.parent.mkdir(parents=True, exist_ok=True)
            if src.is_dir():
                shutil.copytree(str(src), str(dst), dirs_exist_ok=True)
            else:
                shutil.copy2(str(src), str(dst))
    
    def delete(self, path: Path):
        self.actions.append(f"DELETE: {path.relative_to(PROJECT_ROOT)}")
        if not self.dry_run:
            if path.is_dir():
                shutil.rmtree(str(path))
            else:
                path.unlink()
    
    def write(self, path: Path, content: str):
        self.actions.append(f"WRITE: {path.relative_to(PROJECT_ROOT)} ({len(content)} bytes)")
        if not self.dry_run:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
    
    def summary(self):
        if self.dry_run:
            print("\n" + "=" * 60)
            print("DRY RUN - No changes made. Actions that WOULD be performed:")
            print("=" * 60)
        else:
            print("\n" + "=" * 60)
            print("EXECUTED - The following actions were performed:")
            print("=" * 60)
        
        for action in self.actions:
            print(f"  {action}")
        
        if self.dry_run:
            print("\nRun with --execute to perform these actions.")


# =============================================================================
# JAVA PARSING FOR DOCUMENTATION GENERATION
# =============================================================================

class JavaClass:
    def __init__(self, filepath: Path):
        self.filepath = filepath
        self.package = ""
        self.name = filepath.stem
        self.type = "class"
        self.extends = None
        self.implements = []
        self.imports = []
        self.public_methods = []


def parse_java_file(filepath: Path) -> Optional[JavaClass]:
    """Parse a Java file and extract basic info."""
    try:
        content = filepath.read_text(encoding='utf-8', errors='replace')
    except:
        return None
    
    jc = JavaClass(filepath)
    
    # Package
    m = re.search(r'package\s+([\w.]+)\s*;', content)
    if m:
        jc.package = m.group(1)
    
    # Imports (only our own)
    jc.imports = [i for i in re.findall(r'import\s+(net\.cyberpunk042[\w.]+)\s*;', content)]
    
    # Class type
    if re.search(r'\binterface\s+\w+', content):
        jc.type = "interface"
    elif re.search(r'\benum\s+\w+', content):
        jc.type = "enum"
    elif re.search(r'\brecord\s+\w+', content):
        jc.type = "record"
    
    # Class name
    m = re.search(r'(?:class|interface|enum|record)\s+(\w+)', content)
    if m:
        jc.name = m.group(1)
    
    # Extends
    m = re.search(r'extends\s+([\w.<>]+)', content)
    if m:
        jc.extends = m.group(1).split('<')[0]
    
    # Implements
    m = re.search(r'implements\s+([\w.<>,\s]+?)(?:\{|extends)', content)
    if m:
        jc.implements = [i.strip().split('<')[0] for i in m.group(1).split(',')]
    
    # Public methods (simplified)
    jc.public_methods = re.findall(r'public\s+(?:\w+\s+)?(\w+)\s*\([^)]*\)\s*(?:throws|\{)', content)
    
    return jc


def scan_source(src_dir: Path) -> List[JavaClass]:
    """Scan all Java files."""
    classes = []
    for f in src_dir.rglob("*.java"):
        jc = parse_java_file(f)
        if jc:
            classes.append(jc)
    return classes


# =============================================================================
# DOCUMENTATION GENERATION
# =============================================================================

def generate_package_overview(classes: List[JavaClass]) -> str:
    """Generate package overview."""
    packages = defaultdict(list)
    for c in classes:
        packages[c.package].append(c)
    
    lines = [
        "# 📦 Package Overview",
        "",
        f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        "",
        f"**{len(classes)} classes** across **{len(packages)} packages**",
        "",
    ]
    
    # Group by top-level
    groups = defaultdict(lambda: defaultdict(list))
    for pkg, cls_list in sorted(packages.items()):
        parts = pkg.split('.')
        top = '.'.join(parts[:4]) if len(parts) >= 4 else pkg
        sub = '.'.join(parts[4:]) if len(parts) > 4 else ""
        groups[top][sub] = cls_list
    
    for top in sorted(groups.keys()):
        lines.append(f"## `{top}`")
        lines.append("")
        
        for sub, cls_list in sorted(groups[top].items()):
            label = f"`.{sub}`" if sub else "(root)"
            icons = {"class": "📄", "interface": "🔌", "enum": "📋", "record": "📝"}
            
            lines.append(f"### {label} ({len(cls_list)} items)")
            for c in sorted(cls_list, key=lambda x: x.name):
                icon = icons.get(c.type, "📄")
                lines.append(f"- {icon} `{c.name}`")
            lines.append("")
    
    return "\n".join(lines)


def generate_class_diagram(classes: List[JavaClass]) -> str:
    """Generate mermaid class diagrams."""
    lines = [
        "# 📊 Class Diagram",
        "",
        f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        "",
    ]
    
    # Generate diagrams for major areas
    areas = [
        ("Visual Rendering", "visual"),
        ("GUI Components", "client.gui"),
        ("Field System", "field"),
    ]
    
    for title, keyword in areas:
        area_classes = [c for c in classes if keyword in c.package]
        if not area_classes:
            continue
        
        lines.append(f"## {title}")
        lines.append("")
        lines.append("```mermaid")
        lines.append("classDiagram")
        
        names = set()
        for c in sorted(area_classes, key=lambda x: x.name)[:40]:
            if c.name in names:
                continue
            names.add(c.name)
            
            annotation = ""
            if c.type == "interface":
                annotation = "<<interface>>"
            elif c.type == "enum":
                annotation = "<<enumeration>>"
            
            lines.append(f"    class {c.name} {annotation}")
        
        # Add relationships
        for c in area_classes[:40]:
            if c.name not in names:
                continue
            if c.extends and c.extends in names:
                lines.append(f"    {c.extends} <|-- {c.name}")
            for impl in c.implements:
                if impl in names:
                    lines.append(f"    {impl} <|.. {c.name}")
        
        lines.append("```")
        lines.append("")
    
    return "\n".join(lines)


def generate_component_graph(classes: List[JavaClass]) -> str:
    """Generate component dependency graph."""
    lines = [
        "# 🔗 Component Graph",
        "",
        f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        "",
        "## Package Dependencies",
        "",
        "```mermaid",
        "graph LR",
    ]
    
    # Analyze dependencies
    deps = defaultdict(set)
    for c in classes:
        src = '.'.join(c.package.split('.')[:4]) if c.package else "unknown"
        for imp in c.imports:
            tgt = '.'.join(imp.split('.')[:4])
            if src != tgt:
                deps[src].add(tgt)
    
    nodes = set()
    for src, tgts in deps.items():
        nodes.add(src)
        nodes.update(tgts)
    
    for node in sorted(nodes):
        short = node.split('.')[-1]
        safe_id = node.replace('.', '_')
        lines.append(f"    {safe_id}[{short}]")
    
    for src, tgts in deps.items():
        src_id = src.replace('.', '_')
        for tgt in tgts:
            tgt_id = tgt.replace('.', '_')
            lines.append(f"    {src_id} --> {tgt_id}")
    
    lines.append("```")
    return "\n".join(lines)


def generate_readme() -> str:
    """Generate main README."""
    return f"""# 📚 Documentation

> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}

## Structure

- `architecture/` - Auto-generated architecture docs
  - `PACKAGE_OVERVIEW.md` - All packages and classes
  - `CLASS_DIAGRAM.md` - Mermaid class diagrams
  - `COMPONENT_GRAPH.md` - Package dependencies

## Regenerating

```bash
python scripts/docs_cleanup.py --generate
```

## Archive

Previous documentation is in `_docs_archive/` (gitignored).
"""


# =============================================================================
# MAIN CLEANUP LOGIC
# =============================================================================

def archive_existing_docs(printer: DryRunPrinter):
    """Archive all existing docs."""
    if not DOCS_DIR.exists():
        print("No docs/ directory to archive.")
        return
    
    # Create archive directory
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    archive_subdir = ARCHIVE_DIR / timestamp
    printer.mkdir(archive_subdir)
    
    # Copy entire docs/ to archive
    printer.copy(DOCS_DIR, archive_subdir / "docs")
    
    # Delete original docs/
    printer.delete(DOCS_DIR)


def generate_fresh_docs(printer: DryRunPrinter):
    """Generate fresh documentation from source."""
    src_dir = PROJECT_ROOT / "src"
    if not src_dir.exists():
        print("No src/ directory found!")
        return
    
    print("Scanning Java source files...")
    classes = scan_source(src_dir)
    print(f"Found {len(classes)} Java classes")
    
    # Create docs structure
    arch_dir = DOCS_DIR / "architecture"
    printer.mkdir(arch_dir)
    
    # Generate files
    print("Generating PACKAGE_OVERVIEW.md...")
    printer.write(arch_dir / "PACKAGE_OVERVIEW.md", generate_package_overview(classes))
    
    print("Generating CLASS_DIAGRAM.md...")
    printer.write(arch_dir / "CLASS_DIAGRAM.md", generate_class_diagram(classes))
    
    print("Generating COMPONENT_GRAPH.md...")
    printer.write(arch_dir / "COMPONENT_GRAPH.md", generate_component_graph(classes))
    
    print("Generating README.md...")
    printer.write(DOCS_DIR / "README.md", generate_readme())


def main():
    parser = argparse.ArgumentParser(description="Documentation cleanup and regeneration")
    parser.add_argument('--dry-run', action='store_true', default=True,
                        help='Preview changes without executing (default)')
    parser.add_argument('--execute', action='store_true',
                        help='Actually perform the cleanup')
    parser.add_argument('--generate', action='store_true',
                        help='Only generate new docs (no archive/delete)')
    args = parser.parse_args()
    
    dry_run = not args.execute
    printer = DryRunPrinter(dry_run)
    
    print("=" * 60)
    print("   DOCUMENTATION CLEANUP SCRIPT")
    print("=" * 60)
    
    if args.generate:
        print("\nMode: GENERATE ONLY (no archive/delete)")
        generate_fresh_docs(printer)
    else:
        print(f"\nMode: {'DRY RUN' if dry_run else 'EXECUTE'}")
        print("\nStep 1: Archive existing docs/")
        archive_existing_docs(printer)
        print("\nStep 2: Generate fresh documentation")
        generate_fresh_docs(printer)
    
    printer.summary()


if __name__ == "__main__":
    main()
