# Shader Library Restructure: Supplement - field_visual.fsh Extraction

## Overview

This document supplements the main restructure plan by detailing the extraction strategy for `field_visual.fsh` (854 lines), which contains a mix of:
- V1 (raymarching) specific code
- V2 setup/glue code  
- General utilities that should be in the library
- Inline implementations that duplicate library code

---

## Current Structure Analysis

### File: `field_visual.fsh` (854 lines)

| Lines | Section | Type | Action |
|-------|---------|------|--------|
| 1-28 | Header, samplers, texCoord | Setup | Keep in main shader |
| 29-119 | Uniform buffer (FieldVisualConfig) | Config | Extract to `uniforms/field_visual_uniforms.glsl` |
| 120-150 | FieldData struct + constants | Types | Extract to `effects/energy_orb_types.glsl` |
| 151-165 | Camera mode settings | Config | Extract to `camera/modes.glsl` |
| 166-175 | #include statements | Imports | Update paths |
| 176-224 | `reconstructWorldPosMultiMode()` | Camera | Already handled by new camera/depth.glsl |
| 225-259 | Voronoi noise, rotate2D, twirlUV | Utils | Extract to `core/noise_utils.glsl` |
| 260-289 | SDF primitives (duplicate) | SDF | DELETE - use `sdf/primitives.glsl` |
| 290-307 | `renderEnergyOrbCore()` | V1 Effect | Extract to `effects/energy_orb_v1.glsl` |
| 308-325 | `renderEnergyOrbEdge()` | V1 Effect | Extract to `effects/energy_orb_v1.glsl` |
| 326-373 | `renderEnergyOrbSpirals()` | V1 Effect | Extract to `effects/energy_orb_v1.glsl` |
| 379-445 | `renderEnergyOrbGlowLines()` | V1 Effect | Extract to `effects/energy_orb_v1.glsl` |
| 446-463 | `sphereNormal()` (duplicate) | SDF | DELETE - use `sdf/primitives.glsl` |
| 464-500 | `raymarchEnergyOrb()` | V1 Core | Extract to `effects/energy_orb_v1.glsl` |
| 504-623 | `renderEnergyOrbRaymarched()` | V1 Main | Extract to `effects/energy_orb_v1.glsl` |
| 624-665 | `renderEnergyOrb()` (legacy surface) | Legacy | Consider deprecation |
| 666-853 | `main()` | Entry Point | Refactor to use library calls |

---

## Extraction Categories

### Category A: Delete (Duplicates)
Code that duplicates library functionality.

```
Lines 260-289: sdfSphere, voronoi (partial duplicate)
Lines 446-463: sphereNormal (duplicate of sdf/primitives)
```

**Action:** Delete after verifying library equivalents work.

---

### Category B: Extract to Core
Utility functions that are generally useful.

#### → `core/noise_utils.glsl` (NEW)

```glsl
// core/noise_utils.glsl
#ifndef CORE_NOISE_UTILS_GLSL
#define CORE_NOISE_UTILS_GLSL

// 2D rotation matrix
mat2 rotate2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, s, -s, c);
}

// Twirl UV coordinates around a center
vec2 twirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    float dist = length(delta);
    float angle = strength * dist;
    delta = rotate2D(angle) * delta;
    return delta + center;
}

// Simple Voronoi distance field
float voronoi2D(vec2 uv, float scale) {
    vec2 grid = floor(uv * scale);
    float minDist = 1e10;
    
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec2 cell = grid + vec2(float(i), float(j));
            vec3 r = fract(sin(vec3(
                dot(cell, vec2(127.1, 311.7)),
                dot(cell, vec2(269.5, 183.3)),
                dot(cell, vec2(113.5, 271.9))
            )) * 43758.5453);
            vec2 p = (cell + r.xy) / scale;
            float d = distance(uv / scale, p);
            minDist = min(minDist, d);
        }
    }
    return minDist;
}

#endif
```

**Source lines:** 225-259

---

### Category C: Extract to Effects/V1

All V1-specific rendering functions.

#### → `effects/energy_orb_v1.glsl` (NEW)

