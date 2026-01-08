package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.RingShape;
import net.minecraft.util.math.Vec3d;

/**
 * A ring (torus) primitive.
 */
public class RingPrimitive_old extends BandPrimitive_old {
    
    private final float innerRadius;
    private final float outerRadius;
    private final int segments;
    
    public RingPrimitive_old(RingShape shape, Transform transform, Appearance appearance, Animation animation) {
        super(shape, transform, appearance, animation);
        this.innerRadius = shape.radius() - shape.thickness() / 2;
        this.outerRadius = shape.radius() + shape.thickness() / 2;
        this.segments = shape.segments();
    }
    
    public float getInnerRadius() { return innerRadius; }
    public float getOuterRadius() { return outerRadius; }
    public int getSegments() { return segments; }
    
    public static final String TYPE = "ring";
    
    @Override
    public String type() { return TYPE; }
    
    /**
     * Gets the ring shape with proper typing.
     * @return the RingShape for this primitive
     */
    public RingShape getRingShape() {
        return (RingShape) shape;
    }
    
    @Override
    public RingPrimitive_old withTransform(Transform transform) {
        return new RingPrimitive_old((RingShape) shape, transform, appearance, animation);
    }
    
    @Override
    public RingPrimitive_old withAppearance(Appearance appearance) {
        return new RingPrimitive_old((RingShape) shape, transform, appearance, animation);
    }
    
    @Override
    public RingPrimitive_old withAnimation(Animation animation) {
        return new RingPrimitive_old((RingShape) shape, transform, appearance, animation);
    }
    
    public static RingPrimitive_old create(RingShape shape, Transform transform, Appearance appearance, Animation animation) {
        return new RingPrimitive_old(shape, transform, appearance, animation);
    }
    
    public static RingPrimitive_old create(float innerRadius, float outerRadius, int segments, float y) {
        float radius = (innerRadius + outerRadius) / 2;
        float thickness = outerRadius - innerRadius;
        return new RingPrimitive_old(
            RingShape.of(y, radius, thickness, segments),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
}
