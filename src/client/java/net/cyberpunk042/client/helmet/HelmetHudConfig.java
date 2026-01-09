package net.cyberpunk042.client.helmet;

import net.cyberpunk042.log.Logging;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Client-side configuration for the Augmented Helmet HUD.
 * 
 * <p>All settings are persisted to disk and can be modified via {@code /helmethud} commands.</p>
 * 
 * <h2>Config Categories</h2>
 * <ul>
 *   <li>Visor Effects - vignette, scan lines, intensity</li>
 *   <li>HUD Panel - enabled, scale, opacity</li>
 *   <li>Markers - max count, render range</li>
 * </ul>
 */
public final class HelmetHudConfig {
    
    private static final HelmetHudConfig INSTANCE = new HelmetHudConfig();
    
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("helmet_hud.properties");
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISOR EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean visorEnabled = true;
    private float visorIntensity = 1.0f;        // 0.0 - 1.0 (master intensity)
    private boolean scanlinesEnabled = true;
    private float scanlinesOpacity = 0.15f;     // 0.0 - 0.5
    private float vignetteStrength = 0.4f;      // 0.0 - 1.0
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HUD PANEL
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean hudEnabled = true;
    private float hudScale = 1.0f;              // 0.5 - 2.0
    private float hudOpacity = 0.85f;           // 0.3 - 1.0
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARKERS & DETECTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean markersEnabled = true;
    private int maxMarkerCount = 20;            // 0 = unlimited
    private float markerRenderRange = 0f;       // 0 = unlimited (show all markers in packet)
    private float detectionRange = 0f;          // 0 = unlimited detection
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════════════════
    
    private HelmetHudConfig() {
        load();
    }
    
