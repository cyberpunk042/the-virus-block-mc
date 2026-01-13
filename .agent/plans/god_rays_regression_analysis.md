# God Rays Regression Analysis

## Session Changes That Broke Things

### Changes Made Today (Uncommitted)

1. **Added travelCount and travelWidth parameters**
   - Changed function signature from 5 params to 7 params
   - `getTravelModulation(t, time, travelMode, speed, rayRand)` → `getTravelModulation(t, time, travelMode, speed, particleCount, particleWidth, rayRand)`

2. **Rewrote COMET mode**
   - OLD: Used `phase` directly (0→1, wrapped with fract), returned 0.0 when outside tail
   - NEW: Uses triangular wave, returns 0.1 as base
   - **Issue**: Triangular wave makes comet oscillate back and forth instead of traveling in one direction

3. **Rewrote SPARK mode**  
   - OLD: Used hash-based deterministic randomness
   - NEW: Uses index-based positions with flicker
   - **Issue**: Might not produce visible sparks

4. **Changed Noise modulation**
   - OLD: Simple `sin(angle * scale + time * speed)`
   - NEW: Quantized bands with `floor(normalizedAngle * scale)`
   - **Issue**: Discrete stepping instead of smooth variation

5. **Changed RADIAL arrangement weight**
   - OLD: Based on direction vector's y-component
   - NEW: Based on vertical distance from light
   - **Issue**: Might not produce the horizontal disk effect correctly

6. **Changed STOCHASTIC distribution**  
   - Added `time * 0.5` for animation
   - **Issue**: Animation speed is hardcoded, no user control

---

## Analysis of User-Reported Issues

### 1. "OSCILLATION has hard cut transition"

**Looking at the code:**
```glsl
} else if (energyMode < 6.5) {
    // Mode 6: OSCILLATION
    float breathPhase = sin(time * 1.5) * 0.5 + 0.5;
    return mix(absorptionDensity, emissionDensity, breathPhase);
}
```

This uses `sin()` which is smooth. **The hard cut might be a mode indexing issue** - the user might be selecting mode 7 (RESONANCE) which uses `fract()`.

**Action needed**: Verify dropdown order matches shader indices.

---

### 2. "Flicker intensity controlling light intensity"

**The flicker formulas:**
```glsl
// SCINTILLATION
flick = 1.0 - intensity * noise;  // When intensity=1, noise=1 → flick=0 → BLACK

// All modes apply:
return clamp(flick, 0.0, 1.0);
```

**Problem**: High intensity + high noise = complete darkness. The user expects "flicker intensity" to control the SPEED or AMOUNT of flicker, not the DIMMING.

**Root cause**: The parameter name "intensity" is confusing. It's actually controlling HOW DARK the flicker can make the ray.

---

### 3. "ALL Travel Modes Broken - Huge Regression"

**I changed the function signature and added count/width parameters.**

Looking at the call site in `god_rays.glsl`:
```glsl
float travelMod = getTravelModulation(t, time, travelMode, travelSpeed, travelCount, travelWidth, rayRand);
```

**Potential issues:**
1. If `travelCount` or `travelWidth` are coming in as 0, modes would break
2. The COMET rewrite broke the one-directional travel
3. The SPARK rewrite removed the hash-based randomness that worked

**The OLD COMET code worked because:**
- `phase` went 0→1 continuously
- Comet traveled in one direction

**The NEW COMET code is broken because:**
- Triangular wave goes 0→1→0
- Comet oscillates back and forth instead of traveling

---

### 4. "Noise is weird, hard to fine-tune"

**My change:**
```glsl
float bandedAngle = floor(normalizedAngle * scale) / scale;
```

This creates DISCRETE bands instead of continuous variation. The noise "jumps" from one value to another instead of smoothly transitioning.

---

### 5. "Stochastic has bug, timebound with no calibration"

**The code:**
```glsl
float timeRand = fract(sin(rayAngle * 43.233 + time * 0.5) * 12345.6789);
```

The `time * 0.5` is hardcoded. User has no slider to control this animation speed.

---

## What Was Working Before

Based on the git diff, the ORIGINAL code for travel modes:
- COMET: Used `phase` directly, traveled in one direction, returned 0.0 outside tail
- SPARK: Used hash function with `h1`, `h2` for deterministic randomness
- Used hardcoded `count = 3` and `width = 0.15`

The original travel modes were simpler and probably worked better.

---

## Recommended Rollback

Consider reverting the uncommitted changes to `god_rays_style.glsl` and then carefully re-implementing only the necessary fixes:

```bash
git checkout HEAD -- src/main/resources/assets/the-virus-block/shaders/post/include/effects/god_rays_style.glsl
```

Then selectively re-apply:
1. The curvature mode fixes (LOGARITHMIC, PINWHEEL, ORBITAL)
2. Keep the arrangement weight fix for RADIAL if it was actually better
