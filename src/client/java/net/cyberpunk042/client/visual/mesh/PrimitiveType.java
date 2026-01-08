package net.cyberpunk042.client.visual.mesh;

/**
 * Defines how vertices are interpreted when rendering a {@link Mesh}.
 * 
 * <h2>Usage in Field System</h2>
 * <p>Most field primitives use {@link #TRIANGLES} for solid/translucent rendering.
 * Use {@link #LINES} for wireframe effects.
 * 
 * <h2>Indexed vs Non-Indexed</h2>
 * <ul>
 *   <li><b>Indexed</b> (TRIANGULAR, QUADS, LINES): Uses index array to reference vertices.
 *       More memory-efficient when vertices are shared.</li>
 *   <li><b>Non-indexed</b> (LINE_STRIP, TRIANGLE_FAN): Vertices are consumed in order.
 *       Simpler but less flexible.</li>
 * </ul>
 * 
 * @see Mesh
 * @see MeshBuilder
 */
public enum PrimitiveType {
    
    /**
     * Every 3 indices form a triangle.
     * <p>Most common type for solid geometry. Used by {@link net.cyberpunk042.client.visual.mesh.SphereTessellator_old}.
     */
    TRIANGLES(3),
    
    /**
     * Every 4 indices form a quad.
     * <p>Useful for billboard/sprite rendering. Minecraft converts to triangles internally.
     */
    QUADS(4),
    
    /**
     * Every 2 indices form a line segment.
     * <p>Used for wireframe rendering. See {@link net.cyberpunk042.client.visual.render.MeshStyle_old#WIREFRAME}.
     */
    LINES(2),
    
    /**
     * Vertices form a connected line strip (v0-v1-v2-v3...).
     * <p>First vertex connects to second, second to third, etc.
     * More efficient than LINES for continuous paths.
     */
    LINE_STRIP(1),
    
    /**
     * Vertices form a triangle fan from first vertex.
     * <p>First vertex is center, subsequent vertices form triangles: (v0,v1,v2), (v0,v2,v3), etc.
     * Efficient for circular/radial geometry.
     */
    TRIANGLE_FAN(1);
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────────
    
    private final int verticesPerPrimitive;
    
    PrimitiveType(int verticesPerPrimitive) {
        this.verticesPerPrimitive = verticesPerPrimitive;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Number of indices consumed per primitive element.
     * 
     * <p>For indexed types (TRIANGLES, QUADS, LINES), this is the number of
     * indices per shape. For strips/fans, returns 1 since vertices are shared.
     * 
     * @return indices per primitive (3 for triangles, 4 for quads, etc.)
     */
    public int verticesPerPrimitive() {
        return verticesPerPrimitive;
    }
    
    /**
     * Whether this type uses an index array.
     * 
     * <p>Indexed types allow vertex reuse - important for spheres where
     * many triangles share the same vertices.
     * 
     * @return true if indices are used, false for strips/fans
     */
    public boolean isIndexed() {
        return this == TRIANGLES || this == QUADS || this == LINES;
    }
}
