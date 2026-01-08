# Reflective UBO System - Implementation Plan

> **STATUS: SUPERSEDED** by `UNIFIED_REFLECTIVE_UBO_PLAN.md`
> 
> This was the initial FieldVisual-only plan. The unified plan extends this to both effects.
> See `REFLECTIVE_UBO_IMPLEMENTATION.md` for the final implementation summary.

## Overview

Replace the manual 300+ line `FieldVisualUBOWriter` with a reflection-based system that:
- Uses Java record declaration order as UBO slot order
- Annotations at usage site declare serialization intent
- Zero maintenance when adding parameters
- Startup validation against GLSL struct

---

## Phase 1: Infrastructure (New Files)

### Task 1.1: Create Annotation Package
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/annotation/`

| File | Purpose | Code |
|------|---------|------|
| `UBOStruct.java` | Marks a record as UBO root | `@Target(TYPE) @Retention(RUNTIME)` |
| `Vec4.java` | Component serializes as 4 floats | `@Target(RECORD_COMPONENT) @Retention(RUNTIME)` |
| `Mat4.java` | Component serializes as 16 floats | `@Target(RECORD_COMPONENT) @Retention(RUNTIME)` |
| `Floats.java` | Component serializes as N floats | `@Target(RECORD_COMPONENT) @Retention(RUNTIME)` + `int count()` |

**Dependencies:** None (new files)
**Estimated size:** ~15 lines each

---

### Task 1.2: Create Vec4Serializable Interface
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/Vec4Serializable.java`

```java
public interface Vec4Serializable {
    float slot0();
    float slot1();
    float slot2();
    float slot3();
}
```

**Purpose:** Any record that implements this can be written as vec4 automatically.
**Alternative:** Use reflection to get first 4 float fields (more magic, less explicit)

---

### Task 1.3: Create ReflectiveUBOWriter
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/ReflectiveUBOWriter.java`

```java
public class ReflectiveUBOWriter {
    
    public static int getBufferSize(Class<?> uboClass) { ... }
    
    public static void write(Std140Builder builder, Object record) { ... }
    
    private static void writeVec4(Std140Builder builder, Object value) { ... }
    
    private static void writeMatrix(Std140Builder builder, Matrix4f mat) { ... }
}
```

**Logic:**
1. Iterate `record.getClass().getRecordComponents()` (preserves declaration order)
2. For each component, check annotations
3. `@Vec4` → extract 4 floats via interface or reflection
4. `@Mat4` → write 16 floats column-major
5. `@Floats(count=N)` → write N floats with std140 padding

**Dependencies:** Annotations (Task 1.1)
**Estimated size:** ~80 lines

---

### Task 1.4: Create GLSLValidator
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/GLSLValidator.java`

```java
public class GLSLValidator {
    
    public static void validate(Class<?> uboClass, String glslPath, String structName) {
        List<String> javaSlots = extractJavaSlots(uboClass);
        List<String> glslSlots = parseGLSLStruct(glslPath, structName);
        
        if (!javaSlots.equals(glslSlots)) {
            throw new UBOMismatchException(javaSlots, glslSlots);
        }
    }
    
    private static List<String> parseGLSLStruct(String path, String name) {
        // Read shader file, find "uniform <name> { ... }", extract field names
    }
}
```

**Logic:**
1. Read GLSL file as resource
2. Parse uniform block by name
3. Extract field names in order
4. Compare with Java record component names

**Dependencies:** None (reads files)
**Estimated size:** ~100 lines

---

## Phase 2: Create UBO Record

