# Omega Blast - Master Design Document

## Overview

A complex, multi-phase visual effect sequence inspired by dramatic anime-style attacks.
The entire sequence is orchestrated through a phased system with configurable timing and parameters.

---

## The Sequence (Timeline)

| Phase | Description | Key Elements |
|-------|-------------|--------------|
| 1 | **Target Mark** appears | Initial target indicator |
| 2 | **Scanner Effect** | Horizontal bands sweep screen + green filter |
| 3 | **Mark Transforms** | Rotates slowly, expands into larger circular mark |
| 4 | **Central Sphere Spawns** | Sphere with rim/corona + ground ripple shockwave |
| 5 | **Orbiting Spheres Branch Out** | 5 spheres emerge from center in orbital pattern |
| 6 | **Beams Fire Upward** | Each sphere launches Kamehameha-style beam skyward |
| 7 | **Beams Retract** | Beams disappear, spheres return to center |
| 8 | **Shockwave Reverses** | Ripples contract back to center |
| 9 | **Sphere Shrinks** | Central sphere reduces progressively |
| 10 | **BLACKOUT** | Screen fades to complete black |
| 11 | **MASSIVE BEAM** | Giant beam expands from center with rim/corona |
| 12 | **Final Shockwave** | Ripple effect from beam impact |

---

## Required Features (Primitives & Systems)

### Already Exists ✅
- **Sphere** - basic sphere shape
- **Kamehameha Beam** - orb + beam composite shape
- **Rim/Corona Shader** - edge glow effect (shader exists)
- **Molecule Shape** - multiple spheres with orbital positioning

### Needs to be Built ❌

| Feature | Type | Priority | Notes |
|---------|------|----------|-------|
| **Shockwave Shape** | New Shape | HIGH | Expanding/contracting rings, terrain-contouring |
| **Screen Blackout Effect** | Screen Effect | HIGH | Uniform fade, configurable (could support vignette) |
| **Screen Color Filter** | Screen Effect | HIGH | Overlay tint (e.g., green) |
| **Scanner Bands** | Screen Effect | MEDIUM | Horizontal lines sweeping screen |
| **Mark/Target Shape** | New Shape | MEDIUM | Initial target indicator, transforms |
| **Phased Sequencer System** | System | HIGH | Orchestrates all phases with timing |

---

## Core Design Principles

1. **EVERYTHING CONFIGURABLE** - All parameters exposed for tweaking
2. **Phased Architecture** - Each element has its own phase/progress
3. **Reversible** - Animations can run forward or backward
4. **Composable** - Complex shapes built from simpler primitives
5. **Code-first, Config-friendly** - Easy to customize in code

---

## Build Order

1. **Shockwave Shape** - New primitive for ground ripple effect
2. **Screen Effects System** - Blackout, filters, scanner bands
3. **Mark/Target Shape** - Initial indicator
4. **Phased Sequencer** - Orchestration system
5. **Omega Blast Composite** - Final assembly using all above

---

## Open Questions

- [ ] Scanner bands: exact sweep pattern?
- [ ] Mark shape: what does initial target look like?
- [ ] Terrain sampling: raycast per vertex? heightmap?
- [ ] Ring shape options: circle, organic, polygon?

---

## Reference Images

See uploaded images in conversation for visual reference of:
- Shockwave rings contouring terrain
- Multiple concentric rings with glow
- Orbiting spheres with upward beams
- Central sphere with rim effect

---

*Document Version: 1.0*
*Last Updated: 2025-12-30*
