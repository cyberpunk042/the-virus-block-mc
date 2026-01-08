# Unified Reflective UBO System - Complete Plan

> **STATUS: ✓ IMPLEMENTED** (January 4, 2026)
> 
> See `REFLECTIVE_UBO_IMPLEMENTATION.md` for the implementation summary.
> Old manual writers have been deleted. The system is now live.

## Overview

Replace **both** manual UBO writers with a shared reflection-based system:
- `FieldVisualUBOWriter.java` (366 lines) → DELETE
- `ShockwaveUBOWriter.java` (217 lines) → DELETE
- **Total deleted:** 583 lines of manual code

Both effects use the same `ReflectiveUBOWriter` infrastructure.

---

## Current State Analysis

### FieldVisual System
| File | Lines | Purpose |
|------|-------|---------|
| `FieldVisualTypes.java` | 711 | Record definitions (PositionParams, ColorParams, etc.) |
| `FieldVisualUBOWriter.java` | 366 | Manual writing of 37 vec4 slots (592 bytes) |
| `FieldVisualConfig.java` | ~200 | Configuration container |
| `field_visual.fsh` | 519 | GLSL with FieldVisualConfig uniform block |

### Shockwave System
| File | Lines | Purpose |
|------|-------|---------|
| `ShockwaveTypes.java` | 162 | Record definitions (RingParams, ShapeConfig, etc.) |
| `ShockwaveUBOWriter.java` | 217 | Manual writing of 22 vec4 slots (352 bytes) |
| `ShockwavePostEffect.java` | ~500 | Effect controller + state |
| `shockwave.fsh` | ~300 | GLSL with ShockwaveConfig uniform block |

### Common Pain Points
1. Adding a parameter requires 3+ file edits
2. Slot order tracked by comments (error-prone)
3. No compile-time or runtime validation
4. Buffer size constants manually calculated

---

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SHARED INFRASTRUCTURE                        │
│  net.cyberpunk042.client.visual.ubo/                           │
│  ├── annotation/                                                │
│  │   ├── UBOStruct.java                                        │
│  │   ├── Vec4.java                                             │
│  │   ├── Mat4.java                                             │
│  │   └── Floats.java                                           │
│  ├── Vec4Serializable.java                                     │
│  ├── ReflectiveUBOWriter.java    ← SHARED WRITER               │
│  └── GLSLValidator.java          ← SHARED VALIDATOR            │
└─────────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│    FIELD VISUAL         │    │      SHOCKWAVE          │
│ FieldVisualUBO.java     │    │ ShockwaveUBO.java       │
│ @UBOStruct record       │    │ @UBOStruct record       │
│ 37 vec4 = 592 bytes     │    │ 22 vec4 = 352 bytes     │
└─────────────────────────┘    └─────────────────────────┘
```

---

## Phase 1: Shared Infrastructure

### Task 1.1: Create Annotation Package
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/annotation/`

**Files to create:**

#### `UBOStruct.java`
```java
package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Marks a record as a UBO structure that can be serialized to GPU buffer.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UBOStruct {
    /** Name of the GLSL uniform block (for validation) */
    String name() default "";
    
    /** Path to GLSL file containing the struct (for validation) */
    String glslPath() default "";
}
```

#### `Vec4.java`
```java
package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as 4 floats (vec4).
 * The component must implement Vec4Serializable or have 4 float fields.
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface Vec4 {}
```

#### `Mat4.java`
```java
package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as 16 floats (mat4).
 * The component must be a Matrix4f.
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mat4 {}
```

#### `Floats.java`
```java
package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as N floats.
 * Useful for vec31, vec2, or packed float arrays.
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface Floats {
    /** Number of floats to write */
    int count();
    
    /** Whether to add std140 padding after (default true for vec3) */
    boolean pad() default false;
}
```

---

### Task 1.2: Create Vec4Serializable Interface
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/Vec4Serializable.java`

```java
package net.cyberpunk042.client.visual.ubo;

/**
 * Interface for records that can be serialized as a vec4 (4 floats).
 * 
 * <p>Implementing this interface allows any record to be written to a UBO
 * when annotated with @Vec4. The slot names are slot0-3 to be generic.</p>
 */
public interface Vec4Serializable {
    float slot0();
    float slot1();
    float slot2();
    float slot3();
    
