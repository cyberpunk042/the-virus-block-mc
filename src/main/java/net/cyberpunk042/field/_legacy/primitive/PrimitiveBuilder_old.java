package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.Transform;
import net.minecraft.util.math.Vec3d;

/**
 * Fluent builder for creating primitives.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Build a spinning sphere
 * Primitive sphere = PrimitiveBuilder_old.sphere()
 *     .radius(2.0f)
 *     .detail(32)
 *     .color("@primary")
 *     .alpha(0.8f)
 *     .spin(0.5f)
 *     .build();
 * 
 * // Build a glowing ring
 * Primitive ring = PrimitiveBuilder_old.ring()
 *     .radius(1.5f)
 *     .thickness(0.2f)
 *     .y(0.5f)
 *     .glow(true)
 *     .color("@glow")
 *     .build();
 * </pre>
 */
public final class PrimitiveBuilder_old {
    
    private PrimitiveBuilder_old() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Entry Points
    // ─────────────────────────────────────────────────────────────────────────
    
    public static SphereBuilder sphere() {
        return new SphereBuilder();
    }
    
    public static RingBuilder ring() {
        return new RingBuilder();
    }
    
    public static PrismBuilder prism() {
        return new PrismBuilder();
    }
    
    public static CageBuilder cage() {
        return new CageBuilder();
    }
    
