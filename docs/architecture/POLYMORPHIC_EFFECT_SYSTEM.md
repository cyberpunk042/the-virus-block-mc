# Field Visual Record System

> **Goal:** Make it easy to add new visual effects without parameter confusion by applying the Shockwave record pattern.

---

## Part 1: Problem Analysis

### Current Data Flow
```
FieldVisualSubPanel (GUI)
       ↓ reads/writes named properties
FieldVisualAdapter (holds mutable state: spiralDensity, glowLineCount, etc.)
       ↓ buildConfig()
FieldVisualConfig (record with 16 params, all named for Energy Orb)
       ↓
PostEffectPassMixin.updateFieldVisualUniforms() (inline UBO writing)
       ↓ std140 buffer
field_visual.fsh (reads uniforms)
```

### The Problem
Every layer uses Energy Orb names:
- Adapter has `spiralDensity`, `glowLineCount` fields
- Config record has `spiralDensity()`, `glowLineCount()` accessors
- Mixin writes `spiralDensity` to UBO
- GUI shows "Spiral Density", "Glow Lines" labels

**When adding a new effect**, you must either:
1. Repurpose existing names (confusing)
2. Add new fields to every layer (explosion of fields)
3. Use "reserved" fields (undocumented, error-prone)

---

## Part 2: Shockwave Pattern (Reference)

Shockwave solves this with **grouped, typed records**:

```java
// ShockwaveTypes.java - ALL types in one place
record RingParams(float thickness, float intensity, ...) { ... }
record ShapeConfig(ShapeType type, float radius, ...) { ... }
record CoronaConfig(Color4f color, float width, float intensity, ...) { ... }
```

```java
// ShockwaveController.java - holds typed records
private RingParams ringParams = RingParams.DEFAULT;
private ShapeConfig shapeConfig = ShapeConfig.POINT;
```

```java
// ShockwaveUBOWriter.java - SINGLE PLACE for slot mapping
builder.putVec4(currentRadius, ringParams.thickness(), ringParams.intensity(), time);
```

**Key Insight:** Multiple purpose-grouped records, each independent.

---

## Part 3: Current UBO Layout (from PostEffectPassMixin)

| Vec4 | Name | Fields |
|------|------|--------|
| 0 | Position | centerX, centerY, centerZ, radius |
| 1 | PrimaryColor | R, G, B, A |
| 2 | SecondaryColor | R, G, B, A |
| 3 | TertiaryColor | R, G, B, A |
| 4 | AnimParams | phase, speed, intensity, effectType |
| 5 | CoreEdgeParams | coreSize, edgeSharpness, shapeType, 0 |
| 6 | SpiralParams | density, twist, 0, 0 |
| 7 | GlowLineParams | count, intensity, flags, version |
| 8 | V2Params | coronaWidth, 0, 0, 0 |
| 9 | Camera | 0, 0, 0, time |
| 10 | Forward | forwardX, forwardY, forwardZ, aspect |
| 11 | Up | upX, upY, upZ, fov |
| 12 | Extra | near, far, 0, isFlying |
| 13-16 | InvViewProj | mat4 |
| 17-20 | ViewProj | mat4 |
| 21 | Runtime | camMode, debugMode, 0, 0 |

---

## Part 4: Proposed Record Groups

### Effect Parameters (UBO Slots 1-13)

| # | Record | Fields | UBO Slot |
|---|--------|--------|----------|
| 1 | ColorParams | primary, secondary, tertiary (ARGB) | 1-3 |
| 2 | AnimParams | intensity, speed, effectType | 4 |
| 3 | CoreEdgeParams | coreSize, edgeSharpness, shapeType | 5 |
| 4 | SpiralParams | density, twist | 6 |
| 5 | GlowLineParams | count, intensity, showRays, showCorona, version | 7 |
| 6 | CoronaParams | width | 8 |
| 7 | OtherParams | pulseFrequency, pulseAmplitude, noiseScale, noiseStrength | 9 (NEW) |
| 8 | ScreenEffects | blackout, vignetteAmount, vignetteRadius, tintAmount | 10 (NEW) |
| 9 | DistortionParams | strength, radius, frequency, speed | 11 (NEW) |
| 10 | BlendParams | opacity, blendMode, fadeIn, fadeOut | 12 (NEW) |
| 11 | ReservedParams | slot1, slot2, slot3, slot4 | 13 (NEW) |

