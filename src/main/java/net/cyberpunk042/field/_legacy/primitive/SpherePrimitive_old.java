package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;

/**
 * A solid sphere primitive.
 */
public class SpherePrimitive_old extends SolidPrimitive_old {
    
    private final float radius;
    private final int detail;
    
    public SpherePrimitive_old(SphereShape shape, Transform transform, Appearance appearance, Animation animation) {
        super(shape, transform, appearance, animation);
        this.radius = shape.radius();
        this.detail = shape.latSteps();
    }
    
    public float getRadius() { return radius; }
    public int getDetail() { return detail; }
    
    public static final String TYPE = "sphere";
    
    @Override
    public String type() { return TYPE; }
    
    /**
     * Gets the sphere shape with proper typing.
     * @return the SphereShape for this primitive
     */
    public SphereShape getSphereShape() {
        return (SphereShape) shape;
    }
    
    @Override
    public SpherePrimitive_old withTransform(Transform transform) {
        return new SpherePrimitive_old((SphereShape) shape, transform, appearance, animation);
    }
    
    @Override
    public SpherePrimitive_old withAppearance(Appearance appearance) {
        return new SpherePrimitive_old((SphereShape) shape, transform, appearance, animation);
    }
    
    @Override
    public SpherePrimitive_old withAnimation(Animation animation) {
        return new SpherePrimitive_old((SphereShape) shape, transform, appearance, animation);
    }
    
    public static SpherePrimitive_old create(SphereShape shape, Transform transform, Appearance appearance, Animation animation) {
        return new SpherePrimitive_old(shape, transform, appearance, animation);
    }
    
    public static SpherePrimitive_old create(float radius, int detail) {
        return new SpherePrimitive_old(
            SphereShape.of(radius, detail),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
}
