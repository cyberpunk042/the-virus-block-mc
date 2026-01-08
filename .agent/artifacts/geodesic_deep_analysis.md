# Geodesic Shader Deep Analysis

## Reference: Original Shadertoy Implementation
The user provided the original shader source from Shadertoy.

---

# PART 1: CRITICAL DIFFERENCES FOUND

## 1.1 Wave Resolution (The "30" hardcoded value)

### ORIGINAL:
```glsl
// In animHex1:
float offset = time * 3. * PI;
offset -= subdivisions;  // SUBDIVISIONS AFFECTS PHASE!
float blend = dot(hexCenter, pca);
blend = cos(blend * 30. + offset) * .5 + .5;  // 30 is FIXED
```

### OUR IMPLEMENTATION:
```glsl
float offset = time * 3.0 * PI;
float blend = dot(points.b, geo_pca);
blend = cos(blend * waveResolution + offset) * 0.5 + 0.5;  // waveResolution from user
```

### PROBLEM:
1. Original has `offset -= subdivisions` - **subdivisions affects the wave phase!**
2. We removed this, breaking the animation behavior
3. Wave Resolution (30) in original is about wave COUNT, not "detail level"

---

## 1.2 Subdivisions Animation

### ORIGINAL:
```glsl
float animSubdivisions1() {
    return mix(2.4, 3.4, cos(time * PI) * .5 + .5);  // ANIMATED!
}
```
Subdivisions SMOOTHLY varies between 2.4 and 3.4 over time!

### OUR IMPLEMENTATION:
Subdivisions is a FIXED user slider value. No animation.

### IMPACT:
In original, the cells "morph" between sizes as subdivisions changes. We lost this.

---

## 1.3 The Three Animation Modes

### ORIGINAL Animation 1 (Mode 1):
```glsl
HexSpec animHex1(vec3 hexCenter, float subdivisions) {
    HexSpec spec = newHexSpec(subdivisions);
    float offset = time * 3. * PI;
    offset -= subdivisions;  // <-- KEY: subdivisions in phase
    float blend = dot(hexCenter, pca);
    blend = cos(blend * 30. + offset) * .5 + .5;
    spec.height = mix(1.75, 2., blend);
    spec.thickness = spec.height;
    return spec;
}
```

### ORIGINAL Animation 2 (Mode 2):
```glsl
HexSpec animHex2(vec3 hexCenter, float subdivisions) {
    HexSpec spec = newHexSpec(subdivisions);
    float blend = hexCenter.y;
    spec.height = mix(1.6, 2., sin(blend * 10. + time * PI) * .5 + .5);
    spec.roundTop = .02 / subdivisions;
    spec.roundCorner = .09 / subdivisions;
    spec.thickness = spec.roundTop * 4.;
    spec.gap = .01;
    return spec;
}
```
Note: Mode 2 OVERRIDES roundTop, roundCorner, thickness, gap with calculated values!

### ORIGINAL Animation 3 (Mode 3):
```glsl
HexSpec animHex3(vec3 hexCenter, float subdivisions) {
    HexSpec spec = newHexSpec(subdivisions);
    float blend = acos(dot(hexCenter, pab)) * 10.;
    blend = cos(blend + time * PI) * .5 + .5;
    spec.gap = mix(.01, .4, blend) / subdivisions;
    spec.thickness = spec.roundTop * 2.;
    return spec;
}
```

---

## 1.4 Height Range

### ORIGINAL:
- Mode 1: height varies 1.75 to 2.0 (only 0.25 range!)
- Mode 2: height varies 1.6 to 2.0 (only 0.4 range)
- Mode 3: height stays at 2.0

### OUR IMPLEMENTATION:
```glsl
float heightVar = hexHeight * waveAmplitude;  // If waveAmplitude=0.25, hexHeight=2
// heightVar = 0.5, so height varies from 1.5 to 2.5
```
This is MUCH larger range! That's why "amplitude too sensitive"

---

## 1.5 Spectrum Color Multiplier

### ORIGINAL:
```glsl
vec3 edgeColor = spectrum(dot(hexCenter, pca) * 5. + length(p) + .8);
```
The multiplier is **5** (fixed).

### OUR IMPLEMENTATION:
```glsl
vec3 edgeColor = geo_spectrum(dot(hexCenter, geo_pca) * edgeColorOffset + length(p) + 0.8);
```
We made it user-controlled. Default should be 5.

---

# PART 2: WHAT "RESOLUTION" SHOULD ACTUALLY CONTROL

The user wants to control the "level of detail that deforms the tiles."

Looking at the original, the key factors are:

1. **Subdivisions** (1-10 range reasonable) - Controls how many cells
2. **Wave frequency** (the "30") - Controls how many wave peaks across sphere
3. **Height range** - How much cells pop up/down (should be SMALL: 0.2-0.5 max)

The "broken quad" appearance is because:
- Each icosahedral face (20 total) is visible
- Within each face, triangular tiling creates the hex cells
- Higher subdivisions = more cells within each triangular group

---

# PART 3: RECOMMENDED FIXES

## Fix 1: Restore subdivisions in wave phase
```glsl
float offset = time * 3.0 * PI - subdivisions;  // Add back: - subdivisions
```

## Fix 2: Fix amplitude scaling
```glsl
// Original height range was only 0.25 (from 1.75 to 2.0)
// So amplitude of 1.0 should give height range of 0.25
float heightVar = hexHeight * 0.125 * waveAmplitude;  // 0.125 = 0.25/2.0
```

## Fix 3: Fix rotation speed range
Original: `pR(p.xz, time * PI/16.);` = very slow rotation
Our slider goes 0-100 which is WAY too fast.
Range should be 0-1 with default 0.0625 (PI/16 ≈ 0.196)

## Fix 4: Fix rounding ranges
Original: 
```glsl
spec.roundTop = .05 / subdivisions;
spec.roundCorner = .1 / subdivisions;
```
With subdivisions=3, roundTop=0.017, roundCorner=0.033
Slider max of 100 is way too high!
Range: 0-1 with defaults 0.05 and 0.1

## Fix 5: Add animating subdivisions option
Allow subdivisions to animate like original: `mix(2.4, 3.4, cos(time * PI) * .5 + .5)`

---

# PART 4: PARAMETER RANGES RECOMMENDATION

| Parameter | ORIGINAL Value | Recommended Range | Recommended Default |
|-----------|----------------|-------------------|---------------------|
| Subdivisions | 2.4-3.4 animated | 1-20 | 3 |
| Height | 2.0 | 0.5-5 | 2.0 |
| Thickness | varies 0.02-2.0 | 0.01-5 | 2.0 |
| Gap | 0.005-0.4 | 0-0.5 | 0.005 |
| RoundTop | 0.05/subdiv | 0-0.5 | 0.05 |
| RoundCorner | 0.1/subdiv | 0-0.5 | 0.1 |
| WaveFrequency | 30 (fixed) | 1-100 | 30 |
| HeightAmplitude | 0.125 (0.25 range on height 2) | 0-1 | 0.125 |
| RotationSpeed | PI/16 ≈ 0.2 | 0-2 | 0.2 |
| SpectrumOffset | 5 | 0-20 | 5 |

---

# PART 5: ACTION ITEMS

1. [ ] Update shader to add `- subdivisions` back to wave offset
2. [ ] Scale amplitude: multiply by 0.125 in shader
3. [ ] Reduce slider ranges: rotation 0-2, rounding 0-0.5, amplitude 0-1
4. [ ] Set correct defaults matching original
5. [ ] Consider adding "Subdivisions Animation" toggle/speed control
6. [ ] Verify each animation mode matches original behavior

