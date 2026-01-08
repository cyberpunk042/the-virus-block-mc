// ═══════════════════════════════════════════════════════════════════════════
// NOISE 4D - Animated 4-Dimensional Noise Functions
// ═══════════════════════════════════════════════════════════════════════════
//
// Source: Panteleymonov Aleksandr's "SunShader" (2015-2016)
//         https://www.shadertoy.com/view/XdXGzH
//
// Purpose:
// ─────────
// High-quality 4D noise for animated procedural textures.
// The 4th dimension (w) is typically time, enabling smooth animation.
//
// Key Functions:
// ─────────
// noise4q(vec4 x)           - Core 4D noise, returns 0-1
// noiseSpherical(...)       - Multi-octave noise sampled on a sphere surface
//
// Usage Examples:
// ─────────
// // Animated surface texture
// float n = noise4q(vec4(surfacePoint * 10.0, time));
//
// // Multi-detail star surface
// float surface = noiseSpherical(point, sphere, sqRadius, zoom, 
//                                seedVec, time, speedHi, speedLow, detail);
//
// Performance Notes:
// ─────────
// - noise4q is relatively expensive (many hash operations)
// - Use detail parameter to control quality vs performance
// - Higher detail levels are gated by conditionals for LOD
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef NOISE_4D_GLSL
#define NOISE_4D_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// HASH FUNCTION
// ═══════════════════════════════════════════════════════════════════════════
// Fast 4-component hash using sin().
// Quality is sufficient for visual noise but not cryptographic.

vec4 noise4d_hash4(vec4 n) { 
    return fract(sin(n) * 1399763.5453123); 
}

// ═══════════════════════════════════════════════════════════════════════════
// CORE 4D NOISE
// ═══════════════════════════════════════════════════════════════════════════
// Returns smooth procedural noise in range [0, 1].
// Uses 4D interpolation with smoothstep for S-curve blending.
//
// Parameters:
//   x - 4D coordinate (xyz = position, w = time or 4th dimension)
//
// Returns:
//   Smooth noise value in [0, 1]

float noise4q(vec4 x)
{
    vec4 n3 = vec4(0.0, 0.25, 0.5, 0.75);
    vec4 p2 = floor(x.wwww + n3);
    vec4 b = floor(x.xxxx + n3) + floor(x.yyyy + n3) * 157.0 + floor(x.zzzz + n3) * 113.0;
    vec4 p1 = b + fract(p2 * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);
    p2 = b + fract((p2 + 1.0) * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);
    vec4 f1 = fract(x.xxxx + n3);
    vec4 f2 = fract(x.yyyy + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);  // Smoothstep
    f2 = f2 * f2 * (3.0 - 2.0 * f2);
    vec4 n1 = vec4(0.0, 1.0, 157.0, 158.0);
    vec4 n2 = vec4(113.0, 114.0, 270.0, 271.0);
    vec4 vs1 = mix(noise4d_hash4(p1), noise4d_hash4(n1.yyyy + p1), f1);
    vec4 vs2 = mix(noise4d_hash4(n1.zzzz + p1), noise4d_hash4(n1.wwww + p1), f1);
    vec4 vs3 = mix(noise4d_hash4(p2), noise4d_hash4(n1.yyyy + p2), f1);
    vec4 vs4 = mix(noise4d_hash4(n1.zzzz + p2), noise4d_hash4(n1.wwww + p2), f1);
    vs1 = mix(vs1, vs2, f2);
    vs3 = mix(vs3, vs4, f2);
    vs2 = mix(noise4d_hash4(n2.xxxx + p1), noise4d_hash4(n2.yyyy + p1), f1);
    vs4 = mix(noise4d_hash4(n2.zzzz + p1), noise4d_hash4(n2.wwww + p1), f1);
    vs2 = mix(vs2, vs4, f2);
    vs4 = mix(noise4d_hash4(n2.xxxx + p2), noise4d_hash4(n2.yyyy + p2), f1);
    vec4 vs5 = mix(noise4d_hash4(n2.zzzz + p2), noise4d_hash4(n2.wwww + p2), f1);
    vs4 = mix(vs4, vs5, f2);
    f1 = fract(x.zzzz + n3);
    f2 = fract(x.wwww + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);
    f2 = f2 * f2 * (3.0 - 2.0 * f2);
    vs1 = mix(vs1, vs2, f1);
    vs3 = mix(vs3, vs4, f1);
    vs1 = mix(vs1, vs3, f2);
    float r = dot(vs1, vec4(0.25));
    return r * r * (3.0 - 2.0 * r);  // Final smoothstep for nicer distribution
}

