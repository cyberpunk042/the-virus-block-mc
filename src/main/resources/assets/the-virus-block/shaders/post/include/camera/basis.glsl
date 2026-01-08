// ═══════════════════════════════════════════════════════════════════════════
// CAMERA: BASIS CALCULATION
// ═══════════════════════════════════════════════════════════════════════════
// 
// Functions for computing camera orientation (right, up vectors) from a
// forward direction. Handles gimbal lock edge cases.
//
// Include: #include "include/camera/basis.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_BASIS_GLSL
#define CAMERA_BASIS_GLSL

#include "../core/constants.glsl"
#include "types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// CAMERA BASIS CALCULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Computes orthonormal camera basis vectors from a forward direction.
 * 
 * @param forward    Normalized camera look direction
 * @param worldUp    World up vector (typically vec3(0,1,0))
 * @param outRight   Output: camera right vector
 * @param outUp      Output: camera up vector (orthogonal to forward)
 * 
 * Handles gimbal lock when forward is nearly parallel to worldUp.
 */
void computeCameraBasis(vec3 forward, vec3 worldUp, out vec3 outRight, out vec3 outUp) {
    // Calculate right vector via cross product
    vec3 right = cross(forward, worldUp);
    float rightLen = length(right);
    
    // Handle gimbal lock: forward nearly parallel to worldUp
    if (rightLen < NEAR_ZERO) {
        // Fall back to world X axis
        right = vec3(1.0, 0.0, 0.0);
    } else {
        right = right / rightLen;
    }
    
    // Up is perpendicular to both forward and right
    outRight = right;
    outUp = normalize(cross(right, forward));
}

/**
 * Simplified version using default world up (0, 1, 0).
 */
void computeCameraBasisDefault(vec3 forward, out vec3 outRight, out vec3 outUp) {
    computeCameraBasis(forward, vec3(0.0, 1.0, 0.0), outRight, outUp);
}

// ═══════════════════════════════════════════════════════════════════════════
// CAMERA DATA BUILDERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Build a complete CameraData struct from basic inputs.
 * Computes right and up vectors automatically.
 */
CameraData buildCameraData(
    vec3 position,
    vec3 forward,
    float fov,
    float aspect,
    float near,
    float far
) {
    CameraData cam;
    cam.position = position;
    cam.forward = normalize(forward);
    computeCameraBasisDefault(cam.forward, cam.right, cam.up);
    cam.fov = fov;
    cam.aspect = aspect;
    cam.near = near;
    cam.far = far;
    return cam;
}

/**
 * Build CameraData with explicit right and up vectors.
 * Use when you have pre-computed basis vectors.
 */
CameraData buildCameraDataExplicit(
    vec3 position,
    vec3 forward,
    vec3 right,
    vec3 up,
    float fov,
    float aspect,
    float near,
    float far
) {
    CameraData cam;
    cam.position = position;
    cam.forward = forward;
    cam.right = right;
    cam.up = up;
    cam.fov = fov;
    cam.aspect = aspect;
    cam.near = near;
    cam.far = far;
    return cam;
}

#endif // CAMERA_BASIS_GLSL
