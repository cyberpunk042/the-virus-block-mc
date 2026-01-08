// ═══════════════════════════════════════════════════════════════════════════
// ENERGY ORB V3 - Raymarched 3D Sphere
// ═══════════════════════════════════════════════════════════════════════════
// 
// A TRUE 3D raymarched energy sphere combining:
// - The visual style of the 2D Shadertoy Energy Orb (V2)
// - The raymarching architecture of Panteleymonov Sun (V7)
//
// Source attribution:
// - Visual style: Shadertoy Energy Orb effect
// - Raymarching: Unity SunShader by Panteleymonov Aleksandr (2015-2016)
//
// Key features:
// - Full ray-sphere intersection (not screen-projected)
// - Spherical UV mapping for surface patterns
// - Fresnel-based core and edge glow
// - Animated voronoi patterns on 3D surface
// - Screen-space radial glow lines
// - Full parametrization (~40 controls)
// - Z-depth occlusion with progressive bleed
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef ENERGY_ORB_V3_GLSL
#define ENERGY_ORB_V3_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// RESULT STRUCT
// ═══════════════════════════════════════════════════════════════════════════

struct EnergyOrbV3Result {
    bool didHit;      // True if ray intersects sphere
    vec3 color;       // Final RGB color
    float alpha;      // Transparency (1.0 = opaque)
    float distance;   // Distance to sphere surface (for depth)
};

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
// PHASE 4: Spherical UV Mapping
// ─────────────────────────────────────────────────────────────────────────────
// Converts a 3D surface point to 2D UV coordinates for pattern sampling.
// The UV is in range [0,1] with proper wrap-around for continuous patterns.

vec2 v3_sphericalUV(vec3 surfacePoint) {
    // Normalize to unit sphere
    vec3 n = normalize(surfacePoint);
    
    // Spherical coordinates:
    // phi (azimuth): angle around Y axis, range [-PI, PI]
    // theta (polar): angle from Y axis, range [0, PI]
    float phi = atan(n.z, n.x);                    // Longitude
    float theta = acos(clamp(n.y, -1.0, 1.0));     // Latitude (clamped for safety)
    
    // Convert to UV range [0, 1]
    // u: wraps around equator (0 at -X, 0.5 at +X)
    // v: 0 at north pole, 1 at south pole
    float u = phi / (2.0 * 3.14159265) + 0.5;
    float v = theta / 3.14159265;
    
    return vec2(u, v);
}

// ─────────────────────────────────────────────────────────────────────────────
// PHASE 5: Core Utility Functions (ported from V2)
// ─────────────────────────────────────────────────────────────────────────────

// --- Rotation ---
mat2 v3_rotate(float rad) {
    return mat2(cos(rad), sin(rad), -sin(rad), cos(rad));
}

vec2 v3_rotateUV(vec2 uv, vec2 center, float rad) {
    vec2 delta = uv - center;
    delta = v3_rotate(rad) * delta;
    return delta + center;
}

vec2 v3_twirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    delta = v3_rotate(strength * length(delta)) * delta;
    return delta + center;
}

// --- SDF Functions ---
float v3_circleSDF(vec2 p, float r) {
    return length(p) - r;
}

float v3_lineSDF(vec2 p, vec2 a, vec2 b, float s) {
    vec2 pa = a - p;
    vec2 ba = a - b;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - s;
}

// --- Noise/Random ---
vec2 v3_randomVec2(vec2 seed) {
    mat2 matrix = mat2(vec2(15.27, 47.63), vec2(99.41, 89.98));
    return fract(sin(seed * matrix) * 46839.32);
}

// --- Voronoi Pattern ---
float v3_voronoi(vec2 uv, float cellDensity, float angleOffset) {
    vec2 gridUV = fract(uv * cellDensity);
    vec2 gridID = floor(uv * cellDensity);
    float minDist = 100.0;

    for (float y = -1.0; y <= 1.0; y++) {
        for (float x = -1.0; x <= 1.0; x++) {
            vec2 offset = vec2(x, y);
            vec2 n = v3_randomVec2(gridID + offset);
            vec2 p = offset + vec2(sin(n.x + angleOffset) * 0.5 + 0.5, 
                                   cos(n.y + angleOffset) * 0.5 + 0.5);
            float d = distance(gridUV, p);
            if (d < minDist) minDist = d;
        }
    }

    return minDist;
}

