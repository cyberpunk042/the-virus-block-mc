# Field Visual Record System - Implementation Plan

> **Reference:** See `POLYMORPHIC_EFFECT_SYSTEM.md` for problem analysis and record definitions.

---

## Current UBO Layout (22 vec4 slots)

| Vec4 | Name | Current Fields |
|------|------|----------------|
| 0 | PositionParams | centerX, centerY, centerZ, radius |
| 1 | PrimaryColor | R, G, B, A |
| 2 | SecondaryColor | R, G, B, A |
| 3 | TertiaryColor | R, G, B, A |
| 4 | AnimParams | phase, speed, intensity, effectType |
| 5 | CoreEdgeParams | coreSize, edgeSharpness, shapeType, reserved |
| 6 | SpiralParams | density, twist, reserved, reserved |
| 7 | GlowLineParams | count, intensity, rayCoronaFlags, version |
| 8 | V2Params | coronaWidth, reserved, reserved, reserved |
| 9 | CameraPos | x(0), y(0), z(0), time |
| 10 | ForwardParams | forwardX, forwardY, forwardZ, aspect |
| 11 | UpParams | upX, upY, upZ, fov |
| 12 | ExtraParams | nearPlane, farPlane, reserved, isFlying |
| 13-16 | InvViewProj | mat4 (4 vec4) |
| 17-20 | ViewProj | mat4 (4 vec4) |
| 21 | RuntimeParams | camMode, debugMode, reserved, reserved |

---

## Proposed Record Groups

### Effect Records (from FieldVisualConfig)

| # | Record | Fields | UBO Slot |
|---|--------|--------|----------|
| 1 | **PositionParams** | centerX, centerY, centerZ, radius | 0 |
| 2 | **ColorParams** | primaryColor (ARGB), secondaryColor (ARGB), tertiaryColor (ARGB) | 1-3 |
| 3 | **AnimParams** | phase, speed, intensity, effectType | 4 |
| 4 | **CoreEdgeParams** | coreSize, edgeSharpness, shapeType | 5 |
| 5 | **SpiralParams** | density, twist | 6 |
| 6 | **GlowLineParams** | count, intensity, showExternalRays, showCorona, version | 7 |
| 7 | **CoronaParams** | width | 8 |

### NEW Effect Records (to add)

| # | Record | Fields | UBO Slot |
|---|--------|--------|----------|
| 8 | **OtherParams** | pulseFrequency, pulseAmplitude, noiseScale, noiseStrength | 9 (replaces CameraPos) |
| 9 | **ScreenEffects** | blackout, vignetteAmount, vignetteRadius, tintAmount | NEW slot |
| 10 | **DistortionParams** | strength, radius, frequency, speed | NEW slot |
| 11 | **BlendParams** | opacity, blendMode, fadeIn, fadeOut | NEW slot |
| 12 | **ReservedParams** | slot1, slot2, slot3, slot4 | NEW slot |

### Camera/Runtime Records (set per frame, not from FieldVisualConfig)

| # | Record | Fields | UBO Slot |
|---|--------|--------|----------|
| 13 | **CameraParams** | posX, posY, posZ, time, forwardX, forwardY, forwardZ, aspect, upX, upY, upZ, fov | 9-11 (after effect records) |
| 14 | **RenderParams** | nearPlane, farPlane, isFlying | 12 |
| 15 | **MatrixParams** | invViewProj, viewProj | 13-20 |
| 16 | **DebugParams** | camMode, debugMode | 21 |

---

