package net.cyberpunk042.client.gui.state;

// SimplifiedFieldRenderer removed - standard mode is always used

import java.util.EnumSet;
import java.util.Set;

/**
 * Defines which features are supported by each renderer mode.
 * 
 * <p>Use this to determine which UI panels should be visible or enabled
 * based on the current renderer mode.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * if (RendererCapabilities.isSupported(Feature.BINDINGS)) {
 *     bindingsPanel.setVisible(true);
 * } else {
 *     bindingsPanel.setVisible(false);
 *     // Or show disabled with tooltip
 * }
 * </pre>
 * 
 * @see SimplifiedFieldRenderer
 * @see net.cyberpunk042.client.field.render.FieldRenderer
 */
public final class RendererCapabilities {
    
    private RendererCapabilities() {}
    
    /**
     * Features that can be controlled in the GUI.
     * Each feature maps to one or more panels/controls.
     */
    public enum Feature {
        // === ALWAYS SUPPORTED (both renderers) ===
        SHAPE("Shape selection and parameters"),
        FILL("Fill mode (solid, wireframe, cage)"),
        VISIBILITY_MASK("Visibility masks (bands, checker, etc.)"),
        APPEARANCE("Colors, alpha, glow"),
        TRANSFORM("Position, rotation, scale"),
        SPIN("Spin animation"),
        PULSE("Pulse animation"),
        ALPHA_PULSE("Alpha pulse animation"),
        
        // === ACCURATE MODE ONLY ===
        BINDINGS("Dynamic property bindings"),
        TRIGGERS("Trigger effects (damage, heal)"),
        LIFECYCLE("Lifecycle states (fade in/out)"),
        COLOR_THEME("Color theme resolution"),
        WAVE("Wave deformation animation"),
        WOBBLE("Wobble/jitter animation"),
        COLOR_CYCLE("Color cycling animation"),
        BLEND_MODES("Advanced layer blend modes"),
        PREDICTION("Movement prediction"),
        FOLLOW_MODE("Follow mode configuration"),
        BEAM("Central beam rendering"),
        LINKING("Primitive linking");
        
        private final String description;
        
        Feature(String description) {
            this.description = description;
        }
        
        public String description() {
            return description;
        }
    }
    
    /**
     * Features supported by SimplifiedFieldRenderer (Fast mode).
     */
    private static final Set<Feature> SIMPLIFIED_FEATURES = EnumSet.of(
        Feature.SHAPE,
        Feature.FILL,
        Feature.VISIBILITY_MASK,
        Feature.APPEARANCE,
        Feature.TRANSFORM,
        Feature.SPIN,
        Feature.PULSE,
        Feature.ALPHA_PULSE
    );
    
    /**
     * Features supported by FieldRenderer (Accurate mode).
     * This is ALL features.
     */
    private static final Set<Feature> FULL_FEATURES = EnumSet.allOf(Feature.class);
    
    // =========================================================================
    // Public API
    // =========================================================================
    
    /**
     * Checks if a feature is supported in the current renderer mode.
     * 
     * @param feature The feature to check
     * @return true if the feature is supported and will be rendered
     */
    public static boolean isSupported(Feature feature) {
        // Standard mode is always enabled - all features supported
        return FULL_FEATURES.contains(feature);
    }
    
    /**
     * Checks if ALL given features are supported in current mode.
     */
    public static boolean areAllSupported(Feature... features) {
        for (Feature f : features) {
            if (!isSupported(f)) return false;
        }
        return true;
    }
    
    /**
     * Checks if ANY of the given features are supported in current mode.
     */
    public static boolean isAnySupported(Feature... features) {
        for (Feature f : features) {
            if (isSupported(f)) return true;
        }
        return false;
    }
    
    /**
     * Returns the set of features supported in the current mode.
     */
    public static Set<Feature> getSupportedFeatures() {
        // Standard mode is always enabled - all features supported
        return EnumSet.copyOf(FULL_FEATURES);
    }
    
    /**
     * Returns the set of features NOT supported in the current mode.
     * Useful for showing "disabled because..." tooltips.
     */
    public static Set<Feature> getUnsupportedFeatures() {
        // Standard mode is always enabled - no unsupported features
        return EnumSet.noneOf(Feature.class);
    }
    
    /**
     * Returns a tooltip message explaining why a feature is disabled.
     */
    public static String getDisabledTooltip(Feature feature) {
        if (isSupported(feature)) {
            return null; // Not disabled
        }
        return "§7" + feature.description() + "\n§cRequires Standard mode";
    }
    
    /**
     * Returns the current mode name for display.
     */
    public static String getCurrentModeName() {
        return "Standard"; // Simplified mode was removed
    }
    
    /**
     * Returns true if currently in standard/full mode.
     */
    public static boolean isStandardMode() {
        return true; // Standard mode is always enabled
    }
    
    /**
     * Returns true if currently in simplified mode.
     */
    public static boolean isSimplifiedMode() {
        return false; // Simplified mode was removed
    }
}


