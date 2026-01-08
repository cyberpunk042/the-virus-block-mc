package net.cyberpunk042.infection.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.scenario.OverworldInfectionScenario;
import net.minecraft.util.Identifier;

/**
 * Immutable snapshot of collapse parameters for a given infection scenario or
 * dimension. These values feed planners, controllers, and effect sets so we can
 * tune behaviour per world without recompiling.
 */
public final class DimensionProfile {
	private final Identifier id;
	private final Collapse collapse;
	private final Effects effects;
	private final Physics physics;

	private DimensionProfile(Identifier id, Collapse collapse, Effects effects, Physics physics) {
		this.id = Objects.requireNonNull(id, "id");
		this.collapse = collapse != null ? collapse : Collapse.defaults();
		this.effects = effects != null ? effects : Effects.defaults();
		this.physics = physics != null ? physics : Physics.defaults();
	}

	public static DimensionProfile of(Identifier id, Collapse collapse, Effects effects, Physics physics) {
		return new DimensionProfile(id, collapse, effects, physics);
	}

	public static DimensionProfile defaults() {
		return of(OverworldInfectionScenario.ID, Collapse.defaults(), Effects.defaults(), Physics.defaults());
	}

	public static DimensionProfile fallback(Identifier scenarioId) {
		return of(scenarioId, Collapse.defaults(), Effects.defaults(), Physics.defaults());
	}

	public Identifier id() {
		return id;
	}

	public Collapse collapse() {
		return collapse;
	}

	public Effects effects() {
		return effects;
	}

	public Physics physics() {
		return physics;
	}

	public static final class Collapse {
		private static final int DEFAULT_RING_START_DELAY_TICKS = 40;
		private static final int DEFAULT_RING_DURATION_TICKS = 200;
		private static final double DEFAULT_BARRIER_START_RADIUS = 120.0D;
		private static final double DEFAULT_BARRIER_END_RADIUS = 0.5D;
		private static final long DEFAULT_BARRIER_DURATION_TICKS = 1_200L;
		private static final long DEFAULT_BARRIER_RESET_DELAY_TICKS = 200L;
		private static final int DEFAULT_VIEW_DISTANCE_CHUNKS = 0;
		private static final int DEFAULT_SIMULATION_DISTANCE_CHUNKS = 0;
		private static final int DEFAULT_BROADCAST_RADIUS_BLOCKS = 96;
		private static final List<RadiusDelay> DEFAULT_RADIUS_DELAYS = defaultRadiusDelays();
		private static final boolean DEFAULT_CHUNK_PREGEN_ENABLED = true;
		private static final int DEFAULT_CHUNK_PREGEN_RADIUS_BLOCKS = 0;
		private static final int DEFAULT_CHUNK_PREGEN_CHUNKS_PER_TICK = 8;
		private static final boolean DEFAULT_CHUNK_PRELOAD_ENABLED = true;
		private static final int DEFAULT_CHUNK_PRELOAD_CHUNKS_PER_TICK = 4;
		private static final WaterDrainMode DEFAULT_WATER_DRAIN_MODE = WaterDrainMode.OFF;
		private static final int DEFAULT_WATER_DRAIN_OFFSET = 1;
		private static final WaterDrainDeferred DEFAULT_WATER_DRAIN_DEFERRED = WaterDrainDeferred.defaults();
		private static final boolean DEFAULT_COLLAPSE_PARTICLES = false;
		private static final CollapseFillMode DEFAULT_FILL_MODE = CollapseFillMode.AIR;
		private static final CollapseFillShape DEFAULT_FILL_SHAPE = CollapseFillShape.WALLS;  // Faster than OUTLINE
		private static final int DEFAULT_OUTLINE_THICKNESS = 2;  // 2-block walls by default
		private static final boolean DEFAULT_USE_NATIVE_FILL = false;  // Block-by-block unless explicitly enabled
		private static final boolean DEFAULT_RESPECT_PROTECTED = true;
		private static final int DEFAULT_MAX_OPERATIONS_PER_TICK = 1;
		private static final boolean DEFAULT_COLLAPSE_INWARD = true;
		private final int columnsPerTick;
		private final int tickInterval;
		private final int maxRadiusChunks;
		private final String mode;
		private final int ringStartDelayTicks;
		private final int ringDurationTicks;
		private final double barrierStartRadius;
		private final double barrierEndRadius;
		private final long barrierDurationTicks;
		private final boolean barrierAutoReset;
		private final long barrierResetDelayTicks;
		private final boolean chunkPreGenEnabled;
		private final int chunkPreGenRadiusBlocks;
		private final int chunkPreGenChunksPerTick;
		private final boolean chunkPreloadEnabled;
		private final int chunkPreloadChunksPerTick;
		private final int viewDistanceChunks;
		private final int simulationDistanceChunks;
		private final CollapseBroadcastMode broadcastMode;
		private final int broadcastRadiusBlocks;
		private final CollapseSyncProfile defaultSyncProfile;
		private final List<RadiusDelay> radiusDelays;
		private final WaterDrainMode waterDrainMode;
		private final int waterDrainOffset;
		private final WaterDrainDeferred waterDrainDeferred;
		private final boolean collapseParticles;
		private final CollapseFillProfile fillProfile;
		private final CollapseFillMode fillMode;
		private final CollapseFillShape fillShape;
		private final int outlineThickness;
		private final boolean useNativeFill;
		private final boolean respectProtectedBlocks;
		private final int maxOperationsPerTick;
		private final boolean collapseInward;
		private final PreCollapseWaterDrainage preCollapseWaterDrainage;

