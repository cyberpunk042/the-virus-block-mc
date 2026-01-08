package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups multiple nodes into a logical stage/phase.
 * 
 * <h2>Why Use Stages?</h2>
 * <ul>
 *   <li>Track progress at a higher level ("Core: 3/5 complete")</li>
 *   <li>Subscribe to stage completion instead of individual nodes</li>
 *   <li>Organize init into logical phases</li>
 *   <li>Show meaningful progress in UI loading screens</li>
 * </ul>
 * 
 * <h2>Example: Defining Stages</h2>
 * <pre>{@code
 * // Create stages to group related nodes
 * InitStage coreStage = InitStage.of("core", "Core Systems")
 *     .add(new PayloadsNode())
 *     .add(new HandlersNode())
 *     .add(new RegistriesNode());
 * 
 * InitStage fieldStage = InitStage.of("field", "Field System")
 *     .dependsOn(coreStage)  // Field stage runs AFTER core stage
 *     .add(new FieldRegistryNode())
 *     .add(new ColorThemesNode())
 *     .add(new ShapeRegistryNode());
 * 
 * InitStage clientStage = InitStage.of("client", "Client Setup")
 *     .dependsOn(fieldStage)
 *     .add(new ReceiversNode())
 *     .add(new RenderersNode());
 * }</pre>
 * 
 * <h2>Example: Using with Orchestrator</h2>
 * <pre>{@code
 * Init.orchestrator()
 *     .stage(coreStage)
 *     .stage(fieldStage)
 *     .stage(clientStage)
 *     .execute();
 * }</pre>
 * 
 * <h2>Example: Subscribing to Stage Events</h2>
 * <pre>{@code
 * Init.store().subscribeToStages(event -> {
 *     System.out.println("Stage " + event.stageName() + ": " + 
 *         event.completedNodes() + "/" + event.totalNodes());
 *     
 *     if (event.isStageComplete()) {
 *         System.out.println(event.stageName() + " finished!");
 *     }
 * });
 * }</pre>
 * 
 * <h2>Nested Stages</h2>
 * <pre>{@code
 * // Stages can contain sub-stages for finer control
 * InitStage mainStage = InitStage.of("main", "Main Init")
 *     .addSubStage(coreStage)
 *     .addSubStage(fieldStage)
 *     .addSubStage(clientStage);
 * }</pre>
 */
public class InitStage {
    
    private final String id;
    private final String displayName;
    private final List<InitNode> nodes = new ArrayList<>();
    private final List<InitStage> subStages = new ArrayList<>();
    private final List<String> stageDependencies = new ArrayList<>();
    private InitStage parent = null;
    
    // Progress tracking
    private int completedCount = 0;
    private int failedCount = 0;
    private InitState state = InitState.PENDING;
    private long startTimeMs = 0;
    private long endTimeMs = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION - Fluent builder style
    // ═══════════════════════════════════════════════════════════════════════════
    
