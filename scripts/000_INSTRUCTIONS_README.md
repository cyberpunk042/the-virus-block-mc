# Minecraft Mod Development Tools

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   ██████╗ ██╗   ██╗██╗     ███████╗███████╗                                  ║
║   ██╔══██╗██║   ██║██║     ██╔════╝██╔════╝                                  ║
║   ██████╔╝██║   ██║██║     █████╗  ███████╗                                  ║
║   ██╔══██╗██║   ██║██║     ██╔══╝  ╚════██║                                  ║
║   ██║  ██║╚██████╔╝███████╗███████╗███████║                                  ║
║   ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚══════╝╚══════╝                                  ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## ⚠️ CRITICAL: SCRIPT NAMING CONVENTION ⚠️

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│   NUMBERED SCRIPTS (00_xxx.py, 01_xxx.py, ...)                              │
│   ════════════════════════════════════════════                              │
│   → PERMANENT, STABLE, CANONICAL TOOLS                                      │
│   → Part of the official toolkit                                            │
│   → Documented, tested, maintained                                          │
│   → DO NOT ADD NEW NUMBERED SCRIPTS WITHOUT REVIEW                          │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────     │
│                                                                             │
│   UNNUMBERED SCRIPTS (fix_something.py, helper_task.py, ...)                │
│   ═════════════════════════════════════════════════════════                 │
│   → TEMPORARY, ONE-OFF, SITUATIONAL                                         │
│   → For immediate tasks, experiments, fixes                                 │
│   → NOT part of official toolkit                                            │
│   → CAN BE DELETED after use                                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### ❌ WRONG:
```
scripts/10_fix_compilation_batch1.py    ← Numbered = implies permanent tool
scripts/17_update_todo_progress.py      ← Numbered = implies permanent tool
```

### ✅ CORRECT:
```
scripts/fix_compilation_batch1.py       ← Unnumbered = temporary fix script
scripts/update_todo_progress.py         ← Unnumbered = one-off helper
scripts/cleanup_legacy_files.py         ← Unnumbered = situational task
```

---

## 📦 NUMBERED TOOLS (Official Stable Toolkit)

---

### **00_search_native_jars.py** `[UPDATED - needs retest]`
Search raw `.jar` files for ASCII byte patterns.

**Features:**
- Recursive JAR discovery
- Scans only `.class` entries
- Case-insensitive by default
- Multiple patterns supported
- Optional `javap` disassembly
- Optional `--save` archiving
- Outputs → `agent-tools/native-search/`

**Usage:**
```bash
python3 scripts/00_search_native_jars.py /path/to/jars heal
python3 scripts/00_search_native_jars.py --javap /path/to/jars jump
python3 scripts/00_search_native_jars.py --save agent-tools /path/to/jars movementSpeed
```

---

### **01_query_tiny_mappings.py** `[UPDATED - needs retest]`
Mapping-aware inspector for Fabric Tiny Mappings.

**Features:**
- Locates classes through named/intermediary/official domains
- Lists methods & descriptors across domains
- Optional `javap` disassembly
- Optional fallback JAR search
- Optional result archiving
- Outputs → `agent-tools/mappings/`

**Usage:**
```bash
python3 scripts/01_query_tiny_mappings.py --class LivingEntity
python3 scripts/01_query_tiny_mappings.py --class LivingEntity --method heal
python3 scripts/01_query_tiny_mappings.py --class Entity --method tick --javap
python3 scripts/01_query_tiny_mappings.py --save
```

---

### **02_update_todo_batch.py** `[STABLE]`
Structured TODO-batch automation for staged refactoring.

**Features:**
- Mark batch complete
- Mark individual IDs complete
- Auto-update stats
- Ideal for large-scale mod rewrites

**Usage:**
```bash
python3 scripts/02_update_todo_batch.py 16 --complete
python3 scripts/02_update_todo_batch.py 17
```

---

