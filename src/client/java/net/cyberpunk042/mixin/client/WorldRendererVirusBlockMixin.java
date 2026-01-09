package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.cyberpunk042.client.visual.shader.VirusBlockPostEffect;
import net.cyberpunk042.client.visual.shader.VirusBlockScanner;
import net.cyberpunk042.client.visual.shader.VirusBlockUniformBinder;
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
 * Mixin to inject the Virus Block post-effect into the FrameGraphBuilder.
 * 
 * <p>Renders toxic smoke and ESP outlines for virus blocks.
 * Uses the modern FrameGraphBuilder API for proper depth buffer access.</p>
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererVirusBlockMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    
    /**
     * Inject BEFORE the frame graph runs.
     * We capture the FrameGraphBuilder and add our virus block pass.
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
    private void theVirusBlock$injectVirusBlockPass(
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
            // Local captures - matches WorldRendererShockwaveMixin
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
        
        // Check if virus block effect is enabled
        if (!VirusBlockUniformBinder.isEnabled()) {
            return;
        }
        
        // Compute InvViewProj for accurate ray generation
        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
        Matrix4f invViewProj = new Matrix4f();
        viewProj.invert(invViewProj);
        VirusBlockPostEffect.updateInvViewProj(invViewProj);
        
        // Load the processor
        PostEffectProcessor processor = VirusBlockPostEffect.loadProcessor();
        if (processor == null) {
            Logging.RENDER.topic("virus_block")
                .warn("PostEffectProcessor is null - failed to load!");
            return;
        }
        
        // DEBUG: Log processor info
        Logging.RENDER.topic("virus_block")
            .kv("processorClass", processor.getClass().getSimpleName())
            .debug("Loaded processor");
        
        // Get framebuffer dimensions
        var mainFb = client.getFramebuffer();
        if (mainFb == null) return;
        
        int width = mainFb.textureWidth;
        int height = mainFb.textureHeight;
        
        try {
            // Use the MODERN API that properly binds depth!
            processor.render(frameGraphBuilder, width, height, framebufferSet);
            
            Logging.RENDER.topic("virus_block")
                .kv("width", width)
                .kv("height", height)
                .debug("Rendered virus block pass");
            
        } catch (Exception e) {
            Logging.RENDER.topic("virus_block")
                .kv("error", e.getMessage())
                .error("Failed to add virus block pass");
        }
    }
}
