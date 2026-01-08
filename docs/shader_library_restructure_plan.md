# Shader Library Restructure: Implementation Plan

## Overview

**Goal:** Create a clean, reliable, well-documented shader library that properly separates the three rendering paradigms and enables easy creation of V1/V2 variants for all effects.

**Principles:**
- Each file has ONE clear responsibility
- Dependencies flow downward (core → camera → sdf → rendering → effects)
- Every file has clear header documentation
- No duplicate code between files
- V1 and V2 provide same visual output through different methods

---

## Phase 0: Preparation & Audit
**Duration:** ~30 minutes

### 0.1 Document Current State
- [ ] List all current include files and their line counts
- [ ] Map dependencies between files
- [ ] Identify duplicate code blocks
- [ ] Mark code that was "trial and error" vs intentional

### 0.2 Create Backup
- [ ] Git commit current working state
- [ ] Tag as `pre-restructure-working`

### 0.3 Define Test Cases
- [ ] V1 orb: walking, standing, flying, transition
- [ ] V2 orb: walking, standing, flying, transition, sprinting
- [ ] Shockwave rings: expanding, with orbitals, with beams
- [ ] Performance baseline (FPS with each effect)

---

## Phase 1: Core Foundation
**Duration:** ~1 hour

### 1.1 Create `core/constants.glsl`
Single source of truth for shared constants.

```glsl
// core/constants.glsl
#ifndef CORE_CONSTANTS_GLSL
#define CORE_CONSTANTS_GLSL

// Numerical precision
#define EPSILON 0.0001
#define NEAR_ZERO 0.001
#define RAYMARCH_EPSILON 0.05
#define MAX_RAYMARCH_STEPS 48

// Mathematical constants
#define PI 3.14159265359
#define TWO_PI 6.28318530718
#define HALF_PI 1.57079632679

// Shape type identifiers
#define SHAPE_POINT 0
#define SHAPE_SPHERE 1
#define SHAPE_TORUS 2
#define SHAPE_POLYGON 3
#define SHAPE_ORBITAL 4

// Render mode identifiers
#define RENDER_V1_RAYMARCH 1
#define RENDER_V2_PROJECTION 2

#endif
```

**Files to create:**
- [ ] `include/core/constants.glsl`

**Code to extract from:**
- `sdf_library.glsl` - SHAPE_* constants
- `raymarching.glsl` - MAX_RAYMARCH_STEPS, RAYMARCH_EPSILON
- Various files - PI constants

---

### 1.2 Create `core/math_utils.glsl`
Common mathematical operations.

```glsl
// core/math_utils.glsl
#ifndef CORE_MATH_UTILS_GLSL
#define CORE_MATH_UTILS_GLSL

#include "constants.glsl"

// Safe normalize (prevents NaN)
vec3 safeNormalize(vec3 v) {
    float len = length(v);
    return len > NEAR_ZERO ? v / len : vec3(0.0, 1.0, 0.0);
}

// Smooth minimum for SDF blending
float smoothMin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

// Remap value from one range to another
float remap(float value, float inMin, float inMax, float outMin, float outMax) {
    return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
}

// Saturate (clamp 0-1)
float saturate(float x) { return clamp(x, 0.0, 1.0); }
vec3 saturate3(vec3 v) { return clamp(v, vec3(0.0), vec3(1.0)); }

#endif
```

**Files to create:**
- [ ] `include/core/math_utils.glsl`

**Code to extract from:**
- `sdf_library.glsl` - smoothMin
- Various inline `max(0.001, length(...))` patterns

---

### 1.3 Create `core/color_utils.glsl`
Color manipulation utilities.

```glsl
// core/color_utils.glsl
#ifndef CORE_COLOR_UTILS_GLSL
#define CORE_COLOR_UTILS_GLSL

// Premultiply alpha
vec4 premultiply(vec4 c) {
    return vec4(c.rgb * c.a, c.a);
}

// HDR to LDR tone mapping
vec3 toneMapAces(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

// Simple exposure adjustment
vec3 applyExposure(vec3 color, float exposure) {
    return color * pow(2.0, exposure);
}

#endif
```

**Files to create:**
- [ ] `include/core/color_utils.glsl`

---

## Phase 2: Camera Domain
**Duration:** ~1.5 hours

### 2.1 Create `camera/types.glsl`
Structs for camera-related data.

