# Design Patterns Reference

> **Purpose:** OOP best practices and design patterns for the Field System  
> **Status:** Reference for implementation  
> **Created:** December 08, 2025  
> **Extracted from:** [00_TODO_DIRECTIVES.md](../00_TODO_DIRECTIVES.md)

---

## 📋 Quick Links

- [TODO_DIRECTIVES](../00_TODO_DIRECTIVES.md) - Main workflow guide
- [CLASS_DIAGRAM](../02_CLASS_DIAGRAM.md) - Target classes
- [SYSTEM_UTILITIES](./SYSTEM_UTILITIES.md) - CommandKnob, Logging, Feedback
- [CODE_QUALITY](./CODE_QUALITY.md) - Commenting & logging standards

---

## 🏗️ Core OOP Principles (MANDATORY)

| Principle | Description | Applied To |
|-----------|-------------|------------|
| **SRP** | Single Responsibility - each class does ONE thing | Primitives, Renderers, Configs |
| **OCP** | Open/Closed - open for extension, closed for modification | Pattern system, Shape interface |
| **LSP** | Liskov Substitution - subtypes must be substitutable | All Primitive implementations |
| **ISP** | Interface Segregation - many specific interfaces > one general | Shape, VertexPattern |
| **DIP** | Dependency Inversion - depend on abstractions, not concretions | Renderers use interfaces |

---

## 🔨 Builder Pattern (PRIMARY PATTERN)

Use the **Builder Pattern** for all complex configuration objects:

```java
// ✅ CORRECT: Builder pattern for immutable configs
public record FillConfig(
    FillMode mode,
    float wireThickness,
    boolean doubleSided,
    CageOptions cage
) {
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private FillMode mode = FillMode.SOLID;
        private float wireThickness = 1.0f;
        private boolean doubleSided = false;
        private CageOptions cage = null;
        
        public Builder mode(FillMode mode) { this.mode = mode; return this; }
        public Builder wireThickness(float t) { this.wireThickness = t; return this; }
        public Builder doubleSided(boolean d) { this.doubleSided = d; return this; }
        public Builder cage(CageOptions c) { this.cage = c; return this; }
        public FillConfig build() { 
            return new FillConfig(mode, wireThickness, doubleSided, cage); 
        }
    }
}

// ✅ Usage
FillConfig config = FillConfig.builder()
    .mode(FillMode.CAGE)
    .wireThickness(2.0f)
    .cage(CageOptions.forSphere(8, 16))
    .build();
```

**Apply Builder Pattern to:**
- `FillConfig` - complex nested structure
- `VisibilityMask` - many optional fields
- `Transform` - 15+ fields with defaults
- `ArrangementConfig` - multi-part optional fields
- `SpinConfig`, `PulseConfig` - animation configs
- `BindingConfig`, `TriggerConfig` - external influences
- `LifecycleConfig` - lifecycle options

---

## 🏭 Factory Pattern

Use **Factory Pattern** for primitive instantiation:

```java
// ✅ CORRECT: Factory creates correct primitive type
public class PrimitiveFactory {
    public static Primitive create(String type, JsonObject json) {
        return switch (type) {
            case "sphere" -> SpherePrimitive.fromJson(json);
            case "ring" -> RingPrimitive.fromJson(json);
            case "disc" -> DiscPrimitive.fromJson(json);
            // ... other types
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

---

## 🎯 Strategy Pattern (For Patterns/Algorithms)

Use **Strategy Pattern** for interchangeable algorithms:

```java
// ✅ CORRECT: Patterns are strategies
public interface VertexPattern {
    String id();
    CellType cellType();
    boolean shouldRender(int index, int total);  // Filter cells
    int[][] getVertexOrder();                     // Reorder vertices
}

