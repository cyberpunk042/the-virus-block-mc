---
description: Implementation plan for the Orb Spawn Animation System ("Coming Into Existence")
---

# Orb Spawn Animation System - Implementation Plan

## Overview

A system for spawning orbs from distant origins with smooth travel animations, configurable easing, lifetime management, and fade in/out effects.

**This is SEPARATE from OrbChargeHandler (C key charge/throw).** The spawn system is its own feature.

---

## 1. System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           USER TRIGGERS SPAWN                                │
│                         (GUI Panel Enable/Spawn)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OrbSpawnManager.spawnOrb()                          │
│  - Reads spawn config from FieldVisualAdapter                                │
│  - Calculates spawn position based on OriginMode + spawnDistance             │
│  - Creates FieldVisualInstance with opacity=0 (invisible)                    │
│  - Registers orb in FieldVisualRegistry                                      │
│  - Creates SpawnAnimationState to track this orb's animation                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    OrbSpawnManager.tick() [every frame]                      │
│  For each active SpawnAnimationState:                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ PHASE 1: FADE IN (0 to fadeInDuration)                                  │ │
│  │   - Interpolate opacity: 0 → 1                                          │ │
│  │   - Orb stays at spawn position                                         │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ PHASE 2: TRAVEL (fadeInDuration to fadeIn + interpolationDuration)     │ │
│  │   - Interpolate position: spawnPos → targetPos (using EasingCurve)      │ │
│  │   - Opacity stays at 1                                                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ PHASE 3: ALIVE (after travel, until lifetime expires)                   │ │
│  │   - If followMode=true: update position to follow player                │ │
│  │   - If followMode=false: stay fixed in world                            │ │
│  │   - If lifetime=0: stay forever                                         │ │
│  │   - If lifetime>0: count down, trigger despawn when expired             │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ PHASE 4: FADE OUT (triggered by lifetime expiry or manual despawn)     │ │
│  │   - Interpolate opacity: 1 → 0                                          │ │
│  │   - When opacity=0: unregister from FieldVisualRegistry                 │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       FieldVisualInstance (existing)                         │
│  - Now has `opacity` field (0.0 to 1.0)                                      │
│  - Position updated by OrbSpawnManager during travel                         │
│  - Registered in FieldVisualRegistry                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Rendering Pipeline (existing)                        │
│  - WorldRendererFieldVisualMixin gets fields from Registry                   │
│  - PostEffectPassMixin packs opacity into UBO                                │
│  - Shader applies opacity to final color output                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. New Classes

### SpawnAnimationState (Internal tracking for each spawning orb)
```java
class SpawnAnimationState {
    UUID orbId;                    // The FieldVisualInstance ID
    OrbSpawnConfig config;         // The spawn configuration
    Vec3d spawnPosition;           // Where orb started
    Vec3d targetPosition;          // Where orb is going
    long spawnTime;                // System.currentTimeMillis() at spawn
    AnimationPhase currentPhase;   // FADE_IN, TRAVEL, ALIVE, FADE_OUT
    long phaseStartTime;           // When current phase started
}

enum AnimationPhase {
    FADE_IN,   // Appearing
    TRAVEL,    // Moving toward target
    ALIVE,     // At destination, waiting for lifetime
    FADE_OUT   // Disappearing
}
```

### OrbSpawnConfig (User-configurable parameters)
```java
public record OrbSpawnConfig(
    SpawnOriginMode originMode,      // FROM_ABOVE, FROM_BELOW, etc.
    TargetMode targetMode,           // RELATIVE or TRUE_TARGET
    float spawnDistance,             // How far away to spawn (defaults to proximityDarken)
    float targetDistance,            // Where to stop (for RELATIVE mode)
    Vec3d trueTargetCoords,          // Exact coords (for TRUE_TARGET mode)
    long interpolationDurationMs,    // Travel time
    EasingCurve easingCurve,         // Movement curve
    long fadeInDurationMs,           // Appear duration
    long fadeOutDurationMs,          // Disappear duration
    long lifetimeMs,                 // 0=infinite, otherwise countdown
    boolean followModeAfterArrival   // Follow player after arrival?
)
```

---

## 3. How Position Calculation Works

### Spawn Position Calculation (at spawn time)
```
referencePos = player position (or custom coords for TRUE_TARGET)

spawnDirection = based on SpawnOriginMode:
  FROM_ABOVE      → Vec3d(0, 1, 0)       // Straight up
  FROM_BELOW      → Vec3d(0, -1, 0)      // Straight down
  FROM_HORIZON    → player.lookDirection  // Where player is facing
  FROM_SKY_HORIZON→ (0, 0.5, 0.5).normalize() // 45° from sky

spawnDistance defaults to adapter.proximityDarken (1000 for V7/V8, lower for simpler versions)

spawnPosition = referencePos + (spawnDirection * spawnDistance)
```

### Target Position Calculation
```
if (targetMode == RELATIVE):
    targetPosition = referencePos + (spawnDirection * targetDistance)
    // e.g., spawn 1000 blocks above, target 500 blocks above → orb stops halfway
    
if (targetMode == TRUE_TARGET):
    targetPosition = trueTargetCoords
    // Orb goes to exact world coordinates
```

---

## 4. Files Structure

