package net.cyberpunk042.mixin;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.explosion.ExplosionImpl")
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private ServerWorld world;

	@Shadow
	@Final
	private Vec3d pos;

	@Shadow
	@Final
	private float power;

	@Shadow
	@Final
	@Nullable
	private Entity entity;

	@Inject(method = "explode", at = @At("TAIL"))
	private void theVirusBlock$handleExplosion(CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Explosion.explode");
		VirusWorldState state = VirusWorldState.get(world);
		if (!state.infectionState().infected()) {
			ctx.exit();
			return;
		}

		if (entity != null && entity.getCommandTags().contains(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG)) {
			ctx.exit();
			return;
		}

		double radius = Math.max(3.0D, power * 6.0F);
		state.disturbance().handleExplosionImpact(entity, pos, radius);
		ctx.exit();
	}
}

