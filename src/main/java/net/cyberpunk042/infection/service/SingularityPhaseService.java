package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.Logging;
import java.util.Objects;

import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.List;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.service.CollapseQueueService.PreCollapseDrainageJob;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

/**
 * Owns the collapse-phase orchestration that used to live directly on
 * {@link VirusWorldState}. The service coordinates the lifecycle service,
 * border service, and the chunk queues required to run a collapse.
 */
public final class SingularityPhaseService {

	// Border diameter constants migrated from VirusWorldState
	public static final double BORDER_MIN_DIAMETER = 24.0D;
	public static final double BORDER_FINAL_DIAMETER = 1.0D;

	private final VirusWorldState host;
	private final SingularityLifecycleService lifecycle;

	public SingularityPhaseService(VirusWorldState host, SingularityLifecycleService lifecycle) {
		this.host = Objects.requireNonNull(host, "host");
		this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
	}

	private SingularityLifecycleService.State singularity() {
		return lifecycle.state();
	}

	public void handleFuseCountdownComplete(SingularityContext ctx) {
		ServerWorld world = ctx.world();
		clearPreCollapseDrainageJob();
		host.singularity().fusing().detonateFuseCore();
		if (!host.collapseConfig().configuredCollapseEnabled(world)) {
			host.singularity().phase().skipSingularityCollapse("disabled before activation", ctx.broadcastManager());
			return;
		}
		lifecycle.logSingularityStateChange(SingularityState.COLLAPSE,
				"fuseComplete elapsed=" + host.singularityState().fuseElapsed
						+ " allowCollapse=" + host.collapseConfig().configuredCollapseEnabled(world));
		SingularityLifecycleService.State state = singularity();
		state.singularityState = SingularityState.COLLAPSE;
		state.singularityDebugLogCooldown = 0;
		state.singularityCollapseRadius = host.collapse().computeInitialCollapseRadius(world);
		state.singularityCollapseDescending = true;

		// Start CollapseProcessor (radius-based fill)
		startCollapseProcessor(world);
		state.singularityCollapseBarDelay = host.collapseConfig().configuredCollapseBarDelay();
		state.singularityCollapseCompleteHold = host.collapseConfig().configuredCollapseCompleteHold();
		host.singularityState().collapseTickCooldown = 0;
		host.singularity().borderState().elapsed = 0L;
		host.singularity().borderState().pendingDeployment = true;
		if (host.singularityState().center == null) {
			BlockPos fallback = host.infection().representativePos(host.world(), world.getRandom(), host.sourceState());
			if (fallback == null && host.hasVirusSources()) {
				fallback = host.getVirusSources().iterator().next();
			}
			if (fallback != null) {
				host.singularityState().center = fallback != null ? fallback.toImmutable() : null;
			}
		}
		host.singularity().fusing().activateSingularityBlock();
		host.infection().broadcast(world,
				Text.translatable("message.the-virus-block.singularity_collapse").formatted(Formatting.DARK_PURPLE));
		host.markDirty();
	}

	public void prepareSingularityChunkQueue(SingularityContext ctx) {
		ServerWorld world = ctx.world();
		host.collapseModule().queues().chunkQueue().clear();
		SingularityLifecycleService.State state = singularity();
		state.singularityCollapseTotalChunks = 0;
		state.singularityCollapseCompletedChunks = 0;
		state.singularityRingThresholds.clear();
		state.singularityRingChunkCounts.clear();
		state.singularityRingRadii.clear();
		state.singularityRingIndex = -1;
		state.singularityRingPendingChunks = 0;
		host.singularity().borderState().initialBorderDiameter = 0.0D;
		host.singularity().borderState().finalBorderDiameter = 0.0D;
		state.singularityTotalRingTicks = 0L;
		state.singularityRingTickAccumulator = 0L;
		host.singularity().ensureCenter(world);
		if (host.singularityState().center == null) {
			Logging.SINGULARITY.topic("phase").warn("[Singularity] Unable to prepare collapse queue because center is null");
			return;
		}
		// Ring planner removed - CollapseProcessor manages collapse state
		host.presentationCoord().broadcastCollapseSchedule();
	}

