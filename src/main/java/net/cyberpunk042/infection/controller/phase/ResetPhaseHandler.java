package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.minecraft.server.world.ServerWorld;

/**
 * Encapsulates the RESET phase logic so {@link net.cyberpunk042.infection.VirusWorldState}
 * no longer drives it directly.
 */
public final class ResetPhaseHandler {
	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		state.orchestrator().phases().processSingularityReset(world);
	}
}
