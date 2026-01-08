# Ray Flow Architecture Proposal

**Date:** 2025-12-27

---

## Current State

### ShapeState (runtime state)
```java
record ShapeState<S extends ShapeStage>(
    S stage,           // DORMANT, SPAWNING, ACTIVE, DESPAWNING
    float phase,       // 0-1 progress within stage
    EdgeTransitionMode edgeMode  // CLIP, SCALE, FADE
)
```

### RayFlowConfig (animation config)
Currently contains BOTH:
1. **Shape behavior** (how shape looks at a given phase):
   - `LengthMode` (RADIATE, ABSORB, SEGMENT, etc.)
   - `segmentLength`
   - `waveArc`, `waveDistribution`, `waveCount`
   - `EdgeTransitionMode`
   
2. **Animation parameters** (how phase changes over time):
   - `lengthSpeed`
   - `travelSpeed`
   - `flickerIntensity, flickerFrequency`
   - `TravelMode`, `FlickerMode`
   - `chaseCount, chaseWidth`
   - `startFullLength`, `followCurve`

### RaysShape (shape definition)
Currently does NOT contain flow/visibility modes.

---

## Proposed Change

### RaysShape (shape definition) - ADD:
```java
// === Visibility/Flow Mode (how shape renders based on phase) ===
LengthMode lengthMode,          // RENAMED: "VisibilityMode" or "FlowMode"?
float segmentLength,            // visible segment length
float waveArc,                  // wave scale
WaveDistribution waveDistribution,
float waveCount,                // sweep copies
EdgeTransitionMode edgeMode,    // CLIP, SCALE, FADE
```

### RayFlowConfig (animation config) - KEEP ONLY:
```java
// === Enable/Disable toggles ===
boolean lengthEnabled,          // toggle length animation
boolean travelEnabled,          // toggle travel animation
boolean flickerEnabled,         // toggle flicker animation

// === Speed parameters ===
float lengthSpeed,
float travelSpeed,
float flickerFrequency,

// === Mode-specific parameters ===
TravelMode travelMode,          // CHASE, SCROLL, SPARK, etc.
int chaseCount,
float chaseWidth,
FlickerMode flickerMode,        // SCINTILLATION, STROBE
float flickerIntensity,

// === Other ===
boolean startFullLength,
boolean followCurve
```

---

## Chain of Responsibility

### 1. Shape Definition (RaysShape)
- Defines WHAT the shape is and HOW it looks at any given phase
- `lengthMode` + `segmentLength` + `waveArc` = "at phase X, this segment of the ray is visible"
- `edgeMode` = "visible segment has CLIP/SCALE/FADE edges"

### 2. Animation Config (RayFlowConfig)
- Controls whether animation is enabled and speed
- Does NOT define appearance, only timing

### 3. FlowPipeline / FlowPhaseStage
- Computes the current phase (0-1) based on:
  - Animation config (speed, pattern)
  - Time
  - Per-ray distribution (waveDistribution, waveCount)
- Outputs: `ShapeState` with updated phase

### 4. TessEdgeModeFactory
- Takes: phase + EdgeMode
- Returns: clipStart, clipEnd, scale, alpha
- Simple mapping from phase to visual output

### 5. RaysRenderer / Tessellators
- Reads ShapeState from primitive
- Queries TessEdgeModeFactory for how to render
- Applies clipping/scaling/fading per-vertex

---

## Data Flow

```
User Config
    ├── RaysShape (defines appearance at any phase)
    │     ├── lengthMode, segmentLength, waveArc, edgeMode
    │     └── (static visual properties)
    │
    ├── RayFlowConfig (defines animation timing)
    │     ├── lengthEnabled, lengthSpeed
    │     ├── travelEnabled, travelSpeed
    │     └── (dynamic timing properties)
    │
    └── ShapeState (runtime state)
          ├── stage: ACTIVE (set by user or animation)
          ├── phase: 0.0-1.0 (animated over time)
          └── edgeMode: (from shape)

                    ↓ (FlowPipeline processes)

         FlowPhaseStage
              │
              └── Computes phase based on time + config
                    │
                    ↓
         
         TessEdgeModeFactory.compute(phase, edgeMode, lengthMode, ...)
              │
              └── Returns clipStart, clipEnd, scale, alpha
                    │
                    ↓
         
         RaysRenderer applies result per-vertex
```

---

## Questions to Resolve

1. **Naming**: Should `LengthMode` be renamed to `VisibilityMode` or `FlowMode`?

2. **ShapeState vs RaysShape**: Where should `edgeMode` live?
   - Currently in both `ShapeState` and `RayFlowConfig`
   - Should be in ONE place only

3. **TessEdgeModeFactory scope**: Should it know about LengthMode?
   - Currently: NO (just phase + EdgeMode)
   - Alternative: YES (takes LengthMode from shape to compute clipping)

4. **Serialization**: Moving fields from RayFlowConfig → RaysShape affects JSON format

---

## Next Steps

1. Confirm naming for LengthMode replacement
2. Add new fields to RaysShape
3. Remove moved fields from RayFlowConfig
4. Update TessEdgeModeFactory to read from shape
5. Update FlowPipeline to use new structure
6. Update GUI panels
