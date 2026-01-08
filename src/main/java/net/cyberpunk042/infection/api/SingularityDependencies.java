package net.cyberpunk042.infection.api;

import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.minecraft.server.world.ServerWorld;

/**
 * Immutable bundle of services that the singularity controller and related
 * components consume. Scenarios compose this once per tick so tests can inject
 * fakes without reaching back into {@link VirusWorldState}.
 */
public record SingularityDependencies(
		ServerWorld world,
		VirusWorldState state,
		EffectBus effectBus,
		VirusScheduler scheduler,
		CollapseBroadcastManager broadcastManager,
		DimensionProfile profile,
		InfectionServiceContainer services) {

	public SingularityDependencies {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(effectBus, "effectBus");
		Objects.requireNonNull(scheduler, "scheduler");
		Objects.requireNonNull(broadcastManager, "broadcastManager");
		Objects.requireNonNull(profile, "profile");
	}
}
