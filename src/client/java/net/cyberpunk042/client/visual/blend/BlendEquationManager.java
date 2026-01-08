package net.cyberpunk042.client.visual.blend;

import org.lwjgl.opengl.GL14;

/**
 * Manager for OpenGL blend equation state.
 * 
 * <p>Provides access to glBlendEquation which Minecraft doesn't expose natively.
 * This enables subtractive blending (GL_FUNC_REVERSE_SUBTRACT) and other
 * advanced blend modes.
 * 
 * <h3>Available Blend Equations</h3>
 * <ul>
 *   <li>{@code GL_FUNC_ADD} (default) - result = src + dst</li>
 *   <li>{@code GL_FUNC_SUBTRACT} - result = src - dst</li>
 *   <li>{@code GL_FUNC_REVERSE_SUBTRACT} - result = dst - src (darkening!)</li>
 *   <li>{@code GL_MIN} - result = min(src, dst)</li>
 *   <li>{@code GL_MAX} - result = max(src, dst)</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * // Enable subtractive blending (darkening effect)
 * BlendEquationManager.setSubtractive();
 * 
 * // Render your dark effect...
 * RenderSystem.enableBlend();
 * RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
 * // Your shader outputs the amount to SUBTRACT from the scene
 * 
 * // Reset to normal
 * BlendEquationManager.resetBlendEquation();
 * }</pre>
 */
public final class BlendEquationManager {
    
    /** Special value indicating blend should be disabled */
    public static final int BLEND_DISABLED = -1;
    
    private static volatile int currentEquation = GL14.GL_FUNC_ADD;
    private static volatile boolean blendDisabled = false;
    
    private BlendEquationManager() {}
    
    /**
     * Get the current blend equation.
     * Called by GlBlendEquationMixin to apply after blend function changes.
     */
    public static int getCurrentEquation() {
        return currentEquation;
    }
    
    /**
     * Returns true if blending should be disabled entirely.
     */
    public static boolean isBlendDisabled() {
        return blendDisabled;
    }
    
    /**
     * Set a custom blend equation.
     * 
     * @param equation One of GL_FUNC_ADD, GL_FUNC_SUBTRACT, GL_FUNC_REVERSE_SUBTRACT, GL_MIN, GL_MAX,
     *                 or -1 (BLEND_DISABLED) to disable blending entirely
     */
    public static void setBlendEquation(int equation) {
        if (equation == BLEND_DISABLED) {
            blendDisabled = true;
            currentEquation = GL14.GL_FUNC_ADD;  // Store a valid equation
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        } else {
            blendDisabled = false;
            currentEquation = equation;
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            GL14.glBlendEquation(equation);
        }
    }
    
    /**
     * Set additive blend equation (default).
     * result = src * srcFactor + dst * dstFactor
     */
    public static void setAdditive() {
        setBlendEquation(GL14.GL_FUNC_ADD);
    }
    
    /**
     * Set subtractive blend equation.
     * result = src * srcFactor - dst * dstFactor
     * 
     * <p>Note: This subtracts the DESTINATION from the SOURCE.
     * For darkening effects, use {@link #setReverseSubtractive()} instead.
     */
    public static void setSubtractive() {
        setBlendEquation(GL14.GL_FUNC_SUBTRACT);
    }
    
    /**
     * Set reverse subtractive blend equation.
     * result = dst * dstFactor - src * srcFactor
     * 
     * <p>This is the equation for DARKENING effects:
     * - With blendFunc(ONE, ONE): result = framebuffer - fragmentOutput
     * - Fragment output of (0.5, 0.0, 0.0) would SUBTRACT 0.5 red from the scene
     */
    public static void setReverseSubtractive() {
        setBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
    }
    
    /**
     * Set MIN blend equation.
     * result = min(src, dst)
     */
    public static void setMin() {
        setBlendEquation(GL14.GL_MIN);
    }
    
    /**
     * Set MAX blend equation.
     * result = max(src, dst)
     */
    public static void setMax() {
        setBlendEquation(GL14.GL_MAX);
    }
    
    /**
     * Reset to default additive blend equation.
     * Should be called after any custom blend equation usage.
     */
    public static void resetBlendEquation() {
        setAdditive();
    }
    
    /**
     * Temporarily use a blend equation and reset after the runnable completes.
     * 
     * <pre>{@code
     * BlendEquationManager.withEquation(GL14.GL_FUNC_REVERSE_SUBTRACT, () -> {
     *     // Render with subtractive blending
     * });
     * // Automatically reset to GL_FUNC_ADD
     * }</pre>
     */
    public static void withEquation(int equation, Runnable action) {
        int previous = currentEquation;
        try {
            setBlendEquation(equation);
            action.run();
        } finally {
            setBlendEquation(previous);
        }
    }
}
