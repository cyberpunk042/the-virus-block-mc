# Phase 2: Ray Animation Core Implementation

## Approach: Foundation First

Build the infrastructure in **DATA FLOW ORDER**:
1. **Tessellator** produces geometry (vertices, UV coordinates, `t` parameter)
2. **Renderer** consumes geometry and applies animations
3. Output goes to GPU

We proceed systematically, layer by layer, ensuring each foundation is solid before building on top.

---

## DATA FLOW

```
RaysShape ──→ RaysTessellator ──→ Mesh ──→ RaysRenderer ──→ VertexConsumer ──→ GPU
   │                │                │            │
   │                │                │            └─ Applies: LengthMode, TravelMode, 
   │                │                │               FlickerMode, MotionMode, WiggleMode,
   │                │                │               TwistMode, ColorMode
   │                │                │
   │                │                └─ Contains: vertices[], normals[], uvs[], t[]
   │                │
   │                └─ Uses: lineShape, curvature, shapeSegments, etc.
   │
   └─ Config: count, length, arrangement, lineShape, curvature, etc.
```

---

## PHASE 2A: TESSELLATOR FOUNDATION

**File:** `RaysTessellator.java`
**Goal:** Generate correct multi-segment geometry with all data needed by renderer

### Step 2A.1: Multi-Segment Ray Infrastructure
**Priority:** FOUNDATION - everything else depends on this

**Current State:**
- Rays are 2 vertices (start, end)
- No intermediate segments for complex shapes

**Target State:**
- Rays have `shapeSegments` vertices when needed
- Each vertex has `t` parameter (0 at start, 1 at end)
- Each vertex has position, normal (if needed)

**Implementation:**
1. Check if `shapeSegments > 1` or `lineShape != STRAIGHT`
2. If so, emit `shapeSegments + 1` vertices along ray
3. Store `t = i / shapeSegments` at each vertex
4. This becomes the foundation for LineShape, Wiggle, Twist

### Step 2A.2: Perpendicular Frame Computation
**Priority:** REQUIRED for LineShape and Wiggle

**What:** For a ray direction, compute stable perpendicular vectors

**Implementation:**
1. Given ray direction `D`
2. Choose reference axis (prefer Y, fallback to X if ray is vertical)
3. Compute `right = normalize(D × reference)`
4. Compute `up = normalize(D × right)`
5. Now `right` and `up` are perpendicular to ray

### Step 2A.3: RayCurvature Implementation
**Priority:** After 2A.1

**What:** Modify ray direction based on curvature mode before generating vertices

**Implementation:**
1. Compute base radial direction (current behavior = NONE)
2. Compute tangent direction (perpendicular to radial in XZ plane)
3. For each curvature mode, blend or transform:
   - VORTEX: `lerp(radial, tangent, intensity)`
   - TANGENTIAL: pure tangent
   - SPIRAL_ARM: logarithmic spiral tangent
   - PINWHEEL: constant angle offset
   - LOGARITHMIC: golden ratio angle
   - ORBITAL: circular path tangent

### Step 2A.4: RayLineShape Implementation
**Priority:** After 2A.1 and 2A.2

**What:** Apply shape offset to each segment vertex

**Implementation:**
For each vertex at parameter `t`:
1. Compute base position along ray: `P = start + t * (end - start)`
2. Compute shape offset using `right` and `up` vectors:
   - SINE_WAVE: `offset = right * amplitude * sin(t * frequency * 2π)`
   - CORKSCREW: `offset = right * cos(θ) + up * sin(θ)` where `θ = t * frequency * 2π`
   - ZIGZAG: triangle wave
   - ARC: `offset = up * amplitude * sin(t * π)` (single bow)
   - etc.
3. Final position: `P + offset * lineShapeAmplitude`

---

## PHASE 2B: RENDERER FOUNDATION

**File:** `RaysRenderer.java`
**Goal:** Properly consume tessellated geometry and apply all animations

