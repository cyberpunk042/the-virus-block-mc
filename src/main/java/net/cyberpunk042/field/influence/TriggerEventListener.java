package net.cyberpunk042.field.influence;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Callback interface for receiving trigger events.
 * 
 * <p>F156: Implementations register with the event system to receive
 * notifications when relevant events occur.
 * 
 * <p>Event sources:
 * <ul>
 *   <li>Damage/Heal: Entity damage events</li>
 *   <li>Death/Respawn: Player death/respawn events</li>
 *   <li>Field spawn/despawn: FieldManager lifecycle</li>
 * </ul>
 */
@FunctionalInterface
public interface TriggerEventListener {
    
    /**
     * Called when an event occurs that may trigger effects.
     * 
     * @param event The event type
     * @param player The player involved (may be null for field events)
     * @param data Additional event data (e.g., damage amount)
     */
    void onEvent(FieldEvent event, PlayerEntity player, Object data);
}
