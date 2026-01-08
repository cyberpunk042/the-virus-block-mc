package net.cyberpunk042.visual.animation;

import net.minecraft.util.math.MathHelper;

/**
 * Defines waveform shapes for animations.
 * 
 * <p>Used by PulseConfig and AlphaPulseConfig to control
 * the shape of the animation curve.</p>
 * 
 * <h3>Visual Characteristics</h3>
 * <ul>
 *   <li>SINE - Smooth oscillation (DEFAULT)</li>
 *   <li>SQUARE - Instant on/off</li>
 *   <li>TRIANGLE_WAVE - Linear up/down</li>
 *   <li>SAWTOOTH - Linear up, instant reset</li>
 * </ul>
 * 
 * <p>Uses {@link MathHelper#sin} for fast lookup-table based sine calculation.</p>
 * 
 * @see PulseConfig
 * @see AlphaPulseConfig
 */
public enum Waveform {
    /** Smooth sinusoidal oscillation (DEFAULT) */
    SINE,
    
    /** Instant on/off switching */
    SQUARE,
    
    /** Linear up and down - named to avoid confusion with TrianglePattern */
    TRIANGLE_WAVE,
    
    /** Linear increase, instant reset to minimum */
    SAWTOOTH;
    
    /** TWO_PI constant for waveform calculations */
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    /**
     * Evaluates the waveform at time t.
     * 
     * <p>Uses {@link MathHelper#sin} for SINE waveform - this is a fast
     * lookup table implementation from Minecraft, more performant than Math.sin().</p>
     * 
     * @param t Time value, typically 0-1 for one cycle
     * @return Value between 0 and 1
     */
    public float evaluate(float t) {
        // Normalize t to 0-1 range using MathHelper.floorMod for consistency
        t = t - MathHelper.floor(t);
        
        return switch (this) {
            // MathHelper.sin() uses a lookup table - much faster than Math.sin()
            case SINE -> MathHelper.sin(t * TWO_PI) * 0.5f + 0.5f;
            case SQUARE -> t < 0.5f ? 1.0f : 0.0f;
            case TRIANGLE_WAVE -> t < 0.5f ? t * 2 : 2 - t * 2;
            case SAWTOOTH -> t;
        };
    }

    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching Waveform, or SINE if not found
     */
    public static Waveform fromId(String id) {
        if (id == null || id.isEmpty()) return SINE;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return SINE;
        }
    }
}
