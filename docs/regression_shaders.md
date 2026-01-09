# Regression Shaders - Golden Tests

> **Purpose:** Minimal shaders that validate base UBO functionality.
> **Rule:** If these break, stop and fix before proceeding.

---

## 1. Frame Time Test

**File:** `test_frame_time.fsh`

**Tests:** FrameUBO binding and time flow

```glsl
#version 150

uniform sampler2D InSampler;
in vec2 texCoord;
out vec4 fragColor;

#include "include/ubo/frame.glsl"

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    
    // Pulsate based on time
    float pulse = sin(FrameTime.x * 2.0) * 0.5 + 0.5;
    
    // Tint scene with time-based color
    vec3 tint = vec3(pulse, 0.3, 1.0 - pulse);
    
    fragColor = vec4(mix(scene, tint, 0.3), 1.0);
}
```

**Pass criteria:**
- Output pulses smoothly
- No jitter or stutter
- Time increases continuously

---

## 2. Camera Ray Test

**File:** `test_camera_ray.fsh`

**Tests:** CameraUBO binding and matrix correctness

```glsl
#version 150

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
in vec2 texCoord;
out vec4 fragColor;

#include "include/ubo/frame.glsl"
#include "include/ubo/camera.glsl"

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    
    // Reconstruct ray direction from matrices
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 clipPos = vec4(ndc, 1.0, 1.0);
    vec4 worldPos = InvViewProj * clipPos;
    vec3 rayDir = normalize(worldPos.xyz / worldPos.w - CameraPosition.xyz);
    
    // Visualize ray direction as color
    vec3 rayColor = rayDir * 0.5 + 0.5;
    
    fragColor = vec4(mix(scene, rayColor, 0.5), 1.0);
}
```

**Pass criteria:**
- Colors change when looking around
- Horizon is consistent
- No sudden jumps or inversions

---

## 3. Config Read Test

**File:** `test_config_read.fsh`

**Tests:** FieldVisualConfigUBO binding and preset values

```glsl
#version 150

uniform sampler2D InSampler;
in vec2 texCoord;
out vec4 fragColor;

#include "include/ubo/frame.glsl"
#include "include/ubo/effects/field_visual_config.glsl"

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    
    // Draw colored bars based on config colors
    float y = texCoord.y;
    vec3 configColor;
    
    if (y < 0.2) {
        configColor = PrimaryColor.rgb;
    } else if (y < 0.4) {
        configColor = SecondaryColor.rgb;
    } else if (y < 0.6) {
        configColor = TertiaryColor.rgb;
    } else if (y < 0.8) {
        configColor = HighlightColor.rgb;
    } else {
        configColor = RayColor.rgb;
    }
    
    // Show bars on left side
    if (texCoord.x < 0.1) {
        fragColor = vec4(configColor, 1.0);
    } else {
        fragColor = vec4(scene, 1.0);
    }
}
```

**Pass criteria:**
- Left side shows 5 distinct color bands
- Colors match preset values
- Changing preset updates colors

---

## 4. Combined Minimal Effect

**File:** `test_combined.fsh`

**Tests:** All base UBOs working together

```glsl
#version 150

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };

#include "include/ubo/frame.glsl"
#include "include/ubo/camera.glsl"
#include "include/ubo/effects/field_visual_config.glsl"

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    float time = FrameTime.x;
    
    // Simple sphere glow at CenterAndRadius
    vec3 center = CenterAndRadius.xyz;
    float radius = CenterAndRadius.w;
    
    // Build ray
    vec2 ndc = texCoord * 2.0 - 1.0;
    ndc.x *= CameraForward.w;  // aspect
    vec4 clipPos = vec4(ndc, 1.0, 1.0);
    vec4 worldPos = InvViewProj * clipPos;
    vec3 rayDir = normalize(worldPos.xyz / worldPos.w - CameraPosition.xyz);
    
    // Ray-sphere intersection (simplified)
    vec3 oc = CameraPosition.xyz - center;
    float b = dot(oc, rayDir);
    float c = dot(oc, oc) - radius * radius;
    float disc = b * b - c;
    
    vec3 finalColor = scene;
    if (disc > 0.0) {
        float t = -b - sqrt(disc);
        if (t > 0.0) {
            // Hit - apply pulsing glow
            float pulse = sin(time * AnimSpeed * 2.0) * 0.3 + 0.7;
            vec3 glow = PrimaryColor.rgb * pulse * Intensity;
            finalColor = scene + glow * 0.5;
        }
    }
    
    fragColor = vec4(finalColor, 1.0);
}
```

**Pass criteria:**
- Sphere visible at configured position
- Pulses with time
- Uses configured colors
- Moves when preset changes position

---

## Test Execution

### Manual
1. Enable test shader via command or config
2. Observe output matches criteria
3. Move camera, change presets
4. Verify no visual glitches

### Automated (Future)
- Screenshot comparison
- Output hash validation
- CI integration

---

## When to Run

- After any UBO layout change
- After any binding index change
- After any GLSL include modification
- Before merging to main

---

> **Golden Rule:** If a golden test fails, the refactor is broken. Fix it before continuing.
