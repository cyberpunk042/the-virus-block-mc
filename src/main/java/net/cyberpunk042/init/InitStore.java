package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central store for initialization state.
 * 
 * <p>This is the "single source of truth" for what's loaded and what isn't.
 * Think of it like a database for init status.
 * 
 * <h2>Common Usage</h2>
 * <pre>{@code
 * InitStore store = Init.store();
 * 
 * // Check if something is ready
 * if (store.isComplete("field_registry")) {
 *     // Safe to use FieldRegistry
 * }
 * 
 * // Subscribe to changes
 * store.subscribe(event -> {
 *     if (event.isComplete("color_themes")) {
 *         // ColorThemes just finished loading
 *     }
 * });
 * 
 * // Get a summary
 * InitSummary summary = store.getSummary();
 * summary.printReport();
 * }</pre>
 */
public class InitStore {
    
    // All registered nodes, keyed by ID
    private final Map<String, InitNode> nodes = new LinkedHashMap<>();
    
    // All registered stages, keyed by ID
    private final Map<String, InitStage> stages = new LinkedHashMap<>();
    
    // Results from execution
    private final Map<String, InitResult> results = new LinkedHashMap<>();
    
    // Event subscribers
    private final List<Consumer<InitEvent>> subscribers = new CopyOnWriteArrayList<>();
    
    // Stage event subscribers
    private final List<Consumer<StageEvent>> stageSubscribers = new CopyOnWriteArrayList<>();
    
    // Timing
    private long startTime = 0;
    private long endTime = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NODE REGISTRATION - Called by orchestrator
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Register a node with the store.
     * <p>This is called internally by the orchestrator - you don't need to call it directly.
     */
    void register(InitNode node) {
        if (node == null) return;
        
        if (nodes.containsKey(node.id())) {
            Logging.REGISTRY.warn("Duplicate node registration: {} (ignoring)", node.id());
            return;
        }
        
        nodes.put(node.id(), node);
    }
    
    /**
     * Record the result of an execution.
     * <p>This is called internally - you don't need to call it directly.
     */
    void recordResult(InitResult result) {
        results.put(result.nodeId(), result);
    }
    
    void markStarted() {
        startTime = System.currentTimeMillis();
    }
    
    void markEnded() {
        endTime = System.currentTimeMillis();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS - Check what's loaded
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if a specific node has completed successfully.
     * 
     * <pre>{@code
     * if (store.isComplete("field_registry")) {
     *     // OK to use FieldRegistry now
     * }
     * }</pre>
     * 
     * @param nodeId The node ID to check
     * @return true if the node completed successfully
     */
    public boolean isComplete(String nodeId) {
        InitNode node = nodes.get(nodeId);
        return node != null && node.isComplete();
    }
    
    /**
     * Check if a specific node failed.
     * 
     * @param nodeId The node ID to check
     * @return true if the node failed
     */
    public boolean isFailed(String nodeId) {
        InitNode node = nodes.get(nodeId);
        return node != null && node.isFailed();
    }
    
    /**
     * Get the current state of a node.
     * 
     * @param nodeId The node ID to check
     * @return The state, or PENDING if node doesn't exist
     */
    public InitState getState(String nodeId) {
        InitNode node = nodes.get(nodeId);
        return node != null ? node.state() : InitState.PENDING;
    }
    
    /**
     * Get a node by ID.
     * 
     * @param nodeId The node ID
     * @return The node, or empty if not found
     */
    public Optional<InitNode> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }
    
