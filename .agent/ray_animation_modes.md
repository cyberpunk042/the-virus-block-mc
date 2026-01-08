# Ray System Infrastructure - Shape vs Animation Split

## CORE PRINCIPLE

Clear separation between:
1. **SHAPE** - What the ray LOOKS LIKE (static geometry, defined at creation)
2. **ANIMATION** - How the ray MOVES/CHANGES over time (dynamic, applied each frame)

---

## PART 1: SHAPE INFRASTRUCTURE (Static Geometry)

These go in **RaysShape** record - defined once when creating the primitive.

### A. Ray Line Shape (Individual Ray Geometry)
**Where:** `RaysShape.lineShape` 
**Type:** `RayLineShape` enum

| Enum | Description |
|------|-------------|
| STRAIGHT | Default straight line |
| CORKSCREW | Ray twists around its own axis (helix) |
| SPRING | Ray coils like a spring/slinky (circular helix) |
| SINE_WAVE | Ray undulates side-to-side (2D) |
| ZIGZAG | Sharp angular bends |
| SAWTOOTH | Sawtooth wave pattern |
| SQUARE_WAVE | Square wave pattern |
| ARC | Single curve (bow shape) |
| S_CURVE | S-shaped double curve |
| TAPERED | Ray visually thins toward end |
| DOUBLE_HELIX | Two intertwined corkscrews (DNA) |

### B. Ray Field Curvature (Global Arrangement)
**Where:** `RaysShape.curvature` (or part of `arrangement`)
**Type:** Could extend `RayArrangement` or new `RayCurvature` enum

| Enum | Description |
|------|-------------|
| RADIAL | Rays point straight outward (current default) |
| VORTEX | Rays curve into whirlpool (accretion disk) |
| SPIRAL_ARM | Galaxy arm pattern |
| TANGENTIAL | Rays perpendicular to radial direction |
| LOGARITHMIC | Nautilus/golden spiral curve |
| PINWHEEL | Rays curve like windmill blades |
| ORBITAL | Rays follow circular orbits around center |

### C. Ray Segments (Multi-Vertex Control)
**Where:** `RaysShape.segments`, `RaysShape.segmentGap`
**Type:** Already exists as `int segments` and `float segmentGap`

Controls how many vertices the ray has (for CORKSCREW, SPRING, etc. to work)

---

## PART 2: ANIMATION INFRASTRUCTURE (Dynamic Effects)

These go in **Animation** record - applied each frame during rendering.

### A. RayFlowConfig (Energy/Length Along Ray)
**Where:** `Animation.rayFlow`
**Controls:** How the visible/active portion changes along the ray

```java
record RayFlowConfig(
    LengthMode length,        // NONE, RADIATE, ABSORB, PULSE
    float lengthSpeed,
    
    TravelMode travel,        // NONE, CHASE, SCROLL, COMET
    float travelSpeed,
    int chaseCount,
    float chaseWidth
)
```

#### LengthMode (How ray LENGTH changes)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | Full length always | No change |
| RADIATE | Grows outward from center | End vertex moves out |
| ABSORB | Shrinks inward to center | Start vertex moves in |
| PULSE | Breathes in/out | Both vertices oscillate |
| SEGMENT | Fixed-length segment visible | Short segment on full ray |
| GROW_SHRINK | Grows then shrinks (one-shot) | Full cycle animation |

#### TravelMode (Energy traveling ALONG ray)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | Uniform brightness | No travel |
| CHASE | Particle segments move along ray | Multiple bright spots moving |
| SCROLL | Continuous gradient slides along ray | Alpha gradient slides |
| COMET | Bright head with fading tail | Bright front, fading back |
| SPARK | Random sparks shoot along ray | Occasional flash along ray |
| PULSE_WAVE | Brightness pulse(s) travel along | Wave pattern travels |
| REVERSE_CHASE | Chase but traveling inward | Particles move toward center |

### B. RayMotionConfig (Per-Ray Movement)
**Where:** `Animation.rayMotion`  
**Controls:** How each ray MOVES/OSCILLATES in space

```java
record RayMotionConfig(
    MotionMode mode,          // NONE, RADIAL_OSC, ANGULAR_OSC, etc.
    float speed,
    float amplitude,
    float frequency
)
```

