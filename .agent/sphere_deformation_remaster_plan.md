# 🔬 CLOUD & MOLECULE Deformation Complete Overhaul

## Research Summary

### Problem Statement
The current CLOUD and MOLECULE deformations exhibit bizarre behavior:
- Shapes start forming toward expected geometry
- At higher intensity, they collapse into a uniformly scaled sphere
- Controls (count, smoothness) don't produce visible distinct effects
- The radiusFactor calculation creates global scaling instead of local bumps

### Root Cause: Fundamentally Wrong Approach
**Current Approach:** Calculate a `radiusFactor` multiplier based on accumulated field values, then multiply the entire sphere by this factor.

**Problem:** When you sum Gaussian contributions and normalize by count, the result becomes nearly uniform across the sphere, producing a uniformly scaled sphere rather than distinct bumps.

**Correct Approach:** Calculate a **local displacement** for each vertex based on its proximity to bump/atom centers, and **add** this displacement to the base radius (not multiply).

---

## Mathematical Foundations (Researched)

### CLOUD: Spherical Gaussian Bump Approach

**Spherical Gaussian (SG) Function:**
```
G(v; p, λ, μ) = μ * e^(λ * (v·p - 1))
```

Where:
- `v` = current vertex direction (unit vector from sphere center)
- `p` = lobe/bump center direction (unit vector) - the "lobe axis"
- `λ` = lobe sharpness (positive value, higher = tighter bump)
- `μ` = lobe amplitude (height of the bump)
- `v·p` = dot product = cos(angular_distance)

**Key Insight:** `v·p - 1` ranges from 0 (when v = p, aligned) to -2 (when v = -p, opposite).
When aligned: e^(λ * 0) = 1 → full bump
When opposite: e^(λ * (-2)) ≈ 0 → no bump

**For multiple bumps (cloud/cumulus effect):**
```
displacement = Σ G_i(v) for all bumps i
finalRadius = baseRadius + displacement * intensity
```

**Parameters:**
- `count` → Number of bump centers distributed via golden angle spiral
- `smoothness` → Maps to sharpness λ: low smoothness = high λ (sharp), high smoothness = low λ (soft)
- `intensity` → Overall amplitude of bumps (how far they protrude)
- `size` (NEW) → Multiplier for individual bump amplitude

### MOLECULE: True Metaball Implicit Surface

**Blinn's Metaball Field Function:**
```
f(r) = a * e^(-b * r²)
```

Or the inverse-square falloff:
```
f(r) = R² / (r² + ε)
```

Where:
- `r` = distance from atom center to evaluation point
- `a` = field strength
- `b` = falloff rate
- `R` = atom radius
- `ε` = small constant to avoid division by zero

**Key Difference from CLOUD:** 
- CLOUD: Bumps are centered ON the sphere surface (spherical gaussians)
- MOLECULE: Atoms are positioned AROUND the center, at a distance from origin

**For molecule effect:**
1. Position atoms at distance `separation * radius` from origin
2. For each surface point, calculate influence from all atoms
3. Surface point pushed outward where atoms are nearby

**True Isosurface Approach (Simplified for our case):**
```
For each vertex at direction v:
  maxInfluence = 0
  for each atom at position A:
    # Distance from ray through v to atom center
    # Using angular proximity for efficiency
    dot = v · normalize(A)  
    influence = atomRadius² / ((1 - dot)² + ε)
    
    if blending mode:
      maxInfluence += influence  # Additive (merged blob)
    else:
      maxInfluence = max(maxInfluence, influence)  # Distinct atoms
  
  displacement = map(maxInfluence, threshold, to 0-1)
  finalRadius = baseRadius * (1 + displacement * intensity)
```

---

## Parameter Design

### CLOUD Parameters

| Parameter | Type | Range | Default | Effect |
|-----------|------|-------|---------|--------|
| `intensity` | float | 0-1 | 0.5 | How far bumps protrude from surface |
| `count` | int | 1-20 | 6 | Number of visible bumps/lobes |
| `smoothness` | float | 0-1 | 0.5 | Blur radius (0=sharp spikes, 1=soft billows) |
| `bumpSize` | float | 0.1-2.0 | 0.5 | Size of individual bumps (NEW) |

