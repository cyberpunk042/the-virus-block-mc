// ═══════════════════════════════════════════════════════════════════════════
// SDF: COMBINATION OPERATIONS
// ═══════════════════════════════════════════════════════════════════════════
// 
// Boolean and blending operations for combining multiple SDFs.
// These properly handle inside/outside/boundary for correct isodistance contours.
//
// Based on Ronja's tutorial and hg_sdf library conventions.
//
// Include: #include "include/sdf/operations.glsl"
// Prerequisites: core/constants.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SDF_OPERATIONS_GLSL
#define SDF_OPERATIONS_GLSL

#include "../core/constants.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// BASIC BOOLEAN OPERATIONS (hard edges)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Union (merge) - returns closest surface.
 * Creates the combined shape of both inputs.
 */
float sdfUnion(float a, float b) {
    return min(a, b);
}

// Alias for compatibility
float sdf_merge(float a, float b) {
    return min(a, b);
}

/**
 * Intersection - returns farthest surface.
 * Creates the overlapping region of both inputs.
 */
float sdfIntersection(float a, float b) {
    return max(a, b);
}

// Alias for compatibility
float sdf_intersect(float a, float b) {
    return max(a, b);
}

/**
 * Subtraction - removes shape b from shape a.
 * Creates a minus b.
 */
float sdfSubtraction(float a, float b) {
    return max(a, -b);
}

// ═══════════════════════════════════════════════════════════════════════════
// SMOOTH OPERATIONS (rounded edges)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Smooth union (round merge).
 * Creates smooth blend at intersection.
 * 
 * @param a       First shape SDF
 * @param b       Second shape SDF  
 * @param radius  Blend zone size (0 = sharp, larger = more rounded)
 */
float sdfSmoothUnion(float a, float b, float radius) {
    if (radius < NEAR_ZERO) return min(a, b);
    
    // Proper smooth min preserving SDF properties
    float h = max(radius - abs(a - b), 0.0) / radius;
    return min(a, b) - h * h * radius * 0.25;
}

// Alias for compatibility (from Ronja's tutorial)
float round_merge(float shape1, float shape2, float radius) {
    if (radius < NEAR_ZERO) return sdf_merge(shape1, shape2);
    
    // Grow shapes by radius, compute smooth inside distance
    vec2 intersectionSpace = vec2(shape1 - radius, shape2 - radius);
    intersectionSpace = min(intersectionSpace, 0.0);
    float insideDistance = -length(intersectionSpace);
    
    // Compute outside distance
    float simpleUnion = sdf_merge(shape1, shape2);
    float outsideDistance = max(simpleUnion, radius);
    
    return insideDistance + outsideDistance;
}

/**
 * Smooth intersection (round intersect).
 * Creates smooth blend at intersection edges.
 */
float sdfSmoothIntersection(float a, float b, float radius) {
    if (radius < NEAR_ZERO) return max(a, b);
    float h = max(radius - abs(a - b), 0.0) / radius;
    return max(a, b) + h * h * radius * 0.25;
}

// Alias for compatibility
float round_intersect(float shape1, float shape2, float radius) {
    if (radius < NEAR_ZERO) return sdf_intersect(shape1, shape2);
    
    vec2 intersectionSpace = vec2(shape1 + radius, shape2 + radius);
    intersectionSpace = max(intersectionSpace, 0.0);
    float outsideDistance = length(intersectionSpace);
    
    float simpleIntersection = sdf_intersect(shape1, shape2);
    float insideDistance = min(simpleIntersection, -radius);
    
    return outsideDistance + insideDistance;
}

/**
 * Smooth subtraction (round subtract).
 */
float sdfSmoothSubtraction(float a, float b, float radius) {
    return sdfSmoothIntersection(a, -b, radius);
}

// ═══════════════════════════════════════════════════════════════════════════
// CHAMFER OPERATIONS (beveled edges)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Chamfer union - creates beveled/chamfered corners.
 * Different from smooth union: produces flat bevels, not curves.
 */
float sdfChamferUnion(float a, float b, float chamferSize) {
    const float SQRT_05 = 0.70710678118;
    float simpleMerge = min(a, b);
    float chamfer = (a + b) * SQRT_05 - chamferSize;
    return min(simpleMerge, chamfer);
}

// Alias for compatibility
float champfer_merge(float shape1, float shape2, float champferSize) {
    return sdfChamferUnion(shape1, shape2, champferSize);
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGACY/ALTERNATE SMOOTH MIN
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Polynomial smooth min (IQ's classic version).
 * Simpler but less geometrically correct than round_merge.
 */
float smin(float a, float b, float k) {
    if (k < NEAR_ZERO) return min(a, b);
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// ═══════════════════════════════════════════════════════════════════════════
// SHAPE MODIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Onion - hollows out a shape (creates shell).
 * 
 * @param d          Original SDF
 * @param thickness  Shell thickness
 */
float sdfOnion(float d, float thickness) {
    return abs(d) - thickness;
}

/**
 * Round - rounds all edges of a shape.
 * 
 * @param d       Original SDF
 * @param radius  Rounding radius
 */
float sdfRound(float d, float radius) {
    return d - radius;
}

/**
 * Elongate - stretches a shape in one direction.
 * Apply to position before SDF evaluation.
 * 
 * @param p       Sample point
 * @param h       Elongation amount (vec3)
 * @return        Modified sample point
 */
vec3 sdfElongate(vec3 p, vec3 h) {
    return p - clamp(p, -h, h);
}

#endif // SDF_OPERATIONS_GLSL
