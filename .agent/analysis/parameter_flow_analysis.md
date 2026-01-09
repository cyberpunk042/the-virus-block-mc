# Parameter Flow Analysis: Original Working vs Current Broken

## Summary
The flickering is caused by inconsistency between what `getTickProgress(false)` returns vs the `tickDelta` captured from the render method locals.

---

## ORIGINAL WORKING FLOW

### FieldVisual (WORKING - orb follows smoothly)

#### Mixin: WorldRendererFieldVisualMixin
```java
// Line 68: tickDelta is CAPTURED from render method locals
float tickDelta,

// Line 95: Stores tickDelta for later use
FieldVisualPostEffect.setTickDelta(tickDelta);

// Line 99-100: Forward computed from camera.getYaw()/getPitch()
float yaw = (float) Math.toRadians(camera.getYaw());
float pitch = (float) Math.toRadians(camera.getPitch());

// Line 106-108: Forward calculation
float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
float fwdY = (float) (-Math.sin(pitch));
float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));

// Line 110: Stored in FieldVisualUniformBinder
FieldVisualPostEffect.updateCameraForward(fwdX, fwdY, fwdZ);

// Line 125-128: invViewProj computed from render matrices
Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
Matrix4f invViewProj = new Matrix4f();
viewProj.invert(invViewProj);
FieldVisualPostEffect.updateViewProjectionMatrices(invViewProj, viewProj);
```

#### PostEffectPassMixin -> updateFieldVisualUniforms()
```java
// Line 217: Uses the CAPTURED tickDelta
float tickDelta = FieldVisualPostEffect.getTickDelta();

// Line 218-219: FOV computed with CAPTURED tickDelta
float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);
float fov = (float) Math.toRadians(dynamicFov);

// Line 225-227: Gets data from FieldVisualUniformBinder
Vec3d cameraPos = FieldVisualUniformBinder.getCameraPosition();
Matrix4f invViewProj = FieldVisualUniformBinder.getInvViewProjection();
Matrix4f viewProj = FieldVisualUniformBinder.getViewProjection();

// Line 246-248: Forward from FieldVisualUniformBinder
FieldVisualUniformBinder.getForwardX(),
FieldVisualUniformBinder.getForwardY(),
FieldVisualUniformBinder.getForwardZ(),
```

### Shockwave (ORIGINAL)

#### Mixin: WorldRendererShockwaveMixin
```java
// Line 66: tickDelta is CAPTURED but NOT STORED anywhere!
float tickDelta,

// Lines 84-101: Updates ShockwavePostEffect
ShockwavePostEffect.updateCameraPosition((float)camX, (float)camY, (float)camZ);
ShockwavePostEffect.updateCameraForward(fwdX, fwdY, fwdZ);
ShockwavePostEffect.updateInvViewProj(invViewProj);
```

#### PostEffectPassMixin -> updateShockwaveUniforms()
```java
// Line 149: Gets tickDelta DIRECTLY from client - NOT the captured one!
float tickDelta = client.getRenderTickCounter().getTickProgress(false);

// Line 150-151: FOV with this different tickDelta
float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);

// Line 155: invViewProj from ShockwavePostEffect
Matrix4f invViewProj = ShockwavePostEffect.getInvViewProj();
```

---

## CRITICAL OBSERVATION

In the ORIGINAL, Shockwave ALSO uses `getTickProgress(false)` but was NOT affected by flickering because:

1. The matrices (invViewProj) are computed in the MIXIN from `positionMatrix` and `projectionMatrix`
2. Those matrices are what Minecraft used for this frame
3. The forward vector is computed from `camera.getYaw()/getPitch()` which SHOULD be consistent

**BUT** the FOV is computed using `getTickProgress(false)` which could be slightly different from what Minecraft used.

---

## CURRENT BROKEN FLOW

### NEW CameraStateManager Update
All mixins now call:
```java
CameraStateManager.updateFromRender(
    (float)camX, (float)camY, (float)camZ,
    camera.getYaw(), camera.getPitch(),
    positionMatrix, projectionMatrix,
    tickDelta  // <-- Now captures tickDelta
);
```

### BaseUBOBinder.updateCameraUBO()
```java
// Line 152: Uses CameraStateManager.getTickDelta()
float tickDelta = CameraStateManager.getTickDelta();
```

This SHOULD be correct now if all mixins pass the captured tickDelta.

---

## WHAT STILL USES getTickProgress(false)?

1. **BaseUBOBinder.updateFrameUBO() line 110:**
   ```java
   float deltaTime = client.getRenderTickCounter().getTickProgress(false) / 20f;
   ```
   This is for `FrameDataUBO.deltaTime` - animation delta, NOT for camera interpolation.

2. **Original Shockwave line 149** (in updateShockwaveUniforms):
   ```java
   float tickDelta = client.getRenderTickCounter().getTickProgress(false);
   ```
   This is ALSO used for FOV calculation which could cause mismatch!

---

## ROOT CAUSE HYPOTHESIS

The issue is NOT in CameraStateManager. The issue is that:

1. **positionMatrix and projectionMatrix** are computed by Minecraft using a specific tickDelta
2. **camera.getYaw() and camera.getPitch()** are interpolated by Minecraft
3. **invokeGetFov(camera, tickDelta, true)** may use a DIFFERENT tickDelta than what Minecraft used

When FOV is computed with a different tickDelta value, it creates a mismatch between:
- The matrices (which use Minecraft's internal tickDelta for scene rendering)
- The FOV passed to the shader (which uses our potentially-stale tickDelta)

---

## RECOMMENDED FIX

Ensure ALL parameters use the same `tickDelta` that was captured from the render method:

1. **CameraStateManager** should store and provide tickDelta ✓ (done)
2. **BaseUBOBinder.updateCameraUBO()** should use `CameraStateManager.getTickDelta()` ✓ (done)
3. **PostEffectPassMixin.updateShockwaveUniforms()** should use stored tickDelta instead of `getTickProgress(false)`
4. **PostEffectPassMixin.updateMagicCircleUniforms()** should use stored tickDelta instead of `getTickProgress(false)`

---

## ACTION ITEMS

1. Check if MagicCircle PostEffectPassMixin uses getTickProgress(false) - line 324
2. Check if Shockwave PostEffectPassMixin uses getTickProgress(false) - line 149
3. Replace ALL occurrences of `client.getRenderTickCounter().getTickProgress(false)` with `CameraStateManager.getTickDelta()` in uniform updates
