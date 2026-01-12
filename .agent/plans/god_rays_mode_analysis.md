# God Rays - Complete Mode Analysis

## Methodology
For each mode category, I will document:
1. **Panel**: What controls exist
2. **RenderConfig**: What fields/getters/setters exist
3. **UBO**: What slot and how wired
4. **Shader**: What code actually runs
5. **Formula**: The actual math (if any)
6. **Status**: WORKING / PARTIAL / NOT CODED

---

## 1. ENERGY MODE (RadiativeInteraction)

### Panel
- Dropdown: Now using `RadiativeInteraction.values()` 
- Values shown: NONE, EMISSION, ABSORPTION, REFLECTION, TRANSMISSION, SCATTERING, OSCILLATION, RESONANCE

### RenderConfig
- Field: `godRaysEnergyMode` (int)
- Getter: `getGodRaysEnergyMode()` → int
- Setter: `setGodRaysEnergyMode(int)`

### UBO
- Slot 52.x (`GodRayStyleParams.energyMode`)
- GLSL uniform: `GodRayEnergyMode`

### Shader Code (god_rays_style.glsl:83-101)
```glsl
vec2 getGodRayDirection(..., float energyMode, ...) {
    if (energyMode < 0.5) {
        // Mode 0: Radiation - march from pixel TOWARD light
        baseDir = normalize(toLight);
    } else if (energyMode < 1.5) {
        // Mode 1: Absorption - march from light TOWARD pixel
        baseDir = normalize(-toLight);
    } else {
        // Mode 2: Pulse - alternate direction based on time
        float pulse = sin(time * 2.0) * 0.5 + 0.5;
        baseDir = normalize((pulse > 0.5) ? toLight : -toLight);
    }
    return applyCurvature(baseDir, ...);
}
```

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| NONE | 0 | **NOT CODED** - Index 0 acts as Radiation | ❌ NO "no effect" option |
| EMISSION | 1 | Coded as mode 0 (index mismatch!) | ⚠️ INDEX MISMATCH |
| ABSORPTION | 2 | Coded as mode 1 | ⚠️ INDEX MISMATCH |
| REFLECTION | 3 | NOT CODED | ❌ |
| TRANSMISSION | 4 | NOT CODED | ❌ |
| SCATTERING | 5 | NOT CODED | ❌ |
| OSCILLATION | 6 | Coded as mode 2 (pulse) | ⚠️ INDEX MISMATCH |
| RESONANCE | 7 | NOT CODED | ❌ |

### CRITICAL ISSUE
Panel sends enum ordinal (0-7) but shader only handles 0-2!
- Panel NONE (0) → Shader mode 0 (Radiation) ✓ accidental match but wrong behavior
- Panel EMISSION (1) → Shader mode 1 (Absorption) ✗ WRONG!
- Panel ABSORPTION (2) → Shader mode 2 (Pulse) ✗ WRONG!

### IMPLEMENTATION PLAN

**Key Insight**: These modes control **visibility along the ray** (which part is visible), not just direction.
The direction is always toward/away from light. The **phase** (time-driven 0-1) controls which segment is visible.

**Step 1**: Add `getEnergyVisibility()` function for visibility masking