#### MotionMode (Per-Ray Position Movement)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | Static position | No movement |
| RADIAL_OSCILLATE | Ray oscillates in/out radially | Sine wave radial displacement |
| RADIAL_DRIFT | Ray slowly drifts outward/inward | Constant radial velocity |
| ANGULAR_OSCILLATE | Ray sways side-to-side angularly | Sine wave angular displacement |
| ANGULAR_DRIFT | Ray slowly rotates around center | Constant angular velocity |
| FLOAT | Ray bobs up/down on Y axis | Vertical sine wave |
| SWAY | Ray tip waves, base stays fixed | Pivot/pendulum animation |
| ORBIT | Ray revolves around center axis | Continuous orbital rotation |
| JITTER | Ray position has random noise | Random small offsets |

### C. RayWiggleConfig (NEW - Ray Undulation)
**Where:** `Animation.rayWiggle` (NEW)
**Controls:** How the ray itself WIGGLES as a wave

```java
record RayWiggleConfig(
    WiggleMode mode,          // NONE, WIGGLE, WRITHE, SHIMMER
    float speed,
    float amplitude,
    float frequency
)
```

#### WiggleMode (Ray Undulation/Deformation Animations)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | No wiggle | Static shape |
| WIGGLE | Snake-like side-to-side motion | Traveling sine wave on ray |
| WOBBLE | Ray wobbles/tips back and forth | Oscillating tilt around base |
| WRITHE | 3D tentacle motion | 3D sine wave combination |
| SHIMMER | Rapid subtle vibration | High-frequency small amplitude |
| RIPPLE | Wave travels from base to tip | Position-phased wave |
| WHIP | Ray cracks like a whip (one-shot) | Whiplash motion |
| FLUTTER | Rapid chaotic motion | Random high-freq displacement |
| SNAKE | Fluid slithering motion | Multi-frequency sine blend |
| PULSE_WAVE | Thickness pulsing along ray | Varying amplitude along length |

### D. RayTwistConfig (NEW - Ray Rotation Around Own Axis)
**Where:** `Animation.rayTwist` (NEW)
**Controls:** How the ray ROTATES around its own lengthwise axis

```java
record RayTwistConfig(
    TwistMode mode,
    float speed,
    float amount         // How much twist (radians or turns)
)
```

#### TwistMode (Axial Rotation)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | No twist | Static |
| TWIST | Ray rotates around its own axis | Constant rotation |
| OSCILLATE_TWIST | Ray twists back and forth | Oscillating rotation |
| WIND_UP | Ray progressively twists more | Increasing rotation |
| UNWIND | Ray untwists over time | Decreasing rotation |
| SPIRAL_TWIST | Different parts twist at different rates | Position-based twist |

### E. FlickerConfig (Alpha/Visibility Modulation)
**Where:** Already in `RayFlowConfig` or separate
**Controls:** Overall ray visibility changes

#### FlickerMode (Alpha Modulation)
| Enum | Description | Implementation |
|------|-------------|----------------|
| NONE | Constant alpha | Full visibility |
| STROBE | All rays blink on/off in sync | Global sine wave |
| SCINTILLATE | Individual rays twinkle randomly | Per-ray random hash |
| FADE_PULSE | Ray fades in/out smoothly | Smooth alpha oscillation |
| FLICKER | Candlelight-style unstable | Random alpha noise |
| LIGHTNING | Flash bright then fade | Spike then decay |
| HEARTBEAT | Double-pulse rhythm | Two quick pulses, pause |

### F. Color Animation - GAP ANALYSIS (UPDATED)

**Existing System (in `visual/appearance/`):**
- `ColorMode` enum: GRADIENT, CYCLING, MESH_GRADIENT, MESH_RAINBOW, RANDOM, **HEAT_MAP**, **RANDOM_PULSE**, **BREATHE**, **REACTIVE**
- `ColorCycleConfig`: handles color list, speed, blend
- `Appearance` record: saturation, brightness, hueShift, timePhase

**Comparison: What We Need vs What Exists:**

| Feature | Status | Where |
|---------|--------|-------|
| COLOR_CYCLE (hue shift per ray) | ✅ EXISTS | ColorMode.CYCLING |
| GRADIENT_SCROLL (along ray) | ✅ EXISTS | ColorMode.MESH_GRADIENT + timePhase |
| MESH_RAINBOW | ✅ EXISTS | ColorMode.MESH_RAINBOW |
| RANDOM per ray | ✅ EXISTS | ColorMode.RANDOM |
| HEAT_MAP (position→color) | ✅ ADDED | ColorMode.HEAT_MAP |
| RANDOM_PULSE (animated flash) | ✅ ADDED | ColorMode.RANDOM_PULSE |
| BREATHE (saturation pulse) | ✅ ADDED | ColorMode.BREATHE |
| REACTIVE (distance-based) | ✅ ADDED | ColorMode.REACTIVE |

