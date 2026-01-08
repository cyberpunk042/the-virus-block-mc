# Ray System Refactoring Plan v2

## Goals
1. All files ≤ 400 lines
2. Single Responsibility Principle
3. Clear OOP abstractions per layer
4. No naming conflicts between layers
5. **NEW: Stage/Phase model for previewable shapes**

---

# PART 0: FOUNDATIONAL ARCHITECTURE - STAGE/PHASE MODEL

## The Problem (Why Animation Breaks Shapes)

**Current:** Animation computes derived values (clipStart, clipEnd, scale, alpha) directly.
- User can't preview what the shape looks like at any point in the animation
- Animation bugs are hard to debug because you can't isolate the problem
- Different animations compute the same things differently

**Solution:** Shapes own their own state. Animation just updates that state.

---

## The Stage/Phase Model

Every shape that can be animated has:

```java
public record ShapeState(
    ShapeStage stage,           // What "mode" the shape is in
    float phase,                // 0-1 progress within the current stage
    EdgeTransitionMode edgeMode // How edges appear (CLIP, SCALE, FADE)
) {
    public static final ShapeState DORMANT = new ShapeState(ShapeStage.DORMANT, 0, EdgeTransitionMode.CLIP);
    public static final ShapeState FULL = new ShapeState(ShapeStage.ACTIVE, 0, EdgeTransitionMode.CLIP);
}
```

### ShapeStage Enum (shared by all animatable shapes)

```java
public enum ShapeStage {
    DORMANT,      // At rest - invisible or minimal
    SPAWNING,     // Transitioning into active (phase 0→1 = appearing)
    ACTIVE,       // Fully active (phase can drive sub-animations like chase/scroll)
    DESPAWNING    // Transitioning out (phase 0→1 = disappearing)
}
```

### EdgeTransitionMode (how edges look during spawn/despawn)

```java
public enum EdgeTransitionMode {
    CLIP,   // Geometric clipping at boundaries
    SCALE,  // Width shrinks near edges
    FADE    // Alpha fades near edges
}
```

---

## How It Works

### In UI (Manual Preview)

```
┌─────────────────────────────────┐
│  Stage: [SPAWNING ▼]            │  ← User picks stage
│  Phase: [====●====] 0.50        │  ← User drags slider
│  Edge Mode: [CLIP ▼]            │  ← User picks edge mode
└─────────────────────────────────┘
                │
                ▼
        Shape renders at 50% spawn with CLIP effect
        User can SEE exactly what it looks like!
```

### With Animation

```
Animation Config (RayFlowConfig)
          │
          │  Computes: what stage + phase at time T?
          ▼
    ShapeState { stage=SPAWNING, phase=0.5, edgeMode=CLIP }
          │
          │  Shape already knows how to render this!
          ▼
    Tessellator reads stage + phase + edgeMode
          │
          ▼
    Correct visual output
```

---

## For Rays: Stage/Phase Interpretation

| Stage | Phase 0 | Phase 1 | What It Looks Like |
|-------|---------|---------|-------------------|
| DORMANT | - | - | No ray visible |
| SPAWNING | Just starting | Fully visible | Ray grows/fades in |
| ACTIVE | Start of path | End of path | Ray at position along path |
| DESPAWNING | Fully visible | Gone | Ray shrinks/fades out |

### Combined with EdgeMode

| EdgeMode | SPAWNING phase=0.5 looks like |
|----------|-------------------------------|
| CLIP | Ray is clipped to 50% of its length |
| SCALE | Ray is full length but 50% width |
| FADE | Ray is full length but 50% alpha |

---

## What This Changes

### EdgeMode Moves to Shape

**Before:** EdgeTransitionMode was in RayFlowConfig (animation).
**After:** EdgeTransitionMode is in ShapeState (shape).

This makes sense because edgeMode affects HOW the shape looks, not WHEN it animates.

### FlowPipeline Output Changes

**Before:** FlowPipeline computed clipStart, clipEnd, widthScale, opacity.
**After:** FlowPipeline computes stage + phase. Shape interprets these.

```java
// BEFORE (complex)
FlowState {
    float clipStart;
    float clipEnd; 
    float widthScale;
    float opacity;
}

// AFTER (simple)
ShapeState {
    ShapeStage stage;
    float phase;
    EdgeTransitionMode edgeMode;
}
```

### Other Shapes Can Use This

SphereShape, ConeShape, etc. can all have stage + phase:
- DORMANT → SPAWNING → ACTIVE → DESPAWNING
- phase = 0-1 progress
- Each shape interprets what that means for its geometry

---

## Benefits

1. **Preview without animation** - Drag phase slider, see result immediately
2. **EdgeMode belongs to shape** - Not buried in animation config
3. **Animation can't break shape** - Animation just outputs stage + phase
4. **Simple debugging** - If it looks wrong at phase=0.5, fix the shape, not animation
5. **Reusable across shapes** - Stage/Phase pattern works for all shapes

---

# PART 0.5: ENERGY INTERACTION MODEL

## The Energy Interaction Hierarchy

```
Energy Interaction (top-level UI control)
└── Radiative Interaction
    ├── EMISSION      (energy radiates outward)
    ├── ABSORPTION    (energy flows inward)
    ├── REFLECTION    (energy bounces back)
    ├── TRANSMISSION  (energy passes through)
    ├── SCATTERING    (energy disperses)
    ├── OSCILLATION   (energy pulses in/out)
    └── RESONANCE     (energy grows then decays)
└── (Future: Thermal, Kinetic, etc.)
```

## Package: `net.cyberpunk042.visual.energy`