```glsl
// camera/types.glsl
#ifndef CAMERA_TYPES_GLSL
#define CAMERA_TYPES_GLSL

// Camera orientation and position
struct CameraData {
    vec3 position;    // Camera world position (usually 0,0,0 in camera-relative)
    vec3 forward;     // Normalized forward direction
    vec3 right;       // Normalized right direction
    vec3 up;          // Normalized up direction
    float fov;        // Vertical FOV in radians
    float aspect;     // Aspect ratio (width/height)
    float near;       // Near plane distance
    float far;        // Far plane distance
};

// Ray for marching
struct Ray {
    vec3 origin;
    vec3 direction;
};

// Sphere projection result
struct SphereProjection {
    vec2 screenCenter;     // Screen UV (0-1)
    float apparentRadius;  // Radius in screen UV units
    float viewDepth;       // Distance from camera
    bool isVisible;        // In front of camera and on screen
};

// Raymarch hit result
struct RaymarchHit {
    bool hit;              // Did we hit something?
    float distance;        // Distance along ray to hit
    vec3 position;         // World position of hit
    vec3 normal;           // Surface normal at hit
    float rimAmount;       // Rim lighting factor
    int hitType;           // What did we hit? (0=sphere, 1=beam, etc)
};

#endif
```

**Files to create:**
- [ ] `include/camera/types.glsl`

**Code to extract/refactor from:**
- `projection_modes.glsl` - SphereProjection, RaymarchResult
- `raymarching.glsl` - implicit hit data in vec4

---

### 2.2 Create `camera/basis.glsl`
Camera basis vector computation.

```glsl
// camera/basis.glsl
#ifndef CAMERA_BASIS_GLSL
#define CAMERA_BASIS_GLSL

#include "../core/constants.glsl"
#include "../core/math_utils.glsl"

// Compute right and up vectors from forward direction
// worldUp is typically vec3(0, 1, 0)
void computeCameraBasis(vec3 forward, vec3 worldUp, out vec3 right, out vec3 up) {
    right = cross(forward, worldUp);
    float rightLen = length(right);
    
    // Handle gimbal lock (looking straight up/down)
    if (rightLen < NEAR_ZERO) {
        right = vec3(1.0, 0.0, 0.0);
    } else {
        right = right / rightLen;
    }
    
    up = normalize(cross(right, forward));
}

// Convenience: use default world up
void computeCameraBasisDefault(vec3 forward, out vec3 right, out vec3 up) {
    computeCameraBasis(forward, vec3(0.0, 1.0, 0.0), right, up);
}

// Build CameraData from common uniforms
CameraData buildCameraData(
    vec3 position, vec3 forward, 
    float fov, float aspect, float near, float far
) {
    CameraData cam;
    cam.position = position;
    cam.forward = normalize(forward);
    computeCameraBasisDefault(cam.forward, cam.right, cam.up);
    cam.fov = fov;
    cam.aspect = aspect;
    cam.near = near;
    cam.far = far;
    return cam;
}

#endif
```

**Files to create:**
- [ ] `include/camera/basis.glsl`

**Code to extract from:**
- `camera_core.glsl` - computeCameraBasis, computeCameraBasisDefault

---

### 2.3 Create `camera/rays.glsl`
Ray generation for raymarching.

