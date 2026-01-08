// ═══════════════════════════════════════════════════════════════════════════
// CORE: CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════
// 
// Shared constants used across the shader library.
// This file has NO dependencies - it's the foundation of everything.
//
// Include: #include "include/core/constants.glsl"
// Prerequisites: None
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORE_CONSTANTS_GLSL
#define CORE_CONSTANTS_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// NUMERICAL PRECISION
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EPSILON
#define EPSILON 0.0001
#endif
#ifndef NEAR_ZERO
#define NEAR_ZERO 0.001
#endif

// Raymarching precision
#ifndef RAYMARCH_EPSILON
#define RAYMARCH_EPSILON 0.05
#endif
#ifndef MAX_RAYMARCH_STEPS
#define MAX_RAYMARCH_STEPS 48
#endif

// ═══════════════════════════════════════════════════════════════════════════
// MATHEMATICAL CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

#ifndef PI
#define PI 3.14159265359
#endif
#ifndef TWO_PI
#define TWO_PI 6.28318530718
#endif
#ifndef HALF_PI
#define HALF_PI 1.57079632679
#endif
#ifndef TAU
#define TAU TWO_PI
#endif

// ═══════════════════════════════════════════════════════════════════════════
// SHAPE TYPE IDENTIFIERS
// Used by shockwave and other multi-shape effects
// ═══════════════════════════════════════════════════════════════════════════

#define SHAPE_POINT 0
#define SHAPE_SPHERE 1
#define SHAPE_TORUS 2
#define SHAPE_POLYGON 3
#define SHAPE_ORBITAL 4

// ═══════════════════════════════════════════════════════════════════════════
// EFFECT TYPE IDENTIFIERS
// Used by field_visual to select rendering mode
// ═══════════════════════════════════════════════════════════════════════════

#define EFFECT_NONE 0
#define EFFECT_ENERGY_ORB 1
#define EFFECT_SHIELD 2
#define EFFECT_AURA 3
#define EFFECT_PORTAL 4

// ═══════════════════════════════════════════════════════════════════════════
// RENDER VERSION IDENTIFIERS
// ═══════════════════════════════════════════════════════════════════════════

#define RENDER_V1_RAYMARCH 1
#define RENDER_V2_PROJECTION 2

#endif // CORE_CONSTANTS_GLSL
