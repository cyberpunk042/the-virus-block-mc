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

// ═══════════════════════════════════════════════════════════════════════════
// STYLED ACCUMULATOR (Uses god_rays_style.glsl functions)
// ═══════════════════════════════════════════════════════════════════════════

// Include style library for advanced features
#include "god_rays_style.glsl"

/**
 * Styled god rays accumulator with full feature support.
 * 
 * New in 360° version: parallelFactor and parallelDir enable distance-based
 * ray parallelism. Close sources have diverging rays, far sources have 
 * parallel rays (like sunlight). This naturally handles behind-camera.
 */
float accumulateGodRaysStyled(
    vec2 pixelUV,
    vec2 lightUV,
    sampler2D occlusionTex,
    int N,
    float L,
    float decayFactor,
    float exposure,
    float orbRadiusX,      // Orb radius in UV space (X direction, aspect-corrected)
    float orbRadiusY,      // Orb radius in UV space (Y direction)
    float energyMode,
    float distributionMode,
    float arrangementMode,
    float noiseScale,
    float noiseSpeed,
    float noiseIntensity,
    float angularBias,
    float curvatureMode,
    float curvatureStrength,
    float flickerMode,
    float flickerIntensity,
    float flickerFrequency,
    float travelMode,
    float travelSpeed,
    float travelCount,
    float travelWidth,
    float time,
    // 360° parallelism parameters
    float parallelFactor,  // 0 = full convergence (close), 1 = parallel rays (far)
    vec2 parallelDir       // 2D direction toward orb (screen-space)
) {
    // Apply arrangement mode (point source, ring, etc)
    vec2 effectiveLightUV = getArrangedLightUV(lightUV, pixelUV, 0.0, arrangementMode);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISTANCE-BASED RAY DIRECTION BLENDING
    // ═══════════════════════════════════════════════════════════════════════════
    // 
    // Blend between:
    //   - Radial rays (converging toward lightUV) - close sources
    //   - Parallel rays (all pointing in parallelDir) - far sources
    //
    // This is physically accurate: distant light sources produce parallel rays
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Radial direction: toward the projected light position (current behavior)
    vec2 toLight = effectiveLightUV - pixelUV;
    float dist = length(toLight);
    vec2 radialDir = (dist > 0.001) ? normalize(toLight) : parallelDir;
    
    // Blend radial and parallel based on distance factor
    vec2 blendedDir = normalize(mix(radialDir, parallelDir, parallelFactor));
    
    // Get ray direction based on energy mode + curvature (uses blended direction)
    // Note: For extreme parallelFactor, the energy mode direction modifiers become
    // less important since rays are all pointing the same way anyway
    vec2 rayDir = getGodRayDirection(pixelUV, effectiveLightUV, energyMode, curvatureMode, curvatureStrength, time);
    
    // Blend the styled ray direction with pure parallel direction
    rayDir = normalize(mix(rayDir, blendedDir, parallelFactor * 0.5));
    
    // At light center: check if inside orb first
    if (dist < 0.001) {
        // If orb masking is enabled and we're at center, return nothing
        if (orbRadiusX > 0.0 && orbRadiusY > 0.0) {
            return 0.0;
        }
        return exposure * float(N);
    }
    
    // March length: min of distance to light and max length L
    float marchLength = min(dist, L);
    vec2 step = rayDir * (marchLength / float(N));
    
    // Apply angular weight for distribution (all modes use scale/speed/intensity)
    float angularWeight = getAngularWeight(pixelUV, effectiveLightUV, distributionMode, angularBias, noiseScale, noiseSpeed, noiseIntensity, time);
    
    // Apply arrangement weight (RADIAL restricts to horizontal band)
    float arrangementWeight = getArrangementWeight(pixelUV, effectiveLightUV, arrangementMode);
    
    // Apply noise modulation ONLY for distribution mode 2 (Noise)
    float noiseWeight = 1.0;
    if (distributionMode > 1.5 && distributionMode < 2.5) {
        noiseWeight = getNoiseModulation(pixelUV, effectiveLightUV, time, noiseScale, noiseSpeed, noiseIntensity);
    }
    
    // Apply flicker modulation (affects whole ray)
    float flickerMod = getFlickerModulation(pixelUV, effectiveLightUV, time, flickerMode, flickerIntensity, flickerFrequency);
    
    // Per-ray random for travel variation
    float rayRand = fract(sin(dot(pixelUV, vec2(12.9898, 78.233))) * 43758.5453);
    
    float illumination = 0.0;
    float decay = 1.0;
    vec2 uv = pixelUV;
    
    for (int i = 0; i < N; i++) {
        uv += step;
        
        // Exit if stepped outside viewport
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            break;
        }
        
        // Skip samples inside the orb (ellipse check for aspect ratio)
        if (orbRadiusX > 0.0 && orbRadiusY > 0.0) {
            vec2 delta = uv - effectiveLightUV;
            float ellipseCheck = (delta.x * delta.x) / (orbRadiusX * orbRadiusX)
                               + (delta.y * delta.y) / (orbRadiusY * orbRadiusY);
            if (ellipseCheck < 1.0) {
                continue;  // Inside orb, skip this sample
            }
        }
        
        // Normalized position along ray (0=at light, 1=at pixel)
        // We invert because we march FROM pixel TOWARD light
        float t = 1.0 - float(i) / float(N);
        
        // Sample occlusion
        float occlusion = texture(occlusionTex, uv).r;
        
        // Apply energy visibility (which part of ray is visible)
        float energyVis = getEnergyVisibility(t, time, energyMode);
        
        // Apply travel modulation (moving particles along ray)
        float travelMod = getTravelModulation(t, time, travelMode, travelSpeed, travelCount, travelWidth, rayRand);
        
        // Per-sample noise grain (only for NOISE distribution mode)
        float sampleNoise = 1.0;
        if (distributionMode > 1.5 && distributionMode < 2.5) {
            // Smooth atmospheric noise along ray (not random speckle)
            float grain = sin(t * 15.0 + sin(t * 7.3) * 2.0) * 0.5 + 0.5;
            sampleNoise = 1.0 - noiseIntensity * 0.3 + grain * noiseIntensity * 0.6;
        }
        
        // Combine all per-sample modulations
        illumination += occlusion * decay * energyVis * travelMod * sampleNoise;
        
        // Exponential falloff
        decay *= decayFactor;
    }
    
    // Apply all modulation weights + intensity breathing
    float breathing = getIntensityBreathing(time, 1.0, 0.5); // Subtle 50% intensity breathing
    return illumination * exposure * angularWeight * arrangementWeight * noiseWeight * flickerMod * breathing;
}

#endif // GOD_RAYS_GLSL
