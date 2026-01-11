# HDR BlurRadius Dynamic Injection - Implementation Plan

**Created**: 2026-01-11  
**Objective**: Send `BlurRadius` value from UI slider to `gaussian_blur.fsh` shader via UBO injection

---

## Overview

### Current State (VERIFIED)
- `RenderConfig.java` has `hdrEnabled`, `blurQuality`, `blurIterations` but **NO `blurRadius`**
- `FieldVisualSubPanel.java` has **NO "Glow Spread" slider** yet
- `gaussian_blur.fsh` shader exists, uses `InSampler` (correct), but has NO `HdrConfig` UBO
- `glow_add.fsh` **DOES NOT EXIST**
- JSON generator uses `"In"` for blur sampler but shader uses `InSampler` - **MISMATCH**

### Target State
- BlurRadius flows: UI Slider → RenderConfig → HdrConfigUBO → Mixin → Shader
- Full HDR pipeline works with dynamic blur spread control

---

## Files to CREATE

### 1. `src/client/java/net/cyberpunk042/client/visual/ubo/HdrConfigUBO.java`

**Purpose**: UBO record that holds HDR configuration (BlurRadius, GlowIntensity)

**Pattern to follow**: `VirusBlockUBO.java` - uses `@UBOStruct` annotation with `@Vec4` fields

**Complete implementation**:

```java
package net.cyberpunk042.client.visual.ubo;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;
import net.cyberpunk042.client.gui.config.RenderConfig;

/**
 * UBO for HDR pipeline dynamic parameters.
 * 
 * <p>Injected by PostEffectPassMixin for gaussian_blur passes.
 * Provides dynamic BlurRadius from UI slider.</p>
 * 
 * <h3>GLSL Layout (std140):</h3>
 * <pre>
 * layout(std140) uniform HdrConfig {
 *     float BlurRadius;     // offset 0
 *     float GlowIntensity;  // offset 4
 *     float HdrPad1;        // offset 8
 *     float HdrPad2;        // offset 12
 * };
 * </pre>
 * 
 * <h3>Usage:</h3>
 * <pre>{@code
 * HdrConfigUBO ubo = HdrConfigUBO.fromConfig();
 * ReflectiveUBOWriter.write(builder, ubo);
 * }</pre>
 * 
 * @see net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter
 * @see net.cyberpunk042.client.gui.config.RenderConfig#getBlurRadius()
 */
@UBOStruct(name = "HdrConfig", glslPath = "the-virus-block:shaders/post/hdr/gaussian_blur.fsh")
public record HdrConfigUBO(
    @Vec4 HdrParamsVec4 hdrParams
) {
    /**
     * Buffer size in bytes (1 vec4 = 16 bytes).
     */
    public static final int BUFFER_SIZE = ReflectiveUBOWriter.calculateBufferSize(HdrConfigUBO.class);
    
    /**
     * UBO name used in JSON and shader.
     * Must match exactly: JSON "uniforms" key and GLSL "uniform" block name.
     */
    public static final String UBO_NAME = "HdrConfig";
    
    /**
     * HDR parameters packed as vec4.
     * 
     * <p>Uses modern ReflectiveUBOWriter pattern: plain 4-float record
     * without Vec4Serializable boilerplate.</p>
     * 
     * @param blurRadius    Blur spread multiplier (from RenderConfig, range 0.001-20.0)
     * @param glowIntensity Reserved for future glow intensity control
     * @param pad1          Padding for std140 alignment
     * @param pad2          Padding for std140 alignment
     */
    public record HdrParamsVec4(
        float blurRadius, 
        float glowIntensity, 
        float pad1, 
        float pad2
    ) {}
    
    /**
     * Create UBO from current RenderConfig values.
     * 
     * <p>Called by PostEffectPassMixin every frame for gaussian_blur passes.</p>
     * 
     * @return HdrConfigUBO with current BlurRadius from UI slider
     */
    public static HdrConfigUBO fromConfig() {
        float blurRadius = RenderConfig.get().getBlurRadius();
        
        return new HdrConfigUBO(
            new HdrParamsVec4(
                blurRadius,   // BlurRadius from slider
                1.0f,         // GlowIntensity (reserved)
                0.0f,         // Padding
                0.0f          // Padding
            )
        );
    }
    
    /**
     * Create UBO with explicit values (for testing).
     * 
     * @param blurRadius explicit blur radius value
     * @return HdrConfigUBO with specified BlurRadius
     */
    public static HdrConfigUBO withBlurRadius(float blurRadius) {
        return new HdrConfigUBO(
            new HdrParamsVec4(blurRadius, 1.0f, 0.0f, 0.0f)
        );
    }
}
```

