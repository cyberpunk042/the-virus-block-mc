package net.cyberpunk042.infection;


import net.cyberpunk042.log.Logging;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.api.BasicSingularityContext;
import net.cyberpunk042.infection.api.BasicVirusWorldContext;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.cyberpunk042.infection.orchestrator.DefaultWorldOrchestrator;
import net.cyberpunk042.infection.orchestrator.IWorldCallbacks;
import net.cyberpunk042.infection.orchestrator.OrchestratorDependencies;
import net.cyberpunk042.infection.orchestrator.WorldOrchestrator;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.profile.DimensionProfileRegistry;
import net.cyberpunk042.infection.service.CollapseConfigurationService;
import net.cyberpunk042.infection.service.InfectionDisturbanceService;
import net.cyberpunk042.infection.service.InfectionLifecycleService;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.MatrixCubeControlService;
import net.cyberpunk042.infection.service.MatrixCubeSpawnService;
import net.cyberpunk042.infection.service.PresentationCoordinatorService;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.cyberpunk042.infection.service.ShieldFieldService;
import net.cyberpunk042.infection.service.SingularityLifecycleService;
import net.cyberpunk042.infection.service.SourceControlService;
import net.cyberpunk042.infection.service.TierProgressionService;
import net.cyberpunk042.infection.service.VirusSourceService;
import net.cyberpunk042.infection.state.CollapseModule;
import net.cyberpunk042.infection.state.CombatModule;
import net.cyberpunk042.infection.state.InfectionState;
import net.cyberpunk042.infection.state.ShellModule;
import net.cyberpunk042.infection.state.SingularityModule;
import net.cyberpunk042.infection.state.TierModule;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/**
 * Central state container for the virus infection system.
 * Manages all infection-related state and coordinates modules/services.
 */
public class VirusWorldState extends PersistentState {

	// ========== Constants ==========

	public static final String ID = TheVirusBlock.MOD_ID + "_infection_state";
	public static final PersistentStateType<VirusWorldState> TYPE = 
			new PersistentStateType<>(ID, VirusWorldState::new, VirusWorldPersistence.CODEC, DataFixTypes.LEVEL);

	// ========== Core State ==========

	/** Cached world reference - set at start of each tick */
	private ServerWorld currentWorld;

	private final InfectionState infectionState = new InfectionState();
	private final LongSet pillarChunks = new LongOpenHashSet();

	// ========== Modules ==========

	private final SingularityModule singularityModule = new SingularityModule(this);
	private final CollapseModule collapseModule = new CollapseModule(this);
	private final CombatModule combatModule = new CombatModule(this);
	private final ShieldFieldService shieldFieldService = new ShieldFieldService(this);
	private final ShellModule shellModule = new ShellModule(this, shieldFieldService);
	private final TierModule tierModule = new TierModule();

	// ========== Services ==========

	private final DefaultWorldOrchestrator orchestrator;
	private final CollapseConfigurationService collapseConfigService = new CollapseConfigurationService();
	private final MatrixCubeSpawnService matrixCubeService = new MatrixCubeSpawnService();
	private final MatrixCubeControlService matrixCubeControlService = new MatrixCubeControlService(this, matrixCubeService);
	private final PresentationCoordinatorService presentationCoordinator = new PresentationCoordinatorService(this);
	private final VirusSourceService.State sourceState = new VirusSourceService.State();

	@Nullable
	private transient VirusSourceService sourceService;

	// Services that depend on other fields (declared after their dependencies)
	private final TierProgressionService tierProgressionService;
	private final InfectionLifecycleService infectionLifecycleService = new InfectionLifecycleService(this);
	private final InfectionDisturbanceService infectionDisturbanceService = new InfectionDisturbanceService(this);
	private final SourceControlService sourceControlService;

	// ========== Operations ==========

	private final CollapseOperations collapseOperations;
	private final InfectionOperations infectionOperations;

	// ========== Cached Contexts (avoid allocation every tick) ==========
	
	private transient VirusWorldContext cachedVirusContext;
	private transient SingularityContext cachedSingularityContext;

	// ========== Instance Initializers ==========

	{
		tierModule.setHost(this);
		singularityState().singularityDebugLogCooldown = SingularityLifecycleService.DEFAULT_DIAGNOSTIC_SAMPLE_INTERVAL;
		singularityState().singularityRemovalLogCooldown = SingularityLifecycleService.DEFAULT_DIAGNOSTIC_SAMPLE_INTERVAL;
		singularity().chunkPreparationState().preloadLogCooldown = SingularityLifecycleService.PRELOAD_LOG_INTERVAL;
		singularity().chunkPreparationState().preGenLogCooldown = SingularityLifecycleService.PREGEN_LOG_INTERVAL;
		singularityState().singularityLayersPerSlice = SingularityLifecycleService.SINGULARITY_COLLAPSE_MIN_LAYERS_PER_SLICE;
		singularityState().singularityLastColumnsPerTick = -1;
		singularityState().singularityLastLayersPerSlice = -1;
	}

