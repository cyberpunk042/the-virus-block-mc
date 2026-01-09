// ═══════════════════════════════════════════════════════════════════════════
// SMOOTH VALUE NOISE
// ═══════════════════════════════════════════════════════════════════════════
//
// Smooth interpolated value noise for organic effects.
// Used by poison_gas and other screen-space effects.
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef VALUE_NOISE_GLSL
#define VALUE_NOISE_GLSL

float valueHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Smooth value noise with interpolation
float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    
    // Smooth interpolation
    f = f * f * (3.0 - 2.0 * f);
    
    float a = valueHash(i);
    float b = valueHash(i + vec2(1.0, 0.0));
    float c = valueHash(i + vec2(0.0, 1.0));
    float d = valueHash(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// 2D noise vector
vec2 valueNoise2D(vec2 p) {
    return vec2(
        valueNoise(p),
        valueNoise(p + vec2(31.416, 17.532))
    );
}

#endif // VALUE_NOISE_GLSL
