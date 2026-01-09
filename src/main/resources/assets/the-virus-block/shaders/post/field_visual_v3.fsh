#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V3 - Standalone Raymarched Energy Orb Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/energy_orb_v3.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    FIELD_VISUAL_DEPTH_OCCLUSION();
    
    // V3 flags
    float rayCoronaFlags = field.GlowLineParams.z;
    float showExternalRays = mod(rayCoronaFlags, 2.0) >= 1.0 ? 1.0 : 0.0;
    float showCorona = mod(rayCoronaFlags, 4.0) >= 2.0 ? 1.0 : 0.0;
    
    EnergyOrbV3Result v3result = renderEnergyOrbV3(
        camPos, ray.direction, forward,
        sceneDepth, sphereCenter, sphereRadius,
        FrameTimeUBO.x * field.AnimParams.y,
        field.PrimaryColor.rgb, field.SecondaryColor.rgb, field.TertiaryColor.rgb,
        field.AnimParams.z,
        field.CoreEdgeParams.x, field.V2CoreSpread, field.V2CoreGlow,
        field.V2CoreRadiusScale, field.V2CoreMaskRadius, field.V2CoreMaskSoft,
        showCorona, field.V2Params.x, CoronaPower, field.V2CoronaStart, field.V2CoronaBrightness,
        field.CoreEdgeParams.y, RingPower, field.V2EdgeRadius, field.V2EdgeSpread,
        field.V2EdgeGlow, field.V2SharpScale,
        field.GlowLineParams.y, field.SpiralParams.x, field.SpiralParams.y,
        field.V2LinesUVScale, field.V2LinesDensityMult, field.V2LinesContrast1,
        field.V2LinesContrast2, field.V2LinesMaskRadius, field.V2LinesMaskSoft,
        showExternalRays, RayPower, RaySharpness, field.GlowLineParams.x,
        field.V2RayRotSpeed, field.V2RayStartRadius,
        field.V2AlphaScale, ColorBlendMode
    );
    
    vec4 fieldEffect = vec4(v3result.color, v3result.alpha);
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
