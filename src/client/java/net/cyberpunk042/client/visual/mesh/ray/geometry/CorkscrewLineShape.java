package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Corkscrew/Helix line shape - spiral around the ray axis.
 * 
 * <p>Based on RayGeometryUtils.computeLineShapeOffsetWithPhase:
 * cos = cos(theta) * amplitude
 * sin = sin(theta) * amplitude
 * Offset in both right and up directions for full helix.</p>
 * 
 * <p>Also used for DOUBLE_HELIX (called twice with 180° phase difference).</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class CorkscrewLineShape implements LineShapeStrategy {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    public static final CorkscrewLineShape INSTANCE = new CorkscrewLineShape();
    
    private CorkscrewLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        // From RayGeometryUtils.computeLineShapeOffsetWithPhase lines 155, 165-170:
        // theta = t * frequency * TWO_PI + phaseOffset
        // cos = cos(theta) * amplitude
        // sin = sin(theta) * amplitude
        // offset = right * cos + up * sin
        float theta = t * frequency * TWO_PI + phaseOffset;
        float cos = (float) Math.cos(theta) * amplitude;
        float sin = (float) Math.sin(theta) * amplitude;
        
        // Return [right_amount, up_amount, 0]
        return new float[] { cos, sin, 0 };
    }
    
    @Override
    public int suggestedMinSegments() {
        return 24;
    }
}
