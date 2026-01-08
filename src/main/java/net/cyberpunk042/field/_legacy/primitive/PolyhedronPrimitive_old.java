package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.shape.PolyhedronShape;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.transform.Transform;

/**
 * A polyhedron primitive (cube, octahedron, icosahedron, etc.).
 * 
 * <p>Renders platonic solids using {@link PolyhedronShape}.
 * 
 * <h2>Types</h2>
 * <ul>
 *   <li>CUBE - 6 faces, classic box shape</li>
 *   <li>OCTAHEDRON - 8 faces, diamond shape</li>
 *   <li>ICOSAHEDRON - 20 faces, approximates sphere</li>
 *   <li>DODECAHEDRON - 12 pentagonal faces</li>
 *   <li>TETRAHEDRON - 4 faces, pyramid</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Create a cube
 * PolyhedronPrimitive_old cube = PolyhedronPrimitive_old.cube(1.0f);
 * 
 * // Create an octahedron with custom appearance
 * PolyhedronPrimitive_old diamond = PolyhedronPrimitive_old.octahedron(0.5f)
 *     .withAppearance(Appearance.glowing("@glow", 0.5f));
 * </pre>
 * 
 * @see PolyhedronShape
 */
public class PolyhedronPrimitive_old extends SolidPrimitive_old {
    
    public static final String TYPE = "polyhedron";
    
    private final PolyhedronShape polyShape;
    
    public PolyhedronPrimitive_old(
            PolyhedronShape shape,
            Transform transform,
            Appearance appearance,
            Animation animation) {
        super(shape, transform, appearance, animation);
        this.polyShape = shape;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public Shape shape() {
        return polyShape;
    }
    
    @Override
    public String type() {
        return TYPE;
    }
    
    public PolyhedronShape getPolyShape() {
        return polyShape;
    }
    
    public PolyhedronShape.Type getPolyType() {
        return polyShape.type();
    }
    
    public float getSize() {
        return polyShape.size();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // With methods
    // ─────────────────────────────────────────────────────────────────────────
    
    @Override
    public PolyhedronPrimitive_old withTransform(Transform transform) {
        return new PolyhedronPrimitive_old(polyShape, transform, appearance, animation);
    }
    
    @Override
    public PolyhedronPrimitive_old withAppearance(Appearance appearance) {
        return new PolyhedronPrimitive_old(polyShape, transform, appearance, animation);
    }
    
    @Override
    public PolyhedronPrimitive_old withAnimation(Animation animation) {
        return new PolyhedronPrimitive_old(polyShape, transform, appearance, animation);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates a cube primitive.
     */
    public static PolyhedronPrimitive_old cube(float size) {
        return new PolyhedronPrimitive_old(
            PolyhedronShape.cube(size),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    /**
     * Creates an octahedron (diamond) primitive.
     */
    public static PolyhedronPrimitive_old octahedron(float size) {
        return new PolyhedronPrimitive_old(
            PolyhedronShape.octahedron(size),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    /**
     * Creates an icosahedron primitive.
     */
    public static PolyhedronPrimitive_old icosahedron(float size) {
        return new PolyhedronPrimitive_old(
            PolyhedronShape.icosahedron(size),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    /**
     * Creates a tetrahedron (pyramid) primitive.
     */
    public static PolyhedronPrimitive_old tetrahedron(float size) {
        return new PolyhedronPrimitive_old(
            PolyhedronShape.tetrahedron(size),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    /**
     * Creates a dodecahedron primitive.
     */
    public static PolyhedronPrimitive_old dodecahedron(float size) {
        return new PolyhedronPrimitive_old(
            PolyhedronShape.dodecahedron(size),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
}

