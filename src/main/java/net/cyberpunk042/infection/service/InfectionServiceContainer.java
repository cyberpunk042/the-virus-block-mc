package net.cyberpunk042.infection.service;

import net.cyberpunk042.log.Logging;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.SimpleEffectBus;
import net.cyberpunk042.infection.api.VirusScheduler;
import net.cyberpunk042.infection.api.SimpleVirusScheduler;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.collapse.BufferedCollapseBroadcastManager;
import net.cyberpunk042.infection.api.ScenarioRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Central container for infection services. Instances are immutable and expose
 * factories for per-world components (effect bus, scheduler, broadcast manager)
 * so scenarios receive the dependencies via DI rather than static lookups.
 */
public final class InfectionServiceContainer {
	private final ConfigService config;
	private final ServiceConfig settings;
	private final WatchdogService watchdog;
	private final AlertingService alerting;
	private final EffectBusFactory effectBusFactory;
	private final Supplier<VirusScheduler> schedulerFactory;
	private final Supplier<ScenarioRegistry> scenarioRegistryFactory;
	private final Function<VirusWorldState, CollapseBroadcastManager> broadcastFactory;
	private final GrowthRegistry growthRegistry;
	private final SingularityHudService hud;
	private final SingularityTelemetryService telemetry;
	private final GuardianFxService guardianFx;
	private final GuardianSpawnService guardianSpawnService;
	private final SingularityPresentationService presentation;
	private final BiFunction<VirusWorldState, VirusSourceService.State, VirusSourceService> sourceFactory;

	private InfectionServiceContainer(Builder builder) {
		this.config = builder.config;
		this.settings = builder.settings;
		this.watchdog = builder.watchdog;
		this.alerting = builder.alerting;
		this.effectBusFactory = builder.effectBusFactory;
		this.schedulerFactory = builder.schedulerFactory;
		this.scenarioRegistryFactory = builder.scenarioRegistryFactory;
		this.broadcastFactory = builder.broadcastFactory;
		this.growthRegistry = builder.growthRegistry;
		this.hud = builder.hud;
		this.telemetry = builder.telemetry;
		this.guardianFx = builder.guardianFx;
		this.guardianSpawnService = builder.guardianSpawnService;
		this.presentation = new SingularityPresentationService(this.hud, this.guardianFx);
		this.sourceFactory = builder.sourceFactory;
	}

	public ConfigService config() {
		return config;
	}

	public ServiceConfig settings() {
		return settings;
	}

	public WatchdogService watchdog() {
		return watchdog;
	}

	public AlertingService alerting() {
		return alerting;
	}

	public EffectBus createEffectBus(VirusWorldState state) {
		return effectBusFactory.create(state);
	}

	public EffectBus createEffectBus() {
		return effectBusFactory.create(null);
	}

	public VirusScheduler createScheduler() {
		return schedulerFactory.get();
	}

	public ScenarioRegistry createScenarioRegistry() {
		return scenarioRegistryFactory.get();
	}

	public CollapseBroadcastManager createBroadcastManager(VirusWorldState state) {
		return broadcastFactory.apply(state);
	}

	public GrowthRegistry growth() {
		return growthRegistry;
	}

	public SingularityHudService hud() {
		return hud;
	}

	public SingularityTelemetryService telemetry() {
		return telemetry;
	}

	public GuardianFxService guardianFx() {
		return guardianFx;
	}

	public SingularityPresentationService presentation() {
		return presentation;
	}

	public GuardianSpawnService guardianSpawnService() {
		return guardianSpawnService;
	}

	public VirusSourceService createSourceService(VirusWorldState state, VirusSourceService.State sourceState) {
		return sourceFactory.apply(state, sourceState);
	}

	public static Builder builder(Path configDir) {
		return new Builder(configDir);
	}

	public static final class Builder {
		private final ConfigService config;
		private ServiceConfig settings;
		private WatchdogService watchdog;
		private AlertingService alerting;
		private EffectBusFactory effectBusFactory;
		private Supplier<VirusScheduler> schedulerFactory;
		private Supplier<ScenarioRegistry> scenarioRegistryFactory;
		private Function<VirusWorldState, CollapseBroadcastManager> broadcastFactory;
		private GrowthRegistry growthRegistry;
		private SingularityHudService hud;
		private SingularityTelemetryService telemetry;
		private GuardianFxService guardianFx;
		private BiFunction<VirusWorldState, VirusSourceService.State, VirusSourceService> sourceFactory;
		private GuardianSpawnService guardianSpawnService;