	public void deploySingularityBorder() {
		ServerWorld world = host.world();
		if (host.singularityState().center == null) {
			Logging.SINGULARITY.topic("phase").warn("[SingularityPhase] Cannot deploy border: center is null");
			return;
		}
		double centerX = host.singularityState().center.getX() + 0.5D;
		double centerZ = host.singularityState().center.getZ() + 0.5D;
		double outerRadius = host.collapseConfig().configuredBarrierStartRadius();
		double finalRadius = host.collapseConfig().configuredBarrierEndRadius();
		double initialDiameter = Math.max(BORDER_MIN_DIAMETER, outerRadius * 2.0D);
		double finalDiameter = Math.max(BORDER_FINAL_DIAMETER, finalRadius * 2.0D);
		int ringStages = Math.max(1, singularity().singularityRingActualCount);
		singularity().singularityTotalRingTicks = (long) ringStages * host.collapseConfig().configuredCollapseTickInterval();
		long durationTicks = host.collapseConfig().configuredBorderDurationTicks();
		long durationMillis = Math.max(50L, durationTicks * 50L);
		Logging.SINGULARITY.topic("border")
				.kv("center", String.format("%.2f,%.2f", centerX, centerZ))
				.kv("outerRadius", String.format("%.2f", outerRadius))
				.kv("finalRadius", String.format("%.2f", finalRadius))
				.kv("initialDiameter", String.format("%.2f", initialDiameter))
				.kv("finalDiameter", String.format("%.2f", finalDiameter))
				.kv("durationTicks", durationTicks)
				.info("Deploying border");
		host.singularity().border().deploy(host.singularity().borderState(), world, centerX, centerZ, initialDiameter, finalDiameter,
				durationTicks);
		singularity().singularityRingTickAccumulator = 0L;
		singularity().singularityRingIndex = -1;
		singularity().singularityRingPendingChunks = 0;
		host.markDirty();
		Logging.SINGULARITY.info("Border initialized size={} (should equal initialDiameter)",
				String.format("%.2f", world.getWorldBorder().getSize()));
		SingularityHudService.BorderSyncData data = host.collapse().createBorderSyncData(initialDiameter, 0L, finalDiameter);
		host.singularity().phase().syncSingularityBorder(data);
		world.getWorldBorder().interpolateSize(initialDiameter, finalDiameter, durationMillis);
	}

	public void restoreSingularityBorder() {
		ServerWorld world = host.world();
		if (!host.singularity().borderState().active && !host.singularity().borderState().hasSnapshot) {
			return;
		}
		host.singularity().border().restore(host.singularity().borderState(), world);
		host.markDirty();
		host.singularity().phase().syncSingularityBorder();
	}

	public void tickPreCollapseDrainage() {
		ServerWorld world = host.world();
		PreCollapseDrainageJob job = host.collapseModule().queues().preCollapseDrainageJob();
		if (job == null) {
			return;
		}
		if (singularity().singularityState != SingularityState.FUSING) {
			host.collapseModule().queues().setPreCollapseDrainageJob(null);
			return;
		}
		// Wait for preGen to complete before starting water drainage
		if (!host.singularity().chunkPreparationState().preGenComplete) {
			return;
		}
		if (job.delayTicks() > 0) {
			job.decrementDelayTicks();
			return;
		}
		if (job.tickCooldown() > 0) {
			job.decrementTickCooldown();
			return;
		}
		
		int maxOps = Math.max(1, job.config().maxOperationsPerTick());
		int thickness = Math.max(1, job.config().thickness());
		int batch = Math.max(1, job.config().batchSize());
		int opsRemaining = maxOps;
		
		while (batch-- > 0 && !job.chunks().isEmpty() && opsRemaining > 0) {
			Long packed = job.chunks().pollFirst();
			if (packed == null) {
				continue;
			}
			int opsBefore = host.collapseModule().execution().getOperationsThisTick();
			host.collapseModule().execution().drainChunkByMode(new ChunkPos(packed), job.config().mode(), thickness, maxOps);
			int opsUsed = host.collapseModule().execution().getOperationsThisTick() - opsBefore;
			opsRemaining -= opsUsed;
		}
		
		host.collapseModule().execution().resetOperationsThisTick();
		
		if (job.chunks().isEmpty()) {
			host.collapseModule().queues().setPreCollapseDrainageJob(null);
		} else {
			job.setTickCooldown(Math.max(1, job.config().tickRate()) - 1);
		}
	}

