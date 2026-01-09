package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;
import net.cyberpunk042.client.visual.effect.FieldVisualUBO;
import net.cyberpunk042.client.visual.effect.ShaderTimeSource;
import net.cyberpunk042.client.visual.effect.uniform.FieldVisualUniformBinder;
import net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter;
import net.cyberpunk042.client.visual.ubo.BaseUBOBinder;
import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.client.visual.shader.MagicCirclePostEffect;
import net.cyberpunk042.client.visual.shader.VirusBlockPostEffect;
import net.cyberpunk042.client.visual.shader.VirusBlockUBO;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin into PostEffectPass to inject dynamic uniform values.
 * 
 * <p>This intercepts the render() call and updates the uniformBuffers map
 * with current values from ShockwavePostEffect or FieldVisualPostEffect
 * BEFORE the pass executes.</p>
 */
@Mixin(PostEffectPass.class)
public class PostEffectPassMixin {
    
    @Shadow @Final private Map<String, GpuBuffer> uniformBuffers;
    @Shadow @Final private String id;
    
    private static int injectCount = 0;
    private static int fieldVisualInjectCount = 0;
    private static int magicCircleInjectCount = 0;
    private static int virusBlockInjectCount = 0;
    
    /**
     * Safely close an old buffer before replacing it.
     * Prevents GPU memory leaks and stale state.
     */
    private void closeOldBuffer(String bufferName) {
        GpuBuffer oldBuffer = uniformBuffers.get(bufferName);
        if (oldBuffer != null && !oldBuffer.isClosed()) {
            oldBuffer.close();
        }
    }
    
