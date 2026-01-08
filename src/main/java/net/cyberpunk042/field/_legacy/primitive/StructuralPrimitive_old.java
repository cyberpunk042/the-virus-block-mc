package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

/**
 * Abstract base for structural primitives (cages, beams).
 * 
 * <p>Structural primitives are typically rendered as wireframe
 * or with specific line thickness.
 * 
 * <p>Subclasses: {@link CagePrimitive_old}, {@link CylinderPrimitive_old}
 */
public abstract class StructuralPrimitive_old implements Primitive {
    
    protected final Transform transform;
    protected final Appearance appearance;
    protected final Animation animation;
    protected final float wireThickness;
    
    protected StructuralPrimitive_old(Transform transform, Appearance appearance, Animation animation,
                                  float wireThickness) {
        this.transform = transform != null ? transform : Transform.identity();
        this.appearance = appearance != null ? appearance : Appearance.wireframe("@primary");
        this.animation = animation != null ? animation : Animation.none();
        this.wireThickness = wireThickness;
    }
    
    @Override
    public Transform transform() {
        return transform;
    }
    
    @Override
    public Appearance appearance() {
        return appearance;
    }
    
    @Override
    public Animation animation() {
        return animation;
    }
    
    public float wireThickness() {
        return wireThickness;
    }
}