    public static BeamBuilder beam() {
        return new BeamBuilder();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Sphere Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static class SphereBuilder {
        private float radius = 1.0f;
        private int detail = 32;
        private String color = "@primary";
        private float alpha = 1.0f;
        private boolean fill = true;
        private boolean glow = false;
        private float spin = 0;
        private float pulse = 0;
        private Vec3d offset = Vec3d.ZERO;
        
        public SphereBuilder radius(float r) { this.radius = r; return this; }
        public SphereBuilder detail(int d) { this.detail = d; return this; }
        public SphereBuilder color(String c) { this.color = c; return this; }
        public SphereBuilder alpha(float a) { this.alpha = a; return this; }
        public SphereBuilder fill(boolean f) { this.fill = f; return this; }
        public SphereBuilder glow(boolean g) { this.glow = g; return this; }
        public SphereBuilder spin(float s) { this.spin = s; return this; }
        public SphereBuilder pulse(float p) { this.pulse = p; return this; }
        public SphereBuilder offset(Vec3d o) { this.offset = o; return this; }
        public SphereBuilder offset(double x, double y, double z) { 
            return offset(new Vec3d(x, y, z)); 
        }
        
        public SpherePrimitive_old build() {
            SphereShape shape = SphereShape.of(radius, detail);
            Transform transform = offset.equals(Vec3d.ZERO) 
                ? Transform.identity() 
                : Transform.offset(offset);
            Appearance appearance = glow 
                ? Appearance.glowing(color, alpha)
                : Appearance.translucent(color, alpha);
            Animation animation = (spin > 0 || pulse > 0) ? Animation.spinningAndPulsing(spin, pulse, 0.1f) : Animation.none();
            
            Logging.REGISTRY.topic("primitive-builder").debug(
                "Built sphere: radius={:.2f}, detail={}, glow={}", radius, detail, glow);
            
            return new SpherePrimitive_old(shape, transform, appearance, animation);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Ring Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static class RingBuilder {
        private float radius = 1.0f;
        private float thickness = 0.1f;
        private float y = 0;
        private int segments = 48;
        private String color = "@primary";
        private float alpha = 1.0f;
        private boolean glow = false;
        private float spin = 0;
        
        public RingBuilder radius(float r) { this.radius = r; return this; }
        public RingBuilder thickness(float t) { this.thickness = t; return this; }
        public RingBuilder y(float y) { this.y = y; return this; }
        public RingBuilder segments(int s) { this.segments = s; return this; }
        public RingBuilder color(String c) { this.color = c; return this; }
        public RingBuilder alpha(float a) { this.alpha = a; return this; }
        public RingBuilder glow(boolean g) { this.glow = g; return this; }
        public RingBuilder spin(float s) { this.spin = s; return this; }
        
        public RingPrimitive_old build() {
            RingShape shape = RingShape.of(y, radius, thickness, segments);
            Transform transform = Transform.identity();
            Appearance appearance = glow 
                ? Appearance.glowing(color, alpha)
                : Appearance.translucent(color, alpha);
            Animation animation = spin > 0 ? Animation.spinning(spin) : Animation.none();
            
            Logging.REGISTRY.topic("primitive-builder").debug(
                "Built ring: radius={:.2f}, y={:.2f}, glow={}", radius, y, glow);
            
            return new RingPrimitive_old(shape, transform, appearance, animation);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Prism Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static class PrismBuilder {
        private int sides = 6;
        private float radius = 1.0f;
        private float height = 1.0f;
        private boolean capped = true;
        private String color = "@primary";
        private float alpha = 1.0f;
        private boolean glow = false;
        
        public PrismBuilder sides(int s) { this.sides = s; return this; }
        public PrismBuilder hexagonal() { return sides(6); }
        public PrismBuilder triangular() { return sides(3); }
        public PrismBuilder square() { return sides(4); }
        public PrismBuilder octagonal() { return sides(8); }
        public PrismBuilder radius(float r) { this.radius = r; return this; }
        public PrismBuilder height(float h) { this.height = h; return this; }
        public PrismBuilder capped(boolean c) { this.capped = c; return this; }
        public PrismBuilder color(String c) { this.color = c; return this; }
        public PrismBuilder alpha(float a) { this.alpha = a; return this; }
        public PrismBuilder glow(boolean g) { this.glow = g; return this; }
        
        public PrismPrimitive_old build() {
            PrismShape shape = new PrismShape(sides, radius, height);
            Transform transform = Transform.identity();
            Appearance appearance = glow 
                ? Appearance.glowing(color, alpha)
                : Appearance.translucent(color, alpha);
            Animation animation = Animation.none();
            
            Logging.REGISTRY.topic("primitive-builder").debug(
                "Built prism: sides={}, radius={:.2f}, height={:.2f}", sides, radius, height);
            
            return new PrismPrimitive_old(shape, transform, appearance, animation, capped);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Cage Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static class CageBuilder {
        private float radius = 1.0f;
        private int lines = 8;
        private float wireThickness = 1.0f;
        private String color = "@wire";
        private float alpha = 1.0f;
        private float spin = 0;
        
        public CageBuilder radius(float r) { this.radius = r; return this; }
        public CageBuilder lines(int l) { this.lines = l; return this; }
        public CageBuilder wireThickness(float t) { this.wireThickness = t; return this; }
        public CageBuilder color(String c) { this.color = c; return this; }
        public CageBuilder alpha(float a) { this.alpha = a; return this; }
        public CageBuilder spin(float s) { this.spin = s; return this; }
        
        public CagePrimitive_old build() {
            SphereShape shape = SphereShape.of(radius);
            Transform transform = Transform.identity();
            Appearance appearance = Appearance.wireframe(color);
            Animation animation = spin > 0 ? Animation.spinning(spin) : Animation.none();
            
            Logging.REGISTRY.topic("primitive-builder").debug(
                "Built cage: radius={:.2f}, lines={}", radius, lines);
            
            return new CagePrimitive_old(shape, transform, appearance, animation, 
                                     wireThickness, lines, lines * 2);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Beam Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static class BeamBuilder {
        private float radius = 0.1f;
        private float height = 10.0f;
        private String color = "@beam";
        private float alpha = 0.8f;
        private boolean glow = true;
        
        public BeamBuilder radius(float r) { this.radius = r; return this; }
        public BeamBuilder height(float h) { this.height = h; return this; }
        public BeamBuilder thin() { return radius(0.05f); }
        public BeamBuilder thick() { return radius(0.2f); }
        public BeamBuilder color(String c) { this.color = c; return this; }
        public BeamBuilder alpha(float a) { this.alpha = a; return this; }
        public BeamBuilder glow(boolean g) { this.glow = g; return this; }
        
        public CylinderPrimitive_old build() {
            CylinderShape shape = CylinderShape.of(radius, height);
            Transform transform = Transform.identity();
            Appearance appearance = glow 
                ? Appearance.glowing(color, alpha)
                : Appearance.translucent(color, alpha);
            Animation animation = Animation.none();
            
            Logging.REGISTRY.topic("primitive-builder").debug(
                "Built beam: radius={:.2f}, height={:.2f}", radius, height);
            
            return new CylinderPrimitive_old(shape, transform, appearance, animation, 1.0f);
        }
    }
}
