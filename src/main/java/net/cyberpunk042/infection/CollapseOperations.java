package net.cyberpunk042.infection;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.TheVirusBlock;

import java.util.List;

import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.service.SingularityHudService;
import net.cyberpunk042.infection.service.SingularityPhaseService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Operations facade for collapse-related logic that spans multiple services.
 * <p>
 * For simple service access, use the modules directly:
 * <ul>
 *   <li>{@code singularityServices().phase()} - phase management</li>
 *   <li>{@code singularityServices().fusing()} - fusing operations</li>
 *   <li>{@code singularityServices().lifecycle()} - lifecycle transitions</li>
 *   <li>{@code collapseModule().execution()} - block execution</li>
 * </ul>
 * <p>
 * This facade only contains methods with real orchestration logic.
 */
public final class CollapseOperations {
	private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;

	private final VirusWorldState state;
	private final SingularityPhaseService singularityPhaseService;

	public CollapseOperations(VirusWorldState state, SingularityPhaseService singularityPhaseService) {
		this.state = state;
		this.singularityPhaseService = singularityPhaseService;
	}

	// ========== Border Sync Data (aggregates multiple sources) ==========

	public SingularityHudService.BorderSyncData createBorderSyncData(
			double reportedDiameter,
			long reportedElapsed,
			double reportedTargetDiameter) {
		return state.singularity().border().createBorderSyncData(
				state.singularity().borderState(),
				state.singularityState().center,
				reportedDiameter,
				reportedElapsed,
				reportedTargetDiameter,
				state.singularityState().singularityState.name());
	}

	// ========== Collapse State Queries ==========

	public boolean hasCollapseWorkRemaining() {
		return state.singularity().collapseProcessor().isActive()
				|| !state.collapseModule().queues().chunkQueue().isEmpty();
	}

	// ========== State Transitions ==========

	public void transitionSingularityState(SingularityState next, String reason) {
		state.singularity().lifecycle().logSingularityStateChange(next, reason);
		state.singularityState().singularityState = next;
	}

	public boolean tryCompleteCollapse(ServerWorld world, CollapseBroadcastManager broadcastManager) {
		try {
			if (state.singularityState().singularityCollapseBarDelay > 0) {
				return true;
			}
			if (state.singularityState().singularityCollapseCompleteHold > 0) {
				state.singularityState().singularityCollapseCompleteHold--;
				return true;
			}
			if (state.singularityState().singularityCollapseCompletedChunks >= state.singularityState().singularityCollapseTotalChunks) {
				state.singularity().lifecycle().logSingularityStateChange(SingularityState.CORE, "collapse complete");
				state.singularityState().singularityState = SingularityState.CORE;
				state.singularityState().singularityRingTicks = state.collapseConfig().getRingDurationTicks();
				state.singularityState().singularityPhaseDelay = state.collapseConfig().configuredCoreChargeTicks();
				state.infection().broadcast(world, Text.translatable("message.the-virus-block.singularity_core").formatted(Formatting.RED));
				SingularityHudService.BorderSyncData data = createBorderSyncData(
						world.getWorldBorder().getSize(),
						state.singularity().borderState().elapsed,
						state.singularity().borderState().targetDiameter);
				if (broadcastManager != null) {
					broadcastManager.syncBorder(world, data);
				}
				state.markDirty();
			}
			return true;
		} catch (Exception e) {
			Logging.INFECTION.error("[CollapseOperations] tryCompleteCollapse failed", e);
			return false;
		}
	}

	// ========== Radius Computation ==========

