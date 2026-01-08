package net.cyberpunk042.block.core;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.entity.CorruptedWormEntity;
import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.BoobytrapHelper.Type;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class InfectedBlock extends Block {
	public static final MapCodec<InfectedBlock> CODEC = createCodec(InfectedBlock::new);

	public InfectedBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int chance = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_INFECTED_BLOCK_EXPLODE_CHANCE), 0, 1000);
		if (chance <= 0) {
			return;
		}
		maybeSpawnWorm(world, pos, random);
		if (random.nextInt(1000) < chance) {
			BoobytrapHelper.triggerExplosion(world, pos, Type.INFECTED_BLOCK, "spontaneous");
			world.breakBlock(pos, false);
		}
	}

	private void maybeSpawnWorm(ServerWorld world, BlockPos pos, Random random) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_WORMS_ENABLED)) {
			return;
		}
		VirusWorldState infection = VirusWorldState.get(world);
		if (!infection.tiers().areLiquidsCorrupted(world)) {
			return;
		}
		int chance = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE), 0, 1000);
		if (chance <= 0 || random.nextInt(1000) >= chance) {
			return;
		}
		BlockPos spawnPos = pos.up();
		if (!world.isChunkLoaded(ChunkPos.toLong(spawnPos)) || !world.isAir(spawnPos)) {
			return;
		}
		if (CorruptedWormEntity.spawn(world, spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D) != null) {
			CorruptionProfiler.logWormSpawn(world, spawnPos);
		}
	}
}

