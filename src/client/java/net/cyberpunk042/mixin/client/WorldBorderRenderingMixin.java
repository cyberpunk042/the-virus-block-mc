package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.render.SingularityBorderClientState;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderStage;
import net.minecraft.client.render.WorldBorderRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorderRendering.class)
public abstract class WorldBorderRenderingMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void theVirusBlock$captureBorder(WorldBorder border, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance, CallbackInfo ci) {
		SingularityBorderClientState.captureBorder(border);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorderStage;getColor()I"))
	private int theVirusBlock$overrideForcefieldColor(WorldBorderStage stage, WorldBorder border, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance) {
		int vanilla = stage.getColor();
		return SingularityBorderClientState.resolveForcefieldColor(vanilla);
	}
}

