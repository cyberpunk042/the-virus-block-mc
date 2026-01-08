package net.cyberpunk042.util;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive tick profiler that captures:
 * 1. TOTAL server tick time (before vs after our handlers)
 * 2. Per-handler timing breakdown
 * 3. Call counts and averages
 * 4. Spike detection (single ticks that exceed threshold)
 * 5. Memory allocation tracking
 * 
 * This helps identify if lag is in OUR code or in VANILLA/OTHER MODS.
 */
public final class HierarchicalProfiler {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TIMING DATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final Map<String, AtomicLong> TOTAL_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> CALL_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, Long> MAX_SINGLE_CALL = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> START_TIMES = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Per-tick tracking
    private static long tickStartTime = 0;
    private static long tickEndTime = 0;
    private static long totalTickTimeNs = 0;
    private static long modCodeTimeNs = 0;  // Time in OUR profiled code
    private static int tickCount = 0;
    private static int spikeCount = 0;
    private static long worstTickMs = 0;
    private static String worstTickLabel = "";
    
    // Memory tracking
    private static long memoryAtStart = 0;
    private static long peakMemoryDelta = 0;
    
    // Config
    private static final long SPIKE_THRESHOLD_MS = 100; // Log individual ticks over 100ms
    private static final long REPORT_INTERVAL_MS = 5000;
    private static long lastReportTime = 0;
    
    private static volatile boolean enabled = true;
    
