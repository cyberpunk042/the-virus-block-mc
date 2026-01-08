package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.List;

import net.cyberpunk042.infection.scenario.NetherInfectionScenario;
import net.cyberpunk042.infection.scenario.OverworldInfectionScenario;

final class ScenarioRegistryConfig {
	public String defaultScenario;
	public List<ScenarioEntry> scenarios = new ArrayList<>();
	public List<BindingEntry> bindings = new ArrayList<>();

	static ScenarioRegistryConfig defaults() {
		ScenarioRegistryConfig config = new ScenarioRegistryConfig();
		config.defaultScenario = OverworldInfectionScenario.ID.toString();
		config.scenarios.add(ScenarioEntry.of(OverworldInfectionScenario.ID.toString(), "overworld"));
		config.scenarios.add(ScenarioEntry.of(NetherInfectionScenario.ID.toString(), "nether"));
		config.bindings.add(BindingEntry.of("minecraft:overworld", OverworldInfectionScenario.ID.toString()));
		config.bindings.add(BindingEntry.of("minecraft:the_nether", NetherInfectionScenario.ID.toString()));
		return config;
	}

	static final class ScenarioEntry {
		public String id;
		public String factory;

		static ScenarioEntry of(String id, String factory) {
			ScenarioEntry entry = new ScenarioEntry();
			entry.id = id;
			entry.factory = factory;
			return entry;
		}
	}

	static final class BindingEntry {
		public String dimension;
		public String scenario;

		static BindingEntry of(String dimension, String scenario) {
			BindingEntry entry = new BindingEntry();
			entry.dimension = dimension;
			entry.scenario = scenario;
			return entry;
		}
	}
}

