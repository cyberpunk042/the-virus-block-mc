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
 * TEST: Custom RenderPipeline for fullscreen shockwave effect.
 * 
 * <p>This uses the EXPLICIT PATH (not PostEffectProcessor) so that
 * CustomUniformBinder can inject our dynamic uniforms.
 * 
 * <p>Command: /shockwavetest
 */
public final class ShockwaveTestPipeline {
    
    private static final Identifier SHADER_ID = 
        Identifier.of("the-virus-block", "core/shockwave_test");
    
    /**
     * Fullscreen shockwave pipeline with dynamic uniform support.
     */
    public static final RenderPipeline SHOCKWAVE_TEST;
    
    static {
        SHOCKWAVE_TEST = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
                .withLocation(SHADER_ID)
                
                // Custom shaders
                .withVertexShader(SHADER_ID)
                .withFragmentShader(SHADER_ID)
                
                // Samplers - we'll bind color and depth textures
                .withSampler("InSampler")
                .withSampler("DepthSampler")
                
                // Our custom uniform block - THIS is why we use explicit pipeline!
                .withUniform("ShockwaveConfig", UniformType.UNIFORM_BUFFER)
                
                // Vertex format for fullscreen quad
                .withVertexFormat(
                    VertexFormats.POSITION_TEXTURE,
                    VertexFormat.DrawMode.TRIANGLES
                )
                
                // Render state
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                
                .build()
        );
    }
    
    public static void init() {
        if (SHOCKWAVE_TEST != null) {
            net.cyberpunk042.log.Logging.RENDER.topic("shockwave_test")
                .info("ShockwaveTestPipeline registered");
        }
    }
    
    private ShockwaveTestPipeline() {}
}