**Sharpness mapping:**
- `λ = 1 + (1 - smoothness) * 20`  (range: 1 to 21)
- Low smoothness (0) → λ = 21 → very sharp, distinct bumps
- High smoothness (1) → λ = 1 → very soft, blended bumps

### MOLECULE Parameters

| Parameter | Type | Range | Default | Effect |
|-----------|------|-------|---------|--------|
| `intensity` | float | 0-1 | 0.5 | How far atoms protrude |
| `count` | int | 1-12 | 4 | Number of atoms |
| `smoothness` | float | 0-1 | 0.5 | Blending (0=distinct spheres, 1=merged blob) |
| `separation` | float | 0.3-1.5 | 0.6 | Distance of atoms from center (NEW) |
| `atomSize` | float | 0.1-1.0 | 0.4 | Size of each atom sphere (NEW) |

---

## Algorithm: CLOUD v2

```java
/**
 * Cloud deformation using Spherical Gaussian bumps.
 * Creates fluffy, cumulus-like protrusions.
 */
public static float[] cloudVertex_v2(
    float theta, float phi, float radius,
    float intensity, float length,
    int count, float smoothness, float bumpSize
) {
    // Base sphere direction
    float sinTheta = (float) Math.sin(theta);
    float cosTheta = (float) Math.cos(theta);
    float sinPhi = (float) Math.sin(phi);
    float cosPhi = (float) Math.cos(phi);
    
    // Current vertex direction (unit vector)
    float vx = sinTheta * cosPhi;
    float vy = cosTheta;
    float vz = sinTheta * sinPhi;
    
    // At intensity 0, return pure spheroid
    if (intensity < 0.01f) {
        return new float[] {
            radius * vx,
            radius * length * vy,
            radius * vz
        };
    }
    
    // Map smoothness to sharpness (λ)
    // Low smoothness = high sharpness (tight bumps)
    // High smoothness = low sharpness (soft bumps)
    float sharpness = 2.0f + (1.0f - smoothness) * 18.0f;  // Range: 2 to 20
    
    // Calculate total displacement from all bumps
    float totalDisplacement = 0.0f;
    float goldenAngle = 2.399963f;  // ~137.5 degrees
    
    for (int i = 0; i < count; i++) {
        // Distribute bump centers using Fibonacci spiral (golden angle)
        // This gives even distribution on sphere surface
        float t = (float) i / (float) count;
        float bumpTheta = (float) Math.acos(1.0 - 2.0 * t);  // 0 to π
        float bumpPhi = goldenAngle * i;
        
        // Bump center direction (unit vector)
        float px = (float) Math.sin(bumpTheta) * (float) Math.cos(bumpPhi);
        float py = (float) Math.cos(bumpTheta);
        float pz = (float) Math.sin(bumpTheta) * (float) Math.sin(bumpPhi);
        
        // Dot product = cos(angular_distance)
        float dot = vx * px + vy * py + vz * pz;
        
        // Spherical Gaussian: G(v) = e^(λ * (dot - 1))
        // When aligned (dot=1): e^0 = 1
        // When opposite (dot=-1): e^(-2λ) ≈ 0
        float gaussian = (float) Math.exp(sharpness * (dot - 1.0f));
        
        // Add bump contribution
        // Each bump contributes independently (no normalization by count!)
        totalDisplacement += gaussian * bumpSize;
    }
    
    // Apply intensity to create final displacement
    // Displacement is additive to base radius
    float displacement = totalDisplacement * intensity * 0.3f;  // Scale factor
    
    // Final radius at this point
    float finalR = radius * (1.0f + displacement);
    
    // Apply to position (with axial stretch on Y)
    return new float[] {
        finalR * vx,
        finalR * length * vy,
        finalR * vz
    };
}
```

---

## Algorithm: MOLECULE v2

