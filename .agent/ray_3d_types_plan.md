# 3D Ray Types Implementation Plan

## Overview
This plan details the methodical approach for implementing 3D ray types (DROPLET, CONE, ARROW, etc.) with orientation support, code reuse, and clean DevOps.

---

## ✅ COMPLETED - Phase 1: Foundation (Orientation System)

### ✅ Step 1.1: Created RayOrientation Enum
**File:** `src/main/java/net/cyberpunk042/visual/shape/RayOrientation.java`
- ALONG_RAY, AGAINST_RAY, OUTWARD, INWARD, UPWARD, DOWNWARD, TANGENT
- Helper methods: requiresCenterReference(), isFixedDirection()

### ✅ Step 1.2: Added to RaysShape  
**File:** `src/main/java/net/cyberpunk042/visual/shape/RaysShape.java`
- Added `rayOrientation` field with ALONG_RAY default
- Updated all presets, Builder, toBuilder()
- Added effectiveRayOrientation() helper

### ✅ Step 1.3: Updated RayContext
**File:** `src/client/.../mesh/ray/RayContext.java`
- Added `orientation` and `orientationVector` fields
- Updated Builder and build()

### ✅ Step 1.4: Updated RayPositioner
**File:** `src/client/.../mesh/ray/RayPositioner.java`
- Added `computeOrientationVector()` method
- Computes orientation based on mode and ray position
- OUTWARD: direction from center to ray start
- INWARD: direction from ray start to center
- UPWARD: [0, 1, 0]
- DOWNWARD: [0, -1, 0]
- ALONG_RAY: ray direction
- AGAINST_RAY: negative ray direction

---

## Phase 2: Shared Utilities

### Step 2.1: Create Ray3DGeometryUtils
**File:** `src/client/.../mesh/ray/Ray3DGeometryUtils.java`

Extract/create common 3D geometry functions from existing tessellators:

```java
public final class Ray3DGeometryUtils {
    
    // === Hemisphere Generation ===
    // Reuse from SphereTessellator
    static void generateHemisphere(MeshBuilder b, float[] center, float radius, 
                                    float[] direction, int rings, int segments,
                                    boolean reversed);
    
    // === Cone/Tapered Cylinder Generation ===
    // Reuse from ConeTessellator
    static void generateTaperedCylinder(MeshBuilder b, float[] start, float[] end,
                                         float startRadius, float endRadius, int segments);
    
    // === Cap Generation ===
    static void generateCircularCap(MeshBuilder b, float[] center, float[] normal,
                                     float radius, int segments, boolean flipWinding);
    
    // === Droplet/Teardrop Profile ===
    static void generateDropletProfile(MeshBuilder b, float[] start, float[] end,
                                        float baseRadius, float taperPower, 
                                        int rings, int segments);
    
    // === Arrow Head ===
    static void generateArrowHead(MeshBuilder b, float[] position, float[] direction,
                                   float width, float length, int segments);
    
    // === Orientation Transform ===
    static float[] computeOrientedDirection(RayContext ctx);
    static float[] rotatePointAroundAxis(float[] point, float[] axis, float angle);
}
```

---

## Phase 3: Implement DROPLET Type

### Step 3.1: RayDropletTessellator
**File:** `src/client/.../mesh/ray/RayDropletTessellator.java`

```java
public class RayDropletTessellator implements RayTypeTessellator {
    
    public static final RayDropletTessellator INSTANCE = new RayDropletTessellator();
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context) {
        // 1. Get position and oriented direction
        float[] start = context.start();
        float[] orientedDir = Ray3DGeometryUtils.computeOrientedDirection(context);
        
        // 2. Compute droplet parameters from shape
        float baseRadius = shape.rayWidth() * 0.5f;  // or new field: dropletBaseRadius
        float length = context.length();
        int rings = Math.max(4, shape.effectiveShapeSegments() / 4);
        int segments = 8;  // or configurable
        
        // 3. Generate droplet geometry
        // - Fat end (hemisphere or bulb) at base
        // - Tapers to point at tip
        Ray3DGeometryUtils.generateDropletProfile(builder, start, 
            add(start, scale(orientedDir, length)), baseRadius, 2.0f, rings, segments);
    }
}
```

### Step 3.2: Register in RayTypeTessellatorRegistry
```java
TESSELLATORS.put(RayType.DROPLET, RayDropletTessellator.INSTANCE);
```

### Step 3.3: RaysShape New Fields
For DROPLET type:
- `dropletBaseRadius` (0.05 - 1.0) - radius of fat end
- `dropletTaper` (1.0 - 4.0) - how quickly it tapers (higher = sharper point)

---

## Phase 4: Implement Remaining Types (After DROPLET Works)

### Priority Order
1. **CONE** - Similar to DROPLET, uses generateTaperedCylinder
2. **ARROW** - LINE + arrowhead at tip
3. **CAPSULE** - Cylinder with hemisphere caps (reuse from CapsuleTessellator)
4. **KAMEHAMEHA** - Cylinder + sphere bulb at end
5. **LASER** - Thin cylinder with glow (needs shader work)
6. **BEADS/CUBES/STARS** - Instanced small shapes along path

---

## Phase 5: GUI Integration

### Step 5.1: ShapeSubPanel Updates
- Add RayType dropdown (already planned)
- Show RayOrientation dropdown when rayType.is3D()
- Show type-specific fields conditionally

### Step 5.2: Type-Specific Field Visibility
```java
if (rayType == RayType.DROPLET) {
    // Show: dropletBaseRadius, dropletTaper
}
if (rayType == RayType.CONE) {
    // Show: coneSegments, coneBaseRadius
}
// etc.
```

---

## Reuse Analysis

| Existing Tessellator | What to Extract | For Which Ray Types |
|---------------------|------------------|---------------------|
| SphereTessellator | Hemisphere generation, UV vertex layout | DROPLET, CAPSULE, KAMEHAMEHA |
| ConeTessellator | Tapered cylinder, cap generation | DROPLET, CONE, ARROW |
| CapsuleTessellator | Hemisphere + cylinder combo | CAPSULE |
| CylinderTessellator | Ring generation, body segments | LASER, BEADS |

---

## Next Immediate Steps

1. ✅ Create this plan document
2. ⬜ Create `RayOrientation` enum
3. ⬜ Add `rayOrientation` to RaysShape
4. ⬜ Update RayContext with orientation data
5. ⬜ Create `Ray3DGeometryUtils` with extracted utilities
6. ⬜ Implement `RayDropletTessellator`
7. ⬜ Test DROPLET rendering in-game
8. ⬜ Add GUI controls for DROPLET

---

## DevOps Notes

- **No code duplication** - All geometry generation goes through shared utilities
- **Incremental testing** - Test each type before moving to next
- **Type-specific parameters** - Add to RaysShape as needed, not all at once
- **Backward compatibility** - Default values ensure existing presets work
