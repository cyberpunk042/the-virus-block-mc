package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for color cycling animation (FUTURE).
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "colorCycle": {
 *   "colors": ["#FF0000", "#00FF00", "#0000FF"],
 *   "speed": 1.0,
 *   "blend": true
 * }
 * </pre>
 * 
 * @see Animation
 */
public record ColorCycleConfig(
    @JsonField(skipIfEmpty = true) @Nullable List<String> colors,
    @Range(ValueRange.POSITIVE) float speed,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean blend
) {
    /**
     * Parses ColorCycleConfig from JSON.
     * 
     * @param json JSON object with colors, speed, blend fields
     * @return parsed config or NONE if invalid
     */
    public static ColorCycleConfig fromJson(com.google.gson.JsonObject json) {
        if (json == null) return NONE;
        
        // Parse colors array
        List<String> colors = null;
        if (json.has("colors") && json.get("colors").isJsonArray()) {
            var arr = json.getAsJsonArray("colors");
            colors = new java.util.ArrayList<>();
            for (var elem : arr) {
                colors.add(elem.getAsString());
            }
        }
        
        // Parse speed and blend
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : 1.0f;
        boolean blend = !json.has("blend") || json.get("blend").getAsBoolean();
        
        // Return NONE if no colors defined
        if (colors == null || colors.isEmpty()) return NONE;
        
        return new ColorCycleConfig(colors, speed, blend);
    }
    
    public static final ColorCycleConfig NONE = new ColorCycleConfig(null, 0, false);
    
    /** Default RGB cycle. */
    public static final ColorCycleConfig RGB = new ColorCycleConfig(
        List.of("#FF0000", "#00FF00", "#0000FF"), 1.0f, true);
    
    /** Rainbow cycle. */
    public static final ColorCycleConfig RAINBOW = new ColorCycleConfig(
        List.of("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#9400D3"),
        0.5f, true);
    
    /** Fire colors. */
    public static final ColorCycleConfig FIRE = new ColorCycleConfig(
        List.of("#FF0000", "#FF4500", "#FFA500", "#FFD700"), 2.0f, true);
    
    /**
     * Creates a color cycle between two colors.
     * @param color1 First color
     * @param color2 Second color
     * @param speed Cycle speed
     */
    public static ColorCycleConfig between(String color1, String color2, @Range(ValueRange.POSITIVE) float speed) {
        return new ColorCycleConfig(List.of(color1, color2), speed, true);
    }
    
    /** Whether color cycling is active. */
    public boolean isActive() {
        return colors != null && !colors.isEmpty() && speed > 0;
    }
    
    /**
     * Serializes this color cycle config to JSON.
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
            .colors(colors)
            .colors(colors)
            .speed(speed)
            .blend(blend);
    }
    
    public static class Builder {
        private List<String> colors = null;
        private @Range(ValueRange.POSITIVE) float speed = 1.0f;
        private boolean blend = true;
        
        public Builder colors(List<String> c) { this.colors = c; return this; }
        public Builder colors(String... c) { this.colors = List.of(c); return this; }
        public Builder speed(float s) { this.speed = s; return this; }
        public Builder blend(boolean b) { this.blend = b; return this; }
        
        public ColorCycleConfig build() {
            return new ColorCycleConfig(colors, speed, blend);
        }
    }
}
