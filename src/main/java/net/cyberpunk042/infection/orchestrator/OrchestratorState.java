package net.cyberpunk042.infection.orchestrator;

/**
 * Lifecycle state of a {@link WorldOrchestrator}.
 * Provides clear state transitions and query methods.
 */
public enum OrchestratorState {
	/** Orchestrator is being initialized, not yet ready for ticks. */
	INITIALIZING,
	
	/** Orchestrator is running and processing ticks normally. */
	RUNNING,
	
	/** Orchestrator is in the process of shutting down. */
	SHUTTING_DOWN,
	
	/** Orchestrator has completed shutdown and should not be used. */
	SHUTDOWN;

	/** Returns true if this state allows tick processing. */
	public boolean canTick() {
		return this == RUNNING;
	}

	/** Returns true if this state is a terminal state. */
	public boolean isTerminal() {
		return this == SHUTDOWN;
	}

	/** Returns true if shutdown has been initiated or completed. */
	public boolean isShuttingDownOrShutdown() {
		return this == SHUTTING_DOWN || this == SHUTDOWN;
	}
}

