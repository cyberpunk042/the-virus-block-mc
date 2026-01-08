#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL GEODESIC - Standalone Animated Geodesic Sphere Shader
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/geodesic_v1.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_GEODESIC);
    FIELD_VISUAL_DEPTH_OCCLUSION();
    
    float maxDist = isSky ? 1000.0 : linearDist;
    
    GeodesicV1Result geo = renderGeodesicV1(
        camPos, ray.direction, forward, maxDist,
        sphereCenter, sphereRadius,
        Time * AnimSpeed, GeoRotationSpeed,
        field.PrimaryColor.rgb, field.SecondaryColor.rgb, field.TertiaryColor.rgb,
        max(GeoSubdivisions, 1.0),
        GeoHeight, GeoThickness, GeoGap,
        GeoRoundTop, GeoRoundCorner,
        clamp(GeoAnimMode, 0.0, 0.3),
        LightFresnel, GeoDomeClip,
        GeoWaveResolution, GeoWaveAmplitude,
        Intensity
    );
    
    vec4 fieldEffect = geo.didHit ? vec4(geo.color, geo.alpha) : vec4(0.0);
    FIELD_VISUAL_COMPOSITE(fieldEffect);
}
