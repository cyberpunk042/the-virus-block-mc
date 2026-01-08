# V2 Full Parameters Expansion Plan

## New Parameters Needed (20 total = 5 vec4 slots)

### Slot V2A: Corona/Core Details
1. coronaStart (0.15) - where glow begins
2. coronaBrightness (0.15) - corona intensity
3. coreRadiusScale (0.1) - core size multiplier
4. coreMaskRadius (0.35) - core cutoff

### Slot V2B: Core Glow Details
5. coreSpread (1.0) - core glow spread
6. coreGlow (1.0) - core glow intensity
7. coreMaskSoft (0.05) - core edge softness
8. edgeRadius (0.3) - ring position

### Slot V2C: Edge Details
9. edgeSpread (1.0) - ring spread
10. edgeGlow (1.0) - ring glow intensity
11. sharpScale (4.0) - sharpness divisor
12. linesUVScale (3.0) - pattern UV scale

### Slot V2D: Lines Details
13. linesDensityMult (1.6) - layer 2 density
14. linesContrast1 (2.5) - layer 1 power
15. linesContrast2 (3.0) - layer 2 power
16. linesMaskRadius (0.3) - pattern cutoff

### Slot V2E: Lines/Alpha Details
17. linesMaskSoft (0.02) - pattern edge
18. rayRotSpeed (0.3) - ray spin speed
19. rayStartRadius (0.32) - ray origin
20. alphaScale (0.5) - output alpha

## Files to Update

1. **FieldVisualTypes.java**
   - Add V2DetailParams record with all 20 fields (or 5 sub-records)

2. **FieldVisualConfig.java**
   - Add v2Detail field

3. **FieldVisualAdapter.java**
   - Add get/set paths for all 20 parameters

4. **FieldVisualUBOWriter.java**
   - Expand to 42 vec4 slots (add 5)
   - Write V2 detail slots

5. **field_visual.json**
   - Add 20 new float uniforms

6. **field_visual.fsh**
   - Expand UBO struct with new slots
   - Pass new params to V2 function

7. **energy_orb_v2.glsl**
   - Add parameters to function signature
   - Replace hardcoded values with parameters

8. **EffectSchemaRegistry.java**
   - Add all 20 V2 controls to schema

## Approach
- Add 5 new vec4 slots after slot 23 (shift camera to 29-32, matrices to 33-40, debug to 41)
- Or use existing reserved slots if available
