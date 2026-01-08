package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.shaders.ShaderType;
import net.cyberpunk042.client.visual.shader.util.ShaderPreprocessor;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept ShaderLoader.getSource() and apply preprocessing.
 * 
 * <p>Modern versions of Minecraft renamed 'loadShader' to 'getSource'.
 * This mixin targets: String getSource(Identifier id, ShaderType type)</p>
 */
@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    /**
     * Intercept getSource to preprocess shader source before it's returned to the renderer.
     * 
     * Target: String getSource(Identifier id, ShaderType type)
     */
    @Inject(
        method = "getSource",
        at = @At("RETURN"),
        cancellable = true
    )
    private void theVirusBlock$preprocessShader(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        
        // Fast path: valid source, matching namespace, has includes
        if (source != null && 
            "the-virus-block".equals(id.getNamespace()) && 
            ShaderPreprocessor.hasIncludes(source)) {
            
            Logging.RENDER.topic("shader_preprocess")
                .kv("shader", id)
                .info("Preprocessing include directives");
                
            try {
                // Process source
                // NOTE: ShaderPreprocessor requires MinecraftClient instance for resource loading
                // This is safe here as ShaderLoader runs on render thread
                String processed = ShaderPreprocessor.process(source, id);
                
                if (processed != null && !processed.equals(source)) {
                    cir.setReturnValue(processed);
                }
            } catch (Exception e) {
                Logging.RENDER.topic("shader_preprocess")
                    .kv("shader", id)
                    .kv("error", e.getMessage())
                    .error("Failed to preprocess shader");
            }
        }
    }
}
