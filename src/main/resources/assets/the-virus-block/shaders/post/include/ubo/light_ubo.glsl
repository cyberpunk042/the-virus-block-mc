// ═══════════════════════════════════════════════════════════════════════════
// LIGHT UBO - Scene Lighting for Volumetric Effects
// ═══════════════════════════════════════════════════════════════════════════
// 
// Binding: 4 (see UBORegistry.LIGHT_BINDING)
// Size: 208 bytes (13 vec4)
//
// Primary use case: Tornado effect with two-point volumetric lighting.
// Supports up to 4 point lights with position, color, and direction.
//
// Usage:
//   #include "include/ubo/light_ubo.glsl"
//   vec3 lightPos = Light0PositionUBO.xyz;
//   float strength = Light0PositionUBO.w;
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef LIGHT_UBO_GLSL
#define LIGHT_UBO_GLSL

layout(std140) uniform LightDataUBO {
    // Header: lightCount (x), ambient RGB (yzw)
    vec4 LightHeaderUBO;
    
    // Light 0 (Tornado: Warm/Orange)
    vec4 Light0PositionUBO;   // xyz = position, w = strength
    vec4 Light0ColorUBO;      // xyz = color, w = attenuation
    vec4 Light0DirectionUBO;  // xyz = direction, w = angle (for spotlights)
    
    // Light 1 (Tornado: Cool/Cyan)
    vec4 Light1PositionUBO;
    vec4 Light1ColorUBO;
    vec4 Light1DirectionUBO;
    
    // Light 2 (Reserved)
    vec4 Light2PositionUBO;
    vec4 Light2ColorUBO;
    vec4 Light2DirectionUBO;
    
    // Light 3 (Reserved)
    vec4 Light3PositionUBO;
    vec4 Light3ColorUBO;
    vec4 Light3DirectionUBO;
};

// Convenience: get light count
#define LightCount int(LightHeaderUBO.x)
#define AmbientColor LightHeaderUBO.yzw

#endif // LIGHT_UBO_GLSL
