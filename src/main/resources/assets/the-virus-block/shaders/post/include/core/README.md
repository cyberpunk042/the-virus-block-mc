# core/ - Foundation Layer

**Dependencies:** None  
**Used by:** All other layers

---

## Purpose

The core layer provides fundamental building blocks with NO external dependencies.
Every file in the library can safely include core files.

## Files

### constants.glsl
```glsl
#include "include/core/constants.glsl"
```

**Exports:**
- `EPSILON`, `NEAR_ZERO`, `RAYMARCH_EPSILON` - Numerical precision
- `PI`, `TWO_PI`, `HALF_PI`, `TAU` - Mathematical constants
- `MAX_RAYMARCH_STEPS` - Raymarch iteration limit
- `SHAPE_*` - Shape type identifiers
- `EFFECT_*` - Effect type identifiers  
- `RENDER_V1_RAYMARCH`, `RENDER_V2_PROJECTION` - Render mode flags

### math_utils.glsl
```glsl
#include "include/core/math_utils.glsl"
```

**Exports:**
- `safeNormalize(v)` - Normalize with zero-length protection
- `safeDivide(a, b)` - Divide with zero protection
- `saturate01(x)` - Clamp to 0-1
- `remap(x, inMin, inMax, outMin, outMax)` - Value remapping
- `smoothMin(a, b, k)`, `smoothMax(a, b, k)` - Smooth blending
- `rotate2D(angle)` - Returns 2x2 rotation matrix
- `rotateVec2(v, angle)` - Rotate a 2D vector

### color_utils.glsl
```glsl
#include "include/core/color_utils.glsl"
```

**Exports:**
- `premultiplyAlpha(color)`, `unpremultiplyAlpha(color)` - Alpha operations
- `toneMapExponential(color, exposure)` - Exponential tone map
- `toneMapAces(color)` - ACES filmic tone map
- `toneMapReinhard(color)` - Reinhard tone map
- `adjustExposure(color, exposure)`, `adjustBrightness`, `adjustContrast`
- `blendAdditive(base, add)`, `blendOver`, `blendScreen`

### noise_utils.glsl
```glsl
#include "include/core/noise_utils.glsl"
```

**Exports:**
- `hash22(p)`, `hash21(p)`, `hash31(p)` - Hash functions
- `voronoi(uv, density, time)` - Animated Voronoi noise
- `voronoiStatic(uv, density)` - Static Voronoi
- `twirlUV(uv, center, strength)` - Spiral UV distortion
- `radialUV(uv, center)` - Convert to polar coordinates
- `sphericalUV(dir)` - Direction to spherical UV

---

## Usage Pattern

```glsl
// Always include constants first
#include "include/core/constants.glsl"

// Then include what you need
#include "include/core/math_utils.glsl"
#include "include/core/noise_utils.glsl"

// Now use them
float r = saturate01(value);
vec2 twisted = twirlUV(uv, vec2(0.5), 2.0);
```

---

## ⚠️ Notes

- All files use include guards (`#ifndef ... #define ... #endif`)
- Safe to include multiple times
- No uniform dependencies - pure functions only
