package net.cyberpunk042.infection.profile;


import net.cyberpunk042.log.Logging;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.profile.DimensionProfile.Collapse;
import net.cyberpunk042.infection.profile.DimensionProfile.Collapse.PreCollapseWaterDrainage.PreDrainMode;
import net.cyberpunk042.infection.profile.DimensionProfile.Effects;
import net.cyberpunk042.infection.profile.DimensionProfile.Physics;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.cyberpunk042.infection.scenario.NetherInfectionScenario;

/**
 * Loader/registry for {@link DimensionProfile} JSON files. Profiles live under
 * {@code config/the-virus-block/dimension_profiles} and are keyed by
 * {@link Identifier}. Missing or malformed files are skipped with a log entry.
 */
public final class DimensionProfileRegistry {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PROFILE_DIR = FabricLoader.getInstance().getConfigDir()
			.resolve("the-virus-block")
			.resolve("dimension_profiles");
	private static final Map<Identifier, DimensionProfile> PROFILES = new HashMap<>();
	private static final Map<Identifier, Path> PROFILE_PATHS = new HashMap<>();
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

	private DimensionProfileRegistry() {
	}

	public static DimensionProfile resolve(Identifier scenarioId) {
		ensureLoaded();
		return PROFILES.getOrDefault(scenarioId, DimensionProfile.fallback(scenarioId));
	}

	public static Map<Identifier, DimensionProfile> snapshot() {
		ensureLoaded();
		return Collections.unmodifiableMap(new HashMap<>(PROFILES));
	}

	public static void reload() {
		PROFILES.clear();
		PROFILE_PATHS.clear();
		loadDefaults();
		ensureDirectory();
		ensureDefaultProfileFiles();
		if (!Files.exists(PROFILE_DIR)) {
			INITIALIZED.set(true);
			return;
		}
		try (var stream = Files.list(PROFILE_DIR)) {
			stream.filter(path -> path.toString().endsWith(".json"))
					.forEach(DimensionProfileRegistry::loadProfile);
		} catch (IOException ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to read profiles", ex);
		}
		INITIALIZED.set(true);
	}

