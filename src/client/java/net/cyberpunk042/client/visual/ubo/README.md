# UBO Package Guide

> **Location:** `net.cyberpunk042.client.visual.ubo`
> **Purpose:** Domain-oriented Uniform Buffer Object architecture

---

## Quick Start

### Creating a Vec4 for UBO

**Clean pattern (recommended):**
```java
// Just declare a record with 4 float fields
public record PositionVec4(float x, float y, float z, float w) {}
```

The `ReflectiveUBOWriter` automatically detects records with 4 float components and reads them in declaration order.

**No need for:**
- `implements Vec4Serializable`
- `@Override slot0/1/2/3` methods
- Any boilerplate

---

## Package Structure

```
ubo/
├── UBORegistry.java           # Binding constants (SINGLE SOURCE OF TRUTH)
├── Vec4Serializable.java      # Legacy interface (still supported)
├── ReflectiveUBOWriter.java   # Serializes records to Std140
├── GLSLValidator.java         # Validates GLSL files
├── annotation/
│   ├── UBOStruct.java         # Marks a record as UBO
│   ├── Vec4.java              # 16-byte vec4 slot
│   ├── Mat4.java              # 64-byte mat4 slot
│   ├── Vec4Array.java         # Array of vec4
│   └── Floats.java            # Raw floats with padding
└── base/
    ├── FrameUBO.java          # Binding 0: time, deltaTime, frameIndex
    ├── CameraUBO.java         # Binding 1: position, vectors, matrices
    ├── ObjectUBO.java         # Binding 2: per-instance (stub)
    ├── MaterialUBO.java       # Binding 3: surface props (stub)
    └── LightUBO.java          # Binding 4: 4 lights for volumetric
```

---

## The 5-Layer Onion Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Frame (0)                         │  ← Global per-frame
│  ┌─────────────────────────────────────────────┐    │
│  │                Camera (1)                    │    │  ← View definition
│  │  ┌─────────────────────────────────────┐    │    │
│  │  │             Object (2)               │    │    │  ← Per-instance
│  │  │  ┌─────────────────────────────┐    │    │    │
│  │  │  │         Material (3)         │    │    │    │  ← Surface props
│  │  │  │  ┌─────────────────────┐    │    │    │    │
│  │  │  │  │      Light (4)       │    │    │    │    │  ← Scene lights
│  │  │  │  │  ┌─────────────┐    │    │    │    │    │
│  │  │  │  │  │EffectConfig │    │    │    │    │    │  ← Preset/style (20+)
│  │  │  │  │  └─────────────┘    │    │    │    │    │
│  │  │  │  └─────────────────────┘    │    │    │    │
│  │  │  └─────────────────────────────┘    │    │    │
│  │  └─────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

## Creating a New UBO

### 1. Define the Record

```java
@UBOStruct(name = "MyEffectConfig")
public record MyEffectConfigUBO(
    @Vec4 ColorVec4 primaryColor,
    @Vec4 ShapeVec4 shapeParams,
    @Mat4 Matrix4f transform
) {
    // Clean vec4 records - no boilerplate needed!
    public record ColorVec4(float r, float g, float b, float a) {}
    public record ShapeVec4(float size, float sharpness, float power, float reserved) {}
    
    // Factory method
    public static MyEffectConfigUBO from(Color c, float size, float sharpness) {
        return new MyEffectConfigUBO(
            new ColorVec4(c.r, c.g, c.b, c.a),
            new ShapeVec4(size, sharpness, 2.0f, 0),
            new Matrix4f()
        );
    }
}
```

### 2. Register Binding in UBORegistry

```java
// In UBORegistry.java
public static final int MY_EFFECT_CONFIG_BINDING = 25;  // Effect configs: 20-29
```

### 3. Create GLSL Include

```glsl
// shaders/post/include/ubo/effects/my_effect_config.glsl
layout(std140) uniform MyEffectConfig {
    vec4 PrimaryColor;     // rgba
    vec4 ShapeParams;      // size, sharpness, power, reserved
    mat4 Transform;
};
```

### 4. Write to Buffer

```java
FrameUBO frame = FrameUBO.from(time, deltaTime, frameIndex);
ReflectiveUBOWriter.write(builder, frame);
```

---

## Binding Convention

| Range | Purpose | Examples |
|-------|---------|----------|
| 0-9 | Base UBOs | Frame, Camera, Object, Material, Light |
| 10-19 | Reserved | Future pass-specific |
| 20-29 | Effect Config | FieldVisualConfig, ShockwaveConfig, TornadoConfig |
| 30-39 | Effect Runtime | Per-frame instance state |

---

## Update Frequency Rules

| UBO | Update Policy |
|-----|---------------|
| Frame | Every frame (smallest, ring-buffer candidate) |
| Camera | Every frame |
| Object | Per draw/instance |
| Material | Rarely (cached) |
| Light | On light changes |
| EffectConfig | On preset change (rare) |
| EffectRuntime | Per frame if CPU-driven |

**Golden Rule:** Let GPU animate via `FrameTime.x`. Only update CPU-side if you truly need CPU-driven curves.

---

## Common Patterns

### Reserved Lanes

Always add reserved fields for future expansion:

```java
public record MyParamsVec4(float used1, float used2, float reserved1, float reserved2) {}
```

### Factory Methods

Provide clean construction:

```java
public static MyUBO from(Config config, Camera cam) {
    return new MyUBO(
        ParamsVec4.from(config),
        CameraVec4.from(cam)
    );
}
```

### Default Values

Provide sensible defaults:

```java
public record ShapeVec4(...) {
    public static final ShapeVec4 DEFAULT = new ShapeVec4(1.0f, 1.0f, 2.0f, 0);
}
```

---

## Validation

```java
// Check size matches expected
int calculated = ReflectiveUBOWriter.calculateBufferSize(MyUBO.class);
UBORegistry.validateSize("MyUBO", calculated, EXPECTED_SIZE);
```

---

## Migration from Legacy Pattern

**Old (verbose):**
```java
public record PosVec4(float x, float y, float z, float w) implements Vec4Serializable {
    @Override public float slot0() { return x; }
    @Override public float slot1() { return y; }
    @Override public float slot2() { return z; }
    @Override public float slot3() { return w; }
}
```

**New (clean):**
```java
public record PosVec4(float x, float y, float z, float w) {}
```

Both work! The `ReflectiveUBOWriter` tries `Vec4Serializable` first for backward compatibility, then auto-detects records.

---

## GLSL Counterparts

Each Java UBO has a matching GLSL include in:
```
shaders/post/include/ubo/
├── frame.glsl
├── camera.glsl
├── object.glsl
├── material.glsl
├── light.glsl
└── effects/
    └── ...
```

Include pattern:
```glsl
#include "include/ubo/frame.glsl"
#include "include/ubo/camera.glsl"

void main() {
    float time = FrameTime.x;
    vec3 camPos = CameraPosition.xyz;
}
```
