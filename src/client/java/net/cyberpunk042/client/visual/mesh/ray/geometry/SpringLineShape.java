package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Spring line shape - compressed helix with lower frequency.
 * 
 * <p>Based on RayGeometryUtils.computeLineShapeOffsetWithPhase:
 * springTheta = t * frequency * 0.5 * 2π
 * cos = cos(springTheta) * amplitude
 * sin = sin(springTheta) * amplitude * 0.8
 * Offset in both right and up for elongated spiral.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class SpringLineShape implements LineShapeStrategy {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    public static final SpringLineShape INSTANCE = new SpringLineShape();
    
    private SpringLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        // From RayGeometryUtils.computeLineShapeOffsetWithPhase lines 173-179:
        // springTheta = t * frequency * 0.5 * TWO_PI + phaseOffset
        // cos = cos(springTheta) * amplitude
        // sin = sin(springTheta) * amplitude * 0.8
        
        float springTheta = t * frequency * 0.5f * TWO_PI + phaseOffset;
        float cos = (float) Math.cos(springTheta) * amplitude;
        float sin = (float) Math.sin(springTheta) * amplitude * 0.8f;
        
        return new float[] { cos, sin, 0 };
    }
    
    @Override
    public int suggestedMinSegments() {
        return 20;
    }
}
