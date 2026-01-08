package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
	@Inject(method = "contains(DD)Z", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$allowSingularityBypass(double x, double z, CallbackInfoReturnable<Boolean> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Border.containsXZ");
		if (SingularityChunkContext.shouldBypassBorder((WorldBorder) (Object) this, x, z)) {
			ctx.exit();
			cir.setReturnValue(true);
			return;
		}
		ctx.exit();
	}

	@Inject(method = "contains(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$allowSingularityBypass(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Border.containsPos");
		if (SingularityChunkContext.shouldBypassBorder((WorldBorder) (Object) this,
				pos.getX() + 0.5D,
				pos.getZ() + 0.5D)) {
			ctx.exit();
			cir.setReturnValue(true);
			return;
		}
		ctx.exit();
	}
}

