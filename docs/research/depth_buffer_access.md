# Depth Buffer Access Research - December 30, 2025

## Objective
Access the depth buffer in Minecraft 1.21.6 to implement a **terrain-conforming shockwave effect**.

---

## Background

The shockwave effect requires:
1. Reading depth values from the rendered scene
2. Calculating world-space distance from depth
3. Applying visual distortion at specific distances (ring effect)
4. Running in real-time (60fps+)

---

## Approaches Attempted

### Approach 1: PostEffectProcessor (Failed)

**Method:** Use Minecraft's built-in PostEffectProcessor system with custom shaders.

**Implementation:**
- Created `depth_test.json` post-effect configuration
- Created `depth_test.fsh` fragment shader to sample depth
- Injected into WorldRenderer after terrain rendering

**Result:** ❌ FAILED
- The shader received depth = 1.0 for all terrain pixels
- Only the held item showed correct depth values
- Terrain appeared completely white (far plane)

**Cause Analysis:**
- The PostEffectProcessor runs as a pass within the FrameGraph
- At the time it runs, terrain depth may not yet be written
- OR it samples from a different framebuffer than the main depth

---

### Approach 2: Direct Framebuffer Access (Partial Success)

**Method:** Access `getDepthAttachmentView()` directly from `client.getFramebuffer()`.

**Implementation:**
- Created `WorldRendererDepthTestMixin` injecting AFTER `FrameGraphBuilder.run()`
- Attempted to blit depth texture using RenderPass API

**Result:** ⚠️ PARTIAL
- Confirmed `getDepthAttachment()` returns non-null `GpuTexture`
- Confirmed `getDepthAttachmentView()` returns non-null `GpuTextureView`
- Blit attempt executed without errors but showed no visible output
- Likely format mismatch: DEPTH32 texture → RGBA8 expected by blit shader

---

### Approach 3: glReadPixels (SUCCESS!)

**Method:** Use raw OpenGL `glReadPixels()` with `GL_DEPTH_COMPONENT` format.

**Implementation:**
- Inject after `frameGraph.run()` completes
- Call `GL11.glReadPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL_FLOAT, buffer)`
- Convert float depth values to grayscale image
- Upload to NativeImageBackedTexture for display

**Result:** ✅ SUCCESS
- Depth values range 0.997 - 0.999 (compressed due to non-linear depth buffer)
- Block shapes clearly visible in grayscale output
- Values change correctly when looking at different distances
- All terrain geometry is properly captured

---

## Key Findings

### 1. Injection Point Matters
The depth buffer is only valid AFTER `frameGraph.run()` completes. Earlier injection points see incomplete or cleared depth.

### 2. Non-Linear Depth Buffer
Minecraft uses a non-linear (hyperbolic) depth buffer:
- Near plane: ~0.05
- Far plane: ~1000
- All visible terrain compressed into 0.99 - 1.0 range
- Requires proper linearization for visualization

### 3. API Differences in 1.21
- `getDepthAttachment()` returns `GpuTexture` (not int)
- `getDepthAttachmentView()` returns `GpuTextureView`
- Many `RenderSystem` methods removed/changed
- Must use new RenderPass/CommandEncoder API for GPU operations

### 4. CPU vs GPU Access
- `glReadPixels`: Works but is CPU-side (slow for real-time effects)
- GPU-side sampling: Requires proper shader binding (not yet solved)

---

## Current Implementation

### DirectDepthRenderer.java

Located at: `src/client/java/net/cyberpunk042/client/visual/shader/DirectDepthRenderer.java`

**Modes:**
1. **Depth Range (Histogram)** - Shows depth distribution as colored bars
2. **Center Crosshair** - Shows depth-reactive crosshair with live values
3. **FBO Info** - Shows framebuffer binding status
4. **Full Depth Image** - Complete grayscale depth visualization

**Usage:**
- `/directdepth` - Cycle through modes
- `/directdepth <0-4>` - Set specific mode

---

## What's Working

| Feature | Status |
|---------|--------|
| Depth capture via glReadPixels | ✅ Working |
| Visualization as grayscale | ✅ Working |
| Correct geometry shapes | ✅ Working |
| Real-time updates | ✅ Working (but CPU-bound) |

---

## What's Not Working

| Feature | Status | Issue |
|---------|--------|-------|
| GPU-side depth sampling | ❌ | PostEffectProcessor shader gets depth=1.0 |
| RenderPass depth blit | ❌ | Format mismatch (DEPTH32 vs RGBA8) |

