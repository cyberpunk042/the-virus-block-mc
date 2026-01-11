// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS MASK PASS - Build occlusion mask from scene brightness
// ═══════════════════════════════════════════════════════════════════════════
//
// Pass 3 of God Rays pipeline:
// Creates a mask where the bright orb = white (light source),
// and dark geometry = black (occluders).
//
// Uses BRIGHTNESS-BASED masking because the orb itself is very bright.
// Pure depth-based masking would treat the orb as an occluder!
//
// Inputs:
//   SceneSampler  - Rendered scene with the bright orb
//   DepthSampler  - Depth buffer (for sky detection)
//   FieldVisualConfig - For threshold and sky toggle
//
// Outputs:
//   fragColor.rgb - Occlusion mask value [0,1]
//
// ═══════════════════════════════════════════════════════════════════════════

#version 150

#define HDR_MODE 1

// Samplers - Minecraft appends "Sampler" to JSON input names
uniform sampler2D SceneSampler;   // From JSON input "Scene"
// Note: DepthSampler declared in field_visual_base.glsl

// Include base for UBO access (we need threshold and sky toggle)
#include "../include/core/field_visual_base.glsl"

// Include occlusion utilities
#include "../include/rendering/depth_occlusion.glsl"

void main() {
    // Early out if god rays disabled
    if (GodRayEnabled < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
    // Sample scene color and depth
    vec3 sceneColor = texture(SceneSampler, texCoord).rgb;
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Get mask params from UBO
    float threshold = GodRayThreshold;
    float softness = GodRaySoftness;
    bool skyEnabled = GodRaySkyEnabled > 0.5;
    
    // Brightness-based mask: bright pixels = light source
    // The orb core is very bright (especially in HDR), so it will be white
    // Dark geometry (occluders) will be black
    float brightMask = buildBrightnessMask(sceneColor, threshold, softness);
    
    // Sky contribution (optional)
    float skyMask = buildOcclusionMask(rawDepth);
    
    // Combine based on sky toggle
    float mask = skyEnabled ? max(brightMask, skyMask) : brightMask;
    
    // Output mask in all channels
    fragColor = vec4(mask, mask, mask, 1.0);
}
