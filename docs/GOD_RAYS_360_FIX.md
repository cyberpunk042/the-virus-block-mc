# God Rays 360° Back Hemisphere Fix - Exploration Document

**Created**: 2026-01-13  
**Status**: In Progress  
**Related Commit**: `2a4faca21f1efa8bcaa28f6e01069bfdb53600d4` (fixed snapback, introduced this issue)

---

## The Problem

**Symptom**: When the orb is in the **back hemisphere** (90°+ away from camera forward):
- If camera has **horizontal offset** from orb → rays point **vertical** (wrong)
- If camera has **vertical offset** from orb → rays point **horizontal** (wrong)
- The angle changes when pitching the camera up/down

**Front hemisphere works correctly** - rays converge to the orb position as expected.

---

## Current Architecture

### Flow:
```
god_rays_accum.fsh
├── Calculate camera basis: right, up, forward
├── Project orb to camera space: xProj, yProj, zDist
├── Calculate parallelDir = normalize(vec2(xProj, yProj))  ← SUSPECT
├── Calculate frontLightUV (perspective projection)
├── Calculate backLightUV = center + parallelDir * 5.0
├── Blend: lightUV = mix(frontLightUV, backLightUV, parallelFactor)
└── Pass to accumulateGodRaysStyled()

god_rays.glsl
├── effectiveLightUV = getArrangedLightUV(lightUV, ...)
├── rayDir = getGodRayDirection(pixelUV, effectiveLightUV, ...)
│   └── outward = normalize(effectiveLightUV - pixelUV)
└── March rays in rayDir direction
```

### The Bug Location:

```glsl
// In god_rays_accum.fsh, lines 93-96:
vec2 orbDir2D = vec2(xProj, yProj);
float orbDir2DLen = length(orbDir2D);
vec2 parallelDir = (orbDir2DLen > 0.001) ? normalize(orbDir2D) : vec2(0.0, 1.0);
```

**Why it's wrong**: `xProj` and `yProj` are dot products with the camera's `right` and `up` vectors. When the camera pitches:
- `up` vector tilts (has forward/backward component)
- `yProj` becomes contaminated with depth information
- `parallelDir` points in the wrong screen direction

---

## Candidate Fixes (Strategic Order)

### Fix A: Use Perspective-Derived Direction (Simplest)
**Concept**: Derive `parallelDir` from `frontLightUV`'s direction, not raw camera-space values.

**Why it might work**: `frontLightUV` is perspective-projected and correctly handles camera orientation.

**Implementation**:
```glsl
// After calculating frontLightUV:
vec2 perspectiveDir = frontLightUV - vec2(0.5, 0.5);
float perspDirLen = length(perspectiveDir);
vec2 parallelDir = (perspDirLen > 0.001) ? normalize(perspectiveDir) : vec2(0.0, 1.0);
```

**Risk**: Already tried, didn't work. May need investigation into WHY.

**Status**: [ ] Not tested / [x] Failed / [ ] Partial / [ ] Success

**Notes**: (fill in after testing)

---

### Fix B: World-Space to Screen-Space Projection
**Concept**: Project the orb's world-space position directly to screen-space direction, bypassing the tilted camera basis entirely.

**Theory**: The issue is using `right`/`up` vectors that tilt with camera pitch. Instead, use a fixed world-to-screen mapping:
- World horizontal (X/Z) → Screen horizontal (X)
- World vertical (Y) → Screen vertical (Y)

**Implementation**:
```glsl
// Get the orb's position in a screen-aligned coordinate system
// Rather than projecting onto tilted camera up/right

// World-space direction to orb (horizontal plane only)
vec2 orbHorizontalWorld = vec2(toOrb.x, toOrb.z);  // XZ plane
float orbHorizontalLen = length(orbHorizontalWorld);

// World-space vertical offset
float orbVerticalWorld = toOrb.y;

// Now we need to convert this to screen space...
// This requires knowing which way the camera is facing (yaw)
float cameraYaw = atan(forward.z, forward.x);

// Rotate horizontal world direction by camera yaw to get screen direction
vec2 screenHorizontal = rotateVec2(normalize(orbHorizontalWorld), -cameraYaw);

// Combine with vertical for final screen direction
vec2 parallelDir = normalize(vec2(screenHorizontal.x, orbVerticalWorld));
```

