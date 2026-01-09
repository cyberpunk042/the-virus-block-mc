#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V7 - Standalone Panteleymonov Sun Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/pulsar_v7.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    
    float maxDist = isSky ? 1000.0 : linearDist;
    
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
        RaySharpness, max(1.0, FadeScale), max(0.1, InsideFalloffPower),
        RayPower, CoronaPower, CoronaMultiplier * 0.02, CoreFalloff,
        CoronaWidth + 1.0, EdgeSharpness * 10.0 + 1.0, RingPower,
        SpeedHigh, SpeedLow, SpeedRay, SpeedRing,
        FadePower * 0.01,
        abs(NoiseSeed) < 0.001 ? 1.0 : NoiseSeed,
        max(0.1, EruptionContrast)  // NEW: Controls ray discreteness
    );
    
    vec4 fieldEffect = vec4(pulsar.color, pulsar.alpha);
    
    // === PROXIMITY BLACKOUT (V7-specific) ===
    if (DistortionStrength > 0.5 && Blackout > 0.001) {
        float distToCam = length(camPos - sphereCenter);
        float darkenRange = max(10.0, DistortionRadius);
        float darkenFade = max(0.01, DistortionFrequency);
        float normalizedDist = clamp(distToCam / darkenRange, 0.0, 1.0);
        float proximityBlackout = pow(1.0 - normalizedDist, darkenFade) * Blackout;
        sceneColor.rgb *= (1.0 - proximityBlackout);
    }
    
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
