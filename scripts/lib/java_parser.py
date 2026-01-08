"""
Robust Java Parser Module
=========================
Multi-backend Java parser that tries multiple parsing strategies
to ensure maximum class coverage.

Backends (in order of preference):
1. tree-sitter (best for modern Java 17+)
2. javalang (good for Java 8-11 syntax)
3. regex fallback (extracts basic info when all else fails)
"""

import re
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Tuple

# Try importing backends
TREE_SITTER_AVAILABLE = False
JAVALANG_AVAILABLE = False

try:
    from tree_sitter_languages import get_language, get_parser
    TREE_SITTER_AVAILABLE = True
except ImportError:
    try:
        import tree_sitter_java
        import tree_sitter
        TREE_SITTER_AVAILABLE = True  # Different import path
    except ImportError:
        pass

try:
    import javalang
    JAVALANG_AVAILABLE = True
except ImportError:
    pass


@dataclass
class JavaMethod:
    """Represents a Java method."""
    name: str
    return_type: str = ""
    parameters: List[str] = field(default_factory=list)
    modifiers: List[str] = field(default_factory=list)
    annotations: List[str] = field(default_factory=list)
    
    @property
    def is_public(self) -> bool:
        return 'public' in self.modifiers
    
    @property
    def signature(self) -> str:
        params = ', '.join(self.parameters)
        mods = ' '.join(self.modifiers)
        return f"{mods} {self.return_type} {self.name}({params})"


@dataclass
class JavaField:
    """Represents a Java field."""
    name: str
    type: str
    modifiers: List[str] = field(default_factory=list)
    
    @property
    def is_public(self) -> bool:
        return 'public' in self.modifiers


def is_valid_java_identifier(name: str) -> bool:
    """Check if a string is a valid Java identifier."""
    if not name:
        return False
    # Must start with letter or underscore, contain only letters/digits/underscores
    # Also reject if contains special chars like (, ), <, >, newlines, spaces
    if not re.match(r'^[A-Za-z_][A-Za-z0-9_]*$', name):
        return False
    return True


def sanitize_for_mermaid(name: str) -> str:
    """Sanitize a name for use in Mermaid diagrams."""
    if not name:
        return "Unknown"
    # Remove generics
    if '<' in name:
        name = name.split('<')[0]
    # Remove package prefix
    if '.' in name:
        name = name.split('.')[-1]
    # Remove invalid characters
    name = re.sub(r'[^A-Za-z0-9_]', '', name)
    # Ensure it starts with a letter
    if name and not name[0].isalpha():
        name = 'C' + name
    return name if name else "Unknown"


def extract_javadoc(content: str, class_name: str) -> str:
    """Extract javadoc comment for a class. Uses simple string search to avoid regex catastrophic backtracking."""
    # Find the class declaration line
    class_pattern = rf'\b(class|interface|enum|record)\s+{re.escape(class_name)}\b'
    class_match = re.search(class_pattern, content)
    
    if not class_match:
        return ""
    
    # Look backwards from the class declaration for /**
    before_class = content[:class_match.start()]
    
    # Find the last /** ... */ before the class
    last_javadoc_end = before_class.rfind('*/')
    if last_javadoc_end == -1:
        return ""
    
    # Find the matching /**
    last_javadoc_start = before_class.rfind('/**', 0, last_javadoc_end)
    if last_javadoc_start == -1:
        return ""
    
    # Check there's no other code between javadoc and class (only whitespace/annotations allowed)
    between = before_class[last_javadoc_end + 2:]
    # Remove annotations and whitespace
    between_cleaned = re.sub(r'@\w+(\([^)]*\))?\s*', '', between)
    between_cleaned = between_cleaned.strip()
    if between_cleaned:
        # There's code between javadoc and class, not a class javadoc
        return ""
    
    # Extract the javadoc content
    javadoc = before_class[last_javadoc_start:last_javadoc_end + 2]
    
    # Clean up - remove /** and */, strip * from lines
    doc = javadoc[3:-2]  # Remove /** and */
    lines = [line.strip().lstrip('*').strip() for line in doc.split('\n')]
    
    # Filter out @param, @return, etc tags
    result = ' '.join(line for line in lines if line and not line.startswith('@'))
    
    return result[:500] if result else ""  # Limit length




