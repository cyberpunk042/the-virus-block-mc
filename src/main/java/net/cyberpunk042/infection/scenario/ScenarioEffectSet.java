package net.cyberpunk042.infection.scenario;

import net.cyberpunk042.infection.api.EffectBus;

/**
 * Minimal contract for scenario-specific effect bundles so they can be
 * installed/removed by a shared scenario base.
 */
public interface ScenarioEffectSet extends AutoCloseable {
	void install(EffectBus bus);

	@Override
	void close();
}

