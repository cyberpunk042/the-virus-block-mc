package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.gui.preview.FramebufferFboAccess;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to enhance Framebuffer with additional functionality for preview rendering.
 * 
 * This mixin tracks the OpenGL FBO ID when the framebuffer is bound,
 * allowing us to use it for direct GL operations.
 */
@Mixin(Framebuffer.class)
public class FramebufferMixin implements FramebufferFboAccess {
    
    @Unique
    private int theVirusBlock$cachedFboId = -1;
    
    /**
     * Called after initFbo to cache the FBO ID.
     */
    @Inject(method = "initFbo", at = @At("RETURN"))
    private void theVirusBlock$onInitFbo(int width, int height, CallbackInfo ci) {
        // The FBO should now be bound, so we can get its ID
        this.theVirusBlock$cachedFboId = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
    }
    
    /**
     * Gets the cached FBO ID for this framebuffer.
     * Returns -1 if not yet initialized.
     */
    @Override
    public int theVirusBlock$getFboId() {
        return this.theVirusBlock$cachedFboId;
    }
    
    /**
     * Binds this framebuffer as the current render target.
     */
    @Override
    public void theVirusBlock$bind() {
        if (this.theVirusBlock$cachedFboId > 0) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.theVirusBlock$cachedFboId);
        }
    }
}
