package net.cyberpunk042.client.visual.shader.shockwave;

import net.cyberpunk042.client.visual.ubo.annotation.*;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveVec4Types.*;
import org.joml.Matrix4f;

/**
 * Complete UBO structure for Shockwave effect.
 * 
 * <p><b>This record IS THE DOCUMENTATION for the UBO layout.</b>
 * Field order here = slot order in GLSL. Adding/removing/reordering
 * components here automatically changes the UBO structure.</p>
 * 
 * <h3>Layout: 18 vec4 + 1 mat4 = 22 vec4 slots = 352 bytes</h3>
 * <ul>
 *   <li>Slots 0-1: Basic ring params</li>
 *   <li>Slots 2-5: Target and camera</li>
 *   <li>Slots 6-8: Screen effects and ring color</li>
 *   <li>Slots 9-10: Shape config</li>
 *   <li>Slots 11-13: Orbital config</li>
 *   <li>Slots 14-17: Beam config</li>
 *   <li>Slots 18-21: InvViewProj matrix</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter
 */
@UBOStruct(name = "ShockwaveConfig", glslPath = "the-virus-block:shaders/post/shockwave.fsh")
public record ShockwaveUBO(
    // ═══════════════════════════════════════════════════════════════════════════
    // BASIC PARAMS (Slots 0-1)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 BasicParamsVec4 basicParams,          // Slot 0: radius, thickness, intensity, time
    @Vec4 RingCountVec4 ringCount,              // Slot 1: count, spacing, contractMode, glowWidth
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TARGET AND CAMERA (Slots 2-5)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 TargetPosVec4 targetPos,              // Slot 2: x, y, z, useWorldOrigin
    @Vec4 CameraPosVec4 cameraPos,              // Slot 3: x, y, z, aspect
    @Vec4 CameraForwardVec4 cameraForward,      // Slot 4: x, y, z, fov
    @Vec4 CameraUpVec4 cameraUp,                // Slot 5: x, y, z, isFlying
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN EFFECTS AND RING COLOR (Slots 6-8)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 ScreenVignetteVec4 screenVignette,    // Slot 6: blackout, vignette, radius, reserved
    @Vec4 ScreenTintVec4 screenTint,            // Slot 7: tintR, tintG, tintB, tintAmount
    @Vec4 RingColorVec4 ringColor,              // Slot 8: r, g, b, opacity
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE CONFIG (Slots 9-10)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 ShapeConfigVec4 shapeConfig,          // Slot 9: type, radius, majorRadius, minorRadius
    @Vec4 ShapeExtrasVec4 shapeExtras,          // Slot 10: sideCount, orbitDist, phase, beamHeight
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORBITAL CONFIG (Slots 11-13)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 CoronaConfigVec4 coronaConfig,        // Slot 11: width, intensity, rimPower, blendRadius
    @Vec4 OrbitalBodyVec4 orbitalBody,          // Slot 12: bodyR, bodyG, bodyB, rimFalloff
    @Vec4 OrbitalCoronaColorVec4 orbitalCorona, // Slot 13: r, g, b, a
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BEAM CONFIG (Slots 14-17)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 BeamBodyVec4 beamBody,                // Slot 14: bodyR, bodyG, bodyB, widthScale
    @Vec4 BeamCoronaColorVec4 beamCoronaColor,  // Slot 15: r, g, b, a
    @Vec4 BeamGeometryVec4 beamGeometry,        // Slot 16: width, taper, retractDelay, combinedMode
    @Vec4 BeamCoronaVec4 beamCorona,            // Slot 17: width, intensity, rimPower, rimFalloff
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MATRIX (Slots 18-21)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Mat4 Matrix4f invViewProj                  // Slots 18-21: inverse view-projection
) {
    
    /** Buffer size in bytes, calculated from annotations */
    public static final int BUFFER_SIZE = net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter.calculateBufferSize(ShockwaveUBO.class);
    
    /**
     * Factory method to build from UBOSnapshot (bridges old API to new).
     * 
     * @param snapshot Current state snapshot from ShockwavePostEffect
     * @param aspectRatio Screen aspect ratio
     * @param fovRadians Field of view in radians
     * @return A complete UBO ready for serialization
     */
    public static ShockwaveUBO from(UBOSnapshot snapshot, 
                                    float aspectRatio, float fovRadians) {
        RingParams ring = snapshot.ringParams();
        CameraState cam = snapshot.cameraState();
        ShapeConfig shape = snapshot.shapeConfig();
        OrbitalEffectConfig orbital = snapshot.orbitalEffectConfig();
        BeamVisualConfig beam = orbital.beam();
        
        // Calculate animated values
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        float animatedOrbitDistance = shape.orbitDistance() * snapshot.orbitalSpawnProgress();
        float beamHeight = orbital.timing().beamHeight();
        float effectiveBeamHeight = beamHeight <= 0.01f ? 10000f : beamHeight;
        float animatedBeamHeight = snapshot.beamProgress() * effectiveBeamHeight;
        
        return new ShockwaveUBO(
            // Basic params (slots 0-1)
            BasicParamsVec4.from(snapshot.currentRadius(), ring, time),
            RingCountVec4.from(ring),
            
            // Target and camera (slots 2-5)
            new TargetPosVec4(snapshot.targetX(), snapshot.targetY(), snapshot.targetZ(),
                snapshot.targetMode() ? 1f : 0f),
            CameraPosVec4.from(cam, aspectRatio),
            CameraForwardVec4.from(cam, fovRadians),
            CameraUpVec4.fromFlying(snapshot.isFlying()),
            
            // Screen effects (slots 6-8)
            ScreenVignetteVec4.from(snapshot.screenEffects()),
            ScreenTintVec4.from(snapshot.screenEffects()),
            RingColorVec4.from(snapshot.ringColor()),
            
            // Shape config (slots 9-10)
            ShapeConfigVec4.from(shape),
            new ShapeExtrasVec4((float) shape.sideCount(), animatedOrbitDistance, 
                snapshot.orbitalPhase(), animatedBeamHeight),
            
            // Orbital config (slots 11-13)
            CoronaConfigVec4.from(orbital.orbital().corona(), orbital.blendRadius()),
            OrbitalBodyVec4.from(orbital.orbital()),
            OrbitalCoronaColorVec4.from(orbital.orbital().corona()),
            
            // Beam config (slots 14-17)
            BeamBodyVec4.from(beam),
            BeamCoronaColorVec4.from(beam),
            BeamGeometryVec4.from(beam, orbital.timing().retractDelay(), orbital.combinedMode()),
            BeamCoronaVec4.from(beam),
            
            // Matrix (slots 18-21)
            snapshot.invViewProj()
        );
    }
}
