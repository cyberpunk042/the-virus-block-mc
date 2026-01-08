package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.TierModule;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.service.SingularityBarrierService;
import net.cyberpunk042.infection.events.DissipationTickEvent;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Skeleton for the singularity lifecycle refactor. The full behavior will be
 * migrated from {@link VirusWorldState} in follow-up slices; for now this
 * service only centralizes the state container and exposes the minimal API
 * needed to keep wiring intact.
 */
public final class SingularityLifecycleService {

	// Constants migrated from VirusWorldState
	public static final int COLLAPSE_MIN_COLUMNS_PER_TICK = 16;
	public static final int COLLAPSE_MAX_COLUMNS_PER_TICK = 256;
	public static final int COLLAPSE_CHUNKS_PER_STEP = 1;
	public static final int PREGEN_LOG_INTERVAL = 40;
	public static final int PRELOAD_LOG_INTERVAL = 20;
	public static final int DEFAULT_DIAGNOSTIC_SAMPLE_INTERVAL = 20;
	public static final int SINGULARITY_COLLAPSE_MIN_LAYERS_PER_SLICE = 1;

	public static final class State {
		// fields migrated so far
		public SingularityState singularityState = SingularityState.DORMANT;
		public SingularityState singularityLastLoggedState = SingularityState.DORMANT;
		public long singularityTicks;
		public int singularityPhaseDelay;
		public int singularityCollapseRadius;
		public boolean singularityCollapseDescending;
		public int singularityCollapseTotalChunks;
		public int singularityCollapseCompletedChunks;
		public int singularityCollapseBarDelay;
		public int singularityCollapseCompleteHold;
		public int singularityLayersPerSlice;
		public int singularityLastColumnsPerTick;
		public int singularityLastLayersPerSlice;
		public int singularityDebugLogCooldown;
		public int singularityRemovalLogCooldown;
		public LongSet singularityPinnedChunks = new LongOpenHashSet();
		public LongSet collapseBufferedChunks = new LongOpenHashSet();
		public final Object2ObjectOpenHashMap<UUID, CollapseSyncProfile> singularityPlayerProfiles = new Object2ObjectOpenHashMap<>();
		// Fuse-related state (moved from VirusWorldState)
		public boolean shellCollapsed;
		public long fuseElapsed;
		public int fusePulseTicker;

		// Collapse tick/distance state (moved from VirusWorldState)
		public int collapseTickCooldown;
		public boolean distanceOverrideActive;
		public int viewDistanceSnapshot = -1;
		public int simulationDistanceSnapshot = -1;
		@Nullable
		public BlockPos center;

		// Ring state fields (stub for transition - ring system being removed)
		public int singularityRingTicks;
		public final DoubleArrayList singularityRingThresholds = new DoubleArrayList();
		public final IntArrayList singularityRingChunkCounts = new IntArrayList();
		public final IntArrayList singularityRingRadii = new IntArrayList();
		public List<List<ChunkPos>> singularityRingChunks = new ArrayList<>();
		public int singularityRingIndex;
		public int singularityRingPendingChunks;
		public int singularityRingActualCount;
		public long singularityTotalRingTicks;
		public long singularityRingTickAccumulator;
	}

	private final VirusWorldState host;
	private final State state;

	public SingularityLifecycleService(VirusWorldState host) {
		this(host, new State());
	}

	public SingularityLifecycleService(VirusWorldState host, State state) {
		this.host = Objects.requireNonNull(host, "host");
		this.state = state != null ? state : new State();
	}

	public State state() {
		return state;
	}

