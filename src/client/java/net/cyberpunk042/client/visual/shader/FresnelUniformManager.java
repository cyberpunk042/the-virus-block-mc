package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.visual.effect.HorizonEffect;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/**
 * Manages Fresnel shader uniforms at render time.
 * 
 * <h2>Uniform Buffer Layout (Std140)</h2>
 * <p>The Fresnel uniforms are packed into a single GPU buffer following
 * GLSL std140 layout rules:</p>
 * <pre>
 * layout(std140) uniform FresnelParams {
 *     vec3 RimColor;      // offset 0,  size 12, align 16 -> 16 bytes
 *     float RimPower;     // offset 16, size 4,  align 4  -> 4 bytes  
 *     float RimIntensity; // offset 20, size 4,  align 4  -> 4 bytes
 * };                      // Total: 24 bytes (padded to 32)
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Set parameters before rendering
 * FresnelUniformManager.setParams(1f, 0.5f, 0f, 3f, 1.5f); // Orange rim
 * 
 * // Get or create the GPU buffer and bind it
 * try (GpuBuffer buffer = FresnelUniformManager.createUniformBuffer()) {
 *     renderPass.setUniform("FresnelParams", buffer);
 *     // render...
 * }
 * </pre>
 * 
 * @see FresnelEffect
 * @see FresnelPipelines
 */
public final class FresnelUniformManager {
    
    // Current uniform values (render thread only)
    private static final Vector3f rimColor = new Vector3f(1f, 1f, 1f);
    private static float rimPower = 3f;
    private static float rimIntensity = 1.5f;
    
    // Cached buffer (recreated each frame)
    private static GpuBuffer cachedBuffer = null;
    private static boolean dirty = true;
    
    // Buffer size for std140 layout
    private static final int BUFFER_SIZE;
    static {
        Std140SizeCalculator calc = new Std140SizeCalculator();
        calc.putVec3();    // RimColor
        calc.putFloat();   // RimPower
        calc.putFloat();   // RimIntensity
        BUFFER_SIZE = calc.get();
    }
    
    /**
     * Sets Fresnel parameters from individual values.
     * 
     * @param r Red component of rim color (0-1)
     * @param g Green component of rim color (0-1)
     * @param b Blue component of rim color (0-1)
     * @param power Edge sharpness (1.0 = soft diffuse, 10.0 = sharp edge)
     * @param intensity Brightness multiplier (0 = off, 1 = normal, 5 = very bright)
     */
    public static void setParams(float r, float g, float b, float power, float intensity) {
        rimColor.set(r, g, b);
        rimPower = power;
        rimIntensity = intensity;
        dirty = true;
    }
    
    /**
     * Sets Horizon parameters from a HorizonEffect record.
     * 
     * @param effect The Horizon effect configuration
     */
    public static void setParams(HorizonEffect effect) {
        if (effect == null || !effect.enabled()) {
            // Disable rim by setting intensity to 0
            setParams(1f, 1f, 1f, 3f, 0f);
        } else {
            setParams(
                effect.red(),
                effect.green(),
                effect.blue(),
                effect.power(),
                effect.intensity()
            );
        }
    }
    
    /**
     * Creates a GPU buffer containing the current Fresnel uniforms.
     * 
     * <p>The returned buffer follows std140 layout matching GLSL:
     * <pre>
     * layout(std140) uniform FresnelParams {
     *     vec4 RimColorAndPower;      // xyz = RimColor, w = RimPower
     *     vec4 RimIntensityAndPad;    // x = RimIntensity, yzw = padding
     * };
     * </pre></p>
     * 
     * <p><b>Note:</b> The caller is responsible for closing the buffer.</p>
     * 
     * @return GpuBuffer with current Fresnel parameters
     */
    public static GpuBuffer createUniformBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // std140 layout: 2 x vec4 = 32 bytes
            Std140Builder builder = Std140Builder.onStack(stack, 32);
            
            // vec4 RimColorAndPower (xyz = color, w = power)
            builder.putVec4(rimColor.x, rimColor.y, rimColor.z, rimPower);
            
            // vec4 RimIntensityAndPad (x = intensity, yzw = padding)
            builder.putVec4(rimIntensity, 0f, 0f, 0f);
            
            // Create GPU buffer
            return RenderSystem.getDevice().createBuffer(
                () -> "FresnelParams uniform buffer",
                128, // Usage flags (uniform buffer)
                builder.get()
            );
        }
    }
    
    /**
     * Binds the Fresnel uniforms to a RenderPass.
     * 
     * <p>This is the main method to call before rendering with Fresnel shaders.</p>
     * 
     * @param renderPass The active render pass
     * @param uniformName The name of the uniform block in GLSL (e.g., "FresnelParams")
     * @return The created GpuBuffer (caller should close after rendering)
     */
    public static GpuBuffer bindUniforms(RenderPass renderPass, String uniformName) {
        GpuBuffer buffer = createUniformBuffer();
        renderPass.setUniform(uniformName, buffer);
        return buffer;
    }
    
    /**
     * Returns the current rim color.
     */
    public static Vector3f getRimColor() {
        return new Vector3f(rimColor);
    }
    
    /**
     * Returns the current rim power (edge sharpness).
     */
    public static float getRimPower() {
        return rimPower;
    }
    
    /**
     * Returns the current rim intensity.
     */
    public static float getRimIntensity() {
        return rimIntensity;
    }
    
    /**
     * Checks if Fresnel effect is currently enabled (intensity > 0).
     */
    public static boolean isEnabled() {
        return rimIntensity > 0.001f;
    }
    
    private FresnelUniformManager() {
        // Utility class
    }
}
