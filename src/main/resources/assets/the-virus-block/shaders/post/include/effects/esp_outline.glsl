// ═══════════════════════════════════════════════════════════════════════════
// ESP (EXTRA-SENSORY PERCEPTION) OUTLINE EFFECTS
// ═══════════════════════════════════════════════════════════════════════════
//
// X-ray style highlighting for threat detection systems.
// Features:
// - Distance-adaptive glow (intensity and size)
// - Sharp edge outlines
// - Pulsing animation
// - Screen-space projection
//
// Usage:
//   ESPResult esp = renderESPOutline(uv, cam, targets, params);
//   color += esp.color;
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_ESP_OUTLINE_GLSL
#define EFFECTS_ESP_OUTLINE_GLSL

#include "../core/constants.glsl"
#include "../camera/types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// DATA STRUCTURES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ESP visual parameters
 */
struct ESPParams {
    // Distance thresholds
    float closeDistance;      // Red zone threshold (30.0)
    float mediumDistance;     // Orange zone threshold (100.0)
    
    // Colors
    vec3 closeColor;          // Close = danger (red)
    vec3 mediumColor;         // Medium = caution (orange)
    vec3 farColor;            // Far = detected (green/teal)
    
    // Style
    float pulseSpeed;         // Pulse animation speed (2.0)
    float pulseAmount;        // Pulse intensity (0.3)
    float glowPower;          // Glow falloff exponent (2.0)
    float glowSize;           // Base glow radius in screen units (0.02)
    float edgeSharpness;      // Edge sharpness (10.0)
    float edgeThickness;      // Edge line thickness (0.003)
};

/**
 * Default ESP parameters
 */
ESPParams defaultESPParams() {
    ESPParams p;
    p.closeDistance = 30.0;
    p.mediumDistance = 100.0;
    p.closeColor = vec3(0.9, 0.2, 0.2);
    p.mediumColor = vec3(0.9, 0.6, 0.2);
    p.farColor = vec3(0.3, 0.8, 0.4);
    p.pulseSpeed = 2.0;
    p.pulseAmount = 0.3;
    p.glowPower = 2.0;
    p.glowSize = 0.02;
    p.edgeSharpness = 10.0;
    p.edgeThickness = 0.003;
    return p;
}

/**
 * ESP target (thing to highlight)
 */
struct ESPTarget {
    vec3 position;      // World position
    float intensity;    // Highlight intensity (0-1+)
    float size;         // Object size for outline (1.0 = 1 block)
};

/**
 * ESP rendering result
 */
struct ESPResult {
    vec3 color;         // Color to add (additive blend)
    float intensity;    // Overall intensity for debugging
};

// ═══════════════════════════════════════════════════════════════════════════
// PROJECTION UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Project world position to screen UV.
 * Returns vec3(screenUV.xy, behindCamera) where behindCamera > 0 if behind.
 */
vec3 worldToScreen(vec3 worldPos, CameraData cam) {
    vec3 toPoint = worldPos - cam.position;
    float dotFwd = dot(toPoint, cam.forward);
    
    if (dotFwd < 0.1) {
        return vec3(0.0, 0.0, 1.0);  // Behind camera
    }
    
    float tanHalfFov = tan(cam.fov * 0.5);
    
    vec2 screenPos = vec2(
        dot(toPoint, cam.right) / (dotFwd * tanHalfFov * cam.aspect),
        dot(toPoint, cam.up) / (dotFwd * tanHalfFov)
    ) * 0.5 + 0.5;
    
    return vec3(screenPos, 0.0);
}

/**
 * Get screen-space size of an object at given distance.
 */