	// ========== Constructor ==========

	public VirusWorldState() {
		// Initialize services that need constructor-time setup
		this.tierProgressionService = new TierProgressionService(
				this, tierModule, shellModule.rebuildService(), shellModule.rebuildState(),
				shellModule.callbacks(), singularityModule.lifecycle(), pillarChunks);
		this.sourceControlService = new SourceControlService(
				this, shellModule.rebuildService(), shellModule.rebuildState(), sources(), sourceState);

		// Create dependencies for orchestrator (no circular reference!)
		OrchestratorDependencies deps = new OrchestratorDependencies(
				this.singularityModule,
				this::createVirusWorldContext,
				this::createSingularityContext,
				this::createCallbacks
		);
		this.orchestrator = new DefaultWorldOrchestrator(deps);

		// Install services from container
		InfectionServiceContainer services = InfectionServices.container();
		if (services != null) {
			orchestrator.installServices(
					services.createEffectBus(this),
					services.createBroadcastManager(this),
					services.createScenarioRegistry()
			);
			installSourceService(services.createSourceService(this, sourceState));
		}

		this.collapseOperations = new CollapseOperations(this, singularityModule.phase());
		this.infectionOperations = new InfectionOperations(
				this,
				infectionLifecycleService,
				combatModule.ambientPressure(),
				collapseModule.snapshot(),
				combatModule.voidTears());
	}

	// ========== Static Factory ==========

