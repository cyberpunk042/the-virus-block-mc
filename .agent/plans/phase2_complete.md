# Phase 2 Complete: Base UBOs Wired to Field Visual Shaders

> **STATUS: ✅ COMPLETE**  
> Date: 2026-01-08

---

## Issues Encountered & Solutions

### Issue 1: Effects Completely Stopped Rendering

**Symptom:** After adding UBO declarations to JSON and updating GLSL preamble, field visual effects disappeared entirely.

**Root Cause:** `BaseUBOBinder.updateCameraUBO()` was calculating camera data **independently** instead of using the **same data sources** as the working `PostEffectPassMixin` code.

**Wrong approach (what I did):**
```java
// WRONG: Calculating forward vector independently
float pitch = camera.getPitch();
float yaw = camera.getYaw();
float forwardX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
// ... etc

// WRONG: Using world position instead of camera-relative
Vec3d pos = camera.getPos();
```

**Correct approach (the fix):**
```java
// CORRECT: Use same source as PostEffectPassMixin
float forwardX = FieldVisualUniformBinder.getForwardX();
float forwardY = FieldVisualUniformBinder.getForwardY();
float forwardZ = FieldVisualUniformBinder.getForwardZ();

// CORRECT: Camera at origin in camera-relative coordinates
float camX = 0f;
float camY = 0f;
float camZ = 0f;

// CORRECT: Matrices from same source
Matrix4f viewProj = FieldVisualUniformBinder.getViewProjection();
Matrix4f invViewProj = FieldVisualUniformBinder.getInvViewProjection();
```

**Lesson:** When creating a new data path (new UBO), you MUST populate it with the EXACT SAME values that the old working path was using. Don't calculate things independently.

---

### Issue 2: JSON Parse Error - "Unknown element name: mat4"

**Symptom:** 
```
Failed to parse post chain at the-virus-block:post_effect/field_visual_v8.json
com.google.gson.JsonSyntaxException: Unknown element name:mat4
```

**Root Cause:** Minecraft's post-effect JSON parser only recognizes these uniform types:
- `float`
- `vec2`
- `vec3`
- `vec4`

It does NOT support `mat4`.

**Wrong approach (what I tried):**
```json
{
    "name": "ViewProjUBO",
    "type": "mat4",
    "value": [1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]
}
```

**Correct approach (the fix):**
Represent each `mat4` as 4 individual `vec4` rows:
```json
{"name": "ViewProjRow0", "type": "vec4", "value": [1.0, 0.0, 0.0, 0.0]},
{"name": "ViewProjRow1", "type": "vec4", "value": [0.0, 1.0, 0.0, 0.0]},
{"name": "ViewProjRow2", "type": "vec4", "value": [0.0, 0.0, 1.0, 0.0]},
{"name": "ViewProjRow3", "type": "vec4", "value": [0.0, 0.0, 0.0, 1.0]}
```

**Lesson:** Check what types the parser actually supports before adding new uniform declarations. The GLSL can still use `mat4` in the uniform block - the JSON just needs to represent it differently.

---

### Issue 3: Missing Matrix Rows Caused Layout Mismatch

**Symptom:** After removing the mat4 entries to fix the parse error, effects still didn't work.

**Root Cause:** The GLSL `CameraDataUBO` uniform block expects 14 vec4 slots:
- 4 vec4 for position/forward/up/clip
- 8 vec4 for 2 matrices (4 rows each)
- 2 vec4 for reserved

But after removing the mat4 entries, the JSON only declared 6 vec4 slots. This caused a **layout mismatch** - the GPU buffer size didn't match what the shader expected.

**Wrong state (after removing mat4):**
```
CameraPositionUBO    (vec4 0)
CameraForwardUBO     (vec4 1)
CameraUpUBO          (vec4 2)
CameraClipUBO        (vec4 3)
CameraReserved1UBO   (vec4 4)  ← WRONG! Should be vec4 12
CameraReserved2UBO   (vec4 5)  ← WRONG! Should be vec4 13
```

**Correct state (the fix):**
```
CameraPositionUBO    (vec4 0)
CameraForwardUBO     (vec4 1)
CameraUpUBO          (vec4 2)
CameraClipUBO        (vec4 3)
ViewProjRow0         (vec4 4)
ViewProjRow1         (vec4 5)
ViewProjRow2         (vec4 6)
ViewProjRow3         (vec4 7)
InvViewProjRow0      (vec4 8)
InvViewProjRow1      (vec4 9)
InvViewProjRow2      (vec4 10)
InvViewProjRow3      (vec4 11)
CameraReserved1UBO   (vec4 12)
CameraReserved2UBO   (vec4 13)
```

**Lesson:** UBO layout in JSON MUST match GLSL layout exactly - same number of fields, same order, same sizes. If GLSL has 14 vec4 slots, JSON must declare 14 vec4 entries.

---

### Issue 4: IsFlying Check Was Incomplete

**Symptom:** (Potential issue caught during review)

