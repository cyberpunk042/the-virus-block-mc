package net.cyberpunk042.infection.service;

import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;

/**
 * Manages the singularity barrier lifecycle (deploy, sync, reset) so the
 * persistent host no longer tracks dozens of raw fields.
 */
public final class SingularityBorderService {

	public static final class State {
		public boolean active;
		public boolean pendingDeployment;
		public boolean hasSnapshot;
		public double centerX;
		public double centerZ;
		public double initialDiameter;
		public double targetDiameter;
		public double lastDiameter;
		public int resetCountdown = -1;
		public long duration;
		public long elapsed;
		public double originalCenterX;
		public double originalCenterZ;
		public double originalDiameter;
		public double originalSafeZone;
		public double originalDamagePerBlock;
		public int originalWarningBlocks;
		public int originalWarningTime;
		public double outerRadius;
		public double innerRadius;
		public double initialBorderDiameter;
		public double finalBorderDiameter;
	}

	public void deploy(State state,
			ServerWorld world,
			double centerX,
			double centerZ,
			double initialDiameter,
			double finalDiameter,
			long durationTicks) {
		WorldBorder border = world.getWorldBorder();
		captureOriginal(state, border);
		state.active = true;
		state.pendingDeployment = false;
		state.centerX = centerX;
		state.centerZ = centerZ;
		state.initialDiameter = initialDiameter;
		state.targetDiameter = finalDiameter;
		state.lastDiameter = initialDiameter;
		state.resetCountdown = -1;
		state.initialBorderDiameter = initialDiameter;
		state.finalBorderDiameter = finalDiameter;
		state.duration = durationTicks;
		state.elapsed = 0L;

		border.setCenter(centerX, centerZ);
		border.setSize(initialDiameter);
		SingularityChunkContext.enableBorderGuard(world);
	}

	public void restore(State state, ServerWorld world) {
		if (!state.active && !state.hasSnapshot) {
			return;
		}
		WorldBorder border = world.getWorldBorder();
		if (state.hasSnapshot) {
			border.setCenter(state.originalCenterX, state.originalCenterZ);
			border.setSize(state.originalDiameter);
			border.setSafeZone(state.originalSafeZone);
			border.setDamagePerBlock(state.originalDamagePerBlock);
			border.setWarningBlocks(state.originalWarningBlocks);
			border.setWarningTime(state.originalWarningTime);
		}
		clear(state);
		SingularityChunkContext.disableBorderGuard(world);
	}

	public boolean maybeAutoReset(State state,
			ServerWorld world,
			boolean barrierAutoResetEnabled,
			long barrierResetDelayTicks,
			boolean collapseFinished,
			boolean shrinkFinished) {
		if (!barrierAutoResetEnabled || world == null) {
			return false;
		}
		if (!state.active) {
			state.resetCountdown = -1;
			return false;
		}
		if (!shrinkFinished || !collapseFinished) {
			return false;
		}
		if (state.resetCountdown < 0) {
			long delay = Math.max(0L, barrierResetDelayTicks);
			state.resetCountdown = delay > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delay;
			return false;
		}
		if (state.resetCountdown-- > 0) {
			return false;
		}
		restore(state, world);
		return true;
	}

	public SingularityHudService.BorderSyncData createBorderSyncData(State state,
			BlockPos singularityCenter,
			double reportedDiameter,
			long reportedElapsed,
			double reportedTargetDiameter,
			String phaseName) {
		double diameter = reportedDiameter;
		double centerX = state.centerX;
		double centerZ = state.centerZ;
		if (centerX == 0.0D && singularityCenter != null) {
			centerX = singularityCenter.getX() + 0.5D;
		}
		if (centerZ == 0.0D && singularityCenter != null) {
			centerZ = singularityCenter.getZ() + 0.5D;
		}
		return new SingularityHudService.BorderSyncData(
				state.active,
				centerX,
				centerZ,
				state.initialDiameter,
				diameter,
				reportedTargetDiameter,
				state.duration,
				reportedElapsed,
				phaseName);
	}

	public void captureOriginal(State state, WorldBorder border) {
		if (state.hasSnapshot) {
			return;
		}
		state.originalCenterX = border.getCenterX();
		state.originalCenterZ = border.getCenterZ();
		state.originalDiameter = border.getSize();
		state.originalSafeZone = border.getSafeZone();
		state.originalDamagePerBlock = border.getDamagePerBlock();
		state.originalWarningBlocks = border.getWarningBlocks();
		state.originalWarningTime = border.getWarningTime();
		state.hasSnapshot = true;
	}

	public void clear(State state) {
		state.active = false;
		state.pendingDeployment = false;
		state.hasSnapshot = false;
		state.centerX = 0.0D;
		state.centerZ = 0.0D;
		state.initialDiameter = 0.0D;
		state.targetDiameter = 0.0D;
		state.duration = 0L;
		state.elapsed = 0L;
		state.lastDiameter = 0.0D;
		state.originalCenterX = 0.0D;
		state.originalCenterZ = 0.0D;
		state.originalDiameter = 0.0D;
		state.originalSafeZone = 0.0D;
		state.originalDamagePerBlock = 0.0D;
		state.originalWarningBlocks = 0;
		state.originalWarningTime = 0;
		state.outerRadius = 0.0D;
		state.innerRadius = 0.0D;
		state.initialBorderDiameter = 0.0D;
		state.finalBorderDiameter = 0.0D;
		state.resetCountdown = -1;
	}
}

