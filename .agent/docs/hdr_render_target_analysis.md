# HDR Render Target Integration - Technical Analysis

## Document Purpose
This document provides a thorough technical analysis of integrating RGBA16F (HDR) render targets
into Minecraft 1.21.6's post-effect rendering pipeline. It documents the architectural differences
between Minecraft versions, what Satin does, what we've attempted, and the remaining gaps.

---

## Part 1: Minecraft Rendering Architecture Evolution

### 1.1 Minecraft 1.20.x - 1.21.4 (Legacy Approach)

In older Minecraft versions, framebuffer texture creation used direct OpenGL calls:

```java
// Framebuffer.initFbo() in MC ≤1.21.4
GlStateManager._texImage2D(
    GL11.GL_TEXTURE_2D,    // target
    0,                      // level
    GL30.GL_RGBA8,          // internalFormat  <-- INTERCEPT HERE
    width, height,
    0,
    GL11.GL_RGBA,           // format
    GL11.GL_UNSIGNED_BYTE,  // type
    null
);
```

**Key Point**: The `internalFormat` parameter (arg index 2) was directly passed to `glTexImage2D`.
Mixins could easily intercept this with `@ModifyArg`.

### 1.2 Minecraft 1.21.6 (New blaze3d Abstraction)

Minecraft 1.21.6 introduced a new `com.mojang.blaze3d` rendering abstraction layer that wraps
OpenGL operations. The call chain is now:

```
Framebuffer.initFbo(width, height)
    └─> GpuDevice.createTexture(supplier, purpose, format, width, height, levels, samples)
        └─> GlBackend.createTexture(...)
            └─> GlConst.toGlInternalId(TextureFormat format)
                └─> Returns GL constant (e.g., 0x8058 for RGBA8)
            └─> GL45.glTextureStorage2D(..., internalFormat, ...)
```

**Critical Difference**: The format is now determined by a `TextureFormat` enum, not a raw integer.

### 1.3 The TextureFormat Enum (Verified via Bytecode)

From `com.mojang.blaze3d.textures.TextureFormat`:

```java
public enum TextureFormat {
    RGBA8,    // ordinal 0 → 0x8058
    RED8,     // ordinal 1 → 0x8229
    RED8I,    // ordinal 2 → 0x8231
    DEPTH32   // ordinal 3 → 0x81A7
}
```

**There is NO RGBA16F in this enum.** This is a fundamental limitation in MC 1.21.6.

### 1.4 The GlConst.toGlInternalId() Method (Verified via Bytecode)

```java
public static int toGlInternalId(TextureFormat format) {
    return switch(format.ordinal()) {
        case 0 -> 0x8058;  // RGBA8 → GL_RGBA8
        case 1 -> 0x8229;  // RED8 → GL_R8
        case 2 -> 0x8231;  // RED8I → GL_R8I
        case 3 -> 0x81A7;  // DEPTH32 → GL_DEPTH_COMPONENT32F
        default -> throw new MatchException(...);
    };
}
```

**This is the function we intercept** to substitute 0x881A (GL_RGBA16F) for 0x8058 (GL_RGBA8).

---

## Part 2: How Satin Implements Custom Formats (MC ≤1.21.4)

### 2.1 Satin's Architecture (from decompiled satin-3.0.0-alpha.1)

Satin uses three components:

#### Component 1: CustomFormatFramebuffers (Utility Class)
```java
public final class CustomFormatFramebuffers {
    private static final ThreadLocal<TextureFormat> CUSTOM_FORMAT = new ThreadLocal<>();
    
    public static void prepareCustomFormat(TextureFormat format) {
        CUSTOM_FORMAT.set(format);
    }
    
    public static TextureFormat getCustomFormat() {
        return CUSTOM_FORMAT.get();
    }
    
    public static void clearCustomFormat() {
        CUSTOM_FORMAT.remove();
    }
}
```

