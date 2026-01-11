# God Rays Integration & Tuning Guide ☀️🌀

This guide covers the implementation and tuning of volumetric light shafts (god rays) within the mod's advanced HDR shader system.

---

## 1. Overview & Mental Model

God rays are implemented as a **screen-space radial blur** emanating from a light source (sun position or energy orb center) toward the current pixel.

### Input Sources:
1.  **Occlusion Mask (Primary)**: A black-and-white buffer where white areas allow light to pass and black areas block it.
2.  **Hybrid Approach**: Uses brightness for the orb core (to prevent self-occlusion) and depth for the terrain/entities.

---

## 2. Pipeline Reference (HDR Chain) 🚀

God rays are implemented as a 7-pass sequence managed by `scripts/a4_generate_hdr_pipeline.py`.

1.  **Pass 1: HDR Effect → `swap`**
    - Logic: Renders core orb (unclamped). Skips procedural rays if `GodRayEnabled > 0.5`.
2.  **Pass 2: Blit `swap` → `main`**
    - Logic: Transfers sharp effect core to main scene.
3.  **Pass 3: God Rays Mask → `god_mask`**
    - Logic: Generates occlusion mask. Samples `SceneSampler` (Brightness) and `DepthSampler`.
4.  **Pass 4: God Rays Accumulate → `god_accum`**
    - Logic: Performs radial blur toward the light source. Uses World-Projection Standard.
5.  **Pass 5: God Rays Blur H → `god_blur_h`**
    - Logic: Softens horizontal sampling noise.
6.  **Pass 6: God Rays Blur V → `god_blur_v`**
    - Logic: Softens vertical sampling noise.
7.  **Pass 7: God Rays Composite → `main`**
    - Logic: Additive blend of blurred rays using `RayColor` parameter.

---

## 3. Algorithm & Tuning 🛠️

### Core Parameters:
-   **N (Sample Count)**: `96`. High count reduces banding.
-   **Reach (Decay Factor)**: `0.97`. Range: 0.94 - 0.99.
-   **Strength (Exposure)**: `0.02`. Range: 0.005 - 0.1.

### The Accumulation Loop (`god_rays.glsl`):
```glsl
float illumination = 0.0, decay = 1.0;
vec2 uv = pixelUV;

for (int i = 0; i < N; i++) {
    uv += step; // Direction toward lightUV
    if (!onScreen(uv)) break;
    illumination += texture(occlusionTex, uv).r * decay;
    decay *= decayFactor;
}
return illumination * exposure;
```

---

## 4. Coordination: The "Skip" Pattern 🎭

To avoid visual doubling, the primary effect shaders (e.g., `pulsar_v7.fsh`) must suppress their own noise-based fake rays when the volumetric system is active.

- **Trigger**: `GodRayEnabled > 0.5`.
- **Implementation**: The library call (e.g., `renderPulsarV7`) accepts a `bool skipRays` flag.

| Shader | skipRays Value | Notes |
| :--- | :--- | :--- |
| `field_visual_v7_hdr.fsh` | `GodRayEnabled > 0.5` | Production HDR Standard. |
| `field_visual_v7.fsh` | `false` | LDR fallback ( fake rays only). |
| `glow_extract.fsh` | `false` | Preserves blur for bloom core. |

---

## 5. Projection & Stability (World-Projection Standard) 🧪

To prevent rays from "drifting" or "following the cursor" while rotating:

1.  **The Pitfall**: UBO coordinates (`CenterX/Y/Z`) are world-oriented offsets (`fieldPos - cameraPos`). They do NOT rotate with the camera.
2.  **The Fix**: Reconstruct absolute world position and use the engine's `ViewProjUBO`.
    ```glsl
    vec3 orbWorldPos = CameraWorldPositionUBO.xyz + vec3(CenterX, CenterY, CenterZ);
    vec4 clipPos = ViewProjUBO * vec4(orbWorldPos, 1.0);
    ```
