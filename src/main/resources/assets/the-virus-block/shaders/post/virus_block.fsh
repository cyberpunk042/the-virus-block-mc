#version 150

// ═══════════════════════════════════════════════════════════════════════════
// VIRUS BLOCK POST-PROCESSING SHADER
// ═══════════════════════════════════════════════════════════════════════════
//
// Effects:
//   - 2D Screen fume (distorted scene sampling)
//   - 2D Poison gas overlay (rising heat effect)
//   - 3D Raymarched smoke above blocks
//
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform VirusBlockParams {
    vec4 CameraPosTime;       // xyz = camera position, w = time
    vec4 EffectFlags;         // x = blockCount, y = espEnabled, z = smokeEnabled (3D gas), w = fumeEnabled (2D screen)
    vec4 InvProjParams;       // x = near, y = far, z = fov, w = aspect
    vec4 CameraForward;       // xyz = forward vector, w = closestBlockDist
    vec4 CameraUp;            // xyz = up vector, w = isFlying
    vec4 SmokeShape;          // x = height, y = spread, z = density, w = turbulence
    vec4 SmokeAnim;           // x = riseSpeed, y = swirl, z = noiseScale, w = fadeHeight
    vec4 SmokeColor;          // xyz = RGB, w = intensity
    vec4 ScreenPoisonParams;  // x = triggerDist, y = intensity, z = unused, w = unused
    vec4 ESPClose;
    vec4 ESPMedium;
    vec4 ESPFar;
    vec4 ESPStyle;
    mat4 InvViewProj;         // Inverse view-projection matrix for accurate rays
    vec4 BlockPos[32];
};

// ═══════════════════════════════════════════════════════════════════════════
// BASE UBOs - Shared Frame and Camera data
// ═══════════════════════════════════════════════════════════════════════════
#include "include/ubo/frame_ubo.glsl"
#include "include/ubo/camera_ubo.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// INCLUDES
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/constants.glsl"
#include "include/utils/value_noise.glsl"
#include "include/camera/types.glsl"
#include "include/camera/basis.glsl"
#include "include/camera/rays.glsl"
#include "include/camera/depth.glsl"
#include "include/effects/screen_fume.glsl"
#include "include/effects/poison_gas.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// 2D SCREEN FUME (scene distortion - uses InSampler)
// ═══════════════════════════════════════════════════════════════════════════

vec3 screenFume(vec2 p, vec2 o, float t, vec3 fumeColor) {
    const int steps = 5;
    vec3 col = vec3(0.0);
    for (int i = 1; i < steps; ++i) {
        p += fumePerlin(p + o) * t * 0.01 / float(i);
        col += texture(InSampler, p).rgb;
        col += fumeColor * t;
    }
    return col / float(steps);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    vec2 uv = texCoord;
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    float time = CameraPosTime.w;
    float closestDist = CameraForward.w;
    vec3 toxicColor = SmokeColor.rgb;
    int blockCount = int(min(EffectFlags.x, 32.0));
    float aspect = InvProjParams.w;
    float near = InvProjParams.x;
    float far = InvProjParams.y;
    float isFlying = CameraUp.w;
    
    vec3 finalColor = texture(InSampler, uv).rgb;
    
    // Calculate proximity factor: 
    // - 0.0 at 100+ blocks (no effect)
    // - ramps smoothly from 100 to 10 blocks
    // - caps at 1.0 at 10 blocks or closer
    float maxDist = 100.0;
    float minDist = 10.0;
    float proximity = clamp((maxDist - closestDist) / (maxDist - minDist), 0.0, 1.0);
    
    // ─────────────────────────────────────────────────────────────────────────
    // 2D SCREEN FUME (distorts scene)
    // ─────────────────────────────────────────────────────────────────────────
    
    if (EffectFlags.w > 0.5 && proximity > 0.01) {
        // Original Shadertoy fume logic
        float t = (2.0 - uv.y);  // Fade towards top
        vec2 distribution = vec2(300.0, 100.0);  // Long vertical fumes
        float speed = -15.0;
        
        // Fume noise scale from UBO (default 256.0, lower = smoother/larger flames)
        float fumeNoiseScale = ScreenPoisonParams.z > 0.0 ? ScreenPoisonParams.z : 256.0;
        
        t *= fumePerlin(uv * distribution + time * vec2(0.0, speed), fumeNoiseScale).x;
        t = pow(abs(t), 2.5);
        
        // screenFume already samples scene, mix with proximity for fade-in
        vec3 fumeCol = screenFume(uv, uv, t, toxicColor);
        finalColor = mix(finalColor, fumeCol, proximity);
    }
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}

