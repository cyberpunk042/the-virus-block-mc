package net.cyberpunk042.client.visual.effect;

/**
 * Tracks the current field being rendered.
 * 
 * <p>Since the same shader's passes may be shared across multiple fields,
 * we cannot use pass identity to determine which field to render.
 * Instead, we set the current field BEFORE calling processor.render()
 * and read it in the pass mixin.</p>
 */
public final class FieldPassTracker {
    
    // The field currently being rendered - set before processor.render()
    private static FieldVisualInstance currentField = null;
    
    private FieldPassTracker() {}
    
    /**
     * Set the current field BEFORE calling processor.render()
     */
    public static void setCurrentField(FieldVisualInstance field) {
        currentField = field;
    }
    
    /**
     * Get the current field being rendered.
     */
    public static FieldVisualInstance getCurrentField() {
        return currentField;
    }
    
    /**
     * Clear after render complete.
     */
    public static void clear() {
        currentField = null;
    }
}
