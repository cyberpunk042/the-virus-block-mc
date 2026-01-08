package net.cyberpunk042.infection.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Straight-forward in-memory registry used during the refactor bootstrap. Once
 * JSON-driven scenario profiles land we can replace this with a loader-backed
 * implementation, but keeping a concrete registry now lets us thread the
 * dependency through {@link net.cyberpunk042.infection.VirusWorldState}
 * immediately.
 */
public final class SimpleScenarioRegistry implements ScenarioRegistry {
	private final Map<Identifier, Supplier<? extends InfectionScenario>> factories = new HashMap<>();
	private final Map<RegistryKey<World>, Identifier> bindings = new HashMap<>();
	private final Map<RegistryKey<World>, InfectionScenario> cachedScenarios = new HashMap<>();
	private Identifier defaultScenarioId;

	@Override
	public void register(Identifier scenarioId, Supplier<? extends InfectionScenario> factory) {
		factories.put(scenarioId, factory);
		if (defaultScenarioId == null) {
			defaultScenarioId = scenarioId;
		}
	}

	@Override
	public void bind(RegistryKey<World> dimension, Identifier scenarioId) {
		if (!factories.containsKey(scenarioId)) {
			throw new IllegalArgumentException("Unknown infection scenario: " + scenarioId);
		}
		bindings.put(dimension, scenarioId);
		cachedScenarios.remove(dimension);
	}

	@Override
	public void unbind(RegistryKey<World> dimension) {
		bindings.remove(dimension);
		cachedScenarios.remove(dimension);
	}

	@Override
	public Optional<InfectionScenario> resolve(RegistryKey<World> dimension) {
		InfectionScenario cached = cachedScenarios.get(dimension);
		if (cached != null) {
			return Optional.of(cached);
		}
		Identifier scenarioId = bindings.getOrDefault(dimension, defaultScenarioId);
		if (scenarioId == null) {
			return Optional.empty();
		}
		Supplier<? extends InfectionScenario> factory = factories.get(scenarioId);
		if (factory == null) {
			return Optional.empty();
		}
		InfectionScenario created = factory.get();
		cachedScenarios.put(dimension, created);
		return Optional.of(created);
	}

	@Override
	public Optional<Identifier> binding(RegistryKey<World> dimension) {
		return Optional.ofNullable(bindings.get(dimension));
	}

	@Override
	public Optional<Identifier> defaultScenarioId() {
		return Optional.ofNullable(defaultScenarioId);
	}

	@Override
	public Set<Identifier> registeredScenarioIds() {
		return Set.copyOf(factories.keySet());
	}
}

