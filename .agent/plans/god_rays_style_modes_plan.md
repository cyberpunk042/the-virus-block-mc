# God Rays Style Modes - Enhancement Plan

## Inspiration Source
Based on the existing field visual raylines system (`visual/energy/`, `visual/animation/`, `visual/shape/`).

---

## Phase 0: Curvature & Energy Mode Animation

### Curvature Modes
Modify the ray **direction** to create curved/spiral patterns:

| Mode | Effect | Implementation |
|------|--------|----------------|
| RADIAL | Straight outward (default) | `dir = normalize(lightUV - pixelUV)` |
| VORTEX | Whirlpool/accretion disk | Rotate dir by angle proportional to distance |
| SPIRAL_ARM | Galaxy arm pattern | Logarithmic spiral: `rotate(dir, log(dist) * twist)` |
| TANGENTIAL | Perpendicular to radial | `dir = vec2(-dir.y, dir.x)` (90° rotate) |
| PINWHEEL | Windmill blades | `rotate(dir, angle * pinwheelFactor + time)` |

**Animation:** Add time to curvature rotation → spinning vortex/pinwheel!

### Energy Mode Animation Ideas
Make Radiation (0) and Absorption (1) more dynamic:

| Mode | Current | Animated Version |
|------|---------|------------------|
| Radiation | Static outward | **Pulsing march length** - rays grow/shrink over time |
| Absorption | Static inward | **Retreating rays** - march start distance oscillates |
| Pulse | Already animated | Keep as-is |

**Implementation:**
```glsl
// Animated march length for Radiation mode
if (energyMode < 0.5) {
    float breathe = sin(time * 2.0) * 0.3 + 1.0; // 0.7 to 1.3
    marchLength *= breathe;
}
```

---

## Phase 1: Quick Wins

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

## Mode Compatibility Matrix

All modes should combine freely:

| curvatureMode | + energyMode | + distributionMode | + flickerMode |
|---------------|--------------|--------------------|--------------| 
| RADIAL | ✓ All | ✓ All | ✓ All |
| VORTEX | ✓ All | ✓ All | ✓ All |
| PINWHEEL | ✓ Radiation best | ✓ Sequential best | ✓ All |
| SPIRAL_ARM | ✓ All | ✓ Golden best | ✓ All |

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
- **Slot 57: curvatureMode, curvatureStrength, curvatureSpeed, reserved**

---

## Priority Order
1. ✅ Fix animation time (DONE)
2. ✅ Add intensity breathing (DONE)
3. 🔲 Add curvatureMode (Phase 0)
4. 🔲 Animate energyMode 0/1 (Phase 0)
5. 🔲 Add flickerMode (Phase 1)
6. 🔲 Add waveDistribution (Phase 1)
7. 🔲 Add travelMode (Phase 2)
8. 🔲 Add heatMap color (Phase 2)
