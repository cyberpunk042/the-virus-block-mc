package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.client.visual.shader.ShockwaveRenderer;
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
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin to inject the shockwave post-effect into the FrameGraphBuilder.
 * 
 * <p>Uses the CORRECT modern API that properly binds depth:
 * {@code processor.render(FrameGraphBuilder, width, height, FramebufferSet)}
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererShockwaveMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    
    /**
     * Inject BEFORE the frame graph runs.
     * We capture the FrameGraphBuilder and add our shockwave pass.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/FrameGraphBuilder;run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0
    )
    private void theVirusBlock$injectShockwavePass(
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
            // Local captures - CORRECTED to match actual LVT
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
        
        // Check if shockwave is enabled
        if (!ShockwavePostEffect.isEnabled()) {
            return;
        }
        
        // Update camera position for target mode calculations
        ShockwavePostEffect.updateCameraPosition((float)camX, (float)camY, (float)camZ);
        
        // Update shockwave position if follow mode is enabled
        ShockwavePostEffect.tickFollowPosition((float)camX, (float)camY, (float)camZ);
        
        // Compute forward vector from Camera object (consistent with depth buffer)
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        
        // Clamp pitch to ±89° to prevent gimbal lock
        // This ensures forward always has some horizontal component for stable reconstruction
        float maxPitch = (float) Math.toRadians(89.0);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));
        
        float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
        float fwdY = (float) (-Math.sin(pitch));
        float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));
        ShockwavePostEffect.updateCameraForward(fwdX, fwdY, fwdZ);
        
        // Compute InvViewProj for flying mode ray generation
        // This must be done here, not rely on FieldVisualUniformBinder, since field visual may not be active
        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
        Matrix4f invViewProj = new Matrix4f();
        viewProj.invert(invViewProj);
        ShockwavePostEffect.updateInvViewProj(invViewProj);
        
        // Update the current radius for animation
        float currentRadius = ShockwavePostEffect.getCurrentRadius();
        
        // Load the processor
        PostEffectProcessor processor = ShockwavePostEffect.loadProcessor();
        if (processor == null) {
            return;
        }
        
        // Get framebuffer dimensions
        var mainFb = client.getFramebuffer();
        if (mainFb == null) return;
        
        int width = mainFb.textureWidth;
        int height = mainFb.textureHeight;
        
        try {
            // Use the MODERN API that properly binds depth!
            processor.render(frameGraphBuilder, width, height, framebufferSet);
            
            // Log animation progress
            if (ShockwavePostEffect.isAnimating()) {
                Logging.RENDER.topic("shockwave")
                    .kv("radius", String.format("%.1f", currentRadius))
                    .debug("Animating...");
            }
        } catch (Exception e) {
            Logging.RENDER.topic("shockwave")
                .kv("error", e.getMessage())
                .error("Failed to add shockwave pass");
        }
    }
}
