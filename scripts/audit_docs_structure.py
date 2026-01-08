#!/usr/bin/env python3
"""
Documentation & Agent Tools Structure Audit Script
===================================================
Analyzes the project documentation spread across:
- .agent/
- .gemini/
- agent-tools/
- docs/
- scripts/

Produces a comprehensive audit report with:
1. File inventory by location
2. Potential duplicates (by name similarity)
3. Files by age (last modified)
4. Category suggestions for consolidation
"""

import os
import json
from pathlib import Path
from datetime import datetime
from collections import defaultdict
import hashlib

# Project root
PROJECT_ROOT = Path(__file__).parent.parent

# Directories to audit
AUDIT_DIRS = [
    ".agent",
    ".gemini",
    "agent-tools",
    "docs",
    "scripts",
]

# Also check root for stray files
CHECK_ROOT_EXTENSIONS = [".md", ".txt", ".json", ".log"]

# File categories based on naming patterns
CATEGORY_PATTERNS = {
    "TODO/Tasks": ["todo", "task", "plan", "issue", "fix", "open-"],
    "Architecture": ["architecture", "diagram", "graph", "structure", "tree"],
    "Analysis/Audit": ["audit", "analysis", "report", "review"],
    "Reference/Legacy": ["legacy", "reference", "archive", "old", "backup"],
    "Workflows": ["workflow", "instruction", "readme", "guide", "how"],
    "Session Logs": ["session", "log", ".txt"],
    "Schema/Config": ["schema", "config", "profile"],
    "Code Reference": [".java", ".py", "src-", "javap"],
    "Search Output": ["search", "output", "native", "mapping"],
}


def get_file_hash(filepath: Path, chunk_size: int = 8192) -> str:
    """Get MD5 hash of first 8KB of file for quick duplicate detection."""
    hasher = hashlib.md5()
    try:
        with open(filepath, "rb") as f:
            chunk = f.read(chunk_size)
            hasher.update(chunk)
    except Exception:
        return ""
    return hasher.hexdigest()


def categorize_file(filename: str) -> str:
    """Categorize a file based on naming patterns."""
    lower = filename.lower()
    for category, patterns in CATEGORY_PATTERNS.items():
        if any(p in lower for p in patterns):
            return category
    return "Uncategorized"


def scan_directory(dir_path: Path) -> list:
    """Recursively scan directory and collect file info."""
    files = []
    if not dir_path.exists():
        return files
    
    try:
        items = list(dir_path.rglob("*"))
    except Exception as e:
        print(f"  Warning: Could not fully scan {dir_path}: {e}")
        items = []
    
    for item in items:
        if item.is_file():
            try:
                stat = item.stat()
                rel_path = item.relative_to(PROJECT_ROOT)
                files.append({
                    "path": str(rel_path),
                    "name": item.name,
                    "size_kb": round(stat.st_size / 1024, 2),
                    "modified": datetime.fromtimestamp(stat.st_mtime),
                    "category": categorize_file(item.name),
                    "hash": get_file_hash(item) if stat.st_size < 10_000_000 else None,
                    "extension": item.suffix.lower(),
                    "parent_dir": str(rel_path.parent),
                })
            except Exception as e:
                print(f"Error scanning {item}: {e}")
    return files


def find_potential_duplicates(files: list) -> dict:
    """Find files with same hash or similar names."""
    hash_groups = defaultdict(list)
    name_groups = defaultdict(list)
    
    for f in files:
        if f["hash"]:
            hash_groups[f["hash"]].append(f["path"])
        # Group by base name without extension
        base_name = Path(f["name"]).stem.lower()
        name_groups[base_name].append(f["path"])
    
    duplicates = {
        "by_content": {k: v for k, v in hash_groups.items() if len(v) > 1},
        "by_name": {k: v for k, v in name_groups.items() if len(v) > 1},
    }
    return duplicates


