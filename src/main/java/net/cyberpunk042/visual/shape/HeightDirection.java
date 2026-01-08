package net.cyberpunk042.visual.shape;

/**
 * Defines the winding direction for helix shapes.
 * 
 * <p>Determines whether the helix spirals clockwise or
 * counter-clockwise when viewed from above.</p>
 * 
 * @see HelixShape
 */
public enum HeightDirection {
    /** Clockwise when viewed from above */
    CW("cw"),
    
    /** Counter-clockwise when viewed from above */
    CCW("ccw");
    
    private final String id;
    
    HeightDirection(String id) {
        this.id = id;
    }
    
    /** String identifier for JSON */
    public String id() { return id; }
    
    /** Returns the angular direction multiplier (-1 or 1) */
    public int multiplier() {
        return this == CW ? 1 : -1;
    }
    
    /**
     * Parse from string (case-insensitive).
     * @param id Direction identifier
     * @return Matching direction, or CCW as default
     */
    public static HeightDirection fromId(String id) {
        if (id == null || id.isEmpty()) {
            return CCW;
        }
        if ("cw".equalsIgnoreCase(id) || "clockwise".equalsIgnoreCase(id)) {
            return CW;
        }
        return CCW;
    }
}
