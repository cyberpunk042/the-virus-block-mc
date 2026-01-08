package net.cyberpunk042.client.util;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client-side performance profiler.
 * Tracks render performance, network latency, and client tick handlers.
 */
public final class ClientProfiler {
    
    private static final Map<String, AtomicLong> TOTAL_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> CALL_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, Long> MAX_SINGLE_CALL = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> START_TIMES = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Network latency tracking
    private static final Map<String, AtomicLong> RTT_TOTAL = new ConcurrentHashMap<>();  // Round-trip time
    private static final Map<String, AtomicLong> RTT_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, Long> RTT_MAX = new ConcurrentHashMap<>();
    private static final Map<String, Long> PENDING_REQUESTS = new ConcurrentHashMap<>();
    
    // Per-tick tracking
    private static long tickStartTime = 0;
    private static long totalTickTimeNs = 0;
    private static long modCodeTimeNs = 0;
    private static int tickCount = 0;
    private static int spikeCount = 0;
    private static long worstTickMs = 0;
    
    // Config
    private static final long SPIKE_THRESHOLD_MS = 50;  // Client is more sensitive
    private static final long REPORT_INTERVAL_MS = 5000;
    private static long lastReportTime = 0;
    
    private static volatile boolean enabled = true;
    
    private ClientProfiler() {}
    
    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> onTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTickEnd(client));
        Logging.PROFILER.info("[ClientProfiler] Client-side monitoring enabled");
    }
    
    private static void onTickStart() {
        if (!enabled) return;
        tickStartTime = System.nanoTime();
    }
    
    private static void onTickEnd(MinecraftClient client) {
        if (!enabled) return;
        long tickTimeNs = System.nanoTime() - tickStartTime;
        long tickTimeMs = tickTimeNs / 1_000_000;
        
        totalTickTimeNs += tickTimeNs;
        tickCount++;
        
        if (tickTimeMs > SPIKE_THRESHOLD_MS) {
            spikeCount++;
            if (tickTimeMs > worstTickMs) {
                worstTickMs = tickTimeMs;
            }
            Logging.PROFILER.warn("[CLIENT SPIKE] {}ms tick | Mod: {}ms",
                tickTimeMs, modCodeTimeNs / 1_000_000);
        }
        
        long now = System.currentTimeMillis();
        if (now - lastReportTime >= REPORT_INTERVAL_MS) {
            generateReport(client);
            lastReportTime = now;
        }
        
        modCodeTimeNs = 0;
    }
    
    // ============================================================================
    // PROFILING API
    // ============================================================================
    
    public static void start(String label) {
        if (!enabled) return;
        START_TIMES.get().put(label, System.nanoTime());
    }
    
    public static void end(String label) {
        if (!enabled) return;
        Long startTime = START_TIMES.get().remove(label);
        if (startTime == null) return;
        
        long elapsed = System.nanoTime() - startTime;
        TOTAL_TIME.computeIfAbsent(label, k -> new AtomicLong()).addAndGet(elapsed);
        CALL_COUNT.computeIfAbsent(label, k -> new AtomicLong()).incrementAndGet();
        modCodeTimeNs += elapsed;
        
        long elapsedMs = elapsed / 1_000_000;
        MAX_SINGLE_CALL.compute(label, (k, v) -> v == null ? elapsedMs : Math.max(v, elapsedMs));
    }
    
    // ============================================================================
    // NETWORK LATENCY TRACKING
    // ============================================================================
    
    /**
     * Call when sending a request to server. Returns a request ID.
     */
    public static long startRequest(String packetType) {
        if (!enabled) return 0;
        long requestId = System.nanoTime();
        PENDING_REQUESTS.put(packetType + ":" + requestId, requestId);
        return requestId;
    }
    
    /**
     * Call when receiving response from server.
     */
    public static void endRequest(String packetType, long requestId) {
        if (!enabled || requestId == 0) return;
        String key = packetType + ":" + requestId;
        Long startTime = PENDING_REQUESTS.remove(key);
        if (startTime == null) return;
        
        long rtt = System.nanoTime() - startTime;
        RTT_TOTAL.computeIfAbsent(packetType, k -> new AtomicLong()).addAndGet(rtt);
        RTT_COUNT.computeIfAbsent(packetType, k -> new AtomicLong()).incrementAndGet();
        
        long rttMs = rtt / 1_000_000;
        RTT_MAX.compute(packetType, (k, v) -> v == null ? rttMs : Math.max(v, rttMs));
    }
    
    /**
     * Track one-way packet (no response expected).
     */
    public static void trackPacketReceived(String packetType, int bytes) {
        if (!enabled) return;
        CALL_COUNT.computeIfAbsent("NET:" + packetType, k -> new AtomicLong()).incrementAndGet();
        TOTAL_TIME.computeIfAbsent("NET:" + packetType, k -> new AtomicLong()).addAndGet(bytes);
    }
    
    // ============================================================================
    // REPORT
    // ============================================================================
    
    private static void generateReport(MinecraftClient client) {
        if (tickCount == 0) return;
        
        long avgTickMs = totalTickTimeNs / tickCount / 1_000_000;
        long modTotalMs = TOTAL_TIME.values().stream()
            .filter(v -> !v.toString().startsWith("NET:"))
            .mapToLong(AtomicLong::get).sum() / 1_000_000;
        long unaccountedMs = (totalTickTimeNs / 1_000_000) - modTotalMs;
        double fps = tickCount / (REPORT_INTERVAL_MS / 1000.0);
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("+========================================================================+\n");
        sb.append("|                 CLIENT PROFILER - PERFORMANCE REPORT                  |\n");
        sb.append("+========================================================================+\n");
        sb.append(String.format("| SUMMARY                                                                |\n"));
        sb.append(String.format("|   Ticks: %-4d  |  Avg: %-3dms  |  FPS: %-5.1f  |  Spikes: %-3d          |\n",
            tickCount, avgTickMs, fps, spikeCount));
        sb.append(String.format("|   Worst: %-3dms  |  Mod: %-4dms  |  Unaccounted: %-5dms              |\n",
            worstTickMs, modTotalMs, unaccountedMs));
        
        // Handler breakdown
        sb.append("+------------------------------------------------------------------------+\n");
        sb.append("| HANDLER BREAKDOWN                                                      |\n");
        sb.append("+------------------------------------------------------------------------+\n");
        
        List<Map.Entry<String, AtomicLong>> sorted = new ArrayList<>(TOTAL_TIME.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()));
        
        int shown = 0;
        for (Map.Entry<String, AtomicLong> entry : sorted) {
            if (shown >= 10) break;
            String label = entry.getKey();
            if (label.startsWith("NET:")) continue;  // Skip network, shown separately
            
            long totalNs = entry.getValue().get();
            long count = CALL_COUNT.getOrDefault(label, new AtomicLong(1)).get();
            long maxMs = MAX_SINGLE_CALL.getOrDefault(label, 0L);
            long totalMs = totalNs / 1_000_000;
            long avgUs = count > 0 ? (totalNs / count / 1_000) : 0;
            
            String shortLabel = label.length() > 22 ? label.substring(0, 19) + "..." : label;
            sb.append(String.format("|   %-22s %4dms  %5d calls  %5dus avg  %3dms max   |\n",
                shortLabel, totalMs, count, avgUs, maxMs));
            shown++;
        }
        
        // Network latency
        if (!RTT_COUNT.isEmpty()) {
            sb.append("+------------------------------------------------------------------------+\n");
            sb.append("| NETWORK LATENCY (RTT)                                                  |\n");
            sb.append("+------------------------------------------------------------------------+\n");
            
            for (var entry : RTT_COUNT.entrySet()) {
                String type = entry.getKey();
                long count = entry.getValue().get();
                long totalNs = RTT_TOTAL.getOrDefault(type, new AtomicLong(0)).get();
                long maxMs = RTT_MAX.getOrDefault(type, 0L);
                long avgMs = count > 0 ? (totalNs / count / 1_000_000) : 0;
                
                sb.append(String.format("|   %-25s %5d reqs  %4dms avg  %4dms max          |\n",
                    type, count, avgMs, maxMs));
            }
        }
        
        // World diagnostics
        ClientWorld world = client.world;
        if (world != null) {
            sb.append("+------------------------------------------------------------------------+\n");
            sb.append("| WORLD DIAGNOSTICS                                                      |\n");
            sb.append("+------------------------------------------------------------------------+\n");
            
            int players = 0, mobs = 0, items = 0, projectiles = 0, other = 0;
            int removed = 0;
            Map<String, Integer> typeCounts = new java.util.HashMap<>();
            
            for (Entity entity : world.getEntities()) {
                if (entity.isRemoved()) {
                    removed++;
                    continue;
                }
                
                if (entity instanceof PlayerEntity) players++;
                else if (entity instanceof MobEntity) mobs++;
                else if (entity instanceof ItemEntity) items++;
                else if (entity instanceof ProjectileEntity) projectiles++;
                else other++;
                
                String type = entity.getType().getTranslationKey()
                    .replace("entity.minecraft.", "")
                    .replace("entity.the-virus-block.", "virus:");
                typeCounts.merge(type, 1, Integer::sum);
            }
            
            int total = players + mobs + items + projectiles + other;
            sb.append(String.format("|   Entities: %-5d (P:%-2d M:%-3d I:%-3d Proj:%-2d Other:%-4d)             |\n",
                total, players, mobs, items, projectiles, other));
            if (removed > 0) {
                sb.append(String.format("|   *** GHOST ENTITIES: %d removed but still in iterator! ***          |\n", removed));
            }
            sb.append(String.format("|   Loaded Chunks: %-4d  |  Render Distance: %-2d                        |\n",
                world.getChunkManager().getLoadedChunkCount(),
                client.options.getViewDistance().getValue()));
            
            // Show top entity types
            if (total > 10 && !typeCounts.isEmpty()) {
                sb.append("|   Top types:                                                           |\n");
                typeCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        String typeName = entry.getKey().length() > 18 
                            ? entry.getKey().substring(0, 15) + "..." : entry.getKey();
                        sb.append(String.format("|     -> %-18s: %4d                                    |\n",
                            typeName, entry.getValue()));
                    });
            }
        }
        
        sb.append("+========================================================================+");
        
        Logging.PROFILER.warn(sb.toString());
        
        reset();
    }
    
    private static void reset() {
        TOTAL_TIME.clear();
        CALL_COUNT.clear();
        MAX_SINGLE_CALL.clear();
        RTT_TOTAL.clear();
        RTT_COUNT.clear();
        RTT_MAX.clear();
        totalTickTimeNs = 0;
        modCodeTimeNs = 0;
        tickCount = 0;
        spikeCount = 0;
        worstTickMs = 0;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) reset();
    }
}