**Location**: `/src/client/java/net/cyberpunk042/client/visual/ubo/HdrConfigUBO.java`

**Dependencies**:
- `net.cyberpunk042.client.visual.ubo.annotation.UBOStruct`
- `net.cyberpunk042.client.visual.ubo.annotation.Vec4`
- `net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter`
- `net.cyberpunk042.client.gui.config.RenderConfig`

---

### 2. `src/main/resources/assets/the-virus-block/shaders/post/hdr/glow_add.fsh`

**Purpose**: Final pass of HDR pipeline - composites blurred glow onto scene

**Pattern to follow**: `composite.fsh` - reads multiple samplers, outputs final result

**Complete implementation**:

```glsl
#version 150

// ═══════════════════════════════════════════════════════════════════════════
// GLOW ADD - Pass 5 of HDR Pipeline
// ═══════════════════════════════════════════════════════════════════════════
//
// Composites the blurred glow (from blur_v) onto the scene (from main).
// Uses additive blending with intensity control from FieldVisualConfig.
//
// Inputs:
//   - Scene: Original rendered scene (minecraft:main after blit)
//   - Glow:  Blurred HDR glow (blur_v output)
//
// Output:
//   - Final composited image → minecraft:main
//
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D Scene;
uniform sampler2D Glow;

in vec2 texCoord;
out vec4 fragColor;

// FieldVisualConfig UBO - read Intensity for glow scaling
layout(std140) uniform FieldVisualConfig {
    // Position (Layer 1) - 4 floats
    float CenterX, CenterY, CenterZ, Radius;
    // Colors (Layer 1) - 16 floats
    float PrimaryR, PrimaryG, PrimaryB, PrimaryA;
    float SecondaryR, SecondaryG, SecondaryB, SecondaryA;
    float TertiaryR, TertiaryG, TertiaryB, TertiaryA;
    float HighlightR, HighlightG, HighlightB, HighlightA;
    // RayColor - 4 floats
    float RayColorR, RayColorG, RayColorB, RayColorA;
    // Animation - 4 floats  
    float Phase, AnimSpeed, Intensity, EffectType;
    // We only need Intensity from this UBO
};

// ═══════════════════════════════════════════════════════════════════════════
// SELECTIVE TONEMAP
// ═══════════════════════════════════════════════════════════════════════════
//
// Only compresses values > 1.0, preserves LDR scene exactly.
// This prevents the glow from affecting non-glowing areas.

vec3 selectiveTonemap(vec3 color) {
    vec3 result;
    for (int i = 0; i < 3; i++) {
        float v = color[i];
        if (v <= 0.8) {
            // Below threshold: pass through unchanged
            result[i] = v;
        } else if (v <= 1.2) {
            // Transition zone: smooth blend to compression
            float t = (v - 0.8) / 0.4;
            float compressed = 0.8 + 0.2 * t * t * (3.0 - 2.0 * t);
            result[i] = mix(v, compressed, t);
        } else {
            // HDR zone: Reinhard-style compression
            result[i] = 1.0 + (v - 1.0) / (1.0 + (v - 1.0));
        }
    }
    return result;
}

void main() {
    // Sample scene and blurred glow
    vec3 scene = texture(Scene, texCoord).rgb;
    vec3 glow = texture(Glow, texCoord).rgb;
    
    // Scale glow by Intensity from FieldVisualConfig
    // Use 0.125 base multiplier for subtle effect
    float glowIntensity = 0.125 * Intensity;
    vec3 scaledGlow = glow * glowIntensity;
    
    // Tonemap only the glow (preserve scene colors exactly)
    vec3 tonemappedGlow = selectiveTonemap(scaledGlow);
    
    // Additive blend
    vec3 result = scene + tonemappedGlow;
    
    // Final clamp to valid range
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
```

