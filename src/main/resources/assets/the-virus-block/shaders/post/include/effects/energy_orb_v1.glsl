// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: ENERGY ORB V1 (Raymarched)
// ═══════════════════════════════════════════════════════════════════════════
// 
// V1 "Bottom-Up" rendering: Shoots rays through each pixel and marches
// until hitting the orb surface. Produces accurate 3D intersection.
//
// FAITHFULLY EXTRACTED from field_visual.fsh lines 290-625.
// All logic, parameters, and magic numbers preserved exactly.
//
// Use when: Need pixel-perfect 3D sphere, volumetric effects.
//
// Include: #include "include/effects/energy_orb_v1.glsl"
// Prerequisites: core/, camera/, sdf/, rendering/, energy_orb_types.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ENERGY_ORB_V1_GLSL
#define EFFECTS_ENERGY_ORB_V1_GLSL

#include "../core/constants.glsl"
#include "../core/math_utils.glsl"
#include "../core/noise_utils.glsl"
#include "../sdf/primitives.glsl"
#include "../rendering/screen_effects.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// NORMAL CALCULATION (from field_visual.fsh:452-461)
// ═══════════════════════════════════════════════════════════════════════════

vec3 calcEnergyOrbNormal(vec3 p, vec3 center, float radius) {
    float eps = 0.01;
    float d = sdfSphere(p, center, radius);
    return normalize(vec3(
        sdfSphere(p + vec3(eps, 0, 0), center, radius) - d,
        sdfSphere(p + vec3(0, eps, 0), center, radius) - d,
        sdfSphere(p + vec3(0, 0, eps), center, radius) - d
    ));
}

// ═══════════════════════════════════════════════════════════════════════════
// CORE EFFECT (from field_visual.fsh:291-303)
// ═══════════════════════════════════════════════════════════════════════════

