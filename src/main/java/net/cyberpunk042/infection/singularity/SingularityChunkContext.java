package net.cyberpunk042.infection.singularity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;

/**
 * Tracks when the Singularity collapse is actively manipulating chunks so that
 * mixins can differentiate collapse-driven loads from normal gameplay.
 */
public final class SingularityChunkContext {
	private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
	private static final Long2IntOpenHashMap ACTIVE_BYPASS = new Long2IntOpenHashMap();
	private static final Set<ServerWorld> GUARDED_WORLDS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Map<WorldBorder, ServerWorld> BORDER_WORLDS =
			Collections.synchronizedMap(new IdentityHashMap<>());

	private SingularityChunkContext() {
	}

	private static boolean chunkLoggingEnabled() {
		return SingularityDiagnostics.logChunkSamples();
	}

	private static boolean bypassLoggingEnabled() {
		return SingularityDiagnostics.logBypasses();
	}

	public static void push(ServerWorld world) {
		ACTIVE.set(new Context(world));
	}

	public static void pop(ServerWorld world) {
		Context ctx = ACTIVE.get();
		if (ctx != null && (ctx.world == world || world == null)) {
			if (chunkLoggingEnabled()) {
				Logging.SINGULARITY.topic("chunk").info(
						"tick={} colsProcessed={} colsCompleted={} blocksCleared={} skippedMissing={} skippedBorder={} waterRemoved={}",
						world.getTime(),
						ctx.columnsProcessed,
						ctx.columnsCompleted,
						ctx.blocksCleared,
						ctx.skippedMissingChunks,
						ctx.skippedBorderChunks,
						ctx.waterCellsCleared);
				ctx.flushBlockedChunkLogs();
				ctx.flushBroadcastLogs();
			}
			ACTIVE.remove();
		}
	}

	public static boolean isActive(ServerWorld world) {
		Context ctx = ACTIVE.get();
		return ctx != null && ctx.world == world;
	}

	public static boolean isActive() {
		return ACTIVE.get() != null;
	}

	public static boolean shouldGuard(ServerWorld world) {
		return isActive(world) || GUARDED_WORLDS.contains(world);
	}

	public static void enableBorderGuard(ServerWorld world) {
		if (world != null) {
			GUARDED_WORLDS.add(world);
			BORDER_WORLDS.put(world.getWorldBorder(), world);
		}
	}

	public static void disableBorderGuard(ServerWorld world) {
		if (world != null) {
			GUARDED_WORLDS.remove(world);
			BORDER_WORLDS.remove(world.getWorldBorder());
		}
	}

	public static ServerWorld getActiveWorld() {
		Context ctx = ACTIVE.get();
		return ctx == null ? null : ctx.world;
	}