```java
// Top-level categorization
public enum EnergyInteractionType {
    NONE,
    RADIATIVE,
    // Future: THERMAL, KINETIC, ELECTROMAGNETIC
}

// Radiative modes (replaces LengthMode)
public enum RadiativeInteraction {
    NONE,           // Full shape visible
    EMISSION,       // Energy radiates outward (was RADIATE)
    ABSORPTION,     // Energy flows inward (was ABSORB)
    REFLECTION,     // Energy bounces back (NEW)
    TRANSMISSION,   // Energy passes through (was SEGMENT)
    SCATTERING,     // Energy disperses (NEW)
    OSCILLATION,    // Energy pulses (was PULSE)
    RESONANCE       // Energy grows then decays (was GROW_SHRINK)
}

// Renamed for consistency
public enum EnergyTravel {    // was TravelMode
    NONE, CHASE, SCROLL, COMET, SPARK, PULSE_WAVE, REVERSE_CHASE
}

public enum EnergyFlicker {   // was FlickerMode
    NONE, SCINTILLATION, STROBE, FADE_PULSE, FLICKER, LIGHTNING, HEARTBEAT
}
```

---

## Key Architectural Change: RadiativeInteraction Lives in SHAPE

### Why?

`RadiativeInteraction` defines HOW the shape looks at a given phase. This is a SHAPE property, not an animation property.

**The shape says:** "At phase X, THIS is what I look like based on my RadiativeInteraction mode."
**Animation says:** "At time T, phase should be X."

### Data Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                           RaysShape                               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ radiativeInteraction: EMISSION                               │ │
│  │ segmentLength: 0.3                                           │ │
│  │ waveArc: 1.0                                                 │ │
│  │ waveDistribution: SEQUENTIAL                                 │ │
│  │ waveCount: 2.0                                               │ │
│  │ shapeState: { stage: ACTIVE, phase: 0.5, edgeMode: CLIP }   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                                │
                                │  Shape interprets phase using RadiativeInteraction
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                    At phase=0.5 with EMISSION:                    │
│    Visible segment is at 50% of the way from inner to outer      │
│    (segment moves outward as phase increases)                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## How RadiativeInteraction Uses Phase

| RadiativeInteraction | Phase 0 | Phase 0.5 | Phase 1 |
|---------------------|---------|-----------|---------|
| NONE | Full visible | Full visible | Full visible |
| EMISSION | Segment at inner edge | Segment at middle | Segment at outer edge |
| ABSORPTION | Segment at outer edge | Segment at middle | Segment at inner edge |
| TRANSMISSION | Segment at t=0 | Segment at t=0.5 | Segment at t=1 |
| OSCILLATION | Contracted | Half-expanded | Fully expanded |
| RESONANCE | Nothing | Fully expanded | Nothing |
| REFLECTION | (TBD) | | |
| SCATTERING | (TBD) | | |

---

## RayFlowConfig Changes

### Fields MOVED to RaysShape

| Field | From | To |
|-------|------|-----|
| `LengthMode length` | RayFlowConfig | RaysShape.radiativeInteraction |
| `float segmentLength` | RayFlowConfig | RaysShape.segmentLength |
| `float waveArc` | RayFlowConfig | RaysShape.waveArc |
| `WaveDistribution waveDistribution` | RayFlowConfig | RaysShape.waveDistribution |
| `float waveCount` | RayFlowConfig | RaysShape.waveCount |

### Fields REMAINING in RayFlowConfig

RayFlowConfig now only controls **enable/speed** for animation:

```java
public record RayFlowConfig(
    // === Energy Animation Controls ===
    boolean radiativeEnabled,     // Enable/disable radiative animation
    float radiativeSpeed,         // Speed of phase change
    
    // === Travel Animation (EnergyTravel) ===
    EnergyTravel travel,          // was TravelMode
    boolean travelEnabled,
    float travelSpeed,
    int chaseCount,
    float chaseWidth,
    
    // === Flicker Animation (EnergyFlicker) ===
    EnergyFlicker flicker,        // was FlickerMode
    boolean flickerEnabled,
    float flickerIntensity,
    float flickerFrequency,
    
    // === Spawn Transition ===
    boolean skipSpawnTransition,  // was startFullLength
    boolean pathFollowing         // was followCurve
) {}
```

---

## How Animation Drives Phase

```
┌─────────────────────────────────────────────────────────────────┐
│                       RayFlowConfig                              │
│  radiativeEnabled: true                                          │
│  radiativeSpeed: 1.5                                             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │  FlowPhaseStage computes phase from time + speed
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  phase = (time * radiativeSpeed) % 1.0                           │
│  (possibly modified by waveDistribution from shape)              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │  Phase is written to ShapeState
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  ShapeState { stage: ACTIVE, phase: 0.73, edgeMode: CLIP }       │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │  Shape interprets phase using its RadiativeInteraction
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  RaysShape.radiativeInteraction == EMISSION                      │
│  → Visible segment is at 73% of travel from inner to outer       │
└─────────────────────────────────────────────────────────────────┘
```

---

## TessEdgeModeFactory: Simplified Role

TessEdgeModeFactory ONLY handles EdgeTransitionMode (CLIP/SCALE/FADE):
- Takes: phase, EdgeTransitionMode
- Returns: clipStart, clipEnd, scale, alpha

It does NOT know about RadiativeInteraction. The interpretation of phase for radiative effects happens in the renderer based on the shape's `radiativeInteraction` field.

```java
// TessEdgeModeFactory - simple, focused
public static TessEdgeResult compute(ShapeStage stage, float phase, EdgeTransitionMode mode) {
    // ONLY handles stage transitions (SPAWNING/DESPAWNING)
    // For ACTIVE stage, returns FULL visibility
    // RadiativeInteraction is handled separately
}
```

---

## Benefits of This Architecture

