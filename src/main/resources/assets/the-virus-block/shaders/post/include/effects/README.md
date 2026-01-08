# effects/ - Effect Implementation Layer

**Dependencies:** All lower layers (core/, camera/, sdf/, rendering/)  
**Used by:** Main shaders (field_visual.fsh, shockwave_ring.fsh, etc.)

---

## ✅ STATUS: FAITHFULLY EXTRACTED (2026-01-03)

All implementations have been re-extracted EXACTLY from original source files.
Every function, parameter, and magic number has been verified to match.

**See:** `docs/shader_library_audit_gaps.md` for verification details.

---

## Purpose

Complete, ready-to-use effect implementations.
Import one file to get a full effect.

## Files

### energy_orb_types.glsl
```glsl
#include "include/effects/energy_orb_types.glsl"
```

**Exports:**
```glsl
struct EnergyOrbConfig {
    vec3 center;
    float radius;
    vec4 primaryColor;
    vec4 secondaryColor;
    vec4 tertiaryColor;
    float intensity;
    float coreSize;
    float edgeSharpness;
    float spiralDensity;
    float spiralTwist;
    float time;
    float glowLineCount;
    float glowLineIntensity;
    float coronaWidth;
    bool showCorona;
    bool showExternalRays;
};

struct EnergyOrbHit { ... };
EnergyOrbConfig defaultEnergyOrbConfig();
EnergyOrbHit noOrbHit();
vec3 orbSphericalCoords(localPos, radius);
```

### energy_orb_v1.glsl - ⚠️ INCOMPLETE
```glsl
#include "include/effects/energy_orb_v1.glsl"
```

**Paradigm:** V1 Raymarching (Bottom-Up)

**⚠️ KNOWN ISSUES:**
- Core uses wrong falloff function
- Spirals use simplified math (not voronoi)
- Glow lines are completely different algorithm
- Missing many animation parameters

**Exports:**
- `energyOrbCore(cfg, distToCenter)` - Core glow
- `energyOrbEdge(cfg, rim)` - Rim lighting
- `energyOrbSpirals(cfg, localPos, distToCenter)` - Spiral patterns
- `energyOrbGlowLines(cfg, localPos, distToCenter)` - Radial lines
- `raymarchEnergyOrb(ray, cfg, maxDist)` - Raymarch loop
- `renderEnergyOrbV1(ray, forward, cfg, sceneDepth)` - Complete render

### orbitals_v1.glsl - ⚠️ PARTIAL
```glsl
#include "include/effects/orbitals_v1.glsl"
```

**Paradigm:** V1 Raymarching (Bottom-Up)

**⚠️ KNOWN ISSUES:**
- Missing BeamWidthAbs/BeamWidthScale logic
- Slightly simplified from raymarching.glsl

**Exports:**
```glsl
struct OrbitalsV1Config { ... };
OrbitalsV1Config defaultOrbitalsV1Config();

float sdfOrbitalsOnly(p, cfg);
float sdfBeamsOnly(p, cfg);
float sdfOrbitalsAndBeams(p, cfg);
vec3 calcOrbitalNormal(p, cfg);

struct OrbitalsHit { ... };
OrbitalsHit noOrbitalsHit();
OrbitalsHit raymarchOrbitals(ray, cfg, maxDist);

vec4 renderOrbitalsV1(ray, forward, cfg, sceneDepth);
```

### orbitals_v2.glsl - NEW (no original to compare)
```glsl
#include "include/effects/orbitals_v2.glsl"
```

**Paradigm:** V2 Screen-Space Projection (Top-Down)

**Exports:**
```glsl
struct OrbitalsV2Config { ... };
OrbitalsV2Config defaultOrbitalsV2Config();

vec4 renderSingleOrbitalV2(texCoord, orbPos, radius, cam, sceneDepth, ...);
vec4 renderBeamV2(texCoord, beamStart, beamEnd, cam, sceneDepth, ...);
vec4 renderOrbitalsV2(texCoord, cfg, cam, sceneDepth);
```

### shockwave_ring.glsl - ⚠️ PARTIAL
```glsl
#include "include/effects/shockwave_ring.glsl"
```

**Paradigm:** V3 Depth-Based Surface Following

**Exports:**
```glsl
struct ShockwaveRingConfig { ... };
ShockwaveRingConfig defaultShockwaveRingConfig();

float shockwaveGlowFalloff(dist, radius);
float ringContribution(dist, ringDist, coreThickness, glowWidth, intensity);
float getShapeDistance(worldPos, cfg);

vec4 renderShockwaveRing(worldPos, cfg);
vec4 renderMultipleRings(worldPos, cfg, ringRadii[8], ringCount);
```

---

## Usage Pattern (V1)

```glsl
#include "include/effects/energy_orb_v1.glsl"

// Build config from uniforms
EnergyOrbConfig cfg;
cfg.center = vec3(CenterX, CenterY, CenterZ);
cfg.radius = Radius;
cfg.primaryColor = vec4(PrimaryR, PrimaryG, PrimaryB, PrimaryA);
// ... fill rest from uniforms

// Build camera
CameraData cam = buildCameraData(camPos, forward, fov, aspect, near, far);

// Generate ray
Ray ray = getRayAdaptive(texCoord, cam, InvViewProj, IsFlying);

// Get scene depth
float rawDepth = texture(DepthSampler, texCoord).r;
float sceneDepth = linearizeDepth(rawDepth, near, far);

// Render
vec4 orbColor = renderEnergyOrbV1(ray, cam.forward, cfg, sceneDepth);
```

## Usage Pattern (V2)

```glsl
#include "include/effects/orbitals_v2.glsl"

// Build config
OrbitalsV2Config cfg = defaultOrbitalsV2Config();
cfg.center = systemCenter;
cfg.count = orbitalCount;
cfg.phase = time * rotationSpeed;
// ...

// Build camera
CameraData cam = buildCameraData(...);

// Get scene depth
float sceneDepth = linearizeDepth(texture(DepthSampler, texCoord).r, near, far);

// Render
vec4 orbColor = renderOrbitalsV2(texCoord, cfg, cam, sceneDepth);
```

---

## ⚠️ Before Using in Production

1. Compare implementation against original source
2. Verify visual output matches original
3. Test all animation parameters
4. Check depth occlusion behavior
5. Verify walking/flying mode handling
