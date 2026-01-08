package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.SingularityState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Handles singularity/border snapshot serialization for {@link VirusWorldState}.
 */
public final class CollapseSnapshotService {

	private final VirusWorldState host;

	public CollapseSnapshotService(VirusWorldState host) {
		this.host = host;
	}

	public Optional<SingularitySnapshot> buildSingularitySnapshot() {
		return Optional.of(new SingularitySnapshot(
				host.singularityState().singularityState.name(),
				host.singularityState().singularityTicks,
				host.singularityState().center,
				host.singularityState().shellCollapsed,
				host.singularityState().singularityCollapseRadius,
				host.singularityState().singularityCollapseDescending,
				0,
				host.singularityState().singularityPhaseDelay,
				host.singularityState().fusePulseTicker,
				host.singularityState().fuseElapsed,
				List.copyOf(host.collapseModule().queues().chunkQueue()),
				host.singularityState().singularityCollapseTotalChunks,
				host.singularityState().singularityCollapseCompletedChunks,
				new CollapseTimingSnapshot(host.singularityState().singularityCollapseBarDelay,
						host.singularityState().singularityCollapseCompleteHold),
				new ResetSnapshot(List.copyOf(host.collapseModule().queues().resetQueue()), host.collapseModule().queues().resetDelay()),
				buildBorderSnapshot().orElse(null)));
	}

	public Optional<SingularityBorderSnapshot> buildBorderSnapshot() {
		SingularityBorderService.State border = host.singularity().borderState();
		if (!border.active && !border.hasSnapshot) {
			return Optional.empty();
		}
		SingularityBorderOriginalSnapshot original = null;
		if (border.hasSnapshot) {
			original = new SingularityBorderOriginalSnapshot(border.originalCenterX,
					border.originalCenterZ,
					border.originalDiameter,
					border.originalSafeZone,
					border.originalDamagePerBlock,
					border.originalWarningBlocks,
					border.originalWarningTime);
		}
		SingularityBorderTimelineSnapshot timeline = new SingularityBorderTimelineSnapshot(
				java.util.Collections.emptyList(),
				java.util.Collections.emptyList(),
				java.util.Collections.emptyList(),
				border.initialBorderDiameter,
				border.targetDiameter,
				0L,
				0L,
				0,
				0,
				border.outerRadius,
				border.innerRadius,
				0);
		return Optional.of(new SingularityBorderSnapshot(border.active,
				border.centerX,
				border.centerZ,
				border.initialDiameter,
				border.targetDiameter,
				border.duration,
				border.elapsed,
				border.lastDiameter,
				original,
				timeline));
	}

	public void applySingularitySnapshot(SingularitySnapshot snapshot) {
		try {
			host.singularityState().singularityState = SingularityState.valueOf(snapshot.state());
		} catch (IllegalArgumentException ex) {
			host.singularityState().singularityState = SingularityState.DORMANT;
		}
		host.singularity().lifecycle().setLastLoggedState(host.singularityState().singularityState);
		host.singularityState().singularityTicks = Math.max(0L, snapshot.ticks());
		host.singularityState().center = snapshot.centerPos() != null ? snapshot.centerPos().toImmutable() : null;
		host.singularityState().shellCollapsed = snapshot.shellCollapsed();
		host.singularityState().singularityCollapseRadius = Math.max(0, snapshot.collapseRadius());
		host.singularityState().singularityCollapseDescending = snapshot.collapseDescending();
		host.singularityState().singularityRingTicks = snapshot.ringTicks();
		host.singularityState().singularityPhaseDelay = snapshot.phaseDelay();
		host.singularityState().fusePulseTicker = MathHelper.clamp(snapshot.fuseTicker(), 0, host.collapseConfig().configuredFusePulseInterval());
		host.singularityState().fuseElapsed = Math.max(0L, snapshot.fuseElapsed());
		host.collapseModule().queues().chunkQueue().clear();
		if (snapshot.chunkQueue() != null) {
			snapshot.chunkQueue().forEach(packed -> {
				if (packed != null) {
					host.collapseModule().queues().chunkQueue().addLast(packed.longValue());
				}
			});
		}
		host.singularityState().singularityCollapseTotalChunks = Math.max(0, snapshot.collapseTotal());
		host.singularityState().singularityCollapseCompletedChunks = MathHelper.clamp(snapshot.collapseCompleted(),
				0,
				host.singularityState().singularityCollapseTotalChunks);
		CollapseTimingSnapshot timings = snapshot.timings();
		if (timings != null) {
			host.singularityState().singularityCollapseBarDelay = timings.delay();
			host.singularityState().singularityCollapseCompleteHold = Math.max(0, timings.hold());
		} else {
			host.singularityState().singularityCollapseBarDelay = 0;
			host.singularityState().singularityCollapseCompleteHold = 0;
		}
		ResetSnapshot reset = snapshot.reset();
		host.collapseModule().queues().resetQueue().clear();
		if (reset != null && reset.queue() != null) {
			reset.queue().forEach(packed -> {
				if (packed != null) {
					host.collapseModule().queues().resetQueue().addLast(packed.longValue());
				}
			});
			host.collapseModule().queues().setResetDelay(Math.max(0, reset.delay()));
		} else {
			host.collapseModule().queues().setResetDelay(0);
		}
		host.collapseModule().queues().clearResetProcessed();
		applySingularityBorderSnapshot(snapshot.border());
		// Destruction service removed - CollapseProcessor manages collapse state
	}

