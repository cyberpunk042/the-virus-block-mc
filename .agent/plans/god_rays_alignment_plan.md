# God Rays - Comprehensive Alignment Plan

## Executive Summary
The god rays post-effect currently uses custom naming for modes that don't align with the established visual field enums. This plan aligns everything with the existing Java enums as the source of truth.

---

## Source of Truth: Java Visual Enums

### RayCurvature (shape/RayCurvature.java)
| Index | Enum Value | Display Name | Description |
|-------|------------|--------------|-------------|
| 0 | NONE | None | Straight radial rays |
| 1 | VORTEX | Vortex | Whirlpool pattern |
| 2 | SPIRAL_ARM | Spiral Arm | Galaxy arm pattern |
| 3 | TANGENTIAL | Tangential | Perpendicular to radial |
| 4 | LOGARITHMIC | Logarithmic | Nautilus shell spiral |
| 5 | PINWHEEL | Pinwheel | Windmill blades |
| 6 | ORBITAL | Orbital | Circular paths |

### RayArrangement (shape/RayArrangement.java)
| Index | Enum Value | Description |
|-------|------------|-------------|
| 0 | RADIAL | 2D star from center outward |
| 1 | SPHERICAL | 3D hedgehog spines |
| 2 | PARALLEL | All same direction |
| 3 | CONVERGING | All toward center |
| 4 | DIVERGING | All away from center |

### RayDistribution (shape/RayDistribution.java)
| Index | Enum Value | Description |
|-------|------------|-------------|
| 0 | UNIFORM | All rays identical |
| 1 | RANDOM | Randomized start/length |
| 2 | STOCHASTIC | Heavily randomized |
We can also keep noise and Weigthed (3-4)

### RadiativeInteraction (energy/RadiativeInteraction.java)
| Index | Enum Value | Display Name | Description |
|-------|------------|--------------|-------------|
| 0 | NONE | None | Full shape visible |
| 1 | EMISSION | Emission | Outward from center |
| 2 | ABSORPTION | Absorption | Inward toward center |
| 3 | REFLECTION | Reflection | Bounce back |
| 4 | TRANSMISSION | Transmission | Pass through segment |
| 5 | SCATTERING | Scattering | Multiple directions |
| 6 | OSCILLATION | Oscillation | Pulse breathing |
| 7 | RESONANCE | Resonance | Grow then decay |

### EnergyFlicker (energy/EnergyFlicker.java)
| Index | Enum Value | Display Name | Description |
|-------|------------|--------------|-------------|
| 0 | NONE | None | No flicker |
| 1 | SCINTILLATION | Scintillation | Star twinkling |
| 2 | STROBE | Strobe | On/off blink |
| 3 | FADE_PULSE | Fade Pulse | Breathing |
| 4 | FLICKER | Flicker | Candlelight |
| 5 | LIGHTNING | Lightning | Flash then fade |
| 6 | HEARTBEAT | Heartbeat | Double pulse |

### EnergyTravel (energy/EnergyTravel.java)
| Index | Enum Value | Display Name | Category |
|-------|------------|--------------|----------|
| 0 | NONE | None | - |
| 1 | CHASE | Chase | Particles outward |
| 2 | SCROLL | Scroll | Gradient scroll |
| 3 | COMET | Comet | Head + tail |
| 4 | SPARK | Spark | Random sparks |
| 5 | PULSE_WAVE | Pulse Wave | Brightness pulse |
| 6-9 | REVERSE_* | Reverse variants | Inward |

### ColorMode (appearance/ColorMode.java)
| Index | Enum Value | Description |
|-------|------------|-------------|
| 0 | GRADIENT | Uniform blend primary→secondary |
| 1 | CYCLING | Animate through color set |
| 2 | MESH_GRADIENT | Per-vertex gradient |
| 3 | MESH_RAINBOW | Rainbow spectrum |
| 4 | RANDOM | Random from set |
| 5 | HEAT_MAP | Inner=hot, outer=cold |
| 6 | RANDOM_PULSE | Random color bursts |
| 7 | BREATHE | Saturation pulsing |
| 8 | REACTIVE | Motion/distance reactive |

---

## Current State vs Target State

### Energy Mode (Slot 52.x)
| Current Panel | Should Be |
|---------------|-----------|
| "Radiation" | "None" → RadiativeInteraction.NONE |
| "Absorption" | "Emission" → RadiativeInteraction.EMISSION |
| "Pulse" | "Absorption" → RadiativeInteraction.ABSORPTION |
| (missing) | + "Oscillation", "Resonance", etc. |

