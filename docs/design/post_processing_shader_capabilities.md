# Post-Processing Shader Capabilities (Minecraft 1.21.6)

> **Document Purpose**: Clear reference for what IS and IS NOT achievable with the current post-processing shader infrastructure. Use this before attempting new shader-based visual effects.

---

## ✅ What WORKS

### 1. Full-Screen Color Tinting
The pipeline successfully applies color modifications to the entire rendered scene.

**Confirmed Working:**
- Red/green/blue tint overlays
- Brightness/darkness adjustments
- Color channel manipulation (swap, invert, boost)
- Saturation changes
- Color matrix transformations

**Infrastructure:**
- JSON: `assets/<modid>/post_effect/<name>.json`
- Fragment Shader: `assets/<modid>/shaders/post/<name>.fsh`
- Vertex Shader: Use `minecraft:post/sobel` (standard)
- Sampler: `InSampler` bound via `"sampler_name": "In"`

**Example Effects Possible:**
| Effect | Description | Feasibility |
|--------|-------------|-------------|
| Screen Flash | Brief white/color flash | ✅ Easy |
| Damage Vignette | Red edges when hurt | ✅ Easy |
| Night Vision Tint | Green overlay | ✅ Easy |
| Sepia/Greyscale | Desaturation effects | ✅ Easy |
| Color Inversion | Enderman-style | ✅ Easy |
| Creeper Vision | Green mosaic filter | ✅ Easy |
| Breathing Pulse | Animated brightness wave | ✅ Medium (needs time uniform) |
| Screen Ripple | Distortion waves from center | ✅ Medium (UV manipulation) |
| Chromatic Aberration | RGB channel offset | ✅ Medium |
| Heat Haze | Animated UV distortion | ✅ Medium |

### 2. UV-Based Distortion Effects
The `texCoord` (screen UV) is fully accessible and can be manipulated.

**Possible:**
- Radial distortions from screen center
- Sine-wave ripples across screen
- Edge vignette darkening
- Lens distortion effects

### 3. Time-Based Animation
With a time uniform passed from Java, animated effects are possible.

**Requires:**
- Custom uniform injection via `PostEffectProcessor` API
- Or: Use `gl_FragCoord` / noise functions as pseudo-time

---

## ❌ What DOES NOT Work

### 1. Depth Buffer Sampling
**Status: BROKEN / INACCESSIBLE**

Despite using `"use_depth_buffer": true` in the JSON configuration, the `DepthSampler` returns `1.0` (far plane) for every pixel.

**Why It Fails:**
- The `processor.render(Framebuffer, Pool)` method (used for spectator effects) does NOT bind the depth attachment
- Vanilla spectator effects (creeper, spider, enderman) only tint colors—they never sample depth
- The depth buffer is cleared by Minecraft before our injection point fires
- The `FrameGraphBuilder` API used by the transparency pass is internal and not easily injectable

**Cannot Do:**
| Effect | Why Blocked |
|--------|-------------|
| Terrain-conforming shockwave | Requires world-space position from depth |
| Edge detection on geometry | Needs depth discontinuity data |
| Fog based on actual distance | Depth = 1.0 everywhere |
| Screen-space decals on terrain | Cannot project to world surface |
| Depth-of-field blur | No depth information |
| SSAO (ambient occlusion) | Requires depth + normals |

### 2. World-Space Position Reconstruction
Since depth sampling fails, reconstructing world-space positions via inverse projection/view matrices is impossible.

### 3. Per-Object Masking
Cannot isolate specific objects (entities, blocks) without stencil buffer access, which is also unavailable through this API.

---

## 🔧 Current Infrastructure

### Files Created:
```
src/client/java/net/cyberpunk042/client/visual/shader/DepthTestShader.java
src/client/java/net/cyberpunk042/mixin/client/GameRendererDepthTestMixin.java
src/main/resources/assets/the-virus-block/post_effect/depth_test.json
src/main/resources/assets/the-virus-block/post_effect/depth_full.json
src/main/resources/assets/the-virus-block/post_effect/depth_passthrough.json
src/main/resources/assets/the-virus-block/post_effect/depth_redtint.json
src/main/resources/assets/the-virus-block/post_effect/shockwave_ring.json
src/main/resources/assets/the-virus-block/shaders/post/depth_test.fsh
src/main/resources/assets/the-virus-block/shaders/post/depth_full.fsh
src/main/resources/assets/the-virus-block/shaders/post/depth_passthrough.fsh
src/main/resources/assets/the-virus-block/shaders/post/depth_redtint.fsh
src/main/resources/assets/the-virus-block/shaders/post/shockwave_ring.fsh
```

### Command:
- `/depthtest` - Cycles through modes 0-5
- `/depthtest <mode>` - Sets specific mode

### Modes:
| Mode | Name | Status |
|------|------|--------|
| 0 | OFF | ✅ Works |
| 1 | Debug Quadrants | ⚠️ Scene works, depth white |
| 2 | Full Depth | ❌ All black (depth=1.0) |
| 3 | Passthrough | ✅ Works |
| 4 | Red Tint | ✅ Works |
| 5 | Shockwave Ring | ❌ Invisible (depth mask fails) |

---

## 📚 External Solutions (Not Yet Integrated)

### Satin API
- **What it is:** Fabric library for managed post-processing shaders
- **Capabilities:** Readable depth texture, managed shader lifecycle, callbacks
- **Problem:** Only supports up to Minecraft **1.21.4** (we are on **1.21.6**)
- **URL:** https://github.com/Ladysnake/Satin

### Iris Shaders
- **What it is:** Full shader mod with extensive depth/buffer access
- **Note:** Would require compatibility layer or complete integration

---

## 🎯 Recommendations for Future Effects

### For Color-Based Effects (USE THIS SYSTEM):
- Screen flashes, vignettes, tints, pulses
- Use `DepthTestShader` as template
- Add new modes with new fragment shaders

### For Depth-Dependent Effects (DO NOT USE THIS SYSTEM):
- Terrain-conforming visuals
- World-space position effects
- Screen-space geometry effects
- **Requires:** Different approach (research pending)

---

## 🔬 Unexplored Tint Possibilities

The following effects could be achieved with the working `InSampler` + UV system but were not fully implemented:

1. **Breathing Effect**: Animated brightness oscillation
2. **Radial Ripples**: Sine-wave UV distortion from center
3. **Screen Shake**: Offset texCoord by small animated amount
4. **Color Pulse**: Cycle through color tints over time
5. **Edge Glow**: Add brightness at screen edges
6. **Scanlines**: Alternating brightness based on y-coordinate
7. **Static/Noise**: Overlay random noise texture
8. **Blur**: Multi-sample averaging (expensive but possible)

These remain viable for future exploration using the existing infrastructure.

---

*Document created: Dec 30, 2025*
*Minecraft version: 1.21.6*
*Last validation: Step 949*
