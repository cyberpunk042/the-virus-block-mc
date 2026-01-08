package net.cyberpunk042.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose GameRenderer's private getFov method.
 * This is needed to get the ACTUAL rendered FOV including dynamic changes
 * from flying, sprinting, speed effects, etc.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    
    @Invoker("getFov")
    float invokeGetFov(Camera camera, float tickDelta, boolean changingFov);
}
