package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;
import java.util.Optional;

/**
 * Fluent accessor for a single init node.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Access a node
 * NodeAccessor node = Init.node("color_themes");
 * 
 * // Check status
 * if (node.isComplete()) { ... }
 * 
 * // Reload
 * node.reload();
 * 
 * // Reset
 * node.reset();
 * 
 * // Listen
 * node.onComplete(() -> System.out.println("Themes loaded!"));
 * }</pre>
 */
public class NodeAccessor {
    
    private final InitStore store;
    private final InitOrchestrator orchestrator;
    private final String nodeId;
    
    NodeAccessor(InitStore store, InitOrchestrator orchestrator, String nodeId) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.nodeId = nodeId;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if this node exists.
     */
    public boolean exists() {
        return store.getNode(nodeId).isPresent();
    }
    
    /**
     * Get the underlying node object.
     */
    public Optional<InitNode> get() {
        return store.getNode(nodeId);
    }
    
    /**
     * Check if this node has completed successfully.
     */
    public boolean isComplete() {
        return store.isComplete(nodeId);
    }
    
    /**
     * Check if this node has failed.
     */
    public boolean isFailed() {
        return store.isFailed(nodeId);
    }
    
    /**
     * Get the current state.
     */
    public InitState state() {
        return store.getState(nodeId);
    }
    
    /**
     * Check if this node supports hot-reload.
     */
    public boolean isReloadable() {
        return get().map(InitNode::isReloadable).orElse(false);
    }
    
    /**
     * Get the loaded item count.
     */
    public int loadedCount() {
        return get().map(InitNode::loadedCount).orElse(0);
    }
    
    /**
     * Get the duration in milliseconds.
     */
    public long durationMs() {
        return get().map(InitNode::durationMs).orElse(0L);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reload this node.
     * 
     * <pre>{@code
     * Init.node("color_themes").reload();
     * }</pre>
     * 
     * @return true if reload succeeded
     */
    public boolean reload() {
        if (!exists()) {
            Logging.REGISTRY.warn("Cannot reload unknown node: {}", nodeId);
            return false;
        }
        return orchestrator.reload(nodeId);
    }
    
    /**
     * Reset this node (mark as STALE, needs re-execution).
     * 
     * <pre>{@code
     * Init.node("color_themes").reset();
     * // Node will be re-executed on next orchestrator.execute()
     * }</pre>
     */
    public NodeAccessor reset() {
        Optional<InitNode> opt = get();
        if (opt.isEmpty()) {
            Logging.REGISTRY.warn("Cannot reset unknown node: {}", nodeId);
            return this;
        }
        
        InitNode node = opt.get();
        node.markStale();
        store.fireEvent(InitEvent.stale(node));
        Logging.REGISTRY.debug("Reset node: {}", nodeId);
        
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Subscribe to this node completing.
     * 
     * <pre>{@code
     * Init.node("field_registry").onComplete(() -> {
     *     // FieldRegistry is now ready
     * });
     * }</pre>
     */
    public InitStore.Subscription onComplete(Runnable callback) {
        return store.subscribe(event -> {
            if (event.isComplete(nodeId)) {
                callback.run();
            }
        }, nodeId);
    }
    
    /**
     * Subscribe to this node completing (with event details).
     */
    public InitStore.Subscription onComplete(java.util.function.Consumer<InitEvent> callback) {
        return store.subscribe(event -> {
            if (event.isComplete(nodeId)) {
                callback.accept(event);
            }
        }, nodeId);
    }
    
    /**
     * Subscribe to this node failing.
     */
    public InitStore.Subscription onFailed(java.util.function.Consumer<Throwable> callback) {
        return store.subscribe(event -> {
            if (event.isFailed(nodeId)) {
                callback.accept(event.error());
            }
        }, nodeId);
    }
    
    @Override
    public String toString() {
        return String.format("Node[%s, %s, %d items]", nodeId, state(), loadedCount());
    }
}
