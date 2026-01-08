package net.cyberpunk042.init;

/**
 * Main entry point for the initialization framework.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the orchestrator to register nodes:
 * Init.orchestrator()
 *     .register(new MyNode())
 *     .execute();
 * 
 * // Get the store to check status or subscribe:
 * Init.store().isComplete("my_node");
 * Init.store().subscribe(event -> { ... });
 * 
 * // Reload a specific node:
 * Init.reload("my_node");
 * 
 * // Reload everything:
 * Init.reloadAll();
 * }</pre>
 * 
 * <h2>Why Use This?</h2>
 * <ul>
 *   <li>Know exactly what loaded and in what order</li>
 *   <li>See clear error messages when things fail</li>
 *   <li>Hot-reload configs without restarting</li>
 *   <li>Dependencies are explicit, not hidden</li>
 * </ul>
 */
public final class Init {
    
    private static final InitStore STORE = new InitStore();
    private static final InitOrchestrator ORCHESTRATOR = new InitOrchestrator(STORE);
    
    private Init() {} // Static access only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY API - These are the methods you'll use most
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get the orchestrator to register and execute init nodes.
     * 
     * <pre>{@code
     * Init.orchestrator()
     *     .register(new FieldRegistryNode())
     *     .register(new ColorThemesNode())
     *     .execute();
     * }</pre>
     * 
     * @return The init orchestrator
     */
    public static InitOrchestrator orchestrator() {
        return ORCHESTRATOR;
    }
    