```glsl
/**
 * Get visibility factor based on energy mode and position along ray.
 * 
 * @param t Position along ray (0=at light, 1=at pixel)
 * @param time Animation time
 * @param energyMode RadiativeInteraction enum index
 * @return Visibility multiplier (0-1)
 */
float getEnergyVisibility(float t, float time, float energyMode) {
    float phase = fract(time * 0.5); // Animated phase 0-1
    
    if (energyMode < 0.5) {
        // Mode 0: NONE - full visibility
        return 1.0;
    } else if (energyMode < 1.5) {
        // Mode 1: EMISSION - visible segment moves inner→outer
        // At phase 0: visible at t=0 (inner), at phase 1: visible at t=1 (outer)
        float visiblePos = phase;
        float falloff = 0.3;
        return exp(-abs(t - visiblePos) / falloff);
    } else if (energyMode < 2.5) {
        // Mode 2: ABSORPTION - visible segment moves outer→inner
        float visiblePos = 1.0 - phase;
        float falloff = 0.3;
        return exp(-abs(t - visiblePos) / falloff);
    } else if (energyMode < 3.5) {
        // Mode 3: REFLECTION - goes out, bounces back
        // phase 0-0.5: moving outward, 0.5-1: moving inward
        float visiblePos = (phase < 0.5) ? phase * 2.0 : (1.0 - phase) * 2.0;
        float falloff = 0.2;
        return exp(-abs(t - visiblePos) / falloff);
    } else if (energyMode < 4.5) {
        // Mode 4: TRANSMISSION - fixed-length segment moves along ray
        float segmentLength = 0.2;
        float segmentCenter = phase;
        float dist = abs(t - segmentCenter);
        return (dist < segmentLength) ? 1.0 : 0.0;
    } else if (energyMode < 5.5) {
        // Mode 5: SCATTERING - scattered/randomized visibility
        float noise = fract(sin(t * 100.0 + time * 5.0) * 43758.5453);
        return noise * 0.7 + 0.3; // Always some visibility, random variation
    } else if (energyMode < 6.5) {
        // Mode 6: OSCILLATION - whole ray breathes in/out
        // phase 0→0.5: shrink (less visible), 0.5→1: expand (more visible)
        float breathe = sin(phase * 6.28318) * 0.5 + 0.5;
        return breathe;
    } else {
        // Mode 7: RESONANCE - grow then decay
        // phase 0→0.5: grow (visibility spreads outward)
        // phase 0.5→1: shrink (visibility contracts inward)
        float reach = (phase < 0.5) ? phase * 2.0 : (1.0 - phase) * 2.0;
        return (t < reach) ? 1.0 : 0.1;
    }
}
```

**Step 2**: Update ray direction (simpler now - just in/out)

```glsl
vec2 getGodRayDirection(vec2 pixelUV, vec2 lightUV, float energyMode, ...) {
    vec2 toLight = lightUV - pixelUV;
    vec2 baseDir;
    
    // Direction is simpler - most modes march toward light
    // ABSORPTION marches away from light
    if (energyMode > 1.5 && energyMode < 2.5) {
        baseDir = normalize(-toLight); // ABSORPTION only
    } else {
        baseDir = normalize(toLight);  // All other modes
    }
    
    return applyCurvature(baseDir, pixelUV, lightUV, curvatureMode, curvatureStrength, time);
}
```

**Step 3**: Apply visibility in ray march loop

```glsl
// In god_rays.glsl or god_rays_accum.fsh
for (int i = 0; i < steps; i++) {
    float t = float(i) / float(steps);
    float sample = texture(scene, samplePos).r;
    
    // Apply energy visibility mask
    float energyVis = getEnergyVisibility(t, time, GodRayEnergyMode);
    sample *= energyVis;
    
    accumulator += sample * weight * decay;
}
```

**Step 4**: Panel already uses RadiativeInteraction enum - no change needed

---

## 2. COLOR MODE

### Panel
- Dropdown: `List.of("Solid", "Gradient", "Temperature")`
- Values: 0, 1, 2

### RenderConfig
- Field: `godRaysColorMode` (int)
- Getter: `getGodRaysColorMode()` → int

### UBO
- Slot 52.y (`GodRayStyleParams.colorMode`)
- GLSL uniform: `GodRayColorMode`

