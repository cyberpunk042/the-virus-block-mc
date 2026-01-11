package net.cyberpunk042.client.visual.ubo;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;
import net.cyberpunk042.client.gui.config.RenderConfig;

/**
 * UBO for HDR pipeline dynamic parameters.
 * 
 * <p>Injected by PostEffectPassMixin for gaussian_blur passes.
 * Provides dynamic BlurRadius from UI slider.</p>
 * 
 * <h3>GLSL Layout (std140):</h3>
 * <pre>
 * layout(std140) uniform HdrConfig {
 *     float BlurRadius;     // offset 0
 *     float GlowIntensity;  // offset 4
 *     float HdrPad1;        // offset 8
 *     float HdrPad2;        // offset 12
 * };
 * </pre>
 * 
 * @see net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter
 * @see net.cyberpunk042.client.gui.config.RenderConfig#getBlurRadius()
 */
@UBOStruct(name = "HdrConfig", glslPath = "the-virus-block:shaders/post/hdr/gaussian_blur.fsh")
public record HdrConfigUBO(
    @Vec4 HdrParamsVec4 hdrParams
) {
    /**
     * Buffer size in bytes (1 vec4 = 16 bytes).
     */
    public static final int BUFFER_SIZE = ReflectiveUBOWriter.calculateBufferSize(HdrConfigUBO.class);
    
    /**
     * UBO name used in JSON and shader.
     * Must match exactly: JSON "uniforms" key and GLSL "uniform" block name.
     */
    public static final String UBO_NAME = "HdrConfig";
    
    /**
     * HDR parameters packed as vec4.
     * 
     * <p>Uses modern ReflectiveUBOWriter pattern: plain 4-float record
     * without Vec4Serializable boilerplate.</p>
     * 
     * @param blurRadius    Blur spread multiplier (from RenderConfig, range 0.001-20.0)
     * @param glowIntensity Reserved for future glow intensity control
     * @param pad1          Padding for std140 alignment
     * @param pad2          Padding for std140 alignment
     */
    public record HdrParamsVec4(
        float blurRadius, 
        float glowIntensity, 
        float pad1, 
        float pad2
    ) {}
    
    /**
     * Create UBO from current RenderConfig values.
     * 
     * <p>Called by PostEffectPassMixin every frame for gaussian_blur passes.</p>
     * 
     * @return HdrConfigUBO with current BlurRadius from UI slider
     */
    public static HdrConfigUBO fromConfig() {
        float blurRadius = RenderConfig.get().getBlurRadius();
        
        return new HdrConfigUBO(
            new HdrParamsVec4(
                blurRadius,   // BlurRadius from slider
                1.0f,         // GlowIntensity (reserved)
                0.0f,         // Padding
                0.0f          // Padding
            )
        );
    }
    
    /**
     * Create UBO with explicit values (for testing).
     * 
     * @param blurRadius explicit blur radius value
     * @return HdrConfigUBO with specified BlurRadius
     */
    public static HdrConfigUBO withBlurRadius(float blurRadius) {
        return new HdrConfigUBO(
            new HdrParamsVec4(blurRadius, 1.0f, 0.0f, 0.0f)
        );
    }
}
