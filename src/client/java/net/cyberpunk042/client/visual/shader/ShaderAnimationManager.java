package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;

/**
 * Manages shader animation parameters for time-based visual effects.
 * 
 * <h2>Animation Modes (Bitmask)</h2>
 * <ul>
 *   <li>{@link #MODE_BREATHE_CORONA} (1): Corona intensity breathing</li>
 *   <li>{@link #MODE_BREATHE_RIM} (2): Rim intensity breathing</li>
 *   <li>{@link #MODE_PULSE_ALPHA} (4): Overall alpha pulsing</li>
 *   <li>{@link #MODE_COLOR_CYCLE} (8): Hue rotation over time</li>
 *   <li>{@link #MODE_FLICKER} (16): Random-ish energy flicker</li>
 *   <li>{@link #MODE_WAVE_INTENSITY} (32): Wave pattern on intensity</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Enable breathing for corona
 * ShaderAnimationManager.setMode(ShaderAnimationManager.MODE_BREATHE_CORONA);
 * 
 * // Combine modes
 * ShaderAnimationManager.setMode(
 *     ShaderAnimationManager.MODE_BREATHE_CORONA | 
 *     ShaderAnimationManager.MODE_COLOR_CYCLE
 * );
 * 
 * // Adjust speed and strength
 * ShaderAnimationManager.setSpeed(1.5f);
 * ShaderAnimationManager.setStrength(0.5f);
 * </pre>
 */
