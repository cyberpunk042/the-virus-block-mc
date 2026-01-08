# Magic Circle - Parameter Sweep & Architecture

## Reference Code Analysis

### Primitive SDF Functions (from Shadertoy)

#### 1. `circle(pre, p, r1, r2, power)` 
**Purpose:** Draws ring shapes with optional fill
```glsl
float circle(float pre, vec2 p, float r1, float r2, float power) {
    float leng = length(p);
    float d = min(abs(leng-r1), abs(leng-r2));
    if (r1<leng && leng<r2) pre /= exp(d)/r2;  // Fill behavior
    float res = power / d;
    return clamp(pre + res, 0.0, 1.0);
}
```

**Parameters to expose:**
| Param | Description | Range | Default |
|-------|-------------|-------|---------|
| r1 | Inner radius | 0.0-1.0 | varies |
| r2 | Outer radius | 0.0-1.0 | varies |
| power | Glow intensity/thickness | 0.001-0.05 | 0.006 |

**Variants in original:**
- Thin ring: `r1 ≈ r2` (e.g., 0.85, 0.9)
- Point circle: `r1 = r2 = 0.001` (just a dot)
- Small decorative: `r` = 0.05

---

#### 2. `rectangle(pre, p, half1, half2, power)`
**Purpose:** Rectangular outline with two corner sizes
```glsl
float rectangle(float pre, vec2 p, vec2 half1, vec2 half2, float power) {
    p = abs(p);
    if ((half1.x<p.x || half1.y<p.y) && (p.x<half2.x && p.y<half2.y)) {
        pre = max(0.01, pre);  // Fill behavior
    }
    // ... complex distance calculation
    return clamp(pre + power/d, 0.0, 1.0);
}
```

**Parameters to expose:**
| Param | Description | Range | Default |
|-------|-------------|-------|---------|
| half1 | Inner corner (x,y) | 0.0-1.0 | varies |
| half2 | Outer corner (x,y) | 0.0-1.0 | varies |
| power | Line thickness | 0.001-0.01 | 0.0015 |

**Usage in original:**
- Hexagram: `half = (0.85/√2, 0.85/√2)` rotated 6 times
- Triangle: `half = (0.36, 0.36)` rotated 3 times

---

#### 3. `radiation(pre, p, r1, r2, num, power)`
**Purpose:** Radial line pattern (spokes)
```glsl
float radiation(float pre, vec2 p, float r1, float r2, int num, float power) {
    float angle = 2.0*PI/float(num);
    float d = 1e10;
    for(int i=0; i<num; i++) {
        float _d = (r1<p.y && p.y<r2) ? 
            abs(p.x) : 
            min(length(p-vec2(0.0, r1)), length(p-vec2(0.0, r2)));
        d = min(d, _d);
        p = rotate(p, angle);
    }
    return clamp(pre + power/d, 0.0, 1.0);
}
```

**Parameters to expose:**
| Param | Description | Range | Default |
|-------|-------------|-------|---------|
| r1 | Inner radius | 0.0-1.0 | 0.25 |
| r2 | Outer radius | 0.0-1.0 | 0.30 |
| num | Number of lines | 3-72 | 12/36 |
| power | Line thickness | 0.0001-0.01 | 0.0008 |

---

### Layers in Original Magic Circle

Analyzing `calc(vec2 p)`:

| Layer ID | Type | Description | Rotation | Elements |
|----------|------|-------------|----------|----------|
| 1 | ring | Outer ring (0.85-0.9) | +time | 1 ring |
| 2 | radiation | Outer spokes (0.87-0.88) | +time | 36 lines |
| 3 | rectangle | Hexagram | +time | 6 rects |
| 4 | circles | Outer dots (r=0.875) | +time | 12 dots |
| 5 | ring | Middle ring (0.5-0.55) | static | 1 ring |
| 6 | rectangle | Triangle | -time | 3 rects |
| 7 | circles | Inner dots (r=0.53) | -time | 12 dots |
| 8 | radiation | Inner spokes (0.25-0.3) | +time | 12 lines |
| 9 | nested circles | Spinning core | -time + orbit | 6 + center |

---

## Parameter Categories

### Global Settings
| Parameter | Type | Range | Default | Description |
|-----------|------|-------|---------|-------------|
| `effectRadius` | float | 1-500 | 10.0 | World-space radius (blocks) |
| `heightTolerance` | float | 0.1-10 | 1.0 | Y-axis render tolerance |
| `globalIntensity` | float | 0.1-5.0 | 1.0 | Master brightness |
| `globalRotationSpeed` | float | 0.0-5.0 | 1.0 | Base rotation multiplier |
| `breathingSpeed` | float | 0.0-2.0 | 1.0 | Scale pulsing speed |
| `breathingAmount` | float | 0.0-0.2 | 0.02 | Scale pulse amplitude |
| `linePower` | float | 0.5-5.0 | 1.0 | Global line thickness mult |

