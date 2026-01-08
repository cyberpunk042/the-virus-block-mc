// ═══════════════════════════════════════════════════════════════════════════
// ENERGY ORB V2 - Direct port from Shadertoy
// Source: Shadertoy Energy Orb effect
// This is an EXACT reproduction - no deviations from the original.
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// ROTATION UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

mat2 orbRotate(float rad) {
    return mat2(cos(rad), sin(rad), -sin(rad), cos(rad));
}

vec2 orbRotateUV(vec2 uv, vec2 center, float rad) {
    vec2 delta = uv - center;
    delta = orbRotate(rad) * delta;
    return delta + center;
}

vec2 orbTwirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    delta = orbRotate(strength * length(delta)) * delta;
    return delta + center;
}

// ═══════════════════════════════════════════════════════════════════════════
// SDF FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

float orbCircleSDF(vec2 p, float r) {
    return length(p) - r;
}

float orbLineSDF(vec2 p, vec2 a, vec2 b, float s) {
    vec2 pa = a - p;
    vec2 ba = a - b;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0., 1.);
    return length(pa - ba * h) - s;
}

// ═══════════════════════════════════════════════════════════════════════════
// VORONOI PATTERN
// ═══════════════════════════════════════════════════════════════════════════

vec2 orbRandomVec2(vec2 seed) {
    mat2 matrix = mat2(vec2(15.27, 47.63), vec2(99.41, 89.98));
    return fract(sin(seed * matrix) * 46839.32);
}