    public static HelmetHudConfig get() {
        return INSTANCE;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISOR GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isVisorEnabled() { return visorEnabled; }
    public void setVisorEnabled(boolean enabled) {
        this.visorEnabled = enabled;
        save();
        log("visorEnabled", enabled);
    }
    
    public float getVisorIntensity() { return visorIntensity; }
    public void setVisorIntensity(float intensity) {
        this.visorIntensity = MathHelper.clamp(intensity, 0f, 1f);
        save();
        log("visorIntensity", this.visorIntensity);
    }
    
    public boolean isScanlinesEnabled() { return scanlinesEnabled; }
    public void setScanlinesEnabled(boolean enabled) {
        this.scanlinesEnabled = enabled;
        save();
        log("scanlinesEnabled", enabled);
    }
    
    public float getScanlinesOpacity() { return scanlinesOpacity; }
    public void setScanlinesOpacity(float opacity) {
        this.scanlinesOpacity = MathHelper.clamp(opacity, 0f, 0.5f);
        save();
        log("scanlinesOpacity", this.scanlinesOpacity);
    }
    
    public float getVignetteStrength() { return vignetteStrength; }
    public void setVignetteStrength(float strength) {
        this.vignetteStrength = MathHelper.clamp(strength, 0f, 1f);
        save();
        log("vignetteStrength", this.vignetteStrength);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HUD GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isHudEnabled() { return hudEnabled; }
    public void setHudEnabled(boolean enabled) {
        this.hudEnabled = enabled;
        save();
        log("hudEnabled", enabled);
    }
    
    public float getHudScale() { return hudScale; }
    public void setHudScale(float scale) {
        this.hudScale = MathHelper.clamp(scale, 0.5f, 2f);
        save();
        log("hudScale", this.hudScale);
    }
    
    public float getHudOpacity() { return hudOpacity; }
    public void setHudOpacity(float opacity) {
        this.hudOpacity = MathHelper.clamp(opacity, 0.3f, 1f);
        save();
        log("hudOpacity", this.hudOpacity);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARKER GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isMarkersEnabled() { return markersEnabled; }
    public void setMarkersEnabled(boolean enabled) {
        this.markersEnabled = enabled;
        save();
        log("markersEnabled", enabled);
    }
    
    public int getMaxMarkerCount() { return maxMarkerCount; }
    public void setMaxMarkerCount(int count) {
        this.maxMarkerCount = Math.max(0, count);
        save();
        log("maxMarkerCount", this.maxMarkerCount);
    }
    
    public float getMarkerRenderRange() { return markerRenderRange; }
    public void setMarkerRenderRange(float range) {
        this.markerRenderRange = Math.max(0f, range);
        save();
        log("markerRenderRange", this.markerRenderRange);
    }
    
    public float getDetectionRange() { return detectionRange; }
    public void setDetectionRange(float range) {
        this.detectionRange = Math.max(0f, range);  // 0 = unlimited
        save();
        log("detectionRange", this.detectionRange);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Resets all settings to defaults.
     */
    public void reset() {
        visorEnabled = true;
        visorIntensity = 1.0f;
        scanlinesEnabled = true;
        scanlinesOpacity = 0.15f;
        vignetteStrength = 0.4f;
        
        hudEnabled = true;
        hudScale = 1.0f;
        hudOpacity = 0.85f;
        
        markersEnabled = true;
        maxMarkerCount = 20;
        markerRenderRange = 0f;
        detectionRange = 0f;
        
        save();
        Logging.RENDER.topic("helmet_hud").info("Config reset to defaults");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return;  // Use defaults
            }
            
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
                props.load(reader);
            }
            
            // Visor
            visorEnabled = Boolean.parseBoolean(props.getProperty("visor.enabled", "true"));
            visorIntensity = parseFloat(props, "visor.intensity", 1.0f);
            scanlinesEnabled = Boolean.parseBoolean(props.getProperty("visor.scanlines", "true"));
            scanlinesOpacity = parseFloat(props, "visor.scanlines.opacity", 0.15f);
            vignetteStrength = parseFloat(props, "visor.vignette", 0.4f);
            
            // HUD
            hudEnabled = Boolean.parseBoolean(props.getProperty("hud.enabled", "true"));
            hudScale = parseFloat(props, "hud.scale", 1.0f);
            hudOpacity = parseFloat(props, "hud.opacity", 0.85f);
            
            // Markers
            markersEnabled = Boolean.parseBoolean(props.getProperty("markers.enabled", "true"));
            maxMarkerCount = parseInt(props, "markers.maxCount", 10);
            markerRenderRange = parseFloat(props, "markers.renderRange", 100f);
            detectionRange = parseFloat(props, "detection.range", 0f);
            
            Logging.RENDER.topic("helmet_hud").info("Config loaded from {}", CONFIG_PATH);
            
        } catch (Exception e) {
            Logging.RENDER.topic("helmet_hud").warn("Failed to load config: {}", e.getMessage());
        }
    }
    
    private void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            
            Properties props = new Properties();
            
            // Visor
            props.setProperty("visor.enabled", String.valueOf(visorEnabled));
            props.setProperty("visor.intensity", String.valueOf(visorIntensity));
            props.setProperty("visor.scanlines", String.valueOf(scanlinesEnabled));
            props.setProperty("visor.scanlines.opacity", String.valueOf(scanlinesOpacity));
            props.setProperty("visor.vignette", String.valueOf(vignetteStrength));
            
            // HUD
            props.setProperty("hud.enabled", String.valueOf(hudEnabled));
            props.setProperty("hud.scale", String.valueOf(hudScale));
            props.setProperty("hud.opacity", String.valueOf(hudOpacity));
            
            // Markers
            props.setProperty("markers.enabled", String.valueOf(markersEnabled));
            props.setProperty("markers.maxCount", String.valueOf(maxMarkerCount));
            props.setProperty("markers.renderRange", String.valueOf(markerRenderRange));
            props.setProperty("detection.range", String.valueOf(detectionRange));
            
            try (var writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, "Augmented Helmet HUD Configuration");
            }
            
        } catch (IOException e) {
            Logging.RENDER.topic("helmet_hud").warn("Failed to save config: {}", e.getMessage());
        }
    }
    
    private static float parseFloat(Properties props, String key, float defaultVal) {
        try {
            return Float.parseFloat(props.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    
    private static int parseInt(Properties props, String key, int defaultVal) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    
    private static void log(String key, Object value) {
        Logging.RENDER.topic("helmet_hud").debug("{} = {}", key, value);
    }
}
