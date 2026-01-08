package net.cyberpunk042.visual.shape;

/**
 * Visual energy type for Kamehameha orb and beam components.
 * 
 * <p>Currently simplified to CLASSIC only. Additional types may be added
 * in the future when visual effects are implemented.</p>
 * 
 * @see KamehamehaShape
 */
public enum EnergyType {
    /** Standard smooth energy beam (classic Kamehameha blue). */
    CLASSIC("Classic", "Smooth flowing energy");
    
    private final String displayName;
    private final String description;
    
    EnergyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String displayName() { return displayName; }
    public String description() { return description; }
    
    /**
     * Base alpha multiplier for this type.
     */
    public float baseAlpha() {
        return 1.0f;
    }
}
