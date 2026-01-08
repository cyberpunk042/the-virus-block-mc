package net.cyberpunk042.infection.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.BoobytrapHelper.TrapSelection;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public final class BlockMutationHelper {
	private BlockMutationHelper() {
	}

	public static void mutateAroundSources(ServerWorld world, Set<BlockPos> sources, InfectionTier tier, boolean apocalypseMode) {
		Random random = world.getRandom();
		VirusWorldState state = VirusWorldState.get(world);
		boolean mutated = false;
		
		// Scale attempts by difficulty AND tier corruption spread multipliers
		// Combined: difficulty scale × tier scale (e.g., EASY tier 1 = 0.3 × 0.2 = 0.06 = 6% of base)
		double difficultyScale = state.tiers().difficulty().getCorruptionSpreadMultiplier();
		double tierScale = tier.getCorruptionSpreadMultiplier();
		double combinedScale = difficultyScale * tierScale;

		List<ServerPlayerEntity> players = world.getPlayers(ServerPlayerEntity::isAlive);
		int surfaceAttemptsRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SURFACE_CORRUPT_ATTEMPTS), 0, 4096);
		// Apply combined scaling to surface attempts
		int scaledSurfaceAttempts = Math.max(1, (int) Math.round(surfaceAttemptsRule * combinedScale));
		
		if (!players.isEmpty()) {
			List<BlockPos> playerAnchors = players.stream().map(ServerPlayerEntity::getBlockPos).toList();
			int attemptsRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_PLAYER_ATTEMPTS), 0, 4096);
			// Apply combined difficulty+tier scaling
			int scaledAttempts = Math.max(1, (int) Math.round(attemptsRule * combinedScale));
			int radiusRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_PLAYER_RADIUS), 4, 256);
			mutated |= mutateFromAnchors(world, playerAnchors, scaledAttempts, radiusRule, tier, apocalypseMode, random, state);
			mutated |= mutateSurfaceLayers(world, playerAnchors, radiusRule, scaledSurfaceAttempts, tier, apocalypseMode, random, state);
		}

		if (!sources.isEmpty()) {
			List<BlockPos> sourceAnchors = new ArrayList<>(sources);
			int attemptsRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_SOURCE_ATTEMPTS), 0, 4096);
			// Apply combined difficulty+tier scaling
			int scaledAttempts = Math.max(1, (int) Math.round(attemptsRule * combinedScale));
			int radiusRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_SOURCE_RADIUS), 4, 256);
			mutated |= mutateFromAnchors(world, sourceAnchors, scaledAttempts, radiusRule, tier, apocalypseMode, random, state);
			mutated |= mutateSurfaceLayers(world, sourceAnchors, radiusRule, scaledSurfaceAttempts, tier, apocalypseMode, random, state);
		}

		if (!mutated && !sources.isEmpty()) {
			// Fallback also scaled by difficulty+tier
			int fallbackAttempts = Math.max(8, (int) Math.round(32 * combinedScale));
			mutateFromAnchors(world, new ArrayList<>(sources), fallbackAttempts, 16, tier, apocalypseMode, random, state);
		}
	}

	private static boolean mutateFromAnchors(ServerWorld world, List<BlockPos> anchors, int attemptsRule, int radiusRule, InfectionTier tier, boolean apocalypseMode, Random random, VirusWorldState state) {
		if (anchors.isEmpty() || attemptsRule <= 0 || radiusRule <= 0) {
			return false;
		}

		int attempts = scaleAttempts(attemptsRule, tier, apocalypseMode);
		int radius = scaleRadius(radiusRule, tier, apocalypseMode);
		boolean mutated = false;
		for (int i = 0; i < attempts; i++) {
			BlockPos origin = anchors.get(random.nextInt(anchors.size()));
			BlockPos target = randomOffset(world, origin, radius, random);
			if (target == null) {
				continue;
			}
			mutateBlock(world, target, random, tier, apocalypseMode, state);
			mutated = true;
		}
		return mutated;
	}

	private static int scaleAttempts(int base, InfectionTier tier, boolean apocalypseMode) {
		int attempts = base + Math.max(1, base / 4) * tier.getIndex();
		if (apocalypseMode) {
			attempts += base;
		}
		return Math.min(4096, attempts);
	}

	private static int scaleRadius(int base, InfectionTier tier, boolean apocalypseMode) {
		int radius = base + tier.getIndex() * Math.max(2, base / 8);
		if (apocalypseMode) {
			radius += 8;
		}
		return MathHelper.clamp(radius, 4, 256);
	}

	@Nullable
	private static BlockPos randomOffset(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int attempt = 0; attempt < 5; attempt++) {
			int x = origin.getX() + random.nextBetween(-radius, radius);
			int y = origin.getY() + random.nextBetween(-radius, radius);
			int z = origin.getZ() + random.nextBetween(-radius, radius);

			// Optimized: avoid BlockPos allocation for chunk check
			if (!isChunkLoaded(world, x, z)) {
				continue;
			}

			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			int clampedY = MathHelper.clamp(y, world.getBottomY(), Math.max(world.getBottomY(), topY));
			return new BlockPos(x, clampedY, z);
		}

		return null;
	}

	private static void mutateBlock(ServerWorld world, BlockPos pos, Random random, InfectionTier tier, boolean apocalypseMode, VirusWorldState state) {
		if (!isChunkLoaded(world, pos)) {
			return;
		}
		if (state.shieldFieldService().isShielding(pos)) {
			return;
		}

		BlockState original = world.getBlockState(pos);
		if (original.isAir()) {
			return;
		}
		if (!original.getFluidState().isEmpty()) {
			return;
		}
		if (isPortalCriticalBlock(original)) {
			return;
		}

		if (original.isOf(ModBlocks.VIRUS_BLOCK)
				|| original.isOf(ModBlocks.SINGULARITY_BLOCK)
				|| original.isOf(ModBlocks.INFECTED_BLOCK)
				|| original.isOf(ModBlocks.INFECTIOUS_CUBE)
				|| original.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)
				|| original.isOf(ModBlocks.BACTERIA)) {
			return;
		}

		BlockState replacement = pickReplacement(world, original, random, tier, apocalypseMode);
		if (replacement == null) {
			return;
		}
		replacement = applyStage(replacement, tier);

		if (apocalypseMode && random.nextFloat() < 0.1F) {
			world.breakBlock(pos, false);
			return;
		}

		TrapSelection trap = BoobytrapHelper.selectTrap(world);
		if (trap != null) {
			BoobytrapHelper.applyTrap(world, pos, trap);
			return;
		}
		world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
	}

	private static BlockState pickReplacement(ServerWorld world, BlockState original, Random random, InfectionTier tier, boolean apocalypseMode) {
		boolean corruptSand = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_SAND);
		boolean corruptIce = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_ICE);
		boolean corruptSnow = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_SNOW);

		if (original.isIn(BlockTags.LOGS) || original.isIn(BlockTags.PLANKS)) {
			return ModBlocks.CORRUPTED_WOOD.getDefaultState();
		}

		if (original.isOf(Blocks.GRASS_BLOCK)) {
			return ModBlocks.INFECTED_GRASS.getDefaultState();
		}

		if (original.isIn(BlockTags.DIRT) || original.isOf(Blocks.FARMLAND) || original.isOf(Blocks.PODZOL)) {
			return ModBlocks.CORRUPTED_DIRT.getDefaultState();
		}

		if (isGlassLike(original) || original.isIn(BlockTags.IMPERMEABLE)) {
			return ModBlocks.CORRUPTED_GLASS.getDefaultState();
		}

		if (corruptIce && original.isOf(Blocks.ICE)) {
			return ModBlocks.CORRUPTED_ICE.getDefaultState();
		}
		if (corruptIce && (original.isOf(Blocks.PACKED_ICE) || original.isOf(Blocks.BLUE_ICE))) {
			return ModBlocks.CORRUPTED_PACKED_ICE.getDefaultState();
		}
		if (corruptSand && (original.isOf(Blocks.SAND) || original.isOf(Blocks.RED_SAND))) {
			return ModBlocks.CORRUPTED_SAND.getDefaultState();
		}
		if (corruptSnow && (original.isOf(Blocks.SNOW_BLOCK) || original.isOf(Blocks.POWDER_SNOW))) {
			return ModBlocks.CORRUPTED_SNOW_BLOCK.getDefaultState();
		}
		if (corruptSnow && original.isOf(Blocks.SNOW)) {
			return ModBlocks.CORRUPTED_SNOW.getDefaultState();
		}

		if (original.isIn(BlockTags.BASE_STONE_OVERWORLD) || original.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
			return ModBlocks.CORRUPTED_STONE.getDefaultState();
		}

		if (original.isOf(Blocks.IRON_BLOCK) || original.isOf(Blocks.ANVIL)) {
			return ModBlocks.CORRUPTED_IRON.getDefaultState();
		}

		if (original.isOf(Blocks.OBSIDIAN) || original.isOf(Blocks.CRYING_OBSIDIAN)) {
			return ModBlocks.CORRUPTED_CRYING_OBSIDIAN.getDefaultState();
		}

		if (original.isOf(Blocks.DIAMOND_BLOCK) || original.isIn(BlockTags.DIAMOND_ORES)) {
			return ModBlocks.CORRUPTED_DIAMOND.getDefaultState();
		}

		if (original.isOf(Blocks.GOLD_BLOCK) || original.isIn(BlockTags.GOLD_ORES)) {
			return ModBlocks.CORRUPTED_GOLD.getDefaultState();
		}

		if (original.isIn(BlockTags.LEAVES)) {
			return Blocks.SCULK.getDefaultState();
		}

		return switch (random.nextInt(8)) {
			case 0 -> ModBlocks.CORRUPTED_STONE.getDefaultState();
			case 1 -> ModBlocks.CORRUPTED_DIRT.getDefaultState();
			case 2 -> ModBlocks.CORRUPTED_WOOD.getDefaultState();
			case 3 -> ModBlocks.CORRUPTED_GLASS.getDefaultState();
			case 4 -> ModBlocks.CORRUPTED_IRON.getDefaultState();
			case 5 -> ModBlocks.CORRUPTED_CRYING_OBSIDIAN.getDefaultState();
			case 6 -> ModBlocks.CORRUPTED_DIAMOND.getDefaultState();
			default -> ModBlocks.CORRUPTED_GOLD.getDefaultState();
		};
	}

	private static boolean mutateSurfaceLayers(ServerWorld world, List<BlockPos> anchors, int radiusRule, int attemptsRule, InfectionTier tier, boolean apocalypseMode, Random random, VirusWorldState state) {
		if (anchors.isEmpty() || attemptsRule <= 0) {
			return false;
		}

		boolean sandEnabled = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_SAND);
		boolean iceEnabled = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_ICE);
		boolean snowEnabled = TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.CORRUPT_SNOW);
		if (!sandEnabled && !iceEnabled && !snowEnabled) {
			return false;
		}

		int radius = MathHelper.clamp(radiusRule * 2, 8, 192);
		int tierScale = Math.max(1, tier.getLevel());
		int attempts = MathHelper.clamp(attemptsRule * tierScale, 0, 4096);
		attempts = Math.max(attempts, anchors.size() * 40);
		attempts = Math.min(attempts, 4096);
		int budget = state.infection().claimSurfaceMutations(world, tier, attempts);
		if (budget <= 0) {
			return false;
		}
		attempts = budget;
		boolean mutated = false;
		for (int i = 0; i < attempts; i++) {
			BlockPos anchor = anchors.get(random.nextInt(anchors.size()));
			BlockPos surface = findSurface(world, anchor, radius, random);
			if (surface == null) {
				continue;
			}
			if (convertSurfaceBlock(world, surface, sandEnabled, iceEnabled, snowEnabled, state)) {
				mutated = true;
			}
		}
		return mutated;
	}

	private static BlockPos findSurface(ServerWorld world, BlockPos anchor, int radius, Random random) {
		for (int attempt = 0; attempt < 6; attempt++) {
			int x = anchor.getX() + random.nextBetween(-radius, radius);
			int z = anchor.getZ() + random.nextBetween(-radius, radius);
			// Optimized: avoid BlockPos allocation for chunk check
			if (!isChunkLoaded(world, x, z)) {
				continue;
			}
			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			if (topY < world.getBottomY()) {
				continue;
			}
			return new BlockPos(x, topY, z);
		}
		return null;
	}

	private static boolean convertSurfaceBlock(ServerWorld world, BlockPos pos, boolean sandEnabled, boolean iceEnabled, boolean snowEnabled, VirusWorldState worldState) {
		if (worldState.shieldFieldService().isShielding(pos)) {
			return false;
		}
		BlockState blockState = world.getBlockState(pos);
		boolean mutated = false;

		if (!blockState.isAir()) {
			if (sandEnabled && (blockState.isOf(Blocks.SAND) || blockState.isOf(Blocks.RED_SAND))) {
				world.setBlockState(pos, ModBlocks.CORRUPTED_SAND.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			} else if (iceEnabled && blockState.isOf(Blocks.ICE)) {
				world.setBlockState(pos, ModBlocks.CORRUPTED_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			} else if (iceEnabled && (blockState.isOf(Blocks.PACKED_ICE) || blockState.isOf(Blocks.BLUE_ICE))) {
				world.setBlockState(pos, ModBlocks.CORRUPTED_PACKED_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			} else if (snowEnabled && (blockState.isOf(Blocks.SNOW_BLOCK) || blockState.isOf(Blocks.POWDER_SNOW))) {
				world.setBlockState(pos, ModBlocks.CORRUPTED_SNOW_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			} else if (snowEnabled && blockState.isOf(Blocks.SNOW)) {
				world.setBlockState(pos, ModBlocks.CORRUPTED_SNOW.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			}
		}

		if (snowEnabled) {
			BlockPos above = pos.up();
			BlockState aboveState = world.getBlockState(above);
			if (aboveState.isOf(Blocks.SNOW)) {
				world.setBlockState(above, ModBlocks.CORRUPTED_SNOW.getDefaultState(), Block.NOTIFY_LISTENERS);
				mutated = true;
			}
		}

		return mutated;
	}

	public static void corruptFlora(ServerWorld world, BlockPos origin, int radius) {
		Random random = world.getRandom();
		VirusWorldState worldState = VirusWorldState.get(world);
		for (int i = 0; i < 32; i++) {
			BlockPos target = randomOffset(world, origin, radius, random);
			if (target == null) {
				continue;
			}
			if (!isChunkLoaded(world, target)) {
				continue;
			}
			if (worldState.shieldFieldService().isShielding(target)) {
				continue;
			}

			BlockState blockState = world.getBlockState(target);
			if (blockState.isOf(ModBlocks.INFECTED_BLOCK) || blockState.isOf(ModBlocks.INFECTIOUS_CUBE) || blockState.isOf(ModBlocks.BACTERIA)) {
				continue;
			}
			if (blockState.isIn(BlockTags.SAPLINGS) || blockState.isOf(Blocks.SHORT_GRASS) || blockState.isOf(Blocks.FERN) || blockState.isOf(Blocks.TALL_GRASS)) {
				world.setBlockState(target, ModBlocks.CORRUPTED_WOOD.getDefaultState(), Block.NOTIFY_ALL);
			} else if (blockState.isIn(BlockTags.LEAVES)) {
				world.setBlockState(target, Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}

	private static boolean isGlassLike(BlockState state) {
		return state.isOf(Blocks.GLASS)
				|| state.isOf(Blocks.TINTED_GLASS)
				|| state.getBlock() instanceof TransparentBlock
				|| state.getBlock() instanceof PaneBlock;
	}

	private static boolean isChunkLoaded(ServerWorld world, BlockPos pos) {
		return world.isChunkLoaded(ChunkPos.toLong(pos));
	}

	/** Optimized overload that avoids BlockPos allocation */
	private static boolean isChunkLoaded(ServerWorld world, int x, int z) {
		return world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4));
	}

	private static BlockState applyStage(BlockState state, InfectionTier tier) {
		CorruptionStage stage = tier.getIndex() >= 2 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			return state.with(CorruptedStoneBlock.STAGE, stage);
		}
		if (state.contains(CorruptedGlassBlock.STAGE)) {
			return state.with(CorruptedGlassBlock.STAGE, stage);
		}
		return state;
	}

	private static boolean isPortalCriticalBlock(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.OBSIDIAN
				|| block == Blocks.CRYING_OBSIDIAN
				|| block == Blocks.NETHER_PORTAL
				|| block == Blocks.END_PORTAL
				|| block == Blocks.END_PORTAL_FRAME
				|| block == Blocks.END_GATEWAY
				|| block == Blocks.RESPAWN_ANCHOR
				|| block == Blocks.CRAFTING_TABLE
				|| block instanceof BedBlock;
	}

	/**
	 * Returns a corruption-staged block state based on tier index.
	 */
	public static BlockState stageState(Block block, int tierIndex) {
		BlockState state = block.getDefaultState();
		CorruptionStage stage = tierIndex >= 2 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			return state.with(CorruptedStoneBlock.STAGE, stage);
		}
		if (state.contains(CorruptedGlassBlock.STAGE)) {
			return state.with(CorruptedGlassBlock.STAGE, stage);
		}
		return state;
	}
}