		public Collapse(int columnsPerTick, int tickInterval, int maxRadiusChunks, String mode) {
			this(columnsPerTick,
					tickInterval,
					maxRadiusChunks,
					mode,
					DEFAULT_RING_START_DELAY_TICKS,
					DEFAULT_RING_DURATION_TICKS,
					DEFAULT_BARRIER_START_RADIUS,
					DEFAULT_BARRIER_END_RADIUS,
					DEFAULT_BARRIER_DURATION_TICKS,
					false,
					DEFAULT_BARRIER_RESET_DELAY_TICKS,
					DEFAULT_CHUNK_PREGEN_ENABLED,
					DEFAULT_CHUNK_PREGEN_RADIUS_BLOCKS,
					DEFAULT_CHUNK_PREGEN_CHUNKS_PER_TICK,
					DEFAULT_CHUNK_PRELOAD_ENABLED,
					DEFAULT_CHUNK_PRELOAD_CHUNKS_PER_TICK,
					DEFAULT_VIEW_DISTANCE_CHUNKS,
					DEFAULT_SIMULATION_DISTANCE_CHUNKS,
					CollapseBroadcastMode.defaultMode(),
					DEFAULT_BROADCAST_RADIUS_BLOCKS,
					CollapseSyncProfile.defaultProfile(),
					DEFAULT_RADIUS_DELAYS,
					DEFAULT_WATER_DRAIN_OFFSET,
				DEFAULT_WATER_DRAIN_MODE,
				DEFAULT_WATER_DRAIN_DEFERRED,
					DEFAULT_COLLAPSE_PARTICLES,
					DEFAULT_FILL_MODE,
					DEFAULT_FILL_SHAPE,
					DEFAULT_OUTLINE_THICKNESS,
				DEFAULT_USE_NATIVE_FILL,
				DEFAULT_RESPECT_PROTECTED,
				PreCollapseWaterDrainage.disabled());
		}

