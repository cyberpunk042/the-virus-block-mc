# System Utilities Reference

> **Purpose:** Document CommandKnob and Logging usage for the field system  
> **Status:** Reference for implementation  
> **Created:** December 8, 2024

---

## 1. CommandKnob System

### Overview

`CommandKnob` is a unified command builder with built-in protection, defaults, and feedback. Every command knob automatically gets:
- Protection check via `CommandProtection`
- Default value registration
- Consistent feedback messages
- Scenario validation (optional)

### Factory Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `toggle(path, name)` | Enable/disable | `toggle("field.spin", "Spin animation")` |
| `value(path, name)` | Integer with range | `value("field.segments", "Segments")` |
| `floatValue(path, name)` | Float with range | `floatValue("field.alpha", "Alpha")` |
| `enumValue(path, name, class)` | Enum selection | `enumValue("field.mode", "Mode", FillMode.class)` |
| `action(path, name)` | No value, just execute | `action("field.reload", "Reload profiles")` |

### Full Examples

#### Toggle
```java
CommandKnob.toggle("field.spin.enabled", "Field spin")
    .defaultValue(true)
    .handler((source, enabled) -> {
        ClientFieldManager.setSpinEnabled(enabled);
        return true;
    })
    .attach(parent);

// Creates: /parent spin enable|disable
```

#### Value with Range and Unit
```java
CommandKnob.value("field.segments", "Segment count")
    .range(8, 256)  // Min and max
    .unit("segments")  // Display unit
    .defaultValue(32)
    .handler((source, value) -> {
        FieldTestManager.setSegments(value);
        return true;
    })
    .attach(parent);

// Creates: /parent segments <8-256>
// Feedback: "Segment count set to 64 segments"
```

#### Float Value
```java
CommandKnob.floatValue("field.alpha", "Alpha")
    .range(0.0f, 1.0f)
    .defaultValue(0.8f)
    .handler((source, alpha) -> {
        FieldTestManager.setAlpha(alpha);
        return true;
    })
    .attach(parent);

// Creates: /parent alpha <0.0-1.0>
```

#### Enum Value
```java
CommandKnob.enumValue("field.fill", "Fill mode", FillMode.class)
    .idMapper(FillMode::name)  // How to display (optional, defaults to lowercase name)
    .parser(FillMode::valueOf)  // How to parse (optional)
    .defaultValue(FillMode.SOLID)
    .handler((source, mode) -> {
        FieldTestManager.setFillMode(mode);
        return true;
    })
    .attach(parent);

// Creates: /parent fill <solid|wireframe|cage|points>
// With autocomplete suggestions!
```

#### Action (No Value)
```java
CommandKnob.action("field.reload", "Reload field profiles")
    .successMessage("Field profiles reloaded")  // Custom message
    .handler(source -> {
        FieldRegistry.reload();
        return true;
    })
    .attach(parent);

// Creates: /parent reload
```

#### With Scenario Check
```java
CommandKnob.value("field.radius", "Radius")
    .range(1, 100)
    .defaultValue(10)
    .scenarioRequired((source, value) -> {
        if (FieldTestManager.getCurrentField() == null) {
            CommandFeedback.error(source, "No field active. Use /fieldtest spawn first.");
            return false;
        }
        return true;
    })
    .handler((source, value) -> {
        FieldTestManager.setRadius(value);
        return true;
    })
    .attach(parent);
```

### Best Practices

1. **Always set defaultValue()** - It's required and auto-registers the default
2. **Use descriptive paths** - `"field.transform.anchor"` not `"anchor"`
3. **Return false from handler on failure** - Automatic error feedback
4. **Use scenarioRequired() for state checks** - Not inside handler

---

## 2. Logging System

### Overview

The logging system uses channels, topics, and a fluent builder API.

### Channels Available

