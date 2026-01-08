package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cyberpunk042.util.VirusEquipmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;

@Mixin(FireworkRocketEntity.class)
abstract class FireworkRocketEntityMixin {
	private static final double COMPOSITE_SPEED_FACTOR = 0.8D;

	@Shadow
	private LivingEntity shooter;

	@Inject(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
					ordinal = 0,
					shift = At.Shift.AFTER
			)
	)
	private void thevirus$slowCompositeBoost(CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Firework.tick");
		if (shooter == null || !shooter.isGliding() || !VirusEquipmentHelper.hasCompositeElytra(shooter)) {
			ctx.exit();
			return;
		}
		Vec3d velocity = shooter.getVelocity();
		shooter.setVelocity(velocity.multiply(COMPOSITE_SPEED_FACTOR, 1.0D, COMPOSITE_SPEED_FACTOR));
		shooter.velocityModified = true;
		shooter.velocityDirty = true;
		ctx.exit();
	}
}