1. **Separation of concerns:**
   - Shape defines WHAT it looks like at any phase
   - Animation defines HOW phase changes over time
   
2. **Easy to preview:**
   - Set phase slider to 0.5, see exactly what EMISSION looks like at 50%
   - No animation needed to test appearance

3. **Clear data ownership:**
   - RadiativeInteraction = SHAPE property
   - Speed/timing = ANIMATION property

4. **Future extensibility:**
   - Add new EnergyInteractionTypes (THERMAL, KINETIC) easily
   - Each can have its own interpretation of phase

---


# PART 1: CURRENT STATE

## Files Needing Refactor

| File | Lines | Issue |
|------|-------|-------|
| RayPositioner.java | 1226 | Monolithic, mixed concerns |
| RaysRenderer.java | 1527 | Monolithic, mixed concerns |
| Ray3DGeometryUtils.java | 577 | Too large, mixed concerns |
| RayLineTessellator.java | 478 | Edge mode duplication |

## Files OK As-Is

| File | Lines | Notes |
|------|-------|-------|
| RaysTessellator.java | 157 | Clean orchestrator |
| RayDropletTessellator.java | 192 | Single responsibility |
| RayGeometryUtils.java | 369 | Pure utilities |
| RayContext.java | 384 | Data + builder |
| RayTypeTessellator.java | ~30 | Interface |
| RayTypeTessellatorRegistry.java | ~50 | Factory |

---

# PART 2: NAMING CONVENTIONS

## Layer Prefixes

| Layer | Prefix | Example |
|-------|--------|---------|
| Computation (RayPositioner) | `Flow*` | `FlowStage`, `FlowPhaseStage` |
| Tessellation | `Tess*` | `TessEdgeMode`, `TessClipEdge` |
| Geometry/Path | `Geo*` | `GeoPath`, `GeoCurvaturePath` |
| Rendering | `Render*` | `RenderVertexEffect`, `RenderMotionEffect` |
| Emission | `Emit*` | `EmitStrategy`, `EmitLineStrategy` |

## Config Field Renames

| Current | New | Why |
|---------|-----|-----|
| `segmentLength` | `visibleRatio` | Not related to dashed segments |
| `startFullLength` | `skipSpawnTransition` | Clearer: skip = no spawn animation |
| `followCurve` | `pathFollowing` | Animation follows the geometric path |
| `waveArc` | `sweepArc` | Confused with "wave" deformation |
| `waveDistribution` | `sweepDistribution` | Consistent with sweepArc |
| `waveCount` | `sweepCount` | Consistent |

## RayContext Field Renames

| Current | New | Why |
|---------|-----|-----|
| `t` | `angularPosition` | "t" too generic, confused with parametric t |
| `flowPositionOffset` | `flowTranslation` | It's a translation offset |
| `travelRange` | `fieldSpan` | It's outerRadius - innerRadius |
| `visibleTStart` | `clipStart` | Clearer, no "T" confusion |
| `visibleTEnd` | `clipEnd` | Consistent |
| `flowScale` | `spawnScale` | From spawn transition |
| `flowAlpha` | `compositeAlpha` | Combined opacity from multiple sources |
| `shapeSegments` | `curveResolution` | Not dashed segments, it's resolution |

## RayPositioner Internal Renames

| Current | New | Why |
|---------|-----|-----|
| `FlowAnimationResult` | `FlowComputeResult` | Specific to computation phase |
| `positionOffset` | `translation` | Clearer |
| `scale` | `spawnScale` | From spawn transition |
| `visibleTStart` | `clipStart` | Consistent |
| `visibleTEnd` | `clipEnd` | Consistent |

## RayLineTessellator Renames

| Current | New | Why |
|---------|-----|-----|
| `segments` (parameter) | `dashCount` | Number of dashes, not shapeSegments |
| `segmentGap` | `dashGap` | Gap between dashes |

## RaysRenderer Renames

| Current | New | Why |
|---------|-----|-----|
| `calculateLengthPhase` | `computeFlowPhase` | "Length" is mode name, not action |
| `calculateTravelOffset` | `computeTravelOffset` | Consistent verb |
| `calculateTravelAlpha` | `computeTravelAlpha` | Consistent |
| `calculateFlickerAlpha` | `computeFlickerAlpha` | Consistent |


# PART 3: OOP ABSTRACTIONS BY LAYER

## 3.1 COMPUTATION LAYER (FlowPipeline)

**Updated for Stage/Phase model:** FlowPipeline now outputs ShapeStage + phase, not derived values.

### FlowStage (Pipeline Pattern)

```java
public interface FlowStage {
    AnimationState process(AnimationState state, FlowContext ctx);
    default boolean shouldRun(FlowContext ctx) { return true; }
}
```

### AnimationState (Simplified - outputs to ShapeState)

```java
public record AnimationState(
    ShapeStage stage,    // What stage the shape should be in
    float phase,         // 0-1 progress within that stage
    float flickerAlpha   // Flicker overlay (separate from stage/phase)
) {
    public static final AnimationState DORMANT = new AnimationState(ShapeStage.DORMANT, 0, 1);
    public static final AnimationState FULL = new AnimationState(ShapeStage.ACTIVE, 0, 1);
    
    public AnimationState withStage(ShapeStage s) { return new AnimationState(s, phase, flickerAlpha); }
    public AnimationState withPhase(float p) { return new AnimationState(stage, p, flickerAlpha); }
    public AnimationState withFlickerAlpha(float a) { return new AnimationState(stage, phase, a); }
}
```

### FlowContext (Immutable Record)

```java
public record FlowContext(
    RayFlowConfig config,
    int rayIndex,
    int rayCount,
    float time,
    float innerRadius,
    float outerRadius
) {}
```

### Flow Stages (Updated)

