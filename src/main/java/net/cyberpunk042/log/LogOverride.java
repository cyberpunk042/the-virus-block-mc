package net.cyberpunk042.log;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global override layer for the logging system.
 * 
 * <p>Provides runtime control that sits ABOVE channel/topic configurations:
 * <ul>
 *   <li><b>Global Mute</b> - Silence all output except passthrough channels</li>
 *   <li><b>Minimum Level</b> - Only show logs at or above a threshold</li>
 *   <li><b>Level Redirect</b> - Route one level to another (e.g., TRACE → INFO)</li>
 *   <li><b>Passthrough</b> - Channels that bypass global mute</li>
 *   <li><b>Force Output</b> - Always output certain channels regardless of level</li>
 * </ul>
 * 
 * <h3>Typical Use Cases:</h3>
 * <pre>
 * // "Show me only errors"
 * LogOverride.setMinLevel(LogLevel.ERROR);
 * 
 * // "Mute everything except my trace output"
 * LogOverride.muteAll();
 * LogOverride.addPassthrough("field");  // Only field channel shows
 * 
 * // "Make TRACE visible without changing startup config"
 * LogOverride.redirect(LogLevel.TRACE, LogLevel.INFO);
 * 
 * // "Show everything for debugging"
 * LogOverride.clearAll();
 * </pre>
 * 
 * <h3>Precedence (highest to lowest):</h3>
 * <ol>
 *   <li>Force Output (always shows)</li>
 *   <li>Global Mute (blocks unless passthrough)</li>
 *   <li>Minimum Level (blocks below threshold)</li>
 *   <li>Level Redirect (changes output level)</li>
 *   <li>Channel/Topic level (normal config)</li>
 * </ol>
 * 
 * @see LogOutput
 * @see LogConfig
 */
public final class LogOverride {
    
    // =========================================================================
    // State
    // =========================================================================
    
    /** Global mute flag - when true, only passthrough channels emit */
    private static volatile boolean globalMute = false;
    
    /** Minimum level - if set, blocks anything below this level */
    private static volatile LogLevel minLevel = null;
    
    /** Level redirects - map one level to another for output */
    private static final Map<LogLevel, LogLevel> levelRedirects = new ConcurrentHashMap<>();
    
    /** Passthrough channels - bypass global mute */
    private static final Set<String> passthrough = ConcurrentHashMap.newKeySet();
    
    /** Force output channels - always output regardless of level */
    private static final Set<String> forceOutput = ConcurrentHashMap.newKeySet();
    
    /** Per-channel level overrides - override the channel's configured level */
    private static final Map<String, LogLevel> channelOverrides = new ConcurrentHashMap<>();
    
    // =========================================================================
    // Global Mute
    // =========================================================================
    
    /**
     * Mutes all logging output except passthrough channels.
     * Use {@link #addPassthrough(String)} to allow specific channels through.
     */
    public static void muteAll() {
        globalMute = true;
    }
    
    /**
     * Unmutes all logging (returns to normal behavior).
     */
    public static void unmuteAll() {
        globalMute = false;
    }
    
    /**
     * Checks if global mute is active.
     */
    public static boolean isMuted() {
        return globalMute;
    }
    
    // =========================================================================
    // Minimum Level
    // =========================================================================
    
    /**
     * Sets a minimum log level - anything below this is blocked.
     * 
     * @param level The minimum level (e.g., WARN blocks INFO, DEBUG, TRACE)
     */
    public static void setMinLevel(LogLevel level) {
        minLevel = level;
    }
    
    /**
     * Clears the minimum level (returns to channel-based filtering).
     */
    public static void clearMinLevel() {
        minLevel = null;
    }
    
    /**
     * Gets the current minimum level, or null if not set.
     */
    public static LogLevel minLevel() {
        return minLevel;
    }
    
    // =========================================================================
    // Level Redirect
    // =========================================================================
    
    /**
     * Redirects one log level to another for output.
     * 
     * <p>This is useful for making TRACE visible without changing startup config:</p>
     * <pre>
     * LogOverride.redirect(LogLevel.TRACE, LogLevel.INFO);
     * // Now TRACE messages appear as INFO in the console
     * </pre>
     * 
     * @param from The original level
     * @param to The level to output as
     */
    public static void redirect(LogLevel from, LogLevel to) {
        if (from != null && to != null) {
            levelRedirects.put(from, to);
        }
    }
    
    /**
     * Clears a level redirect.
     */
    public static void clearRedirect(LogLevel level) {
        levelRedirects.remove(level);
    }
    
    /**
     * Clears all level redirects.
     */
    public static void clearAllRedirects() {
        levelRedirects.clear();
    }
    
    /**
     * Gets the effective output level after applying redirects.
     * 
     * @param level The original level
     * @return The redirected level, or the original if no redirect
     */
    public static LogLevel effectiveOutputLevel(LogLevel level) {
        return levelRedirects.getOrDefault(level, level);
    }
    
    // =========================================================================
    // Passthrough (bypass mute)
    // =========================================================================
    