	public static void recordColumnProcessed() {
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.columnsProcessed++;
		}
	}

	public static void recordSkippedMissing() {
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.skippedMissingChunks++;
		}
	}

	public static void recordSkippedBorder() {
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.skippedBorderChunks++;
		}
	}

	public static void recordColumnCompleted() {
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.columnsCompleted++;
		}
	}

	public static void recordBlocksCleared(int amount) {
		if (amount <= 0) {
			return;
		}
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.blocksCleared += amount;
		}
	}

	public static void recordWaterCleared() {
		Context ctx = ACTIVE.get();
		if (ctx != null) {
			ctx.waterCellsCleared++;
		}
	}

	public static void recordBroadcastBuffered(ChunkPos chunk, CollapseBroadcastMode mode) {
		Context ctx = ACTIVE.get();
		if (ctx == null || !chunkLoggingEnabled()) {
			return;
		}
		ctx.broadcastBuffered++;
		ctx.recordBroadcastSample(chunk, "bypass:" + (mode == null ? "unknown" : mode.id()));
	}

	public static void recordBroadcastFlushed(ChunkPos chunk) {
		Context ctx = ACTIVE.get();
		if (ctx == null || !chunkLoggingEnabled()) {
			return;
		}
		ctx.broadcastFlushed++;
		ctx.recordBroadcastSample(chunk, "flush");
	}

	public static void pushChunkBypass(ChunkPos chunk) {
		synchronized (ACTIVE_BYPASS) {
			long key = chunk.toLong();
			int next = ACTIVE_BYPASS.get(key) + 1;
			ACTIVE_BYPASS.put(key, next);
			if (next == 1) {
				logBypassChange("push", chunk, next);
			}
		}
	}

	public static void popChunkBypass(ChunkPos chunk) {
		synchronized (ACTIVE_BYPASS) {
			long key = chunk.toLong();
			int current = ACTIVE_BYPASS.get(key);
			if (current <= 1) {
				ACTIVE_BYPASS.remove(key);
				if (current > 0) {
					logBypassChange("pop", chunk, 0);
				}
			} else {
				int next = current - 1;
				ACTIVE_BYPASS.put(key, next);
				if (next == 0) {
					logBypassChange("pop", chunk, 0);
				}
			}
		}
	}

	public static boolean isBypassingChunk(int chunkX, int chunkZ) {
		synchronized (ACTIVE_BYPASS) {
			return ACTIVE_BYPASS.containsKey(ChunkPos.toLong(chunkX, chunkZ));
		}
	}

	public static int bypassCountForChunk(int chunkX, int chunkZ) {
		synchronized (ACTIVE_BYPASS) {
			return ACTIVE_BYPASS.get(ChunkPos.toLong(chunkX, chunkZ));
		}
	}

	public static int activeBypassEntries() {
		synchronized (ACTIVE_BYPASS) {
			return ACTIVE_BYPASS.size();
		}
	}

	public static String formatBypassState(int chunkX, int chunkZ) {
		int count = bypassCountForChunk(chunkX, chunkZ);
		int total = activeBypassEntries();
		return "count=" + count + " active=" + total;
	}

	public static boolean shouldBypassBorder(WorldBorder border, double x, double z) {
		if (border == null) {
			return false;
		}
		ServerWorld world = BORDER_WORLDS.get(border);
		if (world == null) {
			return false;
		}
		int chunkX = ChunkSectionPos.getSectionCoord(MathHelper.floor(x));
		int chunkZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(z));
		return isBypassingChunk(chunkX, chunkZ);
	}

	public static void recordChunkBlocked(ChunkPos chunk, String reason) {
		Context ctx = ACTIVE.get();
		if (ctx == null || !chunkLoggingEnabled()) {
			return;
		}
		ctx.recordBlockedChunk(chunk, reason);
	}

	private static final class Context {
		final ServerWorld world;
		int columnsProcessed;
		int skippedMissingChunks;
		int skippedBorderChunks;
		int waterCellsCleared;
		int columnsCompleted;
		int blocksCleared;
		int missingSamples;
		int borderSamples;
		int broadcastBuffered;
		int broadcastFlushed;
		Long2ObjectOpenHashMap<BlockedChunkSample> blockedChunks;
		Long2ObjectOpenHashMap<BlockedChunkSample> broadcastSamples;

		Context(ServerWorld world) {
			this.world = world;
		}

		void recordBlockedChunk(ChunkPos chunk, String reason) {
			if (blockedChunks == null) {
				blockedChunks = new Long2ObjectOpenHashMap<>();
			}
			long key = chunk.toLong();
			BlockedChunkSample sample = blockedChunks.get(key);
			if (sample == null) {
				blockedChunks.put(key, new BlockedChunkSample(chunk, reason));
			} else {
				sample.count++;
			}
		}

		void flushBlockedChunkLogs() {
			if (blockedChunks == null || blockedChunks.isEmpty()) {
				return;
			}
			StringBuilder builder = new StringBuilder("blockedChunks=");
			int shown = 0;
			int limit = 4;
			ObjectIterator<Long2ObjectOpenHashMap.Entry<BlockedChunkSample>> iterator = blockedChunks.long2ObjectEntrySet().iterator();
			while (iterator.hasNext()) {
				Long2ObjectOpenHashMap.Entry<BlockedChunkSample> entry = iterator.next();
				BlockedChunkSample sample = entry.getValue();
				if (shown++ >= limit) {
					builder.append(" ... +").append(blockedChunks.size() - limit).append(" more");
					break;
				}
				builder.append('[')
						.append(sample.chunk.x)
						.append(',')
						.append(sample.chunk.z)
						.append("]=")
						.append(sample.count)
						.append('x')
						.append(' ')
						.append(sample.reason);
				if (iterator.hasNext() && shown < limit) {
					builder.append("; ");
				}
			}
			Logging.SINGULARITY.topic("chunk").info( builder.toString());
			blockedChunks.clear();
		}

		void recordBroadcastSample(ChunkPos chunk, String reason) {
			if (broadcastSamples == null) {
				broadcastSamples = new Long2ObjectOpenHashMap<>();
			}
			long key = chunk.toLong();
			BlockedChunkSample sample = broadcastSamples.get(key);
			if (sample == null) {
				broadcastSamples.put(key, new BlockedChunkSample(chunk, reason));
			} else {
				sample.reason = reason;
				sample.count++;
			}
		}

		void flushBroadcastLogs() {
			if ((broadcastBuffered <= 0 && broadcastFlushed <= 0) || !chunkLoggingEnabled()) {
				if (broadcastSamples != null) {
					broadcastSamples.clear();
				}
				broadcastBuffered = 0;
				broadcastFlushed = 0;
				return;
			}
			StringBuilder builder = new StringBuilder("[broadcast] buffered=")
					.append(broadcastBuffered)
					.append(" flushed=")
					.append(broadcastFlushed);
			if (broadcastSamples != null && !broadcastSamples.isEmpty()) {
				builder.append(" samples=");
				int shown = 0;
				int limit = 3;
				ObjectIterator<Long2ObjectOpenHashMap.Entry<BlockedChunkSample>> iterator = broadcastSamples.long2ObjectEntrySet().iterator();
				while (iterator.hasNext() && shown < limit) {
					BlockedChunkSample sample = iterator.next().getValue();
					if (shown++ > 0) {
						builder.append("; ");
					}
					builder.append('[')
							.append(sample.chunk.x)
							.append(',')
							.append(sample.chunk.z)
							.append("]=")
							.append(sample.count)
							.append('x')
							.append(sample.reason);
				}
				if (broadcastSamples.size() > limit) {
					builder.append(" ... +").append(broadcastSamples.size() - limit).append(" more");
				}
				broadcastSamples.clear();
			}
			Logging.SINGULARITY.topic("chunk").info( builder.toString());
			broadcastBuffered = 0;
			broadcastFlushed = 0;
		}
	}

	private static final class BlockedChunkSample {
		final ChunkPos chunk;
		String reason;
		int count = 1;

		BlockedChunkSample(ChunkPos chunk, String reason) {
			this.chunk = chunk;
			this.reason = reason == null ? "<unknown>" : reason;
		}
	}

	public static void sampleMissingChunk(ChunkPos chunk, double distance, boolean generationAllowed) {
		Context ctx = ACTIVE.get();
		if (ctx == null || !chunkLoggingEnabled()) {
			return;
		}
		if (ctx.missingSamples >= 3) {
			return;
		}
		ctx.missingSamples++;
		Logging.SINGULARITY.topic("chunk").info( "sample missing chunk={} dist={} allowGen={}",
				formatChunk(chunk),
				formatDistance(distance),
				generationAllowed);
	}

	public static void sampleBorderRejectedChunk(ChunkPos chunk, double distance, double borderRadius) {
		Context ctx = ACTIVE.get();
		if (ctx == null || !chunkLoggingEnabled()) {
			return;
		}
		if (ctx.borderSamples >= 3) {
			return;
		}
		ctx.borderSamples++;
		Logging.SINGULARITY.topic("chunk").info( "sample border skip chunk={} dist={} borderRadius={}",
				formatChunk(chunk),
				formatDistance(distance),
				formatDistance(borderRadius));
	}

	private static double formatDistance(double value) {
		return Math.round(value * 100.0D) / 100.0D;
	}

	private static void logBypassChange(String action, ChunkPos chunk, int count) {
		if (!bypassLoggingEnabled()) {
			return;
		}
		Logging.SINGULARITY.topic("chunk").info(
				"[bypass] {} chunk={} count={} thread={}",
				action,
				chunk,
				count,
				Thread.currentThread().getName());
	}

	private static String formatChunk(ChunkPos chunk) {
		return chunk.x + "," + chunk.z;
	}

}

