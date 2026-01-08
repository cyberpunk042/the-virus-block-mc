// ═══════════════════════════════════════════════════════════════════════════
// SHADER LIBRARY: ALL-IN-ONE INCLUDE
// ═══════════════════════════════════════════════════════════════════════════
// 
// Single file that includes the entire modular shader library.
// Use this for quick integration testing before selective includes.
//
// Include: #include "include/shader_library.glsl"
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SHADER_LIBRARY_GLSL
#define SHADER_LIBRARY_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// LAYER 1: CORE (Foundation)
// ═══════════════════════════════════════════════════════════════════════════

#include "core/constants.glsl"
#include "core/math_utils.glsl"
#include "core/color_utils.glsl"
#include "core/noise_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LAYER 2: CAMERA (Space Transformations)
// ═══════════════════════════════════════════════════════════════════════════

#include "camera/types.glsl"
#include "camera/basis.glsl"
#include "camera/rays.glsl"
#include "camera/projection.glsl"
#include "camera/depth.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LAYER 3: SDF (Geometry)
// ═══════════════════════════════════════════════════════════════════════════

#include "sdf/primitives.glsl"
#include "sdf/operations.glsl"
#include "sdf/orbital_system.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LAYER 4: RENDERING (Techniques)
// ═══════════════════════════════════════════════════════════════════════════

#include "rendering/raymarch.glsl"
#include "rendering/screen_effects.glsl"
#include "rendering/depth_mask.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LAYER 5: EFFECTS (Complete Implementations)
// ═══════════════════════════════════════════════════════════════════════════

#include "effects/energy_orb_types.glsl"
#include "effects/energy_orb_v1.glsl"
#include "effects/orbitals_v1.glsl"
#include "effects/orbitals_v2.glsl"
#include "effects/shockwave_ring.glsl"

#endif // SHADER_LIBRARY_GLSL
