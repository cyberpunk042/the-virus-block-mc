# camera/ - Space Transformation Layer

**Dependencies:** core/  
**Used by:** sdf/, rendering/, effects/

---

## Purpose

The camera layer handles all coordinate space transformations.
This is where the critical V1/V2 paradigm split happens.

## The Two Critical Operations

### For V1 (Raymarching):
**Screen → World:** Generate rays from pixels into 3D space
```glsl
#include "include/camera/rays.glsl"
```

### For V2 (Screen Projection):
**World → Screen:** Project 3D objects to 2D screen
```glsl
#include "include/camera/projection.glsl"
```

---

## Files

### types.glsl
```glsl
#include "include/camera/types.glsl"
```

**Exports:**
```glsl
struct CameraData {
    vec3 position;    // Camera world position
    vec3 forward;     // Normalized look direction
    vec3 right;       // Normalized right direction
    vec3 up;          // Normalized up direction
    float fov;        // Vertical FOV in RADIANS
    float aspect;     // width / height
    float near;       // Near plane
    float far;        // Far plane
};

struct Ray {
    vec3 origin;
    vec3 direction;   // Normalized
};

struct SphereProjection {
    vec2 screenCenter;     // Screen UV (0-1)
    float apparentRadius;  // Radius in screen units
    float viewDepth;       // Distance to center
    bool isVisible;        // In front of camera?
};

struct RaymarchHit { ... };
struct DepthInfo { ... };
```

### basis.glsl
```glsl
#include "include/camera/basis.glsl"
```

**Exports:**
- `computeCameraBasis(forward, worldUp, outRight, outUp)` - Get right/up from forward
- `computeCameraBasisDefault(forward, outRight, outUp)` - Uses world up (0,1,0)
- `buildCameraData(pos, forward, fov, aspect, near, far)` - Build complete struct
- `buildCameraDataExplicit(...)` - With explicit right/up

### rays.glsl - CRITICAL FOR V1
```glsl
#include "include/camera/rays.glsl"
```

**Exports:**
- `getRayFromBasis(texCoord, cam)` - Ray from camera basis vectors
  - **USE WHEN:** Walking on ground (stable, no camera bob mismatch)
- `getRayFromMatrix(texCoord, invViewProj, camPos)` - Ray from inverse matrix
  - **USE WHEN:** Flying (accurate, uses Minecraft's exact matrices)
- `getRayAdaptive(texCoord, cam, invViewProj, isFlying)` - Auto-select
  - **USE THIS** for most cases - handles both states

**⚠️ The Walking/Flying Problem:**
When walking, camera has "bob" animation. If we use InvViewProj directly,
rays are computed from the bobbing camera, but the depth buffer was rendered
from a different camera position. This causes flickering.

**Solution:** Use basis-based rays (from yaw/pitch) when walking.

### projection.glsl - CRITICAL FOR V2
```glsl
#include "include/camera/projection.glsl"
```

**Exports:**
- `worldToView(worldPos, cam)` - World to camera-relative
- `viewToNDC(viewPos, cam)` - View to normalized device coords
- `ndcToScreen(ndc)` - NDC to screen UV
- `worldToScreen(worldPos, cam)` - Full pipeline
- `worldToScreenMatrix(worldPos, viewProj)` - Using matrix
- `getApparentRadius(sphereRadius, viewDepth, fov)` - Perspective radius
- `projectSphere(sphereCenter, sphereRadius, cam)` - Full sphere projection

**⚠️ FOV Accuracy:**
For V2, the FOV must match what Minecraft uses for rendering.
Use `GameRenderer.getFov()` via accessor mixin for dynamic FOV.

### depth.glsl
```glsl
#include "include/camera/depth.glsl"
```

**Exports:**
- `linearizeDepth(rawDepth, near, far)` - Buffer to world units
- `isSky(rawDepth)` - True if at far plane
- `getDepthInfo(rawDepth, ...)` - Build DepthInfo struct
- `reconstructWorldPos(texCoord, linearDepth, cam)` - Full reconstruction
- `reconstructWorldPosMatrix(texCoord, rawDepth, invViewProj)` - Matrix method
- `isOccluded(effectDepth, sceneDepth)` - Simple occlusion check
- `softOcclusion(effectDepth, sceneDepth, fadeWidth)` - Soft fade

---

## Usage Patterns

### V1 with Adaptive Rays
```glsl
#include "include/camera/rays.glsl"

CameraData cam = buildCameraData(...);
Ray ray = getRayAdaptive(texCoord, cam, InvViewProj, IsFlying);
// Now raymarch with ray.origin, ray.direction
```

### V2 with Sphere Projection
```glsl
#include "include/camera/projection.glsl"

CameraData cam = buildCameraData(...);
SphereProjection proj = projectSphere(orbCenter, orbRadius, cam);
if (proj.isVisible) {
    float dist = distToPoint(texCoord, proj.screenCenter, cam.aspect);
    // Apply 2D effect based on dist / apparentRadius
}
```

---

## ⚠️ Common Mistakes

1. Using InvViewProj rays when walking → flickering
2. Using hardcoded FOV for V2 → size mismatch
3. Forgetting aspect ratio correction for distances
4. Not checking proj.isVisible before rendering
