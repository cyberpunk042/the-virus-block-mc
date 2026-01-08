package net.cyberpunk042.client.visual.shader.shockwave;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.client.visual.shader.shockwave.UBOSnapshot;
import net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter;
import java.util.Set;

/**
 * Handles GPU resource loading and uniform buffer writing for Shockwave VFX.
 */
public class ShockwaveRenderer {

    private static final Identifier SHADER_ID = Identifier.of("the-virus-block", "shockwave_ring");
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;

    public static PostEffectProcessor loadProcessor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        ShaderLoader shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) return null;
        
        try {
            return shaderLoader.loadPostEffect(SHADER_ID, REQUIRED_TARGETS);
        } catch (Exception e) {
            Logging.RENDER.topic("shockwave_gpu").error("Failed to load shockwave shader", e);
            return null;
        }
    }

    public static void writeUniformBuffer(Std140Builder builder, float aspectRatio, float fovRadians, 
                                          float isFlying, org.joml.Matrix4f invViewProj) {
        ShockwaveController ctrl = ShockwaveController.getInstance();
        
        // Create snapshot of current state for the render thread
        UBOSnapshot snapshot = new UBOSnapshot(
            ctrl.getCurrentRadius(),
            ctrl.getRingParams(),
            ctrl.getRingColor(),
            ctrl.getScreenEffects(),
            ctrl.getCameraState(),
            ctrl.isTargetMode(),
            ctrl.getTargetX(), ctrl.getTargetY(), ctrl.getTargetZ(),
            ctrl.getShapeConfig(),
            ctrl.getOrbitalPhase(),
            ctrl.getOrbitalSpawnProgress(),
            ctrl.getBeamProgress(),
            ctrl.getOrbitalEffectConfig(),
            isFlying,
            invViewProj
        );
        
        // Build UBO record and write using reflection
        ShockwaveUBO ubo = ShockwaveUBO.from(snapshot, aspectRatio, fovRadians);
        ReflectiveUBOWriter.write(builder, ubo);
    }
}