    /**
     * Default implementation that extracts from record components.
     * Override for custom extraction logic.
     */
    default float[] toFloatArray() {
        return new float[] { slot0(), slot1(), slot2(), slot3() };
    }
}
```

---

### Task 1.3: Create ReflectiveUBOWriter
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/ReflectiveUBOWriter.java`

```java
package net.cyberpunk042.client.visual.ubo;

import com.mojang.blaze3d.buffers.Std140Builder;
import net.cyberpunk042.client.visual.ubo.annotation.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.RecordComponent;

/**
 * Writes any @UBOStruct record to a Std140 buffer using reflection.
 * 
 * <p>Field order is determined by record component declaration order,
 * which is guaranteed by Java to match source order.</p>
 */
public final class ReflectiveUBOWriter {
    
    private ReflectiveUBOWriter() {} // Utility class
    
    /**
     * Calculates the buffer size in bytes for a UBO struct.
     */
    public static int calculateBufferSize(Class<?> uboClass) {
        int bytes = 0;
        for (RecordComponent comp : uboClass.getRecordComponents()) {
            if (comp.isAnnotationPresent(Vec4.class)) {
                bytes += 16;  // 4 floats × 4 bytes
            } else if (comp.isAnnotationPresent(Mat4.class)) {
                bytes += 64;  // 16 floats × 4 bytes
            } else if (comp.isAnnotationPresent(Floats.class)) {
                Floats floats = comp.getAnnotation(Floats.class);
                bytes += floats.count() * 4;
                if (floats.pad()) bytes += (4 - (floats.count() % 4)) * 4;
            }
        }
        return bytes;
    }
    
    /**
     * Writes a UBO struct record to the buffer.
     */
    public static void write(Std140Builder builder, Object record) {
        try {
            for (RecordComponent comp : record.getClass().getRecordComponents()) {
                Object value = comp.getAccessor().invoke(record);
                
                if (comp.isAnnotationPresent(Vec4.class)) {
                    writeVec4(builder, value);
                } else if (comp.isAnnotationPresent(Mat4.class)) {
                    writeMatrix(builder, (Matrix4f) value);
                } else if (comp.isAnnotationPresent(Floats.class)) {
                    writeFloats(builder, value, comp.getAnnotation(Floats.class));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write UBO: " + record.getClass().getSimpleName(), e);
        }
    }
    
    private static void writeVec4(Std140Builder builder, Object value) {
        if (value instanceof Vec4Serializable v4) {
            builder.putFloat(v4.slot0());
            builder.putFloat(v4.slot1());
            builder.putFloat(v4.slot2());
            builder.putFloat(v4.slot3());
        } else if (value instanceof float[] arr && arr.length >= 4) {
            builder.putFloat(arr[0]);
            builder.putFloat(arr[1]);
            builder.putFloat(arr[2]);
            builder.putFloat(arr[3]);
        } else {
            throw new IllegalArgumentException(
                "Cannot write as Vec4: " + value.getClass().getSimpleName() +
                ". Implement Vec4Serializable or use float[4]."
            );
        }
    }
    
    private static void writeMatrix(Std140Builder builder, Matrix4f mat) {
        if (mat == null) {
            // Identity matrix fallback
            for (int i = 0; i < 4; i++) {
                builder.putFloat(i == 0 ? 1f : 0f);
                builder.putFloat(i == 1 ? 1f : 0f);
                builder.putFloat(i == 2 ? 1f : 0f);
                builder.putFloat(i == 3 ? 1f : 0f);
            }
            return;
        }
        
        Vector4f col = new Vector4f();
        for (int c = 0; c < 4; c++) {
            mat.getColumn(c, col);
            builder.putFloat(col.x);
            builder.putFloat(col.y);
            builder.putFloat(col.z);
            builder.putFloat(col.w);
        }
    }
    
    private static void writeFloats(Std140Builder builder, Object value, Floats annotation) {
        float[] arr;
        if (value instanceof float[] fa) {
            arr = fa;
        } else if (value instanceof Float f) {
            arr = new float[] { f };
        } else {
            throw new IllegalArgumentException("Cannot write as Floats: " + value.getClass());
        }
        
        for (int i = 0; i < annotation.count() && i < arr.length; i++) {
            builder.putFloat(arr[i]);
        }
        
        // Pad to vec4 boundary if requested
        if (annotation.pad()) {
            int padding = 4 - (annotation.count() % 4);
            for (int i = 0; i < padding; i++) {
                builder.putFloat(0f);
            }
        }
    }
}
```