| Channel | Label | Purpose | Default |
|---------|-------|---------|---------|
| `Logging.FIELD` | Field | Field lifecycle, spawning, parsing | INFO |
| `Logging.RENDER` | Render | Rendering, tessellation, patterns | WARN |
| `Logging.BINDING` | Binding | Binding evaluation (health → alpha) | OFF |
| `Logging.ANIMATION` | Animation | Every-frame updates (spin, pulse) | OFF |
| `Logging.NETWORK` | Network | Packet sync, multiplayer state | WARN |
| `Logging.COMMANDS` | Commands | Command execution | INFO |
| `Logging.REGISTRY` | Registry | Definition loading | WARN |

**Note:** BINDING and ANIMATION are OFF by default because they're very verbose (tick/frame updates).

### Log Levels

| Level | When to Use |
|-------|-------------|
| `ERROR` | Something failed, action needed |
| `WARN` | Unexpected but recoverable |
| `INFO` | Important milestones |
| `DEBUG` | Detailed flow (dev only) |
| `TRACE` | Very verbose (heavy dev only) |

### Basic Logging

```java
// Quick log
Logging.RENDER.info("Field spawned: {}", fieldId);
Logging.RENDER.warn("Pattern mismatch detected");
Logging.RENDER.error("Failed to parse definition");

// With topic (for filtering)
Logging.RENDER.topic("pattern").debug("Applying pattern: {}", pattern);
Logging.RENDER.topic("tessellate").trace("Generating {} vertices", count);
```

### Context Builder (Rich Data)

```java
Logging.RENDER.topic("field")
    .player(player)
    .id(definitionId)
    .kv("radius", radius)
    .kv("layers", layers.size())
    .info("Spawning field");

// Output: [Render:field] player=Jean id=the-virus-block:test_sphere radius=10.0 layers=3 Spawning field
```

### Context Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `.at(BlockPos)` | Position context | `.at(pos)` |
| `.at(Vec3d)` | Precise position | `.at(playerPos)` |
| `.player(PlayerEntity)` | Player context | `.player(player)` |
| `.entity(Entity)` | Entity context | `.entity(mob)` |
| `.world(World)` | World context | `.world(world)` |
| `.id(Identifier)` | Resource ID | `.id(fieldId)` |
| `.kv(key, value)` | Key-value pair | `.kv("count", 5)` |
| `.exception(Throwable)` | Attach exception | `.exception(e)` |
| `.duration(ticks)` | Duration in ticks | `.duration(20)` |
| `.ms(millis)` | Duration in ms | `.ms(150)` |
| `.count(name, value)` | Named counter | `.count("vertices", 1024)` |
| `.percent(name, value)` | Percentage | `.percent("progress", 0.75)` |
| `.reason(string)` | Reason text | `.reason("Invalid CellType")` |
| `.phase(string)` | Phase indicator | `.phase("tessellation")` |
| `.list(name, items)` | List of items | `.list("layers", layers)` |

### Formatted Output (Rich Sections)

```java
Logging.RENDER.topic("debug")
    .formatted()
    .heading("Field Configuration")
    .pairs()
        .add("Type", fieldType)
        .add("Radius", radius)
        .add("Layers", layers.size())
        .done()
    .list("Layer IDs", layers, l -> l.id())
    .table("Primitives")
        .header("ID", "Type", "CellType")
        .row(prim.id(), prim.type(), prim.cellType())
        .done()
    .info();

// Output:
// === Field Configuration ===
// Type: shield
// Radius: 10.0
// Layers: 3
// Layer IDs:
//   - outer_sphere
//   - inner_ring
//   - core
// ┌ Primitives ────────────────────┐
// │ ID          │ Type   │ CellType│
// │ main_sphere │ sphere │ QUAD    │
// └─────────────────────────────────┘
```

### Chat Forwarding

```java
// Channel-level (all messages from channel go to chat)
Logging.RENDER.setChatForward(true);

// Currently NO per-message override
// Need to add: .alwaysChat() method
```

### Proposed: alwaysChat() Method

