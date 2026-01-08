package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.shape.Shape;

/**
 * Abstract base for band/ring-like primitives.
 * 
 * <p>Subclasses: {@link RingPrimitive_old}, {@link RingsPrimitive_old}
 */
public abstract class BandPrimitive_old implements Primitive {
    
    protected final Shape shape;
    protected final Transform transform;
    protected final Appearance appearance;
    protected final Animation animation;
    
    protected BandPrimitive_old(Shape shape, Transform transform, Appearance appearance, Animation animation) {
        this.shape = shape;
        this.transform = transform;
        this.appearance = appearance;
        this.animation = animation;
    }
    
    @Override public Shape shape() { return shape; }
    @Override public Transform transform() { return transform; }
    @Override public Appearance appearance() { return appearance; }
    @Override public Animation animation() { return animation; }
}