### Camera/Runtime Parameters (Set Per Frame)

| # | Record | Fields | UBO Slot |
|---|--------|--------|----------|
| 12 | CameraData | position, forward, up, aspect, fov, isFlying, near, far | 14-17 |
| 13 | DebugParams | camMode, debugMode | 26 |

---

## Part 5: Implementation Plan

### Phase 1: Create Record Types

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualTypes.java`

Create records 1-11 from table above. Each needs:
- Static `DEFAULT` constant
- `withX()` builder method for each field

**Dependency:** None

---

### Phase 2: Create UBO Writer

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/uniform/FieldVisualUBOWriter.java`

1. Create class with method: `write(Std140Builder, FieldVisualConfig, CameraData)`
2. Extract buffer writing logic from `PostEffectPassMixin.updateFieldVisualUniforms()`
3. Map each record group to its vec4 slot(s)

**Dependency:** Phase 1

---

### Phase 3: Update FieldVisualConfig

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualConfig.java`

Change from 16 flat params to composed records:
```java
public record FieldVisualConfig(
    ColorParams colors,
    AnimParams anim,
    CoreEdgeParams coreEdge,
    SpiralParams spiral,
    GlowLineParams glowLine,
    CoronaParams corona,
    OtherParams other,
    ScreenEffects screen,
    DistortionParams distortion,
    BlendParams blend,
    ReservedParams reserved
) { }
```

Update factory methods and `withX()` builders.

**Dependency:** Phase 1

---

### Phase 4: Update FieldVisualAdapter

**File:** `src/client/java/net/cyberpunk042/client/gui/state/adapter/FieldVisualAdapter.java`

Replace 20+ flat fields with record fields:
```java
private ColorParams colors = ColorParams.DEFAULT;
private AnimParams anim = AnimParams.DEFAULT;
private CoreEdgeParams coreEdge = CoreEdgeParams.DEFAULT;
// etc.
```

Update `get(path)` and `set(path)` to translate paths to record fields.

**Dependency:** Phase 1, Phase 3

---

### Phase 5: Update PostEffectPassMixin

**File:** `src/client/java/net/cyberpunk042/mixin/client/PostEffectPassMixin.java`

Replace 150 lines of inline buffer writing with:
```java
FieldVisualUBOWriter.write(builder, config, cameraData);
```

**Dependency:** Phase 2

---

### Phase 6: Update Shader Layout

**File:** `src/main/resources/assets/the-virus-block/shaders/post/field_visual.fsh`

Add new vec4 uniforms for slots 9-13 (OtherParams, ScreenEffects, DistortionParams, BlendParams, ReservedParams).

**Dependency:** Phase 2

---

### Phase 7: Test

Verify compile, rendering, GUI controls, presets, JSON.

---

## Part 6: Execution Order

```
Phase 1 (Types) ────→ Phase 2 (Writer) ────→ Phase 5 (Mixin)
       │                     │
       ↓                     ↓
Phase 3 (Config) ────→ Phase 4 (Adapter)
                             │
                             ↓
                      Phase 6 (Shader)
                             │
                             ↓
                      Phase 7 (Test)
```

**Safe sequence:**
1. Phase 1 - new file, no breaks
2. Phase 2 - new file, no breaks
3. Phase 6 - shader update
4. Phase 3 - config change
5. Phase 4 - adapter update
6. Phase 5 - mixin update
7. Phase 7 - test

---

## Part 7: Files Summary

| File | Action | Phase |
|------|--------|-------|
| `FieldVisualTypes.java` | CREATE | 1 |
| `FieldVisualUBOWriter.java` | CREATE | 2 |
| `FieldVisualConfig.java` | MODIFY | 3 |
| `FieldVisualAdapter.java` | MODIFY | 4 |
| `PostEffectPassMixin.java` | MODIFY | 5 |
| `field_visual.fsh` | MODIFY | 6 |
