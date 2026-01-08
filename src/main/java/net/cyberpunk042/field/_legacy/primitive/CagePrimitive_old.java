package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;

/**
 * A wireframe cage primitive (wireframe sphere).
 */
public class CagePrimitive_old extends StructuralPrimitive_old {
    
    public static final String TYPE = "cage";
    
    private final SphereShape shape;
    private final int latLines;
    private final int lonLines;
    
    public CagePrimitive_old(SphereShape shape, Transform transform, Appearance appearance, Animation animation,
                         float wireThickness, int latLines, int lonLines) {
        super(transform, appearance, animation, wireThickness);
        this.shape = shape != null ? shape : SphereShape.defaults();
        this.latLines = latLines;
        this.lonLines = lonLines;
    }
    
    public static CagePrimitive_old of(float radius, int lines) {
        return new CagePrimitive_old(SphereShape.of(radius), Transform.identity(),
                                 Appearance.wireframe("@wire"), Animation.none(), 1.0f, lines, lines * 2);
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
        return new CagePrimitive_old(shape, newTransform, appearance, animation, wireThickness, latLines, lonLines);
    }
    
    @Override
    public Primitive withAppearance(Appearance newAppearance) {
        return new CagePrimitive_old(shape, transform, newAppearance, animation, wireThickness, latLines, lonLines);
    }
    
    @Override
    public Primitive withAnimation(Animation newAnimation) {
        return new CagePrimitive_old(shape, transform, appearance, newAnimation, wireThickness, latLines, lonLines);
    }
    
    public int getLatLines() {
        return latLines;
    }
    
    public int getLonLines() {
        return lonLines;
    }
}
