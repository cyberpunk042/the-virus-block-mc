package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;

/**
 * DEBUG MIXIN: Track whether Entity.tick() is ever called.
 * This is the base class tick method - if this isn't called, nothing ticks!
 */
@Mixin(Entity.class)
public abstract class EntityTickTraceMixin {
    // Static counters for diagnostic
    private static int totalTickCounter = 0;
    private static long lastLogTime = 0;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void theVirusBlock$traceEntityTick(CallbackInfo ci) {
        totalTickCounter++;
        
        // Log every 5 seconds
        Entity self = (Entity) (Object) this;
        if (!self.getWorld().isClient()) {
            long now = self.getWorld().getTime();
            if (now - lastLogTime >= 100) { // Every 5 seconds
                net.cyberpunk042.log.Logging.PROFILER.warn(
                    "[EntityTick] BASE Entity.tick() called {} times in last 5 sec",
                    totalTickCounter
                );
                totalTickCounter = 0;
                lastLogTime = now;
            }
        }
    }
}
