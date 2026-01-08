package net.cyberpunk042.client.visual.mesh.ray.layer;

import net.cyberpunk042.visual.shape.RaysShape;

/**
 * Strategy for computing layer offsets.
 * 
 * <p>Different layer modes produce different offset patterns:</p>
 * <ul>
 *   <li><b>VERTICAL:</b> Layers stacked along Y axis</li>
 *   <li><b>RADIAL:</b> Layers extend further outward (each starts where previous ends)</li>
 *   <li><b>SHELL:</b> Concentric shells at increasing radii</li>
 *   <li><b>SPIRAL:</b> Both angular and radial offset (spiral pattern)</li>
 * </ul>
 * 
 * <p>Based on RayPositioner.computeRadial lines 1057-1075.</p>
 * 
 * @see RayPositioner
 * @see LayerOffset
 * @see LayerModeFactory
 */
public interface LayerModeStrategy {
    
    /**
     * Compute the offset for a layer.
     * 
     * @param shape The rays shape configuration
     * @param layerIndex Index of the layer (0 to layers-1)
     * @param layerSpacing Spacing between layers
     * @return LayerOffset with yOffset, radiusOffset, and angleOffset
     */
    LayerOffset computeOffset(RaysShape shape, int layerIndex, float layerSpacing);
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
