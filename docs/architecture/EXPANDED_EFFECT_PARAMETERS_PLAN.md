# Expanded Effect Parameters Plan

> **Status**: Planning  
> **Created**: 2026-01-04  
> **Priority**: Infrastructure first, verify existing works, then expand

## Overview

This plan expands the Field Visual parameter system to support complex Shadertoy-style effects including:
- Star/Sun Corona effects (trisomie21)
- Volumetric Star with rays (Panteleymonov)
- Geodesic animated tile effects

The expansion follows the same architecture established in the initial refactor (composable records, centralized UBO writer).

---

## Phase 1: Verify Current System Works

**Before any expansion, confirm the existing system compiles and renders correctly.**

### Checklist:
- [ ] `./gradlew build` succeeds
- [ ] Game loads without shader errors
- [ ] Field Visual effect renders (enable via GUI)
- [ ] Colors respond to changes
- [ ] Animation plays smoothly
- [ ] No visual artifacts

---

## Phase 2: Expanded Record Definitions

### UBO Layout (35 vec4 = 560 bytes)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ EFFECT PARAMETERS (Slots 0-20)                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│ Slot 0:  PositionParams      │ centerX, centerY, centerZ, radius            │
│ Slot 1:  ColorParams.primary │ primaryR, primaryG, primaryB, primaryA       │
│ Slot 2:  ColorParams.second  │ secondaryR, secondaryG, secondaryB, secondA  │
│ Slot 3:  ColorParams.tertiary│ tertiaryR, tertiaryG, tertiaryB, tertiaryA   │
│ Slot 4:  ColorParams.highlight│ highlightR, highlightG, highlightB, highlightA│
│ Slot 5:  ColorParams.ray     │ rayR, rayG, rayB, rayA                       │
│ Slot 6:  AnimParams.base     │ phase, speed, intensity, effectType          │
│ Slot 7:  AnimParams.multi    │ speedHigh, speedLow, speedRay, speedRing     │
│ Slot 8:  AnimParams.timing   │ timeScale, radialSpeed1, radialSpeed2, axial │
│ Slot 9:  CoreEdgeParams      │ coreSize, edgeSharpness, shapeType, falloff  │
│ Slot 10: FalloffParams       │ fadePower, fadeScale, insidePower, coronaEdge│
│ Slot 11: NoiseConfig         │ resLow, resHigh, amplitude, seed             │
│ Slot 12: NoiseDetail         │ baseScale, scaleMult, octaves, baseLevel     │
│ Slot 13: GlowLineParams      │ count, intensity, rayPower, raySharpness     │
│ Slot 14: CoronaParams        │ width, power, multiplier, ringPower          │
│ Slot 15: GeometryParams      │ subdivisions, roundTop, roundCorner, thick   │
│ Slot 16: GeometryParams2     │ gap, height, smoothRadius, reserved          │
│ Slot 17: TransformParams     │ rotationX, rotationY, scale, reserved        │
│ Slot 18: LightingParams      │ diffuse, ambient, backLight, fresnel         │
│ Slot 19: TimingParams        │ sceneDuration, crossfade, loopMode, animFreq │
│ Slot 20: ReservedParams      │ slot1, slot2, slot3, slot4                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ SCREEN EFFECTS (Slots 21-23)                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Slot 21: ScreenEffects       │ blackout, vignetteAmt, vignetteRad, tint     │
│ Slot 22: DistortionParams    │ strength, radius, frequency, speed           │
│ Slot 23: BlendParams         │ opacity, blendMode, fadeIn, fadeOut          │
├─────────────────────────────────────────────────────────────────────────────┤
│ CAMERA/RUNTIME (Slots 24-27)                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Slot 24: CameraPos           │ posX, posY, posZ, time                       │
│ Slot 25: CameraForward       │ forwardX, forwardY, forwardZ, aspect         │
│ Slot 26: CameraUp            │ upX, upY, upZ, fov                           │
│ Slot 27: RenderParams        │ near, far, reserved, isFlying                │
├─────────────────────────────────────────────────────────────────────────────┤
│ MATRICES (Slots 28-35)                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Slot 28-31: InvViewProj      │ mat4 (4 columns × 4 floats)                  │
│ Slot 32-35: ViewProj         │ mat4 (4 columns × 4 floats)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ DEBUG (Slot 36)                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Slot 36: DebugParams         │ camMode, debugMode, reserved, reserved       │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Total: 37 vec4 = 148 floats = 592 bytes**

---

## Phase 3: Record Definitions

### ColorParams (Expanded - 5 colors)

