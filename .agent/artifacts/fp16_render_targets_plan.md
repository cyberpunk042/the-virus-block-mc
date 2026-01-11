# 🎯 Implementation Plan: FP16 (RGBA16F) Render Targets for Post-FX

> **Goal:** Add half-float precision intermediate buffers to eliminate banding artifacts in accumulation-heavy post-effects (Energy Orb glow, shafts, blur chains).
>
> **Approach:** Satin-style mixin injection + JSON format extension

---

## 📊 Executive Summary

### Architecture Decision: Satin-Style Mixin Pattern

After analyzing Satin 3.0.0-alpha.1, we adopt their proven pattern:

| Component | Satin Pattern | Our Implementation |
|-----------|---------------|-------------------|
| Format Injection | ThreadLocal + Mixin | `HdrTargetFactory` + `FramebufferFormatMixin` |
| Target Definition | Programmatic | **JSON extension** (`"format": "RGBA16F"`) |
| Resize Handling | Event callback | `ResolutionChangeCallback` event |
| Depth Sharing | `copyDepthFrom()` | Same pattern |

### Why Satin's Approach?
1. **Reuses Minecraft's framebuffer lifecycle** - resize, delete, bind all handled
2. **Compatible with PostEffectProcessor** - MC's system understands Framebuffer class
3. **Less GL boilerplate** - no manual FBO/texture management
4. **Proven in production** - Satin is widely used

---

## 🎮 Runtime Control Strategy (Hybrid Approach)

### The Problem with Pure JSON
JSON-only format definition is **static** - you can't toggle HDR on/off without reloading shaders. For a good user experience, you need:
- In-game toggle (keybind or GUI)
- Immediate visual feedback
- No shader reload required

### Solution: Hybrid JSON + Programmatic Control

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HYBRID ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   JSON Manifest (Default)          Runtime Config (Override)        │
│   ┌─────────────────────┐          ┌─────────────────────────┐     │
│   │ "glow": {           │          │ RenderConfig.get()      │     │
│   │   "format":"RGBA16F"│  ───────►│   .isHdrEnabled()       │     │
│   │ }                   │  Default │   .setHdrEnabled(bool)  │     │
│   └─────────────────────┘          └──────────┬──────────────┘     │
│                                               │                      │
│                                               ▼                      │
│                              ┌────────────────────────────────┐     │
│                              │     PostFxPipeline.java        │     │
│                              │  ┌──────────────────────────┐  │     │
│                              │  │ if (config.isHdrEnabled) │  │     │
│                              │  │   use RGBA16F targets    │  │     │
│                              │  │ else                     │  │     │
│                              │  │   use RGBA8 (LDR path)   │  │     │
│                              │  └──────────────────────────┘  │     │
│                              └────────────────────────────────┘     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **JSON defines defaults** | Shader artists can tune without Java code |
| **RenderConfig overrides** | Users control quality at runtime |
| **PostFxPipeline orchestrates** | Programmatic creation = instant format switch |
| **Target recreation on toggle** | Only when HDR state changes, not every frame |

### In-Game Toggle Implementation

```java
// In keybind handler or GUI callback:
public void toggleHdr() {
    RenderConfig config = RenderConfig.get();
    config.setHdrEnabled(!config.isHdrEnabled());
    
    // Force pipeline recreation with new format
    PostFxPipeline.getInstance().invalidateTargets();
}
```

### Format Resolution Order

1. **RenderConfig.isHdrEnabled()** → Master toggle (user control)
2. If HDR disabled → Always use RGBA8 (fast LDR path)
3. If HDR enabled:
   - JSON `"format"` field → Per-target override
   - No JSON format → Default to RGBA16F for HDR targets

---

## 🗂️ File Structure Overview

```
src/client/java/net/cyberpunk042/
├── client/
│   ├── gui/config/
│   │   └── RenderConfig.java           # ⭐ Runtime HDR toggle (user control)
│   └── visual/render/
│       ├── target/
│       │   ├── HdrTargetFactory.java   # ThreadLocal format injection
│       │   └── TextureFormat.java      # RGBA8/RGBA16F/RGBA32F enum
│       └── PostFxPipeline.java         # ⭐ Runtime HDR orchestrator
├── mixin/client/
│   ├── FramebufferFormatMixin.java     # Inject format at construction
│   └── PostEffectProcessorFormatMixin.java  # (Optional) Parse JSON defaults

src/main/resources/assets/the-virus-block/
├── post_effect/
│   ├── field_visual_v7.json            # Existing (can add "format" defaults)
│   └── field_visual_v7_hdr.json        # NEW: Multi-pass HDR example
├── shaders/post/
│   └── hdr/                            # NEW: HDR pipeline shaders
│       ├── glow_extract.fsh
│       ├── gaussian_blur.fsh
│       └── composite.fsh

# Key files for RUNTIME CONTROL (⭐):
# - RenderConfig.java: User can toggle HDR on/off in-game
# - PostFxPipeline.java: Recreates targets with correct format on toggle
```