3.  **Coordinate Origin Conflict (UNSTABLE / UNDER INVESTIGATION 🧪)**: OpenGL NDC is centered (-1 to 1) and bottom-up. Minecraft screen UV is top-left (0 to 1). 
    - **Symptom 1 (Vertical Mirroring)**: Rays follow the camera vertically in an inverted manner. This usually requires `1.0 - (ndc.y * 0.5 + 0.5)`.
    - **Symptom 2 (One Quadrant / Horizon Bug)**: Rays are constrained to a single corner and move inversely to rotation. If the rays project exactly to the screen center or the world horizon regardless of the source position, it indicates a **Data Provenance Failure** (zeroed coordinates).
    - **UBO Verification**: Large-scale offsets like `CameraWorldPositionUBO` are prone to being uninitialized `(0,0,0)` in the post-processing chain. 
    - **Current Direction**: If the `World-Projection Standard` fails, immediately roll back to the **Manual Perspective Fallback** (Section 5.4). Avoid adding axis flips to a broken data pipeline.

### 5.4 The Manual Perspective Fallback (Safe Baseline) 🛡️

If the authoritative `ViewProjUBO` matrix or `CameraWorldPositionUBO` produces unstable results (the "Horizon Bug"), use the manual perspective reconstruction. This method is immune to world-coordinate telemetry failure because it operates entirely on camera-relative offsets.

**The Logic**: Reconstruct a perspective projection matrix from FOV and Aspect Ratio, then project the camera-relative `CenterX/Y/Z` manually.

```glsl
vec3 projectViewSpaceToScreen(vec3 viewPos) {
    float fov = CameraUpUBO.w;        // Radians
    float aspect = CameraForwardUBO.w; // W/H
    
    float tanHalfFov = tan(fov * 0.5);
    float f = 1.0 / tanHalfFov;
    
    // Project to Clip Space
    float clipX = viewPos.x * (f / aspect);
    float clipY = viewPos.y * f;
    float clipW = -viewPos.z; // OpenGL convention: camera looks down -Z
    
    if (clipW <= 0.0) return vec3(0.5, 0.5, 0.0);
    
    // NDC & UV
    float ndcX = clipX / clipW;
    float ndcY = clipY / clipW;
    vec2 uv = vec2(ndcX, ndcY) * 0.5 + 0.5;
    
    float visible = (abs(ndcX) < 1.5 && abs(ndcY) < 1.5) ? 1.0 : 0.0;
    return vec3(uv, visible);
}
```
*Note: This method results in **Rotation Drift** (rays follow the camera slightly) but ensures the shafts are anchored to the orb regardless of world-space telemetry state.*

### 5.5 The Vector-Basis Fallback (Most Reliable) 🛡️⚓

If matrix-based fallbacks still show "Quadrant Clipping," use explicit **Basis Vector Projection**. This method manually derives the Right and Up vectors from the Camera Forward vector to project the 3D offset.

**The Logic**:
1.  **Basis Construction**: Build a temporary coordinate system centered on the camera.
2.  **Projection**: Dot-product the target vector (`CenterX/Y/Z`) against these basis vectors.
3.  **Normalization**: Divide X and Y projections by the Z distance to the object.

