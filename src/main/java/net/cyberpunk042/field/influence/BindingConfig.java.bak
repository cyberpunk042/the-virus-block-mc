package net.cyberpunk042.field.influence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

/**
 * Configuration for a single property binding.
 * 
 * <p>Per ARCHITECTURE §12.1 and PARAMETERS §12.1:
 * <ul>
 *   <li>source - binding source ID (e.g., "player.health_percent")</li>
 *   <li>inputRange - range of input values [min, max]</li>
 *   <li>outputRange - range of output values [min, max]</li>
 *   <li>curve - interpolation curve</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "source": "player.health_percent",
 *   "inputRange": [0, 1],
 *   "outputRange": [0.3, 1.0],
 *   "curve": "ease_out"
 * }
 * </pre>
 * 
 * @see BindingSource
 * @see BindingResolver
 */
public record BindingConfig(
    String source,
    float inputMin,
    float inputMax,
    float outputMin,
    float outputMax,
    InterpolationCurve curve
) {
    /** Default binding (pass-through). */
    public static final BindingConfig DEFAULT = new BindingConfig(
        "player.health_percent", 0, 1, 0, 1, InterpolationCurve.LINEAR);
    
    /**
     * Creates a binding config.
     */
    public BindingConfig {
        if (source == null || source.isEmpty()) {
            source = "player.health_percent";
        }
        if (curve == null) {
            curve = InterpolationCurve.LINEAR;
        }
    }
    
    /**
     * Convenience constructor with arrays.
     */
    public static BindingConfig of(String source, float[] inputRange, float[] outputRange, InterpolationCurve curve) {
        float inMin = inputRange != null && inputRange.length >= 2 ? inputRange[0] : 0;
        float inMax = inputRange != null && inputRange.length >= 2 ? inputRange[1] : 1;
        float outMin = outputRange != null && outputRange.length >= 2 ? outputRange[0] : 0;
        float outMax = outputRange != null && outputRange.length >= 2 ? outputRange[1] : 1;
        return new BindingConfig(source, inMin, inMax, outMin, outMax, curve);
    }
    
    /**
     * Parses from JSON.
     * Supports both new field format and legacy array format.
     */
    public static BindingConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        String source = json.has("source") ? json.get("source").getAsString() : "player.health_percent";
        
        // Support both new field format and legacy array format
        float inputMin = 0, inputMax = 1;
        if (json.has("inputMin")) {
            // New format: direct fields
            inputMin = json.get("inputMin").getAsFloat();
            inputMax = json.has("inputMax") ? json.get("inputMax").getAsFloat() : 1;
        } else if (json.has("inputRange") && json.get("inputRange").isJsonArray()) {
            // Legacy format: array [min, max]
            JsonArray arr = json.getAsJsonArray("inputRange");
            if (arr.size() >= 2) {
                inputMin = arr.get(0).getAsFloat();
                inputMax = arr.get(1).getAsFloat();
            }
        }
        
        float outputMin = 0, outputMax = 1;
        if (json.has("outputMin")) {
            // New format: direct fields
            outputMin = json.get("outputMin").getAsFloat();
            outputMax = json.has("outputMax") ? json.get("outputMax").getAsFloat() : 1;
        } else if (json.has("outputRange") && json.get("outputRange").isJsonArray()) {
            // Legacy format: array [min, max]
            JsonArray arr = json.getAsJsonArray("outputRange");
            if (arr.size() >= 2) {
                outputMin = arr.get(0).getAsFloat();
                outputMax = arr.get(1).getAsFloat();
            }
        }
        
        InterpolationCurve curve = InterpolationCurve.LINEAR;
        if (json.has("curve")) {
            curve = InterpolationCurve.fromId(json.get("curve").getAsString());
        }
        
        Logging.FIELD.topic("binding").trace("Parsed BindingConfig: source={}, curve={}", source, curve);
        return new BindingConfig(source, inputMin, inputMax, outputMin, outputMax, curve);
    }
    
    /**
     * Builder pattern.
     */
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this binding config to JSON.
     * Uses field names directly for auto-serialization compatibility.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("source", source);
        json.addProperty("inputMin", inputMin);
        json.addProperty("inputMax", inputMax);
        json.addProperty("outputMin", outputMin);
        json.addProperty("outputMax", outputMax);
        json.addProperty("curve", curve.name().toLowerCase());
        return json;
    }


    
    public static class Builder {
        private String source = "player.health_percent";
        private float inputMin = 0, inputMax = 1;
        private float outputMin = 0, outputMax = 1;
        private InterpolationCurve curve = InterpolationCurve.LINEAR;
        
        public Builder source(String s) { this.source = s; return this; }
        public Builder inputRange(float min, float max) { this.inputMin = min; this.inputMax = max; return this; }
        public Builder outputRange(float min, float max) { this.outputMin = min; this.outputMax = max; return this; }
        public Builder curve(InterpolationCurve c) { this.curve = c; return this; }
        
        public BindingConfig build() {
            return new BindingConfig(source, inputMin, inputMax, outputMin, outputMax, curve);
        }
    }
}