    /**
     * Get the store to check status or subscribe to events.
     * 
     * <pre>{@code
     * // Check if something is loaded:
     * if (Init.store().isComplete("field_registry")) {
     *     // Safe to use field registry
     * }
     * 
     * // Subscribe to all events:
     * Init.store().subscribe(event -> {
     *     System.out.println("Node " + event.nodeId() + " is " + event.state());
     * });
     * }</pre>
     * 
     * @return The init store
     */
    public static InitStore store() {
        return STORE;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS - Shortcuts for common operations
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reload a specific node by ID.
     * 
     * <p>This will also reload any nodes that depend on it.
     * 
     * <pre>{@code
     * // Reload just color themes:
     * Init.reload("color_themes");
     * 
     * // This automatically reloads anything that uses color themes
     * }</pre>
     * 
     * @param nodeId The ID of the node to reload (e.g., "field_registry")
     * @return true if reload succeeded
     */
    public static boolean reload(String nodeId) {
        return ORCHESTRATOR.reload(nodeId);
    }
    
    /**
     * Reload all registered nodes in dependency order.
     * 
     * <pre>{@code
     * // Full refresh - useful after config file changes:
     * Init.reloadAll();
     * }</pre>
     */
    public static void reloadAll() {
        ORCHESTRATOR.reloadAll();
    }
    
    /**
     * Check if a specific node has completed loading.
     * 
     * <pre>{@code
     * if (Init.isReady("field_registry")) {
     *     // Now we can access FieldRegistry
     *     FieldDefinition def = FieldRegistry.get("shield");
     * }
     * }</pre>
     * 
     * @param nodeId The ID of the node to check
     * @return true if the node has completed loading successfully
     */
    public static boolean isReady(String nodeId) {
        return STORE.isComplete(nodeId);
    }
    
    /**
     * Get a summary of the current initialization state.
     * 
     * <pre>{@code
     * InitSummary summary = Init.summary();
     * System.out.println("Loaded " + summary.completedCount() + " nodes");
     * System.out.println("Took " + summary.totalTimeMs() + "ms");
     * }</pre>
     * 
     * @return Summary of all registered nodes
     */
    public static InitSummary summary() {
        return STORE.getSummary();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLUENT ACCESSORS - Navigate stages and nodes
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Access a stage by ID for status, reload, reset, or listening.
     * 
     * <pre>{@code
     * // Check status
     * if (Init.stage("field").isComplete()) { ... }
     * 
     * // Reload entire stage
     * Init.stage("field").reload();
     * 
     * // Reset stage (mark all nodes as pending)
     * Init.stage("field").reset();
     * 
     * // Access a node within the stage
     * Init.stage("field").node("color_themes").reload();
     * 
     * // Listen for completion
     * Init.stage("field").onComplete(() -> System.out.println("Field ready!"));
     * }</pre>
     * 
     * @param stageId The stage ID (e.g., "field", "core")
     * @return Fluent accessor for the stage
     */
    public static StageAccessor stage(String stageId) {
        return new StageAccessor(STORE, ORCHESTRATOR, stageId);
    }
    
    /**
     * Access a node by ID for status, reload, reset, or listening.
     * 
     * <pre>{@code
     * // Check status
     * if (Init.node("color_themes").isComplete()) { ... }
     * 
     * // Reload
     * Init.node("color_themes").reload();
     * 
     * // Reset
     * Init.node("color_themes").reset();
     * 
     * // Listen
     * Init.node("color_themes").onComplete(() -> doSomething());
     * }</pre>
     * 
     * @param nodeId The node ID (e.g., "field_registry", "color_themes")
     * @return Fluent accessor for the node
     */
    public static NodeAccessor node(String nodeId) {
        return new NodeAccessor(STORE, ORCHESTRATOR, nodeId);
    }
    
    /**
     * Access client-side stage by ID.
     */
    public static StageAccessor clientStage(String stageId) {
        return new StageAccessor(clientStore(), clientOrchestrator(), stageId);
    }
    
    /**
     * Access client-side node by ID.
     */
    public static NodeAccessor clientNode(String nodeId) {
        return new NodeAccessor(clientStore(), clientOrchestrator(), nodeId);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG METHODS - For troubleshooting
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Print the current state of all nodes to the log.
     * 
     * <p>Useful for debugging init order issues.
     */
    public static void logStatus() {
        STORE.logStatus();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE CONVENIENCE METHODS - For UI progress
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if a specific stage has completed.
     * 
     * <pre>{@code
     * if (Init.isStageReady("field")) {
     *     // Entire field stage is complete
     * }
     * }</pre>
     */
    public static boolean isStageReady(String stageId) {
        return STORE.isStageComplete(stageId);
    }
    
    /**
     * Get overall progress across all stages (0.0 to 1.0).
     * 
     * <pre>{@code
     * float progress = Init.progress();
     * loadingBar.setWidth((int)(progress * 200));
     * }</pre>
     */
    public static float progress() {
        return STORE.overallProgress();
    }
    
    /**
     * Subscribe to stage events for UI progress updates.
     * 
     * <pre>{@code
     * Init.onStageProgress(event -> {
     *     progressBar.setProgress(event.progress());
     *     progressLabel.setText(event.stageName() + ": " + event.progressText());
     * });
     * }</pre>
     */
    public static InitStore.Subscription onStageProgress(java.util.function.Consumer<StageEvent> listener) {
        return STORE.subscribeToStages(listener);
    }
    
    /**
     * Subscribe to node events.
     * 
     * <pre>{@code
     * Init.onNodeComplete("field_registry", event -> {
     *     // FieldRegistry is ready
     * });
     * }</pre>
     */
    public static InitStore.Subscription onNodeComplete(String nodeId, java.util.function.Consumer<InitEvent> listener) {
        return STORE.subscribe(event -> {
            if (event.isComplete(nodeId)) {
                listener.accept(event);
            }
        });
    }
    
    /**
     * Check if all initialization is complete (no failures).
     */
    public static boolean isAllComplete() {
        InitSummary summary = STORE.getSummary();
        return summary.totalCount() > 0 && summary.allSucceeded();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLIENT/SERVER SEPARATION - For different init contexts
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static InitStore CLIENT_STORE = null;
    private static InitOrchestrator CLIENT_ORCHESTRATOR = null;
    
    /**
     * Get a separate orchestrator for client-side initialization.
     * 
     * <p>Use this in ClientModInitializer to keep client init separate from common init.
     * 
     * <pre>{@code
     * // In TheVirusBlockClient.onInitializeClient():
     * Init.clientOrchestrator()
     *     .stage(receiversStage)
     *     .stage(renderersStage)
     *     .stage(guiStage)
     *     .execute();
     * }</pre>
     */
    public static InitOrchestrator clientOrchestrator() {
        if (CLIENT_ORCHESTRATOR == null) {
            CLIENT_STORE = new InitStore();
            CLIENT_ORCHESTRATOR = new InitOrchestrator(CLIENT_STORE);
        }
        return CLIENT_ORCHESTRATOR;
    }
    
    /**
     * Get the client-side store (for status checks and subscriptions).
     */
    public static InitStore clientStore() {
        if (CLIENT_STORE == null) {
            CLIENT_STORE = new InitStore();
            CLIENT_ORCHESTRATOR = new InitOrchestrator(CLIENT_STORE);
        }
        return CLIENT_STORE;
    }
    
    /**
     * Get client-side overall progress.
     */
    public static float clientProgress() {
        return CLIENT_STORE != null ? CLIENT_STORE.overallProgress() : 1.0f;
    }
    
    /**
     * Check if client initialization is complete (no failures).
     */
    public static boolean isClientReady() {
        if (CLIENT_STORE == null) return true;
        InitSummary summary = CLIENT_STORE.getSummary();
        return summary.totalCount() == 0 || summary.allSucceeded();
    }
}
