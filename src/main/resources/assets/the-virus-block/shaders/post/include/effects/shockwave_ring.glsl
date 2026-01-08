// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: SHOCKWAVE RING
// ═══════════════════════════════════════════════════════════════════════════
// 
// Depth-based ring rendering for shockwave effects.
// This is a "Paradigm 3" effect: reads depth buffer, applies effects
// on the surface of existing geometry.
//
// Use for: Ground-following shockwaves, terrain-conforming rings.
//
// Include: #include "include/effects/shockwave_ring.glsl"
// Prerequisites: camera/, sdf/orbital_system, rendering/
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_SHOCKWAVE_RING_GLSL
#define EFFECTS_SHOCKWAVE_RING_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"
#include "../camera/depth.glsl"
#include "../sdf/orbital_system.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// RING CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

struct ShockwaveRingConfig {
    vec3 center;           // Ring center (Y is ground level)
    float ringRadius;      // Current ring radius (expanding)
    float coreThickness;   // Sharp center line width
    float glowWidth;       // Soft glow around core
    float intensity;       // Overall brightness
    
    // Colors
    vec3 coreColor;        // Bright center color
    vec3 glowColor;        // Softer glow color
    
    // Orbital system (for flower shape)
    float orbitalRadius;   // Individual orbital size
    float orbitDistance;   // Distance from center
    int orbitalCount;      // Number of orbitals  
    float phase;           // Rotation phase
    
    // Blending
    float blendRadius;     // Smooth blend between orbitals
    bool combinedMode;     // True = single flower shockwave, False = individual rings
};

ShockwaveRingConfig defaultShockwaveRingConfig() {
    ShockwaveRingConfig cfg;
    cfg.center = vec3(0.0);
    cfg.ringRadius = 5.0;
    cfg.coreThickness = 0.1;
    cfg.glowWidth = 0.5;
    cfg.intensity = 1.0;
    cfg.coreColor = vec3(1.0, 0.8, 0.4);
    cfg.glowColor = vec3(1.0, 0.4, 0.1);
    cfg.orbitalRadius = 0.0;
    cfg.orbitDistance = 0.0;
    cfg.orbitalCount = 0;
    cfg.phase = 0.0;
    cfg.blendRadius = 0.0;
    cfg.combinedMode = true;
    return cfg;
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Quadratic glow falloff.
 */
float shockwaveGlowFalloff(float dist, float radius) {
    if (radius < NEAR_ZERO) return 0.0;
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

/**
 * Ring contribution with core, inner glow, and outer glow.
 */
float ringContribution(float dist, float ringDist, float coreThickness, 
                       float glowWidth, float intensity) {
    float distFromRing = abs(dist - ringDist);
    
    // Core - bright sharp center
    float coreMask = 1.0 - smoothstep(0.0, coreThickness * 0.5, distFromRing);
    
    // Inner glow - medium falloff
    float innerMask = 1.0 - smoothstep(0.0, glowWidth * 0.5, distFromRing);
    
    // Outer glow - soft falloff
    float outerMask = shockwaveGlowFalloff(distFromRing, glowWidth);
    
    return (coreMask * 0.9 + innerMask * 0.4 + outerMask * 0.2) * intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// SHAPE DISTANCE CALCULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get distance from a world position to the shockwave shape.
 * 
 * @param worldPos   Reconstructed world position (from depth buffer)
 * @param cfg        Shockwave configuration
 * @return           Distance to the shape boundary (for ring rendering)
 */
float getShapeDistance(vec3 worldPos, ShockwaveRingConfig cfg) {
    // Simple circle (no orbitals)
    if (cfg.orbitalCount < 1 || cfg.orbitDistance < 0.1) {
        vec2 delta = worldPos.xz - cfg.center.xz;
        return length(delta);
    }
    
    // Orbital system (flower shape or individual)
    return sdfOrbitalSystem3D(
        worldPos, cfg.center,
        0.0,  // No main circle
        cfg.orbitalRadius,
        cfg.orbitDistance,
        cfg.orbitalCount,
        cfg.phase,
        cfg.blendRadius,
        cfg.combinedMode
    ) + cfg.orbitDistance + cfg.orbitalRadius;  // Offset to make distance positive
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Render shockwave ring at a world position.
 * 
 * @param worldPos   World position (reconstructed from depth)
 * @param cfg        Shockwave configuration
 * @return           Ring effect color with alpha
 */
vec4 renderShockwaveRing(vec3 worldPos, ShockwaveRingConfig cfg) {
    // Get distance to shape
    float dist = getShapeDistance(worldPos, cfg);
    
    // Calculate ring contribution
    float ring = ringContribution(
        dist, cfg.ringRadius,
        cfg.coreThickness, cfg.glowWidth, cfg.intensity
    );
    
    if (ring < 0.01) return vec4(0.0);
    
    // Blend between core and glow color based on intensity
    float colorBlend = smoothstep(0.0, 0.5, ring);
    vec3 color = mix(cfg.glowColor, cfg.coreColor, colorBlend);
    
    return vec4(color * ring, ring);
}

/**
 * Render multiple expanding rings.
 * 
 * @param worldPos      World position
 * @param cfg           Base configuration
 * @param ringRadii     Array of ring radii (up to 8)
 * @param ringCount     Number of rings
 */
vec4 renderMultipleRings(vec3 worldPos, ShockwaveRingConfig cfg, 
                          float ringRadii[8], int ringCount) {
    vec4 result = vec4(0.0);
    
    float dist = getShapeDistance(worldPos, cfg);
    
    for (int i = 0; i < ringCount && i < 8; i++) {
        float ring = ringContribution(
            dist, ringRadii[i],
            cfg.coreThickness, cfg.glowWidth, cfg.intensity
        );
        
        float colorBlend = smoothstep(0.0, 0.5, ring);
        vec3 color = mix(cfg.glowColor, cfg.coreColor, colorBlend);
        
        // Additive blend
        result.rgb += color * ring;
        result.a = max(result.a, ring);
    }
    
    return result;
}

#endif // EFFECTS_SHOCKWAVE_RING_GLSL
