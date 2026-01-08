# Shader Library: Current State Audit
## Date: 2026-01-03

---

## Include Files Summary

| File | Lines | Purpose |
|------|-------|---------|
| camera_core.glsl | 370 | Camera basis, ray generation, projection |
| energy_orb_v2.glsl | 415 | V2 orb screen-space rendering |
| projection_modes.glsl | 280 | Sphere projection, raymarching |
| orbital_math.glsl | 195 | Orbital position calculations |
| raymarching.glsl | 126 | Orbital/beam raymarching |
| sdf_library.glsl | 118 | SDF primitives |
| depth_utils.glsl | 49 | Depth linearization |
| glow_utils.glsl | 27 | Ring contribution |
| **Total** | **1,580** | - |

---

## Main Shader Sizes

| File | Lines | Purpose |
|------|-------|---------|
| field_visual.fsh | 854 | Energy orb effects |
| shockwave_ring.fsh | 368 | Shockwave ring effects |

---

## Dependency Graph

### field_visual.fsh includes:
```
field_visual.fsh
├── include/sdf_library.glsl
├── include/glow_utils.glsl
├── include/energy_orb_v2.glsl
└── include/camera_core.glsl
```

### shockwave_ring.fsh includes:
```
shockwave_ring.fsh
├── include/sdf_library.glsl
├── include/depth_utils.glsl
├── include/orbital_math.glsl
├── include/raymarching.glsl
└── include/glow_utils.glsl
```

### Include files: No internal dependencies
Currently, all include files are self-contained (no nested #includes).
They rely on the main shader to define uniforms and include prerequisites.

---

## Shared Code Analysis

### Used by BOTH shaders:
- `sdf_library.glsl` - SDF primitives
- `glow_utils.glsl` - Ring effects

### Used by field_visual.fsh ONLY:
- `camera_core.glsl` - Camera/ray functions
- `energy_orb_v2.glsl` - V2 orb rendering

### Used by shockwave_ring.fsh ONLY:
- `depth_utils.glsl` - Depth reconstruction
- `orbital_math.glsl` - Orbital positions
- `raymarching.glsl` - Orbital/beam marching

### NOT used directly by either (but referenced in docs):
- `projection_modes.glsl` - Appears unused! (verify before deletion)

---

## Potential Duplicates to Investigate

| Code | File A | File B |
|------|--------|--------|
| sdfSphere() | field_visual.fsh (inline) | sdf_library.glsl |
| rotate2D() | field_visual.fsh (inline) | ? (may be unique) |
| voronoi/noise | field_visual.fsh (inline) | ? (may be unique) |
| linearizeDepth() | depth_utils.glsl | field_visual.fsh? |
| computeCameraBasis | camera_core.glsl | field_visual.fsh? |

---

## Uniform Block Dependencies

### field_visual.fsh requires:
- FieldVisualConfig UBO (21 vec4s + matrices)
- SamplerInfo (standard Minecraft)

### shockwave_ring.fsh requires:
- ShockwaveConfig UBO (18 vec4s)
- SamplerInfo (standard Minecraft)

### Include files assume these uniforms exist:
- `depth_utils.glsl` assumes: CameraX/Y/Z, ForwardX/Y/Z, Fov, AspectRatio
- `camera_core.glsl` assumes: Similar camera uniforms
- `raymarching.glsl` assumes: CoronaWidth, BeamWidth*, RimPower, etc.

---

## Pre-Restructure Checklist

- [x] Line counts documented
- [x] Dependencies mapped
- [ ] Git commit current working state
- [ ] Tag as `pre-restructure-working`
- [ ] Verify projection_modes.glsl usage
- [ ] Identify inline duplicates in field_visual.fsh

---

## Next Step

**Phase 0.2:** Git commit and tag current state before making changes.
