package net.cyberpunk042.visual.animation;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.log.Logging;
import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for scale pulsing animation.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "pulse": {
 *   "scale": 0.1,
 *   "speed": 1.0,
 *   "waveform": "SINE",
 *   "min": 0.9,
 *   "max": 1.1
 * }
 * </pre>
 * 
 * <p>The scale oscillates between min and max using the specified waveform.</p>
 * 
 * @see Waveform
 */
public record PulseConfig(
    @Range(ValueRange.POSITIVE) float scale,
    @Range(ValueRange.POSITIVE) float speed,
    Waveform waveform,
    @Range(ValueRange.POSITIVE) float min,
    @Range(ValueRange.POSITIVE) float max,
    PulseMode mode
){
    /** No pulsing (static scale). */
    public static final PulseConfig NONE = new PulseConfig(0, 0, Waveform.SINE, 1, 1, PulseMode.SCALE);
    
    /** Default gentle pulse. */
    public static final PulseConfig DEFAULT = new PulseConfig(0.1f, 1.0f, Waveform.SINE, 0.9f, 1.1f, PulseMode.SCALE);
    
    /**
     * Creates a simple sine pulse.
     * @param amplitude How much to vary (0.1 = ±10%)
     * @param speed Pulse speed
     */
    public static PulseConfig sine(float amplitude, @Range(ValueRange.POSITIVE) float speed) {
        return new PulseConfig(amplitude, speed, Waveform.SINE, 1 - amplitude, 1 + amplitude, PulseMode.SCALE);
    }
    
    /** Whether this pulse is active. */
    public boolean isActive() {
        return scale != 0 && speed != 0;
    }
    
    /**
     * Evaluates the pulse at a given time.
     * @param time Current time (ticks)
     * @return Scale multiplier between min and max
     */
    public float evaluate(float time) {
        if (!isActive()) return 1.0f;
        float t = time * speed;
        float wave = waveform.evaluate(t);
        return min + (max - min) * wave;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .scale(scale)
            .speed(speed)
            .waveform(waveform)
            .min(min)
            .max(max)
            .mode(mode);
    }
    
    public static class Builder {
        private @Range(ValueRange.POSITIVE) float scale = 0.1f;
        private @Range(ValueRange.POSITIVE) float speed = 1.0f;
        private Waveform waveform = Waveform.SINE;
        private @Range(ValueRange.POSITIVE) float min = 0.9f;
        private @Range(ValueRange.POSITIVE) float max = 1.1f;
        private PulseMode mode = PulseMode.SCALE;
        
        public Builder scale(float s) { this.scale = s; return this; }
        public Builder speed(float s) { this.speed = s; return this; }
        public Builder waveform(Waveform w) { this.waveform = w; return this; }
        public Builder min(float m) { this.min = m; return this; }
        public Builder max(float m) { this.max = m; return this; }
        public Builder mode(PulseMode m) { this.mode = m; return this; }
        
        public PulseConfig build() {
            return new PulseConfig(scale, speed, waveform, min, max, mode);
        }
    }

    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a PulseConfig from JSON.
     * @param json The JSON object
     * @return Parsed config
     */
    public static PulseConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        Logging.FIELD.topic("parse").trace("Parsing PulseConfig...");
        
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : 0.1f;
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : 1.0f;
        
        Waveform waveform = Waveform.SINE;
        if (json.has("waveform")) {
            waveform = Waveform.fromId(json.get("waveform").getAsString());
        }
        
        float min = json.has("min") ? json.get("min").getAsFloat() : 0.8f;
        float max = json.has("max") ? json.get("max").getAsFloat() : 1.2f;
        
        PulseMode mode = PulseMode.SCALE;
        if (json.has("mode")) {
            mode = PulseMode.fromString(json.get("mode").getAsString());
        }
        
        PulseConfig result = new PulseConfig(scale, speed, waveform, min, max, mode);
        Logging.FIELD.topic("parse").trace("Parsed PulseConfig: scale={}, speed={}, mode={}", scale, speed, mode);
        return result;
    }
    
    /**
     * Serializes this pulse config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

}
