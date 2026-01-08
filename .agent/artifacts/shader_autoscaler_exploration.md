# Shader Autoscaler Exploration 🚀

> **Mission**: Analyze and propose solutions for the first-enable shader lag (Energy Orb V3/V7)

---

## 1. Problem Statement

When enabling the shader orb for the first time, there's a noticeable lag/spike. This is caused by:

1. **JIT Shader Compilation**: OpenGL compiles shaders on first use, causing a one-time spike
2. **Include Resolution Overhead**: The `ShaderPreprocessor` resolves 15+ includes at runtime
3. **Effect Complexity**: `field_visual.fsh` is 1005 lines with 8 effect versions (V1-V7, Geodesic)
4. **UBO Initialization**: First render requires CPU→GPU buffer allocation

---

## 2. Current Architecture Analysis

### 2.1 Shader Loading Flow
```
ShaderLoader.getSource()
    ↓ (mixin intercepts)
ShaderLoaderMixin.theVirusBlock$preprocessShader()
    ↓
ShaderPreprocessor.process()      ← Real-time #include resolution
    ↓
Regex + File I/O for each include
    ↓
Compiled source → OpenGL
    ↓
glCompileShader() ← FIRST-TIME LAG HERE
```

### 2.2 Effect Dispatcher (field_visual.fsh)
```glsl
// Lines 350-425: Single giant main() with version branching
void main() {
    if (effectType == EFFECT_ENERGY_ORB) {
        if (Version >= 7.0) { ... renderPulsarV7(...) }
        else if (Version >= 6.0) { ... renderPulsarV6(...) }
        else if (Version >= 5.0) { ... renderEnergyOrbPulsarProjected(...) }
        else if (Version >= 3.0) { ... renderEnergyOrbV3(...) }
        else if (Version >= 2.0) { ... renderEnergyOrbV2(...) }
        else { ... V1 ... }
    }
    else if (effectType == EFFECT_GEODESIC) { ... }
}
```

**Problem**: All effect code is compiled even if only one version is used.

### 2.3 Current Warm-up (`JoinWarmupManager.java`)
- Tessellates sphere meshes (CPU-side)
- Loads profiles
- **Does NOT warm up shaders!**

---

## 3. Proposed Solutions

### 3.1 Shader Warm-up Strategy (Quick Win)

**Concept**: Force shader compilation during join warm-up by rendering a single transparent frame.

```java
// New: ShaderWarmupManager.java (~80 lines)
public final class ShaderWarmupManager {
    
    private static final EffectType[] PRIORITY_EFFECTS = {
        EffectType.ENERGY_ORB_V7,  // Most complex, warm this first
        EffectType.ENERGY_ORB_V3,
        EffectType.GEODESIC
    };
    
    public static void warmupShaders() {
        // Execute minimal render pass with alpha=0 to trigger compilation
        for (EffectType effect : PRIORITY_EFFECTS) {
            warmupEffect(effect);
        }
    }
    
    private static void warmupEffect(EffectType effect) {
        FieldVisualConfig dummyConfig = FieldVisualConfig.builder()
            .effectType(effect.id())
            .radius(0.001f)  // Tiny = almost no work
            .intensity(0.0f) // Invisible
            .build();
        
        // Trigger shader loading through normal path
        FieldVisualPostEffect.preload(dummyConfig);
    }
}
```

**Integration**:
```java
// JoinWarmupManager.java, line 71
asyncTask = CompletableFuture.runAsync(() -> {
    // Existing tessellation...
    warmupSphere(15.0f, 24, 32);
    
    // NEW: Shader warm-up (must run on render thread)
    MinecraftClient.getInstance().execute(() -> {
        ShaderWarmupManager.warmupShaders();
        Logging.RENDER.topic("warmup").info("Shader programs compiled");
    });
});
```

**Effort**: ~80 lines Java | **Impact**: Eliminates first-enable lag

---

### 3.2 Preprocessor Caching (Medium Effort)

**Concept**: Cache preprocessed shader sources to avoid repeated #include resolution.

```java
// Enhanced ShaderPreprocessor.java
public final class ShaderPreprocessor {
    
    // NEW: Cache processed sources
    private static final Map<String, String> PREPROCESSED_CACHE = new ConcurrentHashMap<>();
    
    public static String process(String source, Identifier baseId) {
        String cacheKey = baseId.toString();
        
        // Check cache first
        String cached = PREPROCESSED_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Process and cache
        String processed = processIncludes(source, baseId, new HashSet<>(), 0);
        PREPROCESSED_CACHE.put(cacheKey, processed);
        return processed;
    }
    
    // Called on resource reload
    public static void invalidateCache() {
        PREPROCESSED_CACHE.clear();
    }
}
```

