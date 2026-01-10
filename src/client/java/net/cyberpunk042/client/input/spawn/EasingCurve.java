package net.cyberpunk042.client.input.spawn;

/**
 * Easing curves for spawn animation interpolation.
 */
public enum EasingCurve {
    /**
     * Constant speed throughout.
     */
    LINEAR,
    
    /**
     * Starts slow, accelerates.
     */
    EASE_IN,
    
    /**
     * Starts fast, decelerates.
     */
    EASE_OUT,
    
    /**
     * Slow start and end.
     */
    EASE_IN_OUT;
    
    /**
     * Apply the easing function to a progress value.
     * 
     * @param t Progress value from 0.0 to 1.0
     * @return Eased progress value from 0.0 to 1.0
     */
    public float apply(float t) {
        // Clamp input
        t = Math.max(0f, Math.min(1f, t));
        
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN -> t * t * t;  // Cubic ease in
            case EASE_OUT -> 1f - (float)Math.pow(1 - t, 3);  // Cubic ease out
            case EASE_IN_OUT -> {
                // Cubic ease in-out
                if (t < 0.5f) {
                    yield 4f * t * t * t;
                } else {
                    yield 1f - (float)Math.pow(-2 * t + 2, 3) / 2f;
                }
            }
        };
    }
    
    /**
     * Get display name for GUI.
     */
    public String getDisplayName() {
        return switch (this) {
            case LINEAR -> "Linear";
            case EASE_IN -> "Ease In";
            case EASE_OUT -> "Ease Out";
            case EASE_IN_OUT -> "Ease In/Out";
        };
    }
}
