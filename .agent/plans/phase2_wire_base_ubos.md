# Phase 2: Wire Base UBOs to Shaders (Revised)

> **Status:** READY TO EXECUTE
> **Prereqs:** Phase 1 Complete ✅

---

## Current State

**Java Infrastructure (Working):**
- BaseUBOBinder writes `FrameDataUBO` and `CameraDataUBO` every frame
- Buffer names match GLSL block names

**GLSL Files (Ready to include):**
- `include/ubo/frame_ubo.glsl` → `FrameDataUBO` block
- `include/ubo/camera_ubo.glsl` → `CameraDataUBO` block
- `include/ubo/light_ubo.glsl` → `LightDataUBO` block

**Naming Strategy:**
- All new UBO block names end with `UBO` suffix
- All new field names end with `UBO` suffix (e.g., `FrameTimeUBO`, `CameraPositionUBO`)
- This prevents conflicts with existing `FieldVisualConfig` fields

---

## Execution Plan

### Task 2.1: Add Base UBO Includes to field_visual_base.glsl

**File:** `include/core/field_visual_base.glsl`

**Action:** Add includes after SamplerInfo block:
```glsl
// BASE UBOs (Phase 2)
#include "../ubo/frame_ubo.glsl"
#include "../ubo/camera_ubo.glsl"
```

**Why it works:** New blocks have `UBO` suffix, so no name conflicts.

**Validation:** `python3 scripts/10_validate_shader.py --auto`

---

### Task 2.2: Update Preamble Macro to Read from Base UBOs

**File:** `include/core/field_visual_preamble.glsl`

**Current macro reads:**
```glsl
vec3 camPos = vec3(CameraX, CameraY, CameraZ);  // From FieldVisualConfig
float time = Time;  // From FieldVisualConfig
```

**New macro reads:**
```glsl
vec3 camPos = CameraPositionUBO.xyz;  // From CameraDataUBO
float time = FrameTimeUBO.x;  // From FrameDataUBO
```

**Key changes in preamble:**
| Old (FieldVisualConfig) | New (Base UBO) |
|-------------------------|----------------|
| `CameraX, CameraY, CameraZ` | `CameraPositionUBO.xyz` |
| `ForwardX, ForwardY, ForwardZ` | `CameraForwardUBO.xyz` |
| `Fov` | `CameraUpUBO.w` |
| `AspectRatio` | `CameraForwardUBO.w` |
| `NearPlane` | `CameraClipUBO.x` |
| `FarPlane` | `CameraClipUBO.y` |
| `IsFlying` | `CameraClipUBO.z > 0.5` |
| `Time` | `FrameTimeUBO.x` |

---

### Task 2.3: Test All Field Visual Shaders

Since all 8 shaders use the same preamble, one change updates them all:
- field_visual_v1.fsh
- field_visual_v2.fsh
- field_visual_v3.fsh
- field_visual_v5.fsh
- field_visual_v6.fsh
- field_visual_v7.fsh
- field_visual_v8.fsh
- field_visual_geodesic.fsh

**Validation:**
```bash
python3 scripts/10_validate_shader.py --auto
```

---

### Task 2.4: In-Game Testing

1. Run game
2. Enable field visual effect
3. Verify: renders, animates, tracks camera
4. Look around - no jitter
5. Fly/walk - isFlying works

---

## Post-Migration Cleanup (Phase 3)

After shaders are verified working with base UBOs:
1. Remove camera/time fields from FieldVisualConfig UBO (smaller buffer)
2. Remove camera/time from Java binders (less duplicate code)
3. Repeat for virus_block, shockwave, magic_circle

---

## Risk Mitigation

**If validation fails:** Check the preprocessed output line numbers
**If in-game fails:** Old FieldVisualConfig params still exist as fallback

---

## Ready to Execute?

Start with **Task 2.1: Add includes to field_visual_base.glsl**
