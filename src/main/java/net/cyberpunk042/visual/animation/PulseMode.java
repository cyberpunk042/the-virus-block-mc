package net.cyberpunk042.visual.animation;

/**
 * Defines what property the pulse animation affects.
 * 
 * <ul>
 *   <li>{@link #SCALE} - Pulse affects size/scale</li>
 *   <li>{@link #ALPHA} - Pulse affects transparency</li>
 *   <li>{@link #GLOW} - Pulse affects glow intensity</li>
 *   <li>{@link #COLOR} - Pulse affects color (hue shift)</li>
 * </ul>
 */
public enum PulseMode {
    SCALE("Scale"),
    ALPHA("Alpha"),
    GLOW("Glow"),
    COLOR("Color");
    
    private final String label;
    
    PulseMode(String label) {
        this.label = label;
    }
    
    public String label() {
        return label;
    }
    
    @Override
    public String toString() {
        return label;
    }
    
    /**
     * Parse from string (case-insensitive).
     */
    public static PulseMode fromString(String s) {
        if (s == null) return SCALE;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SCALE;
        }
    }
}

