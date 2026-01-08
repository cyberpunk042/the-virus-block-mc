package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.shader.DepthTestShader;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Pool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject depth-test post effect shader into GameRenderer.
 * 
 * <p>CRITICAL: Must inject BEFORE depth buffer is cleared!
 * We try right after entity outlines are drawn.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererDepthTestMixin {
    
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private Pool pool;
    
    /**
     * Inject right after entity outlines but before other post effects.
     * This should be before depth is cleared.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V",
            shift = At.Shift.AFTER
        ),
        require = 0  // Don't crash if this target doesn't exist
    )
    private void theVirusBlock$renderDepthTest_AfterOutlines(
            RenderTickCounter tickCounter,
            boolean renderWorld,
            CallbackInfo ci) {
        renderDepthEffect();
    }
    
    /**
     * Fallback: Inject after postProcessorId check but before fog.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/GameRenderer;postProcessorId:Lnet/minecraft/util/Identifier;",
            ordinal = 0
        ),
        require = 0
    )
    private void theVirusBlock$renderDepthTest_Fallback(
            RenderTickCounter tickCounter,
            boolean renderWorld,
            CallbackInfo ci) {
        // Only use this if the other injection didn't run
        // We track this with a simple flag
    }
    
    private void renderDepthEffect() {
        if (!DepthTestShader.isEnabled()) {
            return;
        }
        
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null) {
            return;
        }
        
        PostEffectProcessor processor = DepthTestShader.loadProcessor();
        if (processor == null) {
            return;
        }
        
        try {
            processor.render(framebuffer, pool);
        } catch (Exception e) {
            Logging.RENDER.topic("depth_test")
                .kv("error", e.getMessage())
                .error("Failed to render depth test post effect");
        }
    }
}
