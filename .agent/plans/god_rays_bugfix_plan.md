# God Rays - Bug Fix Plan

## Status: FIXES APPLIED - Ready for Retest

### Fixes Applied (2026-01-12)
1. ✅ Color Temperature - Hide Color2 sliders (Option A)
2. ✅ Travel Mode - Fixed CHASE, SPARK, PULSE_WAVE formulas
3. ✅ Curvature Mode 4,5,6 - Made LOGARITHMIC, PINWHEEL, ORBITAL distinct
4. ✅ Energy Mode - Rewrote with wider falloffs and minimum visibility
5. ✅ Arrangement Mode - Fixed SPHERICAL default radius, improved PARALLEL/CONVERGING
6. ✅ Distribution Mode - Added RANDOM and STOCHASTIC modes

---

## Test Results (2026-01-12)

### 1. ENERGY MODE - ALL BROKEN ❌

**Current Implementation**: `getEnergyVisibility(t, time, energyMode)`
- Uses visibility masking based on position along ray
- Multiplies into illumination

**Problem**: Visibility masking approach doesn't create the expected visual effect.
The effect should show *animated segments* moving along the ray, but current
implementation doesn't produce visible results.

**Fix Strategy**: 
- Increase falloff values (currently 0.2-0.3, try 0.5-0.8)
- Add minimum visibility floor so rays don't disappear completely
- Consider alternative approach: modulate intensity rather than visibility

---

### 2. ARRANGEMENT MODE - MOSTLY BROKEN ❌

**SPHERICAL (mode 1)**: Creates weird disk
- Current: `baseLightUV + toPixel * screenRadius`
- Problem: screenRadius calculation may be wrong

**PARALLEL (mode 2)**: Doesn't work
- Current: Places light 10 units away in sunDir
- Problem: Direction hardcoded to (0,1), not configurable

**CONVERGING (mode 3)**: Wrong
- Current: Complex math that doesn't make sense
- Should: Rays from outer edge toward center

**DIVERGING (mode 4)**: Same as RADIAL
- Current: Just returns baseLightUV
- Should: Different visual from RADIAL somehow

**Fix Strategy**:
- Simplify SPHERICAL to use actual orb radius
- Make PARALLEL actually create parallel rays
- Rethink CONVERGING/DIVERGING - maybe affect direction not source?

---

### 3. COLOR TEMPERATURE - UI MISMATCH ⚠️

**Current**: Panel shows Color2 R/G/B when colorMode >= 1 (Gradient or Temperature)
**Problem**: Temperature mode uses hardcoded warm=(1,0.7,0.3) and cool=(0.3,0.7,1)

**Options**:
A. Hide Color2 for Temperature (only show for Gradient)
B. Use Color2 as cool color in Temperature mode

**Decision Needed**: Which option?

---

### 4. CURVATURE MODES 4,5,6 - NEED VERIFICATION

**Claimed not working**: Logarithmic, Pinwheel, Orbital

**Current shader code** (applyCurvature):
```glsl
} else if (curvatureMode < 4.5) {
    // Mode 4: LOGARITHMIC
    float goldenAngle = 2.39996;
    curveAngle = log(max(dist, 0.01) + 1.0) * goldenAngle * curvatureStrength + time * 0.2;
} else if (curvatureMode < 5.5) {
    // Mode 5: PINWHEEL
    curveAngle = angle * curvatureStrength * 0.5 + time * 1.0;
} else {
    // Mode 6: ORBITAL
    vec2 tangent = vec2(-toLight.y, toLight.x);
    vec2 orbitDir = normalize(mix(dir, normalize(tangent), curvatureStrength));
    return orbitDir;
}
```

**Possible issues**:
- Panel indices may not match shader indices
- curvatureStrength may be too low to see effect
- ORBITAL returns early, doesn't use rotateVec2

**Fix Strategy**:
- Verify panel enum order matches shader mode numbers
- Test with high curvatureStrength (1.0+)
- Fix ORBITAL to be more visible

---

### 5. TRAVEL MODE - PARTIALLY BROKEN

**Working**: Scroll, Comet
**Broken**: Chase, Spark, Pulse_Wave

**Current shader code** (getTravelModulation):
- CHASE (mode 1): Uses loop, may be too complex
- SCROLL (mode 2): Simple fract() - WORKS
- COMET (mode 3): Exponential tail - WORKS
- SPARK (mode 4): Random sparks, showSpark threshold may filter all out
- PULSE_WAVE (mode 5): Similar to comet but different params

**Fix Strategy**:
- Simplify CHASE formula
- Lower showSpark threshold (0.7 → 0.3)
- Increase PULSE_WAVE waveWidth

---

## Implementation Order

1. First: Hide Color2 for Temperature (quick fix)
2. Second: Fix Travel Mode broken formulas
3. Third: Fix Curvature Mode 4-6
4. Fourth: Rewrite Energy Mode completely
5. Fifth: Rewrite Arrangement Mode completely

---

*Created: 2026-01-12*