#### Component 2: CustomFormatFramebufferMixin (Mixin into Framebuffer)
```java
@Mixin(Framebuffer.class)
public abstract class CustomFormatFramebufferMixin {
    @Unique
    private int satin$format = GL_RGBA8;  // Default format stored in instance
    
    // Capture format at constructor time
    @Inject(method = "<init>", at = @At("TAIL"))
    private void satin$setFormat(boolean useDepth, CallbackInfo ci) {
        TextureFormat format = CustomFormatFramebuffers.getCustomFormat();
        if (format != null) {
            this.satin$format = format.value;  // Store in instance
            CustomFormatFramebuffers.clearCustomFormat();  // Consume
        }
    }
    
    // Use stored format during initFbo
    @ModifyArg(
        method = "initFbo",
        at = @At(value = "INVOKE", target = "GlStateManager._texImage2D(...)"),
        index = 2  // internalFormat parameter
    )
    private int satin$modifyInternalFormat(int original) {
        return this.satin$format;  // Return stored format
    }
}
```

### 2.2 Why Satin's Approach Doesn't Work in MC 1.21.6

| Satin's Target | MC 1.21.6 Reality |
|----------------|-------------------|
| `GlStateManager._texImage2D(...)` | **Does not exist** - replaced by `GpuDevice.createTexture()` |
| Direct GL integer format | Format is now `TextureFormat` enum |
| `initFbo()` calls `_texImage2D` | `initFbo()` calls `GpuDevice.createTexture()` |

The `@ModifyArg` on `GlStateManager._texImage2D` will simply never fire in MC 1.21.6.

---

## Part 3: Our Implementation Approach

### 3.1 Strategy: Intercept at GlConst.toGlInternalId()

Since we can't modify the `TextureFormat` enum and can't intercept `_texImage2D`, we intercept
the conversion function that translates `TextureFormat` to GL constants.

```
TextureFormat.RGBA8 → GlConst.toGlInternalId() → 0x8058 (GL_RGBA8)
                      ↓ (our mixin)
                      → 0x881A (GL_RGBA16F)
```

### 3.2 Implementation Components

#### Component 1: HdrTargetFactory (ThreadLocal Storage)
- `prepareFormat(TextureFormat)`: Sets pending format in ThreadLocal
- `hasPendingFormat()`: Checks if format is set
- `getPendingFormat()`: Peeks without consuming
- `clearPendingFormat()`: Removes from ThreadLocal

#### Component 2: FrameGraphBuilderHdrMixin
```java
@Mixin(FrameGraphBuilder.class)
public class FrameGraphBuilderHdrMixin {
    @Inject(method = "run(...)", at = @At("HEAD"))
    private void beforeRun(...) {
        if (RenderConfig.isHdrEnabled()) {
            HdrTargetFactory.prepareFormat(TextureFormat.RGBA16F);
        }
    }
    
    @Inject(method = "run(...)", at = @At("RETURN"))
    private void afterRun(...) {
        HdrTargetFactory.clearPendingFormat();
    }
}
```

#### Component 3: GlConstFormatMixin
```java
@Mixin(GlConst.class)
public class GlConstFormatMixin {
    @Inject(method = "toGlInternalId", at = @At("RETURN"), cancellable = true)
    private static void injectHdrFormat(TextureFormat format, CallbackInfoReturnable<Integer> cir) {
        if (format == TextureFormat.RGBA8 && HdrTargetFactory.hasPendingFormat()) {
            cir.setReturnValue(GL_RGBA16F);  // 0x881A
        }
    }
}
```

### 3.3 Verified via Logs

```
[Render:hdr_mixin] format=RGBA8 hasPending=true result=0x8058 toGlInternalId called
[Render:hdr_mixin] original=0x8058 new=0x881A Substituted RGBA16F format in toGlInternalId
```

**Confirmed**: The substitution IS happening at `toGlInternalId` level.

---

## Part 4: PostEffectProcessor Framebuffer Lifecycle

### 4.1 Framebuffer Creation Flow in PostEffectProcessor (from bytecode)

```java
public void render(FrameGraphBuilder builder, int width, int height, FramebufferSet set) {
    for (Entry<Identifier, Targets> entry : internalTargets.entrySet()) {
        Identifier id = entry.getKey();
        Targets targets = entry.getValue();
        
        SimpleFramebufferFactory factory = new SimpleFramebufferFactory(
            targets.width().orElse(width),
            targets.height().orElse(height),
            targets.persistent(),
            targets.clearColor()
        );
        
        if (targets.persistent()) {
            // PERSISTENT: Create framebuffer NOW
            Framebuffer fb = createFramebuffer(id, factory);
            handles.put(id, builder.createObjectNode(id.toString(), fb));
        } else {
            // NON-PERSISTENT: Defer to FrameGraphBuilder.run()
            handles.put(id, builder.createResourceHandle(id.toString(), factory));
        }
    }
}
```

