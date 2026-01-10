package net.cyberpunk042.client.gui.schema;

/**
 * Specification for a single GUI parameter control.
 * 
 * <p>Defines how a parameter should be displayed in the GUI,
 * including its label, input type, range constraints, and grouping.</p>
 * 
 * <h3>Example Usage</h3>
 * <pre>{@code
 * ParameterSpec coreSize = new ParameterSpec(
 *     "fieldVisual.coreSize",
 *     "Core Size",
 *     ControlType.SLIDER,
 *     0.05f, 0.5f, 0.01f,
 *     0.15f,
 *     "Core",
 *     "Size of the bright center relative to radius"
 * );
 * }</pre>
 */
public record ParameterSpec(
    /**
     * State path for binding (e.g., "fieldVisual.coreSize").
     * Must match FieldVisualAdapter's get/set paths.
     */
    String path,
    
    /**
     * Display label shown in the GUI.
     * Can be customized per effect type (e.g., "Core Size" vs "Star Size").
     */
    String label,
    
    /**
     * Type of GUI control to render.
     */
    ControlType type,
    
    /**
     * Minimum value (for sliders/numeric inputs).
     * Ignored for toggles, dropdowns.
     */
    float min,
    
    /**
     * Maximum value (for sliders/numeric inputs).
     * Ignored for toggles, dropdowns.
     */
    float max,
    
    /**
     * Step size for slider increments.
     * Ignored for toggles, dropdowns.
     */
    float step,
    
    /**
     * Default value for this parameter.
     * Used when resetting or initializing.
     */
    float defaultValue,
    
    /**
     * Logical group name for organizing controls (e.g., "Colors", "Animation").
     * Controls with the same group are displayed together.
     */
    String group,
    
    /**
     * Optional tooltip text shown on hover.
     * Can be null if no tooltip is needed.
     */
    String tooltip
) {
    
    /**
     * Types of GUI controls that can be generated.
     */
    public enum ControlType {
        /** Horizontal slider with min/max range */
        SLIDER,
        
        /** Boolean toggle switch */
        TOGGLE,
        
        /** Dropdown selection (uses options array) */
        DROPDOWN,
        
        /** RGB color picker (for color components) */
        COLOR_PICKER,
        
        /** Integer slider (step = 1) */
        INT_SLIDER,
        
        /** Slider with explicit step snapping */
        STEPPED_SLIDER,
        
        /** Read-only display value */
        LABEL
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS for common patterns
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a standard 0-1 normalized slider.
     */
    public static ParameterSpec normalized(String path, String label, String group) {
        return new ParameterSpec(path, label, ControlType.SLIDER, 0f, 1f, 0.01f, 0.5f, group, null);
    }
    
    /**
     * Creates a standard 0-1 normalized slider with default value.
     */
    public static ParameterSpec normalized(String path, String label, float defaultValue, String group) {
        return new ParameterSpec(path, label, ControlType.SLIDER, 0f, 1f, 0.01f, defaultValue, group, null);
    }
    
    /**
     * Creates a slider with custom range.
     */
    public static ParameterSpec slider(String path, String label, float min, float max, float defaultValue, String group) {
        float step = (max - min) / 100f;
        return new ParameterSpec(path, label, ControlType.SLIDER, min, max, step, defaultValue, group, null);
    }
    
    /**
     * Creates a slider with custom range and step.
     */
    public static ParameterSpec slider(String path, String label, float min, float max, float step, float defaultValue, String group) {
        return new ParameterSpec(path, label, ControlType.SLIDER, min, max, step, defaultValue, group, null);
    }
    
    /**
     * Creates a slider with tooltip.
     */
    public static ParameterSpec slider(String path, String label, float min, float max, float defaultValue, String group, String tooltip) {
        float step = (max - min) / 100f;
        return new ParameterSpec(path, label, ControlType.SLIDER, min, max, step, defaultValue, group, tooltip);
    }
    
    /**
     * Creates a stepped slider with explicit step snapping.
     */
    public static ParameterSpec steppedSlider(String path, String label, float min, float max, float step, float defaultValue, String group) {
        return new ParameterSpec(path, label, ControlType.STEPPED_SLIDER, min, max, step, defaultValue, group, null);
    }
    
    /**
     * Creates an integer slider.
     */
    public static ParameterSpec intSlider(String path, String label, int min, int max, int defaultValue, String group) {
        return new ParameterSpec(path, label, ControlType.INT_SLIDER, min, max, 1f, defaultValue, group, null);
    }
    
    /**
     * Creates a boolean toggle.
     */
    public static ParameterSpec toggle(String path, String label, boolean defaultValue, String group) {
        return new ParameterSpec(path, label, ControlType.TOGGLE, 0f, 1f, 1f, defaultValue ? 1f : 0f, group, null);
    }
    
    /**
     * Creates a boolean toggle with tooltip.
     */
    public static ParameterSpec toggle(String path, String label, boolean defaultValue, String group, String tooltip) {
        return new ParameterSpec(path, label, ControlType.TOGGLE, 0f, 1f, 1f, defaultValue ? 1f : 0f, group, tooltip);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a copy with a different label.
     * Useful for effect-specific naming.
     */
    public ParameterSpec withLabel(String newLabel) {
        return new ParameterSpec(path, newLabel, type, min, max, step, defaultValue, group, tooltip);
    }
    
    /**
     * Creates a copy with a different group.
     */
    public ParameterSpec withGroup(String newGroup) {
        return new ParameterSpec(path, label, type, min, max, step, defaultValue, newGroup, tooltip);
    }
    
    /**
     * Creates a copy with tooltip.
     */
    public ParameterSpec withTooltip(String newTooltip) {
        return new ParameterSpec(path, label, type, min, max, step, defaultValue, group, newTooltip);
    }
    
    /**
     * Creates a copy with a different range.
     */
    public ParameterSpec withRange(float newMin, float newMax) {
        return new ParameterSpec(path, label, type, newMin, newMax, step, defaultValue, group, tooltip);
    }
    
    /**
     * Creates a copy with a different default value.
     */
    public ParameterSpec withDefault(float newDefault) {
        return new ParameterSpec(path, label, type, min, max, step, newDefault, group, tooltip);
    }
    
    /**
     * Returns true if this is a boolean control.
     */
    public boolean isBoolean() {
        return type == ControlType.TOGGLE;
    }
    
    /**
     * Returns true if this is a numeric slider.
     */
    public boolean isSlider() {
        return type == ControlType.SLIDER || type == ControlType.INT_SLIDER || type == ControlType.STEPPED_SLIDER;
    }
}
