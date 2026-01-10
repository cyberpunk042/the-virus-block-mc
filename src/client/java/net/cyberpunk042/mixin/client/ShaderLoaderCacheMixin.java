package net.cyberpunk042.mixin.client;

import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

/**
 * Mixin for ShaderLoader.Cache inner class.
 * 
 * <p>Strips the _f_ field suffix from identifiers when loading post effect JSON files,
 * while preserving the full identifier for cache uniqueness.</p>
 */
@Mixin(targets = "net.minecraft.client.gl.ShaderLoader$Cache")
public class ShaderLoaderCacheMixin {
    
    /**
     * Strip _f_ suffix in loadProcessor to find the actual JSON file.
     * The cache key (with full suffix) is preserved at the ShaderLoader.loadPostEffect level.
     */
    @ModifyVariable(
        method = "loadProcessor",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Identifier theVirusBlock$stripFieldSuffixLoadProcessor(Identifier id) {
        if (!"the-virus-block".equals(id.getNamespace())) {
            return id;
        }
        
        String path = id.getPath();
        int sepIndex = path.indexOf("_f_");
        
        if (sepIndex != -1) {
            // Return stripped identifier for JSON file lookup
            return Identifier.of(id.getNamespace(), path.substring(0, sepIndex));
        }
        
        return id;
    }
}