		public Collapse(int columnsPerTick,
				int tickInterval,
				int maxRadiusChunks,
				String mode,
				int ringStartDelayTicks,
				int ringDurationTicks) {
			this(columnsPerTick,
					tickInterval,
					maxRadiusChunks,
					mode,
					ringStartDelayTicks,
					ringDurationTicks,
					DEFAULT_BARRIER_START_RADIUS,
					DEFAULT_BARRIER_END_RADIUS,
					DEFAULT_BARRIER_DURATION_TICKS,
					false,
					DEFAULT_BARRIER_RESET_DELAY_TICKS,
					DEFAULT_CHUNK_PREGEN_ENABLED,
					DEFAULT_CHUNK_PREGEN_RADIUS_BLOCKS,
					DEFAULT_CHUNK_PREGEN_CHUNKS_PER_TICK,
					DEFAULT_CHUNK_PRELOAD_ENABLED,
					DEFAULT_CHUNK_PRELOAD_CHUNKS_PER_TICK,
					DEFAULT_VIEW_DISTANCE_CHUNKS,
					DEFAULT_SIMULATION_DISTANCE_CHUNKS,
					CollapseBroadcastMode.defaultMode(),
					DEFAULT_BROADCAST_RADIUS_BLOCKS,
					CollapseSyncProfile.defaultProfile(),
					DEFAULT_RADIUS_DELAYS,
					DEFAULT_WATER_DRAIN_OFFSET,
				DEFAULT_WATER_DRAIN_MODE,
				DEFAULT_WATER_DRAIN_DEFERRED,
					DEFAULT_COLLAPSE_PARTICLES,
					DEFAULT_FILL_MODE,
					DEFAULT_FILL_SHAPE,
					DEFAULT_OUTLINE_THICKNESS,
				DEFAULT_USE_NATIVE_FILL,
				DEFAULT_RESPECT_PROTECTED,
				PreCollapseWaterDrainage.disabled());
		}

		public Collapse(int columnsPerTick,
				int tickInterval,
				int maxRadiusChunks,
				String mode,
				int ringStartDelayTicks,
				int ringDurationTicks,
				double barrierStartRadius,
				double barrierEndRadius,
				long barrierDurationTicks,
				boolean barrierAutoReset,
				long barrierResetDelayTicks,
				boolean chunkPreGenEnabled,
				int chunkPreGenRadiusBlocks,
				int chunkPreGenChunksPerTick,
				boolean chunkPreloadEnabled,
				int chunkPreloadChunksPerTick,
				int viewDistanceChunks,
				int simulationDistanceChunks,
				CollapseBroadcastMode broadcastMode,
				int broadcastRadiusBlocks,
				CollapseSyncProfile defaultSyncProfile,
				List<RadiusDelay> radiusDelays,
				int waterDrainOffset,
				WaterDrainMode waterDrainMode,
				WaterDrainDeferred waterDrainDeferred,
				boolean collapseParticles,
				CollapseFillMode fillMode,
				CollapseFillShape fillShape,
				int outlineThickness,
				boolean useNativeFill,
				boolean respectProtectedBlocks,
				PreCollapseWaterDrainage preCollapseWaterDrainage) {
			this.columnsPerTick = Math.max(1, columnsPerTick);
			this.tickInterval = Math.max(1, tickInterval);
			this.maxRadiusChunks = Math.max(1, maxRadiusChunks);
			this.mode = mode == null || mode.isBlank() ? "erode" : mode;
			this.ringStartDelayTicks = Math.max(1, ringStartDelayTicks);
			this.ringDurationTicks = Math.max(1, ringDurationTicks);
			this.barrierStartRadius = barrierStartRadius > 0.0D ? barrierStartRadius : DEFAULT_BARRIER_START_RADIUS;
			this.barrierEndRadius = barrierEndRadius > 0.0D ? barrierEndRadius : DEFAULT_BARRIER_END_RADIUS;
			this.barrierDurationTicks = barrierDurationTicks > 0 ? barrierDurationTicks : DEFAULT_BARRIER_DURATION_TICKS;
			this.barrierAutoReset = barrierAutoReset;
			this.barrierResetDelayTicks = barrierResetDelayTicks >= 0 ? barrierResetDelayTicks : DEFAULT_BARRIER_RESET_DELAY_TICKS;
			this.chunkPreGenEnabled = chunkPreGenEnabled;
			this.chunkPreGenRadiusBlocks = Math.max(0, chunkPreGenRadiusBlocks);
			this.chunkPreGenChunksPerTick = Math.max(1, chunkPreGenChunksPerTick);
			this.chunkPreloadEnabled = chunkPreloadEnabled;
			this.chunkPreloadChunksPerTick = Math.max(1, chunkPreloadChunksPerTick);
			this.viewDistanceChunks = Math.max(0, viewDistanceChunks);
			this.simulationDistanceChunks = Math.max(0, simulationDistanceChunks);
			this.broadcastMode = broadcastMode != null ? broadcastMode : CollapseBroadcastMode.defaultMode();
			this.broadcastRadiusBlocks = Math.max(0, broadcastRadiusBlocks);
			this.defaultSyncProfile = defaultSyncProfile != null ? defaultSyncProfile : CollapseSyncProfile.defaultProfile();
			this.radiusDelays = sanitizeRadiusDelays(radiusDelays);
			this.waterDrainMode = waterDrainMode != null ? waterDrainMode : DEFAULT_WATER_DRAIN_MODE;
			this.waterDrainOffset = Math.max(0, waterDrainOffset);
			this.waterDrainDeferred = waterDrainDeferred != null ? waterDrainDeferred : WaterDrainDeferred.defaults();
			this.collapseParticles = collapseParticles;
			this.fillProfile = CollapseFillProfile.DEFAULT;
			this.fillMode = fillMode != null ? fillMode : DEFAULT_FILL_MODE;
			this.fillShape = fillShape != null ? fillShape : DEFAULT_FILL_SHAPE;
			this.outlineThickness = Math.max(1, outlineThickness);
			this.useNativeFill = useNativeFill;
			this.respectProtectedBlocks = respectProtectedBlocks;
			this.maxOperationsPerTick = DEFAULT_MAX_OPERATIONS_PER_TICK;
			this.collapseInward = DEFAULT_COLLAPSE_INWARD;
			this.preCollapseWaterDrainage = preCollapseWaterDrainage != null ? preCollapseWaterDrainage : PreCollapseWaterDrainage.disabled();
		}

