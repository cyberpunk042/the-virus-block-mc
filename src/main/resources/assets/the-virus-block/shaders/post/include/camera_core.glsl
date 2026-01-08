// ═══════════════════════════════════════════════════════════════════════════
// CAMERA CORE - Foundation for 3D Projection & Raymarching
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/camera_core.glsl"
//
// This library provides the fundamental camera operations that ALL rendering
// modes depend on. It ensures consistency across raymarching, screen-space
// projection, and depth-based world reconstruction.
//
// COORDINATE SPACES:
//   World    - Absolute Minecraft coordinates (meters/blocks)
//   View     - Camera-relative (origin at camera, Z = depth)
//   Clip     - Homogeneous coordinates after projection
//   NDC      - Normalized Device Coordinates (-1 to 1)
//   Screen   - Texture coordinates (0 to 1) or pixels
//
// CONVENTIONS:
//   - All vectors are normalized unless noted otherwise
//   - Forward points INTO the screen (positive Z in view space)
//   - Right is camera-right (positive X in view space)
//   - Up is camera-up (positive Y in view space)
//   - Aspect ratio = width / height
//   - FOV is vertical field of view in radians
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_CORE_GLSL
#define CAMERA_CORE_GLSL

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
    if (rightLen < 0.001) {
        // Fall back to world X axis
        right = vec3(1.0, 0.0, 0.0);
    } else {
        right = right / rightLen;
    }
    
    // Up is perpendicular to both forward and right
    // MUST normalize for correct ray generation
    outRight = right;
    outUp = normalize(cross(right, forward));
}

/**
 * Simplified version using default world up.
 */
void computeCameraBasisDefault(vec3 forward, out vec3 outRight, out vec3 outUp) {
    computeCameraBasis(forward, vec3(0.0, 1.0, 0.0), outRight, outUp);
}

// ═══════════════════════════════════════════════════════════════════════════
// RAY GENERATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Generates a ray direction for a screen pixel.
 * 
 * @param texCoord    Screen UV (0,0 = top-left, 1,1 = bottom-right)
 * @param forward     Camera forward direction (normalized)
 * @param right       Camera right direction (normalized)
 * @param up          Camera up direction (normalized)
 * @param fov         Vertical field of view in radians
 * @param aspectRatio Screen width / height
 * @return            Normalized ray direction in world space
 */
vec3 getRayDirection(vec2 texCoord, vec3 forward, vec3 right, vec3 up, 
                     float fov, float aspectRatio) {
    // Convert texCoord to NDC (-1 to 1)
    vec2 ndc = texCoord * 2.0 - 1.0;
    
    // Calculate frustum half-sizes at unit distance
    float halfHeight = tan(fov * 0.5);
    float halfWidth = halfHeight * aspectRatio;
    
    // Build ray direction: forward + screen offset
    vec3 rayDir = forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight);
    
    return normalize(rayDir);
}

/**
 * Generates ray direction with automatic basis calculation.
 * Convenience function for simpler usage.
 */
vec3 getRayDirectionAuto(vec2 texCoord, vec3 forward, float fov, float aspectRatio) {
    vec3 right, up;
    computeCameraBasisDefault(forward, right, up);
    return getRayDirection(texCoord, forward, right, up, fov, aspectRatio);
}

// ═══════════════════════════════════════════════════════════════════════════
// MATRIX-BASED RAY GENERATION (PREFERRED METHOD)
// ═══════════════════════════════════════════════════════════════════════════
// Using the inverse view-projection matrix is the STANDARD approach and
// guarantees correctness because it uses the exact same transformation
// that Minecraft uses for rendering.

/**
 * Generates a ray direction using the inverse view-projection matrix.
 * THIS IS THE PREFERRED METHOD - it uses the exact matrices Minecraft uses.
 * 
 * @param texCoord       Screen UV (0 to 1)
 * @param invViewProj    Inverse of (projection * view) matrix
 * @param camPos         Camera world position
 * @return               Normalized ray direction in world space
 */
