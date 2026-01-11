// ═══════════════════════════════════════════════════════════════════════════
// HDR UTILITIES - Conditional clamping for HDR pipelines
// 
// Include this file and use hdrClamp() instead of clamp() for final outputs.
// In LDR mode: values are clamped to [0,1]
// In HDR mode: values pass through unclamped (> 1.0 allowed)
//
// Usage:
//   #define HDR_MODE 1  // Before including this file (or 0 for LDR)
//   #include "../core/hdr_utils.glsl"
//   ...
//   col = hdrClamp(col);  // Uses mode-aware clamping
// ═══════════════════════════════════════════════════════════════════════════

#ifndef HDR_UTILS_GLSL
#define HDR_UTILS_GLSL

// Default to LDR mode if not defined
#ifndef HDR_MODE
#define HDR_MODE 0
#endif

// ═══════════════════════════════════════════════════════════════════════════
// HDR-aware clamp for vec4
// ═══════════════════════════════════════════════════════════════════════════
vec4 hdrClamp(vec4 v) {
#if HDR_MODE == 1
    // HDR mode: only clamp negative values, allow > 1.0
    return max(v, vec4(0.0));
#else
    // LDR mode: standard clamp
    return clamp(v, 0.0, 1.0);
#endif
}

// ═══════════════════════════════════════════════════════════════════════════
// HDR-aware clamp for vec3
// ═══════════════════════════════════════════════════════════════════════════
vec3 hdrClamp(vec3 v) {
#if HDR_MODE == 1
    // HDR mode: only clamp negative values, allow > 1.0
    return max(v, vec3(0.0));
#else
    // LDR mode: standard clamp
    return clamp(v, 0.0, 1.0);
#endif
}

// ═══════════════════════════════════════════════════════════════════════════
// HDR-aware clamp for float
// ═══════════════════════════════════════════════════════════════════════════
float hdrClamp(float v) {
#if HDR_MODE == 1
    // HDR mode: only clamp negative values, allow > 1.0
    return max(v, 0.0);
#else
    // LDR mode: standard clamp
    return clamp(v, 0.0, 1.0);
#endif
}

// ═══════════════════════════════════════════════════════════════════════════
// HDR accumulation - safe additive blending
// In HDR mode, values can accumulate beyond 1.0
// ═══════════════════════════════════════════════════════════════════════════
vec3 hdrAccumulate(vec3 current, vec3 add) {
#if HDR_MODE == 1
    // HDR mode: free accumulation, values grow naturally
    return current + add;
#else
    // LDR mode: clamp to prevent oversaturation
    return clamp(current + add, 0.0, 1.0);
#endif
}

float hdrAccumulate(float current, float add) {
#if HDR_MODE == 1
    return current + add;
#else
    return clamp(current + add, 0.0, 1.0);
#endif
}

#endif // HDR_UTILS_GLSL