// Parameters from FieldData:
//   field.CoreEdgeParams.x = coreSize
//   field.AnimParams.z = intensity
//   field.PrimaryColor.rgb = coreColor
vec4 renderEnergyOrbCore(vec3 primaryColor, float distToCenter, float radius,
                          float coreSize, float intensity) {
    float coreRadius = radius * coreSize;
    
    // Core glow - bright at center, fading outward
    // EXACT: uses glowFalloff with pow(0.5) bloom
    float coreFactor = glowFalloff(distToCenter, coreRadius);
    coreFactor = pow(coreFactor, 0.5);  // Bloom more
    
    float coreAlpha = coreFactor * intensity * 0.9;  // EXACT: * 0.9
    
    return vec4(primaryColor, coreAlpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// EDGE EFFECT (from field_visual.fsh:309-321)
// ═══════════════════════════════════════════════════════════════════════════

// Parameters from FieldData:
//   field.CoreEdgeParams.y = edgeSharpness
//   field.AnimParams.z = intensity
//   field.SecondaryColor.rgb = edgeColor
vec4 renderEnergyOrbEdge(vec3 secondaryColor, float distToCenter, float radius,
                          float edgeSharpness, float intensity) {
    float thickness = radius * 0.1;  // EXACT: 0.1 factor
    
    float distFromEdge = abs(distToCenter - radius);
    float edgeFactor_val = edgeFalloff(distFromEdge, thickness, edgeSharpness);
    
    float edgeAlpha = edgeFactor_val * intensity * 0.8;  // EXACT: * 0.8
    
    return vec4(secondaryColor, edgeAlpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// SPIRAL EFFECT (from field_visual.fsh:327-374)
// Uses voronoi noise with twirl distortion
// ═══════════════════════════════════════════════════════════════════════════

// Parameters from FieldData:
//   field.SpiralParams.x = density
//   field.SpiralParams.y = twist
//   field.AnimParams.z = intensity
//   field.AnimParams.y = animSpeed
//   field.AnimParams.x = phase
//   field.SecondaryColor, field.TertiaryColor, field.PrimaryColor for colors
vec4 renderEnergyOrbSpirals(vec3 secondaryColor, vec3 tertiaryColor, vec3 primaryColor,
                             vec3 localPos, float distToCenter, float radius, float time,
                             float density, float twist, float intensity,
                             float animSpeed, float phase) {
    // Only render spirals inside the sphere
    if (distToCenter > radius) {
        return vec4(0.0);
    }
    
    // Project 3D position to 2D for the spiral pattern (EXACT)
    vec2 uv = localPos.xz / radius;  // Normalize to -1..1
    uv = uv * 0.5 + 0.5;             // Convert to 0..1
    
    // Apply twirl effect - creates spiral motion (EXACT)
    float animTime = time * animSpeed + phase;
    vec2 twisted = twirlUV(uv, vec2(0.5), twist);
    twisted = rotate2D(-animTime * 0.3) * (twisted - 0.5) + 0.5;
    
    // First Voronoi layer (slow rotation) (EXACT)
    float v1 = voronoi(twisted * 3.0, density, animTime * 0.5);
    v1 = pow(v1, 2.5);
    
    // Second Voronoi layer (counter-rotation) (EXACT)
    vec2 twisted2 = twirlUV(uv, vec2(0.5), -twist);
    twisted2 = rotate2D(animTime * 0.4) * (twisted2 - 0.5) + 0.5;
    float v2 = voronoi(twisted2 * 3.0, density * 1.6, animTime * 0.3);
    v2 = pow(v2, 3.0);
    
    // Combine layers (EXACT)
    float spiral = v1 + v2 * 0.5;
    
    // Fade spirals near center and edge (EXACT)
    float innerFade = smoothstep(0.0, 0.15 * radius, distToCenter);
    float outerFade = smoothstep(radius, radius * 0.7, distToCenter);
    spiral *= innerFade * outerFade;
    
    // Color: blend between secondary and tertiary (EXACT)
    vec3 spiralColor = mix(secondaryColor, tertiaryColor, spiral);
    spiralColor = mix(spiralColor, primaryColor, spiral * 0.3);  // Add some core color
    
    float spiralAlpha = spiral * intensity * 0.6;  // EXACT: * 0.6
    
    return vec4(spiralColor, spiralAlpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW LINES EFFECT (from field_visual.fsh:380-446)
// Complex radial lines with sdLine() and random phases
// ═══════════════════════════════════════════════════════════════════════════

// Parameters from FieldData:
//   field.GlowLineParams.x = lineCount
//   field.GlowLineParams.y = lineIntensity
//   field.AnimParams.z = intensity
//   field.AnimParams.y = animSpeed
//   field.AnimParams.x = phase
vec4 renderEnergyOrbGlowLines(vec3 secondaryColor, vec3 primaryColor,
                               vec3 localPos, float distToCenter, float radius, float time,
                               float lineCount, float lineIntensity, float intensity,
                               float animSpeed, float phase) {
    if (lineIntensity < 0.01 || lineCount < 1.0) {
        return vec4(0.0);
    }
    
    // Work in 2D from top-down view (EXACT)
    vec2 p = localPos.xz;
    float r = length(p);
    
    // Only draw lines around the edge (between radius and 2x radius) (EXACT)
    if (r < radius * 0.95 || r > radius * 2.0) {
        return vec4(0.0);
    }
    
    // Normalize position (EXACT)
    p = abs(p);  // Use abs for mirror symmetry
    
    // Animate rotation (EXACT)
    float animTime = time * animSpeed + phase;
    float rotAngle = animTime * 0.3;
    
    // Accumulate glow from multiple lines (EXACT)
    vec3 totalGlow = vec3(0.0);
    
    int count = int(lineCount);
    for (int i = 0; i < count && i < 24; i++) {
        // Random offset per line (EXACT)
        float linePhase = fract(float(i) * 123.456 + 867.234) * 5.0;
        float offs = sin(animTime * 15.0 + linePhase) * 0.01;
        
        // Line extends from radius outward (EXACT)
        float baseAngle = float(i) * TAU / lineCount + rotAngle;
        float h = radius + fract(float(i) * 345.67 + 12.35) * 0.1 * radius;
        float s = fract(float(i) * 342.968 + 123.467) * 0.001;
        
        // Line start and end points (EXACT)
        vec2 start = vec2(0.0, radius + offs);
        vec2 end = vec2(0.0, h + offs);
        
        // Rotate the line position (EXACT: 20 degrees per line = 0.349 radians)
        vec2 rotP = rotate2D(baseAngle + float(i) * 0.349) * p;
        
        // Distance to this line (EXACT)
        float d = max(0.0, sdLine(rotP, start, end, s));
        
        // Glow contribution (EXACT inverse distance glow)
        vec3 lineGlow = vec3(0.01) / max(d, 0.001);
        lineGlow = 1.0 - exp(-lineGlow * 0.05 * (fract(float(i) * 697.345 + 485.6) + 0.2));
        
        totalGlow += lineGlow;
    }
    
    // Apply intensity and color (EXACT)
    totalGlow *= lineIntensity * intensity;
    vec3 lineColor = mix(secondaryColor, primaryColor, 0.3);
    totalGlow *= lineColor;
    
    float glowAlpha = min(1.0, (totalGlow.r + totalGlow.g + totalGlow.b) / 3.0);
    
    return vec4(totalGlow, glowAlpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCH (from field_visual.fsh:463-499)
// ═══════════════════════════════════════════════════════════════════════════

// Returns: vec4(hitDist, rimAmount/glowAmount, 0, didHit)
vec4 raymarchEnergyOrb(vec3 rayOrigin, vec3 rayDir, float maxDist,
                        vec3 center, float radius, float coronaWidth) {
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = rayOrigin + rayDir * t;
        float d = sdfSphere(p, center, radius);
        
        // Hit surface (EXACT)
        if (d < RAYMARCH_EPSILON) {
            // Calculate rim lighting based on view angle to normal (EXACT)
            vec3 normal = calcEnergyOrbNormal(p, center, radius);
            float rim = 1.0 - abs(dot(normal, -rayDir));
            rim = pow(rim, 2.0);  // Sharpen rim (EXACT)
            
            return vec4(t, rim, 0.0, 1.0);  // Hit!
        }
        
        // Too far (EXACT)
        if (t > maxDist) break;
        
        t += d * 0.8;  // Step forward (0.8 safety factor) (EXACT)
    }
    
    // Check if we're NEAR the sphere even without hitting (for glow/lens) (EXACT)
    float nearestDist = sdfSphere(rayOrigin + rayDir * min(t, maxDist * 0.5), center, radius);
    // coronaWidth controls the lens radius (EXACT)
    float glowWidth = radius * coronaWidth;
    if (nearestDist < glowWidth && nearestDist > 0.0) {
        float glowAmount = 1.0 - (nearestDist / glowWidth);
        return vec4(-1.0, glowAmount * 0.3, 0.0, 0.0);  // No hit, but glow (EXACT)
    }
    
    return vec4(-1.0, 0.0, 0.0, 0.0);  // No hit
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RAYMARCHED RENDER (from field_visual.fsh:505-625)
// This is the composite function that combines all effects
// ═══════════════════════════════════════════════════════════════════════════

// NOTE: This function requires FieldData-style parameters.
// For EnergyOrbConfig usage, see renderEnergyOrbV1Simple below.

vec4 renderEnergyOrbRaymarched(
    // FieldData equivalent parameters
    vec3 fieldCenter, float radius,
    vec3 primaryColor, vec3 secondaryColor, vec3 tertiaryColor,
    float time, float animSpeed, float phase, float intensity,
    float coreSize, float edgeSharpness,
    float spiralDensity, float spiralTwist,
    float glowLineCount, float glowLineIntensity,
    float coronaWidth,
    // Camera/ray parameters
    vec3 camPos, vec3 rayDir, vec3 forward, float sceneDepth,
    // Color mode: 0=multiply, 1=additive, 2=replace
    float colorBlendMode
) {
    // Ensure minimum visibility (EXACT)
    intensity = max(0.5, intensity);
    
    // Distance to target for max raymarch distance (EXACT)
    float distToTarget = length(fieldCenter - camPos);
    float maxDist = distToTarget + radius * 3.0;
    
    // Raymarch to find the orb surface (EXACT)
    vec4 hitInfo = raymarchEnergyOrb(camPos, rayDir, maxDist, fieldCenter, radius, coronaWidth);
    
    if (hitInfo.w > 0.5) {
        // We hit the surface!
        float hitDist = hitInfo.x;
        float rim = hitInfo.y;
        
        // Calculate hit position for effects (EXACT)
        vec3 hitPos = camPos + rayDir * hitDist;
        vec3 localPos = hitPos - fieldCenter;
        float distToCenter = length(localPos);
        
        // Calculate spherical UV for animated patterns (EXACT)
        // Guard against zero-length vector to prevent NaN
        vec3 normLocal = length(localPos) > 0.001 ? normalize(localPos) : vec3(0.0, 1.0, 0.0);
        float theta = atan(normLocal.x, normLocal.z);  // Azimuth (EXACT)
        float phi = asin(clamp(normLocal.y, -1.0, 1.0));  // Elevation (EXACT)
        
        // Animated spiral pattern on sphere surface (EXACT)
        float animTime = time * animSpeed + phase;
        float spiral = sin((theta + phi * spiralTwist) * max(1.0, spiralDensity) + animTime) * 0.5 + 0.5;
        spiral = pow(spiral, 1.5);
        
        // Rim lighting (fresnel-like effect for energy glow) (EXACT)
        float rimGlow = pow(rim, max(0.5, edgeSharpness));
        
        // Core size: project ray to find closest approach to center (EXACT)
        float coreSize_clamped = clamp(coreSize, 0.01, 1.0);
        vec3 toCenter = fieldCenter - camPos;
        float projDist = dot(toCenter, rayDir);
        vec3 closestPoint = camPos + rayDir * max(0.0, projDist);
        float perpDist = length(fieldCenter - closestPoint) / radius;
        float coreGlow = 1.0 - smoothstep(0.0, coreSize_clamped * 2.0, perpDist);
        coreGlow = pow(coreGlow, 2.0);
        
        // For REPLACE mode: use user colors EXACTLY (allow black)
        // For other modes: ensure minimum visibility with fallback
        vec3 coreColor, edgeColor;
        if (colorBlendMode > 1.5 && colorBlendMode < 2.5) {
            // REPLACE: Use colors exactly as provided (allow black)
            coreColor = primaryColor;
            edgeColor = secondaryColor;
        } else {
            // MULTIPLY/ADDITIVE: Fallback to orange if color is black
            coreColor = length(primaryColor) < 0.01 ? vec3(1.0, 0.5, 0.2) : primaryColor;
            edgeColor = length(secondaryColor) < 0.01 ? vec3(1.0, 0.3, 0.1) : secondaryColor;
        }
        
        vec3 spiralColor = mix(edgeColor, tertiaryColor, spiral);
        
        // Glow lines effect (EXACT)
        float lineAngle = theta * max(1.0, glowLineCount);
        float lineGlow = pow(abs(cos(lineAngle + time * 2.0)), 4.0) * glowLineIntensity;
        
        vec3 surfaceColor;
        float alpha;
        
        if (colorBlendMode > 1.5 && colorBlendMode < 2.5) {
            // === REPLACE MODE (2) ===
            // User colors ARE the output - shape determines visibility
            float coreShape = min(1.0, coreGlow * intensity * 2.0);
            float edgeShape = min(1.0, rimGlow * intensity);
            float spiralShape = min(1.0, spiral * 0.8);
            float lineShape = min(1.0, lineGlow);
            
            // Shape-based alpha
            float shapeAlpha = max(coreShape, max(edgeShape, max(spiralShape, lineShape)));
            
            // Build output using user colors with shape blending
            surfaceColor = vec3(0.0);
            surfaceColor = mix(surfaceColor, edgeColor, edgeShape);
            surfaceColor = mix(surfaceColor, tertiaryColor, spiralShape);
            surfaceColor = mix(surfaceColor, coreColor, lineShape * 0.5);
            surfaceColor = mix(surfaceColor, coreColor, coreShape);
            
            alpha = clamp(shapeAlpha * 1.5, 0.0, 0.95);
        } else {
            // === MULTIPLY/ADDITIVE MODE (0, 1, default) ===
            // Original behavior - colors tint procedural base
            float coreInvDist = max(0.01, 1.0 - perpDist);
            vec3 coreEffect = coreColor / (coreInvDist * 3.0 + 0.5);
            coreEffect = 1.0 - exp(-coreEffect * coreSize_clamped * 0.5);
            
            vec3 edgeEffect = edgeColor * rimGlow * 1.5;
            vec3 spiralEffect = spiralColor * spiral * 0.6;
            vec3 lineEffect = coreColor * lineGlow * 0.5;
            
            surfaceColor = coreEffect + edgeEffect + spiralEffect + lineEffect;
            
            // Shape-based alpha (not color-based, for proper opacity control)
            float shapeAlpha = max(coreGlow, max(rimGlow, max(spiral * 0.5, lineGlow * 0.5)));
            alpha = clamp(shapeAlpha * intensity * 1.5, 0.0, 0.95);
        }
        
        // Depth occlusion (EXACT)
        float hitZDepth = hitDist * dot(rayDir, forward);
        if (hitZDepth > sceneDepth) {
            return vec4(0.0);  // Fully occluded
        }
        
        return vec4(surfaceColor * intensity, alpha);
        
    } else if (hitInfo.y > 0.01) {
        // Corona glow (near miss) (EXACT)
        vec3 toOrb = fieldCenter - camPos;
        float orbZDepth = dot(toOrb, forward);
        if (orbZDepth > sceneDepth) {
            return vec4(0.0);  // Corona also occluded
        }
        vec3 glowColor = length(primaryColor) < 0.01 ? vec3(1.0, 0.5, 0.2) : primaryColor;
        float glowAlpha = hitInfo.y * intensity * 0.5;
        return vec4(glowColor * glowAlpha, glowAlpha);
    }
    
    return vec4(0.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// CONVENIENCE WRAPPER using EnergyOrbConfig
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbV1(EnergyOrbConfig cfg, vec3 camPos, vec3 rayDir, vec3 forward, float sceneDepth) {
    return renderEnergyOrbRaymarched(
        cfg.center, cfg.radius,
        cfg.primaryColor.rgb, cfg.secondaryColor.rgb, cfg.tertiaryColor.rgb,
        cfg.time, 1.0, 0.0, cfg.intensity,  // animSpeed=1.0, phase=0.0
        cfg.coreSize, cfg.edgeSharpness,
        cfg.spiralDensity, cfg.spiralTwist,
        cfg.glowLineCount, cfg.glowLineIntensity,
        cfg.coronaWidth,
        camPos, rayDir, forward, sceneDepth,
        0.0  // colorBlendMode: default to MULTIPLY
    );
}

#endif // EFFECTS_ENERGY_ORB_V1_GLSL