```java
public record ColorParams(
    int primaryColor,      // Core/body color - "orange", "_Color"
    int secondaryColor,    // Edge/accent color - "orangeRed", "_Base"
    int tertiaryColor,     // Outer glow - "_Dark"
    int highlightColor,    // Bright specular - "_Light", "_RayLight"
    int rayColor           // Ray/corona tint - "_Ray"
) {
    public static final ColorParams DEFAULT = new ColorParams(
        0xFFFFFF00,  // Yellow
        0xFFFF6600,  // Orange
        0xFFFF0066,  // Magenta
        0xFFFFFFFF,  // White
        0xFFFF9933   // Gold
    );
    
    // Extractors for each color's R,G,B,A
    public float primaryR() { return ((primaryColor >> 16) & 0xFF) / 255f; }
    // ... all 20 extractors
    
    // Builders
    public ColorParams withPrimary(int c) { ... }
    public ColorParams withSecondary(int c) { ... }
    public ColorParams withTertiary(int c) { ... }
    public ColorParams withHighlight(int c) { ... }
    public ColorParams withRay(int c) { ... }
}
```

### AnimParams (Expanded - multi-speed + timing)

```java
public record AnimParams(
    // Base animation
    float phase,           // Animation phase offset
    float speed,           // Base animation speed (1.0 = normal)
    float intensity,       // Overall brightness (0-2)
    EffectType effectType, // Effect type enum
    
    // Multi-speed channels (from Volumetric Star)
    float speedHigh,       // Fast detail animation - "_SpeedHi" (0-10)
    float speedLow,        // Slow base movement - "_SpeedLow" (0-10)
    float speedRay,        // Ray/corona animation - "_SpeedRay" (0-10)
    float speedRing,       // Ring/rotation speed - "_SpeedRing" (0-10)
    
    // Timing modifiers (from Star Corona)
    float timeScale,       // Global time multiplier (0.1 = slow)
    float radialSpeed1,    // First radial noise speed (0.35)
    float radialSpeed2,    // Second radial noise speed (0.15)
    float axialSpeed       // Z/axial animation speed (0.015)
) {
    public static final AnimParams DEFAULT = new AnimParams(
        0f, 1.0f, 1.2f, EffectType.ENERGY_ORB,
        2.0f, 2.0f, 5.0f, 2.0f,
        0.1f, 0.35f, 0.15f, 0.015f
    );
}
```

### CoreEdgeParams (Same + falloff)

```java
public record CoreEdgeParams(
    float coreSize,        // Relative core size (0.05-0.5)
    float edgeSharpness,   // Edge ring sharpness (1-10)
    float shapeType,       // 0=sphere, 1=torus, 2=cylinder, 3=prism
    float coreFalloff      // Core glow falloff power - "_Glow" (1-100)
) {
    public static final CoreEdgeParams DEFAULT = new CoreEdgeParams(
        0.15f, 4.0f, 0f, 4.0f
    );
}
```

### FalloffParams (NEW)

```java
public record FalloffParams(
    float fadePower,           // Distance fade exponent (0.5)
    float fadeScale,           // Fade distance multiplier (2.0)
    float insideFalloffPower,  // Inside-core falloff (24.0)
    float coronaEdge           // Corona edge threshold (1.1)
) {
    public static final FalloffParams DEFAULT = new FalloffParams(
        0.5f, 2.0f, 24.0f, 1.1f
    );
}
```

### NoiseParams (Expanded - config + detail)

```java
public record NoiseConfigParams(
    float resLow,      // Low frequency noise res (15.0)
    float resHigh,     // High frequency noise res (45.0)
    float amplitude,   // Noise amplitude multiplier (2.0)
    float seed         // Noise variation seed (-10 to 10)
) {
    public static final NoiseConfigParams DEFAULT = new NoiseConfigParams(
        15.0f, 45.0f, 2.0f, 0f
    );
}

public record NoiseDetailParams(
    float baseScale,       // Base detail scale (0.03125)
    float scaleMultiplier, // Per-octave scale mult (4.0)
    float octaves,         // FBM octave count (7)
    float baseLevel        // Base noise floor (0.4)
) {
    public static final NoiseDetailParams DEFAULT = new NoiseDetailParams(
        0.03125f, 4.0f, 7f, 0.4f
    );
}
```

### GlowLineParams (Expanded)

```java
public record GlowLineParams(
    float count,           // Radial line count (8-24)
    float intensity,       // Line brightness (0-1)
    float rayPower,        // Ray intensity exponent - "_Rays" (1-10)
    float raySharpness     // Ray edge sharpness - "_RayString" (0.02-10)
) {
    public static final GlowLineParams DEFAULT = new GlowLineParams(
        16.0f, 0.8f, 2.0f, 1.0f
    );
    
    // Legacy flags moved to separate accessor
    public float flags(boolean showRays, boolean showCorona, int version) {
        return (showRays ? 1f : 0f) + (showCorona ? 2f : 0f) + version * 4f;
    }
}
```