float orbVoronoi(vec2 uv, float cellDensity, float angleOffset) {
    vec2 gridUV = fract(uv * cellDensity);
    vec2 gridID = floor(uv * cellDensity);
    float minDist = 100.;

    for (float y = -1.; y <= 1.; y++) {
        for (float x = -1.; x <= 1.; x++) {
            vec2 offset = vec2(x, y);
            vec2 n = orbRandomVec2(gridID + offset);
            vec2 p = offset + vec2(sin(n.x + angleOffset) * .5 + .5, cos(n.y + angleOffset) * .5 + .5);
            float d = distance(gridUV, p);
            if (d < minDist) minDist = d;
        }
    }

    return minDist;
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW LINES (rotating energy spikes)
// ═══════════════════════════════════════════════════════════════════════════

vec3 orbGlowLines(vec2 uv, float r, float time) {
    vec3 glow = vec3(0.0);
    uv = abs(uv);
    for (float i = 1.; i <= 16.; i++) {
        float offs = sin(time * 15. + fract(i * 123.456 + 867.234) * 5.) * .01;
        float h = r + fract(i * 345.67 + 12.35) * .1;
        float s = fract(i * 342.968 + 123.467) * .5 * .001;
        float d = max(0., orbLineSDF(uv, vec2(0, r + offs), vec2(0, h + offs), s));
        vec3 line = vec3(.01) / d;
        line = 1. - exp(-line * .05 * (fract(i * 697.345 + 485.6) + .2));
        glow += line;
        uv = orbRotate(radians(20. * fract(i * 45.29))) * uv;
    }
    return glow;
}

// Parameterized version with configurable line count and intensity
vec3 orbGlowLinesCustom(vec2 uv, float r, float time, float lineCount, float lineIntensity) {
    vec3 glow = vec3(0.0);
    uv = abs(uv);
    float actualLineCount = max(1.0, lineCount);
    float rotationStep = 360.0 / actualLineCount;
    
    for (float i = 1.0; i <= 32.0; i++) {
        if (i > actualLineCount) break;
        
        float offs = sin(time * 15.0 + fract(i * 123.456 + 867.234) * 5.0) * 0.01;
        float h = r + fract(i * 345.67 + 12.35) * 0.1;
        float s = fract(i * 342.968 + 123.467) * 0.5 * 0.001;
        float d = max(0.0, orbLineSDF(uv, vec2(0.0, r + offs), vec2(0.0, h + offs), s));
        vec3 line = vec3(0.01) / d;
        line = 1.0 - exp(-line * 0.05 * (fract(i * 697.345 + 485.6) + 0.2));
        glow += line * lineIntensity;
        uv = orbRotate(radians(rotationStep * fract(i * 45.29))) * uv;
    }
    return glow;
}

// ═══════════════════════════════════════════════════════════════════════════
// BACKGROUND (vignette)
// ═══════════════════════════════════════════════════════════════════════════

vec3 orbBackground(vec2 uv, float aspectRatio) {
    uv.y *= aspectRatio;
    float vignette = 1. - pow(max(0., length(uv) - .15), 5.) * .5;
    return vignette * vec3(.1, .02, .01);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN ENERGY ORB V2 RENDER
// Exact reproduction of the Shadertoy mainImage function
// 
// Parameters:
//   uv: Screen-space UV centered on orb (range roughly -0.5 to 0.5)
//   time: Animation time in seconds
//   aspectRatio: Screen aspect ratio for proper circle rendering
// 
// Returns: vec4(color, alpha)
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbV2(vec2 uv, float time, float aspectRatio) {
    // Background with vignette
    vec3 col = orbBackground(uv, aspectRatio);
    
    // === CORE GLOW ===
    // Bright center with inverse distance falloff
    vec3 core = max(vec3(0), vec3(orbCircleSDF(uv, .05)));
    core = vec3(.2, .05, .1) / core;  // spread
    core = 1. - exp(-core * vec3(.03, .2, .18));  // intensity
    
    // === EDGE GLOW ===
    // Glowing ring at radius 0.3
    vec3 edge = vec3(abs(orbCircleSDF(uv, .3)));
    edge = vec3(.2, .05, .1) / edge;
    edge = 1. - exp(-edge * vec3(.05, .2, .5));
    
    // === VORONOI SPIRAL LINES ===
    float speed = time * 5.;
    
    // First layer: rotating and twirling
    vec2 st = orbTwirlUV(orbRotateUV(uv * 3., vec2(0), -speed), vec2(0), 5.);
    float lines = pow(orbVoronoi(st, 5., 0.), 2.5);
    
    // Second layer: opposite rotation
    st = orbTwirlUV(orbRotateUV(uv * 3., vec2(0), speed), vec2(0), -5.);
    lines += pow(orbVoronoi(st, 8., 0.), 3.);
    
    // Mask to circle boundary
    lines *= smoothstep(.0, -.02, orbCircleSDF(uv, .3));
    
    // === COMPOSITE ===
    col += edge;
    col += core;
    col += lines * vec3(.1, .4, .8);  // Blue voronoi lines
    col += col * orbGlowLines(orbRotate(time * .3) * uv, .32, time);
    
    // Alpha based on combined glow intensity
    float alpha = clamp(length(col) * 0.5, 0.0, 1.0);
    
    return vec4(col, alpha);
}

// ═══════════════════════════════════════════════════════════════════════════
// WRAPPER FOR 3D SPHERE PROJECTION
// 
// This projects the 2D Shadertoy effect onto a 3D sphere location.
// The effect is rendered in screen-space, centered on the sphere's
// projected screen position.
//
// Parameters:
//   texCoord: Current fragment's screen UV (0-1)
//   sphereCenter: World-space center of the sphere
//   sphereRadius: World-space radius
//   camPos: Camera position
//   camForward: Camera forward direction
//   camUp: Camera up direction  
//   fov: Field of view in radians
//   aspectRatio: Screen aspect ratio
//   time: Animation time
//
// Returns: vec4(color, alpha) or vec4(0) if outside sphere projection
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbV2Projected(
    vec2 texCoord,
    vec3 sphereCenter,
    float sphereRadius,
    vec3 camPos,
    vec3 camForward,
    vec3 camUp,
    float fov,
    float aspectRatio,
    float time
) {
    // Calculate sphere center in view space
    vec3 toSphere = sphereCenter - camPos;
    float distToSphere = length(toSphere);
    
    // Check if sphere is behind camera
    if (dot(toSphere, camForward) < 0.0) {
        return vec4(0.0);
    }
    
    // Project sphere center to screen
    vec3 camRight = normalize(cross(camForward, camUp));
    vec3 camUpOrtho = cross(camRight, camForward);
    
    // View-space position of sphere center
    float viewX = dot(toSphere, camRight);
    float viewY = dot(toSphere, camUpOrtho);
    float viewZ = dot(toSphere, camForward);
    
    // Perspective projection
    float tanHalfFov = tan(fov * 0.5);
    float screenX = (viewX / viewZ) / (tanHalfFov * aspectRatio);
    float screenY = (viewY / viewZ) / tanHalfFov;
    
    // Screen center of sphere (in -1 to 1 range)
    vec2 sphereScreenCenter = vec2(screenX, screenY);
    
    // Calculate apparent size of sphere on screen
    float apparentRadius = (sphereRadius / viewZ) / tanHalfFov;
    
    // Transform fragment UV to sphere-relative UV
    // texCoord is 0-1, convert to -1 to 1
    vec2 fragNDC = texCoord * 2.0 - 1.0;
    
    // Offset from sphere center
    vec2 relativeUV = fragNDC - sphereScreenCenter;
    
    // Scale to match the Shadertoy's expected UV range (radius 0.3 = edge)
    // The Shadertoy uses radius 0.3 for the main circle
    float scale = 0.3 / apparentRadius;
    vec2 orbUV = relativeUV * scale;
    
    // Check if we're within the effect's relevant range
    float dist = length(orbUV);
    if (dist > 0.6) {  // Some margin beyond the 0.3 edge for glow
        return vec4(0.0);
    }
    
    // Render the Shadertoy effect
    return renderEnergyOrbV2(orbUV, time, 1.0);  // aspectRatio already handled by projection
}

// ═══════════════════════════════════════════════════════════════════════════
// PARAMETERIZED VERSION - Customizable colors and parameters
// ═══════════════════════════════════════════════════════════════════════════

vec4 renderEnergyOrbV2Custom(
    vec2 uv, 
    float time, 
    vec3 coreColor,
    vec3 edgeColor,
    vec3 linesColor,
    float intensity,
    float coreSize,
    float edgeSharpness,
    float spiralDensity,
    float spiralTwist,
    float glowLineCount,
    float glowLineIntensity,
    float showExternalRays,
    float showCorona,
    float coronaWidth,
    float coronaPower,
    float rayPower,
    float raySharpness,
    float ringPower,
    // V2 Detail parameters
    float coronaStart,
    float coronaBrightness,
    float coreRadiusScale,
    float coreMaskRadius,
    float coreSpread,
    float coreGlow,
    float coreMaskSoft,
    float edgeRadius,
    float edgeSpread,
    float edgeGlow,
    float sharpScale,
    float linesUVScale,
    float linesDensityMult,
    float linesContrast1,
    float linesContrast2,
    float linesMaskRadius,
    float linesMaskSoft,
    float rayRotSpeed,
    float rayStartRadius,
    float alphaScale,
    float colorBlendMode  // NEW: 0=multiply, 1=additive, 2=replace, 3=mix
) {
    // === CORONA / BACKGROUND FILL ===
    // showCorona=false OR coronaBrightness=0 hides background completely
    // This is the "lens fill" that V1 doesn't have - can be turned off
    vec3 col = vec3(0.0);
    if (showCorona > 0.5 && coronaBrightness > 0.001) {
        float falloffScale = 0.5 / max(0.1, coronaWidth);
        float vignette = 1.0 - pow(max(0.0, length(uv) - coronaStart), coronaPower + 3.0) * falloffScale;
        vignette = max(0.0, vignette);
        col = vignette * vec3(0.1, 0.02, 0.01) * edgeColor * coronaBrightness;
    }
    
    // === CORE GLOW ===
    // coreColor multiplies the formula result (white = original look)
    float coreRadius = max(0.02, coreSize * coreRadiusScale);
    vec3 coreBase = max(vec3(0.0), vec3(orbCircleSDF(uv, coreRadius)));
    coreBase = vec3(0.2, 0.05, 0.1) * coreSpread / coreBase;
    coreBase = 1.0 - exp(-coreBase * vec3(0.03, 0.2, 0.18) * coreGlow);
    float coreBaseLum = dot(coreBase, vec3(0.299, 0.587, 0.114));  // Save base luminance BEFORE color multiply
    float coreMask = smoothstep(0.0, -coreMaskSoft, orbCircleSDF(uv, coreMaskRadius));
    vec3 core = coreBase * coreColor * coreMask;  // Color control - white = no change
    
    // === EDGE RING ===
    // edgeColor multiplies the formula result, ringPower scales intensity
    vec3 edgeBase = vec3(abs(orbCircleSDF(uv, edgeRadius)));
    edgeBase = vec3(0.2, 0.05, 0.1) * edgeSpread / edgeBase;
    float sharpFactor = edgeSharpness / sharpScale;
    edgeBase = 1.0 - exp(-edgeBase * vec3(0.05, 0.2, 0.5) * sharpFactor * edgeGlow);
    float edgeBaseLum = dot(edgeBase, vec3(0.299, 0.587, 0.114));  // Save base luminance BEFORE color multiply
    vec3 edge = edgeBase * edgeColor * ringPower;  // Color control + intensity
    
    // === VORONOI SPIRAL LINES ===
    float speed = time;
    vec2 st = orbTwirlUV(orbRotateUV(uv * linesUVScale, vec2(0.0), -speed), vec2(0.0), spiralTwist);
    float lines = pow(orbVoronoi(st, spiralDensity, 0.0), linesContrast1);
    st = orbTwirlUV(orbRotateUV(uv * linesUVScale, vec2(0.0), speed), vec2(0.0), -spiralTwist);
    lines += pow(orbVoronoi(st, spiralDensity * linesDensityMult, 0.0), linesContrast2);
    lines *= smoothstep(0.0, -linesMaskSoft, orbCircleSDF(uv, linesMaskRadius));
    
    // === GLOW LINES / EXTERNAL RAYS ===
    vec3 glowLines = vec3(0.0);
    if (showExternalRays > 0.5) {
        vec3 rawGlow = orbGlowLinesCustom(orbRotate(time * rayRotSpeed) * uv, rayStartRadius, time, glowLineCount, 1.0);
        glowLines = rawGlow * rayPower;
        if (raySharpness != 1.0) {
            glowLines = pow(max(glowLines, vec3(0.001)), vec3(1.0 / max(0.1, raySharpness)));
        }
    }
    
    // === COMPOSITE ===
    // For REPLACE mode (2): Use user colors directly, shape provides alpha
    // For MULTIPLY mode (0, default): Original behavior - colors tint the base formula
    
    float alpha;
    
    if (colorBlendMode > 1.5 && colorBlendMode < 2.5) {
        // === REPLACE MODE (2) ===
        // User colors ARE the output colors. Shape provides visibility/alpha.
        // This allows the full RGB spectrum including black.
        
        // Use BASE luminances (saved before color multiplication) for shape
        float lineLum = lines * glowLineIntensity;
        
        // Power controls: intensity affects core, ringPower affects edge, glowLineIntensity affects lines
        // Boost factors to make shapes more visible/opaque
        float coreShape = min(1.0, coreBaseLum * coreMask * 3.0);  
        float edgeShape = min(1.0, edgeBaseLum * ringPower * 2.0);             // ringPower controls edge
        float lineShape = min(1.0, lineLum * 1.5);                              // glowLineIntensity controls lines
        
        // External rays (glowLines) - calculate their shape for REPLACE mode
        float rayLum = dot(glowLines, vec3(0.299, 0.587, 0.114));
        float rayShape = min(1.0, rayLum * rayPower);
        
        // Shape-based alpha (max visibility from any part) - include rays
        float shapeAlpha = max(coreShape, max(edgeShape, max(lineShape, rayShape)));
        
        // Build output using user colors with proper layering
        // Order: corona (background) -> rays -> edge -> lines -> core (on top)
        // This way core is on top and won't be hidden by lines
        
        // Start with corona (already in col)
        
        // External rays - blend toward edge color (rays are colored by edge)
        col = mix(col, edgeColor, rayShape);
        
        // Edge ring
        col = mix(col, edgeColor, edgeShape);
        
        // Lines (voronoi pattern)
        col = mix(col, linesColor, lineShape);
        
        // Core (on TOP so it's always visible)
        col = mix(col, coreColor, coreShape);
        
        alpha = clamp(shapeAlpha * alphaScale, 0.0, 1.0);
    } else if (colorBlendMode < 0.5) {
        // === MULTIPLY MODE (0, default) ===
        // Colors MULTIPLY the procedural base - white=original, dark colors=darken
        col += edge;
        col += core;
        col += lines * linesColor * glowLineIntensity;
        col += col * glowLines;
        
        // Shape-based alpha for proper opacity control
        float lineLum = lines * glowLineIntensity;
        float rayLum = dot(glowLines, vec3(0.299, 0.587, 0.114));
        float shapeAlpha = max(coreBaseLum * coreMask, 
                              max(edgeBaseLum * ringPower, 
                                 max(lineLum, rayLum * rayPower)));
        alpha = clamp(shapeAlpha * alphaScale, 0.0, 1.0);
    } else {
        // === ADDITIVE MODE (1) ===
        // User colors ADD to the base - black=original, bright colors=add glow
        // This is the classic additive glow effect
        
        // Start with base procedural shapes
        col += edge;
        col += core;
        col += lines * linesColor * glowLineIntensity;
        col += col * glowLines;
        
        // Then ADD user colors weighted by shape visibility
        float lineLum = lines * glowLineIntensity;
        float rayLum = dot(glowLines, vec3(0.299, 0.587, 0.114));
        
        // Add user colors on top (additive glow)
        col += coreColor * coreBaseLum * coreMask * 0.5;
        col += edgeColor * edgeBaseLum * ringPower * 0.5;
        col += linesColor * lineLum * 0.3;
        
        float shapeAlpha = max(coreBaseLum * coreMask, 
                              max(edgeBaseLum * ringPower, 
                                 max(lineLum, rayLum * rayPower)));
        alpha = clamp(shapeAlpha * alphaScale, 0.0, 1.0);
    }
    
    // Apply overall intensity to final output (like V1)
    col *= intensity;
    
    return vec4(col, alpha);
}

vec4 renderEnergyOrbV2ProjectedCustom(
    vec2 texCoord,
    vec3 sphereCenter,
    float sphereRadius,
    vec3 camPos,
    vec3 camForward,
    vec3 camUp,
    float fov,
    float aspectRatio,
    float time,
    vec3 coreColor,
    vec3 edgeColor,
    vec3 linesColor,
    float intensity,
    float coreSize,
    float edgeSharpness,
    float spiralDensity,
    float spiralTwist,
    float glowLineCount,
    float glowLineIntensity,
    float showExternalRays,
    float showCorona,
    float coronaWidth,
    float coronaPower,
    float rayPower,
    float raySharpness,
    float ringPower,
    // V2 Detail parameters
    float coronaStart,
    float coronaBrightness,
    float coreRadiusScale,
    float coreMaskRadius,
    float coreSpread,
    float coreGlow,
    float coreMaskSoft,
    float edgeRadius,
    float edgeSpread,
    float edgeGlow,
    float sharpScale,
    float linesUVScale,
    float linesDensityMult,
    float linesContrast1,
    float linesContrast2,
    float linesMaskRadius,
    float linesMaskSoft,
    float rayRotSpeed,
    float rayStartRadius,
    float alphaScale,
    float sceneDepth,
    float colorBlendMode  // NEW: 0=multiply, 1=additive, 2=replace, 3=mix
) {
    vec3 toSphere = sphereCenter - camPos;
    
    if (dot(toSphere, camForward) < 0.0) {
        return vec4(0.0);
    }
    
    vec3 camRight = normalize(cross(camForward, camUp));
    vec3 camUpOrtho = cross(camRight, camForward);
    
    float viewX = dot(toSphere, camRight);
    float viewY = dot(toSphere, camUpOrtho);
    float viewZ = dot(toSphere, camForward);
    
    // Per-pixel depth occlusion
    float distToOrbSurface = viewZ - sphereRadius;
    if (sceneDepth > 0.0 && sceneDepth < distToOrbSurface) {
        return vec4(0.0);
    }
    
    float tanHalfFov = tan(fov * 0.5);
    float screenX = (viewX / viewZ) / (tanHalfFov * aspectRatio);
    float screenY = (viewY / viewZ) / tanHalfFov;
    vec2 sphereScreenCenter = vec2(screenX, screenY);
    
    float apparentRadius = (sphereRadius / viewZ) / tanHalfFov;
    
    vec2 fragNDC = texCoord * 2.0 - 1.0;
    fragNDC.x *= aspectRatio;
    sphereScreenCenter.x *= aspectRatio;
    
    vec2 relativeUV = fragNDC - sphereScreenCenter;
    
    float scale = 0.3 / apparentRadius;
    vec2 orbUV = relativeUV * scale;
    
    // Lens radius - large enough for all effects including external rays at 0.32
    // Corona adds extra width for the background fill area
    float lensRadius = 0.45 + (showCorona > 0.5 ? coronaWidth * 0.25 : 0.0);
    if (length(orbUV) > lensRadius) {
        return vec4(0.0);
    }
    
    return renderEnergyOrbV2Custom(
        orbUV, time, 
        coreColor, edgeColor, linesColor, intensity,
        coreSize, edgeSharpness, spiralDensity, spiralTwist,
        glowLineCount, glowLineIntensity,
        showExternalRays, showCorona, coronaWidth,
        coronaPower, rayPower, raySharpness, ringPower,
        // V2 Detail parameters
        coronaStart, coronaBrightness, coreRadiusScale, coreMaskRadius,
        coreSpread, coreGlow, coreMaskSoft,
        edgeRadius, edgeSpread, edgeGlow, sharpScale,
        linesUVScale, linesDensityMult, linesContrast1, linesContrast2,
        linesMaskRadius, linesMaskSoft,
        rayRotSpeed, rayStartRadius, alphaScale,
        colorBlendMode  // Pass through to inner function
    );
}

