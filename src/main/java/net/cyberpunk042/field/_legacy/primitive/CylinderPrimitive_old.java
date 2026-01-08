package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.CylinderShape;

/**
 * A vertical beam primitive.
 */
public class CylinderPrimitive_old extends StructuralPrimitive_old {
    
    public static final String TYPE = "beam";
    
    private final CylinderShape shape;
    
    public CylinderPrimitive_old(CylinderShape shape, Transform transform, Appearance appearance, Animation animation,
                         float wireThickness) {
        super(transform, appearance, animation, wireThickness);
        this.shape = shape != null ? shape : CylinderShape.defaults();
    }
    
    public static CylinderPrimitive_old of(float radius, float height) {
        return new CylinderPrimitive_old(CylinderShape.of(radius, height), Transform.identity(),
                                 Appearance.glowing("@beam", 0.5f), Animation.none(), 1.0f);
    }
    
    public static CylinderPrimitive_old thin(float height) {
        return new CylinderPrimitive_old(CylinderShape.thin(height), Transform.identity(),
                                 Appearance.glowing("@beam", 0.8f), Animation.none(), 1.0f);
    }
    
    @Override
    public Shape shape() {
        return shape;
    }
    
    @Override
    public String type() {
        return TYPE;
    }
    
    @Override
    public Primitive withTransform(Transform newTransform) {
        return new CylinderPrimitive_old(shape, newTransform, appearance, animation, wireThickness);
    }
    
    @Override
    public Primitive withAppearance(Appearance newAppearance) {
        return new CylinderPrimitive_old(shape, transform, newAppearance, animation, wireThickness);
    }
    
    @Override
    public Primitive withAnimation(Animation newAnimation) {
        return new CylinderPrimitive_old(shape, transform, appearance, newAnimation, wireThickness);
    }
    
    public CylinderPrimitive_old withShape(CylinderShape newShape) {
        return new CylinderPrimitive_old(newShape, transform, appearance, animation, wireThickness);
    }
    
    public CylinderShape getBeamShape() {
        return shape;
    }
}
