# UBO Architecture Refactor Plan

> **STATUS: ACTIVE REFACTOR PLAN**  
> This is the master plan for restructuring the UBO system from a monolithic approach
> to a domain-oriented composition. All new effects (including Tornado) should wait
> for this refactor before implementation.

---

## Guiding Principles (Non-Negotiable)

### 1. Composition, Not Inheritance
Shaders "use" UBOs; they don't "extend" a base UBO. Base UBOs are always available; feature UBOs are added as needed.

### 2. Domain Ownership is Strict
Every uniform belongs to exactly one domain:
- **Frame:** global per-frame drivers (time, deltaTime, frameIndex, globalDebugFlags)
- **Camera:** view definition and matrices
- **Object:** per-instance identity/transform (if needed later)
- **Material:** per-material constants (if needed later)
- **Light:** scene lights (if needed later)
- **EffectConfig:** preset/style parameters (rare updates)
- **EffectRuntime:** per-frame instance parameters (only if truly CPU-driven)

### 3. Parameter Growth Must Not Reorder Memory
Reserve explicit space ("reserved lanes") per section so new parameters do not force layout shifts.

### 4. ABI Stability for Shader Authors
The preamble macro approach is correct. We formalize it into a stable "shader ABI" and rewire data sources to proper UBOs.

### 5. Debug Trace is Intentional
Debug capability comes from a small set of header/ID/flags fields, not by dumping more random floats into every config.

---

## Target Folder Layout

```
/ubo
 ├─ frame.ubo
 ├─ camera.ubo
 ├─ object.ubo
 ├─ material.ubo
 ├─ light.ubo
 └─ effects/
     ├─ field_visual_config.ubo
     ├─ field_visual_runtime.ubo
     ├─ tornado_config.ubo
     ├─ wormhole_config.ubo
     └─ ...future...
```

Shader side mirrors this layout:
```
/shaders/include/ubo/
/shaders/include/ubo/effects/
```

SamplerInfo UBO remains where Minecraft expects it; it is not merged into our UBO system.

---

## Centralized UBO Registry (No Magic Numbers)

### Binding Convention
| Range | Purpose |
|-------|---------|
| 0–9 | Base UBOs |
| 10–19 | Pass/Post UBOs (only if needed) |
| 20–29 | Feature/Effect UBOs |

### Binding Mapping
| UBO | Binding Index |
|-----|---------------|
| FrameUBO | 0 |
| CameraUBO | 1 |
| ObjectUBO | 2 |
| MaterialUBO | 3 |
| LightUBO | 4 |
| FieldVisualConfigUBO | 20 |
| FieldVisualRuntimeUBO | 21 |
| TornadoConfigUBO | 22 |
| WormholeConfigUBO | 23 |

**All binding calls must route through this registry.**

---

## Performance Update Policy (Golden Rule)

| UBO | Update Frequency | Notes |
|-----|------------------|-------|
| FrameUBO | Every frame | Smallest possible payload, best for ring-buffer + mapped write |
| CameraUBO | On change (or every frame) | Keep matrices here, not inside effects |
| ObjectUBO | Per draw/instance | Ring-buffered if heavy instancing |
| MaterialUBO | Rarely | Cached |
| LightUBO | On light changes | Batched updates |
| EffectConfigUBO | Preset/style changes | Rare |
| EffectRuntimeUBO | Per frame (if CPU-driven) | Otherwise minimal/constant |

**Key Rule:** Animate via Frame time (GPU does the animation) unless you truly need CPU-driven curves per parameter.

---

## Phase 0 — Inventory & Classification

**Goal:** Classify every existing FieldVisualConfig field into the correct domain.

**Deliverable:** Classification table with columns:
- Current name
- Current slot/section
- Domain target (Frame / Camera / EffectConfig / EffectRuntime / Debug)
- Update frequency (per-frame / on-change / preset)
- Notes (why it belongs there)

**Hard Classification Rules:**
- Camera vectors/matrices/near/far/fov/aspect → CameraUBO
- Global time, frame index, global debug flags → FrameUBO
- Effect style/preset knobs (colors, noise, thresholds, modes) → EffectConfigUBO
- Per-instance movement/curves (center/radius if moving, intensity ramps) → EffectRuntimeUBO
- Debug that applies to all effects → FrameUBO header flags
- Debug specific to FieldVisual → EffectRuntime or EffectConfig header flags

---

## Phase 1 — Formalize the Base UBOs (Engine Contract)

