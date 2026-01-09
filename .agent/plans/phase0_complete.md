# Phase 0: UBO Inventory & Classification Complete

> **Status:** COMPLETE  
> **Date:** 2026-01-08  
> **Purpose:** Audit all existing UBOs, classify parameters by domain, identify duplication

---

## Executive Summary

### Current State: Parameter Duplication Across UBOs

The codebase has **4 major UBOs** for post effects, each with its own copy of camera/time parameters:

| UBO | Total Params | Camera Duplication | Frame Duplication |
|-----|-------------|-------------------|-------------------|
| FieldVisualConfig | 170 | 16 params | 1 param (Time) |
| VirusBlockParams | 14 | ~6 params | 1 param (Time) |
| ShockwaveConfig | 73 | ~12 params | 1 param (Time) |
| MagicCircleConfig | 48 | ~4 params | 1 param (Time) |

**Problem:** Same data (camera, time) is duplicated 4 times in memory and written 4 times per frame.

**Solution:** Extract shared data into base UBOs that all effects can reference.

---

## Inventory of All UBO Blocks

### 1. FieldVisualConfig (170 parameters)

**File:** `shaders/post/include/core/field_visual_base.glsl`  
**Java:** `FieldVisualUBO.java`  
**Used by:** field_visual_v1.fsh through v8.fsh, geodesic

| Section | Slot Range | Param Count | Target Domain |
|---------|------------|-------------|---------------|
| Position | 0 | 4 | EffectRuntime |
| Colors (5 palettes) | 1-5 | 20 | EffectConfig |
| Animation | 6-8 | 12 | EffectConfig |
| Core/Edge/Falloff | 9-10 | 8 | EffectConfig |
| Noise | 11-12 | 8 | EffectConfig |
| Glow/Corona | 13-14 | 8 | EffectConfig |
| Geometry | 15-16 | 8 | EffectConfig |
| Transform | 17 | 4 | EffectConfig |
| Lighting | 18 | 4 | EffectConfig |
| Timing | 19 | 4 | EffectConfig |
| Screen Effects | 20 | 4 | EffectConfig |
| Distortion | 21 | 4 | EffectConfig |
| Blend | 22 | 4 | EffectConfig |
| Reserved/Version | 23 | 4 | EffectConfig |
| V2 Detail | 24-28 | 20 | EffectConfig |
| **Camera/Runtime** | 29-32 | 16 | **Camera** ⚠️ |
| **Matrices** | 33-40 | 2 (mat4) | **Camera** ⚠️ |
| Debug | 41 | 4 | Debug |
| Flames V5 | 42-43 | 8 | EffectConfig |
| Geodesic | 44 | 4 | EffectConfig |
| V8 Electric | 45-49 | 20 | EffectConfig |

**Camera params to extract:** CameraX/Y/Z, ForwardX/Y/Z, UpX/Y/Z, Fov, AspectRatio, NearPlane, FarPlane, IsFlying, InvViewProj, ViewProj  
**Frame params to extract:** Time

---

### 2. VirusBlockParams (14 parameters)

**File:** `shaders/post/virus_block.fsh`  
**Java:** `VirusBlockUBO.java`

| Parameter | Type | Target Domain |
|-----------|------|---------------|
| CameraPosTime | vec4 | **Camera + Frame** ⚠️ |
| EffectFlags | vec4 | EffectConfig |
| InvProjParams | vec4 | **Camera** ⚠️ |
| CameraForward | vec4 | **Camera** ⚠️ |
| CameraUp | vec4 | **Camera** ⚠️ |
| SmokeShape | vec4 | EffectConfig |
| SmokeAnim | vec4 | EffectConfig |
| SmokeColor | vec4 | EffectConfig |
| ScreenPoisonParams | vec4 | EffectConfig |
| ESPClose | vec4 | EffectConfig |
| ESPMedium | vec4 | EffectConfig |
| ESPFar | vec4 | EffectConfig |
| ESPStyle | vec4 | EffectConfig |
| InvViewProj | mat4 | **Camera** ⚠️ |

**Camera params to extract:** CameraPosTime.xyz, CameraForward.xyz, CameraUp.xyz, InvProjParams, InvViewProj  
**Frame params to extract:** CameraPosTime.w (Time)

---

### 3. ShockwaveConfig (73 parameters)

**File:** `shaders/post/shockwave_ring.fsh`  
**Java:** `ShockwaveUBO.java`

| Section | Params | Target Domain |
|---------|--------|---------------|
| Ring params | ~20 | EffectConfig/Runtime |
| **Camera** | ~12 | **Camera** ⚠️ |
| **Time** | 1 | **Frame** ⚠️ |
| Mode/Style | ~10 | EffectConfig |
| Colors | ~12 | EffectConfig |
| Terrain/Depth | ~8 | EffectConfig |
| Reserved | ~10 | Reserved |

---

### 4. MagicCircleConfig (48 parameters)

**File:** `shaders/post/magic_circle.fsh`  
**Java:** (needs verification)