### Shader Code (god_rays_style.glsl:192-215)
```glsl
vec3 getGodRayColor(..., float colorMode, float gradientPower) {
    if (colorMode < 0.5) {
        // Mode 0: Solid - just use primary color
        return color1 * illumination;
    } else if (colorMode < 1.5) {
        // Mode 1: Gradient - blend from color1 (center) to color2 (edge)
        float t = pow(clamp(screenDist * 2.0, 0.0, 1.0), gradientPower);
        vec3 blendedColor = mix(color1, color2, t);
        return blendedColor * illumination;
    } else {
        // Mode 2: Temperature - warm at center, cool at edges
        vec3 warm = vec3(1.0, 0.7, 0.3);  // Orange
        vec3 cool = vec3(0.3, 0.7, 1.0);  // Cyan
        float t = pow(clamp(screenDist * 2.0, 0.0, 1.0), gradientPower);
        vec3 tempColor = mix(warm, cool, t) * color1;
        return tempColor * illumination;
    }
}
```

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| Solid | 0 | `color1 * illumination` | ✅ WORKING |
| Gradient | 1 | `mix(color1, color2, t)` with power curve | ✅ WORKING |
| Temperature | 2 | Fixed warm→cool gradient, modulated by color1 | ✅ WORKING |

### Dependencies
- **color1**: From `RayColorR/G/B` (Slot 5) - need to verify panel control
- **color2**: From `GodRayColor2R/G/B` (Slot 53) - JUST ADDED panel sliders!
- **gradientPower**: From `GodRayGradientPower` (Slot 53.w)

---

## 3. DISTRIBUTION MODE

### Panel
- Dropdown: `List.of("Uniform", "Weighted", "Noise")`
- Values: 0, 1, 2

### RenderConfig
- Field: `godRaysDistributionMode` (int)
- Getter: `getGodRaysDistributionMode()` → int

### UBO
- Slot 52.z (`GodRayStyleParams.distributionMode`)
- GLSL uniform: `GodRayDistributionMode`

### Shader Code (god_rays_style.glsl:116-139)
```glsl
float getAngularWeight(..., float distributionMode, float angularBias, float time) {
    if (distributionMode < 0.5) {
        // Mode 0: Uniform - subtle breathing animation
        float breath = sin(time * 1.5) * 0.1 + 1.0;
        return breath;
    }
    
    if (distributionMode < 1.5) {
        // Mode 1: Weighted - bias rotates over time
        float rotatingBias = angularBias + sin(time * 0.5) * 0.5;
        float vertWeight = abs(dir.y);
        float horizWeight = abs(dir.x);
        float weight = mix(vertWeight, horizWeight, rotatingBias * 0.5 + 0.5);
        float pulse = sin(time * 2.0 + angle) * 0.15 + 1.0;
        return (0.3 + weight * 0.7) * pulse;
    }
    
    // Mode 2: Noise - handled separately
    return 1.0;
}
```

### Noise Mode (god_rays_style.glsl:166-174)
```glsl
float getNoiseModulation(..., float scale, float speed, float intensity) {
    float angle = atan(dir.y, dir.x);
    float n = fract(sin(angle * scale + time * speed) * 43758.5453);
    return 1.0 - intensity + n * intensity;
}
```

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| Uniform | 0 | Breathing animation (0.9-1.1) | ✅ WORKING |
| Weighted | 1 | Angular bias with rotation | ✅ WORKING |
| Noise | 2 | Hash-based noise modulation | ✅ WORKING |

### Dependencies for Noise mode
- **noiseScale**: Panel has it, getter/setter exist
- **noiseSpeed**: Panel has it
- **noiseIntensity**: Panel has it

---

## 4. ARRANGEMENT MODE (RayArrangement)

### Panel
- Dropdown: Now using `RayArrangement.values()`
- Values shown: RADIAL, SPHERICAL, PARALLEL, CONVERGING, DIVERGING

### RenderConfig
- Field: `godRaysArrangementMode` (int)
- Getter: `getGodRaysArrangementMode()` → int

### UBO
- Slot 52.w (`GodRayStyleParams.arrangementMode`)
- GLSL uniform: `GodRayArrangementMode`

