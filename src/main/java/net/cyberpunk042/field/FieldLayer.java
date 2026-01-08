package net.cyberpunk042.field;

import com.google.gson.JsonObject;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.layer.BlendMode;
import net.cyberpunk042.visual.transform.Transform;

import java.util.List;

/**
 * A layer within a field definition.
 * 
 * <p>Layers group primitives and apply shared configuration:
 * <ul>
 *   <li><b>transform</b> - Applied to all primitives in layer</li>
 *   <li><b>animation</b> - Shared animation (spin, pulse)</li>
 *   <li><b>alpha</b> - Layer-wide alpha multiplier</li>
 *   <li><b>visible</b> - Toggle layer visibility</li>
 *   <li><b>blendMode</b> - How layer blends with others</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "id": "main_layer",
 *   "primitives": [...],
 *   "transform": { "offset": [0, 1, 0] },
 *   "animation": { "spin": { "speed": 0.02 } },
 *   "alpha": 1.0,
 *   "visible": true,
 *   "blendMode": "NORMAL"
 * }
 * </pre>
 * 
 * @see FieldDefinition
 * @see Primitive
 */
public record FieldLayer(
    String id,
    List<Primitive> primitives,
    @JsonField(skipIfEqualsConstant = "IDENTITY") Transform transform,
    @JsonField(skipIfEqualsConstant = "NONE") Animation animation,
    float alpha,
    boolean visible,
    BlendMode blendMode
) {
    
    /**
     * Creates an empty layer.
     */
    public static FieldLayer empty(String id) {
        return new FieldLayer(id, List.of(), Transform.IDENTITY, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Creates a simple layer with primitives.
     */
    public static FieldLayer of(String id, List<Primitive> primitives) {
        return new FieldLayer(id, primitives, Transform.IDENTITY, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Creates a layer with transform.
     */
    public static FieldLayer of(String id, List<Primitive> primitives, Transform transform) {
        return new FieldLayer(id, primitives, transform, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Builder for complex layers.
     */
    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses FieldLayer from JSON.
     * Delegates to FieldLoader for full parsing with $ref resolution.
     */
    public static FieldLayer fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "layer";
        boolean visible = !json.has("visible") || json.get("visible").getAsBoolean();
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 1.0f;
        
        BlendMode blendMode = BlendMode.NORMAL;
        if (json.has("blendMode")) {
            try {
                blendMode = BlendMode.valueOf(json.get("blendMode").getAsString().toUpperCase());
            } catch (Exception ignored) {}
        }
        
        Transform transform = json.has("transform") 
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : Transform.IDENTITY;
        
        Animation animation = json.has("animation")
            ? Animation.fromJson(json.getAsJsonObject("animation"))
            : Animation.NONE;
        
        // Note: primitives require FieldLoader for full parsing with $ref
        // For direct fromJson, we create empty list - use FieldLoader.parseDefinition() for full parsing
        java.util.List<Primitive> primitives = java.util.Collections.emptyList();
        
        return new FieldLayer(id, primitives, transform, animation, alpha, visible, blendMode);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }
    /**
     * Serializes this layer to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }


    
    public static class Builder {
        private final String id;
        private List<Primitive> primitives = List.of();
        private Transform transform = Transform.IDENTITY;
        private Animation animation = Animation.NONE;
        private float alpha = 1.0f;
        private boolean visible = true;
        private BlendMode blendMode = BlendMode.NORMAL;
        
        public Builder(String id) { this.id = id; }
        
        public Builder primitives(List<Primitive> p) { this.primitives = p; return this; }
        public Builder primitives(Primitive... p) { this.primitives = List.of(p); return this; }
        public Builder transform(Transform t) { this.transform = t; return this; }
        public Builder animation(Animation a) { this.animation = a; return this; }
        public Builder alpha(float a) { this.alpha = a; return this; }
        public Builder visible(boolean v) { this.visible = v; return this; }
        public Builder blendMode(BlendMode b) { this.blendMode = b; return this; }
        
        public FieldLayer build() {
            return new FieldLayer(id, primitives, transform, animation, alpha, visible, blendMode);
        }
    }
}
