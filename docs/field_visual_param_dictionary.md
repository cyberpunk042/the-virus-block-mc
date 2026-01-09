# Field Visual Parameter Dictionary

> **Purpose:** Define the semantic meaning of each parameter to prevent drift.
> **Rule:** Before adding a new parameter, check if an existing one serves the purpose.

---

## Position & Scale

| Parameter | Type | Range | Meaning |
|-----------|------|-------|---------|
| CenterX/Y/Z | float | world coords | Field center position (camera-relative in UBO) |
| Radius | float | 0.1–100 blocks | Outer boundary of effect sphere |

---

## Colors (5 Palette Slots)

| Parameter | Role | Typical Use |
|-----------|------|-------------|
| PrimaryColor | Core/Main | Brightest inner color |
| SecondaryColor | Edge/Accent | Transition at boundaries |
| TertiaryColor | Outer Glow | Subtle outer atmosphere |
| HighlightColor | Specular | Bright hot spots |
| RayColor | Corona/Rays | External emanations |

**Convention:** RGBA floats (0–1 each), A typically = 1.0

---

## Animation

| Parameter | Range | Meaning |
|-----------|-------|---------|
| Phase | 0–2π | Animation phase offset (radians) |
| AnimSpeed | 0.1–5 | Global animation speed multiplier |
| Intensity | 0–2 | Overall effect intensity (1.0 = default) |
| TimeScale | 0.1–10 | Multiplier on FrameUBO.time |
| SpeedHigh | 0–10 | Fast detail layer speed |
| SpeedLow | 0–2 | Slow base movement speed |
| SpeedRay | 0–5 | Corona/ray rotation speed |
| SpeedRing | 0–3 | Pulsating ring speed |

---

## Core & Edge

| Parameter | Range | Meaning |
|-----------|-------|---------|
| CoreSize | 0–1 | Radius of brightest center (normalized to Radius) |
| EdgeSharpness | 0.1–10 | Edge transition steepness (higher = sharper) |
| CoreFalloff | 0.5–5 | Core glow falloff power exponent |
| FadePower | 0.5–5 | Distance fade exponent |
| FadeScale | 1–100 | Distance multiplier for fade calculation |
| InsideFalloffPower | 0–50 | Falloff when inside the sphere |

---

## Noise

| Parameter | Range | Meaning |
|-----------|-------|---------|
| NoiseResLow | 1–20 | Low-frequency noise resolution (larger = fewer features) |
| NoiseResHigh | 5–100 | High-frequency noise resolution |
| NoiseAmplitude | 0–2 | Noise strength multiplier |
| NoiseSeed | any | Variation seed (different seed = different pattern) |
| NoiseBaseScale | 0.1–10 | Base octave scale |
| NoiseScaleMultiplier | 1–3 | Per-octave scale increase |
| NoiseOctaves | 1–8 | FBM octave count (perf impact) |
| NoiseBaseLevel | 0–1 | Minimum noise floor |

---

## Corona & Rays

| Parameter | Range | Meaning |
|-----------|-------|---------|
| CoronaWidth | 0.1–2 | Extent of corona beyond radius |
| CoronaPower | 0.5–5 | Corona falloff exponent |
| CoronaMultiplier | 0–10 | Corona brightness multiplier |
| RingPower | 0.5–5 | Pulsating ring intensity exponent |
| RayPower | 0.5–5 | Ray brightness exponent |
| RaySharpness | 0.1–10 | Ray edge definition |
| GlowLineCount | 0–32 | Number of radial glow lines |
| GlowLineIntensity | 0–2 | Glow line brightness |

---

## Geometry (Geodesic)

| Parameter | Range | Meaning |
|-----------|-------|---------|
| GeoSubdivisions | 1–6 | Tile density (higher = more triangles) |
| GeoRoundTop | 0–1 | Top edge rounding amount |
| GeoRoundCorner | 0–1 | Corner rounding amount |
| GeoThickness | 0–0.5 | Shell thickness (normalized) |
| GeoGap | 0–0.5 | Gap between tiles |
| GeoHeight | 0–0.3 | Extrusion height |
| GeoDomeClip | 0–1 | 0=full sphere, 0.5=hemisphere, 1=flat |

---

## Screen Effects

| Parameter | Range | Meaning |
|-----------|-------|---------|
| Blackout | 0–1 | Screen darkening amount |
| VignetteAmount | 0–1 | Edge darkening strength |
| VignetteRadius | 0.2–1 | Vignette start position |
| TintAmount | 0–1 | Color tint overlay strength |
| DistortionStrength | 0–1 | Distortion intensity |
| DistortionRadius | 1–50 | Distortion effect radius |
| DistortionFrequency | 1–20 | Distortion pattern frequency |

---

## Blend & Compositing

| Parameter | Values | Meaning |
|-----------|--------|---------|
| BlendMode | 0–3 | 0=normal, 1=additive, 2=multiply, 3=screen |
| ColorBlendMode | 0–5 | 0=multiply, 1=additive, 2=replace, 3=mix, 4=direct, 5=subtract |
| FadeIn | 0–5 | Spawn fade-in duration (seconds) |
| FadeOut | 0–5 | Despawn fade-out duration (seconds) |

---

## Flags & Version

| Parameter | Bits/Values | Meaning |
|-----------|-------------|---------|
| EffectType | 0–N | 0=none, 1=energy_orb, 2=geodesic, 3+=future |
| Version | 1.0–9.0 | Effect version for shader selection |
| RayCoronaFlags | bits | bit0=showExternalRays, bit1=showCorona |

---

## V8 Electric Aura Specific

| Parameter | Range | Meaning |
|-----------|-------|---------|
| V8PlasmaScale | 1–50 | Electric pattern size |
| V8PlasmaSpeed | 0–10 | Electric animation speed |
| V8PlasmaTurbulence | 0–2 | Ridged noise intensity |
| V8RingFrequency | 1–20 | Number of pulsating rings |
| V8RingSpeed | 0–20 | Ring expansion rate |
| V8RingSharpness | 0.1–10 | Ring edge definition |
| V8CoronaExtent | 1–10 | Max corona extent multiplier |
| V8ElectricFlash | 0–1 | Flash effect toggle |
| V8ElectricFillDarken | 0–1 | Fill darkness (0=white, 1=black) |

---

> **Maintenance:** Add new parameters here before implementing them.
