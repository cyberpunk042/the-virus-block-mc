# Camera & Projection Library Audit
## Status: Working (as of 2026-01-03)

---

## Architecture Overview

### Two Fundamental Rendering Approaches

#### V1: RAYMARCHING ("Bottom-Up")
- **Direction:** Camera → World (shooting rays outward)
- **Process:** 
  1. For each screen pixel, compute ray direction
  2. March ray through world space
  3. Find intersections with geometry (SDF)
  4. Return hit position and properties
- **Camera Data Required:**
  - `InvViewProj` matrix (for accurate ray directions at distance)
  - OR yaw/pitch forward + FOV (for stable rays when walking)
- **Challenge:** Ray direction accuracy is critical. Small angle errors become large position errors at distance.
- **Files:**
  - `camera_core.glsl` - `getRayDirection()`, `getRayDirectionMatrix()`
  - `raymarching.glsl` - SDF raymarching for orbitals
  - `projection_modes.glsl` - `raymarchSphere()`

#### V2: SCREEN-SPACE PROJECTION ("Top-Down")
- **Direction:** World → Screen (projecting 3D to 2D)
- **Process:**
  1. Take 3D position (orb center)
  2. Project to 2D screen coordinates
  3. Draw 2D effect centered on that position
- **Camera Data Required:**
  - Forward vector (for view-space transformation)
  - FOV (correct aspect ratio and perspective)
  - CRITICAL: Must use DYNAMIC FOV (via GameRendererAccessor)
- **Challenge:** FOV must match actual rendered FOV exactly, otherwise "lensing" distortion occurs.
- **Files:**
  - `camera_core.glsl` - `worldToNDC()`, `projectToScreen()`
  - `projection_modes.glsl` - `projectSphere()`, `SphereProjection` struct
  - `energy_orb_v2.glsl` - Complete V2 orb implementation

---

## Camera Data Sources

### 1. Yaw/Pitch Forward (Stable)
- **Source:** `camera.getYaw()`, `camera.getPitch()` in Java
- **Passed via:** `ForwardX`, `ForwardY`, `ForwardZ` uniforms
- **Pros:** Stable, no camera bob, consistent
- **Cons:** Doesn't include camera bob → mismatch with depth buffer when walking
- **Best for:** Walking on ground, close-range rendering

### 2. InvViewProj Matrix (Accurate)
- **Source:** `projectionMatrix.mul(positionMatrix).invert()` in Java
- **Passed via:** `InvViewProj` mat4 uniform
- **Pros:** Matches exactly what Minecraft rendered
- **Cons:** Includes camera bob → causes jitter when walking
- **Best for:** Flying, long-range rendering where accuracy matters

### 3. Dynamic FOV (Essential for V2)
- **Source:** `GameRenderer.getFov(camera, tickDelta, true)` via accessor
- **CRITICAL:** Static FOV from settings doesn't include flying/sprinting changes
- **Failure mode:** "Lensing" effect - orb appears to stretch/compress from center

---

## State Detection & Mode Switching

### Current Strategy (Working)

```java
// Detect flying state
boolean currentlyFlying = client.player.getAbilities().flying;

// Detect horizontal velocity  
double horizontalSpeed = sqrt(velX*velX + velZ*velZ);
boolean hasVelocity = horizontalSpeed > 0.01;

// Transition cooldown - prevents flicker when state changes
if (stateChanged) {
    flyingTransitionCooldown = 10; // ~0.16 seconds
}

// Decision logic:
// - Transitioning: use yaw/pitch (safer)
// - Walking (velocity + not flying): use yaw/pitch
// - Flying (stabilized): use InvViewProj
float isFlying = (transitioning || walking) ? 0.0 : (flying ? 1.0 : 0.0);
```

### Why This Works
1. **Walking** causes camera bob → yaw/pitch doesn't have bob → no mismatch
2. **Flying** doesn't cause camera bob → InvViewProj is accurate and stable
3. **Transition cooldown** waits for FOV to stabilize before switching modes

