package net.cyberpunk042.mixin;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSkeletonEntityMixin extends HostileEntity {
	protected AbstractSkeletonEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "tickMovement", at = @At("TAIL"))
	private void theVirusBlock$extinguishDuringInfection(CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Skeleton.tick");
		// Fast exit: Only do anything if skeleton is on fire
		if (!this.isOnFire()) {
			ctx.exit();
			return;
		}
		if (!(getWorld() instanceof ServerWorld serverWorld)) {
			ctx.exit();
			return;
		}
		if (VirusWorldState.get(serverWorld).infectionState().infected()) {
			this.setFireTicks(0);
		}
		ctx.exit();
	}
}

