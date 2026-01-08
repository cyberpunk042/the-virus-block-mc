package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.minecraft.server.world.ServerWorld;

public final class FusingPhaseHandler {
	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		state.singularity().chunkPreparation().tickPreGeneration();
		state.singularity().chunkPreparation().tickChunkPreload();
		state.singularity().phase().tickPreCollapseDrainage();
		state.singularity().lifecycle().tickFuseWatchdog();
		if (state.singularityState().singularityTicks > 0) {
			state.singularityState().singularityTicks--;
			state.singularityState().fuseElapsed++;
			state.singularity().fusing().emitFuseEffects();
			state.singularity().fusing().maintainFuseEntities();
			if (!state.singularityState().shellCollapsed
					&& state.singularityState().fuseElapsed >= state.collapseConfig().configuredFuseShellCollapseTicks()) {
				state.singularityState().shellCollapsed = true;
				if (!state.shell().isCollapsed()) {
					state.shell().collapse(world, state.getVirusSources());
				}
			}
			if (state.singularityState().singularityTicks <= 0) {
				state.singularity().phase().handleFuseCountdownComplete(ctx);
			}
		}
	}
}