float getScreenSize(float worldSize, float distance, float fov, float aspect) {
    float tanHalfFov = tan(fov * 0.5);
    return worldSize / (distance * tanHalfFov * 2.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// COLOR FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get ESP color based on distance to target.
 */
vec3 getESPColor(float distance, ESPParams params) {
    if (distance < params.closeDistance) {
        return params.closeColor;
    } else if (distance < params.mediumDistance) {
        float t = (distance - params.closeDistance) / (params.mediumDistance - params.closeDistance);
        return mix(params.closeColor, params.mediumColor, t);
    } else {
        float t = clamp((distance - params.mediumDistance) / params.mediumDistance, 0.0, 1.0);
        return mix(params.mediumColor, params.farColor, t);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EFFECT COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Soft glow falloff
 */
float espGlow(float screenDist, float glowRadius, float power) {
    return exp(-screenDist / glowRadius * power);
}

/**
 * Sharp edge outline
 */
float espEdge(float screenDist, float edgeRadius, float thickness, float sharpness) {
    // Create ring at edgeRadius with thickness
    float distFromEdge = abs(screenDist - edgeRadius);
    return exp(-distFromEdge / thickness * sharpness);
}

/**
 * Pulse animation
 */
float espPulse(float time, float speed, float amount) {
    return 1.0 - amount + amount * (0.5 + 0.5 * sin(time * speed));
}

/**
 * Combined ESP intensity for a single target
 */
float espTargetIntensity(
    vec2 uv,
    vec2 targetScreenPos,
    float targetScreenSize,
    float distance,
    float time,
    ESPParams params
) {
    float screenDist = length(uv - targetScreenPos);
    
    // Adaptive sizing - farther targets get proportionally smaller highlights
    float adaptiveGlowSize = params.glowSize * (30.0 / max(distance, 30.0));
    float adaptiveEdgeRadius = targetScreenSize * 0.5;
    float adaptiveEdgeThickness = params.edgeThickness * (50.0 / max(distance, 50.0));
    
    // Only render if within reasonable range
    if (screenDist > adaptiveGlowSize * 5.0 && screenDist > adaptiveEdgeRadius * 2.0) {
        return 0.0;
    }
    
    // Glow component
    float glow = espGlow(screenDist, adaptiveGlowSize, params.glowPower);
    
    // Edge component (box outline effect)
    float edge = espEdge(screenDist, adaptiveEdgeRadius, adaptiveEdgeThickness, params.edgeSharpness);
    
    // Combine with pulse
    float pulse = espPulse(time, params.pulseSpeed, params.pulseAmount);
    
    // Glow is constant, edge pulses more
    float intensity = glow * 0.6 + edge * pulse * 0.8;
    
    return intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDERING FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Render ESP highlight for a single target.
 * 
 * @param uv          Screen UV (0-1)
 * @param cam         Camera data
 * @param target      Target to highlight
 * @param time        Animation time
 * @param params      Visual parameters
 * @return            ESP result with color to add
 */
ESPResult renderESPTarget(
    vec2 uv,
    CameraData cam,
    ESPTarget target,
    float time,
    ESPParams params
) {
    ESPResult result;
    result.color = vec3(0.0);
    result.intensity = 0.0;
    
    // Project to screen
    vec3 screenData = worldToScreen(target.position, cam);
    if (screenData.z > 0.5) return result;  // Behind camera
    
    vec2 screenPos = screenData.xy;
    float distance = length(target.position - cam.position);
    
    // Get screen-space size
    float screenSize = getScreenSize(target.size, distance, cam.fov, cam.aspect);
    
    // Calculate intensity
    float intensity = espTargetIntensity(uv, screenPos, screenSize, distance, time, params);
    intensity *= target.intensity;
    
    if (intensity < 0.001) return result;
    
    // Get distance-based color
    vec3 color = getESPColor(distance, params);
    
    result.color = color * intensity;
    result.intensity = intensity;
    
    return result;
}

/**
 * Combine multiple ESP results (use max to prevent over-saturation)
 */
ESPResult combineESP(ESPResult a, ESPResult b) {
    ESPResult result;
    if (a.intensity > b.intensity) {
        result = a;
    } else {
        result = b;
    }
    // Blend colors weighted by intensity
    float total = a.intensity + b.intensity;
    if (total > 0.001) {
        result.color = (a.color * a.intensity + b.color * b.intensity) / total;
        result.color *= max(a.intensity, b.intensity);  // Clamp to max single intensity
    }
    return result;
}

#endif // EFFECTS_ESP_OUTLINE_GLSL
