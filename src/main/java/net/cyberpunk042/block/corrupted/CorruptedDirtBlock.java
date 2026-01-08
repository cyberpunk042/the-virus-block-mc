package net.cyberpunk042.block.corrupted;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.entity.CorruptedWormEntity;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class CorruptedDirtBlock extends Block {
	public CorruptedDirtBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_WORMS_ENABLED)) {
			return;
		}
		VirusWorldState infection = VirusWorldState.get(world);
		if (!infection.tiers().areLiquidsCorrupted(world)) { // ensures infection is active and at least tier 2
			return;
		}
		int chance = Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE));
		chance = MathHelper.clamp(MathHelper.floor(chance * infection.tiers().difficulty().getWormSpawnMultiplier()), 0, 1000);
		if (chance <= 0) {
			return;
		}
		if (random.nextInt(1000) >= chance) {
			return;
		}
		BlockPos spawnPos = pos.up();
		if (!world.isAir(spawnPos) || !world.isChunkLoaded(ChunkPos.toLong(spawnPos))) {
			return;
		}
		if (!world.getWorldBorder().contains(spawnPos)) {
			return;
		}
		if (CorruptedWormEntity.spawn(world, spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D) != null) {
			CorruptionProfiler.logWormSpawn(world, spawnPos);
		}
	}
}

