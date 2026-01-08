package net.cyberpunk042.field.influence;

/**
 * Events that can trigger visual effects on fields.
 * 
 * <p>Per ARCHITECTURE §12.2:
 * <ul>
 *   <li>PLAYER_DAMAGE - Player takes any damage</li>
 *   <li>PLAYER_HEAL - Player heals</li>
 *   <li>PLAYER_DEATH - Player dies</li>
 *   <li>PLAYER_RESPAWN - Player respawns</li>
 *   <li>FIELD_SPAWN - Field is created</li>
 *   <li>FIELD_DESPAWN - Field is removed</li>
 *   <li>FORCE_PHASE_ENTER - Force field entering a new phase</li>
 *   <li>FORCE_PHASE_EXIT - Force field exiting a phase</li>
 *   <li>FORCE_PHASE_PULL - Force field entering pull phase</li>
 *   <li>FORCE_PHASE_PUSH - Force field entering push phase</li>
 *   <li>FORCE_PHASE_HOLD - Force field entering hold phase</li>
 *   <li>FORCE_WARNING - Pre-warning before force phase change</li>
 * </ul>
 */
public enum FieldEvent {
    // Player events
    PLAYER_DAMAGE,
    PLAYER_HEAL,
    PLAYER_DEATH,
    PLAYER_RESPAWN,
    
    // Field lifecycle events
    FIELD_SPAWN,
    FIELD_DESPAWN,
    
    // Force phase events
    FORCE_PHASE_ENTER,
    FORCE_PHASE_EXIT,
    FORCE_PHASE_PULL,
    FORCE_PHASE_PUSH,
    FORCE_PHASE_HOLD,
    FORCE_WARNING;
    
    /**
     * Parses from string (e.g., "player.damage" or "PLAYER_DAMAGE").
     */
    public static FieldEvent fromId(String id) {
        if (id == null) return null;
        
        // Handle dot notation (player.damage -> PLAYER_DAMAGE)
        String normalized = id.toUpperCase().replace(".", "_");
        
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Gets the string ID for JSON (player.damage format).
     */
    public String toId() {
        return name().toLowerCase().replace("_", ".");
    }
}
