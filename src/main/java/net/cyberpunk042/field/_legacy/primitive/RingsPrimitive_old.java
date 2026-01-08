package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.shape.Shape;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiple concentric rings primitive.
 * 
 * <p>Creates a series of rings at different radii, useful for:
 * <ul>
 *   <li>Target/bullseye patterns</li>
 *   <li>Ripple effects</li>
 *   <li>Layered shields</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // 3 concentric rings
 * RingsPrimitive_old rings = RingsPrimitive_old.concentric(3, 0.5f, 2.0f, 0.1f);
 * 
 * // Ripple effect (expanding rings)
 * RingsPrimitive_old ripple = RingsPrimitive_old.ripple(5, 1.0f, 0.05f);
 * </pre>
 */
public class RingsPrimitive_old extends BandPrimitive_old {
    
    public static final String TYPE = "rings";
    
    private final int ringCount;
    private final float innerRadius;
    private final float outerRadius;
    private final float ringThickness;
    private final float spacing;
    private final List<RingShape> rings;
    
    public RingsPrimitive_old(
            Transform transform,
            Appearance appearance,
            Animation animation,
            int ringCount,
            float innerRadius,
            float outerRadius,
            float ringThickness) {
        super(null, transform, appearance, animation); // No single shape
        
        this.ringCount = Math.max(1, ringCount);
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.ringThickness = ringThickness;
        this.spacing = (outerRadius - innerRadius - ringThickness * ringCount) / Math.max(1, ringCount - 1);
        this.rings = generateRings();
    }
    
    private List<RingShape> generateRings() {
        List<RingShape> result = new ArrayList<>();
        float currentRadius = innerRadius;
        
        for (int i = 0; i < ringCount; i++) {
            result.add(RingShape.of(0, currentRadius, ringThickness, 48));
            currentRadius += ringThickness + spacing;
        }
        
        return result;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────
    
    public int getRingCount() { return ringCount; }
    public float getInnerRadius() { return innerRadius; }
    public float getOuterRadius() { return outerRadius; }
    public float getRingThickness() { return ringThickness; }
    public float getSpacing() { return spacing; }
    public List<RingShape> getRings() { return rings; }
    
    @Override
    public String type() { return TYPE; }
    
    @Override
    public Shape shape() {
        // Return the outermost ring as representative shape
        return rings.isEmpty() ? null : rings.get(rings.size() - 1);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // With methods
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public RingsPrimitive_old withTransform(Transform transform) {
        return new RingsPrimitive_old(transform, appearance, animation, ringCount, innerRadius, outerRadius, ringThickness);
    }
    
    @Override
    public RingsPrimitive_old withAppearance(Appearance appearance) {
        return new RingsPrimitive_old(transform, appearance, animation, ringCount, innerRadius, outerRadius, ringThickness);
    }
    
    @Override
    public RingsPrimitive_old withAnimation(Animation animation) {
        return new RingsPrimitive_old(transform, appearance, animation, ringCount, innerRadius, outerRadius, ringThickness);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates concentric rings filling a radius range.
     * 
     * @param count Number of rings
     * @param innerRadius Inner edge of innermost ring
     * @param outerRadius Outer edge of outermost ring
     * @param thickness Thickness of each ring
     */
    public static RingsPrimitive_old concentric(int count, float innerRadius, float outerRadius, float thickness) {
        return new RingsPrimitive_old(
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            count, innerRadius, outerRadius, thickness
        );
    }
    
    /**
     * Creates evenly spaced rings.
     * 
     * @param count Number of rings
     * @param baseRadius Radius of first ring
     * @param spacing Gap between rings
     * @param thickness Thickness of each ring
     */
    public static RingsPrimitive_old spaced(int count, float baseRadius, float spacing, float thickness) {
        float outerRadius = baseRadius + (count - 1) * (thickness + spacing) + thickness;
        return concentric(count, baseRadius, outerRadius, thickness);
    }
    
    /**
     * Creates a ripple effect (thin rings, even spacing).
     */
    public static RingsPrimitive_old ripple(int waves, float radius, float thickness) {
        return spaced(waves, radius * 0.2f, radius / waves, thickness);
    }
    
    /**
     * Creates a target/bullseye pattern.
     */
    public static RingsPrimitive_old target(int rings, float radius) {
        float thickness = radius / (rings * 2);
        return concentric(rings, thickness, radius, thickness);
    }
}
