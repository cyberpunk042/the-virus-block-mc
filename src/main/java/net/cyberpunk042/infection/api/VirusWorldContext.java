package net.cyberpunk042.infection.api;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.world.ServerWorld;

/**
 * Host-facing context passed to scenarios each tick. It keeps exposure to the
 * legacy {@link VirusWorldState} narrow so we can gradually peel responsibilities
 * into smaller services without breaking persistence.
 */
public interface VirusWorldContext {
	ServerWorld world();

	VirusWorldState state();

	EffectBus effectBus();

	VirusScheduler scheduler();

	SingularityContext singularity();
}

