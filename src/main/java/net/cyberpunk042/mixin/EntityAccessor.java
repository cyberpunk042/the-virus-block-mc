package net.cyberpunk042.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for Entity to expose protected methods.
 */
@Mixin(Entity.class)
public interface EntityAccessor {
    
    /**
     * Invokes the protected setFlag method to set entity flags.
     * Flag 7 is FALL_FLYING (elytra flying animation).
     */
    @Invoker("setFlag")
    void virus$setFlag(int flag, boolean value);
}
