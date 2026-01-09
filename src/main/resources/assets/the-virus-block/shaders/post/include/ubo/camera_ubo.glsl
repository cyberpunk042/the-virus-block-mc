// ═══════════════════════════════════════════════════════════════════════════
// CAMERA UBO - View Definition and Matrices
// ═══════════════════════════════════════════════════════════════════════════
// 
// Binding: 1 (see UBORegistry.CAMERA_BINDING)
// Size: 224 bytes (4 vec4 + 2 mat4 + 2 reserved vec4)
//
// Contains camera position, orientation, projection parameters, and matrices.
// Layout MUST match CameraUBO.java exactly!
//
// Usage:
//   #include "include/ubo/camera_ubo.glsl"
//   vec3 camPos = CameraPositionUBO.xyz;
//   float aspect = CameraForwardUBO.w;
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_UBO_GLSL
#define CAMERA_UBO_GLSL

layout(std140) uniform CameraDataUBO {
    // vec4 0: Camera world position (xyz), reserved (w)
    vec4 CameraPositionUBO;
    
    // vec4 1: Camera forward direction (xyz), aspect ratio (w)
    vec4 CameraForwardUBO;
    
    // vec4 2: Camera up direction (xyz), fov in radians (w)
    vec4 CameraUpUBO;
    
    // vec4 3: near (x), far (y), isFlying (z), reserved (w)
    vec4 CameraClipUBO;
    
    // mat4 (vec4 4-7): View-Projection matrix
    mat4 ViewProjUBO;
    
    // mat4 (vec4 8-11): Inverse View-Projection matrix
    mat4 InvViewProjUBO;
    
    // vec4 12-13: Reserved for future (e.g., PrevViewProj)
    vec4 CameraReserved1UBO;
    vec4 CameraReserved2UBO;
};

#endif // CAMERA_UBO_GLSL
