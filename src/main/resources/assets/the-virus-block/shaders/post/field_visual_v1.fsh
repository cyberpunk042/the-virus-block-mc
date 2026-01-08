#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL V1 - Standalone Basic Raymarched Energy Orb Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/energy_orb_v1.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    
    vec4 fieldEffect = renderEnergyOrbRaymarched(
        sphereCenter, sphereRadius,
        field.PrimaryColor.rgb, field.SecondaryColor.rgb, field.TertiaryColor.rgb,
        Time, field.AnimParams.y, field.AnimParams.x, field.AnimParams.z,
        field.CoreEdgeParams.x, field.CoreEdgeParams.y,
        field.SpiralParams.x, field.SpiralParams.y,
        field.GlowLineParams.x, field.GlowLineParams.y,
        field.V2Params.x,
        camPos, ray.direction, forward, sceneDepth,
        ColorBlendMode
    );
    
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
