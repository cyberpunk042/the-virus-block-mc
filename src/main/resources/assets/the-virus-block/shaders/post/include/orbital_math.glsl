// ═══════════════════════════════════════════════════════════════════════════
// ORBITAL MATH - Flower patterns and orbital system SDFs
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/orbital_math.glsl"
// Requires: sdf_library.glsl (for round_merge, sdf_merge)
// Requires: BlendRadius, CombinedMode uniforms

#ifndef ORBITAL_MATH_GLSL
#define ORBITAL_MATH_GLSL

// Compute orbital position around center in XZ plane
// Skip if already defined by new library (sdf/orbital_system.glsl)
#ifndef GET_ORBITAL_POSITION_DEFINED
#define GET_ORBITAL_POSITION_DEFINED
vec3 getOrbitalPosition(vec3 center, int index, int count, float distance, float phase) {
    int safeCount = max(1, count);  // Prevent division by zero
    float angle = phase + (float(index) / float(safeCount)) * 6.28318;
    return center + vec3(cos(angle) * distance, 0.0, sin(angle) * distance);
}
#endif

// ═══════════════════════════════════════════════════════════════════════════
// 2D ORBITAL SYSTEM (Individual Mode)
// ═══════════════════════════════════════════════════════════════════════════

// 2D Orbital system - computes SDF in XZ plane only (like Ronja's tutorial)
// This produces proper flower-shaped isodistance contours!
float sdfOrbitalSystem2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                         float orbitDistance, int count, float phase) {
    
    // Start with either main circle or first orbital
    float combined;
    bool hasMainCircle = mainRadius > 0.1;
    
    if (hasMainCircle) {
        // Start with main circle
        combined = length(p - center) - mainRadius;
    } else {
        // Start with first orbital (no main circle)
        int safeCount = max(1, count);
        float angle = phase + (0.0 / float(safeCount)) * 6.28318;
        vec2 firstOrbPos = center + vec2(cos(angle), sin(angle)) * orbitDistance;
        combined = length(p - firstOrbPos) - orbitalRadius;
    }
    
    // Determine which combination function to use
    bool useRoundMerge = BlendRadius > 0.001;
    
    // Combine all orbitals into the system
    int startIdx = hasMainCircle ? 0 : 1;
    int safeCount = max(1, count);
    for (int i = startIdx; i < count && i < 32; i++) {
        float angle = phase + (float(i) / float(safeCount)) * 6.28318;
        vec2 orbPos = center + vec2(cos(angle), sin(angle)) * orbitDistance;
        float orbDist = length(p - orbPos) - orbitalRadius;
        
        if (useRoundMerge) {
            // Proper smooth union - creates correct flower contours!
            combined = round_merge(combined, orbDist, BlendRadius);
        } else {
            // Hard union (BlendRadius = 0 or negative)
            combined = sdf_merge(combined, orbDist);
        }
    }
    
    return combined;
}

// ═══════════════════════════════════════════════════════════════════════════
// COMBINED FLOWER MODE
// ═══════════════════════════════════════════════════════════════════════════

// COMBINED MODE: Distance from center, with flower-shaped radial modulation
// The center is the SOURCE. Orbitals define the shape of the expanding wave.
// This creates a single unified shockwave that follows petal curves.
// 
// KEY INSIGHT: To preserve flower shape at ALL distances, we compute a "normalized"
// distance where 1.0 = on the flower boundary. The RingRadius then scales this.
float sdfCombinedFlower2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                          float orbitDistance, int count, float phase) {
    
    vec2 rel = p - center;
    float distFromCenter = length(rel);
    
    // Handle center point
    if (distFromCenter < 0.001) {
        return -(orbitDistance + orbitalRadius);  // Deeply inside
    }
    
    float angle = atan(rel.y, rel.x);
    
    // Compute angular distance to nearest petal (orbital)
    // Orbitals are at angles: phase, phase + 2π/count, phase + 4π/count, ...
    float angularPeriod = 6.28318 / float(max(1, count));
    float relativeAngle = angle - phase;
    
    // Find distance to nearest petal center (wraps around)
    float nearestPetalAngle = round(relativeAngle / angularPeriod) * angularPeriod;
    float angularDistToPetal = abs(relativeAngle - nearestPetalAngle);
    
    // Normalize to [0, 1]: 0 = at petal center, 1 = between petals
    float normalizedAngularDist = angularDistToPetal / (angularPeriod * 0.5);
    normalizedAngularDist = clamp(normalizedAngularDist, 0.0, 1.0);
    
    // Compute flower radius at this angle
    // At petal (orbital center): radius = orbitDistance + orbitalRadius (outer edge of orbital)
    // Between petals: radius = innerRadius (either mainRadius or valley depth)
    float outerRadius = orbitDistance + orbitalRadius;  // Petal tip
    float innerRadius = mainRadius > 0.1 ? mainRadius : max(0.1, orbitDistance - orbitalRadius);  // Valley
    
    // Smooth interpolation from outer (petal) to inner (valley)
    float t = normalizedAngularDist;
    if (BlendRadius > 0.001) {
        // Smoother transition with blend
        t = smoothstep(0.0, 1.0, t);
    }
    float flowerRadius = mix(outerRadius, innerRadius, t);
    
    // PRESERVE FLOWER SHAPE: Instead of simple distance-to-boundary,
    // compute normalized distance where 1.0 = on the flower surface
    // This way, as RingRadius grows, the shape stays consistent
    float normalizedDist = distFromCenter / flowerRadius;
    
    // Convert back to "SDF-like" value for compatibility with ring system
    // Ring appears where: normalizedDist * outerRadius == RingRadius
    // So we return: (normalizedDist - 1.0) * flowerRadius
    // At boundary: (1-1)*R = 0, Inside: negative, Outside: positive
    return (normalizedDist - 1.0) * outerRadius;
}

