// ═══════════════════════════════════════════════════════════════════════════
// SDF: ORBITAL SYSTEM
// ═══════════════════════════════════════════════════════════════════════════
// 
// Complex SDFs for orbital systems (spheres orbiting a center).
// Includes flower patterns and combined mode for shockwave effects.
//
// Note: This file expects BlendRadius and CombinedMode uniforms to be defined
// by the main shader. These control the blending behavior.
//
// Include: #include "include/sdf/orbital_system.glsl"
// Prerequisites: core/constants.glsl, sdf/primitives.glsl, sdf/operations.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SDF_ORBITAL_SYSTEM_GLSL
#define SDF_ORBITAL_SYSTEM_GLSL

#include "../core/constants.glsl"
#include "primitives.glsl"
#include "operations.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL POSITION CALCULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Compute position of nth orbital around center.
 * Orbitals are arranged in a circle in the XZ plane.
 * 
 * @param center    System center
 * @param index     Which orbital (0 to count-1)
 * @param count     Total number of orbitals
 * @param distance  Distance from center to orbital center
 * @param phase     Rotation phase (radians)
 * @return          World position of orbital center
 */
#ifndef GET_ORBITAL_POSITION_DEFINED
#define GET_ORBITAL_POSITION_DEFINED
vec3 getOrbitalPosition(vec3 center, int index, int count, float distance, float phase) {
    int safeCount = max(1, count);
    float angle = phase + (float(index) / float(safeCount)) * TWO_PI;
    return center + vec3(cos(angle) * distance, 0.0, sin(angle) * distance);
}
#endif

/**
 * 2D version (for ground-plane calculations).
 */
vec2 getOrbitalPosition2D(vec2 center, int index, int count, float distance, float phase) {
    int safeCount = max(1, count);
    float angle = phase + (float(index) / float(safeCount)) * TWO_PI;
    return center + vec2(cos(angle), sin(angle)) * distance;
}

// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL SPHERES SDF
// ═══════════════════════════════════════════════════════════════════════════

/**
 * SDF for multiple orbital spheres only (no main sphere).
 * 
 * @param p               Sample point
 * @param center          System center
 * @param orbitalRadius   Radius of each orbital sphere
 * @param orbitDistance   Distance from center to orbital centers
 * @param count           Number of orbitals
 * @param phase           Rotation phase
 */
float sdfOrbitalSpheres(vec3 p, vec3 center, float orbitalRadius, 
                        float orbitDistance, int count, float phase) {
    float d = 1e10;  // Start far away
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, sdfSphere(p, orbPos, orbitalRadius));
    }
    return d;
}

/**
 * SDF for beams rising from each orbital.
 * 
 * @param p               Sample point
 * @param center          System center
 * @param orbitalRadius   Radius of orbital spheres (base width reference)
 * @param orbitDistance   Distance from center to orbital centers
 * @param count           Number of orbitals
 * @param phase           Rotation phase
 * @param beamHeight      Height of beams (0 = no beams)
 * @param beamRadius      Base radius of beams
 * @param taper           Taper factor (1.0 = uniform, <1 = narrow at top)
 */
float sdfOrbitalBeams(vec3 p, vec3 center, float orbitalRadius,
                      float orbitDistance, int count, float phase,
                      float beamHeight, float beamRadius, float taper) {
    if (beamHeight < 0.1) return 1e10;  // No beams
    
    float d = 1e10;
    float topRadius = beamRadius * taper;
    
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        vec3 beamEnd = orbPos + vec3(0.0, beamHeight, 0.0);
        d = min(d, sdfTaperedCapsule(p, orbPos, beamEnd, beamRadius, topRadius));
    }
    return d;
}

/**
 * Combined SDF for orbitals + optional beams.
 */
float sdfOrbitalsAndBeams(vec3 p, vec3 center, float orbitalRadius,
                          float orbitDistance, int count, float phase,
                          float beamHeight, float beamRadius, float taper) {
    float orbDist = sdfOrbitalSpheres(p, center, orbitalRadius, orbitDistance, count, phase);
    float beamDist = sdfOrbitalBeams(p, center, orbitalRadius, orbitDistance, count, phase,
                                      beamHeight, beamRadius, taper);
    return min(orbDist, beamDist);
}

// ═══════════════════════════════════════════════════════════════════════════
// 2D ORBITAL SYSTEM (for shockwave contours)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * 2D orbital system SDF with optional smooth blending.
 * Creates proper flower-shaped isodistance contours.
 * 
 * @param blendRadius  Blend zone for smooth union (0 = sharp)
 */
float sdfOrbitalSystem2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                         float orbitDistance, int count, float phase, float blendRadius) {
    
    // Start with main circle or first orbital
    float combined;
    bool hasMainCircle = mainRadius > 0.1;
    
    if (hasMainCircle) {
        combined = length(p - center) - mainRadius;
    } else {
        vec2 firstOrbPos = getOrbitalPosition2D(center, 0, count, orbitDistance, phase);
        combined = length(p - firstOrbPos) - orbitalRadius;
    }
    
    bool useRoundMerge = blendRadius > NEAR_ZERO;
    
    int startIdx = hasMainCircle ? 0 : 1;
    for (int i = startIdx; i < count && i < 32; i++) {
        vec2 orbPos = getOrbitalPosition2D(center, i, count, orbitDistance, phase);
        float orbDist = length(p - orbPos) - orbitalRadius;
        
        if (useRoundMerge) {
            combined = round_merge(combined, orbDist, blendRadius);
        } else {
            combined = sdf_merge(combined, orbDist);
        }
    }
    
    return combined;
}