### FrameUBO (binding 0)
```java
@UBOStruct(name = "FrameData")
public record FrameUBO(
    @Vec4 FrameTimeVec4 frameTime,  // x=time, y=deltaTime, z=frameIndex, w=layoutVersion
    @Vec4 FrameFlagsVec4 flags      // x=globalDebugMode, y=debugFlags, z,w=reserved
) {}
```

### CameraUBO (binding 1)
```java
@UBOStruct(name = "CameraData")
public record CameraUBO(
    @Vec4 CameraPosVec4 position,       // xyz=pos, w=reserved
    @Vec4 CameraForwardVec4 forward,    // xyz=forward, w=aspect
    @Vec4 CameraUpVec4 up,              // xyz=up, w=fov
    @Vec4 CameraClipVec4 clip,          // x=near, y=far, z=isFlying, w=reserved
    @Mat4 Matrix4f viewProj,            // View-projection matrix
    @Mat4 Matrix4f invViewProj          // Inverse view-projection matrix
) {}
```

**Success Criteria:** At least one shader reads camera/time from the new base blocks.

---

## Phase 2 — Split FieldVisual into Config vs Runtime

### FieldVisualConfigUBO (binding 20)
**Purpose:** Preset/style, rarely updated.

Contains:
- Header (version, effectType, flags, reserved)
- Palette (primary/secondary/tertiary/highlight/ray)
- Shape/falloff
- Noise
- Glow/rays/corona
- Distortion/screen effects
- Geometry/geodesic settings
- Versioned sections (V2, V5, V8 blocks)
- Explicit reserved lanes per section

### FieldVisualRuntimeUBO (binding 21)
**Purpose:** Per-frame instance state, only if needed.

Contains:
- centerAndRadius (if moving every frame)
- Runtime phase offset (if CPU-driven)
- Runtime intensity multiplier (if CPU-driven)
- fadeIn/fadeOut (if driven)
- Per-instance toggles

**If effect does not need per-frame CPU control:**
- Keep RuntimeUBO minimal or constant
- Rely on FrameUBO time for animation

**Success Criteria:** FieldVisualConfig no longer contains camera/time/matrices.

---

## Phase 3 — Structured Sections Using Nested @UBOStruct Records

### Implementation Requirement
Allow a record component whose type is annotated with @UBOStruct to be written recursively.

### Result
FieldVisualConfigUBO becomes composition of sections:
- Header section
- Palette section
- Noise section
- Shape section
- Distortion section
- V2 section
- V8 section
- Reserved lanes

**Success Criteria:** You can add new V9 parameters by editing only one section record.

---

## Phase 4 — Reserved Lanes & Versioning Strategy

### Reserved Space Per Section
- Reserve N vec4 slots per section (2–8 depending on expected growth)
- Reserved lanes written as zeros

### Versioning Strategy
- FrameUBO has global layoutVersion (optional)
- FieldVisualConfigUBO header has effectVersion
- Optionally include fieldVisualLayoutVersion in header

### Policy
- If you change layout, bump version and keep compatibility code
- Prefer adding new fields into reserved lanes (no version bump required)

---

## Phase 5 — Preamble ABI Formalization

### Formal Contract
After `FIELD_VISUAL_PREAMBLE()` the shader always has:
- sceneColor/rawDepth/isSky/linearDist/sceneDepth
- camPos/forward/cam/ray
- config data (cfg or fieldConfig)
- runtime data (rt or fieldRuntime)
- sphereCenter/sphereRadius

### Rewiring Rule
- Time comes only from FrameUBO
- Camera and matrices come only from CameraUBO
- Effect knobs come only from EffectConfigUBO
- Per-frame effect instance state comes only from EffectRuntimeUBO

---

## Phase 6 — Binding & Validation

### Enhance Reliability
- Compute expected byte size from record components
- Log block name, binding index, size at startup (debug)
- Optionally compute layout ID constant per block

### Validation Rules
- If calculated size differs from expected: fail fast in dev
- If missing required annotations: fail fast if configured

---

## Phase 7 — Performance Hardening (Only After Correctness)

Apply when you see stalls or heavy streaming.

### Ring Buffering Candidates
- FrameUBO: yes (updated every frame)
- RuntimeUBO: yes (if updated every frame)
- ObjectUBO: yes (if heavy instancing)

### Persistent Mapping Candidates
- FrameUBO: best candidate
- Runtime/Object: candidate if heavy

**Do not prematurely optimize.**

---

## Recommended Execution Order (Minimal Risk)

