# 🌍 PLANET Sphere Deformation - Complete Research & Implementation Plan

## Vision
Create a new **PLANET** deformation type (or enhance SPHEROID) that can produce:
- **Continents** (raised landmasses with coastlines)
- **Craters** (impact depressions)
- **Mountains** (sharp ridged peaks)
- **Ocean basins** (depressed areas)
- All controlled via intuitive GUI sliders

---

## Research Summary

### 1. Core Algorithm: Fractal Brownian Motion (fBm)

**The Foundation:** Multi-octave noise layering

```
fBm(point) = Σ (amplitude_i × noise(point × frequency_i))
```

**Key Parameters:**
- **Octaves** (int, 1-8): Number of noise layers. More = more detail
- **Lacunarity** (float, 1.5-3.5): Frequency multiplier per octave (typically 2.0)
- **Persistence/Gain** (float, 0.3-0.7): Amplitude multiplier per octave (typically 0.5)

**Algorithm:**
```java
float fBm(float x, float y, float z, int octaves, float lacunarity, float persistence) {
    float total = 0;
    float amplitude = 1;
    float frequency = 1;
    float maxValue = 0;  // For normalization
    
    for (int i = 0; i < octaves; i++) {
        total += amplitude * noise(x * frequency, y * frequency, z * frequency);
        maxValue += amplitude;
        amplitude *= persistence;
        frequency *= lacunarity;
    }
    
    return total / maxValue;  // Normalize to -1...1
}
```

### 2. Terrain Types via Noise Modifications

#### A. Standard fBm → Rolling Hills, Plains
- Default mode, smooth undulating terrain

#### B. Ridged Multifractal → Mountains
**Key Insight:** Apply `1 - abs(noise)` to create sharp V-ridges

```java
float ridgedNoise(float x, float y, float z, int octaves, float lacunarity, float gain, float offset) {
    float sum = 0;
    float amplitude = 1;
    float frequency = 1;
    float weight = 1;
    
    for (int i = 0; i < octaves; i++) {
        float signal = noise(x * frequency, y * frequency, z * frequency);
        signal = offset - Math.abs(signal);  // Invert to create ridges pointing up
        signal = signal * signal;             // Sharpen the ridges
        signal *= weight;                     // Weight based on previous octave
        
        weight = clamp(signal * gain, 0, 1);  // Higher gain = noisier ridges
        
        sum += signal * amplitude;
        amplitude *= persistence;
        frequency *= lacunarity;
    }
    
    return sum;
}
```

**Parameters:**
- **Offset** (0.5-1.5): Higher = more ridges/rougher
- **Gain** (1.0-3.0): Feedback strength, higher = more dramatic ridges

#### C. Crater Generation → Impact Features
**Formula:** Paraboloid depression with raised rim

```java
float craterProfile(float distance, float radius, float depth, float rimHeight) {
    float r = distance / radius;  // Normalized distance (0 at center, 1 at rim)
    
    if (r > 1.5) return 0;  // Outside influence
    
    if (r <= 1.0) {
        // Inside crater: paraboloid depression
        // depth-to-radius ratio is typically 0.2-0.4 for real craters
        return -depth * (1 - r*r);  // Paraboloid: deepest at center
    } else {
        // Rim and ejecta blanket: raised ring that tapers off
        float rimFalloff = (r - 1.0) / 0.5;  // 0 at rim, 1 at edge
        return rimHeight * (1 - rimFalloff) * Math.exp(-rimFalloff * 2);
    }
}
```

**For multiple craters:** Use Fibonacci spiral to position crater centers, then sum profiles:
```java
for each crater:
    dot = vertex_direction · crater_direction
    angular_distance = acos(dot)
    linear_distance = angular_distance * radius
    displacement += craterProfile(linear_distance, craterRadius, craterDepth, rimHeight)
```

#### D. Worley/Voronoi Noise → Cells, Plates, Tessellated Features
**Algorithm:** For each point, find distance to nearest seed point

