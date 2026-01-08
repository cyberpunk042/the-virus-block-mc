package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.log.Logging;
import org.lwjgl.system.MemoryStack;

/**
 * Binds ShockwaveParams uniform buffer to the RenderPass.
 * 
 * <p>Uniform Layout (std140, 32 bytes):
 * <pre>
 * ShockwaveParams:
 *   vec4 RadiusThicknessIntensityTime;  // x=Radius, y=Thickness, z=Intensity, w=Time
 *   vec4 ColorCore;                      // xyz=CoreColor, w=padding
 * </pre>
 */
public final class ShockwaveUniformBinder {
    
    private static final int BUFFER_SIZE = 32;  // 2 x vec4 = 32 bytes
    
    // Current parameters - updated each frame
    private static float radius = 20.0f;
    private static float thickness = 4.0f;
    private static float intensity = 1.2f;
    private static float time = 0.0f;
    
    // Animation state
    private static boolean animating = false;
    private static long animationStartTime = 0;
    private static float animationSpeed = 20.0f;
    private static float maxRadius = 150.0f;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setRadius(float r) {
        radius = Math.max(0.0f, r);
        animating = false;
    }
    
    public static void setThickness(float t) {
        thickness = Math.max(0.5f, Math.min(50.0f, t));
    }
    
    public static void setIntensity(float i) {
        intensity = Math.max(0.0f, Math.min(3.0f, i));
    }
    
    public static void setSpeed(float s) {
        animationSpeed = Math.max(1.0f, Math.min(200.0f, s));
    }
    
    public static void setMaxRadius(float m) {
        maxRadius = Math.max(10.0f, Math.min(500.0f, m));
    }
    
    public static void trigger() {
        animating = true;
        animationStartTime = System.currentTimeMillis();
        radius = 0.0f;
    }
    
    public static float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            float r = elapsed * animationSpeed;
            if (r >= maxRadius) {
                animating = false;
                return maxRadius;
            }
            return r;
        }
        return radius;
    }
    
    public static boolean isAnimating() {
        return animating;
    }
    
    public static String getStatusString() {
        if (animating) {
            return String.format("ANIM r=%.1f spd=%.1f", getCurrentRadius(), animationSpeed);
        }
        return String.format("r=%.1f t=%.1f i=%.1f", radius, thickness, intensity);
    }
    
    public static float getThickness() {
        return thickness;
    }
    
    public static float getIntensity() {
        return intensity;
    }
    
    public static float getMaxRadius() {
        return maxRadius;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UNIFORM BINDING - Called from CustomUniformBinder or RenderSystemMixin
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Cached GPU buffer to prevent leaks
    private static GpuBuffer cachedBuffer = null;
    
    /**
     * Binds ShockwaveParams uniform buffer to the render pass.
     */
    public static void bindUniforms(RenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, BUFFER_SIZE);
            
            // Update time
            time = (System.currentTimeMillis() % 10000) / 1000.0f;
            
            // Get current animated radius
            float currentRadius = getCurrentRadius();
            
            // vec4 RadiusThicknessIntensityTime
            builder.putVec4(currentRadius, thickness, intensity, time);
            
            // vec4 ColorCore (white core) - could be configurable later
            builder.putVec4(1.0f, 1.0f, 1.0f, 0.0f);
            
            // Close old buffer to prevent leak
            if (cachedBuffer != null && !cachedBuffer.isClosed()) {
                cachedBuffer.close();
            }
            
            // Upload and bind
            cachedBuffer = RenderSystem.getDevice().createBuffer(
                () -> "ShockwaveParams UBO",
                16, // usage: uniform buffer
                builder.get()
            );
            renderPass.setUniform("ShockwaveParams", cachedBuffer);
            
        } catch (Exception e) {
            Logging.RENDER.topic("shockwave_bind")
                .kv("error", e.getMessage())
                .warn("Failed to bind ShockwaveParams");
        }
    }
    
    private ShockwaveUniformBinder() {}
}
