package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualRegistry;
import net.cyberpunk042.client.visual.ubo.CameraStateManager;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Vector4f;

/**
 * Mixin to inject the Field Visual post-effect into the FrameGraphBuilder.
 * 
 * <p>Uses the same pattern as WorldRendererShockwaveMixin to properly
 * integrate with Minecraft's rendering pipeline and depth buffer access.</p>
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererFieldVisualMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    
    /**
     * Inject BEFORE the frame graph runs.
     * We capture the FrameGraphBuilder and add our field visual pass.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/FrameGraphBuilder;run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0  // Don't crash if injection fails
    )
    private void theVirusBlock$injectFieldVisualPass(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogParameters,
            Vector4f fogColor,
            boolean renderWorld,
            CallbackInfo ci,
            // Local captures - must match actual LVT order
            float tickDelta,
            Profiler profiler,
            Vec3d cameraPos,
            double camX,
            double camY,
            double camZ,
            boolean isFrustumCaptured,
            Frustum frustum,
            boolean hasOutlinedEntities,
            Matrix4fStack modelViewStack,
            FrameGraphBuilder frameGraphBuilder
    ) {
        // ═══════════════════════════════════════════════════════════════════════════
        CameraStateManager.updateFromRender(
            (float)camX, (float)camY, (float)camZ,
            camera.getYaw(), camera.getPitch(),
            positionMatrix, projectionMatrix,
            tickDelta
        );
        
        // Check if field visual effect is enabled
        if (!FieldVisualPostEffect.isEnabled()) {
            return;
        }
        
        // Check if there are any fields to render
        if (!FieldVisualRegistry.hasRenderableFields()) {
            return;
        }
        
        // Update camera position for field distance calculations
        Vec3d cameraVec = new Vec3d(camX, camY, camZ);
        FieldVisualPostEffect.updateCameraPosition((float)camX, (float)camY, (float)camZ);
        
        // Set tick delta for position interpolation (smooth movement)
        FieldVisualPostEffect.setTickDelta(tickDelta);
        
        // === COMPUTE FORWARD VECTOR - USE SAME METHOD AS WORKING SHOCKWAVE ===
        // This yaw/pitch -> forward conversion is proven to work in shockwave
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        
        // Clamp pitch to ±89° to prevent gimbal lock
        float maxPitch = (float) Math.toRadians(89.0);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));
        
        float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
        float fwdY = (float) (-Math.sin(pitch));
        float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));
        
        FieldVisualPostEffect.updateCameraForward(fwdX, fwdY, fwdZ);
        
        // Update preview orb position if adapter exists
        // Each orb source (preview, spawn, charge) manages its own position
        var state = net.cyberpunk042.client.gui.state.FieldEditStateHolder.get();
        if (state != null) {
            state.fieldVisualAdapter().tickPreviewPosition();
        }
        
        // Tick throw animation if active
        FieldVisualPostEffect.tickThrowAnimation();
        
        // === USE MINECRAFT'S POSITION MATRIX DIRECTLY ===
        // positionMatrix is exactly what Minecraft used to render the scene.
        // Using anything else can cause mismatches.
        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
        Matrix4f invViewProj = new Matrix4f();
        viewProj.invert(invViewProj);
        FieldVisualPostEffect.updateViewProjectionMatrices(invViewProj, viewProj);
        
        // Get framebuffer dimensions
        var mainFb = client.getFramebuffer();
        if (mainFb == null) return;
        
        int width = mainFb.textureWidth;
        int height = mainFb.textureHeight;
        
        // Ensure HDR pipeline targets are ready (handles resize, format changes)
        net.cyberpunk042.client.visual.render.PostFxPipeline.getInstance().ensureTargetsReady(width, height);
        
        // Get fields to render
        var fieldsToRender = FieldVisualRegistry.getFieldsToRender(cameraVec);
        
        if (fieldsToRender.isEmpty()) {
            Logging.RENDER.topic("field_visual_render")
                .warn("No fields to render - fieldsToRender is empty");
            return;
        }
        
        Logging.RENDER.topic("field_visual_render")
            .kv("fieldCount", fieldsToRender.size())
            .debug("Rendering {} field visuals", fieldsToRender.size());
        
        // Render EACH field with its OWN processor instance
        // Each processor is cached by field ID - passes are registered to fields on creation
        for (FieldVisualInstance field : fieldsToRender) {
            // Load processor for THIS field (creates new one if needed, cached by field ID)
            // Passes are registered to this field in the PASS_TO_FIELD map
            PostEffectProcessor processor = FieldVisualPostEffect.loadProcessor(field);
            if (processor == null) {
                continue; // Skip fields that fail to load processor
            }
            
            Logging.RENDER.topic("field_visual_render")
                .kv("fieldId", field.getId().toString().substring(0, 8))
                .kv("version", field.getConfig().version())
                .kv("pos", String.format("%.1f,%.1f,%.1f", field.getWorldCenter().x, field.getWorldCenter().y, field.getWorldCenter().z))
                .debug("Rendering field");
            
            try {
                // Each field has its own processor with its own passes
                // PASS_TO_FIELD maps each pass to its owning field
                // Note: HDR format injection is handled by FrameGraphBuilderHdrMixin
                processor.render(frameGraphBuilder, width, height, framebufferSet);
            } catch (Exception e) {
                Logging.RENDER.topic("field_visual")
                    .kv("fieldId", field.getId().toString().substring(0, 8))
                    .kv("error", e.getMessage())
                    .error("Failed to render field visual");
            }
        }
    }
}
