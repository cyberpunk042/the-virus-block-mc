# V8 Electric Aura - Complete Parameter Sweep

## Overview
This document maps ALL parameters needed for V8 Electric Aura, organized by layer/component.
It includes V7 source parameters, nimitz parameters, and new V8-specific parameters.

---

# LAYER 1: CORE (4D Plasma)

## Parameters for 4D Plasma Core

| Parameter | V7 Name | V7 Default | V8 Purpose | V8 Range | V8 Default | UBO Field |
|-----------|---------|------------|------------|----------|------------|-----------|
| Core Size | coreScale | 1.0 | Size of the core relative to sphere | 0.01-10 | 0.1 | `coreSize` |
| Core Octaves | detail | 3 | FBM detail levels for 4D noise | 1-6 | 4 | `noiseOctaves` |
| Core Zoom | zoom | 1.0 | Scale of noise pattern | 0.1-10 | 1.0 | `noiseBaseScale` |
| Core Speed Hi | speedHi | 2.0 | High frequency noise animation | 0-50 | 2.0 | `speedHigh` |
| Core Speed Lo | speedLow | 2.0 | Low frequency noise animation | 0-50 | 2.0 | `speedLow` |
| Noise Seed | seed | 1.0 | Pattern variation | -10 to 10 | 1.0 | `noiseSeed` |

## Core Colors

| Color Role | V7 Name | V7 Default RGB | V8 Purpose | UBO Fields |
|------------|---------|----------------|------------|------------|
| Body Color | bodyColor | (1, 1, 0) | Core plasma base | `primaryR/G/B` |
| Light Color | lightColor | (1, 1, 1) | Core highlights | `highlightR/G/B` |
| Base Color | baseColor | (1, 0, 0) | Core accent 1 | `secondaryR/G/B` |
| Dark Color | darkColor | (1, 0, 1) | Core accent 2 | `tertiaryR/G/B` |

## Core Shader Variables
```glsl
// Core 4D plasma function inputs
vec3 surface;      // Point on sphere surface (from intersection)
float time;        // Pre-multiplied animation time
int octaves;       // noiseOctaves
float zoom;        // noiseBaseScale
float speedHi;     // speedHigh
float speedLow;    // speedLow
vec3 seed;         // Derived from noiseSeed
```

---

# LAYER 2: PLASMA CORONA ZONE

## Parameters for 2D Plasma Background

| Parameter | V7 Equiv | V7 Default | V8 Purpose | V8 Range | V8 Default | Notes |
|-----------|----------|------------|------------|----------|------------|-------|
| Plasma Scale | zoom | 1.0 | Size of plasma noise pattern | 0.1-10 | 4.0 | Shared with core |
| Plasma Octaves | detail | 3 | FBM detail for turbulent noise | 1-6 | 5 | Shared with core |
| Domain Warp | NEW | - | Strength of domain warping | 0-0.5 | 0.2 | nimitz default |
| Plasma Speed | speedRay | 5.0 | Animation speed of plasma swirl | 0-50 | 1.0 | Lower = slower swirl |

## NEW V8 Parameters for Corona

| Parameter | Purpose | Range | Default | Notes |
|-----------|---------|-------|---------|-------|
| Corona Warp Amount | Domain displacement strength | 0-0.5 | 0.2 | Controls swirl intensity |
| Corona Warp Speed X | Time offset for X warp | -5 to 5 | -1.6 | nimitz: -1.6 |
| Corona Warp Speed Y | Time offset for Y warp | -5 to 5 | 1.7 | nimitz: +1.7 |
| Corona Rotate | Rotation speed of final sample | 0-1 | 0.2 | nimitz: 0.2 |

## Corona Shader Variables
```glsl
// Inputs from pr (projection point)
vec3 prn = normalize(viewRotation * pr);
float theta = atan(prn.z, prn.x);      // Azimuthal angle
float phi = asin(prn.y);                // Polar angle
vec2 plasmaUV = vec2(theta, phi * 2.0) * plasmaScale;

// Plasma function
float plasma = electricDualFBM(plasmaUV, time * plasmaSpeed, octaves, warpAmount);
```

---

# LAYER 3: PULSATING RINGS

## Parameters for Ring Pattern

| Parameter | V7 Name | V7 Default | V8 Purpose | V8 Range | V8 Default | UBO Field |
|-----------|---------|------------|------------|----------|------------|-----------|
| Ring Frequency | rayString | 1.0 | Number of ring cycles | 0.1-10 | 1.0 | `raySharpness` |
| Ring Reach | rayReach | 1.0 | How far rings extend | 1-150 | 2.0 | `fadeScale` |
| Ring Pulse Speed | speedRing | 2.0 | Pulsation speed | 0-50 | 10.0 | `speedRing` |

## NEW V8 Parameters for Rings

| Parameter | Purpose | Range | Default | Notes |
|-----------|---------|-------|---------|-------|
| Ring Freq Multiplier | Internal frequency scaling | 1-10 | 4.0 | nimitz uses ×4 |
| Ring Modulation Power | pow() exponent for ring mod | 0.1-2 | 0.9 | nimitz: 0.9 |
| Ring Center Value | Target value for ring bright spot | 0-0.5 | 0.1 | nimitz: 0.1 |
| Pulse Cycle | Period of expansion cycle | PI/2 to 2*PI | PI | nimitz: PI |