def analyze_root_strays(root: Path) -> list:
    """Find stray files in project root that should probably be elsewhere."""
    strays = []
    for item in root.iterdir():
        if item.is_file():
            if item.suffix.lower() in CHECK_ROOT_EXTENSIONS or item.suffix == "":
                # Exclude standard files
                if item.name.lower() not in ["readme.md", "license", "license.md", ".gitignore", ".gitattributes"]:
                    strays.append({
                        "name": item.name,
                        "size_kb": round(item.stat().st_size / 1024, 2),
                        "suggestion": "Move to docs/ or delete if obsolete"
                    })
    return strays


def generate_report(all_files: list, duplicates: dict, strays: list) -> str:
    """Generate a markdown audit report."""
    report = []
    report.append("# 📊 Documentation & Agent Tools Audit Report")
    report.append(f"\n**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    # Summary stats
    report.append("## 📈 Summary Statistics\n")
    by_dir = defaultdict(lambda: {"count": 0, "size_kb": 0})
    for f in all_files:
        top_dir = f["path"].split("\\")[0].split("/")[0]
        by_dir[top_dir]["count"] += 1
        by_dir[top_dir]["size_kb"] += f["size_kb"]
    
    report.append("| Location | File Count | Total Size (KB) |")
    report.append("|----------|------------|-----------------|")
    for loc, stats in sorted(by_dir.items()):
        report.append(f"| `{loc}` | {stats['count']} | {stats['size_kb']:.1f} |")
    report.append(f"| **TOTAL** | **{len(all_files)}** | **{sum(f['size_kb'] for f in all_files):.1f}** |")
    
    # Root stray files (urgent!)
    if strays:
        report.append("\n## ⚠️ URGENT: Stray Files in Project Root\n")
        report.append("These files appear to be misplaced or accidentally created:\n")
        for s in strays:
            report.append(f"- `{s['name']}` ({s['size_kb']:.1f} KB) - {s['suggestion']}")
    
    # Files by category
    report.append("\n## 📁 Files by Category\n")
    by_category = defaultdict(list)
    for f in all_files:
        by_category[f["category"]].append(f)
    
    for cat in sorted(by_category.keys()):
        files = by_category[cat]
        report.append(f"\n### {cat} ({len(files)} files)\n")
        report.append("<details>")
        report.append("<summary>Click to expand</summary>\n")
        report.append("| File | Location | Size (KB) | Last Modified |")
        report.append("|------|----------|-----------|---------------|")
        for f in sorted(files, key=lambda x: x["modified"], reverse=True)[:50]:
            report.append(f"| `{f['name']}` | `{f['parent_dir']}` | {f['size_kb']:.1f} | {f['modified'].strftime('%Y-%m-%d')} |")
        if len(files) > 50:
            report.append(f"\n*...and {len(files) - 50} more files*")
        report.append("\n</details>")
    
    # Potential duplicates
    if duplicates["by_content"] or duplicates["by_name"]:
        report.append("\n## 🔄 Potential Duplicates\n")
        
        if duplicates["by_content"]:
            report.append("### Same Content (by hash)\n")
            for hash_val, paths in list(duplicates["by_content"].items())[:10]:
                report.append(f"- Hash `{hash_val[:8]}...`:")
                for p in paths:
                    report.append(f"  - `{p}`")
        
        if duplicates["by_name"]:
            report.append("\n### Similar Names\n")
            for name, paths in list(duplicates["by_name"].items())[:20]:
                if len(paths) <= 5:  # Skip very common names
                    report.append(f"- **{name}**:")
                    for p in paths:
                        report.append(f"  - `{p}`")
    
    # Oldest files (potentially outdated)
    report.append("\n## 📅 Potentially Outdated Files (Oldest 30)\n")
    report.append("| File | Location | Last Modified | Age (Days) |")
    report.append("|------|----------|---------------|------------|")
    sorted_by_age = sorted(all_files, key=lambda x: x["modified"])
    now = datetime.now()
    for f in sorted_by_age[:30]:
        age_days = (now - f["modified"]).days
        report.append(f"| `{f['name']}` | `{f['parent_dir']}` | {f['modified'].strftime('%Y-%m-%d')} | {age_days} |")
    
    # Large files (may be generated/obsolete)
    report.append("\n## 📦 Large Files (>500KB)\n")
    report.append("*These may be generated outputs that shouldn't be in version control*\n")
    report.append("| File | Location | Size (KB) |")
    report.append("|------|----------|-----------|")
    large_files = [f for f in all_files if f["size_kb"] > 500]
    for f in sorted(large_files, key=lambda x: x["size_kb"], reverse=True):
        report.append(f"| `{f['name']}` | `{f['parent_dir']}` | {f['size_kb']:.1f} |")
    
    # Recommendations
    report.append("\n## 💡 Consolidation Recommendations\n")
    report.append("""
### Proposed Structure

```
📁 .agent/                    (AI agent configuration & workflows)
├── workflows/                ✅ Keep - active workflow definitions
├── tasks/                    → Consider: move completed to docs/archive/
├── analysis/                 → Consider: merge with docs/analysis/
└── work/                     → Review: temp work files?

📁 docs/                      (All project documentation)
├── architecture/             (System architecture docs)
├── api/                      (API documentation)
├── guides/                   (How-to guides)
├── analysis/                 (Technical analysis)
├── archive/                  (Old/historical docs)
└── field-system/             (Field system specific docs)

📁 scripts/                   (Python tools - KEEP)
├── lib/                      (Shared utilities)
└── *.py                      (All audit/analysis scripts)

📁 agent-tools/               ⚠️ REVIEW HEAVILY
├── config-schema/            → Move to: docs/schema/
├── legacy-*/                 → Move to: docs/archive/legacy/
├── mappings/                 → Move to: docs/reference/mappings/
├── session-logs/             → DELETE (20MB+ of session data!)
└── *.txt                     → Review each, move to docs/ or delete
```

### High Priority Actions

1. **Delete/Gitignore session-logs/** - 20MB+ of session transcripts
2. **Clean up root directory** - Remove accidentally created files
3. **Consolidate agent-tools/** - Most content belongs in docs/
4. **Merge .gemini/ into .agent/** - Or move its content to docs/analysis/
5. **Archive old docs** - Move outdated docs to docs/archive/
""")
    
    # Location overlap analysis
    report.append("\n## 🔍 Location Overlap Analysis\n")
    report.append("Files that exist in multiple 'documentation' locations:\n")
    
    # Check for README files
    readmes = [f for f in all_files if "readme" in f["name"].lower()]
    if readmes:
        report.append("\n### README files found:\n")
        for f in readmes:
            report.append(f"- `{f['path']}`")
    
    return "\n".join(report)


def main():
    print("🔍 Starting documentation audit...")
    
    all_files = []
    
    # Scan each audit directory
    for dir_name in AUDIT_DIRS:
        dir_path = PROJECT_ROOT / dir_name
        print(f"  Scanning {dir_name}/...")
        files = scan_directory(dir_path)
        all_files.extend(files)
        print(f"    Found {len(files)} files")
    
    # Check for stray files in root
    print("  Checking project root for stray files...")
    strays = analyze_root_strays(PROJECT_ROOT)
    print(f"    Found {len(strays)} stray files")
    
    # Find duplicates
    print("  Analyzing for duplicates...")
    duplicates = find_potential_duplicates(all_files)
    
    # Generate report
    print("  Generating report...")
    report = generate_report(all_files, duplicates, strays)
    
    # Write report
    report_path = PROJECT_ROOT / "docs" / "DOCUMENTATION_AUDIT_REPORT.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report)
    
    print(f"\n✅ Audit complete! Report saved to: {report_path}")
    print(f"   Total files analyzed: {len(all_files)}")
    
    # Also output summary to console
    print("\n📊 Quick Summary:")
    print(f"   - Stray files in root: {len(strays)}")
    print(f"   - Potential content duplicates: {len(duplicates['by_content'])}")
    print(f"   - Similar named files: {len(duplicates['by_name'])}")


if __name__ == "__main__":
    main()