```glsl
// camera/rays.glsl
#ifndef CAMERA_RAYS_GLSL
#define CAMERA_RAYS_GLSL

#include "types.glsl"
#include "basis.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// RAY GENERATION - Two methods for different situations
// ═══════════════════════════════════════════════════════════════════════════
// 
// METHOD 1: YAW/PITCH BASED (getRayFromBasis)
//   - Uses pre-computed forward/right/up vectors
//   - Stable when walking (no camera bob in yaw/pitch)
//   - Use when: Player is walking on ground
//
// METHOD 2: MATRIX BASED (getRayFromMatrix)
//   - Uses InvViewProj matrix directly
//   - More accurate at distance
//   - Use when: Player is flying, standing still, or at distance
// ═══════════════════════════════════════════════════════════════════════════

// Method 1: Generate ray from camera basis vectors
Ray getRayFromBasis(vec2 texCoord, CameraData cam) {
    vec2 ndc = texCoord * 2.0 - 1.0;
    float tanHalfFov = tan(cam.fov * 0.5);
    float halfWidth = tanHalfFov * cam.aspect;
    float halfHeight = tanHalfFov;
    
    vec3 direction = normalize(
        cam.forward + 
        cam.right * (ndc.x * halfWidth) + 
        cam.up * (ndc.y * halfHeight)
    );
    
    Ray ray;
    ray.origin = cam.position;
    ray.direction = direction;
    return ray;
}

// Method 2: Generate ray from inverse view-projection matrix
Ray getRayFromMatrix(vec2 texCoord, mat4 invViewProj, vec3 camPos) {
    vec2 ndc = texCoord * 2.0 - 1.0;
    
    // Transform points on near and far plane
    vec4 clipNear = vec4(ndc, -1.0, 1.0);
    vec4 clipFar = vec4(ndc, 1.0, 1.0);
    
    vec4 worldNear = invViewProj * clipNear;
    vec4 worldFar = invViewProj * clipFar;
    
    worldNear /= worldNear.w;
    worldFar /= worldFar.w;
    
    Ray ray;
    ray.origin = camPos;
    ray.direction = normalize(worldFar.xyz - worldNear.xyz);
    return ray;
}

// Adaptive: choose method based on IsFlying uniform
Ray getRayAdaptive(vec2 texCoord, CameraData cam, mat4 invViewProj, float isFlying) {
    if (isFlying > 0.5) {
        return getRayFromMatrix(texCoord, invViewProj, cam.position);
    } else {
        return getRayFromBasis(texCoord, cam);
    }
}

#endif
```

**Files to create:**
- [ ] `include/camera/rays.glsl`

**Code to extract from:**
- `camera_core.glsl` - getRayDirection, getRayDirectionMatrix
- `field_visual.fsh` - the inline ray generation in main()

---

### 2.4 Create `camera/projection.glsl`
World to screen projection.

```glsl
// camera/projection.glsl
#ifndef CAMERA_PROJECTION_GLSL
#define CAMERA_PROJECTION_GLSL

#include "types.glsl"
#include "../core/math_utils.glsl"

// Project world point to NDC (-1 to 1)
vec3 worldToNDC(vec3 worldPos, CameraData cam) {
    vec3 toPoint = worldPos - cam.position;
    
    // Transform to view space
    float viewX = dot(toPoint, cam.right);
    float viewY = dot(toPoint, cam.up);
    float viewZ = dot(toPoint, cam.forward);
    
    // Behind camera check
    if (viewZ < cam.near) {
        return vec3(0.0, 0.0, -1.0); // Invalid
    }
    
    // Perspective projection
    float tanHalfFov = tan(cam.fov * 0.5);
    float ndcX = (viewX / viewZ) / (tanHalfFov * cam.aspect);
    float ndcY = (viewY / viewZ) / tanHalfFov;
    
    return vec3(ndcX, ndcY, viewZ);
}

// NDC to screen UV (0-1)
vec2 ndcToScreen(vec2 ndc) {
    return ndc * 0.5 + 0.5;
}

// Calculate apparent radius of sphere on screen
float getApparentRadius(float worldRadius, float viewDepth, float fov) {
    if (viewDepth < NEAR_ZERO) return 1.0;
    return worldRadius / (viewDepth * tan(fov * 0.5));
}

// Project sphere to screen space
SphereProjection projectSphere(vec3 sphereCenter, float sphereRadius, CameraData cam) {
    SphereProjection result;
    
    vec3 ndc = worldToNDC(sphereCenter, cam);
    
    // Behind camera
    if (ndc.z < cam.near) {
        result.isVisible = false;
        return result;
    }
    
    result.viewDepth = ndc.z;
    result.screenCenter = ndcToScreen(ndc.xy);
    result.apparentRadius = getApparentRadius(sphereRadius, result.viewDepth, cam.fov);
    result.isVisible = true;
    
    return result;
}

#endif
```

**Files to create:**
- [ ] `include/camera/projection.glsl`

**Code to extract from:**
- `camera_core.glsl` - worldToNDC, ndcToScreen, getApparentRadius
- `projection_modes.glsl` - projectSphere

---

### 2.5 Create `camera/depth.glsl`
Depth buffer utilities.

