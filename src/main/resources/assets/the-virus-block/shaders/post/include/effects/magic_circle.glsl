// ═══════════════════════════════════════════════════════════════════════════
// MAGIC CIRCLE - Ground Effect Pattern Library
// ═══════════════════════════════════════════════════════════════════════════
//
// Renders animated magic circle patterns on ground surfaces.
// Based on Shadertoy reference, fully parameterized for customization.
//
// Include: #include "include/effects/magic_circle.glsl"
// Prerequisites: core/constants.glsl, core/math_utils.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_MAGIC_CIRCLE_GLSL
#define EFFECTS_MAGIC_CIRCLE_GLSL

#include "../core/constants.glsl"
#include "../core/hdr_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// PHASE 3A: LAYER GEOMETRY PARAMETER STRUCTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Layer 4 (Middle Ring) geometry parameters.
 * From layer4_params.md documentation.
 */
struct Layer4Params {
    float innerRadius;  // Inner edge (0.0-1.0) default: 0.5
    float outerRadius;  // Outer edge (0.0-1.0) default: 0.55
    float thickness;    // Line power (0.0005-0.02) default: 0.002
    float rotOffset;    // Initial angle (0-2π) default: 0.0
};

/**
 * Layer 7 (Inner Radiation) geometry parameters.
 * From layer7_params.md documentation.
 */
struct Layer7Params {
    float innerRadius;  // Spoke start (0.0-1.0) default: 0.25
    float outerRadius;  // Spoke end (0.0-1.0) default: 0.3
    int spokeCount;     // Number of spokes (3-72) default: 12
    float thickness;    // Line power (0.0005-0.02) default: 0.005
    float rotOffset;    // Initial angle (0-2π) default: 0.0
};

/**
 * Layer 2 (Hexagram) geometry parameters.
 * From layer2_params.md documentation.
 */
struct Layer2Params {
    int rectCount;      // Number of rectangles (3-12) default: 6
    float rectSize;     // Rectangle half-extent (0.1-1.0) default: 0.601
    float thickness;    // Line power (0.0005-0.01) default: 0.0015
    float rotOffset;    // Initial angle (0-2π) default: 0.0
    bool snapRotation;  // Use angular snapping default: true
};

/**
 * Layer 5 (Inner Triangle) geometry parameters.
 * From layer5_params.md documentation.
 * Identical structure to Layer 2 but different defaults.
 */
struct Layer5Params {
    int rectCount;      // Number of rectangles (3-12) default: 3
    float rectSize;     // Rectangle half-extent (0.1-1.0) default: 0.36
    float thickness;    // Line power (0.0005-0.01) default: 0.0015
    float rotOffset;    // Initial angle (0-2π) default: 0.0
    bool snapRotation;  // Use angular snapping default: true
};

/**
 * Layer 3 (Outer Dot Ring) geometry parameters.
 * From layer3_params.md documentation.
 */
struct Layer3Params {
    int dotCount;       // Number of dots (4-36) default: 12
    float orbitRadius;  // Distance from center (0.1-1.0) default: 0.875
    float ringInner;    // Ring inner edge (0.0-0.1) default: 0.001
    float ringOuter;    // Ring outer edge (0.01-0.2) default: 0.05
    float ringThickness; // Ring power (0.001-0.02) default: 0.004
    float dotRadius;    // Center dot size (0.0-0.05) default: 0.001
    float dotThickness; // Dot brightness (0.001-0.03) default: 0.008
    float rotOffset;    // Initial angle (0-2π) default: 0.262 (π/12)
};

/**
 * Layer 6 (Inner Dot Ring) geometry parameters.
 * From layer6_params.md documentation.
 * Identical structure to Layer 3, just with different defaults (smaller, dimmer).
 */
struct Layer6Params {
    int dotCount;       // Number of dots (4-36) default: 12
    float orbitRadius;  // Distance from center (0.1-1.0) default: 0.53
    float ringInner;    // Ring inner edge (0.0-0.1) default: 0.001
    float ringOuter;    // Ring outer edge (0.01-0.2) default: 0.035
    float ringThickness; // Ring power (0.001-0.02) default: 0.004
    float dotRadius;    // Center dot size (0.0-0.05) default: 0.001
    float dotThickness; // Dot brightness (0.0005-0.03) default: 0.001
    float rotOffset;    // Initial angle (0-2π) default: 0.262 (π/12)
};

/**
 * Layer 1 (Outer Ring + Radiation) geometry parameters.
 * From layer1_params.md documentation.
 */
struct Layer1Params {
    float ringInner;    // Ring inner edge (0.0-1.0) default: 0.85
    float ringOuter;    // Ring outer edge (0.0-1.0) default: 0.9
    float ringThickness; // Ring power (0.001-0.05) default: 0.006
    float radInner;     // Spoke start (0.0-1.0) default: 0.87
    float radOuter;     // Spoke end (0.0-1.0) default: 0.88
    int radCount;       // Number of spokes (3-72) default: 36
    float radThickness; // Spoke power (0.0001-0.01) default: 0.0008
    float rotOffset;    // Initial angle (0-2π) default: 0.0
};

