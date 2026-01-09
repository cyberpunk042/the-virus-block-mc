# Tornado Field Visual Effect - Implementation Plan

> **STATUS: BLOCKED - AWAITING UBO ARCHITECTURE REFACTOR**
> 
> This plan was created before the UBO refactor (see `ubo_architecture_refactor.md`).
> 
> **Dependencies:**
> - Complete UBO Architecture Refactor (Phases 1-4 minimum)
> - FrameUBO and CameraUBO must exist
> - Effect-specific UBO pattern must be established
> 
> Once refactor is complete, update this plan with:
> - Correct file paths matching new folder layout
> - Binding index from centralized registry
> - Nested section pattern for TornadoConfigUBO
> - Reserved lanes strategy

---

## Overview

**Effect Type:** Tornado  
**Version:** 1  
**Source:** Shadertoy "Tornado" (Creative Commons Attribution-NonCommercial-ShareAlike 3.0)

A volumetric tornado effect with:
- Inverted cone funnel shape (wide at bottom, narrow at top)
- Height-based swirl rotation
- Peripheral dust ring around the base
- Two-point volumetric lighting
- FBM noise for turbulence

---

## Visual Components

### 1. Funnel Shape

The tornado uses an inverted cone defined by `1/y`:
- At `y = 1` (bottom): wide
- At `y = 10` (top): narrow

**Two parts:**
- **Center Cone:** Main visible funnel with noise-modulated edges
- **Peripheral Ring:** Dust cloud around the base, fades with height

### 2. Swirl Motion

Position is rotated around Y-axis based on height before noise sampling:
```
rotationAngle = swirlRate * height
```
Creates the spiral pattern characteristic of tornados.

### 3. Volumetric Lighting

Two point lights with ray marching for volumetric shadows:
- **Light 1 (Warm):** Orange tones, positioned offset from tornado
- **Light 2 (Cool):** Green/cyan tones, opposite side
- **Ambient:** Dim background illumination

---

## UBO Design (Dedicated TornadoUBO)

### Decision
Use a **separate, clean UBO** for Tornado rather than extending FieldVisualUBO.
This allows:
- Cleaner parameter organization
- Smaller buffer size
- Effect-specific semantics

### UBO Structure

```java
@UBOStruct(name = "TornadoParams")
public record TornadoUBO(
    // ═══════════════════════════════════════════════════════════════════════════
    // POSITION (Slot 0)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 PositionVec4 position,           // xyz = center, w = radius
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (Slots 1-3)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 ColorVec4 light1Color,           // Light 1 RGBA (warm/orange)
    @Vec4 ColorVec4 light2Color,           // Light 2 RGBA (cool/green)
    @Vec4 ColorVec4 ambientColor,          // Ambient RGBA
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TORNADO SHAPE (Slots 4-5)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 ShapeVec4 shape,                 // x=density, y=swirlRate, z=centerRadiusMult, w=centerNoiseFade
    @Vec4 PeripheralVec4 peripheral,       // x=periRadiusMult, y=periFade1, z=periFade2, w=heightFade
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NOISE & ANIMATION (Slot 6)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 NoiseVec4 noise,                 // x=scale, y=vertSpeed, z=octaves, w=ditherAmount
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RAYMARCH SETTINGS (Slot 7)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 RaymarchVec4 raymarch,           // x=maxDist, y=steps, z=lightSteps, w=ambientMult
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIGHT POSITIONS (Slots 8-9)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 Light1Vec4 light1,               // xyz=offset from center, w=strength
    @Vec4 Light2Vec4 light2,               // xyz=offset from center, w=strength
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA (Slots 10-12)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 CameraPosTimeVec4 cameraPosTime, // xyz=position, w=time
    @Vec4 CameraForwardVec4 cameraForward, // xyz=forward, w=aspect
    @Vec4 CameraUpVec4 cameraUp,           // xyz=up, w=fov
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MATRICES (Slots 13-20)
    // ═══════════════════════════════════════════════════════════════════════════
    @Mat4 Matrix4f invViewProj,            // Inverse view-projection
    @Mat4 Matrix4f viewProj                // View-projection
) {}
```

**Total Size:** ~21 slots = 84 floats = 336 bytes

---

## Parameter Details

### Shape Parameters (Slot 4)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| density | 3.5 | 0.1-10.0 | Overall opacity multiplier |
| swirlRate | 0.628 | 0.0-2.0 | Rotation per Y unit (radians). 0.628 = 0.2*PI = 36° |
| centerRadiusMult | 2.0 | 0.5-10.0 | Center cone width at y=1 |
| centerNoiseFade | 3.25 | 0.5-10.0 | How much noise affects center edge |

### Peripheral Parameters (Slot 5)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| peripheralRadiusMult | 8.0 | 2.0-20.0 | Outer ring width at y=1 |
| peripheralInnerFade | 1.0 | 0.1-5.0 | Inner edge of ring |
| peripheralOuterFade | 2.0 | 0.5-10.0 | Outer edge of ring |
| peripheralHeightFade | 0.07 | 0.01-0.5 | Ring fades away above ground |

### Noise Parameters (Slot 6)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| noiseScale | 0.04 | 0.01-0.2 | FBM sampling scale |
| verticalSpeed | 1.0 | 0.1-5.0 | Noise scroll speed (upward) |
| fbmOctaves | 4 | 1-8 | FBM octave count |
| ditherAmount | 0.3 | 0.0-1.0 | Temporal dithering strength |

