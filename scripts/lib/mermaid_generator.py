"""
Mermaid UML Class Diagram Generator
====================================
Generates proper UML-style class diagrams with:
- Methods inside class boxes
- Fields/attributes inside class boxes  
- Composition and dependency relationships
- Focused diagrams around key classes
"""

import re
from dataclasses import dataclass
from typing import List, Dict, Set, Optional, Tuple
from collections import defaultdict
from datetime import datetime

from .java_parser import JavaClass, JavaMethod, sanitize_for_mermaid, is_valid_java_identifier


@dataclass
class DiagramConfig:
    """Configuration for diagram generation."""
    max_classes_per_diagram: int = 30
    max_methods_per_class: int = 6      # Methods to show inside box
    max_fields_per_class: int = 4       # Fields to show inside box
    show_all_methods: bool = False      # Show all methods (not just public)
    include_external: bool = True       # Include external base classes
    show_composition: bool = True       # Show "uses" relationships
    show_javadoc: bool = True           # Show javadoc in details section


class MermaidGenerator:
    """Generates proper UML-style Mermaid class diagrams."""
    
    def __init__(self, classes: List[JavaClass], config: DiagramConfig = None):
        self.classes = [c for c in classes if is_valid_java_identifier(c.name)]
        self.config = config or DiagramConfig()
        
        # Build lookup tables
        self.by_name = {c.name: c for c in self.classes}
        self.by_fqn = {c.fqn: c for c in self.classes}
        self.by_package = defaultdict(list)
        for c in self.classes:
            self.by_package[c.package].append(c)
    
    def _sanitize(self, name: str) -> str:
        """Sanitize a name for Mermaid."""
        return sanitize_for_mermaid(name)
    
    def _format_method(self, method: JavaMethod) -> str:
        """Format a method for display in class box."""
        visibility = "+" if "public" in method.modifiers else "-"
        params = "..."  if method.parameters else ""
        ret = f" {self._sanitize(method.return_type)}" if method.return_type else ""
        return f"{visibility}{method.name}({params}){ret}"
    
    def _get_stereotype(self, jc: JavaClass) -> str:
        """Get UML stereotype for class."""
        if jc.class_type == 'interface':
            return '<<interface>>'
        elif jc.class_type == 'enum':
            return '<<enumeration>>'
        elif jc.class_type == 'record':
            return '<<record>>'
        elif jc.is_abstract:
            return '<<abstract>>'
        return ''
    
    def generate_class_diagram(
        self, 
        classes: List[JavaClass] = None,
        title: str = "Class Diagram",
        show_methods: bool = True,
        show_fields: bool = True,
        show_inheritance: bool = True,
        show_dependencies: bool = True
    ) -> str:
        """
        Generate a proper UML-style Mermaid class diagram.
        """
        if classes is None:
            classes = self.classes
        
        # Filter valid classes
        classes = [c for c in classes if is_valid_java_identifier(c.name)]
        
        if len(classes) > self.config.max_classes_per_diagram:
            classes = classes[:self.config.max_classes_per_diagram]
        
        lines = []
        lines.append("```mermaid")
        lines.append("classDiagram")
        
        defined = set()
        relationships = []
        
        # FIRST PASS: Collect all class names that will have relationships
        connected_classes = set()
        class_names = {self._sanitize(c.name) for c in classes}
        
        for jc in classes:
            name = self._sanitize(jc.name)
            if not name:
                continue
            # Check if this class has inheritance
            if jc.extends or jc.implements:
                connected_classes.add(name)
                if jc.extends:
                    connected_classes.add(self._sanitize(jc.extends.split('<')[0]))
                for impl in jc.implements:
                    connected_classes.add(self._sanitize(impl.split('<')[0]))
            
            # Check fields
            for field in jc.fields:
                if field.type:
                    ftype = self._sanitize(field.type.split('<')[0].split('[')[0])
                    if ftype in class_names:
                        connected_classes.add(name)
                        connected_classes.add(ftype)
            
            # Check method return types
            for method in jc.methods:
                if method.return_type:
                    rtype = self._sanitize(method.return_type.split('<')[0])
                    if rtype in class_names:
                        connected_classes.add(name)
                        connected_classes.add(rtype)
            
            # If class has any methods or fields, it's interesting on its own
            if jc.public_methods or jc.fields:
                connected_classes.add(name)
        
        # SECOND PASS: Define each class with members (only if connected)
        for jc in classes:
            name = self._sanitize(jc.name)
            if name in defined or not name:
                continue
            # Skip annotations - they're metadata and always appear standalone
            if jc.class_type == 'annotation':
                continue
            # Skip classes that have no relationships
            if name not in connected_classes:
                continue
            defined.add(name)
            
            stereotype = self._get_stereotype(jc)
            methods_to_show = jc.public_methods[:self.config.max_methods_per_class] if show_methods else []
            fields_to_show = [f for f in jc.fields if 'public' in f.modifiers][:self.config.max_fields_per_class] if show_fields else []
            
            # Class with body
            if stereotype or methods_to_show or fields_to_show:
                lines.append(f"    class {name} {{")
                if stereotype:
                    lines.append(f"        {stereotype}")
                
                # Fields first (UML convention)
                for field in fields_to_show:
                    vis = "+" if "public" in field.modifiers else "-"
                    ftype = self._sanitize(field.type.split('<')[0]) if field.type else ""
                    lines.append(f"        {vis}{field.name}: {ftype}")
                
                # Then methods
                for method in methods_to_show:
                    lines.append(f"        {self._format_method(method)}")
                
                lines.append("    }")
            else:
                lines.append(f"    class {name}")
        
        # Add inheritance
        if show_inheritance:
            for jc in classes:
                child = self._sanitize(jc.name)
                if not child:
                    continue
                
                # Extends
                if jc.extends:
                    parent = self._sanitize(jc.extends)
                    if parent and parent != "Unknown":
                        if parent not in defined and self.config.include_external:
                            lines.append(f"    class {parent}")
                            defined.add(parent)
                        if parent in defined:
                            relationships.append(f"    {parent} <|-- {child}")
                
                # Implements
                for impl in jc.implements:
                    impl_name = self._sanitize(impl)
                    if impl_name and impl_name != "Unknown":
                        if impl_name not in defined and self.config.include_external:
                            lines.append(f"    class {impl_name} {{\n        <<interface>>\n    }}")
                            defined.add(impl_name)
                        if impl_name in defined:
                            relationships.append(f"    {impl_name} <|.. {child}")
        
        # Add composition/dependency relationships (limit per class)
        if show_dependencies and self.config.show_composition:
            MAX_DEPS_PER_CLASS = 4  # Limit to avoid clutter
            # Skip Java stdlib and common Minecraft types
            SKIP_TYPES = {
                # Java primitives and wrappers
                'String', 'boolean', 'int', 'float', 'long', 'double', 'void', 'byte', 'char', 'short',
                'Boolean', 'Integer', 'Float', 'Long', 'Double', 'Byte', 'Character', 'Short',
                # Java collections/util
                'Object', 'Optional', 'Consumer', 'Function', 'Supplier', 'Predicate', 'Runnable',
                'List', 'Map', 'Set', 'Collection', 'Iterator', 'Iterable', 'Stream',
                'UUID', 'Path', 'File', 'URI', 'URL', 'Class', 'Enum',
                'AtomicLong', 'AtomicInteger', 'AtomicBoolean', 'AtomicReference',
                # Gson
                'Gson', 'JsonObject', 'JsonArray', 'JsonElement', 'JsonPrimitive',
                # Minecraft common
                'ServerWorld', 'World', 'ClientWorld', 'MinecraftClient', 'MinecraftServer',
                'BlockPos', 'Vec3d', 'Vec3i', 'Box', 'Direction', 'Identifier',
                'Text', 'MutableText', 'NbtCompound', 'NbtElement',
                'Entity', 'PlayerEntity', 'LivingEntity', 'BlockEntity',
                'ItemStack', 'Item', 'Block', 'BlockState',
                'ResourceManager', 'Registry',
                # GUI widgets
                'ButtonWidget', 'LabeledSlider', 'SliderWidget', 'TextFieldWidget', 'ClickableWidget',
            }
            
            for jc in classes:
                src = self._sanitize(jc.name)
                if not src:
                    continue
                
                # Collect types from fields AND methods
                types_found = []
                
                # From fields
                for field in jc.fields:
                    if field.type:
                        types_found.append((field.type, field.name))
                
                # From method return types and parameters
                for method in jc.methods:
                    if method.return_type and method.return_type not in ['void', 'boolean', 'int', 'float', 'String', 'long', 'double']:
                        types_found.append((method.return_type, "returns"))
                    
                    for param in method.parameters:
                        if not param:
                            continue
                        # Just add the param as-is
                        types_found.append((param, "uses"))
                
                deps_added = 0
                for raw_type, label in types_found:
                    if deps_added >= MAX_DEPS_PER_CLASS:
                        break
                    if not raw_type:
                        continue
                    
                    # Extract types to check (including generic inner types)
                    types_to_check = []
                    
                    # Get outer type
                    outer = raw_type.split('<')[0].split('[')[0].strip()
                    if outer not in ['List', 'Map', 'Set', 'Optional', 'Collection']:
                        types_to_check.append(outer)
                    
                    # Get inner generic types like List<TriggerConfig> -> TriggerConfig
                    if '<' in raw_type and '>' in raw_type:
                        inner = raw_type.split('<')[1].split('>')[0]
                        # Handle Map<K,V> -> just get first significant type
                        for part in inner.replace(',', ' ').split():
                            clean = part.strip().split('<')[0]
                            if clean and clean[0].isupper():
                                types_to_check.append(clean)
                    
                    for field_type in types_to_check:
                        field_type = self._sanitize(field_type)
                        if field_type in SKIP_TYPES:
                            continue
                        if not field_type or field_type == src:
                            continue
                        
                        # If target not yet defined but we want external
                        if field_type not in defined and self.config.include_external:
                            # Always add to defined so connections work
                            defined.add(field_type)
                            
                            # But only create stub LINE if name looks valid (not malformed)
                            # Malformed patterns: primitive+name like "floatradius" or Class+var like "FieldDefinitiondefinition"
                            is_malformed = False
                            
                            # Pattern 1: starts with primitive type
                            for prefix in ['float', 'int', 'long', 'double', 'boolean', 'byte', 'char', 'short', 'String', 
                                          'Identifier', 'ServerWorld', 'ResourceManager', 'JsonObject', 'Path', 'Feature',
                                          'GuiMode', 'FieldDefinition', 'FieldLoader', 'FieldType', 'FieldEvent',
                                          'Vec3d', 'Vec3i', 'BlockPos', 'Modifiers', 'Trigger', 'Entity',
                                          'UUID', 'Nullable', 'Optional', 'Consumer', 'Function', 'Supplier']:
                                if field_type.startswith(prefix) and len(field_type) > len(prefix):
                                    # Check if next char is lowercase (variable name continuing)
                                    if field_type[len(prefix)].islower():
                                        is_malformed = True
                                        break
                            
                            # Pattern 2: check lowercase version for primitives too
                            if not is_malformed:
                                lower_name = field_type.lower()
                                for prefix in ['float', 'int', 'long', 'double', 'boolean', 'byte', 'char', 'short', 'string']:
                                    if lower_name.startswith(prefix) and len(field_type) > len(prefix):
                                        is_malformed = True
                                        break
                            
                            # Pattern 3: single uppercase letter followed by lowercase = like "TdefaultValue"
                            if not is_malformed and len(field_type) >= 2:
                                if field_type[0].isupper() and field_type[1].islower() and len(field_type) > 2:
                                    # Could be valid CamelCase or malformed T+varname
                                    # Check if first "word" is just one letter
                                    if field_type[0] in 'TUVKRE':  # Common generic type params
                                        is_malformed = True
                            
                            # Pattern 3: detect ClassName+varname - if last word segment is all lowercase and not the whole name
                            # e.g., "FieldTypetype" -> ends with lowercase segment "type" after "FieldType"
                            if not is_malformed and len(field_type) > 4:
                                # Find where lowercase starts at the end
                                i = len(field_type) - 1
                                while i > 0 and field_type[i].islower():
                                    i -= 1
                                # i is now at last uppercase or start
                                end_segment = field_type[i:]
                                # If there's a lowercase segment at end AND it looks like varname (not just CamelCase)
                                if len(end_segment) > 2 and field_type[i].isupper() and i > 0 and field_type[i-1].islower():
                                    is_malformed = True
                            
                            if not is_malformed:
                                lines.append(f"    class {field_type}")
                        
                        # Only draw relationship if target is valid (not malformed name)
                        # Mermaid will auto-create boxes from relationships, so we need to skip them too
                        if field_type in defined:
                            # Re-check if it's malformed - skip relationship if target is malformed
                            skip_rel = False
                            for prefix in ['float', 'int', 'long', 'double', 'boolean', 'byte', 'char', 'short', 'String', 
                                          'Identifier', 'ServerWorld', 'ResourceManager', 'JsonObject', 'Path', 'Feature',
                                          'GuiMode', 'FieldDefinition', 'FieldLoader', 'FieldType', 'FieldEvent',
                                          'Vec3d', 'Vec3i', 'BlockPos', 'Modifiers', 'Trigger', 'Entity',
                                          'UUID', 'Nullable', 'Optional', 'Consumer', 'Function', 'Supplier']:
                                if field_type.startswith(prefix) and len(field_type) > len(prefix) and field_type[len(prefix)].islower():
                                    skip_rel = True
                                    break
                            
                            if not skip_rel:
                                rel = f"    {src} --> {field_type} : {label}"
                                if rel not in relationships:
                                    relationships.append(rel)
                                    deps_added += 1
                                    break  # Only one link per source type
        
        # Add relationships (deduplicated)
        for rel in sorted(set(relationships)):
            lines.append(rel)
        
        lines.append("```")
        return '\n'.join(lines)
    
    def generate_focused_diagram(
        self,
        focus_class: JavaClass,
        depth: int = 1
    ) -> str:
        """
        Generate a diagram focused on one class showing:
        - The class itself (detailed)
        - Its parent(s)
        - Classes it uses (composition)
        - Classes that extend it
        """
        related = set()
        related.add(focus_class.name)
        
        # Add parent
        if focus_class.extends:
            parent_name = self._sanitize(focus_class.extends)
            related.add(parent_name)
        
        # Add interfaces
        for impl in focus_class.implements:
            related.add(self._sanitize(impl))
        
        # Add field types (composition)
        for field in focus_class.fields:
            ftype = self._sanitize(field.type.split('<')[0]) if field.type else ""
            if ftype and ftype in self.by_name:
                related.add(ftype)
        
        # Add subclasses
        for jc in self.classes:
            if jc.extends and self._sanitize(jc.extends) == focus_class.name:
                related.add(jc.name)
        
        # Get actual class objects
        classes_to_show = [self.by_name[n] for n in related if n in self.by_name]
        # Add focus class if not already there
        if focus_class not in classes_to_show:
            classes_to_show.insert(0, focus_class)
        
        return self.generate_class_diagram(classes_to_show)
    
    def generate_inheritance_hierarchy(
        self,
        root_class_name: str = None
    ) -> str:
        """Generate a clean inheritance hierarchy diagram."""
        lines = []
        lines.append("```mermaid")
        lines.append("graph TD")
        
        defined = set()
        
        # Find classes with inheritance
        for jc in self.classes:
            child = self._sanitize(jc.name)
            
            if jc.extends:
                parent = self._sanitize(jc.extends)
                if parent and child:
                    if parent not in defined:
                        stype = "([interface])" if "interface" in parent.lower() else f"[{parent}]"
                        lines.append(f"    {parent}{stype}")
                        defined.add(parent)
                    if child not in defined:
                        stype = self._get_stereotype(jc)
                        label = f"{child}<br/>{stype}" if stype else child
                        lines.append(f"    {child}[{label}]")
                        defined.add(child)
                    lines.append(f"    {parent} --> {child}")
            
            # Implements
            for impl in jc.implements:
                impl_name = self._sanitize(impl)
                if impl_name and child:
                    if impl_name not in defined:
                        lines.append(f"    {impl_name}([{impl_name}])")
                        defined.add(impl_name)
                    if child not in defined:
                        lines.append(f"    {child}[{child}]")
                        defined.add(child)
                    lines.append(f"    {impl_name} -.-> {child}")
        
        lines.append("```")
        return '\n'.join(lines)
    
    def generate_domain_overview(self) -> str:
        """Generate high-level domain overview."""
        domains = defaultdict(list)
        for jc in self.classes:
            domains[jc.domain].append(jc)
        
        lines = []
        lines.append("```mermaid")
        lines.append("graph TB")
        
        lines.append("    subgraph main[Main/Server]")
        for domain in sorted(domains.keys()):
            if domain in ['client', 'mixin', '', 'net']:
                continue
            count = len([c for c in domains[domain] if c.source_root == 'main'])
            if count > 0:
                lines.append(f"        {domain}[{domain}<br/>{count}]")
        lines.append("    end")
        
        lines.append("    subgraph client[Client]")
        client_classes = domains.get('client', [])
        by_sub = defaultdict(int)
        for c in client_classes:
            by_sub[c.subdomain or 'other'] += 1
        for sub, count in sorted(by_sub.items()):
            lines.append(f"        client_{sub}[{sub}<br/>{count}]")
        lines.append("    end")
        
        lines.append("```")
        return '\n'.join(lines)


