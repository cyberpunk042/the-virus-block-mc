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
    // GOD RAYS STYLE SETTINGS (Slots 52-54)
    // =========================================================================
    
    /** Energy mode: 0=radiation (outward), 1=absorption (inward), 2=pulse */
    private int godRaysEnergyMode = 0;
    
    /** Color mode: 0=solid, 1=gradient, 2=temperature */
    private int godRaysColorMode = 0;
    
    /** Distribution mode: 0=uniform, 1=weighted, 2=noise */
    private int godRaysDistributionMode = 0;
    
    /** Arrangement mode: 0=point, 1=ring, 2=sector */
    private int godRaysArrangementMode = 1; // Default: SPHERICAL
    
    /** Secondary color R for gradient mode (0-1 range) */
    private float godRaysColor2R = 1.0f;
    
    /** Secondary color G for gradient mode */
    private float godRaysColor2G = 0.9f;
    
    /** Secondary color B for gradient mode */
    private float godRaysColor2B = 0.7f;
    
    /** Gradient blend power (1=linear, 2=quadratic) */
    private float godRaysGradientPower = 1.0f;
    
    /** Noise scale for distribution mode 2 */
    private float godRaysNoiseScale = 8.0f;
    
    /** Noise animation speed */
    private float godRaysNoiseSpeed = 0.5f;
    
    /** Noise modulation intensity (0-1) */
    private float godRaysNoiseIntensity = 0.5f;
    
    /** Angular bias for weighted mode (-1=vertical, 0=none, 1=horizontal) */
    private float godRaysAngularBias = 0.0f;
    
    /** Curvature mode: 0=radial, 1=vortex, 2=spiral, 3=tangential, 4=pinwheel */
    private float godRaysCurvatureMode = 0.0f;
    
    /** Curvature strength (0-2 typical) */
    private float godRaysCurvatureStrength = 0.3f;
    
    /** Flicker mode: 0=none, 1=scintillation, 2=strobe, 3=fadePulse, 4=heartbeat, 5=lightning */
    private float godRaysFlickerMode = 0.0f;
    
    /** Flicker intensity (0-1) */
    private float godRaysFlickerIntensity = 0.3f;
    
    /** Flicker frequency */
    private float godRaysFlickerFrequency = 2.0f;
    
    /** Travel mode: 0=none, 1=scroll, 2=chase, 3=pulseWave, 4=comet */
    private float godRaysTravelMode = 0.0f;
    
    /** Travel speed */
    private float godRaysTravelSpeed = 1.0f;
    
    /** Travel particle count (1-10) */
    private float godRaysTravelCount = 3.0f;
    
    /** Travel particle width (0.05-0.5) */
    private float godRaysTravelWidth = 0.15f;
    
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
    // GOD RAYS STYLE GETTERS
    // =========================================================================
    
    /** Get energy mode: 0=radiation, 1=absorption, 2=pulse */
    public int getGodRaysEnergyMode() { return godRaysEnergyMode; }
    
    /** Get color mode: 0=solid, 1=gradient, 2=temperature */
    public int getGodRaysColorMode() { return godRaysColorMode; }
    
    /** Get distribution mode: 0=uniform, 1=weighted, 2=noise */
    public int getGodRaysDistributionMode() { return godRaysDistributionMode; }
    
    /** Get arrangement mode: 0=point, 1=ring, 2=sector */
    public int getGodRaysArrangementMode() { return godRaysArrangementMode; }
    
    /** Get secondary color R for gradient mode */
    public float getGodRaysColor2R() { return godRaysColor2R; }
    
    /** Get secondary color G for gradient mode */
    public float getGodRaysColor2G() { return godRaysColor2G; }
    
    /** Get secondary color B for gradient mode */
    public float getGodRaysColor2B() { return godRaysColor2B; }
    
    /** Get gradient blend power */
    public float getGodRaysGradientPower() { return godRaysGradientPower; }
    
    /** Get noise scale */
    public float getGodRaysNoiseScale() { return godRaysNoiseScale; }
    
    /** Get noise speed */
    public float getGodRaysNoiseSpeed() { return godRaysNoiseSpeed; }
    
    /** Get noise intensity */
    public float getGodRaysNoiseIntensity() { return godRaysNoiseIntensity; }
    
    /** Get angular bias */
    public float getGodRaysAngularBias() { return godRaysAngularBias; }
    
    /** Get curvature mode: 0=radial, 1=vortex, 2=spiral, 3=tangential, 4=pinwheel */
    public float getGodRaysCurvatureMode() { return godRaysCurvatureMode; }
    
    /** Get curvature strength */
    public float getGodRaysCurvatureStrength() { return godRaysCurvatureStrength; }
    
    /** Get flicker mode: 0=none, 1=scintillation, 2=strobe, 3=fadePulse, 4=heartbeat, 5=lightning */
    public float getGodRaysFlickerMode() { return godRaysFlickerMode; }
    
    /** Get flicker intensity */
    public float getGodRaysFlickerIntensity() { return godRaysFlickerIntensity; }
    
    /** Get flicker frequency */
    public float getGodRaysFlickerFrequency() { return godRaysFlickerFrequency; }
    
    /** Get travel mode: 0=none, 1=scroll, 2=chase, 3=pulseWave, 4=comet */
    public float getGodRaysTravelMode() { return godRaysTravelMode; }
    
    /** Get travel speed */
    public float getGodRaysTravelSpeed() { return godRaysTravelSpeed; }
    
    /** Get travel particle count */
    public float getGodRaysTravelCount() { return godRaysTravelCount; }
    
    /** Get travel particle width */
    public float getGodRaysTravelWidth() { return godRaysTravelWidth; }
    
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
        this.godRaysDecay = Math.max(0.90f, Math.min(0.995f, decay));
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
    // GOD RAYS STYLE SETTERS
    // =========================================================================
    
    /** Set energy mode: 0=none, 1=emission, 2=absorption, 3=reflection, 4=transmission, 5=scattering, 6=oscillation, 7=resonance */
    public void setGodRaysEnergyMode(int mode) {
        this.godRaysEnergyMode = Math.max(0, Math.min(7, mode));
    }
    
    /** Set color mode: 0=solid, 1=gradient, 2=temperature */
    public void setGodRaysColorMode(int mode) {
        this.godRaysColorMode = Math.max(0, Math.min(2, mode));
    }
    
    /** Set distribution mode: 0=uniform, 1=weighted, 2=noise, 3=random, 4=stochastic */
    public void setGodRaysDistributionMode(int mode) {
        this.godRaysDistributionMode = Math.max(0, Math.min(4, mode));
    }
    
    /** Set arrangement mode: 0=radial, 1=spherical, 2=parallel, 3=converging, 4=diverging */
    public void setGodRaysArrangementMode(int mode) {
        this.godRaysArrangementMode = Math.max(0, Math.min(4, mode));
    }
    
    /** Set secondary color for gradient mode */
    public void setGodRaysColor2(float r, float g, float b) {
        this.godRaysColor2R = r;
        this.godRaysColor2G = g;
        this.godRaysColor2B = b;
    }
    
    /** Set gradient blend power */
    public void setGodRaysGradientPower(float power) {
        this.godRaysGradientPower = Math.max(0.1f, Math.min(5f, power));
    }
    
    /** Set noise scale (0.5-50) */
    public void setGodRaysNoiseScale(float scale) {
        this.godRaysNoiseScale = Math.max(0.5f, Math.min(50f, scale));
    }
    
    /** Set noise speed */
    public void setGodRaysNoiseSpeed(float speed) {
        this.godRaysNoiseSpeed = Math.max(0f, Math.min(5f, speed));
    }
    
    /** Set noise intensity (0-1) */
    public void setGodRaysNoiseIntensity(float intensity) {
        this.godRaysNoiseIntensity = Math.max(0f, Math.min(1f, intensity));
    }
    
    /** Set angular bias (-1 to 1) */
    public void setGodRaysAngularBias(float bias) {
        this.godRaysAngularBias = Math.max(-1f, Math.min(1f, bias));
    }
    
    /** Set curvature mode: 0=none, 1=vortex, 2=spiral_arm, 3=tangential, 4=logarithmic, 5=pinwheel, 6=orbital */
    public void setGodRaysCurvatureMode(float mode) {
        this.godRaysCurvatureMode = Math.max(0f, Math.min(6f, mode));
    }
    
    /** Set curvature strength (0-2) */
    public void setGodRaysCurvatureStrength(float strength) {
        this.godRaysCurvatureStrength = Math.max(0f, Math.min(2f, strength));
    }
    
    /** Set flicker mode: 0=none, 1=scintillation, 2=strobe, 3=fade_pulse, 4=flicker, 5=lightning, 6=heartbeat */
    public void setGodRaysFlickerMode(float mode) {
        this.godRaysFlickerMode = Math.max(0f, Math.min(6f, mode));
    }
    
    /** Set flicker intensity (0-1) */
    public void setGodRaysFlickerIntensity(float intensity) {
        this.godRaysFlickerIntensity = Math.max(0f, Math.min(1f, intensity));
    }
    
    /** Set flicker frequency (0-20) */
    public void setGodRaysFlickerFrequency(float frequency) {
        this.godRaysFlickerFrequency = Math.max(0.1f, Math.min(20f, frequency));
    }
    
    /** Set travel mode: 0=none, 1=chase, 2=scroll, 3=comet, 4=spark, 5=pulse_wave */
    public void setGodRaysTravelMode(float mode) {
        this.godRaysTravelMode = Math.max(0f, Math.min(5f, mode));
    }
    
    /** Set travel speed (0-5) */
    public void setGodRaysTravelSpeed(float speed) {
        this.godRaysTravelSpeed = Math.max(0f, Math.min(5f, speed));
    }
    
    /** Set travel particle count (1-10) */
    public void setGodRaysTravelCount(float count) {
        this.godRaysTravelCount = Math.max(1f, Math.min(10f, count));
    }
    
    /** Set travel particle width (0.01-0.75) */
    public void setGodRaysTravelWidth(float width) {
        this.godRaysTravelWidth = Math.max(0.01f, Math.min(0.75f, width));
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
        
        // Style settings (slots 52-54)
        if (json.has("god_rays_energy_mode")) {
            setGodRaysEnergyMode(json.get("god_rays_energy_mode").getAsInt());
        }
        if (json.has("god_rays_color_mode")) {
            setGodRaysColorMode(json.get("god_rays_color_mode").getAsInt());
        }
        if (json.has("god_rays_distribution_mode")) {
            setGodRaysDistributionMode(json.get("god_rays_distribution_mode").getAsInt());
        }
        if (json.has("god_rays_arrangement_mode")) {
            setGodRaysArrangementMode(json.get("god_rays_arrangement_mode").getAsInt());
        }
        if (json.has("god_rays_color2_r")) {
            godRaysColor2R = json.get("god_rays_color2_r").getAsFloat();
        }
        if (json.has("god_rays_color2_g")) {
            godRaysColor2G = json.get("god_rays_color2_g").getAsFloat();
        }
        if (json.has("god_rays_color2_b")) {
            godRaysColor2B = json.get("god_rays_color2_b").getAsFloat();
        }
        if (json.has("god_rays_gradient_power")) {
            setGodRaysGradientPower(json.get("god_rays_gradient_power").getAsFloat());
        }
        if (json.has("god_rays_noise_scale")) {
            setGodRaysNoiseScale(json.get("god_rays_noise_scale").getAsFloat());
        }
        if (json.has("god_rays_noise_speed")) {
            setGodRaysNoiseSpeed(json.get("god_rays_noise_speed").getAsFloat());
        }
        if (json.has("god_rays_noise_intensity")) {
            setGodRaysNoiseIntensity(json.get("god_rays_noise_intensity").getAsFloat());
        }
        if (json.has("god_rays_angular_bias")) {
            setGodRaysAngularBias(json.get("god_rays_angular_bias").getAsFloat());
        }
        if (json.has("god_rays_curvature_mode")) {
            setGodRaysCurvatureMode(json.get("god_rays_curvature_mode").getAsFloat());
        }
        if (json.has("god_rays_curvature_strength")) {
            setGodRaysCurvatureStrength(json.get("god_rays_curvature_strength").getAsFloat());
        }
        if (json.has("god_rays_flicker_mode")) {
            setGodRaysFlickerMode(json.get("god_rays_flicker_mode").getAsFloat());
        }
        if (json.has("god_rays_flicker_intensity")) {
            setGodRaysFlickerIntensity(json.get("god_rays_flicker_intensity").getAsFloat());
        }
        if (json.has("god_rays_flicker_frequency")) {
            setGodRaysFlickerFrequency(json.get("god_rays_flicker_frequency").getAsFloat());
        }
        if (json.has("god_rays_travel_mode")) {
            setGodRaysTravelMode(json.get("god_rays_travel_mode").getAsFloat());
        }
        if (json.has("god_rays_travel_speed")) {
            setGodRaysTravelSpeed(json.get("god_rays_travel_speed").getAsFloat());
        }
        if (json.has("god_rays_travel_count")) {
            setGodRaysTravelCount(json.get("god_rays_travel_count").getAsFloat());
        }
        if (json.has("god_rays_travel_width")) {
            setGodRaysTravelWidth(json.get("god_rays_travel_width").getAsFloat());
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
            .kv("godRaysEnergyMode", godRaysEnergyMode)
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
        
        // Style settings (slots 52-54)
        json.addProperty("god_rays_energy_mode", godRaysEnergyMode);
        json.addProperty("god_rays_color_mode", godRaysColorMode);
        json.addProperty("god_rays_distribution_mode", godRaysDistributionMode);
        json.addProperty("god_rays_arrangement_mode", godRaysArrangementMode);
        json.addProperty("god_rays_color2_r", godRaysColor2R);
        json.addProperty("god_rays_color2_g", godRaysColor2G);
        json.addProperty("god_rays_color2_b", godRaysColor2B);
        json.addProperty("god_rays_gradient_power", godRaysGradientPower);
        json.addProperty("god_rays_noise_scale", godRaysNoiseScale);
        json.addProperty("god_rays_noise_speed", godRaysNoiseSpeed);
        json.addProperty("god_rays_noise_intensity", godRaysNoiseIntensity);
        json.addProperty("god_rays_angular_bias", godRaysAngularBias);
        json.addProperty("god_rays_curvature_mode", godRaysCurvatureMode);
        json.addProperty("god_rays_curvature_strength", godRaysCurvatureStrength);
        json.addProperty("god_rays_flicker_mode", godRaysFlickerMode);
        json.addProperty("god_rays_flicker_intensity", godRaysFlickerIntensity);
        json.addProperty("god_rays_flicker_frequency", godRaysFlickerFrequency);
        json.addProperty("god_rays_travel_mode", godRaysTravelMode);
        json.addProperty("god_rays_travel_speed", godRaysTravelSpeed);
        json.addProperty("god_rays_travel_count", godRaysTravelCount);
        json.addProperty("god_rays_travel_width", godRaysTravelWidth);
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
        
        // Style settings defaults (slots 52-54)
        godRaysEnergyMode = 0;      // Radiation
        godRaysColorMode = 0;       // Solid
        godRaysDistributionMode = 0; // Uniform
        godRaysArrangementMode = 1;  // Spherical (default)
        godRaysColor2R = 1.0f;
        godRaysColor2G = 0.9f;
        godRaysColor2B = 0.7f;
        godRaysGradientPower = 1.0f;
        godRaysNoiseScale = 8.0f;
        godRaysNoiseSpeed = 0.5f;
        godRaysNoiseIntensity = 0.5f;
        godRaysAngularBias = 0.0f;
        
        Logging.RENDER.topic("render_config")
            .debug("Reset to defaults");
    }
}

