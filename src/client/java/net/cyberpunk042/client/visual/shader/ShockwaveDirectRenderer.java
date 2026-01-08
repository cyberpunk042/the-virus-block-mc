package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;

/**
 * Direct GPU shockwave renderer - manages state and parameters.
 * 
 * <p>This is a simpler approach that delegates actual rendering to
 * the existing WorldRendererShockwaveMixin + PostEffectProcessor path,
 * but provides dynamic uniform control via ShockwaveUniformBinder.
 */
public class ShockwaveDirectRenderer {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static boolean enabled = false;
    private static boolean animating = false;
    private static long animationStartTime = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURABLE PARAMETERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float currentRadius = 20.0f;
    private static float ringThickness = 4.0f;
    private static float intensity = 1.2f;
    private static float animationSpeed = 20.0f;
    private static float maxRadius = 150.0f;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        Logging.RENDER.topic("shockwave_direct")
            .info("ShockwaveDirectRenderer initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENABLE/DISABLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_direct")
            .kv("enabled", enabled)
            .info("Toggled");
    }
    
    public static void setEnabled(boolean state) {
        enabled = state;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void trigger() {
        enabled = true;
        animating = true;
        currentRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_direct")
            .kv("speed", animationSpeed)
            .kv("maxRadius", maxRadius)
            .info("Triggered!");
    }
    
    public static boolean isAnimating() {
        return animating;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setRadius(float radius) {
        enabled = true;
        animating = false;
        currentRadius = Math.max(0.0f, radius);
    }
    
    public static void setThickness(float thickness) {
        ringThickness = Math.max(0.5f, Math.min(50.0f, thickness));
    }
    
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(3.0f, value));
    }
    
    public static void setSpeed(float speed) {
        animationSpeed = Math.max(1.0f, Math.min(200.0f, speed));
    }
    
    public static void setMaxRadius(float max) {
        maxRadius = Math.max(10.0f, Math.min(500.0f, max));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            float animRadius = elapsed * animationSpeed;
            
            if (animRadius >= maxRadius) {
                animating = false;
                enabled = false;
                return maxRadius;
            }
            return animRadius;
        }
        return currentRadius;
    }
    
    public static float getThickness() { return ringThickness; }
    public static float getIntensity() { return intensity; }
    public static float getSpeed() { return animationSpeed; }
    public static float getMaxRadius() { return maxRadius; }
    
    public static String getStatusString() {
        if (!enabled) return "OFF";
        if (animating) {
            return String.format("ANIM r=%.1f spd=%.1f max=%.1f", 
                getCurrentRadius(), animationSpeed, maxRadius);
        }
        return String.format("STATIC r=%.1f t=%.1f i=%.1f", 
            currentRadius, ringThickness, intensity);
    }
}
