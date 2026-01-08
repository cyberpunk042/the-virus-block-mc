package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Sawtooth line shape - linear ramp pattern.
 * 
 * <p>Based on RayGeometryUtils.computeLineShapeOffsetWithPhase:
 * phase = (t * frequency) % 1.0
 * wave = amplitude * (phase * 2 - 1)
 * Offset in right direction.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class SawtoothLineShape implements LineShapeStrategy {
    
    public static final SawtoothLineShape INSTANCE = new SawtoothLineShape();
    
    private SawtoothLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        // From RayGeometryUtils.computeLineShapeOffsetWithPhase lines 191-196:
        // phase = (t * frequency) % 1.0
        // wave = amplitude * (phase * 2 - 1)
        
        // Adjust t by phase offset
        float adjustedT = t + phaseOffset / ((float) Math.PI * 2);
        float phase = (adjustedT * frequency) % 1.0f;
        if (phase < 0) phase += 1.0f;
        
        float wave = amplitude * (phase * 2 - 1);
        
        return new float[] { wave, 0, 0 };
    }
    
    @Override
    public int suggestedMinSegments() {
        return 8;
    }
}
