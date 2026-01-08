package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Interface for ray path geometry.
 * 
 * Abstracts how a ray's path is computed - straight line, curved, 
 * with line shape modifiers, etc.
 */
public interface GeoPath {
    
    /**
     * Get position at parameter t (0-1 along path).
     * 
     * @param t Parameter along path (0 = start, 1 = end)
     * @return Position [x, y, z]
     */
    float[] positionAt(float t);
    
    /**
     * Get tangent direction at parameter t.
     * 
     * @param t Parameter along path
     * @return Normalized tangent direction [x, y, z]
     */
    float[] tangentAt(float t);
    
    /**
     * Total length of the path.
     */
    float length();
    
    /**
     * Start position.
     */
    default float[] start() {
        return positionAt(0f);
    }
    
    /**
     * End position.
     */
    default float[] end() {
        return positionAt(1f);
    }
}