	public void applySingularityBorderSnapshot(@Nullable SingularityBorderSnapshot snapshot) {
		SingularityBorderService.State border = host.singularity().borderState();
		if (snapshot == null) {
			host.singularity().phase().clearSingularityBorderState();
			return;
		}
		border.active = snapshot.active();
		border.centerX = snapshot.centerX();
		border.centerZ = snapshot.centerZ();
		border.initialDiameter = snapshot.initialDiameter();
		border.targetDiameter = snapshot.targetDiameter();
		border.duration = snapshot.duration();
		border.elapsed = snapshot.elapsed();
		border.lastDiameter = snapshot.lastDiameter();
		SingularityBorderOriginalSnapshot original = snapshot.original();
		border.hasSnapshot = original != null;
		if (original != null) {
			border.originalCenterX = original.centerX();
			border.originalCenterZ = original.centerZ();
			border.originalDiameter = original.diameter();
			border.originalSafeZone = original.safeZone();
			border.originalDamagePerBlock = original.damagePerBlock();
			border.originalWarningBlocks = original.warningBlocks();
			border.originalWarningTime = original.warningTime();
		} else {
			border.originalCenterX = 0.0D;
			border.originalCenterZ = 0.0D;
			border.originalDiameter = 0.0D;
			border.originalSafeZone = 0.0D;
			border.originalDamagePerBlock = 0.0D;
			border.originalWarningBlocks = 0;
			border.originalWarningTime = 0;
		}
		SingularityBorderTimelineSnapshot timeline = snapshot.timeline();
		host.singularityState().singularityRingThresholds.clear();
		host.singularityState().singularityRingChunkCounts.clear();
		host.singularityState().singularityRingRadii.clear();
		if (timeline != null) {
			timeline.thresholds().stream().filter(val -> val != null).forEach(val -> host.singularityState().singularityRingThresholds.add(val.doubleValue()));
			timeline.ringCounts().stream().filter(val -> val != null).forEach(val -> host.singularityState().singularityRingChunkCounts.add(Math.max(0, val.intValue())));
			timeline.ringRadii().stream().filter(val -> val != null).forEach(val -> host.singularityState().singularityRingRadii.add(Math.max(0, val.intValue())));
			border.initialBorderDiameter = timeline.initialDiameter();
			border.finalBorderDiameter = timeline.finalDiameter();
			host.singularityState().singularityTotalRingTicks = Math.max(0L, timeline.totalTicks());
			host.singularityState().singularityRingTickAccumulator = MathHelper.clamp(timeline.elapsedTicks(),
					0L,
					0L);
			host.singularityState().singularityRingIndex = timeline.ringIndex();
			host.singularityState().singularityRingPendingChunks = Math.max(0, timeline.pendingChunks());
			border.outerRadius = Math.max(0.0D, timeline.outerRadius());
			border.innerRadius = Math.max(0.0D, timeline.innerRadius());
			host.singularityState().singularityRingActualCount = Math.max(1, timeline.ringCount());
			border.pendingDeployment = false;
		} else {
			border.initialBorderDiameter = border.initialDiameter;
			border.finalBorderDiameter = border.targetDiameter;
			host.singularityState().singularityTotalRingTicks = 0L;
			host.singularityState().singularityTotalRingTicks = 0L;
			host.singularityState().singularityRingIndex = -1;
			host.singularityState().singularityRingPendingChunks = 0;
			border.outerRadius = 0.0D;
			border.innerRadius = 0.0D;
			host.singularityState().singularityRingPendingChunks = 0;
			border.pendingDeployment = false;
		}
	}

	private static List<Double> copy(List<Double> source) {
		return new ArrayList<>(source);
	}

	private static List<Integer> copyInt(List<Integer> source) {
		return new ArrayList<>(source);
	}

