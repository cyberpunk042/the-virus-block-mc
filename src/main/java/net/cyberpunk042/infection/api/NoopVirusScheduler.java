package net.cyberpunk042.infection.api;

/**
 * Default scheduler that immediately discards every request. The concrete
 * scheduler implementation will land with the scheduler milestone; until then
 * we keep the host wiring simple and deterministic by exposing this singleton.
 */
final class NoopVirusScheduler implements VirusScheduler {
	static final NoopVirusScheduler INSTANCE = new NoopVirusScheduler();

	private NoopVirusScheduler() {
	}

	@Override
	public void schedule(int delayTicks, Runnable task) {
	}

	@Override
	public void tick() {
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public java.util.List<? extends PersistedTaskSnapshot> snapshot() {
		return java.util.List.of();
	}

	@Override
	public void loadSnapshot(java.util.List<? extends PersistedTaskSnapshot> snapshots) {
	}
}

