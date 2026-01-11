package net.cyberpunk042.client.gui.config;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

/**
 * Render quality configuration including HDR settings.
 * 
 * <h3>Key Feature: Runtime HDR Toggle</h3>
 * <p>Users can toggle HDR on/off in-game without shader reload.
 * After changing {@link #setHdrEnabled(boolean)}, call 
 * {@code PostFxPipeline.getInstance().invalidateTargets()} to recreate
 * framebuffers with the new format.
 * 
 * <h3>Usage:</h3>
 * <pre>
 * // Toggle HDR
 * RenderConfig config = RenderConfig.get();
 * config.setHdrEnabled(!config.isHdrEnabled());
 * PostFxPipeline.getInstance().invalidateTargets();
 * </pre>
 * 
 * <h3>Persistence:</h3>
 * <p>Integrates with GuiConfigPersistence for save/load. Call
 * {@link #loadFromJson(JsonObject)} and {@link #saveToJson(JsonObject)}.
 */
public class RenderConfig {
    
    private static final RenderConfig INSTANCE = new RenderConfig();
    
    // =========================================================================
    // HDR SETTINGS
    // =========================================================================
    
    /** Master HDR toggle - when false, all HDR targets use RGBA8 */
    private boolean hdrEnabled = true;
    
    /** Blur quality: 1.0 = full res, 0.5 = half res (faster), 0.25 = quarter res */
    private float blurQuality = 1.0f;
    
    /** Number of blur iterations: 1 = 2 passes (H+V), 2 = 4 passes, etc. */
    private int blurIterations = 2;
    
    // =========================================================================
    // CONSTRUCTOR & SINGLETON
    // =========================================================================
    
    private RenderConfig() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance.
     */
    public static RenderConfig get() {
        return INSTANCE;
    }
    
    // =========================================================================
    // HDR GETTERS
    // =========================================================================
    
    /**
     * Check if HDR targets are enabled.
     * 
     * @return true if HDR (RGBA16F) targets should be used
     */
    public boolean isHdrEnabled() {
        return hdrEnabled;
    }
    
    /**
     * Get the blur quality factor.
     * 
     * @return 0.25 to 1.0, where 1.0 is full resolution
     */
    public float getBlurQuality() {
        return blurQuality;
    }
    
    /**
     * Get the number of blur iterations.
     * 
     * @return 1-8, where each iteration = 2 passes (horizontal + vertical)
     */
    public int getBlurIterations() {
        return blurIterations;
    }
    
    // =========================================================================
    // HDR SETTERS
    // =========================================================================
    
    /**
     * Enable or disable HDR targets.
     * 
     * <p>After calling this, you should call 
     * {@code PostFxPipeline.getInstance().invalidateTargets()}
     * to recreate framebuffers with the new format.
     * 
     * @param enabled true for RGBA16F, false for RGBA8
     */
    public void setHdrEnabled(boolean enabled) {
        if (this.hdrEnabled != enabled) {
            this.hdrEnabled = enabled;
            Logging.RENDER.topic("render_config")
                .kv("hdr", enabled)
                .info("HDR targets {}", enabled ? "enabled" : "disabled");
        }
    }
    
    /**
     * Set the blur quality factor.
     * 
     * <p>Lower values = faster blur but less detail.
     * Recommended: 1.0 for high quality, 0.5 for performance.
     * 
     * @param quality 0.25 to 1.0
     */
    public void setBlurQuality(float quality) {
        this.blurQuality = Math.max(0.25f, Math.min(1.0f, quality));
    }
    
    /**
     * Set the number of blur iterations.
     * 
     * <p>Higher values = smoother blur but more expensive.
     * Recommended: 2 for balanced, 4 for maximum smoothness.
     * 
     * @param iterations 1 to 8
     */
    public void setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(8, iterations));
    }
    
    // =========================================================================
    // PERSISTENCE
    // =========================================================================
    
    /**
     * Load settings from JSON.
     * 
     * <p>Call from GuiConfigPersistence during config load.
     * 
     * @param json The JSON object containing render settings
     */
    public void loadFromJson(JsonObject json) {
        if (json == null) return;
        
        if (json.has("hdr_enabled")) {
            hdrEnabled = json.get("hdr_enabled").getAsBoolean();
        }
        if (json.has("blur_quality")) {
            setBlurQuality(json.get("blur_quality").getAsFloat());
        }
        if (json.has("blur_iterations")) {
            setBlurIterations(json.get("blur_iterations").getAsInt());
        }
        
        Logging.RENDER.topic("render_config")
            .kv("hdr", hdrEnabled)
            .kv("blurQuality", blurQuality)
            .kv("blurIterations", blurIterations)
            .debug("Loaded render config from JSON");
    }
    
    /**
     * Save settings to JSON.
     * 
     * <p>Call from GuiConfigPersistence during config save.
     * 
     * @param json The JSON object to write render settings to
     */
    public void saveToJson(JsonObject json) {
        if (json == null) return;
        
        json.addProperty("hdr_enabled", hdrEnabled);
        json.addProperty("blur_quality", blurQuality);
        json.addProperty("blur_iterations", blurIterations);
    }
    
    /**
     * Reset to default values.
     */
    public void resetToDefaults() {
        hdrEnabled = true;
        blurQuality = 1.0f;
        blurIterations = 2;
        
        Logging.RENDER.topic("render_config")
            .debug("Reset to defaults");
    }
}