**Effort**: ~20 lines | **Impact**: 10-50ms per shader reload saved

---

### 3.3 Effect LOD System (OOP Pattern Opportunity)

**Concept**: Separate shaders per effect version, enabling selective loading.

#### Current Problem
```
field_visual.fsh (1005 lines, 47KB)
├── V1 code (~200 lines)
├── V2 code (~300 lines)  
├── V3 code (~400 lines)   ← All compiled even if unused!
├── V5/V6/V7 code (~800 lines)
└── Geodesic code (~400 lines)
```

#### Proposed: Effect Registry Pattern
```java
// Abstract base for all post-processing effects
public abstract class AbstractFieldEffect {
    abstract String getShaderPath();
    abstract int getComplexityLevel();  // 1-5 for LOD
    abstract void bindUniforms(Std140Builder builder);
}

// Individual effect implementations
public class EnergyOrbV3Effect extends AbstractFieldEffect {
    @Override String getShaderPath() { return "post/effects/energy_orb_v3.fsh"; }
    @Override int getComplexityLevel() { return 3; }
}

public class EnergyOrbV7Effect extends AbstractFieldEffect {
    @Override String getShaderPath() { return "post/effects/pulsar_v7.fsh"; }
    @Override int getComplexityLevel() { return 5; } // Most complex
}

// Registry with lazy loading
public class FieldEffectRegistry {
    private static final Map<EffectType, AbstractFieldEffect> EFFECTS = new ConcurrentHashMap<>();
    private static final Map<EffectType, PostEffectProcessor> LOADED_SHADERS = new ConcurrentHashMap<>();
    
    public static AbstractFieldEffect get(EffectType type) {
        return EFFECTS.computeIfAbsent(type, FieldEffectRegistry::create);
    }
    
    // Only load shader when first used
    public static PostEffectProcessor getProcessor(EffectType type) {
        return LOADED_SHADERS.computeIfAbsent(type, t -> {
            AbstractFieldEffect effect = get(t);
            return loadShader(effect.getShaderPath());
        });
    }
}
```

#### Split Shader Files
```
shaders/post/effects/
├── energy_orb_v2.fsh   (~300 lines)
├── energy_orb_v3.fsh   (~450 lines)
├── pulsar_v5.fsh       (~250 lines)
├── pulsar_v6.fsh       (~350 lines)
├── pulsar_v7.fsh       (~500 lines)
├── geodesic_v1.fsh     (~400 lines)
└── field_dispatch.fsh  (~100 lines, just picks which effect)
```

**Effort**: ~400 lines Java + Shader restructuring | **Impact**: 50-80% smaller per-effect compile

---

### 3.4 Adaptive LOD with Header "Onion" (Advanced)

**Concept**: Use the first 2 vec4s as a "header" that tells the shader what to render.

#### Vec4 Header Protocol
```glsl
// Slot 0: Effect Metadata Header
// x = effectType (1.0, 2.0, 3.0, etc.)
// y = lodLevel (0=full, 1=medium, 2=fast, 3=minimal)
// z = featureFlags (bitfield: 0x01=corona, 0x02=rays, 0x04=noise)
// w = qualityMultiplier (0.0-1.0 adaptive scaling)

// Slot 1: Runtime Intelligence Header
// x = distanceFactor (0=near, 1=far - for LOD selection)
// y = frameTimeBudget (ms remaining for this effect)
// z = previousFrameTime (ms - for adaptive adjustment)
// w = warmupPhase (0.0=cold, 1.0=warm - for progressive detail)
```

#### Shader-Side Adaptive Logic
```glsl
void main() {
    // Parse header
    float lodLevel = HeaderSlot0.y;
    float featureFlags = HeaderSlot0.z;
    
    bool hasCorona = mod(featureFlags, 2.0) >= 1.0;
    bool hasRays = mod(featureFlags / 2.0, 2.0) >= 1.0;
    bool hasNoise = mod(featureFlags / 4.0, 2.0) >= 1.0;
    
    // LOD-based simplification
    if (lodLevel >= 2.0) {
        // Fast path: skip expensive noise, use solid color
        fieldEffect = vec4(primaryColor * intensity, alpha);
        return;
    }
    
    // Full render path
    if (hasNoise) {
        // Expensive 4D noise calculation
    }
}
```

