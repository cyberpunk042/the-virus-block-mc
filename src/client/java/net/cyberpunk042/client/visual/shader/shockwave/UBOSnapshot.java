package net.cyberpunk042.client.visual.shader.shockwave;

import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import org.joml.Matrix4f;

/**
 * Snapshot of all state needed to write the Shockwave UBO.
 * ShockwaveRenderer creates this and passes it to ShockwaveUBO.from().
 */
public record UBOSnapshot(
    // Ring state
    float currentRadius,
    RingParams ringParams,
    RingColor ringColor,
    ScreenEffects screenEffects,
    
    // Camera state
    CameraState cameraState,
    boolean targetMode,
    float targetX, float targetY, float targetZ,
    
    // Shape state
    ShapeConfig shapeConfig,
    float orbitalPhase,
    float orbitalSpawnProgress,
    float beamProgress,
    
    // Orbital config
    OrbitalEffectConfig orbitalEffectConfig,
    
    // Flying state (for ray generation mode switching)
    float isFlying,
    Matrix4f invViewProj
) {}
