package net.cyberpunk042.infection.api;

import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal immutable implementation used by the host during the Milestone 1
 * rollout.
 */
public final class BasicSingularityContext implements SingularityContext {
	private final ServerWorld world;
	private final VirusWorldState state;
	private final EffectBus effectBus;
	private final VirusScheduler scheduler;
	private final CollapseBroadcastManager broadcastManager;
	private final DimensionProfile profile;
	@Nullable private final InfectionServiceContainer services;

	public BasicSingularityContext(ServerWorld world,
			VirusWorldState state,
			EffectBus effectBus,
			VirusScheduler scheduler,
			CollapseBroadcastManager broadcastManager,
			DimensionProfile profile,
			InfectionServiceContainer services) {
		this.world = Objects.requireNonNull(world, "world");
		this.state = Objects.requireNonNull(state, "state");
		this.effectBus = Objects.requireNonNull(effectBus, "effectBus");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.broadcastManager = Objects.requireNonNull(broadcastManager, "broadcastManager");
		this.profile = Objects.requireNonNull(profile, "profile");
		this.services = services;
	}

	public BasicSingularityContext(SingularityDependencies deps) {
		this(deps.world(),
				deps.state(),
				deps.effectBus(),
				deps.scheduler(),
				deps.broadcastManager(),
				deps.profile(),
				deps.services());
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public VirusWorldState state() {
		return state;
	}

	@Override
	public EffectBus effectBus() {
		return effectBus;
	}

	@Override
	public VirusScheduler scheduler() {
		return scheduler;
	}

	@Override
	public CollapseBroadcastManager broadcastManager() {
		return broadcastManager;
	}

	@Override
	public DimensionProfile profile() {
		return profile;
	}

	@Override
	public @Nullable InfectionServiceContainer services() {
		return services;
	}
}