```glsl
// camera/depth.glsl
#ifndef CAMERA_DEPTH_GLSL
#define CAMERA_DEPTH_GLSL

#include "types.glsl"
#include "rays.glsl"

// Convert depth buffer value to linear depth
float linearizeDepth(float rawDepth, float near, float far) {
    float ndcZ = rawDepth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndcZ * (far - near));
}

// Reconstruct world position from depth buffer
vec3 reconstructWorldPos(vec2 texCoord, float linearDepth, CameraData cam) {
    Ray ray = getRayFromBasis(texCoord, cam);
    
    // Convert Z-depth to ray distance
    float rayDistance = linearDepth / dot(ray.direction, cam.forward);
    
    return ray.origin + ray.direction * rayDistance;
}

// Check if depth is at far plane (sky)
bool isSky(float rawDepth) {
    return rawDepth > 0.9999;
}

#endif
```

**Files to create:**
- [ ] `include/camera/depth.glsl`

**Code to extract from:**
- `depth_utils.glsl` - linearizeDepth, reconstructWorldPos

---

## Phase 3: SDF Domain
**Duration:** ~1 hour

### 3.1 Create `sdf/primitives.glsl`
Basic SDF shapes.

```glsl
// sdf/primitives.glsl
#ifndef SDF_PRIMITIVES_GLSL
#define SDF_PRIMITIVES_GLSL

// Sphere
float sdfSphere(vec3 p, vec3 center, float radius) {
    return length(p - center) - radius;
}

// Capsule (line segment with radius)
float sdfCapsule(vec3 p, vec3 a, vec3 b, float radius) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - radius;
}

// Tapered capsule (different radii at each end)
float sdfTaperedCapsule(vec3 p, vec3 a, vec3 b, float radiusA, float radiusB) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    float radius = mix(radiusA, radiusB, h);
    return length(pa - ba * h) - radius;
}

// Torus (donut shape)
float sdfTorus(vec3 p, vec3 center, float majorR, float minorR) {
    vec3 q = p - center;
    vec2 qxz = vec2(length(q.xz), q.y);
    return length(qxz - vec2(majorR, 0.0)) - minorR;
}

// Box
float sdfBox(vec3 p, vec3 center, vec3 halfSize) {
    vec3 d = abs(p - center) - halfSize;
    return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0);
}

// Infinite cylinder (along Y axis)
float sdfCylinder(vec3 p, vec3 center, float radius) {
    return length(p.xz - center.xz) - radius;
}

// Polygon (2D, for ground effects)
float sdfPolygon(vec2 p, int sides, float radius) {
    float angle = atan(p.y, p.x);
    float segment = TWO_PI / float(sides);
    float a = mod(angle, segment) - segment * 0.5;
    return length(p) * cos(a) - radius;
}

#endif
```

**Files to create:**
- [ ] `include/sdf/primitives.glsl`

**Code to extract from:**
- `sdf_library.glsl` - all primitive functions

---

### 3.2 Create `sdf/operations.glsl`
SDF boolean operations.

```glsl
// sdf/operations.glsl
#ifndef SDF_OPERATIONS_GLSL
#define SDF_OPERATIONS_GLSL

#include "../core/math_utils.glsl"

// Union (closest surface)
float sdfUnion(float a, float b) {
    return min(a, b);
}

// Intersection (shared interior)
float sdfIntersection(float a, float b) {
    return max(a, b);
}

// Subtraction (a minus b)
float sdfSubtraction(float a, float b) {
    return max(a, -b);
}

// Smooth union (blended surfaces)
float sdfSmoothUnion(float a, float b, float k) {
    return smoothMin(a, b, k);
}

// Onion (hollow shell)
float sdfOnion(float d, float thickness) {
    return abs(d) - thickness;
}

// Round (add radius to edges)
float sdfRound(float d, float radius) {
    return d - radius;
}

#endif
```

**Files to create:**
- [ ] `include/sdf/operations.glsl`

**Code to extract from:**
- `sdf_library.glsl` - smoothMin usage patterns

---

### 3.3 Create `sdf/orbital_system.glsl`
Complex orbital geometry.