	public void clearPreCollapseDrainageJob() {
		host.collapseModule().queues().setPreCollapseDrainageJob(null);
	}

	public void pinSingularityChunk(ChunkPos pos) {
		ServerWorld world = host.world();
		long packed = pos.toLong();
		if (singularity().singularityPinnedChunks.add(packed)) {
			try {
				world.setChunkForced(pos.x, pos.z, true);
				if (SingularityDiagnostics.enabled()) {
					Logging.SINGULARITY.info("pinned chunk {}", pos);
				}
			} catch (IllegalStateException ex) {
				singularity().singularityPinnedChunks.remove(packed);
				if (SingularityDiagnostics.enabled()) {
					Logging.SINGULARITY.topic("phase").warn("[Singularity] failed to pin chunk {} ({})", pos, ex.getMessage());
				}
			}
		}
	}

	public void releaseSingularityChunkTickets() {
		ServerWorld world = host.world();
		if (singularity().singularityPinnedChunks.isEmpty()) {
			return;
		}
		int released = singularity().singularityPinnedChunks.size();
		for (long packed : singularity().singularityPinnedChunks.toLongArray()) {
			ChunkPos pos = new ChunkPos(packed);
			try {
				world.setChunkForced(pos.x, pos.z, false);
			} catch (IllegalStateException ex) {
				if (SingularityDiagnostics.enabled()) {
					Logging.SINGULARITY.topic("phase").warn("[Singularity] failed to unpin chunk {} ({})", pos, ex.getMessage());
				}
			}
		}
		singularity().singularityPinnedChunks.clear();
		Logging.SINGULARITY.info("[unload] released {} forced chunks", released);
	}

	public boolean areAllCollapseChunksPinned(List<Long> queueSnapshot) {
		if (queueSnapshot.isEmpty()) {
			return true;
		}
		if (singularity().singularityPinnedChunks.isEmpty()
				|| singularity().singularityPinnedChunks.size() < queueSnapshot.size()) {
			return false;
		}
		for (Long packed : queueSnapshot) {
			if (packed == null) {
				continue;
			}
			if (!singularity().singularityPinnedChunks.contains(packed.longValue())) {
				return false;
			}
		}
		return true;
	}

	public void tickSingularityBorder() {
		ServerWorld world = host.world();
		if (!host.singularity().borderState().active) {
			SingularityChunkContext.disableBorderGuard(world);
			host.singularity().borderState().resetCountdown = -1;
			return;
		}
		SingularityChunkContext.enableBorderGuard(world);
		host.singularity().borderState().lastDiameter = world.getWorldBorder().getSize();
		if (host.singularity().borderState().duration > 0L) {
			host.singularity().borderState().elapsed = Math.min(host.singularity().borderState().duration, host.singularity().borderState().elapsed + 1);
		}
		maybeAutoResetBorder(world);
	}

	private void maybeAutoResetBorder(ServerWorld world) {
		boolean collapseFinished = (singularity().singularityState != SingularityState.COLLAPSE
				&& singularity().singularityState != SingularityState.RESET)
				|| host.collapseModule().queues().chunkQueue().isEmpty();
		boolean shrinkFinished = host.singularity().borderState().duration <= 0L
				|| host.singularity().borderState().elapsed >= host.singularity().borderState().duration;
		boolean restored = host.singularity().border().maybeAutoReset(host.singularity().borderState(),
				world,
				host.collapseConfig().barrierAutoResetEnabled(),
				host.collapseConfig().barrierResetDelayTicks(),
				collapseFinished,
				shrinkFinished);
		if (restored) {
			host.markDirty();
			syncSingularityBorder();
		}
	}

