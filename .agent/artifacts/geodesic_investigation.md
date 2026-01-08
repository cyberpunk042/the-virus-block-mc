# Geodesic Effect Complete Pipeline Investigation

## Problem Statement
The user reports:
1. Ranges set in GUI schema are not appearing
2. Default config shows nothing visible on screen
3. Parameters don't seem to work as expected

## Investigation Scope
This document traces EVERY step from JSON config → Java records → UBO upload → Shader reading

---

# PART 1: CONFIGURATION FILES

## 1.1 Current geodesic_sphere.json
Need to verify:
- [ ] All parameter names match exactly what serializer expects
- [ ] Default values produce visible output
- [ ] effectType is set correctly

## 1.2 Current geodesic_dome.json  
Same checks as above

---

# PART 2: SERIALIZATION LAYER

## 2.1 FieldVisualSerializer.java
Functions to trace:
- loadGeometry() - loads geoSubdivisions, geoRoundTop, geoRoundCorner, geoThickness
- loadGeometry2() - loads geoGap, geoHeight, geoWaveResolution, geoWaveAmplitude
- loadTransform() - loads transScale, transRotationX, transRotationY
- loadLighting() - loads lightFresnel

## 2.2 Key Questions:
- [ ] Are all JSON keys correctly mapped to record fields?
- [ ] Are the "with*" methods named correctly in records?
- [ ] Is there case sensitivity issues?

---

# PART 3: ADAPTER LAYER

## 3.1 FieldVisualAdapter.java
Functions:
- getValue(String path) - returns current value for GUI display
- setValue(String path, Object value) - sets value from GUI

## 3.2 Key Questions:
- [ ] Are all geodesic paths registered in switch statements?
- [ ] Do the paths match between get/set?
- [ ] Are both geometry and geometry2 records being used?

---

# PART 4: JAVA RECORD TYPES

## 4.1 FieldVisualTypes.java - GeometryParams
Fields: subdivisions, roundTop, roundCorner, thickness
Default: (3.0f, 0.05f, 0.1f, 2.0f)

## 4.2 FieldVisualTypes.java - GeometryParams2  
Fields: gap, height, waveResolution, waveAmplitude
Default: (0.005f, 2.0f, 30f, 0.25f)

## 4.3 FieldVisualTypes.java - TransformParams
Fields: rotationX, rotationY, scale, reserved
Check: Is scale used for domeClip?

## 4.4 FieldVisualTypes.java - LightingParams
Fields: ambientStrength, diffuseStrength, backLightStrength, fresnelStrength
Check: fresnelStrength maps to spectrum offset?

---

# PART 5: UBO UPLOAD

## 5.1 FieldVisualUBO.java / AnimBaseVec4.java
Check:
- [ ] GeometryParams uploaded to vec4 slot 15
- [ ] GeometryParams2 uploaded to vec4 slot 16
- [ ] TransformParams uploaded to vec4 slot 17
- [ ] LightingParams uploaded to vec4 slot 18

## 5.2 Data Flow:
Java Record → Vec4Serializable.slot0-3() → ByteBuffer → Uniform upload

---

# PART 6: SHADER UBO DEFINITION

## 6.1 field_visual.fsh UBO struct
```glsl
// vec4 15: Geometry params
float GeoSubdivisions;  // slot0
float GeoRoundTop;      // slot1
float GeoRoundCorner;   // slot2
float GeoThickness;     // slot3

// vec4 16: Geometry params 2
float GeoGap;              // slot0
float GeoHeight;           // slot1
float GeoWaveResolution;   // slot2
float GeoWaveAmplitude;    // slot3

// vec4 17: Transform params
float TransRotationX;   // slot0
float TransRotationY;   // slot1
float TransScale;       // slot2
float TransReserved;    // slot3

// vec4 18: Lighting params
float LightDiffuse;     // slot0
float LightAmbient;     // slot1
float LightBackLight;   // slot2
float LightFresnel;     // slot3
```

---

# PART 7: SHADER PARAMETER USAGE

## 7.1 field_visual.fsh - GEODESIC case
Where parameters are read:
```glsl
float subdivisions = clamp(GeoSubdivisions, 1.0, 20.0);  // ← CLAMPED!
float hexHeight = GeoHeight;
float hexThickness = GeoThickness;
float hexGap = GeoGap;
float roundTop = GeoRoundTop;
float roundCorner = GeoRoundCorner;
float animMode = floor(clamp(TransRotationX, 0.0, 3.0));  // ← CLAMPED!
float rotationSpeed = TransRotationY;
float waveResolution = GeoWaveResolution;
float waveAmplitude = GeoWaveAmplitude;
float edgeColorOffset = LightFresnel;
float domeClip = TransScale;
```

