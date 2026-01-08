package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Mutable builder for SimplePrimitive.
 * 
 * <p>Used by adapters to incrementally build a primitive, with each adapter
 * contributing its piece. SimplePrimitive is a record (immutable), so this
 * builder collects all parts before constructing.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * PrimitiveBuilder builder = new PrimitiveBuilder().id("prim1");
 * shapeAdapter.saveTo(builder);
 * animationAdapter.saveTo(builder);
 * fillAdapter.saveTo(builder);
 * SimplePrimitive result = builder.build();
 * </pre>
 */
public class PrimitiveBuilder {
    
    private String id = "primitive";
    private String type = "sphere";
    private Shape shape = SphereShape.DEFAULT;
    private Transform transform = Transform.IDENTITY;
    private FillConfig fill = FillConfig.SOLID;
    private VisibilityMask visibility = VisibilityMask.FULL;
    private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    private Appearance appearance = Appearance.DEFAULT;
    private Animation animation = Animation.NONE;
    private PrimitiveLink link = null;
    
    public PrimitiveBuilder() {}
    
    /**
     * Initialize from an existing primitive (for editing).
     */
    public PrimitiveBuilder(SimplePrimitive source) {
        this.id = source.id();
        this.type = source.type();
        this.shape = source.shape();
        this.transform = source.transform();
        this.fill = source.fill();
        this.visibility = source.visibility();
        this.arrangement = source.arrangement();
        this.appearance = source.appearance();
        this.animation = source.animation();
        this.link = source.link();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public PrimitiveBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    public PrimitiveBuilder type(String type) {
        this.type = type;
        return this;
    }
    
    public PrimitiveBuilder shape(Shape shape) {
        this.shape = shape;
        return this;
    }
    
    public PrimitiveBuilder transform(Transform transform) {
        this.transform = transform;
        return this;
    }
    
    public PrimitiveBuilder fill(FillConfig fill) {
        this.fill = fill;
        return this;
    }
    
    public PrimitiveBuilder visibility(VisibilityMask visibility) {
        this.visibility = visibility;
        return this;
    }
    
    public PrimitiveBuilder arrangement(ArrangementConfig arrangement) {
        this.arrangement = arrangement;
        return this;
    }
    
    public PrimitiveBuilder appearance(Appearance appearance) {
        this.appearance = appearance;
        return this;
    }
    
    public PrimitiveBuilder animation(Animation animation) {
        this.animation = animation;
        return this;
    }
    
    public PrimitiveBuilder link(PrimitiveLink link) {
        this.link = link;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for adapters that need to read current values)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String id() { return id; }
    public String type() { return type; }
    public Shape shape() { return shape; }
    public Transform transform() { return transform; }
    public FillConfig fill() { return fill; }
    public VisibilityMask visibility() { return visibility; }
    public ArrangementConfig arrangement() { return arrangement; }
    public Appearance appearance() { return appearance; }
    public Animation animation() { return animation; }
    public PrimitiveLink link() { return link; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Build the final SimplePrimitive.
     */
    public SimplePrimitive build() {
        return new SimplePrimitive(
            id, type, shape, transform, fill, visibility,
            arrangement, appearance, animation, link
        );
    }
}
