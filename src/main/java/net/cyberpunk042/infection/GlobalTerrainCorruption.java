package net.cyberpunk042.infection;

import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public final class GlobalTerrainCorruption {

	private static final Map<ServerWorld, ChunkWorkTracker> TRACKERS = new WeakHashMap<>();
	private static final int CLEANSE_CHUNKS_PER_TICK = 3;
	private static final int PROCESS_INTERVAL_TICKS = 5;
	
	private static volatile boolean initialized = false;

	private GlobalTerrainCorruption() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			String dim = world.getRegistryKey().getValue().getPath();
			net.cyberpunk042.util.SuperProfiler.start("Terrain:" + dim);
			tickWorld(world);
			net.cyberpunk042.util.SuperProfiler.end("Terrain:" + dim);
		});
	}

	public static void trigger(ServerWorld world, BlockPos origin) {
		VirusWorldState state = VirusWorldState.get(world);
		if (!state.infectionLifecycle().enableTerrainCorruption()) {
			return;
		}
		// Initialize the tracker for this corruption phase (used for cleansing later)
		ChunkWorkTracker tracker = tracker(world);
		tracker.startCorruptionPhase();
		// Mark terrain as corrupted - actual spread handled by BlockMutationHelper
		// which runs every tick with proper difficulty×tier scaling
		state.infectionState().setTerrainCorrupted(true);
		state.markDirty();
	}

	public static void cleanse(ServerWorld world) {
		VirusWorldState state = VirusWorldState.get(world);
		state.infectionLifecycle().beginCleansing();
		ChunkWorkTracker tracker = tracker(world);
		tracker.startCleansingPhase();
		long[] snapshot = tracker.snapshotChunks();
		for (long chunkLong : snapshot) {
			cleanseActiveChunk(world, tracker, new ChunkPos(chunkLong), true);
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			ChunkPos pos = new ChunkPos(player.getBlockPos());
			cleanseActiveChunk(world, tracker, pos, true);
		}
		ChunkPos spawn = new ChunkPos(new BlockPos(0, world.getSeaLevel(), 0));
		cleanseActiveChunk(world, tracker, spawn, true);
	}

	public static int getTrackedChunkCount(ServerWorld world) {
		return tracker(world).trackedChunkCount();
	}

	private static BlockState cleanseBlock(BlockState state) {
		Block block = state.getBlock();
		if (isPortalCriticalBlock(block)) {
			return null;
		}
		if (block == ModBlocks.VIRUS_BLOCK || block == ModBlocks.SINGULARITY_BLOCK || block == ModBlocks.MATRIX_CUBE) {
			return Blocks.AIR.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_DIRT || block == ModBlocks.INFECTED_BLOCK) {
			return Blocks.DIRT.getDefaultState();
		}
		if (block == ModBlocks.INFECTIOUS_CUBE || block == ModBlocks.BACTERIA) {
			return Blocks.AIR.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_GRASS) {
			return Blocks.GRASS_BLOCK.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_WOOD) {
			return Blocks.OAK_PLANKS.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_STONE || block == ModBlocks.CORRUPTED_IRON || block == ModBlocks.CORRUPTED_GLASS
				|| block == ModBlocks.CORRUPTED_CRYING_OBSIDIAN || block == ModBlocks.CORRUPTED_DIAMOND
				|| block == ModBlocks.CORRUPTED_GOLD) {
			return Blocks.STONE.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_ICE) {
			return Blocks.ICE.getDefaultState();
		}
		return null;
	}

	private static void tickWorld(ServerWorld world) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_CHUNK_REWRITE_ON_LOAD)) {
			return;
		}
		VirusWorldState state = VirusWorldState.get(world);
		boolean corrupting = state.infectionState().terrainCorrupted();
		boolean cleansing = state.infectionState().cleansingActive();
		ChunkWorkTracker tracker = TRACKERS.get(world);
		if (!corrupting && !cleansing) {
			if (tracker != null) {
				tracker.reset();
			}
			return;
		}
		tracker = tracker(world);
		// NOTE: Player-following corruption is handled by BlockMutationHelper.mutateAroundSources()
		// which is called every tick from InfectionOperations.runActiveFrame().
		// It uses radius-based spread with proper difficulty×tier scaling.
		// GlobalTerrainCorruption only handles cleansing now.
		if (cleansing) {
			tracker.ensureCleansingPrimed();
			for (int i = 0; i < CLEANSE_CHUNKS_PER_TICK; i++) {
				long chunk = tracker.pollNextChunkForCleanse();
				if (chunk == ChunkWorkTracker.NO_CHUNK) {
					break;
				}
				cleanseActiveChunk(world, tracker, new ChunkPos(chunk), true);
			}
			for (ServerPlayerEntity player : world.getPlayers()) {
				cleanseActiveChunk(world, tracker, new ChunkPos(player.getBlockPos()), false);
			}
		}
	}

	private static ChunkWorkTracker tracker(ServerWorld world) {
		return TRACKERS.computeIfAbsent(world, w -> new ChunkWorkTracker());
	}

	private static void cleanseActiveChunk(ServerWorld world, ChunkWorkTracker tracker, ChunkPos pos, boolean forceLoad) {
		long chunkLong = pos.toLong();
		LongOpenHashSet recorded = tracker.getMutations(chunkLong);
		if ((recorded == null || recorded.isEmpty()) && !forceLoad) {
			return;
		}
		if (!world.isChunkLoaded(chunkLong) && !forceLoad) {
			return;
		}
		WorldChunk chunk = world.getChunk(pos.x, pos.z);
		boolean changed;
		if (recorded == null || recorded.isEmpty()) {
			changed = cleanseRange(world, chunk, world.getBottomY(), worldTop(world));
		} else {
			changed = cleanseRecorded(world, chunk, recorded);
		}
		if (changed) {
			tracker.clearMutations(chunkLong);
		}
	}

	private static boolean cleanseRecorded(ServerWorld world, WorldChunk chunk, LongOpenHashSet recorded) {
		if (recorded.isEmpty()) {
			return false;
		}
		Mutable mutable = new Mutable();
		int conversions = 0;
		// Cache shield service to avoid repeated VirusWorldState.get() lookups
		var shieldService = VirusWorldState.get(world).shieldFieldService();
		LongIterator iterator = recorded.iterator();
		while (iterator.hasNext()) {
			long posLong = iterator.nextLong();
			mutable.set(BlockPos.fromLong(posLong));
			if (shieldService.isShielding(mutable)) {
				continue;
			}
			BlockState replacement = cleanseBlock(chunk.getBlockState(mutable));
			if (replacement != null) {
				world.setBlockState(mutable, replacement, Block.NOTIFY_LISTENERS);
				world.getChunkManager().markForUpdate(mutable);
				conversions++;
			}
		}
		if (conversions > 0) {
			CorruptionProfiler.logChunkRewrite(world, chunk.getPos(), conversions, true);
			return true;
		}
		return false;
	}

	private static boolean cleanseRange(ServerWorld world, WorldChunk chunk, int minY, int maxY) {
		Mutable mutable = new Mutable();
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		int conversions = 0;
		// Cache shield service to avoid repeated VirusWorldState.get() lookups
		var shieldService = VirusWorldState.get(world).shieldFieldService();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y < maxY; y++) {
					mutable.set(startX + x, y, startZ + z);
					if (shieldService.isShielding(mutable)) {
						continue;
					}
					BlockState replacement = cleanseBlock(chunk.getBlockState(mutable));
					if (replacement != null) {
						world.setBlockState(mutable, replacement, Block.NOTIFY_LISTENERS);
						world.getChunkManager().markForUpdate(mutable);
						conversions++;
					}
				}
			}
		}
		if (conversions > 0) {
			CorruptionProfiler.logChunkRewrite(world, chunk.getPos(), conversions, true);
			return true;
		}
		return false;
	}

	private static boolean isPortalCriticalBlock(Block block) {
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

	private static int worldTop(ServerWorld world) {
		return world.getBottomY() + world.getDimension().height();
	}

	private static final class ChunkWorkTracker {

		static final long NO_CHUNK = Long.MIN_VALUE;

		private final Long2LongOpenHashMap nextProcessTick = new Long2LongOpenHashMap();
		private final Long2ObjectOpenHashMap<LongOpenHashSet> mutatedBlocks = new Long2ObjectOpenHashMap<>();
		private long[] cleanseSnapshot = new long[0];
		private int cleanseCursor = 0;
		private boolean cleansingPrimed = false;

		void startCorruptionPhase() {
			nextProcessTick.clear();
			mutatedBlocks.clear();
			cleansingPrimed = false;
			cleanseSnapshot = new long[0];
			cleanseCursor = 0;
		}

		void startCleansingPhase() {
			cleanseSnapshot = mutatedBlocks.keySet().toLongArray();
			cleanseCursor = 0;
			cleansingPrimed = true;
		}

		void ensureCleansingPrimed() {
			if (!cleansingPrimed) {
				startCleansingPhase();
			}
		}

		long[] snapshotChunks() {
			ensureCleansingPrimed();
			return cleanseSnapshot.clone();
		}

		long pollNextChunkForCleanse() {
			if (cleanseCursor >= cleanseSnapshot.length) {
				return NO_CHUNK;
			}
			return cleanseSnapshot[cleanseCursor++];
		}

		boolean canProcess(long chunkPos, long currentTick) {
			return currentTick >= nextProcessTick.getOrDefault(chunkPos, 0L);
		}

		void markProcessed(long chunkPos, long currentTick) {
			nextProcessTick.put(chunkPos, currentTick + PROCESS_INTERVAL_TICKS);
		}

		void recordMutation(long chunkPos, long blockPos) {
			mutatedBlocks.computeIfAbsent(chunkPos, ignored -> new LongOpenHashSet()).add(blockPos);
		}

		LongOpenHashSet getMutations(long chunkPos) {
			return mutatedBlocks.get(chunkPos);
		}

		void clearMutations(long chunkPos) {
			mutatedBlocks.remove(chunkPos);
		}

		void reset() {
			nextProcessTick.clear();
			mutatedBlocks.clear();
			cleansingPrimed = false;
			cleanseSnapshot = new long[0];
			cleanseCursor = 0;
		}

		int trackedChunkCount() {
			return mutatedBlocks.size();
		}
	}

}

