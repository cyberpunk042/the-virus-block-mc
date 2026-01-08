package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.*;

/**
 * Tessellates shapes into triangle meshes for rendering.
 * 
 * <h2>What is Tessellation?</h2>
 * <p>Tessellation converts a mathematical shape definition (like "sphere with radius 1.0")
 * into concrete triangles that can be rendered by the GPU. The result is a {@link Mesh}
 * containing vertices, normals, and texture coordinates.</p>
 * 
 * <h2>Detail Level</h2>
 * <p>The {@code detail} parameter controls mesh quality:</p>
 * <ul>
 *   <li><b>Low (4-8):</b> Fast but visible facets</li>
 *   <li><b>Medium (16-32):</b> Good balance for most uses</li>
 *   <li><b>High (64-128):</b> Smooth but expensive</li>
 * </ul>
 * 
 * <h2>Supported Shapes</h2>
 * <table>
 *   <tr><th>Shape</th><th>Primary CellType</th><th>Status</th></tr>
 *   <tr><td>SphereShape</td><td>QUAD</td><td>TODO: Implement SphereTessellator</td></tr>
 *   <tr><td>RingShape</td><td>SEGMENT</td><td>TODO: Implement RingTessellator</td></tr>
 *   <tr><td>PrismShape</td><td>QUAD</td><td>TODO: Implement PrismTessellator</td></tr>
 *   <tr><td>CylinderShape</td><td>QUAD</td><td>TODO: Implement CylinderTessellator</td></tr>
 *   <tr><td>PolyhedronShape</td><td>TRIANGLE</td><td>PolyhedronTessellator (implemented)</td></tr>
 * </table>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Static factory method (recommended)
 * Mesh mesh = Tessellator.tessellate(shape, DetailLevel.MEDIUM.value);
 * 
 * // With specific tessellator for more control
 * Mesh mesh = PolyhedronTessellator.fromShape(polyShape).tessellate(0);
 * </pre>
 * 
 * @see Mesh
 * @see Shape
 */
public interface Tessellator {
    
    // =========================================================================
    // Detail Level Constants
    // =========================================================================
    
    /**
     * Predefined detail levels for common use cases.
     */
    enum DetailLevel {
        /** Minimal detail - fast but blocky (4 segments) */
        MINIMAL(4),
        
        /** Low detail - visible facets but fast (8 segments) */
        LOW(8),
        
        /** Medium detail - good balance (16 segments) */
        MEDIUM(16),
        
        /** High detail - smooth appearance (32 segments) */
        HIGH(32),
        
        /** Very high detail - very smooth (64 segments) */
        VERY_HIGH(64),
        
        /** Maximum detail - highest quality (128 segments) */
        MAXIMUM(128);
        
        /** The numeric detail value. */
        public final int value;
        
        DetailLevel(int value) {
            this.value = value;
        }
        
        /**
         * Gets the appropriate detail level for a given radius.
         * Larger objects need more detail to look smooth.
         * 
         * @param radius Object radius
         * @return Recommended detail level
         */
        public static DetailLevel forRadius(float radius) {
            if (radius < 0.5f) return LOW;
            if (radius < 1.0f) return MEDIUM;
            if (radius < 2.0f) return HIGH;
            if (radius < 5.0f) return VERY_HIGH;
            return MAXIMUM;
        }
    }
    
    // =========================================================================
    // Minimum Detail Constants
    // =========================================================================
    
    /** Minimum segments for circular shapes (prevents degenerate triangles) */
    int MIN_CIRCULAR_SEGMENTS = 8;
    
    /** Minimum latitude steps for spheres */
    int MIN_SPHERE_LAT_STEPS = 8;
    
    /** Minimum longitude steps for spheres */
    int MIN_SPHERE_LON_STEPS = 8;
    
    /** Minimum sides for prisms/cylinders */
    int MIN_PRISM_SIDES = 3;
    
    // =========================================================================
    // Static Factory Method (Primary API)
    // =========================================================================
    
    /**
     * Tessellates any shape into a mesh.
     * 
     * <p>This is the primary tessellation API. It automatically selects the
     * appropriate tessellator based on the shape's type.</p>
     * 
     * <h3>Detail Guidelines</h3>
     * <ul>
     *   <li>UI previews: {@link DetailLevel#LOW}</li>
     *   <li>In-game effects: {@link DetailLevel#MEDIUM} to {@link DetailLevel#HIGH}</li>
     *   <li>Close-up rendering: {@link DetailLevel#VERY_HIGH}</li>
     * </ul>
     * 
     * @param shape The shape definition to tessellate
     * @param detail Level of detail (higher = more triangles)
     * @return Generated mesh with vertices, normals, and UVs
     * @throws IllegalArgumentException if shape is null
     * 
     * @see DetailLevel
     */
    static Mesh tessellate(Shape shape, int detail) {
        if (shape == null) {
            throw new IllegalArgumentException("Shape cannot be null");
        }
        
        // Log tessellation request
        Logging.RENDER.topic("tessellate")
            .kv("shape", shape.getClass().getSimpleName())
            .kv("detail", detail)
            .debug("Tessellating shape");
        
        // Dispatch to appropriate tessellator based on shape type
        return switch (shape) {
            
            // === SPHERE ===
            case SphereShape sphere -> SphereTessellator.tessellate(sphere);
            
            // === RING ===
            case RingShape ring -> RingTessellator.tessellate(ring);
            
            // === PRISM ===
            case PrismShape prism -> PrismTessellator.tessellate(prism);
            
            // === CYLINDER ===
            case CylinderShape cylinder -> CylinderTessellator.tessellate(cylinder);
            
            // === POLYHEDRON ===
            case PolyhedronShape polyhedron -> PolyhedronTessellator
                .fromShape(polyhedron)
                .tessellate(detail);
            
            // === MOLECULE ===
            case MoleculeShape molecule -> MoleculeTessellator.tessellate(molecule);
            default -> {
                Logging.RENDER.topic("tessellate")
                    .reason("unknown shape type")
                    .alwaysChat()
                    .warn("Cannot tessellate unknown shape type: {}", 
                        shape.getClass().getSimpleName());
                yield Mesh.empty();
            }
        };
    }
    
    /**
     * Tessellates with automatic detail level based on shape size.
     * 
     * @param shape The shape to tessellate
     * @return Generated mesh
     */
    static Mesh tessellateAuto(Shape shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Shape cannot be null");
        }
        
        // Estimate radius from bounds
        float radius = shape.getBounds().length() / 2;
        DetailLevel level = DetailLevel.forRadius(radius);
        
        return tessellate(shape, level.value);
    }
    
    // =========================================================================
    // Instance Methods (for custom tessellators)
    // =========================================================================
    
    /**
     * Generates a mesh with the given level of detail.
     * 
     * @param detail Level of detail (interpretation depends on implementation)
     * @return Generated mesh
     */
    Mesh tessellate(int detail);
    
    /**
     * Returns a default detail level appropriate for this tessellator.
     */
    default int defaultDetail() {
        return DetailLevel.MEDIUM.value;
    }
    
    /**
     * Returns the minimum valid detail level.
     */
    default int minDetail() {
        return DetailLevel.MINIMAL.value;
    }
    
    /**
     * Returns the maximum valid detail level.
     */
    default int maxDetail() {
        return DetailLevel.MAXIMUM.value;
    }
    
    /**
     * Clamps detail to valid range.
     * 
     * @param detail Requested detail level
     * @return Clamped detail level
     */
    default int clampDetail(int detail) {
        return Math.max(minDetail(), Math.min(maxDetail(), detail));
    }
}
