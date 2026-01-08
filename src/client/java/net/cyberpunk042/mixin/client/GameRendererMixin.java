package net.cyberpunk042.mixin.client;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.client.raycast.GrowthRaycastUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "updateCrosshairTarget", at = @At("TAIL"), remap = true)
	private void theVirusBlock$expandGrowthBlockHitbox(float tickDelta, CallbackInfo ci) {
		if (client == null || client.world == null) {
			return;
		}

		ClientWorld world = client.world;
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			return;
		}

		double reach = GrowthRaycastUtil.resolveClientReachDistance(client, tickDelta);
		Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
		Vec3d viewDir = camera.getRotationVec(tickDelta);

		BlockHitResult adjusted = findGrowthHit(world, cameraPos, viewDir, reach, client.crosshairTarget);
		if (adjusted != null) {
			client.crosshairTarget = adjusted;
		}
	}

	private BlockHitResult findGrowthHit(ClientWorld world, Vec3d origin, Vec3d viewDir, double reach, HitResult existing) {
		double bestSq = existing instanceof BlockHitResult ? origin.squaredDistanceTo(existing.getPos()) : Double.POSITIVE_INFINITY;
		BlockHitResult best = existing instanceof BlockHitResult blockHit ? blockHit : null;
		Set<BlockPos> tested = new HashSet<>();
		Vec3d step = viewDir.normalize().multiply(0.25D);
		Vec3d sample = origin;
		double traveled = 0.0D;
		while (traveled <= reach) {
			BlockPos center = BlockPos.ofFloored(sample);
			for (BlockPos pos : BlockPos.iterate(center.add(-3, -3, -3), center.add(3, 3, 3))) {
				if (!tested.add(pos)) {
					continue;
				}
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (blockEntity instanceof ProgressiveGrowthBlockEntity growth) {
					BlockHitResult candidate = GrowthRaycastUtil.raycastGrowthBlock(growth, origin, viewDir, reach);
					if (candidate != null) {
						double distSq = origin.squaredDistanceTo(candidate.getPos());
						if (distSq + 1.0E-5 < bestSq) {
							bestSq = distSq;
							best = candidate;
						}
					}
				}
			}
			sample = sample.add(step);
			traveled += 0.25D;
		}
		return best;
	}
}

