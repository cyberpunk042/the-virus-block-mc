// ═══════════════════════════════════════════════════════════════════════════
// FBM (Fractal Brownian Motion) NOISE
// Uses Perlin3D for layered turbulent noise patterns
// ═══════════════════════════════════════════════════════════════════════════

#ifndef NOISE_FBM_GLSL
#define NOISE_FBM_GLSL

#include "noise_perlin.glsl"

/**
 * Standard FBM with 4 fixed octaves.
 * Frequencies: 0.9, 3.99, 8.01, 15.05
 * Weights: 0.99, 0.49, 0.249, 0.124
 * Returns value in approximate range [-2, 2]
 */
float fbm4(vec3 p) {
    float v = 0.0;
    v += Perlin3D(p * 0.9) * 0.99;
    v += Perlin3D(p * 3.99) * 0.49;
    v += Perlin3D(p * 8.01) * 0.249;
    v += Perlin3D(p * 15.05) * 0.124;
    return v;
}

/**
 * Configurable FBM with variable octaves.
 * @param p Position to sample
 * @param octaves Number of octaves (1-8)
 * @param lacunarity Frequency multiplier per octave (typically 2.0)
 * @param gain Amplitude multiplier per octave (typically 0.5)
 */
float fbmN(vec3 p, int octaves, float lacunarity, float gain) {
    float amplitude = 1.0;
    float frequency = 1.0;
    float value = 0.0;
    
    for (int i = 0; i < octaves && i < 8; ++i) {
        value += amplitude * Perlin3D(p * frequency);
        frequency *= lacunarity;
        amplitude *= gain;
    }
    
    return value;
}

/**
 * Animated FBM with time offset per layer.
 * Creates flowing, organic turbulence.
 * @param p Position to sample
 * @param time Animation time
 * @param octaves Number of layers (also affects time offset)
 */
float fbmAnimated(vec3 p, float time, int octaves) {
    float value = 0.0;
    float amplitude = 0.99;
    float frequency = 0.9;
    
    for (int i = 0; i < octaves && i < 8; ++i) {
        // Time offset varies per layer for organic flow
        float layerTime = time / float(i + 4);
        vec3 offset = vec3(0.0, layerTime, 0.0);
        
        value += amplitude * Perlin3D(p * frequency + offset);
        
        frequency *= 4.0;
        amplitude *= 0.5;
    }
    
    return value;
}

#endif // NOISE_FBM_GLSL
