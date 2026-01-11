#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GAUSSIAN BLUR - Pass 2/3 of HDR Pipeline
// 
// Separable 9-tap Gaussian blur. Run horizontally then vertically.
// Operates on RGBA16F input - preserves HDR range through blur.
//
// Usage:
//   Pass 1: Direction = (1, 0) → horizontal blur
//   Pass 2: Direction = (0, 1) → vertical blur
//   Repeat passes for wider blur (adjustable iterations)
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

// Blur direction - must be declared as UBO to match JSON
// Minecraft parses JSON "uniforms.BlurParams" as a uniform buffer block
layout(std140) uniform BlurParams {
    float DirectionX;   // 1.0 for horizontal, 0.0 for vertical
    float DirectionY;   // 0.0 for horizontal, 1.0 for vertical
    float BlurPad1;     // Padding
    float BlurPad2;     // Padding
};

// 9-tap Gaussian weights (sigma ≈ 2.0)
// These weights sum to 1.0 for energy conservation
const float weights[5] = float[](
    0.227027,   // Center sample (i=0)
    0.1945946,  // ±1 pixel
    0.1216216,  // ±2 pixels
    0.054054,   // ±3 pixels
    0.016216    // ±4 pixels
);

void main() {
    // Get texture size using GLSL function (avoids uniform conflict)
    vec2 texelSize = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 blurDir = vec2(DirectionX, DirectionY) * texelSize;
    
    // Center sample (weight[0])
    vec4 result = texture(InSampler, texCoord) * weights[0];
    
    // Symmetric samples (±1, ±2, ±3, ±4)
    for (int i = 1; i < 5; i++) {
        vec2 offset = blurDir * float(i);
        
        // Sample both directions and weight
        result += texture(InSampler, texCoord + offset) * weights[i];
        result += texture(InSampler, texCoord - offset) * weights[i];
    }
    
    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║ NO CLAMP - Preserve HDR range through entire blur chain               ║
    // ║ Values > 1.0 accumulate properly for soft glow effect                 ║
    // ╚═══════════════════════════════════════════════════════════════════════╝
    
    fragColor = result;
}