---

## 📋 Phase-by-Phase Implementation

### Phase 1: TextureFormat Enum + HdrTargetFactory (Foundation)

**Files:**
- `src/client/java/net/cyberpunk042/client/visual/render/target/TextureFormat.java`
- `src/client/java/net/cyberpunk042/client/visual/render/target/HdrTargetFactory.java`

#### TextureFormat.java
```java
package net.cyberpunk042.client.visual.render.target;

/**
 * OpenGL texture internal formats for framebuffer color attachments.
 * 
 * <p>RGBA16F (half-float) is the sweet spot for HDR post-processing:
 * - 65504 max value (vs 1.0 for RGBA8)
 * - 10-bit mantissa precision 
 * - 2x memory of RGBA8, 0.5x memory of RGBA32F
 */
public enum TextureFormat {
    /** Standard 8-bit per channel (default) */
    RGBA8(0x8058, "RGBA8"),
    
    /** 16-bit unsigned integer per channel */
    RGBA16(0x805B, "RGBA16"),
    
    /** 16-bit half-float per channel (recommended for HDR) */
    RGBA16F(0x881A, "RGBA16F"),
    
    /** 32-bit float per channel (overkill for most uses) */
    RGBA32F(0x8814, "RGBA32F");
    
    private final int glConstant;
    private final String jsonName;
    
    TextureFormat(int glConstant, String jsonName) {
        this.glConstant = glConstant;
        this.jsonName = jsonName;
    }
    
    public int glConstant() {
        return glConstant;
    }
    
    public String jsonName() {
        return jsonName;
    }
    
    /**
     * Parse from JSON string (case-insensitive).
     * Returns null if not recognized.
     */
    public static TextureFormat fromJson(String name) {
        if (name == null) return null;
        for (TextureFormat f : values()) {
            if (f.jsonName.equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }
    
    /**
     * Check if the current GPU supports this format.
     * RGBA16F has been standard since OpenGL 3.0 / GLES 3.0.
     */
    public boolean isSupported() {
        // For RGBA16F: effectively universal on any machine running modern MC
        // Could add explicit capability check if needed
        return true;
    }
}
```

#### HdrTargetFactory.java
```java
package net.cyberpunk042.client.visual.render.target;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Framebuffer;

/**
 * Factory for creating framebuffers with custom texture formats.
 * 
 * <p>Uses a ThreadLocal pattern (inspired by Satin) to inject format
 * during Framebuffer construction via mixin.
 * 
 * <h3>Usage:</h3>
 * <pre>
 * Framebuffer hdrTarget = HdrTargetFactory.create(width, height, true, TextureFormat.RGBA16F);
 * </pre>
 * 
 * <h3>How it works:</h3>
 * <ol>
 *   <li>Call {@link #create} with desired format</li>
 *   <li>Format is stored in ThreadLocal</li>
 *   <li>SimpleFramebuffer constructor is called</li>
 *   <li>FramebufferFormatMixin reads format from ThreadLocal</li>
 *   <li>Mixin modifies glTexImage2D internal format parameter</li>
 *   <li>ThreadLocal is cleared</li>
 * </ol>
 */
public final class HdrTargetFactory {
    
    private static final ThreadLocal<TextureFormat> PENDING_FORMAT = new ThreadLocal<>();
    
    private HdrTargetFactory() {}
    
    /**
     * Create a framebuffer with custom texture format.
     * 
     * @param width Framebuffer width in pixels
     * @param height Framebuffer height in pixels
     * @param useDepth Whether to create a depth attachment
     * @param format Texture format (RGBA8, RGBA16F, etc.)
     * @return New framebuffer instance
     */
    public static Framebuffer create(int width, int height, boolean useDepth, TextureFormat format) {
        if (format == null) {
            format = TextureFormat.RGBA8;
        }
        
        PENDING_FORMAT.set(format);
        try {
            Framebuffer fb = new SimpleFramebuffer(width, height, useDepth);
            Logging.RENDER.topic("hdr_target")
                .kv("format", format.jsonName())
                .kv("size", width + "x" + height)
                .kv("depth", useDepth)
                .debug("Created HDR framebuffer");
            return fb;
        } finally {
            PENDING_FORMAT.remove();
        }
    }
    
    /**
     * Convenience method for RGBA16F (the common HDR case).
     */
    public static Framebuffer createHdr(int width, int height, boolean useDepth) {
        return create(width, height, useDepth, TextureFormat.RGBA16F);
    }
    
    /**
     * Called by FramebufferFormatMixin to get the pending format.
     * Returns null if no custom format was requested (use default RGBA8).
     */
    public static TextureFormat consumePendingFormat() {
        TextureFormat format = PENDING_FORMAT.get();
        PENDING_FORMAT.remove();
        return format;
    }
    
    /**
     * Prepare format for the next framebuffer construction.
     * Used by PostEffectProcessor mixin when parsing JSON targets.
     */
    public static void prepareFormat(TextureFormat format) {
        PENDING_FORMAT.set(format);
    }
    
    /**
     * Clear any pending format. Call in finally blocks.
     */
    public static void clearPendingFormat() {
        PENDING_FORMAT.remove();
    }
}
```

