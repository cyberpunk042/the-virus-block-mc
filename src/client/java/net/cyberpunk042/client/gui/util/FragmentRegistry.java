package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.visual.animation.PulseConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.client.gui.state.ChangeType;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.instance.FollowConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.cyberpunk042.TheVirusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FragmentRegistry - loads GUI presets from JSON config files.
 *
 * <p>Scans the following config folders:
 * <ul>
 *   <li>{@code config/the-virus-block/field_shapes/} - Shape presets (grouped by type)</li>
 *   <li>{@code config/the-virus-block/field_fills/} - Fill mode presets</li>
 *   <li>{@code config/the-virus-block/field_masks/} - Visibility mask presets</li>
 *   <li>{@code config/the-virus-block/field_arrangements/} - Pattern arrangement presets</li>
 *   <li>{@code config/the-virus-block/field_animations/} - Animation presets</li>
 *   <li>{@code config/the-virus-block/field_beams/} - Debug beam presets</li>
 *   <li>{@code config/the-virus-block/field_follows/} - Follow mode presets</li>
 *   <li>{@code config/the-virus-block/field_appearances/} - Appearance presets</li>
 *   <li>{@code config/the-virus-block/field_layers/} - Layer configuration presets</li>
 *   <li>{@code config/the-virus-block/field_links/} - Primitive link presets</li>
 *   <li>{@code config/the-virus-block/field_orbits/} - Orbit configuration presets</li>
 *   <li>{@code config/the-virus-block/field_predictions/} - Prediction presets</li>
 *   <li>{@code config/the-virus-block/field_primitives/} - Primitive presets</li>
 *   <li>{@code config/the-virus-block/field_transforms/} - Transform presets</li>
 * </ul>
 *
 * <p>Each JSON file represents one preset. The "name" field in JSON becomes the dropdown label.
 * Files are loaded on first access and cached. Call {@link #reload()} to refresh after config changes.
 *
 * <p>This integrates with the existing {@code $ref} system - presets ARE the target fragments
 * that {@code $ref} points to. The GUI just provides a convenient dropdown interface.
 */
public final class FragmentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("FragmentRegistry");
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir().resolve("the-virus-block");

    // Folder names
    private static final String SHAPES_FOLDER = "field_shapes";
    private static final String FILLS_FOLDER = "field_fills";
    private static final String MASKS_FOLDER = "field_masks";
    private static final String ARRANGEMENTS_FOLDER = "field_arrangements";
    private static final String ANIMATIONS_FOLDER = "field_animations";
    private static final String BEAMS_FOLDER = "field_beams";
    private static final String FOLLOW_FOLDER = "field_follows";
    private static final String APPEARANCES_FOLDER = "field_appearances";
    private static final String LAYERS_FOLDER = "field_layers";
    private static final String LINKS_FOLDER = "field_links";
    private static final String ORBITS_FOLDER = "field_orbits";
    private static final String PREDICTIONS_FOLDER = "field_predictions";
    private static final String PRIMITIVES_FOLDER = "field_primitives";
    private static final String TRANSFORMS_FOLDER = "field_transforms";
    private static final String FORCE_FOLDER = "field_force";
    private static final String SHOCKWAVE_FOLDER = "field_shockwave";
    private static final String FIELD_VISUAL_FOLDER = "field_visual";

    // Caches: presetName -> JsonObject
    private static final Map<String, Map<String, JsonObject>> shapePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> fillPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> maskPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> arrangementPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> animationPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> beamPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> followPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> appearancePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> layerPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> linkPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> orbitPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> predictionPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> primitivePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> transformPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> forcePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> shockwavePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> fieldVisualPresets = new ConcurrentHashMap<>();

    private static boolean loaded = false;

    private FragmentRegistry() {}

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reload all presets from disk. Call this after config files change.
     */
    public static synchronized void reload() {
        LOGGER.info("Reloading presets from config... (caller: {})", 
            new Throwable().getStackTrace()[1].toString());
        shapePresets.clear();
        fillPresets.clear();
        maskPresets.clear();
        arrangementPresets.clear();
        animationPresets.clear();
        beamPresets.clear();
        followPresets.clear();
        appearancePresets.clear();
        layerPresets.clear();
        linkPresets.clear();
        orbitPresets.clear();
        predictionPresets.clear();
        primitivePresets.clear();
        transformPresets.clear();
        forcePresets.clear();
        shockwavePresets.clear();
        loaded = false;
        ensureLoaded();
    }

    /**
     * Ensures presets are loaded (loads on first call, cached thereafter).
     * Use this for normal initialization - only reloads if not yet loaded.
     */
    public static synchronized void ensureLoaded() {
        if (loaded) return;

        // Create folders if they don't exist
        ensureFolder(SHAPES_FOLDER);
        ensureFolder(FILLS_FOLDER);
        ensureFolder(MASKS_FOLDER);
        ensureFolder(ARRANGEMENTS_FOLDER);
        ensureFolder(ANIMATIONS_FOLDER);
        ensureFolder(BEAMS_FOLDER);
        ensureFolder(FOLLOW_FOLDER);
        ensureFolder(APPEARANCES_FOLDER);
        ensureFolder(LAYERS_FOLDER);
        ensureFolder(LINKS_FOLDER);
        ensureFolder(ORBITS_FOLDER);
        ensureFolder(PREDICTIONS_FOLDER);
        ensureFolder(PRIMITIVES_FOLDER);
        ensureFolder(TRANSFORMS_FOLDER);
        ensureFolder(FORCE_FOLDER);
        ensureFolder(SHOCKWAVE_FOLDER);
        ensureFolder(FIELD_VISUAL_FOLDER);

        // Load each category
        loadShapeFragments();
        loadSimplePresets(FILLS_FOLDER, fillPresets);
        loadSimplePresets(MASKS_FOLDER, maskPresets);
        loadSimplePresets(ARRANGEMENTS_FOLDER, arrangementPresets);
        loadSimplePresets(ANIMATIONS_FOLDER, animationPresets);
        loadSimplePresets(BEAMS_FOLDER, beamPresets);
        loadSimplePresets(FOLLOW_FOLDER, followPresets);
        loadSimplePresets(APPEARANCES_FOLDER, appearancePresets);
        loadSimplePresets(LAYERS_FOLDER, layerPresets);
        loadSimplePresets(LINKS_FOLDER, linkPresets);
        loadSimplePresets(ORBITS_FOLDER, orbitPresets);
        loadSimplePresets(PREDICTIONS_FOLDER, predictionPresets);
        loadSimplePresets(PRIMITIVES_FOLDER, primitivePresets);
        loadSimplePresets(TRANSFORMS_FOLDER, transformPresets);
        loadSimplePresets(FORCE_FOLDER, forcePresets);
        loadSimplePresets(SHOCKWAVE_FOLDER, shockwavePresets);
        loadSimplePresets(FIELD_VISUAL_FOLDER, fieldVisualPresets);

        LOGGER.info("Presets loaded: shapes={}, fills={}, masks={}, arrangements={}, animations={}, beams={}, follow={}, appearances={}, layers={}, links={}, orbits={}, predictions={}, primitives={}, transforms={}",
            shapePresets.values().stream().mapToInt(Map::size).sum(),
            fillPresets.size(), maskPresets.size(), arrangementPresets.size(),
            animationPresets.size(), beamPresets.size(), followPresets.size(),
            appearancePresets.size(), layerPresets.size(), linkPresets.size(),
            orbitPresets.size(), predictionPresets.size(), primitivePresets.size(), transformPresets.size());

        loaded = true;

        // Diagnostic: log counts so we can see if config data is actually loaded
        LOGGER.info("Fragments loaded: shapes={}, fills={}, masks={}, arrangements={}, animations={}, beams={}, follows={}, appearances={}, layers={}, links={}, orbits={}, predictions={}, primitives={}, transforms={}",
            shapePresets.values().stream().mapToInt(Map::size).sum(),
            fillPresets.size(), maskPresets.size(), arrangementPresets.size(),
            animationPresets.size(), beamPresets.size(), followPresets.size(),
            appearancePresets.size(), layerPresets.size(), linkPresets.size(),
            orbitPresets.size(), predictionPresets.size(), primitivePresets.size(), transformPresets.size());
    }

    private static void ensureFolder(String folder) {
        Path path = CONFIG_ROOT.resolve(folder);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                LOGGER.debug("Created config folder: {}", path);
            } catch (IOException e) {
                LOGGER.warn("Failed to create folder {}: {}", path, e.getMessage());
            }
        }
    }

    private static final String MODID = TheVirusBlock.MOD_ID;

    private static void loadShapeFragments() {
        // 1) User config folder
        Path configFolder = CONFIG_ROOT.resolve(SHAPES_FOLDER);
        LOGGER.info("Looking for shape presets in config: {} (exists={})", configFolder, Files.exists(configFolder));
        if (Files.exists(configFolder)) {
            loadShapeFragmentsFromFolder(configFolder);
        }

        // 2) Built-in resources (fallback so presets show even on first launch)
        FabricLoader.getInstance().getModContainer(MODID).ifPresentOrElse(
            container -> {
                String resourcePath = "data/" + MODID + "/" + SHAPES_FOLDER;
                container.findPath(resourcePath)
                    .ifPresentOrElse(
                        path -> {
                            LOGGER.info("Found built-in shape presets at: {}", path);
                            loadShapeFragmentsFromFolder(path);
                        },
                        () -> LOGGER.warn("Built-in shape presets NOT FOUND at: {}", resourcePath)
                    );
            },
            () -> LOGGER.warn("Mod container not found for: {}", MODID)
        );
    }

    /**
     * Load shape presets from a given folder path containing *.json files.
     */
    private static void loadShapeFragmentsFromFolder(Path folder) {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    String name = json.has("name") ? json.get("name").getAsString() : file.getFileName().toString().replace(".json", "");
                    String type = json.has("type") ? json.get("type").getAsString().toLowerCase() : "sphere";

                    shapePresets.computeIfAbsent(type, k -> new LinkedHashMap<>()).put(name, json);
                    LOGGER.info("Loaded shape preset: {} (type={}) from {}", name, type, folder);
                    count++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to load shape preset {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan shapes folder {}: {}", folder, e.getMessage());
        }
        LOGGER.info("Loaded {} shape presets from {}, types available: {}", count, folder, shapePresets.keySet());
    }

    private static void loadSimplePresets(String folder, Map<String, JsonObject> target) {
        // 1) User config folder
        Path configPath = CONFIG_ROOT.resolve(folder);
        LOGGER.info("Looking for {} presets in config: {} (exists={})", folder, configPath, Files.exists(configPath));
        if (Files.exists(configPath)) {
            loadSimplePresetsFromFolder(configPath, target, folder);
        }

        // 2) Built-in resources (fallback) — ensures dropdowns populate even if config is empty
        FabricLoader.getInstance().getModContainer(MODID).ifPresent(container -> {
            String resourcePath = "data/" + MODID + "/" + folder;
            container.findPath(resourcePath)
                .ifPresentOrElse(
                    path -> {
                        LOGGER.info("Found built-in {} at: {}", folder, path);
                        loadSimplePresetsFromFolder(path, target, folder);
                    },
                    () -> LOGGER.warn("Built-in {} NOT FOUND at: {}", folder, resourcePath)
                );
        });
    }

    /**
     * Load simple presets (non-shape categories) from a folder of *.json files.
     */
    private static void loadSimplePresetsFromFolder(Path path, Map<String, JsonObject> target, String folderName) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.json")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    String name = json.has("name") ? json.get("name").getAsString() : file.getFileName().toString().replace(".json", "");
                    target.put(name, json);
                    LOGGER.debug("Loaded preset from {}: {}", path, name);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load preset {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan {} folder: {}", folderName, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHAPE PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listShapeFragments(String shapeType) {
        ensureLoaded();
        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        List<String> list = new ArrayList<>();
        if (map != null) {
            list.addAll(map.keySet());
        }
        // Always include Default and Custom
        if (!list.contains("Default")) list.add(0, "Default");
        if (!list.contains("Custom")) list.add("Custom");
        LOGGER.info("listShapeFragments('{}') -> map={}, result={}", shapeType, 
            map != null ? map.keySet() : "null", list);
        return list;
    }

    public static void applyShapeFragment(FieldEditState state, String shapeType, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return; // Custom = user-defined, don't overwrite

        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        if (map == null) return;
        JsonObject json = map.get(presetName);
        if (json == null) return;

        // Apply based on shape type
        switch (shapeType.toLowerCase()) {
            case "sphere" -> applySpherePreset(state, json);
            case "ring" -> applyRingPreset(state, json);
            case "disc" -> applyDiscPreset(state, json);
            case "prism" -> applyPrismPreset(state, json);
            case "cylinder", "beam" -> applyCylinderPreset(state, json);
            case "cube", "octahedron", "icosahedron" -> applyPolyPreset(state, json);
        }
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    private static void applySpherePreset(FieldEditState state, JsonObject json) {
        if (json.has("radius")) state.set("sphere.radius", json.get("radius").getAsFloat());
        if (json.has("latSteps")) state.set("sphere.latSteps", json.get("latSteps").getAsInt());
        if (json.has("lonSteps")) state.set("sphere.lonSteps", json.get("lonSteps").getAsInt());
        if (json.has("latStart")) state.set("sphere.latStart", json.get("latStart").getAsFloat());
        if (json.has("latEnd")) state.set("sphere.latEnd", json.get("latEnd").getAsFloat());
        if (json.has("algorithm")) state.set("sphere.algorithm", json.get("algorithm").getAsString());
    }

    private static void applyRingPreset(FieldEditState state, JsonObject json) {
        if (json.has("innerRadius")) state.set("ring.innerRadius", json.get("innerRadius").getAsFloat());
        if (json.has("outerRadius")) state.set("ring.outerRadius", json.get("outerRadius").getAsFloat());
        if (json.has("segments")) state.set("ring.segments", json.get("segments").getAsInt());
        if (json.has("height")) state.set("ring.height", json.get("height").getAsFloat());
        if (json.has("y")) state.set("ring.y", json.get("y").getAsFloat());
    }

    private static void applyDiscPreset(FieldEditState state, JsonObject json) {
        if (json.has("radius")) state.set("disc.radius", json.get("radius").getAsFloat());
        if (json.has("segments")) state.set("disc.segments", json.get("segments").getAsInt());
        if (json.has("y")) state.set("disc.y", json.get("y").getAsFloat());
        if (json.has("innerRadius")) state.set("disc.innerRadius", json.get("innerRadius").getAsFloat());
    }

    private static void applyPrismPreset(FieldEditState state, JsonObject json) {
        if (json.has("sides")) state.set("prism.sides", json.get("sides").getAsInt());
        if (json.has("radius")) state.set("prism.radius", json.get("radius").getAsFloat());
        if (json.has("height")) state.set("prism.height", json.get("height").getAsFloat());
        if (json.has("topRadius")) state.set("prism.topRadius", json.get("topRadius").getAsFloat());
    }

    private static void applyCylinderPreset(FieldEditState state, JsonObject json) {
        if (json.has("radius")) state.set("cylinder.radius", json.get("radius").getAsFloat());
        if (json.has("height")) state.set("cylinder.height", json.get("height").getAsFloat());
        if (json.has("segments")) state.set("cylinder.segments", json.get("segments").getAsInt());
        if (json.has("topRadius")) state.set("cylinder.topRadius", json.get("topRadius").getAsFloat());
    }

    private static void applyPolyPreset(FieldEditState state, JsonObject json) {
        if (json.has("type")) state.set("polyhedron.type", json.get("type").getAsString());
        if (json.has("radius")) state.set("polyhedron.radius", json.get("radius").getAsFloat());
        if (json.has("subdivisions")) state.set("polyhedron.subdivisions", json.get("subdivisions").getAsInt());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FILL PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listFillFragments() {
        ensureLoaded();
        return withDefaults(fillPresets.keySet());
    }

    public static void applyFillFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        
        if ("Custom".equals(presetName) || "Default".equals(presetName)) {
            return;
        }

        JsonObject json = fillPresets.get(presetName);
        if (json == null) {
            LOGGER.warn("Fill preset '{}' not found. Available: {}", presetName, fillPresets.keySet());
            return;
        }

        // Apply fill mode (critical for rendering)
        if (json.has("mode")) {
            state.set("fill.mode", json.get("mode").getAsString());
        }
        
        if (json.has("wireThickness")) state.set("fill.wireThickness", json.get("wireThickness").getAsFloat());
        if (json.has("doubleSided")) state.set("fill.doubleSided", json.get("doubleSided").getAsBoolean());
        if (json.has("depthTest")) state.set("fill.depthTest", json.get("depthTest").getAsBoolean());
        if (json.has("depthWrite")) state.set("fill.depthWrite", json.get("depthWrite").getAsBoolean());
        if (json.has("pointSize")) state.set("fill.pointSize", json.get("pointSize").getAsFloat());

        // Cage settings - build proper cage object based on shape type
        if (json.has("cage")) {
            JsonObject cage = json.getAsJsonObject("cage");
            String shapeType = state.getString("shapeType");
            
            // Build cage options via adapter for shape-appropriate handling
            var adapter = net.cyberpunk042.visual.fill.CageOptionsAdapter.forShape(shapeType, state.fill().cage());
            
            if (adapter.supportsCountOptions()) {
                if (cage.has("latitudeCount")) {
                    adapter = adapter.withPrimaryCount(cage.get("latitudeCount").getAsInt());
                }
                if (cage.has("longitudeCount")) {
                    adapter = adapter.withSecondaryCount(cage.get("longitudeCount").getAsInt());
                }
                if (cage.has("primaryCount")) {
                    adapter = adapter.withPrimaryCount(cage.get("primaryCount").getAsInt());
                }
                if (cage.has("secondaryCount")) {
                    adapter = adapter.withSecondaryCount(cage.get("secondaryCount").getAsInt());
                }
            }
            if (cage.has("lineWidth")) {
                adapter = adapter.withLineWidth(cage.get("lineWidth").getAsFloat());
            }
            if (cage.has("showEdges")) {
                adapter = adapter.withShowEdges(cage.get("showEdges").getAsBoolean());
            }
            
            // Update fill config with new cage
            var newFill = state.fill().toBuilder().cage(adapter.build()).build();
            state.set("fill", newFill);
        }
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VISIBILITY PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listVisibilityFragments() {
        ensureLoaded();
        return withDefaults(maskPresets.keySet());
    }

    public static void applyVisibilityFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = maskPresets.get(presetName);
        if (json == null) return;

        if (json.has("mask")) state.set("mask.mask", json.get("mask").getAsString());
        if (json.has("count")) state.set("mask.count", json.get("count").getAsInt());
        if (json.has("thickness")) state.set("mask.thickness", json.get("thickness").getAsFloat());
        if (json.has("offset")) state.set("mask.offset", json.get("offset").getAsFloat());
        if (json.has("invert")) state.set("mask.invert", json.get("invert").getAsBoolean());
        if (json.has("feather")) state.set("mask.feather", json.get("feather").getAsFloat());
        if (json.has("animate")) state.set("mask.animate", json.get("animate").getAsBoolean());
        if (json.has("animateSpeed")) state.set("mask.animSpeed", json.get("animateSpeed").getAsFloat());
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ARRANGEMENT PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listArrangementFragments() {
        ensureLoaded();
        return withDefaults(arrangementPresets.keySet());
    }

    public static void applyArrangementFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = arrangementPresets.get(presetName);
        if (json == null) return;

        if (json.has("quad")) state.set("arrangement.quadPattern", json.get("quad").getAsString());
        if (json.has("segment")) state.set("arrangement.segmentPattern", json.get("segment").getAsString());
        if (json.has("sector")) state.set("arrangement.sectorPattern", json.get("sector").getAsString());
        // multiPart is UI-only, not stored in config
        if (json.has("defaultPattern")) state.set("arrangement.defaultPattern", json.get("defaultPattern").getAsString());
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ANIMATION PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listAnimationFragments() {
        ensureLoaded();
        return withDefaults(animationPresets.keySet());
    }

    public static void applyAnimationFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = animationPresets.get(presetName);
        if (json == null) return;

        // Spin (per-axis or legacy format)
        if (json.has("spin")) {
            JsonObject spin = json.getAsJsonObject("spin");
            
            // New per-axis format
            if (spin.has("speedX")) state.set("spin.speedX", spin.get("speedX").getAsFloat());
            if (spin.has("speedY")) state.set("spin.speedY", spin.get("speedY").getAsFloat());
            if (spin.has("speedZ")) state.set("spin.speedZ", spin.get("speedZ").getAsFloat());
            if (spin.has("oscillateX")) state.set("spin.oscillateX", spin.get("oscillateX").getAsBoolean());
            if (spin.has("oscillateY")) state.set("spin.oscillateY", spin.get("oscillateY").getAsBoolean());
            if (spin.has("oscillateZ")) state.set("spin.oscillateZ", spin.get("oscillateZ").getAsBoolean());
            if (spin.has("rangeX")) state.set("spin.rangeX", spin.get("rangeX").getAsFloat());
            if (spin.has("rangeY")) state.set("spin.rangeY", spin.get("rangeY").getAsFloat());
            if (spin.has("rangeZ")) state.set("spin.rangeZ", spin.get("rangeZ").getAsFloat());
            
            // Legacy format (axis + speed) - convert to per-axis
            if (spin.has("axis") && spin.has("speed")) {
                String axis = spin.get("axis").getAsString().toUpperCase();
                float speed = spin.get("speed").getAsFloat();
                boolean oscillate = spin.has("oscillate") && spin.get("oscillate").getAsBoolean();
                float range = spin.has("range") ? spin.get("range").getAsFloat() : 360f;
                
                // Reset all axes first
                state.set("spin.speedX", 0f);
                state.set("spin.speedY", 0f);
                state.set("spin.speedZ", 0f);
                
                // Apply to the specified axis
                switch (axis) {
                    case "X" -> { state.set("spin.speedX", speed); state.set("spin.oscillateX", oscillate); state.set("spin.rangeX", range); }
                    case "Y" -> { state.set("spin.speedY", speed); state.set("spin.oscillateY", oscillate); state.set("spin.rangeY", range); }
                    case "Z" -> { state.set("spin.speedZ", speed); state.set("spin.oscillateZ", oscillate); state.set("spin.rangeZ", range); }
                    case "XY" -> { 
                        state.set("spin.speedX", speed); state.set("spin.speedY", speed);
                        state.set("spin.oscillateX", oscillate); state.set("spin.oscillateY", oscillate);
                        state.set("spin.rangeX", range); state.set("spin.rangeY", range);
                    }
                }
            }
        }

        // Pulse (enabled = has non-zero speed/scale)
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            if (pulse.has("mode")) state.set("pulse.mode", pulse.get("mode").getAsString());
            if (pulse.has("frequency")) state.set("pulse.speed", pulse.get("frequency").getAsFloat());
            if (pulse.has("speed")) state.set("pulse.speed", pulse.get("speed").getAsFloat());
            if (pulse.has("amplitude")) state.set("pulse.scale", pulse.get("amplitude").getAsFloat());
            if (pulse.has("scale")) state.set("pulse.scale", pulse.get("scale").getAsFloat());
        }

        // Alpha pulse (enabled = min != max && speed != 0)
        if (json.has("alphaPulse")) {
            JsonObject alpha = json.getAsJsonObject("alphaPulse");
            if (alpha.has("min")) state.set("alphaPulse.min", alpha.get("min").getAsFloat());
            if (alpha.has("max")) state.set("alphaPulse.max", alpha.get("max").getAsFloat());
            if (alpha.has("speed")) state.set("alphaPulse.speed", alpha.get("speed").getAsFloat());
        }
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BEAM PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listBeamFragments() {
        ensureLoaded();
        return withDefaults(beamPresets.keySet());
    }

    public static void applyBeamFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = beamPresets.get(presetName);
        if (json == null) return;

        if (json.has("enabled")) state.set("beam.enabled", json.get("enabled").getAsBoolean());
        if (json.has("innerRadius")) state.set("beam.innerRadius", json.get("innerRadius").getAsFloat());
        if (json.has("outerRadius")) state.set("beam.outerRadius", json.get("outerRadius").getAsFloat());
        if (json.has("height")) state.set("beam.height", json.get("height").getAsFloat());
        if (json.has("glow")) state.set("beam.glow", json.get("glow").getAsFloat());

        // Pulse settings - need to ensure pulse is not null before setting nested properties
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            
            // First, ensure beam has a non-null pulse (initialize with defaults if needed)
            if (state.beam() == null || state.beam().pulse() == null) {
                // Set default pulse config so nested properties can be set
                state.set("beam.pulse", PulseConfig.DEFAULT);
            }
            
            if (pulse.has("enabled") && !pulse.get("enabled").getAsBoolean()) {
                state.set("beam.pulse", PulseConfig.NONE);
            } else {
                // Now we can safely set nested properties
                if (pulse.has("scale")) state.set("beam.pulse.scale", pulse.get("scale").getAsFloat());
                if (pulse.has("speed")) state.set("beam.pulse.speed", pulse.get("speed").getAsFloat());
                if (pulse.has("waveform")) state.set("beam.pulse.waveform", pulse.get("waveform").getAsString());
                if (pulse.has("min")) state.set("beam.pulse.min", pulse.get("min").getAsFloat());
                if (pulse.has("max")) state.set("beam.pulse.max", pulse.get("max").getAsFloat());
            }
        }
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listFollowFragments() {
        ensureLoaded();
        return withDefaults(followPresets.keySet());
    }

    public static void applyFollowFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = followPresets.get(presetName);
        if (json == null) return;

        // Apply new FollowConfig fields
        if (json.has("enabled")) state.set("follow.enabled", json.get("enabled").getAsBoolean());
        if (json.has("leadOffset")) state.set("follow.leadOffset", json.get("leadOffset").getAsFloat());
        if (json.has("responsiveness")) state.set("follow.responsiveness", json.get("responsiveness").getAsFloat());
        if (json.has("lookAhead")) state.set("follow.lookAhead", json.get("lookAhead").getAsFloat());
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // APPEARANCE PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listAppearanceFragments() {
        ensureLoaded();
        return withDefaults(appearancePresets.keySet());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORBIT PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listOrbitFragments() {
        ensureLoaded();
        return withDefaults(orbitPresets.keySet());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRANSFORM PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listTransformFragments() {
        ensureLoaded();
        return withDefaults(transformPresets.keySet());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PREDICTION PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listPredictionFragments() {
        ensureLoaded();
        return withDefaults(predictionPresets.keySet());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the raw JSON for a preset (for advanced usage or $ref integration).
     */
    public static Optional<JsonObject> getPresetJson(String category, String presetName) {
        ensureLoaded();
        return Optional.ofNullable(switch (category.toLowerCase()) {
            case "fill", "fills" -> fillPresets.get(presetName);
            case "mask", "masks", "visibility" -> maskPresets.get(presetName);
            case "arrangement", "arrangements" -> arrangementPresets.get(presetName);
            case "animation", "animations" -> animationPresets.get(presetName);
            case "beam", "beams" -> beamPresets.get(presetName);
            case "follow", "follows" -> followPresets.get(presetName);
            case "appearance", "appearances" -> appearancePresets.get(presetName);
            case "layer", "layers" -> layerPresets.get(presetName);
            case "link", "links" -> linkPresets.get(presetName);
            case "orbit", "orbits" -> orbitPresets.get(presetName);
            case "prediction", "predictions" -> predictionPresets.get(presetName);
            case "primitive", "primitives" -> primitivePresets.get(presetName);
            case "transform", "transforms" -> transformPresets.get(presetName);
            default -> null;
        });
    }

    /**
     * Get shape preset JSON (separate because shapes are grouped by type).
     */
    public static Optional<JsonObject> getShapeFragmentJson(String shapeType, String presetName) {
        ensureLoaded();
        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(presetName));
    }

    private static List<String> withDefaults(Collection<String> names) {
        List<String> list = new ArrayList<>(names);
        // Ensure Default first, Custom last
        list.remove("Default");
        list.remove("Custom");
        list.add(0, "Default");
        list.add("Custom");
        return list;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GENERIC $REF LOADING (for commands)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Apply a fragment from a $ref path to state.
     * Routes to the appropriate category-specific apply method.
     * 
     * @param state The state to modify
     * @param category Category name (shape, fill, visibility, appearance, animation, transform)
     * @param ref Reference path like "$field_shapes/simple_sphere" or "field_shapes/simple_sphere"
     */
    public static void applyFragment(FieldEditState state, String category, String ref) {
        // Parse the ref: "$field_shapes/preset_name" or "field_shapes/preset_name"
        String cleanRef = ref.startsWith("$") ? ref.substring(1) : ref;
        String[] parts = cleanRef.split("/", 2);
        if (parts.length < 2) {
            LOGGER.warn("Invalid fragment ref format: {}", ref);
            return;
        }
        
        String folder = parts[0];
        String presetName = parts[1];
        
        LOGGER.debug("Applying fragment: category={}, folder={}, preset={}", category, folder, presetName);
        
        switch (category.toLowerCase()) {
            case "shape" -> {
                // Shape presets might be in field_shapes/<shape_type>/<preset>.json
                // or just field_shapes/<preset>.json for generic shape settings
                String shapeType = state.getString("shapeType");
                applyShapeFragment(state, shapeType, presetName);
            }
            case "fill" -> applyFillFragment(state, presetName);
            case "visibility" -> applyVisibilityFragment(state, presetName);
            case "appearance" -> {
                // Appearance might be in various folders
                // For now, try animation as it contains appearance-like settings
                applyAnimationFragment(state, presetName);
            }
            case "animation" -> applyAnimationFragment(state, presetName);
            case "transform" -> {
                // Transform isn't a separate folder - apply from JSON directly
                getPresetJson("transform", presetName).ifPresent(json -> {
                    if (json.has("anchor")) state.set("transform.anchor", json.get("anchor").getAsString());
                    if (json.has("scale")) state.set("transform.scale", json.get("scale").getAsFloat());
                    // Offset and rotation would need composite setters
                });
            }
            case "arrangement" -> applyArrangementFragment(state, presetName);
            case "beam" -> applyBeamFragment(state, presetName);
            case "follow" -> applyFollowFragment(state, presetName);
            default -> LOGGER.warn("Unknown fragment category: {}", category);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FORCE FRAGMENTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Lists available force field presets.
     * Returns preset names suitable for dropdown display.
     */
    public static List<String> listForceFragments() {
        ensureLoaded();
        List<String> names = new ArrayList<>(forcePresets.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
    
    /**
     * Gets the JSON for a force preset.
     */
    public static Optional<JsonObject> getForceJson(String presetName) {
        ensureLoaded();
        return Optional.ofNullable(forcePresets.get(presetName));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHOCKWAVE FRAGMENTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Lists available shockwave presets.
     * Returns preset names suitable for dropdown display.
     */
    public static List<String> listShockwaveFragments() {
        ensureLoaded();
        return withDefaults(shockwavePresets.keySet());
    }
    
    /**
     * Apply a shockwave preset to current state.
     * Replaces the entire shockwave config with the preset values.
     */
    public static void applyShockwaveFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName) || "Default".equals(presetName)) {
            return;
        }

        JsonObject json = shockwavePresets.get(presetName);
        if (json == null) {
            LOGGER.warn("Shockwave preset '{}' not found. Available: {}", presetName, shockwavePresets.keySet());
            return;
        }

        // Reset shader params to defaults first, then load the preset
        // This ensures values not in the preset return to defaults
        state.shockwaveAdapter().reset();
        
        // Use the adapter's loadFromJson which handles ALL params correctly
        state.shockwaveAdapter().loadFromJson(json);
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
        LOGGER.info("Applied shockwave preset: {}", presetName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FIELD VISUAL FRAGMENTS (Energy Orb effect)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Lists available field visual presets (all types).
     * Returns preset names suitable for dropdown display.
     */
    public static List<String> listFieldVisualFragments() {
        ensureLoaded();
        return withDefaults(fieldVisualPresets.keySet());
    }
    
    /**
     * Lists available field visual presets filtered by effect type.
     * Returns only presets matching the specified EffectType.
     * 
     * @param effectType The effect type to filter by (e.g., ENERGY_ORB, GEODESIC)
     * @return List of preset names for the given effect type
     */
    public static List<String> listFieldVisualFragments(net.cyberpunk042.client.visual.effect.EffectType effectType) {
        ensureLoaded();
        
        Set<String> filtered = new java.util.LinkedHashSet<>();
        for (var entry : fieldVisualPresets.entrySet()) {
            String presetName = entry.getKey();
            JsonObject json = entry.getValue();
            
            // Check if preset has effectType field
            if (json.has("effectType")) {
                try {
                    String presetEffectType = json.get("effectType").getAsString().toUpperCase();
                    if (presetEffectType.equals(effectType.name())) {
                        filtered.add(presetName);
                    }
                } catch (Exception e) {
                    // Skip malformed presets
                }
            } else {
                // Presets without effectType default to ENERGY_ORB
                if (effectType == net.cyberpunk042.client.visual.effect.EffectType.ENERGY_ORB) {
                    filtered.add(presetName);
                }
            }
        }
        
        return withDefaults(filtered);
    }
    
    /**
     * Apply a field visual preset to current state.
     * Replaces the field visual config with the preset values.
     */
    public static void applyFieldVisualFragment(FieldEditState state, String presetName) {
        applyFieldVisualFragment(state, presetName, false);
    }
    
    /**
     * Apply a field visual preset to current state with optional version skipping.
     * 
     * @param state The state to modify
     * @param presetName The preset to apply
     * @param skipVersion If true, the version in the preset is ignored (for version switching)
     */
    public static void applyFieldVisualFragment(FieldEditState state, String presetName, boolean skipVersion) {
        ensureLoaded();
        if ("Custom".equals(presetName) || "Default".equals(presetName)) {
            return;
        }

        JsonObject json = fieldVisualPresets.get(presetName);
        if (json == null) {
            LOGGER.warn("Field visual preset '{}' not found. Available: {}", presetName, fieldVisualPresets.keySet());
            return;
        }

        // Reset shader params to defaults first, then load the preset
        // This ensures values not in the preset return to defaults
        state.fieldVisualAdapter().reset();
        
        // Use the adapter's loadFromJson which handles ALL params correctly
        // including noiseResHigh, flames, corona, lighting, etc.
        state.fieldVisualAdapter().loadFromJson(json, skipVersion);
        
        // Sync to shader effect
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            state.fieldVisualAdapter().syncToEffect(client.player.getBoundingBox().getCenter());
        }
        
        // Notify listeners that a fragment was applied
        state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);
        LOGGER.info("Applied field visual preset: {}{}", presetName, skipVersion ? " (version preserved)" : "");
    }
    
}