**Remaining Action Items:**
1. [ ] Implement rendering logic for new HEAT_MAP mode
2. [ ] Implement rendering logic for new RANDOM_PULSE mode
3. [ ] Implement rendering logic for new BREATHE mode
4. [ ] Implement rendering logic for new REACTIVE mode
5. [ ] Ensure RaysRenderer uses Appearance.colorMode correctly


---

## INFRASTRUCTURE CHANGES NEEDED

### 1. RaysShape Record (Add New Fields)

```java
public record RaysShape(
    // EXISTING
    int count,
    int layers,
    float layerSpacing,
    RayArrangement arrangement,
    RayDistribution distribution,
    float innerRadius,
    float outerRadius,
    float rayLength,
    float rayWidth,
    int segments,
    float segmentGap,
    
    // NEW: Line Shape
    RayLineShape lineShape,      // STRAIGHT, CORKSCREW, SPRING, etc.
    float lineShapeAmplitude,    // How pronounced the shape is
    float lineShapeFrequency,    // How many wiggles/coils
    
    // NEW: Field Curvature
    RayCurvature curvature,      // RADIAL, VORTEX, SPIRAL_ARM
    float curvatureIntensity     // How curved (0 = straight, 1 = full)
) implements Shape {}
```

### 2. New/Updated Enums

```java
// SHAPE: Individual ray line geometry
enum RayLineShape {
    STRAIGHT,      // Default straight line
    CORKSCREW,     // Twists around own axis (helix)
    SPRING,        // Coils like spring/slinky
    SINE_WAVE,     // Undulates side-to-side (2D)
    ZIGZAG,        // Sharp angular bends
    SAWTOOTH,      // Sawtooth wave pattern
    SQUARE_WAVE,   // Square wave pattern
    ARC,           // Single curve (bow)
    S_CURVE,       // S-shaped double curve
    TAPERED,       // Thins toward end
    DOUBLE_HELIX   // Two intertwined corkscrews
}

// SHAPE: Global field curvature
enum RayCurvature {
    NONE,          // Radial - straight outward
    VORTEX,        // Whirlpool/accretion disk
    SPIRAL_ARM,    // Galaxy arm pattern
    TANGENTIAL,    // Perpendicular to radial
    LOGARITHMIC,   // Nautilus/golden spiral
    PINWHEEL,      // Windmill blades
    ORBITAL        // Circular orbits around center
}

// ANIMATION: Length changes
enum LengthMode {
    NONE,          // Full length always
    RADIATE,       // Grows outward from center
    ABSORB,        // Shrinks inward to center
    PULSE,         // Breathes in/out
    SEGMENT,       // Fixed-length segment
    GROW_SHRINK    // Grows then shrinks
}

// ANIMATION: Travel along ray
enum TravelMode {
    NONE,          // Uniform brightness
    CHASE,         // Particle segments moving
    SCROLL,        // Gradient slides
    COMET,         // Head with fading tail
    SPARK,         // Random sparks
    PULSE_WAVE,    // Brightness pulses travel
    REVERSE_CHASE  // Particles move inward
}

// ANIMATION: Per-ray position movement
enum MotionMode {
    NONE,              // Static
    RADIAL_OSCILLATE,  // In/out oscillation
    RADIAL_DRIFT,      // Slow outward/inward drift
    ANGULAR_OSCILLATE, // Side-to-side sway
    ANGULAR_DRIFT,     // Slow rotation around center
    FLOAT,             // Up/down bobbing
    SWAY,              // Tip waves, base fixed
    ORBIT,             // Revolves around center
    JITTER             // Random noise
}

// ANIMATION: Ray undulation/deformation
enum WiggleMode {
    NONE,        // Static shape
    WIGGLE,      // Snake side-to-side
    WOBBLE,      // Tips back and forth
    WRITHE,      // 3D tentacle motion
    SHIMMER,     // Rapid vibration
    RIPPLE,      // Wave base to tip
    WHIP,        // Whiplash motion
    FLUTTER,     // Chaotic motion
    SNAKE,       // Fluid slithering
    PULSE_WAVE   // Thickness pulsing
}

// ANIMATION: Axial twist
enum TwistMode {
    NONE,           // No twist
    TWIST,          // Constant rotation
    OSCILLATE_TWIST,// Back and forth
    WIND_UP,        // Increasing rotation
    UNWIND,         // Decreasing rotation
    SPIRAL_TWIST    // Position-based twist
}

// ANIMATION: Alpha modulation
enum FlickerMode {
    NONE,        // Constant alpha
    STROBE,      // Sync blink
    SCINTILLATE, // Random twinkle
    FADE_PULSE,  // Smooth fade
    FLICKER,     // Candlelight
    LIGHTNING,   // Flash then fade
    HEARTBEAT    // Double-pulse
}

// COLOR: Use existing ColorMode + ColorCycleConfig (NO NEW ENUM)
// ColorMode: GRADIENT, CYCLING, MESH_GRADIENT, MESH_RAINBOW, RANDOM
// ColorCycleConfig: colors list, speed, blend
```