---

## Next Steps

### Goal: GPU-Side Depth Sampling for Shockwave

To achieve real-time performance, the shader must sample depth directly on the GPU.

**Option A: Fix PostEffectProcessor Binding**
- Investigate how to properly bind `depthAttachmentView` to shader samplers
- May need to manually inject the depth texture into the pass

**Option B: Custom RenderPass**
- Create a RenderPass that targets the color buffer
- Bind depth texture as a sampler
- Use custom shader for shockwave effect

**Option C: Hybrid Approach**
- Use glReadPixels for depth (already working)
- Pass depth data to shader as uniform buffer
- Trade accuracy for performance

---

## Files Created/Modified

### New Files
- `DirectDepthRenderer.java` - Depth visualization diagnostic tool
- `WorldRendererDepthTestMixin.java` - Injection point for depth access

### Modified Files
- `TheVirusBlockClient.java` - Added `/directdepth` command
- `ClientFieldNodes.java` - Added initialization and HUD callback

---

## Technical Notes

### Depth Linearization Formula
```java
float near = 0.05f;
float far = 1000.0f;
float linear = (2.0f * near) / (far + near - depth * (far - near));
```

### Contrast Enhancement (for 0.99-1.0 range)
```java
float normalized = (depth - minDepth) / (maxDepth - minDepth);
normalized = (float) Math.pow(normalized, 0.5);  // Gamma boost
```

### Key API Methods (1.21.6)
- `Framebuffer.getDepthAttachment()` → `GpuTexture`
- `Framebuffer.getDepthAttachmentView()` → `GpuTextureView`
- `RenderPass.bindSampler(String, GpuTextureView)` → Binds texture to shader
- `CommandEncoder.createRenderPass(...)` → Creates GPU render pass

---

## Test Progression (December 30, 2025)

### Tests 1-4: Basic Depth Access (COMPLETE)
- Mode 1: Depth Range Histogram ✅
- Mode 2: Center Crosshair ✅  
- Mode 3: FBO Info ✅
- Mode 4: Full Depth Image ✅

### Test 5: Distance Visualization (COMPLETE)
Converts depth to world-space distance in blocks and visualizes as color gradient.

**Features:**
- Color gradient: Red (0-5 blocks) → Orange → Yellow → Green → Cyan → Blue (100+)
- Sky detection (black for depth >= 0.9999)
- Reversed-Z toggle for testing
- Dynamic far plane based on render distance
- Diagnostic logging every second

**Commands:**
- `/directdepth 5` - Enable distance visualization
- `/directdepth reversedz` - Toggle reversed-Z mode

### Test 6: Ring at Fixed Distance (COMPLETE)
Highlights all pixels at a specific distance from the camera.

**Features:**
- Cyan ring at configurable distance
- Configurable ring thickness
- Dark background showing depth context
- Ring pixel count display

**Commands:**
- `/directdepth 6` - Enable ring mode
- `/directdepth ring <distance> <thickness>` - Configure ring (e.g., `/directdepth ring 15 2`)

### Test 7: Ring from World Position (COMPLETE)
Calculates world-space position from depth and highlights ring from player position.

**Features:**
- Uses player position as ring origin (sphere-like ring on terrain)
- Magenta ring color for visibility
- Full depth-to-world-position conversion
- Camera matrix reconstruction for ray casting

**Commands:**
- `/directdepth 7` - Enable world point ring mode
- `/directdepth ring <distance> <thickness>` - Configure ring distance

### Test 8: Animated Expanding Ring (COMPLETE)
Full shockwave simulation with animated expanding radius from player position.

**Features:**
- Animated ring expansion at configurable speed
- Trigger command to start animation
- Shows real-time radius and expansion status
- Reuses Mode 7's world-space calculations

**Commands:**
- `/directdepth 8` - Enable animated ring mode
- `/directdepth trigger` - Start the shockwave animation
- `/directdepth speed <n>` - Set expansion speed (blocks/sec)
- `/directdepth maxradius <n>` - Set maximum radius

---

## All Commands Summary

| Command | Description |
|---------|-------------|
| `/directdepth` | Cycle through modes |
| `/directdepth <0-8>` | Set specific mode |
| `/directdepth reversedz` | Toggle reversed-Z depth |
| `/directdepth ring <dist> <thick>` | Set ring distance and thickness |
| `/directdepth trigger` | Trigger Mode 8 animation |
| `/directdepth speed <n>` | Set animation speed |
| `/directdepth maxradius <n>` | Set max animation radius |