| Class | Responsibility | Lines | Output |
|-------|---------------|-------|--------|
| `FlowPhaseStage` | Compute base phase from time + wave distribution | ~80 | phase |
| `FlowTravelStage` | Modify phase for chase/scroll effects | ~80 | phase (within ACTIVE) |
| `FlowFlickerStage` | Compute flicker alpha overlay | ~80 | flickerAlpha |

### FlowPipeline (Orchestrator)

```java
public final class FlowPipeline {
    private static final List<FlowStage> STAGES = List.of(
        new FlowPhaseStage(),      // Compute base phase
        new FlowTravelStage(),     // Modify phase for travel effects
        new FlowFlickerStage()     // Add flicker overlay
    );
    
    public static AnimationState compute(FlowContext ctx) {
        AnimationState state = AnimationState.DORMANT;
        for (FlowStage stage : STAGES) {
            if (stage.shouldRun(ctx)) {
                state = stage.process(state, ctx);
            }
        }
        return state;
    }
}
```

### How Shape Interprets AnimationState + RadiativeInteraction

```java
// In RaysShape or RadiativeInteractionFactory
public ClipRange computeClipRange(
        AnimationState anim, 
        EdgeTransitionMode edgeMode,
        RadiativeInteraction radiative,
        float segmentLength) {
    
    return switch(anim.stage()) {
        case DORMANT -> new ClipRange(0, 0, 0);  // Nothing visible
        case SPAWNING -> applySpawnClip(anim.phase(), edgeMode);
        case ACTIVE -> applyRadiativeClip(anim.phase(), radiative, segmentLength);
        case DESPAWNING -> applyDespawnClip(anim.phase(), edgeMode);
    };
}

// NEW: RadiativeInteraction interpretation during ACTIVE stage
private ClipRange applyRadiativeClip(float phase, RadiativeInteraction radiative, float segmentLength) {
    float windowStart, windowEnd;
    
    return switch(radiative) {
        case NONE -> new ClipRange(0, 1, 1);  // Full visible
        
        case EMISSION -> {
            // Segment moves outward: phase 0=inner, phase 1=outer
            float travelRange = 1.0f + segmentLength;
            float center = phase * travelRange;
            yield new ClipRange(center - segmentLength/2, center + segmentLength/2, 1);
        }
        
        case ABSORPTION -> {
            // Segment moves inward: phase 0=outer, phase 1=inner
            float travelRange = 1.0f + segmentLength;
            float center = 1.0f - (phase * travelRange);
            yield new ClipRange(center - segmentLength/2, center + segmentLength/2, 1);
        }
        
        case TRANSMISSION -> {
            // Segment slides along ray
            yield new ClipRange(phase - segmentLength/2, phase + segmentLength/2, 1);
        }
        
        case OSCILLATION -> {
            // Whole ray pulses: phase 0=contracted, phase 1=expanded
            float scale = 0.5f + 0.5f * (float)Math.sin(phase * Math.PI);
            yield new ClipRange(0, 1, scale);  // Uses scale, not clip
        }
        
        case RESONANCE -> {
            // Grows then shrinks
            float scale = phase < 0.5f ? phase * 2 : 2 - phase * 2;
            yield new ClipRange(0, scale, 1);
        }
        
        case REFLECTION, SCATTERING -> new ClipRange(0, 1, 1);  // TBD
    };
}

private ClipRange applySpawnClip(float phase, EdgeTransitionMode edgeMode) {
    return switch(edgeMode) {
        case CLIP -> new ClipRange(0, phase, 1);         // Geometric clip
        case SCALE -> new ClipRange(0, 1, phase);        // Width scale
        case FADE -> new ClipRange(0, 1, phase);         // Alpha fade
    };
}
```

---

## 3.2 MISSING ABSTRACTIONS (FROM UI REVIEW)

### DistributionStrategy (Missing!)

```java
public interface DistributionStrategy {
    DistributionResult compute(int rayIndex, int rayCount, Random rng);
}

public record DistributionResult(float angleOffset, float lengthVariation) {}
```

| Class | Responsibility |
|-------|---------------|
| `UniformDistribution` | No offset - even spacing |
| `RandomDistribution` | Per-compute random offset |
| `StochasticDistribution` | Seeded consistent randomness |

### LineShapeStrategy (Missing!)

```java
public interface LineShapeStrategy {
    float[] computeOffset(float t, float amplitude, float frequency, float phase);
}
```

| Class | Responsibility |
|-------|---------------|
| `StraightLineShape` | No offset (identity) |
| `WavyLineShape` | Sine wave perpendicular offset |
| `HelixLineShape` | Spiral around ray axis |
| `CorkscrewLineShape` | Corkscrew pattern |
| `DoubleHelixLineShape` | Two intertwined helices |

### CurvatureStrategy (Missing!)

```java
public interface CurvatureStrategy {
    float[] applyCurvature(float[] position, float t, float intensity, float[] center);
}
```

| Class | Responsibility |
|-------|---------------|
| `NoCurvature` | Identity - no change |
| `VortexCurvature` | Curve around center axis |
| `SpiralCurvature` | Spiral outward/inward |
| `GravitationalCurvature` | Bend toward/away from center |

### LayerModeStrategy (Missing!)

```java
public interface LayerModeStrategy {
    float computeLayerOffset(int layerIndex, float layerSpacing);
}
```

| Class | Responsibility |
|-------|---------------|
| `VerticalLayerMode` | Stack layers on Y axis |
| `HorizontalLayerMode` | Spread layers on XZ plane |

### RadiativeInteractionFactory (NEW!)

Computes clip range based on RadiativeInteraction mode and phase:

