package net.cyberpunk042.infection.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.profile.DimensionProfile.Collapse.PreCollapseWaterDrainage;
import net.cyberpunk042.infection.api.SimpleVirusScheduler;

/**
 * Encapsulates collapse chunk queues, reset queues, and pre-collapse drainage scheduling.
 * Queues are now managed directly here instead of delegating to destruction service.
 */
public final class CollapseQueueService {

	private final VirusWorldState host;
	private PreCollapseDrainageJob preCollapseDrainageJob;
	
	// Queue state (previously in SingularityDestructionService)
	private final Deque<Long> chunkQueue = new ArrayDeque<>();
	private final Deque<Long> resetQueue = new ArrayDeque<>();
	private final LongSet resetProcessed = new LongOpenHashSet();
	private int resetDelay = 0;

	public CollapseQueueService(VirusWorldState host) {
		this.host = host;
	}

	/**
	 * Returns the mutable chunk queue. Internal API - callers must not hold references.
	 */
	public Deque<Long> chunkQueue() {
		return chunkQueue;
	}

	public List<Long> chunkQueueSnapshot() {
		return new ArrayList<>(chunkQueue);
	}

	/**
	 * Returns the mutable reset queue. Internal API - callers must not hold references.
	 */
	public Deque<Long> resetQueue() {
		return resetQueue;
	}

	public List<Long> resetQueueSnapshot() {
		return new ArrayList<>(resetQueue);
	}

	public void clearResetProcessed() {
		resetProcessed.clear();
	}

	public int resetDelay() {
		return resetDelay;
	}

	public void setResetDelay(int ticks) {
		this.resetDelay = Math.max(0, ticks);
	}

	public void decrementResetDelay() {
		if (resetDelay > 0) {
			resetDelay--;
		}
	}

	public int resetProcessedSize() {
		return resetProcessed.size();
	}

	public boolean markResetChunkProcessed(long chunkLong) {
		return resetProcessed.add(chunkLong);
	}
	
	public LongSet resetProcessed() {
		return resetProcessed;
	}

	public void schedulePreCollapseDrainageJob() {
		PreCollapseWaterDrainage config = host.collapseConfig().preCollapseWaterDrainageConfig();
		if (config == null || !config.enabled()) {
			preCollapseDrainageJob = null;
			return;
		}
		if (chunkQueue().isEmpty()) {
			preCollapseDrainageJob = null;
			return;
		}
		List<Long> chunks = chunkQueueSnapshot();
		if (config.startFromCenter() && host.singularityState().center != null) {
			chunks.sort(Comparator.comparingDouble(host.collapseModule().execution()::chunkDistanceFromCenter));
		}
		preCollapseDrainageJob = new PreCollapseDrainageJob(config, new ArrayDeque<>(chunks));
	}

	public void clearPreCollapseDrainageJob() {
		preCollapseDrainageJob = null;
	}

	public List<SimpleVirusScheduler.TaskSnapshot> schedulerTaskSnapshot() {
		return host.orchestrator().services().schedulerService().snapshot();
	}

	public void applySchedulerSnapshot(List<SimpleVirusScheduler.TaskSnapshot> snapshots) {
		host.orchestrator().services().schedulerService().loadSnapshot(snapshots);
	}

	public int resetQueueSize() {
		return resetQueue().size();
	}

	public boolean isResetQueueEmpty() {
		return resetQueue().isEmpty();
	}

	public Long pollResetChunk() {
		return resetQueue().pollFirst();
	}

	public int postResetDelayTicks() {
		ServiceConfig.PostReset config = host.collapseConfig().postResetConfig();
		return Math.max(0, config.delayTicks);
	}

	public int postResetTickDelay() {
		return Math.max(1, host.collapseConfig().postResetConfig().tickDelay);
	}

	public int postResetChunksPerTick() {
		return Math.max(1, host.collapseConfig().postResetConfig().chunksPerTick);
	}

	public int postResetBatchRadius() {
		return Math.max(0, host.collapseConfig().postResetConfig().batchRadius);
	}

	public PreCollapseDrainageJob preCollapseDrainageJob() {
		return preCollapseDrainageJob;
	}

	public void setPreCollapseDrainageJob(PreCollapseDrainageJob job) {
		this.preCollapseDrainageJob = job;
	}

	public static final class PreCollapseDrainageJob {
		private final PreCollapseWaterDrainage config;
		private final Deque<Long> chunks;
		private int delayTicks;
		private int tickCooldown;

		public PreCollapseDrainageJob(PreCollapseWaterDrainage config, Deque<Long> chunks) {
			this.config = config;
			this.chunks = chunks;
			this.delayTicks = Math.max(0, config.startDelayTicks());
			this.tickCooldown = 0;
		}

		public PreCollapseWaterDrainage config() {
			return config;
		}

		public Deque<Long> chunks() {
			return chunks;
		}

		public int delayTicks() {
			return delayTicks;
		}

		public void setDelayTicks(int delayTicks) {
			this.delayTicks = Math.max(0, delayTicks);
		}

		public void decrementDelayTicks() {
			if (delayTicks > 0) {
				delayTicks--;
			}
		}

		public int tickCooldown() {
			return tickCooldown;
		}

		public void setTickCooldown(int tickCooldown) {
			this.tickCooldown = Math.max(0, tickCooldown);
		}

		public void decrementTickCooldown() {
			if (tickCooldown > 0) {
				tickCooldown--;
			}
		}
	}
}
