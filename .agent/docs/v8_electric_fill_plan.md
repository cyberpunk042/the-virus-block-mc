# V8 Electric Core - Fill Implementation Plan

## Current State
- The `didHit` hack has been removed
- Fill logic in `electricCorePattern` is broken (surface-based, wrong approach)
- Need to rewrite fill as volumetric inner glow

---

## Requirements

### 1. Volumetric Inner Glow
- Fill exists INSIDE the orb as a radial gradient
- Brightest at CENTER, fades toward EDGE
- NEVER touches the surface - fade completes before reaching edge

### 2. Fill Intensity Control (gradient spread)
- Controls the SHAPE of the gradient, not extent
- 0 = tight glow only at very center, fades quickly
- 0.5 = medium spread
- 1 = gradient extends almost to surface (full coverage)
- **Formula idea:** Use intensity to control the power/exponent of the fade curve
  ```glsl
  // depth = 0 at center, 1 at edge
  // intensity controls how quickly it fades
  float fadeCurve = pow(1.0 - depth, mix(4.0, 0.5, fillIntensity));
  // Low intensity (4.0 power): fades very quickly
  // High intensity (0.5 power): fades slowly, extends further
  ```

### 3. Fill Darken Control (color adjustment)
- Works on 0-1 scale with 0.5 as midpoint
- 0 = WHITE
- 0.5 = electric line color (match)
- 1 = BLACK
- **Formula:**
  ```glsl
  vec3 fillColor;
  if (fillDarken < 0.5) {
      // Blend from white to line color
      fillColor = mix(vec3(1.0), electricLineColor, fillDarken * 2.0);
  } else {
      // Blend from line color to black
      fillColor = mix(electricLineColor, vec3(0.0), (fillDarken - 0.5) * 2.0);
  }
  ```

### 4. Fill Color Source
- Derived from the ACTUAL electric line color
- Not from independent bodyColor
- The electric pattern output should be sampled/used directly

### 5. No Leak Outside Orb
- For rays that miss the sphere (`sphere >= sqRadius`), fill = 0
- This happens naturally with the depth formula: `depth = sqrt(sphere/sqRadius)` >= 1.0

---

## Implementation Steps

### Step 1: Remove broken fill from `electricCorePattern`
- Remove lines 79-89 (the MAX-based fill logic)
- Function returns only the electric tendrils

### Step 2: Calculate volumetric fill in `pulsar_v8.glsl`
- After calling `electricCorePattern`, calculate fill separately
- Use `sphere` and `sqRadius` for the depth/radial calculation
- Apply the gradient spread based on `fillIntensity`
- Calculate fill color from `electricCol` and `fillDarken`

### Step 3: Blend fill under tendrils
- Add fill to `col.rgb` BEFORE the tendrils
- Or use additive blending with fill as base layer
- Tendrils render on top

### Step 4: Test and verify
- No leak outside orb
- Intensity controls gradient spread correctly
- Darken control: 0=white, 0.5=line color, 1=black

---

## Key Formulas

### Depth (0 at center, 1 at edge, >1 for miss)
```glsl
float depth = sqrt(sphere / sqRadius);
```

### Fade Curve (controlled by intensity)
```glsl
float safeDepth = min(depth, 1.0);  // Clamp for safety
float power = mix(4.0, 0.5, fillIntensity);  // 4.0 at 0, 0.5 at 1
float fillFade = pow(1.0 - safeDepth, power);
// Also ensure never reaches surface:
fillFade *= smoothstep(1.0, 0.9, depth);  // Fade out at edge
```

### Fill Color
```glsl
vec3 fillColor;
if (fillDarken < 0.5) {
    fillColor = mix(vec3(1.0), electricCol, fillDarken * 2.0);
} else {
    fillColor = mix(electricCol, vec3(0.0), (fillDarken - 0.5) * 2.0);
}
```

### Final Fill Contribution
```glsl
vec3 fillContribution = fillColor * fillFade;
```

---

## Files to Modify

1. `core_patterns.glsl` - Remove fill logic from `electricCorePattern`
2. `pulsar_v8.glsl` - Add volumetric fill calculation in electric core section
