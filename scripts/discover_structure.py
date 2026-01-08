#!/usr/bin/env python3
"""
Documentation Structure Discovery
=================================
Analyzes the parsed codebase and creates a documentation structure map.

Phase 2 of the documentation generation pipeline.

Output:
- docs/_meta/structure.json - Complete mapping of source to docs
- Console output with recommended structure
"""

import sys
import json
from pathlib import Path
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Optional, Set
from collections import defaultdict

# Add scripts to path
SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass
from lib.graph_builder import build_graph, ClassGraph


# Configuration
MAX_CLASSES_PER_DIAGRAM = 30  # Split if more than this
MAX_LINES_PER_FILE = 600  # Target max lines per md file
LINES_PER_CLASS = 15  # Estimated lines per class in diagram


@dataclass
class DocNode:
    """Represents a documentation node (folder or file)."""
    name: str
    path: str  # Relative path in docs/
    node_type: str  # 'folder' or 'file'
    source_packages: List[str] = field(default_factory=list)
    class_count: int = 0
    estimated_lines: int = 0
    children: List['DocNode'] = field(default_factory=list)
    source_root: str = ""  # 'main', 'client', or 'shared'
    
    def add_child(self, child: 'DocNode'):
        self.children.append(child)
    
    def to_dict(self) -> dict:
        return {
            'name': self.name,
            'path': self.path,
            'type': self.node_type,
            'source_packages': self.source_packages,
            'class_count': self.class_count,
            'estimated_lines': self.estimated_lines,
            'source_root': self.source_root,
            'children': [c.to_dict() for c in self.children]
        }


@dataclass
class DocStructure:
    """Complete documentation structure."""
    root: DocNode
    total_files: int = 0
    total_classes: int = 0
    source_mapping: Dict[str, str] = field(default_factory=dict)  # package -> doc path
    
    def to_dict(self) -> dict:
        return {
            'root': self.root.to_dict(),
            'total_files': self.total_files,
            'total_classes': self.total_classes,
            'source_mapping': self.source_mapping
        }


def get_domain_mapping() -> Dict[str, Dict]:
    """
    Define the documentation structure for each domain.
    Returns dict mapping domain -> config.
    """
    return {
        # High priority - detailed structure
        'field': {
            'folder': 'main/field',
            'title': 'Field System',
            'split_subdomains': True,
            'priority': 1,
        },
        'visual': {
            'folder': 'main/visual', 
            'title': 'Visual Definitions',
            'split_subdomains': True,
            'priority': 1,
        },
        'infection': {
            'folder': 'main/infection',
            'title': 'Infection System',
            'split_subdomains': True,
            'priority': 2,
        },
        'growth': {
            'folder': 'main/growth',
            'title': 'Growth System',
            'split_subdomains': True,
            'priority': 2,
        },
        
        # Client-side
        'client': {
            'folder': 'client',
            'title': 'Client Systems',
            'split_subdomains': True,
            'priority': 1,
        },
        
        # Medium priority - single folder each
        'block': {
            'folder': 'main/block',
            'title': 'Blocks',
            'split_subdomains': False,
            'priority': 2,
        },
        'command': {
            'folder': 'main/command',
            'title': 'Commands',
            'split_subdomains': False,
            'priority': 3,
        },
        'network': {
            'folder': 'main/network',
            'title': 'Networking',
            'split_subdomains': False,
            'priority': 2,
        },
        'entity': {
            'folder': 'main/entity',
            'title': 'Entities',
            'split_subdomains': False,
            'priority': 3,
        },
        
        # Low priority - utilities
        'mixin': {
            'folder': 'shared/mixin',
            'title': 'Mixins',
            'split_subdomains': False,
            'priority': 3,
        },
        'util': {
            'folder': 'shared/util',
            'title': 'Utilities',
            'split_subdomains': False,
            'priority': 3,
        },
        'config': {
            'folder': 'shared/config',
            'title': 'Configuration',
            'split_subdomains': False,
            'priority': 3,
        },
        'log': {
            'folder': 'shared/log',
            'title': 'Logging',
            'split_subdomains': False,
            'priority': 3,
        },
        'registry': {
            'folder': 'shared/registry',
            'title': 'Registries',
            'split_subdomains': False,
            'priority': 3,
        },
        'screen': {
            'folder': 'shared/screen',
            'title': 'Screens',
            'split_subdomains': False,
            'priority': 3,
        },
        'recipe': {
            'folder': 'shared/other',
            'title': 'Other',
            'split_subdomains': False,
            'priority': 4,
        },
        'item': {
            'folder': 'main/item',
            'title': 'Items',
            'split_subdomains': False,
            'priority': 3,
        },
        'collision': {
            'folder': 'main/collision',
            'title': 'Collision',
            'split_subdomains': False,
            'priority': 3,
        },
        'server': {
            'folder': 'main/server',
            'title': 'Server',
            'split_subdomains': False,
            'priority': 3,
        },
        'status': {
            'folder': 'shared/other',
            'title': 'Other',
            'split_subdomains': False,
            'priority': 4,
        },
    }


