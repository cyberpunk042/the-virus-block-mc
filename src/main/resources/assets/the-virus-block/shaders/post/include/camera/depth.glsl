// ═══════════════════════════════════════════════════════════════════════════
// CAMERA: DEPTH BUFFER UTILITIES
// ═══════════════════════════════════════════════════════════════════════════
// 
// Functions for working with the depth buffer:
// - Linearizing depth values
// - Reconstructing world positions
// - Sky detection
//
// Include: #include "include/camera/depth.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl, camera/rays.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_DEPTH_GLSL
#define CAMERA_DEPTH_GLSL

#include "../core/constants.glsl"
#include "types.glsl"

// Note: rays.glsl is needed for world reconstruction, but we forward-declare
// the function to avoid circular dependency. Include rays.glsl before using
// reconstructWorldPos.

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Converts raw depth buffer value to linear depth (world units).
 * 
 * @param rawDepth  Raw depth buffer value (0-1)
 * @param near      Near plane distance
 * @param far       Far plane distance
 * @return          Linear depth in world units (blocks/meters)
 */
float linearizeDepth(float rawDepth, float near, float far) {
    // Convert depth buffer (0-1) to NDC Z (-1 to 1)
    float ndcZ = rawDepth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndcZ * (far - near));
}

/**
 * Convenience: linearize with CameraData.
 */
float linearizeDepth(float rawDepth, CameraData cam) {
    return linearizeDepth(rawDepth, cam.near, cam.far);
}

// ═══════════════════════════════════════════════════════════════════════════
// SKY DETECTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Checks if depth is at the far plane (sky/void).
 * 
 * @param rawDepth  Raw depth buffer value (0-1)
 * @return          True if this pixel is sky/void
 */
bool isSky(float rawDepth) {
    return rawDepth > 0.9999;
}

/**
 * Build a DepthInfo struct from raw depth.
 */
DepthInfo getDepthInfo(float rawDepth, float near, float far) {
    DepthInfo info;
    info.raw = rawDepth;
    info.linear = linearizeDepth(rawDepth, near, far);
    info.isSky = isSky(rawDepth);
    return info;
}

DepthInfo getDepthInfo(float rawDepth, CameraData cam) {
    return getDepthInfo(rawDepth, cam.near, cam.far);
}

// ═══════════════════════════════════════════════════════════════════════════
// WORLD POSITION RECONSTRUCTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Reconstructs world position from screen UV and linear depth.
 * Uses basis-based ray calculation.
 * 
 * @param texCoord     Screen UV (0 to 1)
 * @param linearDepth  Linear depth in world units
 * @param cam          Camera data
 * @return             World position
 */
vec3 reconstructWorldPos(vec2 texCoord, float linearDepth, CameraData cam) {
    // Calculate ray direction for this pixel
    vec2 ndc = texCoord * 2.0 - 1.0;
    float tanHalfFov = tan(cam.fov * 0.5);
    float halfWidth = tanHalfFov * cam.aspect;
    float halfHeight = tanHalfFov;
    
    vec3 rayDir = normalize(
        cam.forward + 
        cam.right * (ndc.x * halfWidth) + 
        cam.up * (ndc.y * halfHeight)
    );
    
    // linearDepth is Z-distance (along forward), convert to ray-distance
    float rayDistance = linearDepth / dot(rayDir, cam.forward);
    
    return cam.position + rayDir * rayDistance;
}

/**
 * Reconstructs world position using matrix method.
 * 
 * @param texCoord     Screen UV (0 to 1)
 * @param rawDepth     Raw depth buffer value (0-1)
 * @param invViewProj  Inverse view-projection matrix
 * @return             World position
 */
vec3 reconstructWorldPosMatrix(vec2 texCoord, float rawDepth, mat4 invViewProj) {
    // Convert to NDC
    vec2 ndcXY = texCoord * 2.0 - 1.0;
    float ndcZ = rawDepth * 2.0 - 1.0;
    
    // Clip space position
    vec4 clipPos = vec4(ndcXY, ndcZ, 1.0);
    
    // Transform to world space
    vec4 worldPos = invViewProj * clipPos;
    worldPos /= worldPos.w;  // Perspective divide
    
    return worldPos.xyz;
}

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH COMPARISON
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Check if an effect at effectDepth is occluded by scene geometry.
 * 
 * @param effectDepth   Depth of the effect (linear)
 * @param sceneDepth    Depth of scene geometry (linear)
 * @return              True if effect is behind geometry (occluded)
 */
bool isOccluded(float effectDepth, float sceneDepth) {
    return effectDepth > sceneDepth;
}

/**
 * Soft occlusion factor (for fading near geometry).
 * Returns 1.0 if fully visible, 0.0 if fully occluded.
 * 
 * @param effectDepth   Depth of the effect
 * @param sceneDepth    Depth of scene geometry
 * @param fadeWidth     Width of fade zone (world units)
 */
float softOcclusion(float effectDepth, float sceneDepth, float fadeWidth) {
    float diff = sceneDepth - effectDepth;
    if (diff < 0.0) return 0.0;  // Behind geometry
    return smoothstep(0.0, fadeWidth, diff);
}

#endif // CAMERA_DEPTH_GLSL
