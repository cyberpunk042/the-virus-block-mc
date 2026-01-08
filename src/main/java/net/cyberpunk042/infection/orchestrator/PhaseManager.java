package net.cyberpunk042.infection.orchestrator;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.controller.phase.*;
import net.cyberpunk042.infection.state.SingularityModule;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manages singularity phase state machine and transitions.
 * <p>
 * This class owns the phase handlers directly (no separate controller layer).
 * Each phase is dispatched to its corresponding handler based on current state.
 * </p>
 * <p>
 * Dependencies are injected directly - no back-reference to VirusWorldState.
 * </p>
 */
public final class PhaseManager {

	private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;

	private final SingularityModule singularity;
	private final Function<ServerWorld, SingularityContext> contextFactory;
	private final Supplier<OrchestratorState> stateSupplier;

	// Phase handlers (stateless, can be shared)
	private final DormantPhaseHandler dormantHandler = new DormantPhaseHandler();
	private final FusingPhaseHandler fusingHandler = new FusingPhaseHandler();
	private final CollapsePhaseHandler collapseHandler = new CollapsePhaseHandler();
	private final CorePhaseHandler coreHandler = new CorePhaseHandler();
	private final RingPhaseHandler ringHandler = new RingPhaseHandler();
	private final DissipationPhaseHandler dissipationHandler = new DissipationPhaseHandler();
	private final ResetPhaseHandler resetHandler = new ResetPhaseHandler();

	/**
	 * Creates a new PhaseManager.
	 * @param singularity the singularity module (direct dependency, no host traversal)
	 * @param contextFactory creates SingularityContext for a world
	 * @param stateSupplier supplies the current orchestrator state
	 */
	public PhaseManager(
			SingularityModule singularity,
			Function<ServerWorld, SingularityContext> contextFactory,
			Supplier<OrchestratorState> stateSupplier) {
		this.singularity = Objects.requireNonNull(singularity, "singularity");
		this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
		this.stateSupplier = Objects.requireNonNull(stateSupplier, "stateSupplier");
	}

	// ========== Tick ==========

	/**
	 * Ticks the singularity phase state machine.
	 * Dispatches to the appropriate phase handler based on current state.
	 */
	public void tick(ServerWorld world, VirusWorldState state) {
		if (!stateSupplier.get().canTick()) {
			return;
		}

		SingularityContext ctx = contextFactory.apply(world);

		try {
			// Common tasks for all phases
			state.collapseConfig().ensureSingularityGamerules(world);
			state.singularity().phase().tickSingularityBorder();

		if (!state.infectionState().infected()) {
			singularity.fusing().handleSingularityInactive();
			return;
		}

		// Dispatch to phase handler (pregen runs in FusingPhaseHandler, not here)
			dispatchPhase(singularity.state().singularityState, ctx);

		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] Phase tick failed for state: {}", 
				singularity.state().singularityState, e);
		}
	}

	private void dispatchPhase(SingularityState phase, SingularityContext ctx) {
		switch (phase) {
			case DORMANT -> dormantHandler.tick(ctx);
			case FUSING -> fusingHandler.tick(ctx);
			case COLLAPSE -> collapseHandler.tick(ctx);
			case CORE -> coreHandler.tick(ctx);
			case RING -> ringHandler.tick(ctx);
			case DISSIPATION -> dissipationHandler.tick(ctx);
			case RESET -> resetHandler.tick(ctx);
		}
	}

	// ========== Phase Transitions ==========

	public void beginFusing(ServerWorld world, long fuseDelay) {
		if (!canExecuteTransition()) return;
		try {
			singularity.fusing().beginFusing(contextFactory.apply(world), fuseDelay);
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] beginFusing failed", e);
		}
	}

	public void handleFuseCountdownComplete(ServerWorld world) {
		if (!canExecuteTransition()) return;
		try {
			singularity.phase().handleFuseCountdownComplete(contextFactory.apply(world));
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] handleFuseCountdownComplete failed", e);
		}
	}

	public void startPostCollapseReset(ServerWorld world) {
		if (!canExecuteTransition()) return;
		try {
			singularity.lifecycle().startPostCollapseReset();
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] startPostCollapseReset failed", e);
		}
	}

	public void processSingularityReset(ServerWorld world) {
		if (!canExecuteTransition()) return;
		try {
			singularity.lifecycle().processSingularityReset();
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] processSingularityReset failed", e);
		}
	}

	public void finishSingularity(ServerWorld world) {
		if (!canExecuteTransition()) return;
		try {
			singularity.lifecycle().finishSingularity();
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] finishSingularity failed", e);
		}
	}

	public boolean abortSingularity(ServerWorld world) {
		if (!canExecuteTransition()) return false;
		try {
			return singularity.lifecycle().abortSingularity();
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[PhaseManager] abortSingularity failed", e);
			return false;
		}
	}

	// ========== Helpers ==========

	private boolean canExecuteTransition() {
		return stateSupplier.get().canTick();
	}

	// ========== Shutdown ==========

	public void shutdown() {
		// Nothing to clean up - handlers are stateless
	}
}
