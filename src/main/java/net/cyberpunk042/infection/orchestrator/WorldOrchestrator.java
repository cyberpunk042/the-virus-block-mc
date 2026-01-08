package net.cyberpunk042.infection.orchestrator;

import net.minecraft.server.world.ServerWorld;

/**
 * High-level contract for coordinating infection subsystems.
 * <p>
 * Access specific functionality through focused managers:
 * <ul>
 *   <li>{@link #scenarios()} - scenario lifecycle</li>
 *   <li>{@link #services()} - scheduler, effect bus, broadcasts</li>
 *   <li>{@link #phases()} - singularity phase transitions</li>
 * </ul>
 */
public interface WorldOrchestrator {

	/* -------- Core lifecycle -------- */

	void tick(ServerWorld world);

	void shutdown(ServerWorld world);

	boolean isShutdown();

	OrchestratorState state();

	/* -------- Manager access -------- */

	ScenarioManager scenarios();

	ServiceHub services();

	PhaseManager phases();

	/* -------- Callbacks -------- */

	IWorldCallbacks callbacks();
}
