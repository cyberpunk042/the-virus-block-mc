package net.cyberpunk042.client.network;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * G135-G138: Utilities for sending GUI packets to server.
 */
@Environment(EnvType.CLIENT)
public final class GuiPacketSender {
    
    private GuiPacketSender() {}
    
    /**
     * G135: Send profile save request.
     */
    public static void saveProfile(String name, String json) {
        Logging.GUI.topic("network").debug("Sending profile save: {}", name);
        ClientPlayNetworking.send(ProfileSaveC2SPayload.applyToShield(name, json));
    }
    
    /**
     * G136: Send profile load request.
     */
    public static void loadProfile(String name) {
        Logging.GUI.topic("network").debug("Sending profile load: {}", name);
        ClientPlayNetworking.send(new ProfileLoadC2SPayload(name));
    }
    
    /**
     * G137: Send debug field update.
     */
    public static void updateDebugField(String json) {
        Logging.GUI.topic("network").trace("Sending debug field update");
        ClientPlayNetworking.send(DebugFieldC2SPayload.updateField(json));
    }
    
    /**
     * Spawn debug field.
     */
    public static void spawnDebugField(String json) {
        Logging.GUI.topic("network").debug("Sending debug field spawn");
        ClientPlayNetworking.send(DebugFieldC2SPayload.spawnField(json));
    }
    
    /**
     * Despawn debug field.
     */
    public static void despawnDebugField() {
        Logging.GUI.topic("network").debug("Sending debug field despawn");
        ClientPlayNetworking.send(DebugFieldC2SPayload.despawnField());
    }
    
    /**
     * G138: Request server profiles list.
     */
    public static void requestServerProfiles() {
        Logging.GUI.topic("network").debug("Requesting server profiles");
        ClientPlayNetworking.send(new RequestProfilesC2SPayload());
    }
    
    /**
     * Spawn force field at player position with offset.
     * 
     * @param fieldJson Complete field definition JSON
     * @param durationTicks Duration in ticks
     * @param offsetForward Forward offset from player (negative = behind)
     * @param offsetVertical Vertical offset from player
     * @param offsetSide Side offset from player (positive = right)
     */
    public static void spawnForceField(String fieldJson, int durationTicks,
                                        float offsetForward, float offsetVertical, float offsetSide) {
        Logging.GUI.topic("network").info("Sending force field spawn: duration={} ticks, offset=({}, {}, {})", 
            durationTicks, offsetForward, offsetVertical, offsetSide);
        
        // Send the spawn request with the complete field definition JSON
        var payload = FieldSpawnC2SPayload.create(
            fieldJson, durationTicks, offsetForward, offsetVertical, offsetSide);
        Logging.GUI.topic("network").info(">>> SENDING FieldSpawnC2SPayload (ID={}) <<<", payload.getId().id());
        ClientPlayNetworking.send(payload);
    }
    
    /**
     * Spawn shockwave field at absolute world coordinates.
     * 
     * @param fieldJson Complete field definition JSON (includes shockwave config)
     * @param worldX Absolute world X position
     * @param worldY Absolute world Y position
     * @param worldZ Absolute world Z position
     * @param sourcePrimitiveRef Reference to source primitive (e.g., "0.0" for layer0.prim0)
     */
    public static void spawnShockwaveField(String fieldJson, 
            float worldX, float worldY, float worldZ, String sourcePrimitiveRef) {
        Logging.GUI.topic("network").info("Sending shockwave field spawn at ({}, {}, {}) source={}", 
            worldX, worldY, worldZ, sourcePrimitiveRef);
        
        var payload = ShockwaveFieldSpawnC2SPayload.create(
            fieldJson, worldX, worldY, worldZ, sourcePrimitiveRef);
        ClientPlayNetworking.send(payload);
    }
}
