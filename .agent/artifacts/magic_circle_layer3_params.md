# Magic Circle - Layer 3: Outer Dot Ring

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = rotate(q, iTime * PI / 6.0);
    const int n = 12;
    q = rotate(q, 2.0*PI/float(n)/2.0);
    float angle = 2.0*PI / float(n);
    for(int i=0; i<n; i++) {
        dst = circle(dst, q-vec2(0.0, 0.875), 0.001, 0.05, 0.004);
        dst = circle(dst, q-vec2(0.0, 0.875), 0.001, 0.001, 0.008);
        q = rotate(q, angle);
    }
}
```

### What This Does
- Creates 12 small circles arranged in a ring at radius 0.875
- Each "dot" is actually **two overlapping circles**:
  1. Ring circle: r1=0.001, r2=0.05 (thin ring)
  2. Center dot: r1=r2=0.001 (bright point)
- Initial rotation offset: `2π/12/2 = π/12 = 15°` (half-step offset)

---

## Fixed Values in Original

| Value | Meaning |
|-------|---------|
| `12` | Number of dots |
| `0.875` | Orbit radius (placement distance from center) |
| `0.001` | Ring inner radius (tiny) |
| `0.05` | Ring outer radius |
| `0.004` | Ring power |
| `0.001` | Center dot radius |
| `0.008` | Center dot power (brighter than ring) |
| `PI/6` | Rotation speed |
| `2π/12/2` | Initial offset (15°) |

---

## Parameters to Expose

### Placement
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer3_enable` | bool | - | true | - | Enable this layer |
| `layer3_dotCount` | int | 12 | 12 | 4-36 | Number of dots |
| `layer3_orbitRadius` | float | 0.875 | 0.875 | 0.1-1.0 | Distance from center |

### Ring Circle (outer ring of each dot)
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer3_ringEnable` | bool | - | true | - | Enable ring part |
| `layer3_ringInnerRadius` | float | 0.001 | 0.001 | 0.0-0.1 | Ring inner edge |
| `layer3_ringOuterRadius` | float | 0.05 | 0.05 | 0.01-0.2 | Ring outer edge |
| `layer3_ringThickness` | float | 0.004 | 0.004 | 0.001-0.02 | Ring power |

### Center Dot
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer3_dotEnable` | bool | - | true | - | Enable center dot |
| `layer3_dotRadius` | float | 0.001 | 0.001 | 0.0-0.05 | Dot size |
| `layer3_dotThickness` | float | 0.008 | 0.008 | 0.001-0.03 | Dot power (brightness) |

### Animation
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer3_rotationSpeed` | float | PI/6 | 0.524 | -2.0-2.0 | Rotation speed |
| `layer3_rotationOffset` | float | π/12 | 0.262 | 0-2π | Initial angle offset |
| `layer3_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness multiplier |

---

## Layer 3 Complete Parameter List

```
layer3_enable               bool    true
layer3_dotCount             int     12      [4, 36]
layer3_orbitRadius          float   0.875   [0.1, 1.0]

layer3_ringEnable           bool    true
layer3_ringInnerRadius      float   0.001   [0.0, 0.1]
layer3_ringOuterRadius      float   0.05    [0.01, 0.2]
layer3_ringThickness        float   0.004   [0.001, 0.02]

layer3_dotEnable            bool    true
layer3_dotRadius            float   0.001   [0.0, 0.05]
layer3_dotThickness         float   0.008   [0.001, 0.03]

layer3_rotationSpeed        float   0.524   [-2.0, 2.0]
layer3_rotationOffset       float   0.262   [0.0, 6.28]
layer3_intensity            float   1.0     [0.0, 2.0]
```

---

## Layer 3 Total: 13 parameters

---

## UBO Packing (Layer 3)

```glsl
// Layer 3: 13 params = ~4 vec4
vec4 layer3_config;     // enable, dotCount, orbitRadius, intensity
vec4 layer3_ring;       // ringEnable, innerR, outerR, thickness
vec4 layer3_dot;        // dotEnable, radius, thickness, reserved
vec4 layer3_animation;  // rotationSpeed, rotationOffset, reserved, reserved
```
