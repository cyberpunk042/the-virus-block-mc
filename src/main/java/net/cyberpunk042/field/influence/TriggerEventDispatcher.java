package net.cyberpunk042.field.influence;

import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central dispatcher for trigger events.
 * 
 * <p>F156: Routes events from game systems to registered listeners.
 * Each player's field has its own TriggerProcessor registered here.
 */
public final class TriggerEventDispatcher {
    
    private static final Map<UUID, TriggerProcessor> PROCESSORS = new ConcurrentHashMap<>();
    private static final List<TriggerEventListener> GLOBAL_LISTENERS = new ArrayList<>();
    
    /**
     * Registers a trigger processor for a player.
     */
    public static void register(UUID playerId, TriggerProcessor processor) {
        PROCESSORS.put(playerId, processor);
        Logging.FIELD.topic("trigger").debug("Registered TriggerProcessor for {}", playerId);
    }
    
    /**
     * Unregisters a trigger processor.
     */
    public static void unregister(UUID playerId) {
        PROCESSORS.remove(playerId);
        Logging.FIELD.topic("trigger").debug("Unregistered TriggerProcessor for {}", playerId);
    }
    
    /**
     * Adds a global listener for all events.
     */
    public static void addGlobalListener(TriggerEventListener listener) {
        GLOBAL_LISTENERS.add(listener);
    }
    
    /**
     * Dispatches an event to the appropriate processor.
     */
    public static void dispatch(FieldEvent event, PlayerEntity player) {
        dispatch(event, player, null);
    }
    
    /**
     * Dispatches an event with additional data.
     */
    public static void dispatch(FieldEvent event, PlayerEntity player, Object data) {
        if (player == null) return;
        
        // Fire on player's processor
        TriggerProcessor processor = PROCESSORS.get(player.getUuid());
        if (processor != null) {
            processor.fireEvent(event);
        }
        
        // Notify global listeners
        for (TriggerEventListener listener : GLOBAL_LISTENERS) {
            listener.onEvent(event, player, data);
        }
        
        Logging.FIELD.topic("trigger").trace("Dispatched event: {} for {}", event, player.getName().getString());
    }
    
    /**
     * Gets the processor for a player.
     */
    public static TriggerProcessor getProcessor(UUID playerId) {
        return PROCESSORS.get(playerId);
    }
    
    private TriggerEventDispatcher() {}
}