@dataclass 
class JavaClass:
    """Represents a parsed Java class/interface/enum."""
    # File info
    filepath: Path
    source_root: str  # 'main' or 'client'
    
    # Class identity
    package: str
    name: str
    class_type: str  # 'class', 'interface', 'enum', 'record'
    modifiers: List[str] = field(default_factory=list)
    annotations: List[str] = field(default_factory=list)
    
    # Documentation
    javadoc: str = ""  # Extracted javadoc comment
    
    # Relationships
    extends: Optional[str] = None
    implements: List[str] = field(default_factory=list)
    
    # Imports (filtered to internal only)
    internal_imports: List[str] = field(default_factory=list)
    external_imports: List[str] = field(default_factory=list)
    
    # Members
    methods: List[JavaMethod] = field(default_factory=list)
    fields: List[JavaField] = field(default_factory=list)
    
    # Inner classes
    inner_classes: List[str] = field(default_factory=list)
    
    # Parser info
    parser_used: str = "unknown"
    
    @property
    def safe_name(self) -> str:
        """Get a mermaid-safe version of the name."""
        return sanitize_for_mermaid(self.name)
    
    @property
    def fqn(self) -> str:
        """Fully qualified name."""
        return f"{self.package}.{self.name}" if self.package else self.name
    
    @property
    def relative_package(self) -> str:
        """Package relative to net.cyberpunk042."""
        if self.package.startswith("net.cyberpunk042."):
            return self.package[len("net.cyberpunk042."):]
        return self.package
    
    @property
    def domain(self) -> str:
        """Top-level domain (first package segment after net.cyberpunk042)."""
        parts = self.relative_package.split('.')
        return parts[0] if parts else ""
    
    @property
    def subdomain(self) -> str:
        """Second-level domain."""
        parts = self.relative_package.split('.')
        return parts[1] if len(parts) > 1 else ""
    
    @property
    def is_interface(self) -> bool:
        return self.class_type == 'interface'
    
    @property
    def is_enum(self) -> bool:
        return self.class_type == 'enum'
    
    @property
    def is_abstract(self) -> bool:
        return 'abstract' in self.modifiers
    
    @property
    def public_methods(self) -> List[JavaMethod]:
        return [m for m in self.methods if m.is_public]
    
    def depends_on(self, other_fqn: str) -> bool:
        """Check if this class depends on another class."""
        return other_fqn in self.internal_imports


# =============================================================================
# TREE-SITTER PARSER (Best for modern Java)
# =============================================================================

