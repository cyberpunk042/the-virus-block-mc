package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;
import java.util.Collections;
import java.util.Set;

/**
 * Base class for anything that needs to be initialized.
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * public class MyRegistryNode extends InitNode {
 *     
 *     public MyRegistryNode() {
 *         super("my_registry", "My Registry");
 *     }
 *     
 *     @Override
 *     protected int load() {
 *         // Your loading code here
 *         MyRegistry.loadFromDisk();
 *         return MyRegistry.count();
 *     }
 * }
 * }</pre>
 * 
 * <h2>With Dependencies</h2>
 * <pre>{@code
 * public class ColorThemesNode extends InitNode {
 *     
 *     public ColorThemesNode() {
 *         super("color_themes", "Color Themes");
 *         // This node won't run until field_registry is complete:
 *         dependsOn("field_registry");
 *     }
 *     
 *     @Override
 *     protected int load() {
 *         // Safe to assume FieldRegistry is loaded
 *         return ColorThemeRegistry.count();
 *     }
 * }
 * }</pre>
 * 
 * <h2>With Hot-Reload</h2>
 * <pre>{@code
 * public class ConfigNode extends InitNode {
 *     
 *     public ConfigNode() {
 *         super("my_config", "My Config");
 *         enableReload(); // Allow this node to be reloaded at runtime
 *     }
 *     
 *     @Override
 *     protected int load() {
 *         ConfigLoader.load();
 *         return 1;
 *     }
 *     
 *     // Optional: Different behavior for reload
 *     @Override
 *     protected int reload() {
 *         ConfigLoader.reload();
 *         return 1;
 *     }
 * }
 * }</pre>
 * 
 * <h2>With Lifecycle Hooks</h2>
 * <pre>{@code
 * public class AdvancedNode extends InitNode {
 *     
 *     public AdvancedNode() {
 *         super("advanced", "Advanced Example");
 *     }
 *     
 *     @Override
 *     protected void beforeLoad() {
 *         // Called before load() - good for setup
 *     }
 *     
 *     @Override
 *     protected int load() {
 *         return doTheActualWork();
 *     }
 *     
 *     @Override
 *     protected void afterLoad(int loaded) {
 *         // Called after successful load() - good for logging
 *         Logging.REGISTRY.info("Loaded {} items", loaded);
 *     }
 *     
 *     @Override
 *     protected void onError(Throwable error) {
 *         // Called if load() throws - good for cleanup
 *         Logging.REGISTRY.error("Failed: {}", error.getMessage());
 *     }
 * }
 * }</pre>
 */
public abstract class InitNode {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // IDENTITY - What is this node?
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final String id;
    private final String displayName;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION - How should this node behave?
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final java.util.Set<String> dependencies = new java.util.HashSet<>();
    private boolean reloadable = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE - What's the current status?
    // ═══════════════════════════════════════════════════════════════════════════
    
    private InitState state = InitState.PENDING;
    private int loadedCount = 0;
    private long durationMs = 0;
    private Throwable lastError = null;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a new init node.
     * 
     * @param id          Unique identifier (e.g., "field_registry"). 
     *                    Use lowercase_with_underscores.
     * @param displayName Human-readable name (e.g., "Field Registry").
     *                    This shows up in logs and debug output.
     */
    protected InitNode(String id, String displayName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id cannot be null or blank");
        }
        if (!id.matches("[a-z][a-z0-9_]*")) {
            Logging.REGISTRY.warn("Node id '{}' should be lowercase_with_underscores", id);
        }
        this.id = id;
        this.displayName = displayName != null ? displayName : id;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS - Quick node creation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a simple init node from a lambda.
     * 
     * <p>Use this for quick one-liners that don't need full class definitions.
     * 
     * <pre>{@code
     * public static final InitNode MY_NODE = InitNode.simple(
     *     "my_thing", "My Thing",
     *     () -> {
     *         MyThing.init();
     *         return MyThing.count();
     *     }
     * );
     * }</pre>
     * 
     * @param id          Unique identifier
     * @param displayName Human-readable name
     * @param loader      Lambda that performs loading and returns item count
     * @return A new InitNode
     */
    public static InitNode simple(String id, String displayName, java.util.function.IntSupplier loader) {
        return new InitNode(id, displayName) {
            @Override
            protected int load() {
                return loader.getAsInt();
            }
        };
    }
    
