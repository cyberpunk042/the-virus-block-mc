package net.cyberpunk042.client.visual.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import net.cyberpunk042.client.visual.shader.FresnelRenderLayers;
import net.cyberpunk042.visual.effect.HorizonEffect;
import net.cyberpunk042.visual.layer.BlendMode;
import org.lwjgl.opengl.GL14;

import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Custom render layers and blend utilities for field visual effects.
 * 
 * <h2>Available Blend Modes</h2>
 * <ul>
 *   <li><b>NORMAL</b> - Standard alpha blending (src*alpha + dst*(1-alpha))</li>
 *   <li><b>ADD</b> - Additive blending (src*alpha + dst) - glows, light effects</li>
 *   <li><b>MULTIPLY</b> - Multiplicative blending (src*dst) - shadows, darkening</li>
 *   <li><b>SCREEN</b> - Screen blending (1 - (1-src)*(1-dst)) - lightening</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Before rendering:
 * FieldRenderLayers.applyBlendMode("ADD");
 * // ... render geometry ...
 * FieldRenderLayers.resetBlendMode();
 * </pre>
 */
public final class FieldRenderLayers extends RenderPhase {
    
    private FieldRenderLayers() {
        super("field_render_layers", () -> {}, () -> {});
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Render Layers (using Minecraft's built-in layers)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final Identifier WHITE_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");
    
    // Single-sided layer WITH backface culling (for doubleSided=false)
    private static final RenderLayer SOLID_TRANSLUCENT_CULL = RenderLayer.getItemEntityTranslucentCull(WHITE_TEXTURE);
    
    // Double-sided layer WITHOUT backface culling (for doubleSided=true)
    // getEntityTranslucent has no culling by default
    private static final RenderLayer SOLID_TRANSLUCENT_NO_CULL = RenderLayer.getEntityTranslucent(WHITE_TEXTURE);
    
    // Default layer - use culled version for single-sided rendering
    private static final RenderLayer SOLID_TRANSLUCENT = SOLID_TRANSLUCENT_CULL;
    
    // For see-through mode, we use the no-cull layer but manually control depth write
    // via GL state before the buffer is flushed - see LayerRenderer
    private static final RenderLayer SOLID_TRANSLUCENT_NO_DEPTH = SOLID_TRANSLUCENT_NO_CULL;
    
    // Standard lines layer
    private static final RenderLayer LINES_LAYER = RenderLayer.getLines();
    
    /**
     * Translucent solid layer for field geometry with backface culling.
     * <p>This uses getItemEntityTranslucentCull which enables culling for
     * single-sided rendering (front faces only visible).</p>
     */
    public static RenderLayer solidTranslucent() {
        return SOLID_TRANSLUCENT;
    }
    
    /**
     * Translucent solid layer WITH backface culling (single-sided).
     * <p>Only front faces are visible. Use for doubleSided=false.</p>
     */
    public static RenderLayer solidTranslucentCull() {
        return SOLID_TRANSLUCENT_CULL;
    }
    
    /**
     * Translucent layer without depth writing (true see-through).
     * <p>Objects behind this will still be visible. Uses no-cull layer.</p>
     */
    public static RenderLayer solidTranslucentNoDepth() {
        return SOLID_TRANSLUCENT_NO_DEPTH;
    }
    
    /**
     * Translucent solid layer for double-sided geometry (no backface culling).
     * <p>Both front and back faces are visible. Use for doubleSided=true.</p>
     */
    public static RenderLayer solidTranslucentNoCull() {
        return SOLID_TRANSLUCENT_NO_CULL;
    }
    
    /**
     * Translucent solid layer with custom texture.
     */
    public static RenderLayer solidTranslucent(Identifier texture) {
        return RenderLayer.getEntityTranslucent(texture);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Fresnel Rim Effect Layers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Translucent layer with Fresnel rim lighting effect.
     * 
     * <p>Before using this layer, call {@link net.cyberpunk042.client.visual.shader.FresnelUniformManager#setParams}
     * to configure the rim color, power, and intensity.</p>
     * 
     * @return RenderLayer configured for Fresnel rendering
     * @see FresnelRenderLayers#fresnelTranslucent()
     */
    public static RenderLayer fresnelTranslucent() {
        return FresnelRenderLayers.fresnelTranslucent();
    }
    
    /**
     * Translucent layer with Fresnel rim lighting (no backface culling).
     * 
     * @return RenderLayer for double-sided Fresnel rendering
     */
    public static RenderLayer fresnelTranslucentNoCull() {
        return FresnelRenderLayers.fresnelTranslucentNoCull();
    }
    
    /**
     * Lines layer for wireframe rendering with default width (1.0).
     */
    public static RenderLayer lines() {
        return linesWithWidth(1.0f);
    }
    
    /**
     * Lines layer for wireframe rendering with custom width.
     * 
     * <p>Provides two discrete widths:</p>
     * <ul>
     *   <li>THIN (width < 1.0): Uses standard getLines() - 1px lines</li>
     *   <li>THICK (width >= 1.0): Uses getDebugLineStrip(3.0) - thicker lines</li>
     * </ul>
     * 
     * <p><b>Note:</b> OpenGL line width is GPU-dependent. We use LINE_STRIP for 
     * thick mode because getLines() doesn't support width parameter. The LINE_STRIP
     * draw mode connects vertices differently but works for our discrete line pairs.</p>
     * 
     * @param width Line width (0.01 to 10.0)
     * @return RenderLayer configured for line rendering
     */
    public static RenderLayer lines(float width) {
        // Always use custom layer for variable width support
        return linesWithWidth(width);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Custom Lines Layer with Variable Width
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Cache for custom line width layers (avoid recreating every frame)
    private static final Map<Float, RenderLayer> LINES_WIDTH_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Creates a discrete lines layer with custom width.
     * 
     * <p>Uses GL_LINES draw mode (not LINE_STRIP) so vertices are drawn as 
     * independent pairs: (v0,v1), (v2,v3), etc. - no unwanted connections.</p>
     * 
     * <p><b>Note:</b> Line width support depends on GPU/driver. Some systems
     * may clamp very thin or thick widths. This is a best-effort implementation.</p>
     * 
     * @param width Desired line width (0.01 to 10.0)
     * @return RenderLayer with discrete lines and custom width
     */
    public static RenderLayer linesWithWidth(float width) {
        // Clamp width to valid range
        float clampedWidth = Math.max(0.01f, Math.min(width, 10.0f));
        
        // Cache key: round to 0.01 for thin (<1), 0.1 for thick (>=1)
        float cacheKey;
        if (clampedWidth < 1.0f) {
            cacheKey = Math.round(clampedWidth * 100) / 100.0f; // 0.01 increments
        } else {
            cacheKey = Math.round(clampedWidth * 10) / 10.0f; // 0.1 increments
        }
        
        return LINES_WIDTH_CACHE.computeIfAbsent(cacheKey, w -> {
            // Create custom RenderLayer with LINES pipeline and LineWidth
            try {
                return RenderLayer.of(
                    "field_lines_" + w,
                    256,
                    RenderPipelines.LINES,
                    RenderLayer.MultiPhaseParameters.builder()
                        .lineWidth(new LineWidth(OptionalDouble.of(w)))
                        .build(false) // false = doesn't affect outline
                );
            } catch (Exception e) {
                // Fallback to standard lines if custom layer fails
                return LINES_LAYER;
            }
        });
    }
    
    /**
     * Lines render layer intended for no-depth rendering.
     * <p>Note: Caller must manually disable depth test before drawing
     * and restore after.</p>
     * 
     * @param width Desired line width (0.01 to 10.0)
     * @return RenderLayer for lines
     */
    public static RenderLayer linesNoDepthTest(float width) {
        // Just return regular lines layer - caller handles depth state
        return linesWithWidth(width);
    }
    
    // Legacy aliases
    public static RenderLayer glowTranslucent() { return SOLID_TRANSLUCENT; }
    public static RenderLayer glowAdditive() { return SOLID_TRANSLUCENT; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Depth Write Control (call before/after rendering primitive)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Disables depth writing for true translucent rendering.
     * 
     * <p>When depth write is disabled, the mesh will NOT block objects behind it
     * from rendering. This creates a true see-through effect rather than an
     * "invisibility shield" effect.</p>
     * 
     * <p>Call {@link #enableDepthWrite()} after rendering to restore normal behavior!</p>
     * 
     * @see #enableDepthWrite()
     */
    public static void disableDepthWrite() {
        GlStateManager._depthMask(false);
    }
    
    /**
     * Re-enables depth writing after translucent rendering.
     * 
     * <p>This restores the default behavior where rendered geometry writes to
     * the depth buffer, occluding objects behind it.</p>
     * 
     * @see #disableDepthWrite()
     */
    public static void enableDepthWrite() {
        GlStateManager._depthMask(true);
    }
    
    /**
     * Applies depth write state based on the provided flag.
     * 
     * @param depthWrite true to write to depth buffer (solid), false for true translucency
     */
    public static void applyDepthWrite(boolean depthWrite) {
        GlStateManager._depthMask(depthWrite);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Blend Mode Control (call before/after rendering)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Applies a blend mode before rendering.
     * Call {@link #resetBlendMode()} after rendering!
     * 
     * @param blendMode One of: "NORMAL", "ADD", "MULTIPLY", "SCREEN"
     */
    public static void applyBlendMode(String blendMode) {
        if (blendMode == null) blendMode = "NORMAL";
        
        GlStateManager._enableBlend();
        
        switch (blendMode.toUpperCase()) {
            case "ADD", "ADDITIVE" -> {
                // Additive: src*alpha + dst (glow effect)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_SRC_ALPHA,  // srcRGB
                    GL14.GL_ONE,        // dstRGB
                    GL14.GL_ONE,        // srcAlpha
                    GL14.GL_ONE         // dstAlpha
                );
            }
            case "MULTIPLY", "MULT" -> {
                // Multiply: src * dst (darkening)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_DST_COLOR,  // srcRGB
                    GL14.GL_ZERO,       // dstRGB
                    GL14.GL_ONE,        // srcAlpha
                    GL14.GL_ZERO        // dstAlpha
                );
            }
            case "SCREEN" -> {
                // Screen: 1 - (1-src)*(1-dst) (lightening)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_ONE,                    // srcRGB
                    GL14.GL_ONE_MINUS_SRC_COLOR,   // dstRGB
                    GL14.GL_ONE,                    // srcAlpha
                    GL14.GL_ONE_MINUS_SRC_ALPHA    // dstAlpha
                );
            }
            default -> {
                // Normal alpha: src*alpha + dst*(1-alpha)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_SRC_ALPHA,              // srcRGB
                    GL14.GL_ONE_MINUS_SRC_ALPHA,   // dstRGB
                    GL14.GL_ONE,                    // srcAlpha
                    GL14.GL_ONE_MINUS_SRC_ALPHA    // dstAlpha
                );
            }
        }
    }
    
    /**
     * Applies a blend mode before rendering.
     */
    public static void applyBlendMode(BlendMode mode) {
        applyBlendMode(mode.name());
    }
    
    /**
     * Resets blend mode to default after rendering.
     */
    public static void resetBlendMode() {
        // Reset to default alpha blend
        GlStateManager._blendFuncSeparate(
            GL14.GL_SRC_ALPHA,
            GL14.GL_ONE_MINUS_SRC_ALPHA,
            GL14.GL_ONE,
            GL14.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._disableBlend();
    }
    
    /**
     * Gets a render layer for the blend mode.
     * Note: All modes use the same base layer - blend is applied via applyBlendMode().
     */
    public static RenderLayer forBlendMode(String blendMode) {
        return SOLID_TRANSLUCENT;
    }
    
}
