package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.minecraft.server.world.ServerWorld;

@Mixin(ServerWorld.class)
public abstract class ServerWorldChunkGuardMixin {
	@Inject(method = "close", at = @At("HEAD"))
	private void theVirusBlock$clearChunkGuard(CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ServerWorld.close");
		SingularityChunkContext.disableBorderGuard((ServerWorld) (Object) this);
		ctx.exit();
	}
}
 