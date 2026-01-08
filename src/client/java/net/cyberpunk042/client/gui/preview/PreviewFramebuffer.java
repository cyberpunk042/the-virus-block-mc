package net.cyberpunk042.client.gui.preview;

import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.SimpleFramebufferFactory;
import net.cyberpunk042.log.Logging;
import org.jetbrains.annotations.Nullable;

/**
 * Manages an offscreen framebuffer for rendering field previews.
 * 
 * <p>This allows rendering fields using the real FieldRenderer pipeline
 * (which requires world-space rendering context) and then displaying
 * the result as a texture in the GUI.
 * 
 * <p>Updated for Minecraft 1.21 API which uses {@link SimpleFramebufferFactory}
 * and returns {@link GpuTexture} from getColorAttachment().
 * 
 * <h2>Lifecycle</h2>
 * <pre>
 * PreviewFramebuffer fb = new PreviewFramebuffer();
 * 
 * // Each frame:
 * fb.ensureSize(width, height);
 * Framebuffer fbo = fb.getFramebuffer();
 * // ... render to fbo using FieldRenderer ...
 * 
 * // Display:
 * GpuTexture texture = fb.getColorTexture();
 * // ... draw texture to GUI ...
 * 
 * // On screen close:
 * fb.close();
 * </pre>
 * 
 * @see FieldPreviewRenderer
 */
@Environment(EnvType.CLIENT)
public class PreviewFramebuffer {
    
    private static final String NAME = "field_preview";
    private static final int CLEAR_COLOR = 0xFF1E1E2E;  // Opaque dark background (ARGB)
    
    private SimpleFramebufferFactory factory;
    private Framebuffer framebuffer;
    private int width;
    private int height;
    private boolean initialized = false;
    
    /**
     * Creates a new preview framebuffer manager.
     * The actual framebuffer is created lazily on first use.
     */
    public PreviewFramebuffer() {
        // Lazy initialization - framebuffer created in ensureSize()
    }
    
    /**
     * Ensures the framebuffer exists and matches the requested size.
     * Creates the framebuffer on first call, resizes if dimensions change.
     * 
     * @param width Desired width in pixels
     * @param height Desired height in pixels
     */
    public void ensureSize(int width, int height) {
        // Clamp to reasonable values
        width = Math.max(1, Math.min(width, 4096));
        height = Math.max(1, Math.min(height, 4096));
        
        if (framebuffer == null || this.width != width || this.height != height) {
            // Need to create or resize
            createFramebuffer(width, height);
        }
    }
    
    /**
     * Creates a new framebuffer with the specified dimensions.
     * Uses the 1.21 SimpleFramebufferFactory API.
     */
    private void createFramebuffer(int width, int height) {
        // Close existing framebuffer if any
        if (framebuffer != null && factory != null) {
            factory.close(framebuffer);
        }
        
        this.width = width;
        this.height = height;
        
        // Create factory and framebuffer
        // SimpleFramebufferFactory(width, height, useDepth, clearColor)
        this.factory = new SimpleFramebufferFactory(width, height, true, CLEAR_COLOR);
        this.framebuffer = factory.create();
        this.initialized = true;
        
        Logging.GUI.topic("preview").debug("Created preview framebuffer: {}x{}", width, height);
    }
    
    /**
     * Prepares the framebuffer for rendering.
     * Call this before rendering to the framebuffer.
     */
    public void prepare() {
        if (framebuffer != null && factory != null) {
            factory.prepare(framebuffer);
        }
    }
    
    /**
     * Gets the color texture attachment.
     * This texture contains the rendered content and can be used for display.
     * 
     * @return The GpuTexture, or null if not initialized
     */
    @Nullable
    public GpuTexture getColorTexture() {
        if (framebuffer == null) {
            return null;
        }
        return framebuffer.getColorAttachment();
    }
    
    /**
     * Gets the framebuffer instance for rendering.
     * 
     * @return The underlying framebuffer, or null if not initialized
     */
    @Nullable
    public Framebuffer getFramebuffer() {
        return framebuffer;
    }
    
    /**
     * Gets the current width of the framebuffer.
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the current height of the framebuffer.
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Whether the framebuffer has been initialized.
     */
    public boolean isInitialized() {
        return initialized && framebuffer != null;
    }
    
    /**
     * Releases the framebuffer resources.
     * Call when the preview is no longer needed (e.g., screen close).
     */
    public void close() {
        if (framebuffer != null && factory != null) {
            factory.close(framebuffer);
            framebuffer = null;
            factory = null;
            initialized = false;
            Logging.GUI.topic("preview").debug("Closed preview framebuffer");
        }
    }
}