```glsl
// sdf/orbital_system.glsl
#ifndef SDF_ORBITAL_SYSTEM_GLSL
#define SDF_ORBITAL_SYSTEM_GLSL

#include "primitives.glsl"
#include "operations.glsl"

// Get position of nth orbital sphere
vec3 getOrbitalPosition(vec3 center, int index, int count, float distance, float phase) {
    float angle = (float(index) / float(count)) * TWO_PI + phase;
    return center + vec3(
        cos(angle) * distance,
        0.0,
        sin(angle) * distance
    );
}

// SDF for orbital spheres only
float sdfOrbitalSpheres(vec3 p, vec3 center, float orbitalRadius, 
                        float orbitDistance, int count, float phase) {
    float d = 1e10;
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, sdfSphere(p, orbPos, orbitalRadius));
    }
    return d;
}

// SDF for beams from orbitals
float sdfOrbitalBeams(vec3 p, vec3 center, float orbitalRadius,
                      float orbitDistance, int count, float phase,
                      float beamHeight, float beamRadius, float taper) {
    if (beamHeight < 0.1) return 1e10;
    
    float d = 1e10;
    float topRadius = beamRadius * taper;
    
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        vec3 beamEnd = orbPos + vec3(0.0, beamHeight, 0.0);
        d = min(d, sdfTaperedCapsule(p, orbPos, beamEnd, beamRadius, topRadius));
    }
    return d;
}

// Combined orbital system (spheres + optional beams)
float sdfOrbitalSystem(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                       float orbitDistance, int count, float phase) {
    // Main central sphere (if radius > 0)
    float d = (mainRadius > 0.1) ? sdfSphere(p, center, mainRadius) : 1e10;
    
    // Orbital spheres
    d = min(d, sdfOrbitalSpheres(p, center, orbitalRadius, orbitDistance, count, phase));
    
    return d;
}

#endif
```

**Files to create:**
- [ ] `include/sdf/orbital_system.glsl`

**Code to extract from:**
- `orbital_math.glsl` - getOrbitalPosition
- `sdf_library.glsl` - sdfOrbitalSystem
- `raymarching.glsl` - sdfOrbitalSpheresOnly, sdfBeams

---

## Phase 4: Rendering Domain
**Duration:** ~1.5 hours

### 4.1 Create `rendering/raymarch.glsl`
Generic raymarch loop.

```glsl
// rendering/raymarch.glsl
#ifndef RENDERING_RAYMARCH_GLSL
#define RENDERING_RAYMARCH_GLSL

#include "../camera/types.glsl"
#include "../core/constants.glsl"

// Generic raymarch function - you provide the SDF
// Returns RaymarchHit with result
#define DECLARE_RAYMARCH(name, sdfCall) \
RaymarchHit name(Ray ray, float maxDist) { \
    RaymarchHit result; \
    result.hit = false; \
    result.distance = 0.0; \
    \
    float t = 0.0; \
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) { \
        vec3 p = ray.origin + ray.direction * t; \
        float d = sdfCall; \
        \
        if (d < RAYMARCH_EPSILON) { \
            result.hit = true; \
            result.distance = t; \
            result.position = p; \
            break; \
        } \
        \
        if (t > maxDist) break; \
        t += d * 0.8; \
    } \
    \
    return result; \
}

// Calculate normal from SDF gradient
vec3 calcNormalFromSDF(vec3 p, float eps, float sdfCenter,
                       float sdfPlusX, float sdfPlusY, float sdfPlusZ) {
    vec3 grad = vec3(
        sdfPlusX - sdfCenter,
        sdfPlusY - sdfCenter,
        sdfPlusZ - sdfCenter
    );
    float len = length(grad);
    return len > NEAR_ZERO ? grad / len : vec3(0.0, 1.0, 0.0);
}

// Rim lighting calculation
float calcRimLighting(vec3 normal, vec3 viewDir, float power) {
    float rim = 1.0 - abs(dot(normal, viewDir));
    return pow(rim, power);
}

#endif
```

**Files to create:**
- [ ] `include/rendering/raymarch.glsl`

**Code to extract from:**
- `raymarching.glsl` - raymarch loop, calcNormal
- `projection_modes.glsl` - raymarchSphere

---

### 4.2 Create `rendering/screen_effects.glsl`
2D screen-space effect utilities.