### Shader Code (god_rays_style.glsl:231-242)
```glsl
vec2 getArrangedLightUV(..., float arrangementMode) {
    if (arrangementMode < 0.5) {
        // Mode 0: Point - rays from center
        return baseLightUV;
    } else if (arrangementMode < 1.5) {
        // Mode 1: Ring - rays from surface point nearest to pixel
        vec2 toPixel = normalize(pixelUV - baseLightUV);
        return baseLightUV + toPixel * screenRadius;
    } else {
        // Mode 2: Sector - reserved for future
        return baseLightUV;
    }
}
```

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| RADIAL | 0 | Point source mode | ⚠️ NAME MISMATCH |
| SPHERICAL | 1 | Ring source mode | ⚠️ NAME MISMATCH |
| PARALLEL | 2 | Falls through to default | ❌ NOT CODED |
| CONVERGING | 3 | Falls through to default | ❌ NOT CODED |
| DIVERGING | 4 | Falls through to default | ❌ NOT CODED |

### CRITICAL ISSUE
Panel uses RayArrangement enum but shader only has Point/Ring/Sector concepts!
- Enum indices don't match shader mode expectations
- Modes 2-4 have no real implementation

### SOURCE DEFINITIONS (from RayArrangement.java)
- **RADIAL**: Rays emanate from center outward (2D star pattern)
- **SPHERICAL**: Rays in all 3D directions from center (hedgehog spines)
- **PARALLEL**: All rays pointing same direction (like sunlight through window)
- **CONVERGING**: Rays START at outer radius, all point TOWARD center
- **DIVERGING**: Rays START at inner radius, all point AWAY from center

### IMPLEMENTATION PLAN

**Key Insight**: CONVERGING/DIVERGING affect both **ray origin** and **direction**.
- CONVERGING: rays originate from outer edge, march inward
- DIVERGING: rays originate from center, march outward

**Step 1**: Update `getArrangedLightUV()` for source position

```glsl
vec2 getArrangedLightUV(vec2 baseLightUV, vec2 pixelUV, float screenRadius, float arrangementMode) {
    if (arrangementMode < 0.5) {
        // Mode 0: RADIAL - rays from center point
        return baseLightUV;
    } else if (arrangementMode < 1.5) {
        // Mode 1: SPHERICAL - rays from orb surface (ring source)
        vec2 toPixel = normalize(pixelUV - baseLightUV);
        return baseLightUV + toPixel * screenRadius;
    } else if (arrangementMode < 2.5) {
        // Mode 2: PARALLEL - light at infinity in one direction
        // Create parallel rays by placing "light" far away in one direction
        vec2 sunDir = vec2(0.0, 1.0); // Sun above (configurable)
        return pixelUV + sunDir * 10.0; // Far away = parallel rays
    } else if (arrangementMode < 3.5) {
        // Mode 3: CONVERGING - rays start at OUTER edge, aim toward center
        // Invert the pixel→light relationship
        vec2 toCenter = normalize(baseLightUV - pixelUV);
        float outerRadius = 0.5; // Screen edge distance
        return pixelUV + toCenter * outerRadius * -1.0; // Start from outside
    } else {
        // Mode 4: DIVERGING - rays start at center, spread outward
        // Same as RADIAL for source position
        return baseLightUV;
    }
}
```

**Step 2**: Update `getGodRayDirection()` to handle CONVERGING/DIVERGING

```glsl
vec2 getGodRayDirection(vec2 pixelUV, vec2 lightUV, float energyMode, float arrangementMode, ...) {
    vec2 toLight = lightUV - pixelUV;
    vec2 baseDir;
    
    // Arrangement mode overrides energy mode for direction
    if (arrangementMode > 2.5 && arrangementMode < 3.5) {
        // CONVERGING: always march toward center (light)
        baseDir = normalize(toLight);
    } else if (arrangementMode > 3.5) {
        // DIVERGING: always march away from center
        baseDir = normalize(-toLight);
    } else {
        // Use energy mode for direction
        if (energyMode > 1.5 && energyMode < 2.5) {
            baseDir = normalize(-toLight); // ABSORPTION
        } else {
            baseDir = normalize(toLight);  // All others
        }
    }
    
    return applyCurvature(baseDir, pixelUV, lightUV, curvatureMode, curvatureStrength, time);
}
```

