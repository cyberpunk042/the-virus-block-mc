package net.cyberpunk042.client.visual.mesh.ray.geometry;

import net.cyberpunk042.visual.shape.RayCurvature;

/**
 * Factory for creating CurvatureStrategy instances.
 * 
 * <p>Maps RayCurvature enum values to their corresponding strategy implementations.</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils#computeCurvatureAngle
 */
public final class CurvatureFactory {
    
    private CurvatureFactory() {}
    
    /**
     * Get the appropriate strategy for a curvature type.
     */
    public static CurvatureStrategy get(RayCurvature curvature) {
        if (curvature == null) {
            return NoCurvature.INSTANCE;
        }
        
        // Matches RayGeometryUtils.computeCurvatureAngle switch cases
        return switch (curvature) {
            case NONE -> NoCurvature.INSTANCE;
            case VORTEX -> VortexCurvature.INSTANCE;
            case SPIRAL_ARM -> SpiralCurvature.INSTANCE;
            case TANGENTIAL -> TangentialCurvature.INSTANCE;
            case LOGARITHMIC -> LogarithmicCurvature.INSTANCE;
            case PINWHEEL -> PinwheelCurvature.INSTANCE;
            case ORBITAL -> OrbitalCurvature.INSTANCE;
        };
    }
}
