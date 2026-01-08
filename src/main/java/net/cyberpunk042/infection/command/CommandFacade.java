package net.cyberpunk042.infection.command;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.ScenarioRegistry;
import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.profile.DimensionProfileRegistry;
import net.cyberpunk042.infection.profile.WaterDrainMode;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Facade that exposes high-level infection operations for the `/infection`
 * command suite. It sits between Brigadier command handlers and the
 * {@link VirusWorldState} host so commands never have to poke internal fields
 * directly.
 */
public final class CommandFacade {
	private final VirusWorldState state;

	public CommandFacade(VirusWorldState state) {
		this.state = Objects.requireNonNull(state, "state");
	}

	public Set<Identifier> registeredScenarioIds() {
		return registry().registeredScenarioIds();
	}

	public Optional<Identifier> boundScenario(RegistryKey<World> dimension) {
		return registry().binding(dimension);
	}

	public Optional<Identifier> defaultScenarioId() {
		return registry().defaultScenarioId();
	}

	public Optional<Identifier> effectiveScenario(ServerWorld world) {
		return boundScenario(world.getRegistryKey()).or(registry()::defaultScenarioId);
	}

	public Optional<Identifier> activeScenarioId() {
		return state.orchestrator().scenarios().activeId();
	}

	public boolean bindScenario(ServerWorld world, Identifier scenarioId) {
		if (!registry().registeredScenarioIds().contains(scenarioId)) {
			return false;
		}
		registry().bind(world.getRegistryKey(), scenarioId);
		state.orchestrator().scenarios().detach(world);
		return true;
	}

	public boolean unbindScenario(ServerWorld world) {
		RegistryKey<World> dimension = world.getRegistryKey();
		boolean hadBinding = registry().binding(dimension).isPresent();
		registry().unbind(dimension);
		state.orchestrator().scenarios().detach(world);
		return hadBinding;
	}

	public void reloadProfiles(ServerWorld world) {
		DimensionProfileRegistry.reload();
		state.orchestrator().scenarios().detach(world);
	}

