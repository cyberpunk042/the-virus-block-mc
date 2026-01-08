# Magic Circle - Layer 1: Outer Ring

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, iTime * PI / 6.0);
    dst = circle(dst, q, 0.85, 0.9, 0.006);
    dst = radiation(dst, q, 0.87, 0.88, 36, 0.0008);
}
```

This block contains **TWO elements** that share rotation:
1. A ring (circle with r1=0.85, r2=0.9)
2. Radiation lines (36 spokes from 0.87 to 0.88)

---

## Element 1A: Outer Ring

### Fixed Values in Original
| Value | Meaning |
|-------|---------|
| `0.85` | Inner radius (normalized 0-1) |
| `0.9` | Outer radius (normalized 0-1) |
| `0.006` | Power/thickness |
| `PI / 6.0` | Rotation speed (radians per time unit) |
| `+iTime` | Positive = clockwise rotation |

### Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer1_ringEnable` | bool | - | true | - | Enable this ring |
| `layer1_ringInnerRadius` | float | 0.85 | 0.85 | 0.0-1.0 | Inner edge of ring |
| `layer1_ringOuterRadius` | float | 0.9 | 0.9 | 0.0-1.0 | Outer edge of ring |
| `layer1_ringThickness` | float | 0.006 | 0.006 | 0.001-0.05 | Glow line power |
| `layer1_ringRotationSpeed` | float | PI/6 | 0.524 | -2.0-2.0 | Rotation speed (rad/s) |
| `layer1_ringIntensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |

**Total: 6 parameters for Element 1A**

---

## Element 1B: Outer Radiation (Spokes)

### Fixed Values in Original
| Value | Meaning |
|-------|---------|
| `0.87` | Inner radius (start of spokes) |
| `0.88` | Outer radius (end of spokes) |
| `36` | Number of spokes |
| `0.0008` | Power/thickness |
| shares rotation | Same rotation as ring above |

### Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer1_radEnable` | bool | - | true | - | Enable radiation lines |
| `layer1_radInnerRadius` | float | 0.87 | 0.87 | 0.0-1.0 | Spoke start radius |
| `layer1_radOuterRadius` | float | 0.88 | 0.88 | 0.0-1.0 | Spoke end radius |
| `layer1_radCount` | int | 36 | 36 | 3-72 | Number of spokes |
| `layer1_radThickness` | float | 0.0008 | 0.0008 | 0.0001-0.01 | Spoke line power |
| `layer1_radIntensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |
| `layer1_radRotationOffset` | float | 0 | 0 | 0-2π | Angle offset from ring |

**Total: 7 parameters for Element 1B**

---

## Shared Parameters (Layer 1 Group)

Since both elements share rotation:

| Parameter Name | Type | Default | Range | Description |
|----------------|------|---------|-------|-------------|
| `layer1_groupEnable` | bool | true | - | Master enable for entire layer |
| `layer1_groupRotationSpeed` | float | 0.524 | -2.0-2.0 | Shared rotation speed |
| `layer1_groupIntensity` | float | 1.0 | 0.0-2.0 | Master intensity mult |

**Total: 3 shared parameters**

---

## Layer 1 Complete Parameter List

### Element 1A: Ring (6 params)
```
layer1_ringEnable           bool    true
layer1_ringInnerRadius      float   0.85    [0.0, 1.0]
layer1_ringOuterRadius      float   0.9     [0.0, 1.0]  
layer1_ringThickness        float   0.006   [0.001, 0.05]
layer1_ringRotationOffset   float   0.0     [0.0, 6.28]
layer1_ringIntensity        float   1.0     [0.0, 2.0]
```

### Element 1B: Radiation (7 params)
```
layer1_radEnable            bool    true
layer1_radInnerRadius       float   0.87    [0.0, 1.0]
layer1_radOuterRadius       float   0.88    [0.0, 1.0]
layer1_radCount             int     36      [3, 72]
layer1_radThickness         float   0.0008  [0.0001, 0.01]
layer1_radRotationOffset    float   0.0     [0.0, 6.28]
layer1_radIntensity         float   1.0     [0.0, 2.0]
```

### Group-Level (3 params)
```
layer1_enable               bool    true
layer1_rotationSpeed        float   0.524   [-2.0, 2.0]
layer1_intensity            float   1.0     [0.0, 2.0]
```

---

## Layer 1 Total: 16 parameters

**Note:** Original code block actually contains 2 distinct visual elements that I had previously counted as 2 separate layers. For accuracy, this is **Layer 1** containing:
- 1A: Outer Ring
- 1B: Outer Radiation

---

## UBO Packing (Layer 1)

```glsl
// Layer 1: 16 params = 4 vec4
vec4 layer1_enables;      // ringEnable, radEnable, groupEnable, reserved
vec4 layer1_ringParams;   // innerR, outerR, thickness, rotOffset
vec4 layer1_radParams1;   // innerR, outerR, count, thickness
vec4 layer1_radParams2;   // rotOffset, intensity multipliers...
```

Or use struct approach for clarity.