// ═══════════════════════════════════════════════════════════════════════════
// COMBINED FLOWER MODE (for unified shockwave)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Flower-shaped distance for combined shockwave mode.
 * The center is the source. Orbitals define petal shape.
 * Produces a single unified shockwave that follows petal curves.
 */
float sdfCombinedFlower2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                          float orbitDistance, int count, float phase, float blendRadius) {
    
    vec2 rel = p - center;
    float distFromCenter = length(rel);
    
    if (distFromCenter < NEAR_ZERO) {
        return -(orbitDistance + orbitalRadius);  // Deeply inside
    }
    
    float angle = atan(rel.y, rel.x);
    
    // Angular distance to nearest petal
    float angularPeriod = TWO_PI / float(max(1, count));
    float relativeAngle = angle - phase;
    float nearestPetalAngle = round(relativeAngle / angularPeriod) * angularPeriod;
    float angularDistToPetal = abs(relativeAngle - nearestPetalAngle);
    
    // Normalize to [0, 1]: 0 = at petal center, 1 = between petals
    float normalizedAngularDist = angularDistToPetal / (angularPeriod * 0.5);
    normalizedAngularDist = clamp(normalizedAngularDist, 0.0, 1.0);
    
    // Flower radius at this angle
    float outerRadius = orbitDistance + orbitalRadius;  // Petal tip
    float innerRadius = mainRadius > 0.1 ? mainRadius : max(0.1, orbitDistance - orbitalRadius);
    
    float t = normalizedAngularDist;
    if (blendRadius > NEAR_ZERO) {
        t = smoothstep(0.0, 1.0, t);
    }
    float flowerRadius = mix(outerRadius, innerRadius, t);
    
    // Normalized distance (1.0 = on flower surface)
    float normalizedDist = distFromCenter / flowerRadius;
    
    return (normalizedDist - 1.0) * outerRadius;
}

// ═══════════════════════════════════════════════════════════════════════════
// 3D ORBITAL SYSTEM (wraps 2D with Y falloff)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Full 3D orbital system SDF.
 * Projects to XZ plane for flower contours, adds Y falloff.
 * 
 * @param combinedMode  If true, uses combined flower mode (single source).
 *                      If false, each orbital is its own source.
 */
float sdfOrbitalSystem3D(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                         float orbitDistance, int count, float phase,
                         float blendRadius, bool combinedMode) {
    
    vec2 p2D = p.xz;
    vec2 center2D = center.xz;
    float dist2D;
    
    if (combinedMode) {
        // Single shockwave from center with flower shape
        float flowerDist = sdfCombinedFlower2D(p2D, center2D, mainRadius, orbitalRadius,
                                               orbitDistance, count, phase, blendRadius);
        
        // Blend to circle at distance
        float outerRadius = orbitDistance + orbitalRadius;
        float circleDist = length(p2D - center2D) - outerRadius;
        float actualDist = length(p2D - center2D);
        
        float transitionScale = 3.0 - (blendRadius / 25.0);
        transitionScale = max(1.5, transitionScale);
        
        float blendStart = outerRadius;
        float blendEnd = outerRadius * transitionScale;
        
        float circleBlend = smoothstep(blendStart, blendEnd, actualDist);
        dist2D = mix(flowerDist, circleDist, circleBlend);
    } else {
        // Individual orbital sources
        dist2D = sdfOrbitalSystem2D(p2D, center2D, mainRadius, orbitalRadius,
                                     orbitDistance, count, phase, blendRadius);
    }
    
    // Add Y-distance for 3D falloff
    float yDiff = abs(p.y - center.y);
    
    if (dist2D > 0.0) {
        return sqrt(dist2D * dist2D + yDiff * yDiff);
    } else {
        return dist2D + yDiff;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BACKWARD COMPATIBILITY ALIAS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Legacy alias for sdfOrbitalSystem3D.
 * Uses defaults: blendRadius=0, combinedMode=false.
 * This matches the old orbital_math.glsl signature.
 * 
 * NOTE: If legacy orbital_math.glsl is also included, skip this version.
 * The orbital_math.glsl version uses CombinedMode/BlendRadius uniforms directly.
 */
#ifndef SDF_ORBITAL_SYSTEM_UNIFORM_DEFINED
#define SDF_ORBITAL_SYSTEM_PARAM_DEFINED
float sdfOrbitalSystem(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                       float orbitDistance, int count, float phase) {
    return sdfOrbitalSystem3D(p, center, mainRadius, orbitalRadius,
                              orbitDistance, count, phase, 0.0, false);
}
#endif

#endif // SDF_ORBITAL_SYSTEM_GLSL
