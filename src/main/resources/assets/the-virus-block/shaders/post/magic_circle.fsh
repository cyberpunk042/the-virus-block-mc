#version 150

// ═══════════════════════════════════════════════════════════════════════════
// MAGIC CIRCLE - Ground Effect Shader
// ═══════════════════════════════════════════════════════════════════════════
//
// Renders animated magic circle patterns on ground/terrain surfaces.
// Uses depth buffer to project onto world geometry.
//
// Phase 2: Per-layer enable/intensity/speed controls
//
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Magic Circle UBO - Phase 3A: Layer controls + geometry
layout(std140) uniform MagicCircleConfig {
    // vec4 0: Center position (world coords) + radius
    float CenterX;
    float CenterY;
    float CenterZ;
    float EffectRadius;
    
    // vec4 1: [DEPRECATED] Was Camera - Now use CameraWorldPositionUBO
    float Reserved1_0;
    float Reserved1_1;
    float Reserved1_2;
    float Reserved1_3;
    
    // vec4 2: [DEPRECATED] Was Camera forward - Now use CameraForwardUBO
    float Reserved2_0;
    float Reserved2_1;
    float Reserved2_2;
    float Reserved2_3;
    
    // vec4 3: Reserved + BreathTime + RotationTime (BreathTime/Time still used)
    float Reserved3_0;
    float Reserved3_1;
    float BreathTime;      // time * breathingSpeed
    float Time;            // time * rotationSpeed
    
    // vec4 4: Visual settings
    float Intensity;
    float GlowExponent;
    float HeightTolerance;
    float Enabled;
    
    // vec4 5: Primary color + breathing amplitude
    float PrimaryR;
    float PrimaryG;
    float PrimaryB;
    float BreathingAmount;  // Global breathing amplitude
    
    // mat4 (vec4 6-9): [DEPRECATED] Was InvViewProj - Now use InvViewProjUBO
    mat4 Reserved_InvViewProj;
    
    // vec4 10-11: Layer enables (8 floats)
    vec4 LayerEnables1;  // Layers 1-4
    vec4 LayerEnables2;  // Layers 5-8
    
    // vec4 12-13: Layer intensities (8 floats)
    vec4 LayerIntensities1;  // Layers 1-4
    vec4 LayerIntensities2;  // Layers 5-8
    
    // vec4 14-15: Layer speeds (8 floats)
    vec4 LayerSpeeds1;  // Layers 1-4
    vec4 LayerSpeeds2;  // Layers 5-8
    
    // vec4 16: Layer 4 Geometry (Middle Ring)
    vec4 Layer4Geometry;  // innerR, outerR, thickness, rotOffset
    
    // vec4 17-18: Layer 7 Geometry (Inner Radiation)
    vec4 Layer7Geometry1;  // innerR, outerR, spokeCount, thickness
    vec4 Layer7Geometry2;  // rotOffset, reserved, reserved, reserved
    
    // vec4 19-20: Layer 2 Geometry (Hexagram)
    vec4 Layer2Geometry1;  // rectCount, rectSize, thickness, rotOffset
    vec4 Layer2Geometry2;  // snapRotation, reserved, reserved, reserved
    
    // vec4 21-22: Layer 5 Geometry (Inner Triangle)
    vec4 Layer5Geometry1;  // rectCount, rectSize, thickness, rotOffset
    vec4 Layer5Geometry2;  // snapRotation, reserved, reserved, reserved
    
    // vec4 23-24: Layer 3 Geometry (Outer Dot Ring)
    vec4 Layer3Geometry1;  // dotCount, orbitRadius, ringInner, ringOuter
    vec4 Layer3Geometry2;  // ringThickness, dotRadius, dotThickness, rotOffset
    
    // vec4 25-26: Layer 6 Geometry (Inner Dot Ring)
    vec4 Layer6Geometry1;  // dotCount, orbitRadius, ringInner, ringOuter
    vec4 Layer6Geometry2;  // ringThickness, dotRadius, dotThickness, rotOffset
    
    // vec4 27-28: Layer 1 Geometry (Outer Ring + Radiation)
    vec4 Layer1Geometry1;  // ringInner, ringOuter, ringThickness, radInner
    vec4 Layer1Geometry2;  // radOuter, radCount, radThickness, rotOffset
    
    // vec4 29-31: Layer 8 Geometry (Spinning Core)
    vec4 Layer8Geometry1;  // breathAmp, breathCenter, orbitalCount, orbitalStart
    vec4 Layer8Geometry2;  // orbitalStep, orbitalDist, orbitalThickness, centerRadius
    vec4 Layer8Geometry3;  // centerThickness, rotOffset, animationStage, transitionMode
    
    // vec4 32: Animation extra
    vec4 AnimationExtra;   // animationFromCenter, reserved, reserved, reserved
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// BASE UBOs - Shared Frame and Camera data
// ═══════════════════════════════════════════════════════════════════════════
#include "include/ubo/frame_ubo.glsl"
#include "include/ubo/camera_ubo.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// INCLUDES
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/constants.glsl"
#include "include/camera/types.glsl"
#include "include/camera/basis.glsl"
#include "include/camera/depth.glsl"
#include "include/effects/magic_circle.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Early out if disabled
    if (Enabled < 0.5) {
        fragColor = sceneColor;
        return;
    }
    
    // Sky check - no magic circles in the sky
    if (rawDepth > 0.9999) {
        fragColor = sceneColor;
        return;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // WORLD POSITION RECONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    float near = 0.05;
    float far = 1000.0;
    float linearDepth = linearizeDepth(rawDepth, near, far);
    
    // Use CameraWorldPositionUBO for world-anchored ground projection
    vec3 camPos = CameraWorldPositionUBO.xyz;
    vec3 forward = normalize(CameraForwardUBO.xyz);
    float fov = CameraUpUBO.w;
    float aspect = CameraForwardUBO.w;
    
    CameraData cam = buildCameraData(camPos, forward, fov, aspect, near, far);
    vec3 worldPos = reconstructWorldPos(texCoord, linearDepth, cam);
    
    // ═══════════════════════════════════════════════════════════════════════
    // GROUND PROJECTION
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 center = vec3(CenterX, CenterY, CenterZ);
    
    // Height check - only render on surfaces near the target Y level
    float heightDiff = abs(worldPos.y - center.y);
    if (heightDiff > HeightTolerance) {
        fragColor = sceneColor;
        return;
    }
    float heightFade = 1.0 - smoothstep(0.0, HeightTolerance, heightDiff);
    
    // Project to XZ plane (2D ground coordinates)
    vec2 groundPos = worldPos.xz - center.xz;
    
    // Normalize to effect radius (0 = center, 1 = edge)
    vec2 normalizedPos = groundPos / EffectRadius;
    
    // Distance check - only render within effect radius
    float dist = length(normalizedPos);
    if (dist > 1.2) {
        // Allow some glow past edge
        fragColor = sceneColor;
        return;
    }
    
    // Edge fade
    float edgeFade = 1.0 - smoothstep(0.9, 1.1, dist);
    
    // ═══════════════════════════════════════════════════════════════════════
    // PACK LAYER PARAMS FOR RENDER FUNCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    // Convert enables to bools (>0.5 = enabled)
    bool layerEnable[8];
    layerEnable[0] = LayerEnables1.x > 0.5;
    layerEnable[1] = LayerEnables1.y > 0.5;
    layerEnable[2] = LayerEnables1.z > 0.5;
    layerEnable[3] = LayerEnables1.w > 0.5;
    layerEnable[4] = LayerEnables2.x > 0.5;
    layerEnable[5] = LayerEnables2.y > 0.5;
    layerEnable[6] = LayerEnables2.z > 0.5;
    layerEnable[7] = LayerEnables2.w > 0.5;
    
    // Intensities as array
    float layerIntensity[8];
    layerIntensity[0] = LayerIntensities1.x;
    layerIntensity[1] = LayerIntensities1.y;
    layerIntensity[2] = LayerIntensities1.z;
    layerIntensity[3] = LayerIntensities1.w;
    layerIntensity[4] = LayerIntensities2.x;
    layerIntensity[5] = LayerIntensities2.y;
    layerIntensity[6] = LayerIntensities2.z;
    layerIntensity[7] = LayerIntensities2.w;
    
    // Speeds as array
    float layerSpeed[8];
    layerSpeed[0] = LayerSpeeds1.x;
    layerSpeed[1] = LayerSpeeds1.y;
    layerSpeed[2] = LayerSpeeds1.z;
    layerSpeed[3] = LayerSpeeds1.w;
    layerSpeed[4] = LayerSpeeds2.x;
    layerSpeed[5] = LayerSpeeds2.y;
    layerSpeed[6] = LayerSpeeds2.z;
    layerSpeed[7] = LayerSpeeds2.w;
    
    // ═══════════════════════════════════════════════════════════════════════
    // GLOBAL BREATHING - applies to all layers
    // ═══════════════════════════════════════════════════════════════════════
    
    // Calculate global breathing scale using BreathTime and BreathingAmount
    // BreathTime = time * breathingSpeed, BreathingAmount = amplitude
    // Center is 1.0 (no change), oscillates by BreathingAmount
    float breathScale = sin(PI * BreathTime) * BreathingAmount + 1.0;
    
    // Apply breathing by scaling the normalized position
    // (smaller normalizedPos = larger circle appearance)
    vec2 breathedPos = normalizedPos / breathScale;
    
    // ═══════════════════════════════════════════════════════════════════════
    // RENDER MAGIC CIRCLE
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 primaryColor = vec3(PrimaryR, PrimaryG, PrimaryB);
    
    // Pack Layer 4 geometry
    Layer4Params l4;
    l4.innerRadius = Layer4Geometry.x;
    l4.outerRadius = Layer4Geometry.y;
    l4.thickness = Layer4Geometry.z;
    l4.rotOffset = Layer4Geometry.w;
    
    // Pack Layer 7 geometry
    Layer7Params l7;
    l7.innerRadius = Layer7Geometry1.x;
    l7.outerRadius = Layer7Geometry1.y;
    l7.spokeCount = int(Layer7Geometry1.z);
    l7.thickness = Layer7Geometry1.w;
    l7.rotOffset = Layer7Geometry2.x;
    
    // Pack Layer 2 geometry (Phase 3B)
    Layer2Params l2;
    l2.rectCount = int(Layer2Geometry1.x);
    l2.rectSize = Layer2Geometry1.y;
    l2.thickness = Layer2Geometry1.z;
    l2.rotOffset = Layer2Geometry1.w;
    l2.snapRotation = Layer2Geometry2.x > 0.5;
    
    // Pack Layer 5 geometry (Phase 3B)
    Layer5Params l5;
    l5.rectCount = int(Layer5Geometry1.x);
    l5.rectSize = Layer5Geometry1.y;
    l5.thickness = Layer5Geometry1.z;
    l5.rotOffset = Layer5Geometry1.w;
    l5.snapRotation = Layer5Geometry2.x > 0.5;
    
    // Pack Layer 3 geometry (Phase 3C)
    Layer3Params l3;
    l3.dotCount = int(Layer3Geometry1.x);
    l3.orbitRadius = Layer3Geometry1.y;
    l3.ringInner = Layer3Geometry1.z;
    l3.ringOuter = Layer3Geometry1.w;
    l3.ringThickness = Layer3Geometry2.x;
    l3.dotRadius = Layer3Geometry2.y;
    l3.dotThickness = Layer3Geometry2.z;
    l3.rotOffset = Layer3Geometry2.w;
    
    // Pack Layer 6 geometry (Phase 3C)
    Layer6Params l6;
    l6.dotCount = int(Layer6Geometry1.x);
    l6.orbitRadius = Layer6Geometry1.y;
    l6.ringInner = Layer6Geometry1.z;
    l6.ringOuter = Layer6Geometry1.w;
    l6.ringThickness = Layer6Geometry2.x;
    l6.dotRadius = Layer6Geometry2.y;
    l6.dotThickness = Layer6Geometry2.z;
    l6.rotOffset = Layer6Geometry2.w;
    
    // Pack Layer 1 geometry (Phase 3D)
    Layer1Params l1;
    l1.ringInner = Layer1Geometry1.x;
    l1.ringOuter = Layer1Geometry1.y;
    l1.ringThickness = Layer1Geometry1.z;
    l1.radInner = Layer1Geometry1.w;
    l1.radOuter = Layer1Geometry2.x;
    l1.radCount = int(Layer1Geometry2.y);
    l1.radThickness = Layer1Geometry2.z;
    l1.rotOffset = Layer1Geometry2.w;
    
    // Pack Layer 8 geometry (Phase 3D)
    Layer8Params l8;
    l8.breathAmp = Layer8Geometry1.x;
    l8.breathCenter = Layer8Geometry1.y;
    l8.orbitalCount = int(Layer8Geometry1.z);
    l8.orbitalStart = Layer8Geometry1.w;
    l8.orbitalStep = Layer8Geometry2.x;
    l8.orbitalDist = Layer8Geometry2.y;
    l8.orbitalThickness = Layer8Geometry2.z;
    l8.centerRadius = Layer8Geometry2.w;
    l8.centerThickness = Layer8Geometry3.x;
    l8.rotOffset = Layer8Geometry3.y;
    
    // ═══════════════════════════════════════════════════════════════════════
    // STAGE ANIMATION (Phase 4)
    // ═══════════════════════════════════════════════════════════════════════
    
    float animationStage = Layer8Geometry3.z;  // 0-8
    int transitionMode = int(Layer8Geometry3.w + 0.5);  // 0=INSTANT, 1=FADE, 2=SCALE, 3=BOTH
    bool fromCenter = AnimationExtra.x > 0.5;  // true = center outward (layer 8 first)
    
    // Track overall animation progress for SCALE mode (0-1)
    float overallProgress = animationStage / 8.0;
    
    // Calculate per-layer visibility based on animation stage
    // fromCenter: stage 1 = layer 8 (core) appears first, stage 8 = layer 1 (outer) last
    // !fromCenter: stage 1 = layer 1 (outer) appears first, stage 8 = layer 8 (core) last
    for (int i = 0; i < 8; i++) {
        // Map layer index to animation order based on direction
        int layerOrder = fromCenter ? (7 - i) : i;  // Reverse order if from center
        float phase = clamp(animationStage - float(layerOrder), 0.0, 1.0);
        
        // Apply transition based on mode
        float fadeMultiplier = 1.0;
        
        if (transitionMode == 0) {
            // INSTANT: binary visibility
            fadeMultiplier = phase > 0.01 ? 1.0 : 0.0;
        } else if (transitionMode == 1) {
            // FADE: smooth intensity transition
            fadeMultiplier = smoothstep(0.0, 1.0, phase);
        } else if (transitionMode == 2) {
            // SCALE: binary visibility (scale handled below)
            fadeMultiplier = phase > 0.01 ? 1.0 : 0.0;
        } else {
            // BOTH (3): fade + scale
            fadeMultiplier = smoothstep(0.0, 1.0, phase);
        }
        
        // Modulate intensity by fade multiplier
        layerIntensity[i] *= fadeMultiplier;
    }
    
    // For SCALE and BOTH modes, apply scale to the position
    // This makes the circle grow from small (0.5) to full size (1.0)
    vec2 animatedPos = breathedPos;
    if (transitionMode >= 2) {
        // Scale factor: starts at 0.3, grows to 1.0 as animation progresses
        float scaleMin = 0.3;
        float scaleFactor = mix(scaleMin, 1.0, smoothstep(0.0, 1.0, overallProgress));
        // Divide position by scale to make circle appear larger
        animatedPos = breathedPos / scaleFactor;
    }
    
    MagicCircleResult mc = renderMagicCircleFinal(
        animatedPos,  // Use scale-transformed position
        Time,
        Intensity,
        primaryColor,
        GlowExponent,
        layerEnable,
        layerIntensity,
        layerSpeed,
        l4,
        l7,
        l2,
        l5,
        l3,
        l6,
        l1,
        l8
    );
    
    // Apply fades
    float finalAlpha = mc.alpha * heightFade * edgeFade;
    
    // ═══════════════════════════════════════════════════════════════════════
    // COMPOSITE
    // ═══════════════════════════════════════════════════════════════════════
    
    // Additive blend for glow effect
    vec3 finalColor = sceneColor.rgb + mc.color * finalAlpha;
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}

