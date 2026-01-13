# God Rays Shader Robustness Enhancement Plan

**Created**: 2026-01-13  
**Goal**: Enhance shader stability and prevent corruption without breaking existing functionality

---

## Current State Analysis

### Observed Issues
1. **Random intermittent corruption** - Rectangular pixel blocks appearing
2. **Banding artifacts** - Visible step patterns in god rays at high settings
3. **State degradation** - Effect quality degrades over time, worse after restart
4. **Beautiful when working** - Core algorithm is correct, just needs hardening

### Current Parameters (from UBO)
- `GodRaySamples` - Number of ray march steps (typically 48-128)
- `GodRayDecay` - Range control (0.94-0.995)
- `GodRayExposure` - Strength (0.005-0.1)
- Off-screen lightUV distance: 5.0 units

---

## Root Cause Hypotheses

### H1: Floating Point Precision Loss
**Evidence**: Rectangular banding pattern, consistent with quantization
**Mechanism**: 
- `lightUV` pushed 5 units off-screen
- `toLight` vectors become very large
- Small differences between samples lose precision
- Creates stair-step artifacts

**Test**: Reduce off-screen distance, check if banding reduces

### H2: Accumulation Overflow
**Evidence**: Corruption after extended play
**Mechanism**:
- `illumination += occlusion * decay * ...` accumulates each sample
- With high samples (128) and decay (0.995), can exceed HDR range
- Eventually produces Inf/NaN
- NaN propagates through subsequent calculations

**Test**: Add illumination clamp, check if corruption stops

### H3: Division by Zero / Near-Zero
**Evidence**: Random corruption timing
**Mechanism**:
- Many `normalize()` calls on small vectors
- `length(toLight)` used as divisor
- Edge cases where vectors approach zero
- GPU produces garbage for 0/0

**Test**: Add epsilon guards to all divisions/normalizations

### H4: Shader Complexity Timeout
**Evidence**: Worse with high sample counts
**Mechanism**:
- GPU has watchdog timer for shader execution
- Complex loops with many texture samples may timeout
- Causes partial/corrupted frame

**Test**: Check if reducing samples eliminates corruption

---

## Enhancement Plan

### Phase 1: Defensive Hardening (Non-Breaking)
**Goal**: Add guards that only trigger in error cases

#### 1.1 NaN/Inf Protection
```glsl
// At end of accumulation, before output:
if (isnan(illumination) || isinf(illumination)) {
    illumination = 0.0;  // Fail to black, not garbage
}
```

#### 1.2 Illumination Ceiling
```glsl
// After accumulation loop:
illumination = min(illumination, 100.0);  // HDR ceiling
```

#### 1.3 Safe Normalization Helper
```glsl
vec2 safeNormalize(vec2 v, vec2 fallback) {
    float len = length(v);
    return (len > 0.0001) ? v / len : fallback;
}
```

#### 1.4 Epsilon Guards on Divisions
```glsl
// Instead of: x / len
// Use: x / max(len, 0.0001)
```

### Phase 2: Precision Optimization (Careful)
**Goal**: Reduce numerical stress without changing visual output

#### 2.1 Reduce Off-Screen Distance
```glsl
// Current:
vec2 backLightUV = vec2(0.5 + cos(orbAngle) * 5.0, ...);

// Optimized (3.0 is sufficient for parallel rays):
vec2 backLightUV = vec2(0.5 + cos(orbAngle) * 3.0, ...);
```
**Risk**: Lower - 3.0 still produces effectively parallel rays

#### 2.2 Early-Out for Distant Pixels
```glsl
// If pixel is very far from lightUV and decay is high, skip expensive loop
float distToLight = length(lightUV - texCoord);
if (distToLight > 2.0 && parallelFactor < 0.1) {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    return;
}
```
**Risk**: Medium - needs careful threshold tuning

#### 2.3 Adaptive Sample Count
```glsl
// Reduce samples for distant pixels (less visible anyway)
int effectiveSamples = (distToLight > 1.0) ? samples / 2 : samples;
```
**Risk**: Medium - may cause visible quality boundary

### Phase 3: Diagnostic Mode (Development)
**Goal**: Help identify issues without affecting normal play

#### 3.1 Debug Output Mode
```glsl
#ifdef DEBUG_GOD_RAYS
    // Output diagnostic colors instead of rays
    if (isnan(illumination)) fragColor = vec4(1.0, 0.0, 0.0, 1.0);  // Red = NaN
    if (isinf(illumination)) fragColor = vec4(0.0, 1.0, 0.0, 1.0);  // Green = Inf
    if (illumination > 100.0) fragColor = vec4(0.0, 0.0, 1.0, 1.0); // Blue = overflow
#endif
```

---

## Implementation Order

| Priority | Enhancement | Risk | Benefit |
|----------|-------------|------|---------|
| 1 | NaN/Inf Protection (1.1) | None | Prevents garbage output |
| 2 | Illumination Ceiling (1.2) | None | Prevents overflow |
| 3 | Safe Normalization (1.3) | None | Prevents div-by-zero |
| 4 | Reduce Off-Screen Distance (2.1) | Low | Better precision |
| 5 | Epsilon Guards (1.4) | None | General safety |
| 6 | Early-Out (2.2) | Medium | Performance |
| 7 | Adaptive Samples (2.3) | Medium | Performance |

---

## Testing Protocol

For each enhancement:
1. ✓ Apply change
2. ✓ Verify front hemisphere still works
3. ✓ Verify back hemisphere 360° still works  
4. ✓ Test at high settings (decay 0.995, samples 128)
5. ✓ Run for 10+ minutes checking for corruption
6. ✓ Test restart stability

---

## Notes

- All Phase 1 changes are purely defensive - they only activate in error cases
- Phase 2 changes may slightly affect visuals but should be imperceptible
- Start with Phase 1 items, evaluate before Phase 2
- Consider making some Phase 2 items configurable via UBO

---

## Ready to Implement

When ready, we'll implement in priority order, testing after each change.
