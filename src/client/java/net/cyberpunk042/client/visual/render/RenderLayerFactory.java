package net.cyberpunk042.client.visual.render;

import net.minecraft.client.render.RenderLayer;

/**
 * Factory for creating render layers used by the field system.
 */
public final class RenderLayerFactory {
    
    private RenderLayerFactory() {}
    
    /**
     * Creates a solid render layer for opaque geometry.
     */
    public static RenderLayer solid() {
        return FieldRenderLayers.solidTranslucent();
    }
    
    /**
     * Creates a translucent render layer.
     */
    public static RenderLayer translucent() {
        return FieldRenderLayers.solidTranslucent();
    }
    
    /**
     * Creates a lines render layer for wireframe.
     */
    public static RenderLayer lines() {
        return FieldRenderLayers.lines();
    }
    
    /**
     * Creates a glow render layer.
     */
    public static RenderLayer glow() {
        return FieldRenderLayers.glowTranslucent();
    }
}