### Task 2.1: Create FieldVisualUBO Record
**Location:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualUBO.java`

This is the **single source of truth** for UBO structure:

```java
@UBOStruct(name = "FieldVisualConfig", glslPath = "the-virus-block:shaders/post/field_visual.fsh")
public record FieldVisualUBO(
    // ═══════════════════════════════════════════════════════════════
    // EFFECT PARAMS (Slots 0-23)
    // ═══════════════════════════════════════════════════════════════
    @Vec4 PositionParams position,           // Slot 0
    @Vec4 PrimaryColorVec4 primary,          // Slot 1
    @Vec4 SecondaryColorVec4 secondary,      // Slot 2
    @Vec4 TertiaryColorVec4 tertiary,        // Slot 3
    @Vec4 HighlightColorVec4 highlight,      // Slot 4
    @Vec4 RayColorVec4 rayColor,             // Slot 5
    @Vec4 AnimBaseParams animBase,           // Slot 6
    @Vec4 AnimMultiSpeed animMulti,          // Slot 7
    @Vec4 AnimTiming animTiming,             // Slot 8
    @Vec4 CoreEdgeParams coreEdge,           // Slot 9
    @Vec4 FalloffParams falloff,             // Slot 10
    @Vec4 NoiseConfigParams noise,           // Slot 11
    @Vec4 GlowLineParams glowLine,           // Slot 12
    @Vec4 V2Params v2,                       // Slot 13
    @Vec4 V2Detail1 v2Detail1,               // Slot 14
    @Vec4 V2Detail2 v2Detail2,               // Slot 15
    @Vec4 V2Detail3 v2Detail3,               // Slot 16
    @Vec4 V2Detail4 v2Detail4,               // Slot 17
    @Vec4 V2Detail5 v2Detail5,               // Slot 18
    @Vec4 V2Detail6 v2Detail6,               // Slot 19
    @Vec4 V2Detail7 v2Detail7,               // Slot 20
    @Vec4 V2Detail8 v2Detail8,               // Slot 21
    @Vec4 V2Line1 v2Line1,                   // Slot 22
    @Vec4 ReservedParams reserved,           // Slot 23
    
    // ═══════════════════════════════════════════════════════════════
    // CAMERA/RUNTIME (Slots 24-27)
    // ═══════════════════════════════════════════════════════════════
    @Vec4 CameraPosTime cameraPosTime,       // Slot 24
    @Vec4 CameraForwardAspect cameraForward, // Slot 25
    @Vec4 CameraUpFov cameraUp,              // Slot 26
    @Vec4 RenderParams render,               // Slot 27
    
    // ═══════════════════════════════════════════════════════════════
    // MATRICES (Slots 28-35)
    // ═══════════════════════════════════════════════════════════════
    @Mat4 Matrix4f invViewProj,              // Slots 28-31
    @Mat4 Matrix4f viewProj,                 // Slots 32-35
    
    // ═══════════════════════════════════════════════════════════════
    // DEBUG (Slot 36)
    // ═══════════════════════════════════════════════════════════════
    @Vec4 DebugParams debug                  // Slot 36
) {
    // Factory method to build from current components
    public static FieldVisualUBO from(
        FieldVisualConfig config,
        PositionParams position,
        CameraParams camera,
        RenderParams render,
        Matrix4f invViewProj,
        Matrix4f viewProj,
        DebugParams debug
    ) {
        // Extract and transform existing records into vec4-serializable form
        return new FieldVisualUBO(...);
    }
}
```

**Dependencies:** Annotations (Task 1.1), existing records (FieldVisualTypes)
**Estimated size:** ~150 lines

---

### Task 2.2: Create Vec4 Wrapper Records
**Location:** Add to `FieldVisualTypes.java` or new file

Some existing records don't map directly to vec4. Create wrappers:

```java
// Extracts primary color from ColorParams as vec4
public record PrimaryColorVec4(float r, float g, float b, float a) 
    implements Vec4Serializable {
    
    public static PrimaryColorVec4 from(ColorParams colors) {
        return new PrimaryColorVec4(
            colors.primaryR(), colors.primaryG(), 
            colors.primaryB(), colors.primaryA()
        );
    }
    
    @Override public float slot0() { return r; }
    @Override public float slot1() { return g; }
    @Override public float slot2() { return b; }
    @Override public float slot3() { return a; }
}
```

**Records needed:**
- PrimaryColorVec4, SecondaryColorVec4, TertiaryColorVec4, HighlightColorVec4, RayColorVec4
- AnimBaseParams (extract from AnimParams)
- AnimMultiSpeed, AnimTiming
- CameraPosTime, CameraForwardAspect, CameraUpFov

**Dependencies:** Existing FieldVisualTypes records
**Estimated size:** ~20 lines each × 15 = ~300 lines

---

## Phase 3: Integration

### Task 3.1: Update PostEffectPassMixin
**Location:** `src/client/java/net/cyberpunk042/mixin/client/PostEffectPassMixin.java`

**Change (lines 226-235):**
```java
// Before:
FieldVisualUBOWriter.write(builder, config, position, cameraParams, ...);

