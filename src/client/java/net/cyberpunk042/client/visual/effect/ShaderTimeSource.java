package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;

/**
 * Manages shader animation time source for synchronization.
 * 
 * <p>Three modes available:
 * <ul>
 *   <li>CLIENT_TIME - Uses System.currentTimeMillis() (local to each client, smooth)</li>
 *   <li>WORLD_TIME - Uses world.getTime() (synced but 20 ticks/sec = jerky)</li>
 *   <li>SYNCED_TIME - Uses server time with smooth interpolation (best for multiplayer)</li>
 * </ul>
 * 
 * <p>Default is CLIENT_TIME. When a server sync packet is received, 
 * automatically switches to SYNCED_TIME for multiplayer synchronization.</p>
 */
public final class ShaderTimeSource {
    
    public enum Mode {
        /** Local client time - smooth but each client may see different animation phase */
        CLIENT_TIME,
        /** World tick time - synchronized but jerky (20 ticks/second) */
        WORLD_TIME,
        /** Server-synced time - smooth interpolation toward server time reference */
        SYNCED_TIME
    }
    
    private static Mode currentMode = Mode.CLIENT_TIME;
    
    // Sync state for SYNCED_TIME mode
    private static long targetOffset = 0;        // Target offset from server packets
    private static double currentOffset = 0;     // Current offset (lerped toward target)
    private static long lastSyncTime = 0;        // When we last received a sync packet
    private static boolean syncReceived = false; // Have we ever received a sync?
    
    // Lerp speed: lower = smoother but slower to sync
    // 0.02 = 2% per frame, takes ~2 seconds to converge
    private static final double LERP_SPEED = 0.02;
    
    // Fallback to client time if no sync in this many ms
    private static final long SYNC_TIMEOUT_MS = 60000; // 1 minute
    
    /**
     * Get the current time source mode.
     */
    public static Mode getMode() {
        return currentMode;
    }
    
    /**
     * Set the time source mode manually.
     */
    public static void setMode(Mode mode) {
        currentMode = mode;
        Logging.RENDER.topic("time_sync").info("Time source mode set to: {}", mode);
    }
    
    /**
     * Called when server sends a time sync packet.
     * This updates our offset and enables SYNCED_TIME mode.
     */
    public static void onServerTimeSync(long serverTimeMs) {
        long localTimeMs = System.currentTimeMillis();
        long newOffset = serverTimeMs - localTimeMs;
        
        if (!syncReceived) {
            // First sync: jump immediately
            currentOffset = newOffset;
            Logging.RENDER.topic("time_sync")
                .kv("serverTime", serverTimeMs)
                .kv("offset", newOffset)
                .info("Initial shader time sync received");
        } else {
            // Subsequent syncs: just update target, will lerp
            Logging.RENDER.topic("time_sync")
                .kv("oldOffset", targetOffset)
                .kv("newOffset", newOffset)
                .kv("drift", newOffset - targetOffset)
                .debug("Shader time sync update");
        }
        
        targetOffset = newOffset;
        lastSyncTime = localTimeMs;
        syncReceived = true;
        
        // Auto-enable synced mode when we receive a sync
        if (currentMode == Mode.CLIENT_TIME) {
            currentMode = Mode.SYNCED_TIME;
            Logging.RENDER.topic("time_sync").info("Auto-enabled SYNCED_TIME mode");
        }
    }
    
    /**
     * Check if we have valid sync data.
     */
    public static boolean isSynced() {
        if (!syncReceived) return false;
        long age = System.currentTimeMillis() - lastSyncTime;
        return age < SYNC_TIMEOUT_MS;
    }
    
    /**
     * Get the current offset from server time (for debugging).
     */
    public static double getCurrentOffset() {
        return currentOffset;
    }
    
    /**
     * Get the current animation time based on the active mode.
     * 
     * @return Time value suitable for shader animation (seconds, modulo for precision)
     */
    public static float getTime() {
        switch (currentMode) {
            case SYNCED_TIME:
                if (isSynced()) {
                    // Smooth interpolation toward target offset
                    currentOffset += (targetOffset - currentOffset) * LERP_SPEED;
                    long syncedMs = System.currentTimeMillis() + (long) currentOffset;
                    return (syncedMs % 100000) / 1000.0f;
                }
                // Fallthrough to client time if sync is stale
                
            case WORLD_TIME:
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world != null) {
                    long worldTicks = client.world.getTime();
                    return (worldTicks % 2000000) / 20.0f;
                }
                // Fallthrough to client time if no world
                
            case CLIENT_TIME:
            default:
                return (System.currentTimeMillis() % 100000) / 1000.0f;
        }
    }
    
    /**
     * Reset sync state (e.g., on disconnect).
     */
    public static void reset() {
        syncReceived = false;
        targetOffset = 0;
        currentOffset = 0;
        lastSyncTime = 0;
        currentMode = Mode.CLIENT_TIME;
        Logging.RENDER.topic("time_sync").debug("Shader time source reset");
    }
    
    /**
     * Get status string for debugging.
     */
    public static String getStatus() {
        if (currentMode == Mode.SYNCED_TIME && isSynced()) {
            long age = System.currentTimeMillis() - lastSyncTime;
            return String.format("SYNCED (offset=%.0fms, age=%ds)", currentOffset, age / 1000);
        } else if (currentMode == Mode.WORLD_TIME) {
            return "WORLD_TIME (20 ticks/sec)";
        } else {
            return "CLIENT_TIME (local)";
        }
    }
    
    /**
     * Check if world time is available (world loaded).
     */
    public static boolean isWorldTimeAvailable() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null;
    }
    
    private ShaderTimeSource() {}
}
