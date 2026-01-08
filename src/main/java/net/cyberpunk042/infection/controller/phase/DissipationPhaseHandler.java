package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.events.DissipationTickEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handles the DISSIPATION phase of the singularity lifecycle.
 * During this phase, the singularity fades out before transitioning
 * to reset or finishing.
 */
public final class DissipationPhaseHandler {

	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		EffectBus bus = ctx.effectBus();

		BlockPos center = state.singularityState().center;
		if (center == null) {
			state.singularity().fusing().clearFuseEntities();
			state.singularity().phase().resetSingularityState();
			state.markDirty();
			return;
		}

		// Post dissipation tick event for visual effects
		bus.post(new DissipationTickEvent(world, center, state.singularityState().singularityPhaseDelay));

		// Countdown before transition
		if (state.singularityState().singularityPhaseDelay > 0) {
			state.singularityState().singularityPhaseDelay--;
			state.markDirty();
			return;
		}

		// Check if we should start post-collapse reset
		if (state.singularity().lifecycle().shouldStartPostReset()) {
			state.orchestrator().phases().startPostCollapseReset(world);
			return;
		}

		// Finish singularity
		state.orchestrator().phases().finishSingularity(world);
	}
}

