package net.cyberpunk042.network.gui;

import com.google.gson.JsonParser;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.PersonalFieldInstance;
import net.cyberpunk042.field.loader.FieldLoader;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks debug fields spawned by players from the GUI.
 * Debug fields are temporary preview fields that follow the player.
 * 
 * <p>Uses a single reusable definition per player to avoid registry growth.
 * <p>Throttles updates to prevent lag during rapid GUI slider changes.
 */
public final class DebugFieldTracker {
    
    // Player UUID -> debug field instance ID
    private static final Map<UUID, Long> DEBUG_FIELDS = new ConcurrentHashMap<>();
    
    // Player UUID -> their unique debug definition ID
    private static final Map<UUID, String> PLAYER_DEF_IDS = new ConcurrentHashMap<>();
    
    // Player UUID -> last definition JSON hash (to detect actual changes)
    private static final Map<UUID, Integer> LAST_DEF_HASH = new ConcurrentHashMap<>();
    
    // Player UUID -> last update time (for throttling)
    private static final Map<UUID, Long> LAST_UPDATE_TIME = new ConcurrentHashMap<>();
    
    // Minimum ms between updates per player (throttle rapid slider changes)
    private static final long UPDATE_THROTTLE_MS = 100;
    
    private DebugFieldTracker() {}
    
    /**
     * Get or create a stable debug definition ID for this player.
     */
    private static String getDebugDefId(UUID playerUuid) {
        return PLAYER_DEF_IDS.computeIfAbsent(playerUuid, 
            uuid -> "debug_" + uuid.toString().substring(0, 8));
    }
    
    /**
     * Spawn or update a debug field for a player.
     * @param player the player
     * @param definitionJson JSON string of the field definition
     * @return true if spawned/updated successfully
     */
    public static boolean spawnOrUpdate(ServerPlayerEntity player, String definitionJson) {
        UUID playerUuid = player.getUuid();
        
        // Throttle rapid updates (slider drags)
        long now = System.currentTimeMillis();
        Long lastUpdate = LAST_UPDATE_TIME.get(playerUuid);
        if (lastUpdate != null && now - lastUpdate < UPDATE_THROTTLE_MS) {
            return true; // Skip this update, previous one is recent enough
        }
        LAST_UPDATE_TIME.put(playerUuid, now);
        
        // Check if definition actually changed (skip if identical)
        int jsonHash = definitionJson.hashCode();
        Integer lastHash = LAST_DEF_HASH.get(playerUuid);
        boolean definitionChanged = lastHash == null || lastHash != jsonHash;
        
        if (!definitionChanged && DEBUG_FIELDS.containsKey(playerUuid)) {
            // Definition unchanged and field exists - nothing to do
            return true;
        }
        
        ServerWorld world = player.getWorld();
        FieldManager manager = FieldManager.get(world);
        
        try {
            // Parse the definition
            FieldLoader loader = new FieldLoader();
            FieldDefinition definition = loader.parseDefinition(
                JsonParser.parseString(definitionJson).getAsJsonObject()
            );
            
            if (definition == null) {
                Logging.GUI.topic("debug").warn("Failed to parse debug field definition");
                return false;
            }
            
            // Cache the hash
            LAST_DEF_HASH.put(playerUuid, jsonHash);
            
            // Reuse the same debug ID for this player (avoids registry growth)
            String debugId = getDebugDefId(playerUuid);
            FieldDefinition debugDef = FieldDefinition.builder(debugId)
                .type(definition.type())
                .baseRadius(definition.baseRadius())
                .themeId(definition.themeId())
                .layers(definition.layers())
                .modifiers(definition.modifiers())
                .follow(definition.follow())
                .beam(definition.beam())
                .bindings(definition.bindings())
                .triggers(definition.triggers())
                .lifecycle(definition.lifecycle())
                .build();
            
            // Replace existing definition (not add new)
            FieldRegistry.register(debugDef);
            Identifier defId = Identifier.of("the-virus-block", debugId);
            
            // Sync the definition to client
            String syncJson = debugDef.toJson().toString();
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player,
                new net.cyberpunk042.network.FieldDefinitionSyncPayload(defId.toString(), syncJson)
            );
            
            // Only spawn if no existing field
            Long existingId = DEBUG_FIELDS.get(playerUuid);
            if (existingId == null) {
                PersonalFieldInstance instance = manager.spawnForPlayer(defId, playerUuid, 1.0f);
                if (instance != null) {
                    DEBUG_FIELDS.put(playerUuid, instance.id());
                    Logging.GUI.topic("debug").debug("Spawned debug field {} for player {}", 
                        instance.id(), player.getName().getString());
                }
            } else {
                // Field exists - client will pick up the definition change automatically
                Logging.GUI.topic("debug").trace("Updated debug field definition for player {}", 
                    player.getName().getString());
            }
            
            return true;
            
        } catch (Exception e) {
            Logging.GUI.topic("debug").error("Failed to spawn debug field: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Despawn the debug field for a player.
     */
    public static boolean despawn(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        Long fieldId = DEBUG_FIELDS.remove(playerUuid);
        
        if (fieldId == null) {
            return false;
        }
        
        ServerWorld world = player.getWorld();
        FieldManager manager = FieldManager.get(world);
        boolean removed = manager.remove(fieldId);
        
        if (removed) {
            Logging.GUI.topic("debug").debug("Despawned debug field for player {}", 
                player.getName().getString());
        }
        
        return removed;
    }
    
    /**
     * Check if a player has a debug field.
     */
    public static boolean hasDebugField(UUID playerUuid) {
        return DEBUG_FIELDS.containsKey(playerUuid);
    }
    
    /**
     * Get the debug field ID for a player.
     */
    public static Long getDebugFieldId(UUID playerUuid) {
        return DEBUG_FIELDS.get(playerUuid);
    }
    
    /**
     * Clean up all debug fields (call on server stop).
     */
    public static void cleanup() {
        DEBUG_FIELDS.clear();
        PLAYER_DEF_IDS.clear();
        LAST_DEF_HASH.clear();
        LAST_UPDATE_TIME.clear();
    }
}