	public void syncSingularityBorder() {
		ServerWorld world = host.world();
		double reportedDiameter = world != null ? world.getWorldBorder().getSize() : host.singularity().borderState().lastDiameter;
		SingularityHudService.BorderSyncData data = host.collapse().createBorderSyncData(
				reportedDiameter,
				host.singularity().borderState().elapsed,
				host.singularity().borderState().targetDiameter);
		if (data != null) {
			host.collapseModule().watchdog().presentationService().syncBorder(host.world(), data);
		}
	}

	public void syncSingularityBorder(SingularityHudService.BorderSyncData data) {
		ServerWorld world = host.world();
		if (data == null) {
			syncSingularityBorder();
			return;
		}
		host.collapseModule().watchdog().presentationService().syncBorder(host.world(), data);
	}

	public void clearSingularityBorderState() {
		host.singularity().border().clear(host.singularity().borderState());
		SingularityLifecycleService.State state = singularity();
		state.singularityRingThresholds.clear();
		state.singularityRingChunkCounts.clear();
		state.singularityRingChunks.clear();
		state.singularityRingIndex = -1;
		state.singularityRingPendingChunks = 0;
		host.singularity().borderState().initialBorderDiameter = 0.0D;
		host.singularity().borderState().finalBorderDiameter = 0.0D;
		state.singularityTotalRingTicks = 0L;
		state.singularityRingTickAccumulator = 0L;
		host.singularity().borderState().outerRadius = 0.0D;
		host.singularity().borderState().innerRadius = 0.0D;
		state.singularityRingActualCount = 0;
		host.singularity().borderState().resetCountdown = -1;
	}

	public void applyCollapseDistanceOverrides() {
		ServerWorld world = host.world();
		int targetView = host.collapseConfig().configuredCollapseViewDistanceChunks();
		int targetSim = host.collapseConfig().configuredCollapseSimulationDistanceChunks();
		if (targetView <= 0 && targetSim <= 0) {
			return;
		}
		PlayerManager manager = world.getServer().getPlayerManager();
		boolean changed = false;
		if (!host.singularityState().distanceOverrideActive) {
			host.singularityState().viewDistanceSnapshot = manager.getViewDistance();
			host.singularityState().simulationDistanceSnapshot = manager.getSimulationDistance();
		}
		if (targetView > 0 && manager.getViewDistance() != targetView) {
			manager.setViewDistance(targetView);
			changed = true;
		}
		if (targetSim > 0 && manager.getSimulationDistance() != targetSim) {
			manager.setSimulationDistance(targetSim);
			changed = true;
		}
		if (changed) {
			host.singularityState().distanceOverrideActive = true;
			if (SingularityDiagnostics.enabled()) {
				Logging.SINGULARITY.info("[vanillaSync] viewDistance={} (orig={}) simulationDistance={} (orig={})",
						manager.getViewDistance(),
						host.singularityState().viewDistanceSnapshot,
						manager.getSimulationDistance(),
						host.singularityState().simulationDistanceSnapshot);
			}
		} else if (!host.singularityState().distanceOverrideActive) {
			host.singularityState().viewDistanceSnapshot = -1;
			host.singularityState().simulationDistanceSnapshot = -1;
		}
	}

	public void restoreCollapseDistanceOverrides(MinecraftServer server) {
		if (!host.singularityState().distanceOverrideActive) {
			return;
		}
		PlayerManager manager = server.getPlayerManager();
		if (host.singularityState().viewDistanceSnapshot >= 0
				&& manager.getViewDistance() != host.singularityState().viewDistanceSnapshot) {
			manager.setViewDistance(host.singularityState().viewDistanceSnapshot);
		}
		if (host.singularityState().simulationDistanceSnapshot >= 0
				&& manager.getSimulationDistance() != host.singularityState().simulationDistanceSnapshot) {
			manager.setSimulationDistance(host.singularityState().simulationDistanceSnapshot);
		}
		if (SingularityDiagnostics.enabled()) {
			Logging.SINGULARITY.info("[vanillaSync] restored viewDistance={} simulationDistance={}",
					manager.getViewDistance(),
					manager.getSimulationDistance());
		}
		host.singularityState().distanceOverrideActive = false;
		host.singularityState().viewDistanceSnapshot = -1;
		host.singularityState().simulationDistanceSnapshot = -1;
	}

