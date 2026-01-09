# Post-Effect JSON Requirements

## Critical: BlitConfig Uniform for Blit Passes

When using `minecraft:post/blit` for the final pass that copies to `minecraft:main`,
you **MUST** include the `BlitConfig` uniform with `ColorModulate` set to `[1,1,1,1]`.

### ❌ WRONG (Black Screen)
```json
{
    "vertex_shader": "minecraft:post/blit",
    "fragment_shader": "minecraft:post/blit",
    "inputs": [
        { "sampler_name": "In", "target": "swap" }
    ],
    "output": "minecraft:main"
}
```

### ✅ CORRECT
```json
{
    "vertex_shader": "minecraft:post/blit",
    "fragment_shader": "minecraft:post/blit",
    "inputs": [
        { "sampler_name": "In", "target": "swap" }
    ],
    "uniforms": {
        "BlitConfig": [
            {
                "name": "ColorModulate",
                "type": "vec4",
                "value": [1, 1, 1, 1]
            }
        ]
    },
    "output": "minecraft:main"
}
```

## Why?

The `minecraft:post/blit` shader multiplies the input color by `ColorModulate`.
Without this uniform defined, it defaults to `(0,0,0,0)` which results in a **completely black output**.

---

## Uniform Type: `vec4` vs Individual Floats

### Option 1: Using `vec4` (RECOMMENDED)

`vec4` groups related parameters together, is more readable, and more performant:

```json
"uniforms": {
    "MyConfig": [
        {
            "name": "CameraPosTime",
            "type": "vec4",
            "value": [0.0, 64.0, 0.0, 0.0]
        },
        {
            "name": "EffectFlags",
            "type": "vec4",
            "value": [0.0, 1.0, 1.0, 1.0]
        }
    ]
}
```

**Shader UBO:**
```glsl
layout(std140) uniform MyConfig {
    vec4 CameraPosTime;  // xyz = position, w = time
    vec4 EffectFlags;    // x = count, yzw = enable flags
};
```

### Option 2: Individual Floats (Legacy/Fallback)

Only use this if you encounter issues with `vec4` in specific contexts:

```json
"uniforms": {
    "VirusBlockParams": [
        { "name": "CameraPosTime_x", "type": "float", "value": 0.0 },
        { "name": "CameraPosTime_y", "type": "float", "value": 64.0 },
        { "name": "CameraPosTime_z", "type": "float", "value": 0.0 },
        { "name": "CameraPosTime_w", "type": "float", "value": 0.0 }
    ]
}
```

**Note:** The shader UBO must match - either use `vec4` or matching individual floats.

### Key Point: JSON and Shader UBO Must Match

- If JSON uses `"type": "vec4"`, shader must use `vec4 name;`  
- If JSON uses individual floats, shader must use matching `float name_x;` etc.
- **Java code writes vec4s with `putVec4()`** - MC's post-effect system handles the mapping

---

## Discovered

- **Date**: 2026-01-08
- **Issue**: virus_block.json blit pass missing BlitConfig caused black screen
- **Fixed by**: Adding BlitConfig with ColorModulate = [1,1,1,1]
