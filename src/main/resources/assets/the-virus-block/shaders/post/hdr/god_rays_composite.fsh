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

void main() {
    vec4 sceneColor = texture(SceneSampler, texCoord);
    float godRayIntensity = texture(GodRaysSampler, texCoord).r;
    
    // Early out if no god rays or disabled
    if (GodRayEnabled < 0.5 || godRayIntensity < 0.001) {
        fragColor = sceneColor;
        return;
    }
    
    // Get ray color from UBO for tinting (RGB from ray color slot)
    vec3 rayTint = vec3(RayColorR, RayColorG, RayColorB);
    
    // Blend toward white for high intensity (like real light)
    vec3 godRayColor = mix(rayTint, vec3(1.0), godRayIntensity * 0.5);
    
    // Additive blend - god rays ADD light to the scene
    vec3 finalColor = sceneColor.rgb + godRayColor * godRayIntensity;
    
    fragColor = vec4(finalColor, sceneColor.a);
}