/**
 * Layer 8 (Spinning Core) geometry parameters.
 * From layer8_params.md documentation.
 */
struct Layer8Params {
    float breathAmp;      // Breathing amplitude (0.0-0.2) default: 0.04
    float breathCenter;   // Base scale (0.8-1.5) default: 1.1
    int orbitalCount;     // Number of orbitals (1-12) default: 6
    float orbitalStart;   // First circle radius (0.05-0.3) default: 0.13
    float orbitalStep;    // Radius decrement (0.0-0.05) default: 0.01
    float orbitalDist;    // Translation offset (0.0-0.3) default: 0.1
    float orbitalThickness; // Circle power (0.0005-0.01) default: 0.002
    float centerRadius;   // Center dot size (0.01-0.1) default: 0.04
    float centerThickness; // Center power (0.001-0.02) default: 0.004
    float rotOffset;      // Initial angle (0-2π) default: 0.0
};

// ═══════════════════════════════════════════════════════════════════════════
// TRANSFORM UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * 2D rotation matrix.
 * @param rad Rotation angle in radians
 */
vec2 mcRotate(vec2 p, float rad) {
    float c = cos(rad);
    float s = sin(rad);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

/**
 * 2D translation.
 */
vec2 mcTranslate(vec2 p, vec2 offset) {
    return p - offset;
}

/**
 * 2D uniform scale.
 */
vec2 mcScale(vec2 p, float s) {
    return p * s;
}

// ═══════════════════════════════════════════════════════════════════════════
// SDF PRIMITIVES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Ring/circle primitive with glow.
 * 
 * @param pre      Accumulated glow value (0-1)
 * @param p        Position to evaluate
 * @param r1       Inner radius
 * @param r2       Outer radius (use r1==r2 for dot)
 * @param power    Glow intensity/thickness
 * @return         New accumulated glow (clamped 0-1)
 */
float mcCircle(float pre, vec2 p, float r1, float r2, float power) {
    float leng = length(p);
    float d = min(abs(leng - r1), abs(leng - r2));
    
    // Fill behavior: if inside ring, modify pre
    if (r1 < leng && leng < r2) {
        pre /= exp(d) / r2;
    }
    
    float res = power / max(d, 0.0001);
    return hdrAccumulate(pre, res);
}

/**
 * Rectangle outline primitive with glow.
 * 
 * @param pre      Accumulated glow value
 * @param p        Position to evaluate
 * @param half1    Inner corner half-extent (vec2)
 * @param half2    Outer corner half-extent (vec2)
 * @param power    Glow intensity/thickness
 * @return         New accumulated glow
 */
float mcRectangle(float pre, vec2 p, vec2 half1, vec2 half2, float power) {
    p = abs(p);
    
    // Fill behavior
    if ((half1.x < p.x || half1.y < p.y) && (p.x < half2.x && p.y < half2.y)) {
        pre = max(0.01, pre);
    }
    
    // Distance to edges
    float dx1 = (p.y < half1.y) ? abs(half1.x - p.x) : length(p - half1);
    float dx2 = (p.y < half2.y) ? abs(half2.x - p.x) : length(p - half2);
    float dy1 = (p.x < half1.x) ? abs(half1.y - p.y) : length(p - half1);
    float dy2 = (p.x < half2.x) ? abs(half2.y - p.y) : length(p - half2);
    
    float d = min(min(dx1, dx2), min(dy1, dy2));
    float res = power / max(d, 0.0001);
    return hdrAccumulate(pre, res);
}

/**
 * Radiation pattern - radial lines from center.
 * 
 * @param pre      Accumulated glow value
 * @param p        Position to evaluate
 * @param r1       Inner radius (line start)
 * @param r2       Outer radius (line end)
 * @param count    Number of radial lines
 * @param power    Line thickness
 * @return         New accumulated glow
 */
float mcRadiation(float pre, vec2 p, float r1, float r2, int count, float power) {
    float angle = TWO_PI / float(count);
    float d = 1e10;
    
    vec2 q = p;
    for (int i = 0; i < count && i < 72; i++) {
        float _d;
        if (r1 < q.y && q.y < r2) {
            _d = abs(q.x);
        } else {
            _d = min(length(q - vec2(0.0, r1)), length(q - vec2(0.0, r2)));
        }
        d = min(d, _d);
        q = mcRotate(q, angle);
    }
    
    float res = power / max(d, 0.0001);
    return hdrAccumulate(pre, res);
}

// ═══════════════════════════════════════════════════════════════════════════
// LAYER RENDERING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Layer 1: Outer Ring with Radiation
 */
float mcLayer1_OuterRing(
    float pre, vec2 p, float time,
    // Ring params
    bool ringEnable, float ringInnerR, float ringOuterR, float ringThickness,
    // Radiation params  
    bool radEnable, float radInnerR, float radOuterR, int radCount, float radThickness,
    // Shared params
    float rotationSpeed, float intensity
) {
    if (!ringEnable && !radEnable) return pre;
    
    vec2 q = mcRotate(p, time * rotationSpeed);
    
    if (ringEnable) {
        pre = mcCircle(pre, q, ringInnerR, ringOuterR, ringThickness * intensity);
    }
    
    if (radEnable) {
        pre = mcRadiation(pre, q, radInnerR, radOuterR, radCount, radThickness * intensity);
    }
    
    return pre;
}

/**
 * Layer 2: Hexagram (rotated rectangles)
 */
float mcLayer2_Hexagram(
    float pre, vec2 p, float time,
    bool enable, int rectCount, float rectSize, float thickness,
    float rotationSpeed, float rotationOffset, float intensity,
    bool snapRotation
) {
    if (!enable) return pre;
    
    vec2 q = mcRotate(p, time * rotationSpeed + rotationOffset);
    
    float angle = PI / float(rectCount);
    
    // Angular snapping (optional)
    if (snapRotation) {
        q = mcRotate(q, floor(atan(q.x, q.y) / angle + 0.5) * angle);
    }
    
    vec2 halfSize = vec2(rectSize);
    
    for (int i = 0; i < rectCount && i < 12; i++) {
        pre = mcRectangle(pre, q, halfSize, halfSize, thickness * intensity);
        q = mcRotate(q, angle);
    }
    
    return pre;
}

/**
 * Layer 3/6: Dot Ring (circles arranged in a ring)
 */
float mcLayerDotRing(
    float pre, vec2 p, float time,
    bool enable, int dotCount, float orbitRadius,
    // Ring part
    bool ringPartEnable, float ringInnerR, float ringOuterR, float ringThickness,
    // Dot part
    bool dotPartEnable, float dotRadius, float dotThickness,
    // Animation
    float rotationSpeed, float rotationOffset, float intensity
) {
    if (!enable) return pre;
    
    vec2 q = mcRotate(p, time * rotationSpeed);
    q = mcRotate(q, rotationOffset);
    
    float angle = TWO_PI / float(dotCount);
    
    for (int i = 0; i < dotCount && i < 36; i++) {
        vec2 dotPos = q - vec2(0.0, orbitRadius);
        
        if (ringPartEnable) {
            pre = mcCircle(pre, dotPos, ringInnerR, ringOuterR, ringThickness * intensity);
        }
        
        if (dotPartEnable) {
            pre = mcCircle(pre, dotPos, dotRadius, dotRadius, dotThickness * intensity);
        }
        
        q = mcRotate(q, angle);
    }
    
    return pre;
}

/**
 * Layer 4: Middle Ring (simple static ring)
 */
float mcLayer4_MiddleRing(
    float pre, vec2 p, float time,
    bool enable, float innerR, float outerR, float thickness,
    float rotationSpeed, float rotationOffset, float intensity
) {
    if (!enable) return pre;
    
    vec2 q = mcRotate(p, time * rotationSpeed + rotationOffset);
    pre = mcCircle(pre, q, innerR, outerR, thickness * intensity);
    
    return pre;
}

/**
 * Layer 7: Inner Radiation
 */
float mcLayer7_InnerRadiation(
    float pre, vec2 p, float time,
    bool enable, float innerR, float outerR, int spokeCount, float thickness,
    float rotationSpeed, float rotationOffset, float intensity
) {
    if (!enable) return pre;
    
    vec2 q = mcRotate(p, time * rotationSpeed + rotationOffset);
    pre = mcRadiation(pre, q, innerR, outerR, spokeCount, thickness * intensity);
    
    return pre;
}

/**
 * Layer 8: Spinning Core (complex nested orbitals)
 */
float mcLayer8_SpinningCore(
    float pre, vec2 p, float time,
    bool enable,
    // Breathing
    bool breathingEnable, float breathingFreq, float breathingAmp, float breathingCenter,
    // Rotation
    float rotationSpeed, float rotationOffset,
    // Orbitals
    int orbitalCount, float orbitalStartRadius, float orbitalRadiusStep,
    float orbitalDistance, float orbitalThickness, float orbitalRotationSpeed,
    // Center
    bool centerEnable, float centerRadius, float centerThickness,
    // Master
    float intensity
) {
    if (!enable) return pre;
    
    vec2 q = p;
    
    // Apply breathing scale
    if (breathingEnable) {
        float breathScale = sin(breathingFreq * time) * breathingAmp + breathingCenter;
        q = mcScale(q, breathScale);
    }
    
    // Main rotation
    q = mcRotate(q, time * rotationSpeed + rotationOffset);
    
    // Orbital circles
    for (int i = 0; i < orbitalCount && i < 12; i++) {
        float r = orbitalStartRadius - float(i) * orbitalRadiusStep;
        if (r <= 0.0) break;
        
        q = mcTranslate(q, vec2(orbitalDistance, 0.0));
        pre = mcCircle(pre, q, r, r, orbitalThickness * intensity);
        q = mcTranslate(q, -vec2(orbitalDistance, 0.0));
        q = mcRotate(q, time * orbitalRotationSpeed);
    }
    
    // Center dot
    if (centerEnable) {
        pre = mcCircle(pre, q, centerRadius, centerRadius, centerThickness * intensity);
    }
    
    return pre;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDERING FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Result of magic circle rendering.
 */
struct MagicCircleResult {
    float pattern;     // Raw pattern value (0-1)
    vec3 color;        // Final colored output
    float alpha;       // For compositing
};

/**
 * Render complete magic circle with all layers.
 * Phase 1: Hardcoded layer parameters, expose only globals.
 */
MagicCircleResult renderMagicCircle(
    vec2 normalizedPos,   // Position normalized to 0-1 at effect edge
    float time,           // Animation time
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing (applied to input)
    vec2 p = normalizedPos;
    float globalBreath = sin(PI * time) * 0.02 + 1.1;
    p = mcScale(p, globalBreath);
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer1_OuterRing(
        result.pattern, p, time,
        true, 0.85, 0.9, 0.006,           // Ring params
        true, 0.87, 0.88, 36, 0.0008,     // Radiation params
        0.524, globalIntensity            // Rotation speed, intensity
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer2_Hexagram(
        result.pattern, p, time,
        true, 6, 0.601, 0.0015,           // Enable, count, size, thickness
        0.524, 0.0, globalIntensity,      // Rotation speed, offset, intensity
        true                               // Snap rotation
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayerDotRing(
        result.pattern, p, time,
        true, 12, 0.875,                   // Enable, count, orbit radius
        true, 0.001, 0.05, 0.004,          // Ring part
        true, 0.001, 0.008,                // Dot part
        0.524, 0.262, globalIntensity      // Rotation, offset, intensity
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer4_MiddleRing(
        result.pattern, p, time,
        true, 0.5, 0.55, 0.002,            // Enable, inner, outer, thickness
        0.0, 0.0, globalIntensity          // No rotation for static ring
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle (same as hexagram, different params)
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer2_Hexagram(
        result.pattern, p, time,
        true, 3, 0.36, 0.0015,            // 3 rects, smaller
        -0.524, 0.0, globalIntensity,     // Counter-clockwise
        true
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayerDotRing(
        result.pattern, p, time,
        true, 12, 0.53,                    // Smaller orbit
        true, 0.001, 0.035, 0.004,         // Smaller ring
        true, 0.001, 0.001,                // Dimmer dot
        -0.524, 0.262, globalIntensity     // Counter-clockwise
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer7_InnerRadiation(
        result.pattern, p, time,
        true, 0.25, 0.3, 12, 0.005,        // Enable, radii, count, thickness
        0.524, 0.0, globalIntensity        // Clockwise
    );
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core
    // ─────────────────────────────────────────────────────────────────
    result.pattern = mcLayer8_SpinningCore(
        result.pattern, p, time,
        true,                              // Enable
        true, 3.14, 0.04, 1.1,             // Breathing
        -0.524, 0.0,                       // Rotation
        6, 0.13, 0.01, 0.1, 0.002, -0.262, // Orbitals
        true, 0.04, 0.004,                 // Center
        globalIntensity
    );
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

/**
 * Render magic circle with per-layer controls.
 * Phase 2: Configurable layer enable/intensity/speed.
 */
MagicCircleResult renderMagicCircleWithLayers(
    vec2 normalizedPos,   // Position normalized to 0-1 at effect edge
    float time,           // Animation time
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent,
    bool layerEnable[8],
    float layerIntensity[8],
    float layerSpeed[8]
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing (applied to input)
    vec2 p = normalizedPos;
    float globalBreath = sin(PI * time) * 0.02 + 1.1;
    p = mcScale(p, globalBreath);
    
    // Base rotation speed (used as multiplier with layer speeds)
    float baseSpeed = 0.524;
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[0]) {
        float effIntensity = globalIntensity * layerIntensity[0];
        float effSpeed = baseSpeed * layerSpeed[0];
        result.pattern = mcLayer1_OuterRing(
            result.pattern, p, time,
            true, 0.85, 0.9, 0.006,           // Ring params
            true, 0.87, 0.88, 36, 0.0008,     // Radiation params
            effSpeed, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[1]) {
        float effIntensity = globalIntensity * layerIntensity[1];
        float effSpeed = baseSpeed * layerSpeed[1];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 6, 0.601, 0.0015,
            effSpeed, 0.0, effIntensity,
            true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[2]) {
        float effIntensity = globalIntensity * layerIntensity[2];
        float effSpeed = baseSpeed * layerSpeed[2];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.875,
            true, 0.001, 0.05, 0.004,
            true, 0.001, 0.008,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[3]) {
        float effIntensity = globalIntensity * layerIntensity[3];
        float effSpeed = baseSpeed * layerSpeed[3];  // Default is 0 for static
        result.pattern = mcLayer4_MiddleRing(
            result.pattern, p, time,
            true, 0.5, 0.55, 0.002,
            effSpeed, 0.0, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[4]) {
        float effIntensity = globalIntensity * layerIntensity[4];
        float effSpeed = baseSpeed * layerSpeed[4];  // Default is negative
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 3, 0.36, 0.0015,
            effSpeed, 0.0, effIntensity,  // Use layer speed (negative by default)
            true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[5]) {
        float effIntensity = globalIntensity * layerIntensity[5];
        float effSpeed = baseSpeed * layerSpeed[5];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.53,
            true, 0.001, 0.035, 0.004,
            true, 0.001, 0.001,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[6]) {
        float effIntensity = globalIntensity * layerIntensity[6];
        float effSpeed = baseSpeed * layerSpeed[6];
        result.pattern = mcLayer7_InnerRadiation(
            result.pattern, p, time,
            true, 0.25, 0.3, 12, 0.005,
            effSpeed, 0.0, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[7]) {
        float effIntensity = globalIntensity * layerIntensity[7];
        float effSpeed = baseSpeed * layerSpeed[7];
        result.pattern = mcLayer8_SpinningCore(
            result.pattern, p, time,
            true,
            true, 3.14, 0.04, 1.1,
            effSpeed, 0.0,  // Use layer speed
            6, 0.13, 0.01, 0.1, 0.002, effSpeed * 0.5,  // Orbital speed based on layer speed
            true, 0.04, 0.004,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

/**
 * Render magic circle with Phase 3A geometry controls.
 * Adds customizable Layer 4 (Middle Ring) and Layer 7 (Inner Radiation) geometry.
 */
MagicCircleResult renderMagicCirclePhase3A(
    vec2 normalizedPos,
    float time,
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent,
    bool layerEnable[8],
    float layerIntensity[8],
    float layerSpeed[8],
    Layer4Params l4,
    Layer7Params l7
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing (applied to input)
    vec2 p = normalizedPos;
    float globalBreath = sin(PI * time) * 0.02 + 1.1;
    p = mcScale(p, globalBreath);
    
    // Base rotation speed (used as multiplier with layer speeds)
    float baseSpeed = 0.524;
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[0]) {
        float effIntensity = globalIntensity * layerIntensity[0];
        float effSpeed = baseSpeed * layerSpeed[0];
        result.pattern = mcLayer1_OuterRing(
            result.pattern, p, time,
            true, 0.85, 0.9, 0.006,
            true, 0.87, 0.88, 36, 0.0008,
            effSpeed, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[1]) {
        float effIntensity = globalIntensity * layerIntensity[1];
        float effSpeed = baseSpeed * layerSpeed[1];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 6, 0.601, 0.0015,
            effSpeed, 0.0, effIntensity,
            true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[2]) {
        float effIntensity = globalIntensity * layerIntensity[2];
        float effSpeed = baseSpeed * layerSpeed[2];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.875,
            true, 0.001, 0.05, 0.004,
            true, 0.001, 0.008,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[3]) {
        float effIntensity = globalIntensity * layerIntensity[3];
        float effSpeed = baseSpeed * layerSpeed[3];
        // Use geometry from l4 struct
        result.pattern = mcLayer4_MiddleRing(
            result.pattern, p, time,
            true, 
            l4.innerRadius,   // Was hardcoded 0.5
            l4.outerRadius,   // Was hardcoded 0.55
            l4.thickness,     // Was hardcoded 0.002
            effSpeed, 
            l4.rotOffset,     // Was hardcoded 0.0
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[4]) {
        float effIntensity = globalIntensity * layerIntensity[4];
        float effSpeed = baseSpeed * layerSpeed[4];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 3, 0.36, 0.0015,
            effSpeed, 0.0, effIntensity,
            true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[5]) {
        float effIntensity = globalIntensity * layerIntensity[5];
        float effSpeed = baseSpeed * layerSpeed[5];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.53,
            true, 0.001, 0.035, 0.004,
            true, 0.001, 0.001,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[6]) {
        float effIntensity = globalIntensity * layerIntensity[6];
        float effSpeed = baseSpeed * layerSpeed[6];
        // Use geometry from l7 struct
        result.pattern = mcLayer7_InnerRadiation(
            result.pattern, p, time,
            true, 
            l7.innerRadius,   // Was hardcoded 0.25
            l7.outerRadius,   // Was hardcoded 0.3
            l7.spokeCount,    // Was hardcoded 12
            l7.thickness,     // Was hardcoded 0.005
            effSpeed, 
            l7.rotOffset,     // Was hardcoded 0.0
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[7]) {
        float effIntensity = globalIntensity * layerIntensity[7];
        float effSpeed = baseSpeed * layerSpeed[7];
        result.pattern = mcLayer8_SpinningCore(
            result.pattern, p, time,
            true,
            true, 3.14, 0.04, 1.1,
            effSpeed, 0.0,
            6, 0.13, 0.01, 0.1, 0.002, effSpeed * 0.5,
            true, 0.04, 0.004,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

/**
 * Render magic circle with Phase 3B geometry controls.
 * Adds customizable Layer 2 (Hexagram) and Layer 5 (Inner Triangle) geometry
 * on top of Phase 3A's Layer 4 and Layer 7 geometry.
 */
MagicCircleResult renderMagicCirclePhase3B(
    vec2 normalizedPos,
    float time,
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent,
    bool layerEnable[8],
    float layerIntensity[8],
    float layerSpeed[8],
    Layer4Params l4,
    Layer7Params l7,
    Layer2Params l2,
    Layer5Params l5
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing (applied to input)
    vec2 p = normalizedPos;
    float globalBreath = sin(PI * time) * 0.02 + 1.1;
    p = mcScale(p, globalBreath);
    
    // Base rotation speed (used as multiplier with layer speeds)
    float baseSpeed = 0.524;
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[0]) {
        float effIntensity = globalIntensity * layerIntensity[0];
        float effSpeed = baseSpeed * layerSpeed[0];
        result.pattern = mcLayer1_OuterRing(
            result.pattern, p, time,
            true, 0.85, 0.9, 0.006,
            true, 0.87, 0.88, 36, 0.0008,
            effSpeed, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram ** PHASE 3B: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[1]) {
        float effIntensity = globalIntensity * layerIntensity[1];
        float effSpeed = baseSpeed * layerSpeed[1];
        // Use geometry from l2 struct
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l2.rectCount,     // Was hardcoded 6
            l2.rectSize,      // Was hardcoded 0.601
            l2.thickness,     // Was hardcoded 0.0015
            effSpeed, 
            l2.rotOffset,     // Was hardcoded 0.0
            effIntensity,
            l2.snapRotation   // Was hardcoded true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[2]) {
        float effIntensity = globalIntensity * layerIntensity[2];
        float effSpeed = baseSpeed * layerSpeed[2];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.875,
            true, 0.001, 0.05, 0.004,
            true, 0.001, 0.008,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[3]) {
        float effIntensity = globalIntensity * layerIntensity[3];
        float effSpeed = baseSpeed * layerSpeed[3];
        result.pattern = mcLayer4_MiddleRing(
            result.pattern, p, time,
            true, 
            l4.innerRadius,
            l4.outerRadius,
            l4.thickness,
            effSpeed, 
            l4.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle ** PHASE 3B: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[4]) {
        float effIntensity = globalIntensity * layerIntensity[4];
        float effSpeed = baseSpeed * layerSpeed[4];
        // Use geometry from l5 struct
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l5.rectCount,     // Was hardcoded 3
            l5.rectSize,      // Was hardcoded 0.36
            l5.thickness,     // Was hardcoded 0.0015
            effSpeed, 
            l5.rotOffset,     // Was hardcoded 0.0
            effIntensity,
            l5.snapRotation   // Was hardcoded true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[5]) {
        float effIntensity = globalIntensity * layerIntensity[5];
        float effSpeed = baseSpeed * layerSpeed[5];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 12, 0.53,
            true, 0.001, 0.035, 0.004,
            true, 0.001, 0.001,
            effSpeed, 0.262, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[6]) {
        float effIntensity = globalIntensity * layerIntensity[6];
        float effSpeed = baseSpeed * layerSpeed[6];
        result.pattern = mcLayer7_InnerRadiation(
            result.pattern, p, time,
            true, 
            l7.innerRadius,
            l7.outerRadius,
            l7.spokeCount,
            l7.thickness,
            effSpeed, 
            l7.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[7]) {
        float effIntensity = globalIntensity * layerIntensity[7];
        float effSpeed = baseSpeed * layerSpeed[7];
        result.pattern = mcLayer8_SpinningCore(
            result.pattern, p, time,
            true,
            true, 3.14, 0.04, 1.1,
            effSpeed, 0.0,
            6, 0.13, 0.01, 0.1, 0.002, effSpeed * 0.5,
            true, 0.04, 0.004,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

/**
 * Render magic circle with Phase 3C geometry controls.
 * Adds customizable Layer 3 (Outer Dot Ring) and Layer 6 (Inner Dot Ring) geometry
 * on top of Phase 3A (Layer 4, 7) and Phase 3B (Layer 2, 5) geometry.
 */
MagicCircleResult renderMagicCirclePhase3C(
    vec2 normalizedPos,
    float time,
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent,
    bool layerEnable[8],
    float layerIntensity[8],
    float layerSpeed[8],
    Layer4Params l4,
    Layer7Params l7,
    Layer2Params l2,
    Layer5Params l5,
    Layer3Params l3,
    Layer6Params l6
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing (applied to input)
    vec2 p = normalizedPos;
    float globalBreath = sin(PI * time) * 0.02 + 1.1;
    p = mcScale(p, globalBreath);
    
    // Base rotation speed (used as multiplier with layer speeds)
    float baseSpeed = 0.524;
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[0]) {
        float effIntensity = globalIntensity * layerIntensity[0];
        float effSpeed = baseSpeed * layerSpeed[0];
        result.pattern = mcLayer1_OuterRing(
            result.pattern, p, time,
            true, 0.85, 0.9, 0.006,
            true, 0.87, 0.88, 36, 0.0008,
            effSpeed, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram ** PHASE 3B: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[1]) {
        float effIntensity = globalIntensity * layerIntensity[1];
        float effSpeed = baseSpeed * layerSpeed[1];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l2.rectCount,
            l2.rectSize,
            l2.thickness,
            effSpeed, 
            l2.rotOffset,
            effIntensity,
            l2.snapRotation
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring ** PHASE 3C: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[2]) {
        float effIntensity = globalIntensity * layerIntensity[2];
        float effSpeed = baseSpeed * layerSpeed[2];
        // Use geometry from l3 struct
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 
            l3.dotCount,       // Was hardcoded 12
            l3.orbitRadius,    // Was hardcoded 0.875
            true, 
            l3.ringInner,      // Was hardcoded 0.001
            l3.ringOuter,      // Was hardcoded 0.05
            l3.ringThickness,  // Was hardcoded 0.004
            true, 
            l3.dotRadius,      // Was hardcoded 0.001
            l3.dotThickness,   // Was hardcoded 0.008
            effSpeed, 
            l3.rotOffset,      // Was hardcoded 0.262
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[3]) {
        float effIntensity = globalIntensity * layerIntensity[3];
        float effSpeed = baseSpeed * layerSpeed[3];
        result.pattern = mcLayer4_MiddleRing(
            result.pattern, p, time,
            true, 
            l4.innerRadius,
            l4.outerRadius,
            l4.thickness,
            effSpeed, 
            l4.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle ** PHASE 3B: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[4]) {
        float effIntensity = globalIntensity * layerIntensity[4];
        float effSpeed = baseSpeed * layerSpeed[4];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l5.rectCount,
            l5.rectSize,
            l5.thickness,
            effSpeed, 
            l5.rotOffset,
            effIntensity,
            l5.snapRotation
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring ** PHASE 3C: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[5]) {
        float effIntensity = globalIntensity * layerIntensity[5];
        float effSpeed = baseSpeed * layerSpeed[5];
        // Use geometry from l6 struct
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 
            l6.dotCount,       // Was hardcoded 12
            l6.orbitRadius,    // Was hardcoded 0.53
            true, 
            l6.ringInner,      // Was hardcoded 0.001
            l6.ringOuter,      // Was hardcoded 0.035
            l6.ringThickness,  // Was hardcoded 0.004
            true, 
            l6.dotRadius,      // Was hardcoded 0.001
            l6.dotThickness,   // Was hardcoded 0.001
            effSpeed, 
            l6.rotOffset,      // Was hardcoded 0.262
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation ** PHASE 3A: CUSTOMIZABLE GEOMETRY **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[6]) {
        float effIntensity = globalIntensity * layerIntensity[6];
        float effSpeed = baseSpeed * layerSpeed[6];
        result.pattern = mcLayer7_InnerRadiation(
            result.pattern, p, time,
            true, 
            l7.innerRadius,
            l7.outerRadius,
            l7.spokeCount,
            l7.thickness,
            effSpeed, 
            l7.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core (hardcoded geometry)
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[7]) {
        float effIntensity = globalIntensity * layerIntensity[7];
        float effSpeed = baseSpeed * layerSpeed[7];
        result.pattern = mcLayer8_SpinningCore(
            result.pattern, p, time,
            true,
            true, 3.14, 0.04, 1.1,
            effSpeed, 0.0,
            6, 0.13, 0.01, 0.1, 0.002, effSpeed * 0.5,
            true, 0.04, 0.004,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

/**
 * FINAL render function with ALL geometry controls.
 * Complete Phase 3 implementation with geometry customization for all 8 layers.
 */
MagicCircleResult renderMagicCircleFinal(
    vec2 normalizedPos,
    float time,
    float globalIntensity,
    vec3 primaryColor,
    float glowExponent,
    bool layerEnable[8],
    float layerIntensity[8],
    float layerSpeed[8],
    Layer4Params l4,
    Layer7Params l7,
    Layer2Params l2,
    Layer5Params l5,
    Layer3Params l3,
    Layer6Params l6,
    Layer1Params l1,
    Layer8Params l8
) {
    MagicCircleResult result;
    result.pattern = 0.0;
    
    // Global breathing is now applied at the call site (magic_circle.fsh)
    // using BreathTime and BreathingAmount from UBO
    vec2 p = normalizedPos;
    
    // Base rotation speed (used as multiplier with layer speeds)
    float baseSpeed = 0.524;
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 1: Outer Ring + Radiation ** PHASE 3D: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[0]) {
        float effIntensity = globalIntensity * layerIntensity[0];
        float effSpeed = baseSpeed * layerSpeed[0];
        // Use geometry from l1 struct
        result.pattern = mcLayer1_OuterRing(
            result.pattern, p, time,
            true, 
            l1.ringInner,      // Was hardcoded 0.85
            l1.ringOuter,      // Was hardcoded 0.9
            l1.ringThickness,  // Was hardcoded 0.006
            true, 
            l1.radInner,       // Was hardcoded 0.87
            l1.radOuter,       // Was hardcoded 0.88
            l1.radCount,       // Was hardcoded 36
            l1.radThickness,   // Was hardcoded 0.0008
            effSpeed, effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 2: Hexagram ** PHASE 3B: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[1]) {
        float effIntensity = globalIntensity * layerIntensity[1];
        float effSpeed = baseSpeed * layerSpeed[1];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l2.rectCount,
            l2.rectSize,
            l2.thickness,
            effSpeed, 
            l2.rotOffset,
            effIntensity,
            l2.snapRotation
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 3: Outer Dot Ring ** PHASE 3C: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[2]) {
        float effIntensity = globalIntensity * layerIntensity[2];
        float effSpeed = baseSpeed * layerSpeed[2];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 
            l3.dotCount,
            l3.orbitRadius,
            true, 
            l3.ringInner,
            l3.ringOuter,
            l3.ringThickness,
            true, 
            l3.dotRadius,
            l3.dotThickness,
            effSpeed, 
            l3.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 4: Middle Ring ** PHASE 3A: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[3]) {
        float effIntensity = globalIntensity * layerIntensity[3];
        float effSpeed = baseSpeed * layerSpeed[3];
        result.pattern = mcLayer4_MiddleRing(
            result.pattern, p, time,
            true, 
            l4.innerRadius,
            l4.outerRadius,
            l4.thickness,
            effSpeed, 
            l4.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 5: Inner Triangle ** PHASE 3B: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[4]) {
        float effIntensity = globalIntensity * layerIntensity[4];
        float effSpeed = baseSpeed * layerSpeed[4];
        result.pattern = mcLayer2_Hexagram(
            result.pattern, p, time,
            true, 
            l5.rectCount,
            l5.rectSize,
            l5.thickness,
            effSpeed, 
            l5.rotOffset,
            effIntensity,
            l5.snapRotation
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 6: Inner Dot Ring ** PHASE 3C: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[5]) {
        float effIntensity = globalIntensity * layerIntensity[5];
        float effSpeed = baseSpeed * layerSpeed[5];
        result.pattern = mcLayerDotRing(
            result.pattern, p, time,
            true, 
            l6.dotCount,
            l6.orbitRadius,
            true, 
            l6.ringInner,
            l6.ringOuter,
            l6.ringThickness,
            true, 
            l6.dotRadius,
            l6.dotThickness,
            effSpeed, 
            l6.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 7: Inner Radiation ** PHASE 3A: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[6]) {
        float effIntensity = globalIntensity * layerIntensity[6];
        float effSpeed = baseSpeed * layerSpeed[6];
        result.pattern = mcLayer7_InnerRadiation(
            result.pattern, p, time,
            true, 
            l7.innerRadius,
            l7.outerRadius,
            l7.spokeCount,
            l7.thickness,
            effSpeed, 
            l7.rotOffset,
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // LAYER 8: Spinning Core ** PHASE 3D: CUSTOMIZABLE **
    // ─────────────────────────────────────────────────────────────────
    if (layerEnable[7]) {
        float effIntensity = globalIntensity * layerIntensity[7];
        float effSpeed = baseSpeed * layerSpeed[7];
        // Use geometry from l8 struct
        result.pattern = mcLayer8_SpinningCore(
            result.pattern, p, time,
            true,
            true, 
            PI,                 // breathingFreq (hardcoded OK)
            l8.breathAmp,       // Was hardcoded 0.04
            l8.breathCenter,    // Was hardcoded 1.1
            effSpeed, 
            l8.rotOffset,       // Was hardcoded 0.0
            l8.orbitalCount,    // Was hardcoded 6
            l8.orbitalStart,    // Was hardcoded 0.13
            l8.orbitalStep,     // Was hardcoded 0.01
            l8.orbitalDist,     // Was hardcoded 0.1
            l8.orbitalThickness, // Was hardcoded 0.002
            effSpeed * 0.5,     // orbitalRotSpeed (relative)
            true, 
            l8.centerRadius,    // Was hardcoded 0.04
            l8.centerThickness, // Was hardcoded 0.004
            effIntensity
        );
    }
    
    // ─────────────────────────────────────────────────────────────────
    // FINAL COLOR
    // ─────────────────────────────────────────────────────────────────
    result.pattern = pow(result.pattern, glowExponent);
    result.color = result.pattern * primaryColor;
    result.alpha = result.pattern;
    
    return result;
}

#endif // EFFECTS_MAGIC_CIRCLE_GLSL
