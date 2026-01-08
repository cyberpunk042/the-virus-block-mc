// ═══════════════════════════════════════════════════════════════════════════
// CAMERA: RAY GENERATION
// ═══════════════════════════════════════════════════════════════════════════
// 
// Functions for generating rays from screen coordinates.
// Two methods are provided for different situations:
//
// METHOD 1: BASIS-BASED (getRayFromBasis)
//   - Uses yaw/pitch-derived forward, right, up vectors
//   - STABLE when walking (no camera bob mismatch)
//   - Use when: Player is walking on ground
//
// METHOD 2: MATRIX-BASED (getRayFromMatrix) 
//   - Uses InvViewProj matrix directly from Minecraft
//   - ACCURATE at distance (uses exact rendered matrices)
//   - Use when: Player is flying, standing still
//
// ═══════════════════════════════════════════════════════════════════════════
// CRITICAL JAVA-SIDE REQUIREMENTS:
// ═══════════════════════════════════════════════════════════════════════════
//
// 1. FOV MUST be DYNAMIC (includes flying/sprinting changes):
//    - CORRECT: GameRendererAccessor.invokeGetFov(camera, tickDelta, true)
//    - WRONG:   client.options.getFov().getValue() (static settings value)
//
// 2. Camera data MUST be captured at RENDER TIME:
//    - Use WorldRenderer mixin injection point
//    - camera.getYaw(), camera.getPitch() -> forward vector
//    - camX, camY, camZ from render method locals
//
// 3. InvViewProj matrix MUST match the current frame:
//    - Compute from: Matrix4f(projectionMatrix).mul(positionMatrix).invert()
//    - Not from cached or previous frame values
//
// 4. ALL shaders (field_visual, shockwave) must use SAME data sources
//    to ensure consistent ray generation across effects
//
// ═══════════════════════════════════════════════════════════════════════════
//
// Include: #include "include/camera/rays.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl, camera/basis.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_RAYS_GLSL
#define CAMERA_RAYS_GLSL

#include "../core/constants.glsl"
#include "types.glsl"
#include "basis.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// METHOD 1: BASIS-BASED RAY GENERATION
// Stable when walking, uses yaw/pitch forward vector
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Generates a ray direction from camera basis vectors.
 * Use when player is walking on ground (stable, no camera bob mismatch).
 * 
 * @param texCoord    Screen UV (0,0 = top-left, 1,1 = bottom-right)
 * @param cam         Camera data with position, forward, right, up, fov, aspect
 * @return            Ray with origin and normalized direction
 */
Ray getRayFromBasis(vec2 texCoord, CameraData cam) {
    // Convert texCoord to NDC (-1 to 1)
    vec2 ndc = texCoord * 2.0 - 1.0;
    
    // Calculate frustum half-sizes at unit distance
    float tanHalfFov = tan(cam.fov * 0.5);
    float halfWidth = tanHalfFov * cam.aspect;
    float halfHeight = tanHalfFov;
    
    // Build ray direction: forward + screen offset
    vec3 direction = normalize(
        cam.forward + 
        cam.right * (ndc.x * halfWidth) + 
        cam.up * (ndc.y * halfHeight)
    );
    
    Ray ray;
    ray.origin = cam.position;
    ray.direction = direction;
    return ray;
}

/**
 * Returns just the direction (for backward compatibility).
 */
vec3 getRayDirection(vec2 texCoord, vec3 forward, vec3 right, vec3 up, 
                     float fov, float aspectRatio) {
    vec2 ndc = texCoord * 2.0 - 1.0;
    float halfHeight = tan(fov * 0.5);
    float halfWidth = halfHeight * aspectRatio;
    return normalize(forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight));
}

/**
 * Auto-compute basis from forward vector.
 */
vec3 getRayDirectionAuto(vec2 texCoord, vec3 forward, float fov, float aspectRatio) {
    vec3 right, up;
    computeCameraBasisDefault(forward, right, up);
    return getRayDirection(texCoord, forward, right, up, fov, aspectRatio);
}

// ═══════════════════════════════════════════════════════════════════════════
// METHOD 2: MATRIX-BASED RAY GENERATION
// Accurate at distance, uses exact Minecraft matrices
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Generates a ray from the inverse view-projection matrix.
 * Use when player is flying or standing still (accurate, no walking bob issue).
 * 
 * @param texCoord     Screen UV (0 to 1)
 * @param invViewProj  Inverse of (projection * view) matrix
 * @param camPos       Camera world position
 * @return             Ray with origin and normalized direction
 */
Ray getRayFromMatrix(vec2 texCoord, mat4 invViewProj, vec3 camPos) {
    // Convert to NDC (-1 to 1)
    vec2 ndc = texCoord * 2.0 - 1.0;
    
    // Points on near and far plane in clip space
    vec4 clipNear = vec4(ndc, -1.0, 1.0);
    vec4 clipFar = vec4(ndc, 1.0, 1.0);
    
    // Transform to world space
    vec4 worldNear = invViewProj * clipNear;
    vec4 worldFar = invViewProj * clipFar;
    
    worldNear /= worldNear.w;  // Perspective divide
    worldFar /= worldFar.w;
    
    Ray ray;
    ray.origin = camPos;
    ray.direction = normalize(worldFar.xyz - worldNear.xyz);
    return ray;
}

/**
 * Returns just the direction (for backward compatibility).
 */
vec3 getRayDirectionMatrix(vec2 texCoord, mat4 invViewProj, vec3 camPos) {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 clipNear = vec4(ndc, -1.0, 1.0);
    vec4 worldNear = invViewProj * clipNear;
    worldNear /= worldNear.w;
    return normalize(worldNear.xyz - camPos);
}

// ═══════════════════════════════════════════════════════════════════════════
// ADAPTIVE RAY GENERATION
// Automatically selects method based on player state
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Generates ray using the appropriate method based on IsFlying state.
 * 
 * @param texCoord     Screen UV (0 to 1)
 * @param cam          Camera data
 * @param invViewProj  Inverse view-projection matrix
 * @param isFlying     1.0 if player is flying, 0.0 otherwise
 * @return             Ray generated using appropriate method
 */
Ray getRayAdaptive(vec2 texCoord, CameraData cam, mat4 invViewProj, float isFlying) {
    if (isFlying > 0.5) {
        // Flying: use matrix method (accurate at distance, no walking bob)
        return getRayFromMatrix(texCoord, invViewProj, cam.position);
    } else {
        // Walking/standing: use basis method (stable, handles walking bob)
        return getRayFromBasis(texCoord, cam);
    }
}

#endif // CAMERA_RAYS_GLSL