	public void skipSingularityCollapse(String reason,
			CollapseBroadcastManager broadcastManager) {
		ServerWorld world = host.world();
		lifecycle.logSingularityStateChange(SingularityState.DISSIPATION, reason);
		SingularityLifecycleService.State state = singularity();
		state.singularityState = SingularityState.DISSIPATION;
		state.singularityRingTicks = 0;
		state.singularityRingPendingChunks = 0;
		state.singularityRingIndex = -1;
		host.singularityState().collapseTickCooldown = 0;
		state.singularityCollapseBarDelay = 0;
		state.singularityCollapseCompleteHold = 0;
		state.singularityCollapseRadius = 0;
		host.collapseModule().queues().chunkQueue().clear();
		clearPreCollapseDrainageJob();
		host.singularity().borderState().pendingDeployment = false;
		host.singularity().borderState().duration = 0L;
		host.singularity().borderState().elapsed = 0L;
		host.singularity().phase().releaseSingularityChunkTickets();
		restoreSingularityBorder();
		state.singularityPhaseDelay = host.collapseConfig().configuredResetDelayTicks();
		state.singularityRingTickAccumulator = 0L;
		state.singularityRingActualCount = 0;
		state.singularityRingThresholds.clear();
		state.singularityRingChunkCounts.clear();
		state.singularityRingChunks.clear();
		state.singularityRingRadii.clear();
		// Destruction service removed - CollapseProcessor manages state
		SingularityHudService.BorderSyncData resetData = host.collapse().createBorderSyncData(world.getWorldBorder().getSize(),
				0L,
				world.getWorldBorder().getSize());
		broadcastManager.syncBorder(world, resetData);
		SingularityHudService.BorderSyncData currentData = host.collapse().createBorderSyncData(world.getWorldBorder().getSize(),
				host.singularity().borderState().elapsed,
				host.singularity().borderState().targetDiameter);
		broadcastManager.syncBorder(world, currentData);
		if (SingularityDiagnostics.enabled()) {
			Logging.SINGULARITY.info("[skipCollapse] {}", reason);
		}
		broadcastManager.flush(world, true);
		state.collapseBufferedChunks.clear();
		host.infection().broadcast(world, Text.translatable("message.the-virus-block.singularity_detonated").formatted(Formatting.DARK_PURPLE));
		host.markDirty();
	}

	public void resetSingularityPreparation(String reason) {
		host.collapseModule().queues().chunkQueue().clear();
		SingularityLifecycleService.State state = singularity();
		ChunkPreparationService.State prepState = host.singularity().chunkPreparationState();
		prepState.preGenQueue.clear();
		prepState.preloadQueue.clear();
		state.singularityPinnedChunks.clear();
		prepState.preGenMissingChunks = 0;
		prepState.preloadMissingChunks = 0;
		prepState.preGenComplete = false;
		prepState.preloadComplete = false;
		prepState.preGenLogCooldown = 0;
		prepState.preloadLogCooldown = 0;
		host.collapseModule().queues().resetQueue().clear();
		host.collapseModule().queues().clearResetProcessed();
		host.collapseModule().queues().setResetDelay(0);
		// Destruction service removed - CollapseProcessor manages collapse state
		if (SingularityDiagnostics.enabled()) {
			Logging.SINGULARITY.info("[prep] reset collapse staging reason={}", reason);
		}
	}