### Distribution Mode (Slot 52.z)
| Current Panel | Should Be |
|---------------|-----------|
| "Uniform" | ✅ RayDistribution.UNIFORM |
| "Weighted" | Change to "Random" → RayDistribution.RANDOM |
| "Noise" | Change to "Stochastic" → RayDistribution.STOCHASTIC |

### Arrangement Mode (Slot 52.w)
**Currently: NOT IN PANEL**
| Should Add |
|------------|
| "Radial" → RayArrangement.RADIAL |
| "Spherical" → RayArrangement.SPHERICAL |
| "Parallel" → RayArrangement.PARALLEL |
| "Converging" → RayArrangement.CONVERGING |
| "Diverging" → RayArrangement.DIVERGING |

### Color Mode (Slot 52.y)
| Current Panel | Should Be |
|---------------|-----------|
| "Solid" | → "Gradient" (ColorMode.GRADIENT) |
| "Gradient" | → "Mesh Gradient" (ColorMode.MESH_GRADIENT) |
| "Temperature" | → "Heat Map" (ColorMode.HEAT_MAP) |
| (missing) | + "Rainbow", "Breathing", etc. |

### Curvature Mode (Slot 55.x)
| Current Panel | Should Be |
|---------------|-----------|
| "Radial" | → "None" (RayCurvature.NONE) |
| "Vortex" | ✅ RayCurvature.VORTEX |
| "Spiral" | → "Spiral Arm" (RayCurvature.SPIRAL_ARM) |
| "Tangential" | ✅ RayCurvature.TANGENTIAL |
| "Pinwheel" | ✅ RayCurvature.PINWHEEL |
| (missing) | + "Logarithmic", "Orbital" |

### Flicker Mode (Slot 56.x)
| Current Panel | Should Be |
|---------------|-----------|
| "None" | ✅ EnergyFlicker.NONE |
| "Scintillation" | ✅ |
| "Strobe" | ✅ |
| "Fade Pulse" | ✅ |
| "Heartbeat" | ✅ |
| "Lightning" | ✅ |
| (missing) | + "Flicker" (candlelight) |

**CRITICAL: Shader equations not implemented!**

### Travel Mode (Slot 57.x)
| Current Panel | Should Be |
|---------------|-----------|
| "None" | ✅ EnergyTravel.NONE |
| "Scroll" | ✅ |
| "Chase" | ✅ |
| "Pulse Wave" | ✅ |
| "Comet" | ✅ |
| (missing) | + "Spark", + Reverse variants |

**CRITICAL: Shader equations not implemented!**

---

## Implementation Phases

### Phase 1: Panel Alignment (No Shader Changes)
1. Fix Energy Mode dropdown to match RadiativeInteraction enum
2. Fix Distribution Mode dropdown to match RayDistribution enum
3. ADD Arrangement Mode dropdown (missing entirely!)
4. Fix Color Mode dropdown to match ColorMode enum
5. Fix Curvature Mode dropdown to match RayCurvature enum

### Phase 2: Shader Implementation - Flicker
1. Add `getFlickerModulation()` function to god_rays_style.glsl
2. Implement SCINTILLATION, STROBE, FADE_PULSE, FLICKER, LIGHTNING, HEARTBEAT
3. Apply modulation to final illumination in god_rays.glsl

### Phase 3: Shader Implementation - Travel
1. Add `getTravelModulation()` function to god_rays_style.glsl
2. Implement CHASE, SCROLL, COMET, SPARK, PULSE_WAVE
3. Apply along ray march samples in god_rays.glsl

### Phase 4: Extended Modes
1. Add missing curvature modes (LOGARITHMIC, ORBITAL)
2. Add Reverse/Bipolar travel variants
3. Add extended color modes (RAINBOW, BREATHING, REACTIVE)

---

## Files to Modify

### Panel (Phase 1)
- `src/client/java/net/cyberpunk042/client/gui/panel/sub/FieldVisualSubPanel.java`

### Shader (Phases 2-3)
- `src/main/resources/assets/.../shaders/post/include/effects/god_rays_style.glsl`
- `src/main/resources/assets/.../shaders/post/include/effects/god_rays.glsl`
- `src/main/resources/assets/.../shaders/post/hdr/god_rays_accum.fsh`

### RenderConfig (if adding new params)
- `src/client/java/net/cyberpunk042/client/gui/config/RenderConfig.java`
