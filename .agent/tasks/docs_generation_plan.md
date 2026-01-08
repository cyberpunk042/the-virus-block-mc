# Documentation Generation Implementation Plan

> Generated: 2025-12-17

## 🎯 Objective

Generate organized, segmented documentation from Java source code:
- **Class diagrams** (inheritance + dependencies)
- **Architecture diagrams** (component relationships)
- **Package overviews** (what's where)
- **READMEs** (placeholders for manual enrichment)

All files kept **under 600 lines** via intelligent splitting.

---

## 📊 Source Code Structure Analysis

### Main (Common/Server) - `src/main/java/net/cyberpunk042/`

| Domain | Subpackages | Files | Priority |
|--------|-------------|-------|----------|
| **field** | category, definition, effect, influence, instance, loader, primitive, profile, registry | 83 | HIGH |
| **visual** | animation, appearance, color, fill, pattern, shape, transform, visibility | 97 | HIGH |
| **infection** | api, collapse, controller, events, orchestrator, profile, scenario, service, singularity, state | 147 | MEDIUM |
| **block** | (flat) | 35 | MEDIUM |
| **command** | (flat) | 34 | LOW |
| **network** | (flat) | 33 | LOW |
| **growth** | (flat) | 20 | MEDIUM |
| **mixin** | (flat) | 23 | LOW |
| **entity** | (flat) | 4 | LOW |
| **util** | json, etc | 8 | LOW |
| **config** | (flat) | 4 | LOW |
| **log** | (flat) | 25 | LOW |
| **registry** | (flat) | 6 | LOW |

### Client - `src/client/java/net/cyberpunk042/`

| Domain | Subpackages | Files | Priority |
|--------|-------------|-------|----------|
| **client/gui** | component, layout, panel, preview, screen, state, util, widget | 121 | HIGH |
| **client/visual** | animation, mesh, render, tessellator, transform | 26 | HIGH |
| **client/render** | (flat) | 22 | HIGH |
| **client/field** | (flat) | 18 | MEDIUM |
| **client/command** | (flat) | 16 | LOW |
| **mixin** | (flat) | 15 | LOW |

### Config Resources - `config/the-virus-block/`

| Category | Folders | Files |
|----------|---------|-------|
| Field Configs | field_shapes, field_fills, field_animations, field_appearances, ... | 350+ JSON |
| Growth Block | growth_block/ | 65 JSON |
| Other | dimension_profiles, effect_palettes | ~5 JSON |

---

## 📁 Proposed Documentation Structure

```
docs/
├── README.md                          # Master index
├── ARCHITECTURE.md                    # High-level system overview
│
├── main/                              # Common/Server-side code
│   ├── README.md
│   │
│   ├── field/                         # Field System
│   │   ├── README.md
│   │   ├── CLASS_DIAGRAM.md           # Core field classes
│   │   ├── DEPENDENCIES.md            # Dependency graph
│   │   ├── influence/
│   │   │   └── CLASS_DIAGRAM.md
│   │   ├── effect/
│   │   │   └── CLASS_DIAGRAM.md
│   │   └── instance/
│   │       └── CLASS_DIAGRAM.md
│   │
│   ├── visual/                        # Visual Definitions
│   │   ├── README.md
│   │   ├── CLASS_DIAGRAM.md           # Overview
│   │   ├── animation/
│   │   │   └── CLASS_DIAGRAM.md
│   │   ├── pattern/
│   │   │   └── CLASS_DIAGRAM.md
│   │   ├── shape/
│   │   │   └── CLASS_DIAGRAM.md
│   │   └── fill/
│   │       └── CLASS_DIAGRAM.md
│   │
│   ├── infection/                     # Infection System
│   │   ├── README.md
│   │   ├── CLASS_DIAGRAM.md
│   │   ├── service/
│   │   │   └── CLASS_DIAGRAM.md       # 51 files - needs own diagram
│   │   └── orchestrator/
│   │       └── CLASS_DIAGRAM.md
│   │
│   └── other/                         # Block, Entity, Growth, etc.
│       ├── README.md
│       └── CLASS_DIAGRAM.md
│
├── client/                            # Client-side code
│   ├── README.md
│   │
│   ├── gui/                           # GUI System
│   │   ├── README.md
│   │   ├── CLASS_DIAGRAM.md           # High-level GUI
│   │   ├── DEPENDENCIES.md
│   │   ├── panel/
│   │   │   └── CLASS_DIAGRAM.md       # 26 files
│   │   ├── state/
│   │   │   └── CLASS_DIAGRAM.md       # 35 files - big!
│   │   ├── widget/
│   │   │   └── CLASS_DIAGRAM.md       # 17 files
│   │   └── preview/
│   │       └── CLASS_DIAGRAM.md       # 10 files
│   │
│   ├── visual/                        # Client-side Visual
│   │   ├── README.md
│   │   ├── CLASS_DIAGRAM.md
│   │   ├── mesh/
│   │   │   └── CLASS_DIAGRAM.md
│   │   └── render/
│   │       └── CLASS_DIAGRAM.md
│   │
│   └── render/                        # Rendering
│       ├── README.md
│       └── CLASS_DIAGRAM.md
│
├── config/                            # Config JSON Schema docs
│   ├── README.md
│   ├── FIELD_PROFILES.md
│   ├── SHAPES.md
│   └── ANIMATIONS.md
│
└── shared/                            # Cross-cutting concerns
    ├── README.md
    ├── MIXIN_SUMMARY.md
    └── NETWORK_PROTOCOL.md
```

---

## 🔧 Implementation Phases

### Phase 1: Setup & Core Parser

**Goal:** Create Python module with proper Java parsing

1. Install `javalang` dependency
2. Create `scripts/lib/java_parser.py`:
   - Parse Java files into structured data
   - Extract: package, class name, type, extends, implements, imports
   - Extract: public methods, fields, annotations
3. Create `scripts/lib/graph_builder.py`:
   - Build inheritance graph
   - Build dependency graph (from imports)
   - Cluster by package

**Output:** Reusable parsing infrastructure

### Phase 2: Discovery & Mapping

**Goal:** Scan codebase and create documentation map

1. Create `scripts/discover_structure.py`:
   - Scan all Java files
   - Map to documentation structure
   - Identify which classes go where
   - Calculate file sizes (stay under 600 lines)
2. Generate `docs/_meta/structure.json`:
   - Maps source paths to doc paths
   - Tracks class counts per doc file

**Output:** JSON mapping of source → docs

### Phase 3: Diagram Generators

**Goal:** Generate Mermaid diagrams

1. Create `scripts/lib/mermaid_generator.py`:
   - `generate_class_diagram(classes, max_items=30)`
   - `generate_inheritance_diagram(classes)`
   - `generate_dependency_diagram(packages)`
2. Handle splitting:
   - If >30 classes, split by sub-package
   - Link to sub-diagrams from parent

**Output:** Mermaid diagram generation functions

### Phase 4: Documentation Generator

**Goal:** Generate all docs files

1. Create `scripts/generate_docs.py`:
   - Uses structure mapping from Phase 2
   - Generates all `.md` files
   - Creates folder structure
   - Generates READMEs with TOC
2. Template system for READMEs:
   - Auto-generated stats section
   - Placeholder for manual description
   - Links to class diagrams

**Output:** Full docs/ folder structure

### Phase 5: Incremental Updates

**Goal:** Re-generate only changed parts

1. Track file hashes
2. Detect changes since last run
3. Only regenerate affected diagrams

---

## 📋 Diagram Content Specification

### CLASS_DIAGRAM.md

```markdown
# [Package/Area] Class Diagram

> Auto-generated on YYYY-MM-DD

## Overview
- **Classes:** X
- **Interfaces:** Y  
- **Enums:** Z

## Inheritance Hierarchy

\`\`\`mermaid
classDiagram
    class BaseClass
    class ChildA
    BaseClass <|-- ChildA
    ...
\`\`\`

## Dependencies

\`\`\`mermaid
graph LR
    ClassA --> ClassB
    ClassA --> ClassC
    ...
\`\`\`

## Class Details

### ClassName
- **Type:** class/interface/enum
- **Extends:** ParentClass
- **Implements:** Interface1, Interface2
- **Key Methods:** method1(), method2()
```

### DEPENDENCIES.md

```markdown
# [Area] Dependency Graph

## Package Dependencies

\`\`\`mermaid
graph TD
    subgraph field
        field.core
        field.effect
    end
    subgraph visual
        visual.shape
        visual.fill
    end
    field.core --> visual.shape
\`\`\`

## External Dependencies
- Minecraft: net.minecraft.x.y
- Fabric: net.fabricmc.x.y
```

---

## 🚀 Execution Order

```
1. pip install javalang          # Dependency
2. python scripts/discover_structure.py   # Analyze codebase
   → Creates docs/_meta/structure.json
   
3. python scripts/generate_docs.py        # Generate all docs
   → Creates full docs/ structure
   
4. [Manual] Review and enrich READMEs

5. python scripts/generate_docs.py --update  # Future updates
```

---

## ⚙️ Configuration

`scripts/docs_config.json`:
```json
{
  "max_lines_per_file": 600,
  "max_classes_per_diagram": 30,
  "source_roots": [
    "src/main/java/net/cyberpunk042",
    "src/client/java/net/cyberpunk042"
  ],
  "exclude_patterns": [
    "_legacy",
    "*.bak"
  ],
  "priority_packages": [
    "field",
    "visual", 
    "client.gui",
    "client.visual"
  ]
}
```

---

## ✅ Success Criteria

- [ ] All Java classes appear in at least one diagram
- [ ] No file exceeds 600 lines
- [ ] Inheritance relationships are accurate
- [ ] Import-based dependencies are shown
- [ ] READMEs have working links
- [ ] Regeneration is idempotent
- [ ] Client vs Main separation is clear