    /**
     * Create a reloadable init node from lambdas.
     * 
     * <pre>{@code
     * public static final InitNode CONFIG = InitNode.reloadable(
     *     "config", "Configuration",
     *     () -> { Config.load(); return 1; },
     *     () -> { Config.reload(); return 1; }
     * );
     * }</pre>
     * 
     * @param id           Unique identifier
     * @param displayName  Human-readable name
     * @param loader       Lambda for initial load
     * @param reloader     Lambda for reload
     * @return A new reloadable InitNode
     */
    public static InitNode reloadable(String id, String displayName, 
            java.util.function.IntSupplier loader, 
            java.util.function.IntSupplier reloader) {
        return new InitNode(id, displayName) {
            {
                enableReload();
            }
            
            @Override
            protected int load() {
                return loader.getAsInt();
            }
            
            @Override
            protected int reload() {
                return reloader.getAsInt();
            }
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION METHODS - Call these in your constructor
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Declare that this node depends on another node.
     * 
     * <p>The dependency will be loaded first. If it fails, this node will be skipped.
     * 
     * <pre>{@code
     * public MyNode() {
     *     super("my_node", "My Node");
     *     dependsOn("field_registry");  // Must load first
     *     dependsOn("color_themes");    // Must also load first
     * }
     * }</pre>
     * 
     * @param nodeId The ID of the node this depends on
     * @return this (for chaining)
     */
    public InitNode dependsOn(String nodeId) {
        if (nodeId != null && !nodeId.isBlank()) {
            dependencies.add(nodeId);
        }
        return this;
    }
    
    /**
     * Enable hot-reload for this node.
     * 
     * <p>Once enabled, you can reload this node at runtime using {@code Init.reload("my_node")}.
     * 
     * <pre>{@code
     * public ConfigNode() {
     *     super("config", "Config");
     *     enableReload();
     * }
     * }</pre>
     * 
     * @return this (for chaining)
     */
    protected InitNode enableReload() {
        this.reloadable = true;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ABSTRACT METHOD - You MUST implement this
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Perform the actual loading work.
     * 
     * <p><b>You must implement this method.</b>
     * 
     * <pre>{@code
     * @Override
     * protected int load() {
     *     // Load your stuff
     *     MyRegistry.loadAll();
     *     
     *     // Return how many items were loaded (for logging)
     *     return MyRegistry.count();
     * }
     * }</pre>
     * 
     * @return The number of items loaded (shown in logs, use 0 or 1 if not applicable)
     */
    protected abstract int load();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL OVERRIDES - Customize behavior
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Perform reload (if different from initial load).
     * 
     * <p>Default implementation just calls {@link #load()}.
     * Override if you need different reload behavior.
     * 
     * <pre>{@code
     * @Override
     * protected int reload() {
     *     MyRegistry.clear();  // Clear existing data
     *     MyRegistry.loadAll(); // Reload from disk
     *     return MyRegistry.count();
     * }
     * }</pre>
     * 
     * @return The number of items loaded after reload
     */
    protected int reload() {
        return load(); // Default: same as initial load
    }
    
    /**
     * Called before load() starts.
     * 
     * <p>Good for setup, validation, or logging.
     */
    protected void beforeLoad() {
        // Override if needed
    }
    
    /**
     * Called after load() succeeds.
     * 
     * @param loaded The return value from load()
     */
    protected void afterLoad(int loaded) {
        // Override if needed
    }
    
    /**
     * Called if load() throws an exception.
     * 
     * <p>Good for cleanup or custom logging.
     * 
     * @param error The exception that was thrown
     */
    protected void onError(Throwable error) {
        // Override if needed
    }
    
    /**
     * Called before reload() starts.
     * 
     * <p>Good for cleanup before reload.
     */
    protected void beforeReload() {
        // Override if needed
    }
    
    /**
     * Called after reload() succeeds.
     * 
     * @param loaded The return value from reload()
     */
    protected void afterReload(int loaded) {
        // Override if needed
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL - Used by the orchestrator (don't call these directly)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Execute the load sequence. Called by the orchestrator.
     * @return Result of the load operation
     */
    final InitResult execute() {
        if (state != InitState.PENDING && state != InitState.STALE) {
            return new InitResult(this, state, loadedCount, durationMs, lastError);
        }
        
        state = InitState.RUNNING;
        long start = System.currentTimeMillis();
        
        try {
            beforeLoad();
            loadedCount = load();
            durationMs = System.currentTimeMillis() - start;
            afterLoad(loadedCount);
            state = InitState.COMPLETE;
            return new InitResult(this, state, loadedCount, durationMs, null);
            
        } catch (Throwable t) {
            durationMs = System.currentTimeMillis() - start;
            lastError = t;
            state = InitState.FAILED;
            onError(t);
            return new InitResult(this, state, 0, durationMs, t);
        }
    }
    
    /**
     * Execute the reload sequence. Called by the orchestrator.
     * @return Result of the reload operation
     */
    final InitResult executeReload() {
        if (!reloadable) {
            return new InitResult(this, state, loadedCount, 0, 
                new UnsupportedOperationException("Node does not support reload: " + id));
        }
        
        state = InitState.RUNNING;
        long start = System.currentTimeMillis();
        
        try {
            beforeReload();
            loadedCount = reload();
            durationMs = System.currentTimeMillis() - start;
            afterReload(loadedCount);
            state = InitState.COMPLETE;
            return new InitResult(this, state, loadedCount, durationMs, null);
            
        } catch (Throwable t) {
            durationMs = System.currentTimeMillis() - start;
            lastError = t;
            state = InitState.FAILED;
            onError(t);
            return new InitResult(this, state, 0, durationMs, t);
        }
    }
    
    /** 
     * Mark as needing reload due to dependency change.
     * Works on both COMPLETE and FAILED nodes.
     */
    final void markStale() {
        if (state == InitState.COMPLETE || state == InitState.FAILED) {
            state = InitState.STALE;
            lastError = null; // Clear previous error
        }
    }
    
    /**
     * Fully reset this node to PENDING state.
     * Clears all state including errors, counts, and timing.
     */
    final void resetToPending() {
        state = InitState.PENDING;
        loadedCount = 0;
        durationMs = 0;
        lastError = null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS - Check status
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** @return Unique identifier for this node */
    public String id() { return id; }
    
    /** @return Human-readable name */
    public String displayName() { return displayName; }
    
    /** @return Current state (PENDING, RUNNING, COMPLETE, FAILED, STALE) */
    public InitState state() { return state; }
    
    /** @return Number of items loaded (after completion) */
    public int loadedCount() { return loadedCount; }
    
    /** @return Time taken to load in milliseconds */
    public long durationMs() { return durationMs; }
    
    /** @return The last error if state is FAILED */
    public Throwable lastError() { return lastError; }
    
    /** @return Whether this node supports hot-reload */
    public boolean isReloadable() { return reloadable; }
    
    /** @return IDs of nodes this depends on */
    public Set<String> dependencies() { return Collections.unmodifiableSet(dependencies); }
    
    /** @return true if this node has completed successfully */
    public boolean isComplete() { return state == InitState.COMPLETE; }
    
    /** @return true if this node failed */
    public boolean isFailed() { return state == InitState.FAILED; }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", displayName, state);
    }
}
