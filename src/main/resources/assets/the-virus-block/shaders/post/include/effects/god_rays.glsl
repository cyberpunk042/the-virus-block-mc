// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS - Screen-space radial light scattering
// ═══════════════════════════════════════════════════════════════════════════
// 
// Layer: effects/ (self-contained, no dependencies)
// 
// Implements GPU Gems 3 volumetric light scattering as post-process.
// Reference: project KI effects/god_rays/integration_guide.md
//
// TUNING PHILOSOPHY (from Long-range God Rays Recipe):
// ─────────────────────────────────────────────────────
// - decayFactor controls RANGE (0.95 short → 0.985 long)
// - exposure controls STRENGTH (0.01 subtle → 0.05 strong)
// - NEVER use exposure to fix range, NEVER use decay to fix blowout
//
// Pipeline integration:
// ─────────────────────
// 1. Generate occlusion mask from depth buffer
// 2. Run this accumulator at half-res
// 3. Blur result (reuse gaussian_blur.fsh)
// 4. Composite additively with scene
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef GOD_RAYS_GLSL
#define GOD_RAYS_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// DEFAULT TUNING VALUES 
// From KI: 96 samples, 0.4 UV reach, 0.97 decay, 0.02 exposure
// ═══════════════════════════════════════════════════════════════════════════
#define GOD_RAYS_DEFAULT_SAMPLES     96
#define GOD_RAYS_DEFAULT_MAX_LENGTH  0.4
#define GOD_RAYS_DEFAULT_DECAY       0.97
#define GOD_RAYS_DEFAULT_EXPOSURE    0.02

// ═══════════════════════════════════════════════════════════════════════════
// LIGHT SCREEN POSITION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Projects light position to screen UV.
 * 
 * @param lightWorldPos  Light position (camera-relative)
 * @param viewProj       View-projection matrix from CameraDataUBO
 * @return vec3(uv.x, uv.y, visibility) - visibility is 1.0 if on-screen, 0.0 if behind
 */
vec3 getLightScreenUV(vec3 lightWorldPos, mat4 viewProj) {
    vec4 clipPos = viewProj * vec4(lightWorldPos, 1.0);
    
    // Behind camera: w <= 0
    if (clipPos.w <= 0.0) {
        return vec3(0.5, 0.5, 0.0);
    }
    
    vec3 ndc = clipPos.xyz / clipPos.w;
    vec2 uv = ndc.xy * 0.5 + 0.5;
    
    // Allow off-screen margin for edge glow (rays can extend from just off-screen)
    float visible = (abs(ndc.x) < 1.5 && abs(ndc.y) < 1.5 && ndc.z < 1.0) ? 1.0 : 0.0;
    
    return vec3(uv, visible);
}

// ═══════════════════════════════════════════════════════════════════════════
// CORE ACCUMULATOR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Accumulates god ray contribution by marching toward light source.
 * 
 * Algorithm (GPU Gems 3):
 *   illumination = 0, decay = 1
 *   for i in 0..N:
 *       illumination += sample(uv) * decay
 *       decay *= decayFactor
 *   return illumination * exposure
 *
 * @param pixelUV       Fragment UV [0,1]
 * @param lightUV       Light screen UV [0,1]
 * @param occlusionTex  Mask texture (1.0 = open/sky, 0.0 = occluded)
 * @param N             Sample count (default 96)
 * @param L             Max march length in UV space (default 0.4)
 * @param decayFactor   Range control (default 0.97)
 * @param exposure      Strength control (default 0.02)
 * @return              Accumulated illumination (HDR-safe, may exceed 1.0)
 */
float accumulateGodRays(
    vec2 pixelUV,
    vec2 lightUV,
    sampler2D occlusionTex,
    int N,
    float L,
    float decayFactor,
    float exposure
) {
    vec2 toLight = lightUV - pixelUV;
    float dist = length(toLight);
    
    // At light center: full illumination
    if (dist < 0.001) {
        return exposure * float(N);  // Maximum possible accumulation
    }
    
    // March length: min of distance to light and max length L
    float marchLength = min(dist, L);
    vec2 step = normalize(toLight) * (marchLength / float(N));
    
    float illumination = 0.0;
    float decay = 1.0;
    vec2 uv = pixelUV;
    
    for (int i = 0; i < N; i++) {
        uv += step;
        
        // Exit if stepped outside viewport
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            break;
        }
        
        // Occluded pixels contribute 0, open sky contributes 1
        float occlusion = texture(occlusionTex, uv).r;
        illumination += occlusion * decay;
        
        // Exponential falloff for distance
        decay *= decayFactor;
    }
    
    return illumination * exposure;
}

/**
 * Simple wrapper with vec4 config from UBO.
 * config = (enabled, decayFactor, exposure, samples)
 */
float accumulateGodRaysFromUBO(
    vec2 pixelUV,
    vec2 lightUV,
    sampler2D occlusionTex,
    vec4 config
) {
    if (config.x < 0.5) return 0.0;  // Disabled
    
    return accumulateGodRays(
        pixelUV,
        lightUV,
        occlusionTex,
        int(config.w),
        GOD_RAYS_DEFAULT_MAX_LENGTH,
        config.y,
        config.z
    );
}

#endif // GOD_RAYS_GLSL