### Step 2B.1: Renderer Structure Audit
**Priority:** Understand current flow

**What:** Document the current vertex emission pipeline
- Where does tessellated mesh enter?
- How are vertices transformed?
- Where are colors/alpha calculated?
- What hooks exist for animation?

### Step 2B.2: Ensure t-Parameter Flows Through
**Priority:** FOUNDATION for all animations

**What:** The `t` parameter (0-1 along ray) must be available at each vertex

**Current State:** UV.u might contain this, but needs verification

**Implementation:**
1. Tessellator stores `t` in vertex data
2. Renderer reads `t` from vertex data
3. Animation functions receive `t`

### Step 2B.3: Animation Application Order
**Priority:** Define the animation pipeline

**Order of operations for each vertex:**
1. **MotionMode** - Transform vertex position (move entire ray)
2. **WiggleMode** - Deform vertex position (wiggle shape)
3. **TwistMode** - Rotate around local axis (if 3D shape)
4. **LengthMode** - Compute visibility multiplier
5. **TravelMode** - Compute brightness multiplier
6. **FlickerMode** - Compute alpha multiplier
7. **ColorMode** - Compute color

### Step 2B.4: LengthMode Implementation
**Priority:** After 2B.2

**All values:**
- NONE: alpha = 1.0
- RADIATE: visible from 0 to `phase`, where `phase` oscillates 0→1
- ABSORB: visible from `phase` to 1, where `phase` oscillates 0→1
- PULSE: both endpoints oscillate
- SEGMENT: fixed-length window slides along ray
- GROW_SHRINK: one-shot grow then shrink cycle

### Step 2B.5: TravelMode Implementation
**Priority:** After 2B.4

**All values:**
- NONE: uniform
- CHASE: `n` bright segments at positions `(offset + i * spacing) % 1`
- SCROLL: gradient `1 - |t - offset|` slides
- COMET: bright at front (t = offset), fades behind
- SPARK: random bright flashes
- PULSE_WAVE: traveling brightness waves
- REVERSE_CHASE: particles move toward center

### Step 2B.6: FlickerMode Implementation
**Priority:** After 2B.5

**All values:**
- NONE: no flicker
- STROBE: `sin(time * speed) > 0 ? 1 : 0`
- SCINTILLATION: per-ray random phase + strobe
- FADE_PULSE: `(sin(time * speed) + 1) / 2`
- FLICKER: noise-based random
- LIGHTNING: exponential decay after spike
- HEARTBEAT: double-pulse pattern

### Step 2B.7: MotionMode Implementation
**Priority:** After 2B.3

**All values:**
- NONE: no motion
- RADIAL_OSCILLATE: `position += radialDir * amplitude * sin(time * speed)`
- RADIAL_DRIFT: `position += radialDir * time * speed`
- ANGULAR_OSCILLATE: rotate around center by `sin(time * speed) * amplitude`
- ANGULAR_DRIFT: rotate around center by `time * speed`
- FLOAT: `y += amplitude * sin(time * speed)`
- SWAY: tip moves, base fixed (pivot rotation)
- ORBIT: continuous rotation around center
- JITTER: random noise offset
- PRECESS: ray axis traces cone pattern
- RIPPLE: wave propagation based on distance from center

### Step 2B.8: WiggleMode Implementation
**Priority:** After 2B.7, REQUIRES Multi-Segment

**All values:**
- Apply per-segment deformation based on `t` and `time`
- Each mode is a different wave function applied to segments

### Step 2B.9: TwistMode Implementation
**Priority:** After 2B.8

**All values:**
- Rotate segment positions around ray's local axis
- Only visible with non-STRAIGHT lineShape

### Step 2B.10: ColorMode Implementation
**Priority:** Can be done anytime

**New values:**
- HEAT_MAP: `t` or distance → hue
- RANDOM_PULSE: random flash from color set
- BREATHE: oscillating saturation
- REACTIVE: player distance → color

---

## EXECUTION ORDER (In Data Flow Order)

