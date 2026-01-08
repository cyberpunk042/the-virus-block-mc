# Astrophysical Visual Effects Implementation Plan

**Status**: ✅ PHASE 2 COMPLETE  
**Last Updated**: 2024-12-24

## Overview

This plan covers the implementation of astrophysical visual effects for cosmic phenomena:
- ✅ Phase 1: Relativistic Jets (COMPLETE)
- ✅ Phase 2: Rays Shape (COMPLETE - straight line rays with fade support)
- 🔲 Phase 3: Radial Rays Shape (triangular/wedge rays - future)
- 🔲 Phase 4: Presets and Compositions (future)

---

## Phase 1: Relativistic Jets ✅ COMPLETE

### Summary
- **JetShape** - Dual opposing cones/tubes
- **JetTessellator** - Generates geometry
- **JetRenderer** - Renders jets
- Supports: hollow/solid, dual/single, caps, segments

---

## Phase 2: Rays Shape (Current Focus)

### Concept

A **RaysShape** is a collection of straight LINE segments in 3D space. Unlike jets which are 3D volumes (cones/cylinders), rays are **pure lines** rendered with configurable thickness.

Visual examples:
- Laser beams in parallel
- Light rays converging/diverging from a point
- Energy pulses
- Particle trails

### 2.1 RaysShape Record

```java
record RaysShape(
    // === Individual Ray Geometry ===
    float rayLength,           // Length of each ray (default: 2.0)
    float rayWidth,            // Line thickness/width (default: 1.0)
    
    // === Distribution & Count ===
    int count,                 // Number of rays (default: 12)
    RayArrangement arrangement, // How rays are arranged (default: RADIAL)
    float innerRadius,         // Where rays start from center (default: 0.5)
    float outerRadius,         // Where rays end from center (default: 3.0) 
    
    // === Multi-Layer Support ===
    int layers,                // Number of vertical layers (default: 1)
    float layerSpacing,        // Vertical spacing between layers (default: 0.5)
    
    // === Randomness ===
    float randomness,          // Positional randomness 0-1 (default: 0)
    float lengthVariation,     // Random length variation 0-1 (default: 0)
    
    // === Fading (per-ray, not per-shape) ===
    float fadeStart,           // Alpha at ray start 0-1 (default: 1.0 = solid)
    float fadeEnd,             // Alpha at ray end 0-1 (default: 1.0 = solid)
    
    // === Segmentation (for dashed/dotted effects) ===
    int segments,              // Segments per ray (default: 1)
    float segmentGap           // Gap between segments as fraction of length (default: 0)
) implements Shape
```

### 2.2 RayArrangement Enum

```java
enum RayArrangement {
    RADIAL,         // Rays emanating from center outward (2D star on XZ plane)
    SPHERICAL,      // Rays emanating in all 3D directions from center
    PARALLEL,       // All rays pointing same direction (parallel beams)
    CONVERGING,     // All rays pointing toward center point (absorption)
    DIVERGING       // All rays pointing away from center (emission)
}
```

**Visual Reference:**

```
RADIAL (XZ plane):
         ↑
        /|\
       / | \
    ←─●─→   
       \ | /
        \|/
         ↓

SPHERICAL (3D):
         ↑
       ↗ | ↖
      ←──●──→  
       ↙ | ↘
         ↓

PARALLEL:
    →  →  →  →
    →  →  →  →
    →  →  →  →

CONVERGING (inward):
    ↘   ↓   ↙
        ●     
    ↗   ↑   ↖

DIVERGING (outward):
    ↗   ↑   ↖
        ●     
    ↘   ↓   ↙
```

### 2.3 Color Integration

Rays use the existing Appearance color system:

| ColorMode | Effect |
|-----------|--------|
| GRADIENT | Uniform blend from primary to secondary |
| MESH_GRADIENT | Each ray fades from primary (start) to secondary (end) |
| MESH_RAINBOW | Each ray is a different color from spectrum |
| RANDOM | Each ray gets random color from ColorSet |

**ColorDistribution** applies:
- **GRADIENT**: Smooth gradient along each ray
- **INDEXED**: Each ray gets a color based on its index
- **RANDOM**: Each ray gets random color

### 2.4 FadeStart/FadeEnd vs Appearance Alpha

