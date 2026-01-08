package net.cyberpunk042.field.influence;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Source of values for reactive bindings.
 * 
 * <p>Per CLASS_DIAGRAM §16:
 * <ul>
 *   <li>{@link #getId()} - unique identifier for this source</li>
 *   <li>{@link #getValue(PlayerEntity)} - current value from player/game state</li>
 *   <li>{@link #isBoolean()} - whether this is a boolean source (0 or 1)</li>
 * </ul>
 * 
 * @see BindingSources
 * @see BindingResolver
 */
public interface BindingSource {
    
    /**
     * Gets the unique identifier for this source.
     * @return Source ID (e.g., "player.health")
     */
    String getId();
    
    /**
     * Gets the current value from the player/game state.
     * @param player The player to read from
     * @return Current value (float for continuous, 0/1 for boolean)
     */
    float getValue(PlayerEntity player);
    
    /**
     * Whether this source returns boolean values (0 or 1).
     * @return true if boolean source
     */
    boolean isBoolean();
}
