package net.cyberpunk042.util;

import net.cyberpunk042.log.Logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Network performance profiler for tracking:
 * - Packet codec encode/decode times
 * - Round-trip times (RTT) for request/response patterns
 * - Packet counts and sizes
 * - Network latency statistics
 */
public final class NetworkProfiler {
    
    // ========================================================================
    // CODEC TIMING
    // ========================================================================
    
    private static final Map<String, AtomicLong> ENCODE_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> ENCODE_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> DECODE_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> DECODE_COUNT = new ConcurrentHashMap<>();
    
    // ========================================================================
    // RTT TRACKING
    // ========================================================================
    
    private static final Map<String, Long> PENDING_REQUESTS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> RTT_TOTAL = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> RTT_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, Long> RTT_MAX = new ConcurrentHashMap<>();
    private static final Map<String, Long> RTT_MIN = new ConcurrentHashMap<>();
    
    // ========================================================================
    // PACKET STATS
    // ========================================================================
    
    private static final Map<String, AtomicLong> PACKET_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> PACKET_BYTES = new ConcurrentHashMap<>();
    
    private static volatile boolean enabled = true;
    
    private NetworkProfiler() {}
    
    // ========================================================================
    // CODEC PROFILING
    // ========================================================================
    
    /**
     * Start timing an encode operation.
     */
    public static long startEncode(String packetType) {
        if (!enabled) return 0;
        return System.nanoTime();
    }
    
    /**
     * End timing an encode operation.
     */
    public static void endEncode(String packetType, long startTime, int byteSize) {
        if (!enabled || startTime == 0) return;
        long elapsed = System.nanoTime() - startTime;
        ENCODE_TIME.computeIfAbsent(packetType, k -> new AtomicLong()).addAndGet(elapsed);
        ENCODE_COUNT.computeIfAbsent(packetType, k -> new AtomicLong()).incrementAndGet();
        PACKET_BYTES.computeIfAbsent(packetType, k -> new AtomicLong()).addAndGet(byteSize);
    }
    
    /**
     * Start timing a decode operation.
     */
    public static long startDecode(String packetType) {
        if (!enabled) return 0;
        return System.nanoTime();
    }
    
    /**
     * End timing a decode operation.
     */
    public static void endDecode(String packetType, long startTime) {
        if (!enabled || startTime == 0) return;
        long elapsed = System.nanoTime() - startTime;
        DECODE_TIME.computeIfAbsent(packetType, k -> new AtomicLong()).addAndGet(elapsed);
        DECODE_COUNT.computeIfAbsent(packetType, k -> new AtomicLong()).incrementAndGet();
    }
    
    // ========================================================================
    // RTT TRACKING (Request/Response patterns)
    // ========================================================================
    
    /**
     * Mark the start of a request-response cycle.
     * @param requestType Type of request (e.g., "ProfileLoad")
     * @param requestId Unique ID to correlate with response
     */
    public static void startRequest(String requestType, String requestId) {
        if (!enabled) return;
        PENDING_REQUESTS.put(requestType + ":" + requestId, System.nanoTime());
    }
    
    /**
     * Mark the end of a request-response cycle when response is received.
     */
    public static void endRequest(String requestType, String requestId) {
        if (!enabled) return;
        String key = requestType + ":" + requestId;
        Long startTime = PENDING_REQUESTS.remove(key);
        if (startTime == null) return;
        
        long rtt = System.nanoTime() - startTime;
        long rttMs = rtt / 1_000_000;
        
        RTT_TOTAL.computeIfAbsent(requestType, k -> new AtomicLong()).addAndGet(rtt);
        RTT_COUNT.computeIfAbsent(requestType, k -> new AtomicLong()).incrementAndGet();
        RTT_MAX.compute(requestType, (k, v) -> v == null ? rttMs : Math.max(v, rttMs));
        RTT_MIN.compute(requestType, (k, v) -> v == null ? rttMs : Math.min(v, rttMs));
    }
    
    // ========================================================================
    // SIMPLE PACKET TRACKING
    // ========================================================================
    
    /**
     * Track a packet being sent/received.
     */
    public static void trackPacket(String direction, String packetType, int bytes) {
        if (!enabled) return;
        String key = direction + ":" + packetType;
        PACKET_COUNT.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        PACKET_BYTES.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(bytes);
    }
    
