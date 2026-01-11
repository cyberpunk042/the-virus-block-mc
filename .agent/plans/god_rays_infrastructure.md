# God Rays Infrastructure Architecture 🏗️

## Philosophy

> **Infrastructure first, components second, integration third.**

Individual features (energy mode, color, noise) are underwhelming alone. Their COMBINATION creates the "wow" factor. But combinations require consistent infrastructure.

---

## Layer 0: UBO Infrastructure 📦

### Current State
```
Slot 50: GodRayParams     (enabled, decay, exposure, samples)
Slot 51: GodRayMaskParams (threshold, skyEnabled, softness, reserved)
```

### Proposed Extension
```
Slot 50: GodRayParams     (enabled, decay, exposure, samples)         ← Keep
Slot 51: GodRayMaskParams (threshold, skyEnabled, softness, energyMode) ← Use reserved
Slot 52: GodRayStyleParams (colorMode, distributionMode, arrangement, intensity)
Slot 53: GodRayColor2     (r2, g2, b2, gradientPower)
Slot 54: GodRayNoiseParams (scale, speed, angularVariation, reserved)
```

### Infrastructure Task
- [ ] Add slots 52-54 to `field_visual_base.glsl`
- [ ] Add corresponding Java records to `FieldVisualTypes.java`
- [ ] Add to `FieldVisualUBO.java`
- [ ] Add defaults to `RenderConfig.java`
- [ ] Add placeholder UI controls

**This unlocks ALL features without needing per-feature UBO changes.**

---

## Layer 1: Shader Function Library 📚

### Current
- `god_rays.glsl` - accumulator, projection

### Proposed Additions
```glsl
// god_rays_style.glsl - Style modifiers

// Energy direction
vec2 getGodRayDirection(vec2 pixelUV, vec2 lightUV, float energyMode);

// Distribution
float getAngularWeight(vec2 pixelUV, vec2 lightUV, float distributionMode);

// Noise modulation  
float getNoiseModulation(vec2 pixelUV, vec2 lightUV, float time, float scale, float speed);

// Color blending
vec3 getGodRayColor(float illumination, vec3 color1, vec3 color2, float mode, float power);
```

### Infrastructure Task
- [ ] Create `include/effects/god_rays_style.glsl`
- [ ] Define function signatures (even if stub implementations)
- [ ] Include in `god_rays_accum.fsh` and `god_rays_composite.fsh`

**This provides clean API for features without cluttering main shaders.**

---

## Layer 2: Pass Architecture 🔄

### Current (7 passes)
```
1. Effect → swap
2. Blit → main
3. Mask → god_mask
4. Accum → god_accum       ← Energy, Distribution, Noise apply here
5. BlurH → god_blur_h
6. BlurV → god_blur_v
7. Composite → main        ← Color applies here
```

### Observation
- **Accumulator (Pass 4)**: Intensity/shape modifications
- **Composite (Pass 7)**: Color modifications

This separation is GOOD - keeps concerns isolated.

### Infrastructure Task
- [ ] Verify both passes have access to new UBO slots
- [ ] Document which features apply to which pass

---

## Layer 3: Configuration Model 🎛️

### Goal
Enable "presets" that combine multiple settings:

```json
{
  "name": "Dark Absorption",
  "energyMode": 1,           // Absorption
  "colorMode": 1,            // Gradient
  "color1": [0.2, 0.0, 0.4], // Deep purple
  "color2": [0.8, 0.0, 0.2], // Crimson edge
  "distributionMode": 2,     // Noise
  "noiseScale": 4.0,
  "intensity": 1.5
}
```

### Infrastructure Task
- [ ] Define JSON schema for god ray presets
- [ ] Add preset loader to `RenderConfig`
- [ ] Add preset dropdown to UI

---

## Implementation Order

### Sprint 1: Infrastructure (Do First)
1. **UBO Extension** - Add slots 52-54 with defaults
2. **Library Stub** - Create `god_rays_style.glsl` with function signatures
3. **Pass Verification** - Ensure both Accum and Composite see new UBOs

### Sprint 2: Core Features
4. **Energy Mode** - Implement `getGodRayDirection()`
5. **Noise Distribution** - Implement `getNoiseModulation()`
6. **Color Gradient** - Implement `getGodRayColor()`

### Sprint 3: Integration
7. **UI Controls** - Wire up all parameters
8. **Presets** - Create 3-5 preset combinations
9. **Testing** - Verify all combos work together

---

## Checklist

### UBO Infrastructure ✅ COMPLETE
- [x] GLSL: Add GodRayStyleParams (slot 52)
- [x] GLSL: Add GodRayColor2 (slot 53)
- [x] GLSL: Add GodRayNoiseParams (slot 54)
- [x] Java: Add records to FieldVisualTypes.java
- [x] Java: Add to FieldVisualUBO.java
- [ ] Java: Add to RenderConfig.java (TODO when UI needed)
- [ ] Java: Update PostEffectPassMixin if needed

### Shader Library ✅ COMPLETE
- [x] Create god_rays_style.glsl
- [x] Impl: getGodRayDirection()
- [x] Impl: getAngularWeight()
- [x] Impl: getNoiseModulation()
- [x] Impl: getGodRayColor()
- [x] Impl: getArrangedLightUV()
- [ ] Include in god_rays_accum.fsh
- [ ] Include in god_rays_composite.fsh

### Integration
- [ ] UI: Add controls to FieldVisualSubPanel
- [ ] Presets: JSON schema
- [ ] Presets: 3-5 initial presets

---

## Estimated Effort

| Layer | Task | Hours |
|-------|------|-------|
| 0 | UBO Infrastructure | 2-3 |
| 1 | Shader Library Stubs | 1-2 |
| 2 | Pass Verification | 0.5 |
| 3 | Config Model | 1-2 |
| **Total Infrastructure** | | **4-7** |

After infrastructure: Each feature is 1-2 hours.

---

## Next Action

**Start with Layer 0: UBO Infrastructure**

Add the three new vec4 slots with sensible defaults. This unlocks everything else without breaking current functionality.

Ready to begin?

---
*Created: 2026-01-11*
*Status: INFRASTRUCTURE PLANNING*
