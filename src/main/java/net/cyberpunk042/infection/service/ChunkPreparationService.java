package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.LogLevel;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.Logging;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

/**
 * Handles chunk preloading and pre-generation for the singularity collapse.
 * Migrated from {@link SingularityLifecycleService} to reduce its size.
 */
public final class ChunkPreparationService {

	/**
	 * Persistent state for chunk preparation (preload and pre-generation).
	 */
	public static final class State {
		public Deque<Long> preloadQueue = new ArrayDeque<>();
		public boolean preloadComplete;
		public int preloadMissingChunks;
		public int preloadLogCooldown;
		public Deque<Long> preGenQueue = new ArrayDeque<>();
		public boolean preGenComplete;
		public int preGenMissingChunks;
		public int preGenTotalChunks;
		public ChunkPos preGenCenter;
		public int preGenLogCooldown;
	}

	private final VirusWorldState host;
	private final State state;

	public ChunkPreparationService(VirusWorldState host, State state) {
		this.host = Objects.requireNonNull(host, "host");
		this.state = Objects.requireNonNull(state, "state");
	}

	public State state() {
		return state;
	}

	public void tickChunkPreload() {
		ServerWorld world = host.world();
		if (!host.collapseConfig().configuredPreloadEnabled()) {
			if (!state.preloadComplete) {
				state.preloadQueue.clear();
				state.preloadMissingChunks = 0;
				state.preloadComplete = true;
				Logging.SINGULARITY.info("[preload] skipped (disabled via config)");
			}
			return;
		}
		if (state.preloadComplete) {
			return;
		}
		// Wait for preGen to complete before starting preload
		if (!state.preGenComplete) {
			return;
		}
		// Populate preload queue from preGen results if empty
		if (state.preloadQueue.isEmpty() && state.preGenCenter != null) {
			populatePreloadQueue();
		}
		if (state.preloadQueue.isEmpty()) {
			finishChunkPreload(world);
			return;
		}
		ServerChunkManager chunkManager = world.getChunkManager();
		boolean allowGeneration = host.collapseConfig().configuredChunkGenerationAllowed(world);
		int budget = host.collapseConfig().configuredPreloadChunksPerTick();
		int before = state.preloadQueue.size();
	try (LogScope scope = Logging.SINGULARITY.scope("preload-chunks", LogLevel.INFO)) {
		while (budget-- > 0 && !state.preloadQueue.isEmpty()) {
			long packed = state.preloadQueue.pollFirst();
			ChunkPos pos = new ChunkPos(packed);
			SingularityChunkContext.pushChunkBypass(pos);
			try {
				if (world.isChunkLoaded(packed)) {
					host.singularity().phase().pinSingularityChunk(pos);
					if (SingularityDiagnostics.enabled()) {
						scope.branch("chunk").kv("pos", pos).kv("status", "ready").kv("cached", true);
					}
					continue;
				}
				boolean outsideBorder = !world.getWorldBorder().contains(pos.getCenterX(), pos.getCenterZ());
				try {
					Chunk chunk = chunkManager.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
					if (chunk == null) {
						String reason = outsideBorder ? "outsideBorder" : (allowGeneration ? "missingChunk" : "generationDisabled");
						handlePreloadFailure(pos, allowGeneration, reason);
					} else {
						host.singularity().phase().pinSingularityChunk(pos);
						if (SingularityDiagnostics.enabled()) {
							if (allowGeneration) {
								scope.branch("chunk").kv("pos", pos).kv("status", "loaded");
							} else {
								scope.branch("chunk").kv("pos", pos).kv("status", "loaded").kv("mode", "diskOnly");
							}
						}
					}
				} catch (IllegalStateException ex) {
					logChunkLoadException("preload", pos, allowGeneration, ex);
					handlePreloadFailure(pos, allowGeneration, ex.getMessage());
				}
			} finally {
				SingularityChunkContext.popChunkBypass(pos);
			}
		}
	}
		logPreloadProgress(before - state.preloadQueue.size(), state.preloadQueue.size());
		if (state.preloadQueue.isEmpty()) {
			finishChunkPreload(world);
		}
	}

