// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCHING - Render solid orbital spheres and beams with corona effect
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/raymarching.glsl"
// Requires: sdf_library.glsl (for sdfTaperedCapsule)
// Requires: orbital_math.glsl (for getOrbitalPosition)
// Requires: CoronaWidth, BeamWidthAbs, BeamWidthScale, BeamTaper, RimPower, BeamRimPower uniforms

#ifndef RAYMARCHING_GLSL
#define RAYMARCHING_GLSL

#ifndef MAX_RAYMARCH_STEPS
#define MAX_RAYMARCH_STEPS 48
#endif
#ifndef RAYMARCH_EPSILON
#define RAYMARCH_EPSILON 0.05
#endif

// ═══════════════════════════════════════════════════════════════════════════
// SDF FOR RAYMARCHED OBJECTS
// ═══════════════════════════════════════════════════════════════════════════

// SDF for orbital spheres only (for raymarching the visible orbs)
float sdfOrbitalSpheresOnly(vec3 p, vec3 center, float orbitalRadius,
                            float orbitDistance, int count, float phase) {
    float d = 1e10;  // Start far away
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, length(p - orbPos) - orbitalRadius);
    }
    return d;
}

// SDF for beams from orbitals to sky
float sdfBeams(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
               int count, float phase, float beamHeight) {
    if (beamHeight < 0.1) return 1e10;  // No beam
    
    float d = 1e10;
    
    // Beam width: use absolute if provided, otherwise scale
    float baseRadius = (BeamWidthAbs > 0.01) ? BeamWidthAbs : (orbitalRadius * BeamWidthScale);
    
    // Taper: 1.0 = uniform, <1 = narrow at top, >1 = wide at top
    float topRadius = baseRadius * BeamTaper;
    
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        vec3 beamStart = orbPos;
        vec3 beamEnd = orbPos + vec3(0.0, beamHeight, 0.0);  // Straight up
        d = min(d, sdfTaperedCapsule(p, beamStart, beamEnd, baseRadius, topRadius));
    }
    return d;
}

// Combined SDF for orbitals + beams
float sdfOrbitalAndBeams(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
                         int count, float phase, float beamHeight) {
    float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
    float beamDist = sdfBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    return min(orbDist, beamDist);
}

// ═══════════════════════════════════════════════════════════════════════════
// NORMAL CALCULATION
// ═══════════════════════════════════════════════════════════════════════════

// Calculate surface normal from SDF gradient (for orbitals + beams)
vec3 calcNormal(vec3 p, vec3 center, float orbitalRadius, 
                float orbitDistance, int count, float phase, float beamHeight) {
    float eps = 0.01;
    float d = sdfOrbitalAndBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    vec3 grad = vec3(
        sdfOrbitalAndBeams(p + vec3(eps,0,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d,
        sdfOrbitalAndBeams(p + vec3(0,eps,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d,
        sdfOrbitalAndBeams(p + vec3(0,0,eps), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d
    );
    float len = length(grad);
    return len > 0.0001 ? grad / len : vec3(0.0, 1.0, 0.0);  // Fallback to up if zero gradient
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCHING CORE
// ═══════════════════════════════════════════════════════════════════════════

// Raymarch to find orbital/beam hit
// Returns: vec4(hitDist, rimAmount, hitType, didHit) 
// hitType: 0 = orbital sphere, 1 = beam
// hitDist < 0 means no hit (but may have corona glow in rimAmount)
vec4 raymarchOrbitalSpheres(vec3 rayOrigin, vec3 rayDir, float maxDist,
                            vec3 center, float orbitalRadius, float orbitDistance,
                            int count, float phase, float beamHeight) {
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = rayOrigin + rayDir * t;
        float d = sdfOrbitalAndBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
        
        // Hit surface
        if (d < RAYMARCH_EPSILON) {
            // Determine if we hit beam or orbital by comparing distances
            float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
            float beamDist = sdfBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
            float hitType = (beamDist < orbDist) ? 1.0 : 0.0;  // 0=orbital, 1=beam
            
            // Calculate rim/corona based on view angle to normal
            vec3 normal = calcNormal(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
            float rim = 1.0 - abs(dot(normal, -rayDir));
            
            // Use different rim power for beams vs orbitals
            float rimPwr = (hitType > 0.5) ? BeamRimPower : RimPower;
            rim = pow(rim, rimPwr);
            
            return vec4(t, rim, hitType, 1.0);
        }
        
        // Too far
        if (t > maxDist) break;
        
        t += d * 0.8;  // Step forward (0.8 for safety)
    }
    
    // Check if we're NEAR an orbital/beam even without hitting (for corona glow)
    float nearestDist = sdfOrbitalAndBeams(rayOrigin + rayDir * min(t, maxDist * 0.5), 
                                           center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    float cWidth = max(0.1, CoronaWidth);  // Use uniform, minimum 0.1 to avoid div by zero
    if (nearestDist < cWidth) {
        float coronaAmount = 1.0 - (nearestDist / cWidth);
        return vec4(-1.0, coronaAmount * 0.5, 0.0, 0.0);  // No hit, but corona glow
    }
    
    return vec4(-1.0, 0.0, 0.0, 0.0);  // No hit
}

#endif // RAYMARCHING_GLSL
