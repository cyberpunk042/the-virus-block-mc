package net.cyberpunk042.infection.orchestrator;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.api.InfectionScenario;
import net.cyberpunk042.infection.api.ScenarioRegistry;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Manages scenario lifecycle: resolution, attach, detach, and tick.
 * Extracted from {@link DefaultWorldOrchestrator} to follow Single Responsibility Principle.
 */
public final class ScenarioManager {

	private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;

	private @Nullable ScenarioRegistry registry;
	private @Nullable InfectionScenario attached;
	private final Function<ServerWorld, VirusWorldContext> contextFactory;

	/**
	 * Creates a new ScenarioManager.
	 * @param contextFactory function to create VirusWorldContext for a world
	 */
	public ScenarioManager(Function<ServerWorld, VirusWorldContext> contextFactory) {
		this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
	}

	// ========== Registry ==========

	public @Nullable ScenarioRegistry registry() {
		return this.registry;
	}

	public void installRegistry(@Nullable ScenarioRegistry registry) {
		this.registry = registry;
	}

	// ========== Active Scenario ==========

	public @Nullable InfectionScenario active() {
		return this.attached;
	}

	/** Returns the active scenario ID as Optional, empty if none attached. */
	public Optional<Identifier> activeId() {
		return Optional.ofNullable(this.attached != null ? this.attached.id() : null);
	}

	public boolean hasActiveScenario() {
		return this.attached != null;
	}

	// ========== Attach / Detach ==========

	/**
	 * Ensures a scenario is attached for the given world's dimension.
	 * If already attached, returns the current scenario.
	 * If no registry or no scenario found, returns null.
	 */
	public @Nullable InfectionScenario ensure(ServerWorld world) {
		if (this.attached != null) {
			return this.attached;
		}
		if (this.registry == null) {
			return null;
		}
		Optional<InfectionScenario> resolved = this.registry.resolve(world.getRegistryKey());
		if (resolved.isPresent()) {
			InfectionScenario scenario = resolved.get();
			try {
				scenario.onAttach(contextFactory.apply(world));
				this.attached = scenario;
				Logging.ORCHESTRATOR.debug("[ScenarioManager] Attached scenario: {} for dimension: {}",
					scenario.id(), world.getRegistryKey().getValue());
			} catch (Exception e) {
				Logging.ORCHESTRATOR.error("[ScenarioManager] Failed to attach scenario: {} for dimension: {}",
					scenario.id(), world.getRegistryKey().getValue(), e);
				return null;
			}
		}
		return this.attached;
	}

	/**
	 * Detaches the current scenario if one is attached.
	 */
	public void detach(ServerWorld world) {
		if (this.attached != null) {
			Identifier scenarioId = this.attached.id();
			InfectionScenario scenario = this.attached;
			// Clear reference FIRST to ensure consistent state even if onDetach throws
			this.attached = null;
			try {
				scenario.onDetach(contextFactory.apply(world));
				Logging.ORCHESTRATOR.debug("[ScenarioManager] Detached scenario: {}", scenarioId);
			} catch (Exception e) {
				Logging.ORCHESTRATOR.error("[ScenarioManager] Error during scenario detach: {}", scenarioId, e);
			}
		}
	}

	// ========== Tick ==========

	/**
	 * Ticks the active scenario if one is attached.
	 */
	public void tick(ServerWorld world) {
		InfectionScenario scenario = ensure(world);
		if (scenario != null) {
			long start = System.nanoTime();
			VirusWorldContext context = contextFactory.apply(world);
			try {
				// Scenario-specific tasks (guardian beams, etc.)
				scenario.tick(context);
				// Core infection mechanics (continuous + infection frame)
				boolean infected = context.state().infectionState().infected();
				scenario.tickInfection(context, infected);
			} catch (Exception e) {
				Logging.ORCHESTRATOR.error("[ScenarioManager] Scenario tick failed for {}", activeId(), e);
			}
			long elapsed = (System.nanoTime() - start) / 1_000_000;
			if (elapsed > 10) {
				Logging.ORCHESTRATOR.debug("[ScenarioManager] Scenario tick took {}ms", elapsed);
			}
		}
	}

	// ========== Shutdown ==========

	/**
	 * Shuts down the scenario manager, detaching any active scenario.
	 */
	public void shutdown(ServerWorld world) {
		detach(world);
		this.registry = null;
	}
}

