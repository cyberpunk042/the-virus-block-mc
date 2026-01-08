// ═══════════════════════════════════════════════════════════════════════════
// SDF: PRIMITIVE SHAPES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Basic signed distance functions for geometric primitives.
// These return the distance to the surface (negative inside, positive outside).
//
// Include: #include "include/sdf/primitives.glsl"
// Prerequisites: core/constants.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SDF_PRIMITIVES_GLSL
#define SDF_PRIMITIVES_GLSL

#include "../core/constants.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// 3D PRIMITIVES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Sphere SDF.
 * 
 * @param p       Sample point
 * @param center  Sphere center
 * @param radius  Sphere radius
 */
float sdfSphere(vec3 p, vec3 center, float radius) {
    return length(p - center) - radius;
}

/**
 * Box SDF.
 * 
 * @param p         Sample point
 * @param center    Box center
 * @param halfSize  Half-extents in each axis
 */
float sdfBox(vec3 p, vec3 center, vec3 halfSize) {
    vec3 q = abs(p - center) - halfSize;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

/**
 * Torus SDF (donut shape in XZ plane).
 * 
 * @param p        Sample point
 * @param center   Torus center
 * @param majorR   Distance from center to tube center
 * @param minorR   Tube radius
 */
float sdfTorus(vec3 p, vec3 center, float majorR, float minorR) {
    vec3 q = p - center;
    vec2 t = vec2(length(q.xz) - majorR, q.y);
    return length(t) - minorR;
}

/**
 * Capsule SDF (line segment with radius).
 * Perfect for beams.
 * 
 * @param p   Sample point
 * @param a   Start point
 * @param b   End point
 * @param r   Radius
 */
float sdfCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    vec3 closest = a + t * ab;
    return length(p - closest) - r;
}

/**
 * Tapered capsule SDF (cone-like with hemispherical caps).
 * Different radii at each end - great for beams that taper.
 * 
 * @param p        Sample point  
 * @param a        Start point (bottom)
 * @param b        End point (top)
 * @param rBottom  Radius at start
 * @param rTop     Radius at end
 */
float sdfTaperedCapsule(vec3 p, vec3 a, vec3 b, float rBottom, float rTop) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    float radius = mix(rBottom, rTop, t);
    vec3 closest = a + t * ab;
    return length(p - closest) - radius;
}

/**
 * Infinite cylinder SDF (along Y axis).
 * 
 * @param p       Sample point
 * @param center  Cylinder center
 * @param radius  Cylinder radius
 */
float sdfCylinder(vec3 p, vec3 center, float radius) {
    return length(p.xz - center.xz) - radius;
}

/**
 * Plane SDF.
 * 
 * @param p        Sample point
 * @param normal   Plane normal (normalized)
 * @param offset   Distance from origin along normal
 */
float sdfPlane(vec3 p, vec3 normal, float offset) {
    return dot(p, normal) - offset;
}

// ═══════════════════════════════════════════════════════════════════════════
// 2D PRIMITIVES (for ground-plane effects)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Circle SDF (2D) - centered version.
 */
float sdfCircle(vec2 p, vec2 center, float radius) {
    return length(p - center) - radius;
}

/**
 * Circle SDF (2D) - origin centered.
 * Alias for field_visual.fsh compatibility.
 */
float sdCircle(vec2 p, float radius) {
    return length(p) - radius;
}

/**
 * Regular polygon SDF (2D).
 * Creates n-sided shapes (triangle, square, pentagon, hex, etc.)
 * 
 * @param p      Sample point
 * @param sides  Number of sides
 * @param radius Distance to vertices
 */
float sdfPolygon(vec2 p, int sides, float radius) {
    float angle = atan(p.y, p.x);
    float segmentAngle = TWO_PI / float(sides);
    float d = cos(floor(0.5 + angle / segmentAngle) * segmentAngle - angle) * length(p);
    return d - radius;
}

/**
 * Line segment SDF (2D).
 * 
 * @param p          Sample point
 * @param a          Start point
 * @param b          End point
 * @param thickness  Line thickness (radius)
 */
float sdfLine2D(vec2 p, vec2 a, vec2 b, float thickness) {
    vec2 ba = b - a;
    vec2 pa = p - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - thickness;
}

/**
 * Alias for field_visual.fsh compatibility.
 */
float sdLine(vec2 p, vec2 a, vec2 b, float thickness) {
    vec2 ba = b - a;
    vec2 pa = p - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - thickness;
}

// ═══════════════════════════════════════════════════════════════════════════
// NORMAL CALCULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculate normal from SDF gradient (generic).
 * Works for any SDF by sampling nearby points.
 * 
 * Note: This is a helper macro since GLSL doesn't have function pointers.
 * Caller must provide their own SDF function.
 */
// Usage: vec3 normal = calcNormalFromGradient(p, eps, sdfValue, sdfPlusX, sdfPlusY, sdfPlusZ);
vec3 calcNormalFromGradient(vec3 p, float eps, float sdfCenter, 
                             float sdfPlusX, float sdfPlusY, float sdfPlusZ) {
    vec3 grad = vec3(
        sdfPlusX - sdfCenter,
        sdfPlusY - sdfCenter,
        sdfPlusZ - sdfCenter
    );
    float len = length(grad);
    return len > NEAR_ZERO ? grad / len : vec3(0.0, 1.0, 0.0);
}

/**
 * Sphere normal (simple case - just direction from center).
 */
vec3 sphereNormal(vec3 surfacePoint, vec3 sphereCenter) {
    return normalize(surfacePoint - sphereCenter);
}

#endif // SDF_PRIMITIVES_GLSL
