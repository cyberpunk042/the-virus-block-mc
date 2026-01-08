package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.shader.ShockwaveTestRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TEST MIXIN: Injects ShockwaveTestRenderer into HUD rendering.
 * 
 * <p>Uses DrawContext.fill() which is proven to work.
 */
@Mixin(InGameHud.class)
public abstract class ShockwaveTestMixin {
    
    @Shadow @Final private MinecraftClient client;
    
    /**
     * Inject at the end of HUD rendering to overlay our test.
     */
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void theVirusBlock$renderShockwaveTest(
            DrawContext context, 
            RenderTickCounter tickCounter, 
            CallbackInfo ci
    ) {
        if (!ShockwaveTestRenderer.isEnabled()) return;
        
        ShockwaveTestRenderer.render(context);
    }
}
