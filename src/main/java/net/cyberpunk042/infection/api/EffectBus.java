package net.cyberpunk042.infection.api;

import java.util.function.Consumer;

/**
 * Scenario-scoped observer hub. Controllers post infection/singularity events
 * and listeners (guardian beams, FX emitters, ambient audio, etc.) consume
 * them without coupling back to {@link net.cyberpunk042.infection.VirusWorldState}.
 * <p>
 * Milestone 1 only requires the interface so we can begin threading the
 * dependency through the host. Concrete implementations will arrive during the
 * EffectBus migration milestone.
 */
public interface EffectBus {
	<T> void register(Class<T> eventType, Consumer<T> handler);

	<T> void unregister(Class<T> eventType, Consumer<T> handler);

	<T> void post(T event);

	static EffectBus noop() {
		return NoopEffectBus.INSTANCE;
	}
}

