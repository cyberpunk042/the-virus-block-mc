package net.cyberpunk042.infection.api;

/**
 * Optional diagnostics surface for {@link VirusScheduler} implementations.
 * Schedulers that expose their queue depth allow the host to hook watchdogs
 * without knowing the concrete implementation details.
 */
public interface VirusSchedulerMetrics {
	/**
	 * @return the number of scheduled tasks that have yet to execute.
	 */
	int pendingTasks();
}


