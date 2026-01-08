// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: ORBITALS V1 (Raymarched)
// ═══════════════════════════════════════════════════════════════════════════
// 
// V1 "Bottom-Up" rendering for orbital spheres and beams.
// Raymarches through the scene to find orbital/beam surfaces.
//
// FAITHFULLY EXTRACTED from raymarching.glsl.
// All logic, parameters, and magic numbers preserved exactly.
//
// NOTE: This file expects certain uniforms to be defined by the main shader:
//   - BeamWidthAbs, BeamWidthScale, BeamTaper (beam sizing)
//   - RimPower, BeamRimPower (rim lighting)
//   - CoronaWidth (corona glow)
//
// OR: Use the OrbitalsV1Config wrapper which takes all parameters.
//
// Include: #include "include/effects/orbitals_v1.glsl"
// Prerequisites: core/, sdf/, sdf/orbital_system.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ORBITALS_V1_GLSL
#define EFFECTS_ORBITALS_V1_GLSL

#include "../core/constants.glsl"
#include "../sdf/primitives.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL HIT RESULT - Clean abstraction for raymarch results
// ═══════════════════════════════════════════════════════════════════════════

#define HIT_NONE   -1
#define HIT_SPHERE  0
#define HIT_BEAM    1

/**
 * Result from orbital/beam raymarching.
 * Use unpackOrbitalHit() to convert from raw vec4.
 */
struct OrbitalHit {
    bool didHit;       // True if ray hit solid geometry
    float distance;    // Distance along ray to hit point
    float rimAmount;   // Edge glow factor (0=center, 1=edge)
    int hitType;       // HIT_NONE, HIT_SPHERE, or HIT_BEAM
};

/**
 * Convert raw vec4 result from raymarchOrbitalSpheres to clean struct.
 * 
 * Raw vec4 layout: (hitDist, rimAmount, hitType, didHit)
 *   .x = hitDist   - Distance along ray
 *   .y = rimAmount - Edge glow (0-1)
 *   .z = hitType   - 0.0=sphere, 1.0=beam
 *   .w = didHit    - 0.0=miss, 1.0=hit
 */
OrbitalHit unpackOrbitalHit(vec4 hitInfo) {
    OrbitalHit hit;
    hit.didHit = hitInfo.w > 0.5;
    hit.distance = hitInfo.x;
    hit.rimAmount = hitInfo.y;
    hit.hitType = hit.didHit ? (hitInfo.z > 0.5 ? HIT_BEAM : HIT_SPHERE) : HIT_NONE;
    return hit;
}

// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL POSITION HELPER
// ═══════════════════════════════════════════════════════════════════════════

// Note: We need getOrbitalPosition from orbital_system, but can't include
// the full file due to potential uniform dependencies. Define locally:

vec3 getOrbitalPositionV1(vec3 center, int index, int count, float distance, float phase) {
    int safeCount = max(1, count);
    float angle = phase + (float(index) / float(safeCount)) * TWO_PI;
    return center + vec3(cos(angle) * distance, 0.0, sin(angle) * distance);
}

// ═══════════════════════════════════════════════════════════════════════════
// SDF FUNCTIONS (from raymarching.glsl:16-55)
// ═══════════════════════════════════════════════════════════════════════════

// SDF for orbital spheres only (EXACT from raymarching.glsl:17-25)
float sdfOrbitalSpheresOnly(vec3 p, vec3 center, float orbitalRadius,
                            float orbitDistance, int count, float phase) {
    float d = 1e10;  // Start far away
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPositionV1(center, i, count, orbitDistance, phase);
        d = min(d, length(p - orbPos) - orbitalRadius);
    }
    return d;
}

// SDF for beams from orbitals to sky (EXACT from raymarching.glsl:27-47)
// Uses parameters: BeamWidthAbs, BeamWidthScale, BeamTaper
float sdfBeamsWithUniforms(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
                            int count, float phase, float beamHeight,
                            float beamWidthAbs, float beamWidthScale, float beamTaper) {
    if (beamHeight < 0.1) return 1e10;  // No beam
    
    float d = 1e10;
    
    // Beam width: use absolute if provided, otherwise scale (EXACT)
    float baseRadius = (beamWidthAbs > 0.01) ? beamWidthAbs : (orbitalRadius * beamWidthScale);
    
    // Taper: 1.0 = uniform, <1 = narrow at top, >1 = wide at top (EXACT)
    float topRadius = baseRadius * beamTaper;
    
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPositionV1(center, i, count, orbitDistance, phase);
        vec3 beamStart = orbPos;
        vec3 beamEnd = orbPos + vec3(0.0, beamHeight, 0.0);  // Straight up (EXACT)
        d = min(d, sdfTaperedCapsule(p, beamStart, beamEnd, baseRadius, topRadius));
    }
    return d;
}