### 3. Animation Records Needed

- `RayFlowConfig` - Already exists (LengthMode, TravelMode, FlickerMode)
- `RayMotionConfig` - Already exists, update MotionMode enum
- `RayWiggleConfig` - NEW, add WiggleMode
- `RayTwistConfig` - NEW, add TwistMode
- `ColorAnimConfig` - Extend existing ColorCycleConfig

---

## SUMMARY: What Goes Where

| Feature | Type | Location | Enum/Config |
|---------|------|----------|-------------|
| Ray is corkscrew/spring/wavy | Shape | RaysShape.lineShape | RayLineShape |
| Rays curve into vortex/spiral | Shape | RaysShape.curvature | RayCurvature |
| Ray count, length, width | Shape | RaysShape existing | (fields) |
| Ray grows outward (RADIATE) | Animation | RayFlowConfig.length | LengthMode |
| Ray shrinks inward (ABSORB) | Animation | RayFlowConfig.length | LengthMode |
| Particles travel along ray | Animation | RayFlowConfig.travel | TravelMode |
| Ray oscillates in/out | Animation | RayMotionConfig.mode | MotionMode |
| Ray sways side-to-side | Animation | RayMotionConfig.mode | MotionMode |
| Ray wiggles like snake | Animation | RayWiggleConfig.mode | WiggleMode |
| Ray twists around axis | Animation | RayTwistConfig.mode | TwistMode |
| Ray flickers/strobe | Animation | FlickerMode | FlickerMode |
| Ray color cycles/changes | Animation | ColorAnimConfig | ColorAnimMode |

---

## IMPLEMENTATION STATUS (Updated Dec 25, 2024)

### ✅ Phase 1: Infrastructure (COMPLETED)

1. [x] **Create new enums:** RayLineShape, RayCurvature, WiggleMode, TwistMode
2. [x] **Update existing enums:** LengthMode (+2), TravelMode (+4), FlickerMode (+4), MotionMode (+PRECESS, restructured), ColorMode (+4)
3. [x] **Create new config records:** RayWiggleConfig, RayTwistConfig
4. [x] **Update RaysShape record:** Added lineShape, lineShapeAmplitude, lineShapeFrequency, shapeSegments, curvature, curvatureIntensity
5. [x] **Update Animation record:** Added rayWiggle, rayTwist fields
6. [x] **Update DefinitionBuilder:** Added rayWiggle, rayTwist to buildAnimation()
7. [x] **Update AnimationAdapter:** Full path routing for rayWiggle, rayTwist
8. [x] **Update FieldEditState:** Path mappings + delegate methods for rayWiggle, rayTwist
9. [x] **Delete redundant ColorAnimMode enum:** Confirmed removed

### 🔄 Phase 2: Core Implementation (PENDING)

1. [ ] Update RaysTessellator to generate geometry for new line shapes (CORKSCREW, SPRING, etc.)
2. [ ] Update RaysTessellator to apply field curvature (VORTEX, SPIRAL_ARM, etc.)
3. [ ] Implement animation logic in RaysRenderer for:
   - [ ] New MotionMode values (PRECESS, RADIAL_OSCILLATE, etc.)
   - [ ] RayWiggleConfig (WIGGLE, WOBBLE, SNAKE, etc.)
   - [ ] RayTwistConfig (TWIST, OSCILLATE_TWIST, etc.)
   - [ ] New ColorMode values (HEAT_MAP, REACTIVE, etc.)
4. [ ] Fix line width regression

### 🔄 Phase 3: GUI Integration (PENDING)

