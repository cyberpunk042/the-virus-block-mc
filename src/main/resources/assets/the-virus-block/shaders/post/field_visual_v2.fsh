#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V2 - Standalone Shadertoy Energy Orb Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/energy_orb_v2.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    FIELD_VISUAL_DEPTH_OCCLUSION();
    
    // Correct forward vector when flying
    vec3 forwardForV2 = IsFlying > 0.5 ? forward : vec3(ForwardX, ForwardY, ForwardZ);
    
    // Extract V2 parameters
    float rayCoronaFlags = field.GlowLineParams.z;
    bool showExternalRays = mod(rayCoronaFlags, 2.0) >= 1.0;
    bool showCorona = mod(rayCoronaFlags, 4.0) >= 2.0;
    
    vec4 fieldEffect = renderEnergyOrbV2ProjectedCustom(
        texCoord,
        sphereCenter,
        sphereRadius,
        camPos,
        forwardForV2,
        worldUp,
        Fov,
        AspectRatio,
        Time * AnimSpeed,
        field.PrimaryColor.rgb,
        field.SecondaryColor.rgb,
        field.TertiaryColor.rgb,
        field.AnimParams.z,
        field.CoreEdgeParams.x,
        field.CoreEdgeParams.y,
        field.SpiralParams.x,
        field.SpiralParams.y,
        field.GlowLineParams.x,
        field.GlowLineParams.y,
        showExternalRays ? 1.0 : 0.0,
        showCorona ? 1.0 : 0.0,
        field.V2Params.x,
        CoronaPower,
        RayPower,
        RaySharpness,
        RingPower,
        field.V2CoreSpread,
        field.V2CoreGlow,
        field.V2CoreRadiusScale,
        field.V2CoreMaskRadius,
        field.V2CoreMaskSoft,
        field.V2CoronaStart,
        field.V2CoronaBrightness,
        field.V2EdgeRadius,
        field.V2EdgeSpread,
        field.V2EdgeGlow,
        field.V2SharpScale,
        field.V2LinesUVScale,
        field.V2LinesDensityMult,
        field.V2LinesContrast1,
        field.V2LinesContrast2,
        field.V2LinesMaskRadius,
        field.V2LinesMaskSoft,
        field.V2RayRotSpeed,
        field.V2RayStartRadius,
        field.V2AlphaScale,
        isSky ? 10000.0 : linearDist,
        ColorBlendMode
    );
    
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