```glsl
// effects/energy_orb_v1.glsl
#ifndef EFFECTS_ENERGY_ORB_V1_GLSL
#define EFFECTS_ENERGY_ORB_V1_GLSL

#include "../camera/types.glsl"
#include "../camera/rays.glsl"
#include "../sdf/primitives.glsl"
#include "../rendering/raymarch.glsl"
#include "../core/noise_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// TYPES
// ═══════════════════════════════════════════════════════════════════════════

struct EnergyOrbV1Config {
    vec3 center;
    float radius;
    vec4 primaryColor;
    vec4 secondaryColor;
    vec4 tertiaryColor;
    float intensity;
    float coreSize;
    float edgeSharpness;
    float spiralDensity;
    float spiralTwist;
    float glowLineCount;
    float glowLineIntensity;
    float coronaWidth;
    float time;
};

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENT EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

// Bright glowing center
vec4 energyOrbCore(EnergyOrbV1Config cfg, float distToCenter) {
    float coreRadius = cfg.radius * cfg.coreSize;
    float coreFactor = 1.0 - smoothstep(0.0, coreRadius, distToCenter);
    coreFactor = pow(coreFactor, 2.2);
    vec3 coreColor = cfg.primaryColor.rgb * coreFactor;
    float coreAlpha = coreFactor * cfg.intensity;
    return vec4(coreColor, coreAlpha);
}

// Edge glow (Fresnel-like)
vec4 energyOrbEdge(EnergyOrbV1Config cfg, float distToCenter, float rim) {
    float edgeFactor = pow(rim, cfg.edgeSharpness);
    vec3 edgeColor = mix(cfg.secondaryColor.rgb, cfg.primaryColor.rgb, edgeFactor * 0.5);
    float edgeAlpha = edgeFactor * cfg.intensity * 0.7;
    return vec4(edgeColor, edgeAlpha);
}

// Animated spiral patterns
vec4 energyOrbSpirals(EnergyOrbV1Config cfg, vec3 localPos, float distToCenter) {
    if (distToCenter > cfg.radius) return vec4(0.0);
    
    // Calculate spherical coordinates
    float theta = atan(localPos.z, localPos.x);
    float phi = asin(clamp(localPos.y / max(0.001, distToCenter), -1.0, 1.0));
    
    // Spiral pattern with twist
    float spiralAngle = theta * cfg.spiralDensity + phi * cfg.spiralTwist;
    float spiral1 = sin(spiralAngle + cfg.time * 2.0) * 0.5 + 0.5;
    float spiral2 = sin(spiralAngle * 2.0 - cfg.time * 3.0) * 0.5 + 0.5;
    float spiral = spiral1 + spiral2 * 0.5;
    
    // Fade near center and edge
    float innerFade = smoothstep(0.0, 0.15 * cfg.radius, distToCenter);
    float outerFade = smoothstep(cfg.radius, cfg.radius * 0.7, distToCenter);
    spiral *= innerFade * outerFade;
    
    vec3 spiralColor = mix(cfg.secondaryColor.rgb, cfg.tertiaryColor.rgb, spiral);
    float spiralAlpha = spiral * cfg.intensity * 0.6;
    
    return vec4(spiralColor, spiralAlpha);
}

// Radial glow lines  
vec4 energyOrbGlowLines(EnergyOrbV1Config cfg, vec3 localPos, float distToCenter) {
    if (distToCenter > cfg.radius * 1.5) return vec4(0.0);
    
    float theta = atan(localPos.z, localPos.x);
    int count = int(cfg.glowLineCount);
    vec3 totalGlow = vec3(0.0);
    
    for (int i = 0; i < count && i < 24; i++) {
        float lineAngle = (float(i) / float(count)) * TAU;
        float angleDiff = abs(mod(theta - lineAngle + PI, TAU) - PI);
        float lineGlow = exp(-angleDiff * 8.0);
        
        // Animate intensity
        float pulse = sin(cfg.time * 3.0 + float(i) * 0.5) * 0.5 + 0.5;
        totalGlow += lineGlow * pulse * cfg.glowLineIntensity;
    }
    
    vec3 lineColor = mix(cfg.secondaryColor.rgb, cfg.primaryColor.rgb, 0.3);
    totalGlow *= lineColor;
    
    float glowAlpha = min(1.0, (totalGlow.r + totalGlow.g + totalGlow.b) / 3.0);
    return vec4(totalGlow, glowAlpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCHING
// ═══════════════════════════════════════════════════════════════════════════

// Raymarch result for energy orb
struct EnergyOrbHit {
    bool hit;
    float distance;
    vec3 position;
    float rim;        // Fresnel rim factor
    float glow;       // Corona glow (for near misses)
};

EnergyOrbHit raymarchEnergyOrb(Ray ray, EnergyOrbV1Config cfg, float maxDist) {
    EnergyOrbHit result;
    result.hit = false;
    result.glow = 0.0;
    
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = ray.origin + ray.direction * t;
        float d = sdfSphere(p, cfg.center, cfg.radius);
        
        if (d < RAYMARCH_EPSILON) {
            result.hit = true;
            result.distance = t;
            result.position = p;
            
            // Calculate rim/fresnel
            vec3 normal = normalize(p - cfg.center);
            result.rim = 1.0 - abs(dot(normal, -ray.direction));
            
            return result;
        }
        
        if (t > maxDist) break;
        t += d * 0.8;
    }
    
    // Check for corona glow (near miss)
    vec3 nearPoint = ray.origin + ray.direction * min(t, maxDist * 0.5);
    float nearDist = sdfSphere(nearPoint, cfg.center, cfg.radius);
    float glowWidth = cfg.coronaWidth * cfg.radius;
    
    if (nearDist < glowWidth && nearDist > 0.0) {
        result.glow = 1.0 - (nearDist / glowWidth);
    }
    
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION  
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbV1(
    Ray ray,
    vec3 forward,
    EnergyOrbV1Config cfg,
    float sceneDepth
) {
    float distToTarget = length(cfg.center - ray.origin);
    float maxDist = distToTarget + cfg.radius * 3.0;
    
    EnergyOrbHit hit = raymarchEnergyOrb(ray, cfg, maxDist);
    
    if (hit.hit) {
        // Depth occlusion check
        float hitZDepth = hit.distance * dot(ray.direction, forward);
        if (hitZDepth > sceneDepth) {
            return vec4(0.0);  // Occluded
        }
        
        // Calculate local position for effects
        vec3 localPos = hit.position - cfg.center;
        float distToCenter = length(localPos);
        
        // Combine all effect layers
        vec4 core = energyOrbCore(cfg, distToCenter);
        vec4 edge = energyOrbEdge(cfg, distToCenter, hit.rim);
        vec4 spirals = energyOrbSpirals(cfg, localPos, distToCenter);
        vec4 lines = energyOrbGlowLines(cfg, localPos, distToCenter);
        
        // Composite
        vec3 color = core.rgb;
        float alpha = core.a;
        
        color = mix(color, edge.rgb, edge.a * 0.8);
        alpha = max(alpha, edge.a);
        
        color = mix(color, spirals.rgb, spirals.a * 0.6);
        alpha = max(alpha, spirals.a);
        
        color += lines.rgb * lines.a;
        alpha = max(alpha, lines.a);
        
        return vec4(color, alpha);
        
    } else if (hit.glow > 0.01) {
        // Corona glow for near misses
        vec3 glowColor = cfg.primaryColor.rgb * hit.glow * 0.5;
        return vec4(glowColor, hit.glow * 0.3);
    }
    
    return vec4(0.0);
}

#endif
```

