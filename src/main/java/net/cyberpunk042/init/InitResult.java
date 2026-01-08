package net.cyberpunk042.init;

/**
 * Result of a single node's initialization attempt.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * InitResult result = node.execute();
 * 
 * if (result.isSuccess()) {
 *     System.out.println("Loaded " + result.loadedCount() + " items");
 *     System.out.println("Took " + result.durationMs() + "ms");
 * } else {
 *     System.err.println("Failed: " + result.error().getMessage());
 * }
 * }</pre>
 */
public record InitResult(
    /** The node that was executed */
    InitNode node,
    
    /** Final state after execution */
    InitState state,
    
    /** Number of items loaded (0 if failed) */
    int loadedCount,
    
    /** Time taken in milliseconds */
    long durationMs,
    
    /** Error if failed, null otherwise */
    Throwable error
) {
    
    /**
     * @return true if initialization succeeded
     */
    public boolean isSuccess() {
        return state == InitState.COMPLETE;
    }
    
    /**
     * @return true if initialization failed
     */
    public boolean isFailed() {
        return state == InitState.FAILED;
    }
    
    /**
     * @return Node ID for convenience
     */
    public String nodeId() {
        return node.id();
    }
    
    /**
     * @return Display name for logging
     */
    public String displayName() {
        return node.displayName();
    }
    
    /**
     * Format as a single line for logging.
     * <pre>
     * "✓ Field Registry                    12 items     45ms"
     * "✗ Color Themes                      FAILED       12ms"
     * </pre>
     */
    public String toLogLine() {
        String status = state.symbol();
        String name = String.format("%-32s", node.displayName());
        String count = isSuccess() 
            ? String.format("%4d items", loadedCount) 
            : String.format("%-10s", state.label().toUpperCase());
        String time = String.format("%5dms", durationMs);
        return String.format("%s %s %s %s", status, name, count, time);
    }
    
    @Override
    public String toString() {
        return toLogLine();
    }
}
