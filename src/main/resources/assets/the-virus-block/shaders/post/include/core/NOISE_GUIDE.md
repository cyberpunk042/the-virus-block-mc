# Noise Library Selection Guide

Quick reference for choosing which noise to use.

---

## Available Noise Libraries

| Library | File | Best For |
|---------|------|----------|
| **noise_4d.glsl** | `core/noise_4d.glsl` | Stars, planets, multi-scale surfaces |
| **noise_3d.glsl** | `core/noise_3d.glsl` | Fire, flames, organic patterns |

---

## noise_4d.glsl (Panteleymonov/V7)

**Functions:**
```glsl
float noise4q(vec4 x)                    // Core 4D noise
float noiseSpherical(...)                // Multi-scale sphere texture
float noise4q_animated(vec3 pos, time)   // Convenience wrapper
float noise4q_turbulent(pos, time, oct)  // Stacked octaves
```

**Visual Character:**
- Smooth, flowing
- Good continuity across time (4th dimension)
- Multi-scale detail with controllable LOD
- Used by: **V7 Pulsar Sun**

**When to use:**
- Star/sun surfaces with animated plasma
- Planet textures
- Anything needing smooth 4D animation

---

## noise_3d.glsl (trisomie21/V5/V6)

**Functions:**
```glsl
float snoise3d(vec3 uv, float res)           // Core 3D noise
vec3 surfaceTexture3d(vec2 uv, time, scale)  // Multi-octave texture
float flameNoise(...)                        // Fire/corona pattern
```

**Visual Character:**
- More organic, fire-like
- Good for flames and corona
- Range: [-1, 1] (unlike noise4q which is [0, 1])
- Used by: **V5 Pulsar, V6 Pulsar 3D**

**When to use:**
- Fire/flame effects
- Corona/atmosphere glow
- Organic surface patterns

---

## Side-by-Side Comparison

| Feature | noise_4d | noise_3d |
|---------|----------|----------|
| Dimensions | 4D | 3D |
| Output range | [0, 1] | [-1, 1] |
| Animation | Built-in (w=time) | Manual (z=time) |
| Visual style | Smooth, ethereal | Organic, fiery |
| Performance | Heavier | Lighter |
| Multi-scale | noiseSpherical() | surfaceTexture3d() |

---

## Usage Examples

### Animated Star Surface (V7 style)
```glsl
#include "include/core/noise_4d.glsl"

float surfaceDetail = noiseSpherical(
    surfacePoint, perpDistSq, radiusSq,
    zoom, seedVec, time,
    speedHi, speedLow, detail
);
```

### Fire/Flame Corona (V5/V6 style)
```glsl
#include "include/core/noise_3d.glsl"

float flames = flameNoise(
    surfaceCoords, time, 7,
    amplitude, baseScale, scaleMult,
    radialSpeed1, radialSpeed2, loopSpeed
);
```

### Simple Animated Noise
```glsl
// 4D version (smoother)
float n = noise4q(vec4(pos * 10.0, time));

// 3D version (more organic)
float n = snoise3d(vec3(pos.xy * 10.0, time), 15.0);
```

---

## Backwards Compatibility

Legacy names still work:

| Old Name | Maps To |
|----------|---------|
| `v7_noise4q` | `noise4q` |
| `v7_noiseSpere` | `noiseSpherical` |
| `pulsarSnoise` | `snoise3d` |
| `pulsarSurfaceTexture` | `surfaceTexture3d` |
