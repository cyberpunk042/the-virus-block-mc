#version 150

// Red tint - confirms shader is running
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
    vec4 scene = texture(InSampler, texCoord);
    fragColor = vec4(scene.r + 0.3, scene.g * 0.7, scene.b * 0.7, 1.0);
}