def analyze_client_structure(graph: ClassGraph) -> Dict[str, List[JavaClass]]:
    """
    Analyze the client domain structure in detail.
    Returns dict mapping subdomain -> classes.
    """
    client_classes = graph.get_domain_classes('client')
    
    # Group by subdomain
    by_subdomain = defaultdict(list)
    for c in client_classes:
        # Get the subdomain (first part after 'client')
        parts = c.relative_package.split('.')
        if len(parts) > 1:
            subdomain = parts[1]  # e.g., 'gui', 'visual', 'render'
        else:
            subdomain = '_root'
        by_subdomain[subdomain].append(c)
    
    return dict(by_subdomain)


def create_subdomain_nodes(
    subdomain: str, 
    classes: List[JavaClass],
    parent_path: str,
    graph: ClassGraph
) -> List[DocNode]:
    """
    Create documentation nodes for a subdomain, splitting if needed.
    """
    nodes = []
    
    # Group by sub-subdomain
    by_sub = defaultdict(list)
    for c in classes:
        parts = c.relative_package.split('.')
        if len(parts) > 2:
            sub = parts[2]
        else:
            sub = '_root'
        by_sub[sub].append(c)
    
    # If few enough classes, single node
    if len(classes) <= MAX_CLASSES_PER_DIAGRAM:
        node = DocNode(
            name=subdomain,
            path=f"{parent_path}/{subdomain}",
            node_type='folder',
            source_packages=[c.package for c in classes],
            class_count=len(classes),
            estimated_lines=len(classes) * LINES_PER_CLASS + 50,
        )
        # Add CLASS_DIAGRAM.md file
        node.add_child(DocNode(
            name='CLASS_DIAGRAM.md',
            path=f"{parent_path}/{subdomain}/CLASS_DIAGRAM.md",
            node_type='file',
            source_packages=[c.package for c in classes],
            class_count=len(classes),
            estimated_lines=len(classes) * LINES_PER_CLASS + 50,
        ))
        nodes.append(node)
    else:
        # Need to split by sub-subdomain
        parent_node = DocNode(
            name=subdomain,
            path=f"{parent_path}/{subdomain}",
            node_type='folder',
            class_count=len(classes),
        )
        
        # Add overview file
        parent_node.add_child(DocNode(
            name='CLASS_DIAGRAM.md',
            path=f"{parent_path}/{subdomain}/CLASS_DIAGRAM.md",
            node_type='file',
            source_packages=list(set(c.package for c in classes)),
            class_count=min(MAX_CLASSES_PER_DIAGRAM, len(classes)),
            estimated_lines=min(MAX_CLASSES_PER_DIAGRAM, len(classes)) * LINES_PER_CLASS + 50,
        ))
        
        # Add sub-folders for each sub-subdomain
        for sub, sub_classes in sorted(by_sub.items()):
            if sub == '_root':
                continue
            if len(sub_classes) >= 5:  # Only create subfolder if substantial
                sub_node = DocNode(
                    name=sub,
                    path=f"{parent_path}/{subdomain}/{sub}",
                    node_type='folder',
                    source_packages=list(set(c.package for c in sub_classes)),
                    class_count=len(sub_classes),
                )
                sub_node.add_child(DocNode(
                    name='CLASS_DIAGRAM.md',
                    path=f"{parent_path}/{subdomain}/{sub}/CLASS_DIAGRAM.md",
                    node_type='file',
                    source_packages=list(set(c.package for c in sub_classes)),
                    class_count=len(sub_classes),
                    estimated_lines=len(sub_classes) * LINES_PER_CLASS + 50,
                ))
                parent_node.add_child(sub_node)
        
        nodes.append(parent_node)
    
    return nodes


