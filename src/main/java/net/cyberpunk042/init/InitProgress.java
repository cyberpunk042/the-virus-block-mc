package net.cyberpunk042.init;

import net.cyberpunk042.log.Logging;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges the init framework with UI components that need progress info.
 * 
 * <h2>Why Use This?</h2>
 * <p>Your loading screen or overlay doesn't need to know about the init framework.
 * It just needs progress (0-1), label text, and "is loading?" boolean.
 * This class provides those in a simple, thread-safe way.
 * 
 * <h2>Example: Hook into Existing Overlay</h2>
 * <pre>{@code
 * // In your overlay render method:
 * public void render(DrawContext context) {
 *     if (!InitProgress.isLoading()) return;
 *     
 *     float progress = InitProgress.getProgress();
 *     String label = InitProgress.getLabel();
 *     
 *     drawProgressBar(progress);
 *     drawLabel(label);
 * }
 * }</pre>
 * 
 * <h2>Automatic Setup</h2>
 * <pre>{@code
 * // Call once at client init:
 * InitProgress.attachToClient();  // Subscribes to stage events automatically
 * }</pre>
 */
public final class InitProgress {
    
    private InitProgress() {}
    
    // Thread-safe state for render thread access
    private static volatile boolean loading = false;
    private static volatile float progress = 0.0f;
    private static final AtomicReference<String> label = new AtomicReference<>("Initializing...");
    private static volatile String currentStage = "";
    private static volatile int currentNodeCount = 0;
    private static volatile int totalNodeCount = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Call these from your overlay
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if initialization is in progress.
     * 
     * <pre>{@code
     * if (InitProgress.isLoading()) {
     *     renderLoadingOverlay();
     * }
     * }</pre>
     */
    public static boolean isLoading() {
        return loading;
    }
    
    /**
     * Get current progress (0.0 to 1.0).
     */
    public static float getProgress() {
        return progress;
    }
    
    /**
     * Get current progress as percentage (0 to 100).
     */
    public static int getProgressPercent() {
        return Math.round(progress * 100);
    }
    
    /**
     * Get the current stage label (e.g., "Field System").
     */
    public static String getLabel() {
        return label.get();
    }
    
    /**
     * Get the current stage ID.
     */
    public static String getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Get progress as "X/Y" text.
     */
    public static String getProgressText() {
        return currentNodeCount + "/" + totalNodeCount;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SETUP - Call once at init
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static InitStore.Subscription subscription = null;
    
    /**
     * Attach to the client init store and start tracking progress.
     * 
     * <p>Call this once in your ClientModInitializer, BEFORE executing stages.
     * 
     * <pre>{@code
     * // In TheVirusBlockClient.onInitializeClient():
     * InitProgress.attachToClient();
     * 
     * Init.clientOrchestrator()
     *     .stage(...)
     *     .execute();
     * }</pre>
     */
    public static void attachToClient() {
        if (subscription != null) {
            Logging.GUI.warn("InitProgress.attachToClient() called multiple times");
            return;
        }
        
        loading = true;
        subscription = Init.clientStore().subscribeToStages(event -> {
            switch (event.type()) {
                case STAGE_STARTED -> {
                    currentStage = event.stageId();
                    label.set(event.stageName());
                    totalNodeCount = event.totalNodes();
                    currentNodeCount = 0;
                }
                case NODE_COMPLETE, NODE_FAILED -> {
                    currentNodeCount = event.completedNodes();
                    progress = event.progress();
                }
                case STAGE_COMPLETE, STAGE_FAILED -> {
                    // Could show brief completion message
                }
            }
            
            // Check if all done
            if (Init.isClientReady()) {
                complete();
            }
        });
        
        Logging.GUI.debug("InitProgress attached to client store");
    }
    
    /**
     * Attach to the common (server-side) init store.
     */
    public static void attachToCommon() {
        if (subscription != null) {
            Logging.GUI.warn("InitProgress already attached");
            return;
        }
        
        loading = true;
        subscription = Init.store().subscribeToStages(event -> {
            switch (event.type()) {
                case STAGE_STARTED -> {
                    currentStage = event.stageId();
                    label.set(event.stageName());
                    totalNodeCount = event.totalNodes();
                    currentNodeCount = 0;
                }
                case NODE_COMPLETE, NODE_FAILED -> {
                    currentNodeCount = event.completedNodes();
                    progress = event.progress();
                }
                default -> {}
            }
            
            if (Init.isAllComplete()) {
                complete();
            }
        });
    }
    
    /**
     * Mark initialization as complete.
     */
    public static void complete() {
        loading = false;
        progress = 1.0f;
        label.set("Complete");
        Logging.GUI.debug("InitProgress complete");
    }
    
    /**
     * Manually set progress (for custom loading phases).
     */
    public static void setProgress(float value, String message) {
        progress = Math.max(0, Math.min(1, value));
        label.set(message);
    }
    
    /**
     * Start loading (shows overlay).
     */
    public static void startLoading(String message) {
        loading = true;
        progress = 0;
        label.set(message);
    }
    
    /**
     * Detach from events (cleanup).
     */
    public static void detach() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
        loading = false;
    }
}
