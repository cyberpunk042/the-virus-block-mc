# God Rays Implementation Plan 📋

## Overview

Breaking the enhancement into **independent, testable pieces** that can be implemented incrementally.

---

## Phase 0: Foundation ✅ (COMPLETE)
- [x] 7-pass pipeline working
- [x] Vector-Basis Projection (orb-anchored)
- [x] Static rays visible from orb center
- [x] Basic parameters (decay, exposure, samples, threshold)

---

## Phase 1: Energy Mode ✅ (COMPLETE)

**Goal**: Toggle between radiation (OUT) and absorption (IN)

### 1.1 UBO Change
- Add `energyMode` to existing `GodRayParams` (slot 50.w is currently `samples`)
- **OR** use reserved slot in `GodRayMaskParams` (slot 51.w)

**Decision**: Repurpose `GodRayMaskParams.reserved` → `GodRayEnergyMode`

### 1.2 Shader Change (`god_rays_accum.fsh`)
```glsl
// Energy mode: 0 = radiation (outward), 1 = absorption (inward)
float energyMode = GodRayEnergyMode; // from slot 51.w

vec2 rayDir = (energyMode < 0.5) 
    ? normalize(lightUV - texCoord)  // Radiation: toward light
    : normalize(texCoord - lightUV); // Absorption: away from light
```

### 1.3 Java Change
- Add `energyMode` to `RenderConfig`
- Update `GodRayMaskParams.fromRenderConfig()` to include it
- Add UI toggle in `FieldVisualSubPanel`

### 1.4 Deliverable
- [ ] Toggle in UI: "Energy Mode: Radiation | Absorption"
- [ ] Visual: rays either flow out or flow in

**Estimated Effort**: Small (1-2 hours)

---

## Phase 2: Color Enhancement ✅ (COMPLETE)

**Goal**: More control over ray coloring

### 2.1 Options (Choose One)
| Option | Pros | Cons |
|--------|------|------|
| A. Use existing `RayColor` | No UBO change | Limited |
| B. Add gradient (2nd color) | Rich look | 1 new vec4 slot |
| C. Auto-derive from orb | Consistent | Complex sampling |

**Recommendation**: Start with **Option A** (verify current color works), then add **Option B** if needed.

### 2.2 Current Color Path
```
FieldVisualConfig.RayColorR/G/B → god_rays_composite.fsh → rayTint
```

### 2.3 Phase 2A: Verify Current Colors
- [ ] Confirm `RayColor` is being used in composite
- [ ] Test with different ray colors in presets

### 2.4 Phase 2B: Gradient (Optional)
- Add `GodRayColor2` (new slot 52 or reuse existing)
- Blend based on screen distance from center

**Estimated Effort**: Small (A), Medium (B)

---

## Phase 3: Distribution ✅ (COMPLETE)

**Goal**: Vary intensity across directions

### 3.1 Options
| Option | Implementation | Visual |
|--------|----------------|--------|
| A. Uniform (current) | No change | Symmetric |
| B. Angular Weight | `weight = f(angle)` | Biased |
| C. Noise Modulation | `illumination *= noise(angle + time)` | Organic |

**Recommendation**: Start with **Option C** - adds life without complexity

### 3.2 Implementation (Noise Modulation)
```glsl
// In accumulator loop
float angle = atan(texCoord.y - lightUV.y, texCoord.x - lightUV.x);
float noiseVal = noise1D(angle * 8.0 + Time * 0.5); // from noise_utils
illumination *= 0.5 + noiseVal * 0.5; // Modulate 50-100%
```

### 3.3 UBO Addition
- Add `GodRayNoiseScale` and `GodRayNoiseSpeed` to control

**Estimated Effort**: Medium (2-3 hours)

---

## Phase 4: Arrangement 🔲

**Goal**: Control emission source shape

### 4.1 Options
| Option | Description | Complexity |
|--------|-------------|------------|
| A. Point (current) | From center | Done |
| B. Ring | From surface | Medium |
| C. Multi-point | Multiple sources | High |

**Recommendation**: **Option B** (Ring) for realism at close range

### 4.2 Ring Source Implementation
```glsl
// Offset lightUV radially by orb screen radius
float screenRadius = /* calculate from orb radius + distance */;
vec2 toPixel = normalize(texCoord - lightUV);
vec2 surfaceUV = lightUV + toPixel * screenRadius;
```

### 4.3 Challenges
- Need orb screen radius (requires projection of `Radius` to screen space)
- May need per-pixel adjustment

**Estimated Effort**: High (4-6 hours)

---

## Phase 5: Polish & Integration 🎛️

### 5.1 UI
- [ ] Add all new controls to `FieldVisualSubPanel`
- [ ] Group under "God Rays" section
- [ ] Add presets: "Subtle", "Dramatic", "Dark Absorption"

### 5.2 Presets
- [ ] Update field appearance JSONs with god ray configs
- [ ] Test with all orb versions (v5, v6, v7, v8)

### 5.3 Performance
- [ ] Profile at different sample counts
- [ ] Consider half-res option (currently mentioned but need to verify)

---

## Recommended Order

```
Phase 1 → Phase 2A → Phase 3 → Phase 2B → Phase 4 → Phase 5
   ↓          ↓          ↓          ↓          ↓         ↓
Energy    Verify     Add Noise  Gradient   Ring     Polish
Mode      Colors     Variation  (Optional) Source   & UI
```

---

## Summary Table

| Phase | Feature | Priority | Effort | Dependencies |
|-------|---------|----------|--------|--------------|
| 1 | Energy Mode | P0 | Small | None |
| 2A | Verify Colors | P1 | Tiny | None |
| 2B | Color Gradient | P3 | Medium | 2A |
| 3 | Noise Distribution | P1 | Medium | None |
| 4 | Ring Arrangement | P2 | High | None |
| 5 | Polish & UI | P2 | Medium | All |

---

## Next Action

**Start with Phase 1: Energy Mode** - it's the quickest win with the biggest visual impact.

Shall I begin?

---
*Created: 2026-01-11*
