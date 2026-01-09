# Debug Playbook - Fast Diagnosis

> **Purpose:** Quick reference for debugging UBO and shader issues.

---

## Startup Validation Logs

### What to Log
```
[UBO] FrameData: binding=0, size=16 bytes, layoutVersion=1
[UBO] CameraData: binding=1, size=224 bytes
[UBO] FieldVisualConfig: binding=20, size=XXX bytes, effectVersion=8.0
[UBO] Total UBOs registered: 5
```

### What to Check
- All expected UBOs appear
- Sizes match expected values
- No binding collisions
- LayoutVersion/effectVersion present

---

## Common Failure Modes

### 1. All Zeros (Effect Not Visible)

**Symptoms:**
- Effect doesn't render
- Sampling uniform returns 0.0

**Check:**
- Is the UBO being bound before draw?
- Is binding index correct?
- Is the uniform block name matching?
- Is the shader compiled without errors?

**Debug:**
```glsl
// Temporarily output raw uniform value
fragColor = vec4(PrimaryColor.rgb, 1.0);  // Should see color
```

### 2. NaN / Garbage Values

**Symptoms:**
- Black screen or flashing
- Weird colors
- Jittering positions

**Check:**
- Layout mismatch (Java vs GLSL)
- Size calculation wrong
- Reserved lanes not zeroed
- Matrix not initialized

**Debug:**
```glsl
// Check for NaN
if (isnan(CenterX) || isinf(CenterX)) {
    fragColor = vec4(1.0, 0.0, 0.0, 1.0);  // Red = NaN detected
    return;
}
```

### 3. Wrong Values (Shifted Layout)

**Symptoms:**
- Colors in wrong positions
- Values that don't match what was set
- Effect behaves strangely

**Check:**
- Field order mismatch
- Missing vec4 padding
- Nested struct alignment issue

**Debug:**
```glsl
// Output slot by slot to find misalignment
// Slot 0
fragColor = vec4(CenterX, CenterY, CenterZ, Radius);
// Slot 1
// fragColor = vec4(PrimaryR, PrimaryG, PrimaryB, PrimaryA);
```

### 4. Effect Not Updating

**Symptoms:**
- Static when should animate
- Stale camera position

**Check:**
- Is binder updating every frame?
- Is correct UBO being updated (Config vs Runtime)?
- Is Time being passed from FrameUBO?

---

## Debug View Modes

Implement via `DebugMode` uniform:

| Mode | Value | Output |
|------|-------|--------|
| Normal | 0 | Normal rendering |
| Effect Mask | 1 | Show effect alpha as grayscale |
| Distance | 2 | Visualize distance to center |
| Ray Direction | 3 | Show ray.direction as RGB |
| Depth | 4 | Show linearized depth |
| UBO Slot | 5 | Output specific slot for debugging |

```glsl
if (DebugMode > 0.5 && DebugMode < 1.5) {
    // Mode 1: Effect Mask
    fragColor = vec4(vec3(effectAlpha), 1.0);
    return;
}
```

---

## Quick Validation Checklist

Before shipping any UBO change:

- [ ] Java record compiles
- [ ] Size calculation matches GLSL
- [ ] Binding index doesn't collide
- [ ] Golden test shaders pass
- [ ] No NaN in output
- [ ] Animation still works (Time flows)
- [ ] Camera tracking works (look around)

---

## Isolating Single Effect

If multiple effects are on screen:

```glsl
// Add to shader
uniform float DebugEffectId;  // Set from Java

if (abs(currentEffectId - DebugEffectId) > 0.5) {
    discard;  // Only show target effect
}
```

---

## Performance Quick Check

If frame rate drops:

1. Check NoiseOctaves (reduce to 3-4)
2. Check FBM loop count
3. Check raymarch step count
4. Disable corona/rays temporarily
5. Profile with RenderDoc or NSight

---

> **Remember:** Most shader bugs are layout mismatches. When in doubt, output uniform values directly.
