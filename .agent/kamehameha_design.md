# Kamehameha Effect System Design

## Reference Image Analysis

![Kamehameha Reference](../uploaded_image_1766967743318.png)

The classic Kamehameha has distinct visual components:
1. **Charging Orb** - Bright sphere that grows during charging
2. **Beam Core** - The main cylinder/cone blast
3. **Glow Aura** - Energy halo around both parts
4. **Color Profile** - Typically cyan-white core with blue edges

---

## Lifecycle Phases

### Phase 1: CHARGING (Spawn Stage)
- **Orb**: Starts small, grows to full size
- **Orb Effect**: Pulse/throb, energy swirling inward
- **Beam**: Not visible yet
- **Duration**: Configurable (e.g., 40-100 ticks)

### Phase 2: FIRING (Transition)
- **Orb**: Stays at full size or slightly shrinks
- **Beam**: Rapidly extends outward from orb
- **Beam Width**: Starts narrow, expands to full width
- **Duration**: Short (10-20 ticks)

### Phase 3: SUSTAIN (Active Stage)
- **Orb**: Full size, continuous glow
- **Beam**: Full length, continuous energy flow
- **Travel Effect**: Energy particles traveling along beam (SCROLL/COMET)
- **Duration**: Variable (until trigger ends)

### Phase 4: DESPAWN 
- **Beam**: Retracts or fades
- **Orb**: Shrinks and fades
- **Duration**: 20-40 ticks

---

## Current Infrastructure Analysis

### ✅ What We HAVE:

| Component | Location | Status |
|-----------|----------|--------|
| **LifecycleState** | `field.instance.LifecycleState` | SPAWNING, ACTIVE, DESPAWNING, COMPLETE |
| **LifecycleConfig** | `field.influence.LifecycleConfig` | fadeIn/fadeOut/scaleIn/scaleOut timings |
| **Phase** | `visual.animation.Phase` | Animation offset for staggered effects |
| **TravelEffectConfig** | `visual.animation.TravelEffectConfig` | Directional alpha animation (CHASE, SCROLL, COMET, BIPOLAR) |
| **PrimitiveLink** | `field.primitive.PrimitiveLink` | Links primitives together (follow, radiusMatch, phaseOffset) |
| **PulseConfig** | `visual.animation.PulseConfig` | Scale/alpha pulsing |
| **SpinConfig** | `visual.animation.SpinConfig` | Rotation animation |
| **JetShape** | `visual.shape.JetShape` | Tapered cylinder (perfect for beam!) |
| **SphereShape** | `visual.shape.SphereShape` | For the charging orb |

### ❌ What We LACK:

| Need | Description | Priority |
|------|-------------|----------|
| **StageConfig** | Per-primitive stage definitions (CHARGE, FIRE, SUSTAIN, DESPAWN) | HIGH |
| **StageTransition** | Transition triggers between stages (time, input, event) | HIGH |
| **Multi-Primitive Lifecycle** | Coordinated lifecycle across linked primitives | HIGH |
| **Length Animation** | Beam extends/retracts over time | MEDIUM |
| **Radius Animation** | Beam width grows/shrinks over time | MEDIUM |
| **Inward Energy Flow** | Particles flowing TOWARD center (for charging) | MEDIUM |
| **Stage-dependent Animation** | Different animations per stage | MEDIUM |

---

## Proposed Architecture

### New Components

#### 1. `StageConfig` (visual.animation)
```java
public record StageConfig(
    String id,                      // "charge", "fire", "sustain", "despawn"
    int duration,                   // Duration in ticks (0 = indefinite)
    StageTransition exitCondition,  // How to exit this stage
    Animation animation,            // Stage-specific animation settings
    ShapeModifier shapeModifier,    // Scale, length, radius changes
    AppearanceModifier appearance   // Color, alpha, glow changes
) {
    public static final StageConfig CHARGE = ...;
    public static final StageConfig FIRE = ...;
    public static final StageConfig SUSTAIN = ...;
    public static final StageConfig DESPAWN = ...;
}
```

#### 2. `StageTransition` (visual.animation)
```java
public enum StageTransition {
    TIME_COMPLETE,  // Stage ends when duration expires
    EVENT_TRIGGER,  // Stage ends on external event
    MANUAL,         // Stage ends on explicit call
    AUTO_CHAIN      // Automatically chains to next stage
}
```

#### 3. `LifecycleAnimator` (visual.animation)
```java
public record LifecycleAnimator(
    String id,
    List<StageConfig> stages,       // Ordered list of stages
    boolean loop,                   // Loop back to first stage?
    Map<String, String> links       // Cross-primitive coordination
) {
    // Compute current stage based on elapsed time
    public StageConfig currentStage(long elapsedTicks);
    
    // Get stage progress (0-1)
    public float stageProgress(long elapsedTicks);
    
    // Get overall lifecycle progress (0-1)
    public float lifecycleProgress(long elapsedTicks);
}
```

