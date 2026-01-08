package net.cyberpunk042.mixin.client;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.client.render.VirusHorizonClientState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.fog.FogRenderer;

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {
	@Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
	private void theVirusBlock$blendHorizonColor(Camera camera,
			float tickDelta,
			ClientWorld world,
			int renderDistance,
			float skyDarkness,
			boolean thickFog,
			CallbackInfoReturnable<Vector4f> cir) {
		if (!VirusHorizonClientState.isActive()) {
			return;
		}
		Vector4f color = cir.getReturnValue();
		if (color == null) {
			return;
		}
		float mix = VirusHorizonClientState.intensity();
		if (mix <= 0.001F) {
			return;
		}
		float targetR = VirusHorizonClientState.red();
		float targetG = VirusHorizonClientState.green();
		float targetB = VirusHorizonClientState.blue();
		color.set(
				color.x() + (targetR - color.x()) * mix,
				color.y() + (targetG - color.y()) * mix,
				color.z() + (targetB - color.z()) * mix,
				color.w());
		cir.setReturnValue(color);
	}
}

