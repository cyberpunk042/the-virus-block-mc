package net.cyberpunk042.mixin.client.access;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose internal methods of Framebuffer for preview rendering.
 */
@Mixin(Framebuffer.class)
public interface FramebufferAccessor {
    
    /**
     * Invokes the internal initFbo method.
     */
    @Invoker("initFbo")
    void theVirusBlock$initFbo(int width, int height);
}