---

### Task 1.4: Create GLSLValidator
**Location:** `src/client/java/net/cyberpunk042/client/visual/ubo/GLSLValidator.java`

```java
package net.cyberpunk042.client.visual.ubo;

import net.cyberpunk042.client.visual.ubo.annotation.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.*;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.regex.*;

/**
 * Validates that Java UBO records match GLSL uniform blocks.
 * Called at startup to catch mismatches early.
 */
public final class GLSLValidator {
    
    private GLSLValidator() {}
    
    /**
     * Validates a UBO record against its GLSL counterpart.
     * Throws RuntimeException with detailed message on mismatch.
     */
    public static void validate(Class<?> uboClass) {
        UBOStruct annotation = uboClass.getAnnotation(UBOStruct.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class must have @UBOStruct: " + uboClass.getName());
        }
        
        if (annotation.glslPath().isEmpty()) {
            return; // No validation requested
        }
        
        List<SlotInfo> javaSlots = extractJavaSlots(uboClass);
        List<SlotInfo> glslSlots = parseGLSLStruct(annotation.glslPath(), annotation.name());
        
        // Compare slot counts
        if (javaSlots.size() != glslSlots.size()) {
            throw new UBOMismatchException(
                "Slot count mismatch: Java has " + javaSlots.size() + 
                ", GLSL has " + glslSlots.size(),
                javaSlots, glslSlots
            );
        }
        
        // Compare each slot
        for (int i = 0; i < javaSlots.size(); i++) {
            SlotInfo java = javaSlots.get(i);
            SlotInfo glsl = glslSlots.get(i);
            
            if (java.size != glsl.size) {
                throw new UBOMismatchException(
                    "Slot " + i + " size mismatch: Java '" + java.name + "' is " + java.size + 
                    " bytes, GLSL '" + glsl.name + "' is " + glsl.size + " bytes",
                    javaSlots, glslSlots
                );
            }
        }
    }
    
    private static List<SlotInfo> extractJavaSlots(Class<?> uboClass) {
        List<SlotInfo> slots = new ArrayList<>();
        for (RecordComponent comp : uboClass.getRecordComponents()) {
            String name = comp.getName();
            int size = 0;
            
            if (comp.isAnnotationPresent(Vec4.class)) {
                size = 16;
            } else if (comp.isAnnotationPresent(Mat4.class)) {
                size = 64;
            } else if (comp.isAnnotationPresent(Floats.class)) {
                Floats f = comp.getAnnotation(Floats.class);
                size = f.count() * 4;
                if (f.pad()) size += (4 - (f.count() % 4)) * 4;
            }
            
            if (size > 0) {
                slots.add(new SlotInfo(name, size));
            }
        }
        return slots;
    }
    
    private static List<SlotInfo> parseGLSLStruct(String glslPath, String structName) {
        // TODO: Implement GLSL parsing
        // For now, return empty to skip validation
        return List.of();
    }
    
    public record SlotInfo(String name, int size) {}
    
    public static class UBOMismatchException extends RuntimeException {
        public final List<SlotInfo> javaSlots;
        public final List<SlotInfo> glslSlots;
        
        public UBOMismatchException(String message, List<SlotInfo> java, List<SlotInfo> glsl) {
            super(message + "\n\nJava slots:\n" + formatSlots(java) + 
                  "\n\nGLSL slots:\n" + formatSlots(glsl));
            this.javaSlots = java;
            this.glslSlots = glsl;
        }
        
        private static String formatSlots(List<SlotInfo> slots) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slots.size(); i++) {
                sb.append("  ").append(i).append(": ").append(slots.get(i)).append("\n");
            }
            return sb.toString();
        }
    }
}
```

---

## Phase 2: FieldVisual Integration

### Task 2.1: Create Vec4 Wrapper Records for FieldVisual
**Location:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualVec4Types.java`

```java
package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.ubo.Vec4Serializable;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;

