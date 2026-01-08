package net.cyberpunk042.infection.api;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

/**
 * Dependency bundle that the singularity controller will consume once the
 * monolithic logic is extracted from {@link VirusWorldState}. The context keeps
 * pointers to the host world/state plus shared services that will eventually be
 * provided by scenario-specific installers.
 */
public interface SingularityContext {
	ServerWorld world();

	VirusWorldState state();

	EffectBus effectBus();

	VirusScheduler scheduler();

	CollapseBroadcastManager broadcastManager();

	DimensionProfile profile();

	@Nullable InfectionServiceContainer services();
}
