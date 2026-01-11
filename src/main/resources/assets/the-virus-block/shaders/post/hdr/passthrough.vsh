#version 150

// ═══════════════════════════════════════════════════════════════════════════
// HDR PASSTHROUGH VERTEX SHADER
// 
// Simple screen-space quad vertex shader for HDR post-processing passes.
// Compatible with Minecraft's post-effect system.
// ═══════════════════════════════════════════════════════════════════════════

in vec4 Position;

uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    // Position comes in as screen-space coordinates
    vec4 outPos = ProjMat * Position;
    gl_Position = outPos;
    
    // Generate texture coordinates from position
    // Maps from clip space [-1,1] to texture space [0,1]
    texCoord = (outPos.xy * 0.5) + 0.5;
}