---

### Phase 2: Framebuffer Format Mixin (The Core Injection)

**File:** `src/client/java/net/cyberpunk042/mixin/client/FramebufferFormatMixin.java`

```java
package net.cyberpunk042.mixin.client;

import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to support custom texture formats (RGBA16F, etc.) for framebuffers.
 * 
 * <p>Works in conjunction with {@link HdrTargetFactory} to inject format
 * at construction time via ThreadLocal pattern.
 * 
 * <p>Pattern inspired by Satin's CustomFormatFramebufferMixin.
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferFormatMixin {
    
    /**
     * The OpenGL internal format for this framebuffer's color texture.
     * Defaults to RGBA8 (0x8058 = 32856).
     */
    @Unique
    private int theVirusBlock$internalFormat = GL11.GL_RGBA8;
    
    /**
     * Capture the custom format at construction time.
     * Must happen before initFbo() is called.
     */
    @Inject(method = "<init>(Z)V", at = @At("HEAD"))
    private void theVirusBlock$captureFormat(boolean useDepth, CallbackInfo ci) {
        TextureFormat format = HdrTargetFactory.consumePendingFormat();
        if (format != null) {
            this.theVirusBlock$internalFormat = format.glConstant();
        }
    }
    
    /**
     * Alternative injection point for SimpleFramebuffer constructor signature.
     */
    @Inject(method = "<init>(IIZ)V", at = @At("HEAD"), require = 0)
    private void theVirusBlock$captureFormatAlt(int width, int height, boolean useDepth, CallbackInfo ci) {
        TextureFormat format = HdrTargetFactory.consumePendingFormat();
        if (format != null) {
            this.theVirusBlock$internalFormat = format.glConstant();
        }
    }
    
    /**
     * Modify the internal format parameter of glTexImage2D.
     * 
     * <p>The initFbo method calls GlStateManager._texImage2D with:
     * <pre>
     * _texImage2D(target, level, internalFormat, width, height, border, format, type, data)
     * </pre>
     * We intercept the 3rd argument (internalFormat, index 2).
     */
    @ModifyArg(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
        ),
        index = 2  // internalFormat parameter
    )
    private int theVirusBlock$modifyInternalFormat(int originalFormat) {
        // Only substitute if a custom format was set
        if (this.theVirusBlock$internalFormat != GL11.GL_RGBA8) {
            return this.theVirusBlock$internalFormat;
        }
        return originalFormat;
    }
}
```

**Don't forget to register in mixins JSON:**
```json
// mixins.the-virus-block.client.json
{
    "mixins": [
        // ... existing mixins ...
        "FramebufferFormatMixin"
    ]
}
```

---

### Phase 3: PostEffectProcessor JSON Format Extension

**File:** `src/client/java/net/cyberpunk042/mixin/client/PostEffectProcessorJsonMixin.java`

This mixin parses the `"format"` key from JSON target definitions.

```java
package net.cyberpunk042.mixin.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin to parse "format" from post_effect JSON target definitions.
 * 
 * <h3>JSON Format:</h3>
 * <pre>
 * {
 *     "targets": {
 *         "accumulation": { "format": "RGBA16F" },
 *         "swap": {}
 *     }
 * }
 * </pre>
 * 
 * <p>When PostEffectProcessor creates a target, we check for "format" key
 * and inject via HdrTargetFactory's ThreadLocal pattern.
 */
@Mixin(PostEffectProcessor.class)
public abstract class PostEffectProcessorJsonMixin {
    
    // Note: The exact injection point depends on MC version.
    // In 1.21.6, target creation happens in parseTarget or similar.
    // This is a conceptual implementation - adjust based on actual MC code.
    
    /**
     * Intercept target parsing to extract format field.
     * 
     * The structure varies by MC version. In recent versions, targets are parsed
     * from JSON in the PostEffectProcessor constructor or a static parse method.
     */
    @Inject(
        method = "parseTarget",  // Adjust method name based on MC 1.21.6 decompilation
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/SimpleFramebuffer;<init>(IIZ)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0  // Don't crash if signature changes
    )
    private static void theVirusBlock$injectFormat(
        JsonObject targetJson,
        int width, int height,
        CallbackInfo ci
    ) {
        // Check for "format" key
        if (targetJson != null && targetJson.has("format")) {
            JsonElement formatElement = targetJson.get("format");
            if (formatElement.isJsonPrimitive()) {
                String formatName = formatElement.getAsString();
                TextureFormat format = TextureFormat.fromJson(formatName);
                
                if (format != null) {
                    HdrTargetFactory.prepareFormat(format);
                    Logging.RENDER.topic("hdr_json")
                        .kv("format", formatName)
                        .debug("Preparing HDR target from JSON");
                } else {
                    Logging.RENDER.topic("hdr_json")
                        .kv("format", formatName)
                        .warn("Unknown texture format in JSON");
                }
            }
        }
    }
}
```