### CoronaParams (Expanded)

```java
public record CoronaParams(
    float width,       // Corona width (0.2-1.5)
    float power,       // Falloff exponent (2.0)
    float multiplier,  // Brightness multiplier (50.0)
    float ringPower    // Ring glow power - "_RayRing" (1-10)
) {
    public static final CoronaParams DEFAULT = new CoronaParams(
        0.5f, 2.0f, 50.0f, 1.0f
    );
}
```

### GeometryParams (NEW - for SDF effects)

```java
public record GeometryParams(
    float subdivisions,    // Tile density (1-10)
    float roundTop,        // Top edge rounding (0-0.5)
    float roundCorner,     // Corner rounding (0-0.5)
    float thickness        // Shell thickness (0.5-5.0)
) {
    public static final GeometryParams DEFAULT = new GeometryParams(
        3.0f, 0.05f, 0.1f, 2.0f
    );
}

public record GeometryParams2(
    float gap,             // Tile gap (0.005-0.5)
    float height,          // Extrusion height (1.0-5.0)
    float smoothRadius,    // SDF smooth blend radius
    float reserved
) {
    public static final GeometryParams2 DEFAULT = new GeometryParams2(
        0.005f, 2.0f, 0.1f, 0f
    );
}
```

### TransformParams (NEW)

```java
public record TransformParams(
    float rotationX,   // Model rotation X (radians)
    float rotationY,   // Model rotation Y (radians)
    float scale,       // Model scale multiplier
    float reserved
) {
    public static final TransformParams DEFAULT = new TransformParams(
        0.3f, 0.25f, 1.0f, 0f
    );
}
```

### LightingParams (NEW - for 3D effects)

```java
public record LightingParams(
    float diffuseStrength,   // Diffuse light mult (1.20)
    float ambientStrength,   // Ambient light mult (0.80)
    float backLightStrength, // Back rim light (0.30)
    float fresnelStrength    // Fresnel rim mult (0.20)
) {
    public static final LightingParams DEFAULT = new LightingParams(
        1.2f, 0.8f, 0.3f, 0.2f
    );
}
```

### TimingParams (NEW)

```java
public record TimingParams(
    float sceneDuration,       // Animation cycle length (6.0s)
    float crossfadeDuration,   // Transition blend time (2.0s)
    float loopMode,            // 0=none, 1/2/3=loop variants
    float animFrequency        // Pattern frequency (10-30)
) {
    public static final TimingParams DEFAULT = new TimingParams(
        6.0f, 2.0f, 0f, 10.0f
    );
}
```

---

## Phase 4: Updated FieldVisualConfig

```java
public record FieldVisualConfig(
    // Effect parameters (slots 0-20)
    ColorParams colors,              // 5 vec4 (primary, secondary, tertiary, highlight, ray)
    AnimParams anim,                 // 3 vec4 (base, multi-speed, timing)
    CoreEdgeParams coreEdge,         // 1 vec4
    FalloffParams falloff,           // 1 vec4 (NEW)
    NoiseConfigParams noiseConfig,   // 1 vec4 (renamed)
    NoiseDetailParams noiseDetail,   // 1 vec4 (NEW)
    GlowLineParams glowLine,         // 1 vec4
    CoronaParams corona,             // 1 vec4 (expanded)
    GeometryParams geometry,         // 1 vec4 (NEW)
    GeometryParams2 geometry2,       // 1 vec4 (NEW)
    TransformParams transform,       // 1 vec4 (NEW)
    LightingParams lighting,         // 1 vec4 (NEW)
    TimingParams timing,             // 1 vec4 (NEW)
    ReservedParams reserved,         // 1 vec4
    
    // Screen effects (slots 21-23)
    ScreenEffects screen,            // 1 vec4
    DistortionParams distortion,     // 1 vec4
    BlendParams blend                // 1 vec4
) {
    public static final FieldVisualConfig DEFAULT = new FieldVisualConfig(
        ColorParams.DEFAULT,
        AnimParams.DEFAULT,
        CoreEdgeParams.DEFAULT,
        FalloffParams.DEFAULT,
        NoiseConfigParams.DEFAULT,
        NoiseDetailParams.DEFAULT,
        GlowLineParams.DEFAULT,
        CoronaParams.DEFAULT,
        GeometryParams.DEFAULT,
        GeometryParams2.DEFAULT,
        TransformParams.DEFAULT,
        LightingParams.DEFAULT,
        TimingParams.DEFAULT,
        ReservedParams.DEFAULT,
        ScreenEffects.NONE,
        DistortionParams.NONE,
        BlendParams.DEFAULT
    );
}
```

