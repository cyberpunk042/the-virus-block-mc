package net.cyberpunk042.client.visual.mesh.ray.layer;

import net.cyberpunk042.visual.shape.RaysShape;

/**
 * Shell layer mode - concentric shells at increasing radii.
 * 
 * <p>Based on RayPositioner.computeRadial lines 1066-1069:</p>
 * <pre>
 * layerRadiusOffset = layerIndex * layerSpacing;
 * </pre>
 * 
 * <p>Similar to RADIAL but uses layerSpacing instead of rayLength.
 * This creates concentric shells where rays at each shell have the same length
 * but start at different radii.</p>
 * 
 * <p>RADIAL vs SHELL:</p>
 * <ul>
 *   <li>RADIAL: Layers extend continuously (layer 1 starts where layer 0 ends)</li>
 *   <li>SHELL: Layers are at fixed radii separated by layerSpacing (rays overlap)</li>
 * </ul>
 * 
 * @see LayerModeStrategy
 */
public final class ShellLayerMode implements LayerModeStrategy {
    
    public static final ShellLayerMode INSTANCE = new ShellLayerMode();
    
    private ShellLayerMode() {}
    
    @Override
    public LayerOffset computeOffset(RaysShape shape, int layerIndex, float layerSpacing) {
        // Concentric shells at fixed radii intervals
        float radiusOffset = layerIndex * layerSpacing;
        
        return LayerOffset.radial(radiusOffset);
    }
    
    @Override
    public String name() {
        return "SHELL";
    }
}
