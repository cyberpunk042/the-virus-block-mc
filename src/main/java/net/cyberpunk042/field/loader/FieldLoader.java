package net.cyberpunk042.field.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.field.primitive.LinkResolver;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.SpinConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.AlphaRange;
import net.cyberpunk042.visual.layer.BlendMode;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.MaskType;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.DirectoryStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Loads field definitions from JSON files.
 * 
 * <p>Per CLASS_DIAGRAM §15:
 * <ul>
 *   <li>{@link #load(ResourceManager)} - loads all definitions</li>
 *   <li>{@link #reload()} - reloads definitions</li>
 *   <li>{@link #loadDefinition(Path)} - loads single definition</li>
 * </ul>
 * 
 * @see FieldDefinition
 * @see ReferenceResolver
 * @see DefaultsProvider
 */
public final class FieldLoader {
    
    private final ReferenceResolver referenceResolver;
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Map<String, FieldDefinition> loadedDefinitions = new HashMap<>();
    private ResourceManager lastResourceManager;
    
    public FieldLoader() {
        this.referenceResolver = new ReferenceResolver();
        Logging.FIELD.topic("init").debug("FieldLoader initialized");
    }
    
    // =========================================================================
    // Public API (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    /**
     * Loads all field definitions from ResourceManager.
     */
    public void load(ResourceManager resourceManager) {
        this.lastResourceManager = resourceManager;
        loadedDefinitions.clear();
        referenceResolver.clearCache();
        
        // Load from fields/ folder
        // Implementation depends on ResourceManager API
        Logging.FIELD.topic("load").debug("Loading field definitions...");
        
        // For now, definitions are loaded on-demand via loadDefinition()
    }
    
    /**
     * Reloads all definitions.
     */
    public void reload() {
        if (lastResourceManager != null) {
            load(lastResourceManager);
        }
        Logging.FIELD.topic("load").debug("Reloaded field definitions");
    }
    
    /**
     * Loads all definitions (alias for reload).
     * Used by FieldRegistry.registerDefaults().
     */
    public void loadAll() {
        reload();
    }
    
    /**
     * Loads a single field definition from path.
     */
    @Nullable
    public FieldDefinition loadDefinition(Path path) {
        Logging.FIELD.topic("load").debug("Loading field from: {}", path);
        
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            FieldDefinition def = parseDefinition(json);
            if (def != null) {
                loadedDefinitions.put(def.id(), def);
            }
            return def;
        } catch (IOException e) {
            Logging.FIELD.topic("load").error("Failed to read file: {}", path, e);
            return null;
        } catch (Exception e) {
            Logging.FIELD.topic("load").error("Failed to parse field: {}", path, e);
            return null;
        }
    }
    
    /**
     * Gets a loaded definition by ID.
     */
    @Nullable
    public FieldDefinition getDefinition(String id) {
        return loadedDefinitions.get(id);
    }
    
    // =========================================================================
    // Standalone Parsing Utilities (merged from FieldParser)
    // =========================================================================
    
    /**
     * Parses a field definition from a JSON string.
     * Uses $ref resolution and defaults via parseDefinition().
     * 
     * @param json the JSON string
     * @param id the identifier to assign
     * @return parsed definition, or null on error
     */
    @Nullable
    public FieldDefinition parseString(String json, Identifier id) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            // Use parseDefinition() to get $ref resolution and defaults
            // Note: parseDefinition() uses ID from JSON, so we need to override it in the JSON first
            if (id != null && !obj.has("id")) {
                obj.addProperty("id", id.toString());
            }
            FieldDefinition def = parseDefinition(obj);
            if (def != null) {
                loadedDefinitions.put(def.id(), def);
            }
            Logging.FIELD.topic("parse").debug("Parsed field from string: {}", id);
            return def;
        } catch (Exception e) {
            Logging.FIELD.topic("parse").error("Failed to parse field from string {}: {}", id, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a field definition from a classpath resource.
     * Uses $ref resolution and defaults via parseDefinition().
     * 
     * @param resourcePath path relative to assets/the-virus-block/
     * @return parsed definition, or null on error
     */
    @Nullable
    public FieldDefinition parseResource(String resourcePath) {
        String fullPath = "/assets/the-virus-block/" + resourcePath;
        
        try (InputStream stream = FieldLoader.class.getResourceAsStream(fullPath)) {
            if (stream == null) {
                Logging.FIELD.topic("parse").warn("Resource not found: {}", fullPath);
                return null;
            }
            
            String filename = resourcePath.contains("/") 
                ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                : resourcePath;
            String idStr = filename.endsWith(".json")
                ? filename.substring(0, filename.length() - 5)
                : filename;
            
            Identifier identifier = Identifier.of("the-virus-block", idStr);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            
            Logging.FIELD.topic("parse").debug("Reading field resource: {}", resourcePath);
            return parseString(json, identifier);
        } catch (IOException e) {
            Logging.FIELD.topic("parse").error("Failed to read field resource {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses all field definitions from a directory.
     * Uses $ref resolution and defaults for each file.
     * 
     * @param directory the directory to scan
     * @return list of parsed definitions (skips failures)
     */
    public List<FieldDefinition> parseDirectory(Path directory) {
        List<FieldDefinition> results = new ArrayList<>();
        
        if (!Files.isDirectory(directory)) {
            Logging.FIELD.topic("parse").warn("Not a directory: {}", directory);
            return results;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path path : stream) {
                FieldDefinition def = loadDefinition(path);
                if (def != null) {
                    results.add(def);
                }
            }
        } catch (IOException e) {
            Logging.FIELD.topic("parse").error("Failed to read directory {}: {}", directory, e.getMessage());
        }
        
        Logging.FIELD.topic("parse").info("Parsed {} field definitions from {}", results.size(), directory);
        return results;
    }
    
    // =========================================================================
    // Serialization Utilities
    // =========================================================================
    
    /**
     * Serializes a field definition to JSON string.
     * 
     * @param definition the definition to serialize
     * @return JSON string representation
     */
    public static String toJsonString(FieldDefinition definition) {
        if (definition == null) return "null";
        return GSON.toJson(definition.toJson());
    }
    
    /**
     * Static convenience method to parse a FieldDefinition from JSON.
     * Creates a temporary FieldLoader instance for parsing with $ref resolution.
     * 
     * @param json the JSON object to parse
     * @return parsed FieldDefinition
     */
    public static FieldDefinition fromJson(JsonObject json) {
        return new FieldLoader().parseDefinition(json);
    }
    
    /**
     * Writes a field definition to a file.
     * 
     * @param definition the definition to write
     * @param path the file path
     * @throws IOException if writing fails
     */
    public static void writeToFile(FieldDefinition definition, Path path) throws IOException {
        String json = toJsonString(definition);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        Logging.FIELD.topic("serialize").debug("Wrote field definition to: {}", path);
    }
    
    // =========================================================================
    // Parsing - Definition (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    /**
     * Parses a field definition from JSON.
     * Public for client-side parsing of network payloads.
     */
    public FieldDefinition parseDefinition(JsonObject json) {
        json = referenceResolver.resolveWithOverrides(json);
        
        String id = json.has("id") ? json.get("id").getAsString() : "unnamed";
        String theme = json.has("theme") ? json.get("theme").getAsString() : 
                      (json.has("themeId") ? json.get("themeId").getAsString() : null);
        
        FieldType fieldType = FieldType.SHIELD;
        if (json.has("fieldType") || json.has("type")) {
            String typeStr = json.has("fieldType") ? 
                json.get("fieldType").getAsString() : json.get("type").getAsString();
            try {
                fieldType = FieldType.valueOf(typeStr.toUpperCase());
            } catch (Exception e) {
                Logging.FIELD.topic("parse").warn("Unknown fieldType '{}', using SHIELD", typeStr);
            }
        }
        
        List<FieldLayer> layers = new ArrayList<>();
        if (json.has("layers")) {
            for (JsonElement layerEl : json.getAsJsonArray("layers")) {
                FieldLayer layer = parseLayer(layerEl.getAsJsonObject());
                if (layer != null) {
                    layers.add(layer);
                }
            }
        }
        
        // F150: Parse bindings
        Map<String, net.cyberpunk042.field.influence.BindingConfig> bindings = parseBindings(json);
        
        // F157: Parse triggers
        java.util.List<net.cyberpunk042.field.influence.TriggerConfig> triggers = parseTriggers(json);
        
        // Parse additional fields per CLASS_DIAGRAM §1
        float baseRadius = JsonParseUtils.getFloat(json, "baseRadius", 1.0f);
        
        net.cyberpunk042.field.Modifiers modifiers = 
            JsonParseUtils.parseOptional(json, "modifiers", net.cyberpunk042.field.Modifiers::fromJson);
        
        // Parse follow config (new unified format)
        // Also supports legacy 'prediction' and 'followMode' keys
        net.cyberpunk042.field.instance.FollowConfig follow = 
            JsonParseUtils.parseOptional(json, "follow", net.cyberpunk042.field.instance.FollowConfig::fromJson);
        
        net.cyberpunk042.field.BeamConfig beam = 
            JsonParseUtils.parseOptional(json, "beam", net.cyberpunk042.field.BeamConfig::fromJson);
        
        net.cyberpunk042.field.influence.LifecycleConfig lifecycle = 
            JsonParseUtils.parseOptional(json, "lifecycle", net.cyberpunk042.field.influence.LifecycleConfig::fromJson);
        
        // Parse force config (only meaningful for type == FORCE)
        net.cyberpunk042.field.force.ForceFieldConfig forceConfig = 
            JsonParseUtils.parseOptional(json, "forceConfig", net.cyberpunk042.field.force.ForceFieldConfig::fromJson);
        
        // Parse shockwave FX config (field-level visual effect)
        JsonObject shockwave = json.has("shockwave") ? json.getAsJsonObject("shockwave") : null;
        
        // Parse field visual config (Energy Orb effect)
        JsonObject fieldVisual = json.has("fieldVisual") ? json.getAsJsonObject("fieldVisual") : null;
        
        Logging.FIELD.topic("parse").debug("Parsed FieldDefinition '{}' with {} layers, {} bindings, {} triggers", 
            id, layers.size(), bindings.size(), triggers.size());
        return new FieldDefinition(
            id, fieldType, baseRadius, theme, layers,
            modifiers, follow, beam,
            bindings, triggers, lifecycle, forceConfig, shockwave, fieldVisual);
    }
    
    /**
     * F150: Parses the "bindings" block from JSON.
     */
    private Map<String, net.cyberpunk042.field.influence.BindingConfig> parseBindings(JsonObject json) {
        Map<String, net.cyberpunk042.field.influence.BindingConfig> bindings = 
            JsonParseUtils.parseMap(json, "bindings", net.cyberpunk042.field.influence.BindingConfig::fromJson);
        
        // Log parsed bindings
        for (Map.Entry<String, net.cyberpunk042.field.influence.BindingConfig> entry : bindings.entrySet()) {
            Logging.FIELD.topic("binding").trace("Parsed binding for '{}' -> {}", 
                entry.getKey(), entry.getValue().source());
        }
        
        return bindings;
    }
    
    /**
     * F157: Parses the "triggers" array from JSON.
     */
    private java.util.List<net.cyberpunk042.field.influence.TriggerConfig> parseTriggers(JsonObject json) {
        java.util.List<net.cyberpunk042.field.influence.TriggerConfig> triggers = 
            JsonParseUtils.parseArray(json, "triggers", net.cyberpunk042.field.influence.TriggerConfig::fromJson);
        
        // Log parsed triggers
        for (net.cyberpunk042.field.influence.TriggerConfig config : triggers) {
            Logging.FIELD.topic("trigger").trace("Parsed trigger: {} -> {}", config.event(), config.effect());
        }
        
        return triggers;
    }
    
    // =========================================================================
    // Parsing - Layer (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    private FieldLayer parseLayer(JsonObject json) {
        json = referenceResolver.resolveWithOverrides(json);
        
        String id = json.has("id") ? json.get("id").getAsString() : "layer";
        boolean visible = !json.has("visible") || json.get("visible").getAsBoolean();
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 1.0f;
        
        BlendMode blendMode = BlendMode.NORMAL;
        if (json.has("blendMode")) {
            try {
                blendMode = BlendMode.valueOf(json.get("blendMode").getAsString().toUpperCase());
            } catch (Exception e) { /* keep default */ }
        }
        
        Transform transform = json.has("transform") 
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : DefaultsProvider.getDefaultTransform();
        
        Animation animation = parseAnimationWithShorthand(json);
        
        List<Primitive> primitives = new ArrayList<>();
        if (json.has("primitives")) {
            for (JsonElement primEl : json.getAsJsonArray("primitives")) {
                Primitive prim = parsePrimitive(primEl.getAsJsonObject());
                if (prim != null) {
                    primitives.add(prim);
                }
            }
            LinkResolver.validate(primitives);
        }
        
        Logging.FIELD.topic("parse").trace("Parsed layer '{}' with {} primitives", id, primitives.size());
        return new FieldLayer(id, primitives, transform, animation, alpha, visible, blendMode);
    }
    
    // =========================================================================
    // Parsing - Primitive (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    private Primitive parsePrimitive(JsonObject json) {
        json = referenceResolver.resolveWithOverrides(json);
        
        String id = json.has("id") ? json.get("id").getAsString() : "prim_" + System.nanoTime();
        String type = json.has("type") ? json.get("type").getAsString() : "sphere";
        
        Shape shape = parseShape(type, json.has("shape") ? json.getAsJsonObject("shape") : null);
        Transform transform = json.has("transform")
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : DefaultsProvider.getDefaultTransform();
        FillConfig fill = parseFillWithShorthand(json);
        VisibilityMask visibility = parseVisibilityWithShorthand(json);
        ArrangementConfig arrangement = parseArrangementWithShorthand(json);
        Appearance appearance = parseAppearanceWithShorthand(json);
        Animation animation = parseAnimationWithShorthand(json);
        PrimitiveLink link = json.has("link")
            ? PrimitiveLink.fromJson(json.getAsJsonObject("link"))
            : PrimitiveLink.NONE;
        
        Logging.FIELD.topic("parse").trace("Parsed primitive '{}' type={}", id, type);
        return new SimplePrimitive(id, type, shape, transform, fill, visibility, 
            arrangement, appearance, animation, link);
    }
    
    // =========================================================================
    // Shape Parsing
    // =========================================================================
    
    private Shape parseShape(String type, @Nullable JsonObject json) {
        if (json == null) {
            return DefaultsProvider.getDefaultShape(type);
        }
        
        // Apply defaults first
        json = DefaultsProvider.applyDefaults(json, type);
        
        return switch (type.toLowerCase()) {
            case "sphere" -> SphereShape.fromJson(json);
            case "ring" -> RingShape.builder()
                .innerRadius(json.get("innerRadius").getAsFloat())
                .outerRadius(json.get("outerRadius").getAsFloat())
                .segments(json.get("segments").getAsInt())
                .build();
            case "prism" -> PrismShape.builder()
                .sides(json.get("sides").getAsInt())
                .radius(json.get("radius").getAsFloat())
                .height(json.get("height").getAsFloat())
                .build();
            case "cylinder" -> CylinderShape.builder()
                .radius(json.get("radius").getAsFloat())
                .height(json.get("height").getAsFloat())
                .segments(json.get("segments").getAsInt())
                .build();
            case "polyhedron" -> PolyhedronShape.builder()
                .polyType(PolyType.valueOf(json.get("polyType").getAsString().toUpperCase()))
                .radius(json.get("radius").getAsFloat())
                .build();
            case "kamehameha" -> KamehamehaShape.fromJson(json);
            default -> DefaultsProvider.getDefaultShape(type);
        };
    }
    
    // =========================================================================
    // Shorthand Parsing (per CLASS_DIAGRAM §13)
    // =========================================================================
    
    /**
     * Parses appearance with shorthand: "alpha": 0.5
     */
    private Appearance parseAppearanceWithShorthand(JsonObject json) {
        if (json.has("appearance")) {
            JsonObject appObj = json.getAsJsonObject("appearance");
            Appearance.Builder builder = Appearance.builder();
            if (appObj.has("color")) builder.color(appObj.get("color").getAsString());
            if (appObj.has("alpha")) {
                JsonElement alphaEl = appObj.get("alpha");
                if (alphaEl.isJsonPrimitive()) {
                    builder.alpha(AlphaRange.of(alphaEl.getAsFloat()));
                }
            }
            if (appObj.has("glow")) builder.glow(appObj.get("glow").getAsFloat());
            return builder.build();
        }
        
        Appearance.Builder builder = Appearance.builder();
        if (json.has("alpha")) {
            JsonElement alphaEl = json.get("alpha");
            if (alphaEl.isJsonPrimitive()) {
                builder.alpha(DefaultsProvider.expandAlpha(alphaEl.getAsFloat()));
            }
        }
        if (json.has("color")) {
            builder.color(json.get("color").getAsString());
        }
        return builder.build();
    }
    
    /**
     * Parses animation with shorthand: "spin": 0.02
     */
    private Animation parseAnimationWithShorthand(JsonObject json) {
        if (json.has("animation")) {
            JsonObject animObj = json.getAsJsonObject("animation");
            Animation.Builder builder = Animation.builder();
            if (animObj.has("spin")) {
                JsonElement spinEl = animObj.get("spin");
                if (spinEl.isJsonPrimitive()) {
                    builder.spin(DefaultsProvider.expandSpin(spinEl.getAsFloat()));
                } else if (spinEl.isJsonObject()) {
                    builder.spin(SpinConfig.fromJson(spinEl.getAsJsonObject()));
                }
            }
            if (animObj.has("pulse")) {
                JsonElement pulseEl = animObj.get("pulse");
                if (pulseEl.isJsonPrimitive()) {
                    builder.pulse(DefaultsProvider.expandPulse(pulseEl.getAsFloat()));
                } else if (pulseEl.isJsonObject()) {
                    builder.pulse(PulseConfig.fromJson(pulseEl.getAsJsonObject()));
                }
            }
            if (animObj.has("phase")) {
                builder.phase(animObj.get("phase").getAsFloat());
            }
            return builder.build();
        }
        
        Animation.Builder builder = Animation.builder();
        if (json.has("spin")) {
            JsonElement spinEl = json.get("spin");
            if (spinEl.isJsonPrimitive()) {
                builder.spin(DefaultsProvider.expandSpin(spinEl.getAsFloat()));
            }
        }
        if (json.has("pulse")) {
            JsonElement pulseEl = json.get("pulse");
            if (pulseEl.isJsonPrimitive()) {
                builder.pulse(DefaultsProvider.expandPulse(pulseEl.getAsFloat()));
            }
        }
        return builder.build();
    }
    
    /**
     * Parses visibility with shorthand: "visibility": "bands"
     */
    private VisibilityMask parseVisibilityWithShorthand(JsonObject json) {
        if (!json.has("visibility")) {
            return DefaultsProvider.getDefaultVisibility();
        }
        
        JsonElement visEl = json.get("visibility");
        
        // String shorthand: "visibility": "bands"
        if (visEl.isJsonPrimitive()) {
            String maskStr = visEl.getAsString().toUpperCase();
            try {
                MaskType mask = MaskType.valueOf(maskStr);
                return VisibilityMask.builder().mask(mask).build();
            } catch (Exception e) {
                return DefaultsProvider.getDefaultVisibility();
            }
        }
        
        // Full object
        JsonObject visObj = visEl.getAsJsonObject();
        VisibilityMask.Builder builder = VisibilityMask.builder();
        if (visObj.has("mask")) {
            try {
                builder.mask(MaskType.valueOf(visObj.get("mask").getAsString().toUpperCase()));
            } catch (Exception e) { /* keep default */ }
        }
        if (visObj.has("count")) builder.count(visObj.get("count").getAsInt());
        if (visObj.has("thickness")) builder.thickness(visObj.get("thickness").getAsFloat());
        if (visObj.has("invert")) builder.invert(visObj.get("invert").getAsBoolean());
        return builder.build();
    }
    
    /**
     * Parses arrangement with shorthand: "arrangement": "wave_1"
     */
    private ArrangementConfig parseArrangementWithShorthand(JsonObject json) {
        if (!json.has("arrangement")) {
            return ArrangementConfig.DEFAULT;
        }
        
        JsonElement arrEl = json.get("arrangement");
        
        // String shorthand: "arrangement": "wave_1"
        if (arrEl.isJsonPrimitive()) {
            String patternName = arrEl.getAsString();
            return ArrangementConfig.builder().defaultPattern(patternName).build();
        }
        
        // Full object
        JsonObject arrObj = arrEl.getAsJsonObject();
        ArrangementConfig.Builder builder = ArrangementConfig.builder();
        if (arrObj.has("default")) {
            builder.defaultPattern(arrObj.get("default").getAsString());
        }
        // Also check for "defaultPattern" (alternative key name)
        if (arrObj.has("defaultPattern")) {
            builder.defaultPattern(arrObj.get("defaultPattern").getAsString());
        }
        return builder.build();
    }
    
    /**
     * Parses fill with shorthand: "fill": "wireframe"
     */
    private FillConfig parseFillWithShorthand(JsonObject json) {
        if (!json.has("fill")) {
            return DefaultsProvider.getDefaultFill();
        }
        
        JsonElement fillEl = json.get("fill");
        
        // String shorthand
        if (fillEl.isJsonPrimitive()) {
            String mode = fillEl.getAsString().toUpperCase();
            return switch (mode) {
                case "SOLID" -> FillConfig.SOLID;
                case "WIREFRAME" -> DefaultsProvider.getWireframeFill();
                case "CAGE" -> DefaultsProvider.getCageFill();
                case "POINTS" -> DefaultsProvider.getPointsFill();
                default -> DefaultsProvider.getDefaultFill();
            };
        }
        
        // Full object
        JsonObject fillObj = fillEl.getAsJsonObject();
        FillConfig.Builder builder = FillConfig.builder();
        if (fillObj.has("mode")) {
            try {
                builder.mode(FillMode.valueOf(fillObj.get("mode").getAsString().toUpperCase()));
            } catch (Exception e) { /* keep default */ }
        }
        if (fillObj.has("wireThickness")) {
            builder.wireThickness(fillObj.get("wireThickness").getAsFloat());
        }
        if (fillObj.has("doubleSided")) {
            builder.doubleSided(fillObj.get("doubleSided").getAsBoolean());
        }
        return builder.build();
    }
}