    private InitStage(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Create a new stage.
     * 
     * <pre>{@code
     * InitStage stage = InitStage.of("core", "Core Systems");
     * }</pre>
     * 
     * @param id          Unique identifier (lowercase_with_underscores)
     * @param displayName Human-readable name for logs and UI
     */
    public static InitStage of(String id, String displayName) {
        return new InitStage(id, displayName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADDING CONTENT - Build up the stage
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Add a node to this stage.
     * 
     * <pre>{@code
     * stage.add(new FieldRegistryNode())
     *      .add(new ColorThemesNode());
     * }</pre>
     */
    public InitStage add(InitNode node) {
        if (node != null) {
            nodes.add(node);
        }
        return this;
    }
    
    /**
     * Add multiple nodes at once.
     */
    public InitStage addAll(InitNode... nodes) {
        for (InitNode node : nodes) {
            add(node);
        }
        return this;
    }
    
    /**
     * Add a sub-stage (for hierarchical organization).
     * 
     * <pre>{@code
     * InitStage main = InitStage.of("main", "Main")
     *     .addSubStage(coreStage)
     *     .addSubStage(fieldStage);
     * }</pre>
     */
    public InitStage addSubStage(InitStage subStage) {
        if (subStage != null) {
            subStage.parent = this;
            subStages.add(subStage);
        }
        return this;
    }
    
    /**
     * Declare that this stage depends on another stage.
     * All nodes in the dependency stage must complete before this stage starts.
     * 
     * <pre>{@code
     * InitStage fieldStage = InitStage.of("field", "Field")
     *     .dependsOn(coreStage);  // Core must finish first
     * }</pre>
     */
    public InitStage dependsOn(InitStage other) {
        if (other != null) {
            stageDependencies.add(other.id);
        }
        return this;
    }
    
    /**
     * Declare dependency by stage ID.
     */
    public InitStage dependsOn(String stageId) {
        if (stageId != null && !stageId.isBlank()) {
            stageDependencies.add(stageId);
        }
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROGRESS TRACKING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get total node count (including sub-stages).
     */
    public int totalNodeCount() {
        int count = nodes.size();
        for (InitStage sub : subStages) {
            count += sub.totalNodeCount();
        }
        return count;
    }
    
    /**
     * Get completed node count (including sub-stages).
     */
    public int completedNodeCount() {
        int count = completedCount;
        for (InitStage sub : subStages) {
            count += sub.completedNodeCount();
        }
        return count;
    }
    
    /**
     * Get failed node count (including sub-stages).
     */
    public int failedNodeCount() {
        int count = failedCount;
        for (InitStage sub : subStages) {
            count += sub.failedNodeCount();
        }
        return count;
    }
    
    /**
     * Get processed node count (completed + failed).
     * This is how many nodes have finished, regardless of success.
     */
    public int processedNodeCount() {
        int count = completedCount + failedCount;
        for (InitStage sub : subStages) {
            count += sub.processedNodeCount();
        }
        return count;
    }
    
    /**
     * Get progress as a fraction (0.0 to 1.0).
     * Counts both completed AND failed nodes toward progress.
     */
    public float progress() {
        int total = totalNodeCount();
        if (total == 0) return 1.0f;
        return (float) processedNodeCount() / total;
    }
    
    /**
     * Get progress as a percentage (0 to 100).
     */
    public int progressPercent() {
        return Math.round(progress() * 100);
    }
    
    /**
     * Check if this stage (and all sub-stages) completed successfully.
     */
    public boolean isComplete() {
        return state == InitState.COMPLETE;
    }
    
    /**
     * Check if any node in this stage failed.
     */
    public boolean hasFailed() {
        return failedNodeCount() > 0;
    }
    
    /**
     * Get time taken in milliseconds.
     */
    public long durationMs() {
        if (startTimeMs == 0) return 0;
        if (endTimeMs == 0) return System.currentTimeMillis() - startTimeMs;
        return endTimeMs - startTimeMs;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String id() { return id; }
    public String displayName() { return displayName; }
    public InitState state() { return state; }
    public InitStage parent() { return parent; }
    public List<InitNode> nodes() { return Collections.unmodifiableList(nodes); }
    public List<InitStage> subStages() { return Collections.unmodifiableList(subStages); }
    public List<String> stageDependencies() { return Collections.unmodifiableList(stageDependencies); }
    
    /**
     * Get the full path from root (e.g., "main/core/payloads").
     */
    public String path() {
        if (parent == null) {
            return id;
        }
        return parent.path() + "/" + id;
    }
    
    /**
     * Reset this stage and all nodes within it.
     * Marks all nodes as STALE so they can be re-executed.
     */
    public void reset() {
        completedCount = 0;
        failedCount = 0;
        state = InitState.PENDING;
        startTimeMs = 0;
        endTimeMs = 0;
        
        for (InitNode node : nodes) {
            node.markStale();
        }
        for (InitStage sub : subStages) {
            sub.reset();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL - Used by orchestrator
    // ═══════════════════════════════════════════════════════════════════════════
    
    void markStarted() {
        state = InitState.RUNNING;
        startTimeMs = System.currentTimeMillis();
    }
    
    void markComplete() {
        state = hasFailed() ? InitState.FAILED : InitState.COMPLETE;
        endTimeMs = System.currentTimeMillis();
    }
    
    void onNodeComplete(InitNode node, boolean success) {
        if (success) {
            completedCount++;
        } else {
            failedCount++;
        }
    }
    
    /**
     * Get all nodes from this stage and all sub-stages, flattened.
     */
    List<InitNode> allNodes() {
        List<InitNode> all = new ArrayList<>(nodes);
        for (InitStage sub : subStages) {
            all.addAll(sub.allNodes());
        }
        return all;
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s, %d/%d, %s]", 
            displayName, id, completedNodeCount(), totalNodeCount(), state);
    }
}