**Step 3**: Panel already uses RayArrangement enum - no change needed

---

## 5. CURVATURE MODE (RayCurvature)

### Panel
- Dropdown: Now using `RayCurvature.values()`
- Values: NONE, VORTEX, SPIRAL_ARM, TANGENTIAL, LOGARITHMIC, PINWHEEL, ORBITAL

### RenderConfig
- Field: `godRaysCurvatureMode` (float)
- Field: `godRaysCurvatureStrength` (float)

### UBO
- Slot 55.x (`GodRayCurvatureParams.mode`)
- Slot 55.y (`GodRayCurvatureParams.strength`)
- GLSL uniforms: `GodRayCurvatureMode`, `GodRayCurvatureStrength`

### Shader Code (god_rays_style.glsl:39-69)
```glsl
vec2 applyCurvature(..., float curvatureMode, float curvatureStrength, float time) {
    if (curvatureMode < 0.5) {
        // Mode 0: Radial - no curvature
        return dir;
    }
    
    if (curvatureMode < 1.5) {
        // Mode 1: VORTEX
        curveAngle = dist * curvatureStrength * 6.28 + time * 0.5;
    } else if (curvatureMode < 2.5) {
        // Mode 2: SPIRAL_ARM
        curveAngle = log(max(dist, 0.01) + 1.0) * curvatureStrength * 4.0 + time * 0.3;
    } else if (curvatureMode < 3.5) {
        // Mode 3: TANGENTIAL
        curveAngle = 1.5708 * curvatureStrength + sin(time * 1.5) * 0.2;
    } else {
        // Mode 4: PINWHEEL
        curveAngle = angle * curvatureStrength * 0.5 + time * 1.0;
    }
    
    return normalize(rotateVec2(dir, curveAngle));
}
```

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| NONE | 0 | Returns dir unchanged | ✅ WORKING |
| VORTEX | 1 | Distance-based rotation + time | ✅ WORKING |
| SPIRAL_ARM | 2 | Logarithmic spiral + time | ✅ WORKING |
| TANGENTIAL | 3 | 90° + oscillation | ✅ WORKING |
| LOGARITHMIC | 4 | Falls into Mode 4 (PINWHEEL code) | ⚠️ WRONG CODE |
| PINWHEEL | 5 | Falls into else (same as 4) | ⚠️ INDEX MISMATCH |
| ORBITAL | 6 | Falls into else (same as 4) | ❌ NOT CODED |

### CRITICAL ISSUE
Shader handles modes 0-4, but enum has 7 values (0-6).
- Modes 4, 5, 6 all run the same "mode 4" code
- LOGARITHMIC (index 4) runs PINWHEEL formula

### IMPLEMENTATION PLAN

**Step 1**: Update shader to match RayCurvature enum indices

