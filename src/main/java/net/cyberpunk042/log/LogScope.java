package net.cyberpunk042.log;

import java.util.function.Consumer;

/**
 * Deferred hierarchical logger for batched, structured output.
 * 
 * <p>LogScope accumulates log entries during a scope (like a render loop)
 * and emits them as a single structured tree when closed. This prevents
 * log spam while preserving diagnostic detail.</p>
 * 
 * <h3>Usage with try-with-resources:</h3>
 * <pre>
 * try (LogScope frame = Logging.FIELD.scope("render-frame")) {
 *     ScopeNode layer = frame.branch("layer:0");
 *     layer.kv("primitives", 5).kv("visible", true);
 *     
 *     for (Primitive p : primitives) {
 *         layer.branch("prim:" + p.id())
 *              .kv("vertices", count)
 *              .kv("color", color);
 *     }
 * } // Auto-emits on close
 * </pre>
 * 
 * <h3>Usage with lambda:</h3>
 * <pre>
 * Logging.FIELD.scoped("render-frame", frame -> {
 *     frame.branch("layer:0", layer -> {
 *         layer.kv("primitives", 5);
 *     });
 * });
 * </pre>
 * 
 * <h3>Output Example:</h3>
 * <pre>
 * [Field] render-frame
 * ├─ layer:0 {primitives=5, visible=true}
 * │  ├─ prim:sphere {vertices=1089, color=#FF0000}
 * │  └─ prim:cube {vertices=24, color=#00FF00}
 * └─ layer:1 {primitives=1}
 *    └─ prim:ring {vertices=256}
 * </pre>
 * 
 * @see ScopeNode
 * @see Channel#scope(String)
 */
public class LogScope implements AutoCloseable {
    
    /**
     * ThreadLocal to track the current active scope.
     * Enables automatic nesting of scopes across method/class boundaries.
     */
    private static final ThreadLocal<LogScope> CURRENT = new ThreadLocal<>();
    
    private final Channel channel;
    private final String topic;
    private final ScopeNode root;
    private final LogLevel level;
    private final long startTime;
    private final LogScope parent;  // Outer scope if auto-nested
    
    private volatile boolean flushed = false;
    private volatile boolean enabled = true;
    private boolean includeTiming = false;
    private boolean compactIfEmpty = true;
    private boolean autoNest = true;  // Whether to auto-nest into parent
    
    // =========================================================================
    // Construction
    // =========================================================================
    
    /**
     * Creates a new log scope.
     * Use {@link Channel#scope(String)} instead of calling directly.
     * 
     * @param channel The channel to emit to
     * @param name The scope name (root node name)
     * @param level The log level to emit at
     */
    LogScope(Channel channel, String name, LogLevel level) {
        this(channel, null, name, level);
    }
    
    /**
     * Creates a new log scope with topic.
     * 
     * <p>If another LogScope is already active on this thread, this scope
     * automatically becomes a child of it (auto-nesting). When this scope
     * closes, its tree is merged into the parent instead of emitting separately.</p>
     * 
     * @param channel The channel to emit to
     * @param topic Optional topic within the channel
     * @param name The scope name (root node name)
     * @param level The log level to emit at
     */
    LogScope(Channel channel, String topic, String name, LogLevel level) {
        this(channel, topic, name, level, false);
    }
    
    /**
     * Private constructor with ThreadLocal skip option.
     */
    private LogScope(Channel channel, String topic, String name, LogLevel level, boolean skipThreadLocal) {
        this.channel = channel;
        this.topic = topic;
        this.root = new ScopeNode(name);
        this.level = level;
        this.startTime = System.currentTimeMillis();
        
        if (skipThreadLocal) {
            this.parent = null;
        } else {
            // Auto-nesting: capture parent and register as current
            this.parent = CURRENT.get();
            CURRENT.set(this);
        }
    }
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    /**
     * Includes elapsed time in the output.
     * @return this (for chaining)
     */
    public LogScope withTiming() {
        this.includeTiming = true;
        return this;
    }
    
    /**
     * Disables this scope (no output on close).
     * Useful for conditional scoping.
     * @return this (for chaining)
     */
    public LogScope disabled() {
        this.enabled = false;
        return this;
    }
    
    /**
     * Always outputs, even if the tree is empty.
     * @return this (for chaining)
     */
    public LogScope alwaysOutput() {
        this.compactIfEmpty = false;
        return this;
    }
    
    /**
     * Disables auto-nesting for this scope.
     * The scope will emit independently even if a parent scope exists.
     * @return this (for chaining)
     */
    public LogScope independent() {
        this.autoNest = false;
        return this;
    }
    
    /**
     * Checks if this scope is nested inside another.
     * @return true if this scope has a parent scope
     */
    public boolean isNested() {
        return parent != null;
    }
    
    /**
     * Gets the parent scope if nested.
     * @return The parent scope, or null if this is a root scope
     */
    public LogScope parent() {
        return parent;
    }
    
    /**
     * Checks if this scope should actually log based on channel level.
     * @return true if logging is enabled for this scope
     */
    public boolean isEnabled() {
        if (!enabled) return false;
        LogLevel effective = topic != null 
            ? channel.effectiveLevel(topic) 
            : channel.level();
        return effective.includes(level);
    }
    
    // =========================================================================
    // Tree Building (delegate to root)
    // =========================================================================
    
