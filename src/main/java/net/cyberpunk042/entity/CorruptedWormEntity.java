package net.cyberpunk042.entity;

import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CorruptedWormEntity extends SilverfishEntity {
	public CorruptedWormEntity(EntityType<? extends SilverfishEntity> type, World world) {
		super(type, world);
	}

	public static CorruptedWormEntity spawn(ServerWorld world, double x, double y, double z) {
		BlockPos blockPos = BlockPos.ofFloored(x, y, z);
		CorruptedWormEntity worm = ModEntities.CORRUPTED_WORM.spawn(world, blockPos, SpawnReason.TRIGGERED);
		if (worm != null) {
			worm.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360F, 0.0F);
			VirusMobAllyHelper.mark(worm);
		}
		return worm;
	}
}

