# Magic Circle - Complete Parameter Summary

## Global Parameters

```glsl
// Applied BEFORE all layer calculations
{
    vec2 q = p;
    p = scale(p, sin(PI*iTime/1.0)*0.02+1.1);
    // ... layers use p
}
```

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `global_effectRadius` | float | - | 10.0 | 1-500 | World-space radius (blocks) |
| `global_heightTolerance` | float | - | 1.0 | 0.1-10 | Y-axis render tolerance |
| `global_intensity` | float | - | 1.0 | 0.1-5.0 | Master brightness |
| `global_breathingEnable` | bool | true | true | - | Global scale breathing |
| `global_breathingSpeed` | float | π | 3.14 | 0.5-10 | Breathing frequency |
| `global_breathingAmount` | float | 0.02 | 0.02 | 0.0-0.1 | Breathing amplitude |
| `global_breathingCenter` | float | 1.1 | 1.1 | 0.8-1.2 | Breathing base scale |
| `global_glowExponent` | float | 2.5 | 2.5 | 1.0-5.0 | `pow(result, exp)` |

### Colors
| Parameter Name | Type | Default | Range | Description |
|----------------|------|---------|-------|-------------|
| `global_primaryR` | float | 1.0 | 0-1 | Main color R |
| `global_primaryG` | float | 0.95 | 0-1 | Main color G |
| `global_primaryB` | float | 0.8 | 0-1 | Main color B |

**Global Total: 11 parameters**

---

## Layer Parameter Counts

| Layer | Name | Parameters |
|-------|------|------------|
| 1A | Outer Ring | 6 |
| 1B | Outer Radiation | 7 |
| 1 (group) | Outer Ring Group | 3 |
| **1 subtotal** | | **16** |
| 2 | Hexagram | 9 |
| 3 | Outer Dot Ring | 13 |
| 4 | Middle Ring | 7 |
| 5 | Inner Triangle | 9 |
| 6 | Inner Dot Ring | 13 |
| 7 | Inner Radiation | 8 |
| 8 | Spinning Core | 18 |

---

## Grand Total

| Category | Count |
|----------|-------|
| Global | 11 |
| Layer 1 | 16 |
| Layer 2 | 9 |
| Layer 3 | 13 |
| Layer 4 | 7 |
| Layer 5 | 9 |
| Layer 6 | 13 |
| Layer 7 | 8 |
| Layer 8 | 18 |
| **TOTAL** | **104 parameters** |

---

## UBO Size Estimate

104 floats + bool conversion = ~27 vec4 = **432 bytes**

This fits easily within a single UBO block.

---

## Implementation Strategy

### Phase 1: Minimal (Quick Start)
Expose only:
- Global: effectRadius, intensity, primary color (5 params)
- Per-layer: enable only (8 toggles)
- **13 params** - fits in ~4 vec4

### Phase 2: Basic Customization
Add:
- Per-layer intensity (8 floats)
- Per-layer rotation speed (8 floats)
- **29 params** - fits in ~8 vec4

### Phase 3: Geometry Control
Add per-layer geometry params (radii, counts, sizes)
- **~60 params** - fits in ~16 vec4

### Phase 4: Full Customization
All 104 parameters
- **~27 vec4** - still reasonable

---

## File References

Detailed per-layer docs:
- `magic_circle_layer1_params.md` - Outer Ring + Radiation (16 params)
- `magic_circle_layer2_params.md` - Hexagram (9 params)
- `magic_circle_layer3_params.md` - Outer Dot Ring (13 params)
- `magic_circle_layer4_params.md` - Middle Ring (7 params)
- `magic_circle_layer5_params.md` - Inner Triangle (9 params)
- `magic_circle_layer6_params.md` - Inner Dot Ring (13 params)
- `magic_circle_layer7_params.md` - Inner Radiation (8 params)
- `magic_circle_layer8_params.md` - Spinning Core (18 params)

---

## Next: Implementation

1. Create `MagicCircleConfig.java` with all 104 parameters
2. Create `MagicCircleAdapter.java` with path-based get/set
3. Create `magic_circle.glsl` shader library
4. Create `magic_circle.fsh` standalone shader
5. Create `MagicCirclePostEffect.java` controller
6. Create `MagicCircleSubPanel.java` with section-based UI
