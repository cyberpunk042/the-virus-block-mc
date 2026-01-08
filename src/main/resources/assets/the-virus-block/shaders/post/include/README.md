# Shader Library Architecture

## The Onion: Layer Dependencies

```
                    ┌─────────────────────────────────────────────────┐
                    │              MAIN SHADERS (USE)                 │
                    │  field_visual.fsh   shockwave_ring.fsh  etc.   │
                    └─────────────────────────────────────────────────┘
                                          │
                                          ▼
                    ┌─────────────────────────────────────────────────┐
                    │              effects/ (IMPLEMENTATIONS)         │
              ┌─────┤  energy_orb_v1  orbitals_v1  orbitals_v2       │
              │     │  shockwave_ring  energy_orb_types               │
              │     └─────────────────────────────────────────────────┘
              │                           │
              │                           ▼
              │     ┌─────────────────────────────────────────────────┐
              │     │              rendering/ (TECHNIQUES)            │
              ├─────┤  raymarch    screen_effects    depth_mask       │
              │     └─────────────────────────────────────────────────┘
              │                           │
              │                           ▼
              │     ┌────────────────────────────────────────┐
              │     │              sdf/ (GEOMETRY)            │
              ├─────┤  primitives    operations    orbital    │
              │     └────────────────────────────────────────┘
              │                           │
              │                           ▼
              │     ┌────────────────────────────────────────┐
              │     │              camera/ (SPACE)            │
              ├─────┤  types  basis  rays  projection  depth  │
              │     └────────────────────────────────────────┘
              │                           │
              │                           ▼
              │     ┌────────────────────────────────────────┐
              └────►│              core/ (FOUNDATION)         │
                    │  constants  math  color  noise          │
                    └────────────────────────────────────────┘
```

---

## Layer 1: core/ (Foundation)

**Purpose:** Basic building blocks with NO dependencies.

| File | Purpose | Key Exports |
|------|---------|-------------|
| constants.glsl | Numerical constants, shape/effect IDs | PI, TAU, EPSILON, SHAPE_*, EFFECT_* |
| math_utils.glsl | Safe math, remapping, rotation | safeNormalize, remap, smoothMin, rotate2D |
| color_utils.glsl | Color manipulation, tone mapping | premultiplyAlpha, toneMapAces, blendOver |
| noise_utils.glsl | Hash, noise, UV manipulation | hash22, voronoi, twirlUV, radialUV |
| **effect_result.glsl** | **Unified return struct** | **EffectResult, effectResult_empty/hit/full/glow** |
| **noise_4d.glsl** | **4D noise (V7 - smooth stars)** | **noise4q, noiseSpherical, noise4q_turbulent** |
| **noise_3d.glsl** | **3D noise (V5/V6 - flames)** | **snoise3d, surfaceTexture3d, flameNoise** |
| **occlusion_utils.glsl** | **Depth occlusion & visibility** | **calculateOcclusion, occlusionBleed, proximityBlackout** |
| **field_visual_preamble.glsl** | **Shared main() setup macros** | **FIELD_VISUAL_PREAMBLE, FIELD_VISUAL_COMPOSITE** |
| field_visual_base.glsl | UBO + samplers for field effects | FieldData, buildFieldDataFromUBO, compositeFieldEffect |

**When to include:** ALWAYS. These are prerequisites for everything.

```glsl
#include "include/core/constants.glsl"
#include "include/core/math_utils.glsl"
```

---

## Layer 2: camera/ (Space Transformations)

**Purpose:** Camera data structures and coordinate transformations.

| File | Purpose | Key Exports |
|------|---------|-------------|
| types.glsl | Data structures | CameraData, Ray, SphereProjection, DepthInfo |
| basis.glsl | Camera orientation | computeCameraBasis, buildCameraData |
| rays.glsl | Ray generation (V1 critical!) | getRayFromBasis, getRayFromMatrix, getRayAdaptive |
| projection.glsl | 3D to 2D (V2 critical!) | worldToScreen, projectSphere, getApparentRadius |
| depth.glsl | Depth buffer operations | linearizeDepth, isSky, reconstructWorldPos |

**Critical Distinction:**
- **rays.glsl** → For V1 (raymarching). Generates rays FROM screen pixels.
- **projection.glsl** → For V2 (screen-space). Projects 3D points TO screen.

**When to include:**
```glsl
// For raymarching (V1):
#include "include/camera/rays.glsl"

// For screen-space projection (V2):
#include "include/camera/projection.glsl"
```

---

## Layer 3: sdf/ (Geometry)

**Purpose:** Signed Distance Functions for shape definitions.

| File | Purpose | Key Exports |
|------|---------|-------------|
| primitives.glsl | Basic shapes | sdfSphere, sdfBox, sdfTorus, sdfCapsule, sdfPolygon |
| operations.glsl | Combining SDFs | sdfUnion, sdfSmoothUnion, round_merge, sdfOnion |
| orbital_system.glsl | Orbital patterns | getOrbitalPosition, sdfOrbitalSystem3D |
| **ray_sphere.glsl** | **Ray-sphere intersection** | **RaySphereHit, raySphereIntersect, sphereProjection** |

**When to include:**
```glsl
// For ANY SDF work:
#include "include/sdf/primitives.glsl"
#include "include/sdf/operations.glsl"

// For orbital systems (shockwave, field patterns):
#include "include/sdf/orbital_system.glsl"
```

---

## Layer 4: rendering/ (Techniques)

**Purpose:** Rendering algorithms and utilities.

