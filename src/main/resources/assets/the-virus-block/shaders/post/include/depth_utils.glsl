// ═══════════════════════════════════════════════════════════════════════════
// DEPTH & WORLD POSITION UTILITIES
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/depth_utils.glsl"
// Requires: CameraX, CameraY, CameraZ, ForwardX, ForwardY, ForwardZ, Fov, AspectRatio uniforms

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

float linearizeDepth(float depth, float near, float far) {
    // Convert depth buffer (0-1) to NDC Z (-1 to 1) first
    float ndcZ = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndcZ * (far - near));
}

// ═══════════════════════════════════════════════════════════════════════════
// WORLD POSITION RECONSTRUCTION
// ═══════════════════════════════════════════════════════════════════════════

vec3 reconstructWorldPos(vec2 uv, float linearDepth) {
    // Get camera vectors
    vec3 camPos = vec3(CameraX, CameraY, CameraZ);
    vec3 forward = normalize(vec3(ForwardX, ForwardY, ForwardZ));
    
    // Compute camera's LOCAL right and up vectors
    // Note: Java side clamps pitch to ±89° to prevent exact gimbal lock
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    
    // Standard cross product - works because pitch is clamped
    vec3 right = normalize(cross(forward, worldUp));
    vec3 up = normalize(cross(right, forward));
    
    // Calculate ray direction from UV using perspective projection
    // NDC: (0,0) is top-left, (1,1) is bottom-right -> convert to (-1,1) range
    vec2 ndc = uv * 2.0 - 1.0;
    
    // Calculate half-sizes at unit distance from camera
    float halfFovTan = tan(Fov * 0.5);
    float halfHeight = halfFovTan;
    float halfWidth = halfFovTan * AspectRatio;
    
    // Build ray direction: forward + screen offset
    vec3 rayDir = forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight);
    // World position = camera + direction * distance
    // linearDepth is Z-distance (along forward), convert to ray-distance
    float rayDistance = linearDepth / dot(rayDir, forward);
    return camPos + rayDir * rayDistance;
}