## Phase 1: Create Record Types

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualTypes.java`

### Record 1: PositionParams
```java
public record PositionParams(
    float centerX,
    float centerY,
    float centerZ,
    float radius
) {
    public static final PositionParams DEFAULT = new PositionParams(0f, 0f, 0f, 3.0f);
    public PositionParams withCenter(float x, float y, float z) { ... }
    public PositionParams withRadius(float r) { ... }
}
```

### Record 2: ColorParams
```java
public record ColorParams(
    int primaryColor,    // ARGB
    int secondaryColor,  // ARGB
    int tertiaryColor    // ARGB
) {
    public static final ColorParams DEFAULT = new ColorParams(0xFFFFFFFF, 0xFF00FFFF, 0xFF1A0528);
    
    // Extractors for UBO writing
    public float primaryR() { return ((primaryColor >> 16) & 0xFF) / 255f; }
    public float primaryG() { return ((primaryColor >> 8) & 0xFF) / 255f; }
    public float primaryB() { return (primaryColor & 0xFF) / 255f; }
    public float primaryA() { return ((primaryColor >> 24) & 0xFF) / 255f; }
    public float secondaryR() { ... }
    public float secondaryG() { ... }
    public float secondaryB() { ... }
    public float secondaryA() { ... }
    public float tertiaryR() { ... }
    public float tertiaryG() { ... }
    public float tertiaryB() { ... }
    public float tertiaryA() { ... }
    
    public ColorParams withPrimary(int c) { ... }
    public ColorParams withSecondary(int c) { ... }
    public ColorParams withTertiary(int c) { ... }
}
```

### Record 3: AnimParams
```java
public record AnimParams(
    float phase,
    float speed,
    float intensity,
    EffectType effectType
) {
    public static final AnimParams DEFAULT = new AnimParams(0f, 1.0f, 1.2f, EffectType.ENERGY_ORB);
    
    public AnimParams withPhase(float v) { ... }
    public AnimParams withSpeed(float v) { ... }
    public AnimParams withIntensity(float v) { ... }
    public AnimParams withEffectType(EffectType t) { ... }
}
```

### Record 4: CoreEdgeParams
```java
public record CoreEdgeParams(
    float coreSize,       // 0.05 - 0.5
    float edgeSharpness,  // 1 - 10
    float shapeType       // 0=sphere, 1=torus, 2=cylinder, 3=prism
) {
    public static final CoreEdgeParams DEFAULT = new CoreEdgeParams(0.15f, 4.0f, 0f);
    
    public CoreEdgeParams withCoreSize(float v) { ... }
    public CoreEdgeParams withEdgeSharpness(float v) { ... }
    public CoreEdgeParams withShapeType(float v) { ... }
}
```

### Record 5: SpiralParams
```java
public record SpiralParams(
    float density,  // 3 - 12
    float twist     // 1 - 10
) {
    public static final SpiralParams DEFAULT = new SpiralParams(5.0f, 5.0f);
    
    public SpiralParams withDensity(float v) { ... }
    public SpiralParams withTwist(float v) { ... }
}
```

### Record 6: GlowLineParams
```java
public record GlowLineParams(
    float count,           // 8 - 24
    float intensity,       // 0 - 1
    boolean showExternalRays,
    boolean showCorona,
    int version            // 1 or 2
) {
    public static final GlowLineParams DEFAULT = new GlowLineParams(16.0f, 0.8f, true, true, 1);
    
    // For UBO encoding
    public float rayCoronaFlags() {
        return (showExternalRays ? 1f : 0f) + (showCorona ? 2f : 0f);
    }
    
    public GlowLineParams withCount(float v) { ... }
    public GlowLineParams withIntensity(float v) { ... }
    public GlowLineParams withShowExternalRays(boolean v) { ... }
    public GlowLineParams withShowCorona(boolean v) { ... }
    public GlowLineParams withVersion(int v) { ... }
}
```

### Record 7: CoronaParams
```java
public record CoronaParams(
    float width  // 0.2 - 1.5
) {
    public static final CoronaParams DEFAULT = new CoronaParams(0.5f);
    
    public CoronaParams withWidth(float v) { ... }
}
```

### Record 8: OtherParams (NEW - for expansion)
```java
public record OtherParams(
    float pulseFrequency,
    float pulseAmplitude,
    float noiseScale,
    float noiseStrength
) {
    public static final OtherParams DEFAULT = new OtherParams(1.0f, 0.1f, 1.0f, 0f);
    // withX methods...
}
```

### Record 9: ScreenEffects (NEW)
```java
public record ScreenEffects(
    float blackout,
    float vignetteAmount,
    float vignetteRadius,
    float tintAmount
) {
    public static final ScreenEffects NONE = new ScreenEffects(0f, 0f, 0.5f, 0f);
    // withX methods...
}
```

### Record 10: DistortionParams (NEW)
```java
public record DistortionParams(
    float strength,
    float radius,
    float frequency,
    float speed
) {
    public static final DistortionParams NONE = new DistortionParams(0f, 1.0f, 1.0f, 1.0f);
    // withX methods...
}
```

### Record 11: BlendParams (NEW)
```java
public record BlendParams(
    float opacity,
    int blendMode,
    float fadeIn,
    float fadeOut
) {
    public static final BlendParams DEFAULT = new BlendParams(1.0f, 1, 0f, 0f);
    // withX methods...
}
```

### Record 12: ReservedParams (NEW)
```java
public record ReservedParams(
    float slot1,
    float slot2,
    float slot3,
    float slot4
) {
    public static final ReservedParams DEFAULT = new ReservedParams(0f, 0f, 0f, 0f);
    // withX methods...
}
```

### Record 13: CameraParams (runtime)
```java
public record CameraParams(
    float posX, float posY, float posZ,
    float time,
    float forwardX, float forwardY, float forwardZ,
    float aspect,
    float upX, float upY, float upZ,
    float fov
) {
    public static CameraParams fromGame(...) { ... }
}
```

### Record 14: RenderParams (runtime)
```java
public record RenderParams(
    float nearPlane,
    float farPlane,
    float isFlying
) {
    public static final RenderParams DEFAULT = new RenderParams(0.05f, 1000f, 0f);
}
```

### Record 15: DebugParams (runtime)
```java
public record DebugParams(
    float camMode,
    float debugMode
) {
    public static final DebugParams DEFAULT = new DebugParams(0f, 0f);
}
```

**Dependency:** None

---

## Phase 2: Create UBO Writer

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/uniform/FieldVisualUBOWriter.java`