> ⚠️ **Note:** The exact injection point needs verification against MC 1.21.6's PostEffectProcessor. The pattern shown is conceptual - we'll need to trace the actual code path during implementation.

---

### Phase 4: Resolution Change Handling

**File:** Add to existing initialization or create callback

```java
// In your mod's client initialization or a dedicated handler
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class HdrTargetResizeHandler {
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    
    // Store references to HDR targets that need resize
    private static final List<WeakReference<Framebuffer>> hdrTargets = new ArrayList<>();
    
    public static void init() {
        // Check on each tick if resolution changed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            
            int w = client.getWindow().getFramebufferWidth();
            int h = client.getWindow().getFramebufferHeight();
            
            if (w != lastWidth || h != lastHeight) {
                onResolutionChanged(w, h);
                lastWidth = w;
                lastHeight = h;
            }
        });
    }
    
    private static void onResolutionChanged(int width, int height) {
        // PostEffectProcessor handles its own targets automatically
        // This is for any standalone HDR targets we create
        Logging.RENDER.topic("hdr_resize")
            .kv("size", width + "x" + height)
            .debug("Resolution changed");
    }
}
```

---

### Phase 5: Multi-Pass JSON Configuration

**File:** `src/main/resources/assets/the-virus-block/post_effect/field_visual_v7_hdr.json`

This is a new HDR-enabled variant of the field_visual_v7 shader:

```json
{
    "targets": {
        "glow_extract": { "format": "RGBA16F" },
        "blur_h": { "format": "RGBA16F" },
        "blur_v": { "format": "RGBA16F" }
    },
    "passes": [
        {
            "_comment": "Pass 1: Extract glow from main scene",
            "vertex_shader": "minecraft:post/sobel",
            "fragment_shader": "the-virus-block:post/hdr/glow_extract",
            "inputs": [
                { "sampler_name": "In", "target": "minecraft:main" },
                { "sampler_name": "Depth", "target": "minecraft:main", "use_depth_buffer": true }
            ],
            "output": "glow_extract",
            "uniforms": {
                "FieldVisualConfig": [
                    /* ... existing uniforms ... */
                ]
            }
        },
        {
            "_comment": "Pass 2: Horizontal Gaussian blur",
            "vertex_shader": "minecraft:post/sobel",
            "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
            "inputs": [
                { "sampler_name": "In", "target": "glow_extract" }
            ],
            "output": "blur_h",
            "uniforms": {
                "BlurDirection": [
                    { "name": "DirectionX", "type": "float", "value": 1.0 },
                    { "name": "DirectionY", "type": "float", "value": 0.0 }
                ]
            }
        },
        {
            "_comment": "Pass 3: Vertical Gaussian blur",
            "vertex_shader": "minecraft:post/sobel",
            "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
            "inputs": [
                { "sampler_name": "In", "target": "blur_h" }
            ],
            "output": "blur_v",
            "uniforms": {
                "BlurDirection": [
                    { "name": "DirectionX", "type": "float", "value": 0.0 },
                    { "name": "DirectionY", "type": "float", "value": 1.0 }
                ]
            }
        },
        {
            "_comment": "Pass 4: Composite blurred glow onto scene",
            "vertex_shader": "minecraft:post/sobel",
            "fragment_shader": "the-virus-block:post/hdr/composite",
            "inputs": [
                { "sampler_name": "Scene", "target": "minecraft:main" },
                { "sampler_name": "Glow", "target": "blur_v" },
                { "sampler_name": "Depth", "target": "minecraft:main", "use_depth_buffer": true }
            ],
            "output": "minecraft:main",
            "uniforms": {
                "CompositeParams": [
                    { "name": "GlowIntensity", "type": "float", "value": 1.0 },
                    { "name": "TonemapExposure", "type": "float", "value": 1.5 }
                ]
            }
        }
    ]
}
```

---

### Phase 5B: PostFxPipeline (Runtime HDR Orchestrator)

**File:** `src/client/java/net/cyberpunk042/client/visual/render/PostFxPipeline.java`

This is the **core runtime orchestrator** that enables in-game HDR toggle.