```java
// Implementation needed in Context.java:
public Context alwaysChat() {
    this.forceChat = true;
    return this;
}

// Usage for pattern mismatch:
Logging.RENDER.topic("pattern")
    .alwaysChat()  // <-- Bypasses channel chatForward setting
    .kv("expected", shape.primaryCellType())
    .kv("got", pattern.cellType())
    .error("Pattern CellType mismatch - primitive will not render");

// This error ALWAYS appears in chat, even if RENDER channel
// doesn't have chatForward enabled.
```

### Available Channels

```java
// Current channels in Logging.java:
Logging.SINGULARITY   // Singularity lifecycle
Logging.COLLAPSE      // Collapse events
Logging.FUSE          // Fuse operations
Logging.CHUNKS        // Chunk loading
Logging.GROWTH        // Growth blocks
Logging.RENDER        // Rendering (use for field rendering)
Logging.COLLISION     // Collision detection
Logging.PROFILER      // Performance profiling
Logging.ORCHESTRATOR  // Orchestration
Logging.SCENARIO      // Scenario management
Logging.PHASE         // Phase transitions
Logging.SCHEDULER     // Task scheduling
Logging.CONFIG        // Configuration loading
Logging.REGISTRY      // Registry operations
Logging.COMMANDS      // Command execution
Logging.EFFECTS       // Visual effects
Logging.INFECTION     // Infection spread
Logging.CALLBACKS     // Callback handling

// ⚠️ MISSING: Logging.FIELD - should be added!
```

### LogFormat Static Utilities

```java
// Quick formatters
LogFormat.duration(ticks);      // "5.0s", "2.0m", "1.0h"
LogFormat.durationMs(millis);   // "500ms", "1.5s"
LogFormat.heading("Title");     // "=== Title ==="

// Type registration (custom types auto-format)
LogFormat.register(MyType.class, obj -> obj.toLogString());

// List building
LogFormat.list(items);                        // Basic list
LogFormat.list(items, item -> item.name());   // With mapper
LogFormat.listBuilder()
    .bullet("• ")
    .indent("  ")
    .add("item1")
    .add("item2")
    .build();

// Pair building (for standalone use, not FormattedContext)
LogFormat.pairs(map);                         // From map
LogFormat.pairs()
    .add("key1", value1)
    .add("key2", value2)
    .separator()
    .add("key3", value3)
    .build();

// Table building (standalone)
LogFormat.table()
    .headers("ID", "Type", "Status")
    .row("sphere_1", "sphere", "active")
    .row("ring_1", "ring", "pending")
    .style(TableStyle.UNICODE)
    .build();

// Tree building
LogFormat.tree("Field Definition")
    .branch("Layer 1")
        .leaf("SpherePrimitive")
        .leaf("RingPrimitive")
    .up()
    .branch("Layer 2")
        .leaf("DiscPrimitive")
    .build();
```

### TableStyle Options

| Style | Appearance |
|-------|------------|
| `ASCII` | `+---+` borders |
| `UNICODE` | `┌───┐` borders (prettier) |
| `MARKDOWN` | `\|---\|` for docs |
| `COMPACT` | Minimal spacing |

### LogWatchdog - Spam Detection

```java
// Automatically enabled - suppresses repeated messages
// Thresholds: 50/second, 500/minute

// Configure watchdog
LogWatchdog.setEnabled(false);          // Disable spam detection
LogWatchdog.setThresholds(100, 1000);   // Custom limits
LogWatchdog.setSuppress(false);         // Log all (warn instead of suppress)
LogWatchdog.reset();                    // Clear counters
```

### Channel Configuration

```java
// Per-channel settings
Logging.RENDER.setLevel(LogLevel.DEBUG);     // Enable debug for render
Logging.RENDER.setChatForward(true);          // Forward to in-game chat
Logging.RENDER.setTopicLevel("pattern", LogLevel.TRACE);  // Topic-specific

// Check levels
if (Logging.RENDER.is(LogLevel.DEBUG)) {
    // Expensive debug operation
}

// Reset channel to defaults
Logging.RENDER.reset();
```

---

## 3. CommandFeedback System

### Overview

