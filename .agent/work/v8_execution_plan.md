# V8 Electric Aura - Research & Execution Plan

## Mission Statement
Create V8 "Electric Aura" shader that combines:
1. **4D noise plasma core** (animated 3D surface using noise4q with turbulent treatment)
2. **2D plasma corona** (nimitz dualfbm with domain warping)
3. **Pulsating rings** (logarithmic, emanate from surface, light up plasma)

---

# PHASE 1: RESEARCH (Current)

## 1.1 Verify Existing Infrastructure ✓

### Files Already Created
- [x] `electric_plasma.glsl` - Contains initial plasma functions (needs rewrite)
- [x] `pulsar_v8.glsl` - Contains main render function (needs rewrite)
- [x] `field_visual_v8.fsh` - Standalone shader entry point
- [x] `field_visual_v8.json` - Post-effect definition

### Dependencies Verified
- [x] `noise_4d.glsl` - Contains `noise4q()` function for 4D noise
- [x] `noise_3d.glsl` - Contains `snoise3d()` for turbulent FBM
- [x] `effect_result.glsl` - Shared result struct
- [x] `corona_effects.glsl` - Contains `subtractiveRing()` (may not need for V8)

### Java Infrastructure
- [x] `ShaderKey.java` - V8 routing added
- [x] `EffectSchemaRegistry.java` - V8 schema registered
- [x] `ShaderPreprocessor.java` - Fixed trailing comment regex

## 1.2 Study V7 Pipeline (Reference)

### V7 Render Flow
```
1. Ray-sphere intersection → pr, surface, sphere, sqRadius
2. Depth check → sphereInFront
3. Occlusion → effectVisibility
4. Body noise → noiseSpherical(surface, ...) → col.rgb
5. Subtractive ring → ring(ray, pos, ...) → col.rgb -= 
6. Ray corona → animatedRayCorona(pr, ...) → s3
7. Alpha → 1.0 - s3 * dr
8. Ray color → mix(rayColor, rayLightColor, s3³) * s3
9. Fade threshold → optional cleanup
10. Return EffectResult{color, alpha, didHit, distance}
```

### Key Variables from V7
| Variable | Meaning | Used In |
|----------|---------|---------|
| `pr` | Projection point relative to sphere | Corona |
| `surface` | Point on sphere surface | Body noise |
| `sphere` | Perpendicular distance² to center | Intersection test |
| `sqRadius` | Radius² | Intersection test |
| `dr` | Distance ratio perpDistSq/sqRadius | Fade inside sphere |
| `c` | `length(pr) * zoom` | Corona distance |

## 1.3 Study nimitz Shader (Target Effect)

### nimitz Core Functions
| Function | Input | Output | Purpose |
|----------|-------|--------|---------|
| `fbm(p)` | vec2 | float [0,~2] | Turbulent FBM noise |
| `dualfbm(p)` | vec2 | float [0,~2] | Domain-warped FBM |
| `circ(p)` | vec2 | float [0.2,~10] | Logarithmic rings |

### nimitz Pipeline
```
1. p = screenCoords * 4.0
2. rz = dualfbm(p)                    // Plasma background
3. p /= exp(mod(time*10, PI))         // Pulsating coords
4. rz *= pow(abs(0.1 - circ(p)), 0.9) // Ring modulation
5. col = baseColor / rz               // Division coloring
6. col = pow(abs(col), 0.99)          // Gamma
```

---

# PHASE 2: IMPLEMENTATION

## Task 2.1: Rewrite electric_plasma.glsl

### Sub-task 2.1.1: Clean Turbulent FBM
**File**: `electric_plasma.glsl`
**Action**: Rewrite `electricFBM()` to match nimitz exactly
```glsl
float electricFBM(vec2 p, float time, int octaves) {
    float z = 2.0, rz = 0.0;
    for (int i = 1; i <= octaves; i++) {
        float n = snoise3d(vec3(p, time * 0.1), 10.0);
        n = n * 0.5 + 0.5;  // Map [-1,1] to [0,1]
        rz += abs((n - 0.5) * 2.0) / z;
        z *= 2.0;
        p *= 2.0;
    }
    return rz;
}
```
**Verification**: Output should be in range [0, ~2]