def _parse_with_tree_sitter(filepath: Path, content: str, source_root: str) -> Optional[JavaClass]:
    """Parse using tree-sitter."""
    if not TREE_SITTER_AVAILABLE:
        return None
    
    # Normalize line endings
    content = content.replace('\r\n', '\n').replace('\r', '\n')
    
    # CRITICAL: Use bytes for tree-sitter since it returns byte offsets
    # UTF-8 files with multi-byte chars cause string[offset] to differ from bytes[offset]
    content_bytes = content.encode('utf-8')
    
    def get_text(node) -> str:
        """Extract text from a node using byte offsets."""
        return content_bytes[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
    
    try:
        parser = get_parser('java')
        tree = parser.parse(content_bytes)
        root = tree.root_node
    except Exception as e:
        return None
    
    # Extract package
    package = ""
    for node in root.children:
        if node.type == 'package_declaration':
            for child in node.children:
                if child.type == 'scoped_identifier' or child.type == 'identifier':
                    package = get_text(child)
                    break
    
    # Extract imports
    internal_imports = []
    external_imports = []
    for node in root.children:
        if node.type == 'import_declaration':
            import_text = get_text(node)
            match = re.search(r'import\s+(?:static\s+)?([\w.]+)', import_text)
            if match:
                imp_path = match.group(1)
                if imp_path.startswith("net.cyberpunk042"):
                    internal_imports.append(imp_path)
                else:
                    external_imports.append(imp_path)
    
    # Find the main type declaration
    class_name = filepath.stem
    class_type = "class"
    modifiers = []
    annotations = []
    extends = None
    implements = []
    methods = []
    fields = []
    inner_classes = []
    
    def find_type_declarations(node, depth=0):
        nonlocal class_name, class_type, modifiers, annotations, extends, implements
        nonlocal methods, fields, inner_classes
        
        if depth > 0:  # Inner class
            if node.type in ['class_declaration', 'interface_declaration', 'enum_declaration', 'record_declaration']:
                for child in node.children:
                    if child.type == 'identifier':
                        inner_classes.append(get_text(child))
                        break
            return
        
        if node.type == 'class_declaration':
            class_type = 'class'
        elif node.type == 'interface_declaration':
            class_type = 'interface'
        elif node.type == 'enum_declaration':
            class_type = 'enum'
        elif node.type == 'record_declaration':
            class_type = 'record'
        else:
            for child in node.children:
                find_type_declarations(child, depth)
            return
        
        # Process class declaration - first pass for identifier
        found_name = False
        for child in node.children:
            if child.type == 'identifier' and not found_name:
                potential_name = get_text(child)
                # Validate the name is a proper Java identifier
                if is_valid_java_identifier(potential_name):
                    class_name = potential_name
                    found_name = True
        
        # Second pass for everything else
        for child in node.children:
            if child.type == 'modifiers':
                for mod in child.children:
                    if mod.type in ['public', 'private', 'protected', 'abstract', 'final', 'static']:
                        modifiers.append(mod.type)
                    elif mod.type == 'marker_annotation' or mod.type == 'annotation':
                        ann_text = get_text(mod)
                        match = re.search(r'@(\w+)', ann_text)
                        if match:
                            annotations.append(match.group(1))
            elif child.type == 'superclass':
                for c in child.children:
                    if c.type == 'type_identifier' or c.type == 'generic_type':
                        extends = get_text(c).split('<')[0]
                        break
            elif child.type == 'super_interfaces' or child.type == 'extends_interfaces':
                for c in child.children:
                    if c.type == 'type_list':
                        for t in c.children:
                            if t.type in ['type_identifier', 'generic_type']:
                                impl = get_text(t).split('<')[0]
                                implements.append(impl)
            
            # RECORD COMPONENTS - records have their "fields" in formal_parameters
            elif child.type == 'formal_parameters' and class_type == 'record':
                for param in child.children:
                    if param.type == 'formal_parameter' or param.type == 'record_pattern_component':
                        # Extract type and name
                        p_type = ""
                        p_name = ""
                        for p in param.children:
                            if p.type in ['type_identifier', 'generic_type', 'array_type', 'integral_type', 'floating_point_type', 'boolean_type']:
                                p_type = get_text(p)
                            elif p.type == 'identifier':
                                p_name = get_text(p)
                        if p_name and p_type and is_valid_java_identifier(p_name):
                            fields.append(JavaField(name=p_name, type=p_type, modifiers=['public', 'final']))
            
            elif child.type == 'class_body' or child.type == 'interface_body' or child.type == 'enum_body' or child.type == 'record_body':
                # Process body for methods and fields
                for member in child.children:
                    if member.type == 'method_declaration':
                        mods = []
                        name = ""
                        ret_type = ""
                        params = []
                        anns = []
                        
                        for m in member.children:
                            if m.type == 'modifiers':
                                for mod in m.children:
                                    if mod.type in ['public', 'private', 'protected', 'abstract', 'final', 'static']:
                                        mods.append(mod.type)
                                    elif 'annotation' in mod.type:
                                        ann_text = get_text(mod)
                                        match = re.search(r'@(\w+)', ann_text)
                                        if match:
                                            anns.append(match.group(1))
                            elif m.type in ['type_identifier', 'void_type', 'generic_type', 'array_type', 'integral_type', 'floating_point_type', 'boolean_type']:
                                ret_type = get_text(m)
                            elif m.type == 'identifier':
                                name = get_text(m)
                            elif m.type == 'formal_parameters':
                                for p in m.children:
                                    if p.type == 'formal_parameter':
                                        p_text = get_text(p)
                                        params.append(p_text)
                        
                        # Only add method if name is a valid identifier
                        if name and is_valid_java_identifier(name):
                            methods.append(JavaMethod(
                                name=name,
                                return_type=ret_type if is_valid_java_identifier(ret_type.split('<')[0]) else "",
                                parameters=params,
                                modifiers=mods,
                                annotations=anns
                            ))
                    
                    elif member.type == 'field_declaration':
                        mods = []
                        f_type = ""
                        f_names = []
                        
                        for m in member.children:
                            if m.type == 'modifiers':
                                for mod in m.children:
                                    if mod.type in ['public', 'private', 'protected', 'final', 'static']:
                                        mods.append(mod.type)
                            elif m.type in ['type_identifier', 'generic_type', 'array_type', 'integral_type', 'floating_point_type', 'boolean_type']:
                                f_type = get_text(m)
                            elif m.type == 'variable_declarator':
                                for v in m.children:
                                    if v.type == 'identifier':
                                        f_names.append(get_text(v))
                                        break
                        
                        for fname in f_names:
                            fields.append(JavaField(name=fname, type=f_type, modifiers=mods))
                    
                    # Look for inner classes
                    elif member.type in ['class_declaration', 'interface_declaration', 'enum_declaration']:
                        find_type_declarations(member, depth + 1)
    
    for node in root.children:
        find_type_declarations(node, 0)
    
    # Validate the class name - if invalid, fall back to filename
    if not is_valid_java_identifier(class_name):
        class_name = filepath.stem
    
    # Extract javadoc
    javadoc = extract_javadoc(content, class_name)
    
    return JavaClass(
        filepath=filepath,
        source_root=source_root,
        package=package,
        name=class_name,
        class_type=class_type,
        modifiers=modifiers,
        annotations=annotations,
        javadoc=javadoc,
        extends=extends,
        implements=implements,
        internal_imports=internal_imports,
        external_imports=external_imports,
        methods=methods,
        fields=fields,
        inner_classes=inner_classes,
        parser_used='tree-sitter'
    )


# =============================================================================
# JAVALANG PARSER (Fallback for Java 8-11)
# =============================================================================

def _parse_with_javalang(filepath: Path, content: str, source_root: str) -> Optional[JavaClass]:
    """Parse using javalang."""
    if not JAVALANG_AVAILABLE:
        return None
    
    try:
        tree = javalang.parse.parse(content)
    except:
        return None
    
    # Import the existing parsing logic
    package = tree.package.name if tree.package else ""
    
    internal_imports = []
    external_imports = []
    for imp in tree.imports:
        imp_path = imp.path
        if imp_path.startswith("net.cyberpunk042"):
            internal_imports.append(imp_path)
        else:
            external_imports.append(imp_path)
    
    # Find type declaration
    type_decl = None
    class_type = 'class'
    
    for path, node in tree.filter(javalang.tree.TypeDeclaration):
        if len(path) <= 2:
            type_decl = node
            if isinstance(node, javalang.tree.InterfaceDeclaration):
                class_type = 'interface'
            elif isinstance(node, javalang.tree.EnumDeclaration):
                class_type = 'enum'
            break
    
    if type_decl is None:
        return None
    
    # Extract extends
    extends = None
    if hasattr(type_decl, 'extends') and type_decl.extends:
        if isinstance(type_decl.extends, list):
            extends = type_decl.extends[0].name if type_decl.extends else None
        else:
            extends = type_decl.extends.name if hasattr(type_decl.extends, 'name') else str(type_decl.extends)
    
    # Extract implements
    implements = []
    if hasattr(type_decl, 'implements') and type_decl.implements:
        implements = [i.name if hasattr(i, 'name') else str(i) for i in type_decl.implements]
    
    modifiers = list(type_decl.modifiers) if type_decl.modifiers else []
    annotations = [a.name for a in type_decl.annotations] if type_decl.annotations else []
    
    # Extract methods
    methods = []
    if hasattr(type_decl, 'body') and type_decl.body:
        for member in type_decl.body:
            if isinstance(member, javalang.tree.MethodDeclaration):
                params = []
                if member.parameters:
                    for p in member.parameters:
                        ptype = p.type.name if hasattr(p.type, 'name') else str(p.type)
                        params.append(f"{ptype} {p.name}")
                
                ret_type = ""
                if member.return_type:
                    ret_type = member.return_type.name if hasattr(member.return_type, 'name') else str(member.return_type)
                
                methods.append(JavaMethod(
                    name=member.name,
                    return_type=ret_type,
                    parameters=params,
                    modifiers=list(member.modifiers) if member.modifiers else [],
                    annotations=[a.name for a in member.annotations] if member.annotations else []
                ))
    
    return JavaClass(
        filepath=filepath,
        source_root=source_root,
        package=package,
        name=type_decl.name,
        class_type=class_type,
        modifiers=modifiers,
        annotations=annotations,
        extends=extends,
        implements=implements,
        internal_imports=internal_imports,
        external_imports=external_imports,
        methods=methods,
        parser_used='javalang'
    )


# =============================================================================
# REGEX FALLBACK PARSER (Last resort - basic info extraction)
# =============================================================================

def _parse_with_regex(filepath: Path, content: str, source_root: str) -> Optional[JavaClass]:
    """Fallback regex-based parser for basic info extraction."""
    
    # Extract package
    package = ""
    pkg_match = re.search(r'package\s+([\w.]+)\s*;', content)
    if pkg_match:
        package = pkg_match.group(1)
    
    # Extract imports
    internal_imports = []
    external_imports = []
    for match in re.finditer(r'import\s+(?:static\s+)?([\w.]+)\s*;', content):
        imp = match.group(1)
        if imp.startswith("net.cyberpunk042"):
            internal_imports.append(imp)
        else:
            external_imports.append(imp)
    
    # Determine class type and name
    class_type = "class"
    class_name = filepath.stem
    modifiers = []
    extends = None
    implements = []
    annotations = []
    
    # Match class/interface/enum/record declaration
    decl_pattern = r'''
        ((?:@\w+(?:\([^)]*\))?\s*)*)           # Annotations
        ((?:public|private|protected|abstract|final|static|sealed|non-sealed)\s+)*  # Modifiers
        (class|interface|enum|record)\s+        # Type
        (\w+)                                   # Name
        (?:\s*<[^>]+>)?                         # Generics
        (?:\s+extends\s+([\w.<>,\s]+?))?        # Extends
        (?:\s+implements\s+([\w.<>,\s]+?))?     # Implements
        (?:\s+permits\s+([\w.<>,\s]+?))?        # Permits (sealed)
        \s*[\{(]                                # Opening brace or record params
    '''
    
    decl_match = re.search(decl_pattern, content, re.VERBOSE | re.MULTILINE)
    if decl_match:
        # Annotations
        ann_text = decl_match.group(1) or ""
        annotations = re.findall(r'@(\w+)', ann_text)
        
        # Modifiers
        mod_text = decl_match.group(2) or ""
        modifiers = mod_text.split()
        
        # Type
        class_type = decl_match.group(3)
        
        # Name
        class_name = decl_match.group(4)
        
        # Extends
        if decl_match.group(5):
            ext = decl_match.group(5).strip()
            extends = ext.split('<')[0].split(',')[0].strip()
        
        # Implements
        if decl_match.group(6):
            impl_text = decl_match.group(6)
            implements = [i.strip().split('<')[0] for i in impl_text.split(',')]
    
    # Extract public methods (simplified)
    method_pattern = r'public\s+(?:static\s+)?(?:final\s+)?(?:<[^>]+>\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
    methods = []
    for match in re.finditer(method_pattern, content):
        methods.append(JavaMethod(
            name=match.group(2),
            return_type=match.group(1),
            modifiers=['public']
        ))
    
    return JavaClass(
        filepath=filepath,
        source_root=source_root,
        package=package,
        name=class_name,
        class_type=class_type,
        modifiers=modifiers,
        annotations=annotations,
        extends=extends,
        implements=implements,
        internal_imports=internal_imports,
        external_imports=external_imports,
        methods=methods,
        parser_used='regex'
    )


# =============================================================================
# MAIN PARSING FUNCTION
# =============================================================================

def parse_java_file(filepath: Path, source_root: str = 'main') -> Optional[JavaClass]:
    """
    Parse a Java file using multiple backends for maximum compatibility.
    
    Args:
        filepath: Path to the Java file
        source_root: 'main' or 'client' to indicate source tree
        
    Returns:
        JavaClass object or None if all parsers fail
    """
    try:
        content = filepath.read_text(encoding='utf-8')
    except Exception as e:
        print(f"  Warning: Could not read {filepath}: {e}")
        return None
    
    # Try tree-sitter first (best for modern Java)
    if TREE_SITTER_AVAILABLE:
        result = _parse_with_tree_sitter(filepath, content, source_root)
        if result:
            return result
    
    # Try javalang next
    if JAVALANG_AVAILABLE:
        result = _parse_with_javalang(filepath, content, source_root)
        if result:
            return result
    
    # Fall back to regex (always works)
    return _parse_with_regex(filepath, content, source_root)


def scan_source_directory(src_dir: Path, source_root: str = 'main') -> List[JavaClass]:
    """
    Scan a source directory and parse all Java files.
    """
    classes = []
    java_files = list(src_dir.rglob("*.java"))
    
    # Filter out backup files and legacy
    java_files = [f for f in java_files if not f.name.endswith('.bak')]
    java_files = [f for f in java_files if '_legacy' not in str(f)]
    
    print(f"  Scanning {len(java_files)} Java files in {source_root}...")
    
    parser_stats = {'tree-sitter': 0, 'javalang': 0, 'regex': 0, 'failed': 0}
    
    for filepath in java_files:
        jc = parse_java_file(filepath, source_root)
        if jc:
            classes.append(jc)
            parser_stats[jc.parser_used] = parser_stats.get(jc.parser_used, 0) + 1
        else:
            parser_stats['failed'] += 1
    
    print(f"    Parsed: {len(classes)} classes")
    print(f"    Backends: tree-sitter={parser_stats['tree-sitter']}, javalang={parser_stats['javalang']}, regex={parser_stats['regex']}, failed={parser_stats['failed']}")
    
    return classes


def scan_project(project_root: Path) -> List[JavaClass]:
    """
    Scan the entire project source code.
    """
    all_classes = []
    
    print(f"\n  Parser backends available:")
    print(f"    tree-sitter: {'✅' if TREE_SITTER_AVAILABLE else '❌'}")
    print(f"    javalang: {'✅' if JAVALANG_AVAILABLE else '❌'}")
    print(f"    regex: ✅ (always available)")
    print()
    
    # Scan main source
    main_src = project_root / "src" / "main" / "java" / "net" / "cyberpunk042"
    if main_src.exists():
        all_classes.extend(scan_source_directory(main_src, 'main'))
    
    # Scan client source
    client_src = project_root / "src" / "client" / "java" / "net" / "cyberpunk042"
    if client_src.exists():
        all_classes.extend(scan_source_directory(client_src, 'client'))
    
    print(f"\n  Total: {len(all_classes)} classes parsed")
    
    return all_classes


# Quick test
if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        filepath = Path(sys.argv[1])
        jc = parse_java_file(filepath)
        if jc:
            print(f"Class: {jc.fqn}")
            print(f"Type: {jc.class_type}")
            print(f"Parser: {jc.parser_used}")
            print(f"Extends: {jc.extends}")
            print(f"Implements: {jc.implements}")
            print(f"Internal imports: {len(jc.internal_imports)}")
            print(f"Methods: {len(jc.methods)} ({len(jc.public_methods)} public)")
    else:
        print("Checking available parsers:")
        print(f"  tree-sitter: {'✅' if TREE_SITTER_AVAILABLE else '❌ (install: pip install tree-sitter-languages)'}")
        print(f"  javalang: {'✅' if JAVALANG_AVAILABLE else '❌ (install: pip install javalang)'}")
        print(f"  regex: ✅ (always available)")
