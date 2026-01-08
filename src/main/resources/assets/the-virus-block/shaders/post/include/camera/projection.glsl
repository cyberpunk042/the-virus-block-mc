// ═══════════════════════════════════════════════════════════════════════════
// CAMERA: PROJECTION
// ═══════════════════════════════════════════════════════════════════════════
// 
// World-to-screen projection functions for V2 (top-down) rendering.
// Projects 3D positions and spheres to 2D screen coordinates.
//
// Include: #include "include/camera/projection.glsl"
// Prerequisites: core/constants.glsl, camera/types.glsl, camera/basis.glsl
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_PROJECTION_GLSL
#define CAMERA_PROJECTION_GLSL

#include "../core/constants.glsl"
#include "types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// COORDINATE TRANSFORMATIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Transforms a world position to view space (camera-relative).
 * 
 * @param worldPos  Position in world coordinates
 * @param cam       Camera data
 * @return          Position in view space (x=right, y=up, z=depth)
 */
vec3 worldToView(vec3 worldPos, CameraData cam) {
    vec3 toPoint = worldPos - cam.position;
    return vec3(
        dot(toPoint, cam.right),    // How far right
        dot(toPoint, cam.up),       // How far up
        dot(toPoint, cam.forward)   // How far forward (depth)
    );
}

/**
 * Transforms view space position to NDC (-1 to 1).
 * 
 * @param viewPos   Position in view space
 * @param cam       Camera data (needs fov, aspect)
 * @return          Position in NDC (x,y in -1..1, z = depth)
 */
vec3 viewToNDC(vec3 viewPos, CameraData cam) {
    float tanHalfFov = tan(cam.fov * 0.5);
    
    // Behind camera check
    if (viewPos.z < cam.near) {
        return vec3(0.0, 0.0, -1.0);  // Invalid
    }
    
    // Perspective division
    float ndcX = (viewPos.x / viewPos.z) / (tanHalfFov * cam.aspect);
    float ndcY = (viewPos.y / viewPos.z) / tanHalfFov;
    
    return vec3(ndcX, ndcY, viewPos.z);
}

/**
 * Transforms NDC to screen UV coordinates.
 * 
 * @param ndc   Position in NDC (-1 to 1)
 * @return      Screen UV (0 to 1), plus depth in z
 */
vec3 ndcToScreen(vec3 ndc) {
    return vec3(
        ndc.x * 0.5 + 0.5,
        ndc.y * 0.5 + 0.5,
        ndc.z
    );
}

/**
 * Screen UV to NDC.
 */
vec2 screenToNDC(vec2 screenUV) {
    return screenUV * 2.0 - 1.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// WORLD TO SCREEN (full pipeline)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Projects a world position to screen UV.
 * Returns vec3 where xy = screen UV (0-1), z = view depth.
 * 
 * Use this for V2 screen-space rendering.
 */
vec3 worldToScreen(vec3 worldPos, CameraData cam) {
    vec3 viewPos = worldToView(worldPos, cam);
    vec3 ndc = viewToNDC(viewPos, cam);
    return ndcToScreen(ndc);
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN TO WORLD (requires depth)
// ═══════════════════════════════════════════════════════════════════════════

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
 * Reconstructs world position from screen UV and linear depth.
 * Uses basis vectors (yaw/pitch derived) for stability when walking.
 * 
 * @param texCoord    Screen UV (0 to 1)
 * @param linearDepth Linear depth in world units (blocks/meters)
 * @param camPos      Camera world position
 * @param forward     Camera forward direction
 * @param fov         Vertical FOV in radians
 * @param aspectRatio Screen width / height
 * @return            World position
 */
vec3 screenToWorldAuto(vec2 texCoord, float linearDepth, vec3 camPos, vec3 forward,
                       float fov, float aspectRatio) {
    // Compute camera basis
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 right = cross(forward, worldUp);
    float rightLen = length(right);
    if (rightLen < 0.001) {
        right = vec3(1.0, 0.0, 0.0);
    } else {
        right = right / rightLen;
    }
    vec3 up = normalize(cross(right, forward));
    
    // Get ray direction for this pixel
    vec2 ndc = texCoord * 2.0 - 1.0;
    float halfHeight = tan(fov * 0.5);
    float halfWidth = halfHeight * aspectRatio;
    vec3 rayDir = normalize(forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight));
    
    // linearDepth is Z-distance (along forward), convert to ray-distance
    float rayDistance = linearDepth / dot(rayDir, forward);
    
    return camPos + rayDir * rayDistance;
}

/**
 * Projects using view-projection matrix (alternative method).
 * Use when you have the matrix available.
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
// SPHERE PROJECTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Calculates apparent radius of a sphere on screen.
 * 
 * @param sphereRadius   Actual radius in world units
 * @param viewDepth      Distance from camera to sphere center
 * @param fov            Vertical FOV in radians
 * @return               Apparent radius in screen UV units
 */
float getApparentRadius(float sphereRadius, float viewDepth, float fov) {
    if (viewDepth < NEAR_ZERO) return 1.0;  // Very close, fill screen
    float tanHalfFov = tan(fov * 0.5);
    return (sphereRadius / viewDepth) / tanHalfFov;
}

/**
 * Projects a 3D sphere to screen space.
 * Use for V2 orb rendering.
 * 
 * @param sphereCenter   Sphere center in world coordinates
 * @param sphereRadius   Sphere radius in world units
 * @param cam            Camera data
 * @return               SphereProjection with screen center, apparent radius, etc.
 */
SphereProjection projectSphere(vec3 sphereCenter, float sphereRadius, CameraData cam) {
    SphereProjection result;
    
    // Project center to view space
    vec3 viewPos = worldToView(sphereCenter, cam);
    
    // Behind camera check
    if (viewPos.z < cam.near) {
        result.isVisible = false;
        result.screenCenter = vec2(0.5);
        result.apparentRadius = 0.0;
        result.viewDepth = viewPos.z;
        return result;
    }
    
    // Convert to NDC and screen
    vec3 ndc = viewToNDC(viewPos, cam);
    vec3 screen = ndcToScreen(ndc);
    
    result.screenCenter = screen.xy;
    result.viewDepth = viewPos.z;
    result.apparentRadius = getApparentRadius(sphereRadius, viewPos.z, cam.fov);
    result.isVisible = true;
    
    return result;
}

/**
 * Calculate distance from a screen pixel to a projected sphere center.
 * Corrects for aspect ratio.
 * 
 * @param texCoord    Current pixel's screen UV
 * @param proj        Sphere projection result
 * @param aspect      Aspect ratio (width/height)
 * @return            Distance in screen UV units
 */
float distToProjectedCenter(vec2 texCoord, SphereProjection proj, float aspect) {
    vec2 delta = texCoord - proj.screenCenter;
    delta.x *= aspect;  // Correct for aspect ratio
    return length(delta);
}

/**
 * Normalized distance (0 at center, 1 at edge of sphere).
 */
float normalizedDistToProjected(vec2 texCoord, SphereProjection proj, float aspect) {
    return distToProjectedCenter(texCoord, proj, aspect) / max(proj.apparentRadius, NEAR_ZERO);
}

#endif // CAMERA_PROJECTION_GLSL
