// ═══════════════════════════════════════════════════════════════════════════
// FRAME UBO - Global Per-Frame Data
// ═══════════════════════════════════════════════════════════════════════════
// 
// Binding: 0 (see UBORegistry.FRAME_BINDING)
// Size: 16 bytes (1 vec4)
//
// This is the outermost layer of the UBO onion architecture.
// Contains data that is truly global and changes every frame.
//
// Usage:
//   #include "include/ubo/frame_ubo.glsl"
//   float t = FrameTimeUBO.x;
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef FRAME_UBO_GLSL
#define FRAME_UBO_GLSL

layout(std140) uniform FrameDataUBO {
    // x = time (accumulated seconds since game start)
    // y = deltaTime (seconds since last frame)
    // z = frameIndex (frame counter)
    // w = layoutVersion (1.0 = initial)
    vec4 FrameTimeUBO;
};

#endif // FRAME_UBO_GLSL
