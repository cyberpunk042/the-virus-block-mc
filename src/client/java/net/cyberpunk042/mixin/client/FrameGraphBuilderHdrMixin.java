package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.gui.config.RenderConfig;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.ObjectAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject HDR format around FrameGraphBuilder.run().
 */
@Mixin(FrameGraphBuilder.class)
public class FrameGraphBuilderHdrMixin {
    
    private static final String TAG = "HDR_FRAME";
    
    @Unique
    private static boolean theVirusBlock$formatWasSet = false;
    @Unique
    private static int theVirusBlock$runCount = 0;
    @Unique
    private static int theVirusBlock$hdrRunCount = 0;
    
    @Inject(
        method = "run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void theVirusBlock$beforeRun(ObjectAllocator allocator, FrameGraphBuilder.Profiler profiler, CallbackInfo ci) {
        theVirusBlock$runCount++;
        RenderConfig renderConfig = RenderConfig.get();
        boolean hdrEnabled = renderConfig.isHdrEnabled();
        
        if (hdrEnabled) {
            theVirusBlock$hdrRunCount++;
            HdrTargetFactory.prepareFormat(TextureFormat.RGBA16F);
            theVirusBlock$formatWasSet = true;
        }
    }
    
    @Inject(
        method = "run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void theVirusBlock$afterRun(ObjectAllocator allocator, FrameGraphBuilder.Profiler profiler, CallbackInfo ci) {
        if (theVirusBlock$formatWasSet) {
            HdrTargetFactory.clearPendingFormat();
            theVirusBlock$formatWasSet = false;
        }
    }
}
