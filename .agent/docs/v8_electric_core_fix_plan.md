# V8 Electric Core - Bug Analysis & Fix Plan

## Background

The original electric core used a **reciprocal formula** that creates bright spikes where FBM noise approaches zero. These spikes "flash" as the noise animates because the reciprocal function `1/x` produces extreme values when x approaches 0.

**Original formula:**
```glsl
float t = abs(2.0 / (fbmValue * 120.0 + 0.1));
```

The USER wanted:
1. A way to disable the flashing (have smooth tendrils instead)
2. Fill the empty sphere interior with a darkened body color
3. Control the fill intensity

---

## Issues Identified (4 Total)

### Issue 1: SMOOTH mode produces uniform color, not tendrils
**Symptom:** When Flash is OFF (default), no visible electric lines - just uniform color

**Cause:** My smooth mode formula is fundamentally wrong:
```glsl
// WRONG - This was my broken code
t = abs(fbmValue) * 15.0 + 0.5;
```

- FBM values range [-1, 1], so `abs(fbmValue)` is [0, 1]
- This produces `t` values of [0.5, 15.5] - consistently HIGH everywhere
- With 5-8 layers, accumulated color is uniform across entire surface
- **This destroys the tendril structure** - it's just a solid blob

**The reciprocal formula is what creates the sparse tendril pattern.** Without it, there are no tendrils.

---

### Issue 2: Electric lines lost body color tinting
**Symptom:** When Flash is ON, electric tendrils appear but "don't even color anymore" - they're raw cyan/blue instead of tinted by the user's body color

**Cause:** I removed the `* bodyColor` multiplication:
```glsl
// BEFORE (working):
col.rgb = electricCol * bodyColor;

// AFTER (broken):
col.rgb = electricCol;  // No bodyColor tinting!
```

The electric pattern generates a cyan/blue gradient procedurally. The `* bodyColor` was tinting it with the user's chosen color. I removed this thinking the fill would handle coloring, but the fill only affects the gaps.

---

### Issue 3: Fill not visible when Flash is ON
**Symptom:** When Flash is ON, "there is no more fill in the sphere" despite fill settings enabled

**Cause:** The fill blending formula is wrong:
```glsl
// WRONG - additive blending
tendrils = mix(tendrils, fillColor + tendrils, fillAmount);
```

When `fillAmount = 0.5`:
- `mix(a, b, 0.5)` = `0.5*a + 0.5*b`
- With `b = fillColor + tendrils`, this becomes `0.5*tendrils + 0.5*(fillColor + tendrils)`
- Which simplifies to `tendrils + 0.5*fillColor` - **additive, not replacing gaps**

This doesn't fill the **gaps** properly because it adds color everywhere, not just in dark areas.

---

### Issue 4: Light/glow leaking outside the orb boundary
**Symptom:** When electric core is enabled, there's a random glow/light that extends beyond the sphere boundary into the surrounding environment

**Cause:** The electric core pattern is calculated for rays that **miss** the sphere.

**Key discovery:** V7 mode (`noiseSpherical`) has a built-in spatial check:
```glsl
// In noise_4d.glsl, noiseSpherical function:
if (sphere < sqRadius) {
    // Only calculate noise if we hit the sphere
    s = noise4q(...);
}
return s;  // Returns 0.0 if ray missed!
```

**Electric mode lacks this check.** The flow is:
1. Ray misses sphere → `surface = vec3(0.0)` (line 217)
2. BUT execution continues (no early return)
3. `electricCorePattern(surface, ...)` is called with `surface = (0,0,0)`
4. FBM noise at (0,0,0) returns non-zero value
5. This renders for ALL pixels where `effectVisibility > 0.01`

**Result:** Electric pattern renders outside the sphere boundary because there's no hit check.

---

## Fix Plan (4 Fixes)

### Fix 1: SMOOTH mode should use clamped reciprocal
**File:** `core_patterns.glsl`, lines 60-68

Keep the reciprocal formula (which creates the tendril structure) but CLAMP the extreme values to prevent flashing:

```glsl
if (flashEnabled > 0.5) {
    // FLASH MODE: Sharp reciprocal (original behavior)
    t = abs(2.0 / (fbmValue * 120.0 + 0.1));
} else {
    // SMOOTH MODE: Same reciprocal but clamped to prevent extreme spikes
    float raw = abs(2.0 / (fbmValue * 120.0 + 0.1));
    t = min(raw, 2.0);  // Cap at 2.0 to prevent flash
}
```

This preserves the tendril structure but limits brightness.

---

### Fix 2: Restore bodyColor tinting for tendrils
**File:** `core_patterns.glsl`, line 76

Add `* bodyColor` back to tint the tendrils with user's color:

```glsl
vec3 tendrils = color * intensity * 0.025 * bodyColor;  // ADD * bodyColor
```

---

### Fix 3: Fix fill blending - use MAX instead of ADD
**File:** `core_patterns.glsl`, lines 79-85

The fill should REPLACE dark areas, not ADD on top. Use `max()` to ensure fill only appears where tendrils are darker than fill:

```glsl
if (fillIntensity > 0.001) {
    vec3 fillColor = bodyColor * (1.0 - fillDarken);
    
    // Scale fill by intensity
    vec3 scaledFill = fillColor * fillIntensity;
    
    // Use MAX: fill only shows where tendrils are darker than fill
    // This prevents additive brightness and ensures fill stays "under" tendrils
    tendrils = max(tendrils, scaledFill);
}
```

**Why MAX works:**
- Where tendrils are bright (e.g., 2.0): `max(2.0, 0.3)` = 2.0 (tendrils win)
- Where tendrils are dark (e.g., 0.0): `max(0.0, 0.3)` = 0.3 (fill shows)
- No additive brightness

---

### Fix 4: Add spatial check for electric core
**File:** `pulsar_v8.glsl`, line 285

Add `result.didHit` check to ensure electric core only renders when ray actually hits the sphere:

```glsl
// BEFORE:
if (coreType == 1) {

// AFTER:
if (coreType == 1 && result.didHit) {  // ADD && result.didHit
```

This ensures electric pattern isn't calculated for rays that miss the sphere.

---

## Summary Table

| Issue | Symptom | Root Cause | Fix |
|-------|---------|------------|-----|
| 1 | Flash OFF = solid blob | Smooth formula destroys tendril structure | Fix 1: Use clamped reciprocal |
| 2 | Tendrils lost user color | Removed `* bodyColor` | Fix 2: Restore bodyColor multiplication |
| 3 | No fill visible | Wrong blending formula (additive) | Fix 3: Use MAX instead of ADD |
| 4 | Glow leaking outside orb | Missing spatial check (no `didHit`) | Fix 4: Add `&& result.didHit` |

---

## Files to Modify

1. `core_patterns.glsl` - Lines 60-68 (Fix 1), line 76 (Fix 2), lines 79-85 (Fix 3)
2. `pulsar_v8.glsl` - Line 285 (Fix 4)

---

## Recommended Fix Order

1. **Fix 4 first** - Add `didHit` check (prevents light leak, quick one-line change)
2. **Fix 2 second** - Restore bodyColor (simple, restores color tinting)
3. **Fix 3 third** - Change fill to MAX (fixes fill visibility)
4. **Fix 1 last** - Change smooth mode formula (can test with Flash ON first)

This order allows testing after each fix to verify progress.