```java
package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.client.gui.config.RenderConfig;
import net.cyberpunk042.client.visual.render.target.HdrTargetFactory;
import net.cyberpunk042.client.visual.render.target.TextureFormat;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the multi-pass HDR post-effect pipeline with RUNTIME CONTROL.
 * 
 * <h3>Key Feature: In-Game HDR Toggle</h3>
 * <p>Unlike JSON-only approaches, this class supports toggling HDR on/off
 * at runtime without shader reload. Call {@link #invalidateTargets()} 
 * after changing {@link RenderConfig#setHdrEnabled(boolean)}.
 * 
 * <h3>Pipeline Stages:</h3>
 * <ol>
 *   <li>Main scene → Glow extraction → accumulation buffer</li>
 *   <li>Accumulation → Gaussian blur (ping-pong) → blur buffer</li>
 *   <li>Blur result → Final composite → main framebuffer</li>
 * </ol>
 * 
 * <h3>Format Resolution:</h3>
 * <ul>
 *   <li>HDR disabled → Always RGBA8 (fast LDR path)</li>
 *   <li>HDR enabled → RGBA16F for accumulation/blur targets</li>
 * </ul>
 */
public class PostFxPipeline {
    
    private static final PostFxPipeline INSTANCE = new PostFxPipeline();
    
    // HDR targets - lazily created, recreated on toggle
    private Framebuffer glowExtractTarget;
    private Framebuffer blurPingTarget;
    private Framebuffer blurPongTarget;
    
    // State tracking
    private int currentWidth = -1;
    private int currentHeight = -1;
    private boolean currentHdrState = false;  // Track HDR state for invalidation
    private boolean targetsValid = false;
    
    private PostFxPipeline() {}
    
    public static PostFxPipeline getInstance() {
        return INSTANCE;
    }
    
    // =========================================================================
    // PUBLIC API
    // =========================================================================
    
    /**
     * Invalidate targets - call after HDR toggle or quality change.
     * Next render will recreate targets with new format.
     */
    public void invalidateTargets() {
        targetsValid = false;
        Logging.RENDER.topic("hdr_pipeline")
            .debug("Targets invalidated - will recreate on next render");
    }
    
    /**
     * Ensure targets exist and match current size/format.
     * Called at start of each render frame.
     */
    public void ensureTargetsReady(int width, int height) {
        boolean hdrEnabled = RenderConfig.get().isHdrEnabled();
        
        // Check if recreation needed
        boolean sizeChanged = (width != currentWidth || height != currentHeight);
        boolean hdrChanged = (hdrEnabled != currentHdrState);
        
        if (!targetsValid || sizeChanged || hdrChanged) {
            recreateTargets(width, height, hdrEnabled);
            currentWidth = width;
            currentHeight = height;
            currentHdrState = hdrEnabled;
            targetsValid = true;
        }
    }
    
    /**
     * Get the glow extraction target (input for blur chain).
     */
    public Framebuffer getGlowExtractTarget() {
        return glowExtractTarget;
    }
    
    /**
     * Get blur ping target (for ping-pong blur).
     */
    public Framebuffer getBlurPingTarget() {
        return blurPingTarget;
    }
    
    /**
     * Get blur pong target (for ping-pong blur).
     */
    public Framebuffer getBlurPongTarget() {
        return blurPongTarget;
    }
    
    /**
     * Check if HDR is currently active (for shader branching).
     */
    public boolean isHdrActive() {
        return currentHdrState;
    }
    
    /**
     * Cleanup all targets. Call on mod shutdown.
     */
    public void cleanup() {
        deleteTarget(glowExtractTarget);
        deleteTarget(blurPingTarget);
        deleteTarget(blurPongTarget);
        glowExtractTarget = null;
        blurPingTarget = null;
        blurPongTarget = null;
        targetsValid = false;
        
        Logging.RENDER.topic("hdr_pipeline")
            .debug("Pipeline cleaned up");
    }
    
    // =========================================================================
    // INTERNALS
    // =========================================================================
    
    private void recreateTargets(int width, int height, boolean useHdr) {
        // Delete old targets first
        deleteTarget(glowExtractTarget);
        deleteTarget(blurPingTarget);
        deleteTarget(blurPongTarget);
        
        // Choose format based on runtime config
        TextureFormat format = useHdr ? TextureFormat.RGBA16F : TextureFormat.RGBA8;
        
        // Apply blur quality scaling
        float quality = RenderConfig.get().getBlurQuality();
        int blurWidth = Math.max(1, (int)(width * quality));
        int blurHeight = Math.max(1, (int)(height * quality));
        
        // Create targets with appropriate format
        glowExtractTarget = HdrTargetFactory.create(width, height, false, format);
        blurPingTarget = HdrTargetFactory.create(blurWidth, blurHeight, false, format);
        blurPongTarget = HdrTargetFactory.create(blurWidth, blurHeight, false, format);
        
        Logging.RENDER.topic("hdr_pipeline")
            .kv("format", format.jsonName())
            .kv("size", width + "x" + height)
            .kv("blurSize", blurWidth + "x" + blurHeight)
            .kv("quality", quality)
            .info("Recreated HDR targets");
    }
    
    private void deleteTarget(Framebuffer target) {
        if (target != null) {
            target.delete();
        }
    }
    
    /**
     * Copy depth from main framebuffer to a target.
     * Used for depth-aware effects (occlusion, etc).
     */
    public void copyDepthToTarget(Framebuffer target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null && target != null) {
            target.copyDepthFrom(client.getFramebuffer());
        }
    }
}
```

