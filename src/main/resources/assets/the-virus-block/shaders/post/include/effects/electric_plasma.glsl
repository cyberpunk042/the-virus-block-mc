// ═══════════════════════════════════════════════════════════════════════════
// ELECTRIC PLASMA EFFECTS - V8 Electric Aura Components
// ═══════════════════════════════════════════════════════════════════════════
//
// Source: Adapted from "Noise animation - Electric" by nimitz (stormoid.com)
// Original: https://www.shadertoy.com/view/ldlXRS
// License: CC BY-NC-SA 3.0 (Non-commercial)
//
// Components:
// ───────────
// 1. electricFBM()      - Turbulent (ridged) FBM noise
// 2. electricDualFBM()  - Domain-warped FBM for swirling plasma
// 3. electricRingPattern() - Logarithmic ring pattern
// 4. pulsatingDist()    - Ring distance expansion
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef ELECTRIC_PLASMA_GLSL
#define ELECTRIC_PLASMA_GLSL

// Dependencies
#include "../core/noise_3d.glsl"    // snoise3d for turbulent FBM
#include "../core/noise_4d.glsl"    // noise4q for 4D core plasma

// Constants
#define EP_TAU 6.2831853
#define EP_PI  3.14159265

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1: ROTATION HELPER
// ═══════════════════════════════════════════════════════════════════════════