`CommandFeedback` provides consistent, colored feedback for commands.

### Simple Methods

| Method | Color | Use For |
|--------|-------|---------|
| `success(src, msg)` | Green | Success messages |
| `successBroadcast(src, msg)` | Green | Success, notify ops |
| `error(src, msg)` | Red | Error messages |
| `info(src, msg)` | Gray | Informational |
| `warn(src, msg)` | Yellow | Warnings |
| `highlight(src, msg)` | Aqua | Important info |

### Common Patterns

```java
// Toggle feedback
CommandFeedback.toggle(source, "Spin animation", true);
// Output: "Spin animation enabled" (green)

// Value set
CommandFeedback.valueSet(source, "Radius", "10.0 blocks");
// Output: "Radius set to 10.0 blocks" (green)

// Current value
CommandFeedback.valueGet(source, "Radius", "10.0 blocks");
// Output: "Current Radius: 10.0 blocks" (gray)

// Not found
CommandFeedback.notFound(source, "Field definition", "unknown_field");
// Output: "Field definition not found: unknown_field" (red)

// Invalid value
CommandFeedback.invalidValue(source, "Segments", "-5");
// Output: "Invalid Segments: -5" (red)

// Requires
CommandFeedback.requires(source, "an active field");
// Output: "Requires an active field" (red)
```

### Complex Messages (Builder)

```java
// Labeled value
MutableText text = CommandFeedback.labeled("Current radius", 10.0);
// Output: "Current radius: 10.0" (gray label, white value)

// Header
MutableText header = CommandFeedback.header("Field Status");
// Output: "═══ Field Status ═══" (gold, bold)

// Subheader
MutableText sub = CommandFeedback.subheader("Layers");
// Output: "── Layers ──" (yellow)

// Bullet point
MutableText bullet = CommandFeedback.bullet("outer_sphere");
// Output: "  • outer_sphere" (gray)

// Key-value
MutableText kv = CommandFeedback.keyValue("Radius", 10.0, Formatting.GREEN);
// Output: "  Radius: 10.0" (gray key, green value)
```

### Full Example: Field Status Command

```java
private int showStatus(ServerCommandSource source) {
    var field = FieldTestManager.getCurrentField();
    if (field == null) {
        CommandFeedback.error(source, "No active field");
        return 0;
    }
    
    // Header
    source.sendFeedback(() -> CommandFeedback.header("Field Status"), false);
    
    // Basic info
    source.sendFeedback(() -> CommandFeedback.keyValue("Definition", field.id(), Formatting.AQUA), false);
    source.sendFeedback(() -> CommandFeedback.keyValue("Radius", field.radius(), Formatting.WHITE), false);
    source.sendFeedback(() -> CommandFeedback.keyValue("Layers", field.layers().size(), Formatting.WHITE), false);
    
    // Layers
    source.sendFeedback(() -> CommandFeedback.subheader("Layers"), false);
    for (var layer : field.layers()) {
        source.sendFeedback(() -> CommandFeedback.bullet(layer.id()), false);
    }
    
    return 1;
}
```

---

## 4. Integration Example: FieldTest Commands

### Complete Pattern for Field Commands