```glsl
// rendering/screen_effects.glsl
#ifndef RENDERING_SCREEN_EFFECTS_GLSL
#define RENDERING_SCREEN_EFFECTS_GLSL

#include "../camera/types.glsl"

// Distance from pixel to projected point (in UV space)
float distToProjected(vec2 texCoord, SphereProjection proj, float aspectRatio) {
    vec2 delta = texCoord - proj.screenCenter;
    delta.x *= aspectRatio;  // Correct for aspect ratio
    return length(delta);
}

// Normalized distance (0 at center, 1 at edge of apparent radius)
float normalizedDistToProjected(vec2 texCoord, SphereProjection proj, float aspectRatio) {
    return distToProjected(texCoord, proj, aspectRatio) / proj.apparentRadius;
}

// Circular glow falloff
float circularGlow(float normalizedDist, float falloff) {
    return 1.0 / (1.0 + pow(normalizedDist, falloff));
}

// Ring effect (peaks at specific radius)
float ringEffect(float normalizedDist, float ringRadius, float thickness) {
    float distFromRing = abs(normalizedDist - ringRadius);
    return smoothstep(thickness, 0.0, distFromRing);
}

// Radial gradient
float radialGradient(float normalizedDist, float innerRadius, float outerRadius) {
    return 1.0 - smoothstep(innerRadius, outerRadius, normalizedDist);
}

#endif
```

**Files to create:**
- [ ] `include/rendering/screen_effects.glsl`

**Code to extract from:**
- `energy_orb_v2.glsl` - various 2D calculations
- `glow_utils.glsl` - ringContribution patterns

---

### 4.3 Create `rendering/depth_mask.glsl`
Depth-based occlusion for effects.

```glsl
// rendering/depth_mask.glsl
#ifndef RENDERING_DEPTH_MASK_GLSL
#define RENDERING_DEPTH_MASK_GLSL

// Check if effect is occluded by scene geometry
bool isOccludedByScene(float effectDepth, float sceneDepth, float bias) {
    return effectDepth > (sceneDepth + bias);
}

// Soft occlusion (fade near geometry)
float softOcclusion(float effectDepth, float sceneDepth, float fadeDistance) {
    float diff = sceneDepth - effectDepth;
    if (diff < 0.0) return 0.0;  // Behind geometry
    return smoothstep(0.0, fadeDistance, diff);
}

// For screen-projected effects: compare view depth
float depthMaskForProjection(float projectedViewDepth, float linearSceneDepth, float fadeWidth) {
    if (projectedViewDepth > linearSceneDepth + fadeWidth) return 0.0;  // Fully occluded
    if (projectedViewDepth < linearSceneDepth - fadeWidth) return 1.0;  // Fully visible
    
    // Partial fade
    float t = (linearSceneDepth - projectedViewDepth + fadeWidth) / (2.0 * fadeWidth);
    return smoothstep(0.0, 1.0, t);
}

#endif
```

**Files to create:**
- [ ] `include/rendering/depth_mask.glsl`

---

## Phase 5: Effect Implementations
**Duration:** ~2 hours

### 5.1 Create `effects/energy_orb_v1.glsl`
Raymarched energy orb.

**Files to create:**
- [ ] `include/effects/energy_orb_v1.glsl`

**Code to extract from:**
- `field_visual.fsh` - V1 rendering logic

---

### 5.2 Refactor `effects/energy_orb_v2.glsl`
Screen-projected energy orb.

**Files to modify:**
- [ ] `include/energy_orb_v2.glsl` → move to `include/effects/energy_orb_v2.glsl`
- [ ] Update to use new library imports

---

### 5.3 Create `effects/orbitals_v1.glsl`
Raymarched orbital spheres.

**Files to create:**
- [ ] `include/effects/orbitals_v1.glsl`

**Code to extract from:**
- `raymarching.glsl` - orbital-specific raymarching
- `shockwave_ring.fsh` - orbital rendering section

---

### 5.4 Create `effects/orbitals_v2.glsl` (NEW)
Screen-projected orbital spheres.

