package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.visual.util.FieldMath;
import net.cyberpunk042.visual.util.FieldColor;

/**
 * G121-G123: Animation utilities for GUI elements.
 * 
 * <p>Provides smooth transitions and effects using FieldMath.</p>
 */
public final class GuiAnimations {
    
    private GuiAnimations() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G121: EASING FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Smooth ease-in-out interpolation.
     * @param progress 0.0 to 1.0
     * @return eased value
     */
    public static float easeInOut(float progress) {
        return FieldMath.smoothStep(progress);
    }
    
    /**
     * Bouncy ease-out effect.
     */
    public static float bounce(float progress) {
        return FieldMath.bounce(progress);
    }
    
    /**
     * Elastic overshoot effect.
     */
    public static float elastic(float progress) {
        if (progress == 0 || progress == 1) return progress;
        float p = 0.3f;
        return (float) (Math.pow(2, -10 * progress) * Math.sin((progress - p / 4) * (2 * Math.PI) / p) + 1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G122: TRANSITION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Calculates smooth transition between two values over time.
     * @param from starting value
     * @param to ending value
     * @param progress 0.0 to 1.0
     * @return interpolated value
     */
    public static float transition(float from, float to, float progress) {
        float eased = easeInOut(Math.min(1, Math.max(0, progress)));
        return FieldMath.lerp(from, to, eased);
    }
    
    /**
     * Calculates smooth color transition (ARGB).
     */
    /**
     * Calculates smooth color transition (ARGB) using FieldColor.
     */
    public static int transitionColor(int from, int to, float progress) {
        float eased = easeInOut(Math.min(1, Math.max(0, progress)));
        return FieldColor.lerp(eased, from, to);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G123: PULSING & OSCILLATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates a pulsing value (e.g., for glow effects).
     * @param time current time in seconds
     * @param frequency pulses per second
     * @param min minimum value
     * @param max maximum value
     */
    public static float pulse(float time, float frequency, float min, float max) {
        float t = (float) ((Math.sin(time * frequency * 2 * Math.PI) + 1) / 2);
        return FieldMath.lerp(min, max, t);
    }
    
    /**
     * Generates a breathing effect (slower, smoother pulse).
     */
    public static float breathe(float time) {
        return pulse(time, 0.5f, 0.7f, 1.0f);
    }
    
    /**
     * Progress from tick count (for render methods).
     * @param startTick when animation started
     * @param currentTick current game tick
     * @param durationTicks total animation length
     */
    public static float tickProgress(long startTick, long currentTick, int durationTicks) {
        if (durationTicks <= 0) return 1f;
        float elapsed = currentTick - startTick;
        return Math.min(1f, Math.max(0f, elapsed / durationTicks));
    }
}
