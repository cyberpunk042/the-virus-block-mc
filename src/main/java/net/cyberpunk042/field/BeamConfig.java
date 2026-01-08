package net.cyberpunk042.field;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for central beam effect on fields.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "beam": {
 *   "enabled": true,
 *   "innerRadius": 0.05,
 *   "outerRadius": 0.1,
 *   "color": "@beam",
 *   "height": 10.0,
 *   "glow": 0.5,
 *   "pulse": { "scale": 0.1, "speed": 1.0 }
 * }
 * </pre>
 */
public record BeamConfig(
    boolean enabled,
    @Range(ValueRange.RADIUS) float innerRadius,
    @Range(ValueRange.RADIUS) float outerRadius,
    String color,
    @Range(ValueRange.POSITIVE) float height,
    @Range(ValueRange.ALPHA) float glow,
    @Nullable PulseConfig pulse
){

    // === Static Constants ===
    public static final BeamConfig DISABLED = new BeamConfig(false, 0f, 0f, "@beam", 0f, 0f, null);
    public static final BeamConfig DEFAULT_BEAM = new BeamConfig(true, 0.05f, 0.1f, "@beam", 10f, 0.5f, null);
    
    public static BeamConfig custom(float innerRadius, float outerRadius, int color) {
        return new BeamConfig(true, innerRadius, outerRadius, "@beam", 10f, 0.5f, null);
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

    /** No beam. */
    public static final BeamConfig NONE = new BeamConfig(false, 0.05f, 0.1f, "@beam", 10, 0, null);
    
    /** Default beam. */
    
    /** Pulsing beam. */
    public static final BeamConfig PULSING = new BeamConfig(true, 0.05f, 0.1f, "@beam", 10, 0.5f, 
        PulseConfig.DEFAULT);
    
    /** Whether beam is active. */
    public boolean isActive() { return enabled; }
    
    /** Whether beam has pulsing. */
    public boolean hasPulse() { return pulse != null && pulse.isActive(); }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a BeamConfig from JSON.
     * @param json The JSON object
     * @return Parsed config
     */
    public static BeamConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        Logging.FIELD.topic("parse").trace("Parsing BeamConfig...");
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        float innerRadius = json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0.05f;
        float outerRadius = json.has("outerRadius") ? json.get("outerRadius").getAsFloat() : 0.1f;
        String color = json.has("color") ? json.get("color").getAsString() : "@beam";
        float height = json.has("height") ? json.get("height").getAsFloat() : 10.0f;
        float glow = json.has("glow") ? json.get("glow").getAsFloat() : 0.5f;
        
        PulseConfig pulse = null;
        if (json.has("pulse")) {
            pulse = PulseConfig.fromJson(json.getAsJsonObject("pulse"));
        }
        
        BeamConfig result = new BeamConfig(enabled, innerRadius, outerRadius, color, height, glow, pulse);
        Logging.FIELD.topic("parse").trace("Parsed BeamConfig: enabled={}, innerRadius={}, height={}", 
            enabled, innerRadius, height);
        return result;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .enabled(enabled)
            .innerRadius(innerRadius)
            .outerRadius(outerRadius)
            .color(color)
            .height(height)
            .glow(glow)
            .pulse(pulse);
    }
    
    public static class Builder {
        private boolean enabled = true;
        private @Range(ValueRange.RADIUS) float innerRadius = 0.05f;
        private @Range(ValueRange.RADIUS) float outerRadius = 0.1f;
        private String color = "@beam";
        private @Range(ValueRange.POSITIVE) float height = 10;
        private @Range(ValueRange.ALPHA) float glow = 0.5f;
        private @Nullable PulseConfig pulse = null;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder innerRadius(float r) { this.innerRadius = r; return this; }
        public Builder outerRadius(float r) { this.outerRadius = r; return this; }
        public Builder color(String c) { this.color = c; return this; }
        public Builder height(float h) { this.height = h; return this; }
        public Builder glow(float g) { this.glow = g; return this; }
        public Builder pulse(PulseConfig p) { this.pulse = p; return this; }
        
        public BeamConfig build() {
            return new BeamConfig(enabled, innerRadius, outerRadius, color, height, glow, pulse);
        }
    }
}
