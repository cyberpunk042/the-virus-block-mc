package net.cyberpunk042.mixin.client;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.Optional;

/**
 * Mixin for ShaderLoader.Cache inner class.
 * 
 * <p>Strips the _f_ field suffix (and _hdr/_ldr suffix) from identifiers when 
 * loading post effect JSON files, while preserving the full identifier for cache uniqueness.</p>
 * 
 * <p>Also registers the cache map with ShaderLoaderCacheHelper for clearing when HDR changes.</p>
 */
@Mixin(targets = "net.minecraft.client.gl.ShaderLoader$Cache")
public class ShaderLoaderCacheMixin {
    
    @Shadow
    Map<Identifier, Optional<PostEffectProcessor>> postEffectProcessors;
    
    /**
     * Strip _f_ suffix (and _hdr/_ldr) in loadProcessor to find the actual JSON file.
     * The cache key (with full suffix) is preserved at the ShaderLoader.loadPostEffect level.
     */
    @ModifyVariable(
        method = "loadProcessor",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Identifier theVirusBlock$stripFieldSuffixLoadProcessor(Identifier id) {
        // Register the map for cache clearing (only needs to happen once, but is safe to call repeatedly)
        net.cyberpunk042.client.visual.shader.util.ShaderLoaderCacheHelper.setProcessorsMap(postEffectProcessors);
        
        if (!"the-virus-block".equals(id.getNamespace())) {
            return id;
        }
        
        String path = id.getPath();
        int sepIndex = path.indexOf("_f_");
        
        if (sepIndex != -1) {
            // Return stripped identifier for JSON file lookup
            // This removes both _f_<fieldId> AND any _hdr/_ldr suffix
            return Identifier.of(id.getNamespace(), path.substring(0, sepIndex));
        }
        
        return id;
    }
}
