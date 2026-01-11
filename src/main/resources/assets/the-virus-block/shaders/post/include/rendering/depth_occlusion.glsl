// ═══════════════════════════════════════════════════════════════════════════
// DEPTH-BASED OCCLUSION MASK
// ═══════════════════════════════════════════════════════════════════════════
// 
// Layer: rendering/ (depends on camera/depth.glsl for isSky threshold)
// 
// Converts depth buffer to soft occlusion mask for god rays.
// Sky = 1.0 (full contribution), Blocked = 0.0 (no contribution)
//
// Uses smoothstep for soft transitions to prevent harsh beam edges.
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef DEPTH_OCCLUSION_GLSL
#define DEPTH_OCCLUSION_GLSL

// Sky threshold matches camera/depth.glsl isSky() function
#define SKY_DEPTH_THRESHOLD 0.9999

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH-BASED MASK
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Builds soft occlusion value from raw depth.
 * 
 * @param rawDepth      Raw depth buffer value [0,1]
 * @param skyThreshold  Depth value considered "sky" (default 0.9999)
 * @param softness      Transition width for smoothstep (default 0.0001)
 * @return              0.0 = occluded/blocked, 1.0 = open sky
 */
float buildOcclusionMask(float rawDepth, float skyThreshold, float softness) {
    // smoothstep creates soft edge: fully blocked below (threshold - softness),
    // fully open above threshold
    return smoothstep(skyThreshold - softness, skyThreshold, rawDepth);
}

/**
 * Simple version with defaults.
 */
float buildOcclusionMask(float rawDepth) {
    return buildOcclusionMask(rawDepth, SKY_DEPTH_THRESHOLD, 0.0001);
}

// ═══════════════════════════════════════════════════════════════════════════
// LIGHT SOURCE PROXIMITY MASK
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Creates a soft mask based on distance from light source screen position.
 * Useful for focusing god rays on the light center.
 * 
 * @param pixelUV       Current fragment UV
 * @param lightUV       Light source screen UV
 * @param innerRadius   UV radius where mask is 1.0
 * @param outerRadius   UV radius where mask is 0.0
 * @return              1.0 near light, 0.0 far from light
 */
float lightProximityMask(vec2 pixelUV, vec2 lightUV, float innerRadius, float outerRadius) {
    float dist = length(pixelUV - lightUV);
    return 1.0 - smoothstep(innerRadius, outerRadius, dist);
}

// ═══════════════════════════════════════════════════════════════════════════
// BRIGHTNESS-BASED MASK (alternative approach)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Alternative: brightness-based mask from scene color.
 * Useful for effects that should emanate from bright areas.
 * 
 * @param sceneColor    RGB scene color
 * @param threshold     Brightness threshold for contribution
 * @param softness      Transition softness
 * @return              0.0 = dark (occluded), 1.0 = bright (contributes)
 */
float buildBrightnessMask(vec3 sceneColor, float threshold, float softness) {
    // Luminance calculation (Rec. 709)
    float brightness = dot(sceneColor, vec3(0.2126, 0.7152, 0.0722));
    return smoothstep(threshold - softness, threshold + softness, brightness);
}

// ═══════════════════════════════════════════════════════════════════════════
// COMBINED MASK
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Combines depth and brightness masks for hybrid approach.
 * 
 * @param rawDepth      Raw depth buffer value
 * @param sceneColor    RGB scene color
 * @param depthWeight   Weight for depth-based mask (0-1)
 * @return              Combined occlusion mask
 */
float buildCombinedMask(float rawDepth, vec3 sceneColor, float depthWeight) {
    float depthMask = buildOcclusionMask(rawDepth);
    float brightMask = buildBrightnessMask(sceneColor, 0.8, 0.1);
    return mix(brightMask, depthMask, depthWeight);
}

#endif // DEPTH_OCCLUSION_GLSL