```glsl
// effects/orbitals_v2.glsl
#ifndef EFFECTS_ORBITALS_V2_GLSL
#define EFFECTS_ORBITALS_V2_GLSL

#include "../camera/types.glsl"
#include "../camera/projection.glsl"
#include "../sdf/orbital_system.glsl"
#include "../rendering/screen_effects.glsl"
#include "../rendering/depth_mask.glsl"

struct OrbitalV2Config {
    vec3 center;           // Center of orbital system
    float orbitDistance;   // Distance from center
    float orbitalRadius;   // Size of each orbital
    int count;             // Number of orbitals
    float phase;           // Rotation angle
    vec3 bodyColor;        // Main color
    vec3 glowColor;        // Glow color
    float glowIntensity;   // Glow brightness
};

// Render all orbitals using screen projection
vec4 renderOrbitalsV2(vec2 texCoord, OrbitalV2Config config, 
                      CameraData cam, float linearDepth) {
    vec4 result = vec4(0.0);
    
    for (int i = 0; i < config.count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(
            config.center, i, config.count, 
            config.orbitDistance, config.phase
        );
        
        // Project to screen
        SphereProjection proj = projectSphere(orbPos, config.orbitalRadius, cam);
        
        if (!proj.isVisible) continue;
        
        // Depth occlusion
        float depthMask = depthMaskForProjection(proj.viewDepth, linearDepth, 1.0);
        if (depthMask < 0.01) continue;
        
        // Calculate glow
        float dist = normalizedDistToProjected(texCoord, proj, cam.aspect);
        
        if (dist < 2.0) {  // Only process nearby pixels
            // Core (solid color)
            float coreMask = smoothstep(1.0, 0.8, dist);
            vec3 coreColor = config.bodyColor * coreMask;
            
            // Glow (falls off outside)
            float glowMask = circularGlow(dist, 2.0) * config.glowIntensity;
            vec3 glowColor = config.glowColor * glowMask;
            
            // Combine
            result.rgb += (coreColor + glowColor) * depthMask;
            result.a = max(result.a, max(coreMask, glowMask) * depthMask);
        }
    }
    
    return result;
}

#endif
```

**Files to create:**
- [ ] `include/effects/orbitals_v2.glsl`

---

### 5.5 Create `effects/beams_v1.glsl`
Raymarched beams.

**Files to create:**
- [ ] `include/effects/beams_v1.glsl`

**Code to extract from:**
- `raymarching.glsl` - sdfBeams, beam raymarching

---

### 5.6 Create `effects/beams_v2.glsl` (NEW)
Screen-projected beams.

**Files to create:**
- [ ] `include/effects/beams_v2.glsl`

---

### 5.7 Create `effects/shockwave_ring.glsl`
Depth-based shockwave rings.

**Files to create:**
- [ ] `include/effects/shockwave_ring.glsl`

**Code to extract from:**
- `shockwave_ring.fsh` - ring calculation logic
- `glow_utils.glsl` - ringContribution

---

## Phase 6: Migration & Integration
**Duration:** ~1.5 hours

### 6.1 Update Main Shaders
- [ ] `field_visual.fsh` - use new effect files
- [ ] `shockwave_ring.fsh` - use new library structure
- [ ] Keep backward compatibility through aliases

### 6.2 Update Preprocessor
- [ ] Ensure include paths work with new structure
- [ ] Test nested includes (effects → rendering → camera → core)

### 6.3 Deprecate Old Files
- [ ] Add deprecation notices to old files
- [ ] Keep old files temporarily for reference
- [ ] Plan removal in future version

---

## Phase 7: Testing & Polish
**Duration:** ~1 hour

### 7.1 Visual Regression Testing
- [ ] Compare V1 vs V2 output for each effect
- [ ] Test all combinations (walking, flying, standing)
- [ ] Check edge cases (looking up/down, far distance)

### 7.2 Performance Testing
- [ ] Compare FPS: V1 vs V2 for each effect
- [ ] Identify optimization opportunities
- [ ] Document performance characteristics

### 7.3 Documentation
- [ ] Update header comments in all files
- [ ] Create usage examples
- [ ] Document uniform requirements

---

## Estimated Total Time: ~10 hours

### Priority Order
1. **Phase 0-2:** Camera domain (most critical for stability)
2. **Phase 5.4:** Orbitals V2 (immediate need)
3. **Phase 3:** SDF domain
4. **Phase 4:** Rendering domain
5. **Phase 5:** Remaining effects
6. **Phase 6-7:** Integration and testing

---

## Success Criteria

1. ✅ V1 and V2 give visually identical results
2. ✅ No jitter/flicker in any state (walking, flying, standing)
3. ✅ Clear file organization with documented dependencies
4. ✅ Each file under 300 lines (single responsibility)
5. ✅ No duplicate code between files
6. ✅ Easy to add new effects following established patterns
