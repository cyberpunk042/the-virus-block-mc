# Magic Circle - Layer 4: Middle Ring

## Source Code Analysis

```glsl
{
    vec2 q = p;
    dst = circle(dst, q, 0.5, 0.55, 0.002);
}
```

### What This Does
- Simple static ring (no rotation)
- Ring from radius 0.5 to 0.55

---

## Fixed Values in Original

| Value | Meaning |
|-------|---------|
| `0.5` | Inner radius |
| `0.55` | Outer radius |
| `0.002` | Power/thickness |
| No rotation | Static layer |

---

## Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer4_enable` | bool | - | true | - | Enable this layer |
| `layer4_innerRadius` | float | 0.5 | 0.5 | 0.0-1.0 | Inner edge of ring |
| `layer4_outerRadius` | float | 0.55 | 0.55 | 0.0-1.0 | Outer edge of ring |
| `layer4_thickness` | float | 0.002 | 0.002 | 0.0005-0.02 | Power/line thickness |
| `layer4_rotationSpeed` | float | 0 | 0.0 | -2.0-2.0 | Rotation (0 = static) |
| `layer4_rotationOffset` | float | 0 | 0.0 | 0-2π | Initial angle |
| `layer4_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |

---

## Layer 4 Complete Parameter List

```
layer4_enable           bool    true
layer4_innerRadius      float   0.5     [0.0, 1.0]
layer4_outerRadius      float   0.55    [0.0, 1.0]
layer4_thickness        float   0.002   [0.0005, 0.02]
layer4_rotationSpeed    float   0.0     [-2.0, 2.0]
layer4_rotationOffset   float   0.0     [0.0, 6.28]
layer4_intensity        float   1.0     [0.0, 2.0]
```

---

## Layer 4 Total: 7 parameters

---

## UBO Packing (Layer 4)

```glsl
// Layer 4: 7 params = 2 vec4
vec4 layer4_config;     // enable, innerR, outerR, thickness
vec4 layer4_animation;  // rotationSpeed, rotationOffset, intensity, reserved
```
