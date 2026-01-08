# 🚀 Fresnel Rim Shader - Technical Specification

## Research Complete ✅

Based on native JAR analysis of Minecraft 1.21.6, we now have complete understanding of the shader system.

---

## Key Classes Discovered

### 1. `com.mojang.blaze3d.pipeline.RenderPipeline`
The core pipeline class. Contains:
- `location` - Identifier for the pipeline
- `vertexShader` - Identifier for vertex shader
- `fragmentShader` - Identifier for fragment shader
- `shaderDefines` - Preprocessor defines
- `samplers` - Texture samplers list
- `uniforms` - UniformDescription list
- `vertexFormat` - VertexFormat
- `blendFunction` - Optional blend function
- Plus depth/cull/write settings

### 2. `net.minecraft.client.gl.RenderPipelines`
Static registry of all pipelines. Contains:
- `ENTITY_TRANSLUCENT` - The pipeline we want to base our shader on
- `ENTITY_SNIPPET` - Reusable snippet for entity rendering
- `public static RenderPipeline register(RenderPipeline)` - Registration method!

### 3. `RenderPipeline.Builder`
Fluent builder with methods:
```java
.withLocation(String)
.withVertexShader(String)           // e.g., "modid:fresnel_entity"
.withFragmentShader(String)
.withShaderDefine(String)           // Preprocessor define
.withShaderDefine(String, int)
.withShaderDefine(String, float)
.withSampler(String)                // e.g., "Sampler0"
.withUniform(String, UniformType)   // Custom uniforms!
.withDepthTestFunction(DepthTestFunction)
.withCull(boolean)
.withBlend(BlendFunction)
.withColorWrite(boolean, boolean)
.withDepthWrite(boolean)
.withVertexFormat(VertexFormat, DrawMode)
.build()
```

### 4. `RenderPipeline.Snippet`
Reusable pipeline configuration chunks. Existing snippets:
- `ENTITY_SNIPPET` - Standard entity rendering
- `TRANSFORMS_PROJECTION_FOG_SNIPPET` - Matrices + fog
- etc.

---

## Implementation Architecture

### Step 1: Create Shader Files

**Location**: `src/main/resources/assets/the-virus-block/shaders/`

```
shaders/
├── core/
│   ├── fresnel_entity.fsh      # Fragment shader
│   └── fresnel_entity.vsh      # Vertex shader
```

**Note**: In 1.21.6, shaders are loaded by Identifier, NOT by JSON definition.
The `withVertexShader("the-virus-block:fresnel_entity")` will look for:
`assets/the-virus-block/shaders/core/fresnel_entity.vsh`

### Step 2: Create the RenderPipeline

```java
package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import com.mojang.blaze3d.vertex.VertexFormat;

public class FresnelPipelines {
    
    public static final RenderPipeline FRESNEL_ENTITY_TRANSLUCENT;
    
    static {
        FRESNEL_ENTITY_TRANSLUCENT = RenderPipelines.register(
            RenderPipeline.builder(
                RenderPipelines.ENTITY_SNIPPET  // Inherit entity rendering setup
            )
            .withLocation("the-virus-block:fresnel_entity")
            .withVertexShader("the-virus-block:fresnel_entity")
            .withFragmentShader("the-virus-block:fresnel_entity")
            // Custom uniforms for Fresnel
            .withUniform("RimColor", UniformType.VEC3)
            .withUniform("RimPower", UniformType.FLOAT)
            .withUniform("RimIntensity", UniformType.FLOAT)
            // Standard entity setup
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 
                              VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.LEQUAL)
            .withCull(true)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .build()
        );
    }
    
    public static void init() {
        // Static initialization trigger
    }
}
```

### Step 3: Create RenderLayer Using the Pipeline

```java
package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.render.RenderLayer;

public class FresnelRenderLayers {
    
    public static RenderLayer fresnelTranslucent() {
        return RenderLayer.of(
            "fresnel_entity_translucent",
            256,
            FresnelPipelines.FRESNEL_ENTITY_TRANSLUCENT,
            RenderLayer.MultiPhaseParameters.builder()
                // Additional phases if needed
                .build(false)
        );
    }
}
```

### Step 4: GLSL Vertex Shader (fresnel_entity.vsh)

```glsl
#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:light.glsl>

// Vertex attributes
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;    // Overlay
in ivec2 UV2;   // Light
in vec3 Normal;

// Standard uniforms (from ENTITY_SNIPPET)
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;     // For normal transformation
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform int FogShape;

// Outputs to fragment shader
out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
flat out ivec2 texCoord2;
out vec3 vNormal;
out vec3 vViewDir;

void main() {
    // Transform position
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    
    // Fog distance
    vertexDistance = fog_distance(viewPos.xyz, FogShape);
    
    // Transform normal to view space
    vNormal = normalize(NormalMat * Normal);
    
    // View direction (from vertex to camera, in view space camera is at origin)
    vViewDir = normalize(-viewPos.xyz);
    
    // Pass through color with lighting
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, Color);
    
    // Texture coordinates
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;
}
```

### Step 5: GLSL Fragment Shader (fresnel_entity.fsh)

