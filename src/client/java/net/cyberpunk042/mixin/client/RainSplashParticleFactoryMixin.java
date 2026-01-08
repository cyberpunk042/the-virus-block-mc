package net.cyberpunk042.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.client.render.VirusSkyClientState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.RainSplashParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Mixin(RainSplashParticle.Factory.class)
public abstract class RainSplashParticleFactoryMixin {
	@Inject(method = "createParticle", at = @At("RETURN"))
	private void theVirusBlock$tintRain(
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
			particle.setColor(0.92F, 0.08F, 0.16F);
		}
	}
}

