# Magic Circle - Layer 5: Inner Triangle

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, -iTime * PI / 6.0);
    const int n = 3;
    float angle = PI / float(n);
    q = rotate(q, floor(atan(q.x, q.y)/angle + 0.5) * angle);
    for(int i=0; i<n; i++) {
        dst = rectangle(dst, q, vec2(0.36, 0.36), vec2(0.36, 0.36), 0.0015);
        q = rotate(q, angle);
    }
}
```

### What This Does
- Creates 3 rotated rectangles forming a **triangle** pattern
- **Counter-clockwise** rotation (`-iTime`)
- Uses angular snapping like the hexagram
- Smaller than hexagram (0.36 vs 0.601)

---

## Fixed Values in Original

| Value | Meaning |
|-------|---------|
| `3` | Number of rectangles (triangle) |
| `PI/3` | Spacing between rectangles (60°) |
| `0.36` | Rectangle half-size |
| `0.0015` | Line thickness (same as hexagram) |
| `-PI/6.0` | Rotation speed (negative = counter-clockwise) |
| `-iTime` | Counter-clockwise direction |

---

## Parameters to Expose

| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer5_enable` | bool | - | true | - | Enable triangle layer |
| `layer5_rectCount` | int | 3 | 3 | 3-12 | Number of rectangles |
| `layer5_rectInnerSize` | float | 0.36 | 0.36 | 0.1-1.0 | Inner corner half-extent |
| `layer5_rectOuterSize` | float | 0.36 | 0.36 | 0.1-1.0 | Outer corner half-extent |
| `layer5_rectThickness` | float | 0.0015 | 0.0015 | 0.0005-0.01 | Line power |
| `layer5_rotationSpeed` | float | -PI/6 | -0.524 | -2.0-2.0 | Rotation (negative = CCW) |
| `layer5_rotationOffset` | float | 0 | 0.0 | 0-2π | Initial angle offset |
| `layer5_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |
| `layer5_snapRotation` | bool | true | true | - | Use angular snapping |

---

## Layer 5 Complete Parameter List

```
layer5_enable           bool    true
layer5_rectCount        int     3       [3, 12]
layer5_rectInnerSize    float   0.36    [0.1, 1.0]
layer5_rectOuterSize    float   0.36    [0.1, 1.0]
layer5_rectThickness    float   0.0015  [0.0005, 0.01]
layer5_rotationSpeed    float   -0.524  [-2.0, 2.0]
layer5_rotationOffset   float   0.0     [0.0, 6.28]
layer5_intensity        float   1.0     [0.0, 2.0]
layer5_snapRotation     bool    true
```

---

## Layer 5 Total: 9 parameters

**Note:** Identical structure to Layer 2 (Hexagram), just with different defaults.

---

## UBO Packing (Layer 5)

```glsl
// Layer 5: 9 params = ~3 vec4
vec4 layer5_config;    // enable, rectCount, snapRotation, intensity
vec4 layer5_geometry;  // innerSize, outerSize, thickness, reserved
vec4 layer5_animation; // rotationSpeed, rotationOffset, reserved, reserved
```
