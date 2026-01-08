package net.cyberpunk042.command.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Auto-registered defaults from CommandKnob definitions.
 * 
 * <p>When you call {@code .defaultValue(x)} on a CommandKnob builder,
 * the default is automatically registered here.
 * 
 * <p>This replaces the manual CommandDefaults.java - defaults are now
 * defined with the knob, not in a separate file.
 */
public final class CommandKnobDefaults {
    
    private static final Map<String, Object> DEFAULTS = new HashMap<>();
    
    private CommandKnobDefaults() {}
    
    /**
     * Register a default value (called automatically by CommandKnob builders).
     */
    public static void register(String path, Object value) {
        DEFAULTS.put(path, value);
    }
    
    /**
     * Get the default value for a path.
     */
    public static Object get(String path) {
        return DEFAULTS.get(path);
    }
    
    /**
     * Get the default value with fallback.
     */
    public static Object getOrDefault(String path, Object fallback) {
        return DEFAULTS.getOrDefault(path, fallback);
    }
    
    /**
     * Check if a path has a registered default.
     */
    public static boolean has(String path) {
        return DEFAULTS.containsKey(path);
    }
    
    /**
     * Get all registered paths.
     */
    public static Set<String> paths() {
        return DEFAULTS.keySet();
    }
    
    /**
     * Format a default value for display.
     */
    public static String format(String path) {
        Object value = DEFAULTS.get(path);
        if (value == null) return "unknown";
        if (value instanceof Boolean b) return b ? "enabled" : "disabled";
        if (value instanceof String s) return s.toLowerCase();
        return String.valueOf(value);
    }
    
    /**
     * Get count of registered defaults.
     */
    public static int count() {
        return DEFAULTS.size();
    }
}
