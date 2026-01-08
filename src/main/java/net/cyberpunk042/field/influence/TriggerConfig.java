package net.cyberpunk042.field.influence;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Configuration for a single event trigger.
 * 
 * <p>Per ARCHITECTURE §12.2:
 * <ul>
 *   <li>event - what event fires this trigger</li>
 *   <li>effect - visual effect to apply</li>
 *   <li>duration - effect duration in ticks</li>
 *   <li>color - color for FLASH/COLOR_SHIFT</li>
 *   <li>scale - scale multiplier for PULSE</li>
 *   <li>amplitude - amplitude for SHAKE</li>
 *   <li>intensity - intensity for GLOW</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "event": "player.damage",
 *   "effect": "flash",
 *   "color": "#FF0000",
 *   "duration": 6
 * }
 * </pre>
 */
public record TriggerConfig(
    FieldEvent event,
    TriggerEffect effect,
    int duration,
    @Nullable @JsonField(skipIfNull = true) String color,
    @JsonField(skipIfDefault = true, defaultValue = "1.0") float scale,
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float amplitude,
    @JsonField(skipIfDefault = true, defaultValue = "1.0") float intensity
){
    /** Default trigger (damage flash). */
    public static final TriggerConfig DEFAULT = new TriggerConfig(
        FieldEvent.PLAYER_DAMAGE, TriggerEffect.FLASH, 6, "#FF0000", 1.0f, 0.1f, 1.0f);
    
    public TriggerConfig {
        if (event == null) event = FieldEvent.PLAYER_DAMAGE;
        if (effect == null) effect = TriggerEffect.FLASH;
        if (duration <= 0) duration = 6;
        if (scale <= 0) scale = 1.0f;
        if (amplitude <= 0) amplitude = 0.1f;
        if (intensity <= 0) intensity = 1.0f;
    }
    
    /**
     * Parses from JSON.
     */
    public static TriggerConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        FieldEvent event = FieldEvent.PLAYER_DAMAGE;
        if (json.has("event")) {
            event = FieldEvent.fromId(json.get("event").getAsString());
            if (event == null) {
                Logging.FIELD.topic("trigger").warn("Unknown event: {}", json.get("event"));
                event = FieldEvent.PLAYER_DAMAGE;
            }
        }
        
        TriggerEffect effect = TriggerEffect.FLASH;
        if (json.has("effect")) {
            effect = TriggerEffect.fromId(json.get("effect").getAsString());
        }
        
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 6;
        String color = json.has("color") ? json.get("color").getAsString() : null;
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.2f;
        float amplitude = json.has("amplitude") ? json.get("amplitude").getAsFloat() : 0.1f;
        float intensity = json.has("intensity") ? json.get("intensity").getAsFloat() : 1.0f;
        
        Logging.FIELD.topic("trigger").trace("Parsed TriggerConfig: event={}, effect={}", event, effect);
        return new TriggerConfig(event, effect, duration, color, scale, amplitude, intensity);
    }
    
    /**
     * Builder pattern.
     */
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .event(event)
            .effect(effect)
            .duration(duration)
            .color(color)
            .scale(scale)
            .amplitude(amplitude)
            .intensity(intensity);
    }
    /**
     * Serializes this trigger config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }


    
    public static class Builder {
        private FieldEvent event = FieldEvent.PLAYER_DAMAGE;
        private TriggerEffect effect = TriggerEffect.FLASH;
        private int duration = 6;
        private String color = null;
        private float scale = 1.2f;
        private float amplitude = 0.1f;
        private float intensity = 1.0f;
        
        public Builder event(FieldEvent e) { this.event = e; return this; }
        public Builder effect(TriggerEffect e) { this.effect = e; return this; }
        public Builder duration(int d) { this.duration = d; return this; }
        public Builder color(String c) { this.color = c; return this; }
        public Builder scale(float s) { this.scale = s; return this; }
        public Builder amplitude(float a) { this.amplitude = a; return this; }
        public Builder intensity(float i) { this.intensity = i; return this; }
        
        public TriggerConfig build() {
            return new TriggerConfig(event, effect, duration, color, scale, amplitude, intensity);
        }
    }
}