#### 4. `ShapeModifier` (visual.shape)
```java
public record ShapeModifier(
    EaseFunction scaleEase,    // How scale changes over stage
    float scaleStart,          // Scale at stage start
    float scaleEnd,            // Scale at stage end
    
    EaseFunction lengthEase,   // For cylinders/jets - length animation
    float lengthStart,
    float lengthEnd,
    
    EaseFunction radiusEase,   // Radius animation
    float radiusStart,
    float radiusEnd
) {}
```

---

## Kamehameha JSON Example

```json
{
  "id": "kamehameha",
  "primitives": [
    {
      "id": "orb",
      "type": "sphere",
      "shape": { "radius": 0.8, "latSteps": 24, "lonSteps": 24 },
      "appearance": { "color": "#80E0FF", "glow": 1.5, "alpha": { "min": 0.8, "max": 1.0 } },
      "lifecycle": {
        "stages": [
          {
            "id": "charge",
            "duration": 60,
            "shapeModifier": { "scaleStart": 0.1, "scaleEnd": 1.0, "scaleEase": "easeOutBack" },
            "animation": { "pulse": { "speed": 2.0, "scale": 0.1 } }
          },
          {
            "id": "fire",
            "duration": 15,
            "shapeModifier": { "scaleStart": 1.0, "scaleEnd": 0.9 }
          },
          {
            "id": "sustain",
            "duration": 0,
            "animation": { "pulse": { "speed": 0.5, "scale": 0.05 } }
          },
          {
            "id": "despawn",
            "duration": 30,
            "shapeModifier": { "scaleStart": 0.9, "scaleEnd": 0.0 },
            "appearance": { "alpha": { "start": 1.0, "end": 0.0 } }
          }
        ]
      }
    },
    {
      "id": "beam",
      "type": "jet",
      "shape": { 
        "baseRadius": 0.5, 
        "tipRadius": 0.3, 
        "length": 20.0,
        "segments": 1,
        "radialSteps": 16
      },
      "transform": { "offset": { "y": 0, "z": 0.8 } },
      "appearance": { "color": "#A0F0FF", "glow": 2.0 },
      "link": { "target": "orb", "follow": true },
      "lifecycle": {
        "stages": [
          {
            "id": "charge",
            "duration": 60,
            "visible": false
          },
          {
            "id": "fire",
            "duration": 15,
            "shapeModifier": { "lengthStart": 0.1, "lengthEnd": 1.0, "lengthEase": "easeOutExpo" },
            "animation": { 
              "travelEffect": { "mode": "SCROLL", "speed": 3.0, "direction": "Z" }
            }
          },
          {
            "id": "sustain",
            "duration": 0,
            "animation": {
              "travelEffect": { "mode": "SCROLL", "speed": 2.0, "direction": "Z", "count": 3 }
            }
          },
          {
            "id": "despawn",
            "duration": 20,
            "shapeModifier": { "lengthStart": 1.0, "lengthEnd": 0.0, "lengthEase": "easeInQuad" }
          }
        ]
      }
    }
  ]
}
```

---

## Implementation Phases

### Phase 1: Stage System Core
1. Create `StageConfig` record
2. Create `StageTransition` enum  
3. Create `LifecycleAnimator` record
4. Add `stages` field to Primitive or Animation

### Phase 2: Shape Modifiers
1. Create `ShapeModifier` record
2. Implement `EaseFunction` enum (linear, easeIn, easeOut, easeInOut, etc.)
3. Wire modifiers to affect shape during rendering

### Phase 3: Stage-Aware Rendering
1. Modify `AbstractPrimitiveRenderer` to check current stage
2. Apply stage-specific animations
3. Apply stage-specific shape modifiers

### Phase 4: Cross-Primitive Coordination
1. Extend `PrimitiveLink` with stage synchronization
2. Add "waitFor" stage transitions (beam waits for orb to finish charging)
3. Add "triggerOn" stage events

### Phase 5: UI Integration
1. Stage timeline editor
2. Per-stage animation controls
3. Preview with stage scrubbing

---

## Quick Wins (Use Existing Infrastructure)

Before building the full stage system, we can achieve a basic Kamehameha with:

1. **Orb with PulseConfig** - Throb during "charging"
2. **JetShape** - Already supports tapered cylinder
3. **TravelEffectConfig** - SCROLL mode for energy flow
4. **LifecycleConfig** - fadeIn/scaleIn for spawn animation
5. **PrimitiveLink.follow** - Beam follows orb position

**Limitation**: No stage transitions, beam doesn't extend over time.

---

## Next Steps

1. ✅ Complete TravelEffectConfig integration (done!)
2. 🔄 Design and implement `StageConfig`
3. 🔄 Implement `ShapeModifier` for dynamic length/radius
4. 🔄 Create `LifecycleAnimator` for multi-stage coordination
5. 🔄 Add UI for stage-based animation editing