### Colors
| Parameter | Type | Range | Default | Description |
|-----------|------|-------|---------|-------------|
| `primaryR/G/B` | float | 0-1 | 1.0, 0.95, 0.8 | Main glow color |
| `secondaryR/G/B` | float | 0-1 | 0.8, 0.7, 0.5 | Accent color |
| `coreR/G/B` | float | 0-1 | 1.0, 1.0, 0.9 | Center color |
| `glowExponent` | float | 1.0-5.0 | 2.5 | `pow(dst, exponent)` |

### Layer Toggles (per-layer enable)
| Layer | Toggle | Intensity | Speed Mult |
|-------|--------|-----------|------------|
| outerRing | bool | float 0-1 | float -2 to 2 |
| outerRadiation | bool | float 0-1 | float -2 to 2 |
| hexagram | bool | float 0-1 | float -2 to 2 |
| outerDots | bool | float 0-1 | float -2 to 2 |
| middleRing | bool | float 0-1 | float -2 to 2 |
| innerTriangle | bool | float 0-1 | float -2 to 2 |
| innerDots | bool | float 0-1 | float -2 to 2 |
| innerRadiation | bool | float 0-1 | float -2 to 2 |
| spinningCore | bool | float 0-1 | float -2 to 2 |

Per layer (9 layers × 3 params = 27 layer params).

### Layer-Specific Geometry (Advanced)

For maximum customization, each layer can have:

#### Ring Layers (outer, middle)
| Param | Description |
|-------|-------------|
| innerRadius | 0.0-1.0 |
| outerRadius | 0.0-1.0 |
| thickness (power) | 0.001-0.02 |

#### Radiation Layers (outer spokes, inner spokes)
| Param | Description |
|-------|-------------|
| innerRadius | 0.0-1.0 |
| outerRadius | 0.0-1.0 |
| lineCount | 3-72 |
| thickness (power) | 0.0001-0.01 |

#### Rectangle Layers (hexagram, triangle)
| Param | Description |
|-------|-------------|
| sideCount | 3-12 (how many rotated copies) |
| cornerSize | 0.1-1.0 |
| thickness (power) | 0.001-0.01 |

#### Dot Circle Layers (outer dots, inner dots)
| Param | Description |
|-------|-------------|
| orbitRadius | 0.0-1.0 |
| dotCount | 4-36 |
| dotInnerR | 0.001-0.05 |
| dotOuterR | 0.01-0.1 |
| dotPower | 0.001-0.02 |

#### Spinning Core
| Param | Description |
|-------|-------------|
| orbitRadius | 0.0-0.3 |
| circleCount | 1-10 |
| startRadius | 0.05-0.2 |
| radiusStep | 0.005-0.02 |
| orbitSpeed | 0.0-2.0 |

---

## UBO Layout Estimate

Minimum for V1 (basic customization):
- Global: 4 vec4 (position, timing, scale/intensity, reserved)
- Colors: 3 vec4 (primary, secondary, core + exponent)
- Layer toggles: 3 vec4 (9 enables + 9 intensities + 9 speeds = 27 floats → 7 vec4)
- **Total: ~14 vec4 = 224 bytes**

Full customization (per-layer geometry):
- Above + per-layer radii/counts
- Could exceed 80 vec4 (1280 bytes)
- **Recommend: Start with presets, expose key params**

---

## Architecture (Following Shockwave Pattern)

### Files to Create

```
src/client/java/net/cyberpunk042/client/
├── gui/
│   ├── panel/sub/
│   │   └── MagicCircleSubPanel.java          # UI panel (like ShockwaveSubPanel)
│   └── state/adapter/
│       └── MagicCircleAdapter.java           # State adapter
│       └── MagicCircleConfig.java            # Config record
├── visual/shader/
│   └── MagicCirclePostEffect.java            # Post-effect controller

src/main/resources/assets/the-virus-block/
├── shaders/post/
│   ├── magic_circle.fsh                      # Main shader
│   └── include/effects/
│       └── magic_circle.glsl                 # Pattern library
├── post_effect/
│   └── magic_circle.json                     # Pipeline definition
```

### Java Classes

