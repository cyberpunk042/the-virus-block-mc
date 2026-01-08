# V8 Electric Aura - Parameter Refactor Plan

## Goal
Replace confusing V7 ray parameters with dedicated V8 controls for:
- **Plasma** (electric noise texture)
- **Rings** (pulsating logarithmic rings)
- **Corona** (overall envelope)

## New UBO Fields (Slots 45-48)

### Slot 45: Plasma Controls
```glsl
float PlasmaScale;      // Base pattern size (1-50, default 10)
float PlasmaSpeed;      // Animation speed (0-10, default 1)
float PlasmaTurbulence; // Ridged intensity (0-2, default 1)
float PlasmaIntensity;  // Brightness (0-10, default 1)
```

### Slot 46: Ring Controls 1
```glsl
float RingFrequency;    // Number of rings (1-20, default 4)
float RingSpeed;        // Expansion rate (0-20, default 10)
float RingSharpness;    // Edge sharpness (0.1-10, default 3)
float RingCenterValue;  // Brightness target (0-0.5, default 0.1)
```

### Slot 47: Ring Controls 2
```glsl
float RingModPower;     // Modulation strength (0-2, default 0.9)
float RingIntensity;    // Ring brightness (0-10, default 1)
float RingReserved1;    // Reserved
float RingReserved2;    // Reserved
```

### Slot 48: Corona Controls
```glsl
float CoronaExtent;     // Max extent multiplier (1-10, default 2)
float CoronaFadeStart;  // Where fade begins (0-1, default 0.5)
float CoronaFadePower;  // Fade curve (0.1-10, default 1)
float CoronaIntensity;  // Overall brightness (0-10, default 1)
```

## Files to Update

### 1. field_visual_base.glsl
Add the 4 new vec4 slots to UBO after slot 44.

### 2. electric_plasma.glsl
Update `animatedElectricCorona` to use new parameters.

### 3. pulsar_v8.glsl
Update function signature to pass new parameters.

### 4. field_visual_v8.fsh
Update call site to pass new UBO values.

### 5. EffectSchemaRegistry.java
Update `registerElectricAura()` with new controls.

### 6. Check Java UBO binding
Ensure Java side binds the new fields correctly.

## Execution Order
1. Add to shader UBO (field_visual_base.glsl)
2. Update electric_plasma.glsl function
3. Update pulsar_v8.glsl signature
4. Update field_visual_v8.fsh call
5. Update EffectSchemaRegistry.java
6. Validate shaders
7. Test in-game
