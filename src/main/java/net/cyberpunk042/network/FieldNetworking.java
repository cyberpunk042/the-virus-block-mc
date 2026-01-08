package net.cyberpunk042.network;

import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Network utilities for field synchronization.
 */
public final class FieldNetworking {
    
    private FieldNetworking() {}
    
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(FieldSpawnPayload.ID, FieldSpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldRemovePayload.ID, FieldRemovePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldUpdatePayload.ID, FieldUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldDefinitionSyncPayload.ID, FieldDefinitionSyncPayload.CODEC);
        
        Logging.REGISTRY.info("Registered field network payloads (spawn, remove, update)");
    }
    
    public static void sendSpawn(ServerWorld world, FieldInstance instance) {
        FieldSpawnPayload payload = new FieldSpawnPayload(
            instance.id(),
            instance.definitionId().toString(),
            instance.position().x,
            instance.position().y,
            instance.position().z,
            instance.scale(),
            instance.phase(),
            instance.maxLifeTicks()
        );
        
        int playerCount = world.getPlayers().size();
        for (ServerPlayerEntity player : world.getPlayers()) {
            Logging.REGISTRY.topic("field").info(">>> Sending to player: {} <<<", player.getName().getString());
            ServerPlayNetworking.send(player, payload);
        }
        
        Logging.REGISTRY.topic("field").info(
            ">>> SENDING FieldSpawnPayload (ID={}) for field {} to {} players <<<", 
            payload.getId().id(), instance.id(), playerCount);
    }
    
    public static void sendSpawn(ServerPlayerEntity player, FieldInstance instance) {
        FieldSpawnPayload payload = new FieldSpawnPayload(
            instance.id(),
            instance.definitionId().toString(),
            instance.position().x,
            instance.position().y,
            instance.position().z,
            instance.scale(),
            instance.phase(),
            instance.maxLifeTicks()
        );
        ServerPlayNetworking.send(player, payload);
    }
    
    public static void sendRemove(ServerWorld world, long fieldId) {
        FieldRemovePayload payload = new FieldRemovePayload(fieldId);
        int playerCount = world.getPlayers().size();
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
        
        Logging.REGISTRY.topic("field").debug(
            "Sent remove for field {} to {} players", fieldId, playerCount);
    }
    
    public static void sendUpdate(ServerWorld world, FieldInstance instance) {
        sendUpdate(world, instance, null, -1);
    }
    
    public static void sendUpdate(ServerWorld world, FieldInstance instance, String shuffleType, int shuffleIndex) {
        sendUpdateFull(world, instance, shuffleType, shuffleIndex, 
            "snap", false, 0, 0, 0, 0);
    }
    
    /**
     * Sends update with all options including follow mode and prediction.
     */
    public static void sendUpdateFull(ServerWorld world, FieldInstance instance, 
                                       String shuffleType, int shuffleIndex,
                                       String followMode, boolean predictionEnabled,
                                       int predictionLeadTicks, float predictionMaxDistance,
                                       float predictionLookAhead, float predictionVerticalBoost) {
        FieldUpdatePayload payload = FieldUpdatePayload.full(
            instance.id(),
            instance.position().x,
            instance.position().y,
            instance.position().z,
            instance.alpha(),
            shuffleType != null ? shuffleType : "",
            shuffleIndex,
            followMode,
            predictionEnabled,
            predictionLeadTicks,
            predictionMaxDistance,
            predictionLookAhead,
            predictionVerticalBoost
        );
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
        
        Logging.REGISTRY.topic("field").trace(
            "Sent update for field {} (shuffle={}:{}, follow={}, predict={})", 
            instance.id(), shuffleType, shuffleIndex, followMode, predictionEnabled);
    }
    
    /**
     * Sends update with a static pattern ID instead of shuffle index.
     */
    public static void sendUpdateWithPattern(ServerWorld world, FieldInstance instance, String patternId,
                                              String followMode, boolean predictionEnabled,
                                              int predictionLeadTicks, float predictionMaxDistance,
                                              float predictionLookAhead, float predictionVerticalBoost) {
        FieldUpdatePayload payload = FieldUpdatePayload.full(
            instance.id(),
            instance.position().x,
            instance.position().y,
            instance.position().z,
            instance.alpha(),
            "static:" + patternId,  // Prefix with "static:" to distinguish
            -1,
            followMode,
            predictionEnabled,
            predictionLeadTicks,
            predictionMaxDistance,
            predictionLookAhead,
            predictionVerticalBoost
        );
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
        
        Logging.REGISTRY.topic("field").trace(
            "Sent update for field {} (staticPattern={}, follow={}, predict={})", 
            instance.id(), patternId, followMode, predictionEnabled);
    }
    
    public static void syncAllTo(ServerPlayerEntity player, Iterable<FieldInstance> instances) {
        int count = 0;
        for (FieldInstance instance : instances) {
            sendSpawn(player, instance);
            count++;
        }
        
        if (count > 0) {
            Logging.REGISTRY.topic("field").info(
                "Synced {} fields to player {}", count, player.getName().getString());
        }
    }
    
    /**
     * Syncs all field definitions to a player.
     * Call when player joins so they have all definitions for rendering.
     */
    public static void syncDefinitionsTo(ServerPlayerEntity player) {
        var gson = new com.google.gson.Gson();
        
        int count = 0;
        for (var def : net.cyberpunk042.field.FieldRegistry.all()) {
            // Serialize to JSON
            String json = gson.toJson(def.toJson());
            var payload = new FieldDefinitionSyncPayload(def.id().toString(), json);
            ServerPlayNetworking.send(player, payload);
            count++;
        }
        
        Logging.REGISTRY.topic("field").info(
            "Synced {} definitions to {}", count, player.getName().getString());
    }
}
