#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FINAL COMPOSITE - Pass 4 of HDR Pipeline
// 
// Combines blurred HDR glow with original scene.
// Tonemapping happens HERE and ONLY here.
//
// Inputs:
//   SceneSampler - Original scene (minecraft:main, LDR)
//   GlowSampler  - Blurred glow (RGBA16F, HDR)
//
// Output: Final composited frame (clamped to [0,1] for display)
// ═══════════════════════════════════════════════════════════════════════════

// Samplers - Minecraft appends "Sampler" to JSON input names
uniform sampler2D SceneSampler;   // From JSON input "Scene"
uniform sampler2D GlowSampler;    // From JSON input "Glow"

in vec2 texCoord;
out vec4 fragColor;

// Composite parameters - must be declared as UBO to match JSON
// Minecraft parses JSON "uniforms.CompositeParams" as a uniform buffer block
layout(std140) uniform CompositeParams {
    float GlowIntensity;      // Glow strength multiplier (default: 1.0)
    float TonemapExposure;    // Exposure for tonemapping (default: 1.5)
    float TonemapModePad1;    // Padding for std140 (float before int)
    float TonemapModeVal;     // 0=Exposure, 1=ACES, 2=Reinhard (as float)
};

// HDR config from mixin - for BlurRadius compensation
layout(std140) uniform HdrConfig {
    float BlurRadius;     // Blur spread multiplier (from UI slider)
    float GlowIntensityHdr;  // Reserved
    float HdrPad1;
    float HdrPad2;
};

// ═══════════════════════════════════════════════════════════════════════════
// TONEMAPPING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Simple exposure-based tonemap (fast, predictable)
vec3 toneMapExposure(vec3 hdr, float exposure) {
    return 1.0 - exp(-hdr * exposure);
}

// ACES filmic tonemap (cinematic, slightly crushed shadows)
vec3 toneMapACES(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

// Reinhard tonemap (classic, preserves highlights)
vec3 toneMapReinhard(vec3 hdr) {
    return hdr / (hdr + vec3(1.0));
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSITE
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    // Sample inputs
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec4 glowSample = texture(GlowSampler, texCoord);
    vec3 glow = glowSample.rgb;
    float glowAlpha = glowSample.a;
    
    // Apply glow scaling
    // 0.05 base * intensity from slider (via HdrConfig.GlowIntensityHdr)
    glow *= 0.025 * GlowIntensityHdr;
    
    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║ TONEMAP THE GLOW (scene is already LDR)                               ║
    // ║ This is where HDR values > 1.0 get mapped to displayable range        ║
    // ╚═══════════════════════════════════════════════════════════════════════╝
    
    vec3 glowMapped;
    int tonemapMode = int(TonemapModeVal);
    if (tonemapMode == 1) {
        // ACES - cinematic look
        glowMapped = toneMapACES(glow * TonemapExposure);
    } else if (tonemapMode == 2) {
        // Reinhard - preserves highlights
        glowMapped = toneMapReinhard(glow * TonemapExposure);
    } else {
        // Exposure (default) - fast and predictable
        glowMapped = toneMapExposure(glow, TonemapExposure);
    }
    
    // Blend glow onto scene using SCREEN blend
    // Screen: 1 - (1-scene) * (1-glow) = scene + glow - scene*glow
    // This brightens without doubling (unlike additive)
    vec3 glowContribution = glowMapped * glowAlpha;
    vec3 final = 1.0 - (1.0 - scene) * (1.0 - glowContribution);
    
    // Final clamp to [0,1] for display
    fragColor = vec4(clamp(final, 0.0, 1.0), 1.0);
}
