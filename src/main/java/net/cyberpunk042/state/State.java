package net.cyberpunk042.state;

import net.cyberpunk042.log.Logging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Global observable state store.
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Set a value
 * State.set("gui.activeTab", "advanced");
 * 
 * // Get a value
 * String tab = State.getString("gui.activeTab");
 * 
 * // Watch for changes
 * Runnable unsubscribe = State.watch("gui.activeTab", newValue -> {
 *     updateUI();
 * });
 * 
 * // Later, stop watching
 * unsubscribe.run();
 * }</pre>
 * 
 * <h2>Common Patterns</h2>
 * <pre>{@code
 * // With default value (no null checks)
 * int zone = State.get("gui.forceModal.selectedZone", 0);
 * 
 * // Batch updates
 * State.setAll(Map.of(
 *     "gui.modal.open", true,
 *     "gui.modal.type", "force"
 * ));
 * 
 * // Debug - see all state
 * State.dump();
 * }</pre>
 * 
 * <h2>Path Convention</h2>
 * <pre>
 * init.*      - Initialization state (progress, stages)
 * gui.*       - UI state (tabs, modals, panels)
 * data.*      - Domain data (shapes, animations, etc.)
 * </pre>
 */
public final class State {
    
    private static final Map<String, Object> store = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<Object>>> watchers = new ConcurrentHashMap<>();
    
    private State() {} // Static access only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE - The essentials
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get a value by path.
     * 
     * <pre>{@code
     * String tab = State.get("gui.activeTab");
     * ForceConfig cfg = State.get("gui.forceModal.config");
     * }</pre>
     * 
     * @param path Dot-separated path (e.g., "gui.activeTab")
     * @return The value, or null if not set
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String path) {
        return (T) store.get(path);
    }
    
    /**
     * Set a value by path. Fires watchers if value changed.
     * 
     * <pre>{@code
     * State.set("gui.activeTab", "advanced");
     * State.set("gui.modal.open", true);
     * }</pre>
     * 
     * @param path  Dot-separated path
     * @param value The value to set (can be any object)
     */
    public static void set(String path, Object value) {
        Object old = store.put(path, value);
        if (!Objects.equals(old, value)) {
            fire(path, value);
        }
    }
    
    /**
     * Watch a path for changes. Returns an unsubscribe function.
     * 
     * <pre>{@code
     * Runnable unsub = State.watch("gui.activeTab", newValue -> {
     *     System.out.println("Tab changed to: " + newValue);
     * });
     * 
     * // Later, stop watching:
     * unsub.run();
     * }</pre>
     * 
     * @param path     The path to watch
     * @param callback Called when value changes
     * @return A Runnable that unsubscribes when called
     */
    public static Runnable watch(String path, Consumer<Object> callback) {
        watchers.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(callback);
        return () -> {
            List<Consumer<Object>> list = watchers.get(path);
            if (list != null) list.remove(callback);
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE - Typed getters and defaults
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get a value with a default fallback.
     * 
     * <pre>{@code
     * int zone = State.get("gui.selectedZone", 0);  // Returns 0 if not set
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String path, T defaultValue) {
        Object v = store.get(path);
        return v != null ? (T) v : defaultValue;
    }
    
    /**
     * Get an int value (returns 0 if not set or not a number).
     */
    public static int getInt(String path) {
        Object v = store.get(path);
        return v instanceof Number n ? n.intValue() : 0;
    }
    
    /**
     * Get a float value (returns 0 if not set or not a number).
     */
    public static float getFloat(String path) {
        Object v = store.get(path);
        return v instanceof Number n ? n.floatValue() : 0f;
    }
    
    /**
     * Get a double value (returns 0 if not set or not a number).
     */
    public static double getDouble(String path) {
        Object v = store.get(path);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
    
    /**
     * Get a boolean value (returns false if not set or not a boolean).
     */
    public static boolean getBool(String path) {
        Object v = store.get(path);
        return v instanceof Boolean b && b;
    }
    
    /**
     * Get a String value (calls toString if not null).
     */
    public static String getString(String path) {
        Object v = store.get(path);
        return v != null ? v.toString() : null;
    }
    
    /**
     * Get a String with default.
     */
    public static String getString(String path, String defaultValue) {
        Object v = store.get(path);
        return v != null ? v.toString() : defaultValue;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BULK OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Set multiple values at once.
     * 
     * <pre>{@code
     * State.setAll(Map.of(
     *     "gui.modal.open", true,
     *     "gui.modal.type", "force",
     *     "gui.modal.data", config
     * ));
     * }</pre>
     */
    public static void setAll(Map<String, Object> values) {
        values.forEach(State::set);
    }
    
    /**
     * Get all values under a prefix.
     * 
     * <pre>{@code
     * Map<String, Object> guiState = State.getAll("gui");
     * // Returns: {"gui.activeTab": "advanced", "gui.modal.open": false, ...}
     * }</pre>
     */
    public static Map<String, Object> getAll(String prefix) {
        String p = prefix.endsWith(".") ? prefix : prefix + ".";
        Map<String, Object> result = new LinkedHashMap<>();
        store.forEach((k, v) -> {
            if (k.startsWith(p) || k.equals(prefix)) {
                result.put(k, v);
            }
        });
        return result;
    }
    
    /**
     * Remove a value and fire watchers with null.
     */
    public static void remove(String path) {
        Object old = store.remove(path);
        if (old != null) {
            fire(path, null);
        }
    }
    
    /**
     * Remove all values under a prefix.
     */
    public static void removeAll(String prefix) {
        String p = prefix.endsWith(".") ? prefix : prefix + ".";
        List<String> toRemove = new ArrayList<>();
        store.keySet().forEach(k -> {
            if (k.startsWith(p) || k.equals(prefix)) {
                toRemove.add(k);
            }
        });
        toRemove.forEach(State::remove);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if a path exists.
     */
    public static boolean has(String path) {
        return store.containsKey(path);
    }
    
    /**
     * Get number of stored values.
     */
    public static int size() {
        return store.size();
    }
    
    /**
     * Get all stored paths.
     */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(store.keySet());
    }
    
    /**
     * Clear all state and watchers.
     * Useful for testing or full reset.
     */
    public static void clear() {
        store.clear();
        watchers.clear();
        Logging.STATE.debug("[State] Cleared all state");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Log all current state to console.
     * 
     * <pre>
     * [State] 12 entries:
     *   gui.activeTab = advanced
     *   gui.modal.open = false
     *   init.progress = 0.85
     *   init.stage = Field System
     * </pre>
     */
    public static void dump() {
        Logging.STATE.info("[State] {} entries:", store.size());
        store.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> Logging.STATE.info("  {} = {}", e.getKey(), e.getValue()));
    }
    
    /**
     * Log state under a specific prefix.
     */
    public static void dump(String prefix) {
        Map<String, Object> subset = getAll(prefix);
        Logging.STATE.info("[State] {} entries under '{}':", subset.size(), prefix);
        subset.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> Logging.STATE.info("  {} = {}", e.getKey(), e.getValue()));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void fire(String path, Object value) {
        List<Consumer<Object>> list = watchers.get(path);
        if (list != null && !list.isEmpty()) {
            for (Consumer<Object> callback : list) {
                try {
                    callback.accept(value);
                } catch (Exception e) {
                    Logging.STATE.warn("[State] Error in watcher for '{}': {}", path, e.getMessage());
                }
            }
        }
    }
}
