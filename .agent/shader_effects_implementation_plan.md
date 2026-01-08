# Shader Effects Implementation Plan

## Current State (Baseline)

### What We Have Working
- ✅ **Fresnel Shader Pipeline** - Custom shader with UBO binding via Mixin
- ✅ **Horizon Effect** - Rim lighting with power, intensity, color controls
- ✅ **Corona Effect** - Secondary rim glow with offset, width, falloff controls
- ✅ **Combined UBO** - 96 bytes (6 vec4s) for both effects
- ✅ **GUI Controls** - Sliders for all parameters
- ✅ **Pattern Auto-Switch** - Switches to standard_quad when effects enabled

### Architecture
```
FieldEditState → ShapeAdapter → SphereShape → toHorizonEffect()/toCoronaEffect()
                                    ↓
                        CustomUniformBinder.setParams()
                                    ↓
                     Mixin injects into bindDefaultUniforms()
                                    ↓
                         FresnelParams UBO → Shader
```

---

## Phase 1: Time-Based Animation Foundation

### Goal
Add pulsing, flickering effects to existing rim/corona glow.

### New Parameters
| Parameter | Type | Range | Default | Description |
|-----------|------|-------|---------|-------------|
| `animationEnabled` | bool | - | false | Master toggle for all animation |
| `pulseSpeed` | float | 0.1-10 | 1.0 | Speed of pulsing (cycles/second) |
| `pulseAmount` | float | 0-1 | 0.3 | How much intensity varies |
| `flickerSpeed` | float | 0-20 | 0 | Random flicker rate (0=off) |
| `flickerAmount` | float | 0-1 | 0 | Intensity of flicker |

### Implementation Steps

#### Step 1.1: Access Minecraft's Time Uniform
Minecraft provides a `Globals` UBO with `GameTime` that ticks continuously.
```glsl
// In fresnel_entity.fsh - import dynamictransforms which has GameTime
// GameTime is a float that counts up over time
```

#### Step 1.2: Update UBO Layout
Extend FresnelParams to include animation:
```
FresnelParams (128 bytes = 8 vec4s):
  vec4 RimColorAndPower          // existing
  vec4 RimIntensityAndPad        // existing  
  vec4 CoronaColorAndPower       // existing
  vec4 CoronaIntensityFalloff    // existing
  vec4 CoronaOffsetWidthPad      // existing
  vec4 AnimationParams1          // NEW: x=pulseSpeed, y=pulseAmount, z=flickerSpeed, w=flickerAmount
  vec4 AnimationParams2          // NEW: reserved for future
  vec4 Reserved                  // padding to power of 2
```

#### Step 1.3: Shader Animation Logic
```glsl
// Pulsing - smooth sinusoidal
float pulse = sin(GameTime * pulseSpeed * 6.28318) * 0.5 + 0.5;  // 0 to 1
float pulsedIntensity = mix(1.0, pulse, pulseAmount);

// Flickering - high frequency noise
float flicker = fract(sin(GameTime * flickerSpeed * 100.0) * 43758.5453);
float flickeredIntensity = mix(1.0, flicker, flickerAmount);

// Apply to rim intensity
rimIntensity *= pulsedIntensity * flickeredIntensity;
```

#### Step 1.4: GUI Controls
Add to ShapeWidgetSpec SPHERE_SPECS:
- Checkbox: "Animation" → `sphere.animationEnabled`
- Slider: "Pulse" → `sphere.pulseSpeed`
- Slider: "Pulse Amt" → `sphere.pulseAmount`
- Slider: "Flicker" → `sphere.flickerSpeed`
- Slider: "Flicker Amt" → `sphere.flickerAmount`

### Deliverables
- [ ] Updated UBO size and layout in CustomUniformBinder
- [ ] Updated fresnel_entity.fsh with animation logic
- [ ] New animation fields in SphereShape record
- [ ] GUI controls in ShapeWidgetSpec

---

## Phase 2: Procedural Noise Foundation

### Goal
Add animated procedural patterns on the sphere surface.