	public static boolean setCollapseViewDistance(Identifier scenarioId, int chunks) {
		int sanitized = Math.max(0, chunks);
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("view_distance_chunks", sanitized));
	}

	public static boolean setCollapseSimulationDistance(Identifier scenarioId, int chunks) {
		int sanitized = Math.max(0, chunks);
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("simulation_distance_chunks", sanitized));
	}

	public static boolean setCollapseBroadcastMode(Identifier scenarioId, CollapseBroadcastMode mode) {
		CollapseBroadcastMode resolved = mode == null ? CollapseBroadcastMode.defaultMode() : mode;
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("broadcast_mode", resolved.id()));
	}

	public static boolean setCollapseBroadcastRadius(Identifier scenarioId, int blocks) {
		int sanitized = Math.max(0, blocks);
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("broadcast_radius_blocks", sanitized));
	}

	public static boolean setCollapseDefaultProfile(Identifier scenarioId, CollapseSyncProfile profile) {
		CollapseSyncProfile resolved = profile == null ? CollapseSyncProfile.defaultProfile() : profile;
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("default_sync_profile", resolved.id()));
	}

	public static boolean setWaterDrainMode(Identifier scenarioId, WaterDrainMode mode) {
		WaterDrainMode resolved = mode != null ? mode : WaterDrainMode.OFF;
		return updateCollapseJson(scenarioId, collapse -> {
			JsonObject waterNode = ensureWaterDrainNode(collapse);
			waterNode.addProperty("mode", resolved.name().toLowerCase(Locale.ROOT));
		});
	}

	public static boolean setWaterDrainOffset(Identifier scenarioId, int blocks) {
		int sanitized = Math.max(0, blocks);
		return updateCollapseJson(scenarioId, collapse -> {
			JsonObject waterNode = ensureWaterDrainNode(collapse);
			waterNode.addProperty("offset", sanitized);
		});
	}

	public static boolean setCollapseParticles(Identifier scenarioId, boolean enabled) {
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("collapse_particles", enabled));
	}

	public static boolean setFillMode(Identifier scenarioId, CollapseFillMode mode) {
		CollapseFillMode resolved = mode != null ? mode : CollapseFillMode.defaultMode();
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("fill_mode", resolved.id()));
	}

	public static boolean setFillShape(Identifier scenarioId, CollapseFillShape shape) {
		CollapseFillShape resolved = shape != null ? shape : CollapseFillShape.OUTLINE;
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("fill_shape", resolved.id()));
	}

	public static boolean setOutlineThickness(Identifier scenarioId, int thickness) {
		int sanitized = Math.max(1, thickness);
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("outline_thickness", sanitized));
	}

	public static boolean setUseNativeFill(Identifier scenarioId, boolean enabled) {
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("use_native_fill", enabled));
	}

	public static boolean setRespectProtectedBlocks(Identifier scenarioId, boolean enabled) {
		return updateCollapseJson(scenarioId, collapse -> collapse.addProperty("respect_protected_blocks", enabled));
	}

	public static boolean describeErosion(Identifier scenarioId, Consumer<Collapse> consumer) {
		ensureLoaded();
		Collapse profile = resolve(scenarioId).collapse();
		if (profile == null) {
			return false;
		}
		consumer.accept(profile);
		return true;
	}

	private static void ensureDirectory() {
		try {
			Files.createDirectories(PROFILE_DIR);
		} catch (IOException ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to create directory {}", PROFILE_DIR, ex);
		}
	}

	private static void ensureDefaultProfileFiles() {
		ensureProfileFile("overworld.json", DimensionProfile.defaults());
		ensureProfileFile("nether.json", netherDefaults());
	}

	private static void ensureProfileFile(String fileName, DimensionProfile profile) {
		Path target = PROFILE_DIR.resolve(fileName);
		if (Files.exists(target)) {
			PROFILE_PATHS.put(profile.id(), target);
			return;
		}
		try {
			JsonObject root = serializeProfile(profile);
			try (var writer = Files.newBufferedWriter(target)) {
				GSON.toJson(root, writer);
			}
			PROFILE_PATHS.put(profile.id(), target);
		} catch (IOException ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to write default profile {}", target, ex);
		}
	}

	private static void loadDefaults() {
		DimensionProfile defaultProfile = DimensionProfile.defaults();
		PROFILES.put(defaultProfile.id(), defaultProfile);
		DimensionProfile netherProfile = netherDefaults();
		PROFILES.put(netherProfile.id(), netherProfile);
	}

	private static DimensionProfile netherDefaults() {
		DimensionProfile.Collapse collapse = new DimensionProfile.Collapse(6,
				24,
				10,
				"erode",
				40,
				200,
				120.0D,
				0.5D,
				1_000L,
				false,
				200L,
				true,
				0,
				8,
				true,
				4,
				0,
				0,
				CollapseBroadcastMode.defaultMode(),
				96,
				CollapseSyncProfile.defaultProfile(),
				DimensionProfile.Collapse.defaults().radiusDelays(),
				1,
				WaterDrainMode.AHEAD,
				DimensionProfile.Collapse.WaterDrainDeferred.defaults(),
				false,
				CollapseFillMode.AIR,
				CollapseFillShape.OUTLINE,
				2,
				true,
				true,
				DimensionProfile.Collapse.PreCollapseWaterDrainage.disabled());
		DimensionProfile.Effects effects = new DimensionProfile.Effects("#FF6A00FF",
				Identifier.of("minecraft", "ash"),
				Identifier.of("minecraft", "lava"),
				Identifier.of(TheVirusBlock.MOD_ID, "nether"));
		DimensionProfile.Physics physics = new DimensionProfile.Physics(0.3D, 10);
		return DimensionProfile.of(NetherInfectionScenario.ID, collapse, effects, physics);
	}

	private static void ensureLoaded() {
		if (!INITIALIZED.get()) {
			reload();
		}
	}

	private static void loadProfile(Path path) {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				Logging.REGISTRY.warn("[DimensionProfile] Skipping {} (not a JSON object)", path.getFileName());
				return;
			}
			JsonObject root = parsed.getAsJsonObject();
			Identifier id = parseIdentifier(root.get("id"));
			if (id == null) {
				Logging.REGISTRY.warn("[DimensionProfile] Skipping {} (missing or invalid id)", path.getFileName());
				return;
			}
			PROFILE_PATHS.put(id, path);
			Collapse collapse = parseCollapse(root.getAsJsonObject("collapse"));
			Effects effects = parseEffects(root.getAsJsonObject("effects"));
			Physics physics = parsePhysics(root.getAsJsonObject("physics"));
			System.out.println("[DimensionProfileRegistry] Loaded profile " + id
					+ " from " + path
					+ " fillShape=" + collapse.fillShape()
					+ " thickness=" + collapse.outlineThickness()
					+ " useNativeFill=" + collapse.useNativeFill());
			PROFILES.put(id, DimensionProfile.of(id, collapse, effects, physics));
		} catch (Exception ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to load {}", path.getFileName(), ex);
		}
	}

	private static JsonObject serializeProfile(DimensionProfile profile) {
		JsonObject root = new JsonObject();
		root.addProperty("id", profile.id().toString());
		root.add("collapse", serializeCollapse(profile.collapse()));

		JsonObject effects = new JsonObject();
		effects.addProperty("beam_color", profile.effects().beamColor());
		effects.addProperty("veil_particles", profile.effects().veilParticles().toString());
		effects.addProperty("ring_particles", profile.effects().ringParticles().toString());
		effects.addProperty("palette", profile.effects().effectPalette().toString());
		root.add("effects", effects);

		JsonObject physics = new JsonObject();
		physics.addProperty("ring_pull_strength", profile.physics().ringPullStrength());
		physics.addProperty("push_radius", profile.physics().pushRadius());
		root.add("physics", physics);
		return root;
	}

	private static JsonObject serializeCollapse(Collapse collapse) {
		JsonObject node = new JsonObject();
		node.addProperty("columns_per_tick", collapse.columnsPerTick());
		node.addProperty("tick_interval", collapse.tickInterval());
		node.addProperty("max_radius_chunks", collapse.maxRadiusChunks());
		node.addProperty("mode", collapse.mode());
		node.addProperty("ring_start_delay_ticks", collapse.ringStartDelayTicks());
		node.addProperty("ring_duration_ticks", collapse.ringDurationTicks());
		node.addProperty("barrier_start_radius", collapse.barrierStartRadius());
		node.addProperty("barrier_end_radius", collapse.barrierEndRadius());
		node.addProperty("barrier_duration_ticks", collapse.barrierDurationTicks());
		node.addProperty("barrier_auto_reset", collapse.barrierAutoReset());
		node.addProperty("barrier_reset_delay_ticks", collapse.barrierResetDelayTicks());
		node.addProperty("chunk_pregen_enabled", collapse.chunkPreGenEnabled());
		node.addProperty("chunk_pregen_radius_blocks", collapse.chunkPreGenRadiusBlocks());
		node.addProperty("chunk_pregen_chunks_per_tick", collapse.chunkPreGenChunksPerTick());
		node.addProperty("chunk_preload_enabled", collapse.chunkPreloadEnabled());
		node.addProperty("chunk_preload_chunks_per_tick", collapse.chunkPreloadChunksPerTick());
		node.addProperty("view_distance_chunks", collapse.viewDistanceChunks());
		node.addProperty("simulation_distance_chunks", collapse.simulationDistanceChunks());
		node.addProperty("broadcast_mode", collapse.broadcastMode().id());
		node.addProperty("broadcast_radius_blocks", collapse.broadcastRadiusBlocks());
		node.addProperty("default_sync_profile", collapse.defaultSyncProfile().id());
		JsonObject waterDrain = new JsonObject();
		waterDrain.addProperty("mode", collapse.waterDrainMode().name().toLowerCase(Locale.ROOT));
		waterDrain.addProperty("offset", collapse.waterDrainOffset());
		JsonObject deferred = new JsonObject();
		deferred.addProperty("enabled", collapse.waterDrainDeferred().enabled());
		deferred.addProperty("initial_delay_ticks", collapse.waterDrainDeferred().initialDelayTicks());
		deferred.addProperty("columns_per_tick", collapse.waterDrainDeferred().columnsPerTick());
		waterDrain.add("deferred", deferred);
		node.add("water_drain", waterDrain);
		node.addProperty("collapse_particles", collapse.collapseParticles());
		node.addProperty("fill_mode", collapse.fillMode().id());
		node.addProperty("fill_shape", collapse.fillShape().id());
		node.addProperty("outline_thickness", collapse.outlineThickness());
		node.addProperty("use_native_fill", collapse.useNativeFill());
		node.addProperty("respect_protected_blocks", collapse.respectProtectedBlocks());
		JsonObject preDrain = new JsonObject();
		preDrain.addProperty("enabled", collapse.preCollapseWaterDrainage().enabled());
		preDrain.addProperty("profile", collapse.preCollapseWaterDrainage().profile().name().toLowerCase(Locale.ROOT));
		preDrain.addProperty("mode", collapse.preCollapseWaterDrainage().mode().name().toLowerCase(Locale.ROOT));
		preDrain.addProperty("tick_rate", collapse.preCollapseWaterDrainage().tickRate());
		preDrain.addProperty("batch_size", collapse.preCollapseWaterDrainage().batchSize());
		preDrain.addProperty("start_delay_ticks", collapse.preCollapseWaterDrainage().startDelayTicks());
		preDrain.addProperty("start_from_center", collapse.preCollapseWaterDrainage().startFromCenter());
		preDrain.addProperty("max_operations_per_tick", collapse.preCollapseWaterDrainage().maxOperationsPerTick());
		preDrain.addProperty("thickness", collapse.preCollapseWaterDrainage().thickness());
		node.add("pre_collapse_water_drainage", preDrain);
		JsonArray radiusDelays = new JsonArray();
		for (Collapse.RadiusDelay delay : collapse.radiusDelays()) {
			JsonObject delayNode = new JsonObject();
			delayNode.addProperty("side", delay.side());
			delayNode.addProperty("ticks", delay.ticks());
			radiusDelays.add(delayNode);
		}
		node.add("radius_delays", radiusDelays);
		return node;
	}

	private static Collapse parseCollapse(JsonObject node) {
		if (node == null) {
			return Collapse.defaults();
		}
		Collapse defaults = Collapse.defaults();
		int columns = node.has("columns_per_tick") ? node.get("columns_per_tick").getAsInt() : defaults.columnsPerTick();
		int interval = node.has("tick_interval") ? node.get("tick_interval").getAsInt() : defaults.tickInterval();
		int maxRadius = node.has("max_radius_chunks") ? node.get("max_radius_chunks").getAsInt() : defaults.maxRadiusChunks();
		String mode = node.has("mode") ? node.get("mode").getAsString() : defaults.mode();
		int ringStartDelay = node.has("ring_start_delay_ticks") ? node.get("ring_start_delay_ticks").getAsInt()
				: defaults.ringStartDelayTicks();
		int ringDuration = node.has("ring_duration_ticks") ? node.get("ring_duration_ticks").getAsInt()
				: defaults.ringDurationTicks();
		double barrierStart = node.has("barrier_start_radius") ? node.get("barrier_start_radius").getAsDouble()
				: defaults.barrierStartRadius();
		double barrierEnd = node.has("barrier_end_radius") ? node.get("barrier_end_radius").getAsDouble()
				: defaults.barrierEndRadius();
		long barrierDuration = node.has("barrier_duration_ticks") ? node.get("barrier_duration_ticks").getAsLong()
				: defaults.barrierDurationTicks();
		boolean barrierAutoReset = node.has("barrier_auto_reset") ? node.get("barrier_auto_reset").getAsBoolean()
				: defaults.barrierAutoReset();
		long barrierResetDelay = node.has("barrier_reset_delay_ticks") ? node.get("barrier_reset_delay_ticks").getAsLong()
				: defaults.barrierResetDelayTicks();
		boolean chunkPreGenEnabled = node.has("chunk_pregen_enabled") ? node.get("chunk_pregen_enabled").getAsBoolean()
				: defaults.chunkPreGenEnabled();
		int chunkPreGenRadiusBlocks = node.has("chunk_pregen_radius_blocks") ? node.get("chunk_pregen_radius_blocks").getAsInt()
				: defaults.chunkPreGenRadiusBlocks();
		int chunkPreGenChunksPerTick = node.has("chunk_pregen_chunks_per_tick") ? node.get("chunk_pregen_chunks_per_tick").getAsInt()
				: defaults.chunkPreGenChunksPerTick();
		boolean chunkPreloadEnabled = node.has("chunk_preload_enabled") ? node.get("chunk_preload_enabled").getAsBoolean()
				: defaults.chunkPreloadEnabled();
		int chunkPreloadChunksPerTick = node.has("chunk_preload_chunks_per_tick") ? node.get("chunk_preload_chunks_per_tick").getAsInt()
				: defaults.chunkPreloadChunksPerTick();
		int viewDistanceChunks = node.has("view_distance_chunks") ? node.get("view_distance_chunks").getAsInt()
				: defaults.viewDistanceChunks();
		int simulationDistanceChunks = node.has("simulation_distance_chunks") ? node.get("simulation_distance_chunks").getAsInt()
				: defaults.simulationDistanceChunks();
		CollapseBroadcastMode broadcastMode = node.has("broadcast_mode")
				? CollapseBroadcastMode.fromId(node.get("broadcast_mode").getAsString())
				: defaults.broadcastMode();
		if (broadcastMode == null) {
			broadcastMode = defaults.broadcastMode();
		}
		int broadcastRadiusBlocks = node.has("broadcast_radius_blocks") ? node.get("broadcast_radius_blocks").getAsInt()
				: defaults.broadcastRadiusBlocks();
		CollapseSyncProfile defaultProfile = node.has("default_sync_profile")
				? CollapseSyncProfile.fromId(node.get("default_sync_profile").getAsString())
				: defaults.defaultSyncProfile();
		if (defaultProfile == null) {
			defaultProfile = defaults.defaultSyncProfile();
		}
		List<Collapse.RadiusDelay> radiusDelays = node.has("radius_delays") && node.get("radius_delays").isJsonArray()
				? parseRadiusDelays(node.getAsJsonArray("radius_delays"))
				: defaults.radiusDelays();
		WaterDrainMode waterDrainMode = defaults.waterDrainMode();
		int waterDrainOffset = defaults.waterDrainOffset();
		Collapse.WaterDrainDeferred waterDrainDeferred = defaults.waterDrainDeferred();
		if (node.has("water_drain") && node.get("water_drain").isJsonObject()) {
			JsonObject drainNode = node.getAsJsonObject("water_drain");
			if (drainNode.has("mode")) {
				waterDrainMode = parseWaterDrainMode(drainNode.get("mode").getAsString(), waterDrainMode);
			}
			if (drainNode.has("offset")) {
				waterDrainOffset = drainNode.get("offset").getAsInt();
			}
			if (drainNode.has("deferred") && drainNode.get("deferred").isJsonObject()) {
				JsonObject deferredNode = drainNode.getAsJsonObject("deferred");
				boolean enabled = deferredNode.has("enabled") ? deferredNode.get("enabled").getAsBoolean()
						: waterDrainDeferred.enabled();
				int delay = deferredNode.has("initial_delay_ticks")
						? deferredNode.get("initial_delay_ticks").getAsInt()
						: waterDrainDeferred.initialDelayTicks();
				int columnsPerTick = deferredNode.has("columns_per_tick")
						? deferredNode.get("columns_per_tick").getAsInt()
						: waterDrainDeferred.columnsPerTick();
				waterDrainDeferred = new Collapse.WaterDrainDeferred(enabled, delay, columnsPerTick);
			}
		} else {
			boolean drainWaterAhead = node.has("drain_water_ahead") ? node.get("drain_water_ahead").getAsBoolean()
					: defaults.waterDrainMode().drainsAhead();
			waterDrainMode = drainWaterAhead ? WaterDrainMode.AHEAD : WaterDrainMode.OFF;
			waterDrainOffset = node.has("water_drain_offset") ? node.get("water_drain_offset").getAsInt()
					: defaults.waterDrainOffset();
		}
		boolean collapseParticles = node.has("collapse_particles") ? node.get("collapse_particles").getAsBoolean()
				: defaults.collapseParticles();
		CollapseFillMode fillMode = node.has("fill_mode") ? CollapseFillMode.fromId(node.get("fill_mode").getAsString())
				: defaults.fillMode();
		if (fillMode == null) {
			fillMode = defaults.fillMode();
		}
		CollapseFillShape fillShape = node.has("fill_shape") ? CollapseFillShape.fromId(node.get("fill_shape").getAsString())
				: defaults.fillShape();
		if (fillShape == null) {
			fillShape = defaults.fillShape();
		}
		int outlineThickness = node.has("outline_thickness") ? node.get("outline_thickness").getAsInt()
				: defaults.outlineThickness();
		boolean useNativeFill = node.has("use_native_fill") ? node.get("use_native_fill").getAsBoolean()
				: defaults.useNativeFill();
		boolean respectProtectedBlocks = node.has("respect_protected_blocks")
				? node.get("respect_protected_blocks").getAsBoolean()
				: defaults.respectProtectedBlocks();
		Collapse.PreCollapseWaterDrainage preDrainage = defaults.preCollapseWaterDrainage();
		if (node.has("pre_collapse_water_drainage") && node.get("pre_collapse_water_drainage").isJsonObject()) {
			JsonObject preNode = node.getAsJsonObject("pre_collapse_water_drainage");
			boolean enabled = preNode.has("enabled") ? preNode.get("enabled").getAsBoolean() : preDrainage.enabled();
			PreDrainMode preMode = preDrainage.mode();
			if (preNode.has("mode")) {
				preMode = parsePreDrainMode(preNode.get("mode").getAsString(), preMode);
			}
			PreDrainProfile profile = preDrainage.profile();
			if (preNode.has("profile")) {
				profile = PreDrainProfile.fromName(preNode.get("profile").getAsString());
			}
			int tickRate = preNode.has("tick_rate") ? preNode.get("tick_rate").getAsInt() : preDrainage.tickRate();
			int batchSize = preNode.has("batch_size") ? preNode.get("batch_size").getAsInt() : preDrainage.batchSize();
			int startDelay = preNode.has("start_delay_ticks") ? preNode.get("start_delay_ticks").getAsInt()
					: preDrainage.startDelayTicks();
			boolean startCenter = preNode.has("start_from_center") ? preNode.get("start_from_center").getAsBoolean()
					: preDrainage.startFromCenter();
			int maxOps = preNode.has("max_operations_per_tick") ? preNode.get("max_operations_per_tick").getAsInt()
					: 0; // 0 means use profile default
			int thickness = preNode.has("thickness") ? preNode.get("thickness").getAsInt()
					: 0; // 0 means use profile default
			preDrainage = new Collapse.PreCollapseWaterDrainage(enabled, profile, preMode, tickRate, batchSize, startDelay, startCenter, maxOps, thickness);
		}
		return new Collapse(columns,
				interval,
				maxRadius,
				mode,
				ringStartDelay,
				ringDuration,
				barrierStart,
				barrierEnd,
				barrierDuration,
				barrierAutoReset,
				barrierResetDelay,
				chunkPreGenEnabled,
				chunkPreGenRadiusBlocks,
				chunkPreGenChunksPerTick,
				chunkPreloadEnabled,
				chunkPreloadChunksPerTick,
				viewDistanceChunks,
				simulationDistanceChunks,
				broadcastMode,
				broadcastRadiusBlocks,
				defaultProfile,
				radiusDelays,
				waterDrainOffset,
				waterDrainMode,
				waterDrainDeferred,
				collapseParticles,
				fillMode,
				fillShape,
				outlineThickness,
				useNativeFill,
				respectProtectedBlocks,
				preDrainage);
	}

	private static List<Collapse.RadiusDelay> parseRadiusDelays(JsonArray array) {
		if (array == null) {
			return Collections.emptyList();
		}
		List<Collapse.RadiusDelay> delays = new ArrayList<>();
		for (JsonElement element : array) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			int side = obj.has("side") ? obj.get("side").getAsInt() : 0;
			int ticks = 0;
			if (obj.has("ticks")) {
				ticks = obj.get("ticks").getAsInt();
			} else if (obj.has("slices")) {
				ticks = obj.get("slices").getAsInt();
			}
			if (side <= 0 || ticks <= 0) {
				continue;
			}
			delays.add(new Collapse.RadiusDelay(side, ticks));
		}
		return delays;
	}

	private static WaterDrainMode parseWaterDrainMode(String raw, WaterDrainMode fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		try {
			return WaterDrainMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	private static PreDrainMode parsePreDrainMode(String raw,
			PreDrainMode fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		try {
			return PreDrainMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	private static JsonObject ensureWaterDrainNode(JsonObject collapse) {
		if (collapse.has("water_drain") && collapse.get("water_drain").isJsonObject()) {
			return collapse.getAsJsonObject("water_drain");
		}
		JsonObject node = new JsonObject();
		Collapse defaults = Collapse.defaults();
		node.addProperty("mode", defaults.waterDrainMode().name().toLowerCase(Locale.ROOT));
		node.addProperty("offset", defaults.waterDrainOffset());
		JsonObject deferred = new JsonObject();
		Collapse.WaterDrainDeferred drainDefaults = Collapse.WaterDrainDeferred.defaults();
		deferred.addProperty("enabled", drainDefaults.enabled());
		deferred.addProperty("initial_delay_ticks", drainDefaults.initialDelayTicks());
		deferred.addProperty("columns_per_tick", drainDefaults.columnsPerTick());
		node.add("deferred", deferred);
		collapse.add("water_drain", node);
		return node;
	}

	private static Effects parseEffects(JsonObject node) {
		if (node == null) {
			return Effects.defaults();
		}
		Effects defaults = Effects.defaults();
		String beam = node.has("beam_color") ? node.get("beam_color").getAsString() : defaults.beamColor();
		Identifier veil = node.has("veil_particles") ? parseIdentifier(node.get("veil_particles")) : defaults.veilParticles();
		Identifier ring = node.has("ring_particles") ? parseIdentifier(node.get("ring_particles")) : defaults.ringParticles();
		Identifier palette = node.has("palette") ? parseIdentifier(node.get("palette")) : defaults.effectPalette();
		if (veil == null) {
			veil = defaults.veilParticles();
		}
		if (ring == null) {
			ring = defaults.ringParticles();
		}
		if (palette == null) {
			palette = defaults.effectPalette();
		}
		return new Effects(beam, veil, ring, palette);
	}

	private static Physics parsePhysics(JsonObject node) {
		if (node == null) {
			return Physics.defaults();
		}
		double pullStrength = node.has("ring_pull_strength") ? node.get("ring_pull_strength").getAsDouble()
				: Physics.defaults().ringPullStrength();
		int pushRadius = node.has("push_radius") ? node.get("push_radius").getAsInt() : Physics.defaults().pushRadius();
		return new Physics(pullStrength, pushRadius);
	}

	private static boolean updateCollapseJson(Identifier scenarioId, Consumer<JsonObject> mutator) {
		ensureLoaded();
		ensureDirectory();
		Path path = profilePath(scenarioId);
		JsonObject root = readProfileJson(path);
		if (root == null) {
			root = serializeProfile(resolve(scenarioId));
		}
		JsonObject collapse = root.has("collapse") && root.get("collapse").isJsonObject()
				? root.get("collapse").getAsJsonObject()
				: new JsonObject();
		root.add("collapse", collapse);
		mutator.accept(collapse);
		try (var writer = Files.newBufferedWriter(path)) {
			GSON.toJson(root, writer);
		} catch (IOException ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to update profile {}", scenarioId, ex);
			return false;
		}
		PROFILE_PATHS.put(scenarioId, path);
		reload();
		return true;
	}

	private static Path profilePath(Identifier scenarioId) {
		Path path = PROFILE_PATHS.get(scenarioId);
		if (path != null) {
			return path;
		}
		String fileName = scenarioId.getNamespace() + "_" + scenarioId.getPath().replace('/', '_') + ".json";
		path = PROFILE_DIR.resolve(fileName);
		PROFILE_PATHS.put(scenarioId, path);
		return path;
	}

	private static JsonObject readProfileJson(Path path) {
		if (!Files.exists(path)) {
			return null;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed.isJsonObject()) {
				return parsed.getAsJsonObject();
			}
		} catch (Exception ex) {
			Logging.REGISTRY.error("[DimensionProfile] Failed to read {}", path.getFileName(), ex);
		}
		return null;
	}

	private static Identifier parseIdentifier(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return null;
		}
		String raw = element.getAsString();
		return Identifier.tryParse(raw);
	}
}

