# GPU Shockwave - SOLUTION FOUND ✅

## Date: 2024-12-30

---

## 🎉 THE SAINT GRAAL: PostEffectPassMixin

The solution was to **mixin into `PostEffectPass.render()`** and replace the uniform buffer
in the `uniformBuffers` map BEFORE the pass executes.

### The Key Discovery

`PostEffectPass` has a field:
```java
private final Map<String, GpuBuffer> uniformBuffers;
```

This map holds the GPU buffers for all uniform blocks declared in the JSON.
By replacing the buffer for `ShockwaveConfig` with a new one containing our
current Java values, the shader receives dynamic data every frame!

---

## The Solution: PostEffectPassMixin

```java
@Mixin(PostEffectPass.class)
public class PostEffectPassMixin {
    
    @Shadow @Final private Map<String, GpuBuffer> uniformBuffers;
    @Shadow @Final private String id;
    
    @Inject(method = "render", at = @At("HEAD"))
    private void updateShockwaveUniforms(...) {
        if (!ShockwavePostEffect.isEnabled()) return;
        if (!id.contains("shockwave")) return;
        if (!uniformBuffers.containsKey("ShockwaveConfig")) return;
        
        // Create new buffer with current Java values
        GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
            () -> "ShockwaveConfig Dynamic",
            16,
            builder.get()
        );
        
        // Replace in map - THE KEY!
        uniformBuffers.put("ShockwaveConfig", newBuffer);
    }
}
```

---

## Why This Works

1. `PostEffectProcessor` loads passes from JSON with static uniform values
2. Each pass has a `uniformBuffers` map with pre-created GPU buffers
3. Our mixin intercepts `render()` BEFORE the pass uses those buffers
4. We replace the buffer with fresh data from Java
5. The shader receives our dynamic values!

---

## Architecture Summary

```
/shockwavegpu radius 50
         │
         ▼
┌─────────────────────────────┐
│ ShockwavePostEffect         │
│   currentRadius = 50        │  
│   getCurrentRadius() → 50   │
└─────────────────────────────┘
         │
         ▼ (each frame)
┌─────────────────────────────┐
│ PostEffectPassMixin         │ ◄── THE KEY MIXIN!
│   uniformBuffers.put(       │
│     "ShockwaveConfig",      │
│     newBufferWithRadius50   │
│   )                         │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ PostEffectPass.render()     │
│   Uses updated buffer!      │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ shockwave_ring.fsh          │
│   RingRadius = 50.0 ✅      │
└─────────────────────────────┘
```

---

## Commands Working

- `/shockwavegpu` - Toggle effect
- `/shockwavegpu trigger` - Start animation (ring expands from 0)
- `/shockwavegpu radius <n>` - Set static radius
- `/shockwavegpu thickness <n>` - Set ring thickness
- `/shockwavegpu intensity <n>` - Set glow intensity
- `/shockwavegpu speed <n>` - Set animation speed
- `/shockwavegpu maxradius <n>` - Set max animation radius
- `/shockwavegpu status` - Show current state

---

## Files Changed

| File | Purpose |
|------|---------|
| `PostEffectPassMixin.java` | **THE KEY** - Injects dynamic uniforms into PostEffectPass |
| `ShockwavePostEffect.java` | Manages state and animation |
| `shockwave_ring.fsh` | GPU shader with depth-aware ring |
| `shockwave_ring.json` | PostEffect configuration |
| `WorldRendererShockwaveMixin.java` | Injects pass into FrameGraph |

---

## Learnings for Future

This pattern can be applied to ANY PostEffectProcessor to make its uniforms dynamic:

1. Declare uniform block in JSON
2. Use `layout(std140) uniform BlockName { ... }` in shader
3. Create mixin into `PostEffectPass.render()`
4. Shadow `uniformBuffers` map
5. Replace buffer with dynamic values at HEAD of render()

This is the general solution for **dynamic post-processing uniforms in Minecraft 1.21.6**!
