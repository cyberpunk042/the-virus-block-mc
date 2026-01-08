// ═══════════════════════════════════════════════════════════════════════════
// RENDERING: DEPTH MASKING
// ═══════════════════════════════════════════════════════════════════════════
// 
// Depth-based occlusion and masking for effects.
// Handles both V1 (raymarched) and V2 (screen-projected) effects.
//
// Include: #include "include/rendering/depth_mask.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef RENDERING_DEPTH_MASK_GLSL
#define RENDERING_DEPTH_MASK_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// BASIC OCCLUSION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Check if effect is occluded by scene geometry (hard).
 * 
 * @param effectDepth   Depth of the effect
 * @param sceneDepth    Depth of scene geometry
 * @param bias          Small offset to prevent z-fighting
 * @return              True if effect is behind geometry
 */
bool isOccludedHard(float effectDepth, float sceneDepth, float bias) {
    return effectDepth > (sceneDepth + bias);
}

/**
 * Occlusion with soft fade.
 * Returns visibility factor (0 = fully occluded, 1 = fully visible).
 * 
 * @param effectDepth   Depth of the effect
 * @param sceneDepth    Depth of scene geometry
 * @param fadeWidth     Width of fade zone
 */
float occlusionSoft(float effectDepth, float sceneDepth, float fadeWidth) {
    float diff = sceneDepth - effectDepth;
    
    if (diff < 0.0) return 0.0;  // Behind geometry
    if (diff > fadeWidth) return 1.0;  // Fully visible
    
    return smoothstep(0.0, fadeWidth, diff);
}

// ═══════════════════════════════════════════════════════════════════════════
// V1 (RAYMARCHED) DEPTH MASKING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert ray hit distance to comparable depth.
 * 
 * @param hitDist    Distance along ray
 * @param rayDir     Ray direction
 * @param forward    Camera forward
 * @return           Z-depth comparable to linear scene depth
 */
float rayDistToLinearDepth(float hitDist, vec3 rayDir, vec3 forward) {
    return hitDist * dot(rayDir, forward);
}

/**
 * Check if raymarched hit is visible.
 * 
 * @param hitDist       Distance along ray to hit
 * @param rayDir        Ray direction
 * @param forward       Camera forward
 * @param sceneDepth    Linear scene depth
 * @return              Visibility (0 = occluded, 1 = visible)
 */
float raymarchVisibility(float hitDist, vec3 rayDir, vec3 forward, float sceneDepth) {
    float hitZ = rayDistToLinearDepth(hitDist, rayDir, forward);
    return hitZ <= sceneDepth ? 1.0 : 0.0;
}

/**
 * Soft visibility for raymarched effects.
 */
float raymarchVisibilitySoft(float hitDist, vec3 rayDir, vec3 forward, 
                             float sceneDepth, float fadeWidth) {
    float hitZ = rayDistToLinearDepth(hitDist, rayDir, forward);
    return occlusionSoft(hitZ, sceneDepth, fadeWidth);
}

// ═══════════════════════════════════════════════════════════════════════════
// V2 (SCREEN-PROJECTED) DEPTH MASKING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Visibility for screen-projected effects.
 * Uses the sphere's view-space depth.
 * 
 * @param proj          Sphere projection data
 * @param sceneDepth    Linear scene depth at pixel
 * @param fadeWidth     Width of fade zone
 */
float projectedVisibility(SphereProjection proj, float sceneDepth, float fadeWidth) {
    return occlusionSoft(proj.viewDepth, sceneDepth, fadeWidth);
}

/**
 * Per-pixel depth check for projected effects.
 * Samples approximate effect depth based on distance from center.
 * 
 * @param texCoord          Current pixel UV
 * @param proj              Sphere projection
 * @param sceneDepth        Linear scene depth at pixel
 * @param aspect            Aspect ratio
 * @param fadeWidth         Fade width
 */
float projectedVisibilityPerPixel(vec2 texCoord, SphereProjection proj, 
                                   float sceneDepth, float aspect, float fadeWidth) {
    // Approximate depth varies across projected sphere
    // Closer to center = closer to camera, edges = deeper
    float normalizedDist = length((texCoord - proj.screenCenter) * vec2(aspect, 1.0)) 
                          / max(proj.apparentRadius, NEAR_ZERO);
    
    // Simple approximation: depth increases at edges
    float pixelDepth = proj.viewDepth * (1.0 + normalizedDist * 0.1);
    
    return occlusionSoft(pixelDepth, sceneDepth, fadeWidth);
}

// ═══════════════════════════════════════════════════════════════════════════
// SKY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get scene depth, treating sky as very far.
 * 
 * @param linearDepth   Linear scene depth
 * @param isSky         True if pixel is sky
 * @param skyDepth      Depth to use for sky (typically 10000)
 */
float getEffectiveSceneDepth(float linearDepth, bool isSky, float skyDepth) {
    return isSky ? skyDepth : linearDepth;
}

/**
 * Combined sky check and scene depth.
 */
float getEffectiveSceneDepth(DepthInfo depth, float skyDepth) {
    return depth.isSky ? skyDepth : depth.linear;
}

// ═══════════════════════════════════════════════════════════════════════════
// CORONA / GLOW DEPTH HANDLING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Visibility for corona/glow effects.
 * Corona extends slightly in front of the object center.
 * 
 * @param objectDepth   Depth of object center
 * @param sceneDepth    Scene depth
 * @param coronaExtend  How far corona extends in front (world units)
 */
float coronaVisibility(float objectDepth, float sceneDepth, float coronaExtend) {
    // Corona starts earlier than object
    float coronaDepth = objectDepth - coronaExtend;
    
    if (coronaDepth > sceneDepth) return 0.0;  // Fully behind
    if (objectDepth <= sceneDepth) return 1.0;  // Object visible, so is corona
    
    // Partial: object occluded but corona visible
    return (sceneDepth - coronaDepth) / coronaExtend;
}

#endif // RENDERING_DEPTH_MASK_GLSL
