// ═══════════════════════════════════════════════════════════════════════════
// ENERGY ORB PULSAR V5 - Based on https://www.shadertoy.com/view/lsf3RH
// Original by trisomie21 - Parameterized version
// ═══════════════════════════════════════════════════════════════════════════
// 
// NAMING: Original's "corona" is actually animated noise (FLAMES)
// We add a separate REAL corona (smooth glow halo)
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ENERGY_ORB_PULSAR_GLSL
#define EFFECTS_ENERGY_ORB_PULSAR_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// NOISE FUNCTIONS (Now from shared library)
// ═══════════════════════════════════════════════════════════════════════════

#include "../core/noise_3d.glsl"

// pulsarSnoise and pulsarSurfaceTexture are now aliases defined in noise_3d.glsl

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// Each hardcoded value from original is replaced with a parameter
// Original line shown in comment, parameterized version below
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbPulsar(
    vec2 uv,
    float aspectRatio,
    float time,                  // Already scaled by animSpeed from caller
    
    // Colors
    vec3 surfaceColor,           // PrimaryColor - star surface
    vec3 flamesColor,            // SecondaryColor - original "orange" (flames)
    vec3 glowColor,              // TertiaryColor - original "orangeRed" (starGlow)
    vec3 coronaColor,            // HighlightColor - NEW corona halo color
    
    // Core - original: radius = 0.24 + brightness * 0.2
    float coreSize,              // Original: 0.24
    float brightness,            // Original: 0.1
    
    // Fade - original: fade = pow(length(2.0 * p), 0.5)
    float fadeScale,             // Original: 2.0
    float fadePower,             // Original: 0.5
    
    // Animation speeds
    float radialSpeed1,          // Original: 0.35
    float radialSpeed2,          // Original: 0.15
    float axialSpeed,            // Original: 0.015
    float loopSpeed,             // Original: 0.2
    
    // Noise - for newTime1/2 and fVal1/2 loops
    float noiseResLow,           // Original: 15.0
    float noiseResHigh,          // Original: 45.0
    float noiseAmplitude,        // Original: 0.5
    float noiseOctaves,          // Original: 7
    float noiseBaseScale,        // Original: 10.0
    float noiseScaleMult,        // Original: 25.0
    
    // Flames (original's "corona") - original: pow(fVal * max(1.1-fade,0), 2.0) * 50.0
    float flamesEdge,            // Original: 1.1
    float flamesPower,           // Original: 2.0
    float flamesMult,            // Original: 50.0
    float flamesTimeScale,       // Original: 1.2
    float flamesInsideFalloff,   // Original: 24.0
    
    // Real Corona (NEW - not in original)
    float coronaWidth,           // Spread of glow
    float coronaPower,           // Falloff exponent
    float coronaBrightness,      // Intensity (0 = off)
    
    // Lighting - original: f * (0.75 + brightness * 0.3)
    float surfaceScale,          // Original: 2.0 (sp *= 2.0 - brightness)
    float lightAmbient,          // Original: 0.75
    float lightDiffuse,          // Original: 0.3
    float surfaceNoiseScale,     // For procedural texture
    
    // Output
    float alphaScale
) {
    // ═══════════════════════════════════════════════════════════════════════
    // Original: float radius = 0.24 + brightness * 0.2;
    float radius = coreSize + brightness * 0.2;
    float invRadius = 1.0 / radius;
    
    // ═══════════════════════════════════════════════════════════════════════
    // Original: vec2 p = -0.5 + uv; p.x *= aspect;
    vec2 p = uv - 0.5;
    p.x *= aspectRatio;
    
    // ═══════════════════════════════════════════════════════════════════════
    // Original: float fade = pow(length(2.0 * p), 0.5);
    float fade = pow(length(fadeScale * p), fadePower);
    float fVal1 = 1.0 - fade;
    float fVal2 = 1.0 - fade;
    
    // ═══════════════════════════════════════════════════════════════════════
    // Original: float angle = atan(p.x, p.y)/6.2832; float dist = length(p);
    float angle = atan(p.x, p.y) / 6.2832;
    float dist = length(p);
    vec3 coord = vec3(angle, dist, time * 0.1);
    
    // ═══════════════════════════════════════════════════════════════════════
    // Original: newTime1 = abs(snoise(coord + vec3(0, -time*(0.35+brightness*0.001), time*0.015), 15.0))
    float newTime1 = abs(pulsarSnoise(
        coord + vec3(0.0, -time * (radialSpeed1 + brightness * 0.001), time * axialSpeed),
        noiseResLow
    ));
    
    // Original: newTime2 = abs(snoise(coord + vec3(0, -time*(0.15+brightness*0.001), time*0.015), 45.0))
    float newTime2 = abs(pulsarSnoise(
        coord + vec3(0.0, -time * (radialSpeed2 + brightness * 0.001), time * axialSpeed),
        noiseResHigh
    ));
    
    // ═══════════════════════════════════════════════════════════════════════
    // Original: for(int i=1; i<=7; i++) { ... fVal1 += (0.5/power) * snoise(..., power*10*(newTime1+1)) }
    int octaves = int(clamp(noiseOctaves, 1.0, 10.0));
    for (int i = 1; i <= 10; i++) {
        if (i > octaves) break;
        
        float power = pow(2.0, float(i + 1));
        
        fVal1 += (noiseAmplitude / power) * pulsarSnoise(
            coord + vec3(0.0, -time, time * loopSpeed),
            power * noiseBaseScale * (newTime1 + 1.0)
        );
        
        fVal2 += (noiseAmplitude / power) * pulsarSnoise(
            coord + vec3(0.0, -time, time * loopSpeed),
            power * noiseScaleMult * (newTime2 + 1.0)
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FLAMES (original's "corona")
    // Original: corona = pow(fVal1 * max(1.1 - fade, 0.0), 2.0) * 50.0
    // Added max(0.0, ...) before pow to prevent NaN from negative values
    float flames = pow(max(0.0, fVal1 * max(flamesEdge - fade, 0.0)), flamesPower) * flamesMult;
    flames += pow(max(0.0, fVal2 * max(flamesEdge - fade, 0.0)), flamesPower) * flamesMult;
    
    // Original: corona *= 1.2 - newTime1
    flames *= max(0.0, flamesTimeScale - newTime1);
    
    // ═══════════════════════════════════════════════════════════════════════
    // STAR SPHERE SURFACE
    vec3 starSphere = vec3(0.0);
    
    // Original: sp = -1.0 + 2.0 * uv; sp.x *= aspect; sp *= (2.0 - brightness)
    vec2 sp = -1.0 + 2.0 * uv;
    sp.x *= aspectRatio;
    sp *= (surfaceScale - brightness);
    
    // Original: r = dot(sp,sp); f = (1.0-sqrt(abs(1.0-r)))/(r) + brightness * 0.5
    float r = dot(sp, sp);
    float f = (1.0 - sqrt(abs(1.0 - r))) / (r + 0.001) + brightness * 0.5;
    
    if (dist < radius) {
        // Original: corona *= pow(dist * invRadius, 24.0)
        flames *= pow(dist * invRadius, flamesInsideFalloff);
        
        // Original: newUv.x = sp.x*f; newUv.y = sp.y*f; newUv += vec2(time, 0)
        vec2 newUv;
        newUv.x = sp.x * f;
        newUv.y = sp.y * f;
        newUv += vec2(time, 0.0);
        
        // Original: texSample = texture(iChannel0, newUv).rgb
        vec3 texSample = pulsarSurfaceTexture(newUv, time, surfaceNoiseScale);
        
        // Original: uOff = texSample.g * brightness * 3.14 + time
        float uOff = texSample.g * brightness * 3.14 + time;
        vec2 starUV = newUv + vec2(uOff, 0.0);
        
        // Original: starSphere = texture(iChannel0, starUV).rgb
        starSphere = pulsarSurfaceTexture(starUV, time, surfaceNoiseScale) * surfaceColor;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STAR GLOW
    // Original: starGlow = min(max(1.0 - dist * (1.0 - brightness), 0.0), 1.0)
    float starGlow = clamp(1.0 - dist * (1.0 - brightness), 0.0, 1.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // REAL CORONA (NEW - smooth glow halo not in original)
    // Calculate distance from the edge of the star
    float edgeDist = max(0.0, dist - radius);
    // Power-based falloff from the edge - coronaWidth controls spread, coronaPower controls sharpness
    float coronaFalloff = edgeDist / max(0.01, coronaWidth);
    float corona = pow(max(0.0, 1.0 - coronaFalloff), coronaPower) * coronaBrightness;
    // Only show corona outside the star surface (slightly overlapping is fine)
    corona *= smoothstep(radius * 0.9, radius, dist);
    
    // ═══════════════════════════════════════════════════════════════════════
    // FINAL COMPOSITION
    // Original: fragColor.rgb = vec3(f * (0.75 + brightness * 0.3) * orange) + starSphere + corona * orange + starGlow * orangeRed
    // Note: original "corona" is now "flames", real corona is added separately
    vec3 col = vec3(0.0);
    
    // 1. f-term (uses flamesColor like original's "orange")
    col += vec3(f * (lightAmbient + brightness * lightDiffuse)) * flamesColor;
    
    // 2. Star sphere surface
    col += starSphere;
    
    // 3. Flames (original's "corona" - animated noise)
    col += flames * flamesColor;
    
    // 4. Real Corona (NEW) - uses its own color
    col += corona * coronaColor;
    
    // 5. Star glow (uses glowColor like original's "orangeRed")
    col += starGlow * glowColor;
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALPHA
    float shapeAlpha = max(starGlow, max(flames * 0.02, max(corona, dist < radius ? 0.8 : 0.0)));
    float alpha = clamp(shapeAlpha * alphaScale, 0.0, 1.0);
    
    return vec4(col, alpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// PROJECTED VERSION - For 3D sphere positioning in world space
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbPulsarProjected(
    vec2 texCoord,
    vec3 sphereCenter,
    float sphereRadius,
    vec3 camPos,
    vec3 camForward,
    vec3 camUp,
    float fov,
    float aspectRatio,
    
    // Pass through all parameters (MUST match renderEnergyOrbPulsar order)
    float time,
    // Colors
    vec3 surfaceColor,
    vec3 flamesColor,
    vec3 glowColor,
    vec3 coronaColor,
    // Core
    float coreSize,
    float brightness,
    // Fade
    float fadeScale,
    float fadePower,
    // Animation speeds
    float radialSpeed1,
    float radialSpeed2,
    float axialSpeed,
    float loopSpeed,
    // Noise
    float noiseResLow,
    float noiseResHigh,
    float noiseAmplitude,
    float noiseOctaves,
    float noiseBaseScale,
    float noiseScaleMult,
    // Flames
    float flamesEdge,
    float flamesPower,
    float flamesMult,
    float flamesTimeScale,
    float flamesInsideFalloff,
    // Corona
    float coronaWidth,
    float coronaPower,
    float coronaBrightness,
    // Lighting
    float surfaceScale,
    float lightAmbient,
    float lightDiffuse,
    float surfaceNoiseScale,
    // Output
    float alphaScale
) {
    // Project sphere to screen
    vec3 toSphere = sphereCenter - camPos;
    float viewZ = dot(toSphere, camForward);
    if (viewZ < 0.1) return vec4(0.0);  // Behind camera
    
    vec3 camRight = normalize(cross(camForward, camUp));
    vec3 camUpTrue = cross(camRight, camForward);
    
    float viewX = dot(toSphere, camRight);
    float viewY = dot(toSphere, camUpTrue);
    
    float tanHalfFov = tan(fov * 0.5);
    
    // Project to normalized screen space
    float screenX = (viewX / viewZ) / (tanHalfFov * aspectRatio);
    float screenY = (viewY / viewZ) / tanHalfFov;
    
    // Apparent radius in NDC (accounts for distance)
    float apparentRadius = (sphereRadius / viewZ) / tanHalfFov;
    
    // Convert texCoord (0-1) to NDC (-1 to 1) with aspect correction
    vec2 fragNDC = texCoord * 2.0 - 1.0;
    fragNDC.x *= aspectRatio;  // Match V2's approach
    
    // Sphere center in NDC with aspect correction
    vec2 sphereNDC = vec2(screenX * aspectRatio, screenY);  // No Y negation, match V2
    
    // Relative position from sphere center
    vec2 relativeNDC = fragNDC - sphereNDC;
    
    // Scale to Shadertoy UV range (0-1 where 0.5 is center)
    // The Pulsar effect expects center at 0.5, edge at radius ~0.24
    float scale = 0.5 / max(apparentRadius, 0.001);  // UV 0.5 = edge of sphere
    vec2 localUV = relativeNDC * scale + 0.5;
    
    // Early out if very far from sphere
    if (length(localUV - 0.5) > 1.5) {
        return vec4(0.0);
    }
    
    return renderEnergyOrbPulsar(
        localUV, 1.0, time,  // aspectRatio=1.0 since projection already handled it
        surfaceColor, flamesColor, glowColor, coronaColor,
        coreSize, brightness,
        fadeScale, fadePower,
        radialSpeed1, radialSpeed2, axialSpeed, loopSpeed,
        noiseResLow, noiseResHigh, noiseAmplitude, noiseOctaves, noiseBaseScale, noiseScaleMult,
        flamesEdge, flamesPower, flamesMult, flamesTimeScale, flamesInsideFalloff,
        coronaWidth, coronaPower, coronaBrightness,
        surfaceScale, lightAmbient, lightDiffuse, surfaceNoiseScale,
        alphaScale
    );
}

#endif // EFFECTS_ENERGY_ORB_PULSAR_GLSL