    /**
     * Inject at HEAD of render() to update uniforms before the pass runs.
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void theVirusBlock$updatePostEffectUniforms(
            FrameGraphBuilder frameGraphBuilder,
            Map<Identifier, Handle<Framebuffer>> targets,
            GpuBufferSlice bufferSlice,
            CallbackInfo ci
    ) {
        
        // ═══════════════════════════════════════════════════════════════════════════
        // BASE UBOs - Update once per frame (FrameData, CameraData)
        // These are shared across all effects, written before effect-specific updates
        // ═══════════════════════════════════════════════════════════════════════════
        BaseUBOBinder.updateBaseUBOs(uniformBuffers);
        
        // Handle Shockwave pass
        if (ShockwavePostEffect.isEnabled() && id != null && id.contains("shockwave")) {
            updateShockwaveUniforms();
        }
        
        // DEBUG: Log all passes to see what's happening
        if (id != null && (id.contains("field") || id.contains("the-virus") || id.contains("magic_circle") || id.contains("virus_block"))) {
            Logging.RENDER.topic("posteffect_debug")
                .kv("passId", id)
                .kv("fieldVisualEnabled", FieldVisualPostEffect.isEnabled())
                .kv("magicCircleEnabled", MagicCirclePostEffect.isEnabled())
                .info("Post effect pass intercepted");
        }
        
        // Handle Field Visual pass
        if (FieldVisualPostEffect.isEnabled() && id != null && id.contains("field_visual")) {
            updateFieldVisualUniforms();
        }
        
        // Handle Magic Circle pass
        if (MagicCirclePostEffect.isEnabled() && id != null && id.contains("magic_circle")) {
            updateMagicCircleUniforms();
        }
        
        // Handle Virus Block pass
        if (net.cyberpunk042.client.visual.shader.VirusBlockUniformBinder.isEnabled() 
            && id != null && id.contains("virus_block")) {
            updateVirusBlockUniforms();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLYING STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private float getEffectiveIsFlying() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null && client.player.getAbilities().flying) {
            return 1.0f;
        }
        return 0.0f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHOCKWAVE UNIFORM UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateShockwaveUniforms() {
        injectCount++;
        
        if (injectCount % 60 == 1) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("id", id)
                .kv("hasConfig", uniformBuffers.containsKey("ShockwaveConfig"))
                .debug("Intercepting shockwave pass");
        }
        
        if (!uniformBuffers.containsKey("ShockwaveConfig")) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, ShockwavePostEffect.BUFFER_SIZE + 16);
            
            var client = net.minecraft.client.MinecraftClient.getInstance();
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            
            var camera = client.gameRenderer.getCamera();
            float tickDelta = client.getRenderTickCounter().getTickProgress(false);
            float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);
            float fov = (float) Math.toRadians(dynamicFov);
            
            float isFlying = getEffectiveIsFlying();
            
            Matrix4f invViewProj = ShockwavePostEffect.getInvViewProj();
            
            ShockwavePostEffect.tickOrbitalPhase();
            ShockwavePostEffect.writeUniformBuffer(builder, aspect, fov, isFlying, invViewProj);
            
            // Close old buffer to prevent GPU memory leak
            closeOldBuffer("ShockwaveConfig");
            
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "ShockwaveConfig Dynamic",
                16,
                builder.get()
            );
            
            uniformBuffers.put("ShockwaveConfig", newBuffer);
            
            if (injectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("radius", String.format("%.1f", ShockwavePostEffect.getCurrentRadius()))
                    .debug("Updated ShockwaveConfig UBO");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update ShockwaveConfig");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD VISUAL UNIFORM UPDATE (Using ReflectiveUBOWriter)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateFieldVisualUniforms() {
        fieldVisualInjectCount++;
        
        if (!uniformBuffers.containsKey("FieldVisualConfig")) {
            if (fieldVisualInjectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("availableBuffers", uniformBuffers.keySet().toString())
                    .warn("FieldVisualConfig UBO not found in pass");
            }
            return;
        }
        
        FieldVisualInstance currentField = FieldVisualPostEffect.getCurrentField();
        if (currentField == null) {
            if (fieldVisualInjectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .warn("No current field set for rendering");
            }
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, FieldVisualUBO.BUFFER_SIZE + 16);
            
            var client = net.minecraft.client.MinecraftClient.getInstance();
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            
            net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
            float tickDelta = FieldVisualPostEffect.getTickDelta();
            float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);
            float fov = (float) Math.toRadians(dynamicFov);
            
            // Use synchronized time source (world time by default for multiplayer sync)
            float globalTime = ShaderTimeSource.getTime();
            float isFlying = getEffectiveIsFlying();
            
            Vec3d cameraPos = FieldVisualUniformBinder.getCameraPosition();
            Matrix4f invViewProj = FieldVisualUniformBinder.getInvViewProjection();
            Matrix4f viewProj = FieldVisualUniformBinder.getViewProjection();
            
            FieldVisualConfig config = currentField.getConfig();
            Vec3d fieldPos = currentField.getRenderPosition(tickDelta);
            
            // Calculate camera-relative position
            Vec3d relativePos = fieldPos.subtract(cameraPos);
            
            // Build position params
            PositionParams position = new PositionParams(
                (float) relativePos.x,
                (float) relativePos.y,
                (float) relativePos.z,
                currentField.getEffectiveRadius()  // Warmup-scaled radius
            );
            
            // Build camera params (camera at origin in camera-relative coords)
            CameraParams cameraParams = new CameraParams(
                0f, 0f, 0f, globalTime,
                FieldVisualUniformBinder.getForwardX(),
                FieldVisualUniformBinder.getForwardY(),
                FieldVisualUniformBinder.getForwardZ(),
                aspect,
                0f, 1f, 0f, fov
            );
            
            // Build render params
            RenderParams renderParams = new RenderParams(0.05f, 1000f, isFlying);
            
            // Build debug params
            DebugParams debugParams = DebugParams.DEFAULT;
            
            // Build complete UBO record with warmup scaling
            float warmupProgress = currentField.getWarmupProgress();
            FieldVisualUBO ubo = FieldVisualUBO.fromWithWarmup(
                config,
                position,
                cameraParams,
                renderParams,
                invViewProj,
                viewProj,
                debugParams,
                warmupProgress
            );
            ReflectiveUBOWriter.write(builder, ubo);
            
            // Close old buffer to prevent GPU memory leak
            closeOldBuffer("FieldVisualConfig");
            
            // Create and put buffer
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "FieldVisualConfig Dynamic",
                16,
                builder.get()
            );
            
            uniformBuffers.put("FieldVisualConfig", newBuffer);
            
            if (fieldVisualInjectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("pos", String.format("%.1f,%.1f,%.1f", fieldPos.x, fieldPos.y, fieldPos.z))
                    .kv("radius", String.format("%.1f/%.1f", currentField.getEffectiveRadius(), currentField.getRadius()))
                    .kv("time", String.format("%.1f", globalTime))
                    .info("Updated FieldVisualConfig UBO via writer");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update FieldVisualConfig");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAGIC CIRCLE UNIFORM UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateMagicCircleUniforms() {
        magicCircleInjectCount++;
        
        if (!uniformBuffers.containsKey("MagicCircleConfig")) {
            if (magicCircleInjectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("availableBuffers", uniformBuffers.keySet().toString())
                    .warn("MagicCircleConfig UBO not found in pass");
            }
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, MagicCirclePostEffect.BUFFER_SIZE + 16);
            
            var client = net.minecraft.client.MinecraftClient.getInstance();
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            
            var camera = client.gameRenderer.getCamera();
            float tickDelta = client.getRenderTickCounter().getTickProgress(false);
            float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);
            float fov = (float) Math.toRadians(dynamicFov);
            
            MagicCirclePostEffect.writeUniformBuffer(builder, aspect, fov);
            
            // Close old buffer to prevent GPU memory leak
            closeOldBuffer("MagicCircleConfig");
            
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "MagicCircleConfig Dynamic",
                16,
                builder.get()
            );
            
            uniformBuffers.put("MagicCircleConfig", newBuffer);
            
            if (magicCircleInjectCount % 300 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("center", String.format("%.1f,%.1f,%.1f", 
                        MagicCirclePostEffect.getConfig().centerX(),
                        MagicCirclePostEffect.getConfig().centerY(),
                        MagicCirclePostEffect.getConfig().centerZ()))
                    .kv("radius", String.format("%.1f", MagicCirclePostEffect.getConfig().effectRadius()))
                    .info("Updated MagicCircleConfig UBO");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update MagicCircleConfig");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VIRUS BLOCK UNIFORM UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateVirusBlockUniforms() {
        virusBlockInjectCount++;
        
        if (!uniformBuffers.containsKey("VirusBlockParams")) {
            return;
        }
        
        // Get data from VirusBlockTelemetryState (server-provided, unlimited range)
        var telemetryState = net.cyberpunk042.client.visual.shader.VirusBlockTelemetryState.get();
        var blocks = telemetryState.getNearbyPositions();
        float closestDist = telemetryState.hasData() ? telemetryState.getClosestDistance() : Float.MAX_VALUE;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            var camera = client.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();
            float time = (System.currentTimeMillis() % 60000) / 1000.0f;
            
            // Check helmet for ESP
            boolean hasHelmet = false;
            if (client.player != null) {
                var headSlot = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
                hasHelmet = !headSlot.isEmpty() && headSlot.isOf(net.cyberpunk042.registry.ModItems.AUGMENTED_HELMET);
            }
            
            // Camera params
            float fov = (float) Math.toRadians(client.options.getFov().getValue());
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            Vec3d up = Vec3d.fromPolar(camera.getPitch() - 90, camera.getYaw());
            boolean isFlying = client.player != null && client.player.getAbilities().flying;
            
            int blockCount = Math.min(blocks.size(), 32);
            
            // Build block positions array
            VirusBlockUBO.BlockPosVec4[] blockPositions = new VirusBlockUBO.BlockPosVec4[blockCount];
            for (int i = 0; i < blockCount; i++) {
                var pos = blocks.get(i);
                blockPositions[i] = new VirusBlockUBO.BlockPosVec4(pos.getX(), pos.getY(), pos.getZ(), 1.0f);
            }
            
            // Build the UBO record
            VirusBlockUBO ubo = new VirusBlockUBO(
                new VirusBlockUBO.CameraPosTimeVec4((float) camPos.x, (float) camPos.y, (float) camPos.z, time),
                new VirusBlockUBO.EffectFlagsVec4(blockCount, hasHelmet ? 1f : 0f, 1f, 1f),
                new VirusBlockUBO.InvProjParamsVec4(0.05f, 1000f, fov, aspect),
                new VirusBlockUBO.CameraForwardVec4((float) forward.x, (float) forward.y, (float) forward.z, closestDist),
                new VirusBlockUBO.CameraUpVec4((float) up.x, (float) up.y, (float) up.z, isFlying ? 1f : 0f),
                new VirusBlockUBO.SmokeShapeVec4(5f, 3f, 1.5f, 1f),
                new VirusBlockUBO.SmokeAnimVec4(1.5f, 0.3f, 2f, 0.5f),
                new VirusBlockUBO.SmokeColorVec4(0.1f, 0.6f, 0.15f, 1f),
                new VirusBlockUBO.ScreenPoisonParamsVec4(15f, 1f, 64f, 0f),  // z = fumeNoiseScale
                new VirusBlockUBO.ESPCloseVec4(0.9f, 0.2f, 0.2f, 30f),
                new VirusBlockUBO.ESPMediumVec4(0.9f, 0.6f, 0.2f, 100f),
                new VirusBlockUBO.ESPFarVec4(0.3f, 0.8f, 0.4f, 0f),
                new VirusBlockUBO.ESPStyleVec4(2f, 2f, 0.02f, 10f),
                VirusBlockPostEffect.getInvViewProj(),
                blockPositions
            );
            
            // Write using ReflectiveUBOWriter
            Std140Builder builder = Std140Builder.onStack(stack, VirusBlockUBO.BUFFER_SIZE + 16);
            net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter.write(builder, ubo);
            
            // Close old buffer and create new
            closeOldBuffer("VirusBlockParams");
            
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "VirusBlockParams Dynamic",
                16,
                builder.get()
            );
            
            uniformBuffers.put("VirusBlockParams", newBuffer);
            
            if (virusBlockInjectCount % 300 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("blocks", blockCount)
                    .kv("closest", String.format("%.1f", closestDist))
                    .info("Updated VirusBlockParams via ReflectiveUBOWriter");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update VirusBlockParams");
        }
    }
}
