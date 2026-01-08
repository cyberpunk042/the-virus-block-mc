package net.cyberpunk042.client.visual.mesh.ray.geometry;

import net.cyberpunk042.visual.shape.RayLineShape;

/**
 * Factory for creating LineShapeStrategy instances.
 * 
 * <p>Maps RayLineShape enum values to their corresponding strategy implementations.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeLineShapeOffsetWithPhase
 */
public final class LineShapeFactory {
    
    private LineShapeFactory() {}
    
    /**
     * Get the appropriate strategy for a line shape type.
     */
    public static LineShapeStrategy get(RayLineShape lineShape) {
        if (lineShape == null) {
            return StraightLineShape.INSTANCE;
        }
        
        // Matches RayGeometryUtils.computeLineShapeOffsetWithPhase switch cases
        return switch (lineShape) {
            case STRAIGHT -> StraightLineShape.INSTANCE;
            case SINE_WAVE -> SineWaveLineShape.INSTANCE;
            case CORKSCREW, DOUBLE_HELIX -> CorkscrewLineShape.INSTANCE;
            case SPRING -> SpringLineShape.INSTANCE;
            case ZIGZAG -> ZigzagLineShape.INSTANCE;
            case SAWTOOTH -> SawtoothLineShape.INSTANCE;
            case SQUARE_WAVE -> SquareWaveLineShape.INSTANCE;
            case ARC -> ArcLineShape.INSTANCE;
            case S_CURVE -> SCurveLineShape.INSTANCE;
        };
    }
}
