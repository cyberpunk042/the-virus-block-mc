package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Spiral curvature - rays curve in a full spiral pattern.
 * 
 * <p>Based on RayGeometryUtils.computeCurvatureAngle:
 * angle = t * π * intensity
 * Creates curved arms like a galaxy spiral.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class SpiralCurvature implements CurvatureStrategy {
    
    private static final float PI = (float) Math.PI;
    
    public static final SpiralCurvature INSTANCE = new SpiralCurvature();
    
    private SpiralCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        if (intensity < 0.001f) {
            return position;
        }
        
        // From RayGeometryUtils.computeCurvatureAngle line 309:
        // SPIRAL_ARM -> t * PI
        float angle = t * PI * intensity;
        
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