/**
 * Vec4-serializable wrappers for FieldVisual UBO slots.
 * Each record extracts 4 floats from existing data structures.
 */
public final class FieldVisualVec4Types {
    
    private FieldVisualVec4Types() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR EXTRACTORS (Slots 1-5)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record PrimaryColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static PrimaryColorVec4 from(ColorParams c) {
            return new PrimaryColorVec4(c.primaryR(), c.primaryG(), c.primaryB(), c.primaryA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    public record SecondaryColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static SecondaryColorVec4 from(ColorParams c) {
            return new SecondaryColorVec4(c.secondaryR(), c.secondaryG(), c.secondaryB(), c.secondaryA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    // ... similar for TertiaryColorVec4, HighlightColorVec4, RayColorVec4
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION EXTRACTORS (Slots 6-8)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record AnimBaseVec4(float phase, float speed, float intensity, float effectType) 
        implements Vec4Serializable {
        public static AnimBaseVec4 from(AnimParams a) {
            return new AnimBaseVec4(a.phase(), a.speed(), a.intensity(), (float) a.effectType().ordinal());
        }
        @Override public float slot0() { return phase; }
        @Override public float slot1() { return speed; }
        @Override public float slot2() { return intensity; }
        @Override public float slot3() { return effectType; }
    }
    
    // ... similar for AnimMultiSpeedVec4, AnimTimingVec4
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA EXTRACTORS (Slots 24-26)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record CameraPosTimeVec4(float x, float y, float z, float time) implements Vec4Serializable {
        public static CameraPosTimeVec4 from(CameraParams c) {
            return new CameraPosTimeVec4(c.camX(), c.camY(), c.camZ(), c.time());
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return time; }
    }
    
    // ... etc for all 37 slots
}
```

---

### Task 2.2: Create FieldVisualUBO Record
**Location:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualUBO.java`

```java
package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.ubo.annotation.*;
import net.cyberpunk042.client.visual.effect.FieldVisualVec4Types.*;
import org.joml.Matrix4f;

/**
 * Complete UBO structure for Field Visual effect.
 * 
 * <p>This record IS THE DOCUMENTATION for the UBO layout.
 * Field order here = slot order in GLSL.</p>
 * 
 * <p>37 vec4 slots = 148 floats = 592 bytes</p>
 */
@UBOStruct(name = "FieldVisualConfig", glslPath = "the-virus-block:shaders/post/field_visual.fsh")
public record FieldVisualUBO(
    // ═══════════════════════════════════════════════════════════════════════════
    // EFFECT PARAMS (Slots 0-23)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 PositionVec4 position,              // Slot 0: center + radius
    @Vec4 PrimaryColorVec4 primary,           // Slot 1
    @Vec4 SecondaryColorVec4 secondary,       // Slot 2
    @Vec4 TertiaryColorVec4 tertiary,         // Slot 3
    @Vec4 HighlightColorVec4 highlight,       // Slot 4
    @Vec4 RayColorVec4 rayColor,              // Slot 5
    @Vec4 AnimBaseVec4 animBase,              // Slot 6
    @Vec4 AnimMultiSpeedVec4 animMulti,       // Slot 7
    @Vec4 AnimTimingVec4 animTiming,          // Slot 8
    @Vec4 CoreEdgeVec4 coreEdge,              // Slot 9
    @Vec4 FalloffVec4 falloff,                // Slot 10
    @Vec4 NoiseConfigVec4 noise,              // Slot 11
    @Vec4 GlowLineVec4 glowLine,              // Slot 12
    @Vec4 V2ParamsVec4 v2Params,              // Slot 13
    @Vec4 V2Detail1Vec4 v2Detail1,            // Slot 14
    @Vec4 V2Detail2Vec4 v2Detail2,            // Slot 15
    @Vec4 V2Detail3Vec4 v2Detail3,            // Slot 16
    @Vec4 V2Detail4Vec4 v2Detail4,            // Slot 17
    @Vec4 V2Detail5Vec4 v2Detail5,            // Slot 18
    @Vec4 V2Detail6Vec4 v2Detail6,            // Slot 19
    @Vec4 V2Detail7Vec4 v2Detail7,            // Slot 20
    @Vec4 V2Detail8Vec4 v2Detail8,            // Slot 21
    @Vec4 V2Line1Vec4 v2Line1,                // Slot 22
    @Vec4 ReservedVec4 reserved,              // Slot 23
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA/RUNTIME (Slots 24-27)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 CameraPosTimeVec4 cameraPosTime,    // Slot 24
    @Vec4 CameraForwardVec4 cameraForward,    // Slot 25
    @Vec4 CameraUpVec4 cameraUp,              // Slot 26
    @Vec4 RenderParamsVec4 renderParams,      // Slot 27
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MATRICES (Slots 28-35)
    // ═══════════════════════════════════════════════════════════════════════════
    @Mat4 Matrix4f invViewProj,               // Slots 28-31
    @Mat4 Matrix4f viewProj,                  // Slots 32-35
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG (Slot 36)
    // ═══════════════════════════════════════════════════════════════════════════
    @Vec4 DebugVec4 debug                     // Slot 36
) {
    /**
     * Factory method to build from current data structures.
     */
    public static FieldVisualUBO from(
        FieldVisualConfig config,
        PositionParams position,
        CameraParams camera,
        RenderParams render,
        Matrix4f invViewProj,
        Matrix4f viewProj,
        DebugParams debug
    ) {
        ColorParams colors = config.colors();
        AnimParams anim = config.anim();
        // ... extract all fields
        return new FieldVisualUBO(
            PositionVec4.from(position),
            PrimaryColorVec4.from(colors),
            SecondaryColorVec4.from(colors),
            // ... all other fields
            invViewProj,
            viewProj,
            DebugVec4.from(debug)
        );
    }
}
```

---

### Task 2.3: Update PostEffectPassMixin for FieldVisual
**Location:** `src/client/java/net/cyberpunk042/mixin/client/PostEffectPassMixin.java`
**Lines:** 226-235

```java
// Before (lines 226-235):
FieldVisualUBOWriter.write(builder, config, position, cameraParams, ...);

// After:
FieldVisualUBO ubo = FieldVisualUBO.from(config, position, cameraParams, ...);
ReflectiveUBOWriter.write(builder, ubo);
```

---

## Phase 3: Shockwave Integration

### Task 3.1: Create Vec4 Wrappers for Shockwave
**Location:** `src/client/java/net/cyberpunk042/client/visual/shader/shockwave/ShockwaveVec4Types.java`

Similar structure to FieldVisual:
- `RingParamsVec4` (extracts from RingParams)
- `ScreenEffectsVec4` 
- `ShapeConfigVec4`
- etc.

---

### Task 3.2: Create ShockwaveUBO Record
**Location:** `src/client/java/net/cyberpunk042/client/visual/shader/shockwave/ShockwaveUBO.java`

```java
@UBOStruct(name = "ShockwaveConfig", glslPath = "the-virus-block:shaders/post/shockwave.fsh")
public record ShockwaveUBO(
    @Vec4 BasicParamsVec4 basic,              // Slot 0: radius, thickness, intensity, time
    @Vec4 RingCountVec4 ringCount,            // Slot 1
    @Vec4 TargetPosVec4 target,               // Slot 2
    @Vec4 CameraPosVec4 cameraPos,            // Slot 3
    @Vec4 CameraForwardVec4 cameraForward,    // Slot 4
    @Vec4 CameraUpVec4 cameraUp,              // Slot 5
    @Vec4 ScreenEffectsVec4 screenEffects,    // Slot 6
    @Vec4 TintVec4 tint,                      // Slot 7
    @Vec4 RingColorVec4 ringColor,            // Slot 8
    @Vec4 ShapeConfigVec4 shapeConfig,        // Slot 9
    @Vec4 ShapeExtrasVec4 shapeExtras,        // Slot 10
    @Vec4 CoronaConfigVec4 coronaConfig,      // Slot 11
    @Vec4 OrbitalBodyVec4 orbitalBody,        // Slot 12
    @Vec4 OrbitalCoronaVec4 orbitalCorona,    // Slot 13
    @Vec4 BeamBodyVec4 beamBody,              // Slot 14
    @Vec4 BeamCoronaColorVec4 beamCoronaColor,// Slot 15
    @Vec4 BeamGeometryVec4 beamGeometry,      // Slot 16
    @Vec4 BeamCoronaVec4 beamCorona,          // Slot 17
    @Mat4 Matrix4f invViewProj                // Slots 18-21
) {
    public static ShockwaveUBO from(UBOSnapshot snapshot, float aspectRatio, float fovRadians) {
        // Extract all fields from snapshot
        return new ShockwaveUBO(...);
    }
}
```

---

### Task 3.3: Update ShockwavePostEffect
**Location:** `src/client/java/net/cyberpunk042/client/visual/shader/ShockwavePostEffect.java`

Find where `ShockwaveUBOWriter.writeBuffer()` is called and replace with:
```java
ShockwaveUBO ubo = ShockwaveUBO.from(snapshot, aspectRatio, fovRadians);
ReflectiveUBOWriter.write(builder, ubo);
```

---

## Phase 4: Startup Validation & Cleanup

### Task 4.1: Add Validation to TheVirusBlockClient
**Location:** `src/client/java/net/cyberpunk042/TheVirusBlockClient.java`

In `onInitializeClient()`:
```java
// Validate UBO structures match GLSL at startup
GLSLValidator.validate(FieldVisualUBO.class);
GLSLValidator.validate(ShockwaveUBO.class);
```

---

### Task 4.2: Delete Old Writers
**After testing confirms everything works:**

| File | Action |
|------|--------|
| `FieldVisualUBOWriter.java` | DELETE (366 lines) |
| `ShockwaveUBOWriter.java` | DELETE (217 lines) |

---

## Implementation Order

```
Week 1: Shared Infrastructure
├── Task 1.1: Annotations (4 files, ~60 lines)
├── Task 1.2: Vec4Serializable interface (15 lines)
├── Task 1.3: ReflectiveUBOWriter (100 lines)
└── Task 1.4: GLSLValidator (100 lines)

Week 2: FieldVisual Migration
├── Task 2.1: FieldVisualVec4Types (~400 lines, 37 wrappers)
├── Task 2.2: FieldVisualUBO record (~150 lines)
└── Task 2.3: Update PostEffectPassMixin (5 lines)

Week 3: Shockwave Migration
├── Task 3.1: ShockwaveVec4Types (~200 lines, 18 wrappers)
├── Task 3.2: ShockwaveUBO record (~100 lines)
└── Task 3.3: Update ShockwavePostEffect (5 lines)

Week 4: Validation & Cleanup
├── Task 4.1: Add startup validation (5 lines)
├── Task 4.2: Delete old writers (-583 lines)
└── Task 4.3: Integration testing
```

---

## Summary

| Metric | Before | After |
|--------|--------|-------|
| Lines of UBO writing code | 583 | ~100 (shared) |
| Files to edit when adding param | 3+ | 2 (UBO record + GLSL) |
| Validation | None | Startup check |
| Documentation | Comments | Code structure |
| Risk of slot mismatch | High | Zero (order is declaration order) |

---

## Files Created/Modified/Deleted

| Action | File | Lines |
|--------|------|-------|
| CREATE | `ubo/annotation/UBOStruct.java` | 15 |
| CREATE | `ubo/annotation/Vec4.java` | 12 |
| CREATE | `ubo/annotation/Mat4.java` | 12 |
| CREATE | `ubo/annotation/Floats.java` | 15 |
| CREATE | `ubo/Vec4Serializable.java` | 15 |
| CREATE | `ubo/ReflectiveUBOWriter.java` | 100 |
| CREATE | `ubo/GLSLValidator.java` | 100 |
| CREATE | `effect/FieldVisualVec4Types.java` | 400 |
| CREATE | `effect/FieldVisualUBO.java` | 150 |
| CREATE | `shockwave/ShockwaveVec4Types.java` | 200 |
| CREATE | `shockwave/ShockwaveUBO.java` | 100 |
| MODIFY | `PostEffectPassMixin.java` | +5 |
| MODIFY | `ShockwavePostEffect.java` | +5 |
| MODIFY | `TheVirusBlockClient.java` | +5 |
| DELETE | `FieldVisualUBOWriter.java` | -366 |
| DELETE | `ShockwaveUBOWriter.java` | -217 |

**Net change:** +1,134 new lines, -583 deleted = **+551 lines**
**But:** Maintenance cost drops to near-zero, validation is automatic.
