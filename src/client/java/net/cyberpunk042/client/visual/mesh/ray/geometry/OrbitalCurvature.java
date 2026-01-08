package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Orbital curvature - rays follow circular orbital paths.
 * 
 * <p>Based on RayGeometryUtils.computeCurvatureAngle:
 * angle = t * 2π * intensity
 * Creates full circular orbits around the center.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class OrbitalCurvature implements CurvatureStrategy {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    public static final OrbitalCurvature INSTANCE = new OrbitalCurvature();
    
    private OrbitalCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        if (intensity < 0.001f) {
            return position;
        }
        
        // From RayGeometryUtils.computeCurvatureAngle line 313:
        // ORBITAL -> t * TWO_PI
        float angle = t * TWO_PI * intensity;
        
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