```java
public final class FieldVisualUBOWriter {
    
    public static void write(Std140Builder builder, 
                             FieldVisualConfig config,
                             PositionParams position,
                             CameraParams camera,
                             RenderParams render,
                             Matrix4f invViewProj,
                             Matrix4f viewProj,
                             DebugParams debug) {
        
        // vec4 0: Position
        builder.putFloat(position.centerX());
        builder.putFloat(position.centerY());
        builder.putFloat(position.centerZ());
        builder.putFloat(position.radius());
        
        // vec4 1-3: Colors
        ColorParams c = config.colors();
        builder.putFloat(c.primaryR());
        builder.putFloat(c.primaryG());
        builder.putFloat(c.primaryB());
        builder.putFloat(c.primaryA());
        // ... secondary, tertiary
        
        // vec4 4: AnimParams
        AnimParams a = config.anim();
        builder.putFloat(a.phase());
        builder.putFloat(a.speed());
        builder.putFloat(a.intensity());
        builder.putFloat(a.effectType().ordinal());
        
        // vec4 5: CoreEdgeParams
        CoreEdgeParams ce = config.coreEdge();
        builder.putFloat(ce.coreSize());
        builder.putFloat(ce.edgeSharpness());
        builder.putFloat(ce.shapeType());
        builder.putFloat(0f);
        
        // vec4 6: SpiralParams
        SpiralParams sp = config.spiral();
        builder.putFloat(sp.density());
        builder.putFloat(sp.twist());
        builder.putFloat(0f);
        builder.putFloat(0f);
        
        // vec4 7: GlowLineParams
        GlowLineParams gl = config.glowLine();
        builder.putFloat(gl.count());
        builder.putFloat(gl.intensity());
        builder.putFloat(gl.rayCoronaFlags());
        builder.putFloat(gl.version());
        
        // vec4 8: CoronaParams
        builder.putFloat(config.corona().width());
        builder.putFloat(0f);
        builder.putFloat(0f);
        builder.putFloat(0f);
        
        // vec4 9: OtherParams (NEW)
        OtherParams op = config.other();
        builder.putFloat(op.pulseFrequency());
        builder.putFloat(op.pulseAmplitude());
        builder.putFloat(op.noiseScale());
        builder.putFloat(op.noiseStrength());
        
        // vec4 10: ScreenEffects (NEW)
        ScreenEffects se = config.screen();
        builder.putFloat(se.blackout());
        builder.putFloat(se.vignetteAmount());
        builder.putFloat(se.vignetteRadius());
        builder.putFloat(se.tintAmount());
        
        // vec4 11: DistortionParams (NEW)
        DistortionParams dp = config.distortion();
        builder.putFloat(dp.strength());
        builder.putFloat(dp.radius());
        builder.putFloat(dp.frequency());
        builder.putFloat(dp.speed());
        
        // vec4 12: BlendParams (NEW)
        BlendParams bp = config.blend();
        builder.putFloat(bp.opacity());
        builder.putFloat(bp.blendMode());
        builder.putFloat(bp.fadeIn());
        builder.putFloat(bp.fadeOut());
        
        // vec4 13: ReservedParams (NEW)
        ReservedParams rp = config.reserved();
        builder.putFloat(rp.slot1());
        builder.putFloat(rp.slot2());
        builder.putFloat(rp.slot3());
        builder.putFloat(rp.slot4());
        
        // vec4 14: Camera position + time
        builder.putFloat(camera.posX());
        builder.putFloat(camera.posY());
        builder.putFloat(camera.posZ());
        builder.putFloat(camera.time());
        
        // vec4 15: Forward + aspect
        builder.putFloat(camera.forwardX());
        builder.putFloat(camera.forwardY());
        builder.putFloat(camera.forwardZ());
        builder.putFloat(camera.aspect());
        
        // vec4 16: Up + fov
        builder.putFloat(camera.upX());
        builder.putFloat(camera.upY());
        builder.putFloat(camera.upZ());
        builder.putFloat(camera.fov());
        
        // vec4 17: Render params
        builder.putFloat(render.nearPlane());
        builder.putFloat(render.farPlane());
        builder.putFloat(0f);
        builder.putFloat(render.isFlying());
        
        // mat4 18-21: InvViewProj
        writeMatrix(builder, invViewProj);
        
        // mat4 22-25: ViewProj
        writeMatrix(builder, viewProj);
        
        // vec4 26: Debug
        builder.putFloat(debug.camMode());
        builder.putFloat(debug.debugMode());
        builder.putFloat(0f);
        builder.putFloat(0f);
    }
    
    private static void writeMatrix(Std140Builder builder, Matrix4f m) {
        for (int c = 0; c < 4; c++) {
            Vector4f col = new Vector4f();
            m.getColumn(c, col);
            builder.putFloat(col.x);
            builder.putFloat(col.y);
            builder.putFloat(col.z);
            builder.putFloat(col.w);
        }
    }
}
```