		public static Collapse defaults() {
			return new Collapse(8, 20, 12, "erode");
		}

		public int columnsPerTick() {
			return columnsPerTick;
		}

		public int tickInterval() {
			return tickInterval;
		}

		public int maxRadiusChunks() {
			return maxRadiusChunks;
		}

		public String mode() {
			return mode;
		}

		public int ringStartDelayTicks() {
			return ringStartDelayTicks;
		}

		public int ringDurationTicks() {
			return ringDurationTicks;
		}

		public double barrierStartRadius() {
			return barrierStartRadius;
		}

		public double barrierEndRadius() {
			return barrierEndRadius;
		}

		public long barrierDurationTicks() {
			return barrierDurationTicks;
		}

		public boolean barrierAutoReset() {
			return barrierAutoReset;
		}

		public long barrierResetDelayTicks() {
			return barrierResetDelayTicks;
		}

		public boolean chunkPreGenEnabled() {
			return chunkPreGenEnabled;
		}

		public int chunkPreGenRadiusBlocks() {
			return chunkPreGenRadiusBlocks;
		}

		public int chunkPreGenChunksPerTick() {
			return chunkPreGenChunksPerTick;
		}

		public boolean chunkPreloadEnabled() {
			return chunkPreloadEnabled;
		}

		public int chunkPreloadChunksPerTick() {
			return chunkPreloadChunksPerTick;
		}

		public int viewDistanceChunks() {
			return viewDistanceChunks;
		}

		public int simulationDistanceChunks() {
			return simulationDistanceChunks;
		}

		public CollapseBroadcastMode broadcastMode() {
			return broadcastMode;
		}

		public int broadcastRadiusBlocks() {
			return broadcastRadiusBlocks;
		}

		public CollapseSyncProfile defaultSyncProfile() {
			return defaultSyncProfile;
		}

		public List<RadiusDelay> radiusDelays() {
			return radiusDelays;
		}

		public WaterDrainMode waterDrainMode() {
			return waterDrainMode;
		}
		public WaterDrainDeferred waterDrainDeferred() {
			return waterDrainDeferred;
		}

		public PreCollapseWaterDrainage preCollapseWaterDrainage() {
			return preCollapseWaterDrainage;
		}


		public int waterDrainOffset() {
			return waterDrainOffset;
		}

		public boolean collapseParticles() {
			return collapseParticles;
		}

		public CollapseFillProfile fillProfile() {
			return fillProfile;
		}

		public CollapseFillMode fillMode() {
			return fillMode;
		}

		public CollapseFillShape fillShape() {
			return fillShape;
		}