mat2 epRotate2D(float theta) {
    float c = cos(theta);
    float s = sin(theta);
    return mat2(c, -s, s, c);
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2: TURBULENT FBM (Ridged Noise)
// ═══════════════════════════════════════════════════════════════════════════
// Creates ridge-like patterns using abs(noise - 0.5).
// This produces sharp creases that look like electric arcs.
//
// nimitz original:
//   rz += abs((noise(p) - 0.5) * 2.0) / z;
//
// Parameters:
//   p        - 2D sample position
//   time     - Animation time (used as Z coordinate for 3D noise)
//   octaves  - Number of octaves (1-6)
//
// Returns:
//   Turbulent noise value in range [0, ~2]

float electricFBM(vec2 p, float time, int octaves) {
    float z = 2.0;
    float rz = 0.0;
    
    // 3D noise with time as Z for animation
    vec3 p3 = vec3(p, time * 0.1);
    
    for (int i = 1; i <= 6; i++) {
        if (i > octaves) break;
        
        // Sample 3D noise, map [-1,1] to [0,1]
        float n = snoise3d(p3, 10.0);
        n = n * 0.5 + 0.5;
        
        // Ridged: take abs of (n - 0.5) * 2
        rz += abs((n - 0.5) * 2.0) / z;
        
        z *= 2.0;
        p3 *= 2.0;
    }
    
    return rz;
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3: DOMAIN-WARPED FBM (Dual FBM)
// ═══════════════════════════════════════════════════════════════════════════
// Displaces the sampling domain using two FBM calls for organic swirling.
//
// nimitz original:
//   vec2 basis = vec2(fbm(p2 - time*1.6), fbm(p2 + time*1.7));
//   basis = (basis - 0.5) * 0.2;
//   p += basis;
//   return fbm(p * makem2(time * 0.2));
//
// Parameters:
//   p        - 2D sample position
//   time     - Animation time
//   octaves  - FBM octaves (1-6)
//   warpAmt  - Domain warp strength (default 0.2)
//
// Returns:
//   Warped noise value in range [0, ~2]

float electricDualFBM(vec2 p, float time, int octaves, float warpAmt) {
    // Two FBM samples at offset time phases
    vec2 p2 = p * 0.7;
    vec2 basis = vec2(
        electricFBM(p2 - time * 1.6, time, octaves),
        electricFBM(p2 + time * 1.7, time, octaves)
    );
    
    // Displace domain
    basis = (basis - 0.5) * warpAmt;
    p += basis;
    
    // Final sample with time-based rotation
    return electricFBM(p * epRotate2D(time * 0.2), time, octaves);
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4: LOGARITHMIC RING PATTERN
// ═══════════════════════════════════════════════════════════════════════════
// Creates concentric rings with logarithmic spacing.
// Dense near center, spreading outward.
//
// nimitz original:
//   float r = length(p);
//   r = log(sqrt(r));
//   return abs(mod(r * 4.0, tau) - 3.14) * 3.0 + 0.2;
//
// Parameters:
//   dist     - Distance from sphere surface (not 2D coords!)
//   ringFreq - Ring frequency multiplier (default 4.0)
//
// Returns:
//   Ring value, typically in range [0.2, ~10]
//   Lower values near ring centers (where we want brightness)

float electricRingPattern(float dist, float ringFreq) {
    // Prevent sqrt of negative and log of zero
    float r = sqrt(max(dist, 0.0001));
    r = log(r + 0.0001);  // +epsilon prevents log(0)
    
    // Create ring pattern using modulo
    return abs(mod(r * ringFreq, EP_TAU) - EP_PI) * 3.0 + 0.2;
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5: PULSATING DISTANCE EXPANSION
// ═══════════════════════════════════════════════════════════════════════════
// Contracts the ring distance over time, making rings appear to expand outward.
//
// nimitz original:
//   p /= exp(mod(time * 10.0, 3.14159));
//
// Parameters:
//   dist       - Distance from sphere surface
//   time       - Animation time
//   pulseSpeed - Speed of pulsation (default 10.0)
//   pulseCycle - Period of one pulse cycle (default PI)
//
// Returns:
//   Modified distance for ring calculation

float pulsatingDist(float dist, float time, float pulseSpeed, float pulseCycle) {
    float expansion = exp(mod(time * pulseSpeed, pulseCycle));
    return dist / expansion;
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7: RING MODULATION
// ═══════════════════════════════════════════════════════════════════════════
// Combines ring pattern with plasma to create the modulation effect.
//
// nimitz original:
//   rz *= pow(abs(0.1 - circ(p)), 0.9);
//
// Parameters:
//   rings       - Value from electricRingPattern()
//   centerValue - Target ring value for brightness (default 0.1)
//   power       - Power curve for modulation (default 0.9)
//
// Returns:
//   Modulation factor [0, ~1]

float ringModulation(float rings, float centerValue, float power) {
    return pow(abs(centerValue - rings), power);
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 8: DISTANCE FADE (Corona Boundary)
// ═══════════════════════════════════════════════════════════════════════════
// Smooth fade at corona edge to blend into scene.
//
// Parameters:
//   surfaceDist - Distance from sphere surface
//   maxReach    - Maximum corona extent
//   fadeWidth   - Width of fade transition zone
//
// Returns:
//   Fade factor [0, 1]

float coronaFade(float surfaceDist, float maxReach, float fadeWidth) {
    if (surfaceDist > maxReach) return 0.0;
    
    float fadeStart = maxReach - fadeWidth;
    if (surfaceDist < fadeStart) return 1.0;
    
    return 1.0 - smoothstep(fadeStart, maxReach, surfaceDist);
}


// ═══════════════════════════════════════════════════════════════════════════
// SECTION 9: ANIMATED ELECTRIC CORONA V8 (Full V7 + V8 Control)
// ═══════════════════════════════════════════════════════════════════════════
// V8 version with proper separation:
//   - V7 params: Base effect behavior (backwards compatible)
//   - V8 Plasma: Additional noise texture control
//   - V8 Ring: Additional ring pattern control  
//   - V8 Corona: Edge fade/transition envelope
//
// V8 params ADD control, they don't replace V7.

float animatedElectricCoronaV8(
    // ─── Core inputs (9) ───
    vec3 pr,              // Projection point (3D)
    mat3 viewRotation,    // View rotation matrix
    float radius,         // Sphere radius
    float zoom,           // Zoom factor
    float perpDistSq,     // Perpendicular distance squared
    float sqRadius,       // Squared radius
    vec3 seedVec,         // Seed for variation
    float time,           // Animation time
    int detail,           // LOD level
    
    // ─── Ground transposition (clamp ring to ground) ───
    float sceneDistance,       // Z-depth to scene geometry (from depth buffer)
    bool isSky,                // Is this pixel sky?
    vec3 rayOrigin,            // Camera position
    vec3 rayDirection,         // Ray direction
    vec3 cameraForward,        // Camera forward (for Z-depth to ray distance conversion)
    vec3 sphereCenter,         // Orb center in world space
    
    // ─── V7 params (9) - base effect control ───
    float speedRing,      // Ring pulse speed
    float speedRay,       // Plasma animation speed
    float rayString,      // Ring frequency base
    float rayReach,       // Corona extent base
    float rays,           // Intensity power
    float rayRing,        // Ring modulation power
    float rayGlow,        // Ring center value
    float glow,           // Warp/turbulence control
    float rayFade,        // Fade curve power
    
    // ─── V8 PLASMA (4) - noise texture control ───
    float v8PlasmaScale,      // Noise frequency (1-50, default 10)
    float v8PlasmaSpeed,      // Noise animation speed multiplier (0-10, default 1)
    float v8PlasmaTurbulence, // Ridged intensity (0=smooth, 1=ridged, default 1)
    float v8PlasmaIntensity,  // Plasma brightness (0-10, default 1)
    
    // ─── V8 RINGS (6) - ring pattern control ───
    float v8RingFrequency,    // Ring count multiplier (1-20, default 4)
    float v8RingSpeed,        // Ring expansion speed (0-20, default 10)
    float v8RingSharpness,    // Ring edge definition (0.1-10, default 3)
    float v8RingCenterValue,  // Ring brightness target (0-0.5, default 0.1)
    float v8RingModPower,     // Ring modulation curve (0-2, default 0.9)
    float v8RingIntensity,    // Ring brightness (0-10, default 1)
    
    // ─── V8 CORONA (4) - edge fade envelope ───
    float v8CoronaExtent,     // Max reach multiplier (1-10, default 2)
    float v8CoronaFadeStart,  // Fade zone start 0-1 (0=edge, 1=center, default 0.5)
    float v8CoronaFadePower,  // Fade curve power (0.1-10, default 1)
    float v8CoronaIntensity,  // Edge brightness (0-10, default 1)
    
    // ─── Proximity darkness range (from main shader) ───
    float darkenRange         // Maximum range where effect is visible
) {
    // ─── Distance from center (same as V7) ───
    float c = length(pr) * zoom;
    
    // ─── Rotate and normalize projection direction (same as V7) ───
    vec3 prn = normalize(viewRotation * pr);
    
    // ─── SURFACE DISTANCE: Unified world-space calculation ───
    // For rings to sync between ground and sky, both must use world-space 3D distance
    float surfaceDist;
    
    if (!isSky && sceneDistance > 0.0) {
        // GROUND: Use ground position for surfaceDist
        // The ring transposes onto the terrain surface
        
        // CRITICAL FIX: sceneDistance is Z-depth (along camera forward), not ray distance!
        // Convert Z-depth to ray distance: rayDist = zDepth / dot(rayDir, forward)
        float cosAngle = dot(rayDirection, cameraForward);
        float rayDist = (cosAngle > 0.001) ? sceneDistance / cosAngle : sceneDistance;
        
        vec3 groundPos = rayOrigin + rayDirection * rayDist;
        float distFromOrb = length(groundPos - sphereCenter);
        surfaceDist = max(0.0, distFromOrb - radius);
    } else {
        // SKY/AIR: Use world-space distance (closest point on ray to orb)
        // This ensures rings travel at the same speed as ground rings
        
        vec3 toOrb = sphereCenter - rayOrigin;
        float rayDot = dot(rayDirection, toOrb);  // Distance along ray to closest approach
        
        // The closest point on the ray to the orb center
        vec3 closestPoint = rayOrigin + rayDirection * max(0.0, rayDot);
        float distFromOrb = length(closestPoint - sphereCenter);
        surfaceDist = max(0.0, distFromOrb - radius);
    }
    
    // ─── Distance ratio for fading inside sphere (same as V7) ───
    float dr = 1.0;
    if (perpDistSq < sqRadius) {
        dr = perpDistSq / sqRadius;
    }
    
    // ─── FADE based on radius and rayReach ───
    // rayReach: 1-1000 slider, multiplies the radius for ray extent
    // Rays should scale WITH the sphere radius
    float effectExtent = radius * rayReach * v8CoronaExtent;
    float fade = 1.0 - (surfaceDist / effectExtent);
    fade = clamp(fade, 0.0, 1.0);
    fade = pow(fade, rayFade);
    
    // ═══════════════════════════════════════════════════════════════════════
    // V8 PLASMA: Domain-warped turbulent FBM (nimitz dualfbm style)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Combined speed: V7 speedRay + V8 plasmaSpeed multiplier
    float plasmaTime = time * speedRay * v8PlasmaSpeed;
    
    // === DOMAIN WARPING (nimitz dualfbm) ===
    vec3 p3Base = prn * v8PlasmaScale * 0.7;
    
    float warp1 = 0.0;
    float warp2 = 0.0;
    {
        float zw = 2.0;
        vec3 pw1 = p3Base;
        vec3 pw2 = p3Base;
        for (int i = 1; i <= 4; i++) {
            float n1 = noise4q(vec4(pw1 + seedVec, -plasmaTime * 1.6));
            float n2 = noise4q(vec4(pw2 + seedVec, plasmaTime * 1.7));
            warp1 += abs((n1 - 0.5) * 2.0) / zw;
            warp2 += abs((n2 - 0.5) * 2.0) / zw;
            zw *= 2.0;
            pw1 *= 2.0;
            pw2 *= 2.0;
        }
    }
    
    // Displace domain with warp (V7 glow controls warp amount)
    vec3 warpOffset = vec3((warp1 - 0.5), (warp2 - 0.5), 0.0) * 0.2 * glow;
    vec3 p3 = prn * v8PlasmaScale + warpOffset;
    
    // === TURBULENT FBM on warped domain ===
    float z = 2.0;
    float rz = 0.0;
    
    for (int i = 1; i <= 6; i++) {
        if (i > detail) break;
        
        float n = noise4q(vec4(p3 + seedVec, -plasmaTime + c));
        
        // V8 turbulence: 0=smooth, 1=fully ridged
        float ridged = abs((n - 0.5) * 2.0);
        float turbulent = mix(n, ridged, v8PlasmaTurbulence);
        
        rz += turbulent / z;
        
        z *= 2.0;
        p3 *= 2.0;
    }
    
    // V8 plasma intensity
    rz *= v8PlasmaIntensity;
    
    // ═══════════════════════════════════════════════════════════════════════
    // V8 3D RINGS: Logarithmic ring pattern (nimitz original approach)
    // ═══════════════════════════════════════════════════════════════════════
    // The mod in the ring pattern creates MULTIPLE bands at once.
    // When expansion wraps, all rings shift inward together - seamless.
    
    float ringTime = time * speedRing * (v8RingSpeed / 10.0);
    
    // Expansion factor: 1 to exp(PI) ≈ 23, then wraps
    float expansion = exp(mod(ringTime, EP_PI));
    float pulseDist = surfaceDist / expansion;
    
    // Logarithmic ring pattern - creates MULTIPLE concentric bands
    float ringDist = sqrt(max(pulseDist, 0.0001));
    float ringVal = log(ringDist + 0.0001);
    
    // mod creates multiple bands, v8RingFrequency controls band count
    float rings = abs(mod(ringVal * rayString * v8RingFrequency, EP_TAU) - EP_PI) * v8RingSharpness + 0.2;
    
    // No visibility fade needed - multiple bands make transitions seamless
    float ringVisibility = 1.0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // RING MODULATION (V7 base + V8 control)
    // ═══════════════════════════════════════════════════════════════════════
    
    // V7 provides base, V8 adds fine control
    float centerVal = (rayGlow * 0.01) + v8RingCenterValue;
    float modPow = (rayRing * 0.1) + v8RingModPower;
    
    float ringMod = pow(abs(centerVal - rings), modPow);
    
    // Apply ring visibility (fade in/out at cycle edges)
    ringMod = mix(1.0, ringMod, ringVisibility);
    
    // BLEND rings with plasma instead of multiplying
    // v8RingIntensity controls how much rings affect the plasma
    // 0 = pure plasma, 1 = full ring modulation (like original nimitz)
    rz = mix(rz, rz * ringMod, v8RingIntensity);
    
    // Prevent division by zero
    rz = max(rz, 0.01);
    
    // ═══════════════════════════════════════════════════════════════════════
    // FINAL INTENSITY
    // ═══════════════════════════════════════════════════════════════════════
    
    float intensity = (1.0 / rz) * fade * dr * rays * 0.1;
    intensity *= v8CoronaIntensity;
    
    return clamp(intensity, 0.0, 1.0);
}

#endif // ELECTRIC_PLASMA_GLSL
