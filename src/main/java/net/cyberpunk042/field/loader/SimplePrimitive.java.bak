package net.cyberpunk042.field.loader;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Simple implementation of {@link Primitive} for JSON loading.
 * 
 * <p>This is a straightforward record-based implementation used by
 * {@link FieldLoader} when parsing JSON. For runtime modifications,
 * use the builder pattern or create a new instance.</p>
 * 
 * @see FieldLoader
 * @see Primitive
 */
public record SimplePrimitive(
    String id,
    String type,
    Shape shape,
    Transform transform,
    FillConfig fill,
    VisibilityMask visibility,
    ArrangementConfig arrangement,
    Appearance appearance,
    Animation animation,
    PrimitiveLink link
) implements Primitive {
    
    /**
     * Creates a SimplePrimitive with default values for optional fields.
     */
    public static SimplePrimitive of(String id, String type, Shape shape) {
        return new SimplePrimitive(
            id, type, shape,
            Transform.IDENTITY,
            FillConfig.SOLID,
            VisibilityMask.FULL,
            ArrangementConfig.DEFAULT,
            Appearance.DEFAULT,
            Animation.NONE,
            PrimitiveLink.NONE
        );
    }
    
    /**
     * Returns a copy with a different shape.
     */
    public SimplePrimitive withShape(Shape newShape) {
        return new SimplePrimitive(id, type, newShape, transform, fill, visibility,
            arrangement, appearance, animation, link);
    }
    
    /**
     * Returns a copy with a different transform.
     */
    public SimplePrimitive withTransform(Transform newTransform) {
        return new SimplePrimitive(id, type, shape, newTransform, fill, visibility,
            arrangement, appearance, animation, link);
    }
    
    /**
     * Returns a copy with a different appearance.
     */
    public SimplePrimitive withAppearance(Appearance newAppearance) {
        return new SimplePrimitive(id, type, shape, transform, fill, visibility,
            arrangement, newAppearance, animation, link);
    }
    
    /**
     * Returns a copy with a different animation.
     */
    public SimplePrimitive withAnimation(Animation newAnimation) {
        return new SimplePrimitive(id, type, shape, transform, fill, visibility,
            arrangement, appearance, newAnimation, link);
    }
    
    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses SimplePrimitive from JSON.
     * Note: For full parsing with $ref resolution, use FieldLoader.
     */
    public static SimplePrimitive fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "primitive";
        String type = json.has("type") ? json.get("type").getAsString() : "sphere";
        
        // Shape - needs type-specific parsing
        Shape shape = null;
        if (json.has("shape") && json.get("shape").isJsonObject()) {
            JsonObject shapeJson = json.getAsJsonObject("shape");
            shape = parseShape(type, shapeJson);
        }
        if (shape == null) {
            shape = net.cyberpunk042.visual.shape.SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        }
        
        Transform transform = json.has("transform")
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : Transform.IDENTITY;
        
        FillConfig fill = json.has("fill")
            ? FillConfig.fromJson(json.get("fill"))
            : FillConfig.SOLID;
        
        VisibilityMask visibility = json.has("visibility")
            ? VisibilityMask.fromJson(json.get("visibility"))
            : VisibilityMask.FULL;
        
        ArrangementConfig arrangement = json.has("arrangement")
            ? ArrangementConfig.fromJson(json.get("arrangement"))
            : ArrangementConfig.DEFAULT;
        
        Appearance appearance = json.has("appearance")
            ? Appearance.fromJson(json.getAsJsonObject("appearance"))
            : Appearance.DEFAULT;
        
        Animation animation = json.has("animation")
            ? Animation.fromJson(json.getAsJsonObject("animation"))
            : Animation.NONE;
        
        PrimitiveLink link = json.has("link")
            ? PrimitiveLink.fromJson(json.getAsJsonObject("link"))
            : PrimitiveLink.NONE;
        
        return new SimplePrimitive(id, type, shape, transform, fill, visibility, arrangement, appearance, animation, link);
    }
    
    private static Shape parseShape(String type, JsonObject json) {
        return switch (type.toLowerCase()) {
            case "sphere" -> net.cyberpunk042.visual.shape.SphereShape.fromJson(json);
            case "ring" -> net.cyberpunk042.visual.shape.RingShape.builder()
                .innerRadius(json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0.8f)
                .outerRadius(json.has("outerRadius") ? json.get("outerRadius").getAsFloat() : 1.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "disc" -> net.cyberpunk042.visual.shape.DiscShape.builder()
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "prism" -> net.cyberpunk042.visual.shape.PrismShape.builder()
                .sides(json.has("sides") ? json.get("sides").getAsInt() : 6)
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
                .build();
            case "cylinder" -> net.cyberpunk042.visual.shape.CylinderShape.builder()
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "polyhedron" -> net.cyberpunk042.visual.shape.PolyhedronShape.builder()
                .polyType(json.has("polyType") 
                    ? net.cyberpunk042.visual.shape.PolyType.valueOf(json.get("polyType").getAsString().toUpperCase())
                    : net.cyberpunk042.visual.shape.PolyType.ICOSAHEDRON)
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .build();
            default -> net.cyberpunk042.visual.shape.SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        };
    }

    /**
     * Serializes this primitive to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type);
        
        // Serialize shape
        if (shape != null) {
            json.add("shape", shape.toJson());
        }
        
        // Serialize transform
        if (transform != null && transform != Transform.IDENTITY) {
            json.add("transform", transform.toJson());
        }
        
        // Serialize fill
        if (fill != null && fill != FillConfig.SOLID) {
            json.add("fill", fill.toJson());
        }
        
        // Serialize visibility
        if (visibility != null && visibility != VisibilityMask.FULL) {
            json.add("visibility", visibility.toJson());
        }
        
        // Serialize arrangement
        if (arrangement != null && arrangement != ArrangementConfig.DEFAULT) {
            json.add("arrangement", arrangement.toJson());
        }
        
        // Serialize appearance
        if (appearance != null && appearance != Appearance.DEFAULT) {
            json.add("appearance", appearance.toJson());
        }
        
        // Serialize animation
        if (animation != null && animation != Animation.NONE) {
            json.add("animation", animation.toJson());
        }
        
        // Serialize link
        if (link != null && link != PrimitiveLink.NONE) {
            json.add("link", link.toJson());
        }
        
        return json;
    }
}
