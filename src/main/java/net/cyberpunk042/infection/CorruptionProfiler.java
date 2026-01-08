package net.cyberpunk042.infection;


import net.cyberpunk042.log.Logging;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class CorruptionProfiler {
	private CorruptionProfiler() {
	}

	public static void logChunkRewrite(ServerWorld world, ChunkPos pos, int conversions, boolean cleanse) {
		if (conversions <= 0 || !isEnabled(world)) {
			return;
		}
		String action = cleanse ? "cleansed" : "corrupted";
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] chunk {} {} {} blocks",
				worldId(world), pos, action, conversions);
	}

	public static void logBoobytrapPlacement(ServerWorld world, BlockPos pos, String type) {
		if (!shouldLogBoobytrap(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] boobytrap {} armed at {}",
				worldId(world), type, pos);
	}

	public static void logBoobytrapSpread(ServerWorld world, BlockPos pos, String type, int conversions) {
		if (conversions <= 0 || !shouldLogBoobytrap(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] boobytrap {} spread {} blocks around {}",
				worldId(world), type, conversions, pos);
	}

	public static void logBoobytrapTrigger(ServerWorld world, BlockPos pos, String type, String reason, int affectedPlayers) {
		if (!shouldLogBoobytrap(world)) {
			return;
		}
		Logging.INFECTION.topic("boobytrap")
				.at(pos)
				.kv("world", worldId(world))
				.kv("type", type)
				.kv("reason", reason)
				.kv("affectedPlayers", affectedPlayers)
				.warn("[CorruptionProfiler] Boobytrap triggered");
	}

	public static void logMatrixCubeSkip(ServerWorld world, String reason, @Nullable String detail, int active, int maxActive) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] matrix cube skipped: {}{} (active {}/{})",
				worldId(world),
				reason,
				detail == null || detail.isEmpty() ? "" : " [" + detail + "]",
				active,
				maxActive);
	}

	public static void logMatrixCubeSpawn(ServerWorld world, BlockPos pos, int active, int maxActive) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] matrix cube armed at {} (active {}/{})",
				worldId(world), pos, active + 1, maxActive);
	}

	public static void logMatrixCubeEntity(ServerWorld world, BlockPos pos) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] matrix cube entity spawned at {}",
				worldId(world), pos);
	}

	public static void logMatrixCubeAttempt(ServerWorld world, BlockPos anchor, BlockPos target, int seaLevel, int anchorY, int base, int maxY) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("matrix")
				.at(anchor)
				.kv("world", worldId(world))
				.kv("target", target)
				.kv("seaLevel", seaLevel)
				.kv("anchorY", anchorY)
				.kv("base", base)
				.kv("maxY", maxY)
				.info("[CorruptionProfiler] Matrix cube attempt");
	}

	public static void logMatrixCubeCleanup(ServerWorld world, @Nullable BlockPos pos, UUID id) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] matrix cube tracker pruned {} at {}",
				worldId(world), id, pos == null ? "unknown" : pos);
	}

	public static void logTierEvent(ServerWorld world, VirusEventType type, @Nullable BlockPos origin, @Nullable String detail) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] event {} at {}{}",
				worldId(world),
				type.name(),
				origin == null ? "unknown" : origin,
				detail == null || detail.isEmpty() ? "" : " [" + detail + "]");
	}

	public static void logWormSpawn(ServerWorld world, BlockPos pos) {
		if (!isEnabled(world)) {
			return;
		}
		Logging.INFECTION.topic("chunk").info("[CorruptionProfiler:{}] worm spawned at {}",
				worldId(world), pos);
	}

	private static boolean isEnabled(ServerWorld world) {
		return world.getGameRules().getBoolean(TheVirusBlock.VIRUS_CORRUPTION_PROFILER);
	}

	private static Identifier worldId(ServerWorld world) {
		return world.getRegistryKey().getValue();
	}

	// Keyed by dimension ID - naturally bounded to number of dimensions (~3-5)
	private static final Map<Identifier, Long> LAST_BOOBYTRAP_LOG = new ConcurrentHashMap<>();
	private static final long BOOBYTRAP_LOG_INTERVAL_TICKS = 40;

	private static boolean shouldLogBoobytrap(ServerWorld world) {
		if (!isEnabled(world)) {
			return false;
		}
		long now = world.getTime();
		Identifier id = worldId(world);
		Long last = LAST_BOOBYTRAP_LOG.get(id);
		if (last == null || now - last >= BOOBYTRAP_LOG_INTERVAL_TICKS) {
			LAST_BOOBYTRAP_LOG.put(id, now);
			return true;
		}
		return false;
	}
}

