package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * G139-G140: Packet type registration for GUI networking.
 * 
 * <p>Call {@link #registerAll()} from mod initializer.</p>
 */
public final class GuiPacketRegistration {
    
    private GuiPacketRegistration() {}
    
    /**
     * G139: Register all payload types.
     */
    public static void registerAll() {
        // Server -> Client
        PayloadTypeRegistry.playS2C().register(GuiOpenS2CPayload.ID, GuiOpenS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProfileSyncS2CPayload.ID, ProfileSyncS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DebugFieldS2CPayload.ID, DebugFieldS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerProfilesS2CPayload.ID, ServerProfilesS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldEditUpdateS2CPayload.ID, FieldEditUpdateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShockwaveTriggerS2CPayload.ID, ShockwaveTriggerS2CPayload.CODEC);
        
        // Client -> Server
        PayloadTypeRegistry.playC2S().register(ProfileSaveC2SPayload.ID, ProfileSaveC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfileLoadC2SPayload.ID, ProfileLoadC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DebugFieldC2SPayload.ID, DebugFieldC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestProfilesC2SPayload.ID, RequestProfilesC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FieldSpawnC2SPayload.ID, FieldSpawnC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ShockwaveFieldSpawnC2SPayload.ID, ShockwaveFieldSpawnC2SPayload.CODEC);
        
        Logging.GUI.topic("network").info("GUI packets registered (6 S2C, 6 C2S)");
    }
    
    /**
     * G140: Register server-side packet handlers.
     * Call this after registerAll() on server.
     */
    public static void registerServerHandlers() {
        // Load server profiles on registration
        ServerProfileProvider.load();
        
        // Profile save handler - apply to shield OR OP save to server
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileSaveC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    
                    if (payload.saveToServer()) {
                        // OP saving profile to server_profiles
                        if (!player.hasPermissionLevel(2)) {
                            Logging.GUI.topic("network").warn("Non-OP {} tried to save server profile", 
                                player.getName().getString());
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.error("Operator permission required"));
                            return;
                        }
                        
                        Logging.GUI.topic("network").info("OP {} saving server profile: {}", 
                            player.getName().getString(), payload.profileName());
                        
                        boolean saved = ServerProfileProvider.saveProfile(payload.profileName(), payload.profileJson());
                        if (saved) {
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.success(payload.profileName(), "Server profile saved"));
                        } else {
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.error("Failed to save server profile"));
                        }
                    } else {
                        // Apply profile to player's shield (future gameplay)
                        Logging.GUI.topic("network").info("Player {} applying profile: {}", 
                            player.getName().getString(), payload.profileName());
                        // Future: Apply profile to player's actual shield gameplay
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.success(payload.profileName(), ""));
                    }
                });
            }
        );
        
        // Profile load handler - load a server profile by name
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileLoadC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    String profileName = payload.profileName();
                    Logging.GUI.topic("network").debug("Player {} requesting server profile: {}", 
                        player.getName().getString(), profileName);
                    
                    String json = ServerProfileProvider.getProfile(profileName);
                    if (json != null) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.success(profileName, json));
                    } else {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.error("Profile not found: " + profileName));
                    }
                });
            }
        );
        
        // Debug field handler - spawn/update/despawn preview fields
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            DebugFieldC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    if (payload.spawn()) {
                        Logging.GUI.topic("network").debug("Player {} spawning debug field", player.getName().getString());
                        boolean success = DebugFieldTracker.spawnOrUpdate(player, payload.fieldJson());
                        // Send confirmation
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, new DebugFieldS2CPayload("", success, success ? "Debug field spawned" : "Failed to spawn"));
                    } else if (payload.despawn()) {
                        Logging.GUI.topic("network").debug("Player {} despawning debug field", player.getName().getString());
                        boolean success = DebugFieldTracker.despawn(player);
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, new DebugFieldS2CPayload("", success, success ? "Debug field removed" : "No debug field to remove"));
                    } else {
                        // Update existing debug field
                        Logging.GUI.topic("network").trace("Player {} updating debug field", player.getName().getString());
                        DebugFieldTracker.spawnOrUpdate(player, payload.fieldJson());
                    }
                });
            }
        );
        
        // Request profiles handler - send list of available server profiles
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            RequestProfilesC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").debug("Player {} requesting server profiles list", player.getName().getString());
                    
                    java.util.List<String> profiles = ServerProfileProvider.listProfiles();
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                        player, new ServerProfilesS2CPayload(profiles));
                });
            }
        );
        
        // Field spawn handler - spawn force fields from GUI
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            FieldSpawnC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    var world = player.getWorld();
                    
                    Logging.GUI.topic("network").info(">>> FIELD SPAWN PACKET RECEIVED <<<");
                    Logging.GUI.topic("network").info("Player {} spawning field", player.getName().getString());
                    
                    try {
                        // Parse the field definition from JSON
                        net.cyberpunk042.field.loader.FieldLoader loader = new net.cyberpunk042.field.loader.FieldLoader();
                        net.cyberpunk042.field.FieldDefinition definition = loader.parseDefinition(
                            com.google.gson.JsonParser.parseString(payload.configJson()).getAsJsonObject()
                        );
                        
                        if (definition == null) {
                            Logging.GUI.topic("network").warn("Failed to parse field definition");
                            return;
                        }
                        
                        // Create unique ID for this spawn
                        String fullId = "force_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis();
                        net.minecraft.util.Identifier defId = net.minecraft.util.Identifier.of("the-virus-block", fullId);
                        
                        // Build the spawn definition with unique ID
                        net.cyberpunk042.field.FieldDefinition spawnDef = net.cyberpunk042.field.FieldDefinition.builder(fullId)
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
                            .forceConfig(definition.forceConfig())
                            .build();
                        net.cyberpunk042.field.FieldRegistry.register(spawnDef);
                        
                        // Sync definition to client - use full identifier with namespace!
                        String spawnDefJson = spawnDef.toJson().toString();
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player,
                            new net.cyberpunk042.network.FieldDefinitionSyncPayload(defId.toString(), spawnDefJson)
                        );
                        
                        // Calculate spawn position
                        net.minecraft.util.math.Vec3d playerPos = player.getPos();
                        net.minecraft.util.math.Vec3d lookDir = player.getRotationVector();
                        double spawnX = playerPos.x + lookDir.x * payload.offsetX();
                        double spawnY = playerPos.y + payload.offsetY() + 1.0;
                        double spawnZ = playerPos.z + lookDir.z * payload.offsetX();
                        net.minecraft.util.math.Vec3d spawnPos = new net.minecraft.util.math.Vec3d(spawnX, spawnY, spawnZ);
                        
                        // Spawn the field (this triggers onSpawn callback which sends FieldSpawnPayload)
                        net.cyberpunk042.field.FieldManager manager = net.cyberpunk042.field.FieldManager.get(world);
                        var instance = manager.spawnAt(defId, spawnPos, 1.0f, payload.durationTicks());
                        
                        if (instance != null) {
                            // onSpawn callback sends FieldSpawnPayload to all players
                            Logging.GUI.topic("network").info("Field {} spawned at {} for {} ticks", 
                                instance.id(), spawnPos, payload.durationTicks());
                        } else {
                            Logging.GUI.topic("network").warn("Failed to spawn field");
                        }
                    } catch (Exception e) {
                        Logging.GUI.topic("network").error("Failed to spawn field: {}", e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        );
        
        // Shockwave field spawn handler - spawn at absolute world coordinates
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ShockwaveFieldSpawnC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    var world = player.getWorld();
                    
                    Logging.GUI.topic("network").info(">>> SHOCKWAVE FIELD SPAWN PACKET RECEIVED <<<");
                    Logging.GUI.topic("network").info("Player {} spawning shockwave field at ({}, {}, {})", 
                        player.getName().getString(), payload.worldX(), payload.worldY(), payload.worldZ());
                    
                    try {
                        // Parse the field definition from JSON
                        net.cyberpunk042.field.loader.FieldLoader loader = new net.cyberpunk042.field.loader.FieldLoader();
                        net.cyberpunk042.field.FieldDefinition definition = loader.parseDefinition(
                            com.google.gson.JsonParser.parseString(payload.fieldJson()).getAsJsonObject()
                        );
                        
                        if (definition == null) {
                            Logging.GUI.topic("network").warn("Failed to parse shockwave field definition");
                            return;
                        }
                        
                        // Create unique ID for this spawn
                        String fullId = "shockwave_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis();
                        net.minecraft.util.Identifier defId = net.minecraft.util.Identifier.of("the-virus-block", fullId);
                        
                        // Build the spawn definition with unique ID
                        net.cyberpunk042.field.FieldDefinition spawnDef = net.cyberpunk042.field.FieldDefinition.builder(fullId)
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
                            .forceConfig(definition.forceConfig())
                            .build();
                        net.cyberpunk042.field.FieldRegistry.register(spawnDef);
                        
                        // Sync definition to client
                        String spawnDefJson = spawnDef.toJson().toString();
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player,
                            new net.cyberpunk042.network.FieldDefinitionSyncPayload(defId.toString(), spawnDefJson)
                        );
                        
                        // Spawn at absolute world position (from raycast)
                        net.minecraft.util.math.Vec3d spawnPos = new net.minecraft.util.math.Vec3d(
                            payload.worldX(), payload.worldY(), payload.worldZ());
                        
                        // Spawn the field with indefinite duration (shockwave is visual only)
                        net.cyberpunk042.field.FieldManager manager = net.cyberpunk042.field.FieldManager.get(world);
                        var instance = manager.spawnAt(defId, spawnPos, 1.0f, 600); // 30 seconds visual duration
                        
                        if (instance != null) {
                            Logging.GUI.topic("network").info("Shockwave field {} spawned at {}", 
                                instance.id(), spawnPos);
                            
                            // Broadcast shockwave trigger to ALL players in range (256 blocks)
                            var triggerPayload = ShockwaveTriggerS2CPayload.create(
                                payload.worldX(), payload.worldY(), payload.worldZ(), 
                                payload.fieldJson());
                            
                            for (var targetPlayer : world.getPlayers()) {
                                double distance = targetPlayer.getPos().distanceTo(spawnPos);
                                if (distance < 256.0) {
                                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                        targetPlayer, triggerPayload);
                                    Logging.GUI.topic("network").debug("Sent shockwave trigger to {} (dist={})", 
                                        targetPlayer.getName().getString(), distance);
                                }
                            }
                        } else {
                            Logging.GUI.topic("network").warn("Failed to spawn shockwave field");
                        }
                    } catch (Exception e) {
                        Logging.GUI.topic("network").error("Failed to spawn shockwave field: {}", e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        );
        
        Logging.GUI.topic("network").info("GUI server handlers registered");
    }
}
