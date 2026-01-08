# V2 Orb Parameter Flow Investigation

## COMPLETE DATA FLOW TRACE

### Problem 1: "Lines Opacity" controls External Rays, not Inner Lines

#### GUI Definition (EffectSchemaRegistry.java line 167):
```java
slider(FV + "glowLineIntensity", "Lines Opacity", 0f, 3f, 0.8f, "Inner Lines")
```
- GUI label: "Lines Opacity"
- GUI group: "Inner Lines"
- Parameter: `glowLineIntensity`

#### Java Flow:
1. **FieldVisualAdapter.java line 169**: `"glowLineIntensity" -> glowLine.intensity()`
2. **FieldVisualAdapter.java line 358**: `"glowLineIntensity" -> glowLine = glowLine.withIntensity(toFloat(value))`
3. **FieldVisualConfig.java line 297**: `glowLineIntensity() { return glowLine.intensity(); }`

#### UBO Writer (FieldVisualUBOWriter.java line 190):
```java
// vec4 13: GlowLineParams (expanded)
builder.putFloat(glowLine.count());      // x
builder.putFloat(glowLine.intensity());  // y = glowLineIntensity
builder.putFloat(glowLine.rayPower());   // z
builder.putFloat(glowLine.raySharpness()); // w
```

#### Shader Types (energy_orb_types.glsl line 34):
```glsl
vec4 GlowLineParams;  // x=count, y=intensity, z=rayCoronaFlags, w=version
```

#### Shader Extraction (field_visual.fsh line 427):
```glsl
float glowLineIntensity = field.GlowLineParams.y;
```

#### Shader Usage (energy_orb_v2.glsl line 341):
```glsl
vec3 rawGlow = orbGlowLinesCustom(..., glowLineCount, glowLineIntensity);
//                                                     ^^^^^^^^^^^^^^^^
//                                    EXTERNAL RAY INTENSITY, NOT INNER LINES!
```

#### What Inner Lines Actually Use (line 351):
```glsl
col += lines * linesColor * intensity;
//                          ^^^^^^^^^
//              Global intensity only, NO separate opacity control!
```

### CONCLUSION - Problem 1:
GUI says "Lines Opacity" in "Inner Lines" group, but it actually controls EXTERNAL RAY brightness.
The inner voronoi lines have NO separate opacity control.

---

### Problem 2: Alpha/Visual is Completely Wrong

Looking at the screenshot - the orb is washed out white instead of the beautiful energy effect.

#### Possible causes:

1. **Alpha calculation**: `alpha = clamp(length(col) * alphaScale, 0.0, 1.0)`
   - If col is very bright (length > 1), alpha saturates
   - But the issue is the color looks white, not just alpha

2. **Corona brightness too high?**
   - Current default: V2CoronaBrightness = 0.5 (in schema)
   - Original default in types.glsl: 0.15
   - MISMATCH!

3. **Check the hardcoded defaults in energy_orb_types.glsl**:
```glsl
f.V2CoronaBrightness = 0.15;  // But Java/JSON/Schema have 0.5!
```

4. **coreSpread and edgeSpread multipliers**:
   - Original formula: `vec3(0.2, 0.05, 0.1) / core`
   - Current formula: `vec3(0.2, 0.05, 0.1) * coreSpread / core`
   - If coreSpread = 1.0, this is the same
   - But is coreSpread being set correctly?

---

### Problem 3: Complete Defaults Mismatch

Checking current state across all sources:

| Parameter | Original Shadertoy | Java Default | JSON Default | Schema Default | GLSL Fallback |
|-----------|-------------------|--------------|--------------|----------------|---------------|
| coronaStart | 0.15 | ? | 0.15 | 0.15 | 0.15 |
| coronaBrightness | 0.15* | 0.5 | 0.5 | 0.5 | 0.15 |
| coreSize | 0.05 (direct) | 0.5 | 0.5 | 0.5 | N/A |
| spiralTwist | 5.0 | 5.0 | 5.0 | 5.0 | N/A |
| spiralDensity | 5.0 | 5.0 | 5.0 | 5.0 | N/A |
| alphaScale | 1.0 (alpha=1) | 1.0 | 1.0 | 1.0 | N/A |

*Note: Original uses `vec3(.1, .02, .01)` for corona (background), not a scalar brightness.

---

### Problem 4: UBO Slot Mismatch?

The UBOWriter writes:
- Slot 13: `count, intensity, rayPower, raySharpness`

But energy_orb_types.glsl expects:
- `x=count, y=intensity, z=rayCoronaFlags, w=version`

**CRITICAL MISMATCH!**
- Java puts `rayPower` in z
- GLSL expects `rayCoronaFlags` in z

This means:
- `rayCoronaFlags` (which controls showExternalRays and showCorona) is getting the wrong value!
- `showExternalRays` and `showCorona` booleans are extracted from a royPower float instead of flags!

---

### IMMEDIATE FIXES NEEDED:

1. **Fix UBO slot 13 mismatch** between Java and GLSL
2. **Add separate linesOpacity parameter** for inner voronoi pattern
3. **Sync all default values** across Java, JSON, Schema, and GLSL
4. **Rename GUI control** or fix the parameter it maps to

---

### FILES TO CHECK:

1. `FieldVisualUBOWriter.java` - UBO packing order
2. `energy_orb_types.glsl` - FieldData struct layout
3. `EffectSchemaRegistry.java` - GUI parameter names
4. `FieldVisualTypes.java` - Java record defaults
5. `field_visual.json` - JSON defaults
6. `energy_orb_v2.glsl` - Shader parameter usage
