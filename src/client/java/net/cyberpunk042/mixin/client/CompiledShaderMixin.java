package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.shader.util.ShaderPreprocessor;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.CompiledShader;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.shaders.ShaderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept shader source compilation and apply preprocessing.
 * 
 * <p>This enables #include directive support for GLSL shaders in our mod,
 * allowing modularization of large shader files.</p>
 */
@Mixin(CompiledShader.class)
public class CompiledShaderMixin {
    
    private static int callCount = 0;
    
    @Inject(
        method = "compile",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void theVirusBlock$preprocessShaderSource(
            Identifier id,
            ShaderType type,
            String source,
            CallbackInfoReturnable<CompiledShader> cir
    ) throws net.minecraft.client.gl.ShaderLoader.LoadException {
        callCount++;
        
        // Log generic interception to verify mixin is active
        if (callCount <= 5) {
            Logging.RENDER.topic("shader_mixin")
                .kv("call", callCount)
                .kv("id", id)
                .info("CompiledShader.compile intercepted");
        }
        
        if (source == null) return;
        
        // Check for includes - Fast path
        if (!ShaderPreprocessor.hasIncludes(source)) {
            return;
        }
        
        // Only process our namespace
        if (id == null || !"the-virus-block".equals(id.getNamespace())) {
            return;
        }
        
        Logging.RENDER.topic("shader_preprocess")
            .kv("shader", id)
            .info("Processor active on shader with includes");
        
        try {
            String processed = ShaderPreprocessor.process(source, id);
            
            // CRITICAL CHECK: If processing failed or changed nothing, 
            // DO NOT recurse, otherwise StackOverflow!
            if (processed == null || processed.equals(source)) {
                return; 
            }
            
            int originalLines = source.split("\n", -1).length;
            int processedLines = processed.split("\n", -1).length;
            
            Logging.RENDER.topic("shader_preprocess")
                .kv("lines_in", originalLines)
                .kv("lines_out", processedLines)
                .info("Includes resolved successfully");
            
            // Recurse with processed source
            // Since includes are removed, the next call will hit !hasIncludes(source) and return early
            CompiledShader result = CompiledShader.compile(id, type, processed);
            
            cir.setReturnValue(result);
            
        } catch (Exception e) {
            Logging.RENDER.topic("shader_preprocess")
                .kv("shader", id)
                .kv("error", e.getMessage())
                .error("Preprocessing failed - falling back to original");
            // Do not start recursion on failure
        }
    }
}
