package net.cyberpunk042.infection.service;

import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.cyberpunk042.infection.api.InfectionScenario;
import net.cyberpunk042.infection.api.ScenarioRegistry;
import net.cyberpunk042.infection.api.SimpleScenarioRegistry;
import net.cyberpunk042.infection.scenario.NetherInfectionScenario;
import net.cyberpunk042.infection.scenario.OverworldInfectionScenario;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

final class ScenarioRegistryLoader {
    private static final String CONFIG_NAME = "scenarios.json";

    private final ConfigService configService;

    ScenarioRegistryLoader(ConfigService configService) {
        this.configService = configService;
    }

    ScenarioRegistry load() {
        ScenarioRegistryConfig config = configService.readJson(CONFIG_NAME,
                ScenarioRegistryConfig.class,
                ScenarioRegistryConfig::defaults);
        if (!configService.exists(CONFIG_NAME)) {
            configService.writeJson(CONFIG_NAME, config);
        }
        SimpleScenarioRegistry registry = new SimpleScenarioRegistry();
        registerScenarios(registry, config);
        if (registry.registeredScenarioIds().isEmpty()) {
            registerFallback(registry);
        }
        bindDimensions(registry, config);
        return registry;
    }

    private void registerFallback(SimpleScenarioRegistry registry) {
        registry.register(OverworldInfectionScenario.ID, OverworldInfectionScenario::new);
        registry.register(NetherInfectionScenario.ID, NetherInfectionScenario::new);
        registry.bind(World.NETHER, NetherInfectionScenario.ID);
        Logging.SCENARIO.topic("loader").warn("[scenario] using fallback registry (invalid config)");
    }

    private void registerScenarios(SimpleScenarioRegistry registry, ScenarioRegistryConfig config) {
        Map<String, Supplier<? extends InfectionScenario>> factories = builtInFactories();
        List<ScenarioRegistryConfig.ScenarioEntry> ordered = new ArrayList<>(config.scenarios);
        ordered.sort((a, b) -> {
            if (a == null || b == null) {
                return 0;
            }
            String defaultId = config.defaultScenario;
            if (defaultId == null) {
                return 0;
            }
            boolean aDefault = defaultId.equals(a.id);
            boolean bDefault = defaultId.equals(b.id);
            if (aDefault == bDefault) {
                return 0;
            }
            return aDefault ? -1 : 1;
        });
        for (ScenarioRegistryConfig.ScenarioEntry entry : ordered) {
            registerScenarioEntry(registry, entry, factories);
        }
    }

    private void registerScenarioEntry(SimpleScenarioRegistry registry,
            ScenarioRegistryConfig.ScenarioEntry entry,
            Map<String, Supplier<? extends InfectionScenario>> factories) {
        if (entry == null || entry.id == null || entry.factory == null) {
            return;
        }
        Identifier id = Identifier.tryParse(entry.id);
        if (id == null) {
            Logging.SCENARIO.topic("loader").warn("[scenario] invalid scenario id {}", entry.id);
            return;
        }
        Supplier<? extends InfectionScenario> factory = factories.get(entry.factory);
        if (factory == null) {
            Logging.SCENARIO.topic("loader").warn("[scenario] unknown factory '{}' for scenario {}",
                    entry.factory,
                    id);
            return;
        }
        registry.register(id, factory);
    }

    private void bindDimensions(SimpleScenarioRegistry registry, ScenarioRegistryConfig config) {
        Set<Identifier> registered = registry.registeredScenarioIds();
        for (ScenarioRegistryConfig.BindingEntry binding : config.bindings) {
            if (binding == null || binding.dimension == null || binding.scenario == null) {
                continue;
            }
            Identifier dimensionId = Identifier.tryParse(binding.dimension);
            if (dimensionId == null) {
                Logging.SCENARIO.topic("loader").warn("[scenario] invalid dimension {}", binding.dimension);
                continue;
            }
            Identifier scenarioId = Identifier.tryParse(binding.scenario);
            if (scenarioId == null) {
                Logging.SCENARIO.topic("loader").warn("[scenario] invalid scenario reference {}", binding.scenario);
                continue;
            }
            if (!registered.contains(scenarioId)) {
                Logging.SCENARIO.topic("loader").warn("[scenario] dimension {} references unregistered scenario {}",
                        dimensionId,
                        scenarioId);
                continue;
            }
            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
            registry.bind(dimension, scenarioId);
        }
    }

    private Map<String, Supplier<? extends InfectionScenario>> builtInFactories() {
        Map<String, Supplier<? extends InfectionScenario>> factories = new HashMap<>();
        factories.put("overworld", OverworldInfectionScenario::new);
        factories.put("nether", NetherInfectionScenario::new);
        return factories;
    }
}