**Source lines:** 290-623 (condensed and refactored)

---

### Category D: Extract to Uniforms

Keep uniform definitions separate for reuse.

#### → `uniforms/field_visual.glsl` (NEW)

```glsl
// uniforms/field_visual.glsl
#ifndef UNIFORMS_FIELD_VISUAL_GLSL
#define UNIFORMS_FIELD_VISUAL_GLSL

// Standard sampler info (provided by Minecraft)
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Field visual configuration
// Layout: 21 vec4s for basic config + mat4 for matrices
layout(std140) uniform FieldVisualConfig {
    // ... (copy current uniform block)
};

#endif
```

**Source lines:** 29-119

---

### Category E: Refactor Main

The `main()` function should become a thin orchestrator.

#### After Extraction: `field_visual.fsh` (~150 lines)

```glsl
#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL - Energy Orb Post-Effect Fragment Shader
// Orchestrates rendering of energy field effects using the shader library.
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

// Uniforms
#include "include/uniforms/field_visual.glsl"

// Library imports
#include "include/core/constants.glsl"
#include "include/camera/types.glsl"
#include "include/camera/basis.glsl"
#include "include/camera/rays.glsl"
#include "include/camera/depth.glsl"

// Effect implementations
#include "include/effects/energy_orb_v1.glsl"
#include "include/effects/energy_orb_v2.glsl"

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    bool isSky = (rawDepth > 0.9999);
    
    // Build camera data from uniforms
    CameraData cam = buildCameraData(
        vec3(CameraX, CameraY, CameraZ),
        vec3(ForwardX, ForwardY, ForwardZ),
        Fov, AspectRatio, NearPlane, FarPlane
    );
    
    // Get ray direction (adaptive based on flying state)
    Ray ray = getRayAdaptive(texCoord, cam, InvViewProj, IsFlying);
    
    // Linearize depth
    float linearDepth = linearizeDepth(rawDepth, cam.near, cam.far);
    float sceneDepth = isSky ? 10000.0 : linearDepth;
    
    // Determine effect type and version
    int effectType = int(EffectType);
    float version = Version;
    
    vec4 fieldEffect = vec4(0.0);
    
    if (effectType == EFFECT_ENERGY_ORB) {
        if (version >= 2.0) {
            // V2: Screen-space projection
            fieldEffect = renderEnergyOrbV2(
                texCoord, cam,
                vec3(CenterX, CenterY, CenterZ), Radius,
                // ... pass relevant uniforms
                sceneDepth
            );
        } else {
            // V1: Raymarched
            EnergyOrbV1Config cfg;
            cfg.center = vec3(CenterX, CenterY, CenterZ);
            cfg.radius = Radius;
            cfg.primaryColor = vec4(PrimaryR, PrimaryG, PrimaryB, PrimaryA);
            // ... populate config from uniforms
            
            fieldEffect = renderEnergyOrbV1(ray, cam.forward, cfg, sceneDepth);
        }
    }
    
    // Final compositing
    if (fieldEffect.a < 0.001) {
        fragColor = sceneColor;
        return;
    }
    
    vec3 finalColor = mix(sceneColor.rgb, fieldEffect.rgb, fieldEffect.a);
    finalColor = 1.0 - exp(-finalColor * 1.5);  // Tone mapping
    
    fragColor = vec4(finalColor, 1.0);
}
```

