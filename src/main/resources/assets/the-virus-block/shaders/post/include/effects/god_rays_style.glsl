// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS STYLE LIBRARY
// ═══════════════════════════════════════════════════════════════════════════
//
// Advanced style functions for god rays effect.
// These functions modify the appearance based on UBO parameters:
//   - GodRayEnergyMode: Direction of ray travel
//   - GodRayColorMode: Color blending strategy
//   - GodRayDistributionMode: Angular intensity variation
//   - GodRayArrangementMode: Source geometry (point, ring, sector)
//
// Usage: Include this in god_rays_accum.fsh and god_rays_composite.fsh
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef GOD_RAYS_STYLE_GLSL
#define GOD_RAYS_STYLE_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// ENERGY DIRECTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get the ray march direction based on energy mode.
 *
 * @param pixelUV Current pixel UV coordinate
 * @param lightUV Light source UV coordinate
 * @param energyMode 0=radiation (outward), 1=absorption (inward), 2=pulse
 * @param time For pulse mode animation
 * @return Normalized direction vector for ray marching
 */
vec2 getGodRayDirection(vec2 pixelUV, vec2 lightUV, float energyMode, float time) {
    vec2 toLight = lightUV - pixelUV;
    
    if (energyMode < 0.5) {
        // Mode 0: Radiation - march from pixel TOWARD light (standard)
        return normalize(toLight);
    } else if (energyMode < 1.5) {
        // Mode 1: Absorption - march from light TOWARD pixel (reverse)
        return normalize(-toLight);
    } else {
        // Mode 2: Pulse - alternate direction based on time
        float pulse = sin(time * 2.0) * 0.5 + 0.5;
        vec2 dir = (pulse > 0.5) ? toLight : -toLight;
        return normalize(dir);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ANGULAR DISTRIBUTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get angular weight for ray intensity based on distribution mode.
 *
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param distributionMode 0=uniform, 1=weighted (vertical bias), 2=noise
 * @param angularBias Bias direction (-1=vertical, 0=none, 1=horizontal)
 * @return Weight multiplier [0-1]
 */
float getAngularWeight(vec2 pixelUV, vec2 lightUV, float distributionMode, float angularBias) {
    if (distributionMode < 0.5) {
        // Mode 0: Uniform - no angular variation
        return 1.0;
    }
    
    vec2 dir = normalize(pixelUV - lightUV);
    float angle = atan(dir.y, dir.x); // Range: -PI to PI
    
    if (distributionMode < 1.5) {
        // Mode 1: Weighted - bias toward vertical or horizontal
        float vertWeight = abs(dir.y);      // 1 when vertical, 0 when horizontal
        float horizWeight = abs(dir.x);     // 1 when horizontal, 0 when vertical
        float weight = mix(vertWeight, horizWeight, angularBias * 0.5 + 0.5);
        return 0.3 + weight * 0.7; // Range: 0.3 to 1.0
    }
    
    // Mode 2: Noise - handled separately in getNoiseModulation
    return 1.0;
}

/**
 * Get noise modulation for organic ray distribution.
 *
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param time Animation time
 * @param scale Noise frequency
 * @param speed Animation speed
 * @param intensity Modulation strength 0-1
 * @return Modulation multiplier (1.0 - intensity) to 1.0
 */
float getNoiseModulation(vec2 pixelUV, vec2 lightUV, float time, float scale, float speed, float intensity) {
    vec2 dir = pixelUV - lightUV;
    float angle = atan(dir.y, dir.x);
    
    // Simple hash-based noise
    float n = fract(sin(angle * scale + time * speed) * 43758.5453);
    
    // Map to range: (1-intensity) to 1.0
    return 1.0 - intensity + n * intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// COLOR BLENDING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get god ray color based on color mode.
 *
 * @param illumination Ray intensity (from accumulator)
 * @param screenDist Distance from light UV in screen space [0-1]
 * @param color1 Primary ray color (RayColorR/G/B)
 * @param color2 Secondary ray color (GodRayColor2R/G/B)
 * @param colorMode 0=solid, 1=gradient, 2=temperature
 * @param gradientPower Curve power for gradient blend
 * @return Final tinted color
 */
vec3 getGodRayColor(
    float illumination,
    float screenDist,
    vec3 color1,
    vec3 color2,
    float colorMode,
    float gradientPower
) {
    if (colorMode < 0.5) {
        // Mode 0: Solid - just use primary color
        return color1 * illumination;
    } else if (colorMode < 1.5) {
        // Mode 1: Gradient - blend from color1 (center) to color2 (edge)
        float t = pow(clamp(screenDist * 2.0, 0.0, 1.0), gradientPower);
        vec3 blendedColor = mix(color1, color2, t);
        return blendedColor * illumination;
    } else {
        // Mode 2: Temperature - warm at center, cool at edges
        vec3 warm = vec3(1.0, 0.7, 0.3);  // Orange
        vec3 cool = vec3(0.3, 0.7, 1.0);  // Cyan
        float t = pow(clamp(screenDist * 2.0, 0.0, 1.0), gradientPower);
        vec3 tempColor = mix(warm, cool, t) * color1;
        return tempColor * illumination;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SOURCE ARRANGEMENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Adjust light UV based on arrangement mode.
 *
 * @param baseLightUV Original light center UV
 * @param pixelUV Current pixel UV
 * @param screenRadius Orb's apparent radius in screen space
 * @param arrangementMode 0=point, 1=ring, 2=sector
 * @return Adjusted light UV for this pixel
 */
vec2 getArrangedLightUV(vec2 baseLightUV, vec2 pixelUV, float screenRadius, float arrangementMode) {
    if (arrangementMode < 0.5) {
        // Mode 0: Point - rays from center
        return baseLightUV;
    } else if (arrangementMode < 1.5) {
        // Mode 1: Ring - rays from surface point nearest to pixel
        vec2 toPixel = normalize(pixelUV - baseLightUV);
        return baseLightUV + toPixel * screenRadius;
    } else {
        // Mode 2: Sector - reserved for future (sectored emission)
        return baseLightUV;
    }
}

#endif // GOD_RAYS_STYLE_GLSL
