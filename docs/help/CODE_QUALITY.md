# Code Quality Standards

> **Purpose:** Commenting, documentation, and logging standards  
> **Status:** Reference for implementation  
> **Created:** December 08, 2025  

---

## 📋 Quick Links

- [DESIGN_PATTERNS](./DESIGN_PATTERNS.md) - OOP principles
- [SYSTEM_UTILITIES](./SYSTEM_UTILITIES.md) - CommandKnob, Logging API details

---

## 📝 Commenting & Documentation

Every class must have:

### 1. Class-level Javadoc

```java
/**
 * Renders sphere primitives using the tessellation pipeline.
 * 
 * <p>Supports all FillModes (SOLID, WIREFRAME, CAGE, POINTS) and 
 * applies VisibilityMask filtering before emission.</p>
 * 
 * @see SphereTessellator
 * @see VertexEmitter
 */
public class SphereRenderer implements PrimitiveRenderer {
```

### 2. Method-level Comments for Non-trivial Logic

```java
// Apply layer spin first (rotates entire layer around anchor)
// Then apply primitive spin (rotates individual shape)
// This enables "orbiting orbs that also spin" effect
matrices.multiply(layerRotation);
matrices.multiply(primitiveRotation);
```

### 3. Section Markers for Long Methods

```java
// === PHASE 1: Tessellation ===
Mesh mesh = tessellator.tessellate(shape, pattern, visibility);

// === PHASE 2: Transform ===
applyTransform(matrices, primitive.transform(), time);

// === PHASE 3: Emission ===
emitter.emitMesh(consumer, mesh, matrices.peek(), color, light);
```

---

## 📊 Logging Standards

Every significant class must integrate with the Logging system.

### 1. Use the FIELD Channel

```java
private static final Channel LOG = Logging.FIELD;
```

### 2. Constructor Logging (DEBUG level)

```java
Logging.of(LOG).topic("init").debug("SphereRenderer initialized");
```

### 3. Operation Logging (TRACE level for hot paths)

```java
Logging.of(LOG).topic("render")
    .with("primitive", primitive.id())
    .with("fillMode", fill.mode())
    .trace("Rendering sphere");
```

### 4. Error Logging with Context

```java
Logging.of(LOG).topic("pattern")
    .with("expected", shape.primaryCellType())
    .with("got", pattern.cellType())
    .alwaysChat()  // Shows in player chat
    .error("Pattern CellType mismatch");
```

### 5. Performance Logging (for expensive operations)

```java
long start = System.nanoTime();
// ... expensive operation ...
Logging.of(LOG).topic("perf")
    .with("operation", "tessellate")
    .with("ms", (System.nanoTime() - start) / 1_000_000.0)
    .debug("Tessellation complete");
```

---

## 📌 NOTE Comments

Add `// NOTE:` comments for:
- Design decisions that aren't obvious
- Workarounds for Minecraft API quirks
- Performance considerations
- Future improvement opportunities

```java
// NOTE: We use QUADS render layer even for triangles because
// Minecraft's line rendering has Z-fighting issues with translucent
// surfaces. Converting to degenerate quads avoids this.

// NOTE: This could be optimized by caching the mesh, but we
// regenerate each frame to support dynamic patterns. Consider
// caching for static patterns in Phase 2.

// NOTE: Minecraft's VertexConsumer expects ARGB, not RGBA.
// Always pack colors with (alpha << 24) | (red << 16) | ...
```

---

## 🔧 System Utilities Integration

Every command-related class must use **CommandKnob** and **CommandFeedback**:

```java
// ✅ CORRECT: Use CommandKnob for all command parameters
CommandKnob.floatValue("field.radius", "Radius")
    .range(0.1f, 100f)
    .unit("blocks")
    .defaultValue(10f)
    .handler((src, radius) -> {
        Logging.FIELD.topic("command").kv("radius", radius).debug("Setting radius");
        return FieldManager.setRadius(radius);
    })
    .attach(parent);

// ❌ WRONG: Manual command building without CommandKnob
parent.then(literal("radius")
    .then(argument("value", FloatArgumentType.floatArg(0.1f, 100f))
        .executes(ctx -> { ... })));  // No protection, no defaults, no feedback
```

**For full API documentation see:** [SYSTEM_UTILITIES.md](./SYSTEM_UTILITIES.md)

---

*Reference document for code quality - see [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) for OOP patterns*
