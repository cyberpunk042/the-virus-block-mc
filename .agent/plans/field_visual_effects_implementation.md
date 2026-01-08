# Field Visual Effects System - Implementation Plan

## Overview

A post-process effect system for rendering advanced visual effects on energy fields.
Supports multiple concurrent fields and extensible effect types.

**Reference Effect:** Rasengan-style energy orb (see Shadertoy shader)
- Bright white core with bloom
- Cyan/blue edge ring with glow
- Twisting internal Voronoi patterns (double spiral)
- Radial glow lines emanating outward
- Color gradient: white → cyan → blue → purple
- All animated and time-synchronized

---

## Phase 1: Core Infrastructure

### 1.1 Data Structures

#### Task 1.1.1: EffectType Enum
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/EffectType.java`
```java
public enum EffectType {
    NONE,           // No post-effect (mesh-only rendering)
    ENERGY_ORB,       // Full orb with core, spirals, glow lines
    SHIELD,         // Hexagonal force field
    AURA,           // Flowing flame-like upward energy
    PORTAL,         // Vortex with depth distortion
    // Future: ELEMENTAL_FIRE, ELEMENTAL_ICE, DIVINE, VOID, TECH
}
```
**Status:** [ ] Not Started

#### Task 1.1.2: FieldVisualConfig Record
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualConfig.java`
```java
public record FieldVisualConfig(
    EffectType effectType,
    Color primaryColor,      // Core/main color
    Color secondaryColor,    // Edge/accent color  
    Color tertiaryColor,     // Outer glow/background tint
    float intensity,         // Overall brightness (0-2)
    float animationSpeed,    // Time multiplier (0.5-3.0)
    float coreSize,          // Relative core size (0.1-0.5)
    float edgeSharpness,     // Edge ring sharpness (1-10)
    float spiralDensity,     // Voronoi cell density (3-12)
    float spiralTwist,       // Twist strength (1-10)
    float glowLineCount,     // Radial line count (8-24)
    float glowLineIntensity  // Line brightness (0-1)
) {
    public static FieldVisualConfig defaultRasengan() {
        return new FieldVisualConfig(
            EffectType.RASENGAN,
            Color.WHITE,           // Core
            Color.CYAN,            // Edge
            new Color(0.1f, 0.02f, 0.15f), // Purple tint
            1.2f, 1.0f, 0.15f, 4.0f, 5.0f, 5.0f, 16.0f, 0.8f
        );
    }
}
```
**Status:** [ ] Not Started

#### Task 1.1.3: FieldVisualInstance Class
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualInstance.java`
```java
public class FieldVisualInstance {
    private UUID id;                    // Unique identifier
    private UUID ownerId;               // Player/entity owner (for network sync)
    private Vec3d worldCenter;          // World position
    private float radius;               // Field radius in blocks
    private ShapeType shapeType;        // SPHERE, TORUS, etc.
    private FieldVisualConfig config;   // Visual configuration
    private float animationPhase;       // Current animation time offset
    
    // Updated from Field system each frame
    public void updatePosition(Vec3d pos) { ... }
    public void updateRadius(float r) { ... }
}
```
**Status:** [ ] Not Started

#### Task 1.1.4: FieldVisualRegistry Class
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualRegistry.java`
```java
public class FieldVisualRegistry {
    private static final Map<UUID, FieldVisualInstance> activeFields = new ConcurrentHashMap<>();
    private static final int MAX_RENDERED_FIELDS = 8;  // Shader UBO limit
    
    public static void register(UUID id, FieldVisualInstance instance) { ... }
    public static void unregister(UUID id) { ... }
    public static void update(UUID id, Vec3d position, float radius) { ... }
    public static List<FieldVisualInstance> getActiveFields() { ... }
    public static List<FieldVisualInstance> getClosestFields(Vec3d camera, int max) { ... }
}
```
**Status:** [ ] Not Started

---

### 1.2 UBO Layout

#### Task 1.2.1: FieldVisualUBO Class
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/uniform/FieldVisualUBO.java`

UBO Layout (std140, per field = 128 bytes, max 8 fields = 1024 bytes):
```
struct FieldData {
    vec4 CenterAndRadius;       // xyz = world pos, w = radius
    vec4 PrimaryColor;          // rgba
    vec4 SecondaryColor;        // rgba
    vec4 TertiaryColor;         // rgba
    vec4 AnimParams;            // x=time, y=speed, z=intensity, w=effectType
    vec4 CoreEdgeParams;        // x=coreSize, y=edgeSharpness, z=padding, w=padding
    vec4 SpiralParams;          // x=density, y=twist, z=padding, w=padding
    vec4 GlowLineParams;        // x=count, y=intensity, z=padding, w=padding
};