```glsl
// Project orb position using camera basis vectors
vec3 toOrb = vec3(CenterX, CenterY, CenterZ); // Camera-relative world offset
vec3 forward = normalize(CameraForwardUBO.xyz);
float fov = CameraUpUBO.w;
float aspect = CameraForwardUBO.w;

// Build Basis: Right and Up from Forward
vec3 right = normalize(cross(forward, vec3(0.0, 1.0, 0.0)));
vec3 up = normalize(cross(right, forward));

// Project onto axes
float zDist = dot(toOrb, forward);
if (zDist <= 0.0) return vec3(0.5, 0.5, 0.0); // Behind camera

float xProj = dot(toOrb, right);
float yProj = dot(toOrb, up);

// Convert to NDC using FOV (Perspective Divide)
float tanHalfFov = tan(fov * 0.5);
float ndcX = (xProj / zDist) / (tanHalfFov * aspect);
float ndcY = (yProj / zDist) / tanHalfFov;

// NDC to UV
vec2 lightUV = vec2(ndcX * 0.5 + 0.5, ndcY * 0.5 + 0.5);
float lightVisible = (abs(ndcX) < 1.5 && abs(ndcY) < 1.5) ? 1.0 : 0.0;
```
*Tip: This is the current **Production standard for God Rays** as of Step 1224, as it bypasses both UBO slot displacement and coordinate origin conflicts.*

### 5.6 The Emergency Baseline (Screen Center) 🆘🏮

If all mathematical projections fail to produce visible rays or result in static "Horizon" artifacts, use the **Hardcoded Center** fallback. This ensures the effect is always visible and follows the player's focus.

**The Implementation**:
```glsl
// god_rays_accum.fsh
vec2 lightUV = vec2(0.5, 0.5); // Fixed to screen center
float lightVisible = 1.0;      // Always on
```
*Note: This results in "Perfect Follow" (rays come from the cursor). While not spatitally anchored, it preserves the radial shaft visual and confirms the accumulation loop is working.*

---

### 5.7 Coordinate Space Warning: Camera-Relative Offsets 📏
UBO coordinates (`CenterX/Y/Z`) are **camera-relative offsets** (`fieldPos - cameraPos`), provided by `PostEffectPassMixin.java`.  The camera is always at `(0,0,0)` in this space.  This is why simply projecting them with a world `ViewProjUBO` often fails—you are projecting an offset as if it were a world coordinate.

---

## 6. Masking Strategy & Occlusion Paths 🌑

### 6.1 The "Stuck in Orb" Bug (Pathing Failure)
Even with correct projection, rays may appear "trapped" within the orb's volume if the surrounding 2D space is occluded in the mask.

- **Cause**: The radial blur marches from the pixel toward the `lightUV`. If the path between them is entirely black (occluded) in the mask, the accumulation loop adds nothing.
- **The "Sky" Confusion**: While the sky/background provides a natural "passable path" for rays to travel through, blindly forcing the sky mask `on` can confuse the user if they are only trying to fix the orb's shafts. 
- **Requirement**: For shafts to emanate FROM the orb, the mask must have non-zero (white/grey) values in the regions where shafts should appear. 
- **Guidance**: User rejection (Step 1248) confirms that "Forcing Sky" is not the correct solution for orb-specific shafts. Instead, ensure the effect being SHAFTED is sufficiently bright in the mask or that the background threshold allows some propagation.

### 6.2 Surface-Based Emission (Future)
Experimental goal: Sample rays from the orb's **physical surface** (`Center + Radius`) rather than the mathematical center to improve realism at close ranges.

---

## 7. UI Integration & Controls 🎛️

All controls are in `FieldVisualSubPanel.java` within the HDR section.

- **Reach**: `decayFactor`.
- **Strength**: `exposure`.
- **Threshold**: Brightness cutoff for rays.
- **Sky Rays**: Toggle for atmospheric shafts.

---

## 8. Implementation Checklist ⚠️

1.  **Sampler Suffix**: Declared as `uniform sampler2D OcclusionSampler;` if JSON input is `"Occlusion"`.
2.  **UBO Slot Parity**: Slot 50 (`GodRayParams`) and Slot 51 (`GodRayMaskParams`) must match exactly between Java and GLSL.
3.  **HDR Dependency**: Enforcement of `hdrEnabled` in `RenderConfig`.
4.  **Diagnostic**: Use `fragColor = vec4(lightUV, visible, 1.0)` to verify projection stability.

---
*Last Updated: 2026-01-11*