## 7.2 geodesic_v1.glsl - geodesicModel()
How parameters are used in SDF:
```glsl
spec.roundTop = roundTop / subdivisions;     // DIVIDED!
spec.roundCorner = roundCorner / subdivisions;  // DIVIDED!
spec.height = hexHeight;
spec.thickness = hexThickness;
spec.gap = hexGap;
```

## 7.3 geodesic_v1.glsl - hexModel()
```glsl
float edgeADist = dot(p, edgeA) + spec.gap;  // gap ADDED directly
float outerDist = length(p) - spec.height;   // height SUBTRACTED
float innerDist = length(p) - spec.height + spec.thickness;  // thickness ADDED
```

---

# PART 8: IDENTIFIED ISSUES

## Issue 1: Shader clamped subdivisions to 20 ✅ FIXED
```glsl
// WAS: float subdivisions = clamp(GeoSubdivisions, 1.0, 20.0);
// NOW: float subdivisions = max(GeoSubdivisions, 1.0);
```

## Issue 2: TransformParams DEFAULT had WRONG VALUES ✅ FIXED
```java
// WAS: DEFAULT = new TransformParams(0.3f, 0.25f, 1.0f, 0f);
//      rotationX=0.3 (weird animMode), scale=1.0 (clips entire sphere!)
// NOW: DEFAULT = new TransformParams(1.0f, 0.2f, 0.0f, 0f);
//      rotationX=1.0 (animMode=wave), scale=0.0 (full sphere visible)
```
**THIS WAS THE MAIN BUG** - scale=1.0 meant domeClip=1.0 which clips almost everything!

## Issue 3: Rounding values divided by subdivisions
```glsl  
spec.roundTop = roundTop / subdivisions;
```
If subdivisions=10 and roundTop=1, effective roundTop = 0.1
Need values up to 100+ to see effect with high subdivisions ✅ Already set to 0-100

## Issue 4: EffectType ordinal - VERIFIED OK ✅
GEODESIC = ordinal 2 in Java enum
EFFECT_GEODESIC = 2 in shader - MATCHES!

---

# PART 9: ROOT CAUSE SUMMARY

## WHY NOTHING WAS VISIBLE:
1. **TransformParams.DEFAULT.scale = 1.0** → This maps to `domeClip` in shader
2. `domeClip = 1.0` means clip Y below `(1.0 * 2.0 - 1.0) * hexHeight = hexHeight`
3. This clips the ENTIRE sphere (only shows top point)
4. Result: **NOTHING VISIBLE**

## WHY SUBDIVISIONS WERE LIMITED:
1. Shader had `clamp(GeoSubdivisions, 1.0, 20.0)` 
2. Even if GUI allowed 100, shader capped at 20

---

# PART 10: ADDITIONAL CHECKS NEEDED

## 10.1 Check if UBO slots are correct
Need to verify the UBO upload order matches shader struct order:
- Slot 15: GeometryParams → GeoSubdivisions, GeoRoundTop, GeoRoundCorner, GeoThickness
- Slot 16: GeometryParams2 → GeoGap, GeoHeight, GeoWaveResolution, GeoWaveAmplitude
- Slot 17: TransformParams → TransRotationX, TransRotationY, TransScale, TransReserved
- Slot 18: LightingParams → LightDiffuse, LightAmbient, LightBackLight, LightFresnel

## 10.2 Check shader UBO struct order
Verify field_visual.fsh UBO struct has same order as Java records

---

# PART 11: FIXES APPLIED

1. ✅ Fixed TransformParams.DEFAULT: scale 1.0 → 0.0 (full sphere)
2. ✅ Fixed TransformParams.DEFAULT: rotationX 0.3 → 1.0 (wave animation)
3. ✅ Removed subdivisions clamp in shader
4. ✅ GUI ranges expanded to 100-1000

---

# PART 12: REMAINING WORK

1. [ ] Rebuild the project
2. [ ] Test that sphere is now visible with defaults
3. [ ] Verify all sliders work with full range
4. [ ] Check if any other clamps exist in geodesic_v1.glsl
