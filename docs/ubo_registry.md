# UBO Registry - Centralized Binding Authority

> **Purpose:** Single source of truth for all UBO bindings, sizes, and ownership.
> **Rule:** All binding calls MUST route through this registry. No magic numbers.
> 
> **Implementation:** See `UBORegistry.java`

---

## Binding Convention

| Range | Purpose | Update Policy |
|-------|---------|---------------|
| 0–9 | Base UBOs (engine-level) | Per-frame or on-change |
| 10–19 | Pass/Post UBOs (reserved) | If needed |
| 20–29 | Effect Config UBOs | On preset change (rare) |
| 30–39 | Effect Runtime UBOs | Per-frame (if CPU-driven) |

---

## Base UBOs (The 5-Layer Onion)

| Binding | Block Name | Java Record | GLSL File | Size | Update |
|---------|------------|-------------|-----------|------|--------|
| 0 | `FrameData` | `FrameUBO.java` | `frame.glsl` | 16 bytes | per-frame |
| 1 | `CameraData` | `CameraUBO.java` | `camera.glsl` | 224 bytes | per-frame |
| 2 | `ObjectData` | `ObjectUBO.java` | `object.glsl` | 32 bytes | per-draw |
| 3 | `MaterialData` | `MaterialUBO.java` | `material.glsl` | 32 bytes | rare |
| 4 | `LightData` | `LightUBO.java` | `light.glsl` | 208 bytes | on-change |

### Minecraft-Provided (Not in Registry)

| Block Name | Provider | Notes |
|------------|----------|-------|
| `SamplerInfo` | Minecraft | OutSize/InSize, do not modify |

---

## Effect Config UBOs (Preset/Style)

| Binding | Block Name | Java Record | Size (bytes) | Sections |
|---------|------------|-------------|--------------|----------|
| 20 | `FieldVisualConfig` | `FieldVisualConfigUBO.java` | ~TBD | Header, Palette, Shape, Noise, Corona, V2, V8, Reserved |
| 21 | `VirusBlockConfig` | `VirusBlockConfigUBO.java` | ~TBD | Smoke, ESP, Screen, Reserved |
| 22 | `ShockwaveConfig` | `ShockwaveConfigUBO.java` | ~TBD | Ring, Mode, Colors, Reserved |
| 23 | `MagicCircleConfig` | `MagicCircleConfigUBO.java` | ~TBD | Circle, Reserved |
| 24 | `TornadoConfig` | `TornadoConfigUBO.java` | ~TBD | Shape, Peripheral, Noise, Lighting, Reserved |

---

## Effect Runtime UBOs (Per-Frame Instance State)

| Binding | Block Name | Java Record | Size (bytes) | Contents |
|---------|------------|-------------|--------------|----------|
| 30 | `FieldVisualRuntime` | `FieldVisualRuntimeUBO.java` | ~32 | center, radius, phase, intensity, fade |
| 31 | `VirusBlockRuntime` | (optional) | ~16 | If CPU-driven state needed |
| 32 | `ShockwaveRuntime` | `ShockwaveRuntimeUBO.java` | ~32 | ringRadius, progress, etc. |

---

## UBO Size Calculations

### FrameUBO (16 bytes)
```
vec4 FrameTime    // x=time, y=deltaTime, z=frameIndex, w=layoutVersion
```

### CameraUBO (224 bytes)
```
vec4 Position     // xyz=pos, w=reserved                  = 16 bytes
vec4 Forward      // xyz=forward, w=aspect                = 16 bytes
vec4 Up           // xyz=up, w=fov                        = 16 bytes
vec4 Clip         // x=near, y=far, z=isFlying, w=res     = 16 bytes
mat4 ViewProj                                             = 64 bytes
mat4 InvViewProj                                          = 64 bytes
vec4 Reserved[2]  // For future expansion                 = 32 bytes
                                              TOTAL       = 224 bytes
```

---

## File Paths