**Dependency:** Phase 1

---

## Phase 3: Update FieldVisualConfig

**File:** `src/client/java/net/cyberpunk042/client/visual/effect/FieldVisualConfig.java`

```java
public record FieldVisualConfig(
    ColorParams colors,
    AnimParams anim,
    CoreEdgeParams coreEdge,
    SpiralParams spiral,
    GlowLineParams glowLine,
    CoronaParams corona,
    OtherParams other,
    ScreenEffects screen,
    DistortionParams distortion,
    BlendParams blend,
    ReservedParams reserved
) {
    public static FieldVisualConfig defaultEnergyOrb() {
        return new FieldVisualConfig(
            ColorParams.DEFAULT,
            AnimParams.DEFAULT,
            CoreEdgeParams.DEFAULT,
            SpiralParams.DEFAULT,
            GlowLineParams.DEFAULT,
            CoronaParams.DEFAULT,
            OtherParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            ReservedParams.DEFAULT
        );
    }
    
    // Convenience accessors
    public EffectType effectType() { return anim.effectType(); }
    public float intensity() { return anim.intensity(); }
    public float animationSpeed() { return anim.speed(); }
    public float coreSize() { return coreEdge.coreSize(); }
    public float edgeSharpness() { return coreEdge.edgeSharpness(); }
    public float spiralDensity() { return spiral.density(); }
    public float spiralTwist() { return spiral.twist(); }
    public float glowLineCount() { return glowLine.count(); }
    public float glowLineIntensity() { return glowLine.intensity(); }
    public boolean showExternalRays() { return glowLine.showExternalRays(); }
    public boolean showCorona() { return glowLine.showCorona(); }
    public float coronaWidth() { return corona.width(); }
    public int version() { return glowLine.version(); }
    public int primaryColor() { return colors.primaryColor(); }
    public int secondaryColor() { return colors.secondaryColor(); }
    public int tertiaryColor() { return colors.tertiaryColor(); }
    
    // Color extractors (delegate)
    public float primaryRed() { return colors.primaryR(); }
    public float primaryGreen() { return colors.primaryG(); }
    public float primaryBlue() { return colors.primaryB(); }
    public float primaryAlpha() { return colors.primaryA(); }
    // ... etc
}
```

