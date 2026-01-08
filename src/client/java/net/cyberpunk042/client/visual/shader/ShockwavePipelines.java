package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Custom render pipeline for GPU-driven terrain-conforming shockwave.
 * 
 * <p>This pipeline:
 * <ul>
 *   <li>Reads from depth texture via sampler</li>
 *   <li>Has a ShockwaveParams UBO for dynamic control from Java</li>
 *   <li>Renders a fullscreen quad with the shockwave effect</li>
 * </ul>
 * 
 * <p>The uniform values are bound each frame by {@link ShockwaveUniformBinder}
 * through the same mixin that handles FresnelParams.
 */
public final class ShockwavePipelines {
    
    private static final Identifier SHADER_ID = 
        Identifier.of("the-virus-block", "post/shockwave_fullscreen");
    
    /**
     * Fullscreen shockwave pipeline.
     * 
     * <p>Custom uniforms:
     * <ul>
     *   <li>ShockwaveParams - Contains RingRadius, RingThickness, Intensity, Time</li>
     * </ul>
     */
    public static final RenderPipeline SHOCKWAVE_FULLSCREEN;
    
    static {
        SHOCKWAVE_FULLSCREEN = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
                .withLocation(SHADER_ID)
                
                // Custom shaders for shockwave effect
                .withVertexShader(SHADER_ID)
                .withFragmentShader(SHADER_ID)
                
                // Samplers
                .withSampler("InSampler")      // Main color texture
                .withSampler("DepthSampler")   // Depth texture
                
                // Custom UBO for shockwave parameters (bound by ShockwaveUniformBinder)
                .withUniform("ShockwaveParams", UniformType.UNIFORM_BUFFER)
                
                // Vertex format: simple position + texture coords for fullscreen quad
                .withVertexFormat(
                    VertexFormats.POSITION_TEXTURE,
                    VertexFormat.DrawMode.TRIANGLES
                )
                
                // Render state: no depth test, no cull, blend over existing
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                
                .build()
        );
    }
    
    /**
     * Initialize pipelines. Call from client init.
     */
    public static void init() {
        // Force static initializer to run
        if (SHOCKWAVE_FULLSCREEN != null) {
            net.cyberpunk042.log.Logging.RENDER.topic("shockwave_pipeline")
                .info("ShockwavePipelines initialized");
        }
    }
    
    private ShockwavePipelines() {}
}
