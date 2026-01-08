// ═══════════════════════════════════════════════════════════════════════════
// OCCLUSION UTILS - Depth and Visibility Calculations
// ═══════════════════════════════════════════════════════════════════════════
//
// Purpose:
// ─────────
// Provides consistent occlusion and visibility calculations for all effects.
// Handles the tricky math of when effects should be visible behind geometry.
//
// Key Features:
// ─────────
// - Z-depth calculation (proper depth buffer comparison)
// - Progressive occlusion bleed (effects partially visible behind geometry)
// - Sky detection
// - Proximity-based effects (blackout when near sun, etc.)
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef OCCLUSION_UTILS_GLSL
#define OCCLUSION_UTILS_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// OCCLUSION RESULT
// ═══════════════════════════════════════════════════════════════════════════

struct OcclusionResult {
    bool isFullyOccluded;  // True if completely hidden
    float visibility;      // 0.0 = hidden, 1.0 = fully visible
    float zDepth;          // Calculated Z-depth for this effect
    float distBehind;      // How far behind scene geometry (0 if in front)
};

// ═══════════════════════════════════════════════════════════════════════════
// CALCULATE OCCLUSION
// ═══════════════════════════════════════════════════════════════════════════
// Determines visibility of an effect relative to scene geometry.
//
// Uses V1's formula: zDepth = distance * dot(rayDir, forward)
// This projects the distance along camera's forward axis for depth comparison.
//
// Parameters:
//   effectDist   - Distance from camera to effect
//   rayDir       - Normalized ray direction for this pixel
//   forward      - Camera forward vector
//   sceneDepth   - Linearized scene depth from depth buffer
//   bleedRange   - How far behind geometry the effect is still partially visible
//   maxBleed     - Maximum visibility when behind geometry (0.7 = 70% max)
//
// Returns:
//   OcclusionResult with visibility information

OcclusionResult calculateOcclusion(
    float effectDist,
    vec3 rayDir,
    vec3 forward,
    float sceneDepth,
    float bleedRange,
    float maxBleed
) {
    OcclusionResult result;
    
    // Project effect distance along camera forward axis
    result.zDepth = effectDist * dot(rayDir, forward);
    
    // Is effect behind scene geometry?
    if (result.zDepth > sceneDepth) {
        // Effect is behind - calculate partial visibility
        result.distBehind = result.zDepth - sceneDepth;
        
        // Floor distance to avoid division issues at very close range
        result.distBehind = max(5.0, result.distBehind);
        
        // Linear falloff over bleedRange
        float rawVisibility = max(0.0, 1.0 - (result.distBehind / bleedRange));
        
        // Cap at maxBleed (so player is always visible)
        result.visibility = min(rawVisibility, maxBleed);
        result.isFullyOccluded = (result.visibility < 0.01);
    } else {
        // Effect is in front of scene - fully visible
        result.isFullyOccluded = false;
        result.visibility = 1.0;
        result.distBehind = 0.0;
    }
    
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════
// SIMPLE OCCLUSION
// ═══════════════════════════════════════════════════════════════════════════
// Quick check without progressive bleed.
// Returns 1.0 if visible, 0.0 if occluded.

float simpleOcclusion(float effectDist, vec3 rayDir, vec3 forward, float sceneDepth) {
    float zDepth = effectDist * dot(rayDir, forward);
    return zDepth > sceneDepth ? 0.0 : 1.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// PROGRESSIVE BLEED (Standalone)
// ═══════════════════════════════════════════════════════════════════════════
// Returns visibility factor for effects behind geometry.
// Call after determining the effect IS behind geometry.
//
// Parameters:
//   distBehind  - How far behind scene (zDepth - sceneDepth)
//   bleedRange  - Distance over which visibility fades (e.g., 500 blocks)
//   maxBleed    - Maximum visibility when behind (e.g., 0.7)

float occlusionBleed(float distBehind, float bleedRange, float maxBleed) {
    distBehind = max(5.0, distBehind);  // Floor to avoid artifacts
    float rawVisibility = max(0.0, 1.0 - (distBehind / bleedRange));
    return min(rawVisibility, maxBleed);
}

// ═══════════════════════════════════════════════════════════════════════════
// PROXIMITY BLACKOUT
// ═══════════════════════════════════════════════════════════════════════════
// Darkens scene as camera approaches a large effect (like a sun).
// Used to prevent overwhelming brightness at close range.
//
// Parameters:
//   distToCenter - Distance from camera to effect center
//   blackoutRange - Distance at which blackout starts fading
//   blackoutPower - Falloff curve (0.1=fast at edge, 1=linear, 2=slow)
//   blackoutMax   - Maximum darkening (0-1, 1=complete black)
//
// Returns:
//   Darkening factor to multiply scene color by (1.0 = no change, 0.0 = black)

float proximityBlackout(
    float distToCenter, 
    float blackoutRange, 
    float blackoutPower,
    float blackoutMax
) {
    // Normalize distance: 0 at center, 1 at range edge
    float normalizedDist = clamp(distToCenter / blackoutRange, 0.0, 1.0);
    
    // Proximity factor: 1 at center, 0 at edge
    float proximityFactor = pow(1.0 - normalizedDist, blackoutPower);
    
    // Darkening amount
    float darkening = proximityFactor * blackoutMax;
    
    // Return multiplier for scene color
    return 1.0 - darkening;
}

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Check if pixel is sky (depth buffer reads ~1.0 for sky).
 */
bool isSky(float rawDepth) {
    return rawDepth > 0.9999;
}

/**
 * Get effective scene depth (very far for sky).
 */
float getEffectiveSceneDepth(float linearDepth, bool isSky) {
    return isSky ? 10000.0 : linearDepth;
}

#endif // OCCLUSION_UTILS_GLSL
