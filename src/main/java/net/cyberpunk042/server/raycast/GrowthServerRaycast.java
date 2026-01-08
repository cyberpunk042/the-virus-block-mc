package net.cyberpunk042.server.raycast;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class GrowthServerRaycast {

	private GrowthServerRaycast() {
	}

	public static @Nullable BlockHitResult serverRaycast(ServerPlayerEntity player, ProgressiveGrowthBlockEntity block) {
		if (player == null || block == null || block.getWorld() == null) {
			return null;
		}
		VoxelShape shape = block.worldShape(ProgressiveGrowthBlock.ShapeType.OUTLINE);
		if (shape == null || shape.isEmpty()) {
			return null;
		}
		Vec3d origin = player.getCameraPosVec(1.0F);
		Vec3d look = player.getRotationVec(1.0F);
		double reach = player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
		Vec3d target = origin.add(look.multiply(reach + 0.5D));
		BlockPos pos = block.getPos();
		BlockHitResult hit = shape.raycast(origin, target, pos);
		if (hit == null) {
			return null;
		}
		return withinReach(player, hit.getPos(), reach + 1.0D) ? hit : null;
	}

	public static boolean withinReach(ServerPlayerEntity player, Vec3d hitPos, double maxDistance) {
		Vec3d eye = player.getCameraPosVec(1.0F);
		return eye.squaredDistanceTo(hitPos) <= maxDistance * maxDistance;
	}
}





