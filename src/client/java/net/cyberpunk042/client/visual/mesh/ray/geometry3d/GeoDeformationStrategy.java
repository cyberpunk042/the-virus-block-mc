package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

import net.cyberpunk042.visual.shape.FieldDeformationMode;

/**
 * Strategy for applying gravitational deformation to 3D ray shapes.
 */
public interface GeoDeformationStrategy {
    
    /**
     * Apply deformation to a vertex position.
     * 
     * @param position Vertex position [x, y, z] - modified in place
     * @param t Position along ray (0 = base, 1 = tip)
     * @param fieldCenter Field center position [x, y, z]
     * @param intensity Deformation intensity
     * @param fieldRadius Field outer radius
     */
    void apply(float[] position, float t, float[] fieldCenter, float intensity, float fieldRadius);
    
    /**
     * Convenience method that returns a new deformed position.
     */
    default float[] deform(float[] position, float[] fieldCenter, float intensity, float fieldRadius) {
        float[] result = new float[] { position[0], position[1], position[2] };
        apply(result, 0.5f, fieldCenter, intensity, fieldRadius);  // Use t=0.5 for uniform deformation
        return result;
    }
    
    /**
     * Whether this strategy applies any deformation.
     */
    default boolean isActive() {
        return true;  // Override in GeoNoDeformation
    }
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}

