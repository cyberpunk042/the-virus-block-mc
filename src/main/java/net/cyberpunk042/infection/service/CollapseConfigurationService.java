package net.cyberpunk042.infection.service;

import java.util.List;

import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillProfile;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.profile.WaterDrainMode;
import net.cyberpunk042.infection.singularity.CollapseErosionSettings;
import net.cyberpunk042.infection.singularity.SingularityExecutionSettings;
import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;

/**
 * Manages collapse-related configuration state extracted from VirusWorldState.
 * Holds mutable config fields loaded from DimensionProfile and provides typed accessors.
 */
public final class CollapseConfigurationService {

	// ─────────────────────────────────────────────────────────────────────────────
	// Static defaults (previously in VirusWorldState)
	// ─────────────────────────────────────────────────────────────────────────────

	private static final DimensionProfile.Collapse DEFAULT_COLLAPSE_PROFILE = DimensionProfile.Collapse.defaults();
	private static final int SINGULARITY_RING_COLUMNS_PER_TICK = 64;
	private static final double SINGULARITY_RING_PULL_RADIUS = 24.0D;
	private static final double SINGULARITY_RING_PULL_STRENGTH = 0.35D;
	private static final int SINGULARITY_RING_START_DELAY = 40;
	private static final int SINGULARITY_RING_DURATION = 200;
	private static final double HEAVY_PANTS_VOID_TEAR_WEAR = 4.0D / 3.0D;
	
	// Singularity timing constants
	private static final int SINGULARITY_CORE_CHARGE_TICKS = 80;
	private static final int SINGULARITY_RESET_DELAY_TICKS = 160;
	private static final int SINGULARITY_COLLAPSE_BAR_DELAY_TICKS = 60;
	private static final int SINGULARITY_COLLAPSE_COMPLETE_HOLD_TICKS = 40;
	
	// Default service configs
	private static final ServiceConfig.Singularity DEFAULT_SINGULARITY_SERVICE_CONFIG = new ServiceConfig.Singularity();
	private static final ServiceConfig.PostReset DEFAULT_POST_RESET_CONFIG = new ServiceConfig.PostReset();
	private static final ServiceConfig.Fuse DEFAULT_FUSE_CONFIG = new ServiceConfig.Fuse();

	// ─────────────────────────────────────────────────────────────────────────────
	// Mutable config state (loaded from DimensionProfile)
	// ─────────────────────────────────────────────────────────────────────────────

	private int ringColumnsPerTickConfig = SINGULARITY_RING_COLUMNS_PER_TICK;
	private double ringPullRadiusConfig = SINGULARITY_RING_PULL_RADIUS;
	private double ringPullStrengthConfig = SINGULARITY_RING_PULL_STRENGTH;
	private int ringStartDelayConfig = SINGULARITY_RING_START_DELAY;
	private int ringDurationConfig = SINGULARITY_RING_DURATION;

	private double barrierStartRadiusConfig = DEFAULT_COLLAPSE_PROFILE.barrierStartRadius();
	private double barrierEndRadiusConfig = DEFAULT_COLLAPSE_PROFILE.barrierEndRadius();
	private long barrierDurationTicksConfig = DEFAULT_COLLAPSE_PROFILE.barrierDurationTicks();
	private boolean barrierAutoResetEnabled = DEFAULT_COLLAPSE_PROFILE.barrierAutoReset();
	private long barrierResetDelayTicksConfig = DEFAULT_COLLAPSE_PROFILE.barrierResetDelayTicks();

	private boolean chunkPreGenEnabledConfig = DEFAULT_COLLAPSE_PROFILE.chunkPreGenEnabled();
	private int chunkPreGenRadiusBlocksConfig = DEFAULT_COLLAPSE_PROFILE.chunkPreGenRadiusBlocks();
	private int chunkPreGenChunksPerTickConfig = DEFAULT_COLLAPSE_PROFILE.chunkPreGenChunksPerTick();
	private boolean chunkPreloadEnabledConfig = DEFAULT_COLLAPSE_PROFILE.chunkPreloadEnabled();
	private int chunkPreloadChunksPerTickConfig = DEFAULT_COLLAPSE_PROFILE.chunkPreloadChunksPerTick();

