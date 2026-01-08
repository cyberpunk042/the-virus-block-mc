package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.client.render.VirusHorizonClientState;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.client.state.SingularityScheduleClientState;
import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.client.render.SingularityBorderClientState;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client network receiver initialization nodes.
 */
public final class ClientNetworkNodes {
    
    private ClientNetworkNodes() {}
    
    /**
     * All client network receivers (visual state updates from server).
     */
    public static final InitNode RECEIVERS = InitNode.simple(
        "client_receivers", "Network Receivers",
        () -> {
            // Sky/horizon tints
            ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
                context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
            ClientPlayNetworking.registerGlobalReceiver(HorizonTintPayload.ID, (payload, context) ->
                context.client().execute(() -> VirusHorizonClientState.apply(payload.enabled(), payload.intensity(), payload.argb())));
            
            // Difficulty sync
            ClientPlayNetworking.registerGlobalReceiver(DifficultySyncPayload.ID, (payload, context) ->
                context.client().execute(() -> VirusDifficultyClientState.set(payload.difficulty())));
            
            // Singularity visuals
            ClientPlayNetworking.registerGlobalReceiver(SingularityVisualStartPayload.ID, (payload, context) ->
                context.client().execute(() -> net.cyberpunk042.client.render.SingularityVisualManager.add(payload.pos())));
            ClientPlayNetworking.registerGlobalReceiver(SingularityVisualStopPayload.ID, (payload, context) ->
                context.client().execute(() -> net.cyberpunk042.client.render.SingularityVisualManager.remove(payload.pos())));
            ClientPlayNetworking.registerGlobalReceiver(SingularityBorderPayload.ID, (payload, context) ->
                context.client().execute(() -> SingularityBorderClientState.apply(payload)));
            ClientPlayNetworking.registerGlobalReceiver(SingularitySchedulePayload.ID, (payload, context) ->
                context.client().execute(() -> SingularityScheduleClientState.apply(payload)));
            
            // Shader time sync (for multiplayer animation synchronization)
            ClientPlayNetworking.registerGlobalReceiver(
                net.cyberpunk042.network.shader.ShaderTimeSyncS2CPayload.ID, (payload, context) ->
                    context.client().execute(() -> 
                        net.cyberpunk042.client.visual.effect.ShaderTimeSource.onServerTimeSync(payload.serverTimeMs())));
            
            return 8;
        }
    );
    
    /**
     * Disconnect cleanup handler.
     */
    public static final InitNode DISCONNECT_HANDLER = InitNode.simple(
        "disconnect_handler", "Disconnect Handler",
        () -> {
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> {
                    VirusSkyClientState.reset();
                    VirusHorizonClientState.reset();
                    VirusDifficultyClientState.set(VirusDifficulty.HARD);
                    SingularityBorderClientState.reset();
                    SingularityScheduleClientState.reset();
                    net.cyberpunk042.client.gui.state.FieldEditStateHolder.despawnTestField();
                    net.cyberpunk042.client.visual.effect.ShaderTimeSource.reset();
                }));
            return 1;
        }
    );
}