vec3 getRayDirectionMatrix(vec2 texCoord, mat4 invViewProj, vec3 camPos) {
    // Convert to NDC (-1 to 1)
    vec2 ndc = texCoord * 2.0 - 1.0;
    
    // Point on the near plane in NDC (z = -1)
    vec4 clipNear = vec4(ndc, -1.0, 1.0);
    
    // Transform to world space
    vec4 worldNear = invViewProj * clipNear;
    worldNear /= worldNear.w;  // Perspective divide
    
    // Ray direction is from camera to this world point
    return normalize(worldNear.xyz - camPos);
}

/**
 * Reconstructs world position using the inverse view-projection matrix.
 * THIS IS THE PREFERRED METHOD for depth-based reconstruction.
 * 
 * @param texCoord       Screen UV (0 to 1)
 * @param depth          Raw depth buffer value (0 to 1)
 * @param invViewProj    Inverse of (projection * view) matrix
 * @return               World position
 */
vec3 screenToWorldMatrix(vec2 texCoord, float depth, mat4 invViewProj) {
    // Convert to NDC (-1 to 1)
    vec2 ndc = texCoord * 2.0 - 1.0;
    float ndcZ = depth * 2.0 - 1.0;
    
    // Clip space position
    vec4 clipPos = vec4(ndc, ndcZ, 1.0);
    
    // Transform to world space
    vec4 worldPos = invViewProj * clipPos;
    worldPos /= worldPos.w;  // Perspective divide
    
    return worldPos.xyz;
}

/**
 * Projects a world point to screen using the view-projection matrix.
 * THIS IS THE PREFERRED METHOD for screen-space projection.
 * 
 * @param worldPos    World position
 * @param viewProj    View-projection matrix (projection * view)
 * @return            vec3(screenUV.xy, depth)
 */
vec3 worldToScreenMatrix(vec3 worldPos, mat4 viewProj) {
    vec4 clipPos = viewProj * vec4(worldPos, 1.0);
    
    // Perspective divide
    vec3 ndc = clipPos.xyz / clipPos.w;
    
    // NDC to screen UV
    return vec3(
        ndc.x * 0.5 + 0.5,
        ndc.y * 0.5 + 0.5,
        ndc.z * 0.5 + 0.5  // Depth in 0-1 range
    );
}

// ═══════════════════════════════════════════════════════════════════════════
// COORDINATE TRANSFORMATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Transforms a world position to view space (camera-relative).
 * 
 * @param worldPos    Position in world coordinates
 * @param camPos      Camera position in world coordinates
 * @param forward     Camera forward direction
 * @param right       Camera right direction
 * @param up          Camera up direction
 * @return            Position in view space (x=right, y=up, z=depth)
 */
vec3 worldToView(vec3 worldPos, vec3 camPos, vec3 forward, vec3 right, vec3 up) {
    vec3 toPoint = worldPos - camPos;
    return vec3(
        dot(toPoint, right),    // How far right
        dot(toPoint, up),       // How far up
        dot(toPoint, forward)   // How far forward (depth)
    );
}

/**
 * Transforms view space position to NDC (-1 to 1).
 * 
 * @param viewPos     Position in view space
 * @param fov         Vertical FOV in radians
 * @param aspectRatio Screen width / height
 * @return            Position in NDC (x,y in -1..1, z = depth)
 */
vec3 viewToNDC(vec3 viewPos, float fov, float aspectRatio) {
    float tanHalfFov = tan(fov * 0.5);
    
    // Perspective division
    float ndcX = (viewPos.x / viewPos.z) / (tanHalfFov * aspectRatio);
    float ndcY = (viewPos.y / viewPos.z) / tanHalfFov;
    
    return vec3(ndcX, ndcY, viewPos.z);
}

/**
 * Transforms NDC to screen UV coordinates.
 * 
 * @param ndc    Position in NDC (-1 to 1)
 * @return       Screen UV (0 to 1), plus depth in z
 */
vec3 ndcToScreen(vec3 ndc) {
    return vec3(
        ndc.x * 0.5 + 0.5,
        ndc.y * 0.5 + 0.5,
        ndc.z
    );
}

/**
 * Full pipeline: world position to screen UV.
 * Returns vec3 where xy = screen UV, z = view depth.
 */