	public void tickPreGeneration() {
		ServerWorld world = host.world();
		if (!host.collapseConfig().configuredPreGenEnabled()) {
			if (!state.preGenComplete) {
				state.preGenQueue.clear();
				state.preGenMissingChunks = 0;
				state.preGenComplete = true;
				Logging.SINGULARITY.info("[preGen] skipped (disabled via config)");
			}
			return;
		}
		if (state.preGenComplete) {
			return;
		}
		if (host.singularityState().singularityState != SingularityState.DORMANT
				&& host.singularityState().singularityState != SingularityState.FUSING) {
			return;
		}
		host.singularity().ensureCenter(world);
		BlockPos center = host.singularityState().center;
		if (center == null) {
			return;
		}
		if (state.preGenQueue.isEmpty()) {
			rebuildPreGenQueue(center);
		}
		if (state.preGenQueue.isEmpty()) {
			return;
		}
		ServerChunkManager chunkManager = world.getChunkManager();
		int budget = host.collapseConfig().configuredPreGenChunksPerTick();
		int before = state.preGenQueue.size();
		while (budget-- > 0 && !state.preGenQueue.isEmpty()) {
			long packed = state.preGenQueue.pollFirst();
			ChunkPos pos = new ChunkPos(packed);
			try {
				chunkManager.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
			} catch (IllegalStateException ex) {
				logChunkLoadException("pregen", pos, true, ex);
				state.preGenMissingChunks++;
			}
		}
		logPreGenProgress(before - state.preGenQueue.size(), state.preGenQueue.size());
		if (state.preGenQueue.isEmpty()) {
			state.preGenComplete = true;
			Logging.SINGULARITY.info("[preGen] complete center={} total={} missing={}",
					state.preGenCenter,
					state.preGenTotalChunks,
					state.preGenMissingChunks);
			state.preGenQueue.clear();
			state.preGenLogCooldown = SingularityLifecycleService.PREGEN_LOG_INTERVAL;
			host.markDirty();
		}
	}

	public void rebuildPreGenQueue(BlockPos centerPos) {
		ServerWorld world = host.world();
		ChunkPos centerChunk = new ChunkPos(centerPos);
		state.preGenCenter = centerChunk;
		state.preGenQueue.clear();
		state.preGenMissingChunks = 0;
		state.preGenComplete = false;
		LongSet seen = new LongOpenHashSet();
		for (int radius = host.collapseConfig().configuredPregenRadiusChunks(); radius >= 0; radius--) {
			appendChunkRing(centerChunk, radius, seen, state.preGenQueue);
		}
		state.preGenTotalChunks = state.preGenQueue.size();
		Logging.SINGULARITY.info("[preGen] queued {} chunks within {} blocks (center={})",
				state.preGenTotalChunks,
				String.format("%.2f", host.collapseConfig().configuredPreGenRadiusBlocks()),
				state.preGenCenter);
		host.markDirty();
	}

	/**
	 * Populates preload queue with the same chunks that were pre-generated.
	 * Called after preGen completes.
	 */
	private void populatePreloadQueue() {
		if (state.preGenCenter == null) {
			return;
		}
		state.preloadQueue.clear();
		state.preloadMissingChunks = 0;
		LongSet seen = new LongOpenHashSet();
		for (int radius = host.collapseConfig().configuredPregenRadiusChunks(); radius >= 0; radius--) {
			appendChunkRing(state.preGenCenter, radius, seen, state.preloadQueue);
		}
		Logging.SINGULARITY.info("[preload] queued {} chunks (after preGen complete)",
				state.preloadQueue.size());
	}

