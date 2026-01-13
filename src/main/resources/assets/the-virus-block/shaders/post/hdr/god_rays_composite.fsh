// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS COMPOSITE - Blend god rays with scene
// ═══════════════════════════════════════════════════════════════════════════
//
// Final pass of God Rays pipeline:
// Additively blends the accumulated god ray illumination with the scene.
// Applies ray color tinting from the orb's ray color.
//
// Inputs:
//   Scene      - Main scene buffer
//   GodRays    - Blurred god ray accumulation (half-res, upscaled)
//   FieldVisualConfig - For ray color tinting
//
// Outputs:
//   fragColor  - Scene + god rays
//
// ═══════════════════════════════════════════════════════════════════════════

#version 150

#define HDR_MODE 1

// Samplers - Minecraft appends "Sampler" to JSON input names
uniform sampler2D SceneSampler;     // From JSON input "Scene"
uniform sampler2D GodRaysSampler;   // From JSON input "GodRays"

// Include base for UBO access (we need ray color for tinting)
// Also provides: texCoord, fragColor
#include "../include/core/field_visual_base.glsl"

// Include style library for color function
#include "../include/effects/god_rays_style.glsl"

void main() {
    vec4 sceneColor = texture(SceneSampler, texCoord);
    float godRayIntensity = texture(GodRaysSampler, texCoord).r;
    
    // Phase 1: Clamp incoming intensity
    godRayIntensity = clamp(godRayIntensity, 0.0, 100.0);
    
    // Phase 1: Check for NaN/Inf
    if (isnan(godRayIntensity) || isinf(godRayIntensity)) {
        godRayIntensity = 0.0;
    }
    
    // Early out if no god rays or disabled
    if (GodRayEnabled < 0.5 || godRayIntensity < 0.001) {
        fragColor = sceneColor;
        return;
    }
    
    // Get colors from UBO
    vec3 rayColor1 = vec3(RayColorR, RayColorG, RayColorB);
    vec3 rayColor2 = vec3(GodRayColor2R, GodRayColor2G, GodRayColor2B);
    
    // Get style parameters
    float colorMode = GodRayColorMode;
    float gradientPower = GodRayGradientPower;
    
    // Calculate screen distance from light center for gradient effects
    // Note: We don't have lightUV here, so we use distance from screen center as approximation
    // For more accurate gradients, lightUV would need to be passed via a uniform
    float screenDist = length(texCoord - vec2(0.5, 0.5));
    
    // Get styled color based on mode
    vec3 godRayColor = getGodRayColor(
        godRayIntensity,
        screenDist,
        rayColor1,
        rayColor2,
        colorMode,
        gradientPower
    );
    
    // Blend toward white for very high intensity (like real light)
    godRayColor = mix(godRayColor, vec3(godRayIntensity), godRayIntensity * 0.3);
    
    // Additive blend - god rays ADD light to the scene
    vec3 finalColor = sceneColor.rgb + godRayColor;
    
    // Phase 1: Cap final output to prevent extreme HDR values
    finalColor = min(finalColor, vec3(10.0));
    
    fragColor = vec4(finalColor, sceneColor.a);
}