| Type | Purpose |
|------|---------|
| **Appearance Alpha** | Uniform transparency for entire shape |
| **FadeStart/FadeEnd** | Per-ray gradient - ray can fade from solid to transparent |

Example: 
- `fadeStart=1.0, fadeEnd=0.0` → Rays fade out at their ends
- `fadeStart=0.0, fadeEnd=1.0` → Rays fade in at their ends
- Combined with `alpha=0.5` → Entire ray is 50% transparent AND fades

### 2.5 Files to Create

| File | Action |
|------|--------|
| `RaysShape.java` | CREATE - Shape record |
| `RayArrangement.java` | CREATE - Arrangement enum |
| `RaysTessellator.java` | CREATE - Line geometry generation |
| `RaysRenderer.java` | CREATE - Rendering logic |
| `ShapeRegistry.java` | MODIFY - Register shape |
| `PrimitiveRenderers.java` | MODIFY - Register renderer |
| `ShapeTypeAdapter.java` | MODIFY - JSON parsing |
| `ShapeSubPanel.java` | MODIFY - GUI controls |

### 2.6 Example Configurations

**Black Hole Absorption (converging rays):**
```json
{
  "shape": {
    "type": "rays",
    "count": 48,
    "arrangement": "SPHERICAL",
    "layers": 8,
    "innerRadius": 0.8,
    "outerRadius": 4.0,
    "fadeStart": 1.0,
    "fadeEnd": 0.2
  },
  "appearance": {
    "color": "#FF6600",
    "colorMode": "MESH_GRADIENT",
    "direction": "ALONG_LENGTH"
  }
}
```

**Parallel Laser Grid:**
```json
{
  "shape": {
    "type": "rays",
    "count": 16,
    "arrangement": "PARALLEL",
    "rayLength": 5.0,
    "layers": 4,
    "layerSpacing": 0.3
  },
  "appearance": {
    "colorMode": "MESH_RAINBOW",
    "colorSet": "NEON"
  }
}
```

**Dashed Pulse Rays:**
```json
{
  "shape": {
    "type": "rays",
    "count": 12,
    "arrangement": "RADIAL",
    "segments": 4,
    "segmentGap": 0.2
  }
}
```

---

## Phase 3: Radial Rays Shape (Future)

### Concept

After RaysShape is complete, we may add a more specialized **RadialRaysShape** for triangular/wedge-shaped rays (like sun rays or light beams with width that expands).

**Key Differences from RaysShape:**
- Rays are TRIANGULAR (not lines) - they have width that can vary
- `startWidth` and `endWidth` parameters
- Creates wedge/pie-slice geometry instead of lines

**This is NOT the current priority.** RaysShape with simple lines covers most use cases. RadialRays would add visual variety for sun-beam effects.

---

## Phase 4: Presets and Compositions (Future)

Once RaysShape is complete:
1. Create black_hole.json preset (sphere + jets + rays)
2. Create pulsar.json preset
3. Study inter-primitive relationships
4. Animation synchronization patterns

---

## Resolved Design Decisions

| Question | Decision |
|----------|----------|
| Jet geometry | Cylinder-based (tipRadius controls shape) ✅ |
| Rays vs RadialRays | Start with simple line Rays, triangular RadialRays later |
| Ray color | Use existing ColorMode/ColorSet system |
| Ray fade | Separate from Appearance alpha (fadeStart/fadeEnd) |
| Arrangements | Support all 5: RADIAL, SPHERICAL, PARALLEL, CONVERGING, DIVERGING |

---

## Implementation Order

### Current: Phase 2 - RaysShape
1. Create `RaysShape.java` record
2. Create `RayArrangement.java` enum  
3. Create `RaysTessellator.java` (line generation)
4. Create `RaysRenderer.java`
5. Register in ShapeRegistry, PrimitiveRenderers
6. Add JSON parsing
7. Test all arrangement modes
8. Add GUI controls

---

## Notes

- Ring shape working with CellType.QUAD ✅
- DiscShape removed ✅
- JetShape complete ✅
- ColorContext now includes alpha ✅
- Per-vertex colors (MESH_GRADIENT, MESH_RAINBOW, RANDOM) now respect alpha ✅
