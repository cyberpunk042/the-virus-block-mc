package net.cyberpunk042.client.visual.shader.util;

import net.cyberpunk042.log.Logging;

import java.util.Map;

/**
 * Utility class for managing ShaderLoader cache.
 */
public final class ShaderLoaderCacheHelper {
    
    private static final String TAG = "HDR_CACHE";
    private static Map<?, ?> cachedProcessorsMap;
    
    private ShaderLoaderCacheHelper() {}
    
    /**
     * Register the processors map from the mixin.
     */
    public static void setProcessorsMap(Map<?, ?> map) {
        boolean wasNull = (cachedProcessorsMap == null);
        cachedProcessorsMap = map;
        
        if (wasNull && map != null) {
            Logging.RENDER.topic(TAG)
                .kv("mapSize", map.size())
                .info("[CACHE_REGISTER] ShaderLoader cache map registered");
        }
    }
    
    /**
     * Clear our mod's processors from the ShaderLoader cache.
     */
    public static void clearOurProcessors() {
        Logging.RENDER.topic(TAG)
            .kv("mapIsNull", cachedProcessorsMap == null)
            .info("[CACHE_CLEAR_START] Attempting to clear ShaderLoader cache");
        
        if (cachedProcessorsMap == null) {
            Logging.RENDER.topic(TAG)
                .warn("[CACHE_CLEAR_FAIL] cachedProcessorsMap is NULL - mixin never registered it");
            return;
        }
        
        int before = cachedProcessorsMap.size();
        
        // Log what we're about to remove
        for (Object key : cachedProcessorsMap.keySet()) {
            String keyStr = key != null ? key.toString() : "null";
            boolean willRemove = keyStr.contains("the-virus-block");
            Logging.RENDER.topic(TAG)
                .kv("key", keyStr)
                .kv("willRemove", willRemove)
                .info("[CACHE_ENTRY] Found cache entry");
        }
        
        cachedProcessorsMap.entrySet().removeIf(entry -> {
            Object key = entry.getKey();
            if (key != null) {
                return key.toString().contains("the-virus-block");
            }
            return false;
        });
        
        int after = cachedProcessorsMap.size();
        int removed = before - after;
        
        Logging.RENDER.topic(TAG)
            .kv("before", before)
            .kv("after", after)
            .kv("removed", removed)
            .info("[CACHE_CLEAR_DONE] Cleared {} processors from ShaderLoader cache", removed);
    }
}