// --- Glow Lines (External Rays) ---
vec3 v3_glowLinesCustom(vec2 uv, float r, float time, float lineCount, float lineIntensity) {
    vec3 glow = vec3(0.0);
    uv = abs(uv);
    float actualLineCount = max(1.0, lineCount);
    float rotationStep = 360.0 / actualLineCount;
    
    for (float i = 1.0; i <= 64.0; i++) {
        if (i > actualLineCount) break;
        
        float offs = sin(time * 15.0 + fract(i * 123.456 + 867.234) * 5.0) * 0.01;
        float h = r + fract(i * 345.67 + 12.35) * 0.1;
        float s = fract(i * 342.968 + 123.467) * 0.5 * 0.001;
        float d = max(0.0, v3_lineSDF(uv, vec2(0.0, r + offs), vec2(0.0, h + offs), s));
        vec3 line = vec3(0.01) / max(d, 0.001);
        line = 1.0 - exp(-line * 0.05 * (fract(i * 697.345 + 485.6) + 0.2));
        glow += line * lineIntensity;
        uv = v3_rotate(radians(rotationStep * fract(i * 45.29))) * uv;
    }
    return glow;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════
// Full parametrization matching V2 + 3D-specific additions

EnergyOrbV3Result renderEnergyOrbV3(
    // ─── Ray & Position (from camera system) ───
    vec3 rayOrigin,           // Camera position
    vec3 rayDir,              // Normalized ray direction
    vec3 forward,             // Camera forward (for Z-depth comparison)
    float maxDist,            // Scene depth for occlusion
    vec3 sphereCenter,        // World position of sphere
    float sphereRadius,       // Base radius
    
    // ─── Time ───
    float time,               // Animated time (pre-multiplied by animSpeed)
    
    // ─── Colors (3 total) ───
    vec3 coreColor,           // Core/center color
    vec3 edgeColor,           // Edge ring color
    vec3 linesColor,          // Voronoi pattern color
    
    // ─── General ───
    float intensity,          // Overall brightness (0-5, default 1.0)
    
    // ─── Core (6 params) ───
    float coreSize,           // Core radius (0.01-3, default 0.5)
    float coreSpread,         // Core falloff rate (0.1-10, default 1.0)
    float coreGlow,           // Core brightness (0-5, default 1.0)
    float coreRadiusScale,    // Core radius multiplier (0.1-2, default 0.35)
    float coreMaskRadius,     // Core visibility mask (0.1-1, default 0.35)
    float coreMaskSoft,       // Mask edge softness (0.001-0.5, default 0.05)
    
    // ─── Corona (5 params) ───
    float showCorona,         // Toggle (0 or 1)
    float coronaWidth,        // Corona size (0.01-3, default 0.5)
    float coronaPower,        // Falloff exponent (0.1-10, default 2.0)
    float coronaStart,        // Start distance (0-1, default 0.15)
    float coronaBrightness,   // Corona intensity (0-3, default 1.0)
    
    // ─── Edge Ring (6 params) ───
    float edgeSharpness,      // Ring sharpness (0.5-20, default 4.0)
    float ringPower,          // Ring intensity (0-20, default 1.0)
    float edgeRadius,         // Ring radius (0.05-1, default 0.3)
    float edgeSpread,         // Ring falloff (0.1-5, default 1.0)
    float edgeGlow,           // Ring brightness (0-5, default 1.0)
    float sharpScale,         // Sharpness scale (0.5-20, default 4.0)
    
    // ─── Inner Lines / Voronoi (9 params) ───
    float linesIntensity,     // Pattern opacity (0-3, default 1.0)
    float spiralDensity,      // Cell count (1-50, default 5.0)
    float spiralTwist,        // Twist amount (0-20, default 5.0)
    float linesUVScale,       // UV multiplier (0.5-10, default 3.0)
    float linesDensityMult,   // Layer 2 density (0.5-5, default 1.6)
    float linesContrast1,     // Layer 1 contrast (0.5-10, default 2.5)
    float linesContrast2,     // Layer 2 contrast (0.5-10, default 3.0)
    float linesMaskRadius,    // Pattern mask (0.1-1, default 0.3)
    float linesMaskSoft,      // Mask softness (0.001-0.2, default 0.02)
    
    // ─── External Rays (6 params) ───
    float showExternalRays,   // Toggle (0 or 1)
    float rayPower,           // Ray intensity (0-20, default 2.0)
    float raySharpness,       // Ray sharpness (0.01-20, default 1.0)
    float rayCount,           // Number of rays (4-64, default 16)
    float rayRotSpeed,        // Rotation speed (0-2, default 0.3)
    float rayStartRadius,     // Ray start distance (0.1-1, default 0.32)
    
    // ─── Output ───
    float alphaScale,         // Alpha multiplier (0-1, default 1.0)
    float colorBlendMode      // 0=multiply, 1=additive, 2=replace
) {
    EnergyOrbV3Result result;
    result.didHit = false;
    result.color = vec3(0.0);
    result.alpha = 0.0;
    result.distance = maxDist;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 2: Ray-Sphere Intersection
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from Panteleymonov Sun (pulsar_v7.glsl)
    
    // Position relative to camera
    vec3 pos = sphereCenter - rayOrigin;
    vec3 ray = rayDir;
    
    // Ray projection onto sphere-to-camera vector
    float rayProj = dot(ray, pos);
    
    // Squared distance to sphere center
    float sqDist = dot(pos, pos);
    
    // Perpendicular distance squared (closest point on ray to center)
    float sphere = sqDist - rayProj * rayProj;
    
    // Radius squared
    float sqRadius = sphereRadius * sphereRadius;
    
    // Behind camera check
    if (rayProj <= 0.0) sphere = sqRadius;
    
    // Projection point (closest point on ray to sphere center)
    vec3 pr = ray * abs(rayProj) - pos;
    
    // Surface point calculation
    vec3 surface = vec3(0.0);
    
    if (sqDist <= sqRadius) {
        // Camera is INSIDE sphere - don't render effect over player
        return result;
    } else if (sphere < sqRadius) {
        // Ray hits sphere surface
        float l1 = sqrt(sqRadius - sphere);
        surface = pr - ray * l1;  // Surface intersection point (camera-relative)
        result.didHit = true;
        result.distance = rayProj - l1;
    } else {
        // Ray misses sphere - surface stays at origin
        // Effect will still render glow/rays around sphere
        surface = vec3(0.0);
    }
    
    // Store intermediate values for later phases
    // (These will be used by glow, edge, pattern calculations)
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 3: Z-Depth Occlusion
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from Panteleymonov Sun (pulsar_v7.glsl)
    // Uses Z-depth (not Euclidean distance) for proper depth buffer comparison
    
    float distCamToOrb = sqrt(sqDist);
    
    // Use hit distance for surface, cam-to-orb distance for glow
    float orbDist = result.didHit ? result.distance : distCamToOrb;
    
    // Convert to Z-depth (project along camera forward)
    // This is the critical fix that makes occlusion work correctly
    float orbZDepth = orbDist * dot(rayDir, forward);
    
    // Is orb behind scene geometry?
    bool orbBehindScene = (orbZDepth > maxDist);
    
    // Calculate visibility with progressive bleed
    float effectVisibility = 1.0;
    if (orbBehindScene) {
        // Distance from scene to orb in Z-depth space
        float distSceneToOrb = orbZDepth - maxDist;
        distSceneToOrb = max(10.0, distSceneToOrb);  // Floor at 10 blocks
        
        // Linear falloff over 1000 blocks
        float bleedRange = 1000.0;
        float occlusionBleed = max(0.0, 1.0 - (distSceneToOrb / bleedRange));
        
        // Cap at 0.9 so player is always visible
        effectVisibility = min(occlusionBleed, 0.9);
    }
    
    // Early exit if fully occluded
    if (effectVisibility < 0.01) {
        return result;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 4: Spherical UV Mapping
    // ═══════════════════════════════════════════════════════════════════════
    // Convert 3D surface point to 2D UV for pattern sampling
    
    vec2 sphereUV = vec2(0.0);
    if (result.didHit) {
        sphereUV = v3_sphericalUV(surface);
    }
    
    // Also compute a "view UV" for screen-space effects
    // This is the projection of the sphere onto the screen (for rays/corona)
    float c = length(pr);  // Distance from center in view space
    vec2 viewUV = vec2(0.0, 1.0);  // Default to pointing up if at center
    if (c > 0.001) {
        // Normalize pr to get direction
        viewUV = pr.xy / c;  // 2D projection in view space (normalized)
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 6: Core Glow
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from V2: Bright center with inverse distance falloff
    // In 3D: Uses view-space distance from sphere center
    
    // Normalize view distance to match V2's UV-based approach
    // V2 uses coreSize ~0.05, so we scale c to similar range
    float normalizedDist = c / (sphereRadius * 2.0);  // c is view-space dist
    
    // Core glow - only computed within sphere influence area
    vec3 coreBase = vec3(0.0);
    float coreBaseLum = 0.0;
    float coreMask = 0.0;
    
    float coreInfluenceRange = coreMaskRadius * 2.0;  // Core visible within 2x mask radius
    if (normalizedDist < coreInfluenceRange) {
        // Core glow (inverse distance from center)
        float coreRadius = max(0.02, coreSize * coreRadiusScale);
        float coreDist = max(0.01, normalizedDist - coreRadius * 0.1);  // Floor at 0.01
        
        // Core falloff formula from V2
        coreBase = vec3(0.2, 0.05, 0.1) * coreSpread / coreDist;
        coreBase = 1.0 - exp(-coreBase * vec3(0.03, 0.2, 0.18) * coreGlow);
        
        // Save base luminance before color multiply (for blend modes)
        coreBaseLum = dot(coreBase, vec3(0.299, 0.587, 0.114));
        
        // Core mask - fade out at edges
        coreMask = smoothstep(coreMaskRadius, coreMaskRadius - coreMaskSoft, normalizedDist);
    }
    
    // Apply color and mask
    vec3 core = coreBase * coreColor * coreMask * effectVisibility;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 7: Edge Ring (Fresnel Rim Lighting)
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from V2: Glowing ring at sphere edge
    // In 3D: Uses distance from edge radius in normalized view space
    
    // Edge ring - glows at specific radius from center
    float edgeSDF = abs(normalizedDist - edgeRadius);
    
    // CRITICAL FIX: Only compute edge when ray is near the sphere
    // Otherwise edge effect saturates and creates black/white everywhere
    vec3 edgeBase = vec3(0.0);
    float edgeBaseLum = 0.0;
    
    // Only render edge effect within a reasonable range of the edge radius
    float edgeInfluenceRange = edgeRadius * 2.0;  // Edge only visible within 2x radius
    if (normalizedDist < edgeInfluenceRange) {
        // Edge falloff formula from V2
        edgeBase = vec3(0.2, 0.05, 0.1) * edgeSpread / max(edgeSDF, 0.01);  // Floor at 0.01 instead of 0.001
        float sharpFactor = edgeSharpness / sharpScale;
        edgeBase = 1.0 - exp(-edgeBase * vec3(0.05, 0.2, 0.5) * sharpFactor * edgeGlow);
        
        // Fade out edge at distance
        float edgeFalloff = 1.0 - smoothstep(0.0, edgeInfluenceRange, normalizedDist);
        edgeBase *= edgeFalloff;
        
        // Save base luminance before color multiply (for blend modes)
        edgeBaseLum = dot(edgeBase, vec3(0.299, 0.587, 0.114));
    }
    
    // Apply color and intensity
    vec3 edge = edgeBase * edgeColor * ringPower * effectVisibility;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 8: Voronoi Pattern on Sphere Surface
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from V2: Two-layer voronoi with twirl animation
    // In 3D: Uses spherical UV from surface point
    
    float lines = 0.0;
    
    if (result.didHit && linesIntensity > 0.01) {
        // Scale UV for pattern density
        vec2 patternUV = sphereUV * linesUVScale;
        
        // Layer 1: Rotating and twirling voronoi
        vec2 st1 = v3_twirlUV(v3_rotateUV(patternUV, vec2(0.5), -time), vec2(0.5), spiralTwist);
        float layer1 = pow(v3_voronoi(st1, spiralDensity, time * 0.5), linesContrast1);
        
        // Layer 2: Opposite rotation, higher density
        vec2 st2 = v3_twirlUV(v3_rotateUV(patternUV, vec2(0.5), time), vec2(0.5), -spiralTwist);
        float layer2 = pow(v3_voronoi(st2, spiralDensity * linesDensityMult, time * 0.5), linesContrast2);
        
        // Combine layers
        lines = layer1 + layer2;
        
        // Mask to sphere visible area
        float linesMask = smoothstep(linesMaskRadius, linesMaskRadius - linesMaskSoft, normalizedDist);
        lines *= linesMask;
    }
    
    // Final lines contribution
    vec3 linesContrib = lines * linesColor * linesIntensity * effectVisibility;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 9: External Rays (Screen-Space Radial Lines)
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from V2: Radial glow lines emanating from sphere center
    // In 3D: Uses view-space projection (viewUV, normalized distance)
    
    vec3 glowLines = vec3(0.0);
    
    if (showExternalRays > 0.5 && rayPower > 0.01) {
        // Use normalized view-space UV for ray calculation
        // Scale to match V2's expected coordinate range
        vec2 rayUV = viewUV * normalizedDist * 3.0;
        
        // Rotate rays over time
        rayUV = v3_rotate(time * rayRotSpeed) * rayUV;
        
        // Generate glow lines
        vec3 rawGlow = v3_glowLinesCustom(rayUV, rayStartRadius, time, rayCount, 1.0);
        glowLines = rawGlow * rayPower;
        
        // Apply sharpness control
        if (raySharpness != 1.0) {
            glowLines = pow(max(glowLines, vec3(0.001)), vec3(1.0 / max(0.1, raySharpness)));
        }
        
        glowLines *= effectVisibility;
    }
    
    // Ray luminance for alpha calculation
    float rayLum = dot(glowLines, vec3(0.299, 0.587, 0.114));
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 10: Corona Background (Optional Lens Fill)
    // ═══════════════════════════════════════════════════════════════════════
    // Adapted from V2: Background vignette/fill around sphere
    // Can be turned off for transparent overlay style
    
    vec3 corona = vec3(0.0);
    
    if (showCorona > 0.5 && coronaBrightness > 0.001) {
        // Corona falloff based on distance from edge
        float falloffScale = 0.5 / max(0.1, coronaWidth);
        float vignette = 1.0 - pow(max(0.0, normalizedDist - coronaStart), coronaPower + 3.0) * falloffScale;
        vignette = max(0.0, vignette);
        
        // Apply edge color tint to corona
        corona = vignette * vec3(0.1, 0.02, 0.01) * edgeColor * coronaBrightness * effectVisibility;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PHASE 11: Color Blend Modes & Final Composite
    // ═══════════════════════════════════════════════════════════════════════
    // CRITICAL FIX: Only output effect where it actually exists
    // The issue was that shapeAlpha was being used as mix factor, causing 
    // dark colors to create black backgrounds
    
    vec3 col = vec3(0.0);
    float alpha = 0.0;
    
    // Lines luminance for alpha - only count if hit the sphere
    float lineLum = result.didHit ? lines * linesIntensity : 0.0;
    
    // Calculate visibility based on actual rendered components
    float coronaLum = dot(corona, vec3(0.299, 0.587, 0.114));
    
    if (colorBlendMode > 1.5 && colorBlendMode < 2.5) {
        // === REPLACE MODE (2) ===
        // User colors control output, but ONLY where effect exists
        // Don't mix to colors where there's no effect!
        
        float coreShape = coreBaseLum * coreMask;
        float edgeShape = edgeBaseLum * ringPower;
        float lineShape = lineLum;
        float rayShape = rayLum;
        
        // Total visibility - this determines alpha, NOT mix factor
        float totalVisibility = coreShape + edgeShape + lineShape + rayShape + coronaLum;
        
        if (totalVisibility > 0.001) {
            // Weight colors by their contribution
            col = vec3(0.0);
            col += coreColor * coreShape;
            col += edgeColor * (edgeShape + rayShape);
            col += linesColor * lineShape;
            col += corona;
            
            // Normalize by total contribution (avoid over-saturation)
            float totalWeight = coreShape + edgeShape + rayShape + lineShape + coronaLum;
            if (totalWeight > 1.0) {
                col /= totalWeight;
            }
        }
        
        alpha = clamp(totalVisibility * alphaScale, 0.0, 1.0);
        
    } else if (colorBlendMode < 0.5) {
        // === MULTIPLY MODE (0, default) ===
        // Standard additive glow - colors tint the procedural base
        
        col = corona;
        col += edge;
        col += core;
        col += linesContrib;
        col += col * glowLines;
        
        // Alpha based on color brightness
        alpha = clamp(length(col) * alphaScale, 0.0, 1.0);
        
    } else {
        // === ADDITIVE MODE (1) ===
        // Colors add extra glow to procedural base
        
        col = corona;
        col += edge;
        col += core;
        col += linesContrib;
        col += col * glowLines;
        
        // Add user colors weighted by shape visibility
        col += coreColor * coreBaseLum * coreMask * 0.5;
        col += edgeColor * edgeBaseLum * ringPower * 0.5;
        col += linesColor * lineLum * 0.3;
        
        // Alpha based on color brightness
        alpha = clamp(length(col) * alphaScale, 0.0, 1.0);
    }
    
    // Apply overall intensity
    col *= intensity;
    
    // Don't clamp - let HDR pass through for tone mapping
    // col = clamp(col, 0.0, 1.0);  // Removed!
    
    // Set result
    result.color = col;
    result.alpha = alpha * effectVisibility;
    
    return result;
}

#endif // ENERGY_ORB_V3_GLSL
