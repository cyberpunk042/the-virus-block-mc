#version 150

// Full-screen depth visualization
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec2 oneTexel;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    
    // Show inverted depth (white = near, black = far)
    float invDepth = 1.0 - depth;
    
    // Apply some contrast enhancement
    invDepth = pow(invDepth, 0.5);
    
    fragColor = vec4(vec3(invDepth), 1.0);
}
