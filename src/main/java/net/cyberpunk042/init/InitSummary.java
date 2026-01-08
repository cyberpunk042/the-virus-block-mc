package net.cyberpunk042.init;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Summary of all initialization results.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * InitSummary summary = Init.summary();
 * 
 * // Quick stats
 * System.out.println("Completed: " + summary.completedCount());
 * System.out.println("Failed: " + summary.failedCount());
 * System.out.println("Total time: " + summary.totalTimeMs() + "ms");
 * 
 * // Check overall success
 * if (summary.allSucceeded()) {
 *     System.out.println("All systems go!");
 * }
 * 
 * // Print detailed report
 * summary.printReport();
 * }</pre>
 */
public record InitSummary(
    List<InitResult> results,
    long totalTimeMs
) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COUNTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** @return Total number of nodes */
    public int totalCount() {
        return results.size();
    }
    
    /** @return Number of nodes that completed successfully */
    public int completedCount() {
        return (int) results.stream().filter(InitResult::isSuccess).count();
    }
    
    /** @return Number of nodes that failed */
    public int failedCount() {
        return (int) results.stream().filter(InitResult::isFailed).count();
    }
    
    /** @return Number of nodes still pending (shouldn't happen after execute) */
    public int pendingCount() {
        return (int) results.stream()
            .filter(r -> r.state() == InitState.PENDING)
            .count();
    }
    
    /** @return Total items loaded across all nodes */
    public int totalItemsLoaded() {
        return results.stream().mapToInt(InitResult::loadedCount).sum();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** @return true if all nodes completed successfully */
    public boolean allSucceeded() {
        return failedCount() == 0 && completedCount() == totalCount();
    }
    
    /** @return true if any node failed */
    public boolean anyFailed() {
        return failedCount() > 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** @return List of only failed results */
    public List<InitResult> failures() {
        return results.stream()
            .filter(InitResult::isFailed)
            .toList();
    }
    
    /** @return List of only successful results */
    public List<InitResult> successes() {
        return results.stream()
            .filter(InitResult::isSuccess)
            .toList();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generate a formatted report suitable for logging.
     * 
     * <pre>
     * ═══════════════════════════════════════════════════════════════
     *   INITIALIZATION SUMMARY
     * ═══════════════════════════════════════════════════════════════
     *   Status:     COMPLETED
     *   Nodes:      7 completed, 0 failed
     *   Items:      47 total loaded
     *   Duration:   156ms
     * ───────────────────────────────────────────────────────────────
     *   ✓ Field Registry                    12 items    45ms
     *   ✓ Color Themes                       6 items     8ms
     *   ✓ Shape Registry                    15 items    12ms
     * ═══════════════════════════════════════════════════════════════
     * </pre>
     */
    public String toReport() {
        StringBuilder sb = new StringBuilder();
        String line = "═".repeat(65);
        String halfLine = "─".repeat(65);
        
        sb.append("\n").append(line).append("\n");
        sb.append("  INITIALIZATION SUMMARY\n");
        sb.append(line).append("\n");
        
        String status = allSucceeded() ? "COMPLETED" : "FAILED";
        sb.append("  Status:     ").append(status).append("\n");
        sb.append("  Nodes:      ").append(completedCount())
          .append(" completed, ").append(failedCount()).append(" failed\n");
        sb.append("  Items:      ").append(totalItemsLoaded()).append(" total loaded\n");
        sb.append("  Duration:   ").append(totalTimeMs).append("ms\n");
        
        sb.append(halfLine).append("\n");
        
        for (InitResult result : results) {
            sb.append("  ").append(result.toLogLine()).append("\n");
        }
        
        sb.append(line).append("\n");
        
        // If any failures, show error details
        if (anyFailed()) {
            sb.append("  ERRORS:\n");
            for (InitResult failure : failures()) {
                sb.append("  • ").append(failure.displayName()).append(": ");
                if (failure.error() != null) {
                    sb.append(failure.error().getMessage());
                } else {
                    sb.append("Unknown error");
                }
                sb.append("\n");
            }
            sb.append(line).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Print the report to stdout.
     */
    public void printReport() {
        System.out.println(toReport());
    }
    
    @Override
    public String toString() {
        return String.format("InitSummary[%d/%d complete, %dms]", 
            completedCount(), totalCount(), totalTimeMs);
    }
}