    public static void trackSend(String packetType, int bytes) {
        trackPacket("S2C", packetType, bytes);
    }
    
    public static void trackReceive(String packetType, int bytes) {
        trackPacket("C2S", packetType, bytes);
    }
    
    // ========================================================================
    // REPORT GENERATION
    // ========================================================================
    
    /**
     * Generate a network stats report section for the main profiler.
     */
    public static String generateReport() {
        if (!enabled) return "";
        
        StringBuilder sb = new StringBuilder();
        
        // Codec timing
        if (!ENCODE_COUNT.isEmpty() || !DECODE_COUNT.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------+\n");
            sb.append("| NETWORK CODEC TIMING                                                     |\n");
            sb.append("+--------------------------------------------------------------------------+\n");
            
            for (String type : ENCODE_COUNT.keySet()) {
                long encCount = ENCODE_COUNT.getOrDefault(type, new AtomicLong(0)).get();
                long encTimeNs = ENCODE_TIME.getOrDefault(type, new AtomicLong(0)).get();
                long decCount = DECODE_COUNT.getOrDefault(type, new AtomicLong(0)).get();
                long decTimeNs = DECODE_TIME.getOrDefault(type, new AtomicLong(0)).get();
                long bytes = PACKET_BYTES.getOrDefault(type, new AtomicLong(0)).get();
                
                long encAvgUs = encCount > 0 ? (encTimeNs / encCount / 1000) : 0;
                long decAvgUs = decCount > 0 ? (decTimeNs / decCount / 1000) : 0;
                
                String shortType = type.length() > 18 ? type.substring(0, 15) + "..." : type;
                sb.append(String.format("|   %-18s Enc: %4dus x%-4d  Dec: %4dus x%-4d  %5.1fKB |\n",
                    shortType, encAvgUs, encCount, decAvgUs, decCount, bytes / 1024.0));
            }
        }
        
        // RTT stats
        if (!RTT_COUNT.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------+\n");
            sb.append("| NETWORK LATENCY (RTT)                                                    |\n");
            sb.append("+--------------------------------------------------------------------------+\n");
            
            for (String type : RTT_COUNT.keySet()) {
                long count = RTT_COUNT.getOrDefault(type, new AtomicLong(0)).get();
                long totalNs = RTT_TOTAL.getOrDefault(type, new AtomicLong(0)).get();
                long maxMs = RTT_MAX.getOrDefault(type, 0L);
                long minMs = RTT_MIN.getOrDefault(type, 0L);
                long avgMs = count > 0 ? (totalNs / count / 1_000_000) : 0;
                
                String shortType = type.length() > 20 ? type.substring(0, 17) + "..." : type;
                sb.append(String.format("|   %-20s %4d reqs  Avg: %4dms  Min: %3dms  Max: %4dms |\n",
                    shortType, count, avgMs, minMs, maxMs));
            }
        }
        
        // Packet stats
        if (!PACKET_COUNT.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------+\n");
            sb.append("| PACKET TRAFFIC                                                           |\n");
            sb.append("+--------------------------------------------------------------------------+\n");
            
            // Sort by count descending
            PACKET_COUNT.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(8)
                .forEach(entry -> {
                    String key = entry.getKey();
                    long count = entry.getValue().get();
                    long bytes = PACKET_BYTES.getOrDefault(key, new AtomicLong(0)).get();
                    String shortKey = key.length() > 28 ? key.substring(0, 25) + "..." : key;
                    sb.append(String.format("|   %-28s %6d packets  %8.1fKB      |\n",
                        shortKey, count, bytes / 1024.0));
                });
        }
        
        return sb.toString();
    }
    
    /**
     * Clear all stats for a fresh interval.
     */
    public static void reset() {
        ENCODE_TIME.clear();
        ENCODE_COUNT.clear();
        DECODE_TIME.clear();
        DECODE_COUNT.clear();
        RTT_TOTAL.clear();
        RTT_COUNT.clear();
        RTT_MAX.clear();
        RTT_MIN.clear();
        PACKET_COUNT.clear();
        PACKET_BYTES.clear();
        // Note: PENDING_REQUESTS is intentionally NOT cleared (in-flight requests)
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) reset();
    }
}
