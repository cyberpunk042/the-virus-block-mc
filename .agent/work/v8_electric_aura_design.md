# V8 Electric Aura - Detailed Implementation Plan

## Reference: nimitz Shader Analysis

### Core Math
```glsl
// Rotation helper
mat2 makem2(float theta) { return mat2(cos(theta), -sin(theta), sin(theta), cos(theta)); }

// Base noise (texture lookup in original, we use procedural)
float noise(vec2 x) { return texture(iChannel0, x * 0.01).x; }

// Turbulent (ridged) FBM - creates sharp creases
float fbm(vec2 p) {
    float z = 2.0, rz = 0.0;
    for (int i = 1; i < 6; i++) {
        rz += abs((noise(p) - 0.5) * 2.0) / z;  // abs() creates ridges
        z *= 2.0;
        p *= 2.0;
    }
    return rz;
}

// Domain-warped FBM - creates swirling organic motion
float dualfbm(vec2 p) {
    vec2 p2 = p * 0.7;
    vec2 basis = vec2(
        fbm(p2 - time * 1.6),
        fbm(p2 + time * 1.7)
    );
    basis = (basis - 0.5) * 0.2;  // Displacement amount
    p += basis;                     // Warp the domain
    return fbm(p * makem2(time * 0.2));  // Final sample with rotation
}

// Logarithmic ring pattern
float circ(vec2 p) {
    float r = length(p);
    r = log(sqrt(r));              // Log spacing: dense near center, sparse at edges
    return abs(mod(r * 4.0, TAU) - PI) * 3.0 + 0.2;
}
```

---

## Component 1: Core (4D Noise Plasma)