		private Builder(Path configDir) {
			this.config = new ConfigService(Objects.requireNonNull(configDir, "configDir"));
		}

		public Builder watchdog(WatchdogService watchdog) {
			this.watchdog = watchdog;
			return this;
		}

		public Builder alerting(AlertingService alerting) {
			this.alerting = alerting;
			return this;
		}

		public Builder effectBusFactory(Supplier<EffectBus> factory) {
			this.effectBusFactory = state -> factory.get();
			return this;
		}

		public Builder effectBusFactory(EffectBusFactory factory) {
			this.effectBusFactory = factory;
			return this;
		}

		public Builder schedulerFactory(Supplier<VirusScheduler> factory) {
			this.schedulerFactory = factory;
			return this;
		}

		public Builder scenarioRegistryFactory(Supplier<ScenarioRegistry> factory) {
			this.scenarioRegistryFactory = factory;
			return this;
		}

		public Builder broadcastFactory(Function<VirusWorldState, CollapseBroadcastManager> factory) {
			this.broadcastFactory = factory;
			return this;
		}

		public Builder hud(SingularityHudService hud) {
			this.hud = hud;
			return this;
		}

		public Builder telemetry(SingularityTelemetryService telemetry) {
			this.telemetry = telemetry;
			return this;
		}

		public Builder guardianFx(GuardianFxService guardianFx) {
			this.guardianFx = guardianFx;
			return this;
		}

		public Builder sourceFactory(BiFunction<VirusWorldState, VirusSourceService.State, VirusSourceService> factory) {
			this.sourceFactory = factory;
			return this;
		}

