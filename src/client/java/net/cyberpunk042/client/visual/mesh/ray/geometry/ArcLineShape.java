package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Arc line shape - single smooth curve.
 * 
 * <p>Based on RayGeometryUtils.computeLineShapeOffsetWithPhase:
 * curve = amplitude * sin(t * π)
 * Offset in up direction (bowing effect).</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class ArcLineShape implements LineShapeStrategy {
    
    private static final float PI = (float) Math.PI;
    
    public static final ArcLineShape INSTANCE = new ArcLineShape();
    
    private ArcLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        // From RayGeometryUtils.computeLineShapeOffsetWithPhase lines 206-210:
        // curve = amplitude * sin(t * PI)
        // offset in up direction
        
        float curve = amplitude * (float) Math.sin(t * PI);
        
        // Return [right_amount, up_amount, 0]
        // Arc goes in UP direction (Y), not right
        return new float[] { 0, curve, 0 };
    }
    
    @Override
    public int suggestedMinSegments() {
        return 12;
    }
}