## Ring Shader Variables
```glsl
// Distance from sphere surface
float surfaceDist = max(0.0, length(pr) - radius);

// Pulsating expansion
float pulseDist = surfaceDist / exp(mod(time * pulseSpeed, pulseCycle));

// Logarithmic ring pattern
float r = log(sqrt(pulseDist) + 0.001);
float rings = abs(mod(r * ringFreq * ringFreqMult, TAU) - PI) * 3.0 + 0.2;

// Ring modulation
float ringMod = pow(abs(ringCenterValue - rings), ringModPower);
```

---

# LAYER 4: DISTANCE FADE (CORONA BOUNDARY)

## Parameters for Edge Fade

| Parameter | V7 Name | V7 Default | V8 Purpose | V8 Range | V8 Default | UBO Field |
|-----------|---------|------------|------------|----------|------------|-----------|
| Max Reach | rayReach | 1.0 | Maximum corona extent | 1-150 | 2.0 | `fadeScale` |
| Fade Curve | rayFade | 1.0 | Fade transition curve | 0.1-100 | 1.0 | `insideFalloffPower` |

## NEW V8 Parameters for Fade

| Parameter | Purpose | Range | Default | Notes |
|-----------|---------|-------|---------|-------|
| Fade Width Ratio | Width of fade zone vs max reach | 0.1-0.9 | 0.3 | 0.3 = fade starts at 70% |
| Fade Power | Smoothstep power curve | 0.5-3 | 1.0 | 1.0 = linear smoothstep |

## Fade Shader Variables
```glsl
float maxReach = sphereRadius * rayReach * 2.0;
float fadeWidth = maxReach * fadeWidthRatio;
float fadeStart = maxReach - fadeWidth;

float coronaFade = (surfaceDist > maxReach) ? 0.0 :
                   (surfaceDist < fadeStart) ? 1.0 :
                   1.0 - smoothstep(fadeStart, maxReach, surfaceDist);
```

---

# LAYER 5: COLOR COMPOSITING

## Parameters for Division-Based Coloring

| Parameter | V7 Name | V7 Default | V8 Purpose | V8 Range | V8 Default | UBO Field |
|-----------|---------|------------|------------|----------|------------|-----------|
| Ray Color R | rayR | 1.0 | Corona/ring base color R | 0-1 | 0.2 | `rayR` |
| Ray Color G | rayG | 0.6 | Corona/ring base color G | 0-1 | 0.1 | `rayG` |
| Ray Color B | rayB | 0.1 | Corona/ring base color B | 0-1 | 0.4 | `rayB` |

## NEW V8 Parameters for Color

| Parameter | Purpose | Range | Default | Notes |
|-----------|---------|-------|---------|-------|
| Min RZ (Div Floor) | Prevent division by zero | 0.001-0.1 | 0.01 | Safety clamp |
| Max Color Value | Prevent oversaturation | 1-20 | 10.0 | Clamp after division |
| Gamma Power | Final gamma adjustment | 0.9-1.1 | 0.99 | nimitz: 0.99 |

## Color Shader Variables
```glsl
// Division-based coloring
vec3 plasmaColor = vec3(rayR, rayG, rayB);
float rz = max(plasma * ringMod, minRZ);
vec3 col = plasmaColor / rz;
col = min(col, vec3(maxColorValue));
col = pow(abs(col), vec3(gammaPower));
```

---

# LAYER 6: OCCLUSION

## Parameters (Keep from V7)

| Parameter | V7 Default | Purpose | UBO Field |
|-----------|------------|---------|-----------|
| Min Bleed Dist | 5.0 | Min distance before occlusion | Hardcoded |
| Bleed Range | 400.0 | Distance over which effect fades | Hardcoded |
| Max Bleed | 0.5 | Maximum visibility when occluded | Hardcoded |

## Optional V8 Overrides

| Parameter | Purpose | Range | Default | Notes |
|-----------|---------|-------|---------|-------|
| Occlusion Bleed | Enable/disable bleed-through | bool | true | Keep V7 behavior |
| Bleed Distance | Tune bleed range | 100-1000 | 400 | Could expose to UI |

---

# LAYER 7: GLOBAL / SYSTEM

## Always-Present Parameters

| Parameter | Purpose | V8 Range | V8 Default | UBO Field |
|-----------|---------|----------|------------|-----------|
| Enabled | Toggle effect on/off | bool | true | `enabled` |
| Intensity | Overall intensity multiplier | 0-5 | 1.0 | `intensity` |
| Animation Speed | Global time multiplier | 0.01-10 | 1.0 | `animationSpeed` |
| Preview Radius | Base sphere radius | 0.5-500 | 5.0 | `previewRadius` |

## System Inputs (From Camera/Scene)

