#version 150

// Scene color buffer (sampler_name "In" -> InSampler)
uniform sampler2D InSampler;
// Depth buffer (sampler_name "Depth" -> DepthSampler)
uniform sampler2D DepthSampler;

// From sobel.vsh
in vec2 texCoord;
in vec2 oneTexel;

// UBO from vertex shader
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Mode uniform (set from Java) - for now we use different shader files
// Mode 1 = Debug Quadrants (this file)

out vec4 fragColor;

void main() {
    // Sample scene color
    vec4 sceneColor = texture(InSampler, texCoord);
    
    // Sample raw depth
    float depth = texture(DepthSampler, texCoord).r;
    
    // 4-QUADRANT DEBUG VIEW (Y-axis corrected for OpenGL):
    // In OpenGL: texCoord.y = 0 is BOTTOM, texCoord.y = 1 is TOP
    // Screen layout:
    // ┌─────────┬─────────┐
    // │ Raw     │ Inverted│  <- top = texCoord.y > 0.5
    // │ Depth   │ Depth   │
    // ├─────────┼─────────┤
    // │ Scene   │ Scene + │  <- bottom = texCoord.y < 0.5
    // │ Color   │ Depth   │
    // └─────────┴─────────┘
    
    bool left = texCoord.x < 0.5;
    bool top = texCoord.y > 0.5;  // OpenGL Y is bottom-up
    
    if (left && top) {
        // TOP-LEFT: Raw depth (white = 1.0 = far/sky, black = 0.0 = near)
        fragColor = vec4(vec3(depth), 1.0);
    }
    else if (!left && top) {
        // TOP-RIGHT: Inverted depth (black = far, white = near)
        // Shows terrain as white, sky as black
        fragColor = vec4(vec3(1.0 - depth), 1.0);
    }
    else if (left && !top) {
        // BOTTOM-LEFT: Scene color passthrough (should look normal)
        fragColor = sceneColor;
    }
    else {
        // BOTTOM-RIGHT: Scene blended with depth visualization
        // Red channel shows depth, green/blue show scene
        float invDepth = 1.0 - depth;
        fragColor = vec4(
            invDepth,                    // R = depth (white = near)
            sceneColor.g * 0.5,         // G = scene (dimmed)
            sceneColor.b * 0.5,         // B = scene (dimmed)
            1.0
        );
    }
    
    // Draw divider lines (yellow)
    float lineWidth = 0.004;
    if (abs(texCoord.x - 0.5) < lineWidth || abs(texCoord.y - 0.5) < lineWidth) {
        fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    }
    
    // Draw corner labels using colored squares
    float labelSize = 0.05;
    
    // Top-left corner indicator: GRAY square = depth mode
    if (texCoord.x < labelSize && texCoord.y > (1.0 - labelSize)) {
        fragColor = vec4(0.5, 0.5, 0.5, 1.0);
    }
    // Top-right corner indicator: CYAN square = inverted
    if (texCoord.x > (1.0 - labelSize) && texCoord.y > (1.0 - labelSize)) {
        fragColor = vec4(0.0, 1.0, 1.0, 1.0);
    }
    // Bottom-left corner indicator: GREEN square = scene
    if (texCoord.x < labelSize && texCoord.y < labelSize) {
        fragColor = vec4(0.0, 1.0, 0.0, 1.0);
    }
    // Bottom-right corner indicator: MAGENTA square = blend
    if (texCoord.x > (1.0 - labelSize) && texCoord.y < labelSize) {
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
    }
}