```java
public class FieldTestCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root = CommandManager.literal("fieldtest")
            .requires(s -> s.hasPermissionLevel(2));
        
        // Toggle: spin enable/disable
        CommandKnob.toggle("fieldtest.spin", "Field spin")
            .defaultValue(true)
            .handler((src, enabled) -> {
                Logging.COMMANDS.topic("fieldtest")
                    .player(src.getPlayer())
                    .kv("spin", enabled)
                    .debug("Setting spin");
                return RenderOverrides.setSpin(enabled);
            })
            .attach(root);
        
        // Float: radius
        CommandKnob.floatValue("fieldtest.radius", "Field radius")
            .range(0.1f, 100f)
            .unit("blocks")
            .defaultValue(10f)
            .scenarioRequired((src, v) -> requireField(src))
            .handler((src, radius) -> {
                Logging.COMMANDS.topic("fieldtest")
                    .player(src.getPlayer())
                    .kv("radius", radius)
                    .debug("Setting radius");
                return RenderOverrides.setRadius(radius);
            })
            .attach(root);
        
        // Enum: fill mode
        CommandKnob.enumValue("fieldtest.fill", "Fill mode", FillMode.class)
            .defaultValue(FillMode.SOLID)
            .handler((src, mode) -> {
                Logging.RENDER.topic("fill")
                    .kv("mode", mode)
                    .debug("Changing fill mode");
                return RenderOverrides.setFillMode(mode);
            })
            .attach(root);
        
        // Action: spawn
        root.then(CommandManager.literal("spawn")
            .then(CommandManager.argument("profile", StringArgumentType.word())
                .suggests(FieldRegistry::suggest)
                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "profile")))));
        
        dispatcher.register(root);
    }
    
    private static boolean requireField(ServerCommandSource source) {
        if (FieldTestManager.getCurrentField() == null) {
            CommandFeedback.error(source, "No field active. Use /fieldtest spawn first.");
            return false;
        }
        return true;
    }
    
    private static int spawn(ServerCommandSource source, String profile) {
        Identifier id = Identifier.of(TheVirusBlock.MOD_ID, profile);
        
        // Log the attempt
        Logging.FIELD.topic("spawn")
            .player(source.getPlayer())
            .id(id)
            .info("Spawning field");
        
        var definition = FieldRegistry.get(id);
        if (definition == null) {
            // Log error + notify player
            Logging.FIELD.topic("spawn")
                .alwaysChat()  // <-- Make sure player sees this
                .id(id)
                .error("Field definition not found");
            return 0;
        }
        
        FieldTestManager.spawn(source.getPlayer(), definition);
        
        // Success feedback
        CommandFeedback.successBroadcast(source, "Field spawned: " + profile);
        return 1;
    }
}
```

---

## 5. Implementation TODO: alwaysChat()

### Files to Modify

1. **Context.java** - Add `forceChat` field and `alwaysChat()` method
2. **LogOutput.java** - Add overload accepting `forceChat` parameter
3. **FormattedContext.java** - Propagate forceChat to parent Context

### Proposed Changes

```java
// Context.java
public class Context implements ContextBuilder<Context> {
    // ... existing fields ...
    private boolean forceChat = false;  // NEW
    
    public Context alwaysChat() {  // NEW
        this.forceChat = true;
        return this;
    }
    
    void log(LogLevel level, String message, Object... args) {
        if (!effectiveLevel.includes(level)) return;
        String fullMessage = buildMessage(message, args);
        LogOutput.emit(channel, topicName, level, fullMessage, exception, forceChat);  // MODIFIED
    }
    
    // Accessor for FormattedContext
    boolean forceChat() { return forceChat; }  // NEW
}

// LogOutput.java
public static void emit(Channel channel, String topic, LogLevel level, 
                       String message, Throwable exception) {
    emit(channel, topic, level, message, exception, false);  // Default
}

public static void emit(Channel channel, String topic, LogLevel level, 
                       String message, Throwable exception, boolean forceChat) {  // NEW
    // ... watchdog check ...
    
    // Log to console
    doLog(level, message, exception);
    
    // Forward to chat if enabled OR forced
    if ((channel.chatForward() || forceChat) && LogConfig.chatEnabled()) {  // MODIFIED
        LogChatBridge.forward(channel, topic, level, message);
    }
}
```

---

## 6. Additional Command Utilities

### CommandProtection - Permission & Blacklist System

Controls which commands are blocked or warned:

```java
// Check and warn (call in every knob handler)
if (!CommandProtection.checkAndWarn(source, "field.radius")) {
    return 0;  // Blocked
}

// Protection levels:
// - Untouchable: completely blocked, shows error
// - Blacklisted: warns but allows execution
```

Config file: `config/the-virus-block/command_protection.json`
```json
{
  "untouchable": ["field.unsafe_mode"],
  "blacklisted": [
    { "path": "field.huge_radius", "reason": "May cause lag" }
  ]
}
```