    /**
     * Adds a channel to the passthrough list.
     * Passthrough channels bypass global mute.
     * 
     * @param channelId The channel ID (e.g., "field", "render")
     */
    public static void addPassthrough(String channelId) {
        if (channelId != null) {
            passthrough.add(channelId);
        }
    }
    
    /**
     * Removes a channel from the passthrough list.
     */
    public static void removePassthrough(String channelId) {
        passthrough.remove(channelId);
    }
    
    /**
     * Clears all passthrough channels.
     */
    public static void clearPassthrough() {
        passthrough.clear();
    }
    
    /**
     * Checks if a channel is in the passthrough list.
     */
    public static boolean isPassthrough(String channelId) {
        return passthrough.contains(channelId);
    }
    
    // =========================================================================
    // Force Output (always show)
    // =========================================================================
    
    /**
     * Forces a channel to always output, regardless of level settings.
     * Use sparingly - this bypasses all filtering.
     * 
     * @param channelId The channel ID
     */
    public static void forceOutput(String channelId) {
        if (channelId != null) {
            forceOutput.add(channelId);
        }
    }
    
    /**
     * Removes a channel from force output.
     */
    public static void unforceOutput(String channelId) {
        forceOutput.remove(channelId);
    }
    
    /**
     * Clears all force output channels.
     */
    public static void clearForceOutput() {
        forceOutput.clear();
    }
    
    /**
     * Checks if a channel has force output enabled.
     */
    public static boolean isForceOutput(String channelId) {
        return forceOutput.contains(channelId);
    }
    
    // =========================================================================
    // Per-Channel Overrides
    // =========================================================================
    
    /**
     * Overrides the level for a specific channel.
     * This takes precedence over the channel's configured level.
     * 
     * @param channelId The channel ID
     * @param level The override level (or null to clear)
     */
    public static void setChannelLevel(String channelId, LogLevel level) {
        if (channelId != null) {
            if (level != null) {
                channelOverrides.put(channelId, level);
            } else {
                channelOverrides.remove(channelId);
            }
        }
    }
    
    /**
     * Clears the override for a specific channel.
     */
    public static void clearChannelLevel(String channelId) {
        channelOverrides.remove(channelId);
    }
    
    /**
     * Clears all channel overrides.
     */
    public static void clearAllChannelOverrides() {
        channelOverrides.clear();
    }
    
    /**
     * Gets the override level for a channel, or null if not overridden.
     */
    public static LogLevel getChannelOverride(String channelId) {
        return channelOverrides.get(channelId);
    }
    
    // =========================================================================
    // Decision Logic
    // =========================================================================
    
    /**
     * Determines if a log message should be emitted based on overrides.
     * 
     * @param channel The channel
     * @param level The log level
     * @return true if the message should be emitted
     */
    public static boolean shouldEmit(Channel channel, LogLevel level) {
        String channelId = channel.id();
        
        // Force output always wins
        if (forceOutput.contains(channelId)) {
            return true;
        }
        
        // Global mute blocks unless passthrough
        if (globalMute && !passthrough.contains(channelId)) {
            return false;
        }
        
        // Minimum level check
        if (minLevel != null && !minLevel.includes(level)) {
            return false;
        }
        
        // Check channel override
        LogLevel override = channelOverrides.get(channelId);
        if (override != null) {
            return override.includes(level);
        }
        
        // Fall through to normal channel logic
        return true;
    }
    
    /**
     * Gets the effective level for a channel, considering overrides.
     * 
     * @param channel The channel
     * @return The effective level (override or channel's level)
     */
    public static LogLevel effectiveLevel(Channel channel) {
        LogLevel override = channelOverrides.get(channel.id());
        return override != null ? override : channel.level();
    }
    
    // =========================================================================
    // Reset
    // =========================================================================
    
    /**
     * Clears all overrides and returns to normal behavior.
     */
    public static void clearAll() {
        globalMute = false;
        minLevel = null;
        levelRedirects.clear();
        passthrough.clear();
        forceOutput.clear();
        channelOverrides.clear();
    }
    
    // =========================================================================
    // Status
    // =========================================================================
    
    /**
     * Checks if any overrides are active.
     */
    public static boolean hasOverrides() {
        return globalMute 
            || minLevel != null 
            || !levelRedirects.isEmpty()
            || !passthrough.isEmpty()
            || !forceOutput.isEmpty()
            || !channelOverrides.isEmpty();
    }
    
    /**
     * Returns a summary of active overrides for debugging.
     */
    public static String summary() {
        if (!hasOverrides()) {
            return "No overrides active";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("LogOverride: ");
        
        if (globalMute) sb.append("[MUTED] ");
        if (minLevel != null) sb.append("[min=").append(minLevel).append("] ");
        if (!levelRedirects.isEmpty()) sb.append("[redirects=").append(levelRedirects.size()).append("] ");
        if (!passthrough.isEmpty()) sb.append("[passthrough=").append(passthrough).append("] ");
        if (!forceOutput.isEmpty()) sb.append("[force=").append(forceOutput).append("] ");
        if (!channelOverrides.isEmpty()) sb.append("[overrides=").append(channelOverrides.size()).append("] ");
        
        return sb.toString().trim();
    }
    
    private LogOverride() {}
}