// Each pattern uses semantic enums internally, returns int[][] for renderer
public enum QuadPattern implements VertexPattern {
    FILLED_1("filled_1",
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT}
    );
    
    @Override
    public int[][] getVertexOrder() {
        // Converts Corner[] to int[][] for rendering
        return cachedVertexOrder;
    }
    
    public enum Corner { TOP_LEFT(0), TOP_RIGHT(1), BOTTOM_LEFT(2), BOTTOM_RIGHT(3); ... }
}
```

---

## 📏 Value Range Validation

Use the **@Range annotation** with **ValueRange enum** for documented, validated numeric bounds:

```java
// ✅ CORRECT: Annotated and validated
public record AlphaRange(
    @Range(ValueRange.ALPHA) float min,
    @Range(ValueRange.ALPHA) float max
) {
    public AlphaRange {
        min = ValueRange.ALPHA.clamp(min);  // Auto-clamp to 0-1
        max = ValueRange.ALPHA.clamp(max);
    }
}
```

### Available ValueRange Constants

| Range | Min | Max | Use For |
|-------|-----|-----|---------|
| `ALPHA` | 0 | 1 | Opacity values |
| `NORMALIZED` | 0 | 1 | Lat/lon ranges, progress |
| `DEGREES` | 0 | 360 | Rotation angles |
| `DEGREES_SIGNED` | -180 | 180 | Signed angles |
| `POSITIVE` | 0 | MAX | Any positive value |
| `POSITIVE_NONZERO` | 0.001 | MAX | Non-zero positive |
| `SCALE` | 0.01 | 100 | Scale factors |
| `RADIUS` | 0.01 | MAX | Radius values |
| `STEPS` | 1 | 1024 | Tessellation steps |
| `SIDES` | 3 | 64 | Polygon sides |

---

## 🔒 Immutability

**ALL configuration objects must be immutable:**

```java
// ✅ CORRECT: Immutable record
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    SphereAlgorithm algorithm
) implements Shape {
    public SphereShape {
        if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
    }
}

// ❌ WRONG: Mutable class
public class SphereShape {
    private float radius; // Mutable!
    public void setRadius(float r) { this.radius = r; } // Setter!
}
```

---

## 🧩 Composition Over Inheritance

```java
// ❌ WRONG: Deep inheritance hierarchy
abstract class SolidPrimitive extends AbstractPrimitive { }
class SpherePrimitive extends SolidPrimitive { }

// ✅ CORRECT: Flat implementation with composition
class SpherePrimitive implements Primitive {
    private final SphereShape shape;      // Composition
    private final Transform transform;     // Composition
    private final FillConfig fill;         // Composition
    private final Appearance appearance;   // Composition
}
```

---

## 🚫 Null Object Pattern

Avoid null checks with Null Object Pattern:

```java
// ✅ CORRECT: Default/empty implementations
public record SpinConfig(Axis axis, float speed, boolean oscillate, float range) {
    public static final SpinConfig NONE = new SpinConfig(Axis.Y, 0, false, 360);
    
    public boolean isActive() { return speed != 0; }
}

// Usage: No null checks needed
SpinConfig spin = primitive.spin() != null ? primitive.spin() : SpinConfig.NONE;
```

---

## 📊 SRP Examples in This Codebase

| Class | Responsibility | What It Doesn't Do |
|-------|---------------|-------------------|
| `SphereShape` | Holds sphere geometry params | Doesn't render, doesn't tessellate |
| `SpherePrimitive` | Combines shape + config + appearance | Doesn't render directly |
| `SphereRenderer` | Renders a SpherePrimitive | Doesn't know about other shapes |
| `SphereTessellator` | Converts SphereShape to Mesh | Doesn't know about rendering |
| `FieldLoader` | Parses JSON into FieldDefinition | Doesn't manage runtime state |
| `FieldManager` | Manages field instances | Doesn't parse JSON |
| `CombatTracker` | Tracks combat state | Doesn't apply field effects |

---

## ⚠️ Anti-Patterns to Avoid

| Anti-Pattern | Example | Problem | Fix |
|--------------|---------|---------|-----|
| God Class | `FieldRenderer` doing parse + render + animate | Violates SRP | Split into focused classes |
| Primitive Obsession | `render(float r, int lat, int lon, String color...)` | Too many params | Use parameter objects |
| Leaky Abstraction | `Primitive.getTriangleList()` | Exposes implementation | Use `Tessellator` separately |
| instanceof Chains | `if (p instanceof Sphere) else if (p instanceof Ring)...` | Violates OCP | Use polymorphism |
| Magic Numbers | `alpha = 0.35f` | Unclear intent | Use named constants or config |

---

*Reference document for design patterns - see [SYSTEM_UTILITIES.md](./SYSTEM_UTILITIES.md) for CommandKnob/Logging patterns*