### Java Records
```
src/client/java/net/cyberpunk042/client/visual/ubo/
├── base/
│   ├── FrameUBO.java
│   └── CameraUBO.java
├── effects/
│   ├── FieldVisualConfigUBO.java
│   ├── FieldVisualRuntimeUBO.java
│   ├── VirusBlockConfigUBO.java
│   ├── ShockwaveConfigUBO.java
│   ├── MagicCircleConfigUBO.java
│   └── TornadoConfigUBO.java
└── UBORegistry.java           # Binding constants
```

### GLSL Includes
```
src/main/resources/assets/the-virus-block/shaders/post/include/ubo/
├── frame.glsl                 # layout(std140) uniform FrameData
├── camera.glsl                # layout(std140) uniform CameraData
└── effects/
    ├── field_visual_config.glsl
    ├── field_visual_runtime.glsl
    ├── virus_block_config.glsl
    ├── shockwave_config.glsl
    └── tornado_config.glsl
```

---

## Target Folder Layout (New Structure)

This is the **target** folder structure after refactor. The `.ubo` naming is conceptual - actual files will be `.java` and `.glsl`.

### Conceptual Organization
```
/ubo (conceptual domain)
 ├─ frame          → FrameUBO.java + frame.glsl
 ├─ camera         → CameraUBO.java + camera.glsl
 ├─ object         → ObjectUBO.java + object.glsl (future)
 ├─ material       → MaterialUBO.java + material.glsl (future)
 ├─ light          → LightUBO.java + light.glsl (future)
 └─ effects/
     ├─ field_visual_config   → FieldVisualConfigUBO.java + field_visual_config.glsl
     ├─ field_visual_runtime  → FieldVisualRuntimeUBO.java + field_visual_runtime.glsl
     ├─ virus_block_config    → VirusBlockConfigUBO.java + virus_block_config.glsl
     ├─ shockwave_config      → ShockwaveConfigUBO.java + shockwave_config.glsl
     ├─ tornado_config        → TornadoConfigUBO.java + tornado_config.glsl
     └─ wormhole_config       → WormholeConfigUBO.java + wormhole_config.glsl (future)
```

### Mapping to Actual Paths

| Conceptual | Java Path | GLSL Path |
|------------|-----------|-----------|
| /ubo/frame | client/visual/ubo/base/FrameUBO.java | include/ubo/frame.glsl |
| /ubo/camera | client/visual/ubo/base/CameraUBO.java | include/ubo/camera.glsl |
| /ubo/effects/field_visual_config | client/visual/ubo/effects/FieldVisualConfigUBO.java | include/ubo/effects/field_visual_config.glsl |

### SamplerInfo Exception
```
SamplerInfo remains where Minecraft expects it - NOT in our UBO folder.
It is provided by the post-effect pipeline and should not be modified.
```

---

## Validation Rules

1. **Size assertion:** At bind time, calculated size must match expected size
2. **Binding collision:** Registry enforces unique binding per block
3. **Layout ID:** Header contains layoutVersion for mismatch detection
4. **Missing annotation:** Fail fast if record component lacks @Vec4/@Mat4

---

## Usage Example

### Java (Binding)
```java
// From UBORegistry
public static final int FRAME_BINDING = 0;
public static final int CAMERA_BINDING = 1;
public static final int FIELD_VISUAL_CONFIG_BINDING = 20;

// Bind call
UBORegistry.bind(FRAME_BINDING, frameUbo);
```

### GLSL (Include)
```glsl
#include "include/ubo/frame.glsl"
#include "include/ubo/camera.glsl"
#include "include/ubo/effects/field_visual_config.glsl"

void main() {
    float time = FrameTime.x;           // From FrameData
    vec3 camPos = CameraPosition.xyz;   // From CameraData
    vec4 primary = PrimaryColor;        // From FieldVisualConfig
}
```

---

> **Maintenance:** Update this registry whenever adding/modifying UBOs.
