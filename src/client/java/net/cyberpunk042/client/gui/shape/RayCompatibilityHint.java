package net.cyberpunk042.client.gui.shape;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.visual.shape.RayLineShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Computes and reports compatibility hints for ray animation configurations.
 * 
 * <p>Different animation modes have different requirements and limitations.
 * This class checks for incompatible combinations and performance concerns,
 * providing user-friendly messages.</p>
 * 
 * <h3>Warning Types</h3>
 * <ul>
 *   <li><b>Compatibility:</b> Animation won't work with current settings</li>
 *   <li><b>Performance:</b> Configuration may cause lag</li>
 *   <li><b>Tip:</b> Helpful information about optimal usage</li>
 * </ul>
 */
public final class RayCompatibilityHint {
    
    private RayCompatibilityHint() {} // Static utility
    
    /**
     * Computes and sends compatibility warnings for ray configurations.
     * 
     * @param state The field edit state to read values from
     * @param callback Callback to receive (warningText, color) or (null, 0) to clear
     */
    public static void compute(FieldEditState state, BiConsumer<String, Integer> callback) {
        if (callback == null) return;
        
        List<String> warnings = new ArrayList<>();
        int maxSeverity = 0; // 0=none, 1=info, 2=warning, 3=error
        
        // Get current configuration
        String shapeType = state.getString("shapeType");
        if (!"rays".equalsIgnoreCase(shapeType)) {
            callback.accept(null, 0);
            return;
        }
        
        // Ray shape settings
        String lineShapeName = state.getString("rays.lineShape");
        RayLineShape lineShape = RayLineShape.STRAIGHT;
        try {
            lineShape = RayLineShape.valueOf(lineShapeName.toUpperCase());
        } catch (Exception ignored) {}
        
        int rayCount = state.getInt("rays.count");
        int lineSegments = state.getInt("rays.lineResolution");
        boolean isMultiSegment = lineShape != RayLineShape.STRAIGHT || lineSegments > 1;
        boolean is3DShape = lineShape == RayLineShape.CORKSCREW || 
                            lineShape == RayLineShape.SPRING || 
                            lineShape == RayLineShape.DOUBLE_HELIX;
        
        // === CHECK WIGGLE COMPATIBILITY ===
        RayWiggleConfig wiggle = state.rayWiggle();
        if (wiggle != null && wiggle.isActive()) {
            WiggleMode wiggleMode = wiggle.mode();
            
            // Most wiggle modes need multi-segment rays
            if (!isMultiSegment && wiggleMode != WiggleMode.NONE) {
                warnings.add("Wiggle needs multi-segment rays");
                maxSeverity = Math.max(maxSeverity, 2);
            }
        }
        
        // === CHECK TWIST COMPATIBILITY ===
        RayTwistConfig twist = state.rayTwist();
        if (twist != null && twist.isActive()) {
            // Twist only visible on 3D shapes
            if (!is3DShape) {
                warnings.add("Twist needs 3D shape (CORKSCREW/HELIX)");
                maxSeverity = Math.max(maxSeverity, 2);
            }
        }
        
        // === CHECK MOTION PERFORMANCE ===
        RayMotionConfig motion = state.rayMotion();
        if (motion != null && motion.isActive()) {
            MotionMode motionMode = motion.mode();
            
            // PRECESS is expensive
            if (motionMode == MotionMode.PRECESS && rayCount > 50) {
                warnings.add("PRECESS + " + rayCount + " rays may lag");
                maxSeverity = Math.max(maxSeverity, 2);
            }
        }
        
        // === CHECK SEGMENT PERFORMANCE ===
        if (lineSegments > 64 && rayCount > 100) {
            warnings.add("High segments + count may lag");
            maxSeverity = Math.max(maxSeverity, 1);
        }
        
        // === SEND RESULT ===
        if (warnings.isEmpty()) {
            callback.accept(null, 0);
        } else {
            // Combine warnings (max 2 to fit display)
            String message = String.join(" | ", warnings.subList(0, Math.min(2, warnings.size())));
            
            int color = switch (maxSeverity) {
                case 3 -> GuiConstants.ERROR;
                case 2 -> GuiConstants.WARNING;
                default -> GuiConstants.TEXT_SECONDARY;
            };
            
            callback.accept("⚠ " + message, color);
        }
    }
    
    /**
     * Gets a tooltip hint for a specific animation mode.
     * This can be used for tooltips on dropdowns.
     * 
     * @param animationType Type of animation ("wiggle", "twist", "motion", "length", "travel")
     * @param modeName The specific mode name
     * @return Tooltip text or null if no special hint
     */
    public static String getModeHint(String animationType, String modeName) {
        return switch (animationType.toLowerCase()) {
            case "wiggle" -> switch (modeName.toUpperCase()) {
                case "WIGGLE", "WOBBLE", "WRITHE", "RIPPLE", "WHIP", "SNAKE", "PULSE_WAVE" -> 
                    "Requires Line Shape ≠ STRAIGHT or Line Segments > 1";
                case "SHIMMER", "FLUTTER" -> "Works on all ray types";
                default -> null;
            };
            case "twist" -> switch (modeName.toUpperCase()) {
                case "TWIST", "OSCILLATE_TWIST", "WIND_UP", "UNWIND", "SPIRAL_TWIST" -> 
                    "Requires 3D Line Shape (CORKSCREW, SPRING, HELIX)";
                default -> null;
            };
            case "motion" -> switch (modeName.toUpperCase()) {
                case "PRECESS" -> "May lag with >50 rays. Consider ANGULAR_DRIFT instead.";
                default -> null;
            };
            default -> null;
        };
    }
}