### Usage Example: In-Game Toggle

```java
// Register keybind (e.g., in ClientModInitializer)
KeyBinding hdrToggleKey = new KeyBinding(
    "key.the-virus-block.toggle_hdr",
    InputUtil.Type.KEYSYM,
    GLFW.GLFW_KEY_H,
    "category.the-virus-block"
);

// Handle toggle (in client tick event)
if (hdrToggleKey.wasPressed()) {
    RenderConfig config = RenderConfig.get();
    boolean newState = !config.isHdrEnabled();
    config.setHdrEnabled(newState);
    PostFxPipeline.getInstance().invalidateTargets();
    
    // Optional: Show on-screen message
    MinecraftClient.getInstance().player.sendMessage(
        Text.literal("HDR: " + (newState ? "ON" : "OFF")),
        true  // Action bar
    );
}
```

---


### Phase 6: HDR Pipeline Shaders

#### 6.1 Glow Extraction Shader

**File:** `src/main/resources/assets/the-virus-block/shaders/post/hdr/glow_extract.fsh`

```glsl
#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GLOW EXTRACTION - Pass 1 of HDR Pipeline
// 
// Renders the field effect into an RGBA16F buffer WITHOUT clamping.
// Allows intensity values > 1.0 for proper HDR glow accumulation.
// ═══════════════════════════════════════════════════════════════════════════

#include "include/core/field_visual_base.glsl"
#include "include/core/field_visual_preamble.glsl"
#include "include/effects/pulsar_v7.glsl"

void main() {
    FIELD_VISUAL_PREAMBLE();
    FIELD_VISUAL_CHECK_EFFECT_TYPE(EFFECT_ENERGY_ORB);
    
    float maxDist = isSky ? 1000.0 : linearDist;
    
    // Render V7 pulsar effect
    PulsarV7Result pulsar = renderPulsarV7(
        camPos, ray.direction, forward, maxDist,
        sphereCenter, sphereRadius,
        FrameTimeUBO.x * AnimSpeed,
        field.HighlightColor.rgb, field.PrimaryColor.rgb,
        field.SecondaryColor.rgb, field.TertiaryColor.rgb,
        vec3(RayColorR, RayColorG, RayColorB),
        mix(vec3(RayColorR, RayColorG, RayColorB), field.HighlightColor.rgb, 0.7),
        int(NoiseOctaves), max(0.1, NoiseBaseScale),
        CoreSize,
        Intensity,
        RaySharpness, max(1.0, FadeScale), max(0.1, InsideFalloffPower),
        RayPower, CoronaPower, CoronaMultiplier * 0.02, CoreFalloff,
        CoronaWidth + 1.0, EdgeSharpness * 10.0 + 1.0, RingPower,
        SpeedHigh, SpeedLow, SpeedRay, SpeedRing,
        FadePower * 0.01,
        abs(NoiseSeed) < 0.001 ? 1.0 : NoiseSeed,
        max(0.1, EruptionContrast)
    );
    
    // KEY DIFFERENCE: Output raw HDR values - NO clamping or tonemapping!
    // Values > 1.0 are preserved in RGBA16F buffer
    fragColor = vec4(pulsar.color * Intensity, pulsar.alpha);
}
```

#### 6.2 Gaussian Blur Shader

**File:** `src/main/resources/assets/the-virus-block/shaders/post/hdr/gaussian_blur.fsh`

```glsl
#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GAUSSIAN BLUR - Pass 2/3 of HDR Pipeline
// 
// Separable 9-tap Gaussian blur. Run horizontally then vertically.
// Operates on RGBA16F input - preserves HDR range through blur.
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;

// SamplerInfo provided by Minecraft's post system
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Blur direction: (1,0) for horizontal, (0,1) for vertical
layout(std140) uniform BlurDirection {
    float DirectionX;
    float DirectionY;
};

in vec2 texCoord;
out vec4 fragColor;

// 9-tap Gaussian weights (sigma ≈ 2.0)
const float weights[5] = float[](
    0.227027,   // Center
    0.1945946,  // ±1
    0.1216216,  // ±2
    0.054054,   // ±3
    0.016216    // ±4
);

void main() {
    vec2 texelSize = 1.0 / InSize;
    vec2 direction = vec2(DirectionX, DirectionY);
    
    // Center sample
    vec3 result = texture(InSampler, texCoord).rgb * weights[0];
    
    // Symmetric samples
    for (int i = 1; i < 5; i++) {
        vec2 offset = direction * texelSize * float(i);
        result += texture(InSampler, texCoord + offset).rgb * weights[i];
        result += texture(InSampler, texCoord - offset).rgb * weights[i];
    }
    
    // NO clamp - preserve HDR range through blur chain
    fragColor = vec4(result, 1.0);
}
```

