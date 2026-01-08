// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: ENERGY ORB TYPES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Shared types and structures for energy orb effects (V1 and V2).
// Designed to map directly from FieldVisualConfig uniform buffer.
//
// Include: #include "include/effects/energy_orb_types.glsl"
// Prerequisites: core/constants.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ENERGY_ORB_TYPES_GLSL
#define EFFECTS_ENERGY_ORB_TYPES_GLSL

#include "../core/constants.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// FIELD DATA - Runtime effect parameters
// ═══════════════════════════════════════════════════════════════════════════

/**
 * FieldData struct - internal struct for passing field data to render functions.
 * This is the single source of truth - matches UBO layout in field_visual.fsh.
 */
struct FieldData {
    vec4 CenterAndRadius;       // xyz = world pos, w = radius
    vec4 PrimaryColor;          // rgba
    vec4 SecondaryColor;        // rgba
    vec4 TertiaryColor;         // rgba
    vec4 HighlightColor;        // rgba - NEW for corona color
    vec4 AnimParams;            // x=phase, y=speed, z=intensity, w=effectType
    vec4 CoreEdgeParams;        // x=coreSize, y=edgeSharpness, z=shapeType, w=pad
    vec4 SpiralParams;          // x=density, y=twist, z=pad, w=pad
    vec4 GlowLineParams;        // x=count, y=intensity, z=rayCoronaFlags, w=version
    vec4 V2Params;              // x=coronaWidth, yzw=reserved
    
    // V2 Detail parameters (20 floats across 5 vec4s - NEW)
    float V2CoronaStart;
    float V2CoronaBrightness;
    float V2CoreRadiusScale;
    float V2CoreMaskRadius;
    
    float V2CoreSpread;
    float V2CoreGlow;
    float V2CoreMaskSoft;
    float V2EdgeRadius;
    
    float V2EdgeSpread;
    float V2EdgeGlow;
    float V2SharpScale;
    float V2LinesUVScale;
    
    float V2LinesDensityMult;
    float V2LinesContrast1;
    float V2LinesContrast2;
    float V2LinesMaskRadius;
    
    float V2LinesMaskSoft;
    float V2RayRotSpeed;
    float V2RayStartRadius;
    float V2AlphaScale;
};

// ═══════════════════════════════════════════════════════════════════════════
// BUILD FIELD DATA FROM UNIFORMS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Helper to build FieldData from individual values.
 * This is what field_visual.fsh main() does internally.
 */
FieldData buildFieldData(
    vec3 center, float radius,
    vec4 primaryColor, vec4 secondaryColor, vec4 tertiaryColor, vec4 highlightColor,
    float phase, float animSpeed, float intensity, float effectType,
    float coreSize, float edgeSharpness, float shapeType,
    float spiralDensity, float spiralTwist,
    float glowLineCount, float glowLineIntensity, float rayCoronaFlags, float version,
    float coronaWidth
) {
    FieldData f;
    f.CenterAndRadius = vec4(center, radius);
    f.PrimaryColor = primaryColor;
    f.SecondaryColor = secondaryColor;
    f.TertiaryColor = tertiaryColor;
    f.HighlightColor = highlightColor;
    f.AnimParams = vec4(phase, animSpeed, intensity, effectType);
    f.CoreEdgeParams = vec4(coreSize, edgeSharpness, shapeType, 0.0);
    f.SpiralParams = vec4(spiralDensity, spiralTwist, 0.0, 0.0);
    f.GlowLineParams = vec4(glowLineCount, glowLineIntensity, rayCoronaFlags, version);
    f.V2Params = vec4(coronaWidth, 0.0, 0.0, 0.0);
    
    // V2 Detail defaults
    f.V2CoronaStart = 0.15;
    f.V2CoronaBrightness = 1.0;  // 1.0 = original intensity
    f.V2CoreRadiusScale = 0.1;
    f.V2CoreMaskRadius = 0.35;
    f.V2CoreSpread = 1.0;
    f.V2CoreGlow = 1.0;
    f.V2CoreMaskSoft = 0.05;
    f.V2EdgeRadius = 0.3;
    f.V2EdgeSpread = 1.0;
    f.V2EdgeGlow = 1.0;
    f.V2SharpScale = 4.0;
    f.V2LinesUVScale = 3.0;
    f.V2LinesDensityMult = 1.6;
    f.V2LinesContrast1 = 2.5;
    f.V2LinesContrast2 = 3.0;
    f.V2LinesMaskRadius = 0.3;
    f.V2LinesMaskSoft = 0.02;
    f.V2RayRotSpeed = 0.3;
    f.V2RayStartRadius = 0.32;
    f.V2AlphaScale = 0.5;
    
    return f;
}

/**
 * Default FieldData values.
 */
