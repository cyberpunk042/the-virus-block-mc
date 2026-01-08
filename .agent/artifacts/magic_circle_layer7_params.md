# Magic Circle - Layer 7: Inner Radiation

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, iTime * PI / 6.0);
    dst = radiation(dst, q, 0.25, 0.3, 12, 0.005);
}
```

### What This Does
- Creates 12 radial spokes from radius 0.25 to 0.3
- **Clockwise** rotation (`+iTime`)
- Thicker lines than outer radiation (0.005 vs 0.0008)

---

## Fixed Values in Original

| Value | Meaning |
|-------|---------|
| `0.25` | Inner radius (spoke start) |
| `0.3` | Outer radius (spoke end) |
| `12` | Number of spokes |
| `0.005` | Line thickness (power) |
| `PI/6` | Rotation speed |
| `+iTime` | Clockwise direction |

---

## Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer7_enable` | bool | - | true | - | Enable this layer |
| `layer7_innerRadius` | float | 0.25 | 0.25 | 0.0-1.0 | Spoke start radius |
| `layer7_outerRadius` | float | 0.3 | 0.3 | 0.0-1.0 | Spoke end radius |
| `layer7_spokeCount` | int | 12 | 12 | 3-72 | Number of spokes |
| `layer7_thickness` | float | 0.005 | 0.005 | 0.0005-0.02 | Line power |
| `layer7_rotationSpeed` | float | PI/6 | 0.524 | -2.0-2.0 | Rotation speed |
| `layer7_rotationOffset` | float | 0 | 0.0 | 0-2π | Initial angle |
| `layer7_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness mult |

---

## Layer 7 Complete Parameter List

```
layer7_enable           bool    true
layer7_innerRadius      float   0.25    [0.0, 1.0]
layer7_outerRadius      float   0.3     [0.0, 1.0]
layer7_spokeCount       int     12      [3, 72]
layer7_thickness        float   0.005   [0.0005, 0.02]
layer7_rotationSpeed    float   0.524   [-2.0, 2.0]
layer7_rotationOffset   float   0.0     [0.0, 6.28]
layer7_intensity        float   1.0     [0.0, 2.0]
```

---

## Layer 7 Total: 8 parameters

---

## UBO Packing (Layer 7)

```glsl
// Layer 7: 8 params = 2 vec4
vec4 layer7_config;     // enable, innerR, outerR, spokeCount
vec4 layer7_style;      // thickness, rotSpeed, rotOffset, intensity
```
