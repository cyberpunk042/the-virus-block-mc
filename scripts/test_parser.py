#!/usr/bin/env python3
"""
Test Java Parser
================
Quick test to verify the java parsing works on the project.
Run from WSL: python3 scripts/test_parser.py
"""

import sys
from pathlib import Path

# Add scripts to path
SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project
from lib.graph_builder import build_graph

def main():
    project_root = SCRIPT_DIR.parent
    
    print("=" * 60)
    print("   JAVA PARSER TEST")
    print("=" * 60)
    
    # Scan project
    print("\n📂 Scanning project...")
    classes = scan_project(project_root)
    
    if not classes:
        print("❌ No classes found! Check paths.")
        return
    
    # Build graph
    print("\n📊 Building graph...")
    graph = build_graph(classes)
    
    # Print statistics
    print("\n=== Overall Statistics ===")
    stats = graph.get_stats()
    for key, value in stats.items():
        print(f"  {key}: {value}")
    
    # Print domains
    print("\n=== Domains ===")
    domain_stats = graph.get_domain_stats()
    for domain in sorted(domain_stats.keys()):
        s = domain_stats[domain]
        print(f"  {domain:20} | {s['total']:4} total | {s['main']:4} main | {s['client']:4} client | {s['subdomains']} subdomains")
    
    # Sample: show a few classes from main domain
    print("\n=== Sample Classes (field domain) ===")
    field_classes = graph.get_domain_classes('field')[:10]
    for c in field_classes:
        print(f"  {c.class_type:10} {c.name:30} | extends: {c.extends or '-':20} | methods: {len(c.public_methods)}")
    
    # Sample: inheritance edges
    print("\n=== Sample Inheritance (first 10) ===")
    for edge in graph.get_inheritance_edges()[:10]:
        print(f"  {edge.child.split('.')[-1]:30} --{edge.relationship}--> {edge.parent.split('.')[-1]}")
    
    # Package hierarchy sample
    print("\n=== Package Hierarchy (visual domain) ===")
    packages = graph.get_packages(prefix="net.cyberpunk042.visual")
    for pkg in sorted(packages, key=lambda p: p.name):
        print(f"  {pkg.name}: {pkg.class_count} classes")
    
    print("\n✅ Parser test complete!")
    print(f"   Ready for Phase 2: Documentation structure discovery")


if __name__ == "__main__":
    main()
