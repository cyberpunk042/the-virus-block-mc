"""
Graph Builder Module
====================
Builds inheritance and dependency graphs from parsed Java classes.
Provides clustering and analysis for documentation structure.
"""

from dataclasses import dataclass, field
from typing import List, Dict, Set, Tuple, Optional
from collections import defaultdict
from pathlib import Path

# Import from sibling module
from .java_parser import JavaClass


@dataclass
class PackageInfo:
    """Information about a package."""
    name: str
    classes: List[JavaClass] = field(default_factory=list)
    
    @property
    def class_count(self) -> int:
        return len(self.classes)
    
    @property
    def interface_count(self) -> int:
        return len([c for c in self.classes if c.is_interface])
    
    @property
    def enum_count(self) -> int:
        return len([c for c in self.classes if c.is_enum])
    
    @property
    def subpackages(self) -> Set[str]:
        """Get immediate subpackages."""
        subs = set()
        for c in self.classes:
            if c.package.startswith(self.name + "."):
                rest = c.package[len(self.name) + 1:]
                first_part = rest.split('.')[0]
                subs.add(f"{self.name}.{first_part}")
        return subs


@dataclass
class InheritanceEdge:
    """Represents an inheritance relationship."""
    child: str  # FQN of child class
    parent: str  # FQN or simple name of parent
    relationship: str  # 'extends' or 'implements'


@dataclass
class DependencyEdge:
    """Represents a dependency (import) relationship."""
    source: str  # FQN of class that imports
    target: str  # FQN of imported class
    
    