	public int computeInitialCollapseRadius(ServerWorld world) {
		int radius = state.collapseConfig().configuredCollapseMaxRadius();
		List<net.minecraft.server.network.ServerPlayerEntity> players = 
				world.getPlayers(net.minecraft.entity.player.PlayerEntity::isAlive);
		if (!players.isEmpty()) {
			double maxDistance = 0.0D;
			BlockPos center = state.singularityState().center != null 
					? state.singularityState().center 
					: players.get(0).getBlockPos();
			for (net.minecraft.server.network.ServerPlayerEntity player : players) {
				double distance = Math.max(
						player.squaredDistanceTo(net.minecraft.util.math.Vec3d.ofCenter(center)), 
						0.0D);
				maxDistance = Math.max(maxDistance, Math.sqrt(distance));
			}
			radius = (int) Math.min(
					state.collapseConfig().configuredCollapseMaxRadius(), 
					Math.max(32.0D, maxDistance + 32.0D));
		}
		return Math.max(16, radius);
	}

	// ========== Force Start (Command) ==========

	public boolean forceStartSingularity(ServerWorld world, int seconds, BlockPos fallbackCenter) {
		try {
			return doForceStartSingularity(world, seconds, fallbackCenter);
		} catch (Exception e) {
			Logging.INFECTION.error("[CollapseOperations] forceStartSingularity failed", e);
			return false;
		}
	}

	private boolean doForceStartSingularity(ServerWorld world, int seconds, BlockPos fallbackCenter) {
		state.collapseConfig().ensureSingularityGamerules(world);
		state.singularity().phase().restoreSingularityBorder();
		if (!state.infectionState().infected()) {
			state.infection().ensureDebugInfection(world, fallbackCenter);
		}
		state.singularity().barrier().reset();
		if (!state.hasVirusSources()) {
			state.sources().addSource(state.sourceState(), fallbackCenter);
		}
		int clampedSeconds = MathHelper.clamp(seconds, 5, 120);
		InfectionTier tier = InfectionTier.byIndex(InfectionTier.maxIndex());
		state.tiers().setIndex(tier.getIndex());
		state.tiers().setTicksInTier(state.tiers().duration(tier));
		state.tiers().setApocalypseMode(false);
		state.singularity().barrier().resetTimers();
		state.singularity().lifecycle().logSingularityStateChange(
				SingularityState.FUSING, "force command seconds=" + clampedSeconds);
		state.singularityState().singularityState = SingularityState.FUSING;
		state.singularityState().singularityTicks = Math.min(
				state.collapseConfig().configuredFuseExplosionDelayTicks(), 
				clampedSeconds * 20L);
		state.singularityState().shellCollapsed = false;
		state.singularityState().singularityCollapseRadius = 0;
		state.singularityState().singularityCollapseDescending = true;
		state.singularityState().singularityCollapseTotalChunks = 0;
		state.singularityState().singularityCollapseCompletedChunks = 0;
		state.singularityState().singularityCollapseBarDelay = 0;
		state.singularityState().singularityRingTicks = 0;
		state.singularityState().singularityPhaseDelay = 0;
		state.singularityState().fusePulseTicker = 0;
		state.singularityState().fuseElapsed = 0;
		singularityPhaseService.resetSingularityPreparation("forceStart");
		if (state.collapseConfig().configuredCollapseEnabled(world)) {
			singularityPhaseService.prepareSingularityChunkQueue(state.createSingularityContext(world));
		} else {
			state.collapseModule().queues().chunkQueue().clear();
		}
		BlockPos center = state.singularityState().center;
		if (center == null || !world.isChunkLoaded(net.minecraft.util.math.ChunkPos.toLong(center))) {
			center = state.infection().representativePos(world, world.getRandom(), state.sourceState());
		}
		if (center == null) {
			center = fallbackCenter.toImmutable();
		}
		state.singularityState().center = center != null ? center.toImmutable() : null;
		state.singularity().fusing().maintainFuseEntities();
		singularityPhaseService.applyCollapseDistanceOverrides();
		state.markDirty();
		state.infection().broadcast(world, 
				Text.translatable("message.the-virus-block.singularity_forced").formatted(Formatting.LIGHT_PURPLE));
		return true;
	}
}
