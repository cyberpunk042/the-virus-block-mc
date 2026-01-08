# V8 Ground Transposition - TRUE Plan

## THE CONCEPT

The ring expands as a spherical shell from the orb.

- Where the shell is above ground → render in air
- Where the shell would go below ground → render ON the ground surface

**The ring BENDS onto the terrain. It doesn't pass through.**

---

## CURRENT PROBLEM

The corona computes the ring along the ray from camera. It doesn't know about the ground. When the ring would be underground, nothing happens - the ring just disappears.

---

## THE SOLUTION

When computing where to render the ring:
1. Check where the ground is (from depth buffer)
2. If the ring's 3D position would be underground → clamp it to the ground surface
3. Render the ring at the clamped position

**This is not adding a second ring. This is modifying WHERE the single ring renders.**

---

## IMPLEMENTATION

### In `animatedElectricCoronaV8` (or `renderPulsarV8`):

Currently the render position is computed from ray-sphere intersection.

Add:
1. Pass in `sceneDistance` (depth to geometry) and `isSky`
2. If NOT sky, clamp the render distance to not exceed `sceneDistance`
3. The ring renders at the clamped position

### Concrete Change:

```glsl
// Current: surfaceDist is used as-is
float surfaceDist = max(0.0, c / zoom - radius);

// NEW: clamp surfaceDist to not exceed the ground distance
if (!isSky) {
    vec3 groundPos = rayOrigin + rayDir * sceneDistance;
    float groundDistFromOrb = length(groundPos - sphereCenter);
    float groundSurfaceDist = max(0.0, groundDistFromOrb - radius);
    
    // If ring would be past the ground, clamp to ground
    surfaceDist = min(surfaceDist, groundSurfaceDist);
}

// Rest of ring computation uses clamped surfaceDist
```

**This makes the ring STOP at the ground instead of going through.**

---

## WHY THIS IS CORRECT

1. **ONE ring** - not computing a separate ground ring
2. **Modifying render position** - the ring is clamped to ground surface
3. **No separate cases** - same computation, just clamped distance
4. **The ring transposes** - where it would go underground, it appears ON the ground

---

## FILES TO MODIFY

1. **`pulsar_v8.glsl`** - Add sceneDistance/isSky params, add clamp logic
2. **`field_visual_v8.fsh`** - Pass linearDist and isSky

---

## THE RING'S BEHAVIOR

- Ring expands from orb
- Where it's above ground: visible in air (normal)
- Where it would go below ground: visible ON the ground surface
- Same ring, just clamped to terrain
