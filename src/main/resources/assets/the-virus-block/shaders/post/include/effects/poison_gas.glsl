// ═══════════════════════════════════════════════════════════════════════════
// POISON GAS - Rising Toxic Gas Effect
// ═══════════════════════════════════════════════════════════════════════════
//
// Ported from "Poison Gas" by Jianing Zhang 2022
// Based on "Fire" by Ben Wheatley - 2018 (MIT License)
//
// Uses octave-based Perlin noise with time affecting Y for rising motion.
// Adds toxic green heat overlay to the scene.
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef POISON_GAS_GLSL
#define POISON_GAS_GLSL

#include "../utils/value_noise.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

const int POISON_MAX_OCTAVE = 8;

// ═══════════════════════════════════════════════════════════════════════════
// COSINE INTERPOLATION
// ═══════════════════════════════════════════════════════════════════════════

float poisonCosineInterpolate(float a, float b, float x) {
    float ft = x * 3.14159265359;
    float f = (1.0 - cos(ft)) * 0.5;
    return a * (1.0 - f) + b * f;
}

// ═══════════════════════════════════════════════════════════════════════════
// PERLIN NOISE (with rising motion baked in)
// ═══════════════════════════════════════════════════════════════════════════

float poisonPerlinNoise(float x_arg, float y_arg, float time_arg) {
    float sum = 0.0;
    for (int octave = 0; octave < POISON_MAX_OCTAVE; ++octave) {
        float sf = pow(2.0, float(octave));
        float x = x_arg * sf;
        float y = (y_arg * sf) + (1.5 * time_arg * log2(sf + 1.0));
        float y_scale = 1.0 * sf;
        
        float x_floor = floor(x);
        float y_floor = floor(y);
        float fraction_x = x - x_floor;
        float fraction_y = y - y_floor;
        
        float t1 = valueHash(vec2(x_floor + y_scale * y_floor));
        float t2 = valueHash(vec2(x_floor + y_scale * (y_floor + 1.0)));
        
        x_floor += 1.0;
        float t3 = valueHash(vec2(x_floor + y_scale * y_floor));
        float t4 = valueHash(vec2(x_floor + y_scale * (y_floor + 1.0)));
        
        float i1 = poisonCosineInterpolate(t1, t2, fraction_y);
        float i2 = poisonCosineInterpolate(t3, t4, fraction_y);
        
        sum += poisonCosineInterpolate(i1, i2, fraction_x) / sf;
    }
    return 2.0 * sum;
}

// ═══════════════════════════════════════════════════════════════════════════
// POISON GAS OVERLAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculate poison gas overlay color to add to scene.
 * 
 * @param uv         Screen UV (0-1)
 * @param aspect     Aspect ratio (width/height)
 * @param time       Animation time
 * @param gasColor   Base gas color (e.g., vec3(0.045, 0.95, 0.133) for green)
 * @param intensity  Effect intensity multiplier
 * @return           Color to ADD to scene (additive blend)
 */
vec3 poisonGasOverlay(vec2 uv, float aspect, float time, vec3 gasColor, float intensity) {
    float dx = 0.5 - uv.x;
    float dy = 0.5 - uv.y;
    dy *= aspect;
    
    float c = poisonPerlinNoise(dx, dy, time);
    c -= 0.4;
    c *= cos(dx * 3.14159265359);
    c *= 2.0 - 2.0 * uv.y;
    
    // Apply color (original used red/green for fire, we use green for poison)
    vec3 heat = vec3(
        c * gasColor.r,
        c * (dy + gasColor.g),
        gasColor.b
    );
    
    return heat * intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// 3D WORLD-SPACE SMOKE (for raymarching above blocks)
// ═══════════════════════════════════════════════════════════════════════════

struct PoisonGasParams {
    float maxHeight;
    float spread;
    float density;
    float turbulence;
    float riseSpeed;
    float swirl;
    float fadeHeight;
    float noiseScale;
    int octaves;
};

PoisonGasParams defaultPoisonGasParams() {
    PoisonGasParams p;
    p.maxHeight = 5.0;
    p.spread = 3.0;
    p.density = 1.5;
    p.turbulence = 1.0;
    p.riseSpeed = 1.5;
    p.swirl = 0.3;
    p.fadeHeight = 0.5;
    p.noiseScale = 2.0;
    p.octaves = 6;
    return p;
}

float poisonGasDensity3D(vec3 worldPos, vec3 sourcePos, float time, PoisonGasParams params) {
    // Relative position to block top-center
    vec3 rel = worldPos - sourcePos;
    
    // ─────────────────────────────────────────────────────────────────────────
    // PRISM BOUNDS CHECK - rectangular column above the block
    // ─────────────────────────────────────────────────────────────────────────
    float halfWidth = params.spread;  // Half-width of gas column
    float height = rel.y;
    
    // Outside height bounds? No gas
    if (height < 0.0 || height > params.maxHeight) return 0.0;
    
    // Outside horizontal bounds? No gas (rectangular prism, not cylinder)
    if (abs(rel.x) > halfWidth || abs(rel.z) > halfWidth) return 0.0;
    
    // ─────────────────────────────────────────────────────────────────────────
    // CONVERT TO "FAKE UV" - match original shader's expected input range
    // ─────────────────────────────────────────────────────────────────────────
    
    // dx, dy in original are distances from screen center, range ~[-0.5, 0.5]
    // We map our 3D relative position to the same range
    float dx = rel.x / (halfWidth * 2.0);  // [-0.5, 0.5]
    float dz = rel.z / (halfWidth * 2.0);  // [-0.5, 0.5]
    
    // Height normalized [0 = bottom, 1 = top of column]
    float normalizedHeight = height / params.maxHeight;
    
    // ─────────────────────────────────────────────────────────────────────────
    // APPLY ORIGINAL FORMULA (this is the magic from Shadertoy)
    // ─────────────────────────────────────────────────────────────────────────
    
    // The original uses perlinNoise(dx, dy, time)
    // In 3D, we use both dx and dz as spatial coordinates
    float c = poisonPerlinNoise(dx * params.noiseScale, dz * params.noiseScale, time * params.riseSpeed);
    
    // Original: c -= 0.4; 
    // Noise outputs ~2.0-4.0
    // Bias 2.0 → output range 0.0-2.0 (always positive, visible)
    c -= 2.0;
    
    // Original: c *= cos(dx*PI); (fade at horizontal edges)
    c *= cos(dx * 3.14159265359);
    c *= cos(dz * 3.14159265359);  // Also fade in Z direction (3D)
    
    // VERTICAL PROFILE: thin at base, peaks in middle, fades at top
    // Like a flame shape using sin() for smooth bell curve
    // normalizedHeight: 0 = bottom, 1 = top
    // sin(π * h) gives: 0 at h=0, 1 at h=0.5, 0 at h=1
    float verticalProfile = sin(normalizedHeight * 3.14159265359);
    c *= verticalProfile;
    
    c *= params.density;
    
    // Apply turbulence for variation
    c *= (0.5 + 0.5 * params.turbulence);
    
    return max(0.0, c);
}

vec3 poisonGasColor3D(float density, vec3 baseColor, float intensity) {
    if (density < 0.01) return vec3(0.0);
    vec3 color = baseColor * (0.6 + density * 0.8);
    color.g += density * 0.1;
    return color * density * intensity;
}

#endif // POISON_GAS_GLSL
