package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Tangential curvature - rays are perpendicular to radial direction.
 * 
 * <p>Based on RayGeometryUtils.computeCurvatureAngle:
 * angle = π/2 * intensity (always 90 degrees, constant)
 * Creates rays that are tangent to circles around the center.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class TangentialCurvature implements CurvatureStrategy {
    
    private static final float HALF_PI = (float) (Math.PI * 0.5);
    
    public static final TangentialCurvature INSTANCE = new TangentialCurvature();
    
    private TangentialCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        if (intensity < 0.001f) {
            return position;
        }
        
        // From RayGeometryUtils.computeCurvatureAngle line 310:
        // TANGENTIAL -> PI * 0.5f (constant 90 degrees)
        float angle = HALF_PI * intensity;
        
        // Rotate around Y axis (simplified curvature)
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        float x = position[0] * cos - position[2] * sin;
        float z = position[0] * sin + position[2] * cos;
        
        position[0] = x;
        position[2] = z;
        
        return position;
    }
}