vec3 worldToScreen(vec3 worldPos, vec3 camPos, vec3 forward, vec3 right, vec3 up,
                   float fov, float aspectRatio) {
    vec3 viewPos = worldToView(worldPos, camPos, forward, right, up);
    vec3 ndc = viewToNDC(viewPos, fov, aspectRatio);
    return ndcToScreen(ndc);
}

/**
 * Convenience: world to screen with auto basis calculation.
 */
vec3 worldToScreenAuto(vec3 worldPos, vec3 camPos, vec3 forward, float fov, float aspectRatio) {
    vec3 right, up;
    computeCameraBasisDefault(forward, right, up);
    return worldToScreen(worldPos, camPos, forward, right, up, fov, aspectRatio);
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN TO WORLD (requires depth)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Reconstructs world position from screen UV and linear depth.
 * 
 * @param texCoord    Screen UV (0 to 1)
 * @param linearDepth Linear depth in world units (blocks/meters)
 * @param camPos      Camera world position
 * @param forward     Camera forward direction
 * @param right       Camera right direction
 * @param up          Camera up direction
 * @param fov         Vertical FOV in radians
 * @param aspectRatio Screen width / height
 * @return            World position
 */
vec3 screenToWorld(vec2 texCoord, float linearDepth, vec3 camPos,
                   vec3 forward, vec3 right, vec3 up, float fov, float aspectRatio) {
    // Get ray direction for this pixel
    vec3 rayDir = getRayDirection(texCoord, forward, right, up, fov, aspectRatio);
    
    // linearDepth is Z-distance (along forward), convert to ray-distance
    float rayDistance = linearDepth / dot(rayDir, forward);
    
    return camPos + rayDir * rayDistance;
}

/**
 * Convenience: screen to world with auto basis calculation.
 */
vec3 screenToWorldAuto(vec2 texCoord, float linearDepth, vec3 camPos, vec3 forward,
                       float fov, float aspectRatio) {
    vec3 right, up;
    computeCameraBasisDefault(forward, right, up);
    return screenToWorld(texCoord, linearDepth, camPos, forward, right, up, fov, aspectRatio);
}

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH UTILITIES
// Note: linearizeDepth() and isSkyDepth() are in depth_utils.glsl
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// PROJECTION UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculates apparent radius of a sphere on screen.
 * 
 * @param sphereRadius   Actual radius in world units
 * @param viewDepth      Distance from camera to sphere center
 * @param fov            Vertical FOV in radians
 * @return               Apparent radius in NDC units (screen fraction)
 */
float getApparentRadius(float sphereRadius, float viewDepth, float fov) {
    float tanHalfFov = tan(fov * 0.5);
    return (sphereRadius / viewDepth) / tanHalfFov;
}

/**
 * Checks if a world point is in front of the camera.
 */
bool isInFrontOfCamera(vec3 worldPos, vec3 camPos, vec3 forward) {
    return dot(worldPos - camPos, forward) > 0.0;
}

/**
 * Checks if a screen position is within visible bounds.
 */
bool isOnScreen(vec2 screenUV) {
    return screenUV.x >= 0.0 && screenUV.x <= 1.0 && 
           screenUV.y >= 0.0 && screenUV.y <= 1.0;
}

/**
 * Checks if NDC position is within visible bounds.
 */
bool isInNDCBounds(vec2 ndc) {
    return ndc.x >= -1.0 && ndc.x <= 1.0 && 
           ndc.y >= -1.0 && ndc.y <= 1.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// ASPECT RATIO UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Converts screen UV to aspect-corrected coordinates.
 * Makes coordinates square for circular effects.
 * 
 * @param screenUV      Screen UV (0 to 1)
 * @param aspectRatio   Screen width / height
 * @return              Aspect-corrected coordinates
 */
vec2 toAspectCorrected(vec2 screenUV, float aspectRatio) {
    vec2 centered = screenUV * 2.0 - 1.0;  // -1 to 1
    centered.x *= aspectRatio;              // Stretch X to make square
    return centered;
}

/**
 * Converts aspect-corrected coordinates back to screen UV.
 */
vec2 fromAspectCorrected(vec2 corrected, float aspectRatio) {
    corrected.x /= aspectRatio;
    return corrected * 0.5 + 0.5;
}

#endif // CAMERA_CORE_GLSL
