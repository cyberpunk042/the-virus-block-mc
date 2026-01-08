package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;
import net.cyberpunk042.visual.pattern.CellType;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Interface for all geometric shapes in the field system.
 * 
 * <p>Shapes define geometry parameters only - they don't know about
 * rendering, colors, or animation. Those are handled by Primitive.</p>
 * 
 * <h2>Shape Types</h2>
 * <ul>
 *   <li>{@link SphereShape} - Lat/lon sphere, icosphere, UV sphere</li>
 *   <li>{@link RingShape} - Torus-like ring</li>
 *   <li>{@link PrismShape} - N-sided prism/cylinder</li>
 *   <li>{@link PolyhedronShape} - Platonic solids</li>
 *   <li>{@link CylinderShape} - Cylinder/tube/beam</li>
 * </ul>
 * 
 * <h2>Multi-Part Shapes</h2>
 * <p>Some shapes have multiple parts with different cell types:</p>
 * <ul>
 *   <li>Sphere: main (QUAD), poles (TRIANGLE), equator (QUAD)</li>
 *   <li>Prism: sides (QUAD), caps (SECTOR)</li>
 * </ul>
 * 
 * @see CellType
 * @see net.cyberpunk042.field.primitive.Primitive
 */
public interface Shape {
    
    /**
     * Gets the shape type identifier.
     * @return Type name (e.g., "sphere", "ring")
     */
    String getType();
    
    /**
     * Gets the bounding box dimensions.
     * @return Vector3f with (width, height, depth)
     */
    Vector3f getBounds();
    
    /**
     * Gets the primary cell type for this shape.
     * <p>This is the most common cell type used in tessellation.</p>
     * @return Primary CellType
     */
    CellType primaryCellType();
    
    /**
     * Gets all parts of this shape with their cell types.
     * <p>Used for multi-part arrangement configuration.</p>
     * @return Map of part name to CellType
     */
    Map<String, CellType> getParts();
    
    /**
     * Gets the approximate radius of this shape.
     * <p>Used for radius linking between primitives.</p>
     * @return Approximate radius
     */
    default float getRadius() {
        Vector3f bounds = getBounds();
        return Math.max(bounds.x, Math.max(bounds.y, bounds.z)) / 2;
    }
    
    /**
     * Serializes this shape to JSON.
     * <p>Implementations should include all shape-specific parameters.</p>
     * @return JsonObject with shape data
     */
    JsonObject toJson();
}
