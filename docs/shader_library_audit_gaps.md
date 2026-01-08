# Shader Library Audit: New vs Original

## STATUS UPDATE: 2026-01-03 19:15

**After faithful re-extraction, most items are now COMPLETE.**

---

## Audit Summary (UPDATED)

| File | Status | Notes |
|------|--------|-------|
| energy_orb_v1.glsl | ✅ FIXED | Faithfully re-extracted from field_visual.fsh |
| energy_orb_types.glsl | ✅ FIXED | Now includes FieldData struct matching original |
| orbitals_v1.glsl | ✅ FIXED | Now includes BeamWidthAbs/Scale/Taper logic |
| orbitals_v2.glsl | ✅ NEW | No original - new V2 implementation |
| shockwave_ring.glsl | ⚠️ PARTIAL | Based on glow_utils - needs verification |

---

## What Was Fixed

### 1. sdf/primitives.glsl
- Added `sdCircle(p, radius)` - origin-centered alias
- Added `sdLine(p, a, b, thickness)` - alias for field_visual.fsh compatibility

### 2. rendering/screen_effects.glsl
- **FIXED** `glowFalloff()` - now matches glow_utils.glsl exactly:
  ```glsl
  float glowFalloff(float dist, float radius) {
      float t = dist / radius;
      return max(0.0, 1.0 - t * t);  // Quadratic falloff
  }
  ```
- Added `glowInverse()` for Shadertoy-style 1/(1+dist) falloff
- Added `edgeFalloff()` from field_visual.fsh:
  ```glsl
  float edgeFalloff(float dist, float thickness, float sharpness)
  ```

### 3. effects/energy_orb_types.glsl
- Added `FieldData` struct matching field_visual.fsh exactly
- Added `buildFieldData()` constructor
- Added `configToFieldData()` converter
- Kept `EnergyOrbConfig` for library convenience

### 4. effects/energy_orb_v1.glsl - COMPLETE REWRITE
**Now faithfully extracted from field_visual.fsh:**

| Function | Lines | Status |
|----------|-------|--------|
| `calcEnergyOrbNormal` | 452-461 | ✅ EXACT |
| `renderEnergyOrbCore` | 291-303 | ✅ EXACT - uses glowFalloff, pow(0.5), * 0.9 |
| `renderEnergyOrbEdge` | 309-321 | ✅ EXACT - uses edgeFalloff |
| `renderEnergyOrbSpirals` | 327-374 | ✅ EXACT - voronoi, twirlUV, rotate2D |
| `renderEnergyOrbGlowLines` | 380-446 | ✅ EXACT - sdLine, random phases |
| `raymarchEnergyOrb` | 463-499 | ✅ EXACT - coronaWidth passed |
| `renderEnergyOrbRaymarched` | 505-625 | ✅ EXACT - all logic preserved |

### 5. effects/orbitals_v1.glsl - COMPLETE REWRITE
**Now faithfully extracted from raymarching.glsl:**

- `sdfOrbitalSpheresOnly` - EXACT
- `sdfBeamsWithUniforms` - NOW includes BeamWidthAbs/Scale/Taper logic
- `sdfOrbitalAndBeamsWithUniforms` - EXACT
- `calcOrbitalNormal` - EXACT
- `raymarchOrbitalSpheres` - EXACT with all parameters

---

## Remaining Items

### shockwave_ring.glsl
Needs verification against:
- `glow_utils.glsl` - ringContribution function
- `orbital_math.glsl` - orbital distance functions

**Current status:** Uses correct functions but may need parameter adjustment.

---

## Action Items (UPDATED)

### Completed:
- [x] Re-extract `renderEnergyOrbCore` exactly
- [x] Re-extract `renderEnergyOrbEdge` exactly  
- [x] Re-extract `renderEnergyOrbSpirals` exactly (with voronoi!)
- [x] Re-extract `renderEnergyOrbGlowLines` exactly (with sdLine!)
- [x] Re-extract `renderEnergyOrbRaymarched` exactly
- [x] Add missing 2D SDFs to sdf/primitives.glsl
- [x] Add `glowFalloff` and `edgeFalloff` to rendering
- [x] Fix orbital beam width logic (BeamWidthAbs/Scale/Taper)
- [x] Add per-domain README files

### Remaining:
- [ ] Verify shockwave_ring.glsl against original
- [ ] Test in-game to verify visual parity
- [ ] Integration test: field_visual.fsh using new library
- [ ] Remove dead code: projection_modes.glsl

---

## File Size Comparison

| File | Before (simplified) | After (faithful) |
|------|---------------------|------------------|
| energy_orb_v1.glsl | ~200 lines | ~320 lines |
| energy_orb_types.glsl | ~130 lines | ~210 lines |
| orbitals_v1.glsl | ~220 lines | ~270 lines |
| sdf/primitives.glsl | 189 lines | ~210 lines |
| rendering/screen_effects.glsl | 227 lines | ~250 lines |

**Total library:** ~3,800 lines across 20+ files