1. Create FrameUBO + CameraUBO records and GLSL blocks; bind via centralized registry
2. Update FieldVisual shader preamble to read time/camera/matrices from Frame/Camera
3. Split FieldVisual into FieldVisualConfigUBO + FieldVisualRuntimeUBO
4. Refactor FieldVisualConfig into vec4-grouped sections and add reserved lanes
5. Upgrade ReflectiveUBOWriter to support nested @UBOStruct sections
6. Add validation/logging (sizes, block bindings, versions)
7. If needed: ring-buffer Frame + Runtime; optionally persistent mapping

---

## Definition of "Done"

You are done when:
- Base UBOs exist and are stable (Frame, Camera)
- FieldVisual is split into Config vs Runtime
- New effects can be added without modifying base UBOs
- You can add new parameters into reserved lanes without breaking older shaders
- Binder is centralized and validated
- Per-frame updates are limited to Frame and optionally Runtime (not the full config)

---

---

# Part 2: Companion Documents & Complementary Steps

---

## 1) UBO Spec Sheet (Per-Block Contract)

Create one page per UBO with:
- Block name
- Binding index
- Update frequency
- Ownership
- Layout version
- Size in bytes
- Sections
- Reserved lanes count
- Shader include path(s)
- Java record class name

---

## 2) Field/Parameter Dictionary (Domain Glossary)

Define parameters precisely:
- CoreSize: what "size" means (radius? normalized?)
- EdgeSharpness: higher = sharper edge
- NoiseResLow/High: frequency or resolution
- FadePower/FadeScale: distance fade math
- CoronaWidth/Power/Multiplier: each role
- Flags bit meanings

---

## 3) Ownership Checklist (One-page Guardrail)

Before adding any new parameter:
- Which domain owns it?
- How often does it change?
- Is it per-view, per-instance, or global?
- Can it be derived from existing values?
- Does it require reserved lanes?
- Does it need version bump?

---

## 4) Layout Compatibility Policy

- If you only fill reserved lanes: no version bump needed
- If you reorder or remove fields: bump layout version
- If shader depends on new fields: check version and fallback safely
- Maintain one "current" layout

---

## 5) Debug Playbook

- What to log on startup
- How to detect wrong binds (zeros, NaNs)
- Debug view modes (0=normal, 1=mask, 2=depth, 3=ray viz)
- How to isolate single effect instance

---

## 6) Golden Test Shaders (Regression Anchors)

Pick 2–3 canary shaders:
- Basic post shader reading FrameUBO time only
- Camera ray shader reading CameraUBO matrices only
- Minimal FieldVisual reading ConfigUBO + Frame time

---

## 7) Automation: Emit GLSL Block Declarations from Java Records

Add generator that outputs:
- GLSL uniform block declarations matching Java records
- Optional .md report with sizes/sections/reserved lanes

---

## 8) Complementary Implementation Steps

### Step A: Nested @UBOStruct support
So you can express sections cleanly.

### Step B: @ReservedVec4(count=N) convenience
Writes N zero vec4 slots explicitly.

### Step C: Expected size assertions
At bind time, compare calculated vs expected size.

### Step D: layoutId constant
Simple integer per block, stored in header.

---

## 9) Replace Many Floats with Vec4s

Goal:
- CenterX/Y/Z/Radius → CenterAndRadius vec4
- PrimaryR/G/B/A → PrimaryColor vec4

Reduces binder complexity, name drift, layout foot-guns.

---

## 10) Suggested Docs Folder Layout

```
/docs
 ├─ ubo_spec.md
 ├─ ubo_registry.md
 ├─ field_visual_param_dictionary.md
 ├─ layout_compat_policy.md
 ├─ debug_playbook.md
 └─ regression_shaders.md
```

---

## 11) Migration Checklist (Per Change)

For every UBO layout change:
1. Update Java record
2. Update calculated size snapshot
3. Update GLSL block (or regenerate)
4. Update version/layoutId
5. Run golden test shaders
6. Validate runtime logs for binding/size/version

---

## Impact on Tornado Effect

The Tornado implementation plan (see `tornado_effect_plan.md`) will need to be updated after this refactor:

1. **TornadoConfigUBO** will follow the new pattern:
   - Binding 22
   - Nested sections with reserved lanes
   - Read time from FrameUBO
   - Read camera from CameraUBO
   - Only effect-specific parameters in TornadoConfigUBO

2. **File paths** will change to match new folder layout

3. **Panel integration** will benefit from standardized UBO structure

---

> **NEXT STEPS:**
> 1. Complete Phase 0 (inventory & classification of existing FieldVisualConfig)
> 2. Create FrameUBO + CameraUBO
> 3. Update one shader to validate the new structure
> 4. Then proceed with remaining phases
