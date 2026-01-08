package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Sine wave line shape - serpentine wavy pattern in one axis.
 * 
 * <p>Based on RayGeometryUtils.computeLineShapeOffsetWithPhase:
 * wave = amplitude * sin(t * frequency * 2π + phaseOffset)
 * Offset in "right" direction perpendicular to ray.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class SineWaveLineShape implements LineShapeStrategy {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    public static final SineWaveLineShape INSTANCE = new SineWaveLineShape();
    
    private SineWaveLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        // From RayGeometryUtils.computeLineShapeOffsetWithPhase lines 155, 158-162:
        // theta = t * frequency * TWO_PI + phaseOffset
        // wave = amplitude * sin(theta)
        // offset in right direction
        float theta = t * frequency * TWO_PI + phaseOffset;
        float wave = amplitude * (float) Math.sin(theta);
        
        // Return [right_amount, up_amount, 0]
        // Caller transforms using actual right/up vectors
        return new float[] { wave, 0, 0 };
    }
    
    @Override
    public int suggestedMinSegments() {
        return 16;
    }
}