### Key Concepts
- **Simplex Noise**: Smooth, organic-looking pseudorandom patterns
- **3D Noise for Spheres**: Use world position to avoid UV seams
- **Fractal Brownian Motion (fBm)**: Layer multiple noise octaves for detail

### New Parameters
| Parameter | Type | Range | Default | Description |
|-----------|------|-------|---------|-------------|
| `noiseEnabled` | bool | - | false | Enable noise patterns |
| `noiseScale` | float | 0.5-10 | 2.0 | Size of noise features |
| `noiseSpeed` | float | 0-5 | 1.0 | Animation speed |
| `noiseOctaves` | int | 1-4 | 2 | Detail layers |
| `noiseAmplitude` | float | 0-2 | 0.5 | Strength of noise effect |

### Implementation Steps

#### Step 2.1: Add Simplex Noise to Shader
```glsl
// Classic 3D Simplex noise implementation
// Source: Stefan Gustavson's implementation (public domain)
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise(vec3 v) {
    // ... full implementation
    // Returns value in range [-1, 1]
}

// fBm for layered detail
float fbm(vec3 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < octaves; i++) {
        value += amplitude * snoise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    return value;
}
```

#### Step 2.2: Apply Noise to Surface
```glsl
// In fragment shader - vWorldPos passed from vertex shader
vec3 noisePos = vWorldPos * noiseScale + vec3(0, GameTime * noiseSpeed, 0);
float noise = fbm(noisePos, noiseOctaves) * noiseAmplitude;

// Modulate rim intensity or color based on noise
rimIntensity *= 1.0 + noise;
// OR add noise as color variation
color.rgb += noise * coronaColor;
```

### Deliverables
- [ ] Simplex noise functions in shader (or include file)
- [ ] vWorldPos passed from vertex shader
- [ ] Noise parameters in UBO
- [ ] GUI controls for noise

---

## Phase 3: Effect Mode Presets

### Goal
Create named presets that combine parameters for specific looks.

### Effect Modes Enum
```java
public enum SphereEffectMode {
    NONE,           // No shader effects
    RIM_LIGHT,      // Basic Fresnel rim lighting
    ENERGY,         // Rasengan-style swirling energy
    FIRE,           // Animated fire/plasma
    ELECTRIC,       // Crackling electricity  
    SPIRIT,         // Spirit bomb / energy gathering
    HOLOGRAM,       // Sci-fi hologram with scanlines
    CUSTOM          // User-defined parameters
}
```

### Preset Configurations

#### ENERGY (Rasengan)
- Horizon: ON, power=3, intensity=1.5, color=cyan
- Corona: ON, power=2, intensity=2, color=light_blue, offset=0.3, width=1.5
- Animation: pulse=2.0, pulseAmt=0.3
- Noise: ON, scale=3, speed=2, octaves=2, amplitude=0.4
- Special: UV swirl effect (Phase 4)

#### FIRE
- Horizon: ON, power=2, intensity=2, color=orange
- Corona: ON, power=1.5, intensity=1.5, color=yellow, offset=0.4, width=2
- Animation: flicker=15, flickerAmt=0.3
- Noise: ON, scale=2, speed=3, octaves=3, amplitude=0.6
- Special: Color gradient mapping (Phase 5)

#### ELECTRIC
- Horizon: ON, power=5, intensity=3, color=white
- Corona: ON, power=4, intensity=1, color=electric_blue
- Animation: flicker=20, flickerAmt=0.8
- Noise: ON, scale=5, speed=5, octaves=1, amplitude=0.8

#### SPIRIT
- Horizon: ON, power=2, intensity=1, color=blue
- Corona: ON, power=1, intensity=2, color=light_blue, offset=0.5, width=3
- Animation: pulse=0.5, pulseAmt=0.5
- Noise: ON, scale=1.5, speed=-1 (inward), octaves=2

### Implementation
- Add dropdown to GUI for SphereEffectMode
- Apply preset values when mode changes
- Lock/unlock individual controls based on mode (CUSTOM = all unlocked)

---

## Phase 4: Advanced UV Effects (Swirl/Flow)

### Goal
Add directional movement and swirling patterns.

