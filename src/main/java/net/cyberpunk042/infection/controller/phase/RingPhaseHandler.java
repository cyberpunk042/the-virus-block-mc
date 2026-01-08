package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handles the RING phase of the singularity lifecycle.
 * During this phase, visual ring effects pulse outward from the center
 * and columns are carved in a circular pattern.
 */
public final class RingPhaseHandler {

	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		EffectBus bus = ctx.effectBus();
		DimensionProfile profile = ctx.profile();

		BlockPos center = state.singularityState().center;
		if (center == null) {
			state.collapse().transitionSingularityState(SingularityState.DISSIPATION, "ring center missing");
			state.singularity().phase().syncSingularityBorder();
			state.markDirty();
			return;
		}

		// Phase delay countdown
		if (state.singularityState().singularityPhaseDelay > 0) {
			state.singularityState().singularityPhaseDelay--;
			state.markDirty();
			return;
		}

		// Check if ring phase is complete
		if (state.singularityState().singularityRingTicks <= 0) {
			state.collapse().transitionSingularityState(SingularityState.DISSIPATION, "ring finished");
			state.singularityState().singularityPhaseDelay = state.collapseConfig().configuredResetDelayTicks();
			state.singularity().phase().syncSingularityBorder();
			state.markDirty();
			return;
		}

		// Decrement ring ticks
		state.singularityState().singularityRingTicks--;

		// Calculate ring radius based on elapsed time
		int ringDuration = state.collapseConfig().getRingDurationTicks();
		int elapsed = ringDuration - state.singularityState().singularityRingTicks;
		double radius = 8.0D + elapsed * 0.25D;

		// Carve columns in circular pattern
		int columns = Math.max(1, profile.collapse().columnsPerTick());
		for (int i = 0; i < columns; i++) {
			double angle = world.random.nextDouble() * Math.PI * 2;
			double columnRadius = radius + world.random.nextBetween(-2, 2);
			double x = center.getX() + 0.5D + Math.cos(angle) * columnRadius;
			double z = center.getZ() + 0.5D + Math.sin(angle) * columnRadius;
			BlockPos destroy = BlockPos.ofFloored(x, center.getY(), z);
			int minY = world.getBottomY();
			int maxY = world.getBottomY() + world.getDimension().height() - 1;
			boolean fromBottom = world.random.nextBoolean();
			state.collapseModule().execution().carveColumn(destroy, fromBottom ? minY : maxY, fromBottom ? maxY : minY, fromBottom ? 1 : -1, state.collapseConfig().collapseParticlesEnabled(), 6);
		}

		// Pull entities toward the ring
		state.presentationCoord().pushEntitiesTowardRing(radius + state.collapseConfig().getRingPullRadius(), state.singularityState().center);
		state.markDirty();
	}
}

