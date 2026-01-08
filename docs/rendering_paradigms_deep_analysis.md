# Deep Analysis: Rendering Approaches for Post-Processing Effects

## The Three Fundamental Paradigms

After analyzing the codebase, there are actually **THREE** distinct rendering paradigms, not two:

---

## Paradigm 1: RAYMARCHING ("Bottom-Up")

### Concept
Shoot rays from camera through each pixel, march along ray until hitting geometry.

### Mathematical Foundation
```
for each pixel:
    rayOrigin = cameraPos
    rayDir = getRayDirection(pixel, camera)
    
    for step in march:
        position = rayOrigin + rayDir * distance
        sdf = signedDistanceFunction(position, geometry)
        if sdf < epsilon:
            HIT! → compute color at this point
        distance += sdf  // step forward by SDF distance
```

### Use Cases
- **Solid 3D objects** (orbs, spheres, capsules)
- **Volumetric effects** (clouds, fog)
- **Complex boolean geometry** (union/intersection of shapes)
- **Sub-pixel precision** (smooth edges at any distance)

### Camera Requirements
- **Ray direction accuracy is CRITICAL**
- Small angle error → large position error at distance
- Must match depth buffer's camera orientation

### Current Implementation
- `energy_orb V1` - raymarch sphere SDF
- `orbitals` in shockwave_ring.fsh - raymarch multiple spheres
- `beams` - raymarch tapered capsules

### Files
```
raymarching.glsl      → raymarchOrbitalSpheres()
sdf_library.glsl      → sdfSphere(), sdfCapsule()
camera_core.glsl      → getRayDirectionMatrix(), getRayDirection()
```

---

## Paradigm 2: SCREEN-SPACE PROJECTION ("Top-Down")

### Concept  
Take 3D world positions, project to 2D screen coordinates, draw 2D effects.

### Mathematical Foundation
```
for effect_source in world:
    screenPos = worldToScreen(source.position, camera)
    apparentRadius = calculateApparentRadius(source.radius, distance, fov)
    
for each pixel:
    relativeUV = pixel - screenPos
    dist = length(relativeUV) / apparentRadius
    color = draw2DEffect(dist)  // circles, gradients, etc.
```