| Section | Params | Target Domain |
|---------|--------|---------------|
| Center/Radius | 4 | EffectRuntime |
| **Camera** | ~4 | **Camera** ⚠️ |
| Circle params | ~40 | EffectConfig |

---

## Duplicated Parameters (To Be Extracted)

### Camera Domain (Binding 1)

These parameters appear in **multiple** UBOs and should be in one shared CameraUBO:

| Parameter | FieldVisual | VirusBlock | Shockwave | MagicCircle |
|-----------|-------------|------------|-----------|-------------|
| CameraX/Y/Z | ✅ | ✅ | ✅ | ✅ |
| ForwardX/Y/Z | ✅ | ✅ | ✅ | ❌ |
| UpX/Y/Z | ✅ | ✅ | ✅ | ❌ |
| Fov | ✅ | ✅ | ✅ | ❌ |
| AspectRatio | ✅ | ✅ | ✅ | ❌ |
| NearPlane | ✅ | ✅ | ❌ | ❌ |
| FarPlane | ✅ | ✅ | ❌ | ❌ |
| IsFlying | ✅ | ✅ | ❌ | ❌ |
| InvViewProj | ✅ | ✅ | ❌ | ❌ |
| ViewProj | ✅ | ❌ | ❌ | ❌ |

### Frame Domain (Binding 0)

| Parameter | FieldVisual | VirusBlock | Shockwave | MagicCircle |
|-----------|-------------|------------|-----------|-------------|
| Time | ✅ | ✅ | ✅ | ✅ |

---

## Proposed Base UBOs

### FrameUBO (Binding 0) - ~16 bytes

```glsl
layout(std140) uniform FrameData {
    vec4 FrameTime;  // x=time, y=deltaTime, z=frameIndex, w=layoutVersion
};
```

### CameraUBO (Binding 1) - ~224 bytes

```glsl
layout(std140) uniform CameraData {
    vec4 Position;      // xyz=pos, w=reserved
    vec4 Forward;       // xyz=forward, w=aspect
    vec4 Up;            // xyz=up, w=fov
    vec4 Clip;          // x=near, y=far, z=isFlying, w=reserved
    mat4 ViewProj;      // 64 bytes
    mat4 InvViewProj;   // 64 bytes
};
```

---

## Impact Analysis

### Memory Savings

| Before | After | Savings |
|--------|-------|---------|
| Camera duplicated 4x | Camera shared 1x | ~672 bytes |
| Time duplicated 4x | Time shared 1x | ~48 bytes |

### Update Frequency Optimization

| Before | After |
|--------|-------|
| Every UBO updated every frame | Frame/Camera updated once, effects on-change only |

---

## Files That Need Modification

### Java Files

| File | Change |
|------|--------|
| `FieldVisualUBO.java` | Remove camera/time params |
| `VirusBlockUBO.java` | Remove camera/time params |
| `ShockwaveUBO.java` | Remove camera/time params |
| NEW: `FrameUBO.java` | Create |
| NEW: `CameraUBO.java` | Create |
| NEW: `UBORegistry.java` | Create binding registry |

### Shader Files

| File | Change |
|------|--------|
| `field_visual_base.glsl` | Split FieldVisualConfig, add Frame/Camera blocks |
| `virus_block.fsh` | Split VirusBlockParams, add Frame/Camera blocks |
| `shockwave_ring.fsh` | Split ShockwaveConfig, add Frame/Camera blocks |
| `magic_circle.fsh` | Split MagicCircleConfig, add Frame/Camera blocks |
| NEW: `include/ubo/frame.glsl` | Shared Frame UBO declaration |
| NEW: `include/ubo/camera.glsl` | Shared Camera UBO declaration |

### Binder Files

| File | Change |
|------|--------|
| `FieldVisualUniformBinder.java` | Stop writing camera/time |
| `VirusBlockUniformBinder.java` | Stop writing camera/time |
| `ShockwaveUniformBinder.java` | Stop writing camera/time |
| NEW: `FrameUniformBinder.java` | Write Frame UBO |
| NEW: `CameraUniformBinder.java` | Write Camera UBO |

---

## Next Steps (Phase 1)

1. Create `FrameUBO.java` record with `@Vec4` annotation
2. Create `CameraUBO.java` record with `@Vec4`, `@Mat4` annotations
3. Create `include/ubo/frame.glsl` GLSL declaration
4. Create `include/ubo/camera.glsl` GLSL declaration
5. Create binding registry with constants
6. Update **one shader** (field_visual_v8) to use new base UBOs
7. Verify it still works
8. Proceed to other shaders

---

## Appendix: Full Parameter Classifications

See individual audit files:
- `phase0_classification.md` - FieldVisualConfig (170 params)
- Generated audits for other UBOs available via `scripts/ubo_audit.py`

---

> **Phase 0 Status: COMPLETE**  
> Ready to proceed to Phase 1: Create Base UBOs (Frame + Camera)
