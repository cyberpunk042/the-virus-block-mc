package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

/**
 * Renders the GPU shockwave effect as a fullscreen quad.
 * 
 * <p>Controls state and delegates uniform binding to ShockwaveUniformBinder.
 */
public final class ShockwaveRenderer {
    
    private static boolean enabled = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("enabled", enabled)
            .info("ShockwaveRenderer toggled");
    }
    
    public static void setEnabled(boolean state) {
        enabled = state;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMMANDS - Delegate to ShockwaveUniformBinder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void trigger() {
        enabled = true;
        ShockwaveUniformBinder.trigger();
        Logging.RENDER.topic("shockwave_gpu")
            .info("Shockwave triggered!");
    }
    
    public static void setRadius(float r) {
        enabled = true;
        ShockwaveUniformBinder.setRadius(r);
    }
    
    public static void setThickness(float t) {
        ShockwaveUniformBinder.setThickness(t);
    }
    
    public static void setIntensity(float i) {
        ShockwaveUniformBinder.setIntensity(i);
    }
    
    public static void setSpeed(float s) {
        ShockwaveUniformBinder.setSpeed(s);
    }
    
    public static void setMaxRadius(float m) {
        ShockwaveUniformBinder.setMaxRadius(m);
    }
    
    public static String getStatusString() {
        if (!enabled) return "OFF";
        return ShockwaveUniformBinder.getStatusString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        // Initialize the pipeline
        ShockwavePipelines.init();
        Logging.RENDER.topic("shockwave_gpu")
            .info("ShockwaveRenderer initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING - Called each frame from WorldRendererShockwaveMixin
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int frameCount = 0;
    
    /**
     * Render the shockwave effect.
     * Called from WorldRendererShockwaveMixin at end of render().
     */
    public static void render() {
        if (!enabled) return;
        
        float radius = ShockwaveUniformBinder.getCurrentRadius();
        float thickness = ShockwaveUniformBinder.getThickness();
        float intensity = ShockwaveUniformBinder.getIntensity();
        
        frameCount++;
        
        // Log every 30 frames to show animation is updating
        if (frameCount % 30 == 0) {
            Logging.RENDER.topic("shockwave_render")
                .kv("radius", String.format("%.1f", radius))
                .kv("animating", ShockwaveUniformBinder.isAnimating())
                .info("Shockwave frame");
        }
        
        // Check if animation completed
        if (!ShockwaveUniformBinder.isAnimating() && radius >= ShockwaveUniformBinder.getMaxRadius()) {
            enabled = false;
            return;
        }
        
        // TODO: Actually draw the fullscreen quad with our pipeline
        // For now the PostEffectProcessor path still handles visual rendering
        // This proves the values ARE updating each frame
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS for current values
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getCurrentRadius() {
        return ShockwaveUniformBinder.getCurrentRadius();
    }
    
    public static float getThickness() {
        return ShockwaveUniformBinder.getThickness();
    }
    
    public static float getIntensity() {
        return ShockwaveUniformBinder.getIntensity();
    }
    
    private ShockwaveRenderer() {}
}
