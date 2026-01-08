package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles the dormant state monitoring so {@link net.cyberpunk042.infection.VirusWorldState}
 * no longer needs to host the logic inline with the singularity tick.
 */
public final class DormantPhaseHandler {
	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		if (state.tiers().isApocalypseMode()) {
			return;
		}
		long remaining = state.tiers().ticksUntilFinalWave();
		long fuseDelay = state.collapseConfig().configuredFuseExplosionDelayTicks();
		if (remaining > 0 && remaining <= fuseDelay && state.tiers().currentHealth() <= 0.0D) {
			state.singularity().fusing().beginFusing(ctx, fuseDelay);
		}
		state.singularity().fusing().clearFuseEntities();
	}
}