### Approach
- Use 4D noise (like V7's `noise4q`) for animated 3D surface
- Apply turbulent/ridged treatment for plasma character
- Time dimension provides smooth animation
- Surface coordinates from ray-sphere intersection

### Implementation

```glsl
// 4D turbulent plasma for core surface
float core4DPlasma(vec3 surface, float time, int octaves, vec3 seed) {
    float z = 2.0, rz = 0.0;
    vec4 p4 = vec4(surface + seed, time);
    
    for (int i = 1; i <= octaves; i++) {
        float n = noise4q(p4);  // Existing 4D noise from V7
        rz += abs((n - 0.5) * 2.0) / z;  // Turbulent (ridged)
        z *= 2.0;
        p4 *= 2.0;
    }
    return rz;
}
```

### Difference from V7
- V7: `noiseSpherical` → smooth layered noise with power curves
- V8: `core4DPlasma` → turbulent/ridged for electric character

### Color Treatment
```glsl
// Core uses division formula for plasma consistency
vec3 coreColor = coreBaseColor / max(coreRz, 0.01);
```

### Integration with Corona
- Core visible when `sphere < sqRadius` (ray hits sphere body)
- Corona visible when `sphere >= sqRadius` (ray passes near sphere)
- Smooth blend at the boundary using `dr = perpDistSq / sqRadius`

---

## Component 2: Plasma Corona Zone

### Purpose
- Fill the space around the sphere with electric plasma texture
- Provides the dark purple background that rings light up
- Creates the organic swirling motion

### Coordinate Mapping (3D → 2D)

```glsl
// Input: pr = projection point relative to sphere center
// Output: 2D coords for noise sampling

vec3 prn = normalize(pr);  // Direction on sphere surface

// Spherical UVs
float theta = atan(prn.z, prn.x);  // Azimuthal [-π, π]
float phi = asin(prn.y);            // Polar [-π/2, π/2]

// Scale for desired detail level
vec2 plasmaUV = vec2(theta, phi * 2.0) * noiseScale;
```

### Noise Implementation

```glsl
float electricPlasma(vec2 uv, float time, int octaves) {
    // Turbulent FBM (ridged)
    float z = 2.0, rz = 0.0;
    vec3 p3 = vec3(uv, time * 0.1);  // Use time as Z for animation
    
    for (int i = 1; i <= octaves; i++) {
        float n = snoise3d(p3, 10.0);  // Our existing noise
        n = n * 0.5 + 0.5;              // Map to [0, 1]
        rz += abs((n - 0.5) * 2.0) / z; // Ridged
        z *= 2.0;
        p3 *= 2.0;
    }
    return rz;
}

float electricDualFBM(vec2 uv, float time, int octaves) {
    // Domain warping
    vec2 uv2 = uv * 0.7;
    vec2 warp = vec2(
        electricPlasma(uv2 - time * 1.6, time, octaves),
        electricPlasma(uv2 + time * 1.7, time, octaves)
    );
    warp = (warp - 0.5) * 0.2;
    uv += warp;
    
    // Final sample with rotation
    float angle = time * 0.2;
    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return electricPlasma(uv * rot, time, octaves);
}
```

### Parameters
- `octaves`: 4-6 for good detail (default 5)
- `noiseScale`: Controls pattern size (default 4.0)
- `warpAmount`: Domain warping strength (default 0.2)
- `animSpeed`: Time multiplier for animation

---

## Component 3: Pulsating Rings

### Purpose
- Create bright ring structures that emanate from sphere
- Pulse/expand outward over time
- Both emit light AND modulate plasma

### Ring Distance Calculation

```glsl
// Distance from sphere SURFACE (not center)
float surfaceDist = length(pr) - sphereRadius;

// Only positive (outside sphere)
surfaceDist = max(0.0, surfaceDist);

// Normalize by ray reach
float normalizedDist = surfaceDist / rayReach;
```

### Logarithmic Ring Pattern

```glsl
float electricRings(float dist, float time, float ringFreq) {
    // Pulsating expansion: rings move outward
    float pulseDist = dist / exp(mod(time * pulseSpeed, PI));
    
    // Logarithmic spacing
    float r = log(sqrt(pulseDist) + 0.001);  // +epsilon prevents log(0)
    
    // Ring pattern
    float rings = abs(mod(r * ringFreq, TAU) - PI) * 3.0 + 0.2;
    
    return rings;
}
```

### Ring-Plasma Combination

```glsl
// Plasma value from dualFBM
float plasma = electricDualFBM(plasmaUV, time, octaves);

// Ring modulation factor
float rings = electricRings(surfaceDist, time, ringFreq);
float ringMod = pow(abs(0.1 - rings), 0.9);

// Combine: where rings ≈ 0.1, ringMod → 0, making final rz small
float rz = plasma * ringMod;
```

### Parameters
- `ringFreq`: Number of ring cycles (default 4.0)
- `pulseSpeed`: Ring expansion speed (default 10.0)
- `pulseCycle`: Expansion period (default PI ≈ 3.14)

---

## Component 4: Color Compositing

### nimitz Formula
```glsl
vec3 col = vec3(0.2, 0.1, 0.4) / rz;  // Purple base divided by intensity
col = pow(abs(col), vec3(0.99));       // Slight gamma adjustment
```

### How It Works
- Small rz → large color values → BRIGHT
- Large rz → small color values → DARK
- The division inverts the relationship

### For V8
```glsl
// Base plasma color (configurable via UBO)
vec3 plasmaBaseColor = rayColor;  // Or custom V8 color

// Division-based coloring
float rz = max(rz, 0.01);  // Prevent division by zero
vec3 col = plasmaBaseColor / rz;

// Clamp to prevent oversaturation
col = min(col, vec3(10.0));

// Optional gamma
col = pow(abs(col), vec3(0.99));
```

---

## Component 5: Distance Fade (Corona Boundary)

### Purpose
- Define the outer edge of the corona zone
- Smooth transition into the scene

### Implementation
```glsl
float coronaFade(float surfaceDist, float maxReach, float fadeWidth) {
    if (surfaceDist > maxReach) return 0.0;
    
    float fadeStart = maxReach - fadeWidth;
    if (surfaceDist < fadeStart) return 1.0;
    
    // Smooth fade in the transition zone
    return 1.0 - smoothstep(fadeStart, maxReach, surfaceDist);
}
```

### Parameters
- `maxReach`: Maximum corona extent (default: `sphereRadius * rayReach`)
- `fadeWidth`: Width of fade zone (default: `maxReach * 0.3`)

---

## Component 6: Occlusion

### Keep V7's Logic
- Calculate Z-depth of sphere vs scene
- If sphere behind scene, apply visibility multiplier
- Progressive bleed for smooth transitions

### Apply to V8
```glsl
// From V7's occlusion calculation
float effectVisibility = /* V7 logic */;

// Apply to final color
col *= effectVisibility;
```

---

## Integration: Full Pipeline

```glsl
float renderElectricAura(...) {
    // 1. Ray-sphere intersection (from V7)
    vec3 pr = /* projection point */;
    float surfaceDist = length(pr) - sphereRadius;
    
    // 2. Spherical UVs for plasma
    vec3 prn = normalize(pr);
    vec2 plasmaUV = vec2(atan(prn.z, prn.x), asin(prn.y) * 2.0) * noiseScale;
    
    // 3. Plasma layer
    float plasma = electricDualFBM(plasmaUV, time, octaves);
    
    // 4. Ring layer
    float rings = electricRings(surfaceDist, time, ringFreq);
    float ringMod = pow(abs(0.1 - rings), 0.9);
    
    // 5. Combine
    float rz = plasma * ringMod;
    
    // 6. Color
    vec3 col = plasmaBaseColor / max(rz, 0.01);
    
    // 7. Distance fade
    float fade = coronaFade(surfaceDist, maxReach, fadeWidth);
    col *= fade;
    
    // 8. Occlusion
    col *= effectVisibility;
    
    return col;
}
```