**Risk**: Complex, may have edge cases at poles (looking straight up/down).

**Status**: [ ] Not tested / [ ] Failed / [ ] Partial / [ ] Success

**Notes**: (fill in after testing)

---

### Fix C: Separate Horizontal and Vertical Components
**Concept**: The horizontal component (`xProj`) is mostly correct. The vertical component (`yProj`) is contaminated. Handle them separately.

**Theory**: 
- Keep `xProj` for screen X (horizontal offset is reliable)
- Calculate screen Y from world-space height difference directly

**Implementation**:
```glsl
// Horizontal: use xProj (reliable - right vector is always horizontal)
float screenX = xProj;

// Vertical: use world-space Y difference, scaled appropriately
// This avoids the tilted 'up' vector contamination
float screenY = toOrb.y * some_scale_factor;

vec2 parallelDir = normalize(vec2(screenX, screenY));
```

**Risk**: Need to determine correct scale factor for Y. May not handle all camera orientations.

**Status**: [ ] Not tested / [ ] Failed / [ ] Partial / [ ] Success

**Notes**: (fill in after testing)

---

### Fix D: Use CameraUpUBO Directly
**Concept**: Instead of computing `up = cross(right, forward)`, use the actual camera up vector from the UBO.

**Theory**: The engine's camera up vector might already be screen-aligned, avoiding the tilt issue.

**Implementation**:
```glsl
// Instead of:
vec3 up = normalize(cross(right, forward));

// Try:
vec3 up = normalize(CameraUpUBO.xyz);
```

**Risk**: CameraUpUBO might be the same tilted vector. Need to test.

**Status**: [ ] Not tested / [ ] Failed / [ ] Partial / [ ] Success

**Notes**: (fill in after testing)

---

### Fix E: Revert to Ray Direction Blending (Previous Approach)
**Concept**: Re-add the removed ray direction blending from commit `2a4faca21`.

**What was removed**:
```glsl
// In god_rays.glsl (was removed in 2a4faca21):
vec2 radialDir = (dist > 0.001) ? normalize(toLight) : parallelDir;
vec2 blendedDir = normalize(mix(radialDir, parallelDir, parallelFactor));
```

**Theory**: This blending might have been compensating for the parallelDir issue by smoothing out the transition.

**Risk**: This was removed to fix the "snapback glitch" - might reintroduce that issue.

**Status**: [ ] Not tested / [ ] Failed / [ ] Partial / [ ] Success

**Notes**: (fill in after testing)

---

### Fix F: Hybrid Approach
**Concept**: Combine multiple fixes - e.g., use perspective-derived direction for the angle, but apply magnitude clamping and blending.

**Details**: TBD based on results from A-E.

**Status**: [ ] Not tested / [ ] Failed / [ ] Partial / [ ] Success

---

## Testing Protocol

For each fix:
1. Apply the change
2. Test with orb in front (should still work)
3. Test with orb behind, camera level (horizontal offset)
4. Test with orb behind, camera pitched up
5. Test with orb behind, camera pitched down
6. Test rotation around orb at various pitches
7. Check for snapback glitch at 90° boundary

---

## Progress Log

| Date | Fix | Result | Notes |
|------|-----|--------|-------|
| 2026-01-13 | A (Perspective-Derived) | Failed | Direction still wrong |
| | | | |

---

## References

- Commit that introduced issue: `2a4faca21f1efa8bcaa28f6e01069bfdb53600d4`
- Previous working state: `2a4faca21^` (parent commit)
- Related files:
  - `shaders/post/hdr/god_rays_accum.fsh` (main projection logic)
  - `shaders/post/include/effects/god_rays.glsl` (accumulation)
  - `shaders/post/include/effects/god_rays_style.glsl` (direction calculation)
