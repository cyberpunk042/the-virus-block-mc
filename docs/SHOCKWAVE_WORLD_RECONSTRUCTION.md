# Shockwave GPU Post-Effect System

## Overview

The shockwave post-processing effect is a terrain-conforming visual effect that displays expanding/contracting rings in the game world. It supports two origin modes:

1. **CAMERA mode**: Rings expand from the player's camera position (depth-based)
2. **TARGET mode**: Rings expand from a fixed world position (e.g., raycast hit point)

This document covers the complete architecture, including the **critical mixin injection system** that enables real-time animation and parameter updates.

---

## Why Mixins Are Essential

The shockwave effect requires **continuous, per-frame updates** to function:

1. **Animation**: Ring radius changes every frame during expansion/contraction
2. **Camera Tracking**: In TARGET mode, we need the camera's current position and orientation to reconstruct world positions
3. **Parameter Updates**: User can change intensity, thickness, colors etc. via commands at any time

**Without the mixin injection system, the shader would only see the initial values from when it was loaded - no animation would occur.**

Minecraft's vanilla post-effect system loads uniform values once from JSON and doesn't provide a built-in mechanism to update them dynamically. Our mixins solve this by intercepting the render pipeline and injecting fresh values every frame.

---

## Architecture: Complete Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SHOCKWAVE RENDER PIPELINE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. USER TRIGGERS EFFECT                                                    │
│     └─ Command: /shockwavegpu cursor                                        │
│     └─ Calls: ShockwavePostEffect.trigger() / .setTargetPosition()          │
│     └─ Stores: Target world position, animation start time                  │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  2. WorldRendererShockwaveMixin (EVERY FRAME, during world render)          │
│     ├─ INJECTION POINT: Before FrameGraphBuilder.run()                      │
│     ├─ CAPTURES from render method parameters:                              │
│     │   ├─ Camera object (yaw, pitch for view direction)                    │
│     │   ├─ camX, camY, camZ (camera world position from locals)             │
│     │   ├─ FrameGraphBuilder (to add our post-effect pass)                  │
│     │   └─ framebufferSet (for depth buffer access)                         │
│     ├─ COMPUTES:                                                            │
│     │   └─ Forward vector from camera yaw/pitch                             │
│     ├─ STORES in ShockwavePostEffect static fields:                         │
│     │   ├─ cameraX, cameraY, cameraZ                                        │
│     │   └─ forwardX, forwardY, forwardZ                                     │
│     └─ CALLS: processor.render(frameGraphBuilder, width, height, fbSet)     │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  3. PostEffectPassMixin (EVERY FRAME, during pass execution)                │
│     ├─ INJECTION POINT: HEAD of PostEffectPass.render()                     │
│     ├─ FILTER: Only processes passes with "shockwave" in ID                 │
│     ├─ READS from ShockwavePostEffect static fields:                        │
│     │   ├─ getCurrentRadius() - animated, changes every frame!              │
│     │   ├─ getThickness(), getIntensity() - user-configurable               │
│     │   ├─ getTargetX/Y/Z() - world anchor point                            │
│     │   ├─ getCameraX/Y/Z() - camera position (from step 2)                 │
│     │   ├─ getForwardX/Y/Z() - camera direction (from step 2)               │
│     │   └─ getBlackout/Vignette/Tint amounts - screen effects               │
│     ├─ COMPUTES:                                                            │
│     │   ├─ Aspect ratio from window dimensions                              │
│     │   └─ FOV from game options                                            │
│     ├─ BUILDS: Std140 uniform buffer (128 bytes, 8 vec4)                    │
│     └─ REPLACES: uniformBuffers["ShockwaveConfig"] with fresh data          │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  4. shockwave_ring.fsh (GPU SHADER, executes on graphics card)              │
│     ├─ RECEIVES: Fresh uniform data from step 3                             │
│     ├─ SAMPLES: InSampler (scene color), DepthSampler (depth buffer)        │
│     ├─ FOR EACH PIXEL:                                                      │
│     │   ├─ Linearize depth → world distance                                 │
│     │   ├─ Reconstruct world position (if TARGET mode)                      │
│     │   ├─ Calculate distance from origin                                   │
│     │   ├─ Apply ring mask based on radius                                  │
│     │   └─ Composite ring glow onto scene                                   │
│     └─ OUTPUTS: Final colored pixel                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Mixin Details

### WorldRendererShockwaveMixin

**Target Class**: `net.minecraft.client.render.WorldRenderer`

**Injection Point**: `render()` method, right before `FrameGraphBuilder.run()` is called

**Purpose**: 
- Capture camera state at the exact moment of rendering (ensures consistency with depth buffer)
- Add the shockwave post-effect pass to the frame graph