// ═══════════════════════════════════════════════════════════════════════════
// 3D ORBITAL SYSTEM WRAPPER
// ═══════════════════════════════════════════════════════════════════════════

// 3D wrapper - projects to XZ plane for flower-shaped shockwave contours
// The Y difference from center adds to the distance for proper 3D falloff
// Skip if already defined by new library (which has different signature)
#ifndef SDF_ORBITAL_SYSTEM_UNIFORM_DEFINED
#define SDF_ORBITAL_SYSTEM_UNIFORM_DEFINED
float sdfOrbitalSystem(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                       float orbitDistance, int count, float phase) {
    
    vec2 p2D = p.xz;
    vec2 center2D = center.xz;
    float dist2D;
    
    // CombinedMode > 0.5 = Combined shockwave from center (flower-shaped)
    // CombinedMode <= 0.5 = Individual orbital sources (legacy SDF union)
    if (CombinedMode > 0.5) {
        // COMBINED MODE: Single shockwave from center
        // 
        // KEY INSIGHT: Like non-combined mode, we want the wave to naturally
        // become circular as it travels farther from the source. This happens
        // automatically with SDF unions, but we need to simulate it here.
        //
        // Strategy: Blend from flower shape to circle based on ACTUAL distance
        // from center compared to the flower's size.
        
        // Compute flower-shaped distance (petal contours)
        float flowerDist = sdfCombinedFlower2D(p2D, center2D, mainRadius, orbitalRadius,
                                               orbitDistance, count, phase);
        
        // Compute simple circular distance (unified wave from outer petal tips)
        float outerRadius = orbitDistance + orbitalRadius;
        float circleDist = length(p2D - center2D) - outerRadius;
        
        // Actual distance from center (not the SDF, the raw distance)
        float actualDist = length(p2D - center2D);
        
        // AUTOMATIC CIRCULARIZATION based on distance:
        // - At actualDist <= outerRadius: pure flower shape (we're near/inside the source)
        // - At actualDist = outerRadius * 2: 50% blend
        // - At actualDist >= outerRadius * 3: fully circular (far from source)
        //
        // BlendRadius can adjust the transition speed:
        //   BlendRadius = 0: default transition (circular at 3x flower size)
        //   BlendRadius > 0: faster transition (becomes circular sooner)
        //   BlendRadius < 0: slower transition (stays flower-shaped longer)
        
        float transitionScale = 3.0 - (BlendRadius / 25.0);  // BlendRadius=50 -> 1x, BlendRadius=-50 -> 5x
        transitionScale = max(1.5, transitionScale);  // Never blend before 1.5x the flower size
        
        float blendStart = outerRadius;
        float blendEnd = outerRadius * transitionScale;
        
        float circleBlend = smoothstep(blendStart, blendEnd, actualDist);
        
        dist2D = mix(flowerDist, circleDist, circleBlend);
    } else {
        // INDIVIDUAL MODE: Each orbital is its own source (legacy behavior)
        dist2D = sdfOrbitalSystem2D(p2D, center2D, mainRadius, orbitalRadius,
                                     orbitDistance, count, phase);
    }
    
    // Add Y-distance contribution for proper 3D behavior
    float yDiff = abs(p.y - center.y);
    
    if (dist2D > 0.0) {
        // Outside the shape - add 3D falloff
        return sqrt(dist2D * dist2D + yDiff * yDiff);
    } else {
        // Inside the shape - adjust for Y distance
        return dist2D + yDiff;
    }
}
#endif // SDF_ORBITAL_SYSTEM_UNIFORM_DEFINED

#endif // ORBITAL_MATH_GLSL