**Root Cause:** The original BaseUBOBinder only checked `player.getAbilities().flying`, but PostEffectPassMixin also checks third-person mode.

**Wrong approach:**
```java
boolean isFlying = client.player != null && client.player.getAbilities().flying;
```

**Correct approach:**
```java
float isFlying = 0f;
if (client.player != null) {
    if (client.player.getAbilities().flying) {
        isFlying = 1f;
    } else if (camera.isThirdPerson()) {
        isFlying = 1f;  // Third person also uses "flying" ray casting mode
    }
}
```

**Lesson:** Don't just copy part of the logic - copy ALL the logic from the working code path.

---

### Issue 5: Duplicate/Missing JSON Name Field

**Symptom:** When adding the matrix rows, one entry was missing its "name" field.

**Root Cause:** The replacement text didn't include the full structure for CameraReserved1UBO.

**Wrong state:**
```json
{
    "type": "vec4",  ← Missing "name" field!
    "value": [0.0, 0.0, 0.0, 0.0]
}
```

**Correct state:**
```json
{
    "name": "CameraReserved1UBO",
    "type": "vec4",
    "value": [0.0, 0.0, 0.0, 0.0]
}
```

**Lesson:** When doing text replacements in JSON, verify the complete structure of each entry.

---

### 3. GLSL Preamble Migrated

**field_visual_base.glsl:**
Added includes:
```glsl
#include "../ubo/frame_ubo.glsl"
#include "../ubo/camera_ubo.glsl"
```

**field_visual_preamble.glsl:**
Updated FIELD_VISUAL_PREAMBLE macro to use new UBO fields:

| Old (FieldVisualConfig) | New (CameraDataUBO) |
|------------------------|---------------------|
| `NearPlane` | `CameraClipUBO.x` |
| `FarPlane` | `CameraClipUBO.y` |
| `CameraX, CameraY, CameraZ` | `CameraPositionUBO.xyz` |
| `ForwardX, ForwardY, ForwardZ` | `CameraForwardUBO.xyz` |
| `Fov` | `CameraUpUBO.w` |
| `AspectRatio` | `CameraForwardUBO.w` |
| `IsFlying` | `CameraClipUBO.z` |

Note: `InvViewProj` still read from `FieldVisualConfig` (existing matrix in FieldVisualUBO)

---

## Current Architecture (After Phase 2)

```
PostEffectPassMixin
    │
    ├── BaseUBOBinder.updateBaseUBOs(uniformBuffers)
    │       │
    │       ├── FrameDataUBO  → FrameTimeUBO (time, deltaTime, frameCount)
    │       │
    │       └── CameraDataUBO → CameraPositionUBO, CameraForwardUBO,
    │                           CameraUpUBO, CameraClipUBO, ViewProjRow*, InvViewProjRow*
    │
    └── FieldVisualUBO (existing) → FieldVisualConfig (effect-specific params)
```

**Data Flow:**
- Camera/time data: `BaseUBOBinder` → `CameraDataUBO` → GLSL preamble
- Effect config: `PostEffectPassMixin` → `FieldVisualUBO` → `FieldVisualConfig` (GLSL)

---

## Key Lessons Learned

1. **Same Data Sources:** New UBO paths MUST use identical data sources as existing working code
2. **JSON mat4 Not Supported:** Minecraft's post-effect JSON parser only supports float/vec2/vec3/vec4
3. **Layout Matters:** JSON field order determines memory layout - must match GLSL exactly
4. **Test Incrementally:** Change one thing, build, test, repeat

---

## What Still Uses FieldVisualConfig

The following are still read from the old `FieldVisualConfig` UBO:
- All effect-specific parameters (colors, noise, corona, etc.)
- `InvViewProj` matrix (still in FieldVisualUBO)
- All V2/V5/V8 block parameters

---

## Next Steps (From Master Plan)

### Phase 3: Split FieldVisual into Config vs Runtime
- [ ] Create `FieldVisualConfigUBO` (binding 20) - preset/style, rarely updated
- [ ] Create `FieldVisualRuntimeUBO` (binding 21) - per-frame instance state
- [ ] Move matrices to CameraDataUBO exclusively

### Phase 4: Reserved Lanes & Versioning
- [ ] Add reserved vec4 slots to each section
- [ ] Add version fields to headers

### Cleanup (Later)
- [ ] Remove redundant camera fields from FieldVisualConfig (CameraX, CameraY, etc.)
- [ ] Remove redundant time fields from FieldVisualConfig
- [ ] Consolidate matrix handling

---

## Files Modified in This Phase

### Java
- `src/client/java/net/cyberpunk042/client/visual/ubo/BaseUBOBinder.java`

### GLSL
- `src/main/resources/assets/the-virus-block/shaders/post/include/core/field_visual_base.glsl`
- `src/main/resources/assets/the-virus-block/shaders/post/include/core/field_visual_preamble.glsl`

### JSON (All 8)
- `src/main/resources/assets/the-virus-block/post_effect/field_visual_*.json`