```java
float worleyNoise(float x, float y, float z, int seedCount) {
    float minDist = Float.MAX_VALUE;
    
    // Generate seed points deterministically using hash
    for each cell in neighborhood:
        seed = hash(cellX, cellY, cellZ)
        seedPos = cellCenter + jitter(seed)
        dist = distance(point, seedPos)
        minDist = min(minDist, dist)
    
    return minDist;
}
```

**Uses:**
- Low values (near seeds) = depressed
- High values (far from seeds) = raised
- Can create: plate boundaries, hexagonal cells, organic patterns

### 3. Spherical Noise Sampling

**Critical:** Sample 3D noise using the 3D Cartesian position of each vertex

```java
// Convert spherical coordinates to 3D position for noise sampling
float vx = sin(theta) * cos(phi);
float vy = cos(theta);
float vz = sin(theta) * sin(phi);

// Scale by frequency
float noiseX = vx * baseFrequency;
float noiseY = vy * baseFrequency;
float noiseZ = vz * baseFrequency;

// Sample 3D noise
float noiseValue = noise3D(noiseX, noiseY, noiseZ);
```

### 4. Simplex Noise Implementation

We need a pure Java 3D Simplex noise implementation. Based on Stefan Gustavson's work:

**Core Components:**
1. **Permutation Table:** 512-element table for pseudo-random indexing
2. **Gradient Vectors:** 12 gradients (midpoints of cube edges)
3. **Skew/Unskew:** Transform to simplex coordinates
4. **Contribution Calculation:** Weighted sum of gradient contributions

---

## Proposed Deformation Type: PLANET

### New Parameters for SphereShape

| Parameter | Type | Range | Default | Effect |
|-----------|------|-------|---------|--------|
| `planetMode` | enum | TERRAIN, CRATERS, COMBINED | TERRAIN | Feature type |
| `planetFrequency` | float | 0.5-10 | 2.0 | Base noise scale |
| `planetOctaves` | int | 1-8 | 4 | Detail layers |
| `planetPersistence` | float | 0.2-0.8 | 0.5 | Amplitude decay |
| `planetLacunarity` | float | 1.5-3.5 | 2.0 | Frequency growth |
| `planetRidged` | float | 0-1 | 0 | 0=smooth, 1=sharp ridges |
| `planetCraterCount` | int | 0-20 | 0 | Number of craters |
| `planetCraterDepth` | float | 0.1-0.5 | 0.2 | Crater depression depth |
| `planetSeed` | int | 0-999 | 42 | Random seed for reproducibility |

### Mode Descriptions

**TERRAIN Mode:**
- Uses fBm with configurable ridged multifractal blending
- `planetRidged = 0`: Smooth continents and gentle hills
- `planetRidged = 1`: Sharp mountain ranges

**CRATERS Mode:**
- Places `planetCraterCount` craters using Fibonacci spiral
- Each crater has paraboloid depression + raised rim
- Useful for moon/asteroid surfaces

**COMBINED Mode:**
- Layer craters ON TOP of terrain
- Full planet with both features

---

## Algorithm: PLANET Vertex Calculation

