#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GPU SHOCKWAVE - Fullscreen Fragment Shader
// Receives dynamic uniforms from Java every frame!
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

// Dynamic uniform block - updated by ShockwaveUniformBinder each frame!
layout(std140) uniform ShockwaveParams {
    vec4 RadiusThicknessIntensityTime;  // x=Radius, y=Thickness, z=Intensity, w=Time
    vec4 ColorCore;                      // xyz=CoreColor, w=padding
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

float linearizeDepth(float depth) {
    float near = 0.05;
    float far = 256.0;
    return (2.0 * near * far) / (far + near - depth * (far - near));
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW
// ═══════════════════════════════════════════════════════════════════════════

float glowFalloff(float dist, float radius) {
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Extract parameters from uniform
    float ringRadius = RadiusThicknessIntensityTime.x;
    float ringThickness = RadiusThicknessIntensityTime.y;
    float intensity = RadiusThicknessIntensityTime.z;
    float time = RadiusThicknessIntensityTime.w;
    
    // Sky detection
    if (rawDepth > 0.9999) {
        fragColor = sceneColor;
        return;
    }
    
    // Linearize depth
    float worldDistance = linearizeDepth(rawDepth);
    
    // Ring calculation
    float distFromRing = abs(worldDistance - ringRadius);
    
    // Core mask (sharp center)
    float coreWidth = ringThickness * 0.2;
    float coreMask = 1.0 - smoothstep(0.0, coreWidth, distFromRing);
    
    // Inner ring mask
    float innerMask = 1.0 - smoothstep(0.0, ringThickness * 0.5, distFromRing);
    
    // Outer glow
    float outerMask = glowFalloff(distFromRing, ringThickness * 2.0);
    
    // Colors
    vec3 coreColor = ColorCore.rgb;
    vec3 ringColor = vec3(0.0, 1.0, 1.0);   // Cyan
    vec3 outerGlow = vec3(0.2, 0.5, 1.0);   // Blue
    
    vec3 finalColor = sceneColor.rgb;
    
    // Layer composition
    finalColor = mix(finalColor, outerGlow, outerMask * 0.4 * intensity);
    finalColor = mix(finalColor, ringColor, innerMask * 0.7 * intensity);
    finalColor = mix(finalColor, coreColor, coreMask * 0.9 * intensity);
    
    // Additive bloom
    finalColor += ringColor * coreMask * 0.3 * intensity;
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
