// ═══════════════════════════════════════════════════════════════════════════
// EFFECT RESULT - Unified Return Structure for All Effect Renderers
// ═══════════════════════════════════════════════════════════════════════════
//
// Purpose:
// ─────────
// Single return type for all visual effect rendering functions.
// Eliminates the need for PulsarV7Result, EnergyOrbV3Result, GeodesicV1Result, etc.
//
// Usage:
// ─────────
// EffectResult myEffect = renderMyEffect(...);
// if (myEffect.alpha > 0.001) {
//     vec3 finalColor = compositeFieldEffect(sceneColor.rgb, 
//                                            vec4(myEffect.color, myEffect.alpha));
//     fragColor = vec4(finalColor, 1.0);
// }
//
// Fields:
// ─────────
// didHit    - True if ray intersected the effect geometry (for raymarched effects)
// color     - Final RGB color of the effect at this fragment
// alpha     - Transparency (0 = invisible, 1 = fully opaque)
// distance  - Distance from camera to hit point (for depth sorting/occlusion)
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECT_RESULT_GLSL
#define EFFECT_RESULT_GLSL

struct EffectResult {
    bool didHit;      // True if ray intersects effect geometry
    vec3 color;       // Final RGB color
    float alpha;      // Transparency (0-1)
    float distance;   // Distance to hit point (for depth)
};

// ═══════════════════════════════════════════════════════════════════════════
// FACTORY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Creates an empty/invisible effect result.
 * Use when the effect should not render (e.g., behind occlusion).
 */
EffectResult effectResult_empty() {
    return EffectResult(false, vec3(0.0), 0.0, 10000.0);
}

/**
 * Creates a hit result with color and alpha.
 * Distance defaults to 0 (closest possible).
 */
EffectResult effectResult_hit(vec3 color, float alpha) {
    return EffectResult(true, color, alpha, 0.0);
}

/**
 * Creates a full hit result with all fields.
 */
EffectResult effectResult_full(vec3 color, float alpha, float distance) {
    return EffectResult(true, color, alpha, distance);
}

/**
 * Creates a miss result (no geometry hit, but may have glow/rays).
 */
EffectResult effectResult_glow(vec3 color, float alpha) {
    return EffectResult(false, color, alpha, 10000.0);
}

#endif // EFFECT_RESULT_GLSL
