package net.cyberpunk042.infection.orchestrator;

import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.cyberpunk042.infection.state.SingularityModule;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;
import java.util.function.Function;

/**
 * Bundles dependencies needed by the orchestrator.
 * This avoids passing the entire VirusWorldState and breaking circular dependencies.
 */
public record OrchestratorDependencies(
	SingularityModule singularityModule,
	Function<ServerWorld, VirusWorldContext> contextFactory,
	Function<ServerWorld, SingularityContext> singularityContextFactory,
	CallbacksFactory callbacksFactory
) {
	public OrchestratorDependencies {
		Objects.requireNonNull(singularityModule, "singularityModule");
		Objects.requireNonNull(contextFactory, "contextFactory");
		Objects.requireNonNull(singularityContextFactory, "singularityContextFactory");
		Objects.requireNonNull(callbacksFactory, "callbacksFactory");
	}

	/**
	 * Factory for creating world callbacks.
	 * Allows VirusWorldState to provide callbacks without exposing itself.
	 */
	@FunctionalInterface
	public interface CallbacksFactory {
		IWorldCallbacks create(ServiceAccessor services);
	}

	/**
	 * Provides access to services without exposing the full orchestrator.
	 */
	public interface ServiceAccessor {
		net.cyberpunk042.infection.api.VirusScheduler schedulerOrNoop();
	}
}