### 4.2 The persistent() Behavior

From `PostEffectPipeline$Targets` bytecode:
```java
// Default value for persistent when not specified in JSON:
optionalFieldOf("persistent", false)
```

**Our shader JSONs do NOT specify persistent**, so they default to `false` (non-persistent).

### 4.3 When Framebuffers Are Actually Created

| Target Type | When Created | Our Format Injection |
|-------------|--------------|---------------------|
| `persistent: true` | During `PostEffectProcessor.render()` | ❌ Format NOT set at this time |
| `persistent: false` | During `FrameGraphBuilder.run()` | ✅ Format IS set (via our mixin) |

Our shaders use `persistent: false`, so the timing SHOULD be correct.

### 4.4 PostEffectProcessor Internal Caching

```java
private final Map<Identifier, Framebuffer> framebuffers;

private Framebuffer createFramebuffer(Identifier id, SimpleFramebufferFactory factory) {
    Framebuffer cached = framebuffers.get(id);
    if (cached != null 
        && cached.textureWidth == factory.width() 
        && cached.textureHeight == factory.height()) {
        return cached;  // REUSE EXISTING
    }
    
    if (cached != null) {
        cached.delete();
    }
    
    Framebuffer newFb = factory.create();
    framebuffers.put(id, newFb);
    return newFb;
}
```

**Key Insight**: Even if dimensions match, the cached framebuffer is reused.
**BUT**: This cache is per-processor instance. A NEW processor has an empty cache.

---

## Part 5: Cache Hierarchy and Clearing

### 5.1 Cache Layers

```
Layer 1: FieldVisualPostEffect.PROCESSOR_CACHE
         Map<UUID, PostEffectProcessor> by field ID
         
Layer 2: ShaderLoader$Cache.postEffectProcessors
         Map<Identifier, Optional<PostEffectProcessor>> by shader ID
         
Layer 3: PostEffectProcessor.framebuffers (internal)
         Map<Identifier, Framebuffer> by target ID
```

### 5.2 What We Clear on HDR Toggle

```java
// FieldVisualSubPanel HDR toggle handler
renderConfig.setHdrEnabled(v);
PostFxPipeline.getInstance().invalidateTargets();
FieldVisualPostEffect.clearProcessorCache();        // Clears Layer 1
ShaderLoaderCacheHelper.clearOurProcessors();       // Attempts Layer 2
```

### 5.3 Layer 2 Clearing Issue

`ShaderLoaderCacheHelper` requires the map to be registered first:

```java
public static void clearOurProcessors() {
    if (cachedProcessorsMap == null) {
        // SILENT RETURN - nothing cleared!
        return;
    }
    cachedProcessorsMap.entrySet().removeIf(...);
}
```

The map is registered in `ShaderLoaderCacheMixin` during `loadProcessor()`:
```java
private Identifier theVirusBlock$stripFieldSuffixLoadProcessor(Identifier id) {
    ShaderLoaderCacheHelper.setProcessorsMap(postEffectProcessors);
    // ...
}
```

**Problem**: If `loadProcessor` was never called through our mod's path, the map is null.

---

## Part 6: Current State and Known Gaps

### 6.1 What IS Working (Verified)

| Component | Status | Evidence |
|-----------|--------|----------|
| GlConstFormatMixin applied | ✅ | Logs show `toGlInternalId called` |
| FrameGraphBuilderHdrMixin applied | ✅ | Logs show `run() started` |
| ThreadLocal format passed | ✅ | Logs show `hasPending=true` |
| Format substitution | ✅ | Logs show `Substituted RGBA16F` |

### 6.2 What Is NOT Working (Observed)

| Symptom | Observation |
|---------|-------------|
| Visual change on toggle | ❌ None observed |
| Framebuffer recreation | ❌ No shader refresh/flicker |
| New substitution logs after toggle | ❓ Need to verify timestamps |

