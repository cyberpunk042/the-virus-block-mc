// ═══════════════════════════════════════════════════════════════════════════
// CORE: NOISE & PATTERN UTILITIES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Procedural noise, hashing, and UV manipulation functions.
// Extracted from field_visual.fsh for reuse.
//
// Include: #include "include/core/noise_utils.glsl"
// Prerequisites: core/constants.glsl, core/math_utils.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORE_NOISE_UTILS_GLSL
#define CORE_NOISE_UTILS_GLSL

#include "constants.glsl"
#include "math_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// HASH FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// 2D to 2D hash (pseudo-random)
vec2 hash22(vec2 p) {
    mat2 m = mat2(vec2(15.27, 47.63), vec2(99.41, 89.98));
    return fract(sin(p * m) * 46839.32);
}

// 2D to 1D hash
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// 3D to 1D hash
float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

// ═══════════════════════════════════════════════════════════════════════════
// VORONOI NOISE
// ═══════════════════════════════════════════════════════════════════════════

// Animated Voronoi pattern
// Returns distance to nearest cell point
float voronoi(vec2 uv, float cellDensity, float animOffset) {
    vec2 gridUV = fract(uv * cellDensity);
    vec2 gridID = floor(uv * cellDensity);
    float minDist = 100.0;
    
    for (float y = -1.0; y <= 1.0; y++) {
        for (float x = -1.0; x <= 1.0; x++) {
            vec2 offset = vec2(x, y);
            vec2 n = hash22(gridID + offset);
            vec2 p = offset + vec2(
                sin(n.x * TAU + animOffset) * 0.5 + 0.5,
                cos(n.y * TAU + animOffset) * 0.5 + 0.5
            );
            float d = distance(gridUV, p);
            minDist = min(minDist, d);
        }
    }
    return minDist;
}

// Static Voronoi (no animation)
float voronoiStatic(vec2 uv, float cellDensity) {
    return voronoi(uv, cellDensity, 0.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// UV MANIPULATION
// ═══════════════════════════════════════════════════════════════════════════

// Twirl UV coordinates around a center point
// Creates a spiral distortion effect
vec2 twirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    float dist = length(delta);
    float angle = strength * dist;
    delta = rotate2D(angle) * delta;
    return delta + center;
}

// Radial UV (converts cartesian to polar-like)
// Returns vec2(angle, distance) normalized to 0-1 range
vec2 radialUV(vec2 uv, vec2 center) {
    vec2 delta = uv - center;
    float angle = (atan(delta.y, delta.x) / TWO_PI) + 0.5; // 0-1
    float dist = length(delta);
    return vec2(angle, dist);
}

// Spherical UV from 3D position (for mapping textures onto spheres)
vec2 sphericalUV(vec3 localPos, float radius) {
    vec3 n = localPos / max(radius, NEAR_ZERO);
    float theta = atan(n.z, n.x);                          // -PI to PI
    float phi = asin(clamp(n.y, -1.0, 1.0));              // -PI/2 to PI/2
    
    float u = (theta / TWO_PI) + 0.5;                      // 0 to 1
    float v = (phi / PI) + 0.5;                            // 0 to 1
    return vec2(u, v);
}

#endif // CORE_NOISE_UTILS_GLSL
