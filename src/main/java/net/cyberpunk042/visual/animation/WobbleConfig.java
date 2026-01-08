package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for random jitter/wobble animation (FUTURE).
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "wobble": {
 *   "amplitude": [0.1, 0.05, 0.1],
 *   "speed": 1.0,
 *   "randomize": true
 * }
 * </pre>
 * 
 * @see Animation
 */
public record WobbleConfig(
    @JsonField(skipIfNull = true) @Nullable Vector3f amplitude,
    @Range(ValueRange.POSITIVE) float speed,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean randomize
) {
    /**
     * Parses WobbleConfig from JSON.
     * 
     * @param json JSON object with amplitude (array or number), speed, randomize fields
     * @return parsed config or NONE if invalid
     */
    public static WobbleConfig fromJson(com.google.gson.JsonObject json) {
        if (json == null) return NONE;
        
        // Parse amplitude - can be array [x, y, z] or single number
        Vector3f amplitude = null;
        if (json.has("amplitude")) {
            var ampElem = json.get("amplitude");
            if (ampElem.isJsonArray()) {
                var arr = ampElem.getAsJsonArray();
                float x = arr.size() > 0 ? arr.get(0).getAsFloat() : 0.1f;
                float y = arr.size() > 1 ? arr.get(1).getAsFloat() : 0.05f;
                float z = arr.size() > 2 ? arr.get(2).getAsFloat() : 0.1f;
                amplitude = new Vector3f(x, y, z);
            } else {
                float val = ampElem.getAsFloat();
                amplitude = new Vector3f(val, val * 0.5f, val);
            }
        }
        
        // Parse speed and randomize
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : 1.0f;
        boolean randomize = !json.has("randomize") || json.get("randomize").getAsBoolean();
        
        // Return NONE if no amplitude or speed
        if (amplitude == null || speed <= 0) return NONE;
        
        return new WobbleConfig(amplitude, speed, randomize);
    }
    
    public static final WobbleConfig NONE = new WobbleConfig(null, 0, false);
    
    /** Default gentle wobble. */
    public static final WobbleConfig DEFAULT = new WobbleConfig(
        new Vector3f(0.1f, 0.05f, 0.1f), 1.0f, true);
    
    /** Strong wobble. */
    public static final WobbleConfig STRONG = new WobbleConfig(
        new Vector3f(0.3f, 0.15f, 0.3f), 1.5f, true);
    
    /**
     * Creates a uniform wobble.
     * @param amplitude Wobble amplitude (same on all axes)
     * @param speed Wobble speed
     */
    public static WobbleConfig uniform(float amplitude, @Range(ValueRange.POSITIVE) float speed) {
        return new WobbleConfig(new Vector3f(amplitude), speed, true);
    }
    
    /** Whether wobble is active. */
    public boolean isActive() {
        return amplitude != null && speed > 0;
    }
    
    /**
     * Serializes this wobble config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .amplitude(amplitude)
            .amplitude(amplitude)
            .speed(speed)
            .randomize(randomize);
    }
    
    public static class Builder {
        private Vector3f amplitude = new Vector3f(0.1f, 0.05f, 0.1f);
        private @Range(ValueRange.POSITIVE) float speed = 1.0f;
        private boolean randomize = true;
        
        public Builder amplitude(Vector3f a) { this.amplitude = a; return this; }
        public Builder amplitude(float x, float y, float z) { this.amplitude = new Vector3f(x, y, z); return this; }
        public Builder speed(float s) { this.speed = s; return this; }
        public Builder randomize(boolean r) { this.randomize = r; return this; }
        
        public WobbleConfig build() {
            return new WobbleConfig(amplitude, speed, randomize);
        }
    }
}
