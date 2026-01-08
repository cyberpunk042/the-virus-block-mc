package net.cyberpunk042.field.effect;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Utility for managing effects associated with field definitions.
 * 
 * <p>Since FieldDefinition is a record (immutable), we use a separate
 * registry to associate effects with field definitions.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register effects for a field definition
 * FieldEffects.register("shield_default", List.of(
 *     EffectConfig.damage("magic", 2.0f, 20),
 *     EffectConfig.knockback(1.5f)
 * ));
 * 
 * // Get effects for a field
 * List<EffectConfig> effects = FieldEffects.getEffects(definition);
 * </pre>
 */
public final class FieldEffects {
    
    private static final Map<String, List<EffectConfig>> EFFECTS = new HashMap<>();
    
    private FieldEffects() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers effects for a field definition ID.
     */
    public static void register(String fieldId, List<EffectConfig> effects) {
        EFFECTS.put(fieldId, new ArrayList<>(effects));
        Logging.REGISTRY.topic("effects").info(
            "Registered {} effects for field: {}", effects.size(), fieldId);
    }
    
    /**
     * Registers a single effect for a field definition ID.
     */
    public static void register(String fieldId, EffectConfig effect) {
        EFFECTS.computeIfAbsent(fieldId, k -> new ArrayList<>()).add(effect);
        Logging.REGISTRY.topic("effects").debug(
            "Added {} effect to field: {}", effect.type(), fieldId);
    }
    
    /**
     * Clears all registered effects.
     */
    public static void clear() {
        EFFECTS.clear();
        Logging.REGISTRY.topic("effects").info("Cleared all field effects");
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Gets effects for a field definition.
     */
    public static List<EffectConfig> getEffects(FieldDefinition definition) {
        if (definition == null) return Collections.emptyList();
        return getEffects(definition.id().toString());
    }
    
    /**
     * Gets effects for a field ID.
     */
    public static List<EffectConfig> getEffects(String fieldId) {
        return EFFECTS.getOrDefault(fieldId, Collections.emptyList());
    }
    
    /**
     * Checks if a field has any effects.
     */
    public static boolean hasEffects(String fieldId) {
        List<EffectConfig> effects = EFFECTS.get(fieldId);
        return effects != null && !effects.isEmpty();
    }
    
    /**
     * Gets all field IDs with registered effects.
     */
    public static Set<String> getFieldIds() {
        return Collections.unmodifiableSet(EFFECTS.keySet());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // JSON Parsing
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Parses effects from a JSON array and registers them.
     * 
     * @param fieldId The field definition ID
     * @param effectsArray JSON array of effect configs
     */
    public static void parseAndRegister(String fieldId, JsonArray effectsArray) {
        List<EffectConfig> effects = new ArrayList<>();
        
        for (JsonElement element : effectsArray) {
            if (!element.isJsonObject()) continue;
            
            JsonObject obj = element.getAsJsonObject();
            EffectConfig config = parseEffectConfig(obj);
            if (config != null) {
                effects.add(config);
            }
        }
        
        if (!effects.isEmpty()) {
            register(fieldId, effects);
        }
    }
    
    private static EffectConfig parseEffectConfig(JsonObject json) {
        try {
            String typeStr = json.has("type") ? json.get("type").getAsString() : "damage";
            EffectType type = EffectType.valueOf(typeStr.toUpperCase());
            
            float strength = json.has("strength") ? json.get("strength").getAsFloat() : 1.0f;
            int interval = json.has("interval") ? json.get("interval").getAsInt() : 20;
            float radius = json.has("radius") ? json.get("radius").getAsFloat() : -1;
            boolean affectsOwner = json.has("affectsOwner") && json.get("affectsOwner").getAsBoolean();
            
            return new EffectConfig(type, strength, radius, interval, java.util.Map.of());
            
        } catch (Exception e) {
            Logging.REGISTRY.topic("effects").warn(
                "Failed to parse effect config: {}", e.getMessage());
            return null;
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Default Effects
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers default effects for common field types.
     */
    public static void registerDefaults() {
        // Shield field - damages entities that touch it
        register("shield_default", List.of(
            EffectConfig.damage(2.0f, 10)
        ));
        
        // Force field - pushes entities away
        register("force_default", List.of(
            EffectConfig.push(1.5f)
        ));
        
        // Healing field - heals entities inside
        register("healing_field", List.of(
            new EffectConfig(EffectType.HEAL, 1.0f, 0, 40, Map.of())
        ));
        
        // Slowness field - slows entities inside
        register("slowness_field", List.of(
            EffectConfig.slow(2.0f)
        ));
        
        Logging.REGISTRY.topic("effects").info("Registered default field effects");
    }
}