```glsl
#version 150

#moj_import <minecraft:fog.glsl>

// Samplers
uniform sampler2D Sampler0;  // Texture (white for our case)
uniform sampler2D Sampler2;  // Lightmap

// Standard uniforms
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// CUSTOM: Fresnel uniforms
uniform vec3 RimColor;
uniform float RimPower;
uniform float RimIntensity;

// Inputs from vertex shader
in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
flat in ivec2 texCoord2;
in vec3 vNormal;
in vec3 vViewDir;

out vec4 fragColor;

void main() {
    // Sample texture
    vec4 texColor = texture(Sampler0, texCoord0);
    
    // Base color = texture * vertex color * modulator
    vec4 baseColor = texColor * vertexColor * ColorModulator;
    
    // ==========================================
    // FRESNEL RIM CALCULATION
    // ==========================================
    // fresnel = how much the surface faces away from camera
    // 1.0 = perpendicular (edge), 0.0 = facing camera
    float fresnel = 1.0 - max(dot(normalize(vViewDir), normalize(vNormal)), 0.0);
    
    // Apply power for sharpness control
    float rim = pow(fresnel, RimPower);
    
    // Add rim color
    vec3 rimContribution = RimColor * rim * RimIntensity;
    
    // Combine base + rim
    vec3 finalColor = baseColor.rgb + rimContribution;
    
    // Apply lightmap
    vec4 lightMapColor = texelFetch(Sampler2, texCoord2 / 16, 0);
    finalColor *= lightMapColor.rgb;
    
    // Apply fog
    fragColor = linear_fog(vec4(finalColor, baseColor.a), vertexDistance, FogStart, FogEnd, FogColor);
}
```

---

## Uniform Management

### Setting Uniforms at Render Time

```java
public class FresnelUniformManager {
    
    private static Vec3f rimColor = new Vec3f(1f, 1f, 1f);
    private static float rimPower = 3f;
    private static float rimIntensity = 1.5f;
    
    public static void setFresnelParams(float r, float g, float b, float power, float intensity) {
        rimColor = new Vec3f(r, g, b);
        rimPower = power;
        rimIntensity = intensity;
    }
    
    /**
     * Called each frame before rendering fresnel objects.
     * Accesses the currently bound shader and sets uniforms.
     */
    public static void applyUniforms(ShaderProgram shader) {
        shader.getUniform("RimColor").set(rimColor.x, rimColor.y, rimColor.z);
        shader.getUniform("RimPower").set(rimPower);
        shader.getUniform("RimIntensity").set(rimIntensity);
    }
}
```

---

## Integration Points

### 1. Registration (Client Init)

In your `ClientModInitializer`:
```java
@Override
public void onInitializeClient() {
    FresnelPipelines.init();  // Triggers static initializer
}
```

### 2. FieldRenderLayers.java

Add new method:
```java
public static RenderLayer fresnelTranslucent() {
    return FresnelRenderLayers.fresnelTranslucent();
}
```

### 3. LayerRenderer (or ShapePrimitiveRenderer)

Before rendering with fresnel:
```java
if (layer.hasFresnelEffect()) {
    FresnelUniformManager.setFresnelParams(
        layer.fresnelColor(),
        layer.fresnelPower(),
        layer.fresnelIntensity()
    );
    RenderLayer renderLayer = FieldRenderLayers.fresnelTranslucent();
    // Render...
}
```

---

## Data Model

### FresnelEffect Record

```java
public record FresnelEffect(
    boolean enabled,
    float power,        // 1.0 - 10.0
    float intensity,    // 0.0 - 5.0
    float red,          // 0.0 - 1.0
    float green,        // 0.0 - 1.0  
    float blue          // 0.0 - 1.0
) {
    public static final FresnelEffect NONE = new FresnelEffect(false, 3f, 1f, 1f, 1f, 1f);
    public static final FresnelEffect DEFAULT = new FresnelEffect(true, 3f, 1.5f, 1f, 1f, 1f);
    
    public static FresnelEffect corona() {
        return new FresnelEffect(true, 2f, 3f, 1f, 0.5f, 0f); // Orange glow
    }
    
    public static FresnelEffect ice() {
        return new FresnelEffect(true, 4f, 2f, 0.5f, 0.8f, 1f); // Ice blue
    }
}
```

---

## Implementation Files Checklist

| File | Purpose | Status |
|------|---------|--------|
| `shaders/core/fresnel_entity.vsh` | Vertex shader | TODO |
| `shaders/core/fresnel_entity.fsh` | Fragment shader | TODO |
| `FresnelPipelines.java` | Pipeline registration | TODO |
| `FresnelRenderLayers.java` | RenderLayer wrapper | TODO |
| `FresnelUniformManager.java` | Uniform setting | TODO |
| `FresnelEffect.java` | Data model | TODO |
| `FieldRenderLayers.java` | Integration | TODO |

---

## Implementation Order

1. **Create GLSL shaders** - fresnel_entity.vsh and fresnel_entity.fsh
2. **Create FresnelPipelines** - Register the RenderPipeline
3. **Create FresnelRenderLayers** - Create RenderLayer using the pipeline
4. **Create FresnelUniformManager** - Uniform handling
5. **Test basic rendering** - Verify shader loads and renders
6. **Add FresnelEffect data model** - Parameterize
7. **Integrate with LayerRenderer** - Hook into rendering flow
8. **Add GUI controls** - Expose parameters

---

## Risk Mitigation

| Risk | Strategy |
|------|----------|
| Shader compile error | Start with minimal shader, add features incrementally |
| Import path issues | Use `#moj_import <minecraft:fog.glsl>` syntax |
| Uniform not found | Check exact uniform names match between Java and GLSL |
| Pipeline not registered | Ensure `FresnelPipelines.init()` called before first render |

---

## Ready to Implement! 🎯
