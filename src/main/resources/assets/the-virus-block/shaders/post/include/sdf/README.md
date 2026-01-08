# sdf/ - Signed Distance Function Layer

**Dependencies:** core/  
**Used by:** effects/, main shaders

---

## Purpose

Signed Distance Functions (SDFs) define shapes mathematically.
Returns: negative inside, zero on surface, positive outside.

## Files

### primitives.glsl
```glsl
#include "include/sdf/primitives.glsl"
```

**3D Primitives:**
- `sdfSphere(p, center, radius)` - Sphere
- `sdfBox(p, center, halfSize)` - Axis-aligned box
- `sdfTorus(p, center, majorR, minorR)` - Donut in XZ plane
- `sdfCapsule(p, a, b, r)` - Line segment with radius
- `sdfTaperedCapsule(p, a, b, rBottom, rTop)` - Cone-like capsule
- `sdfCylinder(p, center, radius)` - Infinite Y-axis cylinder
- `sdfPlane(p, normal, offset)` - Infinite plane

**2D Primitives:**
- `sdfCircle(p, center, radius)` - Circle
- `sdfPolygon(p, sides, radius)` - Regular n-gon
- `sdfLine2D(p, a, b, thickness)` - Line segment

**Normals:**
- `calcNormalFromGradient(...)` - From SDF samples
- `sphereNormal(surfacePoint, sphereCenter)` - Simple sphere case

### operations.glsl
```glsl
#include "include/sdf/operations.glsl"
```

**Boolean Operations (Hard):**
- `sdfUnion(a, b)` / `sdf_merge(a, b)` - Combine shapes
- `sdfIntersection(a, b)` / `sdf_intersect(a, b)` - Overlap only
- `sdfSubtraction(a, b)` - Remove b from a

**Smooth Operations (Rounded):**
- `sdfSmoothUnion(a, b, radius)` - Rounded union
- `round_merge(a, b, radius)` - Ronja's smooth union
- `sdfSmoothIntersection(a, b, radius)` - Rounded intersection
- `round_intersect(a, b, radius)` - Ronja's version
- `smin(a, b, k)` - IQ's polynomial smooth min

**Chamfer (Beveled):**
- `sdfChamferUnion(a, b, chamferSize)` / `champfer_merge(...)`

**Modifications:**
- `sdfOnion(d, thickness)` - Hollow out (shell)
- `sdfRound(d, radius)` - Round all edges
- `sdfElongate(p, h)` - Stretch shape

### orbital_system.glsl
```glsl
#include "include/sdf/orbital_system.glsl"
```

**Orbital Positions:**
- `getOrbitalPosition(center, index, count, distance, phase)` - 3D position
- `getOrbitalPosition2D(center, index, count, distance, phase)` - 2D version

**Orbital SDFs:**
- `sdfOrbitalSpheres(p, center, orbitalRadius, orbitDist, count, phase)` - All orbitals
- `sdfOrbitalBeams(p, ...)` - Beams rising from orbitals
- `sdfOrbitalsAndBeams(p, ...)` - Combined

**2D Flower Patterns (for shockwaves):**
- `sdfOrbitalSystem2D(p, center, mainR, orbR, orbitDist, count, phase, blend)` - Individual mode
- `sdfCombinedFlower2D(...)` - Combined mode (petal contours)

**3D Orbital System:**
- `sdfOrbitalSystem3D(p, center, mainR, orbR, orbitDist, count, phase, blend, combinedMode)`

---

## Usage Patterns

### Simple Shape
```glsl
float d = sdfSphere(p, vec3(0.0), 1.0);
if (d < 0.0) {
    // Inside sphere
}
```

### Combined Shapes with Smooth Blend
```glsl
float sphere1 = sdfSphere(p, center1, r1);
float sphere2 = sdfSphere(p, center2, r2);
float combined = round_merge(sphere1, sphere2, 0.5);  // Smooth union
```

### Orbital System for Shockwave
```glsl
float dist = sdfOrbitalSystem3D(
    worldPos,
    systemCenter,
    0.0,           // No main circle
    orbitalRadius,
    orbitDistance,
    6,             // 6 orbitals
    phase,
    blendRadius,
    true           // Combined mode = flower shape
);
```

---

## ⚠️ Notes

- SDFs return SIGNED distance (negative = inside)
- For proper normals, sample multiple points and take gradient
- Smooth operations create "meat" at intersections (proper SDF, not just visual blend)
