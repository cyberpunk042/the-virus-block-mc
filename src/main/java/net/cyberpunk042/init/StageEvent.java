package net.cyberpunk042.init;

/**
 * Event fired when stage/phase progress changes.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * Init.store().subscribeToStages(event -> {
 *     // Update loading bar
 *     loadingBar.setProgress(event.progress());
 *     loadingBar.setText(event.stageName() + ": " + event.progressPercent() + "%");
 *     
 *     // React to stage completion
 *     if (event.isStageComplete()) {
 *         System.out.println("Stage " + event.stageName() + " finished!");
 *     }
 *     
 *     // React to all stages done
 *     if (event.isAllComplete()) {
 *         hideLoadingScreen();
 *     }
 * });
 * }</pre>
 */
public record StageEvent(
    /** Type of event */
    EventType type,
    
    /** Stage ID */
    String stageId,
    
    /** Stage display name */
    String stageName,
    
    /** Full path (e.g., "main/core/payloads") */
    String path,
    
    /** Nodes completed successfully in this stage */
    int completedNodes,
    
    /** Nodes processed (completed + failed) - use this for progress */
    int processedNodes,
    
    /** Total nodes in this stage */
    int totalNodes,
    
    /** Current stage state */
    InitState stageState,
    
    /** The node that triggered this event (for NODE_COMPLETE) */
    String nodeId,
    
    /** Error if node failed */
    Throwable error
) {
    
    public enum EventType {
        /** Stage execution started */
        STAGE_STARTED,
        
        /** A node within the stage completed */
        NODE_COMPLETE,
        
        /** A node within the stage failed */
        NODE_FAILED,
        
        /** Entire stage completed */
        STAGE_COMPLETE,
        
        /** Entire stage failed (one or more nodes failed) */
        STAGE_FAILED
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROGRESS - For UI display
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get progress as fraction (0.0 to 1.0).
     * Uses processedNodes (completed + failed) so progress shows work done.
     * 
     * <pre>{@code
     * loadingBar.setProgress(event.progress());
     * }</pre>
     */
    public float progress() {
        if (totalNodes == 0) return 1.0f;
        return (float) processedNodes / totalNodes;
    }
    
    /**
     * Get progress as percentage (0 to 100).
     * 
     * <pre>{@code
     * label.setText(event.progressPercent() + "%");
     * }</pre>
     */
    public int progressPercent() {
        return Math.round(progress() * 100);
    }
    
    /**
     * Get a progress string like "3/5".
     */
    public String progressText() {
        return completedNodes + "/" + totalNodes;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if this stage just completed (all nodes done, no failures).
     */
    public boolean isStageComplete() {
        return type == EventType.STAGE_COMPLETE;
    }
    
    /**
     * Check if this stage failed.
     */
    public boolean isStageFailed() {
        return type == EventType.STAGE_FAILED;
    }
    
    /**
     * Check if this event is about a specific stage.
     */
    public boolean isStage(String id) {
        return stageId.equals(id);
    }
    
    /**
     * Check if this event is about a stage completing (success OR failure).
     */
    public boolean isStageFinished() {
        return type == EventType.STAGE_COMPLETE || type == EventType.STAGE_FAILED;
    }
    
    /**
     * Check if ALL stages are complete (useful for hiding loading screen).
     * Note: You typically call this after receiving a STAGE_COMPLETE event.
     */
    public boolean isAllComplete() {
        // All nodes processed and we're in a finished state
        return processedNodes == totalNodes && isStageFinished();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS - Used internally
    // ═══════════════════════════════════════════════════════════════════════════
    
    static StageEvent stageStarted(InitStage stage) {
        return new StageEvent(
            EventType.STAGE_STARTED,
            stage.id(), stage.displayName(), stage.path(),
            0, 0, stage.totalNodeCount(),
            InitState.RUNNING,
            null, null
        );
    }
    
    static StageEvent nodeComplete(InitStage stage, InitNode node) {
        return new StageEvent(
            EventType.NODE_COMPLETE,
            stage.id(), stage.displayName(), stage.path(),
            stage.completedNodeCount(), stage.processedNodeCount(), stage.totalNodeCount(),
            stage.state(),
            node.id(), null
        );
    }
    
    static StageEvent nodeFailed(InitStage stage, InitNode node, Throwable error) {
        return new StageEvent(
            EventType.NODE_FAILED,
            stage.id(), stage.displayName(), stage.path(),
            stage.completedNodeCount(), stage.processedNodeCount(), stage.totalNodeCount(),
            stage.state(),
            node.id(), error
        );
    }
    
    static StageEvent stageComplete(InitStage stage) {
        return new StageEvent(
            EventType.STAGE_COMPLETE,
            stage.id(), stage.displayName(), stage.path(),
            stage.completedNodeCount(), stage.processedNodeCount(), stage.totalNodeCount(),
            InitState.COMPLETE,
            null, null
        );
    }
    
    static StageEvent stageFailed(InitStage stage) {
        return new StageEvent(
            EventType.STAGE_FAILED,
            stage.id(), stage.displayName(), stage.path(),
            stage.completedNodeCount(), stage.processedNodeCount(), stage.totalNodeCount(),
            InitState.FAILED,
            null, null
        );
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%d/%d)", 
            type, stageName, stageState, completedNodes, totalNodes);
    }
}