---

## Phase 5: Implementation Order

### Step 1: Verify Current System
- [ ] Build and test existing field_visual effect
- [ ] Confirm all current parameters work
- [ ] Document any existing bugs

### Step 2: Expand FieldVisualTypes.java
- [ ] Add FalloffParams record
- [ ] Expand ColorParams to 5 colors
- [ ] Expand AnimParams with multi-speed + timing
- [ ] Add NoiseDetailParams record
- [ ] Expand CoronaParams
- [ ] Add GeometryParams, GeometryParams2
- [ ] Add TransformParams
- [ ] Add LightingParams
- [ ] Add TimingParams

### Step 3: Update FieldVisualConfig.java
- [ ] Add new record fields
- [ ] Update DEFAULT constant
- [ ] Add convenience accessors
- [ ] Update withX() builders

### Step 4: Update FieldVisualUBOWriter.java
- [ ] Expand BUFFER_SIZE to 592
- [ ] Add write calls for new records
- [ ] Update slot documentation

### Step 5: Update field_visual.fsh
- [ ] Add new uniform declarations
- [ ] Update layout comments
- [ ] Verify std140 alignment

### Step 6: Update field_visual.json
- [ ] Add all new float entries
- [ ] Update for 148 floats

### Step 7: Update FieldVisualAdapter.java
- [ ] Add new record fields
- [ ] Update get/set paths
- [ ] Update buildConfig()
- [ ] Update JSON serialization

### Step 8: Testing
- [ ] Verify build succeeds
- [ ] Verify shader loads
- [ ] Verify effect renders
- [ ] Test parameter changes via GUI

---

## Shader Parameter Mapping Reference

| Shadertoy Parameter | Our Record.field | Default |
|---------------------|------------------|---------|
| `brightness` | `anim.intensity` | 1.2 |
| `radius` | `position.radius` | 3.0 |
| `orange` | `colors.primary` | 0xFFFFFF00 |
| `orangeRed` | `colors.secondary` | 0xFFFF6600 |
| `_Light` | `colors.highlight` | 0xFFFFFFFF |
| `_RayLight` | `colors.ray` | 0xFFFF9933 |
| `_SpeedHi` | `anim.speedHigh` | 2.0 |
| `_SpeedLow` | `anim.speedLow` | 2.0 |
| `_SpeedRay` | `anim.speedRay` | 5.0 |
| `_SpeedRing` | `anim.speedRing` | 2.0 |
| `time * 0.1` | `anim.timeScale` | 0.1 |
| `_Glow` | `coreEdge.coreFalloff` | 4.0 |
| `pow(length, 0.5)` | `falloff.fadePower` | 0.5 |
| `pow(dist*invR, 24)` | `falloff.insideFalloffPower` | 24.0 |
| `15.0` (snoise res) | `noiseConfig.resLow` | 15.0 |
| `45.0` (snoise res) | `noiseConfig.resHigh` | 45.0 |
| `*2.0` (noise amp) | `noiseConfig.amplitude` | 2.0 |
| `_Seed` | `noiseConfig.seed` | 0 |
| `0.03125` | `noiseDetail.baseScale` | 0.03125 |
| `*4.0` (scale mult) | `noiseDetail.scaleMultiplier` | 4.0 |
| `i<=7` (FBM loops) | `noiseDetail.octaves` | 7 |
| `_Rays` | `glowLine.rayPower` | 2.0 |
| `_RayString` | `glowLine.raySharpness` | 1.0 |
| `pow(fVal, 2.0)` | `corona.power` | 2.0 |
| `*50.0` | `corona.multiplier` | 50.0 |
| `_RayRing` | `corona.ringPower` | 1.0 |
| `subdivisions` | `geometry.subdivisions` | 3.0 |
| `.gap` | `geometry2.gap` | 0.005 |
| `.height` | `geometry2.height` | 2.0 |
| `MODEL_ROTATION` | `transform.rotationX/Y` | 0.3, 0.25 |
| `1.20*dif` | `lighting.diffuseStrength` | 1.2 |
| `0.80*amb` | `lighting.ambientStrength` | 0.8 |
| `SCENE_DURATION` | `timing.sceneDuration` | 6.0 |
| `cos(blend*30)` | `timing.animFrequency` | 30 |

---

## Notes

- This expansion adds ~320 bytes to the UBO (27→37 slots)
- All new parameters have sensible defaults that maintain current behavior
- Existing effects continue to work with DEFAULT values
- New parameters only affect behavior when explicitly set
- GUI will need expansion to expose new controls (future phase)