	public void flushBufferedChunks(boolean force) {
		ServerWorld world = host.world();
		if (singularity().collapseBufferedChunks.isEmpty()) {
			return;
		}
		CollapseBroadcastMode mode = host.collapseConfig().configuredCollapseBroadcastMode();
		int radius = host.collapseConfig().configuredCollapseBroadcastRadius();
		LongIterator iterator = singularity().collapseBufferedChunks.iterator();
		while (iterator.hasNext()) {
			long packed = iterator.nextLong();
			ChunkPos chunk = new ChunkPos(packed);
			boolean shouldFlush = force
					|| mode == CollapseBroadcastMode.IMMEDIATE
					|| radius <= 0
					|| host.presentationCoord().isChunkWithinBroadcastRadius(chunk, radius);
			if (!shouldFlush) {
				continue;
			}
			BlockPos pos = new BlockPos(chunk.getCenterX(), world.getBottomY(), chunk.getCenterZ());
			world.getChunkManager().markForUpdate(pos);
			SingularityChunkContext.recordBroadcastFlushed(chunk);
			iterator.remove();
		}
	}

	public void resetSingularityState() {
		ServerWorld world = host.world();
		releaseSingularityChunkTickets();
		restoreCollapseDistanceOverrides(world.getServer());
		restoreSingularityBorder();
		clearSingularityState();
	}

	public void clearSingularityState() {
		SingularityLifecycleService.State state = singularity();
		state.singularityState = SingularityState.DORMANT;
		lifecycle.setLastLoggedState(SingularityState.DORMANT);
		host.collapseModule().watchdog().controller().resetAll();
		state.singularityTicks = 0L;
		host.singularityState().center = null;
		host.singularityState().shellCollapsed = false;
		state.singularityCollapseRadius = 0;
		state.singularityCollapseDescending = true;
		state.singularityRingTicks = 0;
		state.singularityPhaseDelay = 0;
		host.singularityState().fusePulseTicker = 0;
		host.singularityState().fuseElapsed = 0L;
		host.collapseModule().queues().chunkQueue().clear();
		host.collapseModule().queues().setPreCollapseDrainageJob(null);
		host.collapseModule().queues().resetQueue().clear();
		host.collapseModule().queues().clearResetProcessed();
		host.collapseModule().queues().setResetDelay(0);
		host.singularityState().collapseTickCooldown = 0;
		state.singularityCollapseTotalChunks = 0;
		state.singularityCollapseCompletedChunks = 0;
		state.singularityCollapseBarDelay = 0;
		state.singularityCollapseCompleteHold = 0;
		// Destruction/scheduler removed - CollapseProcessor manages state
		ChunkPreparationService.State prepState = host.singularity().chunkPreparationState();
		prepState.preloadQueue.clear();
		prepState.preloadComplete = false;
		prepState.preloadMissingChunks = 0;
		state.singularityDebugLogCooldown = host.collapseModule().watchdog().diagnosticsSampleInterval();
		host.singularity().fusing().clearFuseClearedBlocks();
		state.collapseBufferedChunks.clear();
		host.sources().clearSuppressed(host.sourceState());
		clearSingularityBorderState();
		// state.singularityRingRadii.clear(); // Ring system removed
	}

	/**
	 * Starts the new simplified collapse processor with radius-based fill.
	 * Collapse spreads the rings across the configured duration.
	 */
	private void startCollapseProcessor(ServerWorld world) {
		BlockPos center = host.singularityState().center;
		if (center == null) {
			return;
		}

		double configuredRadius = host.collapseConfig().configuredCollapseMaxRadius();
		
		// Use a reasonable radius - clamp to prevent absurd values
		double startRadius = Math.min(configuredRadius, 200.0); // Max 200 blocks for sanity
		double endRadius = 0; // Collapse inward to center
		
		// Duration: 60 seconds = 1200 ticks
		// This spreads the rings across the duration (e.g., 120 rings over 1200 ticks = 1 ring per 10 ticks)
		long durationTicks = 1200L;
		
		boolean inward = singularity().singularityCollapseDescending;
		
		Logging.SINGULARITY.topic("phase").info("[SingularityPhase] Starting collapse: radius={} blocks, duration={} ticks ({} seconds)", 
				(int) startRadius, durationTicks, durationTicks / 20);

		host.singularity().collapseProcessor().start(center, startRadius, endRadius, durationTicks, inward);
	}

}
