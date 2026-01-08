// ═══════════════════════════════════════════════════════════════════════════
// CORONA EFFECTS - Glow Rings, Light Rays, and Atmospheric Effects
// ═══════════════════════════════════════════════════════════════════════════
//
// Source: Panteleymonov Aleksandr's "SunShader" (Unity version)
//
// Purpose:
// ─────────
// Provides reusable functions for corona effects around spherical objects:
// - Subtractive dark rings (limb darkening)
// - Additive glow rings (halos)  
// - Animated light rays (god rays, solar flares)
//
// Dependencies:
// ─────────
// #include "include/core/noise_4d.glsl"  // For ray animation
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORONA_EFFECTS_GLSL
#define CORONA_EFFECTS_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// SUBTRACTIVE GLOW RING - REMOVED
// ═══════════════════════════════════════════════════════════════════════════
// This effect (limb darkening) was disabled in V7/V8 as it created visual
// artifacts and didn't fit the electric/plasma aesthetic. Left as documentation.
// Original function was: subtractiveRing(ray, toCenter, radius, falloff)

// ═══════════════════════════════════════════════════════════════════════════
// ADDITIVE GLOW RING
// ═══════════════════════════════════════════════════════════════════════════
// Creates an additive glow halo around the sphere.
// Added to the final color for atmospheric glow.
//
// Parameters:
//   ray           - Normalized ray direction
//   toCenter      - Vector from camera to sphere center  
//   radius        - Glow radius (sphere radius + corona width)
//   falloffPower  - Falloff exponent (higher = sharper edge)
//   intensity     - Overall brightness multiplier
//
// Returns:
//   Glow intensity [0, 1] to add to color

float additiveGlowRing(
    vec3 ray, 
    vec3 toCenter, 
    float radius, 
    float falloffPower,
    float intensity
) {
    // Distance from ray to sphere center
    float b = dot(ray, toCenter);
    float perpDistSq = dot(toCenter, toCenter) - b * b;
    float perpDist = sqrt(perpDistSq);
    
    // Glow based on proximity to radius
    float glow = max(0.0, 1.0 - abs(perpDist - radius) / radius);
    
    return pow(glow, falloffPower) * intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATED RAY CORONA
// ═══════════════════════════════════════════════════════════════════════════
// Creates animated light rays emanating from the sphere.
// Based on Unity SunShader's fragment ray calculation.
//
// Requires: noise_4d.glsl for noise4q()
//
// Parameters:
//   pr           - Projection point (use sphereProjection().pr)
//   viewRotation - View rotation matrix (can be identity)
//   radius       - Sphere radius (post-zoom)
//   zoom         - Zoom factor
//   perpDistSq   - Perpendicular distance squared (sphere in V7 terms)
//   sqRadius     - Squared radius
//   seedVec      - Seed vector for variation
//   time         - Animated time
//   speedRing    - Ring rotation speed
//   speedRay     - Ray animation speed
//   rayString    - Ray thickness (0.02-10, default 1.0)
//   rayReach     - Ray extent multiplier (1.0 = default)
//   rays         - Intensity exponent (1-10, default 2.0)
//   rayRing      - Ring noise power (1-10, default 1.0)
//   rayGlow      - Ray glow power (1-10, default 2.0)
//   glow         - Overall glow falloff (1-100, default 4.0)
//   rayFade      - Edge fade curve (1=linear, <1=smoother)
//   detail       - LOD level (affects noise sampling)
//
// Returns:
//   Ray corona intensity [0, 1]

float animatedRayCorona(
    vec3 pr,
    mat3 viewRotation,
    float radius,
    float zoom,
    float perpDistSq,
    float sqRadius,
    vec3 seedVec,
    float time,
    float speedRing,
    float speedRay,
    float rayString,
    float rayReach,
    float rays,
    float rayRing,
    float rayGlow,
    float glow,
    float rayFade,
    int detail,
    float eruptionContrast  // NEW: Controls ray discreteness (2.0=default, higher=discrete eruptions)
) {
    // Distance from center in screen space
    float c = length(pr) * zoom;
    
    // Rotate and normalize projection direction
    pr = normalize(viewRotation * pr);
    
    // Ring falloff based on distance from sphere edge
    // Ray extent scales with radius - larger sphere = larger rays
    float rayExtent = radius * rayString * rayReach;
    float s = max(0.0, (1.0 - abs(radius * zoom - c) / rayExtent));
    
    // Apply fade curve
    s = pow(s, rayFade);
    
    // Ring noise (controls ray distribution around the ring)
    // eruptionContrast controls discreteness: higher = more gaps between rays
    float nd = noise4q(vec4(pr + seedVec, -time * speedRing + c)) * 2.0;
    nd = pow(nd, eruptionContrast);  // Was hardcoded 2.0, now configurable
    
    // Distance ratio for fading inside sphere
    float dr = 1.0;
    if (perpDistSq < sqRadius) {
        dr = perpDistSq / sqRadius;
    }
    
    // Ray noise layer 1 (×10 scale)
    vec3 pr10 = pr * 10.0;
    float n = noise4q(vec4(pr10 + seedVec, -time * speedRing + c)) * dr;
    
    // Ray noise layer 2 (×50 scale)
    vec3 pr50 = pr10 * 5.0;
    float ns = noise4q(vec4(pr50 + seedVec, -time * speedRay + c)) * 2.0 * dr;
    
    // Extra detail layer at detail >= 3
    if (detail >= 3) {
        vec3 pr150 = pr50 * 3.0;
        ns = ns * 0.5 + noise4q(vec4(pr150 + seedVec, -time * speedRay + 0.0)) * dr;
    }
    
    // Combine noise layers with exponents
    n = pow(n, rays) * pow(nd, rayRing) * ns;
    
    // Final ray intensity: glow base + ray-modulated glow
    float s3 = pow(s, glow) + pow(s, rayGlow) * n;
    
    return s3;
}

// ═══════════════════════════════════════════════════════════════════════════
// SIMPLE HALO
// ═══════════════════════════════════════════════════════════════════════════
// Quick atmospheric halo without animation.
// Good for basic glow effects.
//
// Parameters:
//   distFromCenter - Distance from pixel to sphere center
//   sphereRadius   - Sphere radius
//   haloWidth      - Width of halo (in world units)
//   falloff        - Falloff exponent (1=linear, 2=quadratic, etc.)
//
// Returns:
//   Halo intensity [0, 1]

float simpleHalo(float distFromCenter, float sphereRadius, float haloWidth, float falloff) {
    float haloEdge = sphereRadius + haloWidth;
    if (distFromCenter > haloEdge) return 0.0;
    if (distFromCenter < sphereRadius) return 1.0;
    
    float t = (distFromCenter - sphereRadius) / haloWidth;
    return pow(1.0 - t, falloff);
}

// ═══════════════════════════════════════════════════════════════════════════
// COLOR HELPERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Blends ray color based on intensity (V7 pattern).
 * Lower intensity uses base color, higher uses light color.
 */
vec3 rayColorBlend(vec3 baseColor, vec3 lightColor, float intensity) {
    return mix(baseColor, lightColor, intensity * intensity * intensity);
}

#endif // CORONA_EFFECTS_GLSL
