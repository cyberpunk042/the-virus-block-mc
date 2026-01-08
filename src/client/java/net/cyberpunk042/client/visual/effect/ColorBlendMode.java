package net.cyberpunk042.client.visual.effect;

/**
 * Color blending modes for the Field Visual effect.
 * 
 * <p>Controls how user-defined colors combine with the shader's
 * procedurally-generated base colors.</p>
 * 
 * <p>The shader generates base colors from mathematical formulas
 * (e.g., glow falloffs, SDF distances). These modes control how
 * the user's RGB values interact with those base colors.</p>
 */
public enum ColorBlendMode {
    
    /**
     * Multiply mode (default): base × color
     * 
     * <p>White (1,1,1) = original look (no change).
     * Colors tint the effect. Black (0,0,0) = invisible.</p>
     * 
     * <p>Best for: tinting existing glow, shifting hue while preserving formula</p>
     */
    MULTIPLY(0, "Multiply", "Tint (white=original)"),
    
    /**
     * Additive mode: base + (color × intensity)
     * 
     * <p>Black (0,0,0) = original look (no change).
     * Colors add luminance/glow on top. Bright colors = extra brightness.</p>
     * 
     * <p>Best for: adding extra glow, highlights, making things brighter</p>
     */
    ADDITIVE(1, "Additive", "Add glow (black=original)"),
    
    /**
     * Replace mode: color (ignores base)
     * 
     * <p>Your RGB directly controls output color. Formula structure
     * (falloff, intensity) still applies, but base color is replaced.</p>
     * 
     * <p>Best for: exact color control, dark/muted effects, black cores</p>
     */
    REPLACE(2, "Replace", "Direct color control"),
    
    /**
     * Mix mode: mix(base, color, blend)
     * 
     * <p>Blends between formula's base and your color based on
     * an additional blend factor (uses reserved slot or fixed 0.5).</p>
     * 
     * <p>Best for: subtle color shifts, partial formula preservation</p>
     */
    MIX(3, "Mix", "Blend with formula"),
    
    /**
     * Direct mode: Your color exactly, intensity from formula
     * 
     * <p>Your RGB IS the output color. The formula only controls
     * brightness/intensity, not the actual color values.</p>
     * 
     * <p>Best for: exact color match including dark/black</p>
     */
    DIRECT(4, "Direct", "Exact RGB (test)"),
    
    /**
     * Subtract mode: Darkening effect
     * 
     * <p>Your color determines HOW MUCH to subtract from the scene.
     * White (1,1,1) = maximum darkening. Black (0,0,0) = no effect.</p>
     * 
     * <p>Best for: shadows, black holes, void effects</p>
     */
    SUBTRACT(5, "Subtract", "Darken scene (shadows)");
    
    private final int shaderValue;
    private final String displayName;
    private final String tooltip;
    
    ColorBlendMode(int shaderValue, String displayName, String tooltip) {
        this.shaderValue = shaderValue;
        this.displayName = displayName;
        this.tooltip = tooltip;
    }
    
    /**
     * Returns the integer value passed to the shader.
     */
    public int getShaderValue() {
        return shaderValue;
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
     * Looks up a mode by shader value.
     */
    public static ColorBlendMode fromShaderValue(int value) {
        for (ColorBlendMode mode : values()) {
            if (mode.shaderValue == value) {
                return mode;
            }
        }
        return MULTIPLY;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
