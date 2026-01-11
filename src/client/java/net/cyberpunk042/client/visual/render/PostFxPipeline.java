package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.client.gui.config.RenderConfig;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory.HdrTarget;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the multi-pass HDR post-effect pipeline with RUNTIME CONTROL.
 * 
 * <h3>Key Feature: In-Game HDR Toggle</h3>
 * <p>Unlike JSON-only approaches, this class supports toggling HDR on/off
 * at runtime without shader reload. Call {@link #invalidateTargets()} 
 * after changing {@link RenderConfig#setHdrEnabled(boolean)}.
 * 
 * <h3>Pipeline Stages:</h3>
 * <ol>
 *   <li>Main scene → Glow extraction → accumulation buffer</li>
 *   <li>Accumulation → Gaussian blur (ping-pong) → blur buffer</li>
 *   <li>Blur result → Final composite → main framebuffer</li>
 * </ol>
 * 
 * <h3>Format Resolution:</h3>
 * <ul>
 *   <li>HDR disabled → Always RGBA8 (fast LDR path)</li>
 *   <li>HDR enabled → RGBA16F for accumulation/blur targets</li>
 * </ul>
 * 
 * <h3>Usage:</h3>
 * <pre>
 * // In render loop:
 * PostFxPipeline pipeline = PostFxPipeline.getInstance();
 * pipeline.ensureTargetsReady(width, height);
 * 
 * // Render glow extraction to accumulation target
 * HdrTarget accum = pipeline.getGlowExtractTarget();
 * accum.prepare();
 * // ... render glow ...
 * 
 * // Blur passes
 * HdrTarget blurA = pipeline.getBlurPingTarget();
 * HdrTarget blurB = pipeline.getBlurPongTarget();
 * // ... ping-pong blur ...
 * 
 * // After HDR toggle:
 * RenderConfig.get().setHdrEnabled(newValue);
 * pipeline.invalidateTargets();  // Force recreation
 * </pre>
 */
public class PostFxPipeline {
    
    private static final PostFxPipeline INSTANCE = new PostFxPipeline();
    
    // =========================================================================
    // HDR TARGETS
    // =========================================================================
    
    /** Glow accumulation buffer - full resolution, RGBA16F when HDR enabled */
    @Nullable
    private HdrTarget glowExtractTarget;
    
    /** Blur ping buffer - scaled by blur quality, RGBA16F when HDR enabled */
    @Nullable
    private HdrTarget blurPingTarget;
    
    /** Blur pong buffer - scaled by blur quality, RGBA16F when HDR enabled */
    @Nullable
    private HdrTarget blurPongTarget;
    
    // =========================================================================
    // STATE TRACKING
    // =========================================================================
    
    /** Current framebuffer width (full resolution) */
    private int currentWidth = -1;
    
    /** Current framebuffer height (full resolution) */
    private int currentHeight = -1;
    
    /** Current blur width (scaled by quality) */
    private int currentBlurWidth = -1;
    
    /** Current blur height (scaled by quality) */
    private int currentBlurHeight = -1;
    
    /** Track HDR state to detect changes */
    private boolean currentHdrState = false;
    
    /** Track blur quality to detect changes */
    private float currentBlurQuality = 1.0f;
    
    /** Whether targets are valid and ready to use */
    private boolean targetsValid = false;
    
    /** Whether the pipeline has been initialized */
    private boolean initialized = false;
    
    // =========================================================================
    // SINGLETON
    // =========================================================================
    
    private PostFxPipeline() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton pipeline instance.
     */
    public static PostFxPipeline getInstance() {
        return INSTANCE;
    }
    
    // =========================================================================
    // PUBLIC API: TARGET ACCESS
    // =========================================================================
    
    /**
     * Get the glow extraction target (input for blur chain).
     * 
     * @return The glow/accumulation target, or null if not ready
     */
    @Nullable
    public HdrTarget getGlowExtractTarget() {
        return glowExtractTarget;
    }
    
    /**
     * Get blur ping target (for ping-pong blur).
     * 
     * @return The blur ping target, or null if not ready
     */
    @Nullable
    public HdrTarget getBlurPingTarget() {
        return blurPingTarget;
    }
    
    /**
     * Get blur pong target (for ping-pong blur).
     * 
     * @return The blur pong target, or null if not ready
     */
    @Nullable
    public HdrTarget getBlurPongTarget() {
        return blurPongTarget;
    }
    
    /**
     * Get the underlying framebuffer for glow extraction.
     * Convenience method for direct Framebuffer access.
     */
    @Nullable
    public Framebuffer getGlowFramebuffer() {
        return glowExtractTarget != null ? glowExtractTarget.framebuffer() : null;
    }
    
    /**
     * Get the underlying framebuffer for blur ping.
     */
    @Nullable
    public Framebuffer getBlurPingFramebuffer() {
        return blurPingTarget != null ? blurPingTarget.framebuffer() : null;
    }
    
    /**
     * Get the underlying framebuffer for blur pong.
     */
    @Nullable
    public Framebuffer getBlurPongFramebuffer() {
        return blurPongTarget != null ? blurPongTarget.framebuffer() : null;
    }
    
    // =========================================================================
    // PUBLIC API: STATE QUERIES
    // =========================================================================
    
    /**
     * Check if HDR is currently active.
     * 
     * @return true if current targets are RGBA16F
     */
    public boolean isHdrActive() {
        return currentHdrState;
    }
    
    /**
     * Check if targets are ready for use.
     * 
     * @return true if targets exist and are valid
     */
    public boolean isReady() {
        return targetsValid && glowExtractTarget != null;
    }
    
    /**
     * Get current target dimensions.
     * 
     * @return [width, height] of full-resolution targets
     */
    public int[] getDimensions() {
        return new int[] { currentWidth, currentHeight };
    }
    
    /**
     * Get current blur dimensions.
     * 
     * @return [width, height] of blur targets
     */
    public int[] getBlurDimensions() {
        return new int[] { currentBlurWidth, currentBlurHeight };
    }
    
    /**
     * Get the current texture format being used.
     */
    public TextureFormat getCurrentFormat() {
        return currentHdrState ? TextureFormat.RGBA16F : TextureFormat.RGBA8;
    }
    
    // =========================================================================
    // PUBLIC API: LIFECYCLE
    // =========================================================================
    
    /**
     * Invalidate targets - call after HDR toggle or quality change.
     * 
     * <p>Next call to {@link #ensureTargetsReady} will recreate targets
     * with the new format/quality settings.
     */
    public void invalidateTargets() {
        targetsValid = false;
        Logging.RENDER.topic("hdr_pipeline")
            .debug("Targets invalidated - will recreate on next render");
    }
    
    /**
     * Ensure targets exist and match current size/format.
     * 
     * <p>Call at start of each render frame before using targets.
     * Automatically handles:
     * <ul>
     *   <li>Initial creation</li>
     *   <li>Resolution changes</li>
     *   <li>HDR toggle (after invalidateTargets())</li>
     *   <li>Blur quality changes</li>
     * </ul>
     * 
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     */
    public void ensureTargetsReady(int width, int height) {
        RenderConfig config = RenderConfig.get();
        boolean hdrEnabled = config.isHdrEnabled();
        float blurQuality = config.getBlurQuality();
        
        // Calculate blur dimensions
        int blurWidth = Math.max(1, (int)(width * blurQuality));
        int blurHeight = Math.max(1, (int)(height * blurQuality));
        
        // Check if recreation needed
        boolean sizeChanged = (width != currentWidth || height != currentHeight);
        boolean blurSizeChanged = (blurWidth != currentBlurWidth || blurHeight != currentBlurHeight);
        boolean hdrChanged = (hdrEnabled != currentHdrState);
        boolean qualityChanged = (Math.abs(blurQuality - currentBlurQuality) > 0.001f);
        
        if (!targetsValid || sizeChanged || blurSizeChanged || hdrChanged || qualityChanged) {
            recreateTargets(width, height, blurWidth, blurHeight, hdrEnabled, blurQuality);
            
            currentWidth = width;
            currentHeight = height;
            currentBlurWidth = blurWidth;
            currentBlurHeight = blurHeight;
            currentHdrState = hdrEnabled;
            currentBlurQuality = blurQuality;
            targetsValid = true;
            initialized = true;
        }
    }
    
    /**
     * Cleanup all targets. Call on mod shutdown.
     */
    public void cleanup() {
        closeTarget(glowExtractTarget);
        closeTarget(blurPingTarget);
        closeTarget(blurPongTarget);
        
        glowExtractTarget = null;
        blurPingTarget = null;
        blurPongTarget = null;
        
        targetsValid = false;
        initialized = false;
        
        Logging.RENDER.topic("hdr_pipeline")
            .debug("Pipeline cleaned up");
    }
    
    // =========================================================================
    // PUBLIC API: UTILITY
    // =========================================================================
    
    /**
     * Copy depth from main framebuffer to a target.
     * 
     * <p>Used for depth-aware effects (occlusion, etc).
     * 
     * @param target The target to copy depth to
     */
    public void copyDepthToTarget(HdrTarget target) {
        if (target == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainFb = client.getFramebuffer();
        
        if (mainFb != null) {
            target.framebuffer().copyDepthFrom(mainFb);
        }
    }
    
    /**
     * Copy depth from main framebuffer to the glow extraction target.
     */
    public void copyDepthToGlow() {
        copyDepthToTarget(glowExtractTarget);
    }
    
    // =========================================================================
    // INTERNALS
    // =========================================================================
    
    private void recreateTargets(int width, int height, int blurWidth, int blurHeight,
                                  boolean useHdr, float quality) {
        // Close old targets first
        closeTarget(glowExtractTarget);
        closeTarget(blurPingTarget);
        closeTarget(blurPongTarget);
        
        // Choose format based on runtime config
        TextureFormat format = useHdr ? TextureFormat.RGBA16F : TextureFormat.RGBA8;
        
        // Create targets with appropriate format
        glowExtractTarget = HdrTargetFactory.create("hdr_glow", width, height, false, format);
        blurPingTarget = HdrTargetFactory.create("hdr_blur_ping", blurWidth, blurHeight, false, format);
        blurPongTarget = HdrTargetFactory.create("hdr_blur_pong", blurWidth, blurHeight, false, format);
        
        Logging.RENDER.topic("hdr_pipeline")
            .kv("format", format.jsonName())
            .kv("fullSize", width + "x" + height)
            .kv("blurSize", blurWidth + "x" + blurHeight)
            .kv("quality", String.format("%.2f", quality))
            .info("Recreated HDR targets");
    }
    
    private void closeTarget(@Nullable HdrTarget target) {
        if (target != null) {
            try {
                target.close();
            } catch (Exception e) {
                Logging.RENDER.topic("hdr_pipeline")
                    .kv("target", target.name())
                    .warn("Error closing target: {}", e.getMessage());
            }
        }
    }
    
    // =========================================================================
    // DEBUG
    // =========================================================================
    
    /**
     * Get debug info about the current pipeline state.
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("HDR Pipeline:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Targets Valid: ").append(targetsValid).append("\n");
        sb.append("  HDR Enabled: ").append(currentHdrState).append("\n");
        sb.append("  Format: ").append(getCurrentFormat().jsonName()).append("\n");
        sb.append("  Full Size: ").append(currentWidth).append("x").append(currentHeight).append("\n");
        sb.append("  Blur Size: ").append(currentBlurWidth).append("x").append(currentBlurHeight).append("\n");
        sb.append("  Blur Quality: ").append(String.format("%.2f", currentBlurQuality)).append("\n");
        return sb.toString();
    }
}
