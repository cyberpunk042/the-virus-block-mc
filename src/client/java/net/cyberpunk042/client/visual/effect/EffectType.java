package net.cyberpunk042.client.visual.effect;

/**
 * Types of post-process visual effects for energy fields.
 * 
 * <p>Each type corresponds to a specific shader implementation and
 * visual style. Effects can be mixed per-field, allowing different
 * fields to have different visual appearances.</p>
 */
public enum EffectType {
    
    /**
     * No post-process effect. Field uses mesh-based rendering only
     * (fresnel shader for rim/corona on the geometry itself).
     */
    NONE("none", "No Effect"),
    
    /**
     * Full energy orb effect with:
     * - Bright white core bloom
     * - Cyan/blue edge ring with glow
     * - Twisting internal Voronoi spiral patterns
     * - Radial glow lines emanating outward
     * - Color gradient from center to edge
     */
    ENERGY_ORB("energy_orb", "Energy Orb"),
    
    /**
     * Animated geodesic dome/sphere with:
     * - Icosahedral symmetry (20-fold)
     * - Hexagonal cells with depth
     * - Spectrum-colored edges
     * - Multiple animation modes
     */
    GEODESIC("geodesic", "Geodesic Sphere");
    
    // Future effect types:
    // SHIELD, AURA, PORTAL
    // ELEMENTAL_FIRE, ELEMENTAL_ICE, ELEMENTAL_LIGHTNING
    // DIVINE, VOID, TECH
    
    private final String id;
    private final String displayName;
    
    EffectType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Returns the serialization ID for this effect type.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Looks up an effect type by its ID.
     * 
     * @param id The effect type ID
     * @return The matching EffectType, or NONE if not found
     */
    public static EffectType fromId(String id) {
        if (id == null) return NONE;
        for (EffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NONE;
    }
    
    /**
     * Returns whether this effect requires the post-process pipeline.
     */
    public boolean requiresPostProcess() {
        return this != NONE;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
