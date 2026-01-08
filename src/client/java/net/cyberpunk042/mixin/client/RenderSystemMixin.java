package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.client.visual.shader.CustomUniformBinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject custom uniform binding into RenderSystem.bindDefaultUniforms().
 * 
 * <p>This is the PERFECT injection point because:
 * <ul>
 *   <li>bindDefaultUniforms is called for EVERY RenderPass that needs uniforms</li>
 *   <li>It's a static method on a concrete class (easy to mixin)</li>
 *   <li>We get the RenderPass as a parameter - no local capture needed!</li>
 *   <li>Happens BEFORE geometry is drawn but AFTER the pipeline is set</li>
 * </ul>
 */
@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    
    /**
     * Inject at the TAIL of bindDefaultUniforms to add our custom uniform binding.
     * 
     * <p>After Minecraft binds Projection, Fog, Globals, and Lighting uniforms,
     * we add our FresnelParams and CoronaParams uniforms if applicable.</p>
     */
    @Inject(
        method = "bindDefaultUniforms",
        at = @At("TAIL")
    )
    private static void onBindDefaultUniforms(RenderPass pass, CallbackInfo ci) {        
        // Bind our custom uniforms  
        CustomUniformBinder.bindCustomUniforms(pass);
    }
}
