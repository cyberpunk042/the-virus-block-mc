package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.cyberpunk042.client.visual.blend.BlendEquationManager;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to expose glBlendEquation for subtractive blending.
 * 
 * <p>Minecraft's GlStateManager doesn't expose glBlendEquation, which is needed
 * for true subtractive blending (GL_FUNC_REVERSE_SUBTRACT).
 * 
 * <p>This mixin hooks into _blendFuncSeparate to also apply any custom blend equation
 * set via {@link BlendEquationManager}.
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * // Before rendering with subtractive blending:
 * BlendEquationManager.setBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
 * 
 * // Render your effect...
 * 
 * // After rendering, reset to normal:
 * BlendEquationManager.resetBlendEquation();
 * }</pre>
 */
@Mixin(GlStateManager.class)
public class GlBlendEquationMixin {
    
    /**
     * After blend function is set, also apply blend equation if needed.
     */
    @Inject(method = "_blendFuncSeparate", at = @At("TAIL"))
    private static void onBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        // Apply custom blend equation if set
        int equation = BlendEquationManager.getCurrentEquation();
        if (equation != GL14.GL_FUNC_ADD) {
            GL14.glBlendEquation(equation);
        }
    }
}
