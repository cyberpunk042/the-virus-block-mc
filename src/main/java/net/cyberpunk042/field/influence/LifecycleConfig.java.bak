package net.cyberpunk042.field.influence;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.instance.LifecycleState;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for field spawn/despawn lifecycle animations.
 * 
 * <h2>Lifecycle States</h2>
 * <ol>
 *   <li><b>SPAWNING</b>: fadeIn/scaleIn ticks</li>
 *   <li><b>ACTIVE</b>: normal operation, decay applies</li>
 *   <li><b>DESPAWNING</b>: fadeOut/scaleOut ticks</li>
 *   <li><b>COMPLETE</b>: ready for removal</li>
 * </ol>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "lifecycle": {
 *   "fadeIn": 20,
 *   "fadeOut": 40,
 *   "scaleIn": 10,
 *   "scaleOut": 20,
 *   "decay": { "rate": 0.95, "min": 0.1 }
 * }
 * </pre>
 * 
 * @see DecayConfig
 * @see net.cyberpunk042.field.instance.LifecycleState
 */
public record LifecycleConfig(
    @Range(ValueRange.STEPS) int fadeIn,
    @Range(ValueRange.STEPS) int fadeOut,
    @Range(ValueRange.STEPS) int scaleIn,
    @Range(ValueRange.STEPS) int scaleOut,
    DecayConfig decay
) {
    /** Instant spawn/despawn (no animation). */
    public static final LifecycleConfig INSTANT = new LifecycleConfig(0, 0, 0, 0, DecayConfig.NONE);
    
    /** Default smooth transitions. */
    public static final LifecycleConfig DEFAULT = new LifecycleConfig(20, 40, 10, 20, DecayConfig.NONE);
    
    /** With decay after spawn. */
    public static final LifecycleConfig WITH_DECAY = new LifecycleConfig(20, 40, 10, 20, DecayConfig.DEFAULT);
    
    /** Whether fade-in animation is used. */
    public boolean hasFadeIn() { return fadeIn > 0; }
    
    /** Whether fade-out animation is used. */
    public boolean hasFadeOut() { return fadeOut > 0; }
    
    /** Whether scale-in animation is used. */
    public boolean hasScaleIn() { return scaleIn > 0; }
    
    /** Whether scale-out animation is used. */
    public boolean hasScaleOut() { return scaleOut > 0; }
    
    /** Whether any spawn animation is used. */
    public boolean hasSpawnAnimation() { return hasFadeIn() || hasScaleIn(); }
    
    /** Whether any despawn animation is used. */
    public boolean hasDespawnAnimation() { return hasFadeOut() || hasScaleOut(); }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a LifecycleConfig from JSON.
     * @param json The JSON object
     * @return Parsed config or null if json is null
     */
    public static LifecycleConfig fromJson(JsonObject json) {
        if (json == null) return null;
        
        Logging.FIELD.topic("parse").trace("Parsing LifecycleConfig...");
        
        int fadeIn = json.has("fadeIn") ? json.get("fadeIn").getAsInt() : 20;
        int fadeOut = json.has("fadeOut") ? json.get("fadeOut").getAsInt() : 40;
        int scaleIn = json.has("scaleIn") ? json.get("scaleIn").getAsInt() : 10;
        int scaleOut = json.has("scaleOut") ? json.get("scaleOut").getAsInt() : 20;
        
        DecayConfig decay = DecayConfig.NONE;
        if (json.has("decay") && json.get("decay").isJsonObject()) {
            decay = DecayConfig.fromJson(json.getAsJsonObject("decay"));
        }
        
        LifecycleConfig result = new LifecycleConfig(fadeIn, fadeOut, scaleIn, scaleOut, decay);
        Logging.FIELD.topic("parse").trace("Parsed LifecycleConfig: fadeIn={}, fadeOut={}, scaleIn={}, scaleOut={}", 
            fadeIn, fadeOut, scaleIn, scaleOut);
        return result;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this lifecycle config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("fadeIn", fadeIn);
        json.addProperty("fadeOut", fadeOut);
        json.addProperty("scaleIn", scaleIn);
        json.addProperty("scaleOut", scaleOut);
        if (decay != null && decay != DecayConfig.NONE) {
            json.add("decay", decay.toJson());
        }
        return json;
    }


    
    public static class Builder {
        private @Range(ValueRange.STEPS) int fadeIn = 20;
        private @Range(ValueRange.STEPS) int fadeOut = 40;
        private @Range(ValueRange.STEPS) int scaleIn = 10;
        private @Range(ValueRange.STEPS) int scaleOut = 20;
        private DecayConfig decay = DecayConfig.NONE;
        
        public Builder fadeIn(int ticks) { this.fadeIn = ticks; return this; }
        public Builder fadeOut(int ticks) { this.fadeOut = ticks; return this; }
        public Builder scaleIn(int ticks) { this.scaleIn = ticks; return this; }
        public Builder scaleOut(int ticks) { this.scaleOut = ticks; return this; }
        public Builder decay(DecayConfig d) { this.decay = d; return this; }
        
        public LifecycleConfig build() {
            return new LifecycleConfig(fadeIn, fadeOut, scaleIn, scaleOut, decay);
        }
    }
}