**Location**: `/src/main/resources/assets/the-virus-block/shaders/post/hdr/glow_add.fsh`

**Inputs from JSON pipeline**:
- `Scene` sampler → `minecraft:main` target
- `Glow` sampler → `blur_v` target
- `FieldVisualConfig` UBO (for Intensity value)

---

## Files to MODIFY

### 3. `src/main/resources/assets/the-virus-block/shaders/post/hdr/gaussian_blur.fsh`

**Purpose**: Add HdrConfig UBO and use BlurRadius in blur calculation

**Current state** (lines 20-27):
```glsl
// Blur direction - must be declared as UBO to match JSON
// Minecraft parses JSON "uniforms.BlurParams" as a uniform buffer block
layout(std140) uniform BlurParams {
    float DirectionX;   // 1.0 for horizontal, 0.0 for vertical
    float DirectionY;   // 0.0 for horizontal, 1.0 for vertical
    float BlurPad1;     // Padding
    float BlurPad2;     // Padding
};
```

**ADD after BlurParams** (insert at line 28):
```glsl

// Dynamic HDR parameters from mixin
// Injected every frame by PostEffectPassMixin
layout(std140) uniform HdrConfig {
    float BlurRadius;     // Blur spread multiplier (from UI slider, 0.001-20.0)
    float GlowIntensity;  // Reserved for future use
    float HdrPad1;        // Padding
    float HdrPad2;        // Padding
};
```

**Current state** (line 42):
```glsl
    vec2 blurDir = vec2(DirectionX, DirectionY) * texelSize;
```

**CHANGE to**:
```glsl
    // Apply BlurRadius to control blur spread
    // Higher BlurRadius = wider blur = more glow spread
    float radius = max(BlurRadius, 0.1);  // Clamp to prevent zero
    vec2 blurDir = vec2(DirectionX, DirectionY) * texelSize * radius;
```

---

### 4. `scripts/a4_generate_hdr_pipeline.py`

**Purpose**: Add HdrConfig UBO to blur passes and fix sampler name

**Current state** (around lines 109-120, horizontal blur pass):
```python
    blur_h_pass = {
        "_comment": "Pass 3: Horizontal Blur",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "In", "target": "swap"}],
        "output": "blur_h",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 1.0},
                {"name": "DirectionY", "type": "float", "value": 0.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ]
        }
    }
```

**CHANGE TO** (fix sampler + add HdrConfig):
```python
    blur_h_pass = {
        "_comment": "Pass 3: Horizontal Blur",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "InSampler", "target": "swap"}],  # FIXED: was "In"
        "output": "blur_h",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 1.0},
                {"name": "DirectionY", "type": "float", "value": 0.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ],
            # ADDED: HdrConfig - stub values, mixin injects real BlurRadius
            "HdrConfig": [
                {"name": "BlurRadius", "type": "float", "value": 1.0},
                {"name": "GlowIntensity", "type": "float", "value": 1.0},
                {"name": "HdrPad1", "type": "float", "value": 0.0},
                {"name": "HdrPad2", "type": "float", "value": 0.0}
            ]
        }
    }
```

**SAME CHANGES for vertical blur pass** (around lines 127-138):
- Change `"sampler_name": "In"` to `"sampler_name": "InSampler"`
- Add `"HdrConfig": [...]` uniform block

