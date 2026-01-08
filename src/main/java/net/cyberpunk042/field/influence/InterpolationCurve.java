package net.cyberpunk042.field.influence;

/**
 * Interpolation curves for binding value transformation.
 * 
 * <p>Per ARCHITECTURE §12.1:
 * <ul>
 *   <li>LINEAR - straight line (default)</li>
 *   <li>EASE_IN - slow start, fast end</li>
 *   <li>EASE_OUT - fast start, slow end</li>
 *   <li>EASE_IN_OUT - slow start and end</li>
 * </ul>
 */
public enum InterpolationCurve {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT;
    
    /**
     * Applies the interpolation curve to a normalized value (0-1).
     * 
     * @param t Input value, typically normalized 0-1
     * @return Transformed value
     */
    public float apply(float t) {
        // Clamp to 0-1
        t = Math.max(0, Math.min(1, t));
        
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN -> t * t;  // Quadratic ease in
            case EASE_OUT -> t * (2 - t);  // Quadratic ease out
            case EASE_IN_OUT -> t < 0.5f 
                ? 2 * t * t 
                : -1 + (4 - 2 * t) * t;  // Quadratic ease in-out
        };
    }
    
    /**
     * Parses from string, case-insensitive.
     */
    public static InterpolationCurve fromId(String id) {
        if (id == null) return LINEAR;
        try {
            return valueOf(id.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
