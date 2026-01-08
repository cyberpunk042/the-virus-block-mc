package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;

import java.util.*;

/**
 * Manages execution of init nodes in the correct order.
 * 
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * Init.orchestrator()
 *     .register(new FieldRegistryNode())
 *     .register(new ColorThemesNode())
 *     .register(new ShapeRegistryNode())
 *     .execute();
 * }</pre>
 * 
 * <h2>What It Does</h2>
 * <ul>
 *   <li>Sorts nodes by dependencies (loads X before Y if Y depends on X)</li>
 *   <li>Catches errors and continues with other nodes</li>
 *   <li>Fires events so you can track progress</li>
 *   <li>Logs a nice summary at the end</li>
 * </ul>
 * 
 * <h2>Dependency Example</h2>
 * <pre>{@code
 * // If ColorThemesNode.dependsOn("field_registry"), the orchestrator 
 * // will automatically run FieldRegistryNode first.
 * 
 * Init.orchestrator()
 *     .register(new ColorThemesNode())   // Will run second
 *     .register(new FieldRegistryNode()) // Will run first
 *     .execute();
 * }</pre>
 */
public class InitOrchestrator {
    
    private final InitStore store;
    private final List<InitNode> registrationOrder = new ArrayList<>();
    private boolean executed = false;
    
