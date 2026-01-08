package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.render.RenderLayer;

/**
 * Custom RenderLayers using the Fresnel shader pipeline.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Before rendering, set Fresnel parameters
 * FresnelUniformManager.setParams(1f, 1f, 1f, 3f, 1.5f);
 * 
 * // Get the render layer
 * RenderLayer layer = FresnelRenderLayers.fresnelTranslucent();
 * VertexConsumer consumer = vertexConsumers.getBuffer(layer);
 * 
 * // Render geometry...
 * </pre>
 * 
 * @see FresnelPipelines
 * @see FresnelUniformManager
 */
public final class FresnelRenderLayers {
    
    // White texture so vertex colors work properly (texture * vertexColor = vertexColor)
    private static final net.minecraft.util.Identifier WHITE_TEXTURE = 
        net.minecraft.util.Identifier.of("minecraft", "textures/misc/white.png");
    
    // Cached layer instance (immutable, can be shared)
    private static RenderLayer FRESNEL_TRANSLUCENT;
    
    /**
     * Returns a translucent render layer with Fresnel rim effect.
     * 
     * <p>This layer uses the {@link FresnelPipelines#FRESNEL_ENTITY_TRANSLUCENT}
     * pipeline which includes custom uniforms for rim color, power, and intensity.</p>
     * 
     * <p><b>Important:</b> Call {@link FresnelUniformManager#setParams} before
     * rendering to configure the effect.</p>
     * 
     * @return RenderLayer configured for Fresnel rendering
     */
    public static RenderLayer fresnelTranslucent() {
        if (FRESNEL_TRANSLUCENT == null) {
            FRESNEL_TRANSLUCENT = RenderLayer.of(
                "fresnel_entity_translucent",
                256, // Buffer size
                FresnelPipelines.FRESNEL_ENTITY_TRANSLUCENT,
                RenderLayer.MultiPhaseParameters.builder()
                    .build(false) // No outline pass
            );
        }
        return FRESNEL_TRANSLUCENT;
    }
    
    /**
     * Returns a translucent render layer with Fresnel rim effect (no backface culling).
     * 
     * <p>Use this for double-sided geometry where both front and back faces
     * should be visible.</p>
     */
    public static RenderLayer fresnelTranslucentNoCull() {
        // For double-sided, we'd need a separate pipeline without cull
        // For now, return the standard layer
        // TODO: Create FRESNEL_ENTITY_TRANSLUCENT_NO_CULL pipeline
        return fresnelTranslucent();
    }
    
    private FresnelRenderLayers() {
        // Utility class
    }
}