### CommandKnobConfig - Global Limiter Settings

```java
// Check if limits are removed (unsafe mode)
if (CommandKnobConfig.isLimiterRemoved()) {
    // Warning: all min/max limits are disabled!
}

// Get effective limits (respects limiter removal)
int effectiveMin = CommandKnobConfig.effectiveMin(1);   // Returns 1 or MIN_VALUE
int effectiveMax = CommandKnobConfig.effectiveMax(100); // Returns 100 or MAX_VALUE

// Warn player about unsafe values
CommandKnobConfig.warnPlayerUnsafeValue(source, "Radius", 500, 1, 100);
```

### CommandKnobDefaults - Auto-Registered Defaults

```java
// Automatically registered when you call .defaultValue()
CommandKnob.value("field.radius", "Radius")
    .defaultValue(10)  // Registers "field.radius" -> 10
    ...

// Query defaults later
Object defaultValue = CommandKnobDefaults.get("field.radius");  // 10
String formatted = CommandKnobDefaults.format("field.radius");  // "10"
boolean hasDefault = CommandKnobDefaults.has("field.radius");   // true
Set<String> allPaths = CommandKnobDefaults.paths();             // All registered paths
```

### EnumSuggester - Tab Completion for Enums

```java
// Simple (lowercase enum names)
.suggests(EnumSuggester.of(FillMode.class))

// Custom mapper
.suggests(EnumSuggester.of(FillMode.class, mode -> mode.getDisplayName()))

// Filtered
.suggests(EnumSuggester.ofFiltered(FillMode.class, 
    mode -> mode.name().toLowerCase(),
    mode -> mode.isAvailable()))

// Parsing
FillMode mode = EnumSuggester.parse(FillMode.class, "solid");
FillMode mode2 = EnumSuggester.parse(FillMode.class, "Solid", FillMode::getDisplayName);
```

### RegistrySuggester - Tab Completion from Registries

```java
// Create suggester from registry supplier
RegistrySuggester<FieldRegistry> suggester = new RegistrySuggester<>(FieldRegistry::get);

// Suggest identifiers
.suggests((ctx, builder) -> suggester.suggest(ctx, builder, r -> r.getIds()))

// Filtered suggestions
.suggests((ctx, builder) -> suggester.suggestFiltered(ctx, builder, 
    r -> r.getIds(),
    id -> id.startsWith("the-virus-block:")))

// Static helpers
RegistrySuggester.suggestIdentifiers(builder, identifiers);
RegistrySuggester.suggestStrings(builder, List.of("option1", "option2"));
```

### ReportBuilder - Multi-Line Status Output

```java
ReportBuilder.create("Field Status")
    .kv("Definition", fieldId)
    .kv("Radius", radius)
    .kv("Active", true, Formatting.GREEN)  // Custom color
    .section("Layers", s -> s
        .kv("Count", layers.size())
        .kv("Visible", visibleCount))
    .section("Transform", s -> s
        .kv("Anchor", "center")
        .kv("Scale", 1.0))
    .blank()  // Empty line
    .line("Use /fieldtest edit to modify", Formatting.GRAY)
    .send(source);

// Output:
// ═══ Field Status ═══
//  • Definition: the-virus-block:test_sphere
//  • Radius: 10.0
//  • Active: true
// Layers:
//    • Count: 3
//    • Visible: 3
// Transform:
//    • Anchor: center
//    • Scale: 1.0
//
// Use /fieldtest edit to modify
```

### ListFormatter - List Output with Tags

```java
ListFormatter.create("Available Profiles:")
    .emptyMessage("No profiles found.")
    .showCount(true)  // Shows "(5)" after header
    .items(profiles, profile -> {
        var entry = ListFormatter.entry(profile.id().toString());
        entry.tagIf(profile.isActive(), "active", Formatting.GREEN);
        entry.tagIf(profile.hasErrors(), "error", Formatting.RED);
        return entry;
    })
    .send(source);

// Output:
// Available Profiles: (5)
//  • the-virus-block:test_sphere [active]
//  • the-virus-block:test_ring
//  • the-virus-block:broken_profile [error]
//  • ...
```

