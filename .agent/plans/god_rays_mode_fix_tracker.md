# God Rays Mode Fix Tracker

## Status Legend
- ❌ BROKEN - Not working at all
- ⚠️ PARTIAL - Partially working
- ✅ FIXED - Verified working
- 🔲 TODO - Not yet addressed

---

## ENERGY MODE (RadiativeInteraction)

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 1 | NONE | 0 | ⚠️ PENDING | Rewritten |
| 2 | EMISSION | 1 | ⚠️ PENDING | Exponential falloff + outward pulse |
| 3 | ABSORPTION | 2 | ⚠️ PENDING | Inverse exponential + inward pulse |
| 4 | REFLECTION | 3 | ⚠️ PENDING | Peak at surface + shimmer |
| 5 | TRANSMISSION | 4 | ⚠️ PENDING | Clean beam |
| 6 | SCATTERING | 5 | ⚠️ PENDING | Chaotic noise |
| 7 | OSCILLATION | 6 | ⚠️ PENDING | Breathing between emission/absorption |
| 8 | RESONANCE | 7 | ⚠️ PENDING | Asymmetric grow/decay |

**Fixes Applied**: 
1. Fixed `t` inversion in god_rays.glsl (t=0 now at light, t=1 at pixel)
2. Rewrote getEnergyVisibility with dramatic exponential/pulse effects

---

## DISTRIBUTION MODE

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 9 | UNIFORM | 0 | 🔲 TODO | Panel works? |
| 10 | WEIGHTED | 1 | 🔲 TODO | Panel works? |
| 11 | NOISE | 2 | 🔲 TODO | Panel works? |
| 12 | RANDOM | 3 | ❌ BROKEN | Can't access in panel |
| 13 | STOCHASTIC | 4 | ❌ BROKEN | Can't access in panel |

---

## ARRANGEMENT MODE (RayArrangement)

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 14 | RADIAL | 0 | ⚠️ PENDING | Now has horizontal band restriction |
| 15 | SPHERICAL | 1 | ⚠️ PENDING | Fixed: removed broken ring formula |
| 16 | PARALLEL | 2 | ⚠️ PENDING | Larger offset for parallel rays |
| 17 | CONVERGING | 3 | ⚠️ PENDING | Light behind pixel |
| 18 | DIVERGING | 4 | ⚠️ PENDING | Same as SPHERICAL position |

**Fixes Applied**:
1. RADIAL: Added `getArrangementWeight()` for horizontal band restriction
2. SPHERICAL: Removed broken `dir * radius` formula, now uses center
3. PARALLEL: Larger offset (-5.0) for truly parallel rays
4. CONVERGING: Light placed behind pixel (`pixelUV + dir * 0.8`)
5. DIVERGING: Uses center like SPHERICAL

---

## CURVATURE MODE (RayCurvature)

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 19 | NONE | 0 | ⚠️ PENDING | Returns unchanged dir |
| 20 | VORTEX | 1 | ⚠️ PENDING | Existing formula |
| 21 | SPIRAL_ARM | 2 | ⚠️ PENDING | Existing formula |
| 22 | TANGENTIAL | 3 | ⚠️ PENDING | Existing formula |
| 23 | LOGARITHMIC | 4 | ⚠️ PENDING | Fixed: `log(1 + t*3) * PI*0.5` |
| 24 | PINWHEEL | 5 | ⚠️ PENDING | Fixed: `t * PI * 0.75` |
| 25 | ORBITAL | 6 | ⚠️ PENDING | Fixed: `t * 2PI` |

**Fixes Applied**:
1. RenderConfig setter now allows 0-6 (was 0-4)
2. Modes 4,5,6 formulas now match Java strategies

---

## TRAVEL MODE (EnergyTravel)

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 26 | NONE | 0 | ⚠️ PENDING | Returns 1.0 |
| 27 | CHASE | 1 | ⚠️ PENDING | Rewritten to match Java - loop, quadratic falloff |
| 28 | SCROLL | 2 | ⚠️ PENDING | Fixed: `1 - abs(scrolledT - 0.5) * 2` |
| 29 | COMET | 3 | ⚠️ PENDING | Rewritten to match Java - tail behind head |
| 30 | SPARK | 4 | ⚠️ PENDING | Hash-based random sparks |
| 31 | PULSE_WAVE | 5 | ⚠️ PENDING | Gaussian: `exp(-normalized² * 4)` |

**Fixes Applied**:
1. All modes rewritten to match `FlowTravelStage.java` exactly
2. Added proper wrap-around distance calculations
3. Quadratic falloff instead of exponential where Java uses it

---

## FLICKER MODE (EnergyFlicker)

| # | Mode | Index | Status | Notes |
|---|------|-------|--------|-------|
| 32 | NONE | 0 | 🔲 TODO | |
| 33 | SCINTILLATION | 1 | 🔲 TODO | |
| 34 | STROBE | 2 | 🔲 TODO | |
| 35 | FADE_PULSE | 3 | 🔲 TODO | |
| 36 | FLICKER | 4 | 🔲 TODO | |
| 37 | LIGHTNING | 5 | 🔲 TODO | |
| 38 | HEARTBEAT | 6 | 🔲 TODO | |

---

## FIX LOG

### Panel Access Fix (Distribution & Arrangement)
**Date**: 2026-01-12
**Issue**: RenderConfig.setGodRaysDistributionMode() and setGodRaysArrangementMode() clamped to 0-2, preventing access to modes 3 and 4
**Fix**: Changed `Math.min(2, mode)` to `Math.min(4, mode)` for both setters
**Result**: PENDING TEST

---

### Mode #1 - NONE (Energy)
**Date**: 
**Current Code**: 
**Expected**: 
**Fix Applied**: 
**Result**: 

---

*Created: 2026-01-12*
