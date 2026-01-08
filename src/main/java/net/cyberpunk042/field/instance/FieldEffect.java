package net.cyberpunk042.field.instance;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.effect.EffectType;

import java.util.HashMap;
import java.util.Map;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for a field effect.
 * 
 * <p>Per ARCHITECTURE.md (line 151):
 * <pre>
 * field/instance/
 * └── FieldEffect.java  # push, pull, shield, damage
 * </pre>
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>type</b>: Effect type (push, pull, damage, etc.)</li>
 *   <li><b>strength</b>: Effect intensity</li>
 *   <li><b>radius</b>: Effect radius (0 = use field radius)</li>
 *   <li><b>cooldown</b>: Ticks between applications</li>
 *   <li><b>params</b>: Additional type-specific parameters</li>
 * </ul>
 * 
 * @see EffectType
 */
public record FieldEffect(
    EffectType type,
    @JsonField(skipIfDefault = true, defaultValue = "1.0") float strength,
    @JsonField(skipIfDefault = true) float radius,
    @JsonField(skipIfDefault = true) int cooldown,
    Map<String, Object> params
){
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static FieldEffect push(float strength) {
        return new FieldEffect(EffectType.PUSH, strength, 0, 0, Map.of());
    }
    
    public static FieldEffect pull(float strength) {
        return new FieldEffect(EffectType.PULL, strength, 0, 0, Map.of());
    }
    
    public static FieldEffect damage(float amount, int cooldown) {
        return new FieldEffect(EffectType.DAMAGE, amount, 0, cooldown, Map.of());
    }
    
    public static FieldEffect shield() {
        return new FieldEffect(EffectType.SHIELD, 1.0f, 0, 0, Map.of());
    }
    
    public static FieldEffect slow(float factor) {
        return new FieldEffect(EffectType.SLOW, factor, 0, 0, Map.of());
    }
    
    public static FieldEffect heal(float amount, int cooldown) {
        return new FieldEffect(EffectType.HEAL, amount, 0, cooldown, Map.of());
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // With methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    public FieldEffect withStrength(float newStrength) {
        return new FieldEffect(type, newStrength, radius, cooldown, params);
    }
    
    public FieldEffect withRadius(float newRadius) {
        return new FieldEffect(type, strength, newRadius, cooldown, params);
    }
    
    public FieldEffect withCooldown(int newCooldown) {
        return new FieldEffect(type, strength, radius, newCooldown, params);
    }
    
    public FieldEffect withParam(String key, Object value) {
        Map<String, Object> newParams = new HashMap<>(params);
        newParams.put(key, value);
        return new FieldEffect(type, strength, radius, cooldown, newParams);
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
    
    public static FieldEffect fromJson(JsonObject json) {
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
        
        return new FieldEffect(type, strength, radius, cooldown, params);
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Compatibility with EffectConfig
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Converts from the old EffectConfig type.
     * @deprecated Use FieldEffect directly
     */
    @Deprecated
    public static FieldEffect from(net.cyberpunk042.field.effect.EffectConfig config) {
        return new FieldEffect(config.type(), config.strength(), config.radius(), 
                               config.cooldown(), config.params());
    }
}

