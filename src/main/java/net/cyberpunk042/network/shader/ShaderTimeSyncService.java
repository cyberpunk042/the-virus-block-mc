package net.cyberpunk042.network.shader;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;

/**
 * Service that periodically broadcasts shader time sync to all players.
 * 
 * <p>This ensures all clients stay synchronized even over long play sessions,
 * correcting for any clock drift between computers.</p>
 * 
 * <p>Sync is sent:
 * <ul>
 *   <li>On player join (handled by TheVirusBlock.handlePlayerJoin)</li>
 *   <li>Every 30 seconds to all connected players (handled here)</li>
 * </ul>
 */
public final class ShaderTimeSyncService {
    
    /** Sync interval in ticks (30 seconds = 600 ticks at 20 TPS) */
    private static final int SYNC_INTERVAL_TICKS = 600;
    
    private static int tickCounter = 0;
    private static boolean initialized = false;
    
    /**
     * Initialize the periodic sync service.
     * Call this once during mod initialization.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        ServerTickEvents.END_SERVER_TICK.register(ShaderTimeSyncService::onServerTick);
        Logging.RENDER.topic("time_sync").info("ShaderTimeSyncService initialized (interval: {}s)", 
            SYNC_INTERVAL_TICKS / 20);
    }
    
    /**
     * Called every server tick. Broadcasts sync packet every SYNC_INTERVAL_TICKS.
     */
    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        
        if (tickCounter >= SYNC_INTERVAL_TICKS) {
            tickCounter = 0;
            broadcastTimeSync(server);
        }
    }
    
    /**
     * Broadcast time sync packet to all connected players.
     */
    public static void broadcastTimeSync(MinecraftServer server) {
        var players = PlayerLookup.all(server);
        if (players.isEmpty()) return;
        
        ShaderTimeSyncS2CPayload payload = ShaderTimeSyncS2CPayload.now();
        
        for (var player : players) {
            ServerPlayNetworking.send(player, payload);
        }
        
        Logging.RENDER.topic("time_sync")
            .kv("players", players.size())
            .kv("serverTime", payload.serverTimeMs())
            .debug("Broadcast shader time sync");
    }
    
    private ShaderTimeSyncService() {}
}
