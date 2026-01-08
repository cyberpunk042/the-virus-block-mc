# 🎨 Custom Shader Effects Research for Minecraft 1.21 Fabric

## Reference Images Analysis

Based on the provided reference images, we need to implement:

### Image 1: Black Hole with Accretion Disk
- **Gravitational Lensing**: Warping of space/background around the black hole
- **Accretion Disk**: Glowing ring of matter with Doppler color shift
- **Event Horizon**: Pure black central sphere
- **Corona Glow**: Bright rim around the disk

### Image 2: Neutron Star / Pulsar
- **Turbulent Surface**: Emissive, cloud-like surface (already have CLOUD deformation!)
- **Fresnel Rim Glow**: Bright edge outline around the sphere
- **Magnetic Field Lines**: Toroidal loops (tilted rings)
- **Polar Jets**: Bright beams emanating from poles (rays)

### Image 3: Eclipse / Corona
- **Pure Black Sphere**: Silhouette (depth buffer trick or pure black material)
- **Corona Glow**: Intense rim light with graduated falloff
- **Hot colors**: Orange/yellow gradient from center outward

---

## Shader Effects Catalog

### 1. FRESNEL RIM EFFECT ⭐ (Priority 1)

**The Problem:** Highlight edges of objects when viewed at grazing angles.

**The Math:**
```glsl
// In fragment shader
float fresnel = 1.0 - max(dot(viewDir, normal), 0.0);
float rim = pow(fresnel, rimPower);
vec3 finalColor = baseColor + rimColor * rim * rimIntensity;
```

**Parameters:**
| Parameter | Range | Effect |
|-----------|-------|--------|
| `rimPower` | 1.0 - 10.0 | Falloff sharpness (1=soft, 10=hard edge) |
| `rimIntensity` | 0.0 - 5.0 | Brightness multiplier |
| `rimColor` | RGB | Color of the rim light |
| `rimBias` | 0.0 - 1.0 | Minimum rim (starts from this value) |

**Variations:**
- **Inner Rim**: Invert the fresnel for a darkening at edges
- **Pulsing Rim**: Animate `rimIntensity` over time
- **Rainbow Rim**: Shift hue based on angle

---

### 2. BLOOM / GLOW EFFECT ⭐ (Priority 2)

**The Problem:** Make bright areas bleed light into surrounding pixels.

**Implementation (Post-Processing Pipeline):**
```
1. Render scene → SceneTexture
2. Extract bright pixels → BrightTexture (threshold filter)
3. Blur horizontally → BlurH (Gaussian)
4. Blur vertically → BlurV (Gaussian)
5. Combine: SceneTexture + BlurV → FinalOutput
```

**GLSL Threshold Pass:**
```glsl
// Extract brightness
float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
if (brightness > threshold) {
    FragColor = color;
} else {
    FragColor = vec4(0.0);
}
```

**GLSL Gaussian Blur Pass:**
```glsl
// 9-tap Gaussian blur (horizontal or vertical)
vec3 result = vec3(0.0);
float weight[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
for (int i = -4; i <= 4; i++) {
    vec2 offset = vec2(float(i) * texelSize, 0.0); // horizontal
    result += texture(image, texCoord + offset).rgb * weight[abs(i)];
}
FragColor = vec4(result, 1.0);
```

**Parameters:**
| Parameter | Range | Effect |
|-----------|-------|--------|
| `bloomThreshold` | 0.0 - 2.0 | Brightness cutoff |
| `bloomIntensity` | 0.0 - 3.0 | Glow strength |
| `bloomRadius` | 1 - 20 | Blur kernel size |

---

### 3. GRAVITATIONAL LENSING (Screen-Space Distortion)

**The Problem:** Warp background as if bent by gravity.

**Screen-Space Approach (Game-Friendly):**
```glsl
uniform vec2 blackHolePos;   // Screen-space center
uniform float radius;         // Effect radius
uniform float strength;       // Distortion power

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    vec2 toCenter = blackHolePos - uv;
    float dist = length(toCenter);
    
    // Distortion falls off with distance
    float distortion = strength / (dist * dist + 0.01);
    distortion = min(distortion, 0.5); // Clamp
    
    // Offset UV towards center
    vec2 warpedUV = uv + normalize(toCenter) * distortion;
    
    // Sample scene at warped coordinates
    FragColor = texture(sceneTexture, warpedUV);
    
    // Black out event horizon
    if (dist < eventHorizonRadius) {
        FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
```

**Parameters:**
| Parameter | Range | Effect |
|-----------|-------|--------|
| `lensStrength` | 0.0 - 1.0 | Warping intensity |
| `lensRadius` | 0.1 - 2.0 | Effect extent (in world units) |
| `eventHorizon` | 0.0 - 1.0 | Size of black center (0 = none) |

---

### 4. CORONA / SOLAR GLOW

**The Problem:** Create a graduated glow around an emissive surface.

**Approach:** Combine Fresnel + Radial Gradient + Bloom

```glsl
// Corona = base emission + fresnel rim + radial falloff
float fresnel = pow(1.0 - dot(viewDir, normal), rimPower);
float corona = coronaIntensity * fresnel;

// Add radial falloff for softer outer edge
float radialFactor = 1.0 - smoothstep(innerRadius, outerRadius, dist);
corona *= radialFactor;

// Color gradient from center (white/yellow) to edge (orange/red)
vec3 coronaColor = mix(innerColor, outerColor, 1.0 - radialFactor);

FragColor = vec4(baseColor + coronaColor * corona, 1.0);
```

---

### 5. DOPPLER SHIFT (For Accretion Disk)

**The Problem:** Color shift based on motion towards/away from viewer.

