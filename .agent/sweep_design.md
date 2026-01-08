# Sweep # Feature Design Document

## Current RADIATE Animation Behavior (Sweep = 1, Normal)

### How it works step-by-step:

1. **Each ray has an angular position** around the circle: `rayAngle = idx / rayCount` (0 to 1 = 0° to 360°)

2. **Each ray gets a phase based on its angle**:
   ```
   scaledAngle = rayAngle * waveArc
   rayLengthPhase = (baseLengthPhase + scaledAngle) % 1.0
   ```
   - `baseLengthPhase` is the animation time (0→1 cycles repeatedly)
   - Different rays have different phases based on their angular position

3. **The phase determines WHERE on the ray the visible segment is**:
   ```
   travelRange = 1.0 + segmentLength  (e.g., 1.3 for segLen=0.3)
   windowCenter = rayLengthPhase * travelRange
   windowStart = windowCenter - segmentLength/2
   windowEnd = windowCenter + segmentLength/2
   ```

4. **A ray's segment is visible when the window overlaps [0,1]**:
   - Phase 0: window = [-0.15, +0.15] → partially visible (entering)
   - Phase 0.5: window = [0.5, 0.8] → fully visible (middle)
   - Phase 0.85: window = [0.95, 1.25] → partially visible (exiting)
   - Phase 1.0: window = [1.15, 1.45] → invisible (passed)

5. **Visual result**: A "wedge" of visible rays sweeps around the circle. The wedge size depends on how long each ray stays in the visible phase range (~85% of the cycle for segLen=0.3).

---

## TRIM (Sweep < 1) - What it SHOULD do:

**Goal**: Reduce the angular coverage of the sweep. sweep=0.5 means only a 180° wedge ever becomes visible.

**Visual**: Same single sweeping wedge, but SMALLER. Rays outside the trim arc are always hidden.

### Implementation approach:

1. The TRIM arc is centered at the **current sweep position** (same as the animation wedge center)
2. Rays OUTSIDE this arc are completely hidden (skip rendering)
3. Rays INSIDE the arc animate normally

**Key insight**: The sweep position is where the "leading edge" of the visible wedge is. For RADIATE, this is where rays are just starting to show their segment (phase ~0).

The "sweep center" should be: rays with phase near 0 (segment just entering visibility).

**WRONG approach** (what I've been doing): Using `baseLengthPhase` as arc center. This doesn't match the actual visible wedge position.

**CORRECT approach**: 
- Find which rays are currently in the "visible phase range" (segment overlaps [0,1])
- The arc should trim based on whether the ray WOULD be visible, not based on angular distance to baseLengthPhase

Actually, the SIMPLEST approach for TRIM:
- Don't change the visibility logic at all
- Just MULTIPLY the effective angle coverage: `effectiveRayAngle = rayAngle * sweepCopies`
- This compresses the full sweep into a smaller arc

Wait, that's SPLITTING not TRIMMING. Let me think again.

For TRIM, we want:
- 50% of the CIRCLE is covered, not 50% of the TIME
- At sweep=0.5, rays from 0° to 180° participate in the sweep
- Rays from 180° to 360° are always hidden

**Implementation**:
```java
if (sweepCopies < 1.0f) {
    // Only rays in the first (sweepCopies * 100)% of the circle participate
    if (rayAngle > sweepCopies) {
        rayVisible = false;  // This ray is outside the active zone
    }
}
```

This is STATIC trimming - always the same rays hidden. But did the user want ROTATING trimming?

Let me re-read: "reduce the actual azimuth coverage" - this suggests the VISIBLE WEDGE at any instant should be smaller, not that specific rays are always hidden.

**Alternative interpretation**: 
- sweep=1: At any instant, ~30% of rays have visible segments (for segLen=0.3)
- sweep=0.5: At any instant, only ~15% of rays have visible segments

This would mean: make the visible WEDGE smaller, so fewer rays are lit at once.

To achieve this, we need to change when rays are considered "visible" in the RADIATE window check.

---

## DUPLICATE (Sweep > 1) - What it SHOULD do:

**Goal**: Create N identical copies of the sweep wedge, evenly spaced around the circle.

**Visual**: 
- sweep=2: TWO wedges, 180° apart, sweeping together
- sweep=3: THREE wedges, 120° apart
- sweep=5: FIVE wedges, 72° apart (at sweep=5, they might overlap = full circle lit)

### Implementation approach:

For each ray, check if it's visible in ANY of the N copies.

The copies are at angular positions: 0, 1/N, 2/N, ... (N-1)/N around the circle.

**Key insight**: In the original animation, a ray at angle A is visible when rays near A have phases that put their windows in [0,1].

For duplication, we want:
- Ray at angle A visible if original animation shows A visible
- ALSO visible if original animation shows (A - 1/N) visible
- ALSO visible if original animation shows (A - 2/N) visible
- etc.

**Implementation**:
```java
boolean rayVisible = false;
for (int c = 0; c < numCopies; c++) {
    float copyOffset = (float) c / numCopies;
    // Check if a ray at angle (rayAngle - copyOffset) would be visible
    float equivalentAngle = (rayAngle - copyOffset + 1.0f) % 1.0f;
    float equivalentPhase = (baseLengthPhase + equivalentAngle * waveArc) % 1.0f;
    
    // Calculate window for this equivalent ray
    float windowCenter = equivalentPhase * travelRange;
    float windowStart = windowCenter - segmentLength/2;
    float windowEnd = windowCenter + segmentLength/2;
    
    if (windowEnd >= 0 && windowStart <= 1) {
        rayVisible = true;
        // Use this window for rendering
        break;
    }
}
```

---

## Summary of Correct Implementation:

### TRIM (sweep < 1):
Option A (Static): Hide rays whose angle > sweepCopies
Option B (Dynamic): Scale the visible window width by sweepCopies

### DUPLICATE (sweep > 1):
Check if ray would be visible at any of N rotated positions.
If visible at copy c, use the window calculated for the ray at angle (rayAngle - c/N).

---

## Questions to clarify:

1. **TRIM**: Should it be STATIC (always same rays hidden) or ROTATING (trim arc follows the sweep)?

2. **DUPLICATE**: When a ray is visible in copy c, should it show:
   a) The same segment position as the original ray at (rayAngle - c/N), OR
   b) A segment position based on this ray's own phase?

---

## Implementation Status: ✅ COMPLETE

**Implemented in `RayPositioner.computeFlowAnimation()`:**

### TRIM (sweep < 1):
- Scales down the visible window proportionally
- `effectiveWindowHalf = (segmentLength / 2) * sweepCopies`
- Results in smaller visible wedge at any instant

### DUPLICATE (sweep > 1):
- Checks N copies (where N = floor(sweepCopies))
- Each copy is offset by (c/N) around the circle
- If ANY copy would be visible, the ray is shown
- Uses the first matching copy's window for position calculation

### Visibility:
- Rays outside all visible windows get `alpha = 0`
- `RayDropletTessellator` early-exits when `flowAlpha < 0.001`

