// ═══════════════════════════════════════════════════════════════════════════
// CORE: MATH UTILITIES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Common mathematical operations used throughout the shader library.
//
// Include: #include "include/core/math_utils.glsl"
// Prerequisites: core/constants.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORE_MATH_UTILS_GLSL
#define CORE_MATH_UTILS_GLSL

#include "constants.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// SAFE OPERATIONS (prevent NaN/Inf)
// ═══════════════════════════════════════════════════════════════════════════

// Normalize with fallback for zero-length vectors
vec3 safeNormalize(vec3 v) {
    float len = length(v);
    return len > NEAR_ZERO ? v / len : vec3(0.0, 1.0, 0.0);
}

vec2 safeNormalize2(vec2 v) {
    float len = length(v);
    return len > NEAR_ZERO ? v / len : vec2(1.0, 0.0);
}

// Safe division (returns 0 on divide by zero)
float safeDivide(float a, float b) {
    return abs(b) > NEAR_ZERO ? a / b : 0.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// CLAMPING & SATURATION
// ═══════════════════════════════════════════════════════════════════════════

// Clamp to 0-1 range (avoid overloading built-in clamp)
float saturate01(float x) {
    return clamp(x, 0.0, 1.0);
}

vec2 saturate01_2(vec2 v) {
    return clamp(v, vec2(0.0), vec2(1.0));
}

vec3 saturate01_3(vec3 v) {
    return clamp(v, vec3(0.0), vec3(1.0));
}

vec4 saturate01_4(vec4 v) {
    return clamp(v, vec4(0.0), vec4(1.0));
}

// ═══════════════════════════════════════════════════════════════════════════
// REMAPPING
// ═══════════════════════════════════════════════════════════════════════════

// Remap value from one range to another
float remap(float value, float inMin, float inMax, float outMin, float outMax) {
    float t = (value - inMin) / (inMax - inMin);
    return outMin + t * (outMax - outMin);
}

// Remap with clamping
float remapClamped(float value, float inMin, float inMax, float outMin, float outMax) {
    float t = saturate01((value - inMin) / (inMax - inMin));
    return outMin + t * (outMax - outMin);
}

// ═══════════════════════════════════════════════════════════════════════════
// SMOOTH OPERATIONS (for SDF blending)
// ═══════════════════════════════════════════════════════════════════════════

// Smooth minimum - blends two values smoothly
// k controls blend radius (0 = hard min, larger = smoother)
float smoothMin(float a, float b, float k) {
    if (k < NEAR_ZERO) return min(a, b);
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

// Smooth maximum
float smoothMax(float a, float b, float k) {
    return -smoothMin(-a, -b, k);
}

// ═══════════════════════════════════════════════════════════════════════════
// 2D ROTATION
// ═══════════════════════════════════════════════════════════════════════════

// Create 2D rotation matrix
mat2 rotate2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, s, -s, c);
}

// Rotate a 2D vector by angle (radians)
vec2 rotateVec2(vec2 v, float angle) {
    return rotate2D(angle) * v;
}

#endif // CORE_MATH_UTILS_GLSL
