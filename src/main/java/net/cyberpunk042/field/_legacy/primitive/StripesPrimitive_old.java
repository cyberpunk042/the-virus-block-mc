package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;

import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;

/**
 * Latitude stripe bands on a sphere.
 * 
 * <p>Creates horizontal bands/stripes around a sphere, useful for:
 * <ul>
 *   <li>Planet-like bands</li>
 *   <li>Scanning effects</li>
 *   <li>Layered shields</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // 5 horizontal stripes
 * StripesPrimitive_old stripes = StripesPrimitive_old.horizontal(5, 1.0f);
 * 
 * // Alternating visible/invisible bands
 * StripesPrimitive_old zebra = StripesPrimitive_old.zebra(8, 1.0f);
 * </pre>
 */
public class StripesPrimitive_old extends SolidPrimitive_old {
    
    public static final String TYPE = "stripes";
    
    private final SphereShape sphereShape;
    private final int stripeCount;
    private final float stripeRatio;  // 0.5 = equal bands, <0.5 = thin stripes
    private final boolean alternate;   // true = every other stripe visible
    
    public StripesPrimitive_old(
            SphereShape shape,
            Transform transform,
            Appearance appearance,
            Animation animation,
            int stripeCount,
            float stripeRatio,
            boolean alternate) {
        super(shape, transform, appearance, animation);
        this.sphereShape = shape;
        this.stripeCount = Math.max(1, stripeCount);
        this.stripeRatio = Math.max(0.1f, Math.min(0.9f, stripeRatio));
        this.alternate = alternate;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────
    
    public int getStripeCount() { return stripeCount; }
    public float getStripeRatio() { return stripeRatio; }
    public boolean isAlternate() { return alternate; }
    public SphereShape getSphereShape() { return sphereShape; }
    
    @Override
    public String type() { return TYPE; }
    
    /**
     * Checks if a given latitude (0=top, 1=bottom) is in a visible stripe.
     */
    public boolean isInStripe(float latitude) {
        float stripeHeight = 1.0f / stripeCount;
        int stripeIndex = (int) (latitude / stripeHeight);
        float posInStripe = (latitude % stripeHeight) / stripeHeight;
        
        if (alternate) {
            // Alternating: even stripes visible
            return stripeIndex % 2 == 0 && posInStripe < stripeRatio;
        } else {
            // All stripes visible with gaps
            return posInStripe < stripeRatio;
        }
    }
    
    /**
     * Gets the stripe index for a latitude.
     */
    public int getStripeIndex(float latitude) {
        float stripeHeight = 1.0f / stripeCount;
        return (int) (latitude / stripeHeight);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // With methods
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public StripesPrimitive_old withTransform(Transform transform) {
        return new StripesPrimitive_old(sphereShape, transform, appearance, animation, stripeCount, stripeRatio, alternate);
    }
    
    @Override
    public StripesPrimitive_old withAppearance(Appearance appearance) {
        return new StripesPrimitive_old(sphereShape, transform, appearance, animation, stripeCount, stripeRatio, alternate);
    }
    
    @Override
    public StripesPrimitive_old withAnimation(Animation animation) {
        return new StripesPrimitive_old(sphereShape, transform, appearance, animation, stripeCount, stripeRatio, alternate);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates horizontal stripes around a sphere.
     */
    public static StripesPrimitive_old horizontal(int count, float radius) {
        return new StripesPrimitive_old(
            SphereShape.of(radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            count, 0.5f, false
        );
    }
    
    /**
     * Creates zebra-style alternating stripes.
     */
    public static StripesPrimitive_old zebra(int count, float radius) {
        return new StripesPrimitive_old(
            SphereShape.of(radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            count, 0.9f, true
        );
    }
    
    /**
     * Creates thin scan lines.
     */
    public static StripesPrimitive_old scanLines(int count, float radius) {
        return new StripesPrimitive_old(
            SphereShape.of(radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            count, 0.2f, false
        );
    }
    
    /**
     * Creates thick bands with small gaps.
     */
    public static StripesPrimitive_old bands(int count, float radius) {
        return new StripesPrimitive_old(
            SphereShape.of(radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none(),
            count, 0.8f, false
        );
    }
}
