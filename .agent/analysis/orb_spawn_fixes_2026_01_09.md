# Orb Spawn Animation - Issue Analysis and Fixes

## Date: 2026-01-09

## Issues Addressed

### Issue A: Slider Label Shows "X: 0X" (Double Label)
**Symptom**: The target distance percentage slider displayed "X: 0X" instead of just "0%".

**Root Cause**: I passed `"%"` as the label parameter to `GuiWidgets.slider()`. The slider infrastructure combines label + formatted value, giving `%: 0%`. The Minecraft font made `%` look like `X`.

**Fix**: Changed slider label from `"%"` to empty string `""` in `FieldVisualSubPanel.java:336`.

### Issue B: Target Distance Shows 500 at 0%
**Symptom**: When slider is at 0%, target distance displayed 500 instead of 1000.

**Root Cause**: The `buildSpawnConfig()` method uses `distortion.radius()` as the spawn distance. The default `DistortionParams.NONE` has `radius = 500f`, not 1000f. For orbs without proximityDarken configured, this caused an incorrect fallback.

**Formula**: `targetDistance = spawnDistance * (1f - targetDistancePercent / 100f)`
- At 0%: 500 * (1 - 0/100) = 500 ✗
- Expected at 0%: 1000 * (1 - 0/100) = 1000 ✓

**Fix**: Added fallback `Math.max(distortion.radius(), 1000f)` in `FieldVisualAdapter.buildSpawnConfig()` at line 759.

### Issue C: SUMMON Only Works for V1
**Symptom**: 
- V1 works (sometimes)
- Other versions (V2, V7, V8) spawn nothing or a static V1
- Only "above" origin works

**Root Cause**: The mixin at `WorldRendererFieldVisualMixin.java:156` only rendered the **first field** from the list:
```java
FieldVisualInstance field = fieldsToRender.get(0);
```

When the GUI is open, there is ALWAYS a **preview orb** registered by the adapter. When SUMMON is clicked:
1. The spawn orb is created with the correct config (V7, V8, etc.)
2. BUT the preview orb is ALSO still registered
3. The render list is sorted by distance - **preview orb is closest to camera** (at player center)
4. Therefore `get(0)` returns the **preview orb's config**
5. **The preview's shader (V1) is loaded instead of the spawn orb's shader**

**Fix**: Modified `WorldRendererFieldVisualMixin` to render ALL fields with independent shaders:
- Changed from single field render to loop: `for (FieldVisualInstance field : fieldsToRender)`
- Each field now gets its own processor via `FieldVisualPostEffect.loadProcessor(field.getConfig())`
- `setCurrentField()` is called for each field before `processor.render()`

### Issue D: Spawn Opacity Only Worked for V2
**Symptom**: Fade-in animation during spawn wasn't working for non-V2 shaders.

**Root Cause**: The spawn opacity was applied via `v2Alpha.alphaScale()` which is V2-specific:
```java
config = config.withV2Alpha(config.v2Alpha().withAlphaScale(originalAlpha * spawnOpacity));
```

**Fix**: Changed to use universal `intensity` modulation which works for all shader versions:
```java
config = config.withIntensity(originalIntensity * spawnOpacity);
```

## Files Modified

1. **FieldVisualSubPanel.java**
   - Line 336: Changed slider label from `"%"` to `""`
   - Lines 424-428: Updated SUMMON button comment (multi-field support)

2. **FieldVisualAdapter.java**
   - Lines 758-759: Added `Math.max(distortion.radius(), 1000f)` fallback

3. **WorldRendererFieldVisualMixin.java**
   - Lines 155-191: Replaced single-field render with multi-field loop

4. **PostEffectPassMixin.java**
   - Lines 240-245: Changed spawn opacity from V2-specific alpha to universal intensity

## Multi-Shader Support Implementation

### Problem
The original code used a single static `currentField` variable to pass field data from the mixin to the pass:
1. `setCurrentField(field1)` → `processor1.render()` adds pass to frame graph
2. `clearCurrentField()`
3. `setCurrentField(field2)` → `processor2.render()` adds pass to frame graph
4. `clearCurrentField()`
5. **Frame graph runs** - but `currentField` is null!

Each pass tried to read `currentField` during execution, but by then it was cleared.

### Solution: Queue-Based Field Storage
Replaced single static field with a `ConcurrentLinkedQueue<FieldVisualInstance>`:

1. **WorldRendererFieldVisualMixin**:
   - Clears queue at start of frame with `clearFieldQueue()`
   - For each field, calls `pushField(field)` then `processor.render()`

2. **PostEffectPassMixin**:
   - When pass executes, calls `popField()` to get the correct field
   - Each pass pops exactly one field in FIFO order

### Files Modified
1. **FieldVisualPostEffect.java**:
   - Replaced `currentField` with `ConcurrentLinkedQueue<FieldVisualInstance>`
   - Added `pushField()`, `popField()`, `clearFieldQueue()` methods
   - Deprecated old `setCurrentField/getCurrentField/clearCurrentField`

2. **WorldRendererFieldVisualMixin.java**:
   - Added `clearFieldQueue()` at start
   - Changed `setCurrentField()` to `pushField()`
   - Removed `clearCurrentField()` in finally block

3. **PostEffectPassMixin.java**:
   - Changed `getCurrentField()` to `popField()`

### Result
Preview orb and spawn orb(s) can now render simultaneously, each with their own shader version and independent UBO data.

## Testing Notes

When testing spawn animation:
1. Select different shader versions (V1, V2, V3, V6, V7, V8)
2. Set spawn origin (Above, Below, Horizon, Diagonal)
3. Click SUMMON
4. Verify:
   - Orb spawns from correct direction
   - Orb travels toward player
   - Orb uses correct shader (visual appearance matches version)
   - Fade-in animation works
   - Preview orb (on player) remains visible with its own shader
   - Multiple spawn orbs can coexist