	private void appendChunkRing(ChunkPos center, int radius, LongSet seen, Deque<Long> target) {
		if (radius == 0) {
			addChunkToQueue(center.x, center.z, seen, target);
			return;
		}
		int minX = center.x - radius;
		int maxX = center.x + radius;
		int minZ = center.z - radius;
		int maxZ = center.z + radius;
		for (int x = minX; x <= maxX; x++) {
			addChunkToQueue(x, minZ, seen, target);
			addChunkToQueue(x, maxZ, seen, target);
		}
		for (int z = minZ + 1; z <= maxZ - 1; z++) {
			addChunkToQueue(minX, z, seen, target);
			addChunkToQueue(maxX, z, seen, target);
		}
	}

	private void addChunkToQueue(int chunkX, int chunkZ, LongSet seen, Deque<Long> target) {
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		long packed = pos.toLong();
		if (seen.add(packed)) {
			target.addLast(packed);
		}
	}

	private void finishChunkPreload(ServerWorld world) {
		if (state.preloadComplete) {
			return;
		}
		state.preloadComplete = true;
		Logging.SINGULARITY.info("[preload] complete missing={} pinned={}",
				state.preloadMissingChunks,
				host.singularityState().singularityPinnedChunks.size());
		state.preloadLogCooldown = SingularityLifecycleService.PRELOAD_LOG_INTERVAL;
		if (state.preloadMissingChunks > 0) {
			Logging.SINGULARITY.warn("[Singularity] {} collapse chunks were not prepared before the event. Enable chunk generation or pre-load the area to avoid skips.",
					state.preloadMissingChunks);
		}
		// Schedule pre-collapse water drainage now that chunks are loaded
		host.collapseModule().queues().schedulePreCollapseDrainageJob();
	}

	private void handlePreloadFailure(ChunkPos pos, boolean generationAttempted, String reason) {
		state.preloadMissingChunks++;
		if (SingularityDiagnostics.enabled()) {
			Logging.SINGULARITY.warn("[Singularity] chunk preload failed for {} (generationAllowed={} reason={})",
					pos,
					generationAttempted,
					reason == null ? "<unknown>" : reason);
		}
	}

	private void logPreloadProgress(int processed, int remaining) {
		if (remaining > 0 && processed <= 0 && state.preloadLogCooldown > 0) {
			state.preloadLogCooldown--;
			return;
		}
		state.preloadLogCooldown = SingularityLifecycleService.PRELOAD_LOG_INTERVAL;
		Logging.SINGULARITY.info("[preload] processed={} remaining={} missing={} pinned={}",
				processed,
				remaining,
				state.preloadMissingChunks,
				host.singularityState().singularityPinnedChunks.size());
	}

	private void logPreGenProgress(int processed, int remaining) {
		if (remaining > 0 && processed <= 0 && state.preGenLogCooldown > 0) {
			state.preGenLogCooldown--;
			return;
		}
		state.preGenLogCooldown = SingularityLifecycleService.PREGEN_LOG_INTERVAL;
		Logging.SINGULARITY.info("[preGen] processed={} remaining={} missing={} center={}",
				processed,
				remaining,
				state.preGenMissingChunks,
				state.preGenCenter);
	}

	public void logChunkLoadException(String phase,
			ChunkPos chunk,
			boolean generationAllowed,
			IllegalStateException error) {
		ServerWorld world = host.world();
		if (!SingularityDiagnostics.enabled()) {
			return;
		}
		boolean bypassing = SingularityChunkContext.isBypassingChunk(chunk.x, chunk.z);
		boolean outsideBorder = !world.getWorldBorder().contains(chunk.getCenterX(), chunk.getCenterZ());
		double distance = host.collapseModule().execution().chunkDistanceFromCenter(chunk.toLong());
		Logging.SINGULARITY.warn("[Singularity] chunk load failed phase={} chunk={} allowGen={} bypass={} outsideBorder={} distance={} thread={}",
				phase,
				chunk,
				generationAllowed,
				bypassing,
				outsideBorder,
				String.format("%.2f", distance),
				Thread.currentThread().getName(),
				error);
	}
}

