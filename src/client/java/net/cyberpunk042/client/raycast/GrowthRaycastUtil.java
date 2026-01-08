package net.cyberpunk042.client.raycast;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class GrowthRaycastUtil {

	private GrowthRaycastUtil() {
	}

	public static @Nullable BlockHitResult raycastGrowthBlock(ProgressiveGrowthBlockEntity block,
			Vec3d origin,
			Vec3d viewDirection,
			double reachDistance) {
		if (block.getWorld() == null || reachDistance <= 0.0D) {
			return null;
		}
		VoxelShape shape = block.shape(ProgressiveGrowthBlock.ShapeType.OUTLINE);
		if (shape == null || shape.isEmpty()) {
			return null;
		}
		BlockPos pos = block.getPos();
		Vec3d end = origin.add(viewDirection.multiply(reachDistance + 0.5D));
		BlockHitResult hit = shape.raycast(origin, end, pos);
		if (hit == null) {
			return null;
		}
		return new BlockHitResult(hit.getPos(), hit.getSide(), pos, hit.isInsideBlock());
	}

	public static double resolveClientReachDistance(MinecraftClient client, float tickDelta) {
		if (client == null || client.player == null) {
			return 4.5D;
		}
		return client.player.isCreative() ? 5.0D : 4.5D;
	}
}