---

## Updated File Structure

After extraction, the full structure becomes:

```
include/
├── core/
│   ├── constants.glsl       # Shared constants
│   ├── math_utils.glsl      # Math utilities
│   ├── color_utils.glsl     # Color utilities
│   └── noise_utils.glsl     # Voronoi, rotate2D, twirlUV (NEW)
│
├── camera/
│   ├── types.glsl           # CameraData, Ray, SphereProjection
│   ├── basis.glsl           # computeCameraBasis
│   ├── rays.glsl            # getRayFromBasis, getRayFromMatrix, getRayAdaptive
│   ├── projection.glsl      # worldToNDC, projectSphere
│   ├── depth.glsl           # linearizeDepth, reconstructWorldPos
│   └── modes.glsl           # Camera mode defines (NEW)
│
├── uniforms/                # UNIFORM DEFINITIONS (NEW)
│   ├── field_visual.glsl    # FieldVisualConfig UBO
│   └── shockwave.glsl       # ShockwaveConfig UBO
│
├── sdf/
│   ├── primitives.glsl      # Sphere, capsule, box, etc.
│   ├── operations.glsl      # Union, intersection, smooth blend
│   └── orbital_system.glsl  # Orbital-specific SDFs
│
├── rendering/
│   ├── raymarch.glsl        # Generic raymarch loop
│   ├── screen_effects.glsl  # 2D effect utilities
│   └── depth_mask.glsl      # Depth occlusion
│
└── effects/
    ├── energy_orb_types.glsl  # FieldData struct (NEW)
    ├── energy_orb_v1.glsl     # Raymarched orb (NEW - extracted)
    ├── energy_orb_v2.glsl     # Projected orb (existing, updated)
    ├── orbitals_v1.glsl       # Raymarched orbitals
    ├── orbitals_v2.glsl       # Projected orbitals (NEW)
    ├── beams_v1.glsl          # Raymarched beams
    ├── beams_v2.glsl          # Projected beams (NEW)
    └── shockwave_ring.glsl    # Depth-based rings
```

