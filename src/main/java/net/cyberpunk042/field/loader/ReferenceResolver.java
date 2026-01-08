package net.cyberpunk042.field.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.log.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves $ref references in JSON field definitions.
 * 
 * <p>Per CLASS_DIAGRAM §15:
 * <ul>
 *   <li>{@link #resolve(String)} - resolves a reference path</li>
 *   <li>{@link #resolveWithOverrides(String, JsonObject)} - resolves with merged overrides</li>
 *   <li>Caches loaded references for performance</li>
 * </ul>
 * 
 * <h2>Reference Syntax</h2>
 * <pre>
 * "$shapes/smooth_sphere"     → config/the-virus-block/field_shapes/smooth_sphere.json
 * "$appearances/glowing"      → config/the-virus-block/field_appearances/glowing.json
 * </pre>
 * 
 * @see FieldLoader
 * @see DefaultsProvider
 */
public class ReferenceResolver {
    
    private static final String CONFIG_BASE = "config/the-virus-block/";
    
    /** Cache of loaded references. */
    private final Map<String, JsonObject> cache = new HashMap<>();
    
    // =========================================================================
    // Public API (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    /**
     * Resolves a reference string to a JsonObject.
     * 
     * @param ref Reference string (e.g., "$shapes/smooth_sphere")
     * @return Resolved JsonObject, or null if not found
     */
    @Nullable
    public JsonObject resolve(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        
        // Check cache
        if (cache.containsKey(ref)) {
            Logging.FIELD.topic("ref").trace("Cache hit: {}", ref);
            return cache.get(ref);
        }
        
        // Convert ref to path
        String path = refToPath(ref);
        JsonObject loaded = loadReference(path);
        
        if (loaded != null) {
            cache.put(ref, loaded);
        }
        
        return loaded;
    }
    
    /**
     * Resolves a reference with overrides merged on top.
     * 
     * @param ref Reference string (e.g., "$shapes/smooth_sphere")
     * @param overrides JsonObject with override values
     * @return Merged JsonObject (base + overrides)
     */
    public JsonObject resolveWithOverrides(String ref, JsonObject overrides) {
        JsonObject base = resolve(ref);
        if (base == null) {
            Logging.FIELD.topic("ref").warn("Reference not found: {}", ref);
            return overrides != null ? overrides : new JsonObject();
        }
        
        if (overrides == null || overrides.size() == 0) {
            return base;
        }
        
        return mergeJson(base, overrides);
    }
    
    /**
     * Resolves $ref in a JsonObject, applying any sibling overrides.
     * 
     * <p>If json contains "$ref", loads that reference and merges
     * any other fields as overrides.</p>
     * 
     * @param json JsonObject that may contain "$ref"
     * @return Resolved and merged JsonObject
     */
    public JsonObject resolveWithOverrides(JsonObject json) {
        if (json == null) {
            return new JsonObject();
        }
        
        if (!json.has("$ref")) {
            return json;
        }
        
        String ref = json.get("$ref").getAsString();
        
        // Build overrides from non-$ref fields
        JsonObject overrides = new JsonObject();
        for (String key : json.keySet()) {
            if (!"$ref".equals(key)) {
                overrides.add(key, json.get(key));
            }
        }
        
        return resolveWithOverrides(ref, overrides);
    }
    
    // =========================================================================
    // Private Methods (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    /**
     * Loads a reference from a file path.
     */
    private JsonObject loadReference(String path) {
        Path filePath = Paths.get(path);
        
        if (!Files.exists(filePath)) {
            Logging.FIELD.topic("ref").warn("Reference file not found: {}", path);
            return null;
        }
        
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                Logging.FIELD.topic("ref").trace("Loaded reference: {}", path);
                return element.getAsJsonObject();
            } else {
                Logging.FIELD.topic("ref").warn("Reference is not a JsonObject: {}", path);
                return null;
            }
        } catch (IOException e) {
            Logging.FIELD.topic("ref").error("Failed to load reference: {}", path, e);
            return null;
        }
    }
    
    /**
     * Merges base and overrides JsonObjects.
     * Override values take precedence.
     */
    private JsonObject mergeJson(JsonObject base, JsonObject overrides) {
        JsonObject result = base.deepCopy();
        
        for (String key : overrides.keySet()) {
            JsonElement overrideValue = overrides.get(key);
            
            // Deep merge for nested objects
            if (overrideValue.isJsonObject() && result.has(key) && result.get(key).isJsonObject()) {
                result.add(key, mergeJson(result.getAsJsonObject(key), overrideValue.getAsJsonObject()));
            } else {
                result.add(key, overrideValue);
            }
        }
        
        return result;
    }
    
    /**
     * Converts a $ref string to a file path.
     */
    private String refToPath(String ref) {
        // Remove leading $ if present
        String cleaned = ref.startsWith("$") ? ref.substring(1) : ref;
        
        // Map prefix to folder
        // $shapes/x → field_shapes/x.json
        // $appearances/x → field_appearances/x.json
        // etc.
        
        String folder;
        String name;
        
        int slashIndex = cleaned.indexOf('/');
        if (slashIndex > 0) {
            String prefix = cleaned.substring(0, slashIndex);
            name = cleaned.substring(slashIndex + 1);
            folder = "field_" + prefix;
        } else {
            folder = "fields";
            name = cleaned;
        }
        
        return CONFIG_BASE + folder + "/" + name + ".json";
    }
    
    /**
     * Clears the reference cache.
     */
    public void clearCache() {
        cache.clear();
        Logging.FIELD.topic("ref").debug("Reference cache cleared");
    }
}
