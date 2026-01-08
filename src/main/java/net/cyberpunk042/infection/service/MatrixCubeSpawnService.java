package net.cyberpunk042.infection.service;

import java.util.List;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Encapsulates matrix cube spawn attempts so {@link net.cyberpunk042.infection.VirusWorldState}
 * no longer micromanages the logic.
 */
public final class MatrixCubeSpawnService {

	public boolean maybeSpawnMatrixCube(ServerWorld world, float singularityFactor, double activityMultiplier) {
		int maxActive = Math.max(1, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_MAX_ACTIVE));
		if (singularityFactor <= 0.0F) {
			MatrixCubeBlockEntity.trimActive(world, 0);
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_halt", null, MatrixCubeBlockEntity.getActiveCount(world), 0);
			return false;
		}
		maxActive = Math.max(1, MathHelper.floor(maxActive * singularityFactor));
		MatrixCubeBlockEntity.trimActive(world, maxActive);
		int active = MatrixCubeBlockEntity.getActiveCount(world);
		if (active >= maxActive) {
			CorruptionProfiler.logMatrixCubeSkip(world, "active_limit", null, active, maxActive);
			return false;
		}

		List<ServerPlayerEntity> players = world.getPlayers(ServerPlayerEntity::isAlive);
		if (players.isEmpty()) {
			CorruptionProfiler.logMatrixCubeSkip(world, "no_players", null, active, maxActive);
			return false;
		}

		Random random = world.getRandom();
		if (activityMultiplier <= 0.0D) {
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_freeze", null, active, maxActive);
			return false;
		}
		int attempts = Math.max(1, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_SPAWN_ATTEMPTS));
		attempts = Math.max(0, MathHelper.floor(attempts * activityMultiplier * singularityFactor));
		if (attempts <= 0) {
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_freeze", null, active, maxActive);
			return false;
		}
		int placements = 0;
		for (int attempt = 0; attempt < attempts && active < maxActive; attempt++) {
			ServerPlayerEntity anchor = players.get(random.nextInt(players.size()));
			int x = MathHelper.floor(anchor.getX()) + random.nextBetween(-48, 48);
			int z = MathHelper.floor(anchor.getZ()) + random.nextBetween(-48, 48);
			// Optimized: avoid BlockPos allocation for chunk check
			if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
				continue;
			}

			int maxY = world.getBottomY() + world.getDimension().height() - 1;
			int anchorY = MathHelper.floor(anchor.getY());
			int minSeaLevel = world.getSeaLevel() + 64;
			int minAnchor = anchorY + 96;
			int base = Math.max(minSeaLevel, minAnchor);
			int y = Math.min(maxY - 4, base + random.nextBetween(0, 24));
			// Optimized: check chunk before creating BlockPos
			if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
				continue;
			}
			BlockPos pos = new BlockPos(x, y, z);
			CorruptionProfiler.logMatrixCubeAttempt(world, anchor.getBlockPos(), pos, world.getSeaLevel(), anchorY, base, maxY);
			if (!world.getBlockState(pos).isAir()) {
				continue;
			}

			BlockState cube = net.cyberpunk042.registry.ModBlocks.MATRIX_CUBE.getDefaultState();
			FallingMatrixCubeEntity entity = new FallingMatrixCubeEntity(world, pos, cube);
			MatrixCubeBlockEntity.register(world, entity.getUuid(), pos);
			entity.markRegistered();
			if (world.spawnEntity(entity)) {
				world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.0F, 0.6F);
				CorruptionProfiler.logMatrixCubeSpawn(world, pos, active, maxActive);
				CorruptionProfiler.logMatrixCubeEntity(world, pos);
				active++;
				placements++;
			} else {
			MatrixCubeBlockEntity.unregister(world, entity.getUuid());
			}
		}
		if (placements == 0) {
			CorruptionProfiler.logMatrixCubeSkip(world, "attempts_exhausted", "attempts=" + attempts, active, maxActive);
		}
		return placements > 0;
	}
}

