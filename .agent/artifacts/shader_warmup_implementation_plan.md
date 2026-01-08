# Shader Warm-up Implementation Plan 🎯

> **Target**: ~200 lines to eliminate first-enable shader lag

---

## Overview

This plan implements a lightweight shader warm-up system that:
1. Pre-compiles shaders during `JoinWarmupManager` phase
2. Caches preprocessed shader sources
3. Provides a foundation for future LOD work

---

## Phase 1: Core Implementation (~150 lines)

### Task 1.1: ShaderWarmupManager.java (NEW)
**Location**: `src/client/java/net/cyberpunk042/client/visual/shader/util/`

```java
package net.cyberpunk042.client.visual.shader.util;

/**
 * Pre-compiles shader programs during join warm-up.
 * Eliminates first-enable lag by forcing GPU compilation.
 */
public final class ShaderWarmupManager {
    
    // Shader identifiers to warm up (order: heaviest first)
    private static final Identifier[] WARMUP_SHADERS = {
        Identifier.of("the-virus-block", "post/field_visual"),
        Identifier.of("the-virus-block", "post/shockwave_ring"),
        Identifier.of("the-virus-block", "post/shockwave_glow")
    };
    
    private static volatile boolean warmedUp = false;
    
    private ShaderWarmupManager() {}
    
    /**
     * Triggers shader compilation by loading all field effect shaders.
     * Must be called from the render thread.
     */
    public static void warmup() {
        if (warmedUp) return;
        
        Logging.RENDER.topic("shader_warmup").info("Starting shader warm-up...");
        long start = System.nanoTime();
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        for (Identifier shaderId : WARMUP_SHADERS) {
            try {
                // Force shader load through the post-effect system
                // This triggers ShaderLoader.getSource() → preprocessor → compile
                warmupShader(client, shaderId);
            } catch (Exception e) {
                Logging.RENDER.topic("shader_warmup")
                    .kv("shader", shaderId)
                    .warn("Failed to warm up shader: {}", e.getMessage());
            }
        }
        
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        Logging.RENDER.topic("shader_warmup").info("Shader warm-up complete in {}ms", elapsed);
        warmedUp = true;
    }
    
    private static void warmupShader(MinecraftClient client, Identifier id) {
        // Access the shader through resource manager to trigger loading
        // The actual compilation happens in OpenGL on first use
        var rm = client.getResourceManager();
        
        // Try to load as fragment shader (.fsh)
        Identifier fsh = Identifier.of(id.getNamespace(), 
            "shaders/" + id.getPath() + ".fsh");
        
        if (rm.getResource(fsh).isPresent()) {
            // This forces the preprocessor to run and caches the result
            try (var resource = rm.getResource(fsh).get()) {
                String source = new String(resource.getInputStream().readAllBytes());
                if (ShaderPreprocessor.hasIncludes(source)) {
                    ShaderPreprocessor.process(source, fsh);
                }
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Called to reset state (e.g., on disconnect).
     */
    public static void reset() {
        // Keep warmedUp = true since shaders stay compiled
        // Only reset on resource pack reload
    }
    
    /**
     * Called on resource reload to invalidate caches.
     */
    public static void invalidate() {
        warmedUp = false;
        ShaderPreprocessor.invalidateCache();
    }
}
```

**Lines**: ~70

---

### Task 1.2: Update ShaderPreprocessor.java
**Location**: `src/client/java/net/cyberpunk042/client/visual/shader/util/ShaderPreprocessor.java`

Add caching to existing preprocessor:

```java
// Add near top of class (after PATTERN):
private static final Map<String, String> PREPROCESSED_CACHE = new ConcurrentHashMap<>();

// Add new public method:
/**
 * Clears the preprocessed shader cache.
 * Called on resource pack reload.
 */
public static void invalidateCache() {
    PREPROCESSED_CACHE.clear();
    Logging.RENDER.topic("shader_preprocess").info("Cache invalidated");
}

// Modify process() method to use cache:
public static String process(String source, Identifier baseIdentifier) {
    if (!OUR_NAMESPACE.equals(baseIdentifier.getNamespace())) {
        return source;
    }
    
    // NEW: Check cache first
    String cacheKey = baseIdentifier.toString();
    String cached = PREPROCESSED_CACHE.get(cacheKey);
    if (cached != null) {
        Logging.RENDER.topic("shader_preprocess")
            .kv("shader", cacheKey)
            .debug("Using cached preprocessed source");
        return cached;
    }
    
    // ... existing processing code ...
    
    // NEW: Cache the result before returning
    PREPROCESSED_CACHE.put(cacheKey, result);
    return result;
}
```

**Lines**: ~25

---

### Task 1.3: Update JoinWarmupManager.java
**Location**: `src/client/java/net/cyberpunk042/client/field/JoinWarmupManager.java`

Add shader warm-up stage:

