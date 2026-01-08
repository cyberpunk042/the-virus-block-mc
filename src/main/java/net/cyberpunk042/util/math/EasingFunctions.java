package net.cyberpunk042.util.math;

/**
 * Easing functions for smooth animations.
 * 
 * <p>All functions map input t ∈ [0,1] to output ∈ [0,1].</p>
 */
public final class EasingFunctions {
    
    private EasingFunctions() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════
    // EASING TYPES
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum Type {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_EXPO,
        EASE_OUT_EXPO,
        EASE_IN_OUT_EXPO,
        EASE_IN_BACK,
        EASE_OUT_BACK,
        EASE_IN_OUT_BACK,
        EASE_IN_ELASTIC,
        EASE_OUT_ELASTIC,
        EASE_IN_OUT_ELASTIC,
        EASE_OUT_BOUNCE
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // APPLY BY TYPE
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Apply easing function by type.
     * @param t Progress value [0,1]
     * @param type Easing type
     * @return Eased value [0,1]
     */
    public static float apply(float t, Type type) {
        return switch (type) {
            case LINEAR -> linear(t);
            case EASE_IN -> easeInQuad(t);
            case EASE_OUT -> easeOutQuad(t);
            case EASE_IN_OUT -> easeInOutQuad(t);
            case EASE_IN_QUAD -> easeInQuad(t);
            case EASE_OUT_QUAD -> easeOutQuad(t);
            case EASE_IN_OUT_QUAD -> easeInOutQuad(t);
            case EASE_IN_CUBIC -> easeInCubic(t);
            case EASE_OUT_CUBIC -> easeOutCubic(t);
            case EASE_IN_OUT_CUBIC -> easeInOutCubic(t);
            case EASE_IN_EXPO -> easeInExpo(t);
            case EASE_OUT_EXPO -> easeOutExpo(t);
            case EASE_IN_OUT_EXPO -> easeInOutExpo(t);
            case EASE_IN_BACK -> easeInBack(t);
            case EASE_OUT_BACK -> easeOutBack(t);
            case EASE_IN_OUT_BACK -> easeInOutBack(t);
            case EASE_IN_ELASTIC -> easeInElastic(t);
            case EASE_OUT_ELASTIC -> easeOutElastic(t);
            case EASE_IN_OUT_ELASTIC -> easeInOutElastic(t);
            case EASE_OUT_BOUNCE -> easeOutBounce(t);
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LINEAR
    // ═══════════════════════════════════════════════════════════════════════
    
    public static float linear(float t) {
        return t;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // QUADRATIC
    // ═══════════════════════════════════════════════════════════════════════
    
    public static float easeInQuad(float t) {
        return t * t;
    }
    
    public static float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }
    
    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CUBIC
    // ═══════════════════════════════════════════════════════════════════════
    
    public static float easeInCubic(float t) {
        return t * t * t;
    }
    
    public static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }
    
    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXPONENTIAL
    // ═══════════════════════════════════════════════════════════════════════
    
    public static float easeInExpo(float t) {
        return t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
    }
    
    public static float easeOutExpo(float t) {
        return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
    }
    
    public static float easeInOutExpo(float t) {
        if (t == 0) return 0;
        if (t == 1) return 1;
        return t < 0.5f
            ? (float) Math.pow(2, 20 * t - 10) / 2
            : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BACK (overshoot)
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final float C1 = 1.70158f;
    private static final float C2 = C1 * 1.525f;
    private static final float C3 = C1 + 1;
    
    public static float easeInBack(float t) {
        return C3 * t * t * t - C1 * t * t;
    }
    
    public static float easeOutBack(float t) {
        return 1 + C3 * (float) Math.pow(t - 1, 3) + C1 * (float) Math.pow(t - 1, 2);
    }
    
    public static float easeInOutBack(float t) {
        return t < 0.5f
            ? ((float) Math.pow(2 * t, 2) * ((C2 + 1) * 2 * t - C2)) / 2
            : ((float) Math.pow(2 * t - 2, 2) * ((C2 + 1) * (t * 2 - 2) + C2) + 2) / 2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ELASTIC
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final float C4 = (float) (2 * Math.PI) / 3;
    private static final float C5 = (float) (2 * Math.PI) / 4.5f;
    
    public static float easeInElastic(float t) {
        if (t == 0) return 0;
        if (t == 1) return 1;
        return -(float) Math.pow(2, 10 * t - 10) * (float) Math.sin((t * 10 - 10.75f) * C4);
    }
    
    public static float easeOutElastic(float t) {
        if (t == 0) return 0;
        if (t == 1) return 1;
        return (float) Math.pow(2, -10 * t) * (float) Math.sin((t * 10 - 0.75f) * C4) + 1;
    }
    
    public static float easeInOutElastic(float t) {
        if (t == 0) return 0;
        if (t == 1) return 1;
        return t < 0.5f
            ? -(float) (Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125f) * C5)) / 2
            : (float) (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125f) * C5)) / 2 + 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BOUNCE
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final float N1 = 7.5625f;
    private static final float D1 = 2.75f;
    
    public static float easeOutBounce(float t) {
        if (t < 1 / D1) {
            return N1 * t * t;
        } else if (t < 2 / D1) {
            return N1 * (t -= 1.5f / D1) * t + 0.75f;
        } else if (t < 2.5f / D1) {
            return N1 * (t -= 2.25f / D1) * t + 0.9375f;
        } else {
            return N1 * (t -= 2.625f / D1) * t + 0.984375f;
        }
    }
    
    public static float easeInBounce(float t) {
        return 1 - easeOutBounce(1 - t);
    }
    
    public static float easeInOutBounce(float t) {
        return t < 0.5f
            ? (1 - easeOutBounce(1 - 2 * t)) / 2
            : (1 + easeOutBounce(2 * t - 1)) / 2;
    }
}
