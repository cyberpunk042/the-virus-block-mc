package net.cyberpunk042.client.visual.effect;

import org.lwjgl.opengl.GL14;

/**
 * OpenGL blend equation modes for hardware blending.
 * 
 * <p>Controls the mathematical operation used when blending the fragment shader's
 * output with the existing framebuffer content.</p>
 * 
 * <p>Standard formula with GL_FUNC_ADD:
 * <pre>result = src * srcFactor + dst * dstFactor</pre></p>
 * 
 * <p>With GL_FUNC_REVERSE_SUBTRACT:
 * <pre>result = dst * dstFactor - src * srcFactor</pre>
 * This enables true darkening effects!</p>
 */
public enum BlendEquation {
    
    /**
     * Blending OFF: disables GL_BLEND entirely.
     * 
     * <p>Fragment output directly replaces framebuffer content.
     * No transparency at all - useful for debugging to see exact shader output.</p>
     */
    OFF(-1, "Off", "No blending (direct replace)"),
    
    /**
     * Additive (default): result = src + dst (after factor multiplication)
     * 
     * <p>Standard blending - combines colors by adding them together.
     * Used for glows, particles, light effects.</p>
     */
    ADD(GL14.GL_FUNC_ADD, "Add", "Standard (src + dst)"),
    
    /**
     * Subtractive: result = src - dst
     * 
     * <p>Subtracts destination from source.
     * Fragment output REDUCES the existing color.</p>
     */
    SUBTRACT(GL14.GL_FUNC_SUBTRACT, "Subtract", "src - dst"),
    
    /**
     * Reverse Subtractive: result = dst - src
     * 
     * <p>Subtracts source from destination.
     * Fragment output DARKENS the scene - the key to dark effects!</p>
     * 
     * <p>Example: Output (0.5, 0, 0) subtracts 0.5 red from the scene.</p>
     */
    REVERSE_SUBTRACT(GL14.GL_FUNC_REVERSE_SUBTRACT, "Darken", "dst - src (darkness!)"),
    
    /**
     * Minimum: result = min(src, dst)
     * 
     * <p>Takes the darker of source and destination for each channel.</p>
     */
    MIN(GL14.GL_MIN, "Min", "Darker of two"),
    
    /**
     * Maximum: result = max(src, dst)
     * 
     * <p>Takes the brighter of source and destination for each channel.</p>
     */
    MAX(GL14.GL_MAX, "Max", "Brighter of two");
    
    /**
     * Returns true if this mode disables blending entirely.
     */
    public boolean isBlendDisabled() {
        return this == OFF;
    }
    
    private final int glValue;
    private final String displayName;
    private final String tooltip;
    
    BlendEquation(int glValue, String displayName, String tooltip) {
        this.glValue = glValue;
        this.displayName = displayName;
        this.tooltip = tooltip;
    }
    
    /**
     * Returns the OpenGL constant value.
     */
    public int getGlValue() {
        return glValue;
    }
    
    /**
     * Returns the display name for GUI.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the tooltip/help text.
     */
    public String getTooltip() {
        return tooltip;
    }
    
    /**
     * Looks up a mode by GL value.
     */
    public static BlendEquation fromGlValue(int value) {
        for (BlendEquation mode : values()) {
            if (mode.glValue == value) {
                return mode;
            }
        }
        return ADD;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