#### Java-Side Frame Budget Controller
```java
public class ShaderFrameBudgetController {
    private static final float TARGET_FRAME_MS = 16.67f; // 60 FPS
    private static final float EFFECT_BUDGET_RATIO = 0.3f; // 30% for effects
    
    private float lastEffectTime = 0.0f;
    private float currentLOD = 0.0f;
    
    public float getLODLevel() {
        float budget = TARGET_FRAME_MS * EFFECT_BUDGET_RATIO;
        
        if (lastEffectTime > budget * 1.5f) {
            // Exceeded budget significantly - reduce quality
            currentLOD = Math.min(3.0f, currentLOD + 0.5f);
        } else if (lastEffectTime < budget * 0.5f) {
            // Under budget - can increase quality
            currentLOD = Math.max(0.0f, currentLOD - 0.1f);
        }
        
        return currentLOD;
    }
}
```

**Effort**: ~200 lines Java + ~100 lines GLSL | **Impact**: Adaptive quality, no spikes

---

### 3.5 Audit Script for Optimal Loading Order

**Concept**: Profile shader compilation times to determine optimal warm-up sequence.

```python
# scripts/shader_profiler.py
import subprocess
import time
import json

SHADERS = [
    "pulsar_v7.glsl",     # Most complex
    "energy_orb_v3.glsl",
    "pulsar_v6.glsl",
    "geodesic_v1.glsl",
    "energy_orb_v2.glsl",
    # ... etc
]

def profile_shader_compile(shader_path):
    """Measures compile time using glGetProgramiv(GL_COMPILE_STATUS)"""
    # Would need a test harness that loads/compiles each shader
    pass

def find_optimal_order():
    """
    Determines the best warm-up order based on:
    1. Complexity (lines of code, includes)
    2. Compile time (measured)
    3. Usage frequency (from config)
    """
    results = []
    for shader in SHADERS:
        t = profile_shader_compile(shader)
        results.append({"shader": shader, "compile_ms": t})
    
    # Sort by compile time descending - warm heaviest first
    return sorted(results, key=lambda x: -x["compile_ms"])
```

**Effort**: ~150 lines Python | **Impact**: Optimized warm-up sequence

---

## 4. Implementation Plan

### Phase 1: Quick Wins (1-2 hours)
1. **Add shader warm-up** to `JoinWarmupManager`
2. **Add preprocessor cache** to `ShaderPreprocessor`
3. Update warm-up overlay to show "Loading Shaders..."

### Phase 2: Effect Registry (4-6 hours)  
1. Create `AbstractFieldEffect` base class
2. Implement effect-specific classes
3. Refactor `field_visual.fsh` into separate files
4. Wire up lazy loading

### Phase 3: Adaptive LOD (8-12 hours)
1. Design header protocol
2. Implement `ShaderFrameBudgetController`
3. Add LOD branching to shaders
4. Create profiling dashboard

---

## 5. OOP Patterns Identified

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Strategy** | `AbstractFieldEffect` + per-version implementations | Swappable rendering strategies |
| **Registry/Factory** | `FieldEffectRegistry` | Lazy loading, centralized management |
| **Flyweight** | Shared preprocessed shader cache | Memory efficiency |
| **Observer** | Frame budget feedback | Adaptive quality |
| **Template Method** | Base warmup + effect-specific overrides | Consistent warm-up flow |

---

## 6. Decision Points / Questions

1. **Split shaders vs. single dispatcher?**
   - Split: Faster per-effect compile, more files to manage
   - Single: Simpler, but always compiles everything

2. **Warm-up timing?**
   - Join screen: Best UX, player sees progress
   - Main menu: Longer initial load but no in-game lag
   - Lazy (first use): Simple but causes the spike

3. **LOD granularity?**
   - Per-effect (V7 vs V3): Coarse, easy to implement
   - Per-feature (corona, rays, noise): Fine, complex but powerful

4. **Header protocol worth the complexity?**
   - Yes if adaptive quality is required
   - No if simple preloading resolves the issue

---

## 7. Size Estimate

| Component | Lines |
|-----------|-------|
| ShaderWarmupManager | ~80 |
| Preprocessor caching | ~20 |
| AbstractFieldEffect | ~60 |
| FieldEffectRegistry | ~120 |
| Per-effect classes (6×40) | ~240 |
| Frame budget controller | ~100 |
| Shader LOD branching | ~50 |
| **Total** | **~670 lines** |

Can be phased to fit within 400 lines for initial version by implementing only Phase 1 + partial Phase 2.

---

## 8. Recommended Starting Point

**Implement Phase 1 first** - this will likely resolve the immediate problem with minimal code:

1. Add `ShaderWarmupManager.warmupShaders()` → ~80 lines
2. Call it from `JoinWarmupManager` during "Loading Effects" phase
3. Test if first-enable lag is eliminated

If lag persists, proceed with Phase 2 (Effect Registry).