### Sub-task 2.1.2: Domain-Warped DualFBM
**File**: `electric_plasma.glsl`
**Action**: Match nimitz `dualfbm()` exactly
```glsl
float electricDualFBM(vec2 p, float time, int octaves, float warpAmt) {
    vec2 p2 = p * 0.7;
    vec2 basis = vec2(
        electricFBM(p2 - time * 1.6, time, octaves),
        electricFBM(p2 + time * 1.7, time, octaves)
    );
    basis = (basis - 0.5) * warpAmt;  // nimitz uses 0.2
    p += basis;
    
    float angle = time * 0.2;
    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return electricFBM(p * rot, time, octaves);
}
```
**Verification**: Output should have swirling animated character

### Sub-task 2.1.3: Ring Pattern
**File**: `electric_plasma.glsl`
**Action**: Match nimitz `circ()` exactly
```glsl
float electricRingPattern(float dist, float ringFreq) {
    float r = log(sqrt(dist) + 0.001);
    return abs(mod(r * ringFreq, TAU) - PI) * 3.0 + 0.2;
}
```
**Note**: Input is DISTANCE from surface, not 2D coords

### Sub-task 2.1.4: Pulsating Expansion
**File**: `electric_plasma.glsl`
**Action**: Ring distance expansion
```glsl
float pulsatingDist(float dist, float time, float speed) {
    return dist / exp(mod(time * speed, PI));
}
```

### Sub-task 2.1.5: 4D Core Plasma
**File**: `electric_plasma.glsl`
**Action**: New function for core
```glsl
float core4DPlasma(vec3 surface, float time, int octaves, vec3 seed) {
    float z = 2.0, rz = 0.0;
    vec4 p4 = vec4(surface + seed, time);
    
    for (int i = 1; i <= octaves; i++) {
        float n = noise4q(p4);
        rz += abs((n - 0.5) * 2.0) / z;
        z *= 2.0;
        p4 *= 2.0;
    }
    return rz;
}
```
**Dependency**: Requires `#include "../core/noise_4d.glsl"`

---

## Task 2.2: Rewrite pulsar_v8.glsl

### Sub-task 2.2.1: Keep V7 Foundation
**Sections to KEEP from V7**:
- Ray-sphere intersection (lines ~173-215)
- Depth check (lines ~217-226)
- Occlusion with effectVisibility (lines ~228-259)
- Result struct and return pattern

### Sub-task 2.2.2: Replace Body Noise with 4D Plasma Core
**Section**: Lines ~262-292 in V7
**Replace with**:
```glsl
// 4D PLASMA CORE
vec4 col = vec4(0.0);

if (effectVisibility > 0.01 && sphere < sqRadius) {
    vec3 seedVec = vec3(45.78, 113.04, 28.957) * seed;
    
    // 4D turbulent plasma on sphere surface
    float coreRz = core4DPlasma(surface, fragTime, detail, seedVec);
    
    // Division-based coloring (plasma style)
    coreRz = max(coreRz, 0.01);
    col.rgb = bodyColor / coreRz;
    col.rgb = min(col.rgb, vec3(10.0));  // Prevent oversaturation
    col.rgb *= effectVisibility;
}
```

### Sub-task 2.2.3: Replace Ray Corona with Electric Plasma
**Section**: Lines ~305-340 in V7
**Replace with**:
```glsl
// ELECTRIC PLASMA CORONA
if (effectVisibility > 0.01) {
    // Spherical UVs from projection point
    vec3 prn = normalize(viewRotation * pr);
    float theta = atan(prn.z, prn.x);
    float phi = asin(clamp(prn.y, -1.0, 1.0));
    vec2 plasmaUV = vec2(theta, phi * 2.0) * zoom * 4.0;
    
    // Distance from sphere surface
    float surfaceDist = max(0.0, length(pr) - radius);
    
    // Plasma background
    float plasma = electricDualFBM(plasmaUV + seedVec.xy, fragTime * speedRay, detail, 0.2);
    
    // Pulsating ring distance
    float pulseDist = pulsatingDist(surfaceDist, fragTime, speedRing);
    
    // Ring pattern
    float rings = electricRingPattern(pulseDist, rayString * 4.0);
    
    // Ring modulation
    float ringMod = pow(abs(0.1 - rings), 0.9);
    
    // Combine
    float rz = plasma * ringMod;
    rz = max(rz, 0.01);
    
    // Division coloring
    vec3 coronaCol = rayColor / rz;
    coronaCol = min(coronaCol, vec3(10.0));
    
    // Distance fade
    float maxReach = radius * rayReach * 2.0;
    float fadeWidth = maxReach * 0.3;
    float fadeStart = maxReach - fadeWidth;
    float coronaFade = (surfaceDist > maxReach) ? 0.0 :
                       (surfaceDist < fadeStart) ? 1.0 :
                       1.0 - smoothstep(fadeStart, maxReach, surfaceDist);
    
    // Add to result
    col.rgb += coronaCol * coronaFade * effectVisibility;
}
```

