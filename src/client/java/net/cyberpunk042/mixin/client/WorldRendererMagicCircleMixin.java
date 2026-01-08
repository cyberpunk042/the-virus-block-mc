package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.cyberpunk042.client.visual.shader.MagicCirclePostEffect;
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
 * Mixin to inject the Magic Circle post-effect into the FrameGraphBuilder.
 * 
 * <p>Renders magic circle ground decal effect using depth buffer for terrain conformance.</p>
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMagicCircleMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    
    /**
     * Inject BEFORE the frame graph runs.
     * We capture the FrameGraphBuilder and add our magic circle pass.
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
    private void theVirusBlock$injectMagicCirclePass(
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
        // Early exit if not enabled at all
        if (!MagicCirclePostEffect.isEnabled()) {
            return;
        }
        
        // Update camera position FIRST - this also updates flying detection state
        // Pass tickDelta for proper position interpolation in follow mode
        MagicCirclePostEffect.updateCameraPosition((float)camX, (float)camY, (float)camZ, tickDelta);
        
        // Tick animation state machine - drives spawn/despawn stage progression
        MagicCirclePostEffect.tickAnimation();
        
        // Now check if we should actually render (considers flying state)
        if (!MagicCirclePostEffect.shouldRender()) {
            return;
        }
        
        // DEBUG: Log rendering
        Logging.RENDER.topic("magic_circle")
            .kv("camX", String.format("%.1f", camX))
            .kv("camY", String.format("%.1f", camY))
            .kv("camZ", String.format("%.1f", camZ))
            .debug("Rendering magic circle");
        
        // Compute forward vector from Camera object
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        
        float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
        float fwdY = (float) (-Math.sin(pitch));
        float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));
        MagicCirclePostEffect.updateCameraForward(fwdX, fwdY, fwdZ);
        
        // Compute up vector
        float upX = (float) (-Math.sin(yaw) * -Math.sin(pitch));
        float upY = (float) Math.cos(pitch);
        float upZ = (float) (Math.cos(yaw) * -Math.sin(pitch));
        MagicCirclePostEffect.updateCameraUp(upX, upY, upZ);
        
        // Compute InvViewProj for world position reconstruction
        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);
        Matrix4f invViewProj = new Matrix4f();
        viewProj.invert(invViewProj);
        MagicCirclePostEffect.updateInvViewProj(invViewProj);
        
        // Load the processor
        PostEffectProcessor processor = MagicCirclePostEffect.loadProcessor();
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
        } catch (Exception e) {
            Logging.RENDER.topic("magic_circle")
                .kv("error", e.getMessage())
                .error("Failed to add magic circle pass");
        }
    }
}