### CommandFormatters - Value Formatting

```java
// Numbers
CommandFormatters.formatDouble(3.14159);     // "3.142"
CommandFormatters.formatFloat(1.0f);          // "1"
CommandFormatters.formatFloat(1.5f);          // "1.500"

// Positions
CommandFormatters.formatPos(new BlockPos(10, 64, -5));  // "10, 64, -5"
CommandFormatters.formatBox(box);                        // "[0.00, 0.00, 0.00 -> 1.00, 1.00, 1.00]"

// Duration
CommandFormatters.formatDuration(5200);          // "5s"
CommandFormatters.formatDuration(150000);        // "2m 30s"
CommandFormatters.formatDurationTicks(1200);     // "1m"
CommandFormatters.formatDurationPrecise(1500);   // "1.5s"

// Generic
CommandFormatters.formatAny(value);  // Auto-detects type
```

### FieldCommandBuilder - Dynamic Command Trees

For building commands from field enums:

```java
FieldCommandBuilder<FieldParam, FieldRegistry> builder = 
    new FieldCommandBuilder<>(new RegistrySuggester<>(FieldRegistry::get));

// Profile selection commands
builder.attachProfileCommands(parent, "profile", List.of(
    new ProfileSpec<>("sphere", FieldParam.SPHERE_PROFILE, r -> r.getSphereProfiles()),
    new ProfileSpec<>("ring", FieldParam.RING_PROFILE, r -> r.getRingProfiles())
), this::handleProfile);

// Value commands
builder.attachValueCommands(parent, "set",
    List.of(FieldParam.ENABLED, FieldParam.VISIBLE),  // boolean
    List.of(FieldParam.SEGMENTS, FieldParam.LAYERS),  // int
    List.of(FieldParam.RADIUS, FieldParam.ALPHA),     // double
    param -> param.name().toLowerCase(),
    this::handleValue);
```

---

## 7. Quick Reference Tables

### CommandKnob Types

| Type | Method | Arguments | Example |
|------|--------|-----------|---------|
| Toggle | `.toggle()` | enable/disable | Spin on/off |
| Integer | `.value()` | integer with range | Segments 8-256 |
| Float | `.floatValue()` | float with range | Alpha 0.0-1.0 |
| Enum | `.enumValue()` | enum selection | FillMode |
| Action | `.action()` | no value | Reload |

### CommandKnob Builder Methods

| Method | Purpose | Required? |
|--------|---------|-----------|
| `.defaultValue(x)` | Set default, auto-register | ✅ Yes |
| `.handler((src, val) -> {})` | Handle value change | ✅ Yes |
| `.range(min, max)` | Set valid range | ❌ No |
| `.unit("blocks")` | Display unit | ❌ No |
| `.scenarioRequired(check)` | Pre-validation | ❌ No |
| `.successMessage("...")` | Custom success msg (action) | ❌ No |
| `.idMapper(e -> name)` | Enum display name | ❌ No |
| `.parser(str -> enum)` | Enum parser | ❌ No |
| `.attach(parent)` | Add to command tree | ✅ Yes |

### Feedback Colors

| Method | Color | Use Case |
|--------|-------|----------|
| `success()` | Green | Operation succeeded |
| `error()` | Red | Operation failed |
| `info()` | Gray | Informational |
| `warn()` | Yellow | Caution |
| `highlight()` | Aqua | Important info |

### Logging Levels

| Level | Method | When to Use |
|-------|--------|-------------|
| ERROR | `.error()` | Something broke |
| WARN | `.warn()` | Unexpected but OK |
| INFO | `.info()` | Milestones |
| DEBUG | `.debug()` | Dev flow |
| TRACE | `.trace()` | Verbose dev |

---

*Reference document for system utilities - use these patterns consistently!*