### Sub-task 2.2.4: Remove V7's Subtractive Ring
**Section**: Lines ~294-303 in V7
**Action**: DELETE or comment out - V8 doesn't use V7's dark ring

### Sub-task 2.2.5: Update Alpha Calculation
```glsl
// Alpha based on corona intensity
float coronaIntensity = length(col.rgb);
result.alpha = smoothstep(0.0, 1.0, coronaIntensity);
```

---

## Task 2.3: Update Dependencies

### Sub-task 2.3.1: Fix Include Paths
**File**: `pulsar_v8.glsl`
```glsl
#include "../core/noise_4d.glsl"        // For core4DPlasma
#include "../core/effect_result.glsl"   // Shared result struct
#include "electric_plasma.glsl"         // Corona functions (same dir)
```

### Sub-task 2.3.2: Remove Unused Includes
- Remove `corona_effects.glsl` if not using subtractiveRing

---

## Task 2.4: Verify Shader Compilation

### Sub-task 2.4.1: Build Project
```bash
./gradlew build --no-daemon
```

### Sub-task 2.4.2: Check for Errors
- Preprocessor include resolution
- GLSL syntax errors
- Undefined variable references

---

# PHASE 3: TESTING

## Task 3.1: Visual Verification

### Sub-task 3.1.1: Test Core (4D Plasma)
- Enable V8 in UI
- Set rayReach to minimum
- Verify core has plasma texture (not V7's smooth noise)
- Verify animation is smooth

### Sub-task 3.1.2: Test Corona (Plasma Background)
- Increase rayReach
- Verify plasma fills corona zone
- Verify swirling/animated motion
- Verify no camera-following artifacts

### Sub-task 3.1.3: Test Rings
- Verify rings emanate from sphere surface
- Verify logarithmic spacing (dense near surface)
- Verify pulsating expansion animation
- Verify rings light up the plasma

### Sub-task 3.1.4: Test Distance Fade
- Verify smooth fade at corona edge
- Verify effect doesn't extend infinitely

### Sub-task 3.1.5: Test Occlusion
- Move sphere behind geometry
- Verify bleed-through works correctly

---

# PHASE 4: PARAMETER TUNING

## Task 4.1: Match nimitz Character

### Default Parameter Targets
| Parameter | V7 Default | V8 Target | Purpose |
|-----------|------------|-----------|---------|
| rayString | 1.0 | 1.0 | Ring frequency (×4 internally) |
| rayReach | 1.0 | 2.0 | Corona extent |
| speedRing | 2.0 | 10.0 | Ring pulse speed |
| speedRay | 5.0 | 1.0 | Plasma animation |
| rays | 2.0 | - | Not used in V8 |
| rayRing | 1.0 | - | Not used in V8 |
| glow | 4.0 | - | Not used in V8 |

## Task 4.2: Color Tuning
- Default purple: `vec3(0.2, 0.1, 0.4)`
- Allow override via rayColor uniform

---

# EXECUTION ORDER

| Step | Task | Est. Time | Depends On |
|------|------|-----------|------------|
| 1 | 2.1.1-2.1.4: Corona functions | 15 min | - |
| 2 | 2.1.5: 4D core plasma | 10 min | - |
| 3 | 2.2.1: Keep V7 foundation | 5 min | - |
| 4 | 2.2.2: Replace body with 4D plasma | 10 min | Step 2 |
| 5 | 2.2.3: Replace corona with electric | 15 min | Step 1 |
| 6 | 2.2.4-2.2.5: Cleanup | 5 min | Steps 4,5 |
| 7 | 2.3: Fix includes | 5 min | Steps 4,5 |
| 8 | 2.4: Build & fix errors | 10 min | Step 7 |
| 9 | 3.1: Visual testing | 15 min | Step 8 |
| 10 | 4.1-4.2: Parameter tuning | 10 min | Step 9 |

**Total Estimated Time**: ~100 minutes

---

# SUCCESS CRITERIA

1. **Core**: Animated 4D plasma texture visible on sphere body
2. **Corona**: Swirling dark purple plasma filling space around sphere
3. **Rings**: Logarithmic pulsating rings emanating from surface, lighting up plasma
4. **Color**: Division-based formula creating bright rings on dark background
5. **Stability**: No camera-following, no deformation, fixed to world position
6. **Performance**: Similar to V7 (no major framerate drop)
