# Magic Circle - Layer 6: Inner Dot Ring

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, -iTime * PI / 6.0);
    const int n = 12;
    q = rotate(q, 2.0*PI/float(n)/2.0);
    float angle = 2.0*PI / float(n);
    for(int i=0; i<n; i++) {
        dst = circle(dst, q-vec2(0.0, 0.53), 0.001, 0.035, 0.004);
        dst = circle(dst, q-vec2(0.0, 0.53), 0.001, 0.001, 0.001);
        q = rotate(q, angle);
    }
}
```

### What This Does
- Creates 12 small circles arranged in a ring at radius 0.53
- **Counter-clockwise** rotation (`-iTime`)
- Each "dot" is **two overlapping circles** (like Layer 3)
- Smaller than outer dots (0.035 vs 0.05)
- Dimmer center dot (0.001 vs 0.008)

---

## Fixed Values in Original

| Value | Meaning |
|-------|---------|
| `12` | Number of dots |
| `0.53` | Orbit radius |
| `0.001` | Ring inner radius |
| `0.035` | Ring outer radius (smaller than Layer 3) |
| `0.004` | Ring power |
| `0.001` | Center dot radius |
| `0.001` | Center dot power (dimmer than Layer 3) |
| `-PI/6` | Rotation speed (CCW) |
| `2π/12/2` | Initial offset (15°) |

---

## Parameters to Expose

### Placement
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer6_enable` | bool | - | true | - | Enable this layer |
| `layer6_dotCount` | int | 12 | 12 | 4-36 | Number of dots |
| `layer6_orbitRadius` | float | 0.53 | 0.53 | 0.1-1.0 | Distance from center |

### Ring Circle
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer6_ringEnable` | bool | - | true | - | Enable ring part |
| `layer6_ringInnerRadius` | float | 0.001 | 0.001 | 0.0-0.1 | Ring inner edge |
| `layer6_ringOuterRadius` | float | 0.035 | 0.035 | 0.01-0.2 | Ring outer edge |
| `layer6_ringThickness` | float | 0.004 | 0.004 | 0.001-0.02 | Ring power |

### Center Dot
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer6_dotEnable` | bool | - | true | - | Enable center dot |
| `layer6_dotRadius` | float | 0.001 | 0.001 | 0.0-0.05 | Dot size |
| `layer6_dotThickness` | float | 0.001 | 0.001 | 0.0005-0.03 | Dot power |

### Animation
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer6_rotationSpeed` | float | -PI/6 | -0.524 | -2.0-2.0 | Rotation (CCW) |
| `layer6_rotationOffset` | float | π/12 | 0.262 | 0-2π | Initial angle |
| `layer6_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness mult |

---

## Layer 6 Complete Parameter List

```
layer6_enable               bool    true
layer6_dotCount             int     12      [4, 36]
layer6_orbitRadius          float   0.53    [0.1, 1.0]

layer6_ringEnable           bool    true
layer6_ringInnerRadius      float   0.001   [0.0, 0.1]
layer6_ringOuterRadius      float   0.035   [0.01, 0.2]
layer6_ringThickness        float   0.004   [0.001, 0.02]

layer6_dotEnable            bool    true
layer6_dotRadius            float   0.001   [0.0, 0.05]
layer6_dotThickness         float   0.001   [0.0005, 0.03]

layer6_rotationSpeed        float   -0.524  [-2.0, 2.0]
layer6_rotationOffset       float   0.262   [0.0, 6.28]
layer6_intensity            float   1.0     [0.0, 2.0]
```

---

## Layer 6 Total: 13 parameters

**Note:** Identical structure to Layer 3, just with different defaults (smaller, dimmer, CCW).

---

## UBO Packing (Layer 6)

```glsl
// Layer 6: 13 params = ~4 vec4
vec4 layer6_config;     // enable, dotCount, orbitRadius, intensity
vec4 layer6_ring;       // ringEnable, innerR, outerR, thickness
vec4 layer6_dot;        // dotEnable, radius, thickness, reserved
vec4 layer6_animation;  // rotationSpeed, rotationOffset, reserved, reserved
```
