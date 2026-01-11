# God Rays Pipeline Refactor Log: 2026-01-11 🔄💥

This log captures a significant pivot and "restart" point in the development of the God Rays volumetric system.

## The "Ugly" State (Pre-Refactor)
Prior to this point, the god rays system had become increasingly convoluted due to:
1.  **Coordinate Instabilities**: The system suffered from "The Horizon Bug" and "Rotation Drift," where rays would not stay anchored to the world position of the orb.
2.  **Fallback Proliferation**: Multiple projection methods (Manual Perspective, Basis Mapping, World-Projection) were layered on top of each other, leading to code bloat and "ugly" shader logic.
3.  **Broken State**: An IDE crash and state loss led to a "trash the current work" decision.

## The Re-Analysis Strategy
The user decided to "zoom back and start all over" to build a cleaner, more robust implementation.

### Milestone: Return to Plan (Step 1239) 🔄⚓
The user explicitly ordered a return to "The Plan," rejecting recent speculative analysis. The project has re-aligned with the **"New Best" Baseline**.

- **Focus**: Restoring the 7-pass HDR chain with Hardcoded Center diagnostics.
- **Authority**: The active roadmap is defined in `god_rays_integration_guide.md`.

## Immediate Action (Restoration Phase)
- **Establish Baseline**: Use `vec2(0.5, 0.5)` in `god_rays_accum.fsh` to verify visible output.
- **Chain Verification**: Ensure all 7 passes are firing and producing non-black framebuffers.
- **Goal**: Stable, cursor-anchored shafts first, then orb-anchored.

## Next Steps After Baseline Works

1. **Verify baseline** - Confirm hardcoded center produces visible rays
2. **Implement Section 5.5** - Vector-Basis Fallback (Production standard)
3. **Test orb-anchored** - Rays should emanate from orb position
4. **Tune parameters** - Adjust decay, exposure, threshold

---
*Status: Restoring "New Best" baseline.*
