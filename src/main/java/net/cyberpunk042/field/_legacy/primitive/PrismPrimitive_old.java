package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.shape.PrismShape;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.appearance.Appearance;
import com.google.gson.JsonObject;

/**
 * A prism primitive for regular polygon-based fields.
 * 
 * <p>Prisms are useful for hexagonal shields, crystalline structures,
 * and other angular field effects.
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>sides</b>: Number of sides (3=triangle, 6=hexagon, etc.)</li>
 *   <li><b>radius</b>: Distance from center to vertices</li>
 *   <li><b>height</b>: Vertical extent of the prism</li>
 *   <li><b>capped</b>: Whether to render top/bottom caps</li>
 * </ul>
 */
public class PrismPrimitive_old extends SolidPrimitive_old {
    
    public static final String TYPE = "prism";
    
    private final boolean capped;
    
    public PrismPrimitive_old(PrismShape shape, Transform transform, Appearance appearance, 
                          Animation animation, boolean capped) {
        super(shape, transform, appearance, animation);
        this.capped = capped;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates a hexagonal prism (common for force fields).
     */
    public static PrismPrimitive_old hexagonal(float radius, float height) {
        return new PrismPrimitive_old(
            PrismShape.hexagon(radius, height),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            true
        );
    }
    
    /**
     * Creates a triangular prism.
     */
    public static PrismPrimitive_old triangular(float radius, float height) {
        return new PrismPrimitive_old(
            PrismShape.triangle(radius, height),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            true
        );
    }
    
    /**
     * Creates a square prism (box shape).
     */
    public static PrismPrimitive_old square(float radius, float height) {
        return new PrismPrimitive_old(
            new PrismShape(4, radius, height),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            true
        );
    }
    
    /**
     * Creates an octagonal prism.
     */
    public static PrismPrimitive_old octagonal(float radius, float height) {
        return new PrismPrimitive_old(
            new PrismShape(8, height, radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            true
        );
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public String type() {
        return TYPE;
    }
    
    public PrismShape getPrismShape() {
        return (PrismShape) shape;
    }
    
    public int getSides() {
        return getPrismShape().sides();
    }
    
    public float getRadius() {
        return getPrismShape().radius();
    }
    
    public float getHeight() {
        return getPrismShape().height();
    }
    
    public boolean isCapped() {
        return capped;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Builders
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public PrismPrimitive_old withTransform(Transform transform) {
        return new PrismPrimitive_old(getPrismShape(), transform, appearance, animation, capped);
    }
    
    @Override
    public PrismPrimitive_old withAppearance(Appearance appearance) {
        return new PrismPrimitive_old(getPrismShape(), transform, appearance, animation, capped);
    }
    
    @Override
    public PrismPrimitive_old withAnimation(Animation animation) {
        return new PrismPrimitive_old(getPrismShape(), transform, appearance, animation, capped);
    }
    
    public PrismPrimitive_old withCapped(boolean capped) {
        return new PrismPrimitive_old(getPrismShape(), transform, appearance, animation, capped);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Serialization
    // ─────────────────────────────────────────────────────────────────────────
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", TYPE);
        json.addProperty("sides", getSides());
        json.addProperty("radius", getRadius());
        json.addProperty("height", getHeight());
        json.addProperty("capped", capped);
        // Transform and appearance serialization inherited
        return json;
    }
    
    public static PrismPrimitive_old fromJson(JsonObject json) {
        int sides = json.has("sides") ? json.get("sides").getAsInt() : 6;
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 1.0f;
        float height = json.has("height") ? json.get("height").getAsFloat() : 1.0f;
        boolean capped = json.has("capped") ? json.get("capped").getAsBoolean() : true;
        
        return new PrismPrimitive_old(
            new PrismShape(sides, radius, height),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            capped
        );
    }
}
