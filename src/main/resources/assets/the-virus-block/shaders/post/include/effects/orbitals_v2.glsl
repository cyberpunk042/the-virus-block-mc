// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: ORBITALS V2 (Screen-Space Projected)
// ═══════════════════════════════════════════════════════════════════════════
// 
// V2 "Top-Down" rendering for orbital spheres.
// Projects each orbital to screen space and draws 2D glowing circles.
//
// Use when: Performance matters, simpler visual style acceptable, or for
// distant effects where raymarching precision isn't needed.
//
// ADVANTAGES over V1:
// - Much faster (no raymarching loop per pixel)
// - Simpler math, fewer GPU cycles
// - Works well for distant effects
//
// LIMITATIONS:
// - Less accurate at screen edges (perspective distortion)
// - No volumetric feel
// - Beams rendered as lines, not cylinders
//
// Include: #include "include/effects/orbitals_v2.glsl"
// Prerequisites: camera/, rendering/screen_effects, rendering/depth_mask
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ORBITALS_V2_GLSL
#define EFFECTS_ORBITALS_V2_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"
#include "../camera/projection.glsl"
#include "../sdf/orbital_system.glsl"
#include "../rendering/screen_effects.glsl"
#include "../rendering/depth_mask.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL V2 CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

struct OrbitalsV2Config {
    vec3 center;           // System center
    float orbitalRadius;   // Radius of each orbital sphere
    float orbitDistance;   // Distance from center to orbital centers
    int count;             // Number of orbitals
    float phase;           // Rotation phase (radians)
    
    // Appearance
    vec3 coreColor;        // Center of orbital
    vec3 glowColor;        // Glow around orbital
    float coreIntensity;   // Core brightness
    float glowIntensity;   // Glow brightness
    float glowFalloff;     // Glow falloff exponent
    
    // Beams (V2 renders as lines)
    float beamHeight;      // Height of beams (0 = no beams)
    vec3 beamColor;        // Beam color
    float beamWidth;       // Visual width (screen-space)
    float beamGlow;        // Beam glow intensity
};

OrbitalsV2Config defaultOrbitalsV2Config() {
    OrbitalsV2Config cfg;
    cfg.center = vec3(0.0);
    cfg.orbitalRadius = 0.5;
    cfg.orbitDistance = 3.0;
    cfg.count = 6;
    cfg.phase = 0.0;
    cfg.coreColor = vec3(1.0, 0.6, 0.2);
    cfg.glowColor = vec3(1.0, 0.4, 0.1);
    cfg.coreIntensity = 1.0;
    cfg.glowIntensity = 0.5;
    cfg.glowFalloff = 2.0;
    cfg.beamHeight = 0.0;
    cfg.beamColor = vec3(1.0, 0.8, 0.4);
    cfg.beamWidth = 0.02;
    cfg.beamGlow = 0.5;
    return cfg;
}

// ═══════════════════════════════════════════════════════════════════════════
// SINGLE ORBITAL RENDERING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Render a single orbital sphere at given position.
 */
vec4 renderSingleOrbitalV2(vec2 texCoord, vec3 orbitalPos, float radius,
                            CameraData cam, float sceneDepth,
                            vec3 coreColor, vec3 glowColor,
                            float coreIntensity, float glowIntensity, float glowFalloff) {
    
    // Project orbital to screen
    SphereProjection proj = projectSphere(orbitalPos, radius, cam);
    
    if (!proj.isVisible) return vec4(0.0);
    
    // Depth occlusion
    float visibility = occlusionSoft(proj.viewDepth, sceneDepth, 1.0);
    if (visibility < 0.01) return vec4(0.0);
    
    // Distance from pixel to projected center
    float dist = distToPoint(texCoord, proj.screenCenter, cam.aspect);
    float normalizedDist = dist / max(proj.apparentRadius, NEAR_ZERO);
    
    // Only process nearby pixels
    if (normalizedDist > 3.0) return vec4(0.0);
    
    // Core (solid color)
    float coreMask = softEdge(normalizedDist, 0.0, 1.0) * coreIntensity;
    vec3 core = coreColor * coreMask;
    
    // Glow (extends beyond edge)
    float glowMask = glowFalloff(normalizedDist, glowFalloff) * glowIntensity;
    vec3 glow = glowColor * glowMask;
    
    // Combine
    vec3 color = core + glow;
    float alpha = max(coreMask, glowMask) * visibility;
    
    return vec4(color * visibility, alpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// BEAM RENDERING (as 2D line)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Render a beam as a 2D line on screen.
 */
vec4 renderBeamV2(vec2 texCoord, vec3 beamStart, vec3 beamEnd,
                   CameraData cam, float sceneDepth,
                   vec3 beamColor, float beamWidth, float beamGlow) {
    
    // Project both endpoints
    vec3 screenStart = worldToScreen(beamStart, cam);
    vec3 screenEnd = worldToScreen(beamEnd, cam);
    
    // Check visibility
    if (screenStart.z < cam.near && screenEnd.z < cam.near) return vec4(0.0);
    
    // Distance from pixel to line segment
    vec2 startUV = screenStart.xy;
    vec2 endUV = screenEnd.xy;
    
    vec2 lineDir = endUV - startUV;
    float lineLen = length(lineDir);
    if (lineLen < 0.001) return vec4(0.0);
    
    lineDir /= lineLen;
    
    vec2 toPixel = texCoord - startUV;
    float projected = clamp(dot(toPixel, lineDir), 0.0, lineLen);
    vec2 closestPoint = startUV + lineDir * projected;
    
    float distToLine = length(texCoord - closestPoint);
    
    // Correct for aspect ratio
    distToLine *= cam.aspect * 0.5;
    
    // Beam visibility
    float beamMask = softEdge(distToLine, 0.0, beamWidth);
    float glowMask = glowFalloff(distToLine / beamWidth, 2.0) * beamGlow;
    
    // Depth check at closest point (interpolate)
    float t = projected / lineLen;
    float pointDepth = mix(screenStart.z, screenEnd.z, t);
    float visibility = occlusionSoft(pointDepth, sceneDepth, 1.0);
    
    vec3 color = beamColor * (beamMask + glowMask);
    float alpha = max(beamMask, glowMask) * visibility;
    
    return vec4(color * visibility, alpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Render all orbitals using screen-space projection (V2).
 */
vec4 renderOrbitalsV2(vec2 texCoord, OrbitalsV2Config cfg, CameraData cam, float sceneDepth) {
    vec4 result = vec4(0.0);
    
    for (int i = 0; i < cfg.count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(cfg.center, i, cfg.count, cfg.orbitDistance, cfg.phase);
        
        // Render orbital sphere
        vec4 orbital = renderSingleOrbitalV2(
            texCoord, orbPos, cfg.orbitalRadius, cam, sceneDepth,
            cfg.coreColor, cfg.glowColor,
            cfg.coreIntensity, cfg.glowIntensity, cfg.glowFalloff
        );
        
        // Additive blend
        result.rgb += orbital.rgb * orbital.a;
        result.a = max(result.a, orbital.a);
        
        // Render beam if enabled
        if (cfg.beamHeight > 0.1) {
            vec3 beamEnd = orbPos + vec3(0.0, cfg.beamHeight, 0.0);
            
            vec4 beam = renderBeamV2(
                texCoord, orbPos, beamEnd, cam, sceneDepth,
                cfg.beamColor, cfg.beamWidth, cfg.beamGlow
            );
            
            result.rgb += beam.rgb * beam.a;
            result.a = max(result.a, beam.a);
        }
    }
    
    return result;
}

#endif // EFFECTS_ORBITALS_V2_GLSL
