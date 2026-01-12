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
    float phase = fract(time * 0.4); // Animation phase
    
    if (energyMode < 0.5) {
        // Mode 0: NONE - uniform visibility, no effect
        return 1.0;
        
    } else if (energyMode < 1.5) {
        // Mode 1: EMISSION - Light EXPLODES from center
        // Extremely bright at core (t=0), exponential falloff toward edges
        float baseDensity = exp(-t * 3.0); // Strong exponential: 1.0 at core, ~0.05 at edge
        // Animated pulses pushing OUTWARD
        float pulsePos = phase; // 0->1 over time (moving outward)
        float pulse = exp(-pow((t - pulsePos) * 4.0, 2.0)); // Gaussian pulse
        return baseDensity * 0.7 + pulse * 0.5;
        
    } else if (energyMode < 2.5) {
        // Mode 2: ABSORPTION - Light SUCKED into void
        // Dark at center (t=0), bright at edges (t=1), inverse of emission
        float baseDensity = 1.0 - exp(-t * 3.0); // Inverse exponential
        // Animated pulses pulling INWARD
        float pulsePos = 1.0 - phase; // 1->0 over time (moving inward)
        float pulse = exp(-pow((t - pulsePos) * 4.0, 2.0));
        return baseDensity * 0.7 + pulse * 0.5;
        
    } else if (energyMode < 3.5) {
        // Mode 3: REFLECTION - Light bouncing off invisible sphere
        // Peak brightness at a surface radius, dark inside and outside
        float surfaceRadius = 0.4; // Where the "surface" is
        float surfaceDist = abs(t - surfaceRadius);
        float baseDensity = exp(-surfaceDist * 6.0); // Sharp peak at surface
        // Shimmer/ripple effect
        float ripple = sin(t * 20.0 - time * 5.0) * 0.3 + 0.7;
        return baseDensity * ripple;
        
    } else if (energyMode < 4.5) {
        // Mode 4: TRANSMISSION - Clean beam passing straight through
        // Even density like a laser, minimal variation
        float cleanBeam = 0.8 + sin(time * 0.5) * 0.1; // Very subtle pulse
        return cleanBeam;
        
    } else if (energyMode < 5.5) {
        // Mode 5: SCATTERING - Chaotic dispersal
        // Uneven, chaotic density that dances
        float noise1 = fract(sin(t * 50.0 + time * 3.0) * 43758.5453);
        float noise2 = fract(sin(t * 73.0 + time * 2.3) * 12345.6789);
        float chaos = noise1 * noise2 * 1.5; // Very uneven
        return 0.3 + chaos * 0.7;
        
    } else if (energyMode < 6.5) {
        // Mode 6: OSCILLATION - Breathing in/out
        // Alternates between emission-like and absorption-like
        float breathPhase = sin(time * 1.5) * 0.5 + 0.5; // 0-1 sinusoidal
        float emissionDensity = exp(-t * 3.0);
        float absorptionDensity = 1.0 - exp(-t * 3.0);
        return mix(absorptionDensity, emissionDensity, breathPhase);
        
    } else {
        // Mode 7: RESONANCE - Grows then decays asymmetrically
        // Field grows to maximum then shrinks back
        float growPhase = fract(time * 0.3);
        float asymmetric = (growPhase < 0.7) ? growPhase / 0.7 : (1.0 - growPhase) / 0.3;
        float reach = asymmetric; // 0->1 (slow grow), 1->0 (fast decay)
        // Everything inside 'reach' is visible, sharp cutoff
        float visibility = smoothstep(reach + 0.1, reach - 0.1, t);
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
        // Mode 6: OSCILLATION - smooth blend between in and out (NO HARD CUT)
        float blend = sin(time * 1.5) * 0.5 + 0.5; // 0 to 1 smoothly
        baseDir = normalize(mix(inward, outward, blend));
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
 * @return Weight multiplier [0-1]
 */
float getAngularWeight(vec2 pixelUV, vec2 lightUV, float distributionMode, float angularBias, float time) {
    if (distributionMode < 0.5) {
        // Mode 0: Uniform - subtle breathing animation
        float breath = sin(time * 1.5) * 0.1 + 1.0;
        return breath;
    }
    
    vec2 dir = normalize(pixelUV - lightUV);
    float angle = atan(dir.y, dir.x);
    
    if (distributionMode < 1.5) {
        // Mode 1: Weighted - bias rotates over time for sweeping effect
        float rotatingBias = angularBias + sin(time * 0.5) * 0.5;
        float vertWeight = abs(dir.y);
        float horizWeight = abs(dir.x);
        float weight = mix(vertWeight, horizWeight, rotatingBias * 0.5 + 0.5);
        float pulse = sin(time * 2.0 + angle) * 0.15 + 1.0;
        return (0.3 + weight * 0.7) * pulse;
    } else if (distributionMode < 2.5) {
        // Mode 2: Noise - handled separately in getNoiseModulation
        return 1.0;
    } else if (distributionMode < 3.5) {
        // Mode 3: RANDOM - randomized start/length per ray (from RayDistribution)
        float rayRand = fract(sin(angle * 12.9898) * 43758.5453);
        float randWeight = rayRand * 0.6 + 0.4; // 0.4 to 1.0
        return randWeight;
    } else {
        // Mode 4: STOCHASTIC - heavily randomized with variable density
        float rayRand1 = fract(sin(angle * 12.9898) * 43758.5453);
        float rayRand2 = fract(sin(angle * 78.233 + time) * 43758.5453);
        // Some rays may be very dim, some very bright
        float stochastic = rayRand1 * rayRand2 * 1.5;
        return clamp(stochastic, 0.1, 1.0);
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
        // Mode 0: RADIAL - restrict to XZ plane (horizontal band)
        vec2 dir = pixelUV - lightUV;
        float len = length(dir);
        if (len < 0.001) return 1.0;
        dir /= len;
        
        // Weight based on how horizontal the ray is (|y| small = horizontal)
        float horizontalness = 1.0 - abs(dir.y);
        return horizontalness * horizontalness; // Squared for sharper falloff
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
 * @param rayRand Per-ray random value (0-1)
 * @return Modulation factor (0-1, multiply with sample weight)
 */
float getTravelModulation(float t, float time, float travelMode, float speed, float rayRand) {
    if (travelMode < 0.5) {
        // Mode 0: NONE - no travel animation
        return 1.0;
    }
    
    float phase = fract(time * speed * 0.3);
    int count = 3;  // Number of particles/waves
    float width = 0.15;
    
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
        // Matches Java FlowTravelStage.COMET
        float headPos = phase;
        float tailLength = max(0.1, width * 2.0);
        
        // Distance behind head (wrapping)
        float distBehind;
        if (t > headPos) {
            distBehind = (1.0 - t) + headPos; // Wrapped
        } else {
            distBehind = headPos - t;
        }
        
        if (distBehind >= 0.0 && distBehind <= tailLength) {
            float tailAlpha = 1.0 - (distBehind / tailLength);
            return tailAlpha * tailAlpha; // Quadratic falloff
        }
        return 0.0;
        
    } else if (travelMode < 4.5) {
        // Mode 4: SPARK - random sparks
        // Matches Java FlowTravelStage.SPARK logic
        float maxAlpha = 0.0;
        for (int i = 0; i < count; i++) {
            // Hash for deterministic randomness (matching Java hash function)
            float h1 = float(i) * 374761.393 + floor(phase * 10.0) * 668265.263;
            float h2 = float(i + 1000) * 374761.393 + floor(phase * 3.0) * 668265.263;
            float sparkPhase = fract(sin(h1) * 0.5 + 0.5);
            float sparkPos = fract(sin(h2) * 0.5 + 0.5);
            
            if (sparkPhase > 0.5) {
                float dist = abs(t - sparkPos);
                if (dist < width) {
                    float spark = 1.0 - (dist / width);
                    maxAlpha = max(maxAlpha, spark * spark);
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
