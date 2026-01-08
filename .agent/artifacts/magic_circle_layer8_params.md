# Magic Circle - Layer 8: Spinning Core

## Source Code Analysis

```glsl
{
    vec2 q = p;
    q = scale(q, sin(PI*iTime/1.0)*0.04+1.1);
    q = rotate(q, -iTime * PI / 6.0);
    for(float i=0.0; i<6.0; i++) {
        float r = 0.13-i*0.01;
        q = translate(q, vec2(0.1, 0.0));
        dst = circle(dst, q, r, r, 0.002);
        q = translate(q, -vec2(0.1, 0.0));
        q = rotate(q, -iTime * PI / 12.0);
    }
    dst = circle(dst, q, 0.04, 0.04, 0.004);
}
```

### What This Does
This is the most complex layer - a **spiral of nested circles** with:
1. **Breathing scale**: `sin(PI*time)*0.04 + 1.1` (pulses 1.06 to 1.14)
2. **Main rotation**: `-time * PI/6` (counter-clockwise)
3. **6 orbiting circles** with:
   - Translation offset: 0.1 (distance from center)
   - Progressive radius shrink: 0.13, 0.12, 0.11, 0.10, 0.09, 0.08
   - Each rotates an additional `-time * PI/12` (slower secondary rotation)
4. **Center dot**: radius 0.04, brighter than orbitals

---

## Fixed Values in Original

### Breathing
| Value | Meaning |
|-------|---------|
| `PI/1.0` | Breathing frequency |
| `0.04` | Breathing amplitude |
| `1.1` | Breathing center (base scale) |

### Main Rotation
| Value | Meaning |
|-------|---------|
| `-PI/6` | Main rotation speed (CCW) |

### Orbital Circles
| Value | Meaning |
|-------|---------|
| `6` | Number of orbital circles |
| `0.13` | Starting radius |
| `0.01` | Radius decrement per circle |
| `0.1` | Orbit distance (translation) |
| `0.002` | Circle power |
| `-PI/12` | Per-circle rotation increment |

### Center Dot
| Value | Meaning |
|-------|---------|
| `0.04` | Center radius |
| `0.004` | Center power (brighter) |

---

## Parameters to Expose

### Enable & Intensity
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer8_enable` | bool | - | true | - | Enable core layer |
| `layer8_intensity` | float | - | 1.0 | 0.0-2.0 | Brightness mult |

### Breathing
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer8_breathingEnable` | bool | true | true | - | Enable scale breathing |
| `layer8_breathingFrequency` | float | π | 3.14 | 0.5-10 | Breathing speed |
| `layer8_breathingAmplitude` | float | 0.04 | 0.04 | 0.0-0.2 | Breathing range |
| `layer8_breathingCenter` | float | 1.1 | 1.1 | 0.8-1.5 | Base scale |

### Main Rotation
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer8_rotationSpeed` | float | -PI/6 | -0.524 | -2.0-2.0 | Main rotation (CCW) |
| `layer8_rotationOffset` | float | 0 | 0.0 | 0-2π | Initial angle |

### Orbital Circles
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer8_orbitalCount` | int | 6 | 6 | 1-12 | Number of orbitals |
| `layer8_orbitalStartRadius` | float | 0.13 | 0.13 | 0.05-0.3 | First circle radius |
| `layer8_orbitalRadiusStep` | float | 0.01 | 0.01 | 0.0-0.05 | Radius decrement |
| `layer8_orbitalDistance` | float | 0.1 | 0.1 | 0.0-0.3 | Translation offset |
| `layer8_orbitalThickness` | float | 0.002 | 0.002 | 0.0005-0.01 | Circle power |
| `layer8_orbitalRotationSpeed` | float | -PI/12 | -0.262 | -1.0-1.0 | Per-step rotation |

### Center Dot
| Parameter Name | Type | From | Default | Range | Description |
|----------------|------|------|---------|-------|-------------|
| `layer8_centerEnable` | bool | true | true | - | Enable center dot |
| `layer8_centerRadius` | float | 0.04 | 0.04 | 0.01-0.1 | Center size |
| `layer8_centerThickness` | float | 0.004 | 0.004 | 0.001-0.02 | Center power |

---

## Layer 8 Complete Parameter List

```
layer8_enable                   bool    true
layer8_intensity                float   1.0     [0.0, 2.0]

layer8_breathingEnable          bool    true
layer8_breathingFrequency       float   3.14    [0.5, 10.0]
layer8_breathingAmplitude       float   0.04    [0.0, 0.2]
layer8_breathingCenter          float   1.1     [0.8, 1.5]

layer8_rotationSpeed            float   -0.524  [-2.0, 2.0]
layer8_rotationOffset           float   0.0     [0.0, 6.28]

layer8_orbitalCount             int     6       [1, 12]
layer8_orbitalStartRadius       float   0.13    [0.05, 0.3]
layer8_orbitalRadiusStep        float   0.01    [0.0, 0.05]
layer8_orbitalDistance          float   0.1     [0.0, 0.3]
layer8_orbitalThickness         float   0.002   [0.0005, 0.01]
layer8_orbitalRotationSpeed     float   -0.262  [-1.0, 1.0]

layer8_centerEnable             bool    true
layer8_centerRadius             float   0.04    [0.01, 0.1]
layer8_centerThickness          float   0.004   [0.001, 0.02]
```

---

## Layer 8 Total: 18 parameters

---

## UBO Packing (Layer 8)

```glsl
// Layer 8: 18 params = ~5 vec4
vec4 layer8_config;      // enable, intensity, breathingEnable, centerEnable
vec4 layer8_breathing;   // frequency, amplitude, center, reserved
vec4 layer8_rotation;    // rotSpeed, rotOffset, orbitalRotSpeed, reserved
vec4 layer8_orbital;     // count, startRadius, radiusStep, distance
vec4 layer8_style;       // orbitalThickness, centerRadius, centerThickness, reserved
```
