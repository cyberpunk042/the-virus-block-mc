package net.cyberpunk042.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.client.render.VirusSkyClientState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SnowflakeParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Mixin(SnowflakeParticle.Factory.class)
public abstract class SnowflakeParticleFactoryMixin {
	@Inject(method = "createParticle", at = @At("RETURN"))
	private void theVirusBlock$tintSnow(
			SimpleParticleType type,
			ClientWorld world,
			double x, double y, double z,
			double velocityX, double velocityY, double velocityZ,
			CallbackInfoReturnable<Particle> cir) {
		if (!VirusSkyClientState.isSkyCorrupted()) {
			return;
		}
		Particle particle = cir.getReturnValue();
		if (particle != null) {
			particle.setColor(0.72F, 0.12F, 0.38F);
		}
	}
}

