# Investigation: Scale Mult Slider Causes Visual Bands

## Issue Summary
**Date:** 2026-01-10  
**Effect:** Pulsar Sun V6 (field_visual_v6.fsh)  
**Parameter:** Scale Mult (noiseScaleMultiplier)

---

## CONFIRMED FACTS

### Trigger
- Changing Scale Mult slider **3-5 times** causes the bug
- The ACT of changing triggers it, not a specific value

### Visual Description
- **HORIZONTAL bands** that disrupt the animation
- Animation at those bands appears **at an offset** (out of phase)
- **Loss of details** in places
- **Starts from the BOTTOM** of the sphere first

### What Fixes It
- **Pausing animation** (AnimSpeed → 0.01 or lower) → looks correct
- Returning to normal speed → broken again
- **Magic values 13.25 and 25.50** work consistently without bands
- 25.0 (original value) → "almost perfect but definitely glitchy"

### What Does NOT Fix It
- Setting slider back to original value
- Any value other than the magic numbers (during animation)

### Animation OFF
- **ALL values look correct** when animation is stopped
- This proves the values themselves are valid

---

## KEY OBSERVATIONS

1. **Lower animation speed → works**
2. **Higher animation speed → breaks**
3. **Horizontal bands = phi (vertical) coordinate issue**
4. **Starts from bottom = issue begins at phi ≈ PI (south pole)**
5. **Offset appearance = different phi values have different phase**

---

## WHAT THIS POINTS TO

The visual description strongly suggests:
- Something in the noise calculation creates **phase discontinuities** at different Y/phi coordinates
- The `mod()` wrapping in `snoise3d` creates different "cells" at different heights
- When `noiseScaleMult` is changed, adjacent phi values suddenly land in **different noise cells**
- This creates the "offset" appearance - horizontal bands with different animation phases

The "starts from bottom" suggests the issue severity depends on the phi coordinate value itself.

---

## FAILED ATTEMPTS

1. **Disabled SYNCED_TIME mode** → No effect
2. **Clamped res in snoise3d** → No effect (user said introduced another glitch)
3. **Removed (newTime+1.0) multipliers** → Introduced different glitch
4. **Reset animation time on slider change** → No effect
5. **Wrapped sin() input with mod()** → No effect

---

## NEXT INVESTIGATION DIRECTIONS

### Direction 1: Examine the phi coordinate handling
Why does it start from the bottom? Is there something about `sc.phi * 0.5` that creates discontinuities?

### Direction 2: Look for rounding/precision issues
User suspects "rounding / max attained that disrupts all the rest"
- Check if there's a threshold being hit
- Check float precision in the calculation chain

### Direction 3: Examine why 13.25 and 25.50 work
- What's special about these numbers?
- 13.25 = 53/4
- 25.50 = 51/2 = 102/4
- Are they avoiding some resonance frequency?

### Direction 4: Check the coordinate offset accumulation
```glsl
coord + vec3(0.0, -time * loopSpeed, time * loopSpeed * 0.2)
```
Is this offset creating the horizontal band pattern?

---

## RELEVANT CODE SECTIONS

### Spherical coordinates (pulsar_v6.glsl)
```glsl
SphericalCoords sc = cartesianToSpherical(surfacePoint);
vec3 coord = vec3(sc.theta, sc.phi * 0.5, time * 0.1);
```

### Noise call with noiseScaleMult (pulsar_v6.glsl)
```glsl
fVal2 += (noiseAmplitude / power) * pulsarSnoise(
    coord + vec3(0.0, -time * loopSpeed, time * loopSpeed * 0.2),
    power * noiseScaleMult * (newTime2 + 1.0)
);
```

### The snoise3d mod wrapping (noise_3d.glsl)
```glsl
uv *= res;
vec3 uv0 = floor(mod(uv, res)) * s;
```

---

## FILES
- `pulsar_v6.glsl` - Main effect shader
- `noise_3d.glsl` - Noise function
- `FieldVisualAdapter.java` - Slider handling
- `ShaderTimeSource.java` - Time source

---

## CRITICAL DISCOVERY

### Default Value Mismatch!

**Original Shadertoy value:** `noiseScaleMult = 25.0`  
**Java DEFAULT value:** `noiseScaleMult = 4.0`  

The algorithm was **designed and tuned for 25.0**, but we're defaulting to 4.0!

The user's "magic values" that work:
- **13.25** (roughly half of original)
- **25.50** (very close to original 25.0!)

The user said 25.0 is "almost perfect but glitchy" - this suggests the algorithm IS designed for that range.

### Hypothesis: Algorithm Only Stable Near Original Values

The noise algorithm has been tuned for a specific range of `noiseScaleMult` values. When you stray too far from the original 25.0:
- The phase relationships between noise layers break down
- The `mod()` wrapping creates discontinuities
- The visual "bands" appear

Values near the original (13.25, 25.50) maintain stability because they're within the algorithm's tuned range.
