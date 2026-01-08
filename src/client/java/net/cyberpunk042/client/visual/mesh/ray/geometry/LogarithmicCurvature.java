package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Logarithmic curvature - rays curve with logarithmic angle progression.
 * 
 * <p>Based on RayGeometryUtils.computeCurvatureAngle:
 * angle = log(1 + t * 3) * π * 0.5 * intensity
 * Creates a more natural, organic spiral like nautilus shells.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class LogarithmicCurvature implements CurvatureStrategy {
    
    private static final float PI = (float) Math.PI;
    
    public static final LogarithmicCurvature INSTANCE = new LogarithmicCurvature();
    
    private LogarithmicCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        if (intensity < 0.001f) {
            return position;
        }
        
        // From RayGeometryUtils.computeCurvatureAngle line 311:
        // LOGARITHMIC -> (float) Math.log(1 + t * 3) * PI * 0.5f
        float angle = (float) Math.log(1 + t * 3) * PI * 0.5f * intensity;
        
        // From RayGeometryUtils.computeCurvedPosition lines 249-256:
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
