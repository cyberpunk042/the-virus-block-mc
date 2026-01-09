#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING - WORLD-ANCHORED VERSION
// ═══════════════════════════════════════════════════════════════════════════
// 
// Features:
//   - Fixed world position or camera-follow mode
//   - Multiple rings with expand/contract animation
//   - Orbital spheres with raymarched beams
//   - Flower-pattern SDF with CombinedMode blending
//   - Uses modular shader library with adaptive ray generation
//
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Custom uniform block - Layout: 18 vec4s + 1 mat4 = 22 vec4s = 352 bytes
layout(std140) uniform ShockwaveConfig {
    // vec4 0: Basic params
    float RingRadius;       // Current ring radius (blocks)
    float RingThickness;    // Thickness of each ring
    float Intensity;        // Glow intensity (0-2)
    float Time;             // Animation time
    
    // vec4 1: Ring count and spacing
    float RingCount;        // Number of concentric rings (1-10)
    float RingSpacing;      // Distance between rings (blocks)
    float ContractMode;     // 0 = expand outward, 1 = contract inward
    float GlowWidth;        // Glow falloff width (blocks)
    
    // vec4 2: Target world position (for world-anchored mode)
    float TargetX;
    float TargetY;
    float TargetZ;
    float UseWorldOrigin;   // 0 = camera-centered, 1 = world-anchored
    
    // vec4 3: [DEPRECATED] Was Camera world position - Now use CameraWorldPositionUBO
    float Reserved3_0;
    float Reserved3_1;
    float Reserved3_2;
    float Reserved3_3;
    
    // vec4 4: [DEPRECATED] Was Camera forward - Now use CameraForwardUBO
    float Reserved4_0;
    float Reserved4_1;
    float Reserved4_2;
    float Reserved4_3;
    
    // vec4 5: [DEPRECATED] Was Camera up - Now use CameraUpUBO/CameraClipUBO
    float Reserved5_0;
    float Reserved5_1;
    float Reserved5_2;
    float Reserved5_3;
    
    // vec4 6: Screen blackout / vignette
    float BlackoutAmount;   // 0 = no blackout, 1 = full black
    float VignetteAmount;   // 0 = no vignette, 1 = strong vignette
    float VignetteRadius;   // Inner radius of vignette (0-1)
    float Reserved3;
    
    // vec4 7: Color tint / filter
    float TintR;            // Tint color red (0-1)
    float TintG;            // Tint color green (0-1)
    float TintB;            // Tint color blue (0-1)
    float TintAmount;       // 0 = no tint, 1 = full tint
    
    // vec4 8: Custom ring color
    float RingR;            // Ring color red (0-1)
    float RingG;            // Ring color green (0-1)
    float RingB;            // Ring color blue (0-1)
    float RingOpacity;      // Ring opacity (0-1)
    
    // vec4 9: Shape configuration
    float ShapeType;        // 0=point, 1=sphere, 2=torus, 3=polygon, 4=orbital
    float ShapeRadius;      // Main radius for sphere/polygon
    float ShapeMajorR;      // Torus major radius
    float ShapeMinorR;      // Torus minor radius
    
    // vec4 10: Shape extras (polygon sides / orbital params)
    float ShapeSideCount;   // Polygon sides OR orbital count
    float OrbitDistance;    // Distance from center to orbital centers
    float OrbitalPhase;     // Current orbital rotation angle (radians)
    float BeamHeight;       // Current beam height (0 = no beam, grows when active)
    
    // vec4 11: Corona/blend config
    float CoronaWidth;      // Glow spread (blocks)
    float CoronaIntensity;  // Brightness multiplier
    float RimPower;         // Rim sharpness (1-5)
    float BlendRadius;      // Smooth min blend (0=sharp, 5+=unified)
    
    // vec4 12: Orbital body color (RGB) + rim falloff
    float OrbitalBodyR;
    float OrbitalBodyG;
    float OrbitalBodyB;
    float OrbitalRimFalloff;
    
    // vec4 13: Orbital corona color (RGBA)
    float OrbitalCoronaR;
    float OrbitalCoronaG;
    float OrbitalCoronaB;
    float OrbitalCoronaA;
    
    // vec4 14: Beam body color (RGB) + beam width scale
    float BeamBodyR;
    float BeamBodyG;
    float BeamBodyB;
    float BeamWidthScale;
    
    // vec4 15: Beam corona color (RGBA)
    float BeamCoronaR;
    float BeamCoronaG;
    float BeamCoronaB;
    float BeamCoronaA;
    
    // vec4 16: Beam geometry extras
    float BeamWidthAbs;     // Absolute width (blocks), 0 = use scale
    float BeamTaper;        // Taper factor (1 = uniform, <1 = narrow top)
    float RetractDelay;     // ms delay after beam shrinks before retract
    float CombinedMode;     // 0 = individual orbital sources, 1 = combined shockwave from center
    
    // vec4 17: Beam corona settings (separate from orbital)
    float BeamCoronaWidth;
    float BeamCoronaIntensity;
    float BeamRimPower;
    float BeamRimFalloff;
    
    // mat4 (vec4 18-21): [DEPRECATED] Was InvViewProj - Now use InvViewProjUBO
    mat4 Reserved_InvViewProj;
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// BASE UBOs - Shared Frame and Camera data
// ═══════════════════════════════════════════════════════════════════════════
#include "include/ubo/frame_ubo.glsl"
#include "include/ubo/camera_ubo.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY INCLUDES
// ═══════════════════════════════════════════════════════════════════════════
// Organized by domain: core -> camera -> sdf -> rendering -> effects

// Core utilities
#include "include/core/constants.glsl"
#include "include/core/math_utils.glsl"

// Camera system
#include "include/camera/types.glsl"
#include "include/camera/basis.glsl"
#include "include/camera/rays.glsl"
#include "include/camera/depth.glsl"

// SDF primitives and operations
#include "include/sdf/primitives.glsl"
#include "include/sdf/operations.glsl"

// Orbital math (flower patterns with CombinedMode/BlendRadius)
#include "include/orbital_math.glsl"
#include "include/sdf/orbital_system.glsl"

// Rendering utilities
#include "include/rendering/screen_effects.glsl"

// Orbital sphere/beam raymarching
#include "include/effects/orbitals_v1.glsl"

// Glow and ring utilities
#include "include/glow_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// SHAPE DISTANCE DISPATCH
// ═══════════════════════════════════════════════════════════════════════════

// Main dispatch - returns distance from worldPos to the shape surface
float getShapeDistance(vec3 worldPos, vec3 shapeCenter) {
    int shapeType = int(ShapeType);
    
    if (shapeType == SHAPE_POINT) {
        // Point: just distance to center (no surface offset)
        return length(worldPos - shapeCenter);
    }
    else if (shapeType == SHAPE_SPHERE) {
        return sdfSphere(worldPos, shapeCenter, ShapeRadius);
    }
    else if (shapeType == SHAPE_TORUS) {
        return sdfTorus(worldPos, shapeCenter, ShapeMajorR, ShapeMinorR);
    }
    else if (shapeType == SHAPE_POLYGON) {
        // Project to XZ plane relative to center
        vec2 planar = worldPos.xz - shapeCenter.xz;
        return sdfPolygon(planar, int(ShapeSideCount), ShapeRadius);
    }
    else if (shapeType == SHAPE_ORBITAL) {
        // Main sphere (ShapeRadius) + orbiting spheres (ShapeMinorR)
        // ShapeSideCount = orbital count, OrbitDistance = distance from center
        float rawDist = sdfOrbitalSystem(worldPos, shapeCenter, ShapeRadius, ShapeMinorR,
                                OrbitDistance, int(ShapeSideCount), OrbitalPhase);
        
        // OUTWARD-ONLY WAVE MODE (BlendRadius >= 0):
        // Clamp SDF to max(0, dist) so pixels INSIDE the combined shape
        // return distance=0. This suppresses inward-propagating shockwave rings.
        if (BlendRadius >= 0.0) {
            return max(0.0, rawDist);  // Outward waves only
        } else {
            return rawDist;  // Full bidirectional waves (legacy)
        }
    }
    
    // Fallback: point distance
    return length(worldPos - shapeCenter);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    bool isSky = (rawDepth > 0.9999);
    
    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN EFFECTS - Apply to ALL pixels including sky
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 baseColor = sceneColor.rgb;
    
    // Apply color tint (multiplicative blend)
    if (TintAmount > 0.001) {
        vec3 tintColor = vec3(TintR, TintG, TintB);
        baseColor = mix(baseColor, baseColor * tintColor, TintAmount);
    }
    
    // Apply vignette
    if (VignetteAmount > 0.001) {
        vec2 uv = texCoord * 2.0 - 1.0;
        float dist = length(uv);
        float vignette = 1.0 - smoothstep(VignetteRadius, 1.0 + VignetteRadius, dist);
        baseColor *= mix(1.0, vignette, VignetteAmount);
    }
    
    // Apply blackout (darken everything)
    if (BlackoutAmount > 0.001) {
        baseColor *= (1.0 - BlackoutAmount);
    }
    
    // For sky pixels with NON-ORBITAL shapes, output now (no rings on sky)
    if (isSky && int(ShapeType) != SHAPE_ORBITAL) {
        fragColor = vec4(baseColor, 1.0);
        return;
    }
    
    // Linearize depth
    float near = 0.05;
    float far = 1000.0;
    float linearDepth = linearizeDepth(rawDepth, near, far);
    bool isAtFarPlane = rawDepth > 0.9999;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CALCULATE DISTANCE TO RING ORIGIN
    // ═══════════════════════════════════════════════════════════════════════
    
    float distanceFromOrigin;
    
    if (UseWorldOrigin > 0.5) {
        // WORLD-ANCHORED MODE: Calculate distance from target world position
        // Use CameraWorldPositionUBO for actual world coordinates
        vec3 camPos = CameraWorldPositionUBO.xyz;
        vec3 forward = normalize(CameraForwardUBO.xyz);
        float fov = CameraUpUBO.w;
        float aspect = CameraForwardUBO.w;
        CameraData cam = buildCameraData(camPos, forward, fov, aspect, near, far);
        
        vec3 worldPos = reconstructWorldPos(texCoord, linearDepth, cam);
        vec3 targetPos = vec3(TargetX, TargetY, TargetZ);
        distanceFromOrigin = getShapeDistance(worldPos, targetPos);
    } else {
        // CAMERA MODE: Distance is just the linear depth (always point-based)
        distanceFromOrigin = linearDepth;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ORBITAL SPHERE RAYMARCHING
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 orbitalColor = vec3(0.0);
    float orbitalAlpha = 0.0;
    
    if (int(ShapeType) == SHAPE_ORBITAL && UseWorldOrigin > 0.5) {
        // Use CameraWorldPositionUBO for world-anchored orbital rendering
        vec3 worldCamPos = CameraWorldPositionUBO.xyz;
        vec3 rawForward = CameraForwardUBO.xyz;
        float fwdLen = length(rawForward);
        
        if (fwdLen > 0.001 && OrbitDistance > 0.1 && ShapeSideCount > 0.5) {
            vec3 forward = rawForward / fwdLen;
            vec3 worldTargetPos = vec3(TargetX, TargetY, TargetZ);
            
            // Build CameraData and generate ray using base UBO values
            float fov = CameraUpUBO.w;
            float aspect = CameraForwardUBO.w;
            float isFlying = CameraClipUBO.z;
            CameraData cam = buildCameraData(worldCamPos, forward, fov, aspect, near, far);
            Ray ray = getRayAdaptive(texCoord, cam, InvViewProjUBO, isFlying);
            vec3 rayDir = ray.direction;
            vec3 rayOrigin = worldCamPos;  // Always use world camera position
            vec3 targetPos = worldTargetPos;  // Always use world target position
            
            // Raymarch for orbital spheres and beams
            float distToTarget = length(targetPos - rayOrigin);
            float maxEffectReach = distToTarget + OrbitDistance + BeamHeight + 100.0;
            float maxRaymarchDist = isAtFarPlane ? maxEffectReach : max(linearDepth + 50.0, maxEffectReach);
            
            // Use new library's param-based version with uniform values
            vec4 hitInfo = raymarchOrbitalSpheres(
                rayOrigin, rayDir, maxRaymarchDist,
                targetPos, ShapeMinorR, OrbitDistance,
                int(ShapeSideCount), OrbitalPhase, BeamHeight,
                // Beam params from uniforms
                BeamWidthAbs, BeamWidthScale, BeamTaper,
                // Rim params from uniforms
                RimPower, BeamRimPower,
                // Corona from uniform
                CoronaWidth
            );
        
            // Unpack result using clean struct (see orbitals_v1.glsl)
            OrbitalHit hit = unpackOrbitalHit(hitInfo);
            
            if (hit.didHit) {
                // Convert ray distance to Z-depth for depth comparison
                float hitZDepth = hit.distance * dot(rayDir, forward);
                bool inFrontOfScene = isAtFarPlane || (hitZDepth < linearDepth);
                
                if (inFrontOfScene) {
                    // Select colors based on what we hit
                    vec3 bodyColor, coronaColor;
                    float alpha, intensity;
                    
                    if (hit.hitType == HIT_BEAM) {
                        bodyColor = vec3(BeamBodyR, BeamBodyG, BeamBodyB);
                        coronaColor = vec3(BeamCoronaR, BeamCoronaG, BeamCoronaB);
                        alpha = BeamCoronaA;
                        intensity = BeamCoronaIntensity;
                    } else {  // HIT_SPHERE
                        bodyColor = vec3(OrbitalBodyR, OrbitalBodyG, OrbitalBodyB);
                        coronaColor = vec3(OrbitalCoronaR, OrbitalCoronaG, OrbitalCoronaB);
                        alpha = OrbitalCoronaA;
                        intensity = CoronaIntensity;
                    }
                    
                    // Apply rim glow (stronger at edges)
                    orbitalColor = coronaColor * hit.rimAmount * 2.0 * intensity;
                    orbitalAlpha = alpha;
                    baseColor = mix(bodyColor, coronaColor, hit.rimAmount * 0.8);
                }
            } else if (hit.rimAmount > 0.01 && isAtFarPlane) {
                // No solid hit, but corona glow visible against sky
                vec3 orbCoronaColor = vec3(OrbitalCoronaR, OrbitalCoronaG, OrbitalCoronaB);
                orbitalColor = orbCoronaColor * hit.rimAmount * CoronaIntensity;
                orbitalAlpha = hit.rimAmount * OrbitalCoronaA;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MULTI-RING CALCULATION
    // ═══════════════════════════════════════════════════════════════════════
    
    float totalMask = 0.0;
    int ringCountInt = clamp(int(RingCount), 1, 50);
    float spacing = max(1.0, RingSpacing);
    float glowW = max(1.0, GlowWidth);
    
    for (int i = 0; i < ringCountInt; i++) {
        float ringOffset = float(i) * spacing;
        float thisRingRadius = RingRadius - ringOffset;
        
        if (thisRingRadius <= 0.0) continue;
        
        float ringFade = 1.0 - (float(i) / float(ringCountInt)) * 0.7;
        totalMask += ringContribution(distanceFromOrigin, thisRingRadius, RingThickness, glowW, Intensity * ringFade);
    }
    
    totalMask = clamp(totalMask, 0.0, 1.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLOR COMPOSITION
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 ringColor = vec3(RingR, RingG, RingB);
    vec3 coreColor = vec3(1.0, 1.0, 1.0);
    vec3 outerGlow = ringColor * 0.5;
    
    vec3 effectColor = mix(outerGlow, ringColor, totalMask);
    effectColor = mix(effectColor, coreColor, totalMask * totalMask);
    
    float ringAlpha = totalMask * RingOpacity;
    
    vec3 finalColor = mix(baseColor, effectColor, ringAlpha);
    finalColor += ringColor * ringAlpha * 0.2;
    
    if (orbitalAlpha > 0.01) {
        finalColor += orbitalColor * orbitalAlpha;
    }
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
