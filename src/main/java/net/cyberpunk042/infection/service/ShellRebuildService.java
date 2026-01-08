package net.cyberpunk042.infection.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

/**
 * Handles shell rebuild / collapse bookkeeping (cooldowns, messaging) so
 * {@link net.cyberpunk042.infection.VirusWorldState} can delegate the
 * low-level block-wrangling.
 */
public final class ShellRebuildService {
	private static final long BASE_SHELL_DELAY = 0L;
	private static final long RADIUS_DELAY = 60L;
	private static final long LOW_TIER_DELAY = 80L;
	private static final long SHELL_JITTER = 60L;
	private static final long PLAYER_OCCUPANCY_DELAY = 40L;
	private static final Set<Block> SHELL_BLOCKS = Set.of(
			ModBlocks.CORRUPTED_STONE,
			ModBlocks.CORRUPTED_CRYING_OBSIDIAN,
			ModBlocks.CORRUPTED_DIAMOND,
			ModBlocks.CORRUPTED_GOLD,
			ModBlocks.CORRUPTED_IRON);
	private static final int MAX_SHELL_RADIUS = 5;
	private static final int MAX_SHELL_HEIGHT = 2;

	public static final class State {
		private final Map<BlockPos, Long> cooldowns = new HashMap<>();
		private boolean shellsCollapsed;
		private boolean rebuildPending;
	}

	public interface Callbacks {
		boolean isVirusCoreBlock(BlockPos pos, BlockState state);

		boolean shouldPushDuringShell();

		void pushPlayers(ServerWorld world, BlockPos pos, int radius);

		void onShellRebuild(ServerWorld world);

		void onShellsCollapsed(ServerWorld world);
	}

	public boolean shellsCollapsed(State state) {
		return state.shellsCollapsed;
	}

	public void setShellsCollapsed(State state, boolean collapsed) {
		state.shellsCollapsed = collapsed;
	}

	public boolean shellRebuildPending(State state) {
		return state.rebuildPending;
	}

	public void setShellRebuildPending(State state, boolean pending) {
		state.rebuildPending = pending;
	}

	public void clearCooldowns(State state) {
		state.cooldowns.clear();
	}

	public void collapseShells(ServerWorld world, State state,
			Iterable<BlockPos> cores,
			Callbacks callbacks) {
		if (state.shellsCollapsed) {
			return;
		}
		state.shellsCollapsed = true;
		state.rebuildPending = true;
		state.cooldowns.clear();
		for (BlockPos core : cores) {
			stripShells(world, core);
		}
		callbacks.onShellsCollapsed(world);
	}

	public void reinforceShells(ServerWorld world, State state,
			InfectionTier tier,
			Iterable<BlockPos> cores,
			Callbacks callbacks) {
		if (state.shellsCollapsed) {
			return;
		}
		int tierIndex = tier.getIndex();
		if (tierIndex < 3) {
			return;
		}
		for (BlockPos core : cores) {
			placeLayer(world, state, callbacks, core, 1, ModBlocks.CORRUPTED_STONE, 0, tierIndex);
			if (tierIndex >= 1) {
				placeLayer(world, state, callbacks, core, 2, ModBlocks.CORRUPTED_CRYING_OBSIDIAN, 1, tierIndex);
			}
			if (tierIndex >= 2) {
				placeLayer(world, state, callbacks, core, 3, ModBlocks.CORRUPTED_DIAMOND, 1, tierIndex);
			}
			if (tierIndex >= 3) {
				placeLayer(world, state, callbacks, core, 4, ModBlocks.CORRUPTED_GOLD, 2, tierIndex);
			}
			if (tierIndex >= 4) {
				placeLayer(world, state, callbacks, core, 5, ModBlocks.CORRUPTED_IRON, 2, tierIndex);
			}
		}
	}

	private void placeLayer(ServerWorld world,
			State state,
			Callbacks callbacks,
			BlockPos center,
			int radius,
			Block block,
			int vertical,
			int tierIndex) {
		final long now = world.getTime();
		final Random random = world.getRandom();
		BlockPos.iterate(center.add(-radius, -vertical, -radius), center.add(radius, vertical, radius)).forEach(pos -> {
			if (pos.equals(center)) {
				return;
			}
			double distance = center.getSquaredDistance(pos);
			if (distance > (double) (radius * radius + Math.max(1, vertical) * 2)) {
				return;
			}
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				return;
			}
			BlockState current = world.getBlockState(pos);
			BlockPos key = pos.toImmutable();
			if (callbacks.isVirusCoreBlock(pos, current) || current.isOf(ModBlocks.SINGULARITY_BLOCK) || current.isOf(block)) {
				state.cooldowns.remove(key);
				return;
			}
			if (!current.isAir() && current.getHardness(world, pos) < 0.0F) {
				return;
			}
			long ready = state.cooldowns.getOrDefault(key, Long.MIN_VALUE);
			if (ready == Long.MIN_VALUE) {
				long delay = computeShellDelay(radius, tierIndex) + random.nextBetween(0, (int) SHELL_JITTER);
				state.cooldowns.put(key, now + delay);
				return;
			}
			if (now < ready) {
				return;
			}
			Box bounds = new Box(pos);
			if (!world.getPlayers(player -> player.isAlive() && !player.isSpectator()
					&& player.getBoundingBox().intersects(bounds)).isEmpty()) {
				state.cooldowns.put(key, now + PLAYER_OCCUPANCY_DELAY);
				return;
			}
			state.cooldowns.remove(key);
			BlockState newState = BlockMutationHelper.stageState(block, tierIndex);
			if (callbacks.shouldPushDuringShell()) {
				callbacks.pushPlayers(world, pos, radius);
			}
			world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
			if (state.rebuildPending) {
				state.rebuildPending = false;
				callbacks.onShellRebuild(world);
			}
		});
	}

	private void stripShells(ServerWorld world, BlockPos center) {
		BlockPos.iterate(center.add(-MAX_SHELL_RADIUS, -MAX_SHELL_HEIGHT, -MAX_SHELL_RADIUS),
				center.add(MAX_SHELL_RADIUS, MAX_SHELL_HEIGHT, MAX_SHELL_RADIUS)).forEach(pos -> {
					if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
						return;
					}
					BlockState state = world.getBlockState(pos);
					if (isShellBlock(state)) {
						world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				});
	}

	private boolean isShellBlock(BlockState state) {
		return SHELL_BLOCKS.contains(state.getBlock());
	}

	private long computeShellDelay(int radius, int tierIndex) {
		long delay = BASE_SHELL_DELAY + radius * RADIUS_DELAY;
		int lowTierSteps = Math.max(0, 3 - tierIndex);
		delay += lowTierSteps * LOW_TIER_DELAY;
		double highTierScale = 1.0D;
		if (tierIndex >= 4) {
			highTierScale = 0.35D;
		} else if (tierIndex >= 3) {
			highTierScale = 0.6D;
		}
		return Math.max(PLAYER_OCCUPANCY_DELAY, Math.round(delay * highTierScale));
	}
}
