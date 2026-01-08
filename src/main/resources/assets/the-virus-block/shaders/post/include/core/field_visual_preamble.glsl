// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL PREAMBLE - Common Main() Setup
// 
// This file provides macros and helpers to eliminate boilerplate in standalone
// shaders. Each shader's main() previously had 30+ lines of identical setup.
// Now: include this file and call FIELD_VISUAL_PREAMBLE() in main().
//
// Usage in standalone shader:
// ═══════════════════════════════════════════════════════════════════════════
// void main() {
//     FIELD_VISUAL_PREAMBLE();  // Sets up all common variables
//     
//     // Now you have access to:
//     // - sceneColor (vec4) - sampled scene
//     // - rawDepth (float) - raw depth buffer value  
//     // - isSky (bool) - true if fragment is sky
//     // - linearDist (float) - linearized scene distance
//     // - sceneDepth (float) - isSky ? 10000.0 : linearDist
//     // - camPos (vec3) - camera world position
//     // - forward (vec3) - camera forward direction
//     // - worldUp (vec3) - (0, 1, 0)
//     // - cam (CameraData) - full camera struct
//     // - ray (Ray) - per-pixel ray direction
//     // - field (FieldData) - all UBO parameters
//     // - sphereCenter (vec3) - effect center position
//     // - sphereRadius (float) - effect radius
//     //
//     // Then render your effect and call FIELD_VISUAL_COMPOSITE(fieldEffect);
// }
// ═══════════════════════════════════════════════════════════════════════════

#ifndef FIELD_VISUAL_PREAMBLE_GLSL
#define FIELD_VISUAL_PREAMBLE_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// PREAMBLE MACRO - Defines all common variables
// ═══════════════════════════════════════════════════════════════════════════

#define FIELD_VISUAL_PREAMBLE() vec4 sceneColor = texture(InSampler, texCoord); float rawDepth = texture(DepthSampler, texCoord).r; bool isSky = (rawDepth > 0.9999); float linearDist = linearizeDepth(rawDepth, NearPlane, FarPlane); float sceneDepth = isSky ? 10000.0 : linearDist; vec3 camPos = vec3(CameraX, CameraY, CameraZ); vec3 forward = normalize(vec3(ForwardX, ForwardY, ForwardZ)); vec3 worldUp = vec3(0.0, 1.0, 0.0); CameraData cam = buildCameraData(camPos, forward, Fov, AspectRatio, NearPlane, FarPlane); Ray ray = getRayAdaptive(texCoord, cam, InvViewProj, IsFlying); FieldData field = buildFieldDataFromUBO(); vec3 sphereCenter = field.CenterAndRadius.xyz; float sphereRadius = field.CenterAndRadius.w

// ═══════════════════════════════════════════════════════════════════════════
// EARLY EXIT MACRO - Check effect type and exit if mismatch
// ═══════════════════════════════════════════════════════════════════════════

#define FIELD_VISUAL_CHECK_EFFECT_TYPE(expectedType) int effectType = int(EffectType); if (effectType != expectedType) { fragColor = sceneColor; return; }

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH OCCLUSION MACRO - Standard sphere occlusion check
// ═══════════════════════════════════════════════════════════════════════════

#define FIELD_VISUAL_DEPTH_OCCLUSION() vec3 toSphere = sphereCenter - camPos; float sphereZDepth = dot(toSphere, forward); float sphereNearestZ = sphereZDepth - sphereRadius; if (!isSky && sphereNearestZ > linearDist) { fragColor = sceneColor; return; }

// ═══════════════════════════════════════════════════════════════════════════
// COMPOSITE MACRO - Final compositing with early exit
// ═══════════════════════════════════════════════════════════════════════════

#define FIELD_VISUAL_COMPOSITE(fieldEffect) if (fieldEffect.a < 0.001) { fragColor = sceneColor; return; } vec3 finalColor = compositeFieldEffect(sceneColor.rgb, fieldEffect); fragColor = vec4(finalColor, 1.0)

// ═══════════════════════════════════════════════════════════════════════════
// COMBINED MACRO - Full setup + effect type check + occlusion
// ═══════════════════════════════════════════════════════════════════════════

#define FIELD_VISUAL_FULL_PREAMBLE(expectedType) FIELD_VISUAL_PREAMBLE(); FIELD_VISUAL_CHECK_EFFECT_TYPE(expectedType); FIELD_VISUAL_DEPTH_OCCLUSION()

#endif // FIELD_VISUAL_PREAMBLE_GLSL
