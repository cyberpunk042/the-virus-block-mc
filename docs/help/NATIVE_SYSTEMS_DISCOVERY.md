# Native Systems Discovery

> **Purpose:** Document native Minecraft systems we can leverage  
> **Discovered:** December 8, 2024  
> **Status:** Reference for implementation

---

## 🎯 Summary: What We Found

| System | Class | Relevance to Fields |
|--------|-------|---------------------|
| **Math** | `MathHelper` | Lerp, easing, sin for animations |
| **Colors** | `ColorHelper` | Color manipulation, mixing, brightness |
| **Animation** | `BinaryAnimator.EasingFunction` | Easing curves |
| **Interpolation** | `InterpolationHandler` | Smooth entity movement |
| **Sound** | `SoundEvent` | Trigger audio feedback |
| **Particles** | `ParticleEffect` | Visual effects on events |
| **Rendering** | `RenderLayer`, `RenderLayers` | Layer management |
| **GUI** | 185 widget classes | See GUI_NATIVE_WIDGETS_EXTENDED.md |

---

## 📐 MathHelper (net.minecraft.util.math.MathHelper)

**Perfect for:** Field animations, smooth transitions, value clamping

```java
// Linear interpolation
float result = MathHelper.lerp(delta, start, end);
Vec3d pos = MathHelper.lerp(delta, posA, posB);  // Vec3d variant!

// 2D/3D interpolation
double val2d = MathHelper.lerp2(dx, dy, v00, v10, v01, v11);
double val3d = MathHelper.lerp3(dx, dy, dz, ...);

// Smooth curve (Catmull-Rom spline)
float smooth = MathHelper.catmullRom(delta, p0, p1, p2, p3);

// Clamping
float clamped = MathHelper.clamp(value, min, max);
double mapped = MathHelper.clampedMap(value, oldMin, oldMax, newMin, newMax);

// Trigonometry (fast lookup table)
float sine = MathHelper.sin(radians);
float cosine = MathHelper.cos(radians);

// Color conversion
int argb = MathHelper.hsvToArgb(hue, saturation, value, alpha);

// Float comparison
boolean equal = MathHelper.approximatelyEquals(a, b);
```

### Use Cases for Fields:
- **Pulse animation**: `MathHelper.sin(time * speed) * scale`
- **Fade in/out**: `MathHelper.lerp(fadeProgress, 0, 1)`
- **Smooth follow**: `MathHelper.lerp(0.1f, currentPos, targetPos)`
- **Color transitions**: `MathHelper.lerp(t, colorA, colorB)` on int colors

---

## 🎨 ColorHelper (net.minecraft.util.math.ColorHelper)

**Perfect for:** Our color/appearance module, theme colors, dynamic effects

```java
// Create colors
int color = ColorHelper.getArgb(alpha, red, green, blue);
int color = ColorHelper.fromFloats(a, r, g, b);  // 0.0-1.0 floats
int white = ColorHelper.getWhite(alpha);

// Modify colors
int withAlpha = ColorHelper.withAlpha(0.5f, baseColor);
int brighter = ColorHelper.withBrightness(color, 1.5f);
int scaled = ColorHelper.scaleRgb(color, 0.8f);  // Darken
int scaled2 = ColorHelper.scaleRgb(color, 1.0f, 0.8f, 0.6f);  // Per-channel

// Extract components
int a = ColorHelper.getAlpha(color);
int r = ColorHelper.getRed(color);
int g = ColorHelper.getGreen(color);
int b = ColorHelper.getBlue(color);

// Mix/interpolate
int mixed = ColorHelper.mix(colorA, colorB);  // 50/50 mix
int lerped = ColorHelper.lerp(0.3f, colorA, colorB);  // 30% blend
```

### Use Cases for Fields:
- **Theme colors**: Store as ARGB, use `getAlpha/Red/Green/Blue` to extract
- **Damage flash**: `ColorHelper.lerp(flashProgress, baseColor, RED)`
- **Health binding**: `ColorHelper.lerp(health/maxHealth, LOW_COLOR, HIGH_COLOR)`
- **Glow effect**: `ColorHelper.withBrightness(color, 1.0f + glowAmount)`

---

## 🎬 Animation & Easing

### BinaryAnimator.EasingFunction
```java
// Interface for custom easing
public interface EasingFunction {
    float apply(float t);  // t: 0.0 to 1.0
}
```

### Built-in Easing (we should wrap these):
```java
// Linear (default)
float linear(float t) { return t; }

// Ease in/out (quadratic)
float easeIn(float t) { return t * t; }
float easeOut(float t) { return 1 - (1-t) * (1-t); }
float easeInOut(float t) { return t < 0.5 ? 2*t*t : 1 - 2*(1-t)*(1-t); }

// Smooth step (cubic)
float smoothStep(float t) { return t * t * (3 - 2 * t); }
```

### Use Cases for Fields:
- **Lifecycle fade**: Ease-out for spawn, ease-in for despawn
- **Pulse animation**: Sine wave for breathing effect
- **Scale animation**: Ease-in-out for smooth growth

---

## 🎯 InterpolationHandler (net.minecraft.world.entity.InterpolationHandler)

**Perfect for:** Smooth field follow mode

```java
InterpolationHandler handler = new InterpolationHandler(entity);
handler.setInterpolationLength(3);  // 3 ticks to interpolate
handler.interpolateTo(targetPos, yaw, pitch);

// Each tick:
handler.interpolate();
Vec3d smoothPos = handler.position();
float smoothYaw = handler.yRot();
float smoothPitch = handler.xRot();

// Check state
boolean active = handler.hasActiveInterpolation();
handler.cancel();  // Stop interpolation
```