**Key Technical Details**:
- Uses `LocalCapture.CAPTURE_FAILHARD` to capture local variables from the render method
- The local variable order must match Minecraft's exact bytecode (fragile, may break on updates)
- Injects the PostEffectProcessor using the modern 1.21.6 API: `processor.render(frameGraphBuilder, width, height, framebufferSet)`
- This modern API ensures the depth buffer is properly bound (unlike older approaches)

```java
// Critical: Compute forward vector from Camera object, not player
// Clamp pitch to ±89° to prevent gimbal lock
float yaw = (float) Math.toRadians(camera.getYaw());
float pitch = (float) Math.toRadians(camera.getPitch());
float maxPitch = (float) Math.toRadians(89.0);
pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));

float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
float fwdY = (float) (-Math.sin(pitch));
float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));
ShockwavePostEffect.updateCameraForward(fwdX, fwdY, fwdZ);
```

### PostEffectPassMixin

**Target Class**: `net.minecraft.client.gl.PostEffectPass`

**Injection Point**: `render()` method HEAD

**Purpose**:
- **THE KEY TO ANIMATION**: Replaces the uniform buffer contents EVERY FRAME
- Without this, uniforms would be static (loaded once from JSON)

**Key Technical Details**:
- Filters by pass ID containing "shockwave" (only affects our shader)
- Shadows the `uniformBuffers` map to access internal state
- Uses `Std140Builder` for memory-safe buffer construction
- Creates a NEW `GpuBuffer` each frame with fresh values
- Replaces the old buffer in the map (old one gets garbage collected)

```java
// This is what makes animation work!
Std140Builder builder = Std140Builder.onStack(stack, 128);
builder.putVec4(radius, thickness, intensity, time);  // radius changes every frame!
// ... more vec4s ...

GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
    () -> "ShockwaveConfig Dynamic",
    16,  // uniform buffer usage
    builder.get()
);
uniformBuffers.put("ShockwaveConfig", newBuffer);  // Replace with fresh data!
```

---

## Uniform Buffer Layout (ShockwaveConfig)

The shader receives data via a **std140 uniform block**. Total: 9 vec4 = 144 bytes.

| Vec4 | Offset | Components | Description |
|------|--------|------------|-------------|
| 0 | 0 | RingRadius, RingThickness, Intensity, Time | Basic ring parameters |
| 1 | 16 | RingCount, RingSpacing, ContractMode, GlowWidth | Multi-ring settings |
| 2 | 32 | TargetX, TargetY, TargetZ, UseWorldOrigin | Target world position |
| 3 | 48 | CameraX, CameraY, CameraZ, AspectRatio | Camera world position |
| 4 | 64 | ForwardX, ForwardY, ForwardZ, Fov | Camera forward direction |
| 5 | 80 | UpX, UpY, UpZ, Reserved | Camera up direction |
| 6 | 96 | BlackoutAmount, VignetteAmount, VignetteRadius, Reserved | Screen effects |
| 7 | 112 | TintR, TintG, TintB, TintAmount | Color tint overlay |
| 8 | 128 | RingR, RingG, RingB, RingOpacity | Ring color override |

**Important**: The JSON configuration must define these uniforms, but their values are overwritten by the mixin every frame.

---

## World Position Reconstruction

### The Challenge

In TARGET mode, we need to determine "which pixels are X blocks away from the target point". This requires converting each screen pixel back to world coordinates:

1. Screen position (UV) + Depth buffer value → World XYZ

### The Formula

```glsl
vec3 reconstructWorldPos(vec2 uv, float linearDepth) {
    vec3 camPos = vec3(CameraX, CameraY, CameraZ);
    vec3 forward = normalize(vec3(ForwardX, ForwardY, ForwardZ));
    
    // Compute camera-local right and up vectors
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 right = normalize(cross(forward, worldUp));
    vec3 up = normalize(cross(right, forward));  // LOCAL up, tilts with pitch
    
    // Convert UV to NDC (-1 to 1)
    vec2 ndc = uv * 2.0 - 1.0;
    
    // Build ray direction using perspective FOV
    float halfFovTan = tan(Fov * 0.5);
    vec3 rayDir = forward + right * (ndc.x * halfFovTan * AspectRatio) 
                          + up * (ndc.y * halfFovTan);
    rayDir = normalize(rayDir);
    
    // World position = camera + ray * distance
    return camPos + rayDir * linearDepth;
}
```

### Depth Linearization

**Critical Fix Discovered**: The depth buffer stores values in [0, 1] range, but the standard linearization formula expects NDC Z in [-1, 1] range.

