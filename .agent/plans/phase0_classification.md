# UBO Parameter Classification - Phase 0 Audit

> **Source:** `src/main/resources/assets/the-virus-block/shaders/post/include/core/field_visual_base.glsl`
> **Uniform Block:** `FieldVisualConfig`
> **Total Parameters:** 170
> 
> **Instructions:** Review and confirm the `Domain Target` and `Update Freq` columns.
> Values marked with `**bold**` or `?` are suggestions that need verification.

---

## Domain Legend

| Domain | Description | Update Policy |
|--------|-------------|---------------|
| **Frame** | Global per-frame drivers | Every frame |
| **Camera** | View definition and matrices | Every frame |
| **Object** | Per-instance identity/transform | Per draw (future) |
| **EffectConfig** | Preset/style parameters | On preset change |
| **EffectRuntime** | Per-frame instance state | Per frame (if CPU-driven) |
| **Debug** | Debug flags and values | Dev only |
| **REMOVE** | Candidate for removal | N/A |

---

## Parameter Classification

| Slot | Name | Section | Domain Target | Update Freq | Notes |
|------|------|---------|---------------|-------------|-------|
| 0 | `CenterX` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? |  |
| 0 | `CenterY` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? |  |
| 0 | `CenterZ` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? |  |
| 0 | `Radius` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? |  |
|---|---|---|---|---|---|
| 1 | `PrimaryR` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 1 | `PrimaryG` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 1 | `PrimaryB` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 1 | `PrimaryA` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 2 | `SecondaryR` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 2 | `SecondaryG` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 2 | `SecondaryB` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 2 | `SecondaryA` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 3 | `TertiaryR` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 3 | `TertiaryG` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 3 | `TertiaryB` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 3 | `TertiaryA` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 4 | `HighlightR` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 4 | `HighlightG` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 4 | `HighlightB` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 4 | `HighlightA` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 5 | `RayColorR` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 5 | `RayColorG` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 5 | `RayColorB` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 5 | `RayColorA` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 6 | `Phase` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Animation phase offset |
| 6 | `AnimSpeed` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Animation speed multiplier |
| 6 | `Intensity` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Effect intensity (0-2) |
| 6 | `EffectType` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0=none, 1=energy_orb, 2=geodesic |
|---|---|---|---|---|---|
| 7 | `SpeedHigh` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Fast detail animation |
| 7 | `SpeedLow` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Slow base movement |
| 7 | `SpeedRay` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ray/corona animation |
| 7 | `SpeedRing` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ring/rotation speed |
|---|---|---|---|---|---|
| 8 | `TimeScale` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Global time multiplier |
| 8 | `RadialSpeed1` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | First radial noise speed |
| 8 | `RadialSpeed2` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Second radial noise speed |
| 8 | `AxialSpeed` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Z/axial animation speed |
|---|---|---|---|---|---|
| 9 | `CoreSize` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Size of bright center (0-1) |
| 9 | `EdgeSharpness` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Edge falloff sharpness |
| 9 | `ShapeType` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0=sphere, 1=torus, 2=cylinder, 3=prism |
| 9 | `CoreFalloff` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Core glow falloff power |
|---|---|---|---|---|---|
| 10 | `FadePower` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Distance fade exponent |
| 10 | `FadeScale` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Fade distance multiplier |
| 10 | `InsideFalloffPower` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Inside-core falloff |
| 10 | `CoronaEdge` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Corona edge threshold |
|---|---|---|---|---|---|
| 11 | `NoiseResLow` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Low frequency noise resolution |
| 11 | `NoiseResHigh` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | High frequency noise resolution |
| 11 | `NoiseAmplitude` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Noise amplitude multiplier |
| 11 | `NoiseSeed` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Noise variation seed |
|---|---|---|---|---|---|
| 12 | `NoiseBaseScale` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Base detail scale |
| 12 | `NoiseScaleMultiplier` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Per-octave scale mult |
| 12 | `NoiseOctaves` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | FBM octave count |
| 12 | `NoiseBaseLevel` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Base noise floor |
|---|---|---|---|---|---|
| 13 | `GlowLineCount` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Number of radial glow lines |
| 13 | `GlowLineIntensity` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 13 | `RayPower` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ray intensity exponent |
| 13 | `RaySharpness` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ray edge sharpness |
|---|---|---|---|---|---|
| 14 | `CoronaWidth` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Width of corona glow |
| 14 | `CoronaPower` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Falloff exponent |
| 14 | `CoronaMultiplier` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Brightness multiplier |
| 14 | `RingPower` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ring glow power |
|---|---|---|---|---|---|
| 15 | `GeoSubdivisions` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Tile density |
| 15 | `GeoRoundTop` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Top edge rounding |
| 15 | `GeoRoundCorner` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Corner rounding |
| 15 | `GeoThickness` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Shell thickness |
|---|---|---|---|---|---|
| 16 | `GeoGap` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Tile gap |
| 16 | `GeoHeight` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Extrusion height |
| 16 | `GeoWaveResolution` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Wave pattern resolution |
| 16 | `GeoWaveAmplitude` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Wave amplitude |
|---|---|---|---|---|---|
| 17 | `TransRotationX` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Model rotation X (radians) |
| 17 | `TransRotationY` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Model rotation Y (radians) |
| 17 | `TransScale` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Model scale multiplier |
| 17 | `TransReserved` | EFFECT PARAMS (Slots 0-23) | Debug/Reserved | never | Reserved |
|---|---|---|---|---|---|
| 18 | `LightDiffuse` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Diffuse light strength |
| 18 | `LightAmbient` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Ambient light strength |
| 18 | `LightBackLight` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Back rim light |
| 18 | `LightFresnel` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Fresnel rim strength |
|---|---|---|---|---|---|
| 19 | `TimingSceneDuration` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Animation cycle length |
| 19 | `TimingCrossfade` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Transition blend time |
| 19 | `TimingLoopMode` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Loop mode (0-3) |
| 19 | `TimingAnimFrequency` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Pattern frequency |
|---|---|---|---|---|---|
| 20 | `Blackout` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0-1 screen darkening |
| 20 | `VignetteAmount` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0-1 edge darkening |
| 20 | `VignetteRadius` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? | 0.2-1.0 vignette size |
| 20 | `TintAmount` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0-1 color tint strength |
|---|---|---|---|---|---|
| 21 | `DistortionStrength` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 21 | `DistortionRadius` | EFFECT PARAMS (Slots 0-23) | EffectRuntime? | per-frame? |  |
| 21 | `DistortionFrequency` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 21 | `DistortionSpeed` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 22 | `BlendReserved` | EFFECT PARAMS (Slots 0-23) | Debug/Reserved | never | Reserved |
| 22 | `BlendMode` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0=normal, 1=additive, 2=multiply, 3=screen |
| 22 | `FadeIn` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
| 22 | `FadeOut` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change |  |
|---|---|---|---|---|---|
| 23 | `Version` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | Effect version (1.0, 2.0, 3.0, etc.) |
| 23 | `RayCoronaFlags` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | bit 0 = showExternalRays, bit 1 = showCorona |
| 23 | `ColorBlendMode` | EFFECT PARAMS (Slots 0-23) | EffectConfig | on-change | 0=multiply, 1=additive, 2=replace, 3=mix |
| 23 | `EruptionContrast` | EFFECT PARAMS (Slots 0-23) | EffectConfig | per-frame | V7: Ray discreteness (2.0=default, higher=discrete eruptions) |
|---|---|---|---|---|---|
| 24 | `V2CoronaStart` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Where glow begins (0.15) |
| 24 | `V2CoronaBrightness` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Corona intensity multiplier (0.15) |
| 24 | `V2CoreRadiusScale` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectRuntime? | per-frame? | Core size multiplier (0.1) |
| 24 | `V2CoreMaskRadius` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectRuntime? | per-frame? | Core cutoff radius (0.35) |
|---|---|---|---|---|---|
| 25 | `V2CoreSpread` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Core glow spread multiplier (1.0) |
| 25 | `V2CoreGlow` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Core glow intensity (1.0) |
| 25 | `V2CoreMaskSoft` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Core edge softness (0.05) |
| 25 | `V2EdgeRadius` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectRuntime? | per-frame? | Edge ring position (0.3) |
|---|---|---|---|---|---|
| 26 | `V2EdgeSpread` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Ring spread multiplier (1.0) |
| 26 | `V2EdgeGlow` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Ring glow intensity (1.0) |
| 26 | `V2SharpScale` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Sharpness divisor (4.0) |
| 26 | `V2LinesUVScale` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Pattern UV scale (3.0) |
|---|---|---|---|---|---|
| 27 | `V2LinesDensityMult` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Layer 2 density multiplier (1.6) |
| 27 | `V2LinesContrast1` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Layer 1 power exponent (2.5) |
| 27 | `V2LinesContrast2` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Layer 2 power exponent (3.0) |
| 27 | `V2LinesMaskRadius` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectRuntime? | per-frame? | Pattern cutoff radius (0.3) |
|---|---|---|---|---|---|
| 28 | `V2LinesMaskSoft` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Pattern edge softness (0.02) |
| 28 | `V2RayRotSpeed` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Ray rotation speed (0.3) |
| 28 | `V2RayStartRadius` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectRuntime? | per-frame? | Ray origin radius (0.32) |
| 28 | `V2AlphaScale` | V2/V3 DETAIL PARAMS (Slots 24-28) | EffectConfig | on-change | Output alpha multiplier (0.5) |
|---|---|---|---|---|---|
| 29 | `CameraX` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 29 | `CameraY` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 29 | `CameraZ` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 29 | `Time` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Frame** | per-frame | Global animation time |
|---|---|---|---|---|---|
| 30 | `ForwardX` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 30 | `ForwardY` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 30 | `ForwardZ` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 30 | `AspectRatio` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | on-change |  |
|---|---|---|---|---|---|
| 31 | `UpX` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 31 | `UpY` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 31 | `UpZ` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | per-frame |  |
| 31 | `Fov` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | on-change | Field of view in radians |
|---|---|---|---|---|---|
| 32 | `NearPlane` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | on-change |  |
| 32 | `FarPlane` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | on-change |  |
| 32 | `RenderReserved` | CAMERA/RUNTIME PARAMS (Slots 29-32) | Debug/Reserved | never |  |
| 32 | `IsFlying` | CAMERA/RUNTIME PARAMS (Slots 29-32) | **Camera** | on-change | 1.0 if player is flying, else 0.0 |
|---|---|---|---|---|---|
| 33 | `InvViewProj` | MATRICES (Slots 33-40) | **Camera** | per-frame | (4 slots)  |
|---|---|---|---|---|---|
| 37 | `ViewProj` | MATRICES (Slots 33-40) | **Camera** | per-frame | (4 slots)  |
|---|---|---|---|---|---|
| 41 | `CamMode` | DEBUG (Slot 41) | EffectConfig | on-change |  |
| 41 | `DebugMode` | DEBUG (Slot 41) | Debug/Reserved | on-change |  |
| 41 | `DebugReserved1` | DEBUG (Slot 41) | Debug/Reserved | never |  |
| 41 | `DebugReserved2` | DEBUG (Slot 41) | Debug/Reserved | never |  |
|---|---|---|---|---|---|
| 42 | `FlamesEdge` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Distance threshold (1.1) |
| 42 | `FlamesPower` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Falloff exponent (2.0) |
| 42 | `FlamesMult` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Brightness multiplier (50.0) |
| 42 | `FlamesTimeScale` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Time modulation (1.2) |
|---|---|---|---|---|---|
| 43 | `FlamesInsideFalloff` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Inside sphere falloff (24.0) |
| 43 | `SurfaceNoiseScale` | PULSAR V5+ PARAMS (Slots 42-43) | EffectConfig | on-change | Surface procedural texture scale (5.0) |
| 43 | `FlamesReserved1` | PULSAR V5+ PARAMS (Slots 42-43) | Debug/Reserved | never | Reserved |
| 43 | `FlamesReserved2` | PULSAR V5+ PARAMS (Slots 42-43) | Debug/Reserved | never | Reserved |
|---|---|---|---|---|---|
| 44 | `GeoAnimMode` | GEODESIC ANIMATION PARAMS (Slot 44) | EffectConfig | on-change | Animation mode: 0.0=static, 0.1=wave, 0.2=Y-wave, 0.3=gap |
| 44 | `GeoRotationSpeed` | GEODESIC ANIMATION PARAMS (Slot 44) | EffectConfig | on-change | Sphere rotation speed (rad/sec) |
| 44 | `GeoDomeClip` | GEODESIC ANIMATION PARAMS (Slot 44) | EffectConfig | on-change | Dome clip: 0=sphere, 0.5=hemisphere, 1=flat |
| 44 | `GeoAnimReserved` | GEODESIC ANIMATION PARAMS (Slot 44) | Debug/Reserved | never | Reserved |
|---|---|---|---|---|---|
| 45 | `V8PlasmaScale` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Base pattern size (1-50, default 10) |
| 45 | `V8PlasmaSpeed` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Animation speed (0-10, default 1) |
| 45 | `V8PlasmaTurbulence` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Ridged intensity (0-2, default 1) |
| 45 | `V8PlasmaIntensity` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Brightness (0-10, default 1) |
|---|---|---|---|---|---|
| 46 | `V8RingFrequency` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Number of rings (1-20, default 4) |
| 46 | `V8RingSpeed` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Expansion rate (0-20, default 10) |
| 46 | `V8RingSharpness` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Edge sharpness (0.1-10, default 3) |
| 46 | `V8RingCenterValue` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Brightness target (0-0.5, default 0.1) |
|---|---|---|---|---|---|
| 47 | `V8RingModPower` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Modulation strength (0-2, default 0.9) |
| 47 | `V8RingIntensity` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Ring brightness (0-10, default 1) |
| 47 | `V8CoreType` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Core pattern: 0=Default, 1=Electric, 2+future |
| 47 | `V8RingReserved2` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | Debug/Reserved | never | Reserved |
|---|---|---|---|---|---|
| 48 | `V8CoronaExtent` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Max extent multiplier (1-10, default 2) |
| 48 | `V8CoronaFadeStart` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Where fade begins (0-1, default 0.5) |
| 48 | `V8CoronaFadePower` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Fade curve (0.1-10, default 1) |
| 48 | `V8CoronaIntensity` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Overall brightness (0-10, default 1) |
|---|---|---|---|---|---|
| 49 | `V8ElectricFlash` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Flash effect: 0=off, 1=on scene flash |
| 49 | `V8ElectricFillIntensity` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Fill visibility: 0=minimal fill, 1=rich fill |
| 49 | `V8ElectricFillDarken` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Fill color: 0=white, 0.5=match lines, 1=black (default 0.60) |
| 49 | `V8ElectricLineWidth` | V8 ELECTRIC AURA PARAMS (Slots 45-48) | EffectConfig | on-change | Line thickness: 0=thick, 1=thin (default 0.5) |

---

## Summary by Suggested Domain

- EffectConfig: 132 parameters
- **Camera**: 16 parameters
- EffectRuntime?: 11 parameters
- Debug/Reserved: 10 parameters
- **Frame**: 1 parameters

---

## Next Steps

1. ✅ Review each parameter classification
2. ✅ Confirm update frequencies
3. ✅ Identify consolidation opportunities (e.g., X/Y/Z/W → vec4)
4. ⬜ Group by target UBO
5. ⬜ Create new UBO record definitions
6. ⬜ Update shader uniform blocks