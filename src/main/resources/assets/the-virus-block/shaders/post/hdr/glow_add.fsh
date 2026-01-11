#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GLOW ADD - Pass 5 of HDR Pipeline
// ═══════════════════════════════════════════════════════════════════════════
//
// Composites the blurred glow (from blur_v) onto the scene (from main).
// Uses additive blending with intensity control from FieldVisualConfig.
//
// Inputs:
//   - Scene: Original rendered scene (minecraft:main after blit)
//   - Glow:  Blurred HDR glow (blur_v output)
//
// Output:
//   - Final composited image → minecraft:main
//
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D SceneSampler;
uniform sampler2D GlowSampler;

in vec2 texCoord;
out vec4 fragColor;

// FieldVisualConfig UBO - read Intensity for glow scaling
layout(std140) uniform FieldVisualConfig {
    // Position (Layer 1) - 4 floats
    float CenterX, CenterY, CenterZ, Radius;
    // Colors (Layer 1) - 16 floats
    float PrimaryR, PrimaryG, PrimaryB, PrimaryA;
    float SecondaryR, SecondaryG, SecondaryB, SecondaryA;
    float TertiaryR, TertiaryG, TertiaryB, TertiaryA;
    float HighlightR, HighlightG, HighlightB, HighlightA;
    // RayColor - 4 floats
    float RayColorR, RayColorG, RayColorB, RayColorA;
    // Animation - 4 floats  
    float Phase, AnimSpeed, Intensity, EffectType;
    // We only need Intensity from this UBO
};

// ═══════════════════════════════════════════════════════════════════════════
// SELECTIVE TONEMAP
// ═══════════════════════════════════════════════════════════════════════════
//
// Only compresses values > 1.0, preserves LDR scene exactly.
// This prevents the glow from affecting non-glowing areas.

vec3 selectiveTonemap(vec3 color) {
    vec3 result;
    for (int i = 0; i < 3; i++) {
        float v = color[i];
        if (v <= 0.8) {
            // Below threshold: pass through unchanged
            result[i] = v;
        } else if (v <= 1.2) {
            // Transition zone: smooth blend to compression
            float t = (v - 0.8) / 0.4;
            float compressed = 0.8 + 0.2 * t * t * (3.0 - 2.0 * t);
            result[i] = mix(v, compressed, t);
        } else {
            // HDR zone: Reinhard-style compression
            result[i] = 1.0 + (v - 1.0) / (1.0 + (v - 1.0));
        }
    }
    return result;
}

void main() {
    // Sample scene and blurred glow
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec3 glow = texture(GlowSampler, texCoord).rgb;
    
    // Scale glow by Intensity from FieldVisualConfig
    // Use 0.125 base multiplier for subtle effect
    float glowIntensity = 0.125 * Intensity;
    vec3 scaledGlow = glow * glowIntensity;
    
    // Tonemap only the glow (preserve scene colors exactly)
    vec3 tonemappedGlow = selectiveTonemap(scaledGlow);
    
    // Additive blend
    vec3 result = scene + tonemappedGlow;
    
    // Final clamp to valid range
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
