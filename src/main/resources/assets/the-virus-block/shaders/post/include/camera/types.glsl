// ═══════════════════════════════════════════════════════════════════════════
// CAMERA: TYPE DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════════
// 
// Core data structures used for camera operations, ray casting, and projection.
// These structs provide a clean interface between different camera functions.
//
// Include: #include "include/camera/types.glsl"
// Prerequisites: None (self-contained)
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CAMERA_TYPES_GLSL
#define CAMERA_TYPES_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// CAMERA DATA
// Complete camera state for rendering
// ═══════════════════════════════════════════════════════════════════════════

struct CameraData {
    vec3 position;    // Camera position (usually 0,0,0 in camera-relative space)
    vec3 forward;     // Normalized forward direction (into screen)
    vec3 right;       // Normalized right direction
    vec3 up;          // Normalized up direction
    float fov;        // Vertical field of view in RADIANS
    float aspect;     // Aspect ratio (width / height)
    float near;       // Near plane distance
    float far;        // Far plane distance
};

// ═══════════════════════════════════════════════════════════════════════════
// RAY
// For raymarching and ray-based calculations
// ═══════════════════════════════════════════════════════════════════════════

struct Ray {
    vec3 origin;      // Ray starting point
    vec3 direction;   // Normalized direction
};

// ═══════════════════════════════════════════════════════════════════════════
// SPHERE PROJECTION
// Result of projecting a 3D sphere to screen space (for V2 rendering)
// ═══════════════════════════════════════════════════════════════════════════

struct SphereProjection {
    vec2 screenCenter;     // Center position in screen UV (0-1)
    float apparentRadius;  // Radius in screen UV units (accounts for perspective)
    float viewDepth;       // Distance from camera to sphere center
    bool isVisible;        // True if sphere is in front of camera and on screen
};

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCH HIT
// Result of raymarching against geometry
// ═══════════════════════════════════════════════════════════════════════════

struct RaymarchHit {
    bool hit;              // Did ray hit something?
    float distance;        // Distance along ray to hit point
    vec3 position;         // World position of hit
    vec3 normal;           // Surface normal at hit point
    float rim;             // Rim/fresnel factor (0 = facing camera, 1 = edge)
    int hitType;           // What was hit (0 = primary, 1 = secondary, etc.)
};

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH INFO
// Processed depth buffer information
// ═══════════════════════════════════════════════════════════════════════════

struct DepthInfo {
    float raw;             // Raw depth buffer value (0-1)
    float linear;          // Linearized depth (world units)
    bool isSky;            // True if at far plane (sky/void)
};

#endif // CAMERA_TYPES_GLSL