// ═══════════════════════════════════════════════════════════════════════════
// SPHERICAL MULTI-OCTAVE NOISE
// ═══════════════════════════════════════════════════════════════════════════
// Samples noise at multiple scales on a sphere surface.
// Used for star/planet surface textures.
//
// Scale constants (from Unity SunShader):
//   Level 0: zoom * 3.6864
//   Level 1: zoom * 61.44
//   Level 2: zoom * 307.2
//   Level 3: zoom * 600.0
//   Level 4: zoom * 1200.0
//
// Weight constants:
//   0.625, 0.125, 0.0625, 0.03125, 0.01125
//
// Parameters:
//   surface   - Rotated surface point (in local sphere space)
//   sphere    - sqDist - RayProj² (perpendicular distance squared)
//   sqRadius  - Radius squared
//   zoom      - Detail zoom (1.0 = default)
//   subnoise  - Seed offset vector for variation
//   time      - Animated time
//   speedHi   - High frequency animation speed
//   speedLow  - Low frequency animation speed
//   detail    - LOD level (1-5, higher = more detail)
//
// Returns:
//   Accumulated noise value (not normalized)

float noiseSpherical(
    vec3 surface,
    float sphere,
    float sqRadius,
    float zoom,
    vec3 subnoise,
    float time,
    float speedHi,
    float speedLow,
    int detail
) {
    float s = 0.0;
    
    if (sphere < sqRadius) {
        // Level 0: Base surface texture (detail >= 1)
        if (detail >= 1) {
            s = noise4q(vec4(surface * zoom * 3.6864 + subnoise, time * speedHi)) * 0.625;
        }
        // Level 1: Medium detail (detail >= 2)
        if (detail >= 2) {
            s = s * 0.85 + noise4q(vec4(surface * zoom * 61.44 + subnoise * 3.0, time * speedHi * 3.0)) * 0.125;
        }
        // Level 2: Fine detail (detail >= 3)
        if (detail >= 3) {
            s = s * 0.94 + noise4q(vec4(surface * zoom * 307.2 + subnoise * 5.0, time * speedLow * 5.0)) * 0.0625;
        }
        // Level 3: Very fine detail (detail >= 4)
        if (detail >= 4) {
            s = s * 0.98 + noise4q(vec4(surface * zoom * 600.0 + subnoise * 6.0, time * speedLow * 6.0)) * 0.03125;
        }
        // Level 4: Ultra fine detail (detail >= 5)
        if (detail >= 5) {
            s = s * 0.98 + noise4q(vec4(surface * zoom * 1200.0 + subnoise * 9.0, time * speedLow * 9.0)) * 0.01125;
        }
    }
    
    return s;
}

// ═══════════════════════════════════════════════════════════════════════════
// CONVENIENCE FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Simple animated noise with default time as 4th dimension.
 */
float noise4q_animated(vec3 pos, float time) {
    return noise4q(vec4(pos, time));
}

/**
 * Turbulent noise (stacked octaves with decreasing amplitude).
 * Good for fire, smoke, clouds.
 */
float noise4q_turbulent(vec3 pos, float time, int octaves) {
    float sum = 0.0;
    float amp = 1.0;
    float freq = 1.0;
    float maxAmp = 0.0;
    
    for (int i = 0; i < octaves; i++) {
        sum += noise4q(vec4(pos * freq, time * freq)) * amp;
        maxAmp += amp;
        amp *= 0.5;
        freq *= 2.0;
    }
    
    return sum / maxAmp;  // Normalize to [0, 1]
}

#endif // NOISE_4D_GLSL
