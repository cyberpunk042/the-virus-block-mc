package net.cyberpunk042.client.visual.mesh.ray.arrangement;

import net.cyberpunk042.visual.shape.RayArrangement;

/**
 * Factory for creating ArrangementStrategy instances.
 */
public final class ArrangementFactory {
    
    private ArrangementFactory() {}
    
    /**
     * Get the appropriate strategy for an arrangement type.
     */
    public static ArrangementStrategy get(RayArrangement arrangement) {
        if (arrangement == null) {
            return RadialArrangement.INSTANCE;
        }
        
        return switch (arrangement) {
            case RADIAL -> RadialArrangement.INSTANCE;
            case SPHERICAL, DIVERGING -> SphericalArrangement.DIVERGING;
            case CONVERGING -> SphericalArrangement.CONVERGING;
            case PARALLEL -> ParallelArrangement.INSTANCE;
        };
    }
}
