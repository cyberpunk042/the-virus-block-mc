package net.cyberpunk042.init;

/**
 * An event that fires when initialization state changes.
 * 
 * <h2>Usage in Subscription</h2>
 * <pre>{@code
 * Init.store().subscribe(event -> {
 *     // Check what happened
 *     switch (event.type()) {
 *         case STARTED -> System.out.println("Loading: " + event.nodeId());
 *         case COMPLETED -> System.out.println("Loaded: " + event.loadedCount() + " items");
 *         case FAILED -> System.err.println("Error: " + event.error().getMessage());
 *     }
 *     
 *     // Or use convenience methods
 *     if (event.isComplete("field_registry")) {
 *         // FieldRegistry is now ready to use
 *     }
 * });
 * }</pre>
 */
public record InitEvent(
    /** Type of event */
    EventType type,
    
    /** ID of the node this event is about */
    String nodeId,
    
    /** Display name of the node */
    String displayName,
    
    /** Current state of the node */
    InitState state,
    
    /** Number of items loaded (for COMPLETED events) */
    int loadedCount,
    
    /** Duration in ms (for COMPLETED/FAILED events) */
    long durationMs,
    
    /** Error if failed */
    Throwable error
) {
    
    /**
     * Event types.
     */
    public enum EventType {
        /** Node execution started */
        STARTED,
        
        /** Node completed successfully */
        COMPLETED,
        
        /** Node failed with error */
        FAILED,
        
        /** Node marked stale (needs reload) */
        STALE,
        
        /** Reload started */
        RELOAD_STARTED,
        
        /** Reload completed */
        RELOAD_COMPLETED
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE CHECKS - Makes subscription handlers cleaner
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if this event is about a specific node completing successfully.
     * 
     * <pre>{@code
     * if (event.isComplete("field_registry")) {
     *     // FieldRegistry is ready!
     * }
     * }</pre>
     */
    public boolean isComplete(String id) {
        return nodeId.equals(id) && type == EventType.COMPLETED;
    }
    
    /**
     * Check if this event is about a specific node failing.
     */
    public boolean isFailed(String id) {
        return nodeId.equals(id) && type == EventType.FAILED;
    }
    
    /**
     * Check if this is any kind of completion event (success or failure, including reload).
     */
    public boolean isFinished() {
        return type == EventType.COMPLETED || type == EventType.FAILED || 
               type == EventType.RELOAD_COMPLETED;
    }
    
    /** @return true if this is a success event */
    public boolean isSuccess() {
        return type == EventType.COMPLETED || type == EventType.RELOAD_COMPLETED;
    }
    
    /** @return true if this is a failure event */
    public boolean isError() {
        return type == EventType.FAILED;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS - Used internally to create events
    // ═══════════════════════════════════════════════════════════════════════════
    
    static InitEvent started(InitNode node) {
        return new InitEvent(
            EventType.STARTED, node.id(), node.displayName(),
            InitState.RUNNING, 0, 0, null
        );
    }
    
    static InitEvent completed(InitNode node, int loadedCount, long durationMs) {
        return new InitEvent(
            EventType.COMPLETED, node.id(), node.displayName(),
            InitState.COMPLETE, loadedCount, durationMs, null
        );
    }
    
    static InitEvent failed(InitNode node, long durationMs, Throwable error) {
        return new InitEvent(
            EventType.FAILED, node.id(), node.displayName(),
            InitState.FAILED, 0, durationMs, error
        );
    }
    
    static InitEvent stale(InitNode node) {
        return new InitEvent(
            EventType.STALE, node.id(), node.displayName(),
            InitState.STALE, 0, 0, null
        );
    }
    
    static InitEvent reloadStarted(InitNode node) {
        return new InitEvent(
            EventType.RELOAD_STARTED, node.id(), node.displayName(),
            InitState.RUNNING, 0, 0, null
        );
    }
    
    static InitEvent reloadCompleted(InitNode node, int loadedCount, long durationMs) {
        return new InitEvent(
            EventType.RELOAD_COMPLETED, node.id(), node.displayName(),
            InitState.COMPLETE, loadedCount, durationMs, null
        );
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", type, nodeId, state);
    }
}
