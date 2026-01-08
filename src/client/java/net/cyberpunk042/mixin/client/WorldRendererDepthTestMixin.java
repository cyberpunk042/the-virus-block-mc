package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive;
import net.cyberpunk042.client.visual.shader.DepthTestShader;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject depth-aware rendering AFTER the frame graph completes.
 * 
 * <p>At this point:
 * <ul>
 *   <li>All world rendering is complete</li>
 *   <li>Depth buffer contains valid world depth</li>
 *   <li>We can safely access framebuffer.getDepthAttachment()</li>
 * </ul>
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererDepthTestMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    
    /**
     * Inject AFTER frameGraph.run() - this is when ALL rendering is complete
     * and the depth buffer is guaranteed to have valid world depth.
     * 
     * Target: After FrameGraphBuilder.run() call
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/FrameGraphBuilder;run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V",
            shift = At.Shift.AFTER
        )
    )
    private void theVirusBlock$renderAfterFrameGraph(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogParameters,
            Vector4f fogColor,
            boolean renderWorld,
            CallbackInfo ci
    ) {
        // First try the new direct depth approach
        if (DirectDepthRendererArchive.isEnabled()) {
            DirectDepthRendererArchive.render();
            return;
        }
        
        // Try new ShockwaveGlowRenderer (hybrid approach)
        if (net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.isEnabled()) {
            // Capture happens HERE (after frame graph, depth is valid)
            net.minecraft.client.gl.Framebuffer framebuffer = client.getFramebuffer();
            if (framebuffer != null) {
                net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.captureAndGenerateMask(
                    client, framebuffer.textureWidth, framebuffer.textureHeight);
            }
            return;  // Don't run legacy DepthTestShader
        }
        
        // Fall back to PostEffectProcessor approach if enabled
        if (!DepthTestShader.isEnabled()) {
            return;
        }
        
        // Debug logging - check if depth is available after frame graph
        Framebuffer mainFramebuffer = client.getFramebuffer();
        GpuTexture depthAttachment = mainFramebuffer.getDepthAttachment();
        GpuTexture colorAttachment = mainFramebuffer.getColorAttachment();
        GpuTextureView depthView = mainFramebuffer.getDepthAttachmentView();
        GpuTextureView colorView = mainFramebuffer.getColorAttachmentView();
        
        Logging.RENDER.topic("depth_test")
            .kv("hasDepth", depthAttachment != null)
            .kv("hasDepthView", depthView != null)
            .kv("hasColor", colorAttachment != null)
            .kv("hasColorView", colorView != null)
            .debug("Post-frameGraph depth check");
    }
}