```java
public final class RadiativeInteractionFactory {
    
    public static ClipRange compute(
            RadiativeInteraction mode,
            float phase,
            float segmentLength) {
        
        return switch(mode) {
            case NONE -> ClipRange.FULL;
            case EMISSION -> computeEmission(phase, segmentLength);
            case ABSORPTION -> computeAbsorption(phase, segmentLength);
            case TRANSMISSION -> computeTransmission(phase, segmentLength);
            case OSCILLATION -> computeOscillation(phase);
            case RESONANCE -> computeResonance(phase);
            case REFLECTION, SCATTERING -> ClipRange.FULL;  // TBD
        };
    }
}
```

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `RadiativeInteractionFactory` | Compute clip range from RadiativeInteraction + phase | ~100 |

**Key point:** This factory reads RadiativeInteraction from SHAPE (not animation config).

---

## 3.3 ARRANGEMENT LAYER (Strategy Pattern)

### ArrangementStrategy

```java
public interface ArrangementStrategy {
    void compute(RaysShape shape, int index, int count,
                int layerIndex, float layerSpacing,
                DistributionResult dist,
                float[] outStart, float[] outEnd);
}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `RadialArrangement` | 2D star pattern on XZ plane | ~80 |
| `SphericalArrangement` | 3D uniform distribution | ~100 |
| `ParallelArrangement` | Grid of parallel rays | ~60 |
| `ConvergingArrangement` | Rays pointing toward center | ~80 |
| `DivergingArrangement` | Rays pointing away from center | ~80 |

---

## 3.4 GEOMETRY LAYER (Path Abstraction)

### GeoPath

```java
public interface GeoPath {
    float[] positionAt(float t);
    float[] tangentAt(float t);
    float length();
}
```

### GeoPath Composition

GeoPath composes **CurvatureStrategy** and **LineShapeStrategy**:

```java
public class ComposedGeoPath implements GeoPath {
    private final float[] start, end;
    private final CurvatureStrategy curvature;
    private final LineShapeStrategy lineShape;
    
    public float[] positionAt(float t) {
        // 1. Interpolate base position
        float[] pos = interpolate(start, end, t);
        // 2. Apply curvature
        pos = curvature.apply(pos, t);
        // 3. Apply line shape offset
        float[] offset = lineShape.computeOffset(t);
        return add(pos, offset);
    }
}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `GeoLinearPath` | Straight line interpolation | ~60 |
| `ComposedGeoPath` | Curvature + LineShape composition | ~100 |

---

## 3.5 TESSELLATION LAYER

**Updated for Stage/Phase model:** Tessellator reads ShapeState (stage + phase + edgeMode) from RayContext.

### TessEdgeMode (Now Selected from ShapeState)

```java
// EdgeTransitionMode is now in ShapeState, not in animation config!
// Tessellator selects the right strategy based on ShapeState.edgeMode

public interface TessEdgeMode {
    TessEdgeResult apply(float phase, EdgeTransitionMode mode);
}

public record TessEdgeResult(
    float clipStart,   // 0-1 visible start
    float clipEnd,     // 0-1 visible end
    float widthScale,  // Width multiplier
    float alpha        // Opacity multiplier
) {}
```

### TessEdgeModeFactory

```java
public static TessEdgeResult compute(ShapeStage stage, float phase, EdgeTransitionMode mode) {
    if (stage == ShapeStage.DORMANT) {
        return new TessEdgeResult(0, 0, 0, 0);  // Nothing visible
    }
    if (stage == ShapeStage.ACTIVE) {
        return new TessEdgeResult(0, 1, 1, 1);  // Full visibility
    }
    
    // SPAWNING or DESPAWNING
    float p = (stage == ShapeStage.DESPAWNING) ? (1 - phase) : phase;
    
    return switch(mode) {
        case CLIP -> new TessEdgeResult(0, p, 1, 1);
        case SCALE -> new TessEdgeResult(0, 1, p, 1);
        case FADE -> new TessEdgeResult(0, 1, 1, p);
    };
}
```

### TessSegmentStrategy (unchanged)

```java
public interface TessSegmentStrategy {
    List<TessSegment> generate(float tStart, float tEnd, RayContext ctx);
}

public record TessSegment(float tStart, float tEnd) {}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `TessSimpleSegment` | Single segment [tStart, tEnd] | ~20 |
| `TessDashedSegment` | Multiple segments with gaps | ~60 |
| `TessShapedSegment` | Multi-segment for curves | ~40 |

---

## 3.6 RENDERING LAYER


### RenderVertexEffect (Decorator Pattern)

```java
public interface RenderVertexEffect {
    float[] apply(float[] position, RenderEffectContext ctx);
}

public record RenderEffectContext(
    float[] direction,
    float t,
    float time,
    int rayIndex
) {}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `RenderMotionEffect` | Orbit, drift, float, sway, jitter | ~180 |
| `RenderWiggleEffect` | Snake, writhe, shimmer | ~150 |
| `RenderTwistEffect` | Rotation around ray axis | ~100 |

### RenderEffectChain (Composite Pattern)

```java
public final class RenderEffectChain {
    private final List<RenderVertexEffect> effects;
    
    public float[] apply(float[] position, RenderEffectContext ctx) {
        float[] pos = position.clone();
        for (RenderVertexEffect effect : effects) {
            pos = effect.apply(pos, ctx);
        }
        return pos;
    }
    
    public static RenderEffectChain create(
            RayMotionConfig motion,
            RayWiggleConfig wiggle,
            RayTwistConfig twist) {
        List<RenderVertexEffect> effects = new ArrayList<>();
        if (motion != null && motion.isActive()) effects.add(new RenderMotionEffect(motion));
        if (wiggle != null && wiggle.isActive()) effects.add(new RenderWiggleEffect(wiggle));
        if (twist != null && twist.isActive()) effects.add(new RenderTwistEffect(twist));
        return new RenderEffectChain(effects);
    }
}
```

