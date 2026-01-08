package net.cyberpunk042.field;

/**
 * Types of visual fields that can be rendered.
 * 
 * <p>Each type has different behavior and attachment semantics:</p>
 * <ul>
 *   <li>{@link #SHIELD} - Protective bubble around blocks or players</li>
 *   <li>{@link #PERSONAL} - Player-attached field that follows movement</li>
 *   <li>{@link #FORCE} - Area effect field</li>
 *   <li>{@link #AURA} - Player aura effect</li>
 *   <li>{@link #PORTAL} - Portal visual effect</li>
 * </ul>
 * 
 * <p><b>Removed in Phase 1:</b> GROWTH, SINGULARITY, BARRIER
 * (these are handled by the Growth Block system, not the Field system)</p>
 */
public enum FieldType {
    /**
     * Protective shield bubble.
     * Can be attached to blocks or spawned at world positions.
     */
    SHIELD("shield"),
    
    /**
     * Personal field that follows a player.
     * Supports movement prediction for smooth tracking.
     */
    PERSONAL("personal"),
    
    /**
     * Area effect force field.
     * Used for pushing/pulling effects.
     */
    FORCE("force"),
    
    /**
     * Player aura effect.
     * Cosmetic aura around players.
     */
    AURA("aura"),
    
    /**
     * Portal visual effect.
     * For teleportation or dimensional effects.
     */
    PORTAL("portal"),
    
    /**
     * Test/debug field.
     * Used for /fieldtest command debugging.
     */
    TEST("test");
    
    private final String id;
    
    FieldType(String id) {
        this.id = id;
    }
    
    /**
     * Returns the string identifier for this type.
     */
    public String id() {
        return id;
    }
    
    /**
     * Whether this type supports player attachment.
     */
    public boolean supportsPlayer() {
        return this == PERSONAL || this == AURA;
    }
    
    /**
     * Whether this type supports block attachment.
     */
    public boolean supportsBlock() {
        return this == SHIELD || this == FORCE || this == PORTAL;
    }
    
    /**
     * Whether this type supports world position spawning.
     */
    public boolean supportsWorldPosition() {
        return this == SHIELD || this == FORCE || this == PORTAL;
    }
    
    /**
     * Whether this type uses movement prediction.
     */
    public boolean usesPrediction() {
        return this == PERSONAL;
    }
    
    /**
     * Parse from string (case-insensitive).
     * Returns null if not found.
     */
    public static FieldType fromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String lower = id.toLowerCase();
        for (FieldType type : values()) {
            if (type.id.equals(lower)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Parse from string with default fallback.
     */
    public static FieldType fromIdOrDefault(String id, FieldType defaultType) {
        FieldType type = fromId(id);
        return type != null ? type : defaultType;
    }
}