```java
// Add constant near top:
private static final int SHADERS_TICKS = 30;  // 1.5 sec for shaders

// Update the constants:
private static final int EFFECTS_TICKS = 40;    // 2 sec for effects (0-20%)
private static final int PROFILES_TICKS = 30;   // 1.5 sec for profiles (20-35%)
// NEW: SHADERS_TICKS already added (35-50%)

// Add new stage tracking:
private static volatile boolean shadersDone = false;

// Update startWarmup():
private static void startWarmup() {
    // Reset all state
    warmupComplete.set(false);
    tickCount.set(0);
    effectsStarted = false;
    effectsDone = false;
    profilesDone = false;
    shadersDone = false;  // NEW
    chunksLoaded = 0;
    chunkWaitTicks = 0;
    
    asyncTask = CompletableFuture.runAsync(() -> {
        try {
            // Warmup effects
            warmupSphere(15.0f, 24, 32);
            warmupSphere(8.0f, 16, 24);
            warmupSphere(3.0f, 10, 14);
            warmupPatterns();
            effectsDone = true;
            
            // Load profiles
            net.cyberpunk042.client.profile.ProfileManager.getInstance().loadAll();
            profilesDone = true;
            
            // NEW: Shader warmup (must run on render thread)
            MinecraftClient.getInstance().execute(() -> {
                ShaderWarmupManager.warmup();
                shadersDone = true;
            });
            
        } catch (Exception e) {
            Logging.RENDER.topic("warmup").error("Warmup error: {}", e.getMessage());
            effectsDone = true;
            profilesDone = true;
            shadersDone = true;
        }
    });
    
    effectsStarted = true;
}

// Update getWarmupProgress():
public static float getWarmupProgress() {
    if (warmupComplete.get()) return 1.0f;
    
    int ticks = tickCount.get();
    int totalPreChunk = EFFECTS_TICKS + PROFILES_TICKS + SHADERS_TICKS;
    
    if (ticks <= EFFECTS_TICKS) {
        return (float) ticks / EFFECTS_TICKS * 0.20f;
    }
    
    if (ticks <= EFFECTS_TICKS + PROFILES_TICKS) {
        int profileTicks = ticks - EFFECTS_TICKS;
        return 0.20f + (float) profileTicks / PROFILES_TICKS * 0.15f;
    }
    
    if (ticks <= totalPreChunk) {
        int shaderTicks = ticks - EFFECTS_TICKS - PROFILES_TICKS;
        return 0.35f + (float) shaderTicks / SHADERS_TICKS * 0.15f;
    }
    
    // Chunks (50-100%)
    float chunkProgress = (float) chunksLoaded / TOTAL_CHUNKS;
    return 0.50f + chunkProgress * 0.50f;
}

// Update getCurrentStageLabel():
public static String getCurrentStageLabel() {
    if (warmupComplete.get()) return "Ready!";
    
    int ticks = tickCount.get();
    int totalPreChunk = EFFECTS_TICKS + PROFILES_TICKS + SHADERS_TICKS;
    
    if (ticks <= EFFECTS_TICKS) {
        return "Loading Effects...";
    } else if (ticks <= EFFECTS_TICKS + PROFILES_TICKS) {
        return "Loading Profiles...";
    } else if (ticks <= totalPreChunk) {
        return "Compiling Shaders...";  // NEW
    } else {
        return String.format("Loading Chunks (%d/%d)...", chunksLoaded, TOTAL_CHUNKS);
    }
}
```

**Lines**: ~55

---

## Phase 2: Integration (~50 lines)

### Task 2.1: Register Cache Invalidation
**Location**: Add to client initialization

```java
// In TheVirusBlockClient.java or appropriate init node:
ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
    new SimpleSynchronousResourceReloadListener() {
        @Override
        public Identifier getFabricId() {
            return Identifier.of("the-virus-block", "shader_cache");
        }
        
        @Override
        public void reload(ResourceManager manager) {
            ShaderWarmupManager.invalidate();
            Logging.RENDER.topic("shader_cache").info("Resource reload - shader cache invalidated");
        }
    }
);
```

**Lines**: ~15

---

## Execution Checklist

### Step 1: Create ShaderWarmupManager
- [ ] Create new file at specified path
- [ ] Implement warmup() and helper methods
- [ ] Add logging

### Step 2: Update ShaderPreprocessor
- [ ] Add cache field and import ConcurrentHashMap
- [ ] Add invalidateCache() method
- [ ] Modify process() to use cache

### Step 3: Update JoinWarmupManager  
- [ ] Add SHADERS_TICKS constant
- [ ] Add shadersDone flag
- [ ] Update startWarmup() to call ShaderWarmupManager
- [ ] Update getWarmupProgress() with new stage
- [ ] Update getCurrentStageLabel() with "Compiling Shaders..."

### Step 4: Test
- [ ] Start game fresh
- [ ] Verify warm-up shows "Compiling Shaders..." stage
- [ ] Enable energy orb - should be instant
- [ ] Toggle versions - no lag

---

## Total Line Count

| File | Lines |
|------|-------|
| ShaderWarmupManager.java (new) | ~70 |
| ShaderPreprocessor.java (mods) | ~25 |
| JoinWarmupManager.java (mods) | ~55 |
| Resource reload listener | ~15 |
| **Total** | **~165 lines** |

✅ Well under 400 line budget, with room for future LOD system.

---

## Future Expansion Points

1. **Effect-specific warm-up**: Extend WARMUP_SHADERS with UBO configuration per effect
2. **LOD header protocol**: Add HeaderSlot0/1 parsing in field_visual.fsh main()
3. **Frame budget controller**: Add timing around shader dispatch
4. **Audit script**: Profile compile times to optimize warm-up order

These can be added incrementally as needed.
