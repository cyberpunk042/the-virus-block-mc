package net.cyberpunk042.client.visual.mesh.ray.geometry;

/**
 * Strategy for applying line shape modifiers to rays.
 * 
 * Different line shapes (STRAIGHT, WAVY, HELIX, CORKSCREW, DOUBLE_HELIX)
 * implement this interface.
 */
public interface LineShapeStrategy {
    
    /**
     * Compute perpendicular offset at parameter t.
     * 
     * @param t Parameter along ray (0-1)
     * @param amplitude Shape amplitude
     * @param frequency Shape frequency
     * @param phaseOffset Phase offset for animation
     * @return Offset [x, y, z] to add to base position
     */
    float[] computeOffset(float t, float amplitude, float frequency, float phaseOffset);
    
    /**
     * Whether this shape needs multiple segments to render properly.
     */
    default boolean needsSegments() {
        return true;
    }
    
    /**
     * Suggested minimum segments for smooth rendering.
     */
    default int suggestedMinSegments() {
        return 16;
    }
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
