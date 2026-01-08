# rendering/ - Rendering Technique Layer

**Dependencies:** core/, camera/  
**Used by:** effects/, main shaders

---

## Purpose

Rendering utilities for the three paradigms:
- V1: Raymarching helpers
- V2: Screen-space effect helpers
- Both: Depth occlusion

## Files

### raymarch.glsl - V1 Utilities
```glsl
#include "include/rendering/raymarch.glsl"
```

**Result Types:**
```glsl
struct RaymarchResult {
    bool hit;
    float distance;
    vec3 position;
    vec3 normal;
    float rim;
    int hitType;
    float corona;
};

RaymarchResult noHit();  // Initialize to no-hit state
```

**Lighting:**
- `calcRim(normal, viewDir, power)` - Fresnel-like rim glow
- `calcDiffuse(normal, lightDir)` - Simple diffuse
- `calcSpecular(normal, viewDir, lightDir, shininess)` - Blinn-Phong

**Corona/Glow:**
- `calcCorona(nearestDist, coronaWidth)` - Near-miss glow

**Depth Conversion:**
- `hitDistToZDepth(hitDist, rayDir, forward)` - Ray distance to Z-depth
- `isHitOccluded(hitDist, rayDir, forward, sceneDepth)` - Occlusion check

**Step Size:**
- `safeStep(sdfDist, factor)` - Conservative step
- `adaptiveStep(sdfDist, traveled, maxAdaptive)` - Distance-based adaptive

### screen_effects.glsl - V2 Utilities
```glsl
#include "include/rendering/screen_effects.glsl"
```

**Distance Calculations:**
- `distToPoint(texCoord, center, aspect)` - Aspect-corrected distance
- `normalizedDistToSphere(texCoord, proj, aspect)` - 0=center, 1=edge

**Glow & Falloff:**
- `glowFalloff(dist, falloff)` - Inverse distance glow
- `glowExp(dist, falloff)` - Exponential falloff
- `softEdge(dist, inner, outer)` - Smoothstep fade
- `hardEdge(dist, radius, edgeWidth)` - Sharp edge with smooth transition

**Ring Effects:**
- `ringEffect(dist, ringRadius, thickness)` - Basic ring
- `glowingRing(dist, ringRadius, ringWidth, glowWidth)` - Ring with glow

**Radial Patterns:**
- `radialLines(texCoord, center, lineCount, width, rotation)` - Star pattern
- `starburst(texCoord, center, lineCount, width, falloff, rotation, aspect)`

**Animation:**
- `pulsingGlow(baseIntensity, pulseAmount, time, speed)` - Oscillating
- `rotatingUV(texCoord, center, time, speed)` - Spinning pattern

**Compositing:**
- `addGlow(base, glowColor, intensity)` - Additive blend
- `layerGlows(base, color1, int1, color2, int2, color3, int3)` - Multi-layer

### depth_mask.glsl - Occlusion for Both Paradigms
```glsl
#include "include/rendering/depth_mask.glsl"
```

**Basic Occlusion:**
- `isOccludedHard(effectDepth, sceneDepth, bias)` - Sharp cutoff
- `occlusionSoft(effectDepth, sceneDepth, fadeWidth)` - Smooth fade

**V1 Raymarched:**
- `rayDistToLinearDepth(hitDist, rayDir, forward)` - Convert distance
- `raymarchVisibility(hitDist, rayDir, forward, sceneDepth)` - 0/1
- `raymarchVisibilitySoft(...)` - With fade

**V2 Projected:**
- `projectedVisibility(proj, sceneDepth, fadeWidth)` - Using sphere projection
- `projectedVisibilityPerPixel(...)` - Per-pixel depth approximation

**Sky Handling:**
- `getEffectiveSceneDepth(linearDepth, isSky, skyDepth)` - Treat sky as far
- `getEffectiveSceneDepth(DepthInfo, skyDepth)` - Using struct

**Corona Visibility:**
- `coronaVisibility(objectDepth, sceneDepth, coronaExtend)` - Corona peeks in front

---

## Usage Patterns

### V1 Raymarch with Depth Check
```glsl
#include "include/rendering/raymarch.glsl"
#include "include/rendering/depth_mask.glsl"

// After raymarching...
if (hit.hit) {
    float visibility = raymarchVisibilitySoft(
        hit.distance, ray.direction, cam.forward,
        sceneDepth, 0.5  // fadeWidth
    );
    if (visibility > 0.01) {
        vec4 color = ...;
        return color * visibility;
    }
}
```

### V2 Screen Effect
```glsl
#include "include/rendering/screen_effects.glsl"
#include "include/rendering/depth_mask.glsl"

SphereProjection proj = projectSphere(center, radius, cam);
if (proj.isVisible) {
    float visibility = projectedVisibility(proj, sceneDepth, 1.0);
    if (visibility > 0.01) {
        float dist = normalizedDistToSphere(texCoord, proj, cam.aspect);
        float glow = glowFalloff(dist, 2.0);
        return vec4(glowColor * glow * visibility, glow * visibility);
    }
}
```

---

## ⚠️ Notes

- Always check depth BEFORE heavy calculations
- Use fade width to prevent hard edges at occlusion boundaries
- Corona effects may need separate depth check (they extend beyond object)