class ClassGraph:
    """
    Graph of Java classes with inheritance and dependency relationships.
    """
    
    def __init__(self, classes: List[JavaClass]):
        self.classes = {c.fqn: c for c in classes}
        self.by_name = {c.name: c for c in classes}  # For simple name lookup
        self.by_package = defaultdict(list)
        self.by_domain = defaultdict(list)
        self.by_source_root = defaultdict(list)
        
        # Build indexes
        for c in classes:
            self.by_package[c.package].append(c)
            self.by_domain[c.domain].append(c)
            self.by_source_root[c.source_root].append(c)
        
        # Build graphs
        self._inheritance_edges: List[InheritanceEdge] = []
        self._dependency_edges: List[DependencyEdge] = []
        self._build_graphs()
    
    def _build_graphs(self):
        """Build inheritance and dependency edge lists."""
        for c in self.classes.values():
            # Inheritance
            if c.extends:
                # Try to resolve to FQN
                parent_fqn = self._resolve_type(c.extends, c)
                self._inheritance_edges.append(InheritanceEdge(
                    child=c.fqn,
                    parent=parent_fqn,
                    relationship='extends'
                ))
            
            for impl in c.implements:
                parent_fqn = self._resolve_type(impl, c)
                self._inheritance_edges.append(InheritanceEdge(
                    child=c.fqn,
                    parent=parent_fqn,
                    relationship='implements'
                ))
            
            # Dependencies from imports
            for imp in c.internal_imports:
                # Only include if the target exists in our codebase
                if imp in self.classes:
                    self._dependency_edges.append(DependencyEdge(
                        source=c.fqn,
                        target=imp
                    ))
    
    def _resolve_type(self, type_name: str, context: JavaClass) -> str:
        """
        Try to resolve a type name to FQN using imports.
        """
        # Already FQN?
        if '.' in type_name:
            return type_name
        
        # Check imports
        for imp in context.internal_imports:
            if imp.endswith(f".{type_name}"):
                return imp
        
        # Check same package
        same_pkg = f"{context.package}.{type_name}"
        if same_pkg in self.classes:
            return same_pkg
        
        # Check known classes by simple name
        if type_name in self.by_name:
            return self.by_name[type_name].fqn
        
        # Return as-is (external class)
        return type_name
    
    # === Inheritance queries ===
    
    def get_inheritance_edges(self, package_filter: str = None) -> List[InheritanceEdge]:
        """Get inheritance edges, optionally filtered by package prefix."""
        if package_filter is None:
            return self._inheritance_edges
        
        return [e for e in self._inheritance_edges 
                if e.child.startswith(package_filter) or e.parent.startswith(package_filter)]
    
    def get_parents(self, class_fqn: str) -> List[Tuple[str, str]]:
        """Get all parents of a class as (parent_fqn, relationship) tuples."""
        return [(e.parent, e.relationship) 
                for e in self._inheritance_edges if e.child == class_fqn]
    
    def get_children(self, class_fqn: str) -> List[Tuple[str, str]]:
        """Get all children of a class as (child_fqn, relationship) tuples."""
        # Check both FQN and simple name
        simple_name = class_fqn.split('.')[-1]
        return [(e.child, e.relationship) 
                for e in self._inheritance_edges 
                if e.parent == class_fqn or e.parent == simple_name]
    
    def get_inheritance_tree(self, root_fqn: str, max_depth: int = 10) -> Dict:
        """
        Get inheritance tree starting from a root class.
        Returns nested dict structure.
        """
        if max_depth <= 0:
            return {"name": root_fqn, "children": []}
        
        children = self.get_children(root_fqn)
        return {
            "name": root_fqn,
            "children": [
                self.get_inheritance_tree(child, max_depth - 1) 
                for child, _ in children
            ]
        }
    
    # === Dependency queries ===
    
    def get_dependency_edges(self, package_filter: str = None) -> List[DependencyEdge]:
        """Get dependency edges, optionally filtered by package prefix."""
        if package_filter is None:
            return self._dependency_edges
        
        return [e for e in self._dependency_edges 
                if e.source.startswith(package_filter)]
    
    def get_dependencies(self, class_fqn: str) -> List[str]:
        """Get classes that a given class depends on."""
        return [e.target for e in self._dependency_edges if e.source == class_fqn]
    
    def get_dependents(self, class_fqn: str) -> List[str]:
        """Get classes that depend on a given class."""
        return [e.source for e in self._dependency_edges if e.target == class_fqn]
    
    def get_package_dependencies(self) -> Dict[str, Set[str]]:
        """
        Get package-level dependency graph.
        Returns dict mapping package -> set of packages it depends on.
        """
        pkg_deps = defaultdict(set)
        for edge in self._dependency_edges:
            src_pkg = self.classes[edge.source].package if edge.source in self.classes else ""
            tgt_pkg = self.classes[edge.target].package if edge.target in self.classes else ""
            if src_pkg and tgt_pkg and src_pkg != tgt_pkg:
                pkg_deps[src_pkg].add(tgt_pkg)
        return dict(pkg_deps)
    
    # === Package queries ===
    
    def get_packages(self, prefix: str = None) -> List[PackageInfo]:
        """Get all packages, optionally filtered by prefix."""
        packages = {}
        for pkg_name, classes in self.by_package.items():
            if prefix and not pkg_name.startswith(prefix):
                continue
            packages[pkg_name] = PackageInfo(name=pkg_name, classes=classes)
        return list(packages.values())
    
    def get_package_hierarchy(self, root_package: str = "net.cyberpunk042") -> Dict:
        """
        Get package hierarchy as nested dict.
        """
        hierarchy = {}
        
        for pkg_name in sorted(self.by_package.keys()):
            if not pkg_name.startswith(root_package):
                continue
            
            relative = pkg_name[len(root_package):].lstrip('.')
            parts = relative.split('.') if relative else []
            
            current = hierarchy
            for part in parts:
                if part not in current:
                    current[part] = {"_classes": []}
                current = current[part]
            
            current["_classes"] = [c.name for c in self.by_package[pkg_name]]
        
        return hierarchy
    
    # === Domain queries ===
    
    def get_domains(self) -> List[str]:
        """Get all top-level domains."""
        return sorted(self.by_domain.keys())
    
    def get_domain_classes(self, domain: str) -> List[JavaClass]:
        """Get all classes in a domain."""
        return self.by_domain.get(domain, [])
    
    def get_domain_stats(self) -> Dict[str, Dict]:
        """
        Get statistics for each domain.
        """
        stats = {}
        for domain, classes in self.by_domain.items():
            main_classes = [c for c in classes if c.source_root == 'main']
            client_classes = [c for c in classes if c.source_root == 'client']
            
            stats[domain] = {
                'total': len(classes),
                'main': len(main_classes),
                'client': len(client_classes),
                'interfaces': len([c for c in classes if c.is_interface]),
                'enums': len([c for c in classes if c.is_enum]),
                'subdomains': len(set(c.subdomain for c in classes if c.subdomain)),
            }
        return stats
    
    # === Clustering ===
    
    def cluster_by_package(self, max_per_cluster: int = 30) -> Dict[str, List[JavaClass]]:
        """
        Cluster classes by package, splitting large packages.
        
        Returns dict mapping cluster_name -> classes.
        """
        clusters = {}
        
        for pkg_name, classes in self.by_package.items():
            if len(classes) <= max_per_cluster:
                clusters[pkg_name] = classes
            else:
                # Split by subdomain or alphabetically
                by_subdomain = defaultdict(list)
                for c in classes:
                    sub = c.subdomain if c.subdomain else "_root"
                    by_subdomain[sub].append(c)
                
                if len(by_subdomain) > 1:
                    # Use subdomains
                    for sub, sub_classes in by_subdomain.items():
                        cluster_name = f"{pkg_name}.{sub}" if sub != "_root" else pkg_name
                        clusters[cluster_name] = sub_classes
                else:
                    # Split alphabetically
                    for i in range(0, len(classes), max_per_cluster):
                        chunk = classes[i:i + max_per_cluster]
                        suffix = f"_part{i // max_per_cluster + 1}" if i > 0 else ""
                        clusters[f"{pkg_name}{suffix}"] = chunk
        
        return clusters
    
    # === Statistics ===
    
    def get_stats(self) -> Dict:
        """Get overall statistics."""
        all_classes = list(self.classes.values())
        return {
            'total_classes': len(all_classes),
            'classes': len([c for c in all_classes if c.class_type == 'class']),
            'interfaces': len([c for c in all_classes if c.is_interface]),
            'enums': len([c for c in all_classes if c.is_enum]),
            'abstract_classes': len([c for c in all_classes if c.is_abstract]),
            'packages': len(self.by_package),
            'domains': len(self.by_domain),
            'inheritance_edges': len(self._inheritance_edges),
            'dependency_edges': len(self._dependency_edges),
            'main_classes': len(self.by_source_root.get('main', [])),
            'client_classes': len(self.by_source_root.get('client', [])),
        }


# Convenience function
def build_graph(classes: List[JavaClass]) -> ClassGraph:
    """Build a ClassGraph from a list of JavaClass objects."""
    return ClassGraph(classes)


# Quick test
if __name__ == "__main__":
    from java_parser import scan_project
    from pathlib import Path
    
    project_root = Path(__file__).parent.parent.parent
    classes = scan_project(project_root)
    graph = build_graph(classes)
    
    print("\n=== Statistics ===")
    for key, value in graph.get_stats().items():
        print(f"  {key}: {value}")
    
    print("\n=== Domains ===")
    for domain, stats in graph.get_domain_stats().items():
        print(f"  {domain}: {stats['total']} classes ({stats['main']} main, {stats['client']} client)")