---

## File Structure & Responsibilities

```
include/
├── camera_core.glsl      # Core camera basis & ray generation
│   ├── computeCameraBasis()       - Compute right/up from forward
│   ├── getRayDirection()          - Yaw/pitch based rays  
│   ├── getRayDirectionMatrix()    - InvViewProj based rays
│   ├── worldToNDC()               - World → NDC projection
│   └── screenToWorld()            - NDC → World unprojection
│
├── depth_utils.glsl      # Depth buffer utilities
│   ├── linearizeDepth()           - Convert depth buffer to linear
│   └── reconstructWorldPos()      - Get world pos from depth (SHOCKWAVE)
│
├── projection_modes.glsl # High-level projection abstractions
│   ├── SphereProjection struct    - Result of sphere projection
│   ├── projectSphere()            - Project 3D sphere to 2D
│   ├── raymarchSphere()           - Analytical sphere raymarching
│   ├── distanceToTarget()         - Distance calculations
│   └── billboardOffset()          - Camera-facing offsets
│
├── raymarching.glsl      # SDF raymarching for complex shapes
│   ├── sdfOrbitalSpheresOnly()    - Multiple orbital spheres
│   ├── sdfBeams()                 - Beam geometry
│   └── raymarchOrbitalSpheres()   - Full raymarch with corona
│
├── sdf_library.glsl      # Signed Distance Functions
│   └── Various SDF primitives
│
├── orbital_math.glsl     # Orbital positioning math
│   └── getOrbitalPosition()       - Calculate orbital positions
│
├── glow_utils.glsl       # Glow/bloom utilities
│
└── energy_orb_v2.glsl    # Complete V2 orb implementation
    └── renderEnergyOrbV2ProjectedCustom()  - Main V2 function
```

---

## Known Pitfalls & Lessons Learned

### 1. Camera-Relative Coordinates
- Minecraft renders in camera-relative space (camera at origin)
- All positions passed to shader must be: `worldPos - cameraPos`
- `camPos` in shader uniforms should be `(0, 0, 0)`

### 2. FOV Must Be Dynamic
- `client.options.getFov().getValue()` is WRONG for flying
- Must use `GameRenderer.getFov()` accessor
- Flying/sprinting change FOV dynamically

### 3. Walking Bob Mismatch
- When walking, camera bobs (rotation changes)
- InvViewProj includes this bob
- But camera POSITION doesn't include bob offset
- Result: Rays from InvViewProj point "wrong" relative to orb position
- Solution: Use yaw/pitch rays when walking

### 4. Transition Timing
- State changes (flying on/off) cause brief FOV animation
- Switching ray modes during animation causes flicker
- Solution: Cooldown timer before switching modes

---

## Refactoring Opportunities

### 1. Centralize State Detection
Currently scattered in `PostEffectPassMixin.java`. Could move to:
```java
class CameraStateTracker {
    boolean isFlying();
    boolean isWalking();
    boolean isTransitioning();
    float getIsFlying(); // For shader uniform
}
```

### 2. Unify Ray Generation
Currently have multiple similar implementations. Could consolidate:
```glsl
// Single function with mode parameter
vec3 getRay(vec2 uv, int mode); // mode: 0=yaw/pitch, 1=matrix
```

### 3. Document Uniform Dependencies
Each include file should clearly state required uniforms at top.

### 4. Remove Duplicate Code
Same ray calculation appears in LEGACY, LIBRARY, MATRIX modes in main shader.

---

## Test Checklist

- [ ] V1 walking on ground - no jitter
- [ ] V1 standing still - no jitter  
- [ ] V1 flying far away - orb at correct position
- [ ] V1 transition flying→walking - no flicker
- [ ] V2 walking on ground - stable, slight bob OK
- [ ] V2 flying - orb at correct position, no sinking
- [ ] V2 sprinting - no lensing
- [ ] Both versions with different FOV settings
