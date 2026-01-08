// ═══════════════════════════════════════════════════════════════════════════
// RENDERING: SCREEN-SPACE EFFECTS (V2 rendering utilities)
// ═══════════════════════════════════════════════════════════════════════════
// 
// 2D effect utilities for screen-space (V2/top-down) rendering.
// These work in screen UV coordinates after 3D projection.
//
// Include: #include "include/rendering/screen_effects.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef RENDERING_SCREEN_EFFECTS_GLSL
#define RENDERING_SCREEN_EFFECTS_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// DISTANCE CALCULATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Distance from current pixel to a projected point.
 * Corrects for aspect ratio.
 * 
 * @param texCoord  Current pixel UV
 * @param center    Projected point's screen UV
 * @param aspect    Aspect ratio (width/height)
 * @return          Distance in screen units
 */
float distToPoint(vec2 texCoord, vec2 center, float aspect) {
    vec2 delta = texCoord - center;
    delta.x *= aspect;  // Correct for aspect ratio
    return length(delta);
}

/**
 * Normalized distance to a projected sphere.
 * 0 = at center, 1 = at edge, >1 = outside.
 * 
 * @param texCoord        Current pixel UV
 * @param proj            Sphere projection data
 * @param aspect          Aspect ratio
 * @return                Normalized distance
 */
float normalizedDistToSphere(vec2 texCoord, SphereProjection proj, float aspect) {
    float dist = distToPoint(texCoord, proj.screenCenter, aspect);
    return dist / max(proj.apparentRadius, NEAR_ZERO);
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW & FALLOFF FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Quadratic glow falloff (from glow_utils.glsl).
 * This is the ORIGINAL version used by field_visual.fsh.
 * 
 * @param dist    Distance from center
 * @param radius  Falloff radius (1.0 at center, 0.0 at radius)
 * @return        Glow intensity (1 at center, quadratic falloff to 0)
 */
#define GLOW_FALLOFF_DEFINED
float glowFalloff(float dist, float radius) {
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

/**
 * Inverse-distance glow (Shadertoy style).
 * Different from glowFalloff - use for lens flares, etc.
 * 
 * @param dist      Distance from center
 * @param power     Falloff power (higher = faster falloff)
 * @return          Glow intensity (infinite at center, asymptotic to 0)
 */
float glowInverse(float dist, float power) {
    return 1.0 / (1.0 + pow(dist, power));
}

/**
 * Exponential glow falloff (sharper than quadratic).
 */
float glowExp(float dist, float falloff) {
    return exp(-dist * falloff);
}

/**
 * Edge falloff (from field_visual.fsh).
 * Used for energy orb edge rendering.
 * 
 * @param dist       Distance from edge
 * @param thickness  Edge thickness
 * @param sharpness  Higher = sharper falloff
 * @return           Edge intensity (1 at edge, falling off to 0)
 */
float edgeFalloff(float dist, float thickness, float sharpness) {
    float t = abs(dist) / thickness;
    return pow(max(0.0, 1.0 - t), sharpness);
}

/**
 * Soft edge falloff (smoothstep-based).
 * 
 * @param dist         Distance from center
 * @param innerRadius  Start of falloff (full intensity)
 * @param outerRadius  End of falloff (zero intensity)
 */
float softEdge(float dist, float innerRadius, float outerRadius) {
    return 1.0 - smoothstep(innerRadius, outerRadius, dist);
}

/**
 * Hard edge with smooth transition.
 */
float hardEdge(float dist, float radius, float edgeWidth) {
    return 1.0 - smoothstep(radius - edgeWidth, radius, dist);
}

// ═══════════════════════════════════════════════════════════════════════════
// RING EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Ring effect (peaks at specific radius).
 * 
 * @param dist         Distance from center
 * @param ringRadius   Radius of ring center
 * @param thickness    Ring thickness
 * @return             Ring intensity (1 at ring center, 0 at edges)
 */
float ringEffect(float dist, float ringRadius, float thickness) {
    float distFromRing = abs(dist - ringRadius);
    return smoothstep(thickness, 0.0, distFromRing);
}

/**
 * Glowing ring (ring with outer glow).
 * 
 * @param dist         Distance from center
 * @param ringRadius   Radius of ring
 * @param ringWidth    Width of solid ring
 * @param glowWidth    Additional glow around ring
 */
float glowingRing(float dist, float ringRadius, float ringWidth, float glowWidth) {
    float distFromRing = abs(dist - ringRadius);
    
    // Core ring
    float core = smoothstep(ringWidth, 0.0, distFromRing);
    
    // Outer glow
    float glow = smoothstep(ringWidth + glowWidth, ringWidth, distFromRing);
    
    return max(core, glow * 0.5);
}

// ═══════════════════════════════════════════════════════════════════════════
// RADIAL PATTERNS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Radial lines pattern (for glow beams from center).
 * 
 * @param texCoord   Current pixel UV
 * @param center     Center point UV
 * @param lineCount  Number of lines
 * @param width      Line width (0-1)
 * @param rotation   Rotation offset (radians)
 * @return           Line intensity
 */
float radialLines(vec2 texCoord, vec2 center, float lineCount, float width, float rotation) {
    vec2 delta = texCoord - center;
    float angle = atan(delta.y, delta.x) + rotation;
    
    // Create repeating pattern
    float lineAngle = angle * lineCount;
    float line = abs(cos(lineAngle));
    
    // Sharpen based on width
    return pow(line, 1.0 / max(width, 0.01));
}

/**
 * Starburst pattern (lines that fade with distance).
 */
float starburst(vec2 texCoord, vec2 center, float lineCount, float width, 
                float falloff, float rotation, float aspect) {
    float dist = distToPoint(texCoord, center, aspect);
    float lines = radialLines(texCoord, center, lineCount, width, rotation);
    float fade = glowFalloff(dist, falloff);
    return lines * fade;
}

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATED EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pulsing glow (oscillates intensity).
 * 
 * @param baseIntensity  Base glow intensity
 * @param pulseAmount    Amount of pulse (0-1)
 * @param time           Current time
 * @param speed          Pulse speed
 */
float pulsingGlow(float baseIntensity, float pulseAmount, float time, float speed) {
    float pulse = sin(time * speed) * 0.5 + 0.5;
    return baseIntensity * (1.0 - pulseAmount + pulseAmount * pulse);
}

/**
 * Rotating pattern UV offset.
 * 
 * @param texCoord   Input UV
 * @param center     Rotation center
 * @param time       Current time
 * @param speed      Rotation speed
 * @return           Rotated UV
 */
vec2 rotatingUV(vec2 texCoord, vec2 center, float time, float speed) {
    vec2 delta = texCoord - center;
    float angle = time * speed;
    float c = cos(angle);
    float s = sin(angle);
    return center + vec2(
        delta.x * c - delta.y * s,
        delta.x * s + delta.y * c
    );
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPOSITING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Additive blend with intensity control.
 */
vec4 addGlow(vec4 base, vec3 glowColor, float intensity) {
    return vec4(base.rgb + glowColor * intensity, max(base.a, intensity));
}

/**
 * Layer multiple glow effects.
 */
vec4 layerGlows(vec4 base, vec3 color1, float int1, vec3 color2, float int2, vec3 color3, float int3) {
    vec3 result = base.rgb;
    result += color1 * int1;
    result += color2 * int2;
    result += color3 * int3;
    float alpha = max(base.a, max(int1, max(int2, int3)));
    return vec4(result, alpha);
}

#endif // RENDERING_SCREEN_EFFECTS_GLSL
