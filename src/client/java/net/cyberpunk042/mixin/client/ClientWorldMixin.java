package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.render.VirusHorizonClientState;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	@SuppressWarnings("resource")
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void theVirusBlock$enhanceSkyTint(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
		if (!VirusSkyClientState.isSkyCorrupted()) {
			return;
		}

		ClientWorld world = (ClientWorld) (Object) this;
		int original = cir.getReturnValue();
		float baseR = ((original >> 16) & 255) / 255.0F;
		float baseG = ((original >> 8) & 255) / 255.0F;
		float baseB = (original & 255) / 255.0F;

		double pulse = 0.75D + Math.sin((world.getTime() + tickDelta) * 0.02D) * 0.08D;
		float mix = (float) MathHelper.clamp(pulse, 0.0D, 1.0D);

		float targetR = 0.98F;
		float targetG = 0.07F;
		float targetB = 0.12F;

		float r = MathHelper.clamp(MathHelper.lerp(mix, baseR, targetR), 0.0F, 1.0F);
		float g = MathHelper.clamp(MathHelper.lerp(mix, baseG, targetG), 0.0F, 1.0F);
		float b = MathHelper.clamp(MathHelper.lerp(mix, baseB, targetB), 0.0F, 1.0F);

		if (VirusHorizonClientState.isActive()) {
			float horizonMix = VirusHorizonClientState.intensity();
			r = MathHelper.clamp(MathHelper.lerp(horizonMix, r, VirusHorizonClientState.red()), 0.0F, 1.0F);
			g = MathHelper.clamp(MathHelper.lerp(horizonMix, g, VirusHorizonClientState.green()), 0.0F, 1.0F);
			b = MathHelper.clamp(MathHelper.lerp(horizonMix, b, VirusHorizonClientState.blue()), 0.0F, 1.0F);
		}

		int tinted = ((int) (r * 255.0F) & 255) << 16
				| ((int) (g * 255.0F) & 255) << 8
				| ((int) (b * 255.0F) & 255);
		cir.setReturnValue(tinted);
	}
}


