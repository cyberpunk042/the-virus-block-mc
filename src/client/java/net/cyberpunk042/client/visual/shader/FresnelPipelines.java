package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;

/**
 * Custom render pipelines for field visual effects.
 * 
 * <h2>Fresnel Rim Effect</h2>
 * <p>The Fresnel pipeline adds a configurable rim/edge glow effect to geometry.
 * This works by calculating how much each surface faces away from the camera,
 * and adding a colored glow at grazing angles (edges).</p>
 * 
 * <h2>Uniforms</h2>
 * <p>Custom uniforms (RimColor, RimPower, RimIntensity) are declared in the GLSL
 * shaders and handled by Minecraft's shader loader. Default values can be 
 * overridden at render time via {@link FresnelUniformManager}.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Initialize on client startup
 * FresnelPipelines.init();
 * 
 * // Use in rendering
 * RenderLayer layer = FresnelRenderLayers.fresnelTranslucent();
 * </pre>
 * 
 * @see FresnelRenderLayers
 * @see FresnelUniformManager
 */
public final class FresnelPipelines {
    
    /**
     * Fresnel entity translucent pipeline.
     * 
     * <p>Custom uniforms are declared in the GLSL shaders:</p>
     * <ul>
     *   <li>{@code RimColor} - RGB color of the rim glow (vec3)</li>
     *   <li>{@code RimPower} - Edge sharpness (float, 1.0-10.0)</li>
     *   <li>{@code RimIntensity} - Brightness multiplier (float)</li>
     * </ul>
     */
    public static final RenderPipeline FRESNEL_ENTITY_TRANSLUCENT;
    
    private static final net.minecraft.util.Identifier SHADER_ID = 
        net.minecraft.util.Identifier.of("the-virus-block", "core/fresnel_entity");
    
    static {
        FRESNEL_ENTITY_TRANSLUCENT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                // Pipeline location (for debugging/identification)
                .withLocation(SHADER_ID)
                
                // Custom shaders with Fresnel effect
                .withVertexShader(SHADER_ID)
                .withFragmentShader(SHADER_ID)
                
                // Samplers (inherited from ENTITY_SNIPPET, but declare explicitly)
                .withSampler("Sampler0")  // Main texture
                .withSampler("Sampler1")  // Overlay
                .withSampler("Sampler2")  // Lightmap
                
                // Custom UBO for Fresnel parameters (bound by CustomUniformBinder via Mixin)
                .withUniform("FresnelParams", net.minecraft.client.gl.UniformType.UNIFORM_BUFFER)
                
                // Vertex format: same as entity translucent
                .withVertexFormat(
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                    VertexFormat.DrawMode.TRIANGLES
                )
                
                // Render state
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withCull(true)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(true)
                .withColorWrite(true, true) // RGB and Alpha
                
                .build()
        );
    }
    
    /**
     * Initializes the Fresnel pipelines.
     * 
     * <p>Call this during client initialization to ensure the pipeline
     * is registered before any rendering occurs.</p>
     * 
     * <pre>
     * public void onInitializeClient() {
     *     FresnelPipelines.init();
     * }
     * </pre>
     */
    public static void init() {
        // Static initializer does the work.
        // This method exists to provide a clear initialization point.
    }
    
    private FresnelPipelines() {
        // Utility class
    }
}