---

## Line Count Comparison

### Before Extraction

| File | Lines |
|------|-------|
| field_visual.fsh | 854 |
| camera_core.glsl | 371 |
| depth_utils.glsl | 50 |
| energy_orb_v2.glsl | 437 |
| projection_modes.glsl | 281 |
| raymarching.glsl | 127 |
| sdf_library.glsl | 155 |
| orbital_math.glsl | 276 |
| glow_utils.glsl | 42 |
| **TOTAL** | **2,593** |

### After Extraction (Estimated)

| File | Lines |
|------|-------|
| **field_visual.fsh** | **~150** (↓704) |
| core/constants.glsl | ~30 |
| core/math_utils.glsl | ~40 |
| core/color_utils.glsl | ~30 |
| core/noise_utils.glsl | ~50 |
| camera/types.glsl | ~50 |
| camera/basis.glsl | ~40 |
| camera/rays.glsl | ~70 |
| camera/projection.glsl | ~60 |
| camera/depth.glsl | ~40 |
| camera/modes.glsl | ~20 |
| uniforms/field_visual.glsl | ~100 |
| sdf/primitives.glsl | ~80 |
| sdf/operations.glsl | ~30 |
| sdf/orbital_system.glsl | ~100 |
| rendering/raymarch.glsl | ~60 |
| rendering/screen_effects.glsl | ~50 |
| rendering/depth_mask.glsl | ~30 |
| effects/energy_orb_types.glsl | ~30 |
| effects/energy_orb_v1.glsl | ~200 |
| effects/energy_orb_v2.glsl | ~300 |
| effects/orbitals_v1.glsl | ~100 |
| effects/orbitals_v2.glsl | ~100 |
| effects/beams_v1.glsl | ~80 |
| effects/beams_v2.glsl | ~80 |
| effects/shockwave_ring.glsl | ~100 |
| **TOTAL** | **~2,020** |

**Net reduction:** ~573 lines (~22% smaller)
**Largest file:** ~300 lines (energy_orb_v2.glsl)
**Main shader:** reduced from 854 → 150 lines (~82% reduction)

---

## Extraction Order (Recommended)

### Wave 1: Foundation (No Breaking Changes)
1. Create `core/` directory with constants, math, color
2. Create `camera/types.glsl` with structs
3. Test: Ensure existing shaders still work

### Wave 2: Camera Extraction
4. Create `camera/basis.glsl`, `camera/rays.glsl`
5. Create `camera/depth.glsl`, `camera/projection.glsl`
6. Update camera_core.glsl to re-export from new files (backward compat)
7. Test: Ensure existing shaders still work

### Wave 3: SDF Consolidation
8. Create `sdf/` directory structure
9. Migrate primitives from sdf_library.glsl
10. Update includes in existing shaders
11. Test: All effects still render correctly

### Wave 4: Effects Extraction
12. Extract `effects/energy_orb_v1.glsl` from field_visual.fsh
13. Refactor field_visual.fsh to use extracted code
14. Test: V1 orb renders identically

### Wave 5: V2 Implementations
15. Create `effects/orbitals_v2.glsl`
16. Create `effects/beams_v2.glsl`
17. Test: New V2 effects work alongside V1

### Wave 6: Cleanup
18. Delete deprecated/duplicate code
19. Update all documentation headers
20. Final testing pass

---

## Risk Mitigation

### Include Path Changes
- Use backward-compatible wrapper files initially
- Old imports work via re-exports from new locations

### Breaking Changes
- Each wave tested before proceeding
- Git tags at each wave completion

### Performance
- Measure FPS before/after each wave
- No additional #include overhead (preprocessor resolves at compile time)