| Input | Source | Purpose |
|-------|--------|---------|
| rayOrigin | Camera position | Ray start |
| rayDir | Pixel direction | Ray direction |
| forward | Camera forward | Z-depth comparison |
| maxDist | Depth buffer | Scene occlusion |
| sphereCenter | Config | Orb world position |
| sphereRadius | Config | Orb base radius |
| time | Game time × AnimSpeed | Animation driver |

---

# PARAMETER MAPPING: UI → UBO → Shader

## Complete V8 Schema Proposal

```java
SCHEMAS.put("ENERGY_ORB_V8", EffectSchema.builder("Electric Aura", EffectType.ENERGY_ORB, 8)
    .group("General", List.of(
        slider(FV + "intensity", "Intensity", 0f, 5f, 1f, "General"),
        slider(FV + "animationSpeed", "Animation Speed", 0.01f, 10f, 1f, "General"),
        slider(FV + "previewRadius", "Preview Radius", 0.5f, 500f, 5f, "General")
    ))
    .group("Core", List.of(
        slider(FV + "coreSize", "Core Scale", 0.01f, 10f, 0.1f, "Core"),
        intSlider(FV + "noiseOctaves", "Core Detail", 1, 6, 4, "Core"),
        slider(FV + "noiseBaseScale", "Core Zoom", 0.1f, 10f, 1f, "Core")
    ))
    .group("Plasma", List.of(
        slider(FV + "coronaWarp", "Warp Intensity", 0f, 0.5f, 0.2f, "Plasma"),
        slider(FV + "speedRay", "Swirl Speed", 0f, 10f, 1f, "Plasma")
    ))
    .group("Rings", List.of(
        slider(FV + "raySharpness", "Ring Frequency", 0.1f, 10f, 1f, "Rings"),
        slider(FV + "speedRing", "Pulse Speed", 0f, 50f, 10f, "Rings"),
        slider(FV + "fadeScale", "Ring Reach", 1f, 150f, 2f, "Rings")
    ))
    .group("Animation", List.of(
        slider(FV + "speedHigh", "Core Speed Hi", 0f, 50f, 2f, "Animation"),
        slider(FV + "speedLow", "Core Speed Lo", 0f, 50f, 2f, "Animation")
    ))
    .group("Fade", List.of(
        slider(FV + "insideFalloffPower", "Fade Curve", 0.1f, 10f, 1f, "Fade")
    ))
    .group("Colors", List.of(
        // Plasma color (division base)
        normalized(FV + "rayR", "Plasma R", 0.2f, "Colors"),
        normalized(FV + "rayG", "Plasma G", 0.1f, "Colors"),
        normalized(FV + "rayB", "Plasma B", 0.4f, "Colors"),
        // Core color
        normalized(FV + "primaryR", "Core R", 0.4f, "Colors"),
        normalized(FV + "primaryG", "Core G", 0.2f, "Colors"),
        normalized(FV + "primaryB", "Core B", 0.8f, "Colors"),
        // Highlight
        normalized(FV + "highlightR", "Highlight R", 1f, "Colors"),
        normalized(FV + "highlightG", "Highlight G", 0.8f, "Colors"),
        normalized(FV + "highlightB", "Highlight B", 1f, "Colors")
    ))
    .group("Noise", List.of(
        slider(FV + "noiseSeed", "Seed", -10f, 10f, 1f, "Noise")
    ))
    .build());
```

---

# SUMMARY: V8 vs V7 Parameter Usage

## V7 Parameters KEPT in V8

| Parameter | V7 Usage | V8 Usage |
|-----------|----------|----------|
| intensity | Global multiplier | Same |
| animationSpeed | Time scaling | Same |
| previewRadius | Base radius | Same |
| coreSize | Core scale | Same (smaller default) |
| noiseOctaves | Body detail | Core + corona detail |
| noiseBaseScale | Body zoom | Core zoom |
| speedHigh | Body noise Hi | Core 4D noise Hi |
| speedLow | Body noise Lo | Core 4D noise Lo |
| speedRay | Ray animation | Plasma swirl |
| speedRing | Ring rotation | Ring pulsation |
| raySharpness | Ray thickness | Ring frequency |
| fadeScale | Ray reach | Corona reach |
| noiseSeed | Pattern seed | Same |
| rayR/G/B | Ray color | Plasma color |
| primaryR/G/B | Body color | Core color |
| highlightR/G/B | Light color | Same |

## V7 Parameters NOT USED in V8

| Parameter | Reason |
|-----------|--------|
| rayPower | V8 uses division, not power curve |
| coronaPower | Different ring math |
| coronaMultiplier | Different glow approach |
| coreFalloff | Division replaces falloff |
| secondaryR/G/B | Simplified colors |
| tertiaryR/G/B | Simplified colors |
| coronaWidth (dark ring) | No dark ring in V8 |
| edgeSharpness (dark ring) | No dark ring in V8 |
| ringPower (dark ring) | No dark ring in V8 |
| insideFalloffPower | Could reuse for fade curve |

## NEW V8 Parameters (Not in V7)

| Parameter | Purpose | Needs New UBO Field? |
|-----------|---------|---------------------|
| coronaWarp | Domain warp strength | Yes, or repurpose |
| Ring internal constants | Hardcode for now | No |
| Division safety values | Hardcode | No |
