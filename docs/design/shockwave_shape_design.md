# Shockwave Effect - Design Document

## Overview

**Shockwave is an EFFECT, not a Shape.**

It attaches to **ONE source primitive** and generates expanding/contracting rings that **contour to terrain**. The rings follow the source shape's outline and project onto the world surface.

---

## Core Feature: Terrain Contouring

This is NOT optional - it's what the shockwave IS.

The rings:
- Project from the source shape onto the ground
- Follow terrain height (block surfaces)
- Flow around obstacles organically
- Create dramatic ripple effects across the landscape

---

## Stage System

Uses the existing `ShapeState` pattern:

| Stage | Description |
|-------|-------------|
| **SPAWNING** | Rings expand outward from source |
| **ACTIVE** | Rings at full radius, adapts to source shape changes |
| **DESPAWNING** | Rings contract back to center |

Each stage has a **phase [0-1]** that controls progression within that stage.

### ACTIVE Stage Behavior
- Naturally adaptive to source shape evolution
- If source changes from sphere → sphere with 5 orbiting spheres
- The ring pattern adapts to the new "flower" outline

---

## Effect vs Shape

| Type | Scope | Example |
|------|-------|---------|
| Shape | Defines geometry | Sphere, Kamehameha, OmegaBlast |
| Per-Primitive Effect | Applied to each primitive | Rim/Corona shader |
| **Per-Field Effect** | Applied to ONE source primitive | **Shockwave**, Screen Blackout |

---

## How It Works

1. User selects a primitive in the field
2. User enables "Shockwave Effect" in the Special Effects panel
3. User sets Stage (SPAWNING, ACTIVE, DESPAWNING) and Phase [0-1]
4. The effect computes rings from source shape outline
5. Rings project onto terrain, contouring to surface
6. Effect is saved as part of the field definition

---

## Technical Implementation

### Approach: Projection Volume + Depth Buffer Shader

This is a **shader-based effect**, NOT a tessellated 3D mesh.

#### Step 1: Render Terrain
- Minecraft renders terrain as normal
- Depth buffer contains distance of each pixel from camera

#### Step 2: Render Projection Volume
- Render an **invisible simple mesh** (flat disc at shockwave location)
- This mesh defines WHERE the shader runs (optimization)
- The mesh itself is not visible - it just triggers the shader

#### Step 3: Fragment Shader
For each pixel covered by the projection volume:
1. **Read depth buffer** → get terrain surface distance
2. **Reconstruct world position** → find actual 3D point on terrain
3. **Calculate 2D distance** from shockwave center (XZ plane)
4. **Apply ring pattern** based on distance, radius, thickness, phase
5. **Output glow color** blended onto terrain

### Why This Works
- Rings automatically "contour" terrain because we sample the **actual surface** via depth
- No 3D mesh tessellation needed for the rings themselves
- Shader does all the visual work
- Projection volume is just for tracking/optimization (invisible)

### Key Shader Uniforms
- `centerPosition` - world position of shockwave origin
- `maxRadius` - current expansion radius
- `ringCount` - number of rings
- `ringThickness` - width of each ring
- `phase` - 0-1 progress within stage
- `intensity` - glow strength
- `baseColor` / `edgeColor` - gradient colors

---

## Configurable Parameters

### Core Settings

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `enabled` | boolean | Enable/disable the effect | false |
| `stage` | Stage | SPAWNING, ACTIVE, DESPAWNING | SPAWNING |
| `phase` | float | Progress within stage [0-1] | 0.0 |
| `intensity` | float | Overall effect intensity | 1.0 |
| `edgeMode` | Mode | How ring edges transition | FADE |
| `edgeIntensity` | float | Edge effect strength | 1.0 |

### Ring Configuration

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `ringCount` | int | Number of concentric rings | 5 |
| `maxRadius` | float | Maximum expansion radius | 20.0 |
| `ringThickness` | float | Base width of each ring | 0.5 |
| `thicknessMode` | Mode | How thickness changes per ring | UNIFORM |
| `thicknessFactor` | float | Multiplier for thickness modes | 1.0 |

### Thickness Modes
- `UNIFORM` - All rings same thickness
- `GROW_OUTWARD` - Rings get thicker toward edge
- `SHRINK_OUTWARD` - Rings get thinner toward edge

### Spacing

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `spacingMode` | Mode | How rings are distributed | EVEN |
| `spacingFactor` | float | Multiplier for spacing modes | 1.0 |

### Spacing Modes
- `EVEN` - Equal distance between rings
- `EXPONENTIAL` - Rings get farther apart toward edge
- `LOGARITHMIC` - Rings get closer together toward edge

