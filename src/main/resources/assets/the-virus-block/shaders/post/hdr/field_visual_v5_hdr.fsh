#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V5 HDR - Pulsar Projected with HDR value preservation
// ═══════════════════════════════════════════════════════════════════════════

#define HDR_MODE 1

#include "../include/core/field_visual_base.glsl"
#include "../include/core/field_visual_preamble.glsl"
#include "../include/effects/energy_orb_pulsar.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    FIELD_VISUAL_DEPTH_OCCLUSION();
    
    vec4 fieldEffect = renderEnergyOrbPulsarProjected(
        texCoord,
        sphereCenter,
        sphereRadius * 5.0,
        camPos,
        forward,
        worldUp,
        CameraUpUBO.w,
        CameraForwardUBO.w,
        FrameTimeUBO.x * AnimSpeed,
        field.PrimaryColor.rgb,
        field.SecondaryColor.rgb,
        field.TertiaryColor.rgb,
        field.HighlightColor.rgb,
        CoreSize,
        Intensity * 0.1,
        FadeScale,
        FadePower,
        RadialSpeed1,
        RadialSpeed2,
        AxialSpeed,
        SpeedLow,
        NoiseResLow,
        NoiseResHigh,
        NoiseAmplitude,
        NoiseOctaves,
        NoiseBaseScale,
        NoiseScaleMultiplier,
        FlamesEdge,
        FlamesPower,
        FlamesMult,
        FlamesTimeScale,
        FlamesInsideFalloff,
        CoronaWidth,
        CoronaPower,
        CoronaMultiplier * 0.02,
        TransScale,
        LightAmbient,
        LightDiffuse,
        SurfaceNoiseScale,
        V2AlphaScale
    );
    
    // HDR composite - values > 1.0 pass through!
    FIELD_VISUAL_COMPOSITE_HDR(fieldEffect);
}
