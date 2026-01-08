# Ray Types Implementation Plan

## Overview
Add a RayType selector to allow rays to be rendered as different shapes (lines, droplets, energy beams, etc.) instead of only flat ribbon lines.

---

## ✅ COMPLETED - Phase 1: Foundation

### ✅ Step 1.1: RayType Enum
**File:** `src/main/java/net/cyberpunk042/visual/shape/RayType.java`
- 17 ray types in 4 categories (Basic, Energy, Particle, Organic)
- Category enum for UI grouping
- Helper methods: `is3D()`, `isProcedural()`, `suggestedMinSegments()`

### ✅ Step 1.2: RaysShape Updated
**File:** `src/main/java/net/cyberpunk042/visual/shape/RaysShape.java`
- Added `RayType rayType` field to record
- Updated all presets (DEFAULT, ABSORPTION, etc.)
- Updated Builder with `rayType()` setter
- Added `effectiveRayType()` helper method

---

## ✅ COMPLETED - Phase 2: Infrastructure

### ✅ Step 2.1: RayContext Record
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayContext.java`
- Data container for computed ray position data
- Fields: start, end, direction, width, fade, lineShape, curvature, wave, etc.
- Builder with `computeDirectionAndLength()` helper

### ✅ Step 2.2: RayPositioner Utility
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayPositioner.java`
- Extracted positioning logic from RaysTessellator
- `computeContext()` - main API
- `computeDistribution()` - UNIFORM, RANDOM, STOCHASTIC
- `computePosition()` - RADIAL, SPHERICAL, CONVERGING, PARALLEL

### ✅ Step 2.3: RayTypeTessellator Interface
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayTypeTessellator.java`
- `tessellate(builder, shape, context)` - main method
- `name()`, `isProcedural()` - helper methods

### ✅ Step 2.4: RayLineTessellator
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayLineTessellator.java`
- Default LINE type implementation
- Supports: simple line, segmented, shaped, combined

### ✅ Step 2.5: RayTypeTessellatorRegistry
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayTypeTessellatorRegistry.java`
- Maps RayType → RayTypeTessellator
- Fallback to LINE if not registered

### ✅ Step 2.6: RayGeometryUtils
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayGeometryUtils.java`
- Common geometry utilities for all tessellators
- `interpolate()`, `computeDirectionAndLength()`
- `computePerpendicularFrame()` - right/up vectors
- `computeLineShapeOffset()` - sine wave, corkscrew, zigzag, etc.
- `computeCurvedPosition()` - vortex, spiral curvature
- Vector math: `normalize()`, `dot()`, `cross()`, `scale()`, `add()`

---

## ✅ COMPLETED - Phase 3: Integration

### ✅ Step 3.1: RaysTessellator Refactored
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/RaysTessellator.java`
- Reduced from 847 lines to ~95 lines (90% reduction!)
- Uses RayPositioner.computeContext() for position calculation
- Delegates to RayTypeTessellatorRegistry.get(rayType) for geometry
- All duplicate code extracted to shared utilities

### ✅ Step 3.2: Bug Fixes
- Fixed `effectiveShapeSegments()` to respect user's shapeSegments for straight lines
- Made shapeSegments slider always visible in UI (needed for travel animations)
- Increased shapeSegments slider max from 128 to 256

---

## 📋 TODO - Phase 4: Type Implementations
- [ ] Test in-game that rays render correctly
- [ ] Verify animations still work

---

## 📋 TODO - Phase 4: Type Implementations

### Tier 1: Basic Geometry (Priority: High)
| Type | Status | Complexity |
|------|--------|------------|
| `DROPLET` | ⬜ TODO | Medium |
| `CONE` | ⬜ TODO | Medium |
| `ARROW` | ⬜ TODO | Medium |
| `CAPSULE` | ⬜ TODO | Medium |

### Tier 2: Energy/Effect (Priority: Medium)
| Type | Status | Complexity |
|------|--------|------------|
| `KAMEHAMEHA` | ⬜ TODO | Medium |
| `LASER` | ⬜ TODO | Low |
| `LIGHTNING` | ⬜ TODO | High |
| `FIRE_JET` | ⬜ TODO | High |
| `PLASMA` | ⬜ TODO | High |

### Tier 3: Particle/Object (Priority: Low)
| Type | Status | Complexity |
|------|--------|------------|
| `BEADS` | ⬜ TODO | Medium |
| `CUBES` | ⬜ TODO | Medium |
| `STARS` | ⬜ TODO | Medium |
| `CRYSTALS` | ⬜ TODO | High |

### Tier 4: Organic (Priority: Low)
| Type | Status | Complexity |
|------|--------|------------|
| `TENDRIL` | ⬜ TODO | High |
| `SPINE` | ⬜ TODO | High |
| `ROOT` | ⬜ TODO | Very High |

---

## 📋 TODO - Phase 5: GUI Integration

### Step 5.1: Add RayType Dropdown
**File:** `src/client/java/net/cyberpunk042/client/gui/panel/sub/ShapeSubPanel.java`
- Add dropdown selector for RayType
- Show type description in info text

### Step 5.2: Conditional Type-Specific Fields
- Show/hide fields based on selected RayType
- Each type has its own parameter sliders

### Step 5.3: ShapeAdapter Updates
**File:** `src/client/java/net/cyberpunk042/client/gui/state/adapter/ShapeAdapter.java`
- Add binding for rayType field
- Add bindings for type-specific parameters

---

## 📋 TODO - Phase 6: Type-Specific Parameters

Add these fields to RaysShape when implementing each type:

### DROPLET
- `dropletHeadRadius` (0.1 - 1.0)
- `dropletTaper` (0.0 - 1.0)

### CONE
- `coneSegments` (4 - 32)
- `coneBaseRadius` (0.1 - 1.0)

### ARROW
- `arrowHeadWidth` (0.1 - 1.0)
- `arrowHeadLength` (0.1 - 0.5)
- `arrowShaftWidth` (0.05 - 0.3)

### KAMEHAMEHA
- `kameBeamWidth` (0.1 - 1.0)
- `kameEndBulbSize` (0.1 - 2.0)
- `kameGlowIntensity` (0.0 - 3.0)

*(More parameters defined as needed per type)*

---

## 🔧 Utility Classes Created

| File | Purpose |
|------|---------|
| `ray/RayContext.java` | Computed position data container |
| `ray/RayPositioner.java` | Position calculation utility |
| `ray/RayTypeTessellator.java` | Tessellator interface |
| `ray/RayLineTessellator.java` | LINE type tessellator |
| `ray/RayTypeTessellatorRegistry.java` | Type → Tessellator mapping |

---

## Next Immediate Steps

1. **Update RaysTessellator** - Wire up the new infrastructure
2. **Verify build compiles** - Ensure no breaking changes
3. **Test in-game** - Confirm LINE type still works
4. **Implement DROPLET** - First 3D type as proof of concept
5. **Add GUI controls** - RayType dropdown in ShapeSubPanel
