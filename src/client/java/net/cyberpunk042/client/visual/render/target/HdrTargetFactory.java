package net.cyberpunk042.client.visual.render.target;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebufferFactory;

/**
 * Factory for creating framebuffers with custom texture formats (e.g., RGBA16F for HDR).
 * 
 * <h3>MC 1.21+ API</h3>
 * <p>Minecraft 1.21+ uses {@link SimpleFramebufferFactory} to create framebuffers.
 * This factory wraps that API and provides format injection via mixin.
 * 
 * <h3>Pattern: ThreadLocal Format Injection</h3>
 * <p>Inspired by Satin's approach, this factory uses a ThreadLocal to pass the desired
 * format to the framebuffer constructor, where a mixin intercepts and applies it.
 * 
 * <h3>Usage:</h3>
 * <pre>
 * // Create an HDR framebuffer
 * HdrTarget target = HdrTargetFactory.createHdr("glow", width, height, false);
 * 
 * // Use the framebuffer
 * target.factory().prepare(target.framebuffer());
 * // ... render ...
 * 
 * // Cleanup when done
 * target.close();
 * </pre>
 * 
 * @see TextureFormat
 * @see net.cyberpunk042.mixin.client.FramebufferFormatMixin
 */
public final class HdrTargetFactory {
    
    private static final ThreadLocal<TextureFormat> PENDING_FORMAT = new ThreadLocal<>();
    
    /** Default clear color (transparent black) */
    private static final int CLEAR_COLOR = 0x00000000;
    
    private HdrTargetFactory() {
        // Utility class - no instantiation
    }
    
    /**
     * A managed HDR target that includes both the framebuffer and its factory.
     * 
     * <p>The factory is needed for proper lifecycle management (prepare, close).
     * 
     * @param name Unique name for this target (for debugging)
     * @param factory The factory that created this framebuffer
     * @param framebuffer The actual framebuffer
     * @param format The texture format used
     */
    public record HdrTarget(
        String name,
        SimpleFramebufferFactory factory,
        Framebuffer framebuffer,
        TextureFormat format
    ) {
        /**
         * Prepare the framebuffer for rendering (clears, binds).
         */
        public void prepare() {
            factory.prepare(framebuffer);
        }
        
        /**
         * Close and release resources.
         */
        public void close() {
            factory.close(framebuffer);
        }
    }
    
    /**
     * Create a framebuffer with custom texture format.
     * 
     * @param name Unique name for this target (for debugging/logging)
     * @param width Framebuffer width in pixels
     * @param height Framebuffer height in pixels
     * @param useDepth Whether to create a depth attachment
     * @param format Texture format (RGBA8, RGBA16F, etc.), null defaults to RGBA8
     * @return New HdrTarget containing the framebuffer and its factory
     */
    public static HdrTarget create(String name, int width, int height, boolean useDepth, TextureFormat format) {
        if (format == null) {
            format = TextureFormat.RGBA8;
        }
        
        // Set the pending format before factory creates the framebuffer
        PENDING_FORMAT.set(format);
        try {
            // Create factory - the mixin will intercept when factory.create() is called
            SimpleFramebufferFactory factory = new SimpleFramebufferFactory(width, height, useDepth, CLEAR_COLOR);
            Framebuffer fb = factory.create();
            
            Logging.RENDER.topic("hdr_target")
                .kv("name", name)
                .kv("format", format.jsonName())
                .kv("size", width + "x" + height)
                .kv("depth", useDepth)
                .debug("Created custom format framebuffer");
            
            return new HdrTarget(name, factory, fb, format);
        } finally {
            PENDING_FORMAT.remove();
        }
    }
    
    /**
     * Convenience method for creating RGBA16F (HDR) framebuffers.
     * 
     * @param name Unique name for this target
     * @param width Framebuffer width in pixels
     * @param height Framebuffer height in pixels
     * @param useDepth Whether to create a depth attachment
     * @return New HdrTarget with RGBA16F format
     */
    public static HdrTarget createHdr(String name, int width, int height, boolean useDepth) {
        return create(name, width, height, useDepth, TextureFormat.RGBA16F);
    }
    
    /**
     * Called by FramebufferFormatMixin to get the pending format.
     * 
     * <p>This consumes the format (removes from ThreadLocal), so it should
     * only be called once per framebuffer construction.
     * 
     * @return The pending format, or null if no custom format was requested
     */
    public static TextureFormat consumePendingFormat() {
        TextureFormat format = PENDING_FORMAT.get();
        PENDING_FORMAT.remove();
        return format;
    }
    
    /**
     * Prepare format for the next framebuffer construction.
     * 
     * <p>Used for manual format injection in complex scenarios.
     * 
     * @param format The format to use for the next framebuffer
     */
    public static void prepareFormat(TextureFormat format) {
        PENDING_FORMAT.set(format);
    }
    
    /**
     * Clear any pending format without consuming it.
     * 
     * <p>Call this in finally blocks to ensure cleanup after errors.
     */
    public static void clearPendingFormat() {
        PENDING_FORMAT.remove();
    }
    
    /**
     * Check if there's a pending format set.
     * 
     * @return true if a format is waiting to be applied
     */
    public static boolean hasPendingFormat() {
        return PENDING_FORMAT.get() != null;
    }
    
    /**
     * Peek at the pending format without consuming it.
     * 
     * <p>Use this when you need to check the format but may need it again
     * (e.g., in a mixin that's called multiple times during texture creation).
     * 
     * @return The pending format, or null if none is set
     */
    public static TextureFormat getPendingFormat() {
        return PENDING_FORMAT.get();
    }
}