### New Files (in `client/input/spawn/`)
```
SpawnOriginMode.java    - Enum with direction vectors
TargetMode.java         - Enum (RELATIVE, TRUE_TARGET)
EasingCurve.java        - Enum with math functions
OrbSpawnConfig.java     - Immutable record with all params
SpawnAnimationState.java - Internal state tracking (package-private)
OrbSpawnManager.java    - The core manager, handles all spawns
```

### Modified Files
```
FieldVisualInstance.java  - Add opacity field (float 0-1)
FieldVisualAdapter.java   - Add spawn config storage + buildSpawnConfig()
FieldVisualSubPanel.java  - Add spawn controls section
PostEffectPassMixin.java  - Pack opacity into UBO  
field_visual_*.fsh        - Apply opacity in shader output
ClientGuiNodes.java       - Register OrbSpawnManager tick
```

---

## 5. GUI Controls Layout

### Spawn Animation Section (new section in FieldVisualSubPanel)

```
┌─────────────────────────────────────────────────────────────────┐
│ SPAWN ANIMATION                                                  │
├─────────────────────────────────────────────────────────────────┤
│ [Origin: ▼FROM_ABOVE]  [Target: ▼RELATIVE]                      │
│                                                                  │
│ Spawn Distance: [████████░░] (proximityDarken)                   │
│ Target Distance: [████░░░░░] (50% of spawn)  (for RELATIVE mode) │
│                                                                  │
│ ── If TrueTarget mode: ──────────────────────────────────────── │
│ Target X: [___] Y: [___] Z: [___]                               │
│                                                                  │
│ Travel Time: [████░░░░░░] 10s                                   │
│ Easing: [▼EASE_OUT]                                             │
│                                                                  │
│ Fade In: [██░░] 500ms    Fade Out: [██░░] 500ms                 │
│ Lifetime: [░░░░░░░░░░] 0 (infinite)                             │
│                                                                  │
│           [ ⚡ SUMMON ORB ]                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Trigger

**Dedicated "SUMMON ORB" button** triggers the spawn animation:

```java
// In FieldVisualSubPanel
Button summonButton = new Button("⚡ SUMMON ORB", () -> {
    OrbSpawnConfig config = adapter.buildSpawnConfig();
    Vec3d referencePos;
    
    if (config.targetMode() == TargetMode.TRUE_TARGET) {
        // Use manually entered coordinates
        referencePos = config.trueTargetCoords();
    } else {
        // Use player position as reference
        referencePos = client.player.getBoundingBox().getCenter();
    }
    
    OrbSpawnManager.spawnOrb(config, referencePos);
});
```

This is **completely separate** from the existing "Toggle Effect" functionality.

---

## 6. Task Breakdown (Revised)

### Phase 1: Data Structures (30 min)
| Task | Description |
|------|-------------|
| 1.1 | Create `SpawnOriginMode.java` with direction calculation |
| 1.2 | Create `TargetMode.java` enum |
| 1.3 | Create `EasingCurve.java` with interpolation functions |
| 1.4 | Create `OrbSpawnConfig.java` record |
| 1.5 | Create `SpawnAnimationState.java` for internal tracking |

### Phase 2: Core Manager (60 min)
| Task | Description |
|------|-------------|
| 2.1 | Create `OrbSpawnManager.java` skeleton |
| 2.2 | Implement `spawnOrb(config, referencePos)` |
| 2.3 | Implement `tick()` main loop |
| 2.4 | Implement FADE_IN phase logic |
| 2.5 | Implement TRAVEL phase logic with easing |
| 2.6 | Implement ALIVE phase logic (follow mode, lifetime) |
| 2.7 | Implement FADE_OUT phase logic |
| 2.8 | Implement `despawnOrb(orbId)` for manual despawn |

### Phase 3: Opacity Support (30 min)
| Task | Description | Status |
|------|-------------|--------|
| 3.1 | Add `spawnOpacity` field to FieldVisualInstance | ✅ Done |
| 3.2 | Add `setSpawnOpacity()` / `getSpawnOpacity()` methods | ✅ Done |
| 3.3 | Multiply spawnOpacity into existing `v2Alpha.alphaScale` in PostEffectPassMixin | ✅ Done |
| 3.4 | ~~Modify shaders~~ - NOT NEEDED, alphaScale already controls alpha | ✅ N/A |

### Phase 4: Adapter + GUI (45 min)
| Task | Description |
|------|-------------|
| 4.1 | Add spawn config fields to FieldVisualAdapter |
| 4.2 | Add `buildSpawnConfig()` method |
| 4.3 | Add spawn controls section to FieldVisualSubPanel |
| 4.4 | Add "Spawn Orb" button to trigger spawn |

### Phase 5: Registration + Testing (30 min)
| Task | Description |
|------|-------------|
| 5.1 | Register OrbSpawnManager.tick() in ClientGuiNodes |
| 5.2 | Test all origin modes |
| 5.3 | Test easing curves |
| 5.4 | Test lifetime + despawn |
| 5.5 | Test follow mode behavior |

---

## 7. Open Questions

1. **Multiple simultaneous orbs?** Should we limit to 1, or allow many?
2. **Presets?** Add quick presets like "Descending Sun" (FROM_ABOVE, 1000→500, 30s, EASE_OUT)?

---

## 8. Timeline

| Phase | Time |
|-------|------|
| Phase 1: Data Structures | 30 min |
| Phase 2: Core Manager | 60 min |
| Phase 3: Opacity Support | 30 min |
| Phase 4: Adapter + GUI | 45 min |
| Phase 5: Registration + Testing | 30 min |
| **Total** | **~3.5 hours** |
