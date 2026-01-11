package net.cyberpunk042.client.visual.ubo;

/**
 * Simple static holder for HDR pipeline state.
 * 
 * <p>Used to pass intensity from field visual passes to blur/composite passes
 * without requiring public methods in mixins.</p>
 */
public final class HdrState {
    
    /** Current field intensity - set by PostEffectPassMixin when processing field_visual passes */
    private static float currentIntensity = 1.0f;
    
    /** Get the current intensity for composite pass */
    public static float getIntensity() {
        return currentIntensity;
    }
    
    /** Set the intensity from field visual config */
    public static void setIntensity(float intensity) {
        currentIntensity = intensity;
    }
    
    private HdrState() {}
}