### Use Cases
- **Glows centered on a point** (orb aura)
- **Lens flares** (bright spots with radial effects)
- **Simple spherical effects** (when depth occlusion isn't critical)
- **Performance-sensitive effects** (cheaper than raymarching)

### Camera Requirements
- **FOV accuracy is CRITICAL** (dynamic FOV!)
- **Forward vector accuracy** for view-space transformation
- Small FOV error → "lensing" distortion at screen edges

### Current Implementation
- `energy_orb V2` - project center, draw 2D effect

### Files
```
energy_orb_v2.glsl    → renderEnergyOrbV2ProjectedCustom()
camera_core.glsl      → worldToNDC(), projectToScreen()
projection_modes.glsl → projectSphere(), SphereProjection struct
```

---

## Paradigm 3: DEPTH-BASED SURFACE FOLLOWING

### Concept
Read depth buffer to find world positions of existing geometry, apply effects relative to those positions.

### Mathematical Foundation  
```
for each pixel:
    depth = texture(DepthSampler, uv)
    worldPos = reconstructWorldPos(uv, depth, camera)
    
    // Effect relative to surface position (not camera)
    distance = length(worldPos - effectCenter)
    color = calculate effect based on distance
```

### Use Cases
- **Terrain-following effects** (shockwaves on ground)
- **Decals and projections** (effects that conform to geometry)
- **World-space distance fields** (rings expanding FROM a point)

### Camera Requirements
- **Must reconstruct world position accurately**
- Uses yaw/pitch forward (matches how depth was encoded)
- Less sensitive to camera bob (effect is on surface, not in space)

### Current Implementation
- **Shockwave rings** - reconstruct world pos, measure distance to target

### Files
```
depth_utils.glsl      → linearizeDepth(), reconstructWorldPos()
shockwave_ring.fsh    → getShapeDistance(worldPos, target)
```

---

## The Critical Mismatch Problem

### Why V1 Jitters When Walking

```
DEPTH BUFFER                          OUR RAYMARCHING
    ↓                                      ↓
Rendered with                         Rays generated with
positionMatrix                        yaw/pitch OR InvViewProj
(includes camera bob)                 
    ↓                                      ↓
Depth says "sphere                    Rays say "sphere 
surface is HERE"                      surface is THERE"
         ↓                                 ↓
         ╰────── MISMATCH! ────────────────╯
                     ↓
              VISUAL JITTER
```

### Solution Matrix

| State | Camera Bob? | Best Ray Source | Why |
|-------|-------------|-----------------|-----|
| Walking on ground | YES | yaw/pitch | Bob in matrix doesn't match camera pos |
| Standing still | minimal | either | Little difference |
| Flying | NO | InvViewProj | Most accurate, no bob issue |
| Sprinting | NO | InvViewProj | Same as flying |

---

## The Dynamic FOV Problem

### Why V2 Has "Lensing" When Flying

```
MINECRAFT RENDERS WITH                   V2 PROJECTS WITH
        ↓                                      ↓
Dynamic FOV = 90°                       Static FOV = 70°
(increased for flying)                  (from settings)
        ↓                                      ↓
World appears WIDER                     Orb projected for NARROW view
        ↓                                      ↓
        ╰────── MISMATCH! ────────────────╯
                     ↓
         Orb appears stretched/wrong position
         (worse at screen edges = "lensing")
```

### Solution
Use `GameRenderer.getFov()` through accessor mixin to get actual rendered FOV.

---

## Proposed Library Architecture

```
shaders/post/include/
├── core/                           # Foundation (no dependencies)
│   ├── constants.glsl              # Shared constants, epsilons
│   ├── math_utils.glsl             # Basic math (smoothstep, lerp, etc.)
│   └── color_utils.glsl            # Color space conversions
│
├── camera/                         # Camera handling (depends on core/)
│   ├── camera_types.glsl           # Structs: CameraData, RayData
│   ├── camera_uniforms.glsl        # Standard uniform declarations
│   ├── ray_generation.glsl         # getRayYawPitch(), getRayMatrix()
│   ├── projection.glsl             # worldToScreen(), screenToWorld()
│   └── depth.glsl                  # depth linearization, reconstruction
│
├── sdf/                            # Signed Distance Functions (depends on core/)
│   ├── primitives.glsl             # sphere, capsule, box, torus
│   ├── operations.glsl             # union, intersection, smooth blend
│   └── complex.glsl                # orbital systems, composed shapes
│
├── rendering/                      # Rendering techniques (depends on camera/, sdf/)
│   ├── raymarching.glsl            # Generic raymarch loop
│   ├── screen_projection.glsl      # Screen-space effect rendering
│   └── depth_masked.glsl           # Depth-based surface effects
│
└── effects/                        # Complete effect implementations
    ├── energy_orb_v1.glsl          # Raymarched orb
    ├── energy_orb_v2.glsl          # Screen-projected orb
    ├── orbitals_v1.glsl            # Raymarched orbital spheres
    ├── orbitals_v2.glsl            # Screen-projected orbitals (NEW)
    ├── beams_v1.glsl               # Raymarched beams
    ├── beams_v2.glsl               # Screen-projected beams (NEW)
    └── shockwave_rings.glsl        # Depth-based expanding rings
```

---

## V2 Versions Needed

### Orbitals V2 (Screen-Space)
Instead of raymarching each orbital sphere:
1. Calculate each orbital position in world space
2. Project each to screen coordinates  
3. Draw 2D glowing circles at those positions
4. Handle depth occlusion via depth buffer comparison

**Pros:** Faster, no raymarching artifacts
**Cons:** Less accurate at edges, no SDF blending

### Beams V2 (Screen-Space)
Instead of raymarching capsules:
1. Project beam start (orbital position) to screen
2. Project beam end (orbital + beam height) to screen
3. Draw 2D line/rectangle between them with glow
4. Handle depth occlusion

**Pros:** Much faster, no raymarching needed
**Cons:** No volumetric feel, perspective distortion at edges

---

## State Machine for Mode Switching

```java
enum RenderMode {
    YAW_PITCH_STABLE,    // Walking/ground, use yaw/pitch rays
    MATRIX_ACCURATE      // Flying/still, use InvViewProj rays  
}

class CameraStateDetector {
    private boolean isFlying;
    private boolean hasVelocity;
    private int transitionCooldown;
    
    RenderMode getCurrentMode() {
        // During transition: stay in STABLE mode
        if (transitionCooldown > 0) return YAW_PITCH_STABLE;
        
        // Walking (velocity + ground): STABLE
        if (hasVelocity && !isFlying) return YAW_PITCH_STABLE;
        
        // Flying or standing still: ACCURATE
        return MATRIX_ACCURATE;
    }
}
```

---

## Migration Path

### Phase 1: Document Current State ✓
- Audit complete
- Understand the three paradigms
- Identify mismatch sources

### Phase 2: Centralize Camera Logic
- Create unified CameraStateTracker in Java
- Create camera_types.glsl with standard structs
- Document uniform requirements in each file

### Phase 3: Create V2 Alternatives
- orbitals_v2.glsl - screen-projected orbitals
- beams_v2.glsl - screen-projected beams
- Test performance vs quality tradeoffs

### Phase 4: Unify Effect Entry Points
- Single uniform block for all effects
- Version toggle (V1=raymarch, V2=projection)
- Consistent depth occlusion handling

---

## Key Invariants to Maintain

1. **Camera position is always (0,0,0) in shader** - camera-relative coords
2. **Dynamic FOV is used for all projection** - via GameRendererAccessor
3. **Depth buffer matches ray source** - only use InvViewProj when no bob
4. **Transition cooldown prevents flicker** - wait for FOV to stabilize
5. **V1 and V2 should give same visual result** - just different methods