### RenderAlphaEffect

```java
public interface RenderAlphaEffect {
    float compute(RenderAlphaContext ctx);
}

public record RenderAlphaContext(
    float t,
    float time,
    int rayIndex,
    RayFlowConfig flowConfig
) {}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `RenderTravelAlpha` | Chase/scroll brightness animation | ~100 |
| `RenderFlickerAlpha` | Random/rhythmic brightness | ~80 |
| `RenderFadeAlpha` | Fade gradient from fadeStart/fadeEnd | ~40 |

### EmitStrategy (Strategy Pattern)

```java
public interface EmitStrategy {
    void emit(VertexConsumer consumer, Mesh mesh, EmitContext ctx);
}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `EmitLineStrategy` | Line vertex emission with effects | ~200 |
| `EmitTriangleStrategy` | Triangle emission for 3D shapes | ~100 |
| `EmitCageStrategy` | Wireframe/cage emission | ~150 |

---

## 3.6 3D GEOMETRY LAYER

### GeoRadiusProfile

```java
public interface GeoRadiusProfile {
    float radiusAt(float theta);
}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `GeoSphereProfile` | Constant radius | ~15 |
| `GeoDropletProfile` | sin(θ/2)^power teardrop | ~25 |
| `GeoEggProfile` | 1 + asymmetry × cos(θ) | ~25 |
| `GeoConeProfile` | θ/π linear | ~15 |
| `GeoBulletProfile` | hemisphere + cylinder | ~30 |

### GeoDeform

```java
public interface GeoDeform {
    float[] deform(float[] position, GeoDeformContext ctx);
}
```

### Implementations

| Class | Responsibility | Lines |
|-------|---------------|-------|
| `GeoGravityDeform` | Tidal spaghettification | ~200 |
| `GeoWaveDeform` | Existing wave deformation | ~100 |

---

# PART 4: FILE STRUCTURE

```
src/main/java/net/cyberpunk042/visual/
├── shape/
│   ├── ShapeState.java                 (~40) NEW - Stage/Phase/EdgeMode
│   ├── ShapeStage.java                 (~20) NEW - DORMANT, SPAWNING, ACTIVE, DESPAWNING
│   └── EdgeTransitionMode.java         (existing, moved from animation)
│
├── energy/                             (NEW PACKAGE)
│   ├── EnergyInteractionType.java      (~50) - Top-level: NONE, RADIATIVE, future...
│   ├── RadiativeInteraction.java       (~120) - EMISSION, ABSORPTION, REFLECTION, etc.
│   ├── EnergyTravel.java               (~80) - was TravelMode
│   ├── EnergyFlicker.java              (~80) - was FlickerMode
│   └── package-info.java               (~40) - Documentation

src/client/java/net/cyberpunk042/client/visual/
├── mesh/
│   ├── ray/
│   │   ├── RayPositioner.java              (~150) Facade
│   │   ├── RayContext.java                 (existing, updated to hold ShapeState)
│   │   │
│   │   ├── arrangement/
│   │   │   ├── ArrangementStrategy.java    (~20)
│   │   │   ├── RadialArrangement.java      (~80)
│   │   │   ├── SphericalArrangement.java   (~100)
│   │   │   ├── ParallelArrangement.java    (~60)
│   │   │   ├── ConvergingArrangement.java  (~80)
│   │   │   └── DivergingArrangement.java   (~80)
│   │   │
│   │   ├── distribution/                   NEW PACKAGE
│   │   │   ├── DistributionStrategy.java   (~20)
│   │   │   ├── UniformDistribution.java    (~30)
│   │   │   ├── RandomDistribution.java     (~40)
│   │   │   └── StochasticDistribution.java (~50)
│   │   │
│   │   ├── flow/
│   │   │   ├── AnimationState.java         (~50) RENAMED from FlowState
│   │   │   ├── FlowContext.java            (~30)
│   │   │   ├── FlowStage.java              (~20) Interface
│   │   │   ├── FlowPipeline.java           (~50)
│   │   │   ├── FlowPhaseStage.java         (~80)
│   │   │   ├── FlowTravelStage.java        (~80) NEW - chase/scroll
│   │   │   └── FlowFlickerStage.java       (~80)
│   │   │
│   │   ├── geometry/
│   │   │   ├── GeoPath.java                (~30)
│   │   │   ├── GeoLinearPath.java          (~60)
│   │   │   ├── ComposedGeoPath.java        (~100)
│   │   │   ├── GeoPathFactory.java         (~30)
│   │   │   │
│   │   │   ├── CurvatureStrategy.java      (~20) NEW
│   │   │   ├── NoCurvature.java            (~15)
│   │   │   ├── VortexCurvature.java        (~60)
│   │   │   ├── SpiralCurvature.java        (~60)
│   │   │   ├── GravitationalCurvature.java (~80)
│   │   │   │
│   │   │   ├── LineShapeStrategy.java      (~20) NEW
│   │   │   ├── StraightLineShape.java      (~15)
│   │   │   ├── WavyLineShape.java          (~50)
│   │   │   ├── HelixLineShape.java         (~60)
│   │   │   ├── CorkscrewLineShape.java     (~60)
│   │   │   └── DoubleHelixLineShape.java   (~80)
│   │   │
│   │   ├── layer/                          NEW PACKAGE
│   │   │   ├── LayerModeStrategy.java      (~20)
│   │   │   ├── VerticalLayerMode.java      (~30)
│   │   │   └── HorizontalLayerMode.java    (~30)
│   │   │
│   │   ├── tessellation/
│   │   │   ├── TessEdgeModeFactory.java    (~60) SIMPLIFIED
│   │   │   ├── TessSegmentStrategy.java    (~20)
│   │   │   ├── TessSimpleSegment.java      (~20)
│   │   │   ├── TessDashedSegment.java      (~60)
│   │   │   └── TessShapedSegment.java      (~40)
│   │   │
│   │   └── tessellator/
│   │       ├── RayTypeTessellator.java     (existing)
│   │       ├── RayLineTessellator.java     (~300 cleaned)
│   │       ├── RayDropletTessellator.java  (existing)
│   │       └── RayTypeTessellatorRegistry.java (existing)
│   │
│   └── geometry3d/
│       ├── GeoRadiusProfile.java           (~20)
│       ├── GeoSphereProfile.java           (~15)
│       ├── GeoDropletProfile.java          (~25)
│       ├── GeoEggProfile.java              (~25)
│       ├── GeoConeProfile.java             (~15)
│       ├── GeoBulletProfile.java           (~30)
│       ├── GeoDeform.java                  (~20)
│       ├── GeoGravityDeform.java           (~200)
│       └── GeoWaveDeform.java              (~100)