	public static VirusWorldState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE);
	}

	// ========== World Access ==========

	/**
	 * Returns the current world. Only valid during tick execution.
	 * Services should use this instead of receiving world as a parameter.
	 *
	 * @throws IllegalStateException if called before tick() has been called
	 */
	public ServerWorld world() {
		if (currentWorld == null) {
			throw new IllegalStateException(
					"world() called before tick() - this method is only valid during tick execution. " +
					"For block events, use the world parameter from the event instead.");
		}
		return currentWorld;
	}

	// ========== Module Accessors ==========

	public SingularityModule singularity() {
		return singularityModule;
	}

	public SingularityLifecycleService.State singularityState() {
		return singularityModule.state();
	}

	public CollapseModule collapseModule() {
		return collapseModule;
	}

	public CombatModule combat() {
		return combatModule;
	}

	public ShellModule shell() {
		return shellModule;
	}

	public TierModule tiers() {
		return tierModule;
	}

	public InfectionState infectionState() {
		return infectionState;
	}

	public LongSet pillarChunks() {
		return pillarChunks;
	}

	// ========== Service Accessors ==========

	public WorldOrchestrator orchestrator() {
		return orchestrator;
	}

	public CollapseConfigurationService collapseConfig() {
		return collapseConfigService;
	}

	public ShieldFieldService shieldFieldService() {
		return shieldFieldService;
	}

	public PresentationCoordinatorService presentationCoord() {
		return presentationCoordinator;
	}

	public SourceControlService sourceControl() {
		return sourceControlService;
	}

	public TierProgressionService tierProgression() {
		return tierProgressionService;
	}

	public InfectionDisturbanceService disturbance() {
		return infectionDisturbanceService;
	}

	public InfectionLifecycleService infectionLifecycle() {
		return infectionLifecycleService;
	}

	public VirusSourceService.State sourceState() {
		return sourceState;
	}

	public VirusSourceService sources() {
		if (sourceService == null) {
			InfectionServiceContainer container = InfectionServices.container();
			VirusSourceService service = container != null
					? container.createSourceService(this, sourceState)
					: null;
			installSourceService(service);
		}
		return sourceService;
	}

	public void installSourceService(@Nullable VirusSourceService service) {
		this.sourceService = service != null 
				? service 
				: new VirusSourceService(this, sourceState);
	}

	// ========== Operations Accessors ==========

	public CollapseOperations collapse() {
		return collapseOperations;
	}

	public InfectionOperations infection() {
		return infectionOperations;
	}

	// ========== Lifecycle Methods ==========

	public void tick(ServerWorld world) {
		this.currentWorld = world;  // Cache for service access via host.world()
		orchestrator.tick(world);
		// Tick phases with state access (orchestrator calls back to us)
		orchestrator.tickPhases(world, this);
	}

	public void onWorldLoad(ServerWorld world) {
		this.currentWorld = world;  // Set early so services can use world() during load
		collapseConfigService.ensureSingularityGamerules(world);
		if (singularityModule.borderState().active || singularityModule.borderState().hasSnapshot) {
			singularity().phase().restoreSingularityBorder();
		}
		if (singularityState().singularityState == SingularityState.DORMANT) {
			singularityModule.phase().resetSingularityPreparation("worldLoad");
		}
	}

	// ========== State Mutation Methods ==========

	public void setDifficulty(ServerWorld world, VirusDifficulty newDifficulty) {
		if (!tiers().setDifficulty(newDifficulty)) {
			return;
		}
		markDirty();
		infectionOperations.applyDifficultyRules(world, tiers().currentTier());
		presentationCoordinator.syncDifficulty();
	}

	public void applyDimensionProfile(DimensionProfile profile) {
		if (profile == null) {
			return;
		}
		collapseConfigService.applyDimensionProfile(profile);
		collapseModule().queues().clearPreCollapseDrainageJob();
		markDirty();
	}

	// ========== Query Methods ==========

	public Set<BlockPos> getVirusSources() {
		return sources().view(sourceState);
	}

	public boolean hasVirusSources() {
		return !sources().isEmpty(sourceState);
	}

	// ========== Internal Helpers ==========

	/** Attempts to spawn a matrix cube. Internal helper for InfectionOperations. */
	boolean trySpawnMatrixCube() {
		return matrixCubeControlService.maybeSpawnMatrixCube();
	}

	// ========== Context Factories for Orchestrator ==========

	/**
	 * Gets or creates the cached VirusWorldContext.
	 * Context is created once and reused to avoid allocation every tick.
	 * The world reference is updated via tick() which sets currentWorld.
	 */
	private VirusWorldContext createVirusWorldContext(ServerWorld world) {
		if (cachedVirusContext == null || cachedVirusContext.world() != world) {
			cachedVirusContext = new BasicVirusWorldContext(
					world,
					this,
					orchestrator.effectBusOrNoop(),
					orchestrator.schedulerOrNoop(),
					getOrCreateSingularityContext(world)
			);
		}
		return cachedVirusContext;
	}

	/**
	 * Gets or creates the cached SingularityContext.
	 * Context is created once and reused to avoid allocation every tick.
	 */
	public SingularityContext createSingularityContext(ServerWorld world) {
		return getOrCreateSingularityContext(world);
	}
	
	private SingularityContext getOrCreateSingularityContext(ServerWorld world) {
		if (cachedSingularityContext == null || cachedSingularityContext.world() != world) {
			net.minecraft.util.Identifier scenarioId = orchestrator.getAttachedScenarioId();
			DimensionProfile profile = scenarioId != null
					? DimensionProfileRegistry.resolve(scenarioId)
					: DimensionProfile.defaults();
			InfectionServiceContainer services = InfectionServices.container();
			cachedSingularityContext = new BasicSingularityContext(
					world,
					this,
					orchestrator.effectBusOrNoop(),
					orchestrator.schedulerOrNoop(),
					orchestrator.collapseBroadcastManagerOrNoop(),
					profile,
					services
			);
		}
		return cachedSingularityContext;
	}
	
	/**
	 * Invalidates cached contexts. Call when scenario changes or services are reinstalled.
	 */
	public void invalidateContexts() {
		cachedVirusContext = null;
		cachedSingularityContext = null;
	}

	private IWorldCallbacks createCallbacks(OrchestratorDependencies.ServiceAccessor serviceAccessor) {
		return new WorldCallbacks(this, serviceAccessor);
	}

	// ========== Inner Callbacks Class ==========

	private static class WorldCallbacks implements IWorldCallbacks {
		private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;
		private final VirusWorldState host;
		private final OrchestratorDependencies.ServiceAccessor services;

		WorldCallbacks(VirusWorldState host, OrchestratorDependencies.ServiceAccessor services) {
			this.host = host;
			this.services = services;
		}

		@Override
		public <T> T query(Function<VirusWorldState, T> extractor) {
			try {
				return extractor.apply(host);
			} catch (Exception e) {
				Logging.INFECTION.error("[Callbacks] Query failed", e);
				return null;
			}
		}

		@Override
		public boolean mutate(Consumer<VirusWorldState> mutation) {
			try {
				mutation.accept(host);
				return true;
			} catch (Exception e) {
				Logging.INFECTION.error("[Callbacks] Mutation failed", e);
				return false;
			}
		}

		@Override
		public void broadcast(ServerWorld world, Text message) {
			world.getPlayers().forEach(p -> p.sendMessage(message, false));
		}

		@Override
		public UUID schedule(Runnable task, int delayTicks) {
			UUID id = UUID.randomUUID();
			services.schedulerOrNoop().schedule(delayTicks, task);
			return id;
		}

		@Override
		public void cancel(UUID taskId) {
			Logging.INFECTION.debug("[Callbacks] Cancel requested for task {} - not supported", taskId);
		}

		@Override
		public ServiceConfig.PostReset postResetConfig() {
			InfectionServiceContainer c = InfectionServices.container();
			return c != null ? c.settings().singularity.postReset : new ServiceConfig.PostReset();
		}
	}
}
