package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.TextureFormat;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.log.Logging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept texture format conversion and inject RGBA16F.
 */
@Mixin(GlConst.class)
public class GlConstFormatMixin {
    
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_RGBA16F = 0x881A;
    private static final String TAG = "HDR_GL";
    
    @Unique
    private static int theVirusBlock$substitutionCount = 0;
    
    @Inject(
        method = "toGlInternalId",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private static void theVirusBlock$injectHdrFormat(TextureFormat format, CallbackInfoReturnable<Integer> cir) {
        boolean hasPending = HdrTargetFactory.hasPendingFormat();
        
        // Only process RGBA8 calls with pending format
        if (format == TextureFormat.RGBA8 && hasPending) {
            var hdrFormat = HdrTargetFactory.getPendingFormat();
            if (hdrFormat != null && hdrFormat == net.cyberpunk042.client.visual.render.target.TextureFormat.RGBA16F) {
                theVirusBlock$substitutionCount++;
                Logging.RENDER.topic(TAG)
                    .kv("substitutionNum", theVirusBlock$substitutionCount)
                    .info("[FORMAT_SUBSTITUTED] RGBA8->RGBA16F (0x8058->0x881A)");
                cir.setReturnValue(GL_RGBA16F);
            }
        }
    }
}