```glsl
float linearizeDepth(float depth, float near, float far) {
    // Convert depth buffer (0-1) to NDC Z (-1 to 1) FIRST
    float ndcZ = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndcZ * (far - near));
}
```

---

## Challenges Solved

### 1. Pitch Stability (Up/Down Movement)

**Problem**: Ring moved when looking up/down, but was stable when looking left/right.

**Root Cause**: The original code had `ndc.y = -ndc.y` (Y-flip for OpenGL convention), but this was double-inverting.

**Solution**: Remove the Y-flip. Our forward/up vectors already account for the coordinate system.

### 2. Local Up Vector

**Problem**: Using world up `(0, 1, 0)` directly caused incorrect ray directions when pitched.

**Solution**: Compute camera-local up from cross products:
```glsl
vec3 right = normalize(cross(forward, worldUp));
vec3 up = normalize(cross(right, forward));  // Tilts with pitch
```

### 3. Gimbal Lock at Extreme Pitch

**Problem**: When looking straight up/down (±90° pitch), `cross(forward, worldUp)` produces a zero vector because forward becomes parallel to worldUp.

**Root Cause Analysis**:
- At pitch = 90°, forward = (0, -1, 0)
- forward.xz = (0, 0) - NO horizontal component
- `cross(forward, worldUp) = 0` - undefined result
- The yaw information is completely lost in the forward vector at this angle

**Failed Approaches**:
1. Shader-side fallback with threshold switching - caused abrupt 180° flip when crossing threshold
2. Smooth blending between methods - still produced glitches at certain yaw angles  
3. Fixed fallback direction - worked for one yaw but flipped at other angles
4. Forward.y clamping in shader - didn't help when forward.xz was already zero

**Final Solution**: Clamp pitch in Java BEFORE computing forward vector:
```java
// Clamp pitch to ±89° to prevent gimbal lock
float maxPitch = (float) Math.toRadians(89.0);
pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));
```

This ensures:
- forward always has horizontal component: cos(89°) ≈ 0.017
- `cross(forward, worldUp)` is always well-defined
- Works at ALL yaw angles
- Visually indistinguishable from exact 90°

### 4. Horizontal Offset/Drift

**Problem**: Slight horizontal offset when rotating left/right.

**Root Cause**: Missing NDC conversion. Formula expected [-1, 1] but received [0, 1].

**Solution**: Add `ndcZ = depth * 2.0 - 1.0` before linearization.

### 5. Camera/Player Sync

**Problem**: Forward direction didn't match actual camera.

**Solution**: Use `camera.getYaw()` and `camera.getPitch()` from the Camera object in WorldRendererMixin, not from `client.player`.

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `shockwave_ring.fsh` | GLSL fragment shader - world reconstruction, ring rendering |
| `shockwave_ring.json` | Post-effect configuration - uniform definitions, sampler bindings |
| `ShockwavePostEffect.java` | State manager - animation, coordinates, parameters |
| `WorldRendererShockwaveMixin.java` | Captures camera state, injects post-effect pass |
| `PostEffectPassMixin.java` | **THE HEARTBEAT** - updates uniforms every frame |

---

## Testing Checklist

- [x] **Camera Mode**: Rings expand from player, stable in all directions
- [x] **Target Mode (Cursor)**: `/shockwavegpu cursor` - ring stays at hit point
- [x] **Yaw Stability**: Look left/right - ring doesn't drift
- [x] **Pitch Stability**: Look up/down - ring doesn't drift
- [x] **Extreme Pitch**: Look straight up/down - smooth transition ✓ (pitch clamped in Java)
- [x] **Depth Accuracy**: Ring conforms to terrain at correct distance
- [x] **Animation**: Ring expands/contracts smoothly
- [x] **Screen Effects**: Blackout, vignette, tint work correctly
- [x] **Ring Colors**: Custom RGB + opacity work correctly

---

## Lessons Learned

1. **Mixin Injection is Everything**: Without replacing uniform buffers per-frame, there's no animation

2. **Camera Object vs Player Entity**: Always use the Camera object for view-related data

3. **NDC Conversion Matters**: Depth buffers store [0,1], formulas expect [-1,1]

4. **LocalCapture is Fragile**: Variable order must match exact bytecode

5. **Gimbal Lock is Real**: Cross products fail when vectors are parallel

6. **std140 Layout Rules**: GPU buffer alignment has strict requirements

---

## Future Improvements

1. Pass inverse view-projection matrix instead of manual reconstruction
2. Support for reversed-Z depth buffers
3. Memory pooling for uniform buffers (avoid per-frame allocation)
4. Segment/arc rendering mode