FieldData defaultFieldData() {
    return buildFieldData(
        vec3(0.0),                  // center
        1.0,                        // radius
        vec4(1.0, 0.5, 0.2, 1.0),   // primaryColor
        vec4(1.0, 0.3, 0.1, 1.0),   // secondaryColor
        vec4(1.0, 0.8, 0.5, 1.0),   // tertiaryColor
        vec4(1.0, 0.8, 0.5, 1.0),   // highlightColor (corona)
        0.0, 1.0, 1.0, 1.0,         // phase, animSpeed, intensity, effectType
        0.3, 2.0, 0.0,              // coreSize, edgeSharpness, shapeType
        5.0, 2.0,                   // spiralDensity, spiralTwist
        8.0, 0.5, 0.0, 1.0,         // glowLineCount, glowLineIntensity, rayCoronaFlags, version
        0.5                         // coronaWidth
    );
}

// ═══════════════════════════════════════════════════════════════════════════
// SIMPLIFIED CONFIG (for library convenience)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Simplified configuration for energy orb effects.
 * Flattened for easier usage when not coming from uniform buffer.
 */
struct EnergyOrbConfig {
    // Position and size
    vec3 center;
    float radius;
    
    // Colors
    vec4 primaryColor;
    vec4 secondaryColor;
    vec4 tertiaryColor;
    vec4 highlightColor;
    
    // Animation
    float time;           // Global time
    float animSpeed;      // Speed multiplier
    float phase;          // Phase offset
    float intensity;      // Overall intensity (0-2)
    
    // Appearance
    float coreSize;       // Size of bright core (0-1)
    float edgeSharpness;  // Edge falloff sharpness
    
    // Spirals
    float spiralDensity;  // Number of spiral arms
    float spiralTwist;    // Amount of twist
    
    // Glow lines
    float glowLineCount;      // Number of radial lines
    float glowLineIntensity;  // Line brightness
    
    // Corona
    float coronaWidth;        // Width of corona glow (0.1-1.5)
    bool showCorona;
    bool showExternalRays;
};

/**
 * Default EnergyOrbConfig.
 */
EnergyOrbConfig defaultEnergyOrbConfig() {
    EnergyOrbConfig cfg;
    cfg.center = vec3(0.0);
    cfg.radius = 1.0;
    cfg.primaryColor = vec4(1.0, 0.5, 0.2, 1.0);
    cfg.secondaryColor = vec4(1.0, 0.3, 0.1, 1.0);
    cfg.tertiaryColor = vec4(1.0, 0.8, 0.5, 1.0);
    cfg.highlightColor = vec4(1.0, 0.8, 0.5, 1.0);
    cfg.time = 0.0;
    cfg.animSpeed = 1.0;
    cfg.phase = 0.0;
    cfg.intensity = 1.0;
    cfg.coreSize = 0.3;
    cfg.edgeSharpness = 2.0;
    cfg.spiralDensity = 5.0;
    cfg.spiralTwist = 2.0;
    cfg.glowLineCount = 8.0;
    cfg.glowLineIntensity = 0.5;
    cfg.coronaWidth = 0.5;
    cfg.showCorona = true;
    cfg.showExternalRays = true;
    return cfg;
}

/**
 * Convert EnergyOrbConfig to FieldData for use with library functions.
 */
FieldData configToFieldData(EnergyOrbConfig cfg) {
    // Compute rayCoronaFlags from booleans
    float rayCoronaFlags = (cfg.showExternalRays ? 1.0 : 0.0) + (cfg.showCorona ? 2.0 : 0.0);
    
    return buildFieldData(
        cfg.center, cfg.radius,
        cfg.primaryColor, cfg.secondaryColor, cfg.tertiaryColor, cfg.highlightColor,
        cfg.phase, cfg.animSpeed, cfg.intensity, 1.0,  // effectType = 1 (energy orb)
        cfg.coreSize, cfg.edgeSharpness, 0.0,          // shapeType = 0
        cfg.spiralDensity, cfg.spiralTwist,
        cfg.glowLineCount, cfg.glowLineIntensity, rayCoronaFlags, 1.0,  // V1 version
        cfg.coronaWidth
    );
}

// ═══════════════════════════════════════════════════════════════════════════
// SPHERICAL COORDINATES HELPER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert local position (relative to orb center) to spherical coordinates.
 * 
 * @param localPos     Position relative to orb center
 * @param radius       Orb radius (for normalization)
 * @return             vec3(theta, phi, distToCenter)
 *                     theta = azimuth angle in XZ plane (-PI to PI)
 *                     phi = elevation angle from Y axis (-PI/2 to PI/2)
 */
vec3 orbSphericalCoords(vec3 localPos, float radius) {
    float dist = length(localPos);
    if (dist < NEAR_ZERO) {
        return vec3(0.0, 0.0, 0.0);
    }
    
    vec3 n = localPos / dist;
    float theta = atan(n.x, n.z);   // Note: atan(x,z) as in field_visual.fsh
    float phi = asin(clamp(n.y, -1.0, 1.0));
    
    return vec3(theta, phi, dist);
}

#endif // EFFECTS_ENERGY_ORB_TYPES_GLSL