	public boolean setCollapseViewDistance(ServerWorld world, int chunks) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseViewDistance(id, chunks));
	}

	public boolean setCollapseSimulationDistance(ServerWorld world, int chunks) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseSimulationDistance(id, chunks));
	}

	public boolean setCollapseBroadcastMode(ServerWorld world, CollapseBroadcastMode mode) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseBroadcastMode(id, mode));
	}

	public boolean setCollapseBroadcastRadius(ServerWorld world, int blocks) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseBroadcastRadius(id, blocks));
	}

	public boolean setCollapseDefaultProfile(ServerWorld world, CollapseSyncProfile profile) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseDefaultProfile(id, profile));
	}

	public boolean setWaterDrainMode(ServerWorld world, WaterDrainMode mode) {
		WaterDrainMode resolved = mode != null ? mode : WaterDrainMode.OFF;
		return withScenario(world, id -> DimensionProfileRegistry.setWaterDrainMode(id, resolved));
	}

	public boolean setWaterDrainOffset(ServerWorld world, int blocks) {
		return withScenario(world, id -> DimensionProfileRegistry.setWaterDrainOffset(id, blocks));
	}

	public boolean setCollapseParticles(ServerWorld world, boolean enabled) {
		return withScenario(world, id -> DimensionProfileRegistry.setCollapseParticles(id, enabled));
	}

	public boolean setFillMode(ServerWorld world, CollapseFillMode mode) {
		return withScenario(world, id -> DimensionProfileRegistry.setFillMode(id, mode));
	}

	public boolean setFillShape(ServerWorld world, CollapseFillShape shape) {
		return withScenario(world, id -> DimensionProfileRegistry.setFillShape(id, shape));
	}

	public boolean setOutlineThickness(ServerWorld world, int thickness) {
		return withScenario(world, id -> DimensionProfileRegistry.setOutlineThickness(id, thickness));
	}

	public boolean setUseNativeFill(ServerWorld world, boolean enabled) {
		return withScenario(world, id -> DimensionProfileRegistry.setUseNativeFill(id, enabled));
	}

	public boolean setRespectProtectedBlocks(ServerWorld world, boolean enabled) {
		return withScenario(world, id -> DimensionProfileRegistry.setRespectProtectedBlocks(id, enabled));
	}

	public boolean describeErosion(ServerWorld world, java.util.function.Consumer<DimensionProfile.Collapse> consumer) {
		return withScenario(world, id -> DimensionProfileRegistry.describeErosion(id, consumer));
	}

	public boolean setCollapseEnabled(boolean enabled) {
		return updateExecution(execution -> {
			if (execution.collapseEnabled == enabled) {
				return false;
			}
			execution.collapseEnabled = enabled;
			return true;
		});
	}

	public boolean setChunkGenerationAllowed(boolean enabled) {
		return updateExecution(execution -> {
			if (execution.allowChunkGeneration == enabled) {
				return false;
			}
			execution.allowChunkGeneration = enabled;
			return true;
		});
	}

	public boolean setOutsideBorderLoadAllowed(boolean enabled) {
		return updateExecution(execution -> {
			if (execution.allowOutsideBorderLoad == enabled) {
				return false;
			}
			execution.allowOutsideBorderLoad = enabled;
			return true;
		});
	}

	// Legacy methods removed: setMultithreadedCollapse, setCollapseExecutionMode, setCollapseWorkerCount
	// CollapseProcessor is now the only collapse system

	public boolean setDiagnosticsEnabled(boolean enabled) {
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.enabled == enabled) {
				return false;
			}
			diagnostics.enabled = enabled;
			return true;
		});
	}

	public boolean setDiagnosticsChunkSamples(boolean enabled) {
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logChunkSamples == enabled) {
				return false;
			}
			diagnostics.logChunkSamples = enabled;
			return true;
		});
	}

	public boolean setDiagnosticsBypasses(boolean enabled) {
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logBypasses == enabled) {
				return false;
			}
			diagnostics.logBypasses = enabled;
			return true;
		});
	}

	public boolean setDiagnosticsSampleInterval(int ticks) {
		int clamped = Math.max(1, ticks);
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logSampleIntervalTicks == clamped) {
				return false;
			}
			diagnostics.logSampleIntervalTicks = clamped;
			return true;
		});
	}

	public boolean setDiagnosticsSpamEnabled(boolean enabled) {
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logSpam.enableSpamDetection == enabled) {
				return false;
			}
			diagnostics.logSpam.enableSpamDetection = enabled;
			return true;
		});
	}

	public boolean setDiagnosticsSpamPerSecond(int threshold) {
		int clamped = Math.max(0, threshold);
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logSpam.perSecondThreshold == clamped) {
				return false;
			}
			diagnostics.logSpam.perSecondThreshold = clamped;
			return true;
		});
	}

	public boolean setDiagnosticsSpamPerMinute(int threshold) {
		int clamped = Math.max(0, threshold);
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logSpam.perMinuteThreshold == clamped) {
				return false;
			}
			diagnostics.logSpam.perMinuteThreshold = clamped;
			return true;
		});
	}

	public boolean setDiagnosticsSpamSuppress(boolean enabled) {
		return updateDiagnostics(diagnostics -> {
			if (diagnostics.logSpam.suppressWhenTriggered == enabled) {
				return false;
			}
			diagnostics.logSpam.suppressWhenTriggered = enabled;
			return true;
		});
	}

	private ScenarioRegistry registry() {
		return Objects.requireNonNull(state.orchestrator().scenarios().registry(), "scenarioRegistry");
	}

	private boolean withScenario(ServerWorld world, Function<Identifier, Boolean> updater) {
		Optional<Identifier> scenario = effectiveScenario(world);
		if (scenario.isEmpty()) {
			return false;
		}
		boolean updated = Boolean.TRUE.equals(updater.apply(scenario.get()));
		if (updated) {
			state.orchestrator().scenarios().detach(world);
		}
		return updated;
	}

	private boolean updateExecution(Function<ServiceConfig.SingularityExecution, Boolean> updater) {
		InfectionServiceContainer services = InfectionServices.container();
		if (services == null) {
			return false;
		}
		ServiceConfig settings = services.settings();
		if (settings == null) {
			return false;
		}
		if (settings.singularity == null) {
			settings.singularity = new ServiceConfig.Singularity();
		}
		if (settings.singularity.execution == null) {
			settings.singularity.execution = new ServiceConfig.SingularityExecution();
		}
		boolean changed = Boolean.TRUE.equals(updater.apply(settings.singularity.execution));
		if (changed) {
			services.config().writeJson("services.json", settings);
		}
		return true;
	}

	private boolean updateDiagnostics(Function<ServiceConfig.Diagnostics, Boolean> updater) {
		InfectionServiceContainer services = InfectionServices.container();
		if (services == null) {
			return false;
		}
		ServiceConfig settings = services.settings();
		if (settings == null) {
			return false;
		}
		if (settings.diagnostics == null) {
			settings.diagnostics = new ServiceConfig.Diagnostics();
		}
		if (settings.diagnostics.logSpam == null) {
			settings.diagnostics.logSpam = new ServiceConfig.LogSpamSettings();
		}
		boolean changed = Boolean.TRUE.equals(updater.apply(settings.diagnostics));
		if (changed) {
			services.config().writeJson("services.json", settings);
		}
		return true;
	}
}