```glsl
// god_rays_style.glsl - applyCurvature()
vec2 applyCurvature(vec2 dir, vec2 pixelUV, vec2 lightUV, float curvatureMode, float curvatureStrength, float time) {
    if (curvatureMode < 0.5) {
        // Mode 0: NONE - no curvature
        return dir;
    }
    
    vec2 toLight = lightUV - pixelUV;
    float dist = length(toLight);
    float angle = atan(toLight.y, toLight.x);
    
    float curveAngle = 0.0;
    
    if (curvatureMode < 1.5) {
        // Mode 1: VORTEX - whirlpool/accretion disk
        curveAngle = dist * curvatureStrength * 6.28 + time * 0.5;
    } else if (curvatureMode < 2.5) {
        // Mode 2: SPIRAL_ARM - galaxy arm pattern
        curveAngle = log(max(dist, 0.01) + 1.0) * curvatureStrength * 4.0 + time * 0.3;
    } else if (curvatureMode < 3.5) {
        // Mode 3: TANGENTIAL - perpendicular to radial
        curveAngle = 1.5708 * curvatureStrength + sin(time * 1.5) * 0.2;
    } else if (curvatureMode < 4.5) {
        // Mode 4: LOGARITHMIC - nautilus shell spiral (golden ratio)
        float goldenAngle = 2.39996; // ~137.5 degrees in radians
        curveAngle = log(max(dist, 0.01) + 1.0) * goldenAngle * curvatureStrength + time * 0.2;
    } else if (curvatureMode < 5.5) {
        // Mode 5: PINWHEEL - windmill blade pattern
        curveAngle = angle * curvatureStrength * 0.5 + time * 1.0;
    } else {
        // Mode 6: ORBITAL - circular paths around center
        // Rays curve to follow circular orbits
        vec2 tangent = vec2(-toLight.y, toLight.x); // perpendicular
        float orbitBlend = curvatureStrength;
        vec2 orbitDir = normalize(mix(dir, normalize(tangent), orbitBlend));
        return orbitDir;
    }
    
    return normalize(rotateVec2(dir, curveAngle));
}
```

**Step 2**: Panel already uses RayCurvature enum - no change needed

---

## 6. FLICKER MODE (EnergyFlicker)

### Panel
- Dropdown: Now using `EnergyFlicker.values()`
- Values: NONE, SCINTILLATION, STROBE, FADE_PULSE, FLICKER, LIGHTNING, HEARTBEAT

### RenderConfig
- Fields: `godRaysFlickerMode`, `godRaysFlickerIntensity`, `godRaysFlickerFrequency`
- Getters/setters exist

### UBO
- Slot 56 (`GodRayFlickerParams`)
- GLSL uniforms: `GodRayFlickerMode`, `GodRayFlickerIntensity`, `GodRayFlickerFrequency`

### Shader Code
**NONE** - No function like `getFlickerModulation()` exists!

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| NONE | 0 | - | ❌ NOT CODED |
| SCINTILLATION | 1 | - | ❌ NOT CODED |
| STROBE | 2 | - | ❌ NOT CODED |
| FADE_PULSE | 3 | - | ❌ NOT CODED |
| FLICKER | 4 | - | ❌ NOT CODED |
| LIGHTNING | 5 | - | ❌ NOT CODED |
| HEARTBEAT | 6 | - | ❌ NOT CODED |

### IMPLEMENTATION PLAN

**Step 1**: Add `getFlickerModulation()` function to god_rays_style.glsl

```glsl
/**
 * Get flicker modulation for ray intensity.
 * 
 * @param pixelUV Current pixel UV (for per-ray variation)
 * @param time Animation time
 * @param flickerMode 0=none, 1=scintillation, 2=strobe, 3=fade_pulse, 4=flicker, 5=lightning, 6=heartbeat
 * @param intensity Flicker strength (0-1)
 * @param frequency Flicker speed multiplier
 * @return Modulation factor (0-1, multiply with illumination)
 */
float getFlickerModulation(vec2 pixelUV, float time, float flickerMode, float intensity, float frequency) {
    if (flickerMode < 0.5) {
        // Mode 0: NONE - no flicker
        return 1.0;
    }
    
    // Per-ray random seed based on angle from center
    float angle = atan(pixelUV.y - 0.5, pixelUV.x - 0.5);
    float rayId = floor(angle * 16.0 / 6.28318); // 16 "rays"
    float rayRand = fract(sin(rayId * 12.9898) * 43758.5453);
    
    float t = time * frequency;
    float flick = 1.0;
    
    if (flickerMode < 1.5) {
        // Mode 1: SCINTILLATION - star twinkling (per-ray random)
        float noise = fract(sin(rayRand * 1000.0 + t * 3.0) * 43758.5453);
        flick = 1.0 - intensity * noise;
        
    } else if (flickerMode < 2.5) {
        // Mode 2: STROBE - rhythmic on/off
        float strobe = step(0.5, fract(t));
        flick = mix(1.0, strobe, intensity);
        
    } else if (flickerMode < 3.5) {
        // Mode 3: FADE_PULSE - smooth breathing
        float breath = sin(t * 3.14159) * 0.5 + 0.5;
        flick = 1.0 - intensity * 0.5 + breath * intensity * 0.5;
        
    } else if (flickerMode < 4.5) {
        // Mode 4: FLICKER - candlelight unstable
        float noise1 = fract(sin(t * 10.0 + rayRand) * 43758.5453);
        float noise2 = fract(sin(t * 23.0 + rayRand * 2.0) * 43758.5453);
        flick = 1.0 - intensity * (noise1 * 0.3 + noise2 * 0.2);
        
    } else if (flickerMode < 5.5) {
        // Mode 5: LIGHTNING - flash then fade
        float cycle = fract(t * 0.5); // Slow cycle
        float flash = exp(-cycle * 8.0); // Exponential decay
        flick = mix(0.3, 1.0, flash * intensity) + (1.0 - intensity);
        
    } else {
        // Mode 6: HEARTBEAT - double-pulse rhythm
        float beat = fract(t);
        float pulse1 = exp(-pow((beat - 0.2) * 10.0, 2.0));
        float pulse2 = exp(-pow((beat - 0.4) * 10.0, 2.0));
        flick = 1.0 - intensity * 0.5 + (pulse1 + pulse2 * 0.7) * intensity * 0.5;
    }
    
    return clamp(flick, 0.0, 1.0);
}
```

