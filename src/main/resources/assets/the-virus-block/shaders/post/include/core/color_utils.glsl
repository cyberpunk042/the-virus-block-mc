// ═══════════════════════════════════════════════════════════════════════════
// CORE: COLOR UTILITIES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Color manipulation and tone mapping utilities.
//
// Include: #include "include/core/color_utils.glsl"
// Prerequisites: core/constants.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORE_COLOR_UTILS_GLSL
#define CORE_COLOR_UTILS_GLSL

#include "constants.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// ALPHA OPERATIONS
// ═══════════════════════════════════════════════════════════════════════════

// Premultiply alpha for correct blending
vec4 premultiplyAlpha(vec4 color) {
    return vec4(color.rgb * color.a, color.a);
}

// Unpremultiply (recover original RGB)
vec4 unpremultiplyAlpha(vec4 color) {
    if (color.a < NEAR_ZERO) return vec4(0.0);
    return vec4(color.rgb / color.a, color.a);
}

// ═══════════════════════════════════════════════════════════════════════════
// TONE MAPPING
// ═══════════════════════════════════════════════════════════════════════════

// Simple exponential tone mapping (used in field_visual.fsh)
// Prevents HDR values from clipping while preserving brightness
vec3 toneMapExponential(vec3 color, float exposure) {
    return 1.0 - exp(-color * exposure);
}

// ACES filmic tone mapping (more cinematic)
vec3 toneMapAces(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

// Reinhard tone mapping (simple, preserves colors)
vec3 toneMapReinhard(vec3 color) {
    return color / (color + vec3(1.0));
}

// ═══════════════════════════════════════════════════════════════════════════
// EXPOSURE & BRIGHTNESS
// ═══════════════════════════════════════════════════════════════════════════

// Apply exposure adjustment (EV stops)
vec3 applyExposure(vec3 color, float exposureEV) {
    return color * pow(2.0, exposureEV);
}

// Brightness adjustment (simple multiply)
vec3 applyBrightness(vec3 color, float brightness) {
    return color * brightness;
}

// Contrast adjustment
vec3 applyContrast(vec3 color, float contrast) {
    return (color - 0.5) * contrast + 0.5;
}

// ═══════════════════════════════════════════════════════════════════════════
// COLOR BLENDING
// ═══════════════════════════════════════════════════════════════════════════

// Additive blend (for glows)
vec4 blendAdditive(vec4 base, vec4 add) {
    return vec4(base.rgb + add.rgb * add.a, max(base.a, add.a));
}

// Alpha blend (standard over compositing)
vec4 blendOver(vec4 base, vec4 over) {
    float a = over.a + base.a * (1.0 - over.a);
    if (a < NEAR_ZERO) return vec4(0.0);
    vec3 rgb = (over.rgb * over.a + base.rgb * base.a * (1.0 - over.a)) / a;
    return vec4(rgb, a);
}

// Screen blend (brightening)
vec3 blendScreen(vec3 base, vec3 blend) {
    return 1.0 - (1.0 - base) * (1.0 - blend);
}

#endif // CORE_COLOR_UTILS_GLSL