#### MagicCircleConfig.java
Record with all parameters + defaults:
```java
public record MagicCircleConfig(
    // Global
    float effectRadius,
    float heightTolerance,
    float intensity,
    float rotationSpeed,
    float breathingSpeed,
    float breathingAmount,
    
    // Colors
    float primaryR, primaryG, primaryB,
    float secondaryR, secondaryG, secondaryB,
    float coreR, coreG, coreB,
    float glowExponent,
    
    // Layer enables (bitfield or individual)
    int layerEnables,  // bit flags for 9 layers
    
    // Layer intensities (could be vec4s)
    float[] layerIntensities,  // 9 floats
    float[] layerSpeeds,       // 9 floats
    
    // ... detailed per-layer params for V2
) {
    public static final MagicCircleConfig DEFAULT = new MagicCircleConfig(...);
}
```

---

## V1 Implementation Scope

**Phase 1: Core pattern rendering**
- Single magic circle shader
- Global scale, intensity, rotation speed
- Primary color only
- All layers enabled with fixed geometry

**Phase 2: Layer customization**
- Per-layer enable toggles
- Per-layer intensity
- Per-layer rotation speed/direction

**Phase 3: Geometry customization**
- Per-layer radius controls
- Element count controls
- Thickness controls

**Phase 4: Multiple patterns**
- Different magic circle designs (runic, elemental, etc.)
- Pattern selector in UI

---

## Next Steps

### Phase 1 - Core Implementation ✅ COMPLETE

1. [x] Create `MagicCircleConfig.java` record with V1 params
2. [x] Create `MagicCircleAdapter.java` state adapter
3. [x] Create `magic_circle.glsl` with adapted primitives
4. [x] Create `magic_circle.fsh` standalone shader
5. [x] Create `magic_circle.json` pipeline
6. [x] Create `MagicCirclePostEffect.java` controller
7. [x] Create `MagicCircleSubPanel.java` UI
8. [x] Register adapter in FieldEditState
9. [x] Add to NavigationPanel tab list (ContentArea FX tabs)
10. [x] Wire up in WorldRenderer mixin
11. [x] Register mixin in config

### Phase 2 - Enhancement ✅ COMPLETE

1. [x] Add per-layer enable toggles
2. [x] Add per-layer intensity controls  
3. [x] Add per-layer rotation speed controls
4. [x] Update UBO to include layer params (16 vec4)
5. [x] Update shader to consume layer params
6. [x] Compact multi-column UI layout
7. [x] Quick action buttons (All On/Off, Reset I/S)
8. [x] JSON persistence for layer config

### Phase 3 - Geometry Customization

#### Phase 3A - Simple Layers ✅ COMPLETE
- [x] Layer 4 (Middle Ring): innerRadius, outerRadius, thickness, rotOffset
- [x] Layer 7 (Inner Radiation): innerRadius, outerRadius, spokeCount, thickness, rotOffset
- [x] UBO expansion (16 → 19 vec4)
- [x] Shader structs (Layer4Params, Layer7Params)
- [x] renderMagicCirclePhase3A function
- [x] UI panel geometry controls
- [x] JSON persistence
- [x] Reset defaults buttons

#### Phase 3B - Shape Layers ✅ COMPLETE
- [x] Layer 2 (Hexagram): rectCount, rectSize, thickness, rotOffset, snapRotation
- [x] Layer 5 (Inner Triangle): same structure as Layer 2
- [x] UBO expansion (19 → 23 vec4)
- [x] Shader structs (Layer2Params, Layer5Params)
- [x] renderMagicCirclePhase3B function
- [x] UI panel geometry controls
- [x] JSON persistence
- [x] Snap rotation toggles + reset buttons

#### Phase 3C - Dot Ring Layers ✅ COMPLETE
- [x] Layer 3 (Outer Dot Ring): dotCount, orbitRadius, ringInner/Outer, ringThickness, dotRadius, dotThickness, rotOffset
- [x] Layer 6 (Inner Dot Ring): same structure as Layer 3, different defaults
- [x] UBO expansion (23 → 27 vec4)
- [x] Shader structs (Layer3Params, Layer6Params)
- [x] renderMagicCirclePhase3C function
- [x] UI panel with 8 sliders per layer + reset buttons
- [x] JSON persistence (8 properties per layer)

