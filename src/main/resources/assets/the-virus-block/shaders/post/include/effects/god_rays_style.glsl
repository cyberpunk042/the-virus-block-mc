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
// ENERGY VISIBILITY
// Controls which part of the ray is visible based on RadiativeInteraction mode
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get visibility/density factor based on energy mode.
 * Controls how bright each point along the ray is.
 * 
 * @param t Position along ray (0=at light/core, 1=at pixel/edge)
 * @param time Animation time
 * @param energyMode RadiativeInteraction enum index (0-7)
 * @return Visibility multiplier (0-1)
 */
float getEnergyVisibility(float t, float time, float energyMode) {
    // Smooth triangular wave for animation (avoids hard cuts from fract())
    float rawPhase = time * 0.4;
    float trianglePhase = abs(fract(rawPhase) * 2.0 - 1.0); // 0→1→0 smoothly
    
    if (energyMode < 0.5) {
        // Mode 0: NONE - uniform visibility, no effect
        return 1.0;
        
    } else if (energyMode < 1.5) {
        // Mode 1: EMISSION - Light EXPLODES from center
        // Extremely bright at core (t=0), exponential falloff toward edges
        float baseDensity = exp(-t * 3.0);
        // Animated pulses pushing OUTWARD (smooth oscillation)
        float pulsePos = trianglePhase;
        float pulse = exp(-pow((t - pulsePos) * 4.0, 2.0));
        return baseDensity * 0.7 + pulse * 0.5;
        
    } else if (energyMode < 2.5) {
        // Mode 2: ABSORPTION - Light SUCKED into void
        float baseDensity = 1.0 - exp(-t * 3.0);
        // Animated pulses pulling INWARD (smooth oscillation)
        float pulsePos = 1.0 - trianglePhase;
        float pulse = exp(-pow((t - pulsePos) * 4.0, 2.0));
        return baseDensity * 0.7 + pulse * 0.5;
        
    } else if (energyMode < 3.5) {
        // Mode 3: REFLECTION - Light bouncing off invisible sphere
        float surfaceRadius = 0.4;
        float surfaceDist = abs(t - surfaceRadius);
        float baseDensity = exp(-surfaceDist * 6.0);
        float ripple = sin(t * 20.0 - time * 5.0) * 0.3 + 0.7;
        return baseDensity * ripple;
        
    } else if (energyMode < 4.5) {
        // Mode 4: TRANSMISSION - Clean beam passing straight through
        float cleanBeam = 0.8 + sin(time * 0.5) * 0.1;
        return cleanBeam;
        
    } else if (energyMode < 5.5) {
        // Mode 5: SCATTERING - Chaotic dispersal
        float noise1 = fract(sin(t * 50.0 + time * 3.0) * 43758.5453);
        float noise2 = fract(sin(t * 73.0 + time * 2.3) * 12345.6789);
        float chaos = noise1 * noise2 * 1.5;
        return 0.3 + chaos * 0.7;
        
    } else if (energyMode < 6.5) {
        // Mode 6: OSCILLATION - Standing wave pattern (like vibrating string)
        float phase = time * 1.5;
        
        // Standing wave = spatial pattern × oscillating amplitude
        // Creates nodes that stay fixed while amplitude pulses
        float spatialPattern = sin(t * 6.28 * 2.0);  // ~2 wavelengths along ray
        float temporalOscillation = sin(phase);       // Amplitude oscillates
        float standingWave = spatialPattern * temporalOscillation;
        
        // Convert to visibility (ensure always positive, with base level)
        float visibility = abs(standingWave) * 0.6 + 0.3;
        
        // Add slight falloff toward edge for natural look
        float edgeFade = 1.0 - smoothstep(0.7, 1.0, t);
        return visibility * edgeFade * 0.9 + 0.1;
        
    } else {
        // Mode 7: RESONANCE - Grows then decays asymmetrically (smoother)
        float growPhase = fract(time * 0.3);
        // Use smoothstep for smoother asymmetric curve
        float asymmetric;
        if (growPhase < 0.7) {
            asymmetric = smoothstep(0.0, 0.7, growPhase);
        } else {
            asymmetric = 1.0 - smoothstep(0.7, 1.0, growPhase);
        }
        float visibility = smoothstep(asymmetric + 0.15, asymmetric - 0.05, t);
        return visibility * 0.9 + 0.1;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CURVATURE UTILITY
// Uses rotateVec2() from math_utils.glsl (included via field_visual_base)
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// ENERGY DIRECTION + CURVATURE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Apply curvature to ray direction.
 *
 * @param dir Normalized base direction (toward or away from light)
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param curvatureMode 0=radial, 1=vortex, 2=spiral, 3=tangential, 4=pinwheel
 * @param curvatureStrength How much to curve (0-1 typical)
 * @param time For spinning animation
 * @return Curved direction (normalized)
 */
vec2 applyCurvature(vec2 dir, vec2 pixelUV, vec2 lightUV, float curvatureMode, float curvatureStrength, float time) {
    if (curvatureMode < 0.5) {
        // Mode 0: NONE - no curvature
        return dir;
    }
    
    vec2 toLight = lightUV - pixelUV;
    float dist = length(toLight);
    float angle = atan(toLight.y, toLight.x);
    
    float curveAngle = 0.0;
    
    if (curvatureMode < 1.5) {
        // Mode 1: VORTEX - whirlpool, rotation increases with distance
        curveAngle = dist * curvatureStrength * 6.28 + time * 0.5;
    } else if (curvatureMode < 2.5) {
        // Mode 2: SPIRAL_ARM - galaxy arms, logarithmic + rotation
        curveAngle = log(max(dist, 0.01) + 1.0) * curvatureStrength * 4.0 + time * 0.3;
    } else if (curvatureMode < 3.5) {
        // Mode 3: TANGENTIAL - perpendicular to radial (90 degree offset)
        curveAngle = 1.5708 * curvatureStrength + sin(time * 1.5) * 0.2;
    } else if (curvatureMode < 4.5) {
        // Mode 4: LOGARITHMIC - nautilus shell spiral
        // Java: log(1 + t * 3) * PI * 0.5 * intensity
        float t = clamp(dist * 2.0, 0.0, 1.0);
        curveAngle = log(1.0 + t * 3.0) * 3.14159 * 0.5 * curvatureStrength;
    } else if (curvatureMode < 5.5) {
        // Mode 5: PINWHEEL - windmill blade curves
        // Java: t * PI * 0.75 * intensity
        float t = clamp(dist * 2.0, 0.0, 1.0);
        curveAngle = t * 3.14159 * 0.75 * curvatureStrength;
    } else {
        // Mode 6: ORBITAL - full circular orbits
        // Java: t * 2PI * intensity
        float t = clamp(dist * 2.0, 0.0, 1.0);
        curveAngle = t * 6.28318 * curvatureStrength;
    }
    
    return normalize(rotateVec2(dir, curveAngle));
}

/**
 * Get the ray march direction based on energy mode and curvature.
 *
 * @param pixelUV Current pixel UV coordinate
 * @param lightUV Light source UV coordinate
 * @param energyMode RadiativeInteraction enum (0-7)
 * @param curvatureMode RayCurvature enum (0-6)
 * @param curvatureStrength How much to curve (0-2 typical)
 * @param time For animation
 * @return Normalized direction vector for ray marching
 */
vec2 getGodRayDirection(vec2 pixelUV, vec2 lightUV, float energyMode, float curvatureMode, float curvatureStrength, float time) {
    vec2 toLight = lightUV - pixelUV;
    float dist = length(toLight);
    vec2 outward = normalize(toLight);
    vec2 inward = -outward;
    vec2 tangent = normalize(vec2(-toLight.y, toLight.x));
    vec2 baseDir;
    
    if (energyMode < 0.5) {
        // Mode 0: NONE - standard rays toward light (static)
        baseDir = outward;
    } else if (energyMode < 1.5) {
        // Mode 1: EMISSION - rays radiate outward with subtle pulsing
        float pulse = sin(time * 1.2) * 0.03;
        baseDir = normalize(outward + tangent * pulse);
    } else if (energyMode < 2.5) {
        // Mode 2: ABSORPTION - rays flow inward with subtle pulsing
        float pulse = sin(time * 1.2) * 0.03;
        baseDir = normalize(inward + tangent * pulse);
    } else if (energyMode < 3.5) {
        // Mode 3: REFLECTION - smooth oscillating angle (shimmer effect)
        float shimmerAngle = sin(time * 2.5) * 0.12;
        baseDir = normalize(outward + tangent * shimmerAngle);
    } else if (energyMode < 4.5) {
        // Mode 4: TRANSMISSION - very subtle wave distortion
        float wave = sin(time * 1.0 + dist * 8.0) * 0.025;
        baseDir = normalize(outward + tangent * wave);
    } else if (energyMode < 5.5) {
        // Mode 5: SCATTERING - per-ray random angle that dances
        float randBase = fract(sin(dot(pixelUV, vec2(12.9898, 78.233))) * 43758.5453);
        float dancingAngle = (randBase - 0.5) * 0.35 + sin(time * 1.8 + randBase * 10.0) * 0.08;
        baseDir = normalize(rotateVec2(outward, dancingAngle));
    } else if (energyMode < 6.5) {
        // Mode 6: OSCILLATION - always outward, standing wave handles the visual
        baseDir = outward;
    } else {
        // Mode 7: RESONANCE - direction stays outward, intensity in visibility
        float pulse = sin(time * 0.8) * 0.05;
        baseDir = normalize(outward + tangent * pulse);
    }
    
    // Apply curvature to the base direction
    return applyCurvature(baseDir, pixelUV, lightUV, curvatureMode, curvatureStrength, time);
}

// ═══════════════════════════════════════════════════════════════════════════
// ANGULAR DISTRIBUTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get angular weight for ray intensity based on distribution mode.
 *
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param distributionMode 0=uniform, 1=weighted, 2=noise, 3=random, 4=stochastic
 * @param angularBias Bias direction (-1=vertical, 0=none, 1=horizontal)
 * @param noiseScale For stochastic: number of rays around 360°
 * @param noiseSpeed For stochastic: animation speed
 * @param time Animation time
 * @return Weight multiplier [0-1]
 */
float getAngularWeight(vec2 pixelUV, vec2 lightUV, float distributionMode, float angularBias, float noiseScale, float noiseSpeed, float noiseIntensity, float time) {
    vec2 dir = normalize(pixelUV - lightUV);
    float angle = atan(dir.y, dir.x);
    
    // Proper angle normalization for quantization
    float normalizedAngle = (angle + 3.14159) / 6.28318; // 0 to 1
    
    if (distributionMode < 0.5) {
        // Mode 0: UNIFORM - breathing animation with wave patterns
        // Scale: how many breathing waves around the circle
        // Speed: breathing animation speed  
        // Intensity: breathing amplitude (0=flat, 1=±30%)
        float waveCount = max(1.0, noiseScale);
        float breath = sin(time * noiseSpeed + angle * waveCount) * noiseIntensity * 0.3;
        return 1.0 + breath;
    }
    
    if (distributionMode < 1.5) {
        // Mode 1: WEIGHTED - directional bias with animation
        // Scale: weight sharpness (power exponent)
        // Speed: rotation animation speed
        // Intensity: contrast (0=flat, 1=high contrast)
        float rotatingBias = angularBias + sin(time * noiseSpeed) * 0.5;
        float vertWeight = abs(dir.y);
        float horizWeight = abs(dir.x);
        float weight = mix(vertWeight, horizWeight, rotatingBias * 0.5 + 0.5);
        
        // Apply sharpness (scale as power exponent, clamped)
        weight = pow(weight, max(0.5, noiseScale * 0.3));
        
        // Apply intensity (contrast control)
        float minBright = 1.0 - noiseIntensity * 0.7;
        float pulse = sin(time * noiseSpeed * 2.0 + angle) * 0.1 + 1.0;
        return (minBright + weight * noiseIntensity * 0.7) * pulse;
    } else if (distributionMode < 2.5) {
        // Mode 2: Noise - handled separately in getNoiseModulation
        return 1.0;
    } else if (distributionMode < 3.5) {
        // Mode 3: RANDOM - different brightness per ray with subtle shimmer
        float rayCount = max(4.0, noiseScale * 2.0);
        float bandIndex = floor(normalizedAngle * rayCount);
        float rayRand = fract(sin(bandIndex * 78.233 + 12.9898) * 43758.5453);
        
        // Subtle shimmer animation (speed controls shimmer rate)
        float shimmer = sin(time * noiseSpeed + bandIndex * 2.0) * 0.05;
        
        // Intensity controls brightness range: low intensity = 0.8-1.0, high = 0.3-1.0
        float minBright = 1.0 - noiseIntensity * 0.7;
        return minBright + rayRand * noiseIntensity * 0.7 + shimmer;
    } else {
        // Mode 4: STOCHASTIC - some rays visible, some hidden (flickering)
        float rayCount = max(4.0, noiseScale * 2.0);
        float bandIndex = floor(normalizedAngle * rayCount);
        float rayRand = fract(sin(bandIndex * 12.9898 + 78.233) * 43758.5453);
        
        // Smooth animation instead of chaotic hash
        float flicker = sin(time * noiseSpeed + bandIndex * 3.7) * 0.5 + 0.5; // 0-1 smooth
        
        // Intensity controls on/off threshold: 0.5 = 50/50, 0.2 = 80% on, 0.8 = 20% on
        float threshold = noiseIntensity;
        
        // Binary-ish: some rays on (0.7-1.0), some off (0.1-0.3)
        float visibility = rayRand > threshold ? 0.7 + flicker * 0.3 : 0.1 + flicker * 0.2;
        return visibility;
    }
}

/**
 * Get intensity breathing modulation.
 * Adds life to all modes with subtle pulsing.
 *
 * @param time Animation time
 * @param speed Breathing speed multiplier
 * @param intensity Breathing depth (0=none, 1=full)
 * @return Multiplier typically in range 0.8 to 1.2
 */
float getIntensityBreathing(float time, float speed, float intensity) {
    float breath = sin(time * speed) * 0.5 + 0.5; // 0 to 1
    return 1.0 - intensity * 0.2 + breath * intensity * 0.4; // Range: (1-0.2*i) to (1+0.2*i)
}

/**
 * Get noise modulation for angular distribution.
 *
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param time Animation time
 * @param scale Number of noise bands around the circle (1-50)
 * @param speed Animation speed (how fast noise shifts)
 * @param intensity Modulation strength 0-1
 * @return Modulation multiplier (1-intensity) to 1.0
 */
float getNoiseModulation(vec2 pixelUV, vec2 lightUV, float time, float scale, float speed, float intensity) {
    vec2 dir = pixelUV - lightUV;
    float angle = atan(dir.y, dir.x); // -PI to PI
    
    // Normalize angle to 0-1 range
    float normalizedAngle = (angle + 3.14159) / 6.28318; // 0 to 1
    
    // Animate the angle for smooth rotation effect
    float shiftedAngle = normalizedAngle + time * speed * 0.05;
    
    // Scale creates discrete bands around the circle
    // Higher scale = more bands = higher frequency noise
    float bandedAngle = floor(shiftedAngle * scale) / scale;
    
    // Generate noise value for this band
    float n = fract(sin(bandedAngle * 78.233 + 12.9898) * 43758.5453);
    
    // Smooth interpolation within each band for less harsh edges
    float bandFract = fract(normalizedAngle * scale);
    float smoothN = n * (1.0 - bandFract * 0.3); // Slight gradient within band
    
    // Map to range: (1-intensity) to 1.0
    return 1.0 - intensity + smoothN * intensity;
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
 * @param screenRadius Orb's apparent radius in screen space (may be 0)
 * @param arrangementMode RayArrangement enum (0=radial, 1=spherical, 2=parallel, 3=converging, 4=diverging)
 * @return Adjusted light UV for this pixel
 */
vec2 getArrangedLightUV(vec2 baseLightUV, vec2 pixelUV, float screenRadius, float arrangementMode) {
    vec2 toPixel = pixelUV - baseLightUV;
    float dist = length(toPixel);
    vec2 dir = (dist > 0.001) ? normalize(toPixel) : vec2(1.0, 0.0);
    
    if (arrangementMode < 0.5) {
        // Mode 0: RADIAL - 2D star pattern (XZ plane only)
        // Light at center, horizontal restriction via getArrangementWeight
        return baseLightUV;
        
    } else if (arrangementMode < 1.5) {
        // Mode 1: SPHERICAL - 3D full sphere of rays
        // Light at center, full coverage
        return baseLightUV;
        
    } else if (arrangementMode < 2.5) {
        // Mode 2: PARALLEL - all rays point same direction
        // Light "infinitely far" upward - makes all rays nearly parallel
        return baseLightUV + vec2(0.0, -5.0);
        
    } else if (arrangementMode < 3.5) {
        // Mode 3: CONVERGING - rays from edges toward center
        // Light source is BEHIND each pixel (toward screen edge)
        return pixelUV + dir * 0.8;
        
    } else {
        // Mode 4: DIVERGING - same as SPHERICAL for light position
        // Difference is in intensity (handled elsewhere)
        return baseLightUV;
    }
}

/**
 * Get arrangement-based weight modifier.
 * Used to restrict RADIAL to horizontal band.
 *
 * @param pixelUV Current pixel UV
 * @param lightUV Light source UV
 * @param arrangementMode RayArrangement enum (0-4)
 * @return Weight multiplier [0-1]
 */
float getArrangementWeight(vec2 pixelUV, vec2 lightUV, float arrangementMode) {
    if (arrangementMode < 0.5) {
        // Mode 0: RADIAL - horizontal disk around light (XZ plane projection)
        // Weight based on vertical distance from light's horizontal plane
        float verticalDist = abs(pixelUV.y - lightUV.y);
        
        // Pixels in horizontal band around light get full weight
        // Falloff for pixels above/below
        float bandWidth = 0.3; // Half-width of the horizontal band
        float weight = 1.0 - smoothstep(0.0, bandWidth, verticalDist);
        return max(0.15, weight); // Minimum 15% visibility outside band
    }
    
    // All other modes: full weight
    return 1.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// FLICKER MODULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get flicker modulation for ray intensity.
 * 
 * @param pixelUV Current pixel UV (for per-ray variation)
 * @param lightUV Light source UV
 * @param time Animation time
 * @param flickerMode EnergyFlicker enum (0-6)
 * @param intensity Flicker strength (0-1)
 * @param frequency Flicker speed multiplier
 * @return Modulation factor (0-1, multiply with illumination)
 */
float getFlickerModulation(vec2 pixelUV, vec2 lightUV, float time, float flickerMode, float intensity, float frequency) {
    if (flickerMode < 0.5) {
        // Mode 0: NONE - no flicker
        return 1.0;
    }
    
    // Per-ray random seed based on angle from light
    vec2 toPixel = pixelUV - lightUV;
    float angle = atan(toPixel.y, toPixel.x);
    float rayId = floor(angle * 16.0 / 6.28318);
    float rayRand = fract(sin(rayId * 12.9898) * 43758.5453);
    
    float t = time * frequency;
    float flick = 1.0;
    
    if (flickerMode < 1.5) {
        // Mode 1: SCINTILLATION - star twinkling
        float noise = fract(sin(rayRand * 1000.0 + t * 3.0) * 43758.5453);
        flick = 1.0 - intensity * noise;
    } else if (flickerMode < 2.5) {
        // Mode 2: STROBE - rhythmic on/off
        float strobe = step(0.5, fract(t));
        flick = mix(1.0, strobe, intensity);
    } else if (flickerMode < 3.5) {
        // Mode 3: FADE_PULSE - smooth breathing
        float breath = sin(t * 3.14159) * 0.5 + 0.5;
        flick = 1.0 - intensity * 0.5 + breath * intensity * 0.5;
    } else if (flickerMode < 4.5) {
        // Mode 4: FLICKER - candlelight unstable
        float noise1 = fract(sin(t * 10.0 + rayRand) * 43758.5453);
        float noise2 = fract(sin(t * 23.0 + rayRand * 2.0) * 43758.5453);
        flick = 1.0 - intensity * (noise1 * 0.3 + noise2 * 0.2);
    } else if (flickerMode < 5.5) {
        // Mode 5: LIGHTNING - flash then fade
        float cycle = fract(t * 0.5);
        float flash = exp(-cycle * 8.0);
        flick = mix(0.3, 1.0, flash * intensity) + (1.0 - intensity);
    } else {
        // Mode 6: HEARTBEAT - double-pulse rhythm
        float beat = fract(t);
        float pulse1 = exp(-pow((beat - 0.2) * 10.0, 2.0));
        float pulse2 = exp(-pow((beat - 0.4) * 10.0, 2.0));
        flick = 1.0 - intensity * 0.5 + (pulse1 + pulse2 * 0.7) * intensity * 0.5;
    }
    
    return clamp(flick, 0.0, 1.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// TRAVEL MODULATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get travel modulation for animated particles along ray.
 * Matches FlowTravelStage.computeTravelAlpha from Java.
 *
 * @param t Position along ray (0=light, 1=pixel)
 * @param time Animation time
 * @param travelMode EnergyTravel enum (0-5 for base modes)
 * @param speed Animation speed multiplier
 * @param particleCount Number of particles/waves (1-10)
 * @param particleWidth Width of each particle (0.05-0.5)
 * @param rayRand Per-ray random value (0-1)
 * @return Modulation factor (0-1, multiply with sample weight)
 */
float getTravelModulation(float t, float time, float travelMode, float speed, float particleCount, float particleWidth, float rayRand) {
    if (travelMode < 0.5) {
        // Mode 0: NONE - no travel animation
        return 1.0;
    }
    
    float phase = fract(time * speed * 0.3);
    int count = int(clamp(particleCount, 1.0, 10.0));
    float width = clamp(particleWidth, 0.05, 0.5);
    
    if (travelMode < 1.5) {
        // Mode 1: CHASE - discrete particles traveling
        // Matches Java FlowTravelStage.CHASE
        float spacing = 1.0 / float(count);
        float maxAlpha = 0.0;
        for (int i = 0; i < count; i++) {
            float center = fract(phase + float(i) * spacing);
            // Wrap-around distance
            float dist = min(abs(t - center), min(abs(t - center - 1.0), abs(t - center + 1.0)));
            if (dist <= width / 2.0) {
                float falloff = 1.0 - (dist / (width / 2.0));
                maxAlpha = max(maxAlpha, falloff * falloff); // Quadratic falloff
            }
        }
        return maxAlpha;
        
    } else if (travelMode < 2.5) {
        // Mode 2: SCROLL - continuous gradient scrolling
        // Matches Java: 1 - |scrolledT - 0.5| * 2
        float scrolledT = fract(t + phase);
        return 1.0 - abs(scrolledT - 0.5) * 2.0;
        
    } else if (travelMode < 3.5) {
        // Mode 3: COMET - bright head with fading tail
        // Head travels continuously in one direction (0→1, wraps)
        float headPos = phase;
        float tailLength = width * 3.0; // Tail length based on width param
        
        // Calculate distance from head, with wrap-around
        float distFromHead = t - headPos;
        
        // Handle wrap-around: if head is near 0, tail wraps from 1.0
        // Check both direct distance and wrapped distance
        float wrappedDist = distFromHead;
        if (distFromHead > 0.5) wrappedDist = distFromHead - 1.0;  // t ahead, wrap back
        if (distFromHead < -0.5) wrappedDist = distFromHead + 1.0; // t behind, wrap forward
        
        // Head region (bright) - use wrapped distance
        if (abs(wrappedDist) < 0.05) {
            return 1.0;
        }
        
        // Tail region (behind head, fading) - use wrapped distance
        if (wrappedDist < 0.0 && wrappedDist > -tailLength) {
            float tailDist = -wrappedDist;
            float tailAlpha = 1.0 - (tailDist / tailLength);
            return tailAlpha * tailAlpha * 0.8 + 0.1;
        }
        
        // Ahead of head or far behind - dim
        return 0.1;
        
    } else if (travelMode < 4.5) {
        // Mode 4: SPARK - random sparks along ray
        // Use count parameter for number of sparks
        float maxAlpha = 0.15; // Base visibility
        
        for (int i = 0; i < count; i++) {
            // Each spark has a position based on index
            float sparkBasePos = float(i) / float(count);
            
            // Spark flickers on/off over time
            float flickerSeed = sparkBasePos * 12.9898 + phase * 5.0;
            float flicker = fract(sin(flickerSeed) * 43758.5453);
            
            // Only show spark when flicker is high
            if (flicker > 0.4) {
                // Spark position oscillates slightly
                float sparkPos = sparkBasePos + sin(phase * 6.28 + sparkBasePos * 10.0) * 0.1;
                sparkPos = fract(sparkPos);
                
                float dist = abs(t - sparkPos);
                if (dist > 0.5) dist = 1.0 - dist;
                
                if (dist < width) {
                    float brightness = 1.0 - (dist / width);
                    brightness = brightness * brightness * flicker;
                    maxAlpha = max(maxAlpha, brightness);
                }
            }
        }
        return maxAlpha;
        
    } else {
        // Mode 5: PULSE_WAVE - gaussian pulse waves
        // Matches Java: exp(-normalized² * 4)
        float waveWidth = max(0.1, width * 1.5);
        float maxAlpha = 0.0;
        for (int i = 0; i < count; i++) {
            float waveCenter = fract(phase + float(i) / float(count));
            // Wrap-around distance
            float dist = min(abs(t - waveCenter), min(abs(t - waveCenter - 1.0), abs(t - waveCenter + 1.0)));
            if (dist < waveWidth) {
                float normalized = dist / waveWidth;
                float pulse = exp(-normalized * normalized * 4.0);
                maxAlpha = max(maxAlpha, pulse);
            }
        }
        return maxAlpha;
    }
}

#endif // GOD_RAYS_STYLE_GLSL
