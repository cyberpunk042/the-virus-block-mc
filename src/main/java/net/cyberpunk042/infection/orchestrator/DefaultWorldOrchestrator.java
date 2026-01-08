package net.cyberpunk042.infection.orchestrator;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.*;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Unified orchestrator that coordinates world-level infection subsystems.
 * <p>
 * Access specific functionality through focused managers:
 * <ul>
 *   <li>{@link #scenarios()} - scenario lifecycle</li>
 *   <li>{@link #services()} - scheduler, effect bus, broadcasts</li>
 *   <li>{@link #phases()} - singularity phase transitions</li>
 * </ul>
 * <p>
 * Dependencies are injected via {@link OrchestratorDependencies} to avoid
 * circular references with VirusWorldState.
 */
public class DefaultWorldOrchestrator implements WorldOrchestrator, OrchestratorDependencies.ServiceAccessor {

	private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;

	private volatile OrchestratorState state = OrchestratorState.INITIALIZING;

	// Focused managers
	private final ScenarioManager scenarios;
	private final ServiceHub services;
	private final PhaseManager phases;

	// Callbacks
	private final IWorldCallbacks worldCallbacks;

	/**
	 * Creates a new orchestrator with injected dependencies.
	 * @param deps the bundled dependencies (no VirusWorldState reference)
	 */
	public DefaultWorldOrchestrator(OrchestratorDependencies deps) {
		Objects.requireNonNull(deps, "deps");
		
		// Create managers with direct dependencies
		this.services = new ServiceHub();
		this.scenarios = new ScenarioManager(deps.contextFactory());
		this.phases = new PhaseManager(
			deps.singularityModule(),
			deps.singularityContextFactory(),
			() -> this.state
		);
		
		// Create callbacks using factory
		this.worldCallbacks = deps.callbacksFactory().create(this);
		
		// Ready for use
		this.state = OrchestratorState.RUNNING;
	}

	/**
	 * Install services that were created by VirusWorldState.
	 */
	public void installServices(
			@Nullable EffectBus effectBus,
			@Nullable CollapseBroadcastManager broadcastManager,
			@Nullable ScenarioRegistry registry) {
		if (effectBus != null) {
			services.installEffectBus(effectBus);
		}
		if (broadcastManager != null) {
			services.installCollapseBroadcastManager(broadcastManager);
		}
		if (registry != null) {
			scenarios.installRegistry(registry);
		}
	}

	// ========== ServiceAccessor Implementation ==========

	@Override
	public VirusScheduler schedulerOrNoop() {
		return services.schedulerOrNoop();
	}

	// ========== WorldOrchestrator Implementation ==========

	@Override
	public OrchestratorState state() {
		return this.state;
	}

	@Override
	public boolean isShutdown() {
		return this.state.isShuttingDownOrShutdown();
	}

	@Override
	public ScenarioManager scenarios() {
		return this.scenarios;
	}

	@Override
	public ServiceHub services() {
		return this.services;
	}

	@Override
	public PhaseManager phases() {
		return this.phases;
	}

	@Override
	public IWorldCallbacks callbacks() {
		return this.worldCallbacks;
	}

	@Override
	public void tick(ServerWorld world) {
		if (!state.canTick()) {
			if (state == OrchestratorState.SHUTDOWN) {
				Logging.ORCHESTRATOR.warn("[Orchestrator] tick() called after shutdown - ignoring");
			}
			return;
		}

		// Tick services (scheduler, broadcasts)
		net.cyberpunk042.util.SuperProfiler.start("Orch.services");
		services.tick(world);
		net.cyberpunk042.util.SuperProfiler.end("Orch.services");

		// Tick scenario
		net.cyberpunk042.util.SuperProfiler.start("Orch.scenarios");
		scenarios.tick(world);
		net.cyberpunk042.util.SuperProfiler.end("Orch.scenarios");
	}

	/**
	 * Tick phases - called by VirusWorldState which has access to state.
	 */
	public void tickPhases(ServerWorld world, VirusWorldState state) {
		net.cyberpunk042.util.SuperProfiler.start("Orch.phases");
		phases.tick(world, state);
		net.cyberpunk042.util.SuperProfiler.end("Orch.phases");
	}

	@Override
	public void shutdown(ServerWorld world) {
		if (state.isShuttingDownOrShutdown()) {
			Logging.ORCHESTRATOR.debug("[Orchestrator] Already shutting down or shut down");
			return;
		}
		
		state = OrchestratorState.SHUTTING_DOWN;
		Logging.ORCHESTRATOR.info("[Orchestrator] Shutting down for world: {}", world.getRegistryKey().getValue());
		
		scenarios.shutdown(world);
		phases.shutdown();
		services.shutdown();
		
		state = OrchestratorState.SHUTDOWN;
	}

	// ========== Convenience Methods (used by VirusWorldState context factories) ==========

	/**
	 * Gets the attached scenario ID, used by context factories.
	 */
	public @Nullable net.minecraft.util.Identifier getAttachedScenarioId() {
		return scenarios.active() != null ? scenarios.active().id() : null;
	}

	/**
	 * Gets the effect bus or noop, used by context factories.
	 */
	public EffectBus effectBusOrNoop() {
		return services.effectBusOrNoop();
	}

	/**
	 * Gets the collapse broadcast manager or noop, used by context factories.
	 */
	public CollapseBroadcastManager collapseBroadcastManagerOrNoop() {
		return services.collapseBroadcastManagerOrNoop();
	}
}