### New Parameters
| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `swirlEnabled` | bool | - | Enable swirl effect |
| `swirlStrength` | float | 0-10 | Rotation intensity |
| `swirlSpeed` | float | -5 to 5 | Rotation speed (negative=inward) |
| `flowDirection` | vec3 | - | Direction of pattern flow |
| `flowSpeed` | float | 0-5 | Speed of flow |

### Shader Implementation
```glsl
// Convert fragment position to spherical coordinates
vec3 norm = normalize(vWorldPos);
float theta = atan(norm.z, norm.x);  // azimuth
float phi = acos(norm.y);            // inclination

// Apply swirl - rotate theta based on phi and time
float swirlOffset = sin(phi * 3.14159) * swirlStrength;
float swirledTheta = theta + swirlOffset * sin(GameTime * swirlSpeed);

// Use swirled coordinates for noise sampling
vec3 swirledPos = vec3(
    sin(phi) * cos(swirledTheta),
    cos(phi),
    sin(phi) * sin(swirledTheta)
);
float swirlNoise = snoise(swirledPos * noiseScale + vec3(0, GameTime * noiseSpeed, 0));
```

---

## Phase 5: Color Mapping & Gradients

### Goal
Map noise/intensity values to color gradients for fire, plasma, etc.

### New Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `colorMapEnabled` | bool | Enable color mapping |
| `colorMapType` | enum | FIRE, PLASMA, RAINBOW, CUSTOM |
| `colorMapColors` | vec3[4] | Gradient control points |

### Built-in Color Maps
```glsl
vec3 fireGradient(float t) {
    // t=0: dark red, t=0.3: red, t=0.6: orange, t=0.8: yellow, t=1: white
    vec3 c1 = vec3(0.1, 0.0, 0.0);  // dark
    vec3 c2 = vec3(0.8, 0.1, 0.0);  // red
    vec3 c3 = vec3(1.0, 0.5, 0.0);  // orange
    vec3 c4 = vec3(1.0, 1.0, 0.5);  // yellow-white
    
    if (t < 0.33) return mix(c1, c2, t * 3.0);
    if (t < 0.66) return mix(c2, c3, (t - 0.33) * 3.0);
    return mix(c3, c4, (t - 0.66) * 3.0);
}

vec3 plasmaGradient(float t) {
    return vec3(
        sin(t * 3.14159 + 0.0) * 0.5 + 0.5,
        sin(t * 3.14159 + 2.09) * 0.5 + 0.5,
        sin(t * 3.14159 + 4.19) * 0.5 + 0.5
    );
}
```

---

## Phase 6: Vertex Displacement (Optional Advanced)

### Goal
Make the sphere surface physically move based on noise.

### Implementation
In vertex shader:
```glsl
// Displace vertex along normal based on noise
float displacement = snoise(Position * noiseScale + vec3(0, GameTime * noiseSpeed, 0));
vec3 displacedPos = Position + Normal * displacement * displacementAmount;
gl_Position = ProjMat * ModelViewMat * vec4(displacedPos, 1.0);
```

This creates an animated, undulating surface - very dramatic for energy/fire effects.

---

## Implementation Order

1. **Phase 1: Animation** - Quick win, immediate visual impact
2. **Phase 2: Noise** - Foundation for all advanced effects
3. **Phase 3: Presets** - User-friendly access to complex configurations
4. **Phase 4: Swirl/Flow** - Rasengan and directional effects
5. **Phase 5: Color Maps** - Fire and plasma looks
6. **Phase 6: Vertex Displacement** - Optional dramatic enhancement

---

## Technical Constraints

### UBO Size Limits
- Current: 96 bytes (6 vec4s)
- After Phase 1: 128 bytes (8 vec4s)
- After all phases: ~256 bytes (16 vec4s) - still well within limits

### Performance Considerations
- Noise is computed per-fragment - can be expensive on low-end GPUs
- Provide quality settings (octave count, noise resolution)
- Consider pre-computed noise textures as alternative

### Compatibility
- All effects should degrade gracefully if parameters are zero/default
- Effect mode NONE should just render base color (no shader overhead)