    /**
     * Get all registered nodes.
     */
    public Collection<InitNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }
    
    /**
     * Get all node IDs.
     */
    public Set<String> getAllNodeIds() {
        return Collections.unmodifiableSet(nodes.keySet());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUBSCRIPTIONS - React to changes
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Subscribe to all init events.
     * 
     * <pre>{@code
     * store.subscribe(event -> {
     *     System.out.println(event.nodeId() + " is now " + event.state());
     * });
     * }</pre>
     * 
     * @param listener Called for every init event
     * @return A subscription that can be used to unsubscribe
     */
    public Subscription subscribe(Consumer<InitEvent> listener) {
        subscribers.add(listener);
        return () -> subscribers.remove(listener);
    }
    
    /**
     * Subscribe to events for specific nodes only.
     * 
     * <pre>{@code
     * store.subscribe(event -> {
     *     // Only called for field_registry events
     * }, "field_registry", "color_themes");
     * }</pre>
     * 
     * @param listener Called only for events matching the specified nodes
     * @param nodeIds  Node IDs to filter on
     * @return A subscription that can be used to unsubscribe
     */
    public Subscription subscribe(Consumer<InitEvent> listener, String... nodeIds) {
        Set<String> filter = Set.of(nodeIds);
        Consumer<InitEvent> filtered = event -> {
            if (filter.isEmpty() || filter.contains(event.nodeId())) {
                listener.accept(event);
            }
        };
        return subscribe(filtered);
    }
    
    /**
     * Fire an event to all subscribers.
     */
    void fireEvent(InitEvent event) {
        for (Consumer<InitEvent> sub : subscribers) {
            try {
                sub.accept(event);
            } catch (Exception e) {
                Logging.REGISTRY.warn("Error in init subscriber: {}", e.getMessage());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE SUBSCRIPTIONS - React to stage/phase changes (for UI progress)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Subscribe to stage events (for UI progress bars and loading screens).
     * 
     * <pre>{@code
     * store.subscribeToStages(event -> {
     *     // Update loading bar
     *     loadingBar.setProgress(event.progress());
     *     loadingBar.setText(event.stageName() + ": " + event.progressText());
     *     
     *     if (event.isStageComplete()) {
     *         showNextStageLabel(event.stageName() + " complete!");
     *     }
     * });
     * }</pre>
     * 
     * @param listener Called for stage events
     * @return Subscription to unsubscribe
     */
    public Subscription subscribeToStages(Consumer<StageEvent> listener) {
        stageSubscribers.add(listener);
        return () -> stageSubscribers.remove(listener);
    }
    
    /**
     * Subscribe to events for specific stages only.
     */
    public Subscription subscribeToStages(Consumer<StageEvent> listener, String... stageIds) {
        Set<String> filter = Set.of(stageIds);
        Consumer<StageEvent> filtered = event -> {
            if (filter.isEmpty() || filter.contains(event.stageId())) {
                listener.accept(event);
            }
        };
        return subscribeToStages(filtered);
    }
    
    /**
     * Fire a stage event to all stage subscribers.
     */
    void fireStageEvent(StageEvent event) {
        for (Consumer<StageEvent> sub : stageSubscribers) {
            try {
                sub.accept(event);
            } catch (Exception e) {
                Logging.REGISTRY.warn("Error in stage subscriber: {}", e.getMessage());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE REGISTRATION & ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Register a stage with the store.
     * <p>This is called internally by the orchestrator.
     */
    void registerStage(InitStage stage) {
        if (stage == null) return;
        stages.put(stage.id(), stage);
        // Also register all nodes within the stage
        for (InitNode node : stage.allNodes()) {
            register(node);
        }
    }
    
    /**
     * Get a stage by ID.
     */
    public Optional<InitStage> getStage(String stageId) {
        return Optional.ofNullable(stages.get(stageId));
    }
    
    /**
     * Get all registered stages.
     */
    public Collection<InitStage> getAllStages() {
        return Collections.unmodifiableCollection(stages.values());
    }
    
    /**
     * Check if a stage is complete.
     */
    public boolean isStageComplete(String stageId) {
        InitStage stage = stages.get(stageId);
        return stage != null && stage.isComplete();
    }
    
    /**
     * Get overall progress across all stages (0.0 to 1.0).
     * Counts processed nodes (completed + failed) for accurate progress.
     */
    public float overallProgress() {
        int total = 0;
        int processed = 0;
        for (InitStage stage : stages.values()) {
            total += stage.totalNodeCount();
            processed += stage.processedNodeCount();
        }
        if (total == 0) return 1.0f;
        return (float) processed / total;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUMMARY - Get an overview
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get a summary of all initialization results.
     * 
     * <pre>{@code
     * InitSummary summary = store.getSummary();
     * if (summary.allSucceeded()) {
     *     System.out.println("All good!");
     * }
     * summary.printReport();
     * }</pre>
     */
    public InitSummary getSummary() {
        List<InitResult> resultList = new ArrayList<>(results.values());
        long duration = endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        return new InitSummary(resultList, duration);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG - For troubleshooting
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Log the current status of all nodes.
     */
    public void logStatus() {
        Logging.REGISTRY.info("=== Init Store Status ===");
        for (InitNode node : nodes.values()) {
            Logging.REGISTRY.info("  {} {} - {} items, {}ms", 
                node.state().symbol(),
                node.displayName(),
                node.loadedCount(),
                node.durationMs());
        }
        Logging.REGISTRY.info("=== End Status ===");
    }
    
    /**
     * Reset all state (for testing or re-initialization).
     * Clears all nodes, stages, results, and subscriptions.
     */
    public void reset() {
        nodes.clear();
        stages.clear();
        results.clear();
        subscribers.clear();
        stageSubscribers.clear();
        startTime = 0;
        endTime = 0;
        Logging.REGISTRY.debug("InitStore reset");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER INTERFACE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * A subscription that can be cancelled.
     * 
     * <pre>{@code
     * Subscription sub = store.subscribe(e -> { ... });
     * // Later:
     * sub.unsubscribe();
     * }</pre>
     */
    @FunctionalInterface
    public interface Subscription {
        /** Stop receiving events */
        void unsubscribe();
    }
}