	private WaterDrainMode waterDrainModeConfig = DEFAULT_COLLAPSE_PROFILE.waterDrainMode();
	private int waterDrainOffsetConfig = DEFAULT_COLLAPSE_PROFILE.waterDrainOffset();
	private DimensionProfile.Collapse.WaterDrainDeferred waterDrainDeferredConfig = DEFAULT_COLLAPSE_PROFILE.waterDrainDeferred();
	private boolean collapseParticlesConfig = DEFAULT_COLLAPSE_PROFILE.collapseParticles();
	private CollapseFillProfile fillProfileConfig = CollapseFillProfile.DEFAULT;
	private CollapseFillMode fillModeConfig = DEFAULT_COLLAPSE_PROFILE.fillMode();
	private CollapseFillShape fillShapeConfig = DEFAULT_COLLAPSE_PROFILE.fillShape();
	private int outlineThicknessConfig = DEFAULT_COLLAPSE_PROFILE.outlineThickness();
	private boolean useNativeFillConfig = DEFAULT_COLLAPSE_PROFILE.useNativeFill();
	private boolean respectProtectedBlocksConfig = DEFAULT_COLLAPSE_PROFILE.respectProtectedBlocks();
	private int maxOperationsPerTickConfig = CollapseErosionSettings.DEFAULT_MAX_OPERATIONS_PER_TICK;
	private boolean collapseInwardConfig = true;
	private DimensionProfile.Collapse.PreCollapseWaterDrainage preCollapseWaterDrainageConfig = DEFAULT_COLLAPSE_PROFILE.preCollapseWaterDrainage();

	private int collapseViewDistanceChunksConfig = DEFAULT_COLLAPSE_PROFILE.viewDistanceChunks();
	private int collapseSimulationDistanceChunksConfig = DEFAULT_COLLAPSE_PROFILE.simulationDistanceChunks();
	private CollapseBroadcastMode collapseBroadcastModeConfig = DEFAULT_COLLAPSE_PROFILE.broadcastMode();
	private int collapseBroadcastRadiusBlocksConfig = DEFAULT_COLLAPSE_PROFILE.broadcastRadiusBlocks();
	private CollapseSyncProfile defaultSyncProfileConfig = DEFAULT_COLLAPSE_PROFILE.defaultSyncProfile();
	private List<DimensionProfile.Collapse.RadiusDelay> collapseRadiusDelaysConfig = DEFAULT_COLLAPSE_PROFILE.radiusDelays();

	// Computed cache
	private CollapseErosionSettings erosionSettings;

	// ─────────────────────────────────────────────────────────────────────────────
	// Constructor
	// ─────────────────────────────────────────────────────────────────────────────

