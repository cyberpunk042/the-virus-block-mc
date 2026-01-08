package net.cyberpunk042.client.visual.mesh.ray.layer;

import net.cyberpunk042.visual.shape.RayLayerMode;

/**
 * Factory for creating LayerModeStrategy instances.
 * 
 * <p>Maps {@link RayLayerMode} enum values to their strategy implementations:</p>
 * <ul>
 *   <li>{@link RayLayerMode#VERTICAL} → {@link VerticalLayerMode}</li>
 *   <li>{@link RayLayerMode#RADIAL} → {@link RadialLayerMode}</li>
 *   <li>{@link RayLayerMode#SHELL} → {@link ShellLayerMode}</li>
 *   <li>{@link RayLayerMode#SPIRAL} → {@link SpiralLayerMode}</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayPositioner#computeRadial
 */
public final class LayerModeFactory {
    
    private LayerModeFactory() {}
    
    /**
     * Get the appropriate strategy for a layer mode.
     * 
     * @param layerMode The layer mode type
     * @return Layer mode strategy (never null - defaults to VERTICAL)
     */
    public static LayerModeStrategy get(RayLayerMode layerMode) {
        if (layerMode == null) {
            return VerticalLayerMode.INSTANCE;
        }
        
        return switch (layerMode) {
            case VERTICAL -> VerticalLayerMode.INSTANCE;
            case RADIAL -> RadialLayerMode.INSTANCE;
            case SHELL -> ShellLayerMode.INSTANCE;
            case SPIRAL -> SpiralLayerMode.INSTANCE;
        };
    }
}