		public int outlineThickness() {
			return outlineThickness;
		}

		public boolean useNativeFill() {
			return useNativeFill;
		}

		public boolean respectProtectedBlocks() {
			return respectProtectedBlocks;
		}

		public int maxOperationsPerTick() {
			return maxOperationsPerTick;
		}

		public boolean collapseInward() {
			return collapseInward;
		}

		public static final class WaterDrainDeferred {
			private final boolean enabled;
			private final int initialDelayTicks;
			private final int columnsPerTick;

			public WaterDrainDeferred(boolean enabled, int initialDelayTicks, int columnsPerTick) {
				this.enabled = enabled;
				this.initialDelayTicks = Math.max(0, initialDelayTicks);
				this.columnsPerTick = Math.max(1, columnsPerTick);
			}

			public static WaterDrainDeferred defaults() {
				return new WaterDrainDeferred(false, 20, 16);
			}

			public boolean enabled() {
				return enabled;
			}

			public int initialDelayTicks() {
				return initialDelayTicks;
			}

			public int columnsPerTick() {
				return columnsPerTick;
			}
		}

		public static final class PreCollapseWaterDrainage {
			private static final int DEFAULT_MAX_OPS_PER_TICK = 32;
			private static final int DEFAULT_THICKNESS = 1;
			
			private final boolean enabled;
			private final PreDrainProfile profile;
			private final PreDrainMode mode;
			private final int tickRate;
			private final int batchSize;
			private final int startDelayTicks;
			private final boolean startFromCenter;
			private final int maxOperationsPerTick;
			private final int thickness;

			public PreCollapseWaterDrainage(boolean enabled,
					PreDrainMode mode,
					int tickRate,
					int batchSize,
					int startDelayTicks) {
				this(enabled, PreDrainProfile.DEFAULT, mode, tickRate, batchSize, startDelayTicks, false, DEFAULT_MAX_OPS_PER_TICK, DEFAULT_THICKNESS);
			}

			public PreCollapseWaterDrainage(boolean enabled,
					PreDrainMode mode,
					int tickRate,
					int batchSize,
					int startDelayTicks,
					boolean startFromCenter) {
				this(enabled, PreDrainProfile.DEFAULT, mode, tickRate, batchSize, startDelayTicks, startFromCenter, DEFAULT_MAX_OPS_PER_TICK, DEFAULT_THICKNESS);
			}

			public PreCollapseWaterDrainage(boolean enabled,
					PreDrainMode mode,
					int tickRate,
					int batchSize,
					int startDelayTicks,
					boolean startFromCenter,
					int maxOperationsPerTick,
					int thickness) {
				this(enabled, PreDrainProfile.DEFAULT, mode, tickRate, batchSize, startDelayTicks, startFromCenter, maxOperationsPerTick, thickness);
			}

			public PreCollapseWaterDrainage(boolean enabled,
					PreDrainProfile profile,
					PreDrainMode mode,
					int tickRate,
					int batchSize,
					int startDelayTicks,
					boolean startFromCenter,
					int maxOperationsPerTick,
					int thickness) {
				this.enabled = enabled;
				this.profile = profile != null ? profile : PreDrainProfile.DEFAULT;
				// Use profile defaults if not explicitly set
				this.mode = mode != null ? mode : this.profile.mode();
				this.tickRate = Math.max(1, tickRate);
				this.batchSize = Math.max(1, batchSize);
				this.startDelayTicks = Math.max(0, startDelayTicks);
				this.startFromCenter = startFromCenter;
				this.maxOperationsPerTick = Math.max(1, maxOperationsPerTick > 0 ? maxOperationsPerTick : this.profile.maxOperationsPerTick());
				this.thickness = Math.max(1, thickness > 0 ? thickness : this.profile.thickness());
			}

			public static PreCollapseWaterDrainage disabled() {
				return new PreCollapseWaterDrainage(false, PreDrainProfile.DEFAULT, PreDrainMode.OUTLINE, 20, 4, 60, false, DEFAULT_MAX_OPS_PER_TICK, DEFAULT_THICKNESS);
			}

			public boolean enabled() {
				return enabled;
			}

			public PreDrainProfile profile() {
				return profile;
			}