### 6.3 Remaining Questions

1. **Are the substitution logs from startup or from toggle?**
   - Timestamps suggest startup (22:09:50) not recent toggle
   
2. **Is ShaderLoader cache being cleared?**
   - Need to verify `cachedProcessorsMap` is non-null when clearing
   
3. **Is a NEW processor being loaded after toggle?**
   - Need to log in `FieldVisualPostEffect.loadProcessor()`
   
4. **Are NEW framebuffers being created after toggle?**
   - Need to log in `SimpleFramebufferFactory.create()`

---

## Part 7: Comparison with Satin's Approach

### 7.1 Fundamental Difference

| Aspect | Satin | Our Approach |
|--------|-------|--------------|
| Format storage | Instance field in Framebuffer (`satin$format`) | ThreadLocal in HdrTargetFactory |
| When captured | Constructor (`<init>` TAIL) | During `FrameGraphBuilder.run()` |
| When used | `initFbo()` via `@ModifyArg` | `toGlInternalId()` via `@Inject` |
| Target API | `GlStateManager._texImage2D` | `GlConst.toGlInternalId` |

### 7.2 Satin's Key Advantage

Satin stores the format **IN THE FRAMEBUFFER INSTANCE**. This means:
- Format survives across calls
- No timing dependency on ThreadLocal
- Format is bound to the specific framebuffer forever

Our approach requires:
- ThreadLocal to be set at EXACT moment of texture creation
- All framebuffer creations during `run()` window get same format
- If ANY texture is created outside this window, it gets wrong format

### 7.3 What Would Satin Need to Change for MC 1.21.6

Satin would need to:
1. Keep the instance field approach (`satin$format` in Framebuffer)
2. Change `@ModifyArg` target from `_texImage2D` to intercept at `GlConst.toGlInternalId`
3. Or use `@Redirect` on `GpuDevice.createTexture()` to swap the TextureFormat

---

## Part 8: Next Steps for Resolution

### 8.1 Immediate Diagnostics Needed

1. Change `ShaderLoaderCacheHelper.clearOurProcessors()` logging to INFO level
2. Add logging to `FieldVisualPostEffect.loadProcessor()` to show cache hits vs misses
3. Verify timestamps of substitution logs match AFTER toggle, not just startup

### 8.2 Alternative Approaches to Consider

1. **Store format in Framebuffer instance** (Satin pattern)
   - Add `@Unique` field to Framebuffer
   - Capture format at construction
   - Check field in GlConstFormatMixin instead of ThreadLocal
   
2. **Intercept GpuDevice.createTexture()** instead of GlConst
   - More direct control over texture creation
   - Can swap TextureFormat before it reaches GlConst

3. **Force processor recreation** by changing shader ID suffix
   - Already implemented (_hdr/_ldr suffix)
   - May need to clear additional caches

---

## Appendix A: Relevant Bytecode Excerpts

### GlBackend.createTexture() calls to GlConst
```
274: invokestatic #278  // GlConst.toGlInternalId:(TextureFormat;)I
289: invokestatic #281  // GlConst.toGlExternalId:(TextureFormat;)I
293: invokestatic #284  // GlConst.toGlType:(TextureFormat;)I
```

### PostEffectProcessor.render() persistent check
```
211: invokevirtual #504  // Targets.persistent:()Z
214: ifeq 251            // if NOT persistent, jump to non-persistent path
222: invokevirtual #508  // createFramebuffer (persistent path)
263: invokevirtual #519  // createResourceHandle (non-persistent path)
```

---

## Appendix B: File Locations

| File | Purpose |
|------|---------|
| `mixin/client/GlConstFormatMixin.java` | Format substitution |
| `mixin/client/FrameGraphBuilderHdrMixin.java` | Format timing |
| `mixin/client/ShaderLoaderCacheMixin.java` | Cache access |
| `visual/render/target/HdrTargetFactory.java` | ThreadLocal storage |
| `visual/shader/util/ShaderLoaderCacheHelper.java` | Cache clearing |

---

*Document created: 2026-01-10*
*Minecraft version: 1.21.6*
*Satin reference version: 3.0.0-alpha.1*