    /**
     * Creates a child branch on the root node.
     * @param name Branch name
     * @return The new branch node
     */
    public ScopeNode branch(String name) {
        return root.branch(name);
    }
    
    /**
     * Creates a child branch with a lambda for building.
     * @param name Branch name
     * @param builder Lambda to build the branch
     * @return this (for chaining)
     */
    public LogScope branch(String name, Consumer<ScopeNode> builder) {
        ScopeNode node = root.branch(name);
        builder.accept(node);
        return this;
    }
    
    /**
     * Creates a leaf on the root node.
     * @param text Leaf text
     * @return this (for chaining)
     */
    public LogScope leaf(String text) {
        root.leaf(text);
        return this;
    }
    
    /**
     * Adds a key-value pair to the root node.
     * @param key Key name
     * @param value Value
     * @return this (for chaining)
     */
    public LogScope kv(String key, Object value) {
        root.kv(key, value);
        return this;
    }
    
    /**
     * Adds multiple key-value pairs to the root.
     * @param pairs Alternating key, value, key, value...
     * @return this (for chaining)
     */
    public LogScope kvs(Object... pairs) {
        root.kvs(pairs);
        return this;
    }
    
    /**
     * Adds a count to the root node.
     */
    public LogScope count(String name, int value) {
        root.count(name, value);
        return this;
    }
    
    /**
     * Marks the scope as having an error.
     */
    public LogScope error(String message) {
        root.error(message);
        return this;
    }
    
    /**
     * Gets the root node for direct manipulation.
     * @return The root ScopeNode
     */
    public ScopeNode root() {
        return root;
    }
    
    // =========================================================================
    // Output
    // =========================================================================
    
    /**
     * Manually flushes the scope output.
     * Normally called automatically on close().
     */
    public void flush() {
        if (flushed) return;
        flushed = true;
        
        if (!enabled) return;
        
        // Check if we should log based on level
        LogLevel effective = topic != null 
            ? channel.effectiveLevel(topic) 
            : channel.level();
        if (!effective.includes(level)) return;
        
        // Skip empty scopes if configured
        if (compactIfEmpty && !root.hasChildren()) {
            return;
        }
        
        // Add timing if requested
        if (includeTiming) {
            long elapsed = System.currentTimeMillis() - startTime;
            root.timing(elapsed);
        }
        
        // Render the tree
        String rendered = render();
        
        // Emit via standard pipeline
        LogOutput.emit(channel, topic, level, rendered, null, false);
    }
    
    /**
     * Renders the scope to a string without emitting.
     * @return The rendered tree
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(channel.label());
        if (topic != null) {
            sb.append(":").append(topic);
        }
        sb.append("] ");
        sb.append(root.render());
        return sb.toString();
    }
    
    /**
     * Renders a compact single-line summary.
     * @return Compact summary
     */
    public String renderCompact() {
        return "[" + channel.label() + "] " + root.renderCompact();
    }
    
    @Override
    public void close() {
        // Restore parent as current scope
        CURRENT.set(parent);
        
        if (parent != null && autoNest) {
            // Nested scope: merge our tree into parent instead of emitting
            // This combines cross-method/cross-class scopes automatically
            if (includeTiming) {
                long elapsed = System.currentTimeMillis() - startTime;
                root.timing(elapsed);
            }
            parent.root.addChild(root);
        } else {
            // Root scope: emit the combined tree
            flush();
        }
    }
    
    // =========================================================================
    // Static Utilities
    // =========================================================================
    
    /**
     * Gets the currently active scope on this thread, if any.
     * 
     * <p>Useful for code that wants to add branches to the current scope
     * without creating a new nested scope:</p>
     * <pre>
     * LogScope current = LogScope.current();
     * if (current != null) {
     *     current.branch("extra-info").kv("detail", value);
     * }
     * </pre>
     * 
     * @return The current active scope, or null if none
     */
    public static LogScope current() {
        return CURRENT.get();
    }
    
    /**
     * Gets the current scope, or a no-op scope if none exists.
     * Useful for unconditional branch additions.
     * 
     * @return The current scope or a no-op scope
     */
    public static LogScope currentOrNoop() {
        LogScope scope = CURRENT.get();
        return scope != null ? scope : noop();
    }
    
    /**
     * Returns a disabled/no-op scope for conditional logging.
     * All operations are no-ops, nothing is emitted.
     * 
     * @return A disabled LogScope (singleton)
     */
    public static LogScope noop() {
        return NoOpLogScope.INSTANCE;
    }
    
    /**
     * A no-operation scope that does nothing.
     * Does not participate in ThreadLocal nesting.
     */
    private static class NoOpLogScope extends LogScope {
        private static final NoOpLogScope INSTANCE = new NoOpLogScope();
        
        NoOpLogScope() {
            super(null, null, "noop", LogLevel.OFF, true);  // skipThreadLocal=true
        }
        
        @Override
        public ScopeNode branch(String name) { 
            return new ScopeNode(name); // Returns orphan node
        }
        
        @Override
        public LogScope branch(String name, Consumer<ScopeNode> builder) {
            return this;
        }
        
        @Override
        public LogScope leaf(String text) { return this; }
        
        @Override
        public LogScope kv(String key, Object value) { return this; }
        
        @Override
        public LogScope kvs(Object... pairs) { return this; }
        
        @Override
        public void flush() { /* no-op */ }
        
        @Override
        public void close() { /* no-op - don't touch ThreadLocal */ }
    }
}