    private HierarchicalProfiler() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(HierarchicalProfiler::onTickStart);
        ServerTickEvents.END_SERVER_TICK.register(HierarchicalProfiler::onTickEnd);
        Logging.PROFILER.info("[HierarchicalProfiler] Initialized - comprehensive tick analysis enabled");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void onTickStart(MinecraftServer server) {
        if (!enabled) return;
        tickStartTime = System.nanoTime();
        memoryAtStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    private static void onTickEnd(MinecraftServer server) {
        if (!enabled) return;
        tickEndTime = System.nanoTime();
        long tickTimeNs = tickEndTime - tickStartTime;
        long tickTimeMs = tickTimeNs / 1_000_000;
        
        totalTickTimeNs += tickTimeNs;
        tickCount++;
        
        // Memory tracking
        long memoryNow = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryDelta = memoryNow - memoryAtStart;
        if (memoryDelta > peakMemoryDelta) {
            peakMemoryDelta = memoryDelta;
        }
        
        // Spike detection
        if (tickTimeMs > SPIKE_THRESHOLD_MS) {
            spikeCount++;
            if (tickTimeMs > worstTickMs) {
                worstTickMs = tickTimeMs;
                worstTickLabel = findWorstHandler();
            }
            
            // Log spike immediately
            Logging.PROFILER.warn("[SPIKE] Tick took {}ms! Top handler: {} | Mod code: {}ms of that",
                tickTimeMs, findWorstHandler(), modCodeTimeNs / 1_000_000);
        }
        
        // Periodic report
        long now = System.currentTimeMillis();
        if (now - lastReportTime >= REPORT_INTERVAL_MS) {
            generateReport(server);
            lastReportTime = now;
        }
        
        // Reset per-tick mod code counter
        modCodeTimeNs = 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILING API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Start timing a code section.
     */
    public static void start(String label) {
        if (!enabled) return;
        START_TIMES.get().put(label, System.nanoTime());
    }
    
    /**
     * End timing a code section and record the duration.
     */
    public static void end(String label) {
        if (!enabled) return;
        Long startTime = START_TIMES.get().remove(label);
        if (startTime == null) return;
        
        long elapsed = System.nanoTime() - startTime;
        
        // Add to totals
        TOTAL_TIME.computeIfAbsent(label, k -> new AtomicLong()).addAndGet(elapsed);
        CALL_COUNT.computeIfAbsent(label, k -> new AtomicLong()).incrementAndGet();
        modCodeTimeNs += elapsed;
        
        // Track max single call
        long elapsedMs = elapsed / 1_000_000;
        MAX_SINGLE_CALL.compute(label, (k, v) -> v == null ? elapsedMs : Math.max(v, elapsedMs));
    }
    
    /**
     * Wraps a runnable with profiling.
     */
    public static void profile(String label, Runnable action) {
        start(label);
        try {
            action.run();
        } finally {
            end(label);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void generateReport(MinecraftServer server) {
        if (tickCount == 0) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("+====================================================================+\n");
        sb.append("|            HIERARCHICAL TICK PROFILE                              |\n");
        sb.append("+====================================================================+\n");
        
        // Summary stats
        long avgTickMs = totalTickTimeNs / tickCount / 1_000_000;
        long modTotalMs = TOTAL_TIME.values().stream().mapToLong(AtomicLong::get).sum() / 1_000_000;
        long unaccountedMs = (totalTickTimeNs / 1_000_000) - modTotalMs;
        double tps = tickCount / (REPORT_INTERVAL_MS / 1000.0);
        double modPercent = totalTickTimeNs > 0 ? (modTotalMs * 100.0 / (totalTickTimeNs / 1_000_000)) : 0;
        
        sb.append(String.format("| Ticks: %-4d | Avg: %-4dms | TPS: %-5.1f | Spikes: %-3d          |\n",
            tickCount, avgTickMs, tps, spikeCount));
        sb.append(String.format("| Mod Code: %-4dms (%.1f%%) | Unaccounted: %-5dms (%.1f%%)      |\n",
            modTotalMs, modPercent, unaccountedMs, 100.0 - modPercent));
        sb.append(String.format("| Worst Tick: %-4dms | Peak Mem Delta: %-5.1fMB              |\n",
            worstTickMs, peakMemoryDelta / 1024.0 / 1024.0));
        
        sb.append("+--------------------------------------------------------------------+\n");
        sb.append("| HANDLER BREAKDOWN (sorted by total time)                          |\n");
        sb.append("+--------------------------------------------------------------------+\n");
        
        // Sort by total time descending
        List<Map.Entry<String, AtomicLong>> sorted = new ArrayList<>(TOTAL_TIME.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()));
        
        int shown = 0;
        for (Map.Entry<String, AtomicLong> entry : sorted) {
            if (shown >= 15) break; // Limit to top 15
            String label = entry.getKey();
            long totalNs = entry.getValue().get();
            long count = CALL_COUNT.getOrDefault(label, new AtomicLong(1)).get();
            long maxMs = MAX_SINGLE_CALL.getOrDefault(label, 0L);
            long totalMs = totalNs / 1_000_000;
            long avgUs = count > 0 ? (totalNs / count / 1_000) : 0;
            
            // Only show if > 0ms or top 10
            if (totalMs > 0 || shown < 10) {
                String shortLabel = label.length() > 25 ? label.substring(0, 22) + "..." : label;
                sb.append(String.format("| %-25s %4dms %5d calls %5dus avg %3dms max |\n",
                    shortLabel, totalMs, count, avgUs, maxMs));
                shown++;
            }
        }
        
        if (sorted.isEmpty()) {
            sb.append("| (no profiled handlers ran this interval)                          |\n");
        }
        
        // World info with entity breakdown
        sb.append("+--------------------------------------------------------------------+\n");
        sb.append("| WORLD STATUS & DIAGNOSTICS                                         |\n");
        sb.append("+--------------------------------------------------------------------+\n");
        
        for (ServerWorld world : server.getWorlds()) {
            String dimName = world.getRegistryKey().getValue().getPath();
            if (dimName.length() > 12) dimName = dimName.substring(0, 9) + "...";
            
            // Count entities by type
            Map<String, Integer> entityCounts = new HashMap<>();
            long totalEntities = 0;
            try {
                for (var entity : world.iterateEntities()) {
                    totalEntities++;
                    String type = entity.getType().getTranslationKey();
                    // Simplify type name
                    type = type.replace("entity.minecraft.", "").replace("entity.", "");
                    if (type.length() > 15) type = type.substring(0, 12) + "...";
                    entityCounts.merge(type, 1, Integer::sum);
                }
            } catch (Exception e) { /* ignore */ }
            
            int loadedChunks = world.getChunkManager().getLoadedChunkCount();
            
            // Get mod-specific counts
            int growthBlocks = net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAnyInWorld(world) 
                ? net.cyberpunk042.collision.GrowthCollisionMixinHelper.countActiveGrowth(world) : 0;
            int virusSources = 0;
            try {
                var state = net.cyberpunk042.infection.VirusWorldState.get(world);
                virusSources = state.hasVirusSources() ? state.getVirusSources().size() : 0;
            } catch (Exception e) { /* ignore */ }
            
            sb.append(String.format("| %-12s Ent:%-5d Chunks:%-4d Growth:%-3d Sources:%-2d |\n",
                dimName, totalEntities, loadedChunks, growthBlocks, virusSources));
            
            // Show top 3 entity types
            if (!entityCounts.isEmpty() && totalEntities > 100) {
                List<Map.Entry<String, Integer>> topTypes = entityCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .toList();
                for (var entry : topTypes) {
                    sb.append(String.format("|   -> %-18s: %5d                              |\n",
                        entry.getKey(), entry.getValue()));
                }
            }
        }
        
        sb.append("+====================================================================+");
        
        Logging.PROFILER.warn(sb.toString());
        
        // Reset for next interval
        reset();
    }
    
    private static String findWorstHandler() {
        return TOTAL_TIME.entrySet().stream()
            .max(Comparator.comparingLong(e -> e.getValue().get()))
            .map(Map.Entry::getKey)
            .orElse("(none)");
    }
    
    private static void reset() {
        TOTAL_TIME.clear();
        CALL_COUNT.clear();
        MAX_SINGLE_CALL.clear();
        totalTickTimeNs = 0;
        modCodeTimeNs = 0;
        tickCount = 0;
        spikeCount = 0;
        worstTickMs = 0;
        worstTickLabel = "";
        peakMemoryDelta = 0;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) reset();
    }
}