1. [ ] Update ShapeSubPanel with RayLineShape dropdown
2. [ ] Update ShapeSubPanel with RayCurvature dropdown + intensity slider
3. [ ] Update ModifiersSubPanel with RayWiggle controls
4. [ ] Update ModifiersSubPanel with RayTwist controls
5. [ ] Update FillSubPanel with new ColorMode options

---

## DECISIONS & ANSWERS

### Q1: Should RayCurvature be part of RayArrangement or separate?
**DECISION:** MERGE - Add curvature values to RayArrangement enum.
```java
enum RayArrangement {
    // Existing
    RADIAL, PARALLEL, SPHERICAL, ...
    // Add curvature variants
    VORTEX, SPIRAL_ARM, TANGENTIAL, LOGARITHMIC, PINWHEEL, ORBITAL
}
```
Or use `curvatureIntensity` slider with RayArrangement to blend (0 = radial, 1 = full curvature).

### Q2: How many segments for CORKSCREW/SPRING?
**DECISION:** Configurable, default 32-128 based on ray length.
- Add `RaysShape.shapeSegments` field (separate from `segments` for dashed lines)
- For CORKSCREW/SPRING with length=1.0: ~32 segments
- For longer rays or tighter coils: up to 128

### Q3: WiggleConfig separate or part of RayMotionConfig?
**DECISION:** Keep separate because they STACK.
- **Motion** = WHERE the ray is positioned (oscillate, drift, orbit)
- **Wiggle** = HOW the ray itself undulates (snake, shimmer)
- These COMBINE: ray can oscillate radially WHILE wiggling like snake
- Separate configs allows both to be active simultaneously

---

## CLARIFICATION: WOBBLE at Two Levels

There are TWO WOBBLE animations:

| Animation | Level | Where | Description |
|-----------|-------|-------|-------------|
| **Field WOBBLE** | Field | Animation.wobble | Entire field wobbles/rotates together |
| **Ray WOBBLE** | Ray | WiggleMode.WOBBLE | Individual ray tips back and forth |

These are DIFFERENT and BOTH SHOULD EXIST:
- **Field WOBBLE** (existing) = the whole primitive tilts as one unit
- **Ray WOBBLE** (NEW) = each individual ray tips/wobbles independently

They can STACK: Field wobbles while individual rays also wobble!

---

## STACKING MATRIX (What Can Combine)

All ray animations should be able to STACK/COMBINE:

| Animation A | Animation B | Can Stack? | Notes |
|-------------|-------------|------------|-------|
| RADIATE | + CHASE | ✅ YES | Growing ray with particles |
| RADIATE | + SPIRAL curvature | ✅ YES | Rays grow along curved paths |
| Motion (OSCILLATE) | + Wiggle (SNAKE) | ✅ YES | Ray sways while wiggling |
| Motion (DRIFT) | + Twist (SPIN) | ✅ YES | Drifting, spinning ray |
| Wiggle | + Twist | ✅ YES | Wiggling, twisting ray |
| Flicker | + Any above | ✅ YES | All above but also flickering |

**Key principle:** Each animation config affects a DIFFERENT aspect of the ray, so they don't conflict:
- **LengthMode** → vertex positions (start/end)
- **TravelMode** → alpha along ray
- **MotionMode** → vertex positions (translate)
- **WiggleMode** → vertex positions (perpendicular displacement)
- **TwistMode** → vertex positions (rotation around axis)
- **FlickerMode** → alpha (full ray)
- **ColorMode** → color values

---

## INFRASTRUCTURE LOCATIONS

Based on your guidance:

| Category | Goes In | Panel |
|----------|---------|-------|
| RayLineShape | RaysShape (Shape) | ShapeSubPanel |
| RayCurvature | RaysShape or RayArrangement | ShapeSubPanel |
| Segments | RaysShape | ShapeSubPanel |
| ColorAnimMode | Appearance/Fill | FillSubPanel/AppearancePanel |
| LengthMode | Animation | ModifiersSubPanel |
| TravelMode | Animation | ModifiersSubPanel |
| MotionMode | Animation | ModifiersSubPanel |
| WiggleMode | Animation | ModifiersSubPanel |
| TwistMode | Animation | ModifiersSubPanel |
| FlickerMode | Animation | ModifiersSubPanel |

---

## WHAT'S STILL MISSING?

1. [ ] **WOBBLE** - Already exists at field level, just clarifying relationship
2. [ ] **RIPPLE** - Is this Motion or Wiggle? (radial wave vs ray undulation)
3. [ ] Other existing animations: SPIN, PULSE, PRECESSION, WAVE - how do they interact with ray-specific animations?