**Dependency:** Phase 1

---

## Phase 4: Update FieldVisualAdapter

**File:** `src/client/java/net/cyberpunk042/client/gui/state/adapter/FieldVisualAdapter.java`

**Fields:**
```java
private ColorParams colors = ColorParams.DEFAULT;
private AnimParams anim = AnimParams.DEFAULT;
private CoreEdgeParams coreEdge = CoreEdgeParams.DEFAULT;
private SpiralParams spiral = SpiralParams.DEFAULT;
private GlowLineParams glowLine = GlowLineParams.DEFAULT;
private CoronaParams corona = CoronaParams.DEFAULT;
private OtherParams other = OtherParams.DEFAULT;
private ScreenEffects screen = ScreenEffects.NONE;
private DistortionParams distortion = DistortionParams.NONE;
private BlendParams blend = BlendParams.DEFAULT;
private ReservedParams reserved = ReservedParams.DEFAULT;
```

**get() cases:**
```java
case "effectType" -> anim.effectType();
case "intensity" -> anim.intensity();
case "animationSpeed" -> anim.speed();
case "coreSize" -> coreEdge.coreSize();
case "edgeSharpness" -> coreEdge.edgeSharpness();
case "spiralDensity" -> spiral.density();
case "spiralTwist" -> spiral.twist();
case "glowLineCount" -> glowLine.count();
case "glowLineIntensity" -> glowLine.intensity();
case "showExternalRays" -> glowLine.showExternalRays();
case "showCorona" -> glowLine.showCorona();
case "coronaWidth" -> corona.width();
case "version" -> glowLine.version();
// NEW
case "pulseFrequency" -> other.pulseFrequency();
case "pulseAmplitude" -> other.pulseAmplitude();
case "noiseScale" -> other.noiseScale();
case "noiseStrength" -> other.noiseStrength();
case "blackout" -> screen.blackout();
case "vignetteAmount" -> screen.vignetteAmount();
// ... etc
```

