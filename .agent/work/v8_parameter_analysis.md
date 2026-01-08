# V8 Electric Aura - Parameter Control Analysis

## Current State: Control Confusion

### The Problem
The existing parameter names confuse "ray" and "ring" concepts:
- `rayString` → Actually controls **ring frequency**
- `rayGlow` → Actually controls **ring center value** (nimitz's 0.1)
- `rayRing` → Actually controls **ring modulation power** (nimitz's 0.9)
- `rays` → Actually controls **overall intensity**
- `glow` → Domain warp amount

This makes it extremely hard to configure because:
1. Users expect "ray" params to control rays, but they control rings
2. No separate control for the plasma noise texture
3. The ring pulsation (speed, cycle, restart) is not independently controllable

---

## What We Actually Need - 3 Distinct Components

### Component 1: PLASMA TEXTURE (The Noise Background)
This is the turbulent FBM that creates the "electric texture".

**Controls needed:**
| Parameter | Purpose | Default | Range |
|-----------|---------|---------|-------|
| `plasmaScale` | Base noise frequency (larger = finer detail) | 10.0 | 1-50 |
| `plasmaSpeed` | Animation speed of noise crawl | 1.0 | 0-10 |
| `plasmaTurbulence` | Ridged vs smooth (0=smooth, 1=fully ridged) | 1.0 | 0-2 |
| `plasmaOctaves` | FBM octave count | 4 | 1-6 |
| `plasmaIntensity` | Brightness multiplier | 1.0 | 0-10 |

**In the shader (electric_plasma.glsl):**
```glsl
// Current line 268:
vec3 p3 = prn * 10.0;  // ← plasmaScale should control this!

// Current line 274:
float n = noise4q(vec4(p3 + seedVec, -time * speedRay + c));
//                                         ^ plasmaSpeed
```

### Component 2: LOGARITHMIC RINGS (The Pulsating Bands)
These are the expanding ring patterns from nimitz.

**Controls needed:**
| Parameter | Purpose | Default | Range |
|-----------|---------|---------|-------|
| `ringFrequency` | Number of ring bands | 4.0 | 1-20 |
| `ringSpeed` | Expansion/pulsation speed | 10.0 | 0-20 |
| `ringCycle` | Period of one pulse (radians) | π | 0.5-6.28 |
| `ringSharpness` | Band edge definition | 3.0 | 0.1-10 |
| `ringCenterValue` | Ring brightness target | 0.1 | 0-0.5 |
| `ringModPower` | Modulation intensity | 0.9 | 0-2 |

**In the shader (electric_plasma.glsl):**
```glsl
// Current line 288:
float pulseDist = surfaceDist / exp(mod(time * speedRing, EP_PI));
//                                        ^ ringSpeed      ^ ringCycle

// Current line 293:
float rings = abs(mod(ringVal * rayString * 4.0, EP_TAU) - EP_PI) * 3.0 + 0.2;
//                              ^ should be ringFrequency   ^ should be ringSharpness
```

### Component 3: CORONA ENVELOPE (The Overall Shape)
Controls the overall brightness and falloff.

**Controls needed:**
| Parameter | Purpose | Default | Range |
|-----------|---------|---------|-------|
| `coronaExtent` | How far corona reaches (radius multiplier) | 2.0 | 1-10 |
| `coronaFadeStart` | Where fade begins (0=edge, 1=center) | 0.5 | 0-1 |
| `coronaFadePower` | Fade curve (1=linear, <1=soft, >1=hard) | 1.0 | 0.1-10 |
| `coronaIntensity` | Overall brightness | 1.0 | 0-10 |

---

## Current Parameter Mapping (What V7 params actually do)

Looking at `animatedElectricCorona` lines 221-240:

| V7 Param | Actually Controls | Nimitz Concept |
|----------|-------------------|----------------|
| `speedRing` | Ring expansion speed | `time * 10.0` in pulsation |
| `speedRay` | Plasma noise animation | Time offset in noise4q |
| `rayString` | Ring frequency | `r * 4.0` in log ring |
| `rayReach` | Corona extent | Max reach envelope |
| `rays` | Overall intensity | Multiplier |
| `rayRing` | Ring modulation power | `pow(abs(...), 0.9)` |
| `rayGlow` | Ring center value | `0.1` in circ comparison |
| `glow` | (unused currently) | - |
| `rayFade` | Edge fade power | - |

---

## The Fix: Proper Separation

Instead of overloading V7 params, we should:

1. **Keep V7 params working** for backwards compatibility (they control the overall ray corona from V7)

2. **Add NEW V8 params** with clear names:
   - `v8PlasmaScale`, `v8PlasmaSpeed`, `v8PlasmaTurbulence`, `v8PlasmaIntensity`
   - `v8RingFrequency`, `v8RingSpeed`, `v8RingSharpness`, `v8RingCenterValue`, `v8RingModPower`, `v8RingIntensity`
   - `v8CoronaExtent`, `v8CoronaFadeStart`, `v8CoronaFadePower`, `v8CoronaIntensity`

3. **V8 params ADD to V7 result**, not replace it:
   ```glsl
   float v7Result = /* existing corona calculation */;
   float v8Plasma = /* new plasma calculation using V8 params */;
   float v8Rings = /* new ring calculation using V8 params */;
   
   // Combine additively
   float combined = v7Result + v8Plasma * v8Rings;
   ```

---

## Next Steps

1. **Decide**: Do we want V8 to completely replace V7 corona, or layer on top?
   - Replace = cleaner, but breaks old presets
   - Layer = more complex, but preserves compatibility

2. **Update `animatedElectricCorona`** to properly use the new params

3. **Wire up UBO binding** (already done in Java side)

4. **Test each control independently**
