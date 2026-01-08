# Shader Library Extraction - Summary

> **Date**: 2026-01-06
> **Status**: Phase 1 Complete - V7 Refactored

---

## New Libraries Created

### core/ (Foundation Layer)

| File | Lines | Purpose | Origin |
|------|-------|---------|--------|
| **effect_result.glsl** | 70 | Unified return struct for all effects | New design |
| **noise_4d.glsl** | 175 | 4D animated noise functions | Extracted from pulsar_v7.glsl |
| **occlusion_utils.glsl** | 165 | Depth & visibility calculations | Extracted from pulsar_v7.glsl |
| **field_visual_preamble.glsl** | 95 | Shared main() setup macros | New design |

### sdf/ (Geometry Layer)

| File | Lines | Purpose | Origin |
|------|-------|---------|--------|
| **ray_sphere.glsl** | 205 | Ray-sphere intersection | Consolidated from V1/V6/V7 |

### rendering/ (Techniques Layer)

| File | Lines | Purpose | Origin |
|------|-------|---------|--------|
| **corona_effects.glsl** | 235 | Corona, halos, animated rays | Extracted from pulsar_v7.glsl |

---

## Effect Library Refactoring

### Shared EffectResult Struct

V3, V6, and V7 now use the unified `EffectResult` struct:
```glsl
#include "../core/effect_result.glsl"
#define PulsarV7Result EffectResult  // Backwards compatibility
```

| Effect | Before | After | Change |
|--------|--------|-------|--------|
| pulsar_v7.glsl | 548 | 450 | -98 lines (-18%) |
| pulsar_v6.glsl | 334 | 328 | -6 lines |
| energy_orb_v3.glsl | 570 | 569 | -1 line |

**Note**: `GeodesicV1Result` has an extra `glow` field and cannot be migrated.

### Shared noise4d Library

V7 now uses `#include "../core/noise_4d.glsl"`:
```glsl
#define v7_noise4q noise4q
#define v7_noiseSpere noiseSpherical
```

---

## Standalone Shader Refactoring

Using the new `field_visual_preamble.glsl`:

| Shader | Before | After | Reduction |
|--------|--------|-------|-----------|
| V1 | 78 | 28 | -64% |
| V2 | 145 | 76 | -48% |
| V3 | 177 | 42 | -76% |
| V5 | 122 | 60 | -51% |
| V6 | 119 | 55 | -54% |
| V7 | 150 | 48 | -68% |
| Geodesic | 148 | 34 | -77% |
| **Total** | **939** | **343** | **-63%** |

---

## Usage Examples

### EffectResult (Unified Returns)

```glsl
#include "include/core/effect_result.glsl"

EffectResult renderMyEffect(...) {
    if (noHit) return effectResult_empty();
    if (hasGlow) return effectResult_glow(color, alpha);
    return effectResult_full(color, alpha, distance);
}
```

### noise4q (4D Animated Noise)

```glsl
#include "include/core/noise_4d.glsl"

// Simple animated surface
float n = noise4q(vec4(surfacePoint * 10.0, time));

// Multi-octave spherical texture
float surface = noiseSpherical(point, perpDistSq, sqRadius, 
                               zoom, seedVec, time, 
                               speedHi, speedLow, detail);
```

### Occlusion Utilities

```glsl
#include "include/core/occlusion_utils.glsl"

OcclusionResult occ = calculateOcclusion(
    effectDist, rayDir, forward, sceneDepth,
    500.0,  // bleedRange
    0.7     // maxBleed
);

if (occ.isFullyOccluded) return effectResult_empty();
col.rgb *= occ.visibility;
```

### Corona Effects

```glsl
#include "include/core/noise_4d.glsl"
#include "include/rendering/corona_effects.glsl"

// Subtractive dark ring
col.rgb -= vec3(subtractiveRing(ray, toCenter, radius * 1.03, 11.0)) * 2.0;

// Animated ray corona
float rays = animatedRayCorona(pr, viewRot, radius, zoom, ...);
col.rgb += rayColorBlend(rayBase, rayLight, rays) * rays;
```

---

## Future Work (Phase 2)

### Potential Extractions

1. **Multi-layer color composition** (V7 pattern: `mix(a, b, pow(noise, exp)) * noise`)
2. **Fresnel/rim lighting** (common across V1, V2, V3)
3. **Spiral pattern generator** (shared by V2, V3)
4. **Animated rotation utilities** (used by Geodesic, V6, V7)

### Effect-Specific Refactoring

After libraries are stable:
1. ~~Refactor pulsar_v7.glsl to use new libraries~~ ✅ Done
2. Refactor energy_orb_v3.glsl to use new libraries
3. Unify result structs across all effects → EffectResult

---

## Important: Two Distinct Noise Systems

**DO NOT MERGE** - these serve different visual purposes:

| System | File | Origin | Use Case |
|--------|------|--------|----------|
| **noise4q** | `noise_4d.glsl` | V7 (Panteleymonov) | 4D value noise, multi-scale star surfaces |
| **pulsarSnoise** | `energy_orb_pulsar.glsl` | V5/V6 (trisomie21) | 3D simplex-like noise, flame patterns |

They produce **visually different** results and are both needed.

---

## Architecture Principle

> **Quality over Quantity**: Each library should be:
> - Self-contained with clear documentation
> - Tested in isolation before integration
> - Designed for reuse, not just extraction