layout(std140) uniform FieldVisualParams {
    int fieldCount;             // Active field count
    vec3 padding;
    mat4 invViewProjection;     // For world pos reconstruction
    vec4 cameraPosition;        // xyz = camera pos, w = padding
    vec4 time;                  // x = global time, yzw = padding
    FieldData fields[8];        // Array of field data
};
```
**Status:** [ ] Not Started

#### Task 1.2.2: FieldVisualUniformBinder Class
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/uniform/FieldVisualUniformBinder.java`
- Builds UBO from FieldVisualRegistry
- Packs all active fields
- Passes camera matrices
- Binds to RenderPass
**Status:** [ ] Not Started

---

## Phase 2: Post-Effect Pipeline

### 2.1 Effect Manager

#### Task 2.1.1: FieldVisualPostEffect Class
**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualPostEffect.java`
```java
public class FieldVisualPostEffect {
    private static boolean enabled = true;
    private static PostEffectProcessor processor;
    private static float globalTime = 0f;
    
    public static void init() { ... }
    public static void tick() { globalTime += 0.05f; }  // Called each frame
    public static PostEffectProcessor loadProcessor() { ... }
    public static boolean isEnabled() { return enabled; }
    public static float getGlobalTime() { return globalTime; }
    
    // Camera data (updated by mixin)
    public static void updateCamera(Vec3d pos, Matrix4f invViewProj) { ... }
}
```
**Status:** [ ] Not Started

### 2.2 Render Hook

#### Task 2.2.1: WorldRendererFieldVisualMixin
**File:** `src/client/java/net/cyberpunk042/mixin/client/WorldRendererFieldVisualMixin.java`
- Inject before FrameGraphBuilder.run()
- Update camera position and matrices
- Sync field positions from registry
- Add post-effect pass to frame graph
**Status:** [ ] Not Started

#### Task 2.2.2: Post-Effect JSON Definition
**File:** `src/main/resources/assets/the-virus-block/post_effect/field_visual.json`
```json
{
    "targets": ["swap"],
    "passes": [
        {
            "program": "the-virus-block:post/field_visual",
            "intarget": "minecraft:main",
            "outtarget": "swap",
            "auxtargets": [
                { "id": "DepthSampler", "name": "minecraft:main:depth" }
            ]
        },
        {
            "program": "minecraft:blit",
            "intarget": "swap",
            "outtarget": "minecraft:main"
        }
    ]
}
```
**Status:** [ ] Not Started

---

## Phase 3: Shader Implementation

### 3.1 Base Shaders

#### Task 3.1.1: Vertex Shader
**File:** `src/main/resources/assets/the-virus-block/shaders/post/field_visual.vsh`
- Fullscreen quad vertex shader
- Pass texCoord to fragment
**Status:** [ ] Not Started

#### Task 3.1.2: Shader Program JSON
**File:** `src/main/resources/assets/the-virus-block/shaders/post/field_visual.json`
- Declare samplers: InSampler, DepthSampler
- Uniforms reference
**Status:** [ ] Not Started

### 3.2 GLSL Include Library

#### Task 3.2.1: SDF Primitives
**File:** `src/main/resources/assets/the-virus-block/shaders/include/sdf.glsl`
```glsl
float sdSphere(vec3 p, float r) { return length(p) - r; }
float sdTorus(vec3 p, vec2 t) { ... }
float sdLine(vec2 p, vec2 a, vec2 b, float r) { ... }
// etc.
```
**Status:** [ ] Not Started

#### Task 3.2.2: Noise Functions
**File:** `src/main/resources/assets/the-virus-block/shaders/include/noise.glsl`
```glsl
vec2 hash22(vec2 p) { ... }
float voronoi(vec2 uv, float cellDensity, float time) { ... }
float perlin(vec2 p) { ... }
```
**Status:** [ ] Not Started

#### Task 3.2.3: Transform Functions
**File:** `src/main/resources/assets/the-virus-block/shaders/include/transforms.glsl`
```glsl
mat2 rotate2D(float angle) { ... }
vec2 twirlUV(vec2 uv, vec2 center, float strength) { ... }
vec3 worldPosFromDepth(vec2 uv, float depth, mat4 invViewProj) { ... }
```
**Status:** [ ] Not Started

#### Task 3.2.4: Color Utilities
**File:** `src/main/resources/assets/the-virus-block/shaders/include/color.glsl`
```glsl
vec3 hsv2rgb(vec3 c) { ... }
vec3 rgb2hsv(vec3 c) { ... }
float glowFalloff(float dist, float radius) { ... }
```
**Status:** [ ] Not Started

### 3.3 Energy Orb Effect Shader

#### Task 3.3.1: Main Fragment Shader
**File:** `src/main/resources/assets/the-virus-block/shaders/post/field_visual.fsh`

Structure:
1. Sample scene and depth
2. Reconstruct world position from depth
3. For each active field:
   a. Calculate distance to field center
   b. If within effect range:
      - Render core glow
      - Render edge ring
      - Render Voronoi spirals
      - Render glow lines
   c. Composite with scene
4. Output final color

Key functions to implement:
- `renderEnergyOrbCore()`
- `renderEnergyOrbEdge()`
- `renderEnergyOrbSpirals()`
- `renderEnergyOrbGlowLines()`
- `compositeEffect()`

**Status:** [ ] Not Started

---

## Phase 4: Integration

### 4.1 Field System Connection

#### Task 4.1.1: Connect to Field Lifecycle
- When Field created → register in FieldVisualRegistry
- When Field moves → update position
- When Field destroyed → unregister
**Status:** [ ] Not Started

#### Task 4.1.2: FieldVisualConfig in Field Class
- Add `visualConfig` field to Field class
- Allow GUI to modify visual settings
**Status:** [ ] Not Started

### 4.2 Network Synchronization

#### Task 4.2.1: FieldVisualSyncS2CPayload
- Server → Client packet for other players' fields
- Contains: UUID, position, radius, effectType, colors
**Status:** [ ] Not Started

### 4.3 GUI Integration

#### Task 4.3.1: Visual Effect Controls
- Add to existing Shape Panel or new panel
- Effect type dropdown
- Color pickers
- Intensity/speed sliders
- Preset buttons
**Status:** [ ] Not Started

---

## Implementation Order

### Sprint 1: Foundation ✅ COMPLETE
1. [x] Task 1.1.1: EffectType enum
2. [x] Task 1.1.2: FieldVisualConfig record
3. [x] Task 1.1.3: FieldVisualInstance class
4. [x] Task 1.1.4: FieldVisualRegistry class

### Sprint 2: Pipeline ✅ COMPLETE
5. [x] Task 1.2.1: FieldVisualUBO layout
6. [x] Task 1.2.2: FieldVisualUniformBinder
7. [x] Task 2.1.1: FieldVisualPostEffect manager
8. [x] Task 2.2.1: WorldRendererFieldVisualMixin
9. [x] Task 2.2.2: Post-effect JSON

### Sprint 3: Basic Shader ✅ COMPLETE
10. [x] Task 3.1.1: Vertex shader
11. [x] Task 3.1.2: Shader program JSON
12. [x] Task 3.3.1: Fragment shader (basic version - core + edge)

### Sprint 4: GLSL Library
13. [ ] Task 3.2.1: SDF primitives
14. [ ] Task 3.2.2: Noise functions
15. [ ] Task 3.2.3: Transform functions
16. [ ] Task 3.2.4: Color utilities

### Sprint 5: Full Energy Orb Effect
17. [ ] renderEnergyOrbCore() - bright center bloom
18. [ ] renderEnergyOrbEdge() - cyan ring with glow
19. [ ] renderEnergyOrbSpirals() - twisting Voronoi
20. [ ] renderEnergyOrbGlowLines() - radial beams

### Sprint 6: Integration
21. [ ] Task 4.1.1: Field lifecycle connection
22. [ ] Task 4.1.2: VisualConfig in Field
23. [ ] Task 4.3.1: GUI controls

### Sprint 7: Multi-Player
24. [ ] Task 4.2.1: Network sync

---

## Technical Notes

### World Position Reconstruction
To know where a field is relative to each pixel, we reconstruct world position:
```glsl
vec3 worldPos = worldPosFromDepth(texCoord, depth, invViewProj);
float distToField = length(worldPos - fieldCenter);
```

### Edge Detection (for silhouette glow)
For glow that extends beyond the mesh:
```glsl
float depthEdge = detectEdge(DepthSampler, texCoord);
// Glow where depth changes sharply
```

### Multi-Field Loop
```glsl
for (int i = 0; i < fieldCount; i++) {
    FieldData field = fields[i];
    vec3 toField = worldPos - field.CenterAndRadius.xyz;
    float dist = length(toField);
    float radius = field.CenterAndRadius.w;
    
    if (dist < radius * 2.0) {  // Within effect range
        // Apply effect
    }
}
```

---

## Success Criteria

✅ System renders Energy Orb effect matching reference image
✅ Effect follows field position when player moves
✅ Multiple fields can render simultaneously
✅ No major performance impact (< 2ms per frame)
✅ Adding new effect types requires only new shader file + config
