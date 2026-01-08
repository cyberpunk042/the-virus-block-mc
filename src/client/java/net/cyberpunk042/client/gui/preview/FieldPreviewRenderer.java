package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.client.gui.state.DefinitionBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.FieldDefinition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

/**
 * Static facade for rendering field previews in GUI screens.
 * 
 * <p>This class provides the public API for field preview rendering,
 * handling animation calculation and delegating FBO rendering to
 * {@link PreviewFboRenderer}.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In your screen's render method:
 * FieldPreviewRenderer.drawField(context, state, x1, y1, x2, y2);
 * 
 * // When closing the screen:
 * FieldPreviewRenderer.disposeFramebuffer();
 * </pre>
 * 
 * @see PreviewFboRenderer
 */
@Environment(EnvType.CLIENT)
public class FieldPreviewRenderer {
    
    // Singleton FBO renderer instance
    private static PreviewFboRenderer fboRenderer = null;
    
    /**
     * Main entry point for field preview rendering.
     * 
     * <p>Calculates animations from the edit state and renders the field
     * using the real {@link net.cyberpunk042.client.field.render.FieldRenderer}
     * via an offscreen framebuffer.
     * 
     * @param context Draw context for GUI rendering
     * @param state Current field edit state
     * @param x1 Left bound of preview area
     * @param y1 Top bound of preview area
     * @param x2 Right bound of preview area
     * @param y2 Bottom bound of preview area
     * @param useFullRenderer Deprecated - ignored, always uses full 3D rendering
     */
    public static void drawField(DrawContext context, FieldEditState state,
                                 int x1, int y1, int x2, int y2,
                                 boolean useFullRenderer) {
        // Calculate animation time
        float timeTicks = (System.currentTimeMillis() % 100000) / 50f;
        
        // Calculate animation parameters from state
        float rotationX = calculateRotationX(state, timeTicks);
        float rotationY = calculateRotationY(state, timeTicks);
        float scale = calculateScale(state, timeTicks);
        
        // Get alpha from appearance
        float alpha = 1.0f;
        var appearance = state.appearance();
        if (appearance != null) {
            alpha = appearance.alphaMin();
        }
        
        // Build definition from state
        FieldDefinition definition = DefinitionBuilder.fromState(state);
        
        // Render via FBO renderer
        ensureRenderer();
        fboRenderer.render(context, definition, scale, rotationX, rotationY,
                          x1, y1, x2, y2, timeTicks, alpha);
    }
    
    /**
     * Draws a field preview (backward compatible overload).
     */
    public static void drawField(DrawContext context, FieldEditState state,
                                 int x1, int y1, int x2, int y2) {
        drawField(context, state, x1, y1, x2, y2, true);
    }
    
    /**
     * Disposes the cached framebuffer. Call when closing the screen.
     */
    public static void disposeFramebuffer() {
        if (fboRenderer != null) {
            fboRenderer.dispose();
            fboRenderer = null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE: Animation Calculation
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void ensureRenderer() {
        if (fboRenderer == null) {
            fboRenderer = new PreviewFboRenderer();
        }
    }
    
    /**
     * Calculates X rotation (pitch) from wobble animation.
     * Base tilt of 25° plus wobble modulation.
     */
    private static float calculateRotationX(FieldEditState state, float timeTicks) {
        float rotationX = 25f;  // Base tilt
        
        var wobble = state.wobble();
        if (wobble != null && wobble.isActive()) {
            var amp = wobble.amplitude();
            if (amp != null) {
                float speed = wobble.speed();
                rotationX += (float) Math.sin(timeTicks * speed * 0.1f) * amp.x() * 100f;
            }
        }
        
        return rotationX;
    }
    
    /**
     * Calculates Y rotation (yaw) from spin and wobble animations.
     */
    private static float calculateRotationY(FieldEditState state, float timeTicks) {
        float rotationY = 0f;
        
        // Spin animation (use Y-axis speed for preview rotation)
        var spin = state.spin();
        if (spin != null && spin.isActive()) {
            float speed = spin.speedY();  // Use Y-axis speed for yaw
            rotationY = (timeTicks * speed * 57.3f) % 360f;  // 57.3 = 180/PI
        }
        
        // Wobble modulation
        var wobble = state.wobble();
        if (wobble != null && wobble.isActive()) {
            var amp = wobble.amplitude();
            if (amp != null) {
                float speed = wobble.speed();
                rotationY += (float) Math.cos(timeTicks * speed * 0.1f) * amp.z() * 100f;
            }
        }
        
        return rotationY;
    }
    
    /**
     * Calculates scale from pulse/breathing animation.
     */
    private static float calculateScale(FieldEditState state, float timeTicks) {
        var pulse = state.pulse();
        if (pulse != null && pulse.isActive()) {
            return pulse.evaluate(timeTicks);
        }
        return 1.0f;
    }
}
