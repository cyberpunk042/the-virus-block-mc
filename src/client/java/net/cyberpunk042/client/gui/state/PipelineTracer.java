package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.log.Logging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Traces segment values through the render pipeline.
 * 
 * <p>Usage:
 * <pre>
 * // At each checkpoint:
 * PipelineTracer.trace("A1", 1, "color=0xFFFF0000");  // GUI
 * PipelineTracer.trace("A1", 2, "primaryColor=0xFFFF0000");  // State
 * PipelineTracer.trace("A1", 3, "hex=#FF0000");  // Builder
 * // ...
 * 
 * // To verify:
 * PipelineTracer.verify("A1");  // Checks all 7 checkpoints reached
 * PipelineTracer.dump();  // Dumps all traces
 * </pre>
 */
public final class PipelineTracer {
    
    private static boolean enabled = false;
    private static final int CHECKPOINT_COUNT = 7;
    
    // segment -> checkpoint -> value
    private static final Map<String, Map<Integer, String>> traces = new ConcurrentHashMap<>();
    
    // Checkpoint names for logging
    private static final String[] CHECKPOINT_NAMES = {
        "???",      // 0 (unused)
        "GUI",      // 1
        "STATE",    // 2
        "BUILDER",  // 3
        "DEFINITION", // 4
        "RENDERER", // 5
        "EMITTER",  // 6
        "VERTEX"    // 7
    };
    
    private PipelineTracer() {}
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    public static void enable() {
        enabled = true;
        Logging.GUI.topic("trace").info("PipelineTracer ENABLED");
    }
    
