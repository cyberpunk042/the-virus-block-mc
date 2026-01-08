package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * No curvature - identity transform.
 */
public final class NoCurvature implements CurvatureStrategy {
    
    public static final NoCurvature INSTANCE = new NoCurvature();
    
    private NoCurvature() {}
    
    @Override
    public float[] apply(float[] position, float t, float intensity, float[] center) {
        // No change - return as-is
        return position;
    }
}
