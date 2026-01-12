# God Rays Bug Analysis - What Went Wrong

## Date: 2026-01-12T09:52

---

## ISSUE 1: Distribution Panel Only Shows 3 Modes

**Symptom**: Panel loops back after Noise, only 3 visible despite 5 in code

**Investigation**:
- Panel code DOES have 5 modes: `List.of("Uniform", "Weighted", "Noise", "Random", "Stochastic")`
- But CyclingButtonWidget may be wrapping at 3

**Root Cause**: 
The CyclingButtonWidget `initially()` might be resetting due to distMode value being out of range when loaded from config

**Fix Needed**: Check if RenderConfig stores distribution mode > 2 correctly

---

## ISSUE 2: Energy Mode All Broken

**Symptom**: All energy modes look wrong/same

**Investigation**:
- getEnergyVisibility() function exists and has all 8 modes
- getGodRayDirection() exists and has all 8 modes
- Both are being called from god_rays.glsl

**Possible Root Causes**:
1. The visibility function formulas are too subtle
2. The smoothstep wave math in EMISSION/ABSORPTION is producing near-constant values
3. The direction changes are too subtle (0.03 pulse offset is invisible)

**Specific Problems**:
- EMISSION visibility: `smoothstep(phase - 0.3, phase, t) * smoothstep(phase + 0.3, phase, t)` 
  - This produces a very narrow band that's hard to see
- ABSORPTION direction: `inward + tangent * 0.03` - basically looks like inward with imperceptible change

---

## ISSUE 3: Curvature Modes 4,5,6 All Same

**Symptom**: Pinwheel, Logarithmic, Orbital all look identical

**Investigation**:
```
Mode 4: LOGARITHMIC - curveAngle = dist * goldenAngle * curvatureStrength * 2.0
Mode 5: PINWHEEL - curveAngle = angle * curvatureStrength + time * 0.8
Mode 6: ORBITAL - returns early with different calculation
```

**Possible Root Causes**:
1. Panel index mismatch - RayCurvature enum order might not match shader indices
2. curvatureStrength is too low to see difference
3. The early return in Mode 6 (ORBITAL) might be causing issues

**RayCurvature Enum Order** (from Java):
- 0: NONE
- 1: VORTEX
- 2: SPIRAL_ARM
- 3: TANGENTIAL
- 4: LOGARITHMIC
- 5: PINWHEEL
- 6: ORBITAL

**Shader expects same order** - looks correct

---

## ISSUE 4: Arrangement SPHERICAL Broken, CONVERGING/DIVERGING Same as PARALLEL

**Symptom**: SPHERICAL shows weird disk, CONVERGING/DIVERGING don't work

**Investigation**:
```
Mode 1: SPHERICAL - baseLightUV + dir * radius (radius default 0.05)
Mode 2: PARALLEL - baseLightUV + vec2(0.0, -0.5)
Mode 3: CONVERGING - baseLightUV - dir * 0.6
Mode 4: DIVERGING - just returns baseLightUV (same as RADIAL!)
```

**Root Causes**:
1. SPHERICAL uses dir * 0.05 which is tiny offset
2. PARALLEL adds fixed offset downward - might look same as radial
3. CONVERGING subtracts in wrong direction
4. DIVERGING does NOTHING - just returns baseLightUV (I left it as placeholder!)

**Fix Needed**: Actually implement CONVERGING and DIVERGING properly

---

## ISSUE 5: Travel Mode - Working Ones Broke, Others Still Broken

**Symptom**: SCROLL and COMET stopped working

**Investigation**:
Looking at getTravelModulation:
- Mode 2 SCROLL returns `fract(t - animTime)` - should work
- Mode 3 COMET returns `exp(-dist / tailLength)` - should work

**Possible Root Cause**:
1. The function signature changed
2. travelSpeed parameter might be 0 or wrong value
3. Something in the caller (god_rays.glsl) changed

---

## SUMMARY OF MY MISTAKES

1. **Visibility formulas too subtle** - smoothstep bands too narrow
2. **Direction changes too subtle** - 0.03 offset is invisible
3. **DIVERGING not implemented** - left as placeholder that does nothing
4. **Didn't test after changes** - made multiple changes without incremental testing
5. **Changed too many things at once** - should have fixed one thing, tested, repeated

---

## RECOMMENDED FIX APPROACH

1. Revert to simpler, proven formulas
2. Test one mode at a time
3. Use larger, more visible effects
4. Actually implement DIVERGING instead of placeholder