		public InfectionServiceContainer build() {
			if (settings == null) {
				settings = loadSettings();
			}
			if (settings.watchdog == null) {
				settings.watchdog = new ServiceConfig.Watchdog();
			}
			if (settings.watchdog.singularity == null) {
				settings.watchdog.singularity = new ServiceConfig.SingularityWatchdogs();
			}
			if (settings.watchdog.singularity.fuse == null) {
				settings.watchdog.singularity.fuse = new ServiceConfig.FuseWatchdog();
			}
			if (settings.watchdog.singularity.collapse == null) {
				settings.watchdog.singularity.collapse = new ServiceConfig.CollapsePhaseWatchdog();
			}
			if (settings.watchdog.scheduler == null) {
				settings.watchdog.scheduler = new ServiceConfig.Scheduler();
			}
			if (settings.effects == null) {
				settings.effects = new ServiceConfig.Effects();
			}
			if (settings.guardian == null) {
				settings.guardian = new ServiceConfig.Guardian();
			}
			if (settings.audio == null) {
				settings.audio = new ServiceConfig.Audio();
			}
			if (settings.singularity == null) {
				settings.singularity = new ServiceConfig.Singularity();
			}
			if (settings.singularity.postReset == null) {
				settings.singularity.postReset = new ServiceConfig.PostReset();
			}
			if (settings.singularity.execution == null) {
				settings.singularity.execution = new ServiceConfig.SingularityExecution();
			}
			if (settings.singularity.visuals == null) {
				settings.singularity.visuals = new ServiceConfig.Visuals();
			}
			if (settings.singularity.visuals.horizonDarkening == null) {
				settings.singularity.visuals.horizonDarkening = new ServiceConfig.HorizonDarkening();
			}
			if (settings.fuse == null) {
				settings.fuse = new ServiceConfig.Fuse();
			}
			if (settings.diagnostics == null) {
				settings.diagnostics = new ServiceConfig.Diagnostics();
			}
			if (settings.diagnostics.logSpam == null) {
				settings.diagnostics.logSpam = new ServiceConfig.LogSpamSettings();
			}
			AlertingService alertingService = alerting != null ? alerting : new AlertingService();
			WatchdogService watchdogService = watchdog != null ? watchdog : new WatchdogService(alertingService);
			EffectBusFactory effectBuses = effectBusFactory != null ? effectBusFactory
					: state -> new SimpleEffectBus(new EffectBusTelemetry(state));
			Supplier<VirusScheduler> schedulers = schedulerFactory != null ? schedulerFactory : SimpleVirusScheduler::new;
			ScenarioRegistryLoader scenarioLoader = new ScenarioRegistryLoader(config);
			Supplier<ScenarioRegistry> scenarios = scenarioRegistryFactory != null ? scenarioRegistryFactory : scenarioLoader::load;
			Function<VirusWorldState, CollapseBroadcastManager> broadcasts =
					broadcastFactory != null ? broadcastFactory : BufferedCollapseBroadcastManager::new;
			SingularityHudService hudService = hud != null ? hud : new SingularityHudService();
			SingularityTelemetryService telemetryService = telemetry != null ? telemetry
					: new SingularityTelemetryService();
			GuardianFxService guardianFxService = guardianFx != null ? guardianFx : new GuardianFxService();
			GuardianSpawnService guardianSpawnService = this.guardianSpawnService != null ? this.guardianSpawnService
					: new GuardianSpawnService();
			BiFunction<VirusWorldState, VirusSourceService.State, VirusSourceService> sourceFactory =
					this.sourceFactory != null
							? this.sourceFactory
							: (state, sourceState) -> new VirusSourceService(state, sourceState);
			if (growthRegistry == null) {
				growthRegistry = GrowthRegistry.load(config);
			}
			this.watchdog = watchdogService;
			this.alerting = alertingService;
			this.effectBusFactory = effectBuses;
			this.schedulerFactory = schedulers;
			this.scenarioRegistryFactory = scenarios;
			this.broadcastFactory = broadcasts;
			this.hud = hudService;
			this.telemetry = telemetryService;
			this.guardianFx = guardianFxService;
			this.guardianSpawnService = guardianSpawnService;
			this.sourceFactory = sourceFactory;
			return new InfectionServiceContainer(this);
		}
		private ServiceConfig loadSettings() {
			ServiceConfig loaded = config.readJson("services.json", ServiceConfig.class, ServiceConfig::defaults);
			if (!config.exists("services.json")) {
				config.writeJson("services.json", loaded);
			}
			if (loaded.watchdog == null) {
				loaded.watchdog = new ServiceConfig.Watchdog();
			}
			if (loaded.watchdog.singularity == null) {
				loaded.watchdog.singularity = new ServiceConfig.SingularityWatchdogs();
			}
			if (loaded.watchdog.singularity.fuse == null) {
				loaded.watchdog.singularity.fuse = new ServiceConfig.FuseWatchdog();
			}
			if (loaded.watchdog.singularity.collapse == null) {
				loaded.watchdog.singularity.collapse = new ServiceConfig.CollapsePhaseWatchdog();
			}
			if (loaded.watchdog.scheduler == null) {
				loaded.watchdog.scheduler = new ServiceConfig.Scheduler();
			}
			if (loaded.effects == null) {
				loaded.effects = new ServiceConfig.Effects();
			}
			if (loaded.guardian == null) {
				loaded.guardian = new ServiceConfig.Guardian();
			}
			if (loaded.audio == null) {
				loaded.audio = new ServiceConfig.Audio();
			}
			if (loaded.singularity == null) {
				loaded.singularity = new ServiceConfig.Singularity();
			}
			if (loaded.singularity.postReset == null) {
				loaded.singularity.postReset = new ServiceConfig.PostReset();
			}
			if (loaded.singularity.execution == null) {
				loaded.singularity.execution = new ServiceConfig.SingularityExecution();
			}
			if (loaded.singularity.visuals == null) {
				loaded.singularity.visuals = new ServiceConfig.Visuals();
			}
			if (loaded.singularity.visuals.horizonDarkening == null) {
				loaded.singularity.visuals.horizonDarkening = new ServiceConfig.HorizonDarkening();
			}
			if (loaded.fuse == null) {
				loaded.fuse = new ServiceConfig.Fuse();
			}
			if (loaded.diagnostics == null) {
				loaded.diagnostics = new ServiceConfig.Diagnostics();
			}
			if (loaded.diagnostics.logSpam == null) {
				loaded.diagnostics.logSpam = new ServiceConfig.LogSpamSettings();
			}
			return loaded;
		}

	}

	public interface EffectBusFactory {
		EffectBus create(@Nullable VirusWorldState state);
	}
}
