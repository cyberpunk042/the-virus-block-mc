package net.cyberpunk042.visual.energy;

/**
 * Types of energy interaction for visual effects.
 * 
 * <p>This is the top-level categorization of how energy interacts
 * with shapes. Each type contains specific modes.</p>
 * 
 * <h2>Hierarchy</h2>
 * <pre>
 * EnergyInteraction (UI control)
 * └── RADIATIVE → RadiativeInteraction
 * └── (Future: THERMAL, KINETIC, etc.)
 * </pre>
 * 
 * @see RadiativeInteraction
 */
public enum EnergyInteractionType {
    /** No energy interaction. */
    NONE("None"),
    
    /** Radiative energy (emission, absorption, etc.). */
    RADIATIVE("Radiative"),
    
    // Future types:
    // THERMAL("Thermal"),
    // KINETIC("Kinetic"),
    // ELECTROMAGNETIC("Electromagnetic"),
    ;
    
    private final String displayName;
    
    EnergyInteractionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String displayName() {
        return displayName;
    }
    
    /**
     * Parses from string, case-insensitive.
     */
    public static EnergyInteractionType fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
