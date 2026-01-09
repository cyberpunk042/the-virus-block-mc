// ═══════════════════════════════════════════════════════════════════════════
// SCREEN FUME - 2D Distorted Scene Smoke Effect
// ═══════════════════════════════════════════════════════════════════════════
//
// Ported from Shadertoy smoke effect.
// Creates screen distortion with fume color overlay.
// Scene sampling must be done in main shader (needs sampler access).
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SCREEN_FUME_GLSL
#define SCREEN_FUME_GLSL

#include "../utils/value_noise.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// PERLIN-STYLE NOISE (mimics texture(iChannel1, ...) from Shadertoy)
// ═══════════════════════════════════════════════════════════════════════════

// Matches the original Shadertoy perlin() function
// noiseScale: multiplier for noise frequency (higher = more detailed/grainy)
//             Default: 256.0 (simulates 256x256 texture)
//             Lower values = smoother, larger flames
//             Higher values = grainier, smaller flames
vec2 fumePerlin(vec2 p, float noiseScale) {
    vec2 x = vec2(0.0);
    
    for (int i = 0; i < 3; ++i) {
        float j = pow(2.0, float(i));
        
        // p * 0.001 converts large coords (100-300) to small UVs
        // noiseScale controls how much detail we see
        vec2 noiseCoord = p * j * 0.001 * noiseScale;
        
        // Sample noise and center around 0
        x += (valueNoise2D(noiseCoord) - 0.5) / j;
    }
    return x;
}

// Default version for backwards compatibility
vec2 fumePerlin(vec2 p) {
    return fumePerlin(p, 256.0);
}

#endif // SCREEN_FUME_GLSL
