// ═══════════════════════════════════════════════════════════════════════════
// PULSAR V6 - RAYMARCHED VERSION
// ═══════════════════════════════════════════════════════════════════════════
// 
// Renders the Pulsar effect as true 3D geometry via ray-sphere intersection.
// Like orbital spheres, this enables:
//   - Natural dark/black colors via mix() compositing
//   - True depth-based occlusion
//   - View-stable surface texture (doesn't wobble with camera)
//   - Corona renders behind the solid core
//
// Based on V5 (energy_orb_pulsar.glsl) by trisomie21
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_PULSAR_V6_GLSL
#define EFFECTS_PULSAR_V6_GLSL

// We need the noise functions from V5
// These are already included via energy_orb_pulsar.glsl

// Use shared result struct from core library
#include "../core/effect_result.glsl"

// Backwards compatibility alias
#define PulsarV6Result EffectResult

// Use shared ray-sphere intersection
#include "../sdf/ray_sphere.glsl"

// Backwards compatibility alias
#define pulsarIntersectSphere(ro, rd, c, r) raySphereIntersectSimple(ro, rd, c, r)

// ═══════════════════════════════════════════════════════════════════════════
// SURFACE COORDINATE MAPPING
// ═══════════════════════════════════════════════════════════════════════════

// Convert 3D sphere surface point to coordinates for noise sampling
struct SurfaceCoords {
    float theta;      // Longitude angle (0-1 around equator)
    float phi;        // Latitude angle (0-1 pole to pole)  
    vec3 localPos;    // Normalized position on sphere (-1 to 1)
    float rimFactor;  // 0 = facing camera, 1 = edge (for rim glow)
};