def build_doc_structure(graph: ClassGraph) -> DocStructure:
    """
    Build the complete documentation structure from the class graph.
    """
    domain_mapping = get_domain_mapping()
    
    # Root node
    root = DocNode(
        name='docs',
        path='docs',
        node_type='folder',
    )
    
    # Add top-level files
    root.add_child(DocNode(
        name='README.md',
        path='docs/README.md',
        node_type='file',
        estimated_lines=100,
    ))
    root.add_child(DocNode(
        name='ARCHITECTURE.md',
        path='docs/ARCHITECTURE.md',
        node_type='file',
        estimated_lines=200,
    ))
    
    # Create main, client, shared folders
    main_folder = DocNode(name='main', path='docs/main', node_type='folder', source_root='main')
    client_folder = DocNode(name='client', path='docs/client', node_type='folder', source_root='client')
    shared_folder = DocNode(name='shared', path='docs/shared', node_type='folder', source_root='shared')
    
    # Track source mapping
    source_mapping = {}
    total_files = 2  # README + ARCHITECTURE
    total_classes = 0
    
    # Process each domain
    for domain in sorted(graph.get_domains()):
        if domain in ['', 'net']:  # Skip empty/root
            continue
            
        domain_classes = graph.get_domain_classes(domain)
        if not domain_classes:
            continue
        
        config = domain_mapping.get(domain, {
            'folder': f'shared/{domain}',
            'title': domain.title(),
            'split_subdomains': False,
            'priority': 4,
        })
        
        folder_path = f"docs/{config['folder']}"
        
        # Determine parent folder
        if config['folder'].startswith('main/'):
            parent = main_folder
        elif config['folder'].startswith('client/'):
            parent = client_folder
        else:
            parent = shared_folder
        
        # Special handling for 'client' domain (it's nested differently)
        if domain == 'client':
            # Analyze client substructure
            client_subs = analyze_client_structure(graph)
            
            for subdomain, classes in sorted(client_subs.items()):
                if subdomain == '_root' or not classes:
                    continue
                
                sub_nodes = create_subdomain_nodes(
                    subdomain, 
                    classes, 
                    'docs/client',
                    graph
                )
                for node in sub_nodes:
                    client_folder.add_child(node)
                    total_classes += node.class_count
                    
                    # Count files
                    def count_files(n):
                        count = 1 if n.node_type == 'file' else 0
                        for child in n.children:
                            count += count_files(child)
                        return count
                    total_files += count_files(node)
                    
                    # Map packages
                    for pkg in node.source_packages:
                        source_mapping[pkg] = node.path
        
        else:
            # Create domain folder
            domain_node = DocNode(
                name=domain,
                path=folder_path,
                node_type='folder',
                source_packages=list(set(c.package for c in domain_classes)),
                class_count=len(domain_classes),
            )
            
            # Add README
            domain_node.add_child(DocNode(
                name='README.md',
                path=f"{folder_path}/README.md",
                node_type='file',
                estimated_lines=50,
            ))
            total_files += 1
            
            # Add CLASS_DIAGRAM
            domain_node.add_child(DocNode(
                name='CLASS_DIAGRAM.md',
                path=f"{folder_path}/CLASS_DIAGRAM.md",
                node_type='file',
                source_packages=list(set(c.package for c in domain_classes)),
                class_count=min(MAX_CLASSES_PER_DIAGRAM, len(domain_classes)),
                estimated_lines=min(MAX_CLASSES_PER_DIAGRAM, len(domain_classes)) * LINES_PER_CLASS + 100,
            ))
            total_files += 1
            
            # If needs splitting, add subdomain folders
            if config['split_subdomains'] and len(domain_classes) > MAX_CLASSES_PER_DIAGRAM:
                # Group by subdomain
                by_subdomain = defaultdict(list)
                for c in domain_classes:
                    sub = c.subdomain if c.subdomain else '_root'
                    by_subdomain[sub].append(c)
                
                for subdomain, classes in sorted(by_subdomain.items()):
                    if subdomain == '_root' or len(classes) < 3:
                        continue
                    
                    sub_node = DocNode(
                        name=subdomain,
                        path=f"{folder_path}/{subdomain}",
                        node_type='folder',
                        source_packages=list(set(c.package for c in classes)),
                        class_count=len(classes),
                    )
                    sub_node.add_child(DocNode(
                        name='CLASS_DIAGRAM.md',
                        path=f"{folder_path}/{subdomain}/CLASS_DIAGRAM.md",
                        node_type='file',
                        source_packages=list(set(c.package for c in classes)),
                        class_count=len(classes),
                        estimated_lines=len(classes) * LINES_PER_CLASS + 50,
                    ))
                    domain_node.add_child(sub_node)
                    total_files += 1
            
            parent.add_child(domain_node)
            total_classes += len(domain_classes)
            
            # Map packages
            for pkg in domain_node.source_packages:
                source_mapping[pkg] = folder_path
    
    # Add folders to root
    root.add_child(main_folder)
    root.add_child(client_folder)
    root.add_child(shared_folder)
    
    # Add config folder
    config_folder = DocNode(
        name='config',
        path='docs/config',
        node_type='folder',
    )
    config_folder.add_child(DocNode(
        name='README.md',
        path='docs/config/README.md',
        node_type='file',
        estimated_lines=100,
    ))
    root.add_child(config_folder)
    total_files += 1
    
    return DocStructure(
        root=root,
        total_files=total_files,
        total_classes=total_classes,
        source_mapping=source_mapping,
    )