### Raymarch Parameters (Slot 7)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| maxDistance | 10.0 | 5.0-50.0 | Maximum ray travel distance |
| stepCount | 35 | 10-64 | Number of raymarch samples |
| lightStepCount | 4 | 2-16 | Light shadow samples |
| ambientMult | 0.4 | 0.0-1.0 | Ambient color multiplier |

### Light 1 Parameters (Slot 8)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| offsetX | -5.0 | -20.0-20.0 | X offset from tornado center |
| offsetY | 5.0 | 0.0-20.0 | Y offset (height) |
| offsetZ | -5.0 | -20.0-20.0 | Z offset |
| strength | 20.0 | 1.0-100.0 | Light intensity |

### Light 2 Parameters (Slot 9)

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| offsetX | -5.0 | -20.0-20.0 | X offset from tornado center |
| offsetY | 5.0 | 0.0-20.0 | Y offset (height) |
| offsetZ | 5.0 | -20.0-20.0 | Z offset |
| strength | 20.0 | 1.0-100.0 | Light intensity |

### Default Colors

| Color | Default RGBA | Description |
|-------|--------------|-------------|
| light1Color | (0.6, 0.25, 0.15, 1.0) | Warm orange |
| light2Color | (0.1, 1.0, 0.6, 1.0) | Cool green-cyan |
| ambientColor | (0.01, 0.01, 0.002, 1.0) | Dim warm |

---

## File Structure

### Java Files

```
src/client/java/net/cyberpunk042/client/visual/effect/tornado/
├── TornadoUBO.java              # UBO record with annotations
├── TornadoConfig.java           # Configuration holder
├── TornadoTypes.java            # Parameter record types
├── TornadoVec4Types.java        # Vec4 serialization
├── TornadoPostEffect.java       # Rendering integration
└── TornadoDefaults.java         # Default presets
```

### Shader Files

```
src/main/resources/assets/the-virus-block/shaders/post/
├── tornado.fsh                  # Main fragment shader (or branch in field_visual.fsh)
├── tornado.json                 # Post-effect definition
└── include/effects/
    ├── tornado.glsl             # Core tornado raymarch logic
    └── tornado_types.glsl       # GLSL param structs matching UBO
```

### Panel Files

```
src/client/java/net/cyberpunk042/client/gui/panel/sub/
└── TornadoSubPanel.java         # Dedicated tornado controls (or extend FieldVisualSubPanel)
```

---

## Implementation Phases

### Phase 1: UBO & Types
1. Create TornadoTypes.java with parameter records
2. Create TornadoVec4Types.java with Vec4 serialization
3. Create TornadoUBO.java with @Vec4/@Mat4 annotations
4. Create TornadoConfig.java for holding parameters
5. Create TornadoDefaults.java with preset values

### Phase 2: Shader
1. Create tornado_types.glsl with matching GLSL structs
2. Create tornado.glsl with:
   - rotationMatrix() function
   - fbm() noise (using existing value_noise.glsl)
   - map() density function
   - lightMarch() volumetric shadows
   - calculateLight() point light contribution
   - marchTornado() main raymarch
3. Create tornado.fsh or integrate into field_visual.fsh
4. Create tornado.json post-effect definition

### Phase 3: Integration
1. Create TornadoPostEffect.java (or integrate into existing)
2. Wire up ReflectiveUBOWriter for TornadoUBO
3. Register tornado effect type

### Phase 4: Panel
1. Create TornadoSubPanel.java with controls for all parameters
2. Implement effect type switching in panel system

### Phase 5: Commands & Testing
1. Add /fieldtest tornado command support
2. Implement followMode support
3. Test and tune default values
4. Create presets for different tornado styles

---

## Dependencies

### Shader Dependencies
- `include/utils/value_noise.glsl` - for fbm noise
- Camera utilities (existing or new)
- Depth utilities (if needed)

### Java Dependencies
- `ReflectiveUBOWriter` - for UBO serialization
- `@Vec4`, `@Mat4` annotations
- Panel framework classes

---

## Notes

### Noise Texture
Original Shadertoy uses `iChannel0` texture for noise. We will use:
- Procedural `valueNoise3D()` from our existing library
- May need optimization if performance is an issue

### Performance Considerations
- 35 raymarch steps × nested light march (4 steps) = 140+ samples per pixel
- Consider reducing defaults for lower-end hardware
- Implement LOD based on distance?

### Future Enhancements
- Multiple tornado support
- Debris particles
- Lightning effects
- Sound integration

---

## Reference

### Original Shadertoy
```
License: Creative Commons Attribution-NonCommercial-ShareAlike 3.0
```

Key functions to port:
- `rotationMatrix(axis, angle)` - rotation matrix builder
- `fbm(p)` - 4-octave fractal noise
- `map(p)` - tornado density at point
- `lightMarch(ro, lightPos)` - volumetric shadow
- `calculateLight(pos, lightPos, color, strength)` - point light
- `march(ro, rd, dither, var)` - main raymarch accumulation

---

> **REMINDER:** This plan will need revision after the Shader Architecture and UBO Refactor.
> Return to this document and update file paths, class names, and integration points as needed.
