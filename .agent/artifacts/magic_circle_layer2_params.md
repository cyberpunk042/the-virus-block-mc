# Magic Circle - Layer 2: Hexagram

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, iTime * PI / 6.0);
    const int n = 6;
    float angle = PI / float(n);
    q = rotate(q, floor(atan(q.x, q.y)/angle + 0.5) * angle);
    for(int i=0; i<n; i++) {
        dst = rectangle(dst, q, vec2(0.85/sqrt(2.0)), vec2(0.85/sqrt(2.0)), 0.0015);
        q = rotate(q, angle);
    }
}
```

### What This Does
- Creates 6 rotated rectangles forming a **hexagram** (Star of David shape)
- Uses `floor(atan/angle + 0.5) * angle` for angular snapping (discrete rotation)
- All rectangles have the same size: `0.85/√2 ≈ 0.601`

---

## Fixed Values in Original

| Value | Calculation | Meaning |
|-------|-------------|---------|
| `6` | n | Number of rectangles (hexagram) |
| `PI/6` | angle | Spacing between rectangles (30°) |
| `0.85/√2` | ≈ 0.601 | Rectangle half-size (both corners same) |
| `0.0015` | power | Line thickness |
| `PI/6.0` | rotation speed | Same as Layer 1 |
| `+iTime` | direction | Clockwise rotation |

---

## Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer2_enable` | bool | - | true | - | Enable hexagram layer |
| `layer2_rectCount` | int | 6 | 6 | 3-12 | Number of rectangles |
| `layer2_rectSize` | float | 0.601 | 0.601 | 0.1-1.0 | Rectangle half-extent |
| `layer2_rectThickness` | float | 0.0015 | 0.0015 | 0.0005-0.01 | Line power/thickness |
| `layer2_rotationSpeed` | float | PI/6 | 0.524 | -2.0-2.0 | Rotation speed (rad/s) |
| `layer2_rotationOffset` | float | 0 | 0.0 | 0-2π | Initial angle offset |
| `layer2_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |
| `layer2_snapRotation` | bool | true | true | - | Use angular snapping |

### Advanced (Optional)
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer2_rectInnerSize` | float | 0.601 | 0.601 | 0.1-1.0 | Inner corner (half1) |
| `layer2_rectOuterSize` | float | 0.601 | 0.601 | 0.1-1.0 | Outer corner (half2) |

**Note:** Original uses same value for both corners. Exposing separately allows rectangular outlines vs square.

---

## Layer 2 Complete Parameter List

```
layer2_enable           bool    true
layer2_rectCount        int     6       [3, 12]
layer2_rectInnerSize    float   0.601   [0.1, 1.0]
layer2_rectOuterSize    float   0.601   [0.1, 1.0]
layer2_rectThickness    float   0.0015  [0.0005, 0.01]
layer2_rotationSpeed    float   0.524   [-2.0, 2.0]
layer2_rotationOffset   float   0.0     [0.0, 6.28]
layer2_intensity        float   1.0     [0.0, 2.0]
layer2_snapRotation     bool    true
```

---

## Layer 2 Total: 9 parameters

---

## UBO Packing (Layer 2)

```glsl
// Layer 2: 9 params = ~3 vec4
vec4 layer2_config;    // enable, rectCount, snapRotation, intensity
vec4 layer2_geometry;  // innerSize, outerSize, thickness, reserved
vec4 layer2_animation; // rotationSpeed, rotationOffset, reserved, reserved
```