| Step | Task | Depends On | Description |
|------|------|------------|-------------|
| **2A.1** | Multi-Segment Infrastructure | None | Foundation for all geometry |
| **2A.2** | Perpendicular Frame | None | Math utility for shapes |
| **2A.3** | RayCurvature | 2A.1 | Modify ray directions |
| **2A.4** | RayLineShape | 2A.1, 2A.2 | Apply shape to segments |
| **2B.1** | Renderer Audit | None | Understand existing code |
| **2B.2** | t-Parameter Flow | 2A.1 | Ensure data reaches renderer |
| **2B.3** | Animation Order | 2B.1 | Define pipeline |
| **2B.4** | LengthMode | 2B.2 | Length visibility |
| **2B.5** | TravelMode | 2B.4 | Particle travel |
| **2B.6** | FlickerMode | 2B.5 | Alpha modulation |
| **2B.7** | MotionMode | 2B.3 | Position animation |
| **2B.8** | WiggleMode | 2A.1, 2B.7 | Per-segment deform |
| **2B.9** | TwistMode | 2A.4, 2B.8 | Axial rotation |
| **2B.10** | ColorMode | 2B.3 | Color animation |

---

## STEP-BY-STEP CHECKLIST

### Tessellator (2A)
- [ ] 2A.1: Multi-segment vertices emit correctly
- [ ] 2A.1: t-parameter stored at each vertex
- [ ] 2A.2: Perpendicular frame computes correctly
- [ ] 2A.3: VORTEX curvature works
- [ ] 2A.3: SPIRAL_ARM curvature works
- [ ] 2A.3: TANGENTIAL curvature works
- [ ] 2A.3: LOGARITHMIC curvature works
- [ ] 2A.3: PINWHEEL curvature works
- [ ] 2A.3: ORBITAL curvature works
- [ ] 2A.4: SINE_WAVE shape works
- [ ] 2A.4: CORKSCREW shape works
- [ ] 2A.4: SPRING shape works
- [ ] 2A.4: ZIGZAG shape works
- [ ] 2A.4: SAWTOOTH shape works
- [ ] 2A.4: SQUARE_WAVE shape works
- [ ] 2A.4: ARC shape works
- [ ] 2A.4: S_CURVE shape works
- [ ] 2A.4: TAPERED shape works
- [ ] 2A.4: DOUBLE_HELIX shape works

### Renderer (2B)
- [ ] 2B.1: Renderer flow documented
- [ ] 2B.2: t-parameter accessible in renderer
- [ ] 2B.3: Animation order defined
- [ ] 2B.4: NONE length works
- [ ] 2B.4: RADIATE works
- [ ] 2B.4: ABSORB works
- [ ] 2B.4: PULSE works
- [ ] 2B.4: SEGMENT works
- [ ] 2B.4: GROW_SHRINK works
- [ ] 2B.5: CHASE works
- [ ] 2B.5: SCROLL works
- [ ] 2B.5: COMET works
- [ ] 2B.5: SPARK works
- [ ] 2B.5: PULSE_WAVE works
- [ ] 2B.5: REVERSE_CHASE works
**NOTE:** TravelMode implementation complete in `Ray3DGeometryUtils.computeTravelAlpha()` - needs testing
- [ ] 2B.6: STROBE works
- [ ] 2B.6: SCINTILLATION works
- [ ] 2B.6: FADE_PULSE works
- [ ] 2B.6: FLICKER works
- [ ] 2B.6: LIGHTNING works
- [ ] 2B.6: HEARTBEAT works
- [ ] 2B.7: All MotionModes work
- [ ] 2B.8: All WiggleModes work
- [ ] 2B.9: All TwistModes work
- [ ] 2B.10: All ColorModes work

---

## NOTES

- Each step should be **complete** before moving to the next
- No TODOs or placeholders - implement fully or pause if unsure
- Test visually where possible
- The broken existing animations will be rebuilt properly as part of this process