def generate_class_diagram_md(
    classes: List[JavaClass],
    title: str = "Class Diagram",
    description: str = "",
    config: DiagramConfig = None
) -> str:
    """
    Generate a complete markdown file with UML-style class diagram.
    """
    config = config or DiagramConfig()
    
    # Filter valid classes
    valid_classes = [c for c in classes if is_valid_java_identifier(c.name)]
    
    if not valid_classes:
        return f"# {title}\n\n> No valid classes found.\n"
    
    generator = MermaidGenerator(valid_classes, config)
    
    lines = []
    lines.append(f"# {title}")
    lines.append("")
    lines.append(f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    lines.append("")
    
    if description:
        lines.append(description)
        lines.append("")
    
    # Stats
    lines.append("## Overview")
    lines.append("")
    lines.append(f"| Classes | Interfaces | Enums | Records |")
    lines.append(f"|---------|------------|-------|---------|")
    lines.append(f"| {len([c for c in valid_classes if c.class_type == 'class'])} | {len([c for c in valid_classes if c.is_interface])} | {len([c for c in valid_classes if c.is_enum])} | {len([c for c in valid_classes if c.class_type == 'record'])} |")
    lines.append("")
    
    # Main class diagram with methods inside boxes
    lines.append("## Class Diagram")
    lines.append("")
    lines.append(generator.generate_class_diagram(
        valid_classes,
        show_methods=True,
        show_fields=True,
        show_inheritance=True,
        show_dependencies=True
    ))
    lines.append("")
    
    # Inheritance hierarchy (simpler view)
    if len(valid_classes) <= 30:
        lines.append("## Inheritance Hierarchy")
        lines.append("")
        lines.append(generator.generate_inheritance_hierarchy())
        lines.append("")
    
    return '\n'.join(lines)


def generate_readme_md(
    title: str,
    description: str,
    subdirs: List[Tuple[str, str, int]],
    key_classes: List[JavaClass] = None
) -> str:
    """Generate a README.md for a documentation folder."""
    lines = []
    lines.append(f"# {title}")
    lines.append("")
    lines.append(f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    lines.append("")
    lines.append(description)
    lines.append("")
    
    if subdirs:
        lines.append("## Packages")
        lines.append("")
        lines.append("| Package | Classes |")
        lines.append("|---------|---------|")
        for name, desc, count in subdirs:
            lines.append(f"| [{name}/](./{name}/) | {count} |")
        lines.append("")
    
    if key_classes:
        lines.append("## Key Classes")
        lines.append("")
        for jc in key_classes[:8]:
            extends_info = f" extends `{jc.extends}`" if jc.extends else ""
            lines.append(f"- **`{jc.name}`** ({jc.class_type}){extends_info}")
        lines.append("")
    
    lines.append("---")
    lines.append("[CLASS_DIAGRAM.md](./CLASS_DIAGRAM.md)")
    lines.append("")
    
    return '\n'.join(lines)
