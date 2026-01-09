package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.client.visual.ubo.Vec4Serializable;
import net.cyberpunk042.client.visual.ubo.annotation.*;
import org.joml.Matrix4f;

/**
 * UBO structure for Virus Block effect.
 * 
 * <p>Uses the same ReflectiveUBOWriter pattern as ShockwaveUBO.</p>
 * 
 * <h3>Layout: 13 vec4 + 1 mat4 + 32 vec4 = 49 vec4 slots = 784 bytes</h3>
 * 
 * @see net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter
 */
@UBOStruct(name = "VirusBlockParams", glslPath = "the-virus-block:shaders/post/virus_block.fsh")
public record VirusBlockUBO(
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA AND TIME (Slots 0-4)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 CameraPosTimeVec4 cameraPosTime,       // Slot 0: xyz = camera pos, w = time
    @Vec4 EffectFlagsVec4 effectFlags,           // Slot 1: blockCount, espEnabled, smokeEnabled, screenPoisonEnabled
    @Vec4 InvProjParamsVec4 invProjParams,       // Slot 2: near, far, fov, aspect
    @Vec4 CameraForwardVec4 cameraForward,       // Slot 3: xyz = forward, w = closestBlockDist
    @Vec4 CameraUpVec4 cameraUp,                 // Slot 4: xyz = up, w = isFlying
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SMOKE CONFIG (Slots 5-7)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 SmokeShapeVec4 smokeShape,             // Slot 5: height, spread, density, turbulence
    @Vec4 SmokeAnimVec4 smokeAnim,               // Slot 6: riseSpeed, swirl, noiseScale, fadeHeight
    @Vec4 SmokeColorVec4 smokeColor,             // Slot 7: r, g, b, intensity
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN POISON AND ESP (Slots 8-12)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 ScreenPoisonParamsVec4 screenPoisonParams,  // Slot 8: triggerDist, intensity, distortAmount, unused
    @Vec4 ESPCloseVec4 espClose,                 // Slot 9: rgb, threshold
    @Vec4 ESPMediumVec4 espMedium,               // Slot 10: rgb, threshold
    @Vec4 ESPFarVec4 espFar,                     // Slot 11: rgb, unused
    @Vec4 ESPStyleVec4 espStyle,                 // Slot 12: pulseSpeed, glowPower, glowSize, edgeSharpness
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MATRIX (Slots 13-16)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Mat4 Matrix4f invViewProj,                  // Slots 13-16: inverse view-projection
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BLOCK POSITIONS (Slots 17-48)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4Array(count = 32) BlockPosVec4[] blockPositions  // Slots 17-48: xyz = pos, w = active
) {
    /** Buffer size in bytes */
    public static final int BUFFER_SIZE = net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter.calculateBufferSize(VirusBlockUBO.class);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 TYPES - All implement Vec4Serializable
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record CameraPosTimeVec4(float x, float y, float z, float time) implements Vec4Serializable {
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return time; }
    }
    
    public record EffectFlagsVec4(float blockCount, float espEnabled, float smokeEnabled, float screenPoisonEnabled) implements Vec4Serializable {
        @Override public float slot0() { return blockCount; }
        @Override public float slot1() { return espEnabled; }
        @Override public float slot2() { return smokeEnabled; }
        @Override public float slot3() { return screenPoisonEnabled; }
    }
    
    public record InvProjParamsVec4(float near, float far, float fov, float aspect) implements Vec4Serializable {
        @Override public float slot0() { return near; }
        @Override public float slot1() { return far; }
        @Override public float slot2() { return fov; }
        @Override public float slot3() { return aspect; }
    }
    
    public record CameraForwardVec4(float x, float y, float z, float closestBlockDist) implements Vec4Serializable {
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return closestBlockDist; }
    }
    
    public record CameraUpVec4(float x, float y, float z, float isFlying) implements Vec4Serializable {
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return isFlying; }
    }
    
    public record SmokeShapeVec4(float height, float spread, float density, float turbulence) implements Vec4Serializable {
        @Override public float slot0() { return height; }
        @Override public float slot1() { return spread; }
        @Override public float slot2() { return density; }
        @Override public float slot3() { return turbulence; }
    }
    
    public record SmokeAnimVec4(float riseSpeed, float swirl, float noiseScale, float fadeHeight) implements Vec4Serializable {
        @Override public float slot0() { return riseSpeed; }
        @Override public float slot1() { return swirl; }
        @Override public float slot2() { return noiseScale; }
        @Override public float slot3() { return fadeHeight; }
    }
    
    public record SmokeColorVec4(float r, float g, float b, float intensity) implements Vec4Serializable {
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return intensity; }
    }
    
    public record ScreenPoisonParamsVec4(float triggerDist, float intensity, float distortAmount, float unused) implements Vec4Serializable {
        @Override public float slot0() { return triggerDist; }
        @Override public float slot1() { return intensity; }
        @Override public float slot2() { return distortAmount; }
        @Override public float slot3() { return unused; }
    }
    
    public record ESPCloseVec4(float r, float g, float b, float threshold) implements Vec4Serializable {
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return threshold; }
    }
    
    public record ESPMediumVec4(float r, float g, float b, float threshold) implements Vec4Serializable {
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return threshold; }
    }
    
    public record ESPFarVec4(float r, float g, float b, float unused) implements Vec4Serializable {
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return unused; }
    }
    
    public record ESPStyleVec4(float pulseSpeed, float glowPower, float glowSize, float edgeSharpness) implements Vec4Serializable {
        @Override public float slot0() { return pulseSpeed; }
        @Override public float slot1() { return glowPower; }
        @Override public float slot2() { return glowSize; }
        @Override public float slot3() { return edgeSharpness; }
    }
    
    public record BlockPosVec4(float x, float y, float z, float active) implements Vec4Serializable {
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return active; }
    }
}
