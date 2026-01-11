#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GLOW EXTRACTION - Pass 1 of HDR Pipeline
// 
// Renders the field effect into an RGBA16F buffer WITHOUT clamping.
// Allows intensity values > 1.0 for proper HDR glow accumulation.
//
// Output: Raw HDR color with alpha (NOT tonemapped)
// ═══════════════════════════════════════════════════════════════════════════

#include "../include/core/field_visual_base.glsl"
#include "../include/core/field_visual_preamble.glsl"
#include "../include/effects/pulsar_v7.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    
    float maxDist = isSky ? 1000.0 : linearDist;
    
    // Render V7 pulsar effect
    PulsarV7Result pulsar = renderPulsarV7(
        camPos, ray.direction, forward, maxDist,
        sphereCenter, sphereRadius,
        FrameTimeUBO.x * AnimSpeed,
        field.HighlightColor.rgb, field.PrimaryColor.rgb,
        field.SecondaryColor.rgb, field.TertiaryColor.rgb,
        vec3(RayColorR, RayColorG, RayColorB),
        mix(vec3(RayColorR, RayColorG, RayColorB), field.HighlightColor.rgb, 0.7),
        int(NoiseOctaves), max(0.1, NoiseBaseScale),
        CoreSize,
        Intensity,
        RaySharpness, max(1.0, FadeScale), max(0.1, InsideFalloffPower),
        RayPower, CoronaPower, CoronaMultiplier * 0.02, CoreFalloff,
        CoronaWidth + 1.0, EdgeSharpness * 10.0 + 1.0, RingPower,
        SpeedHigh, SpeedLow, SpeedRay, SpeedRing,
        FadePower * 0.01,
        abs(NoiseSeed) < 0.001 ? 1.0 : NoiseSeed,
        max(0.1, EruptionContrast)
    );
    
    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║ KEY DIFFERENCE FROM LDR SHADERS:                                       ║
    // ║ Output raw HDR values - NO clamping or tonemapping!                    ║
    // ║ Values > 1.0 are preserved in RGBA16F buffer                           ║
    // ║ Tonemapping happens ONLY in the final composite pass                   ║
    // ╚═══════════════════════════════════════════════════════════════════════╝
    
    // Apply intensity scaling but preserve HDR range
    vec3 hdrColor = pulsar.color * Intensity;
    
    // Output raw HDR - alpha controls blend strength
    fragColor = vec4(hdrColor, pulsar.alpha);
}