	public CollapseConfigurationService() {
		refreshErosionSettingsCache();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Profile application
	// ─────────────────────────────────────────────────────────────────────────────

	/**
	 * Apply values from a dimension profile, updating all config fields.
	 * 
	 * @param profile the dimension profile to apply (may be null, in which case nothing happens)
	 */
	public void applyDimensionProfile(DimensionProfile profile) {
		if (profile == null) {
			return;
		}
		DimensionProfile.Collapse collapse = profile.collapse();
		DimensionProfile.Physics physics = profile.physics();
		System.out.println("[CollapseConfig] Applying profile " + profile.id()
				+ " fillShape=" + collapse.fillShape()
				+ " thickness=" + collapse.outlineThickness()
				+ " useNativeFill=" + collapse.useNativeFill());

		// Use profile's columnsPerTick as default if not explicitly set
		CollapseFillProfile baseProfile = collapse.fillProfile();
		int configuredColumns = collapse.columnsPerTick();
		this.ringColumnsPerTickConfig = Math.max(1, configuredColumns > 0 ? configuredColumns : baseProfile.columnsPerTick());
		this.ringPullRadiusConfig = Math.max(1.0D, physics.pushRadius());
		this.ringPullStrengthConfig = Math.max(0.01D, physics.ringPullStrength());
		this.ringStartDelayConfig = Math.max(1, collapse.ringStartDelayTicks());
		this.ringDurationConfig = Math.max(1, collapse.ringDurationTicks());

		this.barrierStartRadiusConfig = Math.max(1.0D, collapse.barrierStartRadius());
		this.barrierEndRadiusConfig = MathHelper.clamp(collapse.barrierEndRadius(), 0.5D, barrierStartRadiusConfig);
		this.barrierDurationTicksConfig = Math.max(20L, collapse.barrierDurationTicks());
		this.barrierAutoResetEnabled = collapse.barrierAutoReset();
		this.barrierResetDelayTicksConfig = Math.max(0L, collapse.barrierResetDelayTicks());

		this.chunkPreGenEnabledConfig = collapse.chunkPreGenEnabled();
		this.chunkPreGenRadiusBlocksConfig = Math.max(0, collapse.chunkPreGenRadiusBlocks());
		this.chunkPreGenChunksPerTickConfig = Math.max(1, collapse.chunkPreGenChunksPerTick());
		this.chunkPreloadEnabledConfig = collapse.chunkPreloadEnabled();
		this.chunkPreloadChunksPerTickConfig = Math.max(1, collapse.chunkPreloadChunksPerTick());

		this.waterDrainModeConfig = collapse.waterDrainMode();
		this.waterDrainOffsetConfig = Math.max(0, collapse.waterDrainOffset());
		this.waterDrainDeferredConfig = collapse.waterDrainDeferred();
		this.collapseParticlesConfig = collapse.collapseParticles();
		
		// Use fill profile as base, allow explicit overrides
		CollapseFillProfile fillProfile = collapse.fillProfile();
		this.fillProfileConfig = fillProfile;
		this.fillModeConfig = collapse.fillMode();
		this.fillShapeConfig = collapse.fillShape() != null ? collapse.fillShape() : fillProfile.shape();
		this.outlineThicknessConfig = Math.max(1, collapse.outlineThickness() > 0 ? collapse.outlineThickness() : fillProfile.thickness());
		this.useNativeFillConfig = collapse.useNativeFill();
		System.out.println("[CollapseConfig] -> effective fillShape=" + this.fillShapeConfig
				+ " thickness=" + this.outlineThicknessConfig
				+ " useNativeFill=" + this.useNativeFillConfig);
		this.respectProtectedBlocksConfig = collapse.respectProtectedBlocks();
		this.maxOperationsPerTickConfig = Math.max(1, collapse.maxOperationsPerTick() > 0 ? collapse.maxOperationsPerTick() : fillProfile.maxOperationsPerTick());
		this.collapseInwardConfig = collapse.collapseInward();
		this.preCollapseWaterDrainageConfig = collapse.preCollapseWaterDrainage();

		this.collapseViewDistanceChunksConfig = Math.max(0, collapse.viewDistanceChunks());
		this.collapseSimulationDistanceChunksConfig = Math.max(0, collapse.simulationDistanceChunks());
		this.collapseBroadcastModeConfig = collapse.broadcastMode();
		this.collapseBroadcastRadiusBlocksConfig = Math.max(0, collapse.broadcastRadiusBlocks());
		this.defaultSyncProfileConfig = collapse.defaultSyncProfile();
		this.collapseRadiusDelaysConfig = collapse.radiusDelays();

		refreshErosionSettingsCache();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Barrier / Collapse radius
	// ─────────────────────────────────────────────────────────────────────────────

	public int configuredCollapseMaxRadius() {
		return Math.max(16, (int) Math.round(barrierStartRadiusConfig));
	}

	public double configuredBarrierStartRadius() {
		return barrierStartRadiusConfig;
	}

	public double configuredBarrierEndRadius() {
		return barrierEndRadiusConfig;
	}

	public long configuredBorderDurationTicks() {
		return Math.max(20L, barrierDurationTicksConfig);
	}

	public boolean barrierAutoResetEnabled() {
		return barrierAutoResetEnabled;
	}

	public long barrierResetDelayTicks() {
		return Math.max(0L, barrierResetDelayTicksConfig);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Pre-gen / Pre-load
	// ─────────────────────────────────────────────────────────────────────────────

	public int configuredPregenRadiusChunks() {
		return Math.max(0, MathHelper.ceil(configuredPreGenRadiusBlocks() / 16.0D));
	}

	public double configuredPreGenRadiusBlocks() {
		int configured = chunkPreGenRadiusBlocksConfig;
		return configured > 0 ? configured : configuredBarrierStartRadius();
	}

	public boolean configuredPreGenEnabled() {
		return chunkPreGenEnabledConfig;
	}

	public boolean configuredPreloadEnabled() {
		return chunkPreloadEnabledConfig;
	}

	public int configuredPreGenChunksPerTick() {
		return chunkPreGenChunksPerTickConfig;
	}

	public int configuredPreloadChunksPerTick() {
		return chunkPreloadChunksPerTickConfig;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Execution mode
	// ─────────────────────────────────────────────────────────────────────────────

	public int configuredCollapseTickInterval() {
		List<DimensionProfile.Collapse.RadiusDelay> delays = configuredRadiusDelays();
		if (delays.isEmpty()) {
			return 1;
		}
		return Math.max(1, delays.get(0).ticks());
	}

	// Legacy methods removed: configuredExecutionMode, configuredUseRingSliceMode, configuredMultithreadedCollapse
	// CollapseProcessor is now the only collapse system

	public boolean configuredCollapseEnabled() {
		return SingularityExecutionSettings.collapseEnabled();
	}

	public boolean configuredCollapseEnabled(ServerWorld world) {
		return SingularityExecutionSettings.collapseEnabled(world);
	}

	public boolean configuredChunkGenerationAllowed() {
		return SingularityExecutionSettings.allowChunkGeneration();
	}

	public boolean configuredChunkGenerationAllowed(ServerWorld world) {
		return SingularityExecutionSettings.allowChunkGeneration(world);
	}

	public boolean configuredOutsideBorderAllowed() {
		return SingularityExecutionSettings.allowOutsideBorderLoad();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// View / Simulation distance & broadcast
	// ─────────────────────────────────────────────────────────────────────────────

	public int configuredCollapseViewDistanceChunks() {
		return Math.max(0, collapseViewDistanceChunksConfig);
	}

	public int configuredCollapseSimulationDistanceChunks() {
		return Math.max(0, collapseSimulationDistanceChunksConfig);
	}

	public CollapseBroadcastMode configuredCollapseBroadcastMode() {
		return collapseBroadcastModeConfig != null ? collapseBroadcastModeConfig : CollapseBroadcastMode.defaultMode();
	}

	public int configuredCollapseBroadcastRadius() {
		return Math.max(0, collapseBroadcastRadiusBlocksConfig);
	}

	public CollapseSyncProfile configuredDefaultSyncProfile() {
		return defaultSyncProfileConfig != null ? defaultSyncProfileConfig : CollapseSyncProfile.defaultProfile();
	}

	public List<DimensionProfile.Collapse.RadiusDelay> configuredRadiusDelays() {
		return collapseRadiusDelaysConfig != null ? collapseRadiusDelaysConfig : DEFAULT_COLLAPSE_PROFILE.radiusDelays();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Water drain / Erosion
	// ─────────────────────────────────────────────────────────────────────────────

	public WaterDrainMode configuredWaterDrainMode() {
		return waterDrainModeConfig != null ? waterDrainModeConfig : WaterDrainMode.OFF;
	}

	public int configuredWaterDrainOffset() {
		return Math.max(0, waterDrainOffsetConfig);
	}

	public boolean collapseParticlesEnabled() {
		return collapseParticlesConfig;
	}

	public CollapseFillMode configuredFillMode() {
		return fillModeConfig != null ? fillModeConfig : CollapseFillMode.AIR;
	}

	public CollapseFillShape configuredFillShape() {
		return fillShapeConfig != null ? fillShapeConfig : CollapseFillShape.OUTLINE;
	}

	public int configuredOutlineThickness() {
		return Math.max(1, outlineThicknessConfig);
	}

	public boolean configuredUseNativeFill() {
		return useNativeFillConfig;
	}

	public boolean configuredRespectProtectedBlocks() {
		return respectProtectedBlocksConfig;
	}

	public int configuredMaxOperationsPerTick() {
		return maxOperationsPerTickConfig;
	}

	public boolean configuredCollapseInward() {
		return collapseInwardConfig;
	}

	public DimensionProfile.Collapse.PreCollapseWaterDrainage preCollapseWaterDrainageConfig() {
		return preCollapseWaterDrainageConfig;
	}

	public CollapseErosionSettings erosionSettings() {
		return erosionSettings;
	}

	public void refreshErosionSettingsCache() {
		DimensionProfile.Collapse.WaterDrainDeferred deferred = waterDrainDeferredConfig != null
				? waterDrainDeferredConfig
				: DimensionProfile.Collapse.WaterDrainDeferred.defaults();
		this.erosionSettings = new CollapseErosionSettings(
				configuredWaterDrainMode(),
				configuredWaterDrainOffset(),
				new CollapseErosionSettings.WaterDrainDeferredSettings(deferred.enabled(),
						deferred.initialDelayTicks(),
						deferred.columnsPerTick()),
				collapseParticlesEnabled(),
				configuredFillMode(),
				configuredFillShape(),
				configuredOutlineThickness(),
				configuredUseNativeFill(),
				configuredRespectProtectedBlocks(),
				configuredMaxOperationsPerTick());
		System.out.println("[CollapseConfig] Cached erosion settings: shape=" + configuredFillShape()
				+ " thickness=" + configuredOutlineThickness()
				+ " useNativeFill=" + configuredUseNativeFill());
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Ring config
	// ─────────────────────────────────────────────────────────────────────────────

	public int getRingStartDelayTicks() {
		return ringStartDelayConfig;
	}

	public int getRingDurationTicks() {
		return ringDurationConfig;
	}

	public int getRingColumnsPerTick() {
		return ringColumnsPerTickConfig;
	}

	public CollapseFillProfile configuredFillProfile() {
		return fillProfileConfig;
	}

	public double getRingPullRadius() {
		return ringPullRadiusConfig;
	}

	public double getRingPullStrength() {
		return ringPullStrengthConfig;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Misc
	// ─────────────────────────────────────────────────────────────────────────────

	public double heavyPantsVoidTearWear() {
		return HEAVY_PANTS_VOID_TEAR_WEAR;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Singularity timing config (extracted from VirusWorldState)
	// ─────────────────────────────────────────────────────────────────────────────

	private ServiceConfig.Singularity singularityConfig() {
		InfectionServiceContainer services = InfectionServices.container();
		ServiceConfig settings = services != null ? services.settings() : null;
		return settings != null && settings.singularity != null ? settings.singularity : DEFAULT_SINGULARITY_SERVICE_CONFIG;
	}

	public ServiceConfig.PostReset postResetConfig() {
		ServiceConfig.Singularity singularity = singularityConfig();
		ServiceConfig.PostReset config = singularity.postReset;
		return config != null ? config : DEFAULT_POST_RESET_CONFIG;
	}

	public int configuredCollapseBarDelay() {
		int configured = singularityConfig().collapseBarDelayTicks;
		return configured >= 0 ? configured : SINGULARITY_COLLAPSE_BAR_DELAY_TICKS;
	}

	public int configuredCollapseCompleteHold() {
		int configured = singularityConfig().collapseCompleteHoldTicks;
		return configured >= 0 ? configured : SINGULARITY_COLLAPSE_COMPLETE_HOLD_TICKS;
	}

	public int configuredCoreChargeTicks() {
		int configured = singularityConfig().coreChargeTicks;
		return configured > 0 ? configured : SINGULARITY_CORE_CHARGE_TICKS;
	}

	public int configuredResetDelayTicks() {
		int configured = singularityConfig().resetDelayTicks;
		return configured > 0 ? configured : SINGULARITY_RESET_DELAY_TICKS;
	}

	public ServiceConfig.Fuse fuseConfig() {
		InfectionServiceContainer services = InfectionServices.container();
		ServiceConfig settings = services != null ? services.settings() : null;
		ServiceConfig.Fuse config = settings != null ? settings.fuse : null;
		return config != null ? config : DEFAULT_FUSE_CONFIG;
	}

	public long configuredFuseExplosionDelayTicks() {
		long configured = fuseConfig().explosionDelayTicks;
		return configured > 0 ? configured : DEFAULT_FUSE_CONFIG.explosionDelayTicks;
	}

	public int configuredFuseShellCollapseTicks() {
		int configured = fuseConfig().shellCollapseTicks;
		return configured > 0 ? configured : DEFAULT_FUSE_CONFIG.shellCollapseTicks;
	}

	public int configuredFusePulseInterval() {
		int configured = fuseConfig().pulseIntervalTicks;
		return configured > 0 ? configured : DEFAULT_FUSE_CONFIG.pulseIntervalTicks;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Gamerule management (extracted from VirusWorldState)
	// ─────────────────────────────────────────────────────────────────────────────

	/**
	 * Ensures singularity-related gamerules match configured values.
	 */
	public void ensureSingularityGamerules(ServerWorld world) {
		if (world == null || world.getServer() == null) {
			return;
		}
		applySingularityGamerule(world,
				TheVirusBlock.VIRUS_SINGULARITY_COLLAPSE_ENABLED,
				configuredCollapseEnabled());
		applySingularityGamerule(world,
				TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION,
				configuredChunkGenerationAllowed());
		applySingularityGamerule(world,
				TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD,
				configuredOutsideBorderAllowed());
		// Set high block limit for /fill command to handle large collapse volumes
		ensureCommandBlockLimit(world, 100_000_000);
	}

	private void applySingularityGamerule(ServerWorld world,
			GameRules.Key<GameRules.BooleanRule> rule,
			boolean desired) {
		GameRules.BooleanRule gamerule = world.getGameRules().get(rule);
		if (gamerule.get() != desired) {
			gamerule.set(desired, world.getServer());
		}
	}

	/**
	 * Ensures commandModificationBlockLimit is set high enough for collapse operations.
	 */
	private void ensureCommandBlockLimit(ServerWorld world, int minLimit) {
		GameRules.IntRule rule = world.getGameRules().get(GameRules.COMMAND_MODIFICATION_BLOCK_LIMIT);
		int current = rule.get();
		if (current < minLimit) {
			System.out.println("[Singularity] Setting commandModificationBlockLimit: " + current + " -> " + minLimit);
			rule.set(minLimit, world.getServer());
		}
	}
}

