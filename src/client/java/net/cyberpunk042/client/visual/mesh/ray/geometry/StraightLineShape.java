package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Straight line shape - no offset (identity).
 */
public final class StraightLineShape implements LineShapeStrategy {
    
    private static final float[] ZERO = { 0, 0, 0 };
    
    public static final StraightLineShape INSTANCE = new StraightLineShape();
    
    private StraightLineShape() {}
    
    @Override
    public float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset) {
        return ZERO;
    }
    
    @Override
    public boolean needsSegments() {
        return false;
    }
    
    @Override
    public int suggestedMinSegments() {
        return 1;
    }
}
