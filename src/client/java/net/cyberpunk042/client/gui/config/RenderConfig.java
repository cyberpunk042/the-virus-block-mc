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
    // GOD RAYS SETTINGS
    // =========================================================================
    
    /** God rays toggle - volumetric light shafts emanating from orb */
    private boolean godRaysEnabled = false;
    
    /** God rays decay factor - controls RANGE (0.95 short, 0.985 long) */
    private float godRaysDecay = 0.97f;
    
    /** God rays exposure - controls STRENGTH (0.01 subtle, 0.05 strong) */
    private float godRaysExposure = 0.02f;
    
    /** God rays threshold - minimum brightness to create rays (0.0-1.0, lower = more rays) */
    private float godRaysThreshold = 0.5f;
    
    /** God rays sky toggle - whether sky also creates atmospheric rays */
    private boolean godRaysSkyEnabled = false;
    
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
    // GOD RAYS GETTERS
    // =========================================================================
    
    /**
     * Check if god rays (volumetric light shafts) are enabled.
     * 
     * @return true if god rays should be rendered
     */
    public boolean isGodRaysEnabled() {
        return godRaysEnabled;
    }
    
    /**
     * Get the god rays decay factor.
     * This controls RANGE - higher values = longer rays.
     * 
     * @return 0.94 to 0.99, default 0.97
     */
    public float getGodRaysDecay() {
        return godRaysDecay;
    }
    
    /**
     * Get the god rays exposure.
     * This controls STRENGTH - higher values = brighter rays.
     * 
     * @return 0.005 to 0.1, default 0.02
     */
    public float getGodRaysExposure() {
        return godRaysExposure;
    }
    
    /**
     * Get the god rays brightness threshold.
     * This controls what creates rays - lower = more rays.
     * 
     * @return 0.0 to 1.0, default 0.5
     */
    public float getGodRaysThreshold() {
        return godRaysThreshold;
    }
    
    /**
     * Check if sky rays are enabled (atmospheric effect).
     * 
     * @return true if sky should also create rays
     */
    public boolean isGodRaysSkyEnabled() {
        return godRaysSkyEnabled;
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
    // GOD RAYS SETTERS
    // =========================================================================
    
    /**
     * Enable or disable god rays.
     * 
     * <p>God rays require HDR mode to be enabled.
     * If HDR is disabled, god rays will be automatically disabled.
     * 
     * @param enabled true to enable volumetric light shafts
     */
    public void setGodRaysEnabled(boolean enabled) {
        if (this.godRaysEnabled != enabled) {
            this.godRaysEnabled = enabled && hdrEnabled; // Requires HDR
            Logging.RENDER.topic("render_config")
                .kv("godRays", this.godRaysEnabled)
                .info("God rays {}", this.godRaysEnabled ? "enabled" : "disabled");
        }
    }
    
    /**
     * Set the god rays decay factor.
     * 
     * <p>This controls RANGE - do NOT use to fix blowout.
     * Range: 0.94 (very short) to 0.99 (very long).
     * Default: 0.97 (medium range).
     * 
     * @param decay 0.94 to 0.99
     */
    public void setGodRaysDecay(float decay) {
        this.godRaysDecay = Math.max(0.94f, Math.min(0.99f, decay));
    }
    
    /**
     * Set the god rays exposure.
     * 
     * <p>This controls STRENGTH - do NOT use to fix range.
     * Range: 0.005 (subtle) to 0.1 (strong).
     * Default: 0.02 (balanced).
     * 
     * @param exposure 0.005 to 0.1
     */
    public void setGodRaysExposure(float exposure) {
        this.godRaysExposure = Math.max(0.005f, Math.min(0.1f, exposure));
    }
    
    /**
     * Set the god rays brightness threshold.
     * Controls what brightness level creates rays.
     * Full range: 0.0 (everything) to 1.0 (only brightest).
     * 
     * @param threshold 0.0 to 1.0
     */
    public void setGodRaysThreshold(float threshold) {
        this.godRaysThreshold = Math.max(0f, Math.min(1f, threshold));
    }
    
    /**
     * Enable or disable atmospheric sky rays.
     * 
     * @param enabled true for sky rays, false for orb-only
     */
    public void setGodRaysSkyEnabled(boolean enabled) {
        this.godRaysSkyEnabled = enabled;
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
        if (json.has("god_rays_enabled")) {
            godRaysEnabled = json.get("god_rays_enabled").getAsBoolean();
        }
        if (json.has("god_rays_decay")) {
            setGodRaysDecay(json.get("god_rays_decay").getAsFloat());
        }
        if (json.has("god_rays_exposure")) {
            setGodRaysExposure(json.get("god_rays_exposure").getAsFloat());
        }
        if (json.has("god_rays_threshold")) {
            setGodRaysThreshold(json.get("god_rays_threshold").getAsFloat());
        }
        if (json.has("god_rays_sky_enabled")) {
            godRaysSkyEnabled = json.get("god_rays_sky_enabled").getAsBoolean();
        }
        
        Logging.RENDER.topic("render_config")
            .kv("hdr", hdrEnabled)
            .kv("blurQuality", blurQuality)
            .kv("blurIterations", blurIterations)
            .kv("godRays", godRaysEnabled)
            .kv("godRaysDecay", godRaysDecay)
            .kv("godRaysExposure", godRaysExposure)
            .kv("godRaysThreshold", godRaysThreshold)
            .kv("godRaysSky", godRaysSkyEnabled)
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
        json.addProperty("god_rays_enabled", godRaysEnabled);
        json.addProperty("god_rays_decay", godRaysDecay);
        json.addProperty("god_rays_exposure", godRaysExposure);
        json.addProperty("god_rays_threshold", godRaysThreshold);
        json.addProperty("god_rays_sky_enabled", godRaysSkyEnabled);
    }
    
    /**
     * Reset to default values.
     */
    public void resetToDefaults() {
        hdrEnabled = true;
        blurQuality = 1.0f;
        blurIterations = 2;
        godRaysEnabled = false;
        godRaysDecay = 0.97f;
        godRaysExposure = 0.02f;
        godRaysThreshold = 0.5f;
        godRaysSkyEnabled = false;
        
        Logging.RENDER.topic("render_config")
            .debug("Reset to defaults");
    }
}
