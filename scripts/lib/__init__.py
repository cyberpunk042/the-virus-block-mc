# Scripts Library
# ===============
# Modular tools for code analysis and documentation generation

# Documentation generation modules
from .java_parser import JavaClass, JavaMethod, JavaField
from .java_parser import parse_java_file, scan_source_directory, scan_project
from .java_parser import sanitize_for_mermaid, is_valid_java_identifier, extract_javadoc
from .graph_builder import ClassGraph, PackageInfo, InheritanceEdge, DependencyEdge
from .graph_builder import build_graph
from .mermaid_generator import MermaidGenerator, DiagramConfig
from .mermaid_generator import generate_class_diagram_md, generate_readme_md