#### Phase 3D - Complex Layers ✅ COMPLETE
- [x] Layer 1 (Outer Ring + Radiation): ringInner/Outer, ringThickness, radInner/Outer, radCount, radThickness, rotOffset (8 params)
- [x] Layer 8 (Spinning Core): breathAmp, breathCenter, orbitalCount/Start/Step/Dist/Thickness, centerRadius/Thickness, rotOffset (10 params)
- [x] UBO expansion (27 → 32 vec4, 512 bytes FINAL)
- [x] Shader structs (Layer1Params, Layer8Params)
- [x] renderMagicCircleFinal function with ALL layer geometry
- [x] UI panel with controls for both layers + reset buttons
- [x] JSON persistence

### 🎉 PHASE 3 COMPLETE - ALL LAYERS CUSTOMIZABLE! 🎉

---

## 📚 CRITICAL: Reference Documentation

> ⚠️ **IMPORTANT**: When implementing Phase 2+, **MUST** consult these detailed parameter files.
> Each layer has extensive documentation about exposed parameters, ranges, UBO packing, and defaults.
> **Do NOT rely on memory/context compression - read the actual files!**

### Layer Parameter Files (`.agent/artifacts/`)

| File | Layer | Description |
|------|-------|-------------|
| `magic_circle_layer1_params.md` | Layer 1 | **Outer Ring + Outer Radiation** - Ring radii, spoke count, thickness |
| `magic_circle_layer2_params.md` | Layer 2 | **Hexagram** - 6 rotated rectangles, corner sizes |
| `magic_circle_layer3_params.md` | Layer 3 | **Outer Dot Ring** - 12 dots, orbit radius, dot size |
| `magic_circle_layer4_params.md` | Layer 4 | **Middle Ring** - Static ring, inner/outer radius |
| `magic_circle_layer5_params.md` | Layer 5 | **Inner Triangle** - 3 rotated rectangles, -time rotation |
| `magic_circle_layer6_params.md` | Layer 6 | **Inner Dot Ring** - 12 dots, -time rotation |
| `magic_circle_layer7_params.md` | Layer 7 | **Inner Radiation** - 12 spokes, inner radii |
| `magic_circle_layer8_params.md` | Layer 8 | **Spinning Core** - Nested orbiting circles |

### Summary/Overview

| File | Purpose |
|------|---------|
| `magic_circle_params_summary.md` | **Consolidated summary** - All 104 parameters, UBO estimates, implementation strategy |
| `magic_circle_implementation_plan.md` | **This file** - Roadmap and architecture |

### Parameter Count per Layer (from detailed analysis)

| Layer | # Params | Key Controls |
|-------|----------|--------------|
| 1 | ~15 | Ring r1/r2, spoke count, power |
| 2 | ~10 | Rectangle corners, count, power |
| 3 | ~12 | Dot orbit radius, count, size, power |
| 4 | ~8 | Ring r1/r2, power |
| 5 | ~10 | Rectangle corners, count, power |
| 6 | ~12 | Dot orbit radius, count, size, power |
| 7 | ~10 | Spoke count, inner/outer radius, power |
| 8 | ~15 | Orbit radius, circle count, step, speed |
| **Global** | ~12 | Position, intensity, rotation, colors |
| **TOTAL** | **~104** | See `magic_circle_params_summary.md` |

### Shader Files (for reference during implementation)

| File | Location |
|------|----------|
| `magic_circle.glsl` | `shaders/post/include/effects/` |
| `magic_circle.fsh` | `shaders/post/` |
| `magic_circle.json` | `post_effect/` |

---

## Implementation Notes

### UBO Sizing (from params_summary.md)
- **Phase 1 (current)**: ~6 vec4 (minimal)
- **Phase 2 (layer toggles)**: ~14 vec4 (224 bytes)
- **Phase 3 (full geometry)**: ~27 vec4 (432 bytes)
- **Max**: Within std140 limits, no issues

### Key Design Decisions
1. **Modular layers** - Each layer is a separate function in GLSL
2. **Hardcoded defaults** - Phase 1 uses fixed geometry, parameterized later
3. **Additive blending** - All layers accumulate into final pattern
4. **Time-based rotation** - Each layer has its own rotation direction/speed

### UI/UX Guidelines
> ⚠️ **IMPORTANT**: Use compact multi-column layouts!

1. **Multi-slider rows** - Use 2 or 3 sliders per row when logical
   - RGB colors: always 3 sliders (R/G/B)
   - Related pairs: 2 sliders (e.g., inner/outer radius, min/max)
   - Single toggles can pair with related sliders
2. **No single-column waste** - Avoid one slider per row unless it truly needs the width
3. **Grouping** - Use `sliderDual()` and `sliderTriple()` from ContentBuilder
4. **Section headers** - Organize by layer or logical grouping
5. **Collapsible sections** - Consider for advanced per-layer geometry

