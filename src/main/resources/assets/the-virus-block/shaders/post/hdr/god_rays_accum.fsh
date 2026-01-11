// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS ACCUMULATION PASS - Radial blur toward light source
// ═══════════════════════════════════════════════════════════════════════════
//
// Pass 2 of God Rays pipeline:
// Marches from each pixel toward the light source, accumulating occlusion.
// This creates the characteristic volumetric light shaft effect.
//
// Runs at HALF RESOLUTION for performance.
//
// Inputs:
//   OcclusionSampler - Occlusion mask from god_rays_mask.fsh
//   FieldVisualConfig - For orb position and god ray params
//   CameraDataUBO    - For ViewProj matrix
//
// Outputs:
//   fragColor.rgb - Accumulated god ray intensity (monochrome, HDR-safe)
//
// ═══════════════════════════════════════════════════════════════════════════

#version 150

#define HDR_MODE 1

uniform sampler2D OcclusionSampler;

// Include base which provides UBOs (FieldVisualConfig, CameraDataUBO)
// Also provides: texCoord, fragColor, samplers
#include "../include/core/field_visual_base.glsl"

// God rays accumulator
#include "../include/effects/god_rays.glsl"

void main() {
    // Early out if god rays disabled
    if (GodRayEnabled < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
    // DEBUG: Use screen center as light position
    // This should make rays come from where camera is pointing
    vec2 lightUV = vec2(0.5, 0.5);
    float lightVisible = 1.0;
    
    // Early out if light is behind camera or way off-screen
    if (lightVisible < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
    // Get god ray config from UBO
    int samples = int(GodRaySamples);
    float decay = GodRayDecay;
    float exposure = GodRayExposure;
    
    // Accumulate god rays
    float illumination = accumulateGodRays(
        texCoord,
        lightUV,
        OcclusionSampler,
        samples,
        GOD_RAYS_DEFAULT_MAX_LENGTH,
        decay,
        exposure
    );
    
    // Output monochrome illumination (HDR-safe, may exceed 1.0)
    // Color tinting happens at composite stage
    fragColor = vec4(illumination, illumination, illumination, 1.0);
}
