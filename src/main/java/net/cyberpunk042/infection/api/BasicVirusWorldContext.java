package net.cyberpunk042.infection.api;

import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.world.ServerWorld;

/**
 * Simple context implementation used to bootstrap scenario hosting from
 * {@link VirusWorldState}. Later milestones will likely replace this with a DI
 * composer that constructs planners, controllers, and effect listeners per
 * scenario.
 */
public final class BasicVirusWorldContext implements VirusWorldContext {
	private final ServerWorld world;
	private final VirusWorldState state;
	private final EffectBus effectBus;
	private final VirusScheduler scheduler;
	private final SingularityContext singularity;

	public BasicVirusWorldContext(ServerWorld world, VirusWorldState state, EffectBus effectBus,
			VirusScheduler scheduler, SingularityContext singularity) {
		this.world = Objects.requireNonNull(world, "world");
		this.state = Objects.requireNonNull(state, "state");
		this.effectBus = Objects.requireNonNull(effectBus, "effectBus");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.singularity = Objects.requireNonNull(singularity, "singularity");
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
	public SingularityContext singularity() {
		return singularity;
	}
}

