package net.cyberpunk042.infection.api;

import java.util.function.Consumer;

/**
 * Lightweight singleton used while the effect bus is under construction. This
 * keeps the host wiring simple without forcing references to {@code null}
 * sentinels all over {@link net.cyberpunk042.infection.VirusWorldState}.
 */
final class NoopEffectBus implements EffectBus {
	static final NoopEffectBus INSTANCE = new NoopEffectBus();

	private NoopEffectBus() {
	}

	@Override
	public <T> void register(Class<T> eventType, Consumer<T> handler) {
	}

	@Override
	public <T> void unregister(Class<T> eventType, Consumer<T> handler) {
	}

	@Override
	public <T> void post(T event) {
	}
}

