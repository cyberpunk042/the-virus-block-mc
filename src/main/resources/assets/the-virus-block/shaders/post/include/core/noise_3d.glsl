// ═══════════════════════════════════════════════════════════════════════════
// NOISE 3D - Simplex-like 3D Noise Functions
// ═══════════════════════════════════════════════════════════════════════════
//
// Source: trisomie21's Shadertoy (V5 Pulsar origin)
//
// Purpose:
// ─────────
// Fast 3D noise for animated surface textures.
// More organic/fiery look than noise_4d's value noise.
//
// Key Functions:
// ─────────
// snoise3d(vec3 uv, float res)        - Core 3D simplex-like noise
// surfaceTexture3d(vec2 uv, time)     - Multi-octave surface texture
// flameNoise(coords, time, octaves)   - Animated flame pattern
//
// Usage Examples:
// ─────────
// // Simple surface noise
// float n = snoise3d(vec3(uv, time), 15.0);
//
// // Flame pattern for fire/corona
// float flames = flameNoise(surfaceCoords, time, 7, noiseAmp, baseScale);
//
// Compare to noise_4d.glsl:
// ─────────
// noise_4d: 4D value noise, smoother, good for multi-scale star surfaces
// noise_3d: 3D simplex-like, more organic, good for flames/fire effects
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef NOISE_3D_GLSL
#define NOISE_3D_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// CORE 3D SIMPLEX-LIKE NOISE
// ═══════════════════════════════════════════════════════════════════════════
// Returns noise in range [-1, 1].
// Based on trisomie21's pulsarSnoise from Shadertoy.
//
// Parameters:
//   uv  - 3D coordinate (xyz = position, z often used for time)
//   res - Resolution/scale (higher = finer detail)
//
// Returns:
//   Noise value in [-1, 1]

float snoise3d(vec3 uv, float res) {
    const vec3 s = vec3(1e0, 1e2, 1e4);
    
    uv *= res;
    
    vec3 uv0 = floor(mod(uv, res)) * s;
    vec3 uv1 = floor(mod(uv + vec3(1.0), res)) * s;
    
    vec3 f = fract(uv);
    f = f * f * (3.0 - 2.0 * f);  // Smoothstep
    
    vec4 v = vec4(uv0.x + uv0.y + uv0.z, uv1.x + uv0.y + uv0.z,
                  uv0.x + uv1.y + uv0.z, uv1.x + uv1.y + uv0.z);
    
    vec4 r = fract(sin(v * 1e-3) * 1e5);
    float r0 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
    
    r = fract(sin((v + uv1.z - uv0.z) * 1e-3) * 1e5);
    float r1 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
    
    return mix(r0, r1, f.z) * 2.0 - 1.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// MULTI-OCTAVE SURFACE TEXTURE
// ═══════════════════════════════════════════════════════════════════════════
// Generates grayscale texture similar to iChannel texture lookup.
// Combines 3 octaves at different scales.
//
// Parameters:
//   uv    - 2D texture coordinates
//   time  - Animation time
//   scale - Base scale (default ~10.0)
//
// Returns:
//   RGB texture value (all channels same for grayscale)

vec3 surfaceTexture3d(vec2 uv, float time, float scale) {
    float n1 = snoise3d(vec3(uv * scale, time * 0.5), scale * 0.5);
    float n2 = snoise3d(vec3(uv * scale * 2.0, time * 0.3), scale);
    float n3 = snoise3d(vec3(uv * scale * 4.0, time * 0.1), scale * 2.0);
    
    float n = n1 * 0.5 + n2 * 0.3 + n3 * 0.2;
    float intensity = n * 0.5 + 0.5;  // Map [-1,1] to [0,1]
    
    return vec3(intensity);
}

// ═══════════════════════════════════════════════════════════════════════════
// FLAME NOISE PATTERN
// ═══════════════════════════════════════════════════════════════════════════
// Generates animated flame/fire pattern for corona effects.
// Uses dual-layer noise with accumulating octaves.
//
// Parameters:
//   coords    - Surface point (from spherical mapping)
//   time      - Animation time
//   octaves   - Number of noise layers (1-10, default 7)
//   amplitude - Noise strength (default 0.5)
//   baseScale - Base noise scale (default 10.0)
//   scaleMult - Scale multiplier between layers (default 25.0)
//   radialSpeed1/2 - Animation speeds for two layers
//   loopSpeed - Overall loop speed
//
// Returns:
//   Flame intensity [0, ~1]

float flameNoise(
    vec3 coords,
    float time,
    int octaves,
    float amplitude,
    float baseScale,
    float scaleMult,
    float radialSpeed1,
    float radialSpeed2,
    float loopSpeed
) {
    // Dual time layers for complexity
    float newTime1 = abs(snoise3d(
        vec3(sin(time * radialSpeed1), sin(time * radialSpeed2), time * loopSpeed) + coords,
        baseScale
    ));
    
    float newTime2 = abs(snoise3d(
        vec3(sin(time * radialSpeed1), sin(time * radialSpeed2), time * loopSpeed) + coords,
        scaleMult
    ));
    
    // Accumulate octaves
    float fVal1 = 1.0;
    float fVal2 = 1.0;
    
    for (int i = 1; i <= octaves && i <= 10; i++) {
        float power = pow(2.0, float(i));
        
        fVal1 += (amplitude / power) * snoise3d(
            coords + vec3(time),
            power * baseScale * (newTime1 + 1.0)
        );
        
        fVal2 += (amplitude / power) * snoise3d(
            coords + vec3(time),
            power * scaleMult * (newTime2 + 1.0)
        );
    }
    
    return (fVal1 + fVal2) * 0.5;
}

// ═══════════════════════════════════════════════════════════════════════════
// BACKWARDS COMPATIBILITY ALIASES
// ═══════════════════════════════════════════════════════════════════════════

#define pulsarSnoise snoise3d
#define pulsarSurfaceTexture surfaceTexture3d

#endif // NOISE_3D_GLSL