│
└── field/render/
    ├── RaysRenderer.java                   (~200) Facade
    │
    ├── effect/
    │   ├── RenderVertexEffect.java         (~30)
    │   ├── RenderEffectContext.java        (~20)
    │   ├── RenderEffectChain.java          (~50)
    │   ├── RenderMotionEffect.java         (~180)
    │   ├── RenderWiggleEffect.java         (~150)
    │   └── RenderTwistEffect.java          (~100)
    │
    ├── alpha/
    │   ├── RenderAlphaEffect.java          (~20)
    │   ├── RenderAlphaContext.java         (~20)
    │   ├── RenderTravelAlpha.java          (~100)
    │   ├── RenderFlickerAlpha.java         (~80)
    │   └── RenderFadeAlpha.java            (~40)
    │
    └── emit/
        ├── EmitStrategy.java               (~20)
        ├── EmitContext.java                (~30)
        ├── EmitLineStrategy.java           (~200)
        ├── EmitTriangleStrategy.java       (~100)
        └── EmitCageStrategy.java           (~150)
```

---

# PART 5: IMPLEMENTATION ORDER

## Phase 0: Stage/Phase Foundation (FIRST - Zero Risk)
Create the foundational Stage/Phase model that everything else depends on.

| Step | File | Lines | Notes |
|------|------|-------|-------|
| 0.1 | `shape/ShapeStage.java` | 20 | DORMANT, SPAWNING, ACTIVE, DESPAWNING |
| 0.2 | `shape/EdgeTransitionMode.java` | 15 | Move from RayFlowConfig |
| 0.3 | `shape/ShapeState.java` | 40 | Stage + Phase + EdgeMode record |
| 0.4 | Update `RaysShape` to include ShapeState | - | Add stage, phase, edgeMode fields |
| 0.5 | Add UI controls for Stage/Phase | - | Preview without animation |

**CHECKPOINT:** Can set stage/phase in UI and see shape change

---

## Phase 1: Core Abstractions (Zero Risk)
Create interfaces and records only - no behavior change.

| Step | File | Lines |
|------|------|-------|
| 1.1 | `flow/AnimationState.java` | 50 |
| 1.2 | `flow/FlowContext.java` | 30 |
| 1.3 | `flow/FlowStage.java` (interface) | 20 |
| 1.4 | `arrangement/ArrangementStrategy.java` | 20 |
| 1.5 | `distribution/DistributionStrategy.java` | 20 |
| 1.6 | `geometry/GeoPath.java` | 30 |
| 1.7 | `geometry/CurvatureStrategy.java` | 20 |
| 1.8 | `geometry/LineShapeStrategy.java` | 20 |
| 1.9 | `layer/LayerModeStrategy.java` | 20 |
| 1.10 | `effect/RenderVertexEffect.java` | 30 |
| 1.11 | `emit/EmitStrategy.java` | 20 |

**CHECKPOINT:** Compiles, no runtime changes

---

## Phase 2: Distribution & Layer Extraction (Low Risk)

| Step | File | Extract From |
|------|------|--------------|
| 2.1 | `UniformDistribution.java` | RayPositioner distribution logic |
| 2.2 | `RandomDistribution.java` | RayPositioner distribution logic |
| 2.3 | `StochasticDistribution.java` | RayPositioner distribution logic |
| 2.4 | `VerticalLayerMode.java` | RayPositioner layer logic |
| 2.5 | `HorizontalLayerMode.java` | RayPositioner layer logic |

**CHECKPOINT:** Distribution and layers work

---

## Phase 3: Arrangement Extraction (Low Risk)

| Step | File | Extract From |
|------|------|--------------|
| 3.1 | `RadialArrangement.java` | RayPositioner:1068-1125 |
| 3.2 | `SphericalArrangement.java` | RayPositioner:1127-1194 |
| 3.3 | `ParallelArrangement.java` | RayPositioner:1196-1224 |
| 3.4 | `ConvergingArrangement.java` | New |
| 3.5 | `DivergingArrangement.java` | New |
| 3.6 | Update RayPositioner to use strategies | |

**CHECKPOINT:** Visual test - rays position correctly

---

## Phase 4: Geometry Strategies (Medium Risk)

| Step | File | Extract From |
|------|------|--------------|
| 4.1 | `NoCurvature.java` | Identity |
| 4.2 | `VortexCurvature.java` | RayGeometryUtils |
| 4.3 | `SpiralCurvature.java` | RayGeometryUtils |
| 4.4 | `GravitationalCurvature.java` | RayGeometryUtils |
| 4.5 | `StraightLineShape.java` | Identity |
| 4.6 | `WavyLineShape.java` | RayGeometryUtils |
| 4.7 | `HelixLineShape.java` | RayGeometryUtils |
| 4.8 | `CorkscrewLineShape.java` | RayGeometryUtils |
| 4.9 | `DoubleHelixLineShape.java` | RayGeometryUtils |
| 4.10 | `ComposedGeoPath.java` | Composes Curvature + LineShape |

**CHECKPOINT:** All curved/shaped rays render correctly

---

## Phase 5: Flow Pipeline with Stage/Phase (Medium Risk)

| Step | File | Notes |
|------|------|-------|
| 5.1 | `FlowPhaseStage.java` | Compute base phase from time |
| 5.3 | `FlowTravelStage.java` | Chase/scroll in ACTIVE stage |
| 5.4 | `FlowFlickerStage.java` | Flicker overlay |
| 5.5 | `FlowPipeline.java` | Orchestrator |
| 5.6 | Update RayPositioner to use pipeline | |

**CHECKPOINT:** Animation outputs correct stage + phase

---

## Phase 6: Tessellation Simplification (Low Risk)

| Step | File | Notes |
|------|------|-------|
| 6.1 | `TessEdgeModeFactory.java` | Reads ShapeState, computes clip/scale/alpha |
| 6.2 | Update RayLineTessellator | Use TessEdgeModeFactory |
| 6.3 | Update RayDropletTessellator | Use TessEdgeModeFactory |

**CHECKPOINT:** Edge modes work with Stage/Phase model

---

## Phase 7: Render Effect Extraction (Medium Risk)

| Step | File | Extract From |
|------|------|--------------|
| 7.1 | `RenderMotionEffect.java` | RaysRenderer:1114-1270 |
| 7.2 | `RenderWiggleEffect.java` | RaysRenderer:1278-1408 |
| 7.3 | `RenderTwistEffect.java` | RaysRenderer:1414-1491 |
| 7.4 | `RenderEffectChain.java` | New |
| 7.5 | Update RaysRenderer to use chain | |

**CHECKPOINT:** Motion, wiggle, twist work

---

## Phase 8: Emit Strategies (Low Risk)

| Step | File | Extract From |
|------|------|--------------|
| 8.1 | `EmitLineStrategy.java` | RaysRenderer |
| 8.2 | `EmitTriangleStrategy.java` | RaysRenderer |
| 8.3 | `EmitCageStrategy.java` | RaysRenderer |
| 8.4 | Simplify RaysRenderer to facade | |

**CHECKPOINT:** All render modes work

---

## Phase 9: 3D Geometry Extraction (Low Risk)

| Step | File | Extract From |
|------|------|--------------|
| 9.1 | `GeoDropletProfile.java` | Ray3DGeometryUtils |
| 9.2 | `GeoEggProfile.java` | Ray3DGeometryUtils |
| 9.3 | `GeoGravityDeform.java` | Ray3DGeometryUtils |
| 9.4 | Simplify Ray3DGeometryUtils | |

**CHECKPOINT:** 3D rays work

---

## Phase 10: Cleanup & Renames

| Step | Action |
|------|--------|
| 10.1 | Remove dead code |
| 10.2 | Apply all variable renames (clipStart, compositeAlpha, etc.) |
| 10.3 | Update all imports |
| 10.4 | Run full preset test |
| 10.5 | Move EdgeTransitionMode out of RayFlowConfig |

---


# PART 6: SUCCESS CRITERIA

## File Size Check

| Category | Files | All ≤400 lines? |
|----------|-------|-----------------|
| Shape State (NEW) | 3 files | ✓ |
| Computation | 8 files | ✓ |
| Distribution (NEW) | 4 files | ✓ |
| Arrangement | 6 files | ✓ |
| Geometry (Curvature + LineShape) | 14 files | ✓ |
| Layer | 3 files | ✓ |
| Tessellation | 5 files | ✓ |
| Rendering Effects | 6 files | ✓ |
| Emit Strategies | 5 files | ✓ |
| 3D Geometry | 9 files | ✓ |
| **Total** | **~63 files** | ✓ |

## Pattern Check

| Pattern | Where Used |
|---------|------------|
| **State Pattern** | ShapeState (Stage/Phase) |
| **Strategy** | Arrangement, Distribution, Curvature, LineShape, LayerMode, Emit, RadiusProfile |
| **Pipeline** | FlowStage chain → AnimationState |
| **Decorator/Composite** | RenderEffectChain |
| **Immutable Record** | ShapeState, AnimationState, FlowContext, TessEdgeResult |
| **Factory** | GeoPathFactory, TessEdgeModeFactory, RenderEffectChain.create() |
| **Facade** | RayPositioner, RaysRenderer |

## Naming Check

| Layer | All prefixed correctly? |
|-------|------------------------|
| Shape* | ✓ (ShapeState, ShapeStage) |
| Flow* | ✓ (FlowPipeline, FlowPhaseStage, etc.) |
| Tess* | ✓ (TessEdgeModeFactory, TessSegmentStrategy) |
| Geo* | ✓ (GeoPath, GeoCurvature, GeoLineShape) |
| Render* | ✓ (RenderVertexEffect, RenderEffectChain) |
| Emit* | ✓ (EmitStrategy, EmitLineStrategy) |

## Stage/Phase Model Check

| Requirement | Met? |
|-------------|------|
| User can set stage/phase in UI without animation | ✓ |
| EdgeTransitionMode is in ShapeState, not animation config | ✓ |
| FlowPipeline outputs AnimationState (stage + phase), not derived values | ✓ |
| Tessellator reads ShapeState to compute visual parameters | ✓ |
| Other shapes can use same Stage/Phase pattern | ✓ |