```java
/**
 * Molecule deformation using metaball-style atom placement.
 * Creates visible spherical "atom" protrusions around center.
 */
public static float[] moleculeVertex_v2(
    float theta, float phi, float radius,
    float intensity, float length,
    int count, float smoothness, float separation, float atomSize
) {
    // Base sphere direction
    float sinTheta = (float) Math.sin(theta);
    float cosTheta = (float) Math.cos(theta);
    float sinPhi = (float) Math.sin(phi);
    float cosPhi = (float) Math.cos(phi);
    
    // Current vertex direction (unit vector)
    float vx = sinTheta * cosPhi;
    float vy = cosTheta;
    float vz = sinTheta * sinPhi;
    
    // At intensity 0, return pure spheroid
    if (intensity < 0.01f) {
        return new float[] {
            radius * vx,
            radius * length * vy,
            radius * vz
        };
    }
    
    // Calculate influence from all atoms
    float goldenAngle = 2.399963f;
    float totalInfluence = 0.0f;
    
    // Atom positions are at distance (separation * radius) from origin
    float atomDist = separation * radius;
    
    for (int i = 0; i < count; i++) {
        // Distribute atoms using Fibonacci spiral
        float t = (float) i / (float) count;
        float atomTheta = (float) Math.acos(1.0 - 2.0 * t);
        float atomPhi = goldenAngle * i;
        
        // Atom center direction (unit vector)
        float ax = (float) Math.sin(atomTheta) * (float) Math.cos(atomPhi);
        float ay = (float) Math.cos(atomTheta);
        float az = (float) Math.sin(atomTheta) * (float) Math.sin(atomPhi);
        
        // Atom position in 3D space
        float atomX = ax * atomDist;
        float atomY = ay * atomDist;
        float atomZ = az * atomDist;
        
        // Ray from origin through current vertex direction
        // We want to find how close this ray passes to the atom
        // Using simplified angular distance approach:
        float dot = vx * ax + vy * ay + vz * az;
        
        // Metaball field: inverse-square falloff on angular distance
        // (1 - dot) is 0 when aligned, 2 when opposite
        float angularDist = 1.0f - dot;
        float atomR2 = atomSize * atomSize;
        float epsilon = 0.05f;
        
        float influence = atomR2 / (angularDist * angularDist + epsilon);
        
        // Blending mode based on smoothness
        if (smoothness > 0.5f) {
            // Additive blending (atoms merge together)
            totalInfluence += influence;
        } else {
            // Max blending (distinct atoms)
            totalInfluence = Math.max(totalInfluence, influence);
        }
    }
    
    // Map influence to displacement
    // Threshold: only create bumps where influence exceeds a minimum
    float threshold = 0.5f;
    float normalizedInfluence = Math.max(0, totalInfluence - threshold) / (1.0f + totalInfluence - threshold);
    
    // Apply intensity
    float displacement = normalizedInfluence * intensity * 0.5f;
    
    // Also add central sphere contribution (base shape)
    float centralContrib = 0.8f + smoothness * 0.2f;  // 0.8 to 1.0
    
    // Final radius
    float finalR = radius * (centralContrib + displacement);
    
    return new float[] {
        finalR * vx,
        finalR * length * vy,
        finalR * vz
    };
}
```

---

## Implementation Steps

### Step 1: Add New Parameters to SphereShape
- Add `deformationBumpSize` (float, default 0.5f) - for CLOUD
- Add `deformationSeparation` (float, default 0.6f) - for MOLECULE
- These replace the generic approach with shape-specific controls

### Step 2: Rewrite cloudVertex in ShapeMath
- Implement the Spherical Gaussian algorithm above
- Remove flawed normalization-by-count
- Use ADDITIVE displacement

### Step 3: Rewrite moleculeVertex in ShapeMath
- Implement the metaball algorithm above
- Position atoms at distance from center
- Use max vs additive blending based on smoothness

### Step 4: Update SphereDeformation.computeVertex
- Pass new parameters through
- Ensure no additional blending is applied (return directly)

### Step 5: Update GUI Controls
- Add sliders for bumpSize and separation
- Potentially increase count range to 1-20

### Step 6: Testing Validation
- CLOUD count=4, intensity=0.8, smoothness=0 → 4 distinct sharp bumps
- CLOUD count=8, intensity=0.5, smoothness=1 → fluffy cloud shape
- MOLECULE count=4, intensity=0.8, smoothness=0 → tetrahedral visible atoms
- MOLECULE count=2, intensity=0.7, smoothness=1 → dumbbell/merged blob

---

## References
- Spherical Gaussians: https://schuttejoe.github.io/post/sphericalgaussians/
- Jim Blinn Metaballs: "The Algebraic Properties of Implicit Blobby Molecules" (SIGGRAPH 1982)
- Fibonacci Spiral on Sphere: Golden angle = 2π / φ² ≈ 2.399963 radians ≈ 137.5°
