package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Pinwheel curvature - rays curve like windmill blades.
 * 
 * <p>Based on RayGeometryUtils.computeCurvatureAngle:
 * angle = t * π * 0.75 * intensity
 * Creates curved blades like a pinwheel toy.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class PinwheelCurvature implements CurvatureStrategy {
    
    private static final float PI = (float) Math.PI;
    
    public static final PinwheelCurvature INSTANCE = new PinwheelCurvature();
    
    private PinwheelCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        if (intensity < 0.001f) {
            return position;
        }
        
        // From RayGeometryUtils.computeCurvatureAngle line 312:
        // PINWHEEL -> t * PI * 0.75f
        float angle = t * PI * 0.75f * intensity;
        
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