| File | Purpose | Key Exports |
|------|---------|-------------|
| raymarch.glsl | V1 utilities | RaymarchResult, calcRim, calcCorona, hitDistToZDepth |
| screen_effects.glsl | V2 utilities | glowFalloff, ringEffect, radialLines, starburst |
| depth_mask.glsl | Occlusion | occlusionSoft, raymarchVisibility, projectedVisibility |
| **corona_effects.glsl** | **Star/sun coronas (V7 origin)** | **subtractiveRing, additiveGlowRing, animatedRayCorona** |

**When to include:**
```glsl
// For V1 raymarching:
#include "include/rendering/raymarch.glsl"
#include "include/rendering/depth_mask.glsl"

// For V2 screen-space:
#include "include/rendering/screen_effects.glsl"
#include "include/rendering/depth_mask.glsl"
```

---

## Layer 5: effects/ (Implementations)

**Purpose:** Complete effect implementations ready to use.

| File | Purpose | Mode |
|------|---------|------|
| energy_orb_types.glsl | Shared types for orb | Both V1/V2 |
| energy_orb_v1.glsl | Raymarched orb | V1 (Bottom-Up) |
| orbitals_v1.glsl | Raymarched orbitals + beams | V1 (Bottom-Up) |
| orbitals_v2.glsl | Screen-projected orbitals | V2 (Top-Down) |
| shockwave_ring.glsl | Depth-based rings | V3 (Depth Surface) |

**When to include:**
```glsl
// For energy orb rendering:
#include "include/effects/energy_orb_v1.glsl"  // V1
// or
#include "include/effects/energy_orb_v2.glsl"  // V2 (when created)

// For orbital effects:
#include "include/effects/orbitals_v1.glsl"  // V1
// or
#include "include/effects/orbitals_v2.glsl"  // V2

// For shockwave:
#include "include/effects/shockwave_ring.glsl"
```

---

## The Three Rendering Paradigms

### V1: Raymarching (Bottom-Up)
- **Direction:** Screen pixels → 3D world
- **Method:** Cast ray, march until SDF < epsilon
- **Critical file:** `camera/rays.glsl`
- **Use for:** Solid 3D objects, accurate occlusion
- **Walking/Flying:** Use `getRayAdaptive()` to avoid camera bob issues

### V2: Screen-Space Projection (Top-Down)
- **Direction:** 3D world → Screen pixels
- **Method:** Project 3D point, draw 2D effect around it
- **Critical file:** `camera/projection.glsl`
- **Use for:** Glows, lens flares, distant effects
- **FOV Critical:** Must use `GameRenderer.getFov()` for accuracy

### V3: Depth-Based Surface (Terrain Following)
- **Direction:** Read depth → Reconstruct world pos → Apply effect
- **Method:** Sample depth buffer, compute world position, test against SDF
- **Critical file:** `camera/depth.glsl`
- **Use for:** Ground shockwaves, terrain-conforming effects

---

## Integration Checklist

Before using the library:

1. [ ] Include `core/constants.glsl` first (always)
2. [ ] Include camera files matching your paradigm (V1 vs V2)
3. [ ] Include sdf files if using distance functions
4. [ ] Include rendering files matching your approach
5. [ ] Include effect files for complete implementations
6. [ ] Build your CameraData from uniforms
7. [ ] Call the appropriate render function
8. [ ] Apply occlusion check with depth_mask

---

## ✅ STATUS: COMPLETE & MIGRATED (2026-01-03)

**All layers faithfully extracted and verified. Main shaders now use the new library.**

- `core/` - Verified ✓
- `camera/` - Verified ✓ (includes Java-side requirements docs)
- `sdf/` - Verified ✓
- `rendering/` - Verified ✓
- `effects/` - Verified ✓

### Main Shader Line Counts After Migration

| Shader | Before | After | Target |
|--------|--------|-------|--------|
| field_visual.fsh | ~897 | ~318 | ~300 ✓ |
| shockwave_ring.fsh | ~470 | ~406 | ~300 |

---

## ⚠️ CRITICAL: Java-Side Requirements

**See: `include/camera/rays.glsl` header for full documentation.**

### 1. FOV MUST be DYNAMIC
```java
// CORRECT - includes flying/sprinting changes:
float dynamicFov = ((GameRendererAccessor) client.gameRenderer)
    .invokeGetFov(camera, tickDelta, true);
float fov = (float) Math.toRadians(dynamicFov);

// WRONG - static settings value, causes flying glitches:
float fov = (float) Math.toRadians(client.options.getFov().getValue());
```

### 2. Camera Data Capture Timing
- Use WorldRenderer mixin injection point
- `camera.getYaw()` / `camera.getPitch()` → forward vector
- `camX`, `camY`, `camZ` from render method locals

### 3. InvViewProj Matrix
```java
Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
Matrix4f invViewProj = new Matrix4f();
viewProj.invert(invViewProj);
```

---

## GLSL Patterns

### Boolean in vec4 (hitInfo.w > 0.5)
Since `vec4` can't hold booleans, we encode:
- `1.0` = true (hit something)
- `0.0` = false (miss)

Check with `> 0.5` for floating-point safety:
```glsl
if (hitInfo.w > 0.5) {
    // We hit something
}
```

---

## Legacy Files (Archived or Bridge)

The following files in `include/` root are either:
- **Bridge files** - Still used but call into new library
- **Archived** - Moved to `shaders/post/_archive/`

| File | Status | Notes |
|------|--------|-------|
| `energy_orb_v2.glsl` | Active | Uses new library internally |
| `glow_utils.glsl` | Active | Shared glow utilities |
| `orbital_math.glsl` | Active | Flower pattern SDFs |
| `camera_core.glsl` | Legacy | Consider migration |
| `depth_utils.glsl` | Legacy | Superseded by camera/depth.glsl |
| `raymarching.glsl` | Legacy | Superseded by effects/orbitals_v1.glsl |

