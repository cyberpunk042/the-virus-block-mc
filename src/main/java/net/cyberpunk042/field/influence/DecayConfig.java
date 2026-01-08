package net.cyberpunk042.field.influence;

import com.google.gson.JsonObject;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for gradual decay of field properties over time.
 * 
 * <p>Used by LifecycleConfig to define how properties fade after spawn.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "decay": {
 *   "rate": 0.95,
 *   "min": 0.1
 * }
 * </pre>
 * 
 * @see LifecycleConfig
 */
public record DecayConfig(
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "0.95") float rate,
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true, defaultValue = "0.1") float min
){
    /** No decay (constant value). */
    public static final DecayConfig NONE = new DecayConfig(1.0f, 1.0f);
    
    /** Default gentle decay. */
    public static final DecayConfig DEFAULT = new DecayConfig(0.95f, 0.1f);
    
    /**
     * Creates a decay config.
     * @param rate Decay rate per tick (0.95 = 5% decay per tick)
     * @param min Minimum value (decay stops here)
     */
    public static DecayConfig of(@Range(ValueRange.POSITIVE) float rate, @Range(ValueRange.ALPHA) float min) {
        return new DecayConfig(rate, min);
    }
    
    /**
     * Serializes this decay config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    /** Whether decay is active. */
    public boolean isActive() {
        return rate < 1.0f;
    }
    
    /**
     * Applies decay to a value.
     * @param current Current value
     * @return Decayed value (clamped to min)
     */
    public float apply(float current) {
        if (!isActive()) return current;
        float decayed = current * rate;
        return Math.max(decayed, min);
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a DecayConfig from JSON.
     * @param json The JSON object
     * @return Parsed config or NONE if json is null
     */
    public static DecayConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        float rate = json.has("rate") ? json.get("rate").getAsFloat() : 0.95f;
        float min = json.has("min") ? json.get("min").getAsFloat() : 0.1f;
        
        return new DecayConfig(rate, min);
    }
}
