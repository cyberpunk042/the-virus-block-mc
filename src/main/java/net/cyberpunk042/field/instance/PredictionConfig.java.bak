package net.cyberpunk042.field.instance;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for movement prediction on personal fields.
 * 
 * <p>Prediction anticipates player movement to reduce visual lag
 * when following the player.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "prediction": {
 *   "enabled": true,
 *   "leadTicks": 2,
 *   "maxDistance": 8.0,
 *   "lookAhead": 0.5,
 *   "verticalBoost": 0.0
 * }
 * </pre>
 * 
 * @see PersonalFieldInstance
 */
public record PredictionConfig(
    boolean enabled,
    @Range(ValueRange.STEPS) int leadTicks,
    @Range(ValueRange.POSITIVE) float maxDistance,
    @Range(ValueRange.POSITIVE) float lookAhead,
    @Range(ValueRange.POSITIVE) float verticalBoost
) {
    /** No prediction (raw position). */
    public static final PredictionConfig NONE = new PredictionConfig(false, 0, 0, 0, 0);
    
    /** Default prediction settings. */
    public static PredictionConfig defaults() { return DEFAULT; }
    
    public static final PredictionConfig DEFAULT = new PredictionConfig(true, 2, 8.0f, 0.5f, 0);
    
    /** Aggressive prediction for fast movement. */
    public static final PredictionConfig AGGRESSIVE = new PredictionConfig(true, 4, 12.0f, 0.8f, 0.2f);
    
    /** Whether prediction is active. */
    public boolean isActive() { return enabled && leadTicks > 0; }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a PredictionConfig from JSON.
     * @param json The JSON object
     * @return Parsed config
     */
    public static PredictionConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        Logging.FIELD.topic("parse").trace("Parsing PredictionConfig...");
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        int leadTicks = json.has("leadTicks") ? json.get("leadTicks").getAsInt() : 2;
        float maxDistance = json.has("maxDistance") ? json.get("maxDistance").getAsFloat() : 8.0f;
        float lookAhead = json.has("lookAhead") ? json.get("lookAhead").getAsFloat() : 0.5f;
        float verticalBoost = json.has("verticalBoost") ? json.get("verticalBoost").getAsFloat() : 0;
        
        PredictionConfig result = new PredictionConfig(enabled, leadTicks, maxDistance, lookAhead, verticalBoost);
        Logging.FIELD.topic("parse").trace("Parsed PredictionConfig: enabled={}, leadTicks={}, maxDistance={}", 
            enabled, leadTicks, maxDistance);
        return result;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this prediction config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("leadTicks", leadTicks);
        json.addProperty("maxDistance", maxDistance);
        json.addProperty("lookAhead", lookAhead);
        json.addProperty("verticalBoost", verticalBoost);
        return json;
    }


    
    public static class Builder {
        private boolean enabled = true;
        private @Range(ValueRange.STEPS) int leadTicks = 2;
        private @Range(ValueRange.POSITIVE) float maxDistance = 8.0f;
        private @Range(ValueRange.POSITIVE) float lookAhead = 0.5f;
        private @Range(ValueRange.POSITIVE) float verticalBoost = 0;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder leadTicks(int t) { this.leadTicks = t; return this; }
        public Builder maxDistance(float d) { this.maxDistance = d; return this; }
        public Builder lookAhead(float l) { this.lookAhead = l; return this; }
        public Builder verticalBoost(float v) { this.verticalBoost = v; return this; }
        
        public PredictionConfig build() {
            return new PredictionConfig(enabled, leadTicks, maxDistance, lookAhead, verticalBoost);
        }
    }
}