### **03_usage_graph.py** `[UNTESTED - needs review]`
Search your source tree for every reference to a class/method/symbol.

**Features:**
- Uses `ripgrep` when available
- Groups results by file
- Provides code-line context
- Optional `--save`
- Ideal before refactoring or renaming

---

### **04_class_tree.py** `[UNTESTED - needs review]`
Builds package-level and class-level hierarchies.

**Features:**
- Shows inheritance trees
- Useful when identifying anchor points for mixins or injections
- Optional save-to-file

---

### **05_method_signature_scanner.py** `[UNTESTED - needs review]`
Search methods by descriptor or shape.

**Features:**
- Finds patterns like `(Lnet/...;)V`
- Helps identify overloads, constructors, and obfuscated targets
- Supports output archiving

---

### **06_mod_interface_map.py** `[UNTESTED - needs review]`
Maps your mod's interaction surface.

**Features:**
- Extracts all entrypoints, mixins, event listeners, injections
- Shows where your mod hooks into Minecraft or other mods
- Great for debugging mod startup or integration failures

---

### **07_config_schema_extractor.py** `[UPDATED - needs retest]`
The unified configuration pipeline.

**Features:**
- Scans selected Java packages for config classes
- Extracts fields, annotation metadata, ranges, defaults
- Detects `$group/id` reference fields
- Generates internal JSON Doc
- Generates full VS Code JSON Schema
- Supports `--package-prefix` and `--class-name-contains`
- Outputs → `agent-tools/config-schema/`

This provides **autocomplete**, **validation**, and **safe refactoring** for all config JSON files.

---

### **08_config_profile_lint.py** `[UNTESTED - complex]`
Validates profile JSON files.

**Features:**
- Schema validation (if schema provided)
- Detects invalid `$group/id` references
- Reports unknown groups or IDs
- Identifies unused profiles
- Perfect for CI or pre-commit pipelines

**Usage:**
```bash
python3 scripts/08_config_profile_lint.py --profiles-root profiles --schema agent-tools/config-schema/field_profiles.schema.json
```

---

## 🔧 UNNUMBERED SCRIPTS (Temporary/Situational)

These are helpers and utilities, **NOT** part of the official pipeline.
They can be created for immediate tasks and deleted after use.

| Script | Purpose |
|--------|---------|
| `fix_*.py` | One-off compilation/bug fixes |
| `cleanup_*.py` | Situational cleanup tasks |
| `migrate_*.py` | One-time migration scripts |
| `analyze_*.py` | Temporary analysis helpers |

---

## 🔗 HOW TOOLS INTERCONNECT

A typical workflow:

```
1. FIND bytecode locations          → 00_search_native_jars.py
           ↓
2. RESOLVE mappings & descriptors   → 01_query_tiny_mappings.py
           ↓
3. EXPLORE references               → 03_usage_graph.py
           ↓
4. UNDERSTAND structure             → 04_class_tree.py, 06_mod_interface_map.py
           ↓
5. CONTROL method changes           → 05_method_signature_scanner.py
           ↓
6. MAINTAIN config systems          → 07_config_schema_extractor.py
           ↓
7. VALIDATE profiles                → 08_config_profile_lint.py
```

**Outputs accumulate under:**
```
agent-tools/
├── native-search/    # 00_search_native_jars.py
├── mappings/         # 01_query_tiny_mappings.py
└── config-schema/    # 07_config_schema_extractor.py (JSON Doc + VS Code Schema)
```

---

## 📋 SUMMARY

| Category | Scripts | Purpose |
|----------|---------|---------|
| **Numbered (00-08)** | Permanent | Official toolkit - stable, tested, maintained |
| **Unnumbered** | Temporary | One-off tasks - experimental, situational |

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   REMEMBER: Only add numbered scripts for PERMANENT, REUSABLE tools!         ║
║   Temporary fixes and helpers should NEVER have number prefixes.             ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
```
