package net.cyberpunk042.client.helmet;

import net.cyberpunk042.network.HelmetHudPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side runtime state for the Augmented Helmet HUD.
 * 
 * <p>Updated from server packets, provides interpolated values for smooth animations.</p>
 * 
 * <h2>Interpolation</h2>
 * <ul>
 *   <li>Direction angle lerps smoothly toward target</li>
 *   <li>Distance/signal strength also lerp for smooth bar animations</li>
 *   <li>Fade in/out when data arrives/expires</li>
 * </ul>
 */
public final class HelmetHudState {
    
    private static final HelmetHudState INSTANCE = new HelmetHudState();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RAW DATA (from server)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private volatile float targetYaw = 0f;
    private volatile float targetSignalStrength = 0f;
    private volatile int totalCount = 0;
    private volatile boolean hasTarget = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERPOLATED VALUES (for smooth rendering)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private float currentYaw = 0f;
    private float currentSignalStrength = 0f;
    private float fadeAlpha = 0f;  // 0 = hidden, 1 = fully visible
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIMING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private long lastUpdateTime = 0;
    private static final long DATA_TIMEOUT_MS = 2000;  // Fade out after 2 seconds without data
    private static final float LERP_SPEED = 0.15f;     // Interpolation speed per tick
    private static final float FADE_SPEED = 0.1f;      // Fade in/out speed per tick
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════════════════
    
    private HelmetHudState() {}
    
    public static HelmetHudState get() {
        return INSTANCE;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE FROM PAYLOAD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates state from a server payload.
     * Called on network thread, values are volatile.
     */
    public void updateFromPayload(HelmetHudPayload payload) {
        this.targetYaw = payload.yawToNearest();
        this.targetSignalStrength = payload.signalStrength();
        this.totalCount = payload.totalCount();
        this.hasTarget = payload.hasTarget();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Clears all state (e.g., on disconnect).
     */
    public void clear() {
        targetYaw = 0f;
        targetSignalStrength = 0f;
        totalCount = 0;
        hasTarget = false;
        lastUpdateTime = 0;
        fadeAlpha = 0f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK (call each frame for interpolation)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates interpolated values. Call once per render frame.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        boolean dataFresh = (now - lastUpdateTime) < DATA_TIMEOUT_MS;
        
        // Fade in/out
        float targetFade = (hasTarget && dataFresh) ? 1f : 0f;
        fadeAlpha = MathHelper.lerp(FADE_SPEED, fadeAlpha, targetFade);
        
        if (fadeAlpha < 0.01f) {
            fadeAlpha = 0f;  // Snap to zero when nearly invisible
            return;
        }
        
        // Lerp direction (handle angle wraparound)
        currentYaw = lerpAngle(currentYaw, targetYaw, LERP_SPEED);
        
        // Lerp signal
        currentSignalStrength = MathHelper.lerp(LERP_SPEED, currentSignalStrength, targetSignalStrength);
    }
    
    /**
     * Lerps between angles, handling wraparound correctly.
     */
    private static float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        // Normalize to -180 to 180
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return from + diff * t;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for rendering)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if data is available (received within timeout).
     */
    public boolean hasData() {
        long now = System.currentTimeMillis();
        return hasTarget && (now - lastUpdateTime) < DATA_TIMEOUT_MS;
    }
    
    /**
     * Returns current fade alpha (0-1) for visibility transitions.
     */
    public float getFadeAlpha() {
        return fadeAlpha;
    }
    
    /**
     * Returns interpolated yaw angle to nearest threat.
     */
    public float getYaw() {
        return currentYaw;
    }
    
    /**
     * Returns interpolated signal strength (0-1).
     */
    public float getSignalStrength() {
        return currentSignalStrength;
    }
    
    /**
     * Returns total threat count (not interpolated).
     */
    public int getTotalCount() {
        return totalCount;
    }
    
    /**
     * Returns compass direction string for the current yaw.
     */
    public String getCompassDirection() {
        float yaw = currentYaw;
        // Normalize to 0-360
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // 8-point compass
        int index = (int)((yaw + 22.5f) / 45f) % 8;
        return switch (index) {
            case 0 -> "N";
            case 1 -> "NE";
            case 2 -> "E";
            case 3 -> "SE";
            case 4 -> "S";
            case 5 -> "SW";
            case 6 -> "W";
            case 7 -> "NW";
            default -> "?";
        };
    }
    
    /**
     * Returns threat level based on distance (from VirusBlockTelemetryState).
     */
    public ThreatLevel getThreatLevel() {
        if (!hasTarget || fadeAlpha < 0.01f) return ThreatLevel.NONE;
        
        // Get distance from virus block telemetry
        float distance = net.cyberpunk042.client.visual.shader.VirusBlockTelemetryState.get().getClosestDistance();
        if (distance < 20) return ThreatLevel.CRITICAL;
        if (distance < 50) return ThreatLevel.HIGH;
        if (distance < 150) return ThreatLevel.MEDIUM;
        return ThreatLevel.LOW;
    }
    
    /**
     * Threat severity levels for color coding.
     */
    public enum ThreatLevel {
        NONE(0xFFAAAAAA),     // Gray
        LOW(0xFF00FF00),      // Green
        MEDIUM(0xFFFFFF00),   // Yellow
        HIGH(0xFFFF8800),     // Orange
        CRITICAL(0xFFFF0000); // Red (blinking)
        
        public final int color;
        
        ThreatLevel(int color) {
            this.color = color;
        }
    }
}