**set() cases:**
```java
case "intensity" -> anim = anim.withIntensity(toFloat(value));
case "animationSpeed" -> anim = anim.withSpeed(toFloat(value));
case "coreSize" -> coreEdge = coreEdge.withCoreSize(toFloat(value));
case "edgeSharpness" -> coreEdge = coreEdge.withEdgeSharpness(toFloat(value));
case "spiralDensity" -> spiral = spiral.withDensity(toFloat(value));
case "spiralTwist" -> spiral = spiral.withTwist(toFloat(value));
case "glowLineCount" -> glowLine = glowLine.withCount(toFloat(value));
case "glowLineIntensity" -> glowLine = glowLine.withIntensity(toFloat(value));
case "showExternalRays" -> glowLine = glowLine.withShowExternalRays(toBool(value));
case "showCorona" -> glowLine = glowLine.withShowCorona(toBool(value));
case "coronaWidth" -> corona = corona.withWidth(toFloat(value));
case "version" -> glowLine = glowLine.withVersion(toInt(value));
// ... etc
```

**buildConfig():**
```java
public FieldVisualConfig buildConfig() {
    return new FieldVisualConfig(
        colors, anim, coreEdge, spiral, glowLine, corona,
        other, screen, distortion, blend, reserved
    );
}
```

**Dependency:** Phase 1, Phase 3

---

## Phase 5: Update PostEffectPassMixin

**File:** `src/client/java/net/cyberpunk042/mixin/client/PostEffectPassMixin.java`

Replace 150 lines of inline buffer writing with:
```java
FieldVisualConfig config = currentField.getConfig();
PositionParams position = new PositionParams(
    (float) relativePos.x, (float) relativePos.y, (float) relativePos.z,
    currentField.getRadius()
);
CameraParams camera = new CameraParams(0f, 0f, 0f, globalTime,
    forwardX, forwardY, forwardZ, aspect,
    0f, 1f, 0f, fov);
RenderParams render = new RenderParams(0.05f, 1000f, isFlying);
DebugParams debug = DebugParams.DEFAULT;

FieldVisualUBOWriter.write(builder, config, position, camera, render, invViewProj, viewProj, debug);
```

**Dependency:** Phase 2

---

## Phase 6: Update Shader Layout

**File:** `src/main/resources/assets/the-virus-block/shaders/post/field_visual.fsh`

Add after V2Params:
```glsl
// vec4 9: OtherParams
float PulseFrequency;
float PulseAmplitude;
float NoiseScale;
float NoiseStrength;

// vec4 10: ScreenEffects
float Blackout;
float VignetteAmount;
float VignetteRadius;
float TintAmount;

// vec4 11: DistortionParams
float DistortionStrength;
float DistortionRadius;
float DistortionFrequency;
float DistortionSpeed;

// vec4 12: BlendParams
float BlendOpacity;
float BlendMode;
float FadeIn;
float FadeOut;

// vec4 13: ReservedParams
float ReservedSlot1;
float ReservedSlot2;
float ReservedSlot3;
float ReservedSlot4;
```

Update camera slots to 14+ and buffer size in mixin.

**Dependency:** Phase 2

---

## Phase 7: Test

- [ ] Compile succeeds
- [ ] Energy Orb renders correctly
- [ ] GUI controls work (`spiralDensity`, `coreSize`, etc.)
- [ ] Presets apply correctly
- [ ] JSON load/save works

---

## Execution Order

```
Phase 1 (Types) ────→ Phase 2 (Writer) ────→ Phase 5 (Mixin)
       │                     │
       ↓                     ↓
Phase 3 (Config) ────→ Phase 4 (Adapter)
                             │
                             ↓
                      Phase 6 (Shader)
                             │
                             ↓
                      Phase 7 (Test)
```

---

## Files Summary

| File | Action | Phase |
|------|--------|-------|
| `FieldVisualTypes.java` | CREATE | 1 |
| `FieldVisualUBOWriter.java` | CREATE | 2 |
| `FieldVisualConfig.java` | MODIFY | 3 |
| `FieldVisualAdapter.java` | MODIFY | 4 |
| `PostEffectPassMixin.java` | MODIFY | 5 |
| `field_visual.fsh` | MODIFY | 6 |
