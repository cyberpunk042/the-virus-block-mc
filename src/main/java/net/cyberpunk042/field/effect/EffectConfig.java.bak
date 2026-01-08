package net.cyberpunk042.field.effect;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a field effect.
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>type</b>: Effect type (push, pull, damage, etc.)</li>
 *   <li><b>strength</b>: Effect intensity</li>
 *   <li><b>radius</b>: Effect radius (0 = use field radius)</li>
 *   <li><b>cooldown</b>: Ticks between applications</li>
 *   <li><b>params</b>: Additional type-specific parameters</li>
 * </ul>
 */
public record EffectConfig(
        EffectType type,
        float strength,
        float radius,
        int cooldown,
        Map<String, Object> params
) {
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static EffectConfig push(float strength) {
        return new EffectConfig(EffectType.PUSH, strength, 0, 0, Map.of());
    }
    
    public static EffectConfig pull(float strength) {
        return new EffectConfig(EffectType.PULL, strength, 0, 0, Map.of());
    }
    
    public static EffectConfig damage(float amount, int cooldown) {
        return new EffectConfig(EffectType.DAMAGE, amount, 0, cooldown, Map.of());
    }
    
    public static EffectConfig shield() {
        return new EffectConfig(EffectType.SHIELD, 1.0f, 0, 0, Map.of());
    }
    
    public static EffectConfig slow(float factor) {
        return new EffectConfig(EffectType.SLOW, factor, 0, 0, Map.of());
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // With methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    public EffectConfig withStrength(float newStrength) {
        return new EffectConfig(type, newStrength, radius, cooldown, params);
    }
    
    public EffectConfig withRadius(float newRadius) {
        return new EffectConfig(type, strength, newRadius, cooldown, params);
    }
    
    public EffectConfig withCooldown(int newCooldown) {
        return new EffectConfig(type, strength, radius, newCooldown, params);
    }
    
    public EffectConfig withParam(String key, Object value) {
        Map<String, Object> newParams = new HashMap<>(params);
        newParams.put(key, value);
        return new EffectConfig(type, strength, radius, cooldown, newParams);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Param access
    // ─────────────────────────────────────────────────────────────────────────────
    
    public <T> T getParam(String key, T defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            @SuppressWarnings("unchecked")
            T typed = (T) value;
            return typed;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    public boolean hasParam(String key) {
        return params.containsKey(key);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // JSON
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static EffectConfig fromJson(JsonObject json) {
        EffectType type = EffectType.fromId(
            json.has("type") ? json.get("type").getAsString() : "custom"
        );
        float strength = json.has("strength") ? json.get("strength").getAsFloat() : 1.0f;
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 0;
        int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
        
        Map<String, Object> params = new HashMap<>();
        if (json.has("params") && json.get("params").isJsonObject()) {
            JsonObject paramsJson = json.getAsJsonObject("params");
            for (String key : paramsJson.keySet()) {
                var element = paramsJson.get(key);
                if (element.isJsonPrimitive()) {
                    var prim = element.getAsJsonPrimitive();
                    if (prim.isBoolean()) {
                        params.put(key, prim.getAsBoolean());
                    } else if (prim.isNumber()) {
                        params.put(key, prim.getAsNumber());
                    } else {
                        params.put(key, prim.getAsString());
                    }
                }
            }
        }
        
        return new EffectConfig(type, strength, radius, cooldown, params);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.id());
        if (strength != 1.0f) json.addProperty("strength", strength);
        if (radius != 0) json.addProperty("radius", radius);
        if (cooldown != 0) json.addProperty("cooldown", cooldown);
        // Skip params serialization for simplicity
        return json;
    }
}