**Implementation:**
```glsl
// velocity.z = towards viewer (+) or away (-)
float dopplerFactor = 1.0 + velocity.z / c; // Simplified

// Shift wavelength
float shiftedWavelength = baseWavelength / dopplerFactor;

// Convert wavelength to RGB (simplified)
vec3 dopplerColor = wavelengthToRGB(shiftedWavelength);

// Also affects brightness (relativistic beaming)
float brightness = pow(dopplerFactor, 3.0); // Brighter when approaching
FragColor = vec4(dopplerColor * brightness, 1.0);
```

---

### 6. SILHOUETTE / DEPTH MASK

**The Problem:** Render a pure black object while preserving the ability to see behind it.

**Approach 1:** Render with solid black, write to depth buffer
```glsl
// Fragment shader
FragColor = vec4(0.0, 0.0, 0.0, 1.0);
gl_FragDepth = gl_FragCoord.z; // Normal depth write
```

**Approach 2:** For true "hole in space", render background distortion only
- Don't render the sphere itself
- Only apply gravitational lensing in that area

---

## Minecraft 1.21 Fabric Implementation Strategy

### Current State (Your Project)
- ✅ `FieldRenderLayers.java` - Custom RenderLayer definitions
- ✅ `RenderLayerFactory.java` - Layer factory methods
- ✅ Blend modes: NORMAL, ADD, MULTIPLY, SCREEN
- ❌ No custom GLSL shaders (.vsh/.fsh files)
- ❌ No custom ShaderProgram registration
- ❌ No post-processing pipeline

### Minecraft 1.21 Shader Architecture

**Key Classes:**
- `net.minecraft.client.gl.ShaderProgram` - Shader program wrapper
- `net.minecraft.client.render.RenderLayer` - Render state configuration
- `net.minecraft.client.render.RenderPhase.ShaderProgram` - Shader phase
- `net.minecraft.client.render.GameRenderer` - Manages shader loading

**Shader File Location:**
```
src/main/resources/assets/<modid>/shaders/core/
├── my_shader.json       # Shader program definition
├── my_shader.vsh        # Vertex shader
└── my_shader.fsh        # Fragment shader
```

**Shader JSON Format (1.21):**
```json
{
    "blend": {
        "func": "add",
        "srcrgb": "srcalpha",
        "dstrgb": "1"
    },
    "vertex": "the-virus-block:my_shader",
    "fragment": "the-virus-block:my_shader",
    "attributes": [
        "Position",
        "Color",
        "UV0",
        "Normal"
    ],
    "samplers": [
        { "name": "Sampler0" }
    ],
    "uniforms": [
        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [...] },
        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [...] },
        { "name": "RimColor", "type": "float", "count": 3, "values": [1.0, 1.0, 1.0] },
        { "name": "RimPower", "type": "float", "count": 1, "values": [2.0] },
        { "name": "RimIntensity", "type": "float", "count": 1, "values": [1.0] },
        { "name": "GameTime", "type": "float", "count": 1, "values": [0.0] }
    ]
}
```

### Recommended Implementation Order

#### Phase 1: Fresnel Rim Effect 🎯
1. Create `fresnel_rim.json`, `.vsh`, `.fsh` in shaders/core/
2. Create `FresnelRenderLayer` extending RenderLayer
3. Register shader program on client init
4. Pass uniforms: rimColor, rimPower, rimIntensity
5. Add GUI controls for rim parameters

#### Phase 2: Bloom Post-Processing
1. Create render targets for multi-pass rendering
2. Implement threshold, blur, composite shaders
3. Hook into world render events
4. Add bloom toggle and intensity controls

#### Phase 3: Advanced Effects
1. Gravitational lensing (screen-space)
2. Corona with color gradients
3. Doppler for animated rings

---

## Proposed Parameter Structure

```java
// New fields for SphereShape or Material
public record ShaderEffects(
    // Fresnel Rim
    boolean rimEnabled,
    float rimPower,          // 1.0 - 10.0
    float rimIntensity,      // 0.0 - 5.0
    int rimColor,            // ARGB packed
    
    // Bloom (global post-process, but could be per-object flag)
    boolean bloomContributor, // Does this object contribute to bloom?
    float emissionIntensity,  // 0.0 - 3.0, how much it glows
    
    // Advanced (future)
    boolean lensDistortion,
    float lensStrength
) {}
```

---

## Summary: Effects vs. Feasibility

| Effect | Complexity | Performance | Priority |
|--------|------------|-------------|----------|
| **Fresnel Rim** | Medium | Low cost | ⭐⭐⭐ HIGH |
| **Bloom/Glow** | High | Medium cost | ⭐⭐ MEDIUM |
| **Gravitational Lensing** | High | Low-Medium | ⭐ LOW |
| **Doppler Shift** | Medium | Low cost | ⭐ LOW |
| **Corona Gradient** | Medium | Low cost | ⭐⭐ MEDIUM |

---

## Next Steps

1. **Prototype Fresnel Rim Shader** - Create minimal vertex/fragment shaders
2. **Test with existing RenderLayer system** - Integrate into FieldRenderLayers
3. **Add uniform passing mechanism** - Pass rim parameters from Java
4. **Build GUI controls** - Expose parameters in ShapeWidgetSpec
5. **Iterate and expand** - Add bloom, then advanced effects

---

## References

- LearnOpenGL Bloom Tutorial: learnopengl.com/Advanced-Lighting/Bloom
- Fresnel Effect Tutorial: kylehalladay.com/blog/tutorial/2014/02/18/fresnel.html
- Minecraft Shader Modding: fabricmc.net/wiki/tutorial:shaders
- Eric Bruneton's Black Hole Shader: ebruneton.github.io/black_hole_shader
- Unity Rim Lighting: ronja-tutorials.com/post/012-fresnel/
