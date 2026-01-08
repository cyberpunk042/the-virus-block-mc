# V8 Electric Corona - Complete Refactor Plan

## PROBLEM STATEMENT

I made a critical error: I REPLACED V7 params with V8 params instead of ADDING V8 params.

**What was needed:**
- Keep all V7 params working (they control valid things)
- ADD new V8 params for controls that didn't exist before

**What I did wrong:**
- Made V7 params (rayString, rays, rayRing, rayGlow, glow, rayFade, speedRay, speedRing, rayReach) DEAD
- V8 params now exist but V7 params are passed through function signature and ignored

---

## CURRENT STATE ANALYSIS

### Files Involved:

1. **field_visual_base.glsl** - UBO with V8 params (slots 45-48) ✅ GOOD
2. **electric_plasma.glsl** - `animatedElectricCoronaV8()` function ❌ BROKEN (uses V8, ignores V7)
3. **pulsar_v8.glsl** - `renderPulsarV8()` function ❌ BROKEN (has V7 params but doesn't pass to corona)
4. **field_visual_v8.fsh** - Passes V7 params to renderPulsarV8 ✅ GOOD
5. **EffectSchemaRegistry.java** - Exposes some V8 controls, missing some V7 ❌ INCOMPLETE

### Current Parameter Flow:

```
.fsh passes V7 params (RaySharpness, SpeedRay, etc.)
    ↓
renderPulsarV8 receives them as (rayString, speedRay, etc.)
    ↓
animatedElectricCoronaV8 is called with V8 UBO params ONLY
    ↓
V7 params are DEAD - not passed to corona function
```

---

## V7 PARAMS - What they control in V7's animatedRayCorona

| Param | V7 Line | Usage | What it controls |
|-------|---------|-------|------------------|
| speedRing | 145,156 | `-time * speedRing + c` | Ring noise animation speed |
| speedRay | 160,165 | `-time * speedRay + c` | Ray noise animation speed |
| rayString | 139 | `(rayString * rayReach)` denominator | Ring width/spread |
| rayReach | 139 | `(rayString * rayReach)` denominator | Ring extent multiplier |
| rays | 169 | `pow(n, rays)` | Ray noise intensity exponent |
| rayRing | 169 | `pow(nd, rayRing)` | Ring noise distribution exponent |
| rayGlow | 172 | `pow(s, rayGlow) * n` | Ray-modulated glow power |
| glow | 172 | `pow(s, glow)` | Base glow falloff power |
| rayFade | 142 | `pow(s, rayFade)` | Edge fade curve |

---

## V8 PARAMS - What they should control (NEW capabilities)

| Param | Purpose | NEW capability |
|-------|---------|----------------|
| V8PlasmaScale | Pattern size | Control noise scale independently |
| V8PlasmaSpeed | Plasma animation | Additional speed control for plasma |
| V8PlasmaTurbulence | Ridged amount | 0=smooth, 1=electric ridged |
| V8PlasmaIntensity | Plasma brightness | Independent intensity control |
| V8RingFrequency | Number of rings | Logarithmic ring count |
| V8RingSpeed | Ring expansion | How fast rings expand outward |
| V8RingSharpness | Ring edge | Soft vs crisp ring edges |
| V8RingCenterValue | Brightness center | nimitz ring modulation target |
| V8RingModPower | Modulation curve | Ring modulation exponent |
| V8RingIntensity | Ring brightness | Independent ring intensity |
| V8CoronaExtent | Max reach | Overall corona envelope size |
| V8CoronaFadeStart | Fade start | Where envelope fade begins |
| V8CoronaFadePower | Fade curve | Envelope fade power |
| V8CoronaIntensity | Overall | Corona brightness multiplier |

---

## FIX PLAN

### Step 1: Update animatedElectricCoronaV8 signature

**Current (WRONG):** Takes only V8 params
**Fixed:** Takes V7 params AND V8 params

New signature (23 control params + 9 core = 32 total):
```glsl
float animatedElectricCoronaV8(
    // Core (9)
    vec3 pr, mat3 viewRotation, float radius, float zoom,
    float perpDistSq, float sqRadius, vec3 seedVec, float time, int detail,
    
    // V7 PARAMS (9) - KEEP ALL WORKING
    float speedRing,    // Ring noise animation
    float speedRay,     // Ray noise animation
    float rayString,    // Ring width
    float rayReach,     // Ring extent
    float rays,         // Ray intensity exponent
    float rayRing,      // Ring noise exponent
    float rayGlow,      // Ray-modulated glow power
    float glow,         // Base glow power
    float rayFade,      // Edge fade curve
    
    // V8 PLASMA PARAMS (4) - NEW
    float plasmaScale,
    float plasmaSpeed,
    float plasmaTurbulence,
    float plasmaIntensity,
    
    // V8 RING PARAMS (6) - NEW
    float ringFrequency,
    float ringSpeed,
    float ringSharpness,
    float ringCenterValue,
    float ringModPower,
    float ringIntensity,
    
    // V8 CORONA PARAMS (4) - NEW
    float coronaExtent,
    float coronaFadeStart,
    float coronaFadePower,
    float coronaIntensity
)
```

### Step 2: Wire V7 params in function body

In the PLASMA section:
- Use `speedRay` for base time offset (like V7)
- ADD `plasmaSpeed` as multiplier
- Use `rays` for intensity exponent (like V7)
- ADD `plasmaTurbulence` for ridged treatment
- ADD `plasmaScale` for pattern size
- ADD `plasmaIntensity` for brightness

In the RING section:
- Use `speedRing` for base ring time (like V7)  
- ADD `ringSpeed` for expansion
- Use `rayRing` for ring exponent (like V7)
- ADD `ringFrequency`, `ringSharpness`, `ringCenterValue`, `ringModPower`, `ringIntensity`

In the FALLOFF section:
- Use `rayString * rayReach` for width calc (like V7)
- Use `rayFade` for pow curve (like V7)
- ADD `coronaExtent`, `coronaFadeStart`, `coronaFadePower` for envelope

In the COMBINATION section:
- Use `glow` for base glow power (like V7)
- Use `rayGlow` for ray-modulated glow (like V7)
- ADD `coronaIntensity` as final multiplier

### Step 3: Update pulsar_v8.glsl call site

Change the `animatedElectricCoronaV8` call to pass BOTH:
- V7 params from function signature (speedRing, speedRay, rayString, etc.)
- V8 params from UBO (V8PlasmaScale, V8RingFrequency, etc.)

### Step 4: Update EffectSchemaRegistry.java

Add V7 controls back to the V8 schema UI:
- "Ray Controls" group: raySharpness (→rayString), fadeScale (→rayReach), rayPower (→rays), coronaPower (→rayRing), coronaMultiplier (→rayGlow), coreFalloff (→glow), insideFalloffPower (→rayFade)
- "Animation" group: speedRay, speedRing (already there via speedHigh/Low mapping)

Keep V8 controls:
- "Plasma" group (4 controls)
- "Rings" group (6 controls)
- "Corona" group (4 controls)

---

## VALIDATION CHECKLIST

After implementation:
- [ ] All 9 V7 params are passed through to animatedElectricCoronaV8
- [ ] All 9 V7 params are USED in the function body (not dead)
- [ ] All 14 V8 params are passed from UBO
- [ ] All 14 V8 params are USED in the function body
- [ ] Schema exposes all 23 controls
- [ ] Shader compiles
- [ ] Effect is visible in-game
- [ ] Each slider changes something visible

---

## FILES TO MODIFY

1. **electric_plasma.glsl** - Rewrite animatedElectricCoronaV8 with new signature and body
2. **pulsar_v8.glsl** - Update call site to pass both V7 and V8 params
3. **EffectSchemaRegistry.java** - Add missing V7 controls to V8 schema

UBO is already correct (V8 params exist in slots 45-48).
.fsh is already correct (passes V7 params).