#### 6.3 Final Composite Shader

**File:** `src/main/resources/assets/the-virus-block/shaders/post/hdr/composite.fsh`

```glsl
#version 150

// ═══════════════════════════════════════════════════════════════════════════
// FINAL COMPOSITE - Pass 4 of HDR Pipeline
// 
// Combines blurred HDR glow with original scene.
// Tonemapping happens HERE and ONLY here.
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D SceneSampler;  // Original scene (minecraft:main)
uniform sampler2D GlowSampler;   // Blurred glow (RGBA16F)
uniform sampler2D DepthSampler;  // For optional depth-based effects

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CompositeParams {
    float GlowIntensity;    // Glow strength multiplier
    float TonemapExposure;  // Exposure for tonemapping
};

in vec2 texCoord;
out vec4 fragColor;

// ACES-ish tonemapping (fast approximation)
vec3 toneMapACES(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

// Simple exposure-based tonemap
vec3 toneMapExposure(vec3 hdr, float exposure) {
    return 1.0 - exp(-hdr * exposure);
}

void main() {
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec3 glow = texture(GlowSampler, texCoord).rgb;
    
    // Apply intensity scaling
    glow *= GlowIntensity;
    
    // Tonemap the glow signal ONLY (scene is already LDR)
    vec3 glowMapped = toneMapExposure(glow, TonemapExposure);
    
    // Additive composite
    vec3 final = scene + glowMapped;
    
    // Final clamp to [0,1] for display
    fragColor = vec4(clamp(final, 0.0, 1.0), 1.0);
}
```

---

### Phase 7: Config Integration

**File:** `src/client/java/net/cyberpunk042/client/gui/config/RenderConfig.java`

```java
package net.cyberpunk042.client.gui.config;

import net.cyberpunk042.log.Logging;

/**
 * Render quality configuration including HDR settings.
 */
public class RenderConfig {
    private static final RenderConfig INSTANCE = new RenderConfig();
    
    // HDR Settings
    private boolean enableHdrTargets = true;
    private float blurQuality = 1.0f;  // 0.5 = half-res blur
    private int blurIterations = 2;    // 2 = 4 passes (H+V twice)
    
    private RenderConfig() {}
    
    public static RenderConfig get() {
        return INSTANCE;
    }
    
    // === Getters ===
    
    public boolean isHdrEnabled() {
        return enableHdrTargets;
    }
    
    public float getBlurQuality() {
        return blurQuality;
    }
    
    public int getBlurIterations() {
        return blurIterations;
    }
    
    // === Setters ===
    
    public void setHdrEnabled(boolean enabled) {
        this.enableHdrTargets = enabled;
        Logging.RENDER.topic("config")
            .kv("hdr", enabled)
            .info("HDR targets {}", enabled ? "enabled" : "disabled");
    }
    
    public void setBlurQuality(float quality) {
        this.blurQuality = Math.max(0.25f, Math.min(1.0f, quality));
    }
    
    public void setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(8, iterations));
    }
    
    // === Persistence (integrate with GuiConfigPersistence) ===
    
    public void loadFromJson(com.google.gson.JsonObject json) {
        if (json.has("hdr_enabled")) {
            enableHdrTargets = json.get("hdr_enabled").getAsBoolean();
        }
        if (json.has("blur_quality")) {
            blurQuality = json.get("blur_quality").getAsFloat();
        }
        if (json.has("blur_iterations")) {
            blurIterations = json.get("blur_iterations").getAsInt();
        }
    }
    
    public void saveToJson(com.google.gson.JsonObject json) {
        json.addProperty("hdr_enabled", enableHdrTargets);
        json.addProperty("blur_quality", blurQuality);
        json.addProperty("blur_iterations", blurIterations);
    }
}
```

---

## 🧪 Phase 8: Validation Tests

### Test A: Multi-Pass Blur Stability
```
1. Enable HDR mode (RenderConfig.setHdrEnabled(true))
2. Spawn V7 orb with CoronaMultiplier = 100.0 (very bright)
3. Observe blur quality over 10+ blur iterations
4. Toggle HDR off and compare:
   - RGBA8: Visible banding, color stepping in gradients
   - RGBA16F: Smooth gradient, no stepping
```

### Test B: Tiny Contribution Accumulation  
```
1. Set CoronaMultiplier = 0.001 (tiny per-sample)
2. Set NoiseOctaves = 12 (many accumulation passes)
3. Expected:
   - RGBA8: Effect vanishes (sub-pixel values round to 0)
   - RGBA16F: Effect remains visible
```

### Test C: HDR Toggle Behavior
```
1. Render scene with both modes
2. Toggle RenderConfig.setHdrEnabled() at runtime
3. Verify no crash, no resource leak
4. Visual comparison should show smoother glow with HDR
```