**Step 2**: Call from god_rays.glsl or god_rays_accum.fsh

```glsl
// After computing illumination, apply flicker
float flickerMod = getFlickerModulation(texCoord, time, 
    GodRayFlickerMode, GodRayFlickerIntensity, GodRayFlickerFrequency);
illumination *= flickerMod;
```

---

## 7. TRAVEL MODE (EnergyTravel)

### Panel
- Dropdown: Using subset of `EnergyTravel` (NONE, CHASE, SCROLL, COMET, SPARK, PULSE_WAVE)

### RenderConfig
- Fields: `godRaysTravelMode`, `godRaysTravelSpeed`
- Getters/setters exist

### UBO
- Slot 57 (`GodRayTravelParams`)
- GLSL uniforms: `GodRayTravelMode`, `GodRayTravelSpeed`

### Shader Code
**NONE** - No function like `getTravelModulation()` exists!

### Mode Status

| Mode | Index | Shader Logic | Status |
|------|-------|--------------|--------|
| NONE | 0 | - | ❌ NOT CODED |
| CHASE | 1 | - | ❌ NOT CODED |
| SCROLL | 2 | - | ❌ NOT CODED |
| COMET | 3 | - | ❌ NOT CODED |
| SPARK | 4 | - | ❌ NOT CODED |
| PULSE_WAVE | 5 | - | ❌ NOT CODED |

### IMPLEMENTATION PLAN

**Step 1**: Add `getTravelModulation()` function to god_rays_style.glsl

