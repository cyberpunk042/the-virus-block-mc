package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.shaders.ShaderType;
import net.cyberpunk042.client.visual.shader.util.ShaderPreprocessor;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept ShaderLoader.getSource() and apply preprocessing.
 * 
 * <p>Also handles field-specific shader identifiers by stripping #fieldId suffix
 * before file lookup, but using full identifier for caching uniqueness.</p>
 */
@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {
    
    /**
     * Thread-local to pass the original identifier (with #fieldId) for caching.
     */
    @Unique
    private static final ThreadLocal<Identifier> theVirusBlock$originalId = new ThreadLocal<>();
    
    /**
     * Modify the identifier at the start of getSource to strip _f_ suffix.
     * This allows Minecraft to find the actual shader file.
     */
    @ModifyVariable(
        method = "getSource",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Identifier theVirusBlock$stripFieldSuffixGetSource(Identifier id) {
        return theVirusBlock$stripFieldIdSuffix(id);
    }
    
    // NOTE: We do NOT mixin loadPostEffect() - we WANT the full _f_ suffix to go into
    // Minecraft's cache so each field gets its own cached processor.
    // We only strip the suffix when loading actual FILES (getSource, loadProcessor).
    
    /**
     * Common method to strip _f_ suffix from identifiers.
     */
    @Unique
    private Identifier theVirusBlock$stripFieldIdSuffix(Identifier id) {
        if (!"the-virus-block".equals(id.getNamespace())) {
            return id;
        }
        
        String path = id.getPath();
        // Look for _f_ separator (indicates field-specific shader)
        int sepIndex = path.indexOf("_f_");
        
        if (sepIndex != -1) {
            // Store original for later use in caching
            theVirusBlock$originalId.set(id);
            // Return stripped identifier for file lookup
            return Identifier.of(id.getNamespace(), path.substring(0, sepIndex));
        }
        
        theVirusBlock$originalId.set(null);
        return id;
    }

    /**
     * Intercept getSource RETURN to preprocess shader source.
     */
    @Inject(
        method = "getSource",
        at = @At("RETURN"),
        cancellable = true
    )
    private void theVirusBlock$preprocessShader(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        
        // Check if this is our namespace
        if (source == null || !"the-virus-block".equals(id.getNamespace())) {
            return;
        }
        
        // Get original identifier (with #fieldId) if present
        Identifier originalId = theVirusBlock$originalId.get();
        theVirusBlock$originalId.remove(); // Clean up
        
        // Use original id (with #fieldId) for cache key, but current id (stripped) for path resolution
        String cacheKey = originalId != null ? originalId.toString() : id.toString();
        
        // Fast path: check if source needs preprocessing  
        if (ShaderPreprocessor.hasIncludes(source)) {
            Logging.RENDER.topic("shader_preprocess")
                .kv("shader", id)
                .kv("hasFieldSuffix", originalId != null)
                .info("Preprocessing include directives");
                
            try {
                // Process source using id for path resolution, but cacheKey for uniqueness
                String processed = ShaderPreprocessor.process(source, id, cacheKey);
                
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
