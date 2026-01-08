#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V6 - Standalone Raymarched Pulsar Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/energy_orb_pulsar.glsl"
#include "include/effects/pulsar_v6.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    // V6 has internal occlusion via maxDist parameter - no preamble macro needed
    
    float maxDist = isSky ? 1000.0 : linearDist;
    
    PulsarV6Result pulsar = renderPulsarV6(
        camPos, ray.direction, maxDist,
        sphereCenter, sphereRadius * 10.0,
        Time * AnimSpeed,
        field.PrimaryColor.rgb,
        field.SecondaryColor.rgb,
        field.TertiaryColor.rgb,
        field.HighlightColor.rgb,
        CoreSize,
        Intensity * 0.1,
        NoiseResLow,
        NoiseResHigh,
        NoiseAmplitude,
        NoiseOctaves,
        NoiseBaseScale,
        NoiseScaleMultiplier,
        FlamesEdge,
        FlamesPower,
        FlamesMult,
        CoronaWidth,
        CoronaPower,
        CoronaMultiplier * 0.02,
        RadialSpeed1,
        RadialSpeed2,
        AxialSpeed,
        SpeedLow,
        LightAmbient,
        LightDiffuse,
        mod(RayCoronaFlags, 2.0),
        vec3(RayColorR, RayColorG, RayColorB),
        RayPower,
        RaySharpness,
        SpeedRay
    );
    
    vec4 fieldEffect = vec4(pulsar.color, pulsar.alpha);
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