			public PreDrainMode mode() {
				return mode;
			}

			public int tickRate() {
				return tickRate;
			}

			public int batchSize() {
				return batchSize;
			}

			public int startDelayTicks() {
				return startDelayTicks;
			}

			public boolean startFromCenter() {
				return startFromCenter;
			}

			public int maxOperationsPerTick() {
				return maxOperationsPerTick;
			}

			public int thickness() {
				return thickness;
			}

			public enum PreDrainMode {
				/** Drain all fluids instantly */
				FULL_INSTANT,
				/** Drain all fluids, chunk by chunk */
				FULL_PER_CHUNK,
				/** Drain all 6 faces of each chunk */
				OUTLINE,
				/** Drain horizontal rows through chunk */
				ROWS,
				/** Drain only the chunk face closest to singularity center */
				FACING_CENTER
			}
		}

		private static List<RadiusDelay> sanitizeRadiusDelays(List<RadiusDelay> raw) {
			List<RadiusDelay> source = raw == null || raw.isEmpty() ? DEFAULT_RADIUS_DELAYS : raw;
			List<RadiusDelay> sanitized = new ArrayList<>();
			for (RadiusDelay delay : source) {
				if (delay == null) {
					continue;
				}
				int side = Math.max(1, delay.side());
				int ticks = Math.max(1, delay.ticks());
				sanitized.add(new RadiusDelay(side, ticks));
			}
			if (sanitized.isEmpty()) {
				sanitized.addAll(DEFAULT_RADIUS_DELAYS);
			}
			sanitized.sort((a, b) -> Integer.compare(a.side(), b.side()));
			return Collections.unmodifiableList(sanitized);
		}

		private static List<RadiusDelay> defaultRadiusDelays() {
			List<RadiusDelay> defaults = new ArrayList<>();
			defaults.add(new RadiusDelay(1, 150));
			defaults.add(new RadiusDelay(3, 100));
			defaults.add(new RadiusDelay(9, 40));
			defaults.add(new RadiusDelay(15, 20));
			return Collections.unmodifiableList(defaults);
		}

		public static final class RadiusDelay {
			private final int side;
			private final int ticks;

			public RadiusDelay(int side, int ticks) {
				this.side = Math.max(1, side);
				this.ticks = Math.max(1, ticks);
			}

			public int side() {
				return side;
			}

			public int ticks() {
				return ticks;
			}
		}
	}

	public static final class Effects {
		private final String beamColor;
		private final Identifier veilParticles;
		private final Identifier ringParticles;
		private final Identifier effectPalette;

		public Effects(String beamColor, Identifier veilParticles, Identifier ringParticles, Identifier effectPalette) {
			this.beamColor = beamColor != null ? beamColor : "#C600FFFF";
			this.veilParticles = veilParticles != null ? veilParticles : Identifier.of("minecraft", "sculk_soul");
			this.ringParticles = ringParticles != null ? ringParticles : Identifier.of("minecraft", "portal");
			this.effectPalette = effectPalette != null ? effectPalette : Identifier.of(TheVirusBlock.MOD_ID, "overworld");
		}

		public static Effects defaults() {
			return new Effects("#C600FFFF",
					Identifier.of("minecraft", "sculk_soul"),
					Identifier.of("minecraft", "portal"),
					Identifier.of(TheVirusBlock.MOD_ID, "overworld"));
		}

		public String beamColor() {
			return beamColor;
		}

		public Identifier veilParticles() {
			return veilParticles;
		}

		public Identifier ringParticles() {
			return ringParticles;
		}

		public Identifier effectPalette() {
			return effectPalette;
		}
	}

	public static final class Physics {
		private final double ringPullStrength;
		private final int pushRadius;

		public Physics(double ringPullStrength, int pushRadius) {
			this.ringPullStrength = ringPullStrength <= 0.0D ? 0.35D : ringPullStrength;
			this.pushRadius = Math.max(1, pushRadius);
		}

		public static Physics defaults() {
			return new Physics(0.35D, 12);
		}

		public double ringPullStrength() {
			return ringPullStrength;
		}

		public int pushRadius() {
			return pushRadius;
		}
	}
}

