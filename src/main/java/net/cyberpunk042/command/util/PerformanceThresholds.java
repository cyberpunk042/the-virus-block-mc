package net.cyberpunk042.command.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance threshold definitions and warning utilities.
 * 
 * <p>Provides absolute thresholds for field parameters that may impact
 * game performance. Used by both commands and GUI to warn users.</p>
 * 
 * <h2>Threshold Levels</h2>
 * <ul>
 *   <li><b>WARN</b> - May impact performance on lower-end hardware</li>
 *   <li><b>CRITICAL</b> - Likely to cause noticeable FPS drops</li>
 * </ul>
 */
public final class PerformanceThresholds {
    
    private PerformanceThresholds() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int LAT_STEPS_WARN = 128;
    public static final int LAT_STEPS_CRITICAL = 256;
    
    public static final int LON_STEPS_WARN = 256;
    public static final int LON_STEPS_CRITICAL = 512;
    
    public static final float RADIUS_WARN = 30f;
    public static final float RADIUS_CRITICAL = 50f;
    
    public static final int RING_SEGMENTS_WARN = 128;
    public static final int RING_SEGMENTS_CRITICAL = 256;
    
    public static final int DISC_SEGMENTS_WARN = 128;
    public static final int DISC_SEGMENTS_CRITICAL = 256;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int MASK_COUNT_WARN = 16;
    public static final int MASK_COUNT_CRITICAL = 32;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILL THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int CAGE_LAT_WARN = 64;
    public static final int CAGE_LAT_CRITICAL = 128;
    
    public static final int CAGE_LON_WARN = 64;
    public static final int CAGE_LON_CRITICAL = 128;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHECK METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum Level { OK, WARN, CRITICAL }
    
    public static Level checkLatSteps(int value) {
        if (value >= LAT_STEPS_CRITICAL) return Level.CRITICAL;
        if (value >= LAT_STEPS_WARN) return Level.WARN;
        return Level.OK;
    }
    
    public static Level checkLonSteps(int value) {
        if (value >= LON_STEPS_CRITICAL) return Level.CRITICAL;
        if (value >= LON_STEPS_WARN) return Level.WARN;
        return Level.OK;
    }
    
    public static Level checkRadius(float value) {
        if (value >= RADIUS_CRITICAL) return Level.CRITICAL;
        if (value >= RADIUS_WARN) return Level.WARN;
        return Level.OK;
    }
    
    public static Level checkMaskCount(int value) {
        if (value >= MASK_COUNT_CRITICAL) return Level.CRITICAL;
        if (value >= MASK_COUNT_WARN) return Level.WARN;
        return Level.OK;
    }
    
    public static Level checkSegments(int value) {
        if (value >= RING_SEGMENTS_CRITICAL) return Level.CRITICAL;
        if (value >= RING_SEGMENTS_WARN) return Level.WARN;
        return Level.OK;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEEDBACK HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sends a performance warning to the player if threshold exceeded.
     * 
     * @param source Command source
     * @param param Parameter name (e.g., "latSteps")
     * @param value Current value
     * @param warnThreshold Warning threshold
     * @param criticalThreshold Critical threshold
     */
    public static void checkAndWarn(ServerCommandSource source, String param, 
                                     int value, int warnThreshold, int criticalThreshold) {
        if (value >= criticalThreshold) {
            source.sendFeedback(() -> Text.literal("⚠ CRITICAL: " + param + " = " + value + 
                " (threshold: " + criticalThreshold + ") - may severely impact FPS")
                .formatted(Formatting.RED), false);
        } else if (value >= warnThreshold) {
            source.sendFeedback(() -> Text.literal("⚠ Performance: " + param + " > " + warnThreshold + 
                " may reduce FPS")
                .formatted(Formatting.YELLOW), false);
        }
    }
    
    /**
     * Float version of checkAndWarn.
     */
    public static void checkAndWarn(ServerCommandSource source, String param,
                                     float value, float warnThreshold, float criticalThreshold) {
        if (value >= criticalThreshold) {
            source.sendFeedback(() -> Text.literal("⚠ CRITICAL: " + param + " = " + value + 
                " (threshold: " + criticalThreshold + ") - may severely impact FPS")
                .formatted(Formatting.RED), false);
        } else if (value >= warnThreshold) {
            source.sendFeedback(() -> Text.literal("⚠ Performance: " + param + " > " + warnThreshold + 
                " may reduce FPS")
                .formatted(Formatting.YELLOW), false);
        }
    }
    
    /**
     * Shows profile value context after a value change.
     * 
     * @param source Command source
     * @param param Parameter name
     * @param profileValue Original profile value (null if not from profile)
     */
    public static void showProfileContext(ServerCommandSource source, String param, Object profileValue) {
        if (profileValue != null) {
            source.sendFeedback(() -> Text.literal("ℹ Profile value: " + profileValue)
                .formatted(Formatting.GRAY), false);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Record of a parameter exceeding threshold.
     */
    public record ThresholdViolation(String param, Object value, Object threshold, Level level) {}
    
    /**
     * Analyzes a set of parameters and returns any threshold violations.
     * 
     * @return List of violations (empty if all OK)
     */
    public static List<ThresholdViolation> analyzeProfile(
            int latSteps, int lonSteps, float radius, int maskCount) {
        
        List<ThresholdViolation> violations = new ArrayList<>();
        
        Level latLevel = checkLatSteps(latSteps);
        if (latLevel != Level.OK) {
            violations.add(new ThresholdViolation("latSteps", latSteps, 
                latLevel == Level.CRITICAL ? LAT_STEPS_CRITICAL : LAT_STEPS_WARN, latLevel));
        }
        
        Level lonLevel = checkLonSteps(lonSteps);
        if (lonLevel != Level.OK) {
            violations.add(new ThresholdViolation("lonSteps", lonSteps,
                lonLevel == Level.CRITICAL ? LON_STEPS_CRITICAL : LON_STEPS_WARN, lonLevel));
        }
        
        Level radiusLevel = checkRadius(radius);
        if (radiusLevel != Level.OK) {
            violations.add(new ThresholdViolation("radius", radius,
                radiusLevel == Level.CRITICAL ? RADIUS_CRITICAL : RADIUS_WARN, radiusLevel));
        }
        
        Level maskLevel = checkMaskCount(maskCount);
        if (maskLevel != Level.OK) {
            violations.add(new ThresholdViolation("maskCount", maskCount,
                maskLevel == Level.CRITICAL ? MASK_COUNT_CRITICAL : MASK_COUNT_WARN, maskLevel));
        }
        
        return violations;
    }
    
    /**
     * Sends profile complexity warnings to player.
     * 
     * @param source Command source
     * @param profileName Profile being loaded
     * @param violations List of threshold violations
     */
    public static void warnProfileComplexity(ServerCommandSource source, String profileName,
                                              List<ThresholdViolation> violations) {
        if (violations.isEmpty()) return;
        
        boolean hasCritical = violations.stream().anyMatch(v -> v.level == Level.CRITICAL);
        
        source.sendFeedback(() -> Text.literal(
            (hasCritical ? "⚠ " : "⚡ ") + "Profile \"" + profileName + "\" has high-complexity settings:")
            .formatted(hasCritical ? Formatting.RED : Formatting.YELLOW), false);
        
        for (ThresholdViolation v : violations) {
            Formatting color = v.level == Level.CRITICAL ? Formatting.RED : Formatting.YELLOW;
            source.sendFeedback(() -> Text.literal(
                "  • " + v.param + ": " + v.value + " (threshold: " + v.threshold + ")")
                .formatted(color), false);
        }
    }
}