**Also fix line 94** (blit pass sampler):
- This uses `"In"` for `minecraft:post/blit` which is correct (Minecraft's blit shader uses `In`)
- No change needed for blit pass

---

### 5. `src/client/java/net/cyberpunk042/mixin/client/PostEffectPassMixin.java`

**Purpose**: Add handler for gaussian_blur passes that injects HdrConfig

**ADD import** (around line 15):
```java
import net.cyberpunk042.client.visual.ubo.HdrConfigUBO;
```

**ADD handler** in `theVirusBlock$updatePostEffectUniforms` method (around line 110, after VirusBlock handler):
```java
        // Handle HDR gaussian_blur passes
        if (id != null && id.contains("gaussian_blur")) {
            updateHdrConfigUniforms();
        }
```

**ADD new method** (after updateVirusBlockUniforms, around line 415):
```java
    // ═══════════════════════════════════════════════════════════════════════════
    // HDR CONFIG UNIFORM UPDATE (For gaussian_blur passes)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int hdrConfigInjectCount = 0;
    
    /**
     * Updates HdrConfig UBO with current BlurRadius from RenderConfig.
     * 
     * <p>Called for each gaussian_blur pass (horizontal and vertical).
     * Provides dynamic blur spread control from the UI slider.</p>
     */
    private void updateHdrConfigUniforms() {
        hdrConfigInjectCount++;
        
        // Guard: only inject if JSON defined HdrConfig
        if (!uniformBuffers.containsKey(HdrConfigUBO.UBO_NAME)) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Create UBO from current config
            HdrConfigUBO ubo = HdrConfigUBO.fromConfig();
            
            // Write to buffer using ReflectiveUBOWriter
            Std140Builder builder = Std140Builder.onStack(stack, HdrConfigUBO.BUFFER_SIZE + 16);
            ReflectiveUBOWriter.write(builder, ubo);
            
            // Close old buffer to prevent GPU memory leak
            closeOldBuffer(HdrConfigUBO.UBO_NAME);
            
            // Create new buffer
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "HdrConfig Dynamic",
                16,  // Buffer usage flag
                builder.get()
            );
            
            // Put into uniformBuffers map (unconditional put pattern)
            uniformBuffers.put(HdrConfigUBO.UBO_NAME, newBuffer);
            
            // Debug logging (occasional)
            if (hdrConfigInjectCount % 120 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("blurRadius", String.format("%.2f", ubo.hdrParams().blurRadius()))
                    .debug("Updated HdrConfig UBO");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update HdrConfig");
        }
    }
```

---

### 6. `src/client/java/net/cyberpunk042/client/gui/config/RenderConfig.java`

**Purpose**: Add `blurRadius` field for storing the glow spread value

**Current state**: Has `hdrEnabled`, `blurQuality`, `blurIterations` but NO `blurRadius`

**ADD field** (around line 43, after blurIterations):
```java
    /** Blur radius: controls glow spread. Higher = wider blur */
    private float blurRadius = 1.0f;
```

**ADD getter** (around line 88, after getBlurIterations):
```java
    /**
     * Get the blur radius (glow spread).
     * 
     * @return 0.001 to 20.0, where 1.0 is default
     */
    public float getBlurRadius() {
        return blurRadius;
    }
```

**ADD setter** (around line 134, after setBlurIterations):
```java
    /**
     * Set the blur radius (glow spread).
     * 
     * <p>Higher values = wider blur = more glow spread.
     * 
     * @param radius 0.001 to 20.0
     */
    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(0.001f, Math.min(20.0f, radius));
    }
```

**ADD to loadFromJson** (around line 158):
```java
        if (json.has("blur_radius")) {
            setBlurRadius(json.get("blur_radius").getAsFloat());
        }
```

**ADD to saveToJson** (around line 179):
```java
        json.addProperty("blur_radius", blurRadius);
```

**ADD to resetToDefaults** (around line 188):
```java
        blurRadius = 1.0f;
```

---

### 7. `src/client/java/net/cyberpunk042/client/gui/panel/sub/FieldVisualSubPanel.java`

**Purpose**: Add "Glow Spread" slider to UI

**Pattern to follow**: Look for existing sliders in this file (e.g., intensity, radius)

**First, find the import section and verify we have imgui slider support**

**ADD slider** in the rendering method (find where other sliders are, add after):
```java
        // ═══════════════════════════════════════════════════════════════════
        // GLOW SPREAD SLIDER
        // ═══════════════════════════════════════════════════════════════════
        
        ImGui.text("Glow Spread");
        float[] glowSpread = new float[] { RenderConfig.get().getBlurRadius() };
        if (ImGui.sliderFloat("##glowSpread", glowSpread, 0.1f, 20.0f, "%.1f")) {
            RenderConfig.get().setBlurRadius(glowSpread[0]);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Controls HDR glow blur spread (0.1 = tight, 20 = wide)");
        }
```

**Note**: The exact location depends on the existing panel structure. Find where other rendering-related sliders are placed.

---

## Execution Sequence

### Step 1: Modify RenderConfig.java
```bash
# Add blurRadius field
# Add getBlurRadius() and setBlurRadius()
# Add persistence (loadFromJson, saveToJson)
# Add to resetToDefaults()
```

### Step 2: Modify FieldVisualSubPanel.java
```bash
# Add "Glow Spread" slider
# Bind to RenderConfig.get().getBlurRadius() / setBlurRadius()
```

### Step 3: Create HdrConfigUBO.java
```bash
# Create the file with content from section 1 above
```

### Step 4: Create glow_add.fsh
```bash
# Create the file with content from section 2 above
```

### Step 5: Modify gaussian_blur.fsh
```bash
# Add HdrConfig UBO block
# Change blur calculation to use BlurRadius
```

### Step 6: Modify a4_generate_hdr_pipeline.py
```bash
# Fix sampler names: "In" → "InSampler" for blur passes
# Add HdrConfig UBO to blur passes
```

### Step 7: Regenerate JSON pipelines
```bash
python3 scripts/a4_generate_hdr_pipeline.py v5,v6,v7,v8 --chain full
```

### Step 8: Modify PostEffectPassMixin.java
```bash
# Add import for HdrConfigUBO
# Add gaussian_blur handler
# Add updateHdrConfigUniforms() method
```

### Step 9: Build
```bash
./gradlew build --no-daemon
```

### Step 10: Test
```bash
# Run Minecraft
# Enable HDR mode
# Move Glow Spread slider
# Verify blur spread changes visually
```

---

## Data Flow Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA FLOW DIAGRAM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐                                                       │
│  │ UI: Glow Spread  │                                                       │
│  │     Slider       │                                                       │
│  └────────┬─────────┘                                                       │
│           │ onChange()                                                      │
│           ▼                                                                 │
│  ┌──────────────────┐                                                       │
│  │  RenderConfig    │                                                       │
│  │  .blurRadius     │  ◄── Stored value (0.001 - 20.0f)                    │
│  └────────┬─────────┘                                                       │
│           │ getBlurRadius()                                                 │
│           ▼                                                                 │
│  ┌──────────────────┐                                                       │
│  │ HdrConfigUBO     │                                                       │
│  │ .fromConfig()    │  ◄── Creates UBO record                              │
│  └────────┬─────────┘                                                       │
│           │ ReflectiveUBOWriter.write()                                     │
│           ▼                                                                 │
│  ┌──────────────────┐                                                       │
│  │ PostEffectPass   │                                                       │
│  │ Mixin            │  ◄── Injects buffer into uniformBuffers map          │
│  └────────┬─────────┘                                                       │
│           │ uniformBuffers.put("HdrConfig", buffer)                         │
│           ▼                                                                 │
│  ┌──────────────────┐                                                       │
│  │ gaussian_blur    │                                                       │
│  │ .fsh             │  ◄── Reads BlurRadius, applies to blur offset         │
│  └────────┬─────────┘                                                       │
│           │ blurDir = direction * texelSize * BlurRadius                    │
│           ▼                                                                 │
│  ┌──────────────────┐                                                       │
│  │ Visual Output    │  ◄── Blur spread changes with slider!                │
│  └──────────────────┘                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Risk Mitigation

### Potential Issues

1. **Sampler name mismatch**
   - Symptom: Black screen or missing texture
   - Fix: Ensure JSON `sampler_name` matches shader `uniform sampler2D` name exactly

2. **UBO layout mismatch**
   - Symptom: Wrong values read by shader (BlurRadius doesn't affect)
   - Fix: Ensure HdrConfigUBO field order matches shader uniform block order

3. **Missing glow_add.fsh**
   - Symptom: Pipeline fails to load, crash or black screen
   - Fix: Create the shader file before running generator

4. **Guard check fails**
   - Symptom: Mixin doesn't inject (HdrConfig not in uniformBuffers)
   - Fix: Ensure JSON has HdrConfig defined in blur passes
