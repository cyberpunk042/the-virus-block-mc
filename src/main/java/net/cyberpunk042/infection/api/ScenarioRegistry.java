package net.cyberpunk042.infection.api;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Registry/factory facade that maps dimensions to scenario factories. The
 * host {@link net.cyberpunk042.infection.VirusWorldState} will look up the
 * resolved scenario once per world and delegate each tick without knowing which
 * implementation is active.
 */
public interface ScenarioRegistry {
	void register(Identifier scenarioId, Supplier<? extends InfectionScenario> factory);

	Optional<InfectionScenario> resolve(RegistryKey<World> dimension);

	void bind(RegistryKey<World> dimension, Identifier scenarioId);

	void unbind(RegistryKey<World> dimension);

	Optional<Identifier> binding(RegistryKey<World> dimension);

	Optional<Identifier> defaultScenarioId();

	Set<Identifier> registeredScenarioIds();
}

