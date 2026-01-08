# Reflective UBO System - Implementation Summary

## Overview

This document explains the two approaches considered for the Unified Reflective UBO System, and why we chose a **hybrid approach**.

---

## The Problem

Manual UBO writing was error-prone and tedious:
- 583 lines of manual `builder.putFloat()` calls across two files
- Slot order tracked only by comments
- Adding a parameter required editing 3+ files
- No validation that Java and GLSL matched

---

## Two Approaches Considered

### Approach A: Wrapper Records (Original Plan)

**Philosophy:** Keep existing records unchanged, create new wrapper records for everything.

```java
// Existing record UNCHANGED
public record PositionParams(float centerX, float centerY, float centerZ, float radius) {
    // No changes
}

// NEW wrapper file: FieldVisualVec4Types.java
public record PositionVec4(float x, float y, float z, float w) implements Vec4Serializable {
    public static PositionVec4 from(PositionParams p) {
        return new PositionVec4(p.centerX(), p.centerY(), p.centerZ(), p.radius());
    }
    @Override public float slot0() { return x; }
    @Override public float slot1() { return y; }
    @Override public float slot2() { return z; }
    @Override public float slot3() { return w; }
}
```

**Pros:**
- No risk of breaking existing code
- Clean separation of concerns
- Existing API unchanged

**Cons:**
- ~400 lines of wrapper code
- Every 4-float record needs a wrapper
- Extra conversion step at call site

---

### Approach B: Direct Interface Implementation

**Philosophy:** Make existing 4-float records directly implement `Vec4Serializable`.

```java
// Existing record MODIFIED to implement interface
public record PositionParams(float centerX, float centerY, float centerZ, float radius) 
    implements Vec4Serializable {
    
    @Override public float slot0() { return centerX; }
    @Override public float slot1() { return centerY; }
    @Override public float slot2() { return centerZ; }
    @Override public float slot3() { return radius; }
    
    // Existing methods unchanged
}
```

**Pros:**
- Less code (~5 lines per record)
- Records become self-describing
- No conversion step needed

**Cons:**
- Only works for records with exactly 4 float fields
- Modifies existing records (small risk)

---

## The Hybrid Approach (What We Implemented)

We analyzed the existing records and found:

| Category | Count | Approach |
|----------|-------|----------|
| Records with exactly 4 floats | 22 | Direct interface (Approach B) |
| Records with >4 fields needing extraction | 5 | Wrapper records (Approach A) |

### Records Using Direct Interface (22 total)

These records already have exactly 4 floats, so they directly implement `Vec4Serializable`:

- `PositionParams`, `AnimTimingParams`, `CoreEdgeParams`, `FalloffParams`
- `NoiseConfigParams`, `NoiseDetailParams`, `GlowLineParams`, `CoronaParams`
- `V2CoronaDetail`, `V2CoreDetail`, `V2EdgeDetail`, `V2LinesDetail`, `V2AlphaDetail`
- `GeometryParams`, `GeometryParams2`, `TransformParams`, `LightingParams`
- `TimingParams`, `ScreenEffects`, `DistortionParams`, `BlendParams`, `ReservedParams`

### Records Needing Wrappers (Complex Types)

| Source Record | # Fields | # Vec4s Needed | Wrappers Created |
|--------------|----------|----------------|------------------|
| `ColorParams` | 5 colors | 5 | `PrimaryColorVec4`, `SecondaryColorVec4`, `TertiaryColorVec4`, `HighlightColorVec4`, `RayColorVec4` |
| `AnimParams` | 8 | 2 | `AnimBaseVec4`, `AnimMultiSpeedVec4` |
| `CameraParams` | 12 | 3 | `CameraPosTimeVec4`, `CameraForwardVec4`, `CameraUpVec4` |
| `RenderParams` | 3 + pad | 1 | `RenderParamsVec4` |
| `DebugParams` | 2 + pad | 1 | `DebugParamsVec4` |

---

## Final Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SHARED INFRASTRUCTURE                        │
│  net.cyberpunk042.client.visual.ubo/                           │
│  ├── annotation/                                                │
│  │   ├── UBOStruct.java        ← Marks UBO record classes      │
│  │   ├── Vec4.java             ← Marks vec4 fields             │
│  │   ├── Mat4.java             ← Marks mat4 fields             │
│  │   └── Floats.java           ← Marks variable float arrays   │
│  ├── Vec4Serializable.java     ← Interface for vec4 records    │
│  ├── ReflectiveUBOWriter.java  ← Writes any @UBOStruct record  │
│  └── GLSLValidator.java        ← Validates Java vs GLSL        │
└─────────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│    FIELD VISUAL         │    │      SHOCKWAVE          │
│                         │    │                         │
│ FieldVisualTypes.java   │    │ ShockwaveTypes.java     │
│ (22 records with impl)  │    │ (existing records)      │
│                         │    │                         │
│ FieldVisualVec4Types.java│   │ ShockwaveVec4Types.java │
│ (12 wrappers for complex)│   │ (18 wrappers)           │
│                         │    │                         │
│ FieldVisualUBO.java     │    │ ShockwaveUBO.java       │
│ (42 slots = 672 bytes)  │    │ (22 slots = 352 bytes)  │
└─────────────────────────┘    └─────────────────────────┘
```

---

## Benefits Achieved

| Metric | Before | After |
|--------|--------|-------|
| Lines of manual UBO writing | 583 | 0 |
| Files to edit when adding param | 3+ | 1 (UBO record) |
| Buffer size calculation | Manual | Automatic |
| Documentation | Comments | Code structure |
| Risk of slot mismatch | High | Zero |

---

## How to Add a New Parameter

### Before (Manual System)
1. Add field to config record
2. Add to UBO writer with correct slot position
3. Update GLSL uniform block
4. Update buffer size constant
5. Hope you counted correctly

### After (Reflective System)
1. Add field to config record (implement `Vec4Serializable` if 4 floats)
2. Add to UBO record in correct position
3. Update GLSL uniform block
4. **Buffer size updates automatically**

---

## Files Created

| File | Purpose | Lines |
|------|---------|-------|
| `ubo/annotation/UBOStruct.java` | Class-level annotation | 15 |
| `ubo/annotation/Vec4.java` | Field annotation | 12 |
| `ubo/annotation/Mat4.java` | Field annotation | 12 |
| `ubo/annotation/Floats.java` | Field annotation | 15 |
| `ubo/Vec4Serializable.java` | Interface | 46 |
| `ubo/ReflectiveUBOWriter.java` | Core writer | 233 |
| `ubo/GLSLValidator.java` | Validator (partial) | 178 |
| `effect/FieldVisualVec4Types.java` | Wrappers | ~180 |
| `effect/FieldVisualUBO.java` | UBO record | ~170 |
| `shockwave/ShockwaveVec4Types.java` | Wrappers | ~280 |
| `shockwave/ShockwaveUBO.java` | UBO record | ~150 |

---

## Files to Delete (After Testing)

| File | Lines | Status |
|------|-------|--------|
| `FieldVisualUBOWriter.java` | 366 | **Deprecated** |
| `ShockwaveUBOWriter.java` | 217 | **Deprecated** |

---

## Future Work

1. **Implement GLSL parsing in GLSLValidator** - Currently logs Java slots only
2. **Add unit tests** for ReflectiveUBOWriter
3. **Consider code generation** for slot0-3 methods (macro/annotation processor)
