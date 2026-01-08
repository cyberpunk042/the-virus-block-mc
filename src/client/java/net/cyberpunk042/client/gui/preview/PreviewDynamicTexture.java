package net.cyberpunk042.client.gui.preview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

/**
 * Manages a dynamic texture that captures framebuffer content via glReadPixels.
 * 
 * This reads pixels from the FBO into a NativeImage, which can then be
 * properly rendered using DrawContext.drawTexture().
 */
public class PreviewDynamicTexture {
    
    private static final Identifier PREVIEW_TEXTURE_ID = Identifier.of("the-virus-block", "gui/preview_framebuffer");
    private static PreviewDynamicTexture instance;
    private static boolean registered = false;
    
    private NativeImage image;
    private NativeImageBackedTexture texture;
    private int lastWidth = 0;
    private int lastHeight = 0;
    
    private PreviewDynamicTexture() {}
    
    /**
     * Gets the singleton instance.
     */
    public static PreviewDynamicTexture getInstance() {
        if (instance == null) {
            instance = new PreviewDynamicTexture();
        }
        return instance;
    }
    
    /**
     * Gets the Identifier used to reference this texture.
     */
    public static Identifier getTextureId() {
        return PREVIEW_TEXTURE_ID;
    }
    
    /**
     * Captures the content of the currently bound framebuffer and uploads it.
     * Call this after rendering to the FBO, before unbinding it.
     * 
     * @param width FBO width
     * @param height FBO height
     */
    public void captureFromBoundFbo(int width, int height) {
        if (width <= 0 || height <= 0) return;
        
        // Resize image if needed
        if (image == null || lastWidth != width || lastHeight != height) {
            if (image != null) {
                image.close();
            }
            image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            lastWidth = width;
            lastHeight = height;
            
            // Also recreate the texture
            if (texture != null) {
                texture.close();
            }
            texture = new NativeImageBackedTexture(PREVIEW_TEXTURE_ID::toString, image);
            
            // Register with texture manager
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(PREVIEW_TEXTURE_ID, texture);
                registered = true;
            }
        }
        
        // Read pixels from the currently bound framebuffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        // Copy to NativeImage (note: OpenGL has origin at bottom-left, need to flip Y)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcY = height - 1 - y;  // Flip Y
                int srcIndex = (srcY * width + x) * 4;
                int r = buffer.get(srcIndex) & 0xFF;
                int g = buffer.get(srcIndex + 1) & 0xFF;
                int b = buffer.get(srcIndex + 2) & 0xFF;
                // Force alpha to 255 (fully opaque) - prevents world showing through
                int a = 255;
                // setColorArgb expects ARGB format
                int color = (a << 24) | (r << 16) | (g << 8) | b;  // ARGB
                image.setColorArgb(x, y, color);
            }
        }
        
        // Upload to GPU
        texture.upload();
    }
    
    /**
     * Returns whether this texture is ready to use.
     */
    public boolean isReady() {
        return registered && texture != null;
    }
    
    /**
     * Cleanup resources.
     */
    public void close() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
        if (image != null) {
            image.close();
            image = null;
        }
        registered = false;
    }
}


