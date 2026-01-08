// ═══════════════════════════════════════════════════════════════════════════
// CORE PATTERN FUNCTIONS
// Provides switchable core patterns for V8 Electric Aura:
//   Mode 0 = Default (V7-style animated body)
//   Mode 1 = Electric (FBM-based electric tendrils)
// ═══════════════════════════════════════════════════════════════════════════

#ifndef CORE_PATTERNS_GLSL
#define CORE_PATTERNS_GLSL

#include "noise_fbm.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// ELECTRIC CORE PATTERN
// Source: ShaderFrog electric effect
// Creates intertwined electric filaments using FBM
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Electric tendril pattern using FBM.
 * 
 * PARAMETER REUSE RATIONALE:
 * - NoiseOctaves → Layer count (semantic: FBM iterations)
 * - NoiseBaseScale → Pattern scale (semantic: base frequency)  
 * - V2CoreGlow → Brightness intensity (repurpose: core glow → electric glow)
 * - SpeedHigh → Animation speed (repurpose: fast detail → electric flow)
 *
 * @param pos Surface position in object space
 * @param time Animation time
 * @param octaves Number of electric filament layers (from NoiseOctaves, 1-8)
 * @param scale Pattern scale (from NoiseBaseScale)
 * @param speed Animation speed (from SpeedHigh)
 * @param intensity Brightness (from V2CoreGlow)
 * @param flashEnabled 0=smooth pattern, 1=flashy reciprocal tendrils
 * @param fillIntensity 0=empty core, 1=solid filled core (0.5 default)
 * @param fillDarken How much to darken the fill color (0=same, 1=black)
 * @param bodyColor The body color to derive fill from
 * @return Electric color (cyan/blue gradient)
 */
vec3 electricCorePattern(
    vec3 pos, float time, int octaves, float scale, float speed, float intensity,
    float flashEnabled, float fillIntensity, vec3 fillColor, float fillDarken, float lineWidth,
    vec3 bodyColor, bool didHit, float distanceFromSurface, float fadeRange
) {
    vec3 noiseVec = pos * scale;
    vec3 color = vec3(0.0);
    
    // Clamp octaves to reasonable range (max 50 for high detail, may impact performance)
    int layers = clamp(octaves, 1, 50);
    
    // Line width controls the denominator multiplier (exponential scaling)
    // lineWidth: -1000 = extremely thick lines, 0 = medium, +1000 = extremely thin
    // Exponential formula gives smooth continuous scaling with no clamping
    // -1000→0.00001, 0→120, +1000→~1 billion (very thin)
    float lineMult = exp(lineWidth * 0.016) * 120.0;
    
    // Flash fades from 1.0 at surface (distanceFromSurface=0) to 0.0 at fadeRange
    // Uses Proximity Darken > Range for the fade distance
    float flashFade = 1.0 - smoothstep(0.0, fadeRange, distanceFromSurface);
    
    for (int i = 0; i < layers; ++i) {
        // Rotate axes each iteration for intertwined filaments
        noiseVec = noiseVec.zyx;
        
        // Time offset varies per layer
        float layerTime = time * speed / float(i + 4);
        
        // FBM sample with time animation
        float fbmValue = fbm4(noiseVec + vec3(0.0, layerTime, 0.0));
        
        float t;
        if (didHit) {
            // ON SPHERE: Always use unclamped reciprocal for bright lines
            t = abs(2.0 / (fbmValue * lineMult + 0.1));
        } else {
            // OFF SPHERE (missed rays): Only render if flash is enabled, with distance fade
            if (flashEnabled > 0.5) {
                // Flash ON: Apply with distance-based fade
                float baseT = abs(2.0 / (fbmValue * lineMult + 0.1));
                t = baseT * flashFade;
            } else {
                // Flash OFF: Don't render for missed rays
                t = 0.0;
            }
        }
        
        // Color gradient: layer-dependent tint from orange to cyan
        vec3 layerColor = vec3(float(i + 1) * 0.1 + 0.1, 0.5, 2.0);
        color += t * layerColor;
    }
    
    // Scale to match V7 default core intensity range and tint with body color
    vec3 result = color * intensity * 0.025 * bodyColor;
    
    // Apply fill color to dim areas (fill pixels), leave lines unchanged
    // SKIP completely for missed rays (brightness near zero)
    float brightness = max(result.r, max(result.g, result.b));
    
    // Skip fill logic entirely for missed rays (brightness < 0.001 means no pattern)
    if (brightness > 0.001) {
        // isFill = 1.0 for dim areas (fill), 0.0 for bright areas (lines)
        float isFill = 1.0 - smoothstep(0.1, 0.6, brightness);
        
        // Darken the Accent1 fill color by fillDarken amount (0=full color, 1=black)
        vec3 darkenedFill = fillColor * (1.0 - fillDarken);
        
        // fillIntensity controls opacity of the fill (0=transparent, 1=fully opaque)
        float fillOpacity = isFill * fillIntensity;
        
        // Apply darkened fill color with opacity
        result = mix(result, darkenedFill, fillOpacity);
    }
    // else: missed ray, result stays vec3(0) - no fill applied
    
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════
// PATTERN DISPATCHER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Computes core pattern based on type.
 * 
 * @param coreType 0=Default, 1=Electric, 2+=future
 * @param pos Surface position
 * @param time Animation time
 * @param octaves Layer count for electric mode
 * @param scale Pattern scale
 * @param speed Animation speed
 * @param intensity Brightness
 * @param flashEnabled Electric: 0=off (no scene flash), 1=on (scene flash)
 * @param fillIntensity Electric: 0=minimal fill, 1=rich fill
 * @param fillColor Electric: Accent1 color for fill areas
 * @param fillDarken Electric: 0=full color, 1=black
 * @param lineWidth Electric: 0=thick lines, 1=thin lines
 * @param bodyColor Body color for tinting
 * @param defaultColor Color to return for default mode (caller provides V7 result)
 * @param didHit Whether ray hit the sphere (controls flash vs no-render for missed rays)
 * @param distanceFromSurface Distance from orb surface (0 at surface, positive = outside)
 * @param fadeRange Fade distance for flash (from Proximity Darken > Range)
 * @return Core pattern color
 */
vec3 computeCorePattern(
    int coreType,
    vec3 pos,
    float time,
    int octaves,
    float scale,
    float speed,
    float intensity,
    float flashEnabled,
    float fillIntensity,
    vec3 fillColor,
    float fillDarken,
    float lineWidth,
    vec3 bodyColor,
    vec3 defaultColor,
    bool didHit,
    float distanceFromSurface,
    float fadeRange
) {
    if (coreType == 1) {
        // Electric mode
        return electricCorePattern(pos, time, octaves, scale, speed, intensity,
                                   flashEnabled, fillIntensity, fillColor, fillDarken, lineWidth, 
                                   bodyColor, didHit, distanceFromSurface, fadeRange);
    }
    
    // Default: return caller-provided V7 color
    return defaultColor;
}

#endif // CORE_PATTERNS_GLSL
