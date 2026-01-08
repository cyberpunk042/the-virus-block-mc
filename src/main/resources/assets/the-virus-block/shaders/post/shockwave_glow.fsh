#version 150

// ═══════════════════════════════════════════════════════════════════════════
// SHOCKWAVE GLOW POST-EFFECT
// Reads a ring mask texture and applies corona-style glow
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;    // Scene color
uniform sampler2D MaskSampler;  // Ring mask (white=ring, black=no ring)

in vec2 texCoord;
in vec2 oneTexel;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Corona/glow parameters (could be uniforms from Java)
const vec3 glowColor = vec3(0.2, 0.9, 1.0);  // Cyan
const vec3 coreColor = vec3(1.0, 1.0, 1.0);  // White
const float glowPower = 2.0;
const float glowIntensity = 2.5;
const float blurRadius = 8.0;  // Pixels to sample for blur

out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float maskValue = texture(MaskSampler, texCoord).r;
    
    // ═══════════════════════════════════════════════════════════════════════
    // BLUR THE MASK for soft glow edges
    // Sample surrounding pixels to create bloom effect
    // ═══════════════════════════════════════════════════════════════════════
    
    float blurredMask = 0.0;
    float totalWeight = 0.0;
    
    // Simple box blur with distance falloff
    for (float dx = -blurRadius; dx <= blurRadius; dx += 1.0) {
        for (float dy = -blurRadius; dy <= blurRadius; dy += 1.0) {
            vec2 offset = vec2(dx, dy) * oneTexel;
            float dist = length(vec2(dx, dy));
            
            // Gaussian-ish weight falloff
            float weight = exp(-dist * dist / (blurRadius * 0.5));
            
            float sample = texture(MaskSampler, texCoord + offset).r;
            blurredMask += sample * weight;
            totalWeight += weight;
        }
    }
    blurredMask /= totalWeight;
    
    // ═══════════════════════════════════════════════════════════════════════
    // APPLY CORONA-STYLE GLOW
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 finalColor = sceneColor.rgb;
    
    // Outer glow (from blurred mask)
    if (blurredMask > 0.001) {
        float glow = pow(blurredMask, 1.0 / glowPower) * glowIntensity;
        finalColor += glowColor * glow * 0.5;
    }
    
    // Inner ring (from sharp mask)
    if (maskValue > 0.001) {
        // Bright core where mask is strongest
        float coreIntensity = pow(maskValue, 0.5) * glowIntensity;
        
        // Blend core color
        finalColor = mix(finalColor, coreColor, maskValue * 0.8);
        
        // Add glow color additively
        finalColor += glowColor * coreIntensity * (1.0 - maskValue * 0.5);
    }
    
    // HDR tone-mapping (clamp extreme brightness)
    finalColor = finalColor / (1.0 + finalColor);
    
    fragColor = vec4(finalColor, 1.0);
}
