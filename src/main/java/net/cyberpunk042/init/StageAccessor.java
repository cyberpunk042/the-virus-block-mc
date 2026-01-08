package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;
import java.util.Optional;

/**
 * Fluent accessor for a stage and its nodes.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Access a stage
 * StageAccessor stage = Init.stage("field");
 * 
 * // Check status
 * if (stage.isComplete()) { ... }
 * float progress = stage.progress();
 * 
 * // Reload entire stage
 * stage.reload();
 * 
 * // Reset (clear and mark pending)
 * stage.reset();
 * 
 * // Access a node within the stage
 * NodeAccessor node = stage.node("color_themes");
 * node.reload();
 * node.reset();
 * 
 * // Fluent listening
 * stage.onComplete(() -> System.out.println("Field stage done!"));
 * stage.node("color_themes").onComplete(() -> doSomething());
 * }</pre>
 */
public class StageAccessor {
    
    private final InitStore store;
    private final InitOrchestrator orchestrator;
    private final String stageId;
    
    StageAccessor(InitStore store, InitOrchestrator orchestrator, String stageId) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.stageId = stageId;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if this stage exists.
     */
    public boolean exists() {
        return store.getStage(stageId).isPresent();
    }
    
    /**
     * Get the underlying stage object.
     */
    public Optional<InitStage> get() {
        return store.getStage(stageId);
    }
    
    /**
     * Check if this stage has completed successfully.
     */
    public boolean isComplete() {
        return store.isStageComplete(stageId);
    }
    
    /**
     * Check if this stage has any failures.
     */
    public boolean hasFailed() {
        return get().map(InitStage::hasFailed).orElse(false);
    }
    
    /**
     * Get the current state of this stage.
     */
    public InitState state() {
        return get().map(InitStage::state).orElse(InitState.PENDING);
    }
    
    /**
     * Get progress (0.0 to 1.0) for this stage.
     */
    public float progress() {
        return get().map(InitStage::progress).orElse(0f);
    }
    
    /**
     * Get progress as percentage (0 to 100).
     */
    public int progressPercent() {
        return Math.round(progress() * 100);
    }
    
    /**
     * Get node count stats: "completed/total"
     */
    public String progressText() {
        return get()
            .map(s -> s.completedNodeCount() + "/" + s.totalNodeCount())
            .orElse("0/0");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reload all reloadable nodes in this stage.
     * 
     * <pre>{@code
     * Init.stage("field").reload();
     * }</pre>
     * 
     * @return true if all reloads succeeded
     */
    public boolean reload() {
        Optional<InitStage> opt = get();
        if (opt.isEmpty()) {
            Logging.REGISTRY.warn("Cannot reload unknown stage: {}", stageId);
            return false;
        }
        
        InitStage stage = opt.get();
        boolean allSuccess = true;
        
        Logging.REGISTRY.info("Reloading stage: {}", stage.displayName());
        
        for (InitNode node : stage.allNodes()) {
            if (node.isReloadable()) {
                boolean success = orchestrator.reload(node.id());
                if (!success) allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    /**
     * Reset this stage (clear state, mark all nodes for re-execution).
     * 
     * <p>After reset, you'll need to call execute() again to re-run the stage.
     * 
     * <pre>{@code
     * Init.stage("field").reset();
     * Init.orchestrator().execute(); // Re-runs pending nodes
     * }</pre>
     */
    public StageAccessor reset() {
        Optional<InitStage> opt = get();
        if (opt.isEmpty()) {
            Logging.REGISTRY.warn("Cannot reset unknown stage: {}", stageId);
            return this;
        }
        
        InitStage stage = opt.get();
        Logging.REGISTRY.info("Resetting stage: {}", stage.displayName());
        
        // Use the stage's built-in reset which clears counts and marks nodes
        stage.reset();
        
        // Fire stale events for each node
        for (InitNode node : stage.allNodes()) {
            store.fireEvent(InitEvent.stale(node));
        }
        
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NODE ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Access a specific node within this stage.
     * 
     * <p>Note: The node is accessed from the global store, but you're responsible
     * for ensuring the node actually belongs to this stage.
     * 
     * <pre>{@code
     * Init.stage("field").node("color_themes").reload();
     * }</pre>
     */
    public NodeAccessor node(String nodeId) {
        // Validate node exists in this stage (warn if not)
        Optional<InitStage> opt = get();
        if (opt.isPresent()) {
            boolean found = opt.get().allNodes().stream()
                .anyMatch(n -> n.id().equals(nodeId));
            if (!found) {
                Logging.REGISTRY.warn("Node '{}' may not belong to stage '{}'", nodeId, stageId);
            }
        }
        return new NodeAccessor(store, orchestrator, nodeId);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Subscribe to this stage completing.
     * 
     * <pre>{@code
     * Init.stage("field").onComplete(() -> {
     *     System.out.println("Field system ready!");
     * });
     * }</pre>
     */
    public InitStore.Subscription onComplete(Runnable callback) {
        return store.subscribeToStages(event -> {
            if (event.stageId().equals(stageId) && event.isStageComplete()) {
                callback.run();
            }
        });
    }
    
    /**
     * Subscribe to progress updates for this stage.
     * 
     * <pre>{@code
     * Init.stage("field").onProgress(progress -> {
     *     loadingBar.setProgress(progress);
     * });
     * }</pre>
     */
    public InitStore.Subscription onProgress(java.util.function.Consumer<Float> callback) {
        return store.subscribeToStages(event -> {
            if (event.stageId().equals(stageId)) {
                callback.accept(event.progress());
            }
        }, stageId);
    }
    
    @Override
    public String toString() {
        return String.format("Stage[%s, %s, %s]", stageId, state(), progressText());
    }
}
