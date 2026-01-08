package net.cyberpunk042.visual.transform;

import net.cyberpunk042.visual.animation.Axis;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.log.Logging;
import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for orbital/circular motion around an anchor point.
 * 
 * <p>When enabled, the primitive orbits around its anchor at the specified
 * radius and speed.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "orbit": {
 *   "enabled": true,
 *   "radius": 2.0,
 *   "speed": 0.5,
 *   "axis": "Y",
 *   "phase": 0.0
 * }
 * </pre>
 * 
 * @see Transform
 */
public record OrbitConfig(
    boolean enabled,
    @Range(ValueRange.RADIUS) float radius,
    @Range(ValueRange.UNBOUNDED) float speed,
    net.cyberpunk042.visual.animation.Axis axis,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float phase
){
    /** Disabled orbit (static position). */
    public static final OrbitConfig NONE = new OrbitConfig(false, 0, 0, 
        net.cyberpunk042.visual.animation.Axis.Y, 0);
    
    /** Default orbit (Y-axis, 1.0 radius, slow spin). */
    public static final OrbitConfig DEFAULT = new OrbitConfig(true, 1.0f, 0.02f,
        net.cyberpunk042.visual.animation.Axis.Y, 0);
    
    /**
     * Creates a simple Y-axis orbit.
     * @param radius Distance from center
     * @param speed Rotation speed (radians per tick)
     */
    public static OrbitConfig yAxis(@Range(ValueRange.RADIUS) float radius, @Range(ValueRange.UNBOUNDED) float speed) {
        return new OrbitConfig(true, radius, speed, 
            net.cyberpunk042.visual.animation.Axis.Y, 0);
    }
    
    /** Whether this orbit is active. */
    public boolean isActive() {
        return enabled && speed != 0;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .enabled(enabled)
            .radius(radius)
            .speed(speed)
            .axis(axis)
            .phase(phase);
    }
    
    public static class Builder {
        private boolean enabled = true;
        private @Range(ValueRange.RADIUS) float radius = 1.0f;
        private @Range(ValueRange.UNBOUNDED) float speed = 0.02f;
        private net.cyberpunk042.visual.animation.Axis axis = 
            net.cyberpunk042.visual.animation.Axis.Y;
        private @Range(ValueRange.NORMALIZED) float phase = 0;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder radius(float r) { this.radius = r; return this; }
        public Builder speed(float s) { this.speed = s; return this; }
        public Builder axis(net.cyberpunk042.visual.animation.Axis a) { this.axis = a; return this; }
        public Builder phase(float p) { this.phase = p; return this; }
        
        public OrbitConfig build() {
            return new OrbitConfig(enabled, radius, speed, axis, phase);
        }
    }

    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses an OrbitConfig from JSON.
     * @param json The JSON object
     * @return Parsed config
     */
    public static OrbitConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        Logging.FIELD.topic("parse").trace("Parsing OrbitConfig...");
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 1.0f;
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : 0.02f;
        
        Axis axis = Axis.Y;
        if (json.has("axis")) {
            axis = Axis.fromId(json.get("axis").getAsString());
        }
        
        float phase = json.has("phase") ? json.get("phase").getAsFloat() : 0.0f;
        
        OrbitConfig result = new OrbitConfig(enabled, radius, speed, axis, phase);
        Logging.FIELD.topic("parse").trace("Parsed OrbitConfig: enabled={}, radius={}, speed={}", enabled, radius, speed);
        return result;
    }
    
    /**
     * Serializes this orbit config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

}