	public record SingularitySnapshot(
			String state,
			long ticks,
			@Nullable BlockPos centerPos,
			boolean shellCollapsed,
			int collapseRadius,
			boolean collapseDescending,
			int ringTicks,
			int phaseDelay,
			int fuseTicker,
			long fuseElapsed,
			List<Long> chunkQueue,
			int collapseTotal,
			int collapseCompleted,
			@Nullable CollapseTimingSnapshot timings,
			@Nullable ResetSnapshot reset,
			@Nullable SingularityBorderSnapshot border) {
		public static final Codec<SingularitySnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.STRING.optionalFieldOf("state", SingularityState.DORMANT.name()).forGetter(SingularitySnapshot::state),
				Codec.LONG.optionalFieldOf("ticks", 0L).forGetter(SingularitySnapshot::ticks),
				BlockPos.CODEC.optionalFieldOf("center").forGetter(snapshot -> Optional.ofNullable(snapshot.centerPos())),
				Codec.BOOL.optionalFieldOf("shellCollapsed", false).forGetter(SingularitySnapshot::shellCollapsed),
				Codec.INT.optionalFieldOf("collapseRadius", 0).forGetter(SingularitySnapshot::collapseRadius),
				Codec.BOOL.optionalFieldOf("collapseDescending", true).forGetter(SingularitySnapshot::collapseDescending),
				Codec.INT.optionalFieldOf("ringTicks", 0).forGetter(SingularitySnapshot::ringTicks),
				Codec.INT.optionalFieldOf("phaseDelay", 0).forGetter(SingularitySnapshot::phaseDelay),
				Codec.INT.optionalFieldOf("fuseTicker", 0).forGetter(SingularitySnapshot::fuseTicker),
				Codec.LONG.optionalFieldOf("fuseElapsed", 0L).forGetter(SingularitySnapshot::fuseElapsed),
				Codec.LONG.listOf().optionalFieldOf("chunkQueue", List.of()).forGetter(SingularitySnapshot::chunkQueue),
				Codec.INT.optionalFieldOf("collapseTotal", 0).forGetter(SingularitySnapshot::collapseTotal),
				Codec.INT.optionalFieldOf("collapseCompleted", 0).forGetter(SingularitySnapshot::collapseCompleted),
				CollapseTimingSnapshot.CODEC.optionalFieldOf("timings").forGetter(snapshot -> Optional.ofNullable(snapshot.timings())),
				ResetSnapshot.CODEC.optionalFieldOf("reset").forGetter(snapshot -> Optional.ofNullable(snapshot.reset())),
				SingularityBorderSnapshot.CODEC.optionalFieldOf("border").forGetter(snapshot -> Optional.ofNullable(snapshot.border()))
		).apply(inst, (state,
				ticks,
				center,
				shellCollapsed,
				collapseRadius,
				collapseDescending,
				ringTicks,
				phaseDelay,
				fuseTicker,
				fuseElapsed,
				queue,
				total,
				completed,
				timings,
				reset,
				border) -> new SingularitySnapshot(state,
						ticks,
						center.orElse(null),
						shellCollapsed,
						collapseRadius,
						collapseDescending,
						ringTicks,
						phaseDelay,
						fuseTicker,
						fuseElapsed,
						queue,
						total,
						completed,
						timings.orElse(null),
						reset.orElse(null),
						border.orElse(null))));
	}

	public record CollapseTimingSnapshot(int delay, int hold) {
		public static final Codec<CollapseTimingSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.INT.optionalFieldOf("delay", 0).forGetter(CollapseTimingSnapshot::delay),
				Codec.INT.optionalFieldOf("hold", 0).forGetter(CollapseTimingSnapshot::hold)
		).apply(inst, CollapseTimingSnapshot::new));
	}

	public record ResetSnapshot(List<Long> queue, int delay) {
		public static final Codec<ResetSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.LONG.listOf().optionalFieldOf("queue", List.of()).forGetter(ResetSnapshot::queue),
				Codec.INT.optionalFieldOf("delay", 0).forGetter(ResetSnapshot::delay)
		).apply(inst, ResetSnapshot::new));
	}

	public record SingularityBorderSnapshot(
			boolean active,
			double centerX,
			double centerZ,
			double initialDiameter,
			double targetDiameter,
			long duration,
			long elapsed,
			double lastDiameter,
			@Nullable SingularityBorderOriginalSnapshot original,
			@Nullable SingularityBorderTimelineSnapshot timeline) {
		public static final Codec<SingularityBorderSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.BOOL.optionalFieldOf("active", false).forGetter(SingularityBorderSnapshot::active),
				Codec.DOUBLE.optionalFieldOf("centerX", 0.0D).forGetter(SingularityBorderSnapshot::centerX),
				Codec.DOUBLE.optionalFieldOf("centerZ", 0.0D).forGetter(SingularityBorderSnapshot::centerZ),
				Codec.DOUBLE.optionalFieldOf("initialDiameter", 0.0D).forGetter(SingularityBorderSnapshot::initialDiameter),
				Codec.DOUBLE.optionalFieldOf("targetDiameter", 0.0D).forGetter(SingularityBorderSnapshot::targetDiameter),
				Codec.LONG.optionalFieldOf("duration", 0L).forGetter(SingularityBorderSnapshot::duration),
				Codec.LONG.optionalFieldOf("elapsed", 0L).forGetter(SingularityBorderSnapshot::elapsed),
				Codec.DOUBLE.optionalFieldOf("lastDiameter", 0.0D).forGetter(SingularityBorderSnapshot::lastDiameter),
				SingularityBorderOriginalSnapshot.CODEC.optionalFieldOf("original").forGetter(snapshot -> Optional.ofNullable(snapshot.original())),
				SingularityBorderTimelineSnapshot.CODEC.optionalFieldOf("timeline").forGetter(snapshot -> Optional.ofNullable(snapshot.timeline()))
		).apply(inst, (active, centerX, centerZ, initialDiameter, targetDiameter, duration, elapsed, lastDiameter, original, timeline) ->
				new SingularityBorderSnapshot(active, centerX, centerZ, initialDiameter, targetDiameter, duration, elapsed, lastDiameter, original.orElse(null), timeline.orElse(null))));
	}

	public record SingularityBorderOriginalSnapshot(
			double centerX,
			double centerZ,
			double diameter,
			double safeZone,
			double damagePerBlock,
			int warningBlocks,
			int warningTime) {
		public static final Codec<SingularityBorderOriginalSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.optionalFieldOf("centerX", 0.0D).forGetter(SingularityBorderOriginalSnapshot::centerX),
				Codec.DOUBLE.optionalFieldOf("centerZ", 0.0D).forGetter(SingularityBorderOriginalSnapshot::centerZ),
				Codec.DOUBLE.optionalFieldOf("diameter", 0.0D).forGetter(SingularityBorderOriginalSnapshot::diameter),
				Codec.DOUBLE.optionalFieldOf("safeZone", 0.0D).forGetter(SingularityBorderOriginalSnapshot::safeZone),
				Codec.DOUBLE.optionalFieldOf("damagePerBlock", 0.0D).forGetter(SingularityBorderOriginalSnapshot::damagePerBlock),
				Codec.INT.optionalFieldOf("warningBlocks", 0).forGetter(SingularityBorderOriginalSnapshot::warningBlocks),
				Codec.INT.optionalFieldOf("warningTime", 0).forGetter(SingularityBorderOriginalSnapshot::warningTime)
		).apply(inst, SingularityBorderOriginalSnapshot::new));
	}

	public record SingularityBorderTimelineSnapshot(
			List<Double> thresholds,
			List<Integer> ringCounts,
			List<Integer> ringRadii,
			double initialDiameter,
			double finalDiameter,
			long totalTicks,
			long elapsedTicks,
			int ringIndex,
			int pendingChunks,
			double outerRadius,
			double innerRadius,
			int ringCount) {
		public static final Codec<SingularityBorderTimelineSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.listOf().optionalFieldOf("thresholds", List.of()).forGetter(SingularityBorderTimelineSnapshot::thresholds),
				Codec.INT.listOf().optionalFieldOf("ringCounts", List.of()).forGetter(SingularityBorderTimelineSnapshot::ringCounts),
				Codec.INT.listOf().optionalFieldOf("ringRadii", List.of()).forGetter(SingularityBorderTimelineSnapshot::ringRadii),
				Codec.DOUBLE.optionalFieldOf("initialDiameter", 0.0D).forGetter(SingularityBorderTimelineSnapshot::initialDiameter),
				Codec.DOUBLE.optionalFieldOf("finalDiameter", 0.0D).forGetter(SingularityBorderTimelineSnapshot::finalDiameter),
				Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(SingularityBorderTimelineSnapshot::totalTicks),
				Codec.LONG.optionalFieldOf("elapsedTicks", 0L).forGetter(SingularityBorderTimelineSnapshot::elapsedTicks),
				Codec.INT.optionalFieldOf("ringIndex", -1).forGetter(SingularityBorderTimelineSnapshot::ringIndex),
				Codec.INT.optionalFieldOf("pendingChunks", 0).forGetter(SingularityBorderTimelineSnapshot::pendingChunks),
				Codec.DOUBLE.optionalFieldOf("outerRadius", 0.0D).forGetter(SingularityBorderTimelineSnapshot::outerRadius),
				Codec.DOUBLE.optionalFieldOf("innerRadius", 0.0D).forGetter(SingularityBorderTimelineSnapshot::innerRadius),
				Codec.INT.optionalFieldOf("ringCount", 0).forGetter(SingularityBorderTimelineSnapshot::ringCount)
		).apply(inst, SingularityBorderTimelineSnapshot::new));
	}
}