```glsl
/**
 * Get travel modulation for ray march position.
 * Creates moving particles/gradients along the ray.
 * 
 * @param t Normalized position along ray (0=light, 1=pixel)
 * @param time Animation time
 * @param travelMode 0=none, 1=chase, 2=scroll, 3=comet, 4=spark, 5=pulse_wave
 * @param speed Animation speed multiplier
 * @param rayRand Per-ray random value for variation
 * @return Modulation factor (0-1, multiply with sample weight)
 */
float getTravelModulation(float t, float time, float travelMode, float speed, float rayRand) {
    if (travelMode < 0.5) {
        // Mode 0: NONE - no travel animation
        return 1.0;
    }
    
    float animTime = time * speed;
    float travel = 1.0;
    
    if (travelMode < 1.5) {
        // Mode 1: CHASE - discrete dots traveling outward
        float numDots = 3.0;
        float dotWidth = 0.1;
        float phase = fract(animTime + rayRand * 0.5);
        
        // Multiple dots with spacing
        float closest = 1.0;
        for (float i = 0.0; i < numDots; i++) {
            float dotPos = fract(phase + i / numDots);
            float dist = abs(t - dotPos);
            closest = min(closest, dist);
        }
        travel = 1.0 - smoothstep(0.0, dotWidth, closest);
        
    } else if (travelMode < 2.5) {
        // Mode 2: SCROLL - continuous gradient scrolling
        float scrollPos = fract(t - animTime);
        travel = scrollPos;
        
    } else if (travelMode < 3.5) {
        // Mode 3: COMET - bright head with fading tail
        float headPos = fract(animTime + rayRand * 0.3);
        float dist = t - headPos;
        if (dist < 0.0) dist += 1.0; // wrap
        float tailLength = 0.4;
        travel = exp(-dist / tailLength);
        
    } else if (travelMode < 4.5) {
        // Mode 4: SPARK - random sparks shooting out
        float sparkPhase = fract(rayRand * 10.0 + animTime * 2.0);
        float sparkPos = sparkPhase;
        float dist = abs(t - sparkPos);
        // Only show spark occasionally
        float showSpark = step(0.7, fract(rayRand * 100.0));
        travel = mix(0.2, exp(-dist * 10.0), showSpark);
        
    } else {
        // Mode 5: PULSE_WAVE - brightness pulse traveling outward
        float wavePos = fract(animTime);
        float waveWidth = 0.15;
        float dist = abs(t - wavePos);
        if (dist > 0.5) dist = 1.0 - dist; // wrap
        travel = 0.3 + 0.7 * exp(-dist / waveWidth);
    }
    
    return clamp(travel, 0.0, 1.0);
}
```

**Step 2**: Call during ray march in god_rays.glsl

```glsl
// Inside the ray march loop, modulate sample weight
float rayRand = fract(sin(dot(pixelUV, vec2(12.9898, 78.233))) * 43758.5453);
for (int i = 0; i < steps; i++) {
    float t = float(i) / float(steps); // 0 to 1 along ray
    
    // Compute sample...
    float sample = texture(scene, samplePos).r;
    
    // Apply travel modulation
    float travelMod = getTravelModulation(t, time, 
        GodRayTravelMode, GodRayTravelSpeed, rayRand);
    sample *= travelMod;
    
    accumulator += sample * weight * decay;
    // ...
}
```

---

## 8. COLOR2 (Secondary Color for Gradient)

### Panel
- R/G/B sliders: JUST ADDED
- Only shown when colorMode >= 1

### RenderConfig
- Fields: `godRaysColor2R`, `godRaysColor2G`, `godRaysColor2B`
- Combined setter: `setGodRaysColor2(r, g, b)`

### UBO
- Slot 53 (`GodRayColor2.r/g/b`)
- GLSL uniforms: `GodRayColor2R/G/B`

### Shader Usage
- Used in `getGodRayColor()` for Gradient mode (mode 1)
- Passed as `color2` parameter

### Status
✅ FULLY WIRED - Panel → RenderConfig → UBO → Shader

---

## ACTION ITEMS

### High Priority (Broken/Wrong)
1. **Fix Energy Mode index mapping** - Panel enum indices don't match shader
2. **Fix Arrangement Mode index mapping** - Panel enum doesn't match shader concepts
3. **Fix Curvature Mode index mapping** - Panel has 7 modes, shader handles 5

### Medium Priority (Missing)
4. **Implement Flicker shader logic** - All 7 modes
5. **Implement Travel shader logic** - All 6 modes
6. **Add missing Curvature modes** - LOGARITHMIC, ORBITAL

### Low Priority (Polish)
7. **Add missing Arrangement modes** - PARALLEL, CONVERGING, DIVERGING
8. **Add missing Energy modes** - REFLECTION, TRANSMISSION, SCATTERING, RESONANCE
9. **Verify primary RayColor control** - Is it exposed in panel?

---

*Analysis completed: 2026-01-12*
