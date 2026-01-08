package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Strategy for applying curvature to ray paths.
 * 
 * Different curvature modes (NONE, VORTEX, SPIRAL, GRAVITATIONAL)
 * implement this interface.
 */
public interface CurvatureStrategy {
    
    /**
     * Apply curvature to a position.
     * 
     * @param position Input position [x, y, z]
     * @param t Parameter along ray (0-1)
     * @param intensity Curvature intensity
     * @param center Field center position [x, y, z]
     * @return Curved position [x, y, z] (may be same array, modified)
     */
    float[] apply(float[] position, float t, float intensity, float[] center);
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
