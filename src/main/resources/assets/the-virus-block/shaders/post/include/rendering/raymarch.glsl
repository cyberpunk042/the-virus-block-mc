// ═══════════════════════════════════════════════════════════════════════════
// RENDERING: RAYMARCHING
// ═══════════════════════════════════════════════════════════════════════════
// 
// Generic raymarching utilities and structures.
// The actual SDF-specific raymarchers are in their respective effect files.
//
// Include: #include "include/rendering/raymarch.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef RENDERING_RAYMARCH_GLSL
#define RENDERING_RAYMARCH_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCH RESULT TYPES
// ═══════════════════════════════════════════════════════════════════════════

// Extended hit information (more detailed than RaymarchHit in camera/types)
struct RaymarchResult {
    bool hit;              // Did we hit something?
    float distance;        // Distance along ray to hit
    vec3 position;         // World position of hit
    vec3 normal;           // Surface normal at hit
    float rim;             // Rim/fresnel factor (0 = facing, 1 = edge)
    int hitType;           // What we hit (effect-specific meaning)
    float corona;          // Corona glow for near-misses
};

// Initialize to "no hit" state
RaymarchResult noHit() {
    RaymarchResult r;
    r.hit = false;
    r.distance = -1.0;
    r.position = vec3(0.0);
    r.normal = vec3(0.0, 1.0, 0.0);
    r.rim = 0.0;
    r.hitType = 0;
    r.corona = 0.0;
    return r;
}

// ═══════════════════════════════════════════════════════════════════════════
// LIGHTING CALCULATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculate rim/fresnel factor.
 * 
 * @param normal     Surface normal
 * @param viewDir    Direction from surface to camera (or negated ray direction)
 * @param power      Rim falloff power (higher = sharper rim)
 * @return           Rim factor (0 = facing camera, 1 = edge)
 */
float calcRim(vec3 normal, vec3 viewDir, float power) {
    float rim = 1.0 - abs(dot(normal, viewDir));
    return pow(rim, power);
}

/**
 * Calculate simple diffuse lighting.
 * 
 * @param normal     Surface normal
 * @param lightDir   Direction TO light source
 * @return           Diffuse factor (0 = shadowed, 1 = fully lit)
 */
float calcDiffuse(vec3 normal, vec3 lightDir) {
    return max(0.0, dot(normal, lightDir));
}

/**
 * Calculate specular highlight (Blinn-Phong).
 * 
 * @param normal     Surface normal
 * @param viewDir    Direction to camera
 * @param lightDir   Direction to light
 * @param shininess  Specular power (higher = tighter highlight)
 */
float calcSpecular(vec3 normal, vec3 viewDir, vec3 lightDir, float shininess) {
    vec3 halfDir = normalize(viewDir + lightDir);
    return pow(max(0.0, dot(normal, halfDir)), shininess);
}

// ═══════════════════════════════════════════════════════════════════════════
// CORONA GLOW
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculate corona glow for near-misses.
 * Used when a ray passes close to but doesn't hit a surface.
 * 
 * @param nearestDist  Closest distance the ray came to a surface
 * @param coronaWidth  Width of the corona effect
 * @return             Corona intensity (0-1)
 */
float calcCorona(float nearestDist, float coronaWidth) {
    if (coronaWidth < NEAR_ZERO) return 0.0;
    if (nearestDist >= coronaWidth) return 0.0;
    return 1.0 - (nearestDist / coronaWidth);
}

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH CONVERSION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert ray hit distance to Z-depth (for depth comparison).
 * 
 * @param hitDist    Distance along ray to hit point
 * @param rayDir     Ray direction
 * @param forward    Camera forward direction
 * @return           Z-depth (distance along forward axis)
 */
float hitDistToZDepth(float hitDist, vec3 rayDir, vec3 forward) {
    return hitDist * dot(rayDir, forward);
}

/**
 * Check if a hit is occluded by scene geometry.
 * 
 * @param hitDist       Ray hit distance
 * @param rayDir        Ray direction
 * @param forward       Camera forward
 * @param sceneDepth    Linear depth of scene geometry
 * @return              True if hit is behind geometry
 */
bool isHitOccluded(float hitDist, vec3 rayDir, vec3 forward, float sceneDepth) {
    float hitZ = hitDistToZDepth(hitDist, rayDir, forward);
    return hitZ > sceneDepth;
}

// ═══════════════════════════════════════════════════════════════════════════
// STEP SIZE HELPERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Safe step size for raymarching.
 * Multiplies SDF distance by a safety factor to avoid overshooting.
 * 
 * @param sdfDist    Distance returned by SDF
 * @param factor     Safety factor (0.5-0.9 typical)
 * @return           Safe step size
 */
float safeStep(float sdfDist, float factor) {
    return sdfDist * factor;
}

/**
 * Adaptive step size based on distance traveled.
 * Allows larger steps when far from camera (where precision matters less).
 * 
 * @param sdfDist      Distance from SDF
 * @param traveled     Distance already traveled along ray
 * @param maxAdaptive  Maximum step multiplier at far distance
 */
float adaptiveStep(float sdfDist, float traveled, float maxAdaptive) {
    // Start with 0.8 safety, scale up to maxAdaptive at distance
    float factor = 0.8 + traveled * 0.001;  // Gradual increase
    factor = min(factor, maxAdaptive);
    return sdfDist * factor;
}

#endif // RENDERING_RAYMARCH_GLSL
