package net.cyberpunk042.init;

/**
 * Possible states for an init node.
 * 
 * <h2>State Flow</h2>
 * <pre>
 * Normal flow:
 *   PENDING → RUNNING → COMPLETE
 * 
 * Error flow:
 *   PENDING → RUNNING → FAILED
 * 
 * Reload flow:
 *   COMPLETE → STALE → RUNNING → COMPLETE
 * </pre>
 */
public enum InitState {
    
    /**
     * Not yet started.
     * <p>This is the initial state for all nodes.
     */
    PENDING("⏳", "Pending"),
    
    /**
     * Currently loading.
     * <p>The node's {@code load()} method is running.
     */
    RUNNING("🔄", "Running"),
    
    /**
     * Successfully completed.
     * <p>The node's {@code load()} method returned without error.
     */
    COMPLETE("✓", "Complete"),
    
    /**
     * Failed with an error.
     * <p>The node's {@code load()} method threw an exception.
     * Check {@link InitNode#lastError()} for details.
     */
    FAILED("✗", "Failed"),
    
    /**
     * Needs reload.
     * <p>A dependency was reloaded, so this node should be reloaded too.
     */
    STALE("↻", "Stale");
    
    private final String symbol;
    private final String label;
    
    InitState(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }
    
    /** @return Symbol for compact display (e.g., ✓) */
    public String symbol() { return symbol; }
    
    /** @return Human-readable label */
    public String label() { return label; }
    
    /** @return true if this state represents a finished (not pending/running) state */
    public boolean isFinished() {
        return this == COMPLETE || this == FAILED;
    }
    
    /** @return true if this is a success state */
    public boolean isSuccess() {
        return this == COMPLETE;
    }
    
    /** @return true if this is an error state */
    public boolean isError() {
        return this == FAILED;
    }
    
    @Override
    public String toString() {
        return symbol + " " + label;
    }
}
