package net.cyberpunk042.log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spam detection for log messages.
 * Tracks per-template rates and suppresses excessive logging.
 */
public final class LogWatchdog {
    
    private static volatile boolean enabled = true;
    private static volatile int perSecond = 50;
    private static volatile int perMinute = 500;
    private static volatile boolean suppress = true;
    
    private static final ConcurrentHashMap<String, Stats> stats = new ConcurrentHashMap<>();
    
    private static class Stats {
        final AtomicInteger secondCount = new AtomicInteger(0);
        final AtomicInteger minuteCount = new AtomicInteger(0);
        final AtomicLong lastSecond = new AtomicLong(0);
        final AtomicLong lastMinute = new AtomicLong(0);
        volatile boolean suppressed = false;
        volatile int suppressedCount = 0;
    }
    
    public static WatchdogDecision observe(String channel, String topic, String message) {
        if (!enabled) return WatchdogDecision.ALLOW;
        
        String key = buildKey(channel, topic, message);
        Stats s = stats.computeIfAbsent(key, k -> new Stats());
        
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        long currentMinute = now / 60000;
        
        // Reset counters on new time windows
        if (s.lastSecond.get() != currentSecond) {
            s.lastSecond.set(currentSecond);
            s.secondCount.set(0);
        }
        if (s.lastMinute.get() != currentMinute) {
            s.lastMinute.set(currentMinute);
            s.minuteCount.set(0);
            if (s.suppressed) {
                int count = s.suppressedCount;
                s.suppressed = false;
                s.suppressedCount = 0;
                return new WatchdogDecision(false, 
                    String.format("Suppressed %d messages for [%s]", count, key));
            }
        }
        
        int secCount = s.secondCount.incrementAndGet();
        int minCount = s.minuteCount.incrementAndGet();
        
        boolean shouldSuppress = suppress && (secCount > perSecond || minCount > perMinute);
        
        if (shouldSuppress) {
            if (!s.suppressed) {
                s.suppressed = true;
                s.suppressedCount = 1;
                return new WatchdogDecision(true, 
                    String.format("Rate limit exceeded for [%s], suppressing...", key));
            } else {
                s.suppressedCount++;
                return WatchdogDecision.SUPPRESS;
            }
        }
        
        return WatchdogDecision.ALLOW;
    }
    
    private static String buildKey(String channel, String topic, String message) {
        // Extract template (first 50 chars, normalized)
        String template = message.length() > 50 ? message.substring(0, 50) : message;
        template = template.replaceAll("\\d+", "#").replaceAll("\\[.*?\\]", "[...]");
        return channel + (topic != null ? ":" + topic : "") + ":" + template.hashCode();
    }
    
    public static void reset() {
        stats.clear();
    }
    
    public static void setEnabled(boolean value) { enabled = value; }
    public static void setThresholds(int perSec, int perMin) { perSecond = perSec; perMinute = perMin; }
    public static void setSuppress(boolean value) { suppress = value; }
    
    public static boolean isEnabled() { return enabled; }
    public static int perSecond() { return perSecond; }
    public static int perMinute() { return perMinute; }
    
    private LogWatchdog() {}
}