public final class ShaderAnimationManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION MODE FLAGS (matches GLSL constants)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Corona intensity breathing - slow sinusoidal pulsing */
    public static final int MODE_BREATHE_CORONA = 1;   // Bit 0
    
    /** Rim intensity breathing - slow sinusoidal pulsing */
    public static final int MODE_BREATHE_RIM = 2;      // Bit 1
    
    /** Overall alpha pulsing - affects transparency */
    public static final int MODE_PULSE_ALPHA = 4;      // Bit 2
    
    /** Color cycling - hue rotation over time */
    public static final int MODE_COLOR_CYCLE = 8;      // Bit 3
    
    /** Energy flicker - random-ish intensity variation */
    public static final int MODE_FLICKER = 16;         // Bit 4
    
    /** Wave intensity - edge-based wave pattern */
    public static final int MODE_WAVE_INTENSITY = 32;  // Bit 5
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET COMBINATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** All effects disabled */
    public static final int PRESET_NONE = 0;
    
    /** Gentle breathing on both rim and corona */
    public static final int PRESET_BREATHING = MODE_BREATHE_CORONA | MODE_BREATHE_RIM;
    
    /** Energetic flickering with breathing */
    public static final int PRESET_ENERGY = MODE_BREATHE_CORONA | MODE_FLICKER;
    
    /** Rainbow cycling effect */
    public static final int PRESET_RAINBOW = MODE_COLOR_CYCLE;
    
    /** Unstable/overloaded effect */
    public static final int PRESET_UNSTABLE = MODE_FLICKER | MODE_PULSE_ALPHA | MODE_WAVE_INTENSITY;
    
    /** All effects enabled */
    public static final int PRESET_ALL = MODE_BREATHE_CORONA | MODE_BREATHE_RIM | 
                                         MODE_PULSE_ALPHA | MODE_COLOR_CYCLE | 
                                         MODE_FLICKER | MODE_WAVE_INTENSITY;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Current animation parameters
    private static float currentTime = 0.0f;
    private static int currentMode = PRESET_NONE;
    private static float currentSpeed = 1.0f;
    private static float currentStrength = 0.3f;
    
    // Time tracking
    private static long lastTickTime = 0;
    private static boolean paused = false;
    
    // Debug logging
    private static int tickCount = 0;
    private static boolean initialized = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the animation manager.
     * Call this during client initialization.
     */
    public static void init() {
        if (initialized) return;
        
        lastTickTime = System.currentTimeMillis();
        initialized = true;
        
        Logging.RENDER.topic("shader_anim")
            .info("ShaderAnimationManager initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK / UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates the animation time. Call this once per frame.
     * 
     * <p>Uses game time so animations pause when game is paused.</p>
     */
    public static void tick() {
        if (!initialized) init();
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Check if game is paused
        boolean gameIsPaused = client.isPaused() || 
                               (client.world == null);
        
        if (gameIsPaused) {
            // Don't advance time when paused
            paused = true;
            return;
        }
        
        // Calculate delta time
        long now = System.currentTimeMillis();
        if (paused) {
            // Coming out of pause - reset delta
            lastTickTime = now;
            paused = false;
        }
        
        float deltaSeconds = (now - lastTickTime) / 1000.0f;
        lastTickTime = now;
        
        // Clamp delta to prevent huge jumps (e.g., after alt-tab)
        deltaSeconds = Math.min(deltaSeconds, 0.1f);
        
        // Advance time
        currentTime += deltaSeconds;
        
        // Wrap time to prevent float precision issues after hours of play
        // 3600 seconds = 1 hour, plenty of range for smooth animations
        if (currentTime > 3600.0f) {
            currentTime -= 3600.0f;
        }
        
        tickCount++;
        
        // Debug log every 5 seconds
        if (tickCount % 300 == 0 && currentMode != 0) {
            Logging.RENDER.topic("shader_anim")
                .kv("time", String.format("%.2f", currentTime))
                .kv("mode", currentMode)
                .kv("speed", currentSpeed)
                .kv("strength", currentStrength)
                .info("Animation tick");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for uniform binding)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets the current animation time in seconds.
     * Use this when binding u_time uniform.
     */
    public static float getTime() {
        return currentTime;
    }
    
    /**
     * Gets the current animation mode bitmask.
     * Use this when binding u_animMode uniform.
     */
    public static int getMode() {
        return currentMode;
    }
    
    /**
     * Gets the current animation speed multiplier.
     * Use this when binding u_animSpeed uniform.
     */
    public static float getSpeed() {
        return currentSpeed;
    }
    
    /**
     * Gets the current animation strength/amplitude.
     * Use this when binding u_animStrength uniform.
     */
    public static float getStrength() {
        return currentStrength;
    }
    
    /**
     * Returns whether any animation mode is active.
     */
    public static boolean isActive() {
        return currentMode != 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the animation mode bitmask.
     * 
     * @param mode Bitmask of MODE_* constants (0 to disable)
     */
    public static void setMode(int mode) {
        currentMode = mode;
        Logging.RENDER.topic("shader_anim")
            .kv("mode", mode)
            .kv("binary", Integer.toBinaryString(mode))
            .info("Animation mode set");
    }
    
    /**
     * Enables specific animation mode(s) without disabling others.
     * 
     * @param mode Mode(s) to enable
     */
    public static void enableMode(int mode) {
        currentMode |= mode;
    }
    
    /**
     * Disables specific animation mode(s) without affecting others.
     * 
     * @param mode Mode(s) to disable
     */
    public static void disableMode(int mode) {
        currentMode &= ~mode;
    }
    
    /**
     * Toggles specific animation mode(s).
     * 
     * @param mode Mode(s) to toggle
     */
    public static void toggleMode(int mode) {
        currentMode ^= mode;
    }
    
    /**
     * Sets animation speed multiplier.
     * 
     * @param speed Speed multiplier (1.0 = normal, 0.5 = half, 2.0 = double)
     */
    public static void setSpeed(float speed) {
        currentSpeed = Math.max(0.0f, Math.min(10.0f, speed));
    }
    
    /**
     * Sets animation strength/amplitude.
     * 
     * @param strength Strength value (0.0 = none, 1.0 = full)
     */
    public static void setStrength(float strength) {
        currentStrength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    /**
     * Applies a preset configuration.
     * 
     * @param preset One of PRESET_* constants
     */
    public static void applyPreset(int preset) {
        setMode(preset);
    }
    
    /**
     * Resets all animation parameters to defaults.
     */
    public static void reset() {
        currentMode = PRESET_NONE;
        currentSpeed = 1.0f;
        currentStrength = 0.3f;
        Logging.RENDER.topic("shader_anim").info("Animation parameters reset");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets a human-readable description of the current mode.
     */
    public static String getModeDescription() {
        if (currentMode == 0) return "None";
        
        StringBuilder sb = new StringBuilder();
        if ((currentMode & MODE_BREATHE_CORONA) != 0) sb.append("Corona ");
        if ((currentMode & MODE_BREATHE_RIM) != 0) sb.append("Rim ");
        if ((currentMode & MODE_PULSE_ALPHA) != 0) sb.append("Pulse ");
        if ((currentMode & MODE_COLOR_CYCLE) != 0) sb.append("Rainbow ");
        if ((currentMode & MODE_FLICKER) != 0) sb.append("Flicker ");
        if ((currentMode & MODE_WAVE_INTENSITY) != 0) sb.append("Wave ");
        
        return sb.toString().trim();
    }
    
    /**
     * Gets status string for debug display.
     */
    public static String getStatusString() {
        return String.format("Mode=%d (%s) Speed=%.1f Str=%.2f Time=%.1f",
            currentMode, getModeDescription(), currentSpeed, currentStrength, currentTime);
    }
    
    private ShaderAnimationManager() {}
}