// Combined SDF for orbitals + beams (EXACT from raymarching.glsl:49-55)
float sdfOrbitalAndBeamsWithUniforms(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
                                      int count, float phase, float beamHeight,
                                      float beamWidthAbs, float beamWidthScale, float beamTaper) {
    float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
    float beamDist = sdfBeamsWithUniforms(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                           beamWidthAbs, beamWidthScale, beamTaper);
    return min(orbDist, beamDist);
}

// ═══════════════════════════════════════════════════════════════════════════
// NORMAL CALCULATION (from raymarching.glsl:61-73)
// ═══════════════════════════════════════════════════════════════════════════

vec3 calcOrbitalNormal(vec3 p, vec3 center, float orbitalRadius,
                        float orbitDistance, int count, float phase, float beamHeight,
                        float beamWidthAbs, float beamWidthScale, float beamTaper) {
    float eps = 0.01;
    float d = sdfOrbitalAndBeamsWithUniforms(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                              beamWidthAbs, beamWidthScale, beamTaper);
    vec3 grad = vec3(
        sdfOrbitalAndBeamsWithUniforms(p + vec3(eps,0,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                        beamWidthAbs, beamWidthScale, beamTaper) - d,
        sdfOrbitalAndBeamsWithUniforms(p + vec3(0,eps,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                        beamWidthAbs, beamWidthScale, beamTaper) - d,
        sdfOrbitalAndBeamsWithUniforms(p + vec3(0,0,eps), center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                        beamWidthAbs, beamWidthScale, beamTaper) - d
    );
    float len = length(grad);
    return len > 0.0001 ? grad / len : vec3(0.0, 1.0, 0.0);  // Fallback (EXACT)
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCH FUNCTION (EXACT from raymarching.glsl:79-126)
// ═══════════════════════════════════════════════════════════════════════════

// Returns: vec4(hitDist, rimAmount, hitType, didHit)
// hitType: 0 = orbital sphere, 1 = beam
// hitDist < 0 means no hit (but may have corona glow in rimAmount)
vec4 raymarchOrbitalSpheres(vec3 rayOrigin, vec3 rayDir, float maxDist,
                             vec3 center, float orbitalRadius, float orbitDistance,
                             int count, float phase, float beamHeight,
                             // Beam params
                             float beamWidthAbs, float beamWidthScale, float beamTaper,
                             // Rim params
                             float rimPower, float beamRimPower,
                             // Corona
                             float coronaWidth) {
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = rayOrigin + rayDir * t;
        float d = sdfOrbitalAndBeamsWithUniforms(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                                   beamWidthAbs, beamWidthScale, beamTaper);
        
        // Hit surface (EXACT)
        if (d < RAYMARCH_EPSILON) {
            // Determine if we hit beam or orbital by comparing distances (EXACT)
            float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
            float beamDist = sdfBeamsWithUniforms(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                                   beamWidthAbs, beamWidthScale, beamTaper);
            float hitType = (beamDist < orbDist) ? 1.0 : 0.0;  // 0=orbital, 1=beam (EXACT)
            
            // Calculate rim/corona based on view angle to normal (EXACT)
            vec3 normal = calcOrbitalNormal(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                             beamWidthAbs, beamWidthScale, beamTaper);
            float rim = 1.0 - abs(dot(normal, -rayDir));
            
            // Use different rim power for beams vs orbitals (EXACT)
            float rimPwr = (hitType > 0.5) ? beamRimPower : rimPower;
            rim = pow(rim, rimPwr);
            
            return vec4(t, rim, hitType, 1.0);
        }
        
        // Too far (EXACT)
        if (t > maxDist) break;
        
        t += d * 0.8;  // Step forward (0.8 for safety) (EXACT)
    }
    
    // Check if we're NEAR an orbital/beam even without hitting (for corona glow) (EXACT)
    float nearestDist = sdfOrbitalAndBeamsWithUniforms(rayOrigin + rayDir * min(t, maxDist * 0.5),
                                                        center, orbitalRadius, orbitDistance, count, phase, beamHeight,
                                                        beamWidthAbs, beamWidthScale, beamTaper);
    float cWidth = max(0.1, coronaWidth);  // minimum 0.1 to avoid div by zero (EXACT)
    if (nearestDist < cWidth) {
        float coronaAmount = 1.0 - (nearestDist / cWidth);
        return vec4(-1.0, coronaAmount * 0.5, 0.0, 0.0);  // No hit, but corona glow (EXACT)
    }
    
    return vec4(-1.0, 0.0, 0.0, 0.0);  // No hit
}

// ═══════════════════════════════════════════════════════════════════════════
// CONFIGURATION STRUCT (for easier usage)
// ═══════════════════════════════════════════════════════════════════════════

struct OrbitalsV1Config {
    vec3 center;
    float orbitalRadius;
    float orbitDistance;
    int count;
    float phase;
    float beamHeight;
    
    // Beam sizing (from uniforms)
    float beamWidthAbs;    // Absolute beam width (if > 0.01, used instead of scale)
    float beamWidthScale;  // Scale factor relative to orbitalRadius
    float beamTaper;       // 1.0 = uniform, <1 = narrow top
    
    // Rim lighting
    float rimPower;        // For orbital spheres
    float beamRimPower;    // For beams
    
    // Corona
    float coronaWidth;
    
    // Appearance
    vec3 orbitalColor;
    vec3 beamColor;
};

OrbitalsV1Config defaultOrbitalsV1Config() {
    OrbitalsV1Config cfg;
    cfg.center = vec3(0.0);
    cfg.orbitalRadius = 0.5;
    cfg.orbitDistance = 3.0;
    cfg.count = 6;
    cfg.phase = 0.0;
    cfg.beamHeight = 0.0;
    cfg.beamWidthAbs = 0.0;
    cfg.beamWidthScale = 0.3;
    cfg.beamTaper = 1.0;
    cfg.rimPower = 2.0;
    cfg.beamRimPower = 1.5;
    cfg.coronaWidth = 0.5;
    cfg.orbitalColor = vec3(1.0, 0.5, 0.2);
    cfg.beamColor = vec3(1.0, 0.8, 0.4);
    return cfg;
}

// ═══════════════════════════════════════════════════════════════════════════
// CONVENIENCE RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderOrbitalsV1(OrbitalsV1Config cfg, vec3 camPos, vec3 rayDir, vec3 forward, float sceneDepth) {
    float maxDist = length(cfg.center - camPos) + cfg.orbitDistance + cfg.beamHeight + 10.0;
    
    vec4 hitInfo = raymarchOrbitalSpheres(
        camPos, rayDir, maxDist,
        cfg.center, cfg.orbitalRadius, cfg.orbitDistance,
        cfg.count, cfg.phase, cfg.beamHeight,
        cfg.beamWidthAbs, cfg.beamWidthScale, cfg.beamTaper,
        cfg.rimPower, cfg.beamRimPower,
        cfg.coronaWidth
    );
    
    if (hitInfo.w > 0.5) {
        // Hit!
        float hitDist = hitInfo.x;
        float rim = hitInfo.y;
        bool isBeam = hitInfo.z > 0.5;
        
        // Depth occlusion check
        float hitZ = hitDist * dot(rayDir, forward);
        if (hitZ > sceneDepth) {
            return vec4(0.0);  // Occluded
        }
        
        // Color based on what we hit
        vec3 baseColor = isBeam ? cfg.beamColor : cfg.orbitalColor;
        vec3 color = baseColor * (0.5 + rim * 0.5);
        float alpha = 0.8 + rim * 0.2;
        
        return vec4(color, alpha);
        
    } else if (hitInfo.y > 0.01) {
        // Corona glow
        vec3 glowColor = cfg.orbitalColor * hitInfo.y;
        return vec4(glowColor, hitInfo.y * 0.3);
    }
    
    return vec4(0.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGACY ALIAS (for shockwave_ring.fsh compatibility)
// ═══════════════════════════════════════════════════════════════════════════

// Old signature (9 params) - uses default beam/rim/corona values
vec4 raymarchOrbitalSpheresLegacy(vec3 rayOrigin, vec3 rayDir, float maxDist,
                                   vec3 center, float orbitalRadius, float orbitDistance,
                                   int count, float phase, float beamHeight) {
    // Use defaults matching old raymarching.glsl behavior
    return raymarchOrbitalSpheres(
        rayOrigin, rayDir, maxDist,
        center, orbitalRadius, orbitDistance,
        count, phase, beamHeight,
        0.0,   // beamWidthAbs (use scale instead)
        0.3,   // beamWidthScale
        1.0,   // beamTaper
        2.0,   // rimPower
        1.5,   // beamRimPower
        0.5    // coronaWidth
    );
}

#endif // EFFECTS_ORBITALS_V1_GLSL
