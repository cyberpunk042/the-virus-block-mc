package net.cyberpunk042.state;

import net.cyberpunk042.init.Init;
import net.cyberpunk042.init.InitStore;
import net.cyberpunk042.log.Logging;

/**
 * Connects the Init framework to the global State store.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Call once during mod initialization:
 * StateInit.connect();
 * 
 * // Now you can watch init progress from anywhere:
 * State.watch("init.progress", progress -> {
 *     loadingBar.setProgress((float) progress);
 * });
 * 
 * // Or check current stage:
 * String stage = State.getString("init.stage");
 * }</pre>
 * 
 * <h2>Exposed Paths</h2>
 * <pre>
 * init.progress      - Overall progress (0.0 to 1.0)
 * init.stage         - Current stage name
 * init.stageId       - Current stage ID
 * init.loading       - true while loading
 * init.complete      - true when all done (no failures)
 * 
 * init.client.progress - Client-side progress
 * init.client.stage    - Client-side current stage
 * init.client.loading  - Client loading state
 * </pre>
 */
public final class StateInit {
    
    private static boolean connected = false;
    private static InitStore.Subscription commonSub;
    private static InitStore.Subscription clientSub;
    
    private StateInit() {}
    
    /**
     * Connect the common (server-side) init store to State.
     * Call this in your ModInitializer, BEFORE Init.orchestrator().execute().
     */
    public static void connect() {
        if (connected) {
            Logging.STATE.warn("[StateInit] Already connected");
            return;
        }
        connected = true;
        
        // Set initial state
        State.set("init.loading", true);
        State.set("init.progress", 0f);
        State.set("init.stage", "Starting...");
        State.set("init.complete", false);
        
        // Subscribe to stage events
        commonSub = Init.store().subscribeToStages(event -> {
            State.set("init.stageId", event.stageId());
            State.set("init.stage", event.stageName());
            State.set("init.progress", event.progress());
            
            if (event.isStageFinished()) {
                // Check if ALL stages complete
                if (Init.isAllComplete()) {
                    State.set("init.loading", false);
                    State.set("init.complete", true);
                }
            }
        });
        
        Logging.STATE.debug("[StateInit] Connected to common init store");
    }
    
    /**
     * Connect the client-side init store to State.
     * Call this in your ClientModInitializer, BEFORE Init.clientOrchestrator().execute().
     */
    public static void connectClient() {
        // Set initial client state
        State.set("init.client.loading", true);
        State.set("init.client.progress", 0f);
        State.set("init.client.stage", "Starting...");
        
        // Subscribe to client stage events
        clientSub = Init.clientStore().subscribeToStages(event -> {
            State.set("init.client.stageId", event.stageId());
            State.set("init.client.stage", event.stageName());
            State.set("init.client.progress", event.progress());
            
            if (event.isStageFinished()) {
                if (Init.isClientReady()) {
                    State.set("init.client.loading", false);
                    State.set("init.client.complete", true);
                }
            }
        });
        
        Logging.STATE.debug("[StateInit] Connected to client init store");
    }
    
    /**
     * Disconnect from init events (for cleanup/testing).
     */
    public static void disconnect() {
        if (commonSub != null) {
            commonSub.unsubscribe();
            commonSub = null;
        }
        if (clientSub != null) {
            clientSub.unsubscribe();
            clientSub = null;
        }
        connected = false;
        State.removeAll("init");
        Logging.STATE.debug("[StateInit] Disconnected");
    }
}