    /**
     * Create an orchestrator that reports to the given store.
     */
    public InitOrchestrator(InitStore store) {
        this.store = store;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION - Tell the orchestrator what to load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Register a node to be executed.
     * 
     * <p>Nodes are executed in dependency order, not registration order.
     * 
     * <pre>{@code
     * Init.orchestrator()
     *     .register(new MyNode())
     *     .register(new AnotherNode())
     *     .execute();
     * }</pre>
     * 
     * @param node The node to register
     * @return this (for chaining)
     */
    public InitOrchestrator register(InitNode node) {
        if (node == null) return this;
        
        if (executed) {
            Logging.REGISTRY.warn("Registering node after execution: {}", node.id());
        }
        
        registrationOrder.add(node);
        store.register(node);
        return this;
    }
    
    /**
     * Register multiple nodes at once.
     * 
     * <pre>{@code
     * Init.orchestrator()
     *     .registerAll(
     *         new FieldRegistryNode(),
     *         new ColorThemesNode(),
     *         new ShapeRegistryNode()
     *     )
     *     .execute();
     * }</pre>
     */
    public InitOrchestrator registerAll(InitNode... nodes) {
        for (InitNode node : nodes) {
            register(node);
        }
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE REGISTRATION - Organize nodes into phases
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<InitStage> stageOrder = new ArrayList<>();
    
    /**
     * Register a stage (group of nodes).
     * 
     * <p>Stages are executed in order, with all nodes in a stage completing
     * before the next stage begins.
     * 
     * <pre>{@code
     * InitStage coreStage = InitStage.of("core", "Core Systems")
     *     .add(new PayloadsNode())
     *     .add(new HandlersNode());
     * 
     * InitStage fieldStage = InitStage.of("field", "Field System")
     *     .dependsOn(coreStage)
     *     .add(new FieldRegistryNode())
     *     .add(new ColorThemesNode());
     * 
     * Init.orchestrator()
     *     .stage(coreStage)
     *     .stage(fieldStage)
     *     .execute();
     * }</pre>
     * 
     * @param stage The stage to register
     * @return this (for chaining)
     */
    public InitOrchestrator stage(InitStage stage) {
        if (stage == null) return this;
        
        if (executed) {
            Logging.REGISTRY.warn("Registering stage after execution: {}", stage.id());
        }
        
        stageOrder.add(stage);
        store.registerStage(stage);
        
        // Also add nodes to registrationOrder for dependency sorting
        for (InitNode node : stage.allNodes()) {
            if (!registrationOrder.contains(node)) {
                registrationOrder.add(node);
            }
        }
        
        return this;
    }
    
    /**
     * Register multiple stages at once.
     */
    public InitOrchestrator stages(InitStage... stages) {
        for (InitStage stage : stages) {
            stage(stage);
        }
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION - Run the init sequence
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Execute all registered nodes in dependency order.
     * 
     * <p>This is typically called once, at mod initialization time.
     * 
     * <pre>{@code
     * InitSummary summary = Init.orchestrator()
     *     .register(new FieldRegistryNode())
     *     .execute();
     * 
     * if (summary.anyFailed()) {
     *     // Handle errors
     * }
     * }</pre>
     * 
     * @return Summary of results
     */
    public InitSummary execute() {
        if (executed) {
            Logging.REGISTRY.warn("Orchestrator.execute() called multiple times!");
            return store.getSummary();
        }
        
        executed = true;
        store.markStarted();
        
        // If we have stages, execute stage by stage
        if (!stageOrder.isEmpty()) {
            executeByStages();
        } else {
            // No stages - execute all nodes in dependency order
            List<InitNode> sorted = sortByDependencies();
            Logging.REGISTRY.info("Starting initialization with {} nodes...", sorted.size());
            for (InitNode node : sorted) {
                executeNode(node, null);
            }
        }
        
        store.markEnded();
        
        // Generate and log summary
        InitSummary summary = store.getSummary();
        logSummary(summary);
        
        return summary;
    }
    
    /**
     * Execute nodes grouped by stages, firing stage events.
     */
    private void executeByStages() {
        int totalNodes = stageOrder.stream().mapToInt(InitStage::totalNodeCount).sum();
        Logging.REGISTRY.info("Starting initialization: {} stages, {} nodes...", 
            stageOrder.size(), totalNodes);
        
        for (InitStage stage : stageOrder) {
            executeStage(stage);
        }
    }
    
    /**
     * Execute all nodes in a stage.
     */
    private void executeStage(InitStage stage) {
        // Check stage dependencies
        for (String depId : stage.stageDependencies()) {
            if (!store.isStageComplete(depId)) {
                Logging.REGISTRY.warn("Skipping stage {} - dependency stage {} not complete", 
                    stage.id(), depId);
                return;
            }
        }
        
        // Fire stage started event
        stage.markStarted();
        store.fireStageEvent(StageEvent.stageStarted(stage));
        Logging.REGISTRY.info("▶ Stage: {}", stage.displayName());
        
        // Get nodes in dependency order
        List<InitNode> stageNodes = sortNodesInStage(stage);
        
        // Execute each node
        for (InitNode node : stageNodes) {
            executeNode(node, stage);
        }
        
        // Fire stage complete/failed event
        stage.markComplete();
        if (stage.hasFailed()) {
            store.fireStageEvent(StageEvent.stageFailed(stage));
            Logging.REGISTRY.warn("✗ Stage {} finished with {} failures", 
                stage.displayName(), stage.failedNodeCount());
        } else {
            store.fireStageEvent(StageEvent.stageComplete(stage));
            Logging.REGISTRY.info("✓ Stage {} complete: {} nodes, {}ms", 
                stage.displayName(), stage.completedNodeCount(), stage.durationMs());
        }
    }
    
    /**
     * Sort nodes within a stage by their dependencies.
     */
    private List<InitNode> sortNodesInStage(InitStage stage) {
        List<InitNode> all = stage.allNodes();
        Map<String, InitNode> byId = new HashMap<>();
        for (InitNode node : all) {
            byId.put(node.id(), node);
        }
        
        List<InitNode> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (InitNode node : all) {
            visit(node, byId, visited, visiting, result);
        }
        
        return result;
    }
    
    /**
     * Execute a single node (used internally and for reload).
     * @param stage The containing stage (for stage events), or null if no stages
     */
    private void executeNode(InitNode node, InitStage stage) {
        // Check dependencies are satisfied
        for (String depId : node.dependencies()) {
            if (!store.isComplete(depId)) {
                Logging.REGISTRY.warn("Skipping {} - dependency {} not complete", 
                    node.id(), depId);
                return;
            }
        }
        
        // Fire started event
        store.fireEvent(InitEvent.started(node));
        
        // Execute
        InitResult result = node.execute();
        store.recordResult(result);
        
        // Update stage progress
        if (stage != null) {
            stage.onNodeComplete(node, result.isSuccess());
        }
        
        // Fire completion/failure event
        if (result.isSuccess()) {
            store.fireEvent(InitEvent.completed(node, result.loadedCount(), result.durationMs()));
            
            // Fire stage progress event
            if (stage != null) {
                store.fireStageEvent(StageEvent.nodeComplete(stage, node));
            }
            
            Logging.REGISTRY.debug("  {} {} - {} items, {}ms",
                node.state().symbol(), node.displayName(), 
                result.loadedCount(), result.durationMs());
        } else {
            store.fireEvent(InitEvent.failed(node, result.durationMs(), result.error()));
            
            // Fire stage failure event
            if (stage != null) {
                store.fireStageEvent(StageEvent.nodeFailed(stage, node, result.error()));
            }
            
            Logging.REGISTRY.error("  {} {} - FAILED: {}",
                node.state().symbol(), node.displayName(),
                result.error() != null ? result.error().getMessage() : "Unknown error");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RELOAD - Hot-reload at runtime
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reload a specific node and anything that depends on it.
     * 
     * <pre>{@code
     * // Reload just color themes:
     * Init.reload("color_themes");
     * 
     * // Any nodes that depend on color_themes will also be reloaded
     * }</pre>
     * 
     * @param nodeId ID of the node to reload
     * @return true if reload succeeded
     */
    public boolean reload(String nodeId) {
        Optional<InitNode> opt = store.getNode(nodeId);
        if (opt.isEmpty()) {
            Logging.REGISTRY.warn("Cannot reload unknown node: {}", nodeId);
            return false;
        }
        
        InitNode node = opt.get();
        if (!node.isReloadable()) {
            Logging.REGISTRY.warn("Node does not support reload: {}", nodeId);
            return false;
        }
        
        Logging.REGISTRY.info("Reloading: {}", node.displayName());
        
        // Mark dependents as stale
        markDependentsStale(nodeId);
        
        // Reload this node
        store.fireEvent(InitEvent.reloadStarted(node));
        InitResult result = node.executeReload();
        store.recordResult(result);
        
        if (result.isSuccess()) {
            store.fireEvent(InitEvent.reloadCompleted(node, result.loadedCount(), result.durationMs()));
            Logging.REGISTRY.info("Reloaded {} - {} items", node.displayName(), result.loadedCount());
            
            // Reload stale dependents
            reloadStaleDependents();
            return true;
        } else {
            store.fireEvent(InitEvent.failed(node, result.durationMs(), result.error()));
            Logging.REGISTRY.error("Failed to reload {}: {}", 
                node.displayName(), 
                result.error() != null ? result.error().getMessage() : "Unknown");
            return false;
        }
    }
    
    /**
     * Reload all reloadable nodes in dependency order.
     * Fires events and records results properly.
     */
    public void reloadAll() {
        Logging.REGISTRY.info("Reloading all nodes...");
        
        List<InitNode> sorted = sortByDependencies();
        int reloadedCount = 0;
        int failedCount = 0;
        
        for (InitNode node : sorted) {
            if (node.isReloadable()) {
                // Use the proper reload method which fires events
                if (reloadSingleNode(node)) {
                    reloadedCount++;
                } else {
                    failedCount++;
                }
            }
        }
        
        if (failedCount > 0) {
            Logging.REGISTRY.warn("Reload complete: {} reloaded, {} failed", reloadedCount, failedCount);
        } else {
            Logging.REGISTRY.info("Reload complete: {} nodes reloaded", reloadedCount);
        }
    }
    
    /**
     * Reload a single node and fire appropriate events.
     * @return true if reload succeeded
     */
    private boolean reloadSingleNode(InitNode node) {
        store.fireEvent(InitEvent.reloadStarted(node));
        InitResult result = node.executeReload();
        store.recordResult(result);
        
        if (result.isSuccess()) {
            store.fireEvent(InitEvent.reloadCompleted(node, result.loadedCount(), result.durationMs()));
            return true;
        } else {
            store.fireEvent(InitEvent.failed(node, result.durationMs(), result.error()));
            return false;
        }
    }
    
    private void markDependentsStale(String nodeId) {
        for (InitNode node : registrationOrder) {
            if (node.dependencies().contains(nodeId) && node.isComplete()) {
                node.markStale();
                store.fireEvent(InitEvent.stale(node));
            }
        }
    }
    
    private void reloadStaleDependents() {
        List<InitNode> sorted = sortByDependencies();
        for (InitNode node : sorted) {
            if (node.state() == InitState.STALE && node.isReloadable()) {
                reload(node.id());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEPENDENCY RESOLUTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sort nodes so dependencies come first.
     * Uses topological sort.
     */
    private List<InitNode> sortByDependencies() {
        Map<String, InitNode> byId = new HashMap<>();
        for (InitNode node : registrationOrder) {
            byId.put(node.id(), node);
        }
        
        List<InitNode> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (InitNode node : registrationOrder) {
            visit(node, byId, visited, visiting, result);
        }
        
        return result;
    }
    
    private void visit(InitNode node, Map<String, InitNode> byId, 
                       Set<String> visited, Set<String> visiting, 
                       List<InitNode> result) {
        if (visited.contains(node.id())) return;
        
        if (visiting.contains(node.id())) {
            Logging.REGISTRY.error("Circular dependency detected involving: {}", node.id());
            return;
        }
        
        visiting.add(node.id());
        
        for (String depId : node.dependencies()) {
            InitNode dep = byId.get(depId);
            if (dep != null) {
                visit(dep, byId, visited, visiting, result);
            } else {
                Logging.REGISTRY.warn("Node {} depends on unregistered node: {}", 
                    node.id(), depId);
            }
        }
        
        visiting.remove(node.id());
        visited.add(node.id());
        result.add(node);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOGGING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void logSummary(InitSummary summary) {
        if (summary.allSucceeded()) {
            Logging.REGISTRY.info("Initialization complete: {} nodes, {} items, {}ms",
                summary.completedCount(),
                summary.totalItemsLoaded(),
                summary.totalTimeMs());
        } else {
            Logging.REGISTRY.warn("Initialization finished with {} failures:",
                summary.failedCount());
            for (InitResult failure : summary.failures()) {
                Logging.REGISTRY.warn("  - {}: {}", 
                    failure.displayName(),
                    failure.error() != null ? failure.error().getMessage() : "Unknown");
            }
        }
    }
}