### Appearance

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `baseColor` | Color | Ring color near center | WHITE |
| `edgeColor` | Color | Ring color at outer edge | CYAN |
| `alpha` | float | Overall opacity | 1.0 |
| `alphaFalloff` | Mode | How alpha decreases | LINEAR |
| `glowIntensity` | float | Shader glow strength | 1.0 |

### Terrain Sampling

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `surfaceOffset` | float | Height above terrain surface | 0.1 |
| `sampleDensity` | int | Sample points per ring | 64 |
| `smoothing` | float | Smooth height transitions | 0.5 |

### Animation Speed

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `speedMode` | Mode | Speed curve type | LINEAR |
| `speedFactor` | float | Speed multiplier | 1.0 |

### Speed Modes
- `LINEAR` - Constant speed
- `EASE_IN` - Starts slow, accelerates
- `EASE_OUT` - Starts fast, decelerates
- `EASE_IN_OUT` - Slow start and end

---

## Source Shape Adaptation

### Simple Source
- Source = Sphere → Circular ring pattern

### Complex Source (e.g., OmegaBlast)
- Source internally manages: center sphere + 5 orbiting spheres
- During ACTIVE stage, as spheres orbit out, ring pattern adapts
- Creates "flower" shaped rings automatically

---

## UI Integration

- Located in: **Special Effects Panel**
- Available for: Any primitive
- Controls: Stage dropdown, Phase slider, all parameters above
- Saved as: Part of the field definition

---

## Infrastructure Requirements

### What Exists ✅

| Component | Location | Notes |
|-----------|----------|-------|
| Custom RenderPipelines | `FresnelPipelines.java` | Pattern for registering shader pipelines |
| UBO Binding System | `CustomUniformBinder.java` | Binds uniform buffers via Mixin |
| Fresnel Shader | `fresnel_entity.fsh` | Edge detection using NdotV |
| RenderLayer Factory | `FresnelRenderLayers.java` | Creates custom render layers |

### What Needs to Be Built ❌

| Component | Description | Priority |
|-----------|-------------|----------|
| **Depth Buffer Sampler** | Pass depth texture to shader | HIGH |
| **World Position Reconstruction** | Calculate 3D from depth + camera matrices | HIGH |
| **Shockwave Fragment Shader** | Ring pattern based on distance from center | HIGH |
| **Projection Volume Mesh** | Simple disc to trigger shader | MEDIUM |
| **ShockwavePipeline** | New RenderPipeline with depth sampler | HIGH |
| **ShockwaveEffect Config** | Stage, phase, ring params, colors | MEDIUM |
| **Linear Depth Conversion** | MC uses non-linear depth, need conversion | HIGH |

### Key Shader Uniforms Needed

```glsl
// Existing (from Minecraft)
uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

// New for Shockwave
uniform sampler2D DepthSampler;     // Depth buffer texture
uniform vec3 ShockwaveCenter;       // World position of center
uniform float MaxRadius;            // Current expansion radius
uniform int RingCount;              // Number of rings
uniform float RingThickness;        // Width of each ring
uniform float Phase;                // 0-1 progress
uniform float Intensity;            // Glow strength
uniform vec3 BaseColor;             // Inner ring color
uniform vec3 EdgeColor;             // Outer ring color
```

### Depth Buffer Access Research

**Challenge:** Minecraft's standard entity pipelines don't expose depth buffer as a sampler.

**Research Findings:**

#### Approach 1: Post-Processing Shader System
- Minecraft has `PostEffectPass` and `PostEffectProcessor` classes
- Post shaders can access `minecraft:main:depth` target
- Configured via JSON in `assets/minecraft/shaders/post/`
- Uses `auxtarget` to pass depth buffer to shader

#### Approach 2: Custom RenderPipeline with Depth Sampler
- Create new `RenderPipeline` that declares depth sampler
- May need to manually bind framebuffer depth attachment
- Similar to existing `FresnelPipelines` but with additional sampler

#### Approach 3: Deferred Rendering Injection
- Inject after main scene renders but before post-processing
- Access depth via `MinecraftClient.getInstance().getFramebuffer()`

**Recommended Approach:** 
Approach 1 (Post-Processing) seems most aligned with Minecraft's architecture.
The shockwave would be rendered as a post-effect that samples both color and depth.

**Next Steps:**
1. Create a test post-processing shader that samples depth
2. Verify we can pass custom uniforms (center position, radius, etc.)
3. Implement ring pattern logic in fragment shader

---

## Open Questions

- [ ] Can we mix post-processing shaders with real-time uniform updates?
- [ ] How to trigger post shader only when shockwave is active?
- [ ] Performance impact of full-screen post-processing for shockwave?
- [ ] How to get world-space position in post shader (needs inverse matrices)?

---

*Document Version: 4.0*
*Last Updated: 2025-12-30*