	/**
	 * Triggers the final vulnerability blast when the virus becomes vulnerable.
	 * This is infection logic - deactivates barrier as a side effect.
	 */
	public void triggerFinalBarrierBlast() {
		ServerWorld world = host.world();
		SingularityBarrierService barrier = host.singularity().barrier();
		if (barrier.isFinalBlastTriggered() || host.getVirusSources().isEmpty()) {
			return;
		}
		barrier.setFinalBlastTriggered(true);
		barrier.setNextPushTick(0L);
		for (BlockPos source : host.getVirusSources()) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			host.presentationCoord().pushPlayersFromBlock(source, TierModule.FINAL_VULNERABILITY_BLAST_RADIUS,
					TierModule.FINAL_VULNERABILITY_BLAST_SCALE, false);
			world.spawnParticles(
					ParticleTypes.SONIC_BOOM,
					source.getX() + 0.5D,
					source.getY() + 1.0D,
					source.getZ() + 0.5D,
					6,
					0.25D,
					0.25D,
					0.25D,
					0.0D);
			world.playSound(null, source, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0F, 0.65F);
		}
		barrier.deactivate();
	}

	public void tickCollapseBarDelay() {
		if (state.singularityCollapseBarDelay >= 0) {
			state.singularityCollapseBarDelay--;
		}
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
		Logging.SINGULARITY.topic("lifecycle").warn("[Singularity] chunk load failed phase={} chunk={} allowGen={} bypass={} outsideBorder={} distance={} thread={}",
				phase,
				chunk,
				generationAllowed,
				bypassing,
				outsideBorder,
				String.format("%.2f", distance),
				Thread.currentThread().getName(),
				error);
	}

	/**
	 * Called when a chunk is completed during collapse.
	 * Updates tracking counters and spawns visual effects.
	 */
	public void onChunkCompleted(ChunkPos chunk) {
		ServerWorld world = host.world();
		SingularityChunkContext.recordColumnCompleted();
		long packed = chunk.toLong();
		host.collapseModule().queues().chunkQueue().remove(packed);
		if (state.singularityCollapseCompletedChunks < state.singularityCollapseTotalChunks) {
			state.singularityCollapseCompletedChunks++;
		}
		updateCollapseRadiusFromChunk(packed);
		state.singularityCollapseDescending = !state.singularityCollapseDescending;
		host.collapseModule().execution().spawnChunkVeil(chunk);
		if (SingularityDiagnostics.enabled()) {
			Logging.SINGULARITY.info("chunk processed {}", chunk);
		}
	}

	public boolean processCollapseWorkload() {
		ServerWorld world = host.world();
		// CollapseProcessor handles all collapse work now
		SingularityChunkContext.push(world);
		try {
			return host.singularity().collapseProcessor().tick();
		} finally {
			SingularityChunkContext.pop(world);
		}
	}

	public boolean shouldStartPostReset() {
		return host.collapseConfig().postResetConfig().enabled && !host.collapseModule().queues().isResetQueueEmpty();
	}

	public void startPostCollapseReset() {
		ServerWorld world = host.world();
		if (!shouldStartPostReset()) {
			Logging.SINGULARITY.info("[reset] skipped enabled={} queue={}",
					host.collapseConfig().postResetConfig().enabled,
					host.collapseModule().queues().resetQueueSize());
			finishSingularity();
			return;
		}
		state.singularityState = SingularityState.RESET;
		host.collapseModule().queues().setResetDelay(Math.max(0, host.collapseModule().queues().postResetDelayTicks()));
		host.collapseModule().queues().clearResetProcessed();
		Logging.SINGULARITY.info("[reset] init queue={} delay={} chunksPerTick={} batchRadius={} tickDelay={}",
				host.collapseModule().queues().resetQueueSize(),
				host.collapseModule().queues().resetDelay(),
				host.collapseModule().queues().postResetChunksPerTick(),
				host.collapseModule().queues().postResetBatchRadius(),
				host.collapseModule().queues().postResetTickDelay());
		host.infection().broadcast(world, Text.literal("Singularity remnant reset initializing...").formatted(Formatting.LIGHT_PURPLE));
		host.markDirty();
	}

	public void processResetQueue(int perTick) {
		ServerWorld world = host.world();
		int processed = 0;
		while (processed < perTick && !host.collapseModule().queues().isResetQueueEmpty()) {
			Long chunkKey = host.collapseModule().queues().pollResetChunk();
			if (chunkKey == null) {
				continue;
			}
			resetChunkBatch(new ChunkPos(chunkKey.longValue()));
			processed++;
		}
		if (host.collapseModule().queues().isResetQueueEmpty()) {
			Logging.SINGULARITY.info("[reset] complete processed={} queue=0",
					host.collapseModule().queues().resetProcessedSize());
			finishSingularity();
		} else if (processed > 0) {
			if (SingularityDiagnostics.enabled()) {
				Logging.SINGULARITY.info("[reset] processed={} remaining={}",
						processed,
						host.collapseModule().queues().resetQueueSize());
			}
			host.markDirty();
		}
	}

	public void processSingularityReset() {
		ServerWorld world = host.world();
		if (!host.collapseConfig().postResetConfig().enabled) {
			finishSingularity();
			return;
		}
		if (host.collapseModule().queues().resetDelay() > 0) {
			host.collapseModule().queues().decrementResetDelay();
			host.markDirty();
			return;
		}
		host.collapseModule().queues().setResetDelay(Math.max(1, host.collapseModule().queues().postResetTickDelay()));
		int perTick = Math.max(1, host.collapseModule().queues().postResetChunksPerTick());
		processResetQueue(perTick);
		if (!host.collapseModule().queues().isResetQueueEmpty() && host.collapseModule().queues().resetProcessedSize() == 0) {
			host.collapseModule().queues().setResetDelay(Math.max(1, host.collapseModule().queues().postResetTickDelay()));
		}
	}

	public void resetChunkBatch(ChunkPos base) {
		ServerWorld world = host.world();
		int radius = host.collapseModule().queues().postResetBatchRadius();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				resetSingleChunk(world, new ChunkPos(base.x + dx, base.z + dz));
			}
		}
	}

	private void resetSingleChunk(ServerWorld world, ChunkPos pos) {
		long key = pos.toLong();
		if (!host.collapseModule().queues().markResetChunkProcessed(key)) {
			return;
		}
		MinecraftServer server = world.getServer();
		if (server == null) {
			return;
		}
		try {
			BlockPos origin = new BlockPos(pos.getStartX(), world.getBottomY(), pos.getStartZ());
			ServerCommandSource source = server.getCommandSource()
					.withWorld(world)
					.withPosition(Vec3d.ofCenter(origin))
					.withLevel(4)
					.withSilent();
			String command = String.format(Locale.ROOT, "chunk reset %d %d", pos.x, pos.z);
			server.getCommandManager().executeWithPrefix(source, command);
			if (SingularityDiagnostics.enabled()) {
				Logging.SINGULARITY.info("[reset] chunk {}", pos);
			}
		} catch (Exception ex) {
			Logging.SINGULARITY.topic("lifecycle").warn("[Singularity] chunk reset failed chunk={} reason={}", pos, ex.toString());
		}
	}

	public void finishSingularity() {
		ServerWorld world = host.world();
		host.singularity().fusing().clearFuseEntities(false);
		host.singularity().fusing().revertSingularityBlock(true);
		host.singularity().phase().flushBufferedChunks(true);
		state.collapseBufferedChunks.clear();
		host.singularity().phase().resetSingularityState();
		host.infection().broadcast(world, Text.translatable("message.the-virus-block.singularity_dissipated").formatted(Formatting.BLUE));
		host.sourceControl().forceContainmentReset();
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(
				Text.translatable("message.the-virus-block.cleansed").formatted(Formatting.AQUA), false));
		host.markDirty();
	}

	public boolean abortSingularity() {
		ServerWorld world = host.world();
		if (state.singularityState == SingularityState.DORMANT) {
			return false;
		}
		host.singularity().fusing().clearFuseEntities();
		host.singularity().fusing().revertSingularityBlock(false);
		host.singularity().phase().resetSingularityState();
		host.markDirty();
		host.infection().broadcast(world, Text.translatable("message.the-virus-block.singularity_aborted").formatted(Formatting.GRAY));
		return true;
	}

	public void logSingularityDebugInfo() {
		ServerWorld world = host.world();
		BlockPos center = host.singularityState().center;
		if (!SingularityDiagnostics.enabled() || center == null || state.singularityState != SingularityState.COLLAPSE) {
			return;
		}
		if (--state.singularityDebugLogCooldown > 0) {
			return;
		}
		state.singularityDebugLogCooldown = host.collapseModule().watchdog().diagnosticsSampleInterval();
		double borderRadius = host.collapseModule().execution().getCurrentCollapseRadius();
		double centerX = center.getX() + 0.5D;
		double centerZ = center.getZ() + 0.5D;
		List<SingularityTelemetryService.PlayerSample> samples = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator())) {
			Vec3d pos = player.getPos();
			samples.add(new SingularityTelemetryService.PlayerSample(
					player.getName().getString(),
					pos.x,
					pos.y,
					pos.z));
		}
		SingularityTelemetryService.CollapseSnapshot snapshot = new SingularityTelemetryService.CollapseSnapshot(
				centerX,
				centerZ,
				borderRadius,
				0, // ringIndex - legacy
				0, // ringPendingChunks - legacy
				0, // activeColumns - legacy
				state.singularityLayersPerSlice,
				samples);
		host.collapseModule().watchdog().telemetryService().logCollapseSnapshot(snapshot);
	}

	public void logSingularityStall() {
		ServerWorld world = host.world();
		if (state.singularityState != SingularityState.COLLAPSE) {
			return;
		}
		int queueSize = host.collapseModule().queues().chunkQueue().size();
		int stallTicks = host.collapseModule().watchdog().controller().recordCollapseStall(new SingularityWatchdogController.CollapseStallSample(
				0, // ringPendingChunks - legacy
				queueSize,
				false, // schedulerPending - legacy
				reason -> host.singularity().phase().skipSingularityCollapse(reason, host.orchestrator().services().collapseBroadcastManagerOrNoop())));
		SingularityTelemetryService.CollapseStallSnapshot snapshot = new SingularityTelemetryService.CollapseStallSnapshot(
				stallTicks,
				0, // ringPendingChunks - legacy
				0, // enginePending - legacy
				false, // schedulerPending - legacy
				host.singularity().chunkPreparationState().preloadComplete,
				queueSize,
				host.singularityState().collapseTickCooldown,
				20);
		host.collapseModule().watchdog().telemetryService().logCollapseStall(snapshot);
	}

	public void logRingActivation(int ringIndex, double borderRadius) {
		ServerWorld world = host.world();
		if (!SingularityDiagnostics.enabled()) {
			return;
		}
		if (ringIndex < 0 || ringIndex >= state.singularityRingThresholds.size()) {
			return;
		}
		double threshold = state.singularityRingThresholds.getDouble(ringIndex);
		int chunkCount = ringIndex < state.singularityRingChunkCounts.size() ? state.singularityRingChunkCounts.getInt(ringIndex) : 0;
		List<ChunkPos> chunks = ringIndex < state.singularityRingChunks.size() ? state.singularityRingChunks.get(ringIndex) : java.util.Collections.emptyList();
		double minDist = Double.POSITIVE_INFINITY;
		double maxDist = 0.0D;
		for (ChunkPos chunk : chunks) {
			double distance = host.collapseModule().execution().chunkDistanceFromCenter(chunk.toLong());
			minDist = Math.min(minDist, distance);
			maxDist = Math.max(maxDist, distance);
		}
		if (chunks.isEmpty()) {
			minDist = 0.0D;
			maxDist = 0.0D;
		}
		Logging.SINGULARITY.info("activating ring {} chunks={} threshold={} borderRadius={} chunkDist={}..{}",
				ringIndex,
				chunkCount,
				roundDistance(threshold),
				roundDistance(borderRadius),
				roundDistance(minDist),
				roundDistance(maxDist));
	}

	public void logSingularityStateChange(SingularityState next, String reason) {
		host.collapseModule().watchdog().controller().onStateTransition(next == SingularityState.FUSING, next == SingularityState.COLLAPSE);
		if (SingularityDiagnostics.enabled()
				&& !(state.singularityLastLoggedState == next && state.singularityState == next)) {
			host.collapseModule().watchdog().telemetryService().logStateChange(state.singularityState, next, reason);
		}
		state.singularityLastLoggedState = next;
	}

	public void tickFuseWatchdog() {
		ServerWorld world = host.world();
		host.collapseModule().watchdog().controller().tickFuseWatchdog(new SingularityWatchdogController.FuseWatchdogSample(
				state.singularityState == SingularityState.FUSING,
				host.singularity().chunkPreparationState().preloadComplete,
				state.singularityCollapseBarDelay,
				reason -> host.singularity().phase().skipSingularityCollapse(reason, host.orchestrator().services().collapseBroadcastManagerOrNoop())));
	}

	public void setLastLoggedState(SingularityState value) {
		state.singularityLastLoggedState = value;
	}

	private static double roundDistance(double value) {
		return Math.round(value * 100.0D) / 100.0D;
	}

	private void updateCollapseRadiusFromChunk(long chunkLong) {
		state.singularityCollapseRadius = (int) Math.max(0.0D, Math.round(host.collapseModule().execution().chunkDistanceFromCenter(chunkLong)));
	}
}

