package net.cyberpunk042.infection.service;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.infection.api.SimpleVirusScheduler;
import net.cyberpunk042.infection.api.VirusScheduler;
import net.cyberpunk042.infection.api.VirusSchedulerMetrics;
/**
 * Hosts the per-world scheduler instance. Persists tasks via a fallback
 * {@link SimpleVirusScheduler} but allows DI to swap in alternative
 * implementations without leaking details back into {@code VirusWorldState}.
 */
public final class VirusSchedulerService {
	private final SimpleVirusScheduler fallback = new SimpleVirusScheduler();
	private VirusScheduler active = fallback;

	public VirusScheduler scheduler() {
		return active;
	}

	public void install(@Nullable VirusScheduler scheduler) {
		if (scheduler == null) {
			active = fallback;
			return;
		}
		List<? extends VirusScheduler.PersistedTaskSnapshot> pending = fallback.snapshot();
		active = scheduler;
		if (!pending.isEmpty()) {
			scheduler.loadSnapshot(pending);
		}
	}

	public void tick() {
		active.tick();
	}

	public List<SimpleVirusScheduler.TaskSnapshot> snapshot() {
		return fallback.snapshot();
	}

	public void loadSnapshot(List<SimpleVirusScheduler.TaskSnapshot> snapshots) {
		fallback.clear();
		fallback.loadSnapshot(snapshots);
	}

	public int backlogSize() {
		if (active instanceof VirusSchedulerMetrics metrics) {
			return Math.max(0, metrics.pendingTasks());
		}
		return Math.max(0, fallback.pendingTasks());
	}

	public boolean usingFallback() {
		return active == fallback;
	}

	public SchedulerDiagnostics diagnostics() {
		String implementation = active.getClass().getSimpleName();
		boolean usingFallback = active == fallback;
		int backlog = backlogSize();
		List<SimpleVirusScheduler.TaskSnapshot> tasks = snapshot();
		return new SchedulerDiagnostics(implementation, usingFallback, backlog, tasks);
	}

	public record SchedulerDiagnostics(
			String implementation,
			boolean usingFallback,
			int backlog,
			List<SimpleVirusScheduler.TaskSnapshot> persistedTasks) {
	}
}

