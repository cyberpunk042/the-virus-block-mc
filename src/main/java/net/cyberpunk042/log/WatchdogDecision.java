package net.cyberpunk042.log;

/**
 * Result of watchdog spam check.
 */
public record WatchdogDecision(boolean suppress, String summary) {
    public static final WatchdogDecision ALLOW = new WatchdogDecision(false, null);
    public static final WatchdogDecision SUPPRESS = new WatchdogDecision(true, null);
}