// After:
FieldVisualUBO ubo = FieldVisualUBO.from(config, position, cameraParams, ...);
ReflectiveUBOWriter.write(builder, ubo);
```

**Dependencies:** FieldVisualUBO (Task 2.1), ReflectiveUBOWriter (Task 1.3)

---

### Task 3.2: Add Startup Validation
**Location:** `src/client/java/net/cyberpunk042/TheVirusBlockClient.java`

Add to `onInitializeClient()`:

```java
// Validate UBO matches GLSL at startup
GLSLValidator.validate(
    FieldVisualUBO.class,
    "the-virus-block:shaders/post/field_visual.fsh",
    "FieldVisualConfig"
);
```

**Dependencies:** GLSLValidator (Task 1.4), FieldVisualUBO (Task 2.1)

---

### Task 3.3: Delete Old Writer
**Location:** `src/client/java/net/cyberpunk042/client/visual/effect/uniform/FieldVisualUBOWriter.java`

After integration verified working:
1. Delete `FieldVisualUBOWriter.java` (366 lines)
2. Remove import from `PostEffectPassMixin`

---

## Phase 4: Testing & Cleanup

### Task 4.1: Create Unit Tests
**Location:** `src/test/java/net/cyberpunk042/client/visual/ubo/`

```java
@Test void testBufferSize() {
    assertEquals(592, ReflectiveUBOWriter.getBufferSize(FieldVisualUBO.class));
}

@Test void testSerializationOrder() {
    // Verify bytes match old writer output
}
```

### Task 4.2: Remove Dead Code
- Delete slot comments from FieldVisualTypes (slot order is implicit now)
- Delete BUFFER_SIZE constant from FieldVisualUBOWriter

---

## Implementation Order

```
Week 1: Infrastructure
├── Task 1.1: Annotations
├── Task 1.2: Vec4Serializable interface
├── Task 1.3: ReflectiveUBOWriter
└── Task 1.4: GLSLValidator

Week 2: UBO Record
├── Task 2.1: FieldVisualUBO record
└── Task 2.2: Vec4 wrapper records

Week 3: Integration
├── Task 3.1: Update PostEffectPassMixin
├── Task 3.2: Add startup validation
└── Task 3.3: Delete old writer

Week 4: Testing
├── Task 4.1: Unit tests
└── Task 4.2: Cleanup
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Reflection performance | Cache RecordComponent[] at startup |
| GLSL parsing errors | Regex is simple; fallback to manual validation |
| Type mismatches | Vec4Serializable interface enforces contract |
| Migration bugs | Keep old writer until new one verified |

---

## Success Metrics

1. **Lines deleted:** ~300 (FieldVisualUBOWriter)
2. **To add a parameter:** Edit 1 line in FieldVisualUBO, 1 line in GLSL
3. **Mismatch detection:** Runtime error with clear message at startup
4. **Documentation:** UBO struct IS the documentation (self-describing)

---

## File Summary

| Action | File | Lines |
|--------|------|-------|
| CREATE | annotation/UBOStruct.java | 10 |
| CREATE | annotation/Vec4.java | 10 |
| CREATE | annotation/Mat4.java | 10 |
| CREATE | annotation/Floats.java | 15 |
| CREATE | Vec4Serializable.java | 10 |
| CREATE | ReflectiveUBOWriter.java | 80 |
| CREATE | GLSLValidator.java | 100 |
| CREATE | FieldVisualUBO.java | 150 |
| CREATE | Vec4 wrapper records | 300 |
| MODIFY | PostEffectPassMixin.java | 5 |
| MODIFY | TheVirusBlockClient.java | 5 |
| DELETE | FieldVisualUBOWriter.java | -366 |

**Net change:** +360 new, -366 deleted = **-6 lines** with massively improved DX
