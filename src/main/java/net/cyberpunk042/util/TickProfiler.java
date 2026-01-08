package net.cyberpunk042.util;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple tick profiler to identify lag sources.
 * 
 * <p>Use {@link #start(String)} and {@link #end(String)} around code blocks
 * to track their execution time. Every 100 ticks, prints a summary.
 */
public final class TickProfiler {
    
    private static final Map<String, AtomicLong> TOTAL_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> CALL_COUNT = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> START_TIMES = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    private static int tickCount = 0;
    private static long lastLogTime = 0;
    private static volatile boolean enabled = true;
    
    private TickProfiler() {}
    
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TickProfiler::onTick);
        Logging.PROFILER.info("[TickProfiler] Initialized - will report every 5 seconds");
    }
    
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
    }
    
    private static void onTick(MinecraftServer server) {
        tickCount++;
        
        long now = System.currentTimeMillis();
        if (now - lastLogTime >= 5000) { // Log every 5 seconds
            logAndReset();
            lastLogTime = now;
        }
    }
    
    private static void logAndReset() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== TICK PROFILE (").append(tickCount).append(" ticks) ===\n");
        
        if (TOTAL_TIME.isEmpty()) {
            sb.append("  (no profiled code ran this interval)\n");
        } else {
            TOTAL_TIME.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .forEach(entry -> {
                    String label = entry.getKey();
                    long totalNs = entry.getValue().get();
                    long count = CALL_COUNT.getOrDefault(label, new AtomicLong(1)).get();
                    long totalMs = totalNs / 1_000_000;
                    long avgUs = count > 0 ? (totalNs / count / 1_000) : 0;
                    
                    // Show ALL entries, even sub-1ms ones
                    if (totalMs >= 1) {
                        sb.append(String.format("  %s: %dms total, %d calls, %dμs avg\n", 
                            label, totalMs, count, avgUs));
                    } else {
                        sb.append(String.format("  %s: %dμs total, %d calls, %dμs avg\n", 
                            label, totalNs / 1_000, count, avgUs));
                    }
                });
        }
        
        sb.append("===========================");
        Logging.PROFILER.warn(sb.toString());
        
        TOTAL_TIME.clear();
        CALL_COUNT.clear();
        tickCount = 0;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
