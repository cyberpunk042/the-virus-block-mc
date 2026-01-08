package net.cyberpunk042.client.gui.preview;

/**
 * Interface for accessing FBO-related functionality injected into Framebuffer.
 * 
 * Cast a Framebuffer to this interface to access the injected methods.
 */
public interface FramebufferFboAccess {
    
    /**
     * Gets the OpenGL FBO ID for this framebuffer.
     * 
     * @return The FBO ID, or -1 if not initialized
     */
    int theVirusBlock$getFboId();
    
    /**
     * Binds this framebuffer as the current OpenGL render target.
     */
    void theVirusBlock$bind();
}
