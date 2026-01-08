package net.cyberpunk042.client.field.render;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.shape.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of primitive renderers by shape type.
 * 
 * <p>Dispatches to the appropriate {@link PrimitiveRenderer} implementation
 * based on the primitive's shape type.
 * 
 * <h2>Supported Shape Types</h2>
 * <ul>
 *   <li>{@code sphere} → {@link SphereRenderer}</li>
 *   <li>{@code ring} → {@link RingRenderer}</li>
 *   <li>{@code prism} → {@link PrismRenderer}</li>
 *   <li>{@code cylinder} → {@link CylinderRenderer}</li>
 *   <li>{@code polyhedron} → {@link PolyhedronRenderer}</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * PrimitiveRenderer renderer = PrimitiveRenderers.get(primitive);
 * if (renderer != null) {
 *     renderer.render(primitive, matrices, consumer, light, time, resolver);
 * }
 * </pre>
 * 
 * @see PrimitiveRenderer
 */
public final class PrimitiveRenderers {
    
    private static final Map<String, PrimitiveRenderer> RENDERERS = new HashMap<>();
    
    // Static initialization - register all renderers
    static {
        register(new SphereRenderer());
        register(new RingRenderer());
        register(new PrismRenderer());
        register(new CylinderRenderer());
        
        // Polyhedron renderer with aliases for specific types
        PolyhedronRenderer polyRenderer = new PolyhedronRenderer();
        register(polyRenderer);
        registerAlias("cube", polyRenderer);
        registerAlias("octahedron", polyRenderer);
        registerAlias("icosahedron", polyRenderer);
        registerAlias("tetrahedron", polyRenderer);
        registerAlias("dodecahedron", polyRenderer);
        
        // Cylinder alias for CylinderRenderer (beam uses cylinder internally)
        registerAlias("beam", RENDERERS.get("cylinder"));
        
        // New shape renderers
        register(new TorusRenderer());
        register(new CapsuleRenderer());
        register(new ConeRenderer());
        register(new JetRenderer());
        register(new RaysRenderer());
        register(new KamehamehaRenderer());
        register(new MoleculeRenderer());
        
        Logging.FIELD.topic("init").debug(
            "Registered {} primitive renderers", RENDERERS.size());
    }
    
    /**
     * Registers a renderer alias (same renderer, different type name).
     */
    public static void registerAlias(String alias, PrimitiveRenderer renderer) {
        RENDERERS.put(alias, renderer);
        Logging.FIELD.topic("init").trace(
            "Registered alias: {} → {}", alias, renderer.getClass().getSimpleName());
    }
    
    private PrimitiveRenderers() {}
    
    /**
     * Registers a renderer for its shape type.
     */
    public static void register(PrimitiveRenderer renderer) {
        RENDERERS.put(renderer.shapeType(), renderer);
        Logging.FIELD.topic("init").trace(
            "Registered renderer: {} → {}", 
            renderer.shapeType(), renderer.getClass().getSimpleName());
    }
    
    /**
     * Gets the renderer for a primitive.
     * 
     * @param primitive The primitive to render
     * @return The appropriate renderer, or null if not found
     */
    public static PrimitiveRenderer get(Primitive primitive) {
        if (primitive == null || primitive.shape() == null) {
            return null;
        }
        return get(primitive.type());
    }
    
    /**
     * Gets the renderer for a shape type.
     * 
     * @param shapeType The shape type string (e.g., "sphere", "ring")
     * @return The appropriate renderer, or null if not found
     */
    public static PrimitiveRenderer get(String shapeType) {
        return RENDERERS.get(shapeType);
    }
    
    /**
     * Gets the renderer for a shape.
     * 
     * @param shape The shape to render
     * @return The appropriate renderer, or null if not found
     */
    public static PrimitiveRenderer get(Shape shape) {
        if (shape == null) return null;
        
        // Determine type from shape class
        String type = switch (shape) {
            case SphereShape s -> "sphere";
            case RingShape r -> "ring";
            case PrismShape p -> "prism";
            case CylinderShape c -> "cylinder";
            case PolyhedronShape p -> "polyhedron";
            case TorusShape t -> "torus";
            case CapsuleShape c -> "capsule";
            case ConeShape c -> "cone";
            case JetShape j -> "jet";
            case RaysShape r -> "rays";
            case KamehamehaShape k -> "kamehameha";
            case MoleculeShape m -> "molecule";
            default -> null;
        };
        
        return type != null ? RENDERERS.get(type) : null;
    }
    
    /**
     * Checks if a renderer exists for the given shape type.
     */
    public static boolean hasRenderer(String shapeType) {
        return RENDERERS.containsKey(shapeType);
    }
    
    /**
     * Returns all registered renderer count.
     */
    public static int count() {
        return RENDERERS.size();
    }
}

