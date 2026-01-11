# God Rays Style Modes - Enhancement Plan

## Inspiration Source
Based on the existing field visual raylines system (`visual/energy/`, `visual/animation/`, `visual/shape/`).

---

-- we forgot field curvature -> investigate for for phase 0
energyMode=0 (Radiation)	 -> We should find a way to animate
energyMode=1 (Absorption)  -> We should find a way to animate
mode should be compatible with curvature time and energyMode and distributionMode an as much mode combo as possible

## Phase 1: Quick Wins (After Animation Fix)

### 1. Flicker Modes
Add alpha/intensity modulation over time:

| Mode | Effect | Implementation |
|------|--------|----------------|
| NONE | No flicker | Default |
| SCINTILLATION | Star twinkling per-ray | Noise(uv + time) |
| STROBE | Rhythmic on/off | step(sin(time * freq)) |
| FADE_PULSE | Smooth breathing | sin(time * freq) * 0.5 + 0.5 |
| HEARTBEAT | Double-pulse rhythm | Custom curve with two peaks |
| LIGHTNING | Flash then fade | exp(-time) after trigger |

### 2. Wave Distribution
Control how animation phase is distributed across rays:

| Mode | Effect | Implementation |
|------|--------|----------------|
| CONTINUOUS | All rays same phase | phase = time |
| SEQUENTIAL | Rotating wedge pattern | phase = time + angle/2π |
| RANDOM | Chaotic per-ray | phase = time + hash(angle) |
| GOLDEN_RATIO | Aesthetically scattered | phase = time + fract(index × φ) |

---

## Phase 2: Polish

### 3. Travel/Chase Effects
Moving particles along rays:

| Mode | Effect | Implementation |
|------|--------|----------------|
| SCROLL | Gradient scrolls outward | intensity *= sin(t - time * speed) |
| CHASE | Discrete dots travel | step function with time offset |
| PULSE_WAVE | Brightness pulses travel | sin((t - time) * freq) shaped |
| COMET | Bright head, fading tail | exp decay after position |

### 4. Advanced Color Modes
| Mode | Effect |
|------|--------|
| HEAT_MAP | Inner=hot colors, outer=cool colors |
| CYCLING | Animate through color palette |
| RAINBOW | Full spectrum along ray length |

---

## Not Porting (3D Geometry Specific)
- RayType (DROPLET, CONE, KAMEHAMEHA, etc.) - Physical geometry
- RayLineShape (CORKSCREW, SPRING, ZIGZAG) - 3D bending
- WiggleMode, TwistMode - Physical deformation
- MotionMode - Orbital motion
- FillMode - Mesh rendering technique

---

## UBO Slot Allocation (if implementing all)

Current slots 52-54 used for:
- Slot 52: energyMode, colorMode, distributionMode, arrangementMode
- Slot 53: color2R, color2G, color2B, gradientPower
- Slot 54: noiseScale, noiseSpeed, noiseIntensity, angularBias

Needed additions:
- Slot 55: flickerMode, flickerIntensity, flickerFrequency, waveDistribution
- Slot 56: travelMode, travelSpeed, chaseCount, chaseWidth

---

## Priority Order
1. ✅ Fix animation time (CURRENT)
2. Add flickerMode (5 modes)
3. Add waveDistribution (4 modes)
4. Add travelMode (4 modes)
5. Add heatMap color
