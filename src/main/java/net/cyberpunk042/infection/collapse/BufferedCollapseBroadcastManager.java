package net.cyberpunk042.infection.collapse;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.service.SingularityHudService.BorderSyncData;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Broadcast manager that respects {@link CollapseBroadcastMode} settings.
 * <ul>
 *   <li><b>IMMEDIATE</b> - Send updates immediately as they happen</li>
 *   <li><b>DELAYED</b> - Buffer updates and flush when players get close</li>
 *   <li><b>SUMMARY</b> - Same as DELAYED but logs what was buffered/flushed</li>
 * </ul>
 */
public final class BufferedCollapseBroadcastManager implements CollapseBroadcastManager {
	private final VirusWorldState state;
	private final Set<ChunkPos> bufferedChunks = new HashSet<>();
	private int ticksSinceLastFlush = 0;

	public BufferedCollapseBroadcastManager(VirusWorldState state) {
		this.state = state;
	}

	@Override
	public void tick(ServerWorld world) {
		CollapseBroadcastMode mode = state.collapseConfig().configuredCollapseBroadcastMode();
		
		if (mode == CollapseBroadcastMode.IMMEDIATE) {
			// Immediate mode: flush every tick
			if (!bufferedChunks.isEmpty()) {
				flushInternal(world, true, mode);
			}
		} else {
			// Delayed/Summary mode: periodic flush based on player proximity
			ticksSinceLastFlush++;
			if (ticksSinceLastFlush >= 20) { // Check every second
				ticksSinceLastFlush = 0;
				flushIfPlayersNearby(world, mode);
			}
		}
	}

	@Override
	public void flush(ServerWorld world, boolean force) {
		CollapseBroadcastMode mode = state.collapseConfig().configuredCollapseBroadcastMode();
		
		if (force || mode == CollapseBroadcastMode.IMMEDIATE) {
			flushInternal(world, force, mode);
		} else {
			// For delayed modes, only flush if players are nearby
			flushIfPlayersNearby(world, mode);
		}
	}

	/**
	 * Buffer a chunk for later broadcast (used in delayed modes).
	 */
	public void bufferChunk(ChunkPos pos) {
		if (pos != null) {
			bufferedChunks.add(pos);
		}
	}

	/**
	 * Get the count of buffered chunks.
	 */
	public int bufferedCount() {
		return bufferedChunks.size();
	}

	private void flushIfPlayersNearby(ServerWorld world, CollapseBroadcastMode mode) {
		if (bufferedChunks.isEmpty()) {
			return;
		}

		int broadcastRadius = state.collapseConfig().configuredCollapseBroadcastRadius();
		if (broadcastRadius <= 0) {
			// No radius restriction - flush all
			flushInternal(world, true, mode);
			return;
		}

		// Check if any player is within broadcast radius of buffered chunks
		Set<ChunkPos> toFlush = new HashSet<>();
		for (ChunkPos chunk : bufferedChunks) {
			int chunkCenterX = (chunk.x << 4) + 8;
			int chunkCenterZ = (chunk.z << 4) + 8;

			boolean playerNearby = world.getPlayers().stream().anyMatch(player -> {
				double dx = player.getX() - chunkCenterX;
				double dz = player.getZ() - chunkCenterZ;
				return (dx * dx + dz * dz) <= (broadcastRadius * broadcastRadius);
			});

			if (playerNearby) {
				toFlush.add(chunk);
			}
		}

		if (!toFlush.isEmpty()) {
			if (mode == CollapseBroadcastMode.SUMMARY) {
				Logging.SINGULARITY.info("[broadcast] %d chunks flushed (radius=%d), %d still buffered",
						toFlush.size(), broadcastRadius, bufferedChunks.size() - toFlush.size());
			}
			bufferedChunks.removeAll(toFlush);
			// Delegate actual chunk updates to phase service
			state.singularity().phase().flushBufferedChunks(false);
		}
	}

	private void flushInternal(ServerWorld world, boolean force, CollapseBroadcastMode mode) {
		if (mode == CollapseBroadcastMode.SUMMARY && !bufferedChunks.isEmpty()) {
			Logging.SINGULARITY.info("[broadcast] %d chunks flushed (force=%s)",
					bufferedChunks.size(), force);
		}
		bufferedChunks.clear();
		state.singularity().phase().flushBufferedChunks(force);
	}

	@Override
	public void deployBorder(ServerWorld world) {
		state.singularity().phase().deploySingularityBorder();
	}

	@Override
	public void syncBorder(ServerWorld world, BorderSyncData data) {
		if (data != null) {
			state.singularity().phase().syncSingularityBorder(data);
		} else {
			state.singularity().phase().syncSingularityBorder();
		}
	}
}