```java
public static float[] planetVertex(
    float theta, float phi, float radius,
    float intensity, float length,
    PlanetMode mode,
    float frequency, int octaves, float persistence, float lacunarity,
    float ridged, int craterCount, float craterDepth, int seed
) {
    // Base direction
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);
    float sinPhi = sin(phi);
    float cosPhi = cos(phi);
    
    float vx = sinTheta * cosPhi;
    float vy = cosTheta;
    float vz = sinTheta * sinPhi;
    
    // Early exit for no intensity
    if (intensity < 0.01f) {
        return new float[] { radius * vx, radius * length * vy, radius * vz };
    }
    
    float displacement = 0;
    
    // === TERRAIN ===
    if (mode == TERRAIN || mode == COMBINED) {
        // Sample fBm at this vertex position
        float fBmValue = fBm(
            vx * frequency + seed * 0.1f,
            vy * frequency + seed * 0.2f,
            vz * frequency + seed * 0.3f,
            octaves, lacunarity, persistence
        );
        
        // Optionally blend with ridged noise
        if (ridged > 0) {
            float ridgedValue = ridgedMultifractal(
                vx * frequency + seed * 0.1f,
                vy * frequency + seed * 0.2f,
                vz * frequency + seed * 0.3f,
                octaves, lacunarity, persistence * 0.8f, 1.0f
            );
            fBmValue = lerp(fBmValue, ridgedValue, ridged);
        }
        
        displacement += fBmValue * 0.3f;  // Scale appropriately
    }
    
    // === CRATERS ===
    if ((mode == CRATERS || mode == COMBINED) && craterCount > 0) {
        float craterDisp = 0;
        float goldenAngle = 2.399963f;
        
        for (int i = 0; i < craterCount; i++) {
            // Crater center position (Fibonacci spiral)
            float t = (i + 0.5f) / craterCount;
            float craterTheta = acos(1 - 2 * t);
            float craterPhi = goldenAngle * i;
            
            float cx = sin(craterTheta) * cos(craterPhi);
            float cy = cos(craterTheta);
            float cz = sin(craterTheta) * sin(craterPhi);
            
            // Angular distance to crater center
            float dot = vx * cx + vy * cy + vz * cz;
            float angularDist = acos(clamp(dot, -1, 1));
            
            // Crater size varies by index (pseudo-random via seed)
            float craterRadius = 0.3f + 0.2f * sin((i + seed) * 1.7f);
            
            // Apply crater profile
            craterDisp += craterProfile(angularDist, craterRadius, craterDepth, craterDepth * 0.3f);
        }
        
        displacement += craterDisp;
    }
    
    // Apply intensity
    displacement *= intensity;
    
    // Final radius
    float finalR = radius * (1.0f + displacement);
    
    return new float[] {
        finalR * vx,
        finalR * length * vy,
        finalR * vz
    };
}
```

---

## Implementation Order

### Step 1: Add SimplexNoise Class to ShapeMath
- Pure Java 3D Simplex noise implementation
- Based on Stefan Gustavson's public domain code
- Include fBm and ridged multifractal wrappers

### Step 2: Add PLANET to SphereDeformation Enum
- New enum value: `PLANET`
- Consider this as THE comprehensive planetary deformation

### Step 3: Add Parameters to SphereShape
- Add 9 new fields for planet controls
- Update all constructors, fromJson, toBuilder, Builder

### Step 4: Implement planetVertex in ShapeMath
- Full implementation with all modes
- Modular design for terrain + craters

### Step 5: Update SphereDeformation.computeVertex
- Route PLANET to new algorithm
- Pass all planet parameters

### Step 6: Update GUI Controls
- Add new slider section for PLANET
- Show/hide based on deformation type (optional advanced feature)

### Step 7: Update Supporting Files
- SphereTessellator, ShapeRegistry
- Test in-game

---

## Validation Tests

1. **TERRAIN mode, ridged=0, octaves=4** → Smooth continents
2. **TERRAIN mode, ridged=1, octaves=6** → Sharp mountains
3. **CRATERS mode, count=10, depth=0.3** → Moon-like surface
4. **COMBINED mode, ridged=0.5, craters=5** → Terrestrial planet with impact features
5. **Seed variation** → Different seeds produce different planets

---

## References

- Stefan Gustavson: "Simplex Noise Demystified" (liu.se)
- Ken Perlin: "Improving Noise" (SIGGRAPH 2002)
- Fractal Brownian Motion: thebookofshaders.com
- Ridged Multifractal: isaratech.com, libnoise documentation
- Crater profiles: University of Chicago planetary science
- Worley Noise: Steven Worley's original 1996 paper
