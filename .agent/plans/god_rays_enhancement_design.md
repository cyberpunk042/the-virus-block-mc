# God Rays Enhancement Design 🌟

## Current State (2026-01-11)
✅ Static god rays working from orb center via Vector-Basis Projection
⚠️ Basic functionality only - needs enhancement for production quality

---

## Enhancement Areas

### 1. Color Choice 🎨

**Current**: God rays use `RayColorR/G/B` from `FieldVisualConfig` (already implemented in `god_rays_composite.fsh`)

**Options to Consider**:
| Mode | Description | Use Case |
|------|-------------|----------|
| **Solid Tint** | Single color multiplied | Simple, predictable |
| **Gradient Tint** | Color shifts center→edge | More organic feel |
| **Temperature Shift** | Warm core → cool edges | Realistic light scattering |
| **Orb-Derived** | Auto-sample from orb colors (Primary/Secondary/Tertiary) | Consistent with effect |

**Questions**:
- Should ray color be independent param or derived from orb palette?
- Do we need per-ray color variation?

---

### 2. Distribution 📊

**Current**: Uniform radial blur in all directions

**Options to Consider**:
| Mode | Description | Visual |
|------|-------------|--------|
| **Uniform** | Equal in all directions | ○ |
| **Weighted** | Stronger in certain directions | Bias toward up/down |
| **Noise-Modulated** | Procedural variation | Organic, chaotic |
| **Discrete Rays** | Distinct beams, not continuous | Star-like |

**Implementation Ideas**:
- Multiply accumulation by angular weight function
- Use noise to modulate step size or decay
- Discrete: Use fewer samples with gaps

---

### 3. Arrangement 🔲

**Current**: Simple radial from point source

**Options to Consider**:
| Mode | Description |
|------|-------------|
| **Point Source** | All rays from center (current) |
| **Ring Source** | Rays from orb surface (center + radius) |
| **Multi-Point** | Multiple emission points (for complex orbs) |
| **Sector Mask** | Block certain angular sectors |

**Implementation Ideas**:
- Ring source: Offset `lightUV` by radius in screen space
- Sector mask: Angular mask in accumulator

---

### 4. Energy Mode 🔄

**Current**: Radiation (outward from orb)

**Options**:
| Mode | Description | Ray Direction |
|------|-------------|---------------|
| **Radiation** | Energy flows OUT | orb → screen edges |
| **Absorption** | Energy flows IN | screen edges → orb |
| **Pulsing** | Alternates | Animated blend |

**Implementation**:
```glsl
// Radiation (current): march FROM pixel TOWARD light
vec2 step = normalize(lightUV - texCoord) * stepSize;

// Absorption: march FROM light TOWARD pixel (reverse)
vec2 step = normalize(texCoord - lightUV) * stepSize;
```

---

## Proposed UBO Extensions

Add to `FieldVisualConfig` (or new `GodRayStyleParams`):

```
vec4 52: GodRayStyle
  .x = energyMode      (0=radiation, 1=absorption, 2=pulse)
  .y = distributionMode (0=uniform, 1=weighted, 2=noise)
  .z = arrangementMode  (0=point, 1=ring, 2=sector)
  .w = reserved

vec4 53: GodRayColor2
  .xyz = secondary color (for gradient mode)
  .w = colorMode (0=solid, 1=gradient, 2=temperature)
```

---

## Implementation Priority

| Priority | Feature | Complexity | Impact |
|----------|---------|------------|--------|
| P1 | Energy Mode (Absorption) | Low | High - dramatic visual difference |
| P2 | Color Gradient | Medium | Medium - polish |
| P3 | Noise Distribution | Medium | Medium - organic feel |
| P4 | Ring Source | High | Medium - closer realism |

---

## Questions for You

1. **Energy Mode**: Do you want absorption mode now? (quick win)
2. **Color**: Should rays auto-derive from orb colors or be independent?
3. **Distribution**: Do you want uniform or some variation?
4. **UBO**: Add new slots (52-53) or reuse existing reserved slots?

---
*Created: 2026-01-11*