def print_structure(node: DocNode, indent: int = 0):
    """Print structure as tree."""
    prefix = "  " * indent
    icon = "📁" if node.node_type == 'folder' else "📄"
    count = f" ({node.class_count} classes)" if node.class_count > 0 else ""
    lines = f" ~{node.estimated_lines}ln" if node.estimated_lines > 0 and node.node_type == 'file' else ""
    print(f"{prefix}{icon} {node.name}{count}{lines}")
    
    for child in node.children:
        print_structure(child, indent + 1)


def main():
    project_root = SCRIPT_DIR.parent
    
    print("=" * 60)
    print("   DOCUMENTATION STRUCTURE DISCOVERY")
    print("=" * 60)
    
    # Scan and build graph
    print("\n📂 Scanning project...")
    classes = scan_project(project_root)
    graph = build_graph(classes)
    
    print(f"\n📊 Analyzed {len(classes)} classes across {len(graph.get_domains())} domains")
    
    # Build documentation structure
    print("\n🔨 Building documentation structure...")
    structure = build_doc_structure(graph)
    
    # Print structure
    print("\n📁 Proposed Documentation Structure:")
    print("-" * 40)
    print_structure(structure.root)
    
    # Print statistics
    print("\n📈 Statistics:")
    print(f"  Total documentation files: {structure.total_files}")
    print(f"  Total classes covered: {structure.total_classes}")
    print(f"  Source packages mapped: {len(structure.source_mapping)}")
    
    # Save structure to JSON
    meta_dir = project_root / "docs" / "_meta"
    meta_dir.mkdir(parents=True, exist_ok=True)
    
    structure_file = meta_dir / "structure.json"
    with open(structure_file, 'w', encoding='utf-8') as f:
        json.dump(structure.to_dict(), f, indent=2)
    
    print(f"\n✅ Structure saved to: docs/_meta/structure.json")
    
    # Print domain summary
    print("\n📋 Domain Summary:")
    print("-" * 60)
    print(f"{'Domain':<15} {'Main':>6} {'Client':>8} {'Total':>7} {'Doc Path':<25}")
    print("-" * 60)
    
    domain_stats = graph.get_domain_stats()
    domain_mapping = get_domain_mapping()
    
    for domain in sorted(domain_stats.keys()):
        if domain in ['', 'net']:
            continue
        stats = domain_stats[domain]
        config = domain_mapping.get(domain, {'folder': f'shared/{domain}'})
        print(f"{domain:<15} {stats['main']:>6} {stats['client']:>8} {stats['total']:>7} {config['folder']:<25}")
    
    print("\n✅ Phase 2 complete! Ready for Phase 3: Mermaid generation")


if __name__ == "__main__":
    main()