### Use Cases for Fields:
- **SMOOTH follow mode**: Use InterpolationHandler for entity-like smoothing
- **Prediction**: Extend target position based on velocity
- **Lag compensation**: Interpolate between network updates

---

## 🔊 SoundEvent (net.minecraft.sound.SoundEvent)

**Perfect for:** Trigger audio feedback

```java
// Create custom sound event
SoundEvent customSound = SoundEvent.createVariableRangeEvent(
    Identifier.of("the-virus-block", "field_activate")
);

// Fixed range (audible within N blocks)
SoundEvent localSound = SoundEvent.createFixedRangeEvent(
    Identifier.of("the-virus-block", "field_pulse"), 
    8.0f  // 8 block range
);

// Play sound
world.playSound(null, pos, customSound, SoundCategory.PLAYERS, volume, pitch);
```

### Use Cases for Fields:
- **Trigger effects**: Play sound on damage, heal, death
- **Lifecycle**: Sound on spawn/despawn
- **Pulse**: Subtle audio feedback on pulse

---

## ✨ ParticleEffect (net.minecraft.particle.ParticleEffect)

**Perfect for:** Visual effects on field events

```java
// Spawn particles
world.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy, vz);
world.addParticle(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);

// Server-side (sends to clients)
ServerWorld serverWorld = (ServerWorld) world;
serverWorld.spawnParticles(ParticleTypes.GLOW, x, y, z, count, dx, dy, dz, speed);
```

### Use Cases for Fields:
- **Damage trigger**: Red particles burst
- **Heal trigger**: Green particles
- **Spawn/despawn**: Particle burst at edges
- **Ambient**: Subtle particles along field surface

---

## 🖼️ RenderLayer / RenderLayers

**We already use these, but good to document:**

```java
// Get layer for items
RenderLayer layer = RenderLayers.getItemLayer(itemStack);

// Text layers
RenderLayer normal = textLayerSet.normal();
RenderLayer seeThrough = textLayerSet.seeThrough();
RenderLayer polygonOffset = textLayerSet.polygonOffset();
```

---

## 🛠️ Recommended Utility Wrappers

### 1. FieldMath.java (wrap MathHelper)
```java
public final class FieldMath {
    public static float lerp(float t, float a, float b) { 
        return MathHelper.lerp(t, a, b); 
    }
    public static float smoothStep(float t) { 
        return t * t * (3 - 2 * t); 
    }
    public static float easeOut(float t) { 
        return 1 - (1-t) * (1-t); 
    }
    public static float pulse(float time, float speed) {
        return (MathHelper.sin(time * speed * MathHelper.TAU) + 1) * 0.5f;
    }
}
```

### 2. FieldColor.java (wrap ColorHelper)
```java
public final class FieldColor {
    public static int withAlpha(int color, float alpha) {
        return ColorHelper.withAlpha(alpha, color);
    }
    public static int lerp(float t, int a, int b) {
        return ColorHelper.lerp(t, a, b);
    }
    public static int brighten(int color, float factor) {
        return ColorHelper.withBrightness(color, factor);
    }
}
```

### 3. FieldSound.java (optional)
```java
public final class FieldSound {
    public static void playTrigger(World world, Vec3d pos, TriggerEvent event) {
        SoundEvent sound = switch(event) {
            case DAMAGE -> FIELD_DAMAGE_SOUND;
            case HEAL -> FIELD_HEAL_SOUND;
            case ACTIVATE -> FIELD_ACTIVATE_SOUND;
        };
        world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, 0.5f, 1.0f);
    }
}
```

---

## 📊 Priority Implementation

| Priority | System | Action |
|----------|--------|--------|
| **HIGH** | ColorHelper | Integrate into color module |
| **HIGH** | MathHelper | Use in animations, follow modes |
| **MEDIUM** | GridWidget | Use in GUI layouts |
| **MEDIUM** | EditBoxWidget | Use for text input |
| **LOW** | SoundEvent | Add optional trigger sounds |
| **LOW** | ParticleEffect | Add optional visual effects |

---

## Created Utilities

### FieldMath.java (`visual/util/FieldMath.java`)
- **Interpolation:** `lerp()`, `catmullRom()`
- **Easing:** `easeIn()`, `easeOut()`, `easeInOut()`, `smoothStep()`, `smootherStep()`
- **Cubic:** `easeInCubic()`, `easeOutCubic()`
- **Animation:** `pulse()`, `breathe()`, `bounce()`, `elastic()`
- **Clamping:** `clamp()`, `clampedMap()`
- **Trig:** `sin()`, `cos()`, `toRadians()`, `toDegrees()`
- **Constants:** `TAU`, `PI`, `HALF_PI`

### FieldColor.java (`visual/util/FieldColor.java`)
- **Presets:** `WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `CYAN`, `MAGENTA`, `YELLOW`, `ORANGE`
- **Creation:** `argb()`, `rgb()`, `fromFloats()`, `fromHsv()`, `gray()`, `white()`
- **Extraction:** `getAlpha()`, `getRed()`, `getGreen()`, `getBlue()`, `getAlphaFloat()`
- **Modification:** `withAlpha()`, `withBrightness()`, `scaleRgb()`
- **Blending:** `lerp()`, `mix()`
- **Utility:** `toHex()`, `fromHex()`, `rainbow()`

---

*v1.1 - Utilities created (December 8, 2024)*

