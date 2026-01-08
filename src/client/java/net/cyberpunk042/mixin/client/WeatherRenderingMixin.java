package net.cyberpunk042.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cyberpunk042.client.render.VirusSkyClientState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
	@Unique
	private float theVirusBlock$currentColorScale;

	@Inject(method = "renderPieces(Lnet/minecraft/client/render/VertexConsumer;Ljava/util/List;Lnet/minecraft/util/math/Vec3d;FIF)V", at = @At("HEAD"))
	private void theVirusBlock$captureColorScale(VertexConsumer consumer, java.util.List<?> pieces, Vec3d cameraPos, float colorScale, int viewDistance, float rainStrength, CallbackInfo ci) {
		this.theVirusBlock$currentColorScale = colorScale;
	}

	@ModifyArg(method = "renderPieces(Lnet/minecraft/client/render/VertexConsumer;Ljava/util/List;Lnet/minecraft/util/math/Vec3d;FIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(I)Lnet/minecraft/client/render/VertexConsumer;"), index = 0)
	private int theVirusBlock$tintWeatherColor(int originalColor) {
		if (!VirusSkyClientState.isSkyCorrupted()) {
			return originalColor;
		}
		float alpha = ((originalColor >> 24) & 255) / 255.0F;
		boolean rain = this.theVirusBlock$currentColorScale >= 0.95F;
		float red = rain ? 0.92F : 0.72F;
		float green = rain ? 0.08F : 0.12F;
		float blue = rain ? 0.16F : 0.38F;
		int packedAlpha = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F) & 255;
		return (packedAlpha << 24)
				| ((int) (red * 255.0F) & 255) << 16
				| ((int) (green * 255.0F) & 255) << 8
				| ((int) (blue * 255.0F) & 255);
	}
}