SurfaceCoords getPulsarSurfaceCoords(vec3 hitPoint, vec3 sphereCenter, float sphereRadius, 
                                      vec3 rayDir, vec3 normal) {
    SurfaceCoords sc;
    
    // Normalized position on sphere surface
    sc.localPos = (hitPoint - sphereCenter) / sphereRadius;
    
    // Spherical coordinates (like latitude/longitude)
    sc.theta = atan(sc.localPos.x, sc.localPos.z) / 6.2832 + 0.5;  // 0-1 around
    sc.phi = asin(clamp(sc.localPos.y, -1.0, 1.0)) / 3.1416 + 0.5;  // 0-1 pole-pole
    
    // Rim factor: how edge-on is this point?
    sc.rimFactor = 1.0 - abs(dot(normal, -rayDir));
    
    return sc;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

PulsarV6Result renderPulsarV6(
    // Ray info
    vec3 rayOrigin, 
    vec3 rayDir, 
    float maxDist,
    
    // Sphere position
    vec3 sphereCenter, 
    float sphereRadius,
    
    // Time
    float time,
    
    // Colors (same as V5)
    vec3 surfaceColor,    // PrimaryColor - star surface
    vec3 flamesColor,     // SecondaryColor - flames
    vec3 glowColor,       // TertiaryColor - rim glow
    vec3 coronaColor,     // HighlightColor - corona halo
    
    // Core params
    float coreSize,       // Original: 0.24
    float brightness,     // Original: 0.1
    
    // Noise params
    float noiseResLow,    // Original: 15.0
    float noiseResHigh,   // Original: 45.0
    float noiseAmplitude, // Original: 0.5
    float noiseOctaves,   // Original: 7
    float noiseBaseScale, // Original: 10.0
    float noiseScaleMult, // Original: 25.0
    
    // Flames params
    float flamesEdge,     // Original: 1.1
    float flamesPower,    // Original: 2.0
    float flamesMult,     // Original: 50.0
    
    // Corona params  
    float coronaWidth,    // Spread of glow
    float coronaPower,    // Falloff exponent
    float coronaBrightness, // Intensity
    
    // Animation speeds
    float radialSpeed1,   // Original: 0.35
    float radialSpeed2,   // Original: 0.15
    float axialSpeed,     // Original: 0.015
    float loopSpeed,      // Original: 0.2
    
    // Lighting
    float lightAmbient,   // Original: 0.75
    float lightDiffuse,   // Original: 0.3
    
    // Rays (optional - default 0 = disabled)
    float rayIntensity,   // 0 = off, 1+ = visible rays (from RayCoronaFlags bit 0)
    vec3 rayColor,        // RayColor - separate color for rays
    float rayPower,       // RayPower - exponent for ray brightness
    float raySharpness,   // RaySharpness - controls ray edge definition
    float raySpeed        // SpeedRay - animation speed for rays
) {
    PulsarV6Result result;
    result.didHit = false;
    result.color = vec3(0.0);
    result.alpha = 0.0;
    result.distance = maxDist;
    
    // Actual sphere radius including core size
    float actualRadius = sphereRadius * coreSize;
    
    // ═══════════════════════════════════════════════════════════════════════
    // DISTANCE CALCULATIONS (used for corona, rays, and glow)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Find closest approach to sphere center along the ray
    vec3 toCenter = sphereCenter - rayOrigin;
    float tClosest = max(0.0, dot(toCenter, rayDir));
    vec3 closestPoint = rayOrigin + rayDir * tClosest;
    float distToCenter = length(closestPoint - sphereCenter);
    
    // Normalized distance from center (0 = at center, 1 = at surface, >1 = outside)
    float normalizedDist = distToCenter / actualRadius;
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXTERNAL RAYS - Radial light beams extending from the star
    // ═══════════════════════════════════════════════════════════════════════
    
    // Get the direction from sphere center to closest point (for radial angle)
    vec3 radialDir = normalize(closestPoint - sphereCenter);
    if (length(closestPoint - sphereCenter) < 0.001) {
        radialDir = vec3(0.0, 1.0, 0.0);
    }
    
    // Compute radial angle for ray pattern
    float rayAngle = atan(radialDir.x, radialDir.z);
    float rayPhiAngle = asin(clamp(radialDir.y, -1.0, 1.0));
    
    // Create ray pattern - multiple rays around the sphere
    float numRays = 8.0 * raySharpness;  // raySharpness controls count
    float rayPattern = abs(sin(rayAngle * numRays + time * raySpeed));
    rayPattern *= abs(cos(rayPhiAngle * 4.0 + time * raySpeed * 0.3));
    rayPattern = pow(rayPattern, rayPower);  // rayPower controls intensity curve
    
    // Rays only visible outside the sphere, fading with distance
    // AND only if the closest point is in front of scene geometry (depth check)
    float rayFalloff = max(0.0, normalizedDist - 1.0) / (coronaWidth * 3.0);
    float rays = rayPattern * pow(max(0.0, 1.0 - rayFalloff), 2.0) * raySharpness * 0.3;
    rays *= smoothstep(1.0, 1.2, normalizedDist);  // Fade in at sphere edge
    rays *= rayIntensity;  // Controlled by parameter (0 = disabled by default)
    
    // Depth check: only render rays where closest approach is in front of scene
    if (tClosest > maxDist) {
        rays = 0.0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORONA (soft glow around the sphere)
    // ═══════════════════════════════════════════════════════════════════════
    
    float outsideDist = max(0.0, distToCenter - actualRadius);
    float coronaFalloff = outsideDist / max(0.01, coronaWidth * sphereRadius);
    float corona = pow(max(0.0, 1.0 - coronaFalloff), coronaPower) * coronaBrightness;
    
    // Star glow (closer = brighter, like V5's starGlow)
    float glowDist = distToCenter / (sphereRadius * 2.0);
    float starGlow = max(0.0, 1.0 - glowDist) * brightness;
    
    // ═══════════════════════════════════════════════════════════════════════
    // SPHERE INTERSECTION
    // ═══════════════════════════════════════════════════════════════════════
    
    float t = pulsarIntersectSphere(rayOrigin, rayDir, sphereCenter, actualRadius);
    
    if (t > 0.0 && t < maxDist) {
        result.didHit = true;
        result.distance = t;
        
        vec3 hitPoint = rayOrigin + rayDir * t;
        vec3 normal = normalize(hitPoint - sphereCenter);
        
        // Get surface coordinates for noise sampling
        SurfaceCoords sc = getPulsarSurfaceCoords(hitPoint, sphereCenter, actualRadius, rayDir, normal);
        
        // ═══════════════════════════════════════════════════════════════════
        // SURFACE NOISE (same algorithm as V5, using spherical coords)
        // ═══════════════════════════════════════════════════════════════════
        
        vec3 coord = vec3(sc.theta, sc.phi * 0.5, time * 0.1);
        
        float newTime1 = abs(pulsarSnoise(
            coord + vec3(0.0, -time * radialSpeed1, time * axialSpeed),
            noiseResLow
        ));
        
        float newTime2 = abs(pulsarSnoise(
            coord + vec3(0.0, -time * radialSpeed2, time * axialSpeed),
            noiseResHigh
        ));
        
        float fVal1 = 1.0;
        float fVal2 = 1.0;
        
        int octaves = int(clamp(noiseOctaves, 1.0, 10.0));
        for (int i = 1; i <= 10; i++) {
            if (i > octaves) break;
            float power = pow(2.0, float(i + 1));
            
            // Use loopSpeed for time scaling (was raw -time which is way too fast)
            fVal1 += (noiseAmplitude / power) * pulsarSnoise(
                coord + vec3(0.0, -time * loopSpeed, time * loopSpeed * 0.2),
                power * noiseBaseScale * (newTime1 + 1.0)
            );
            
            fVal2 += (noiseAmplitude / power) * pulsarSnoise(
                coord + vec3(0.0, -time * loopSpeed, time * loopSpeed * 0.2),
                power * noiseScaleMult * (newTime2 + 1.0)
            );
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // FLAMES (at the edges based on rim factor)
        // ═══════════════════════════════════════════════════════════════════
        
        float edgeFactor = sc.rimFactor;
        float flames = pow(max(0.0, fVal1 * max(flamesEdge - edgeFactor, 0.0)), flamesPower) * flamesMult * 0.01;
        flames += pow(max(0.0, fVal2 * max(flamesEdge - edgeFactor, 0.0)), flamesPower) * flamesMult * 0.01;
        
        // ═══════════════════════════════════════════════════════════════════
        // SURFACE TEXTURE
        // ═══════════════════════════════════════════════════════════════════
        
        vec2 texUV = vec2(sc.theta + time * 0.05, sc.phi);
        vec3 surfaceNoise = pulsarSurfaceTexture(texUV, time, noiseBaseScale * 0.5);
        vec3 starSurface = surfaceNoise * surfaceColor;  // PRIMARY COLOR
        
        // ═══════════════════════════════════════════════════════════════════
        // RIM GLOW (bright at edges - uses TERTIARY color)
        // ═══════════════════════════════════════════════════════════════════
        
        float rimGlow = pow(sc.rimFactor, 2.0) * brightness * 2.0;
        
        // ═══════════════════════════════════════════════════════════════════
        // COMPOSE SURFACE COLOR
        // Color mapping:
        // - surfaceColor (PRIMARY): Base sphere texture
        // - flamesColor (SECONDARY): Animated edge flames
        // - glowColor (TERTIARY): Rim/edge glow on sphere
        // - coronaColor (HIGHLIGHT): External corona halo
        // ═══════════════════════════════════════════════════════════════════
        
        vec3 col = vec3(0.0);
        
        // 1. Base surface with lighting (PRIMARY color)
        col += starSurface * (lightAmbient + lightDiffuse * (1.0 - sc.rimFactor));
        
        // 2. Animated flames at edges (SECONDARY color)
        col += flames * flamesColor;
        
        // 3. Rim glow at edges (TERTIARY color)
        col += rimGlow * glowColor;
        
        result.color = col;
        // Softer alpha at edges based on rim factor (more transparent at edges)
        result.alpha = mix(1.0, 0.9, sc.rimFactor);
        
    } else {
        // ═══════════════════════════════════════════════════════════════════
        // NO SPHERE HIT - show external effects
        // Color mapping:
        // - coronaColor (HIGHLIGHT): Corona halo + general glow
        // - rayColor: External rays (if enabled)
        // ═══════════════════════════════════════════════════════════════════
        
        vec3 col = vec3(0.0);
        
        // 1. Corona halo (HIGHLIGHT color)
        col += corona * coronaColor;
        
        // 2. General star glow (also HIGHLIGHT color - they're the same effect zone)
        col += starGlow * coronaColor;
        
        // 3. External rays (RAY color)
        col += rays * rayColor;
        
        result.color = col;
        result.alpha = max(corona, max(starGlow * 0.5, rays * 2.0));
    }
    
    return result;
}

#endif