    public static void disable() {
        enabled = false;
        Logging.GUI.topic("trace").info("PipelineTracer DISABLED");
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void clear() {
        traces.clear();
    }
    
    // =========================================================================
    // Tracing
    // =========================================================================
    
    /**
     * Records a checkpoint value.
     * 
     * @param segment Segment ID (e.g., "A1" for primaryColor)
     * @param checkpoint Checkpoint number (1-7)
     * @param value Value at this checkpoint
     */
    public static void trace(String segment, int checkpoint, String value) {
        if (!enabled) return;
        if (checkpoint < 1 || checkpoint > CHECKPOINT_COUNT) return;
        
        traces.computeIfAbsent(segment, k -> new ConcurrentHashMap<>())
              .put(checkpoint, value);
    }
    
    /**
     * Convenience method for tracing with auto-formatting.
     */
    public static void trace(String segment, int checkpoint, String key, Object value) {
        trace(segment, checkpoint, key + "=" + value);
    }
    
    // =========================================================================
    // Verification
    // =========================================================================
    
    /**
     * Verifies a segment reached all checkpoints.
     * 
     * @return true if all 7 checkpoints recorded
     */
    public static boolean verify(String segment) {
        Map<Integer, String> checkpoints = traces.get(segment);
        if (checkpoints == null) {
            Logging.GUI.topic("trace").warn("[{}] NO TRACES RECORDED", segment);
            return false;
        }
        
        boolean complete = true;
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(segment).append("] ");
        
        for (int i = 1; i <= CHECKPOINT_COUNT; i++) {
            String value = checkpoints.get(i);
            if (value != null) {
                sb.append("✓CP").append(i).append(" ");
            } else {
                sb.append("✗CP").append(i).append(" ");
                complete = false;
            }
        }
        
        if (complete) {
            Logging.GUI.topic("trace").info("{} COMPLETE", sb);
        } else {
            Logging.GUI.topic("trace").warn("{} INCOMPLETE", sb);
        }
        
        return complete;
    }
    
    /**
     * Gets the last reached checkpoint for a segment.
     */
    public static int lastCheckpoint(String segment) {
        Map<Integer, String> checkpoints = traces.get(segment);
        if (checkpoints == null) return 0;
        
        int last = 0;
        for (int i = 1; i <= CHECKPOINT_COUNT; i++) {
            if (checkpoints.containsKey(i)) {
                last = i;
            }
        }
        return last;
    }
    
    /**
     * Finds where a segment stops in the pipeline.
     */
    public static String findBreak(String segment) {
        int last = lastCheckpoint(segment);
        if (last == 0) return "Never started (no CP1)";
        if (last == CHECKPOINT_COUNT) return "Complete (reached vertex)";
        return "Stopped at " + CHECKPOINT_NAMES[last] + " (CP" + last + "), missing " + CHECKPOINT_NAMES[last + 1];
    }
    
    // =========================================================================
    // Reporting
    // =========================================================================
    
    /**
     * Dumps all traces to log with VALUE CONSISTENCY checks.
     * Clears traces after dump to prevent stale data in next dump.
     */
    public static void dump() {
        dump(true); // Clear after dump by default
    }
    
    /**
     * Dumps all traces to log with VALUE CONSISTENCY checks.
     * 
     * @param clearAfter Whether to clear traces after dumping
     */
    public static void dump(boolean clearAfter) {
        if (traces.isEmpty()) {
            Logging.GUI.topic("trace").info("No traces recorded");
            return;
        }
        
        Logging.GUI.topic("trace").info("=== PIPELINE TRACES ===");
        
        // Sort segments
        List<String> segments = new ArrayList<>(traces.keySet());
        Collections.sort(segments);
        
        int trueComplete = 0;
        int falseComplete = 0;
        int incomplete = 0;
        List<String> issues = new ArrayList<>();
        
        try (LogScope scope = Logging.GUI.topic("trace").scope("process-segments", LogLevel.INFO)) {
            for (String segment : segments) {
                Map<Integer, String> checkpoints = traces.get(segment);
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(segment).append("] ");

                // Track value changes
                String lastValue = null;
                boolean hasNull = false;
                boolean hasValueChange = false;
                String changeDetails = "";

                for (int i = 1; i <= CHECKPOINT_COUNT; i++) {
                    String value = checkpoints.get(i);
                    if (value != null) {
                        sb.append("\n  CP").append(i).append(" (").append(CHECKPOINT_NAMES[i]).append("): ").append(value);

                        // Check for null values
                        if (value.contains("null") || value.contains("n/a")) {
                            hasNull = true;
                        }

                        // Check for significant value changes (comparing actual values, not just format)
                        if (lastValue != null && !valuesEquivalent(segment, lastValue, value)) {
                            hasValueChange = true;
                            changeDetails = "CP" + (i-1) + "→CP" + i + ": " + extractValue(lastValue) + " → " + extractValue(value);
                        }
                        lastValue = value;
                    }
                }

                // Determine TRUE status
                int last = lastCheckpoint(segment);
                boolean reachedEnd = (last == CHECKPOINT_COUNT);
                boolean trulyComplete = reachedEnd && !hasValueChange && !hasNull;

                if (reachedEnd) {
                    if (hasValueChange) {
                        sb.append("\n  ⚠ VALUE CHANGED: ").append(changeDetails);
                        falseComplete++;
                        issues.add(segment + ": " + changeDetails);
                    } else if (hasNull) {
                        sb.append("\n  ⚠ CONTAINS NULL");
                        falseComplete++;
                        issues.add(segment + ": contains null");
                    } else {
                        sb.append("\n  ✓ COMPLETE");
                        trueComplete++;
                    }
                } else {
                    sb.append("\n  ✗ INCOMPLETE: ").append(findBreak(segment));
                    incomplete++;
                    issues.add(segment + ": stopped at CP" + last);
                }

                scope.branch("segment").kv("trace", sb.toString());
            }
        }
        
        Logging.GUI.topic("trace").info("=== END TRACES ===");
        Logging.GUI.topic("trace").info("=== SUMMARY ===");
        Logging.GUI.topic("trace").info("TRUE COMPLETE: {} | FALSE COMPLETE: {} | INCOMPLETE: {}", 
            trueComplete, falseComplete, incomplete);
        if (!issues.isEmpty()) {
            Logging.GUI.topic("trace").info("=== ISSUES ({}) ===", issues.size());
            for (String issue : issues) {
                Logging.GUI.topic("trace").warn("  - {}", issue);
            }
        }
        
        // Clear after dump to prevent stale traces
        if (clearAfter) {
            clear();
            Logging.GUI.topic("trace").debug("Traces cleared after dump");
        }
    }
    
    /**
     * Checks if two values are equivalent (accounting for format differences).
     */
    private static boolean valuesEquivalent(String segment, String v1, String v2) {
        // Extract the actual value part (after '=')
        String val1 = extractValue(v1);
        String val2 = extractValue(v2);
        
        // Special case for colors - normalize to same format
        if (segment.contains("color") || segment.contains("Color")) {
            return normalizeColor(val1).equalsIgnoreCase(normalizeColor(val2));
        }
        
        // For floats, compare numerically with tolerance
        try {
            float f1 = Float.parseFloat(val1);
            float f2 = Float.parseFloat(val2);
            return Math.abs(f1 - f2) < 0.001f;
        } catch (NumberFormatException e) {
            // Not floats, compare as strings
        }
        
        // Generic check - vtx/matrix/applied/emitted/complete are "terminal" values, always OK
        if (val2.equals("matrix") || val2.equals("applied") || val2.equals("emitted") || 
            val2.equals("complete") || val2.startsWith("vtx") || val2.startsWith("r=")) {
            return true;
        }
        
        return val1.equalsIgnoreCase(val2);
    }
    
    /**
     * Extracts the value part from a trace entry (after '=').
     */
    private static String extractValue(String trace) {
        int idx = trace.indexOf('=');
        if (idx >= 0 && idx < trace.length() - 1) {
            return trace.substring(idx + 1).trim();
        }
        return trace;
    }
    
    /**
     * Normalizes color to comparable format.
     */
    private static String normalizeColor(String color) {
        if (color == null) return "";
        // Remove # prefix, 0x prefix, alpha channel
        String c = color.replace("#", "").replace("0x", "");
        // If 8 chars (with alpha), take last 6
        if (c.length() == 8) {
            c = c.substring(2);
        }
        return c.toUpperCase();
    }
    
    /**
     * Gets a summary of all segments with accuracy check.
     */
    public static String summary() {
        if (traces.isEmpty()) return "No traces";
        
        int trueComplete = 0;
        int falseComplete = 0;
        int incomplete = 0;
        StringBuilder issues = new StringBuilder();
        
        for (String segment : traces.keySet()) {
            Map<Integer, String> checkpoints = traces.get(segment);
            int last = lastCheckpoint(segment);
            
            if (last == CHECKPOINT_COUNT) {
                // Check for value consistency
                boolean hasIssue = false;
                String lastValue = null;
                for (int i = 1; i <= CHECKPOINT_COUNT; i++) {
                    String value = checkpoints.get(i);
                    if (value != null) {
                        if (value.contains("null") || value.contains("n/a")) {
                            hasIssue = true;
                        }
                        if (lastValue != null && !valuesEquivalent(segment, lastValue, value)) {
                            hasIssue = true;
                        }
                        lastValue = value;
                    }
                }
                if (hasIssue) {
                    falseComplete++;
                    if (issues.length() > 0) issues.append(", ");
                    issues.append(segment).append("⚠");
                } else {
                    trueComplete++;
                }
            } else {
                incomplete++;
                if (issues.length() > 0) issues.append(", ");
                issues.append(segment).append("@CP").append(last);
            }
        }
        
        return String.format("TRUE: %d | FALSE: %d | INCOMPLETE: %d [%s]", 
            trueComplete, falseComplete, incomplete, issues.toString());
    }
    
    // =========================================================================
    // Segment IDs - APPEARANCE (A1-A10)
    // =========================================================================
    public static final String A1_PRIMARY_COLOR = "A01_color";
    public static final String A2_ALPHA = "A02_alpha";
    public static final String A3_GLOW = "A03_glow";
    public static final String A4_EMISSIVE = "A04_emissive";
    public static final String A5_SATURATION = "A05_saturation";
    public static final String A6_SECONDARY_COLOR = "A06_secondary";
    public static final String A7_BRIGHTNESS = "A07_brightness";
    public static final String A8_HUE_SHIFT = "A08_hueShift";
    public static final String A9_COLOR_BLEND = "A09_colorBlend";
    public static final String A10_ALPHA_MIN = "A10_alphaMin";
    public static final String A11_ALPHA_MAX = "A11_alphaMax";
    
    // =========================================================================
    // Segment IDs - SHAPE (S1-S18)
    // Common: S1_shapeType, S2_radius
    // Sphere: S3_latSteps, S4_lonSteps, S5_algorithm
    // Ring/Disc: S6_innerRadius, S7_outerRadius
    // Cylinder/Prism/Ring: S8_height, S9_segments
    // Prism: S10_sides
    // Polyhedron: S11_polyType, S12_faceCount
    // All: S13_openTop, S14_openBottom, S15_thickness, S16_hollow, S17_ringsCount, S18_stacksCount
    // =========================================================================
    public static final String S1_SHAPE_TYPE = "S01_shapeType";
    public static final String S2_RADIUS = "S02_radius";
    public static final String S3_LAT_STEPS = "S03_latSteps";
    public static final String S4_LON_STEPS = "S04_lonSteps";
    public static final String S5_ALGORITHM = "S05_algorithm";
    public static final String S6_INNER_RADIUS = "S06_innerRadius";
    public static final String S7_OUTER_RADIUS = "S07_outerRadius";
    public static final String S8_HEIGHT = "S08_height";
    public static final String S9_SEGMENTS = "S09_segments";
    public static final String S10_SIDES = "S10_sides";
    public static final String S11_POLY_TYPE = "S11_polyType";
    public static final String S12_FACE_COUNT = "S12_faceCount";
    public static final String S13_OPEN_TOP = "S13_openTop";
    public static final String S14_OPEN_BOTTOM = "S14_openBottom";
    public static final String S15_THICKNESS = "S15_thickness";
    public static final String S16_HOLLOW = "S16_hollow";
    public static final String S17_RINGS_COUNT = "S17_ringsCount";
    public static final String S18_STACKS_COUNT = "S18_stacksCount";
    
    // =========================================================================
    // Segment IDs - FILL (F1-F6)
    // =========================================================================
    public static final String F1_FILL_MODE = "F1_fillMode";
    public static final String F2_WIRE_THICKNESS = "F2_wireThickness";
    public static final String F3_DOUBLE_SIDED = "F3_doubleSided";
    public static final String F4_DEPTH_TEST = "F4_depthTest";
    public static final String F5_DEPTH_WRITE = "F5_depthWrite";
    public static final String F6_CAGE_OPTIONS = "F6_cageOptions";
    
    // =========================================================================
    // Segment IDs - ANIMATION (N1-N16)
    // =========================================================================
    public static final String N1_SPIN_SPEED = "N01_spinSpeed";
    public static final String N2_SPIN_AXIS = "N02_spinAxis";
    public static final String N3_PULSE_SPEED = "N03_pulseSpeed";
    public static final String N4_PULSE_SCALE = "N04_pulseScale";
    public static final String N5_PULSE_MODE = "N05_pulseMode";
    public static final String N6_ALPHA_PULSE_SPEED = "N06_alphaPulseSpeed";
    public static final String N7_ALPHA_PULSE_MIN = "N07_alphaPulseMin";
    public static final String N8_ALPHA_PULSE_MAX = "N08_alphaPulseMax";
    public static final String N9_WAVE_SPEED = "N09_waveFreq";
    public static final String N10_WAVE_AMPLITUDE = "N10_waveAmp";
    public static final String N11_WOBBLE_SPEED = "N11_wobbleSpeed";
    public static final String N12_COLOR_CYCLE = "N12_colorCycle";
    public static final String N13_PHASE = "N13_phase";
    public static final String N14_WOBBLE_AMPLITUDE = "N14_wobbleAmp";
    public static final String N15_COLOR_CYCLE_COLORS = "N15_cycleColors";
    public static final String N16_COLOR_CYCLE_SPEED = "N16_cycleSpeed";
    
    // =========================================================================
    // Segment IDs - TRANSFORM (T1-T12)
    // =========================================================================
    public static final String T1_OFFSET = "T01_offset";
    public static final String T2_ROTATION = "T02_rotation";
    public static final String T3_SCALE = "T03_scale";
    public static final String T4_SCALE_XYZ = "T04_scaleXYZ";
    public static final String T5_ANCHOR = "T05_anchor";
    public static final String T6_BILLBOARD = "T06_billboard";
    public static final String T7_ORBIT = "T07_orbit";
    public static final String T8_INHERIT_ROTATION = "T08_inheritRot";
    public static final String T9_SCALE_WITH_RADIUS = "T09_scaleWithRad";
    public static final String T10_FACING = "T10_facing";
    public static final String T11_UP_VECTOR = "T11_upVector";
    public static final String T12_ORBIT_RADIUS = "T12_orbitRadius";
    public static final String T13_ORBIT_SPEED = "T13_orbitSpeed";
    
    // =========================================================================
    // Segment IDs - VISIBILITY/MASK (V1-V14)
    // =========================================================================
    public static final String V1_MASK_TYPE = "V01_maskType";
    public static final String V2_MASK_COUNT = "V02_count";
    public static final String V3_MASK_THICKNESS = "V03_thickness";
    public static final String V4_MASK_OFFSET = "V04_offset";
    public static final String V5_MASK_ANIMATE = "V05_animate";
    public static final String V6_MASK_ANIM_SPEED = "V06_animSpeed";
    public static final String V7_MASK_INVERT = "V07_invert";
    public static final String V8_MASK_FEATHER = "V08_feather";
    public static final String V9_MASK_DIRECTION = "V09_direction";
    public static final String V10_MASK_FALLOFF = "V10_falloff";
    public static final String V11_GRADIENT_START = "V11_gradStart";
    public static final String V12_GRADIENT_END = "V12_gradEnd";
    public static final String V13_CENTER_X = "V13_centerX";
    public static final String V14_CENTER_Y = "V14_centerY";
    
    // =========================================================================
    // Segment IDs - LAYER (L1-L6)
    // =========================================================================
    public static final String L1_LAYER_ALPHA = "L1_layerAlpha";
    public static final String L2_LAYER_VISIBLE = "L2_layerVisible";
    public static final String L3_BLEND_MODE = "L3_blendMode";
    public static final String L4_LAYER_ID = "L4_layerId";
    public static final String L5_LAYER_TRANSFORM = "L5_layerTransform";
    public static final String L6_LAYER_ANIMATION = "L6_layerAnimation";
    
    // =========================================================================
    // Segment IDs - FIELD-LEVEL (D1-D6)
    // =========================================================================
    public static final String D1_BOBBING = "D01_bobbing";
    public static final String D2_BREATHING = "D02_breathing";
    public static final String D3_PREDICTION = "D03_prediction";
    public static final String D4_FOLLOW_MODE = "D04_followMode";
    public static final String D5_BASE_RADIUS = "D05_baseRadius";
    public static final String D6_FIELD_TYPE = "D06_fieldType";
    public static final String D7_THEME_ID = "D07_themeId";
    
    // =========================================================================
    // Segment IDs - MODIFIERS (M1-M11)
    // =========================================================================
    public static final String M1_RADIUS_MULT = "M01_radiusMult";
    public static final String M2_STRENGTH_MULT = "M02_strengthMult";
    public static final String M3_ALPHA_MULT = "M03_alphaMult";
    public static final String M4_SPIN_MULT = "M04_spinMult";
    public static final String M5_VISUAL_SCALE = "M05_visualScale";
    public static final String M6_TILT_MULT = "M06_tiltMult";
    public static final String M7_SWIRL_STRENGTH = "M07_swirlStrength";
    public static final String M8_INVERTED = "M08_inverted";
    public static final String M9_PULSING = "M09_pulsing";
    
    // =========================================================================
    // Segment IDs - BEAM (B1-B7)
    // =========================================================================
    public static final String B1_BEAM_ENABLED = "B1_beamEnabled";
    public static final String B2_BEAM_INNER_RADIUS = "B2_beamInnerRad";
    public static final String B3_BEAM_OUTER_RADIUS = "B3_beamOuterRad";
    public static final String B4_BEAM_COLOR = "B4_beamColor";
    public static final String B5_BEAM_HEIGHT = "B5_beamHeight";
    public static final String B6_BEAM_GLOW = "B6_beamGlow";
    public static final String B7_BEAM_PULSE = "B7_beamPulse";
    
    // =========================================================================
    // Segment IDs - PRIMITIVE (P1-P4)
    // =========================================================================
    public static final String P1_PRIMITIVE_ID = "P1_primId";
    public static final String P2_PRIMITIVE_TYPE = "P2_primType";
    public static final String P3_ARRANGEMENT = "P3_arrangement";
    public static final String P4_LINK = "P4_link";
}

