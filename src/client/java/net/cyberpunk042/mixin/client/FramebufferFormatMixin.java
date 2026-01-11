package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin to support custom texture formats (RGBA16F, etc.) for framebuffers.
 * 
 * <p>Works in conjunction with {@link HdrTargetFactory} to inject format
 * at texture creation time via ThreadLocal pattern.
 * 
 * <h3>Pattern: Satin-Style Format Injection</h3>
 * <p>This mixin intercepts initFbo() and modifies the internal format
 * parameter of glTexImage2D, allowing RGBA16F (HDR) instead of RGBA8.
 * 
 * <h3>How It Works:</h3>
 * <ol>
 *   <li>{@link HdrTargetFactory#create} sets format in ThreadLocal</li>
 *   <li>Factory creates SimpleFramebuffer</li>
 *   <li>initFbo() is called which calls glTexImage2D</li>
 *   <li>This mixin intercepts glTexImage2D and substitutes the format</li>
 * </ol>
 * 
 * @see HdrTargetFactory
 * @see TextureFormat
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferFormatMixin {
    
    /**
     * The OpenGL internal format for this framebuffer's color texture.
     * Captured from ThreadLocal when initFbo is called.
     */
    @Unique
    private int theVirusBlock$internalFormat = -1;
    
    /**
     * Modify the internal format parameter of glTexImage2D.
     * 
     * <p>The initFbo method calls GlStateManager._texImage2D with:
     * <pre>
     * _texImage2D(target, level, internalFormat, width, height, border, format, type, data)
     * </pre>
     * We intercept the 3rd argument (internalFormat, index 2).
     * 
     * <p>This allows us to substitute RGBA8 (default) with RGBA16F or other formats.
     */
    @ModifyArg(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
        ),
        index = 2,  // internalFormat parameter
        require = 0  // Don't crash if target not found (MC API may change)
    )
    private int theVirusBlock$modifyInternalFormat(int originalFormat) {
        // Check if there's a pending format from HdrTargetFactory
        if (theVirusBlock$internalFormat < 0) {
            TextureFormat format = HdrTargetFactory.consumePendingFormat();
            if (format != null) {
                theVirusBlock$internalFormat = format.glConstant();
                Logging.RENDER.topic("hdr_mixin")
                    .kv("format", format.jsonName())
                    .kv("glConstant", String.format("0x%04X", format.glConstant()))
                    .debug("Captured HDR format from ThreadLocal");
            }
        }
        
        // Only substitute if we have a custom format
        if (theVirusBlock$internalFormat > 0 && theVirusBlock$internalFormat != GL11.GL_RGBA8) {
            Logging.RENDER.topic("hdr_mixin")
                .kv("original", String.format("0x%04X", originalFormat))
                .kv("new", String.format("0x%04X", theVirusBlock$internalFormat))
                .info("Applied HDR format to framebuffer");
            return theVirusBlock$internalFormat;
        }
        return originalFormat;
    }
}