---

## 📦 Deliverables Checklist

### Phase 1: Foundation
- [ ] `TextureFormat.java` - Enum with GL constants
- [ ] `HdrTargetFactory.java` - ThreadLocal factory

### Phase 2: Core Mixin
- [ ] `FramebufferFormatMixin.java` - Inject format at construction
- [ ] Update `mixins.*.json` to include new mixin

### Phase 3: JSON Extension (Optional - for defaults)
- [ ] `PostEffectProcessorJsonMixin.java` - Parse "format" from JSON
- [ ] Verify injection point against MC 1.21.6 decompilation

### Phase 4: Resize Handling
- [ ] Resolution change detection/handling (built into PostFxPipeline)

### Phase 5A: Multi-Pass JSON (Example/Reference)
- [ ] `field_visual_v7_hdr.json` - 4-pass HDR pipeline config

### Phase 5B: Runtime HDR Orchestrator ⭐ (Core for in-game control)
- [ ] `PostFxPipeline.java` - Target management with format switching
- [ ] `invalidateTargets()` method for instant toggle

### Phase 6: HDR Shaders
- [ ] `glow_extract.fsh` - Raw HDR output (no clamp)
- [ ] `gaussian_blur.fsh` - HDR-preserving blur
- [ ] `composite.fsh` - Tonemap + composite

### Phase 7: Config ⭐ (User Control)
- [ ] `RenderConfig.java` - Runtime HDR toggle API
- [ ] Integration with `GuiConfigPersistence`
- [ ] Keybind for in-game toggle (optional)

### Phase 8: Testing
- [ ] Blur stability test (RGBA8 vs RGBA16F)
- [ ] Accumulation test (tiny contributions)
- [ ] Toggle behavior test (no crash, no leak)

---

## ⚠️ Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Mixin injection point changes in MC update | Use `require = 0`, log warnings |
| GPU lacks RGBA16F (very old hardware) | TextureFormat.isSupported() check |
| Performance regression | Config toggle, half-res blur option |
| JSON parsing mismatch | Phase 3 is optional - runtime control works independently |

---

## 🚀 Implementation Status

### Sprint 1: Core Infrastructure ✅ DONE
1. ✅ **Phase 1** - `TextureFormat.java` + `HdrTargetFactory.java`
2. ✅ **Phase 2** - `FramebufferFormatMixin.java`
3. ✅ **Phase 7** - `RenderConfig.java` (runtime toggle API)

### Sprint 2: Runtime Pipeline ✅ DONE
4. ✅ **Phase 5B** - `PostFxPipeline.java` (runtime orchestrator)
5. ✅ **Phase 4** - Resize handling (built into PostFxPipeline)

### Sprint 3: Shaders ✅ DONE
6. ✅ **Phase 6** - HDR shaders:
   - `hdr/glow_extract.fsh` - Raw HDR output (no clamp)
   - `hdr/gaussian_blur.fsh` - 9-tap separable blur
   - `hdr/composite.fsh` - Tonemap + blend
   - `hdr/passthrough.vsh` - Vertex shader
7. ✅ Shader program JSONs (gaussian_blur.json, composite.json)
8. ✅ Example pipeline JSON (field_visual_v7_hdr.json)

### Sprint 4: Integration 🔜 TODO
9. ⏳ **GUI Toggle** - Add HDR toggle to settings panel
10. ⏳ **Hook Pipeline** - Call `PostFxPipeline.ensureTargetsReady()` in render loop
11. ⏳ **Visual Test** - Compare RGBA8 vs RGBA16F blur quality
12. ⏳ (Optional) JSON format parsing for declarative defaults

---

## 📋 Sprint 4 TODO: GUI HDR Toggle

To add the HDR toggle to your GUI:

```java
// In your settings panel (e.g., RenderSettingsPanel.java)
CheckboxWidget hdrToggle = new CheckboxWidget(
    x, y, 20, 20, 
    Text.literal("Enable HDR Targets"),
    RenderConfig.get().isHdrEnabled()
);

hdrToggle.onChange = (checked) -> {
    RenderConfig.get().setHdrEnabled(checked);
    PostFxPipeline.getInstance().invalidateTargets();
};
```

To wire the pipeline into rendering:

```java
// In WorldRendererFieldVisualMixin or similar
PostFxPipeline pipeline = PostFxPipeline.getInstance();
pipeline.ensureTargetsReady(width, height);

// Use pipeline targets for multi-pass rendering
HdrTarget glow = pipeline.getGlowExtractTarget();
// ... render to glow target ...
```

---

*Updated: 2026-01-10*  
*Status: Sprints 1-3 Complete, Sprint 4 (GUI Integration) Pending*  
*Approach: Satin-style mixin + Hybrid runtime/JSON control*  
*Focus: In-game HDR toggle via RenderConfig + PostFxPipeline*
