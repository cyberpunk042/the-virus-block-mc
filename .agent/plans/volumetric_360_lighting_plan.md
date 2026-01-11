# 360° Volumetric Lighting Plan 🌐💡

## Problem Statement

Current screen-space god rays only render when the light source is visible on screen. When the orb is behind the camera, no lighting effect is visible - even though realistically, a bright light source would affect the environment around the player.

**Goal**: Support volumetric lighting effects that work regardless of camera orientation relative to the light source.

---

## Current Limitation

```
Camera facing TOWARD orb:  ✅ Rays visible
Camera facing AWAY from orb: ❌ No rays (correct for god rays, wrong for overall lighting)
```

This is architecturally correct for *god rays* (scattering between eye and light), but leaves a gap in the visual experience.

---

## Research Required

### 1. Comparable Systems
- [ ] Study Minecraft shader packs (SEUS, BSL, Complementary)
- [ ] Study Unreal Engine volumetric fog
- [ ] Study Unity HDRP volumetric lighting

### 2. Techniques to Evaluate

| Technique | Description | Performance | Quality |
|-----------|-------------|-------------|---------|
| **Shadow Volume Fog** | Raymarch through fog, check shadow at each step | Heavy | High |
| **Voxel-Based GI** | 3D voxel grid stores light transport | Very Heavy | Very High |
| **Screen-Space Radiance** | Sample light contribution in screen space | Medium | Medium |
| **Deferred Light Volumes** | Render light influence as geometry | Medium | Medium |
| **Approximated Scattering** | Fake atmospheric effect based on light distance | Light | Low-Medium |

---

## Proposed Architecture

### Layer 1: Screen-Space God Rays (CURRENT)
- **When**: Light source visible on screen
- **Effect**: Radial light shafts
- **Status**: ✅ Implemented

### Layer 2: Peripheral Glow (NEW - PRIORITY)
- **When**: Light source just off-screen (within 180°)
- **Effect**: Edge glow indicating light direction
- **Complexity**: Low
- **Approach**: Project light to nearest screen edge, render attenuated glow

### Layer 3: Ambient Light Influence (NEW)
- **When**: Always (light source anywhere)
- **Effect**: Color tint on scene based on light proximity
- **Complexity**: Medium
- **Approach**: Apply light color/intensity as scene tint, falloff with distance

### Layer 4: Volumetric Fog with Shadows (FUTURE)
- **When**: Fog enabled
- **Effect**: Light scatters through fog volume, casts volumetric shadows
- **Complexity**: High
- **Approach**: Deferred fog pass with shadow sampling

---

## Implementation Phases

### Phase A: Peripheral Glow 🔦
**Goal**: Visual feedback when light is just outside view

```glsl
// When orb is behind but close to screen edge
if (zDist <= 0.0) {
    vec2 behindDir = normalize(vec2(-xProj, -yProj));
    float edgeDist = length(vec2(xProj, yProj)) / zDist;
    
    if (edgeDist < 2.0) { // Within 90° of screen edge
        lightUV = vec2(0.5, 0.5) + behindDir * 0.8;
        lightVisible = 0.5 * (1.0 - edgeDist / 2.0); // Fade with angle
    }
}
```

**Deliverable**: Soft glow at screen edge when orb is just behind

### Phase B: Ambient Light Influence 🌈
**Goal**: Scene takes on light color regardless of view direction

**New Pass**: After composite
```glsl
// Sample light contribution based on world distance
float lightDist = length(vec3(CenterX, CenterY, CenterZ));
float influence = 1.0 / (1.0 + lightDist * 0.1);
vec3 lightTint = vec3(RayColorR, RayColorG, RayColorB) * influence;

fragColor.rgb = mix(fragColor.rgb, fragColor.rgb * (1.0 + lightTint), 0.3);
```

**Deliverable**: Subtle color wash from nearby orbs

### Phase C: Volumetric Fog 🌫️
**Goal**: Full volumetric scattering with shadow

**Requires**:
- Shadow map from orb (or SDF-based occlusion)
- Fog density uniform/texture
- Raymarch through scene depth with fog sampling

**Deliverable**: True volumetric light shafts visible from any angle

---

## UBO Additions Required

```
vec4 54: VolumetricParams
  .x = fogDensity         (0.0 = off, 0.01-0.1 = light fog)
  .y = scatteringStrength (how much light scatters in fog)
  .z = ambientInfluence   (how much orb color tints scene)
  .w = peripheralGlow     (enable edge glow when behind)

vec4 55: VolumetricColor
  .xyz = fog/scatter color (or auto from RayColor)
  .w = reserved
```

---

## Pipeline Changes

### Current Pipeline (7 passes)
```
Effect → Blit → Mask → Accum → BlurH → BlurV → Composite
```

### Extended Pipeline (9+ passes)
```
Effect → Blit → Mask → Accum → BlurH → BlurV → Composite → AmbientTint → FogVolume
```

---

## Priority Order

1. **Phase A (Peripheral Glow)** - Low effort, high UX value
2. **Phase B (Ambient Influence)** - Medium effort, cohesive feel
3. **Phase C (Volumetric Fog)** - High effort, AAA quality

---

## Dependencies

- [ ] Shadow mapping infrastructure (for Phase C)
- [ ] Fog density controls (UI + UBO)
- [ ] Performance profiling on target hardware

---

## Success Criteria

| Phase | Metric |
|-------|--------|
| A | Edge glow visible when orb 0-90° behind camera |
| B | Scene color shifts based on orb proximity/color |
| C | Visible light shafts cutting through fog from any angle |

---

## Estimated Timeline

| Phase | Effort | Calendar |
|-------|--------|----------|
| A | 2-4 hours | Day 1 |
| B | 4-8 hours | Day 1-2 |
| C | 2-4 days | Week 2+ |

---

## Notes

This is a **non-trivial** system that separates "indie" from "polished" visual quality. The phased approach allows incremental improvement while maintaining stability.

---
*Created: 2026-01-11*
*Status: PLANNING*
