// ═══════════════════════════════════════════════════════════════════════════
// FIELD VISUAL BASE - Shared Header for All Effect Shaders
// 
// This file is included by all effect-specific shaders (v2, v3, v6, v7, geodesic).
// It provides:
//   - Samplers and I/O declarations
//   - The complete FieldVisualConfig UBO
//   - Effect type constants  
//   - Core library includes
//   - Legacy compatibility aliases
//
// Usage: #include "include/core/field_visual_base.glsl"
// ═══════════════════════════════════════════════════════════════════════════

// Samplers (post_effect uses "In"/"Depth" which becomes InSampler/DepthSampler)
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

// Vertex shader output
in vec2 texCoord;

// Required by Minecraft post effect system (like shockwave)
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Fragment output
out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// BASE UBOs - Shared Frame and Camera data
// These are bound once per frame by BaseUBOBinder
// ═══════════════════════════════════════════════════════════════════════════
#include "../ubo/frame_ubo.glsl"
#include "../ubo/camera_ubo.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// UNIFORM BUFFER - Field Visual Configuration
// Layout: 50 vec4s = 200 floats = 800 bytes (including 2 mat4s = 8 vec4s)
// 
// CRITICAL: This layout MUST match across ALL effect shaders.
// The Java-side FieldVisualUniformBinder binds by name - any mismatch causes
// silent failures (NaN values, wrong params in wrong uniforms).
// ═══════════════════════════════════════════════════════════════════════════

layout(std140) uniform FieldVisualConfig {
    // ═══════════════════════════════════════════════════════════════════════
    // EFFECT PARAMS (Slots 0-23)
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 0: Field center position + radius (camera-relative)
    float CenterX;
    float CenterY;
    float CenterZ;
    float Radius;
    
    // vec4 1: Primary color (RGBA) - Core/main
    float PrimaryR;
    float PrimaryG;
    float PrimaryB;
    float PrimaryA;
    
    // vec4 2: Secondary color (RGBA) - Edge/accent
    float SecondaryR;
    float SecondaryG;
    float SecondaryB;
    float SecondaryA;
    
    // vec4 3: Tertiary color (RGBA) - Outer glow
    float TertiaryR;
    float TertiaryG;
    float TertiaryB;
    float TertiaryA;
    
    // vec4 4: Highlight color (RGBA) - Bright specular
    float HighlightR;
    float HighlightG;
    float HighlightB;
    float HighlightA;
    
    // vec4 5: Ray color (RGBA) - Ray/corona tint
    float RayColorR;
    float RayColorG;
    float RayColorB;
    float RayColorA;
    
    // vec4 6: Animation base params
    float Phase;           // Animation phase offset
    float AnimSpeed;       // Animation speed multiplier
    float Intensity;       // Effect intensity (0-2)
    float EffectType;      // 0=none, 1=energy_orb, 2=geodesic
    
    // vec4 7: Animation multi-speed channels
    float SpeedHigh;       // Fast detail animation
    float SpeedLow;        // Slow base movement
    float SpeedRay;        // Ray/corona animation
    float SpeedRing;       // Ring/rotation speed
    
    // vec4 8: Animation timing modifiers
    float TimeScale;       // Global time multiplier
    float RadialSpeed1;    // First radial noise speed
    float RadialSpeed2;    // Second radial noise speed
    float AxialSpeed;      // Z/axial animation speed
    
    // vec4 9: Core/Edge params (expanded)
    float CoreSize;        // Size of bright center (0-1)
    float EdgeSharpness;   // Edge falloff sharpness
    float ShapeType;       // 0=sphere, 1=torus, 2=cylinder, 3=prism
    float CoreFalloff;     // Core glow falloff power
    
    // vec4 10: Falloff params
    float FadePower;           // Distance fade exponent
    float FadeScale;           // Fade distance multiplier
    float InsideFalloffPower;  // Inside-core falloff
    float CoronaEdge;          // Corona edge threshold
    
    // vec4 11: Noise config (replaces Spiral)
    float NoiseResLow;     // Low frequency noise resolution
    float NoiseResHigh;    // High frequency noise resolution
    float NoiseAmplitude;  // Noise amplitude multiplier
    float NoiseSeed;       // Noise variation seed
    
    // vec4 12: Noise detail
    float NoiseBaseScale;       // Base detail scale
    float NoiseScaleMultiplier; // Per-octave scale mult
    float NoiseOctaves;         // FBM octave count
    float NoiseBaseLevel;       // Base noise floor
    
    // vec4 13: Glow line params (expanded)
    float GlowLineCount;   // Number of radial glow lines
    float GlowLineIntensity;
    float RayPower;        // Ray intensity exponent
    float RaySharpness;    // Ray edge sharpness
    
    // vec4 14: Corona params (expanded)
    float CoronaWidth;     // Width of corona glow
    float CoronaPower;     // Falloff exponent
    float CoronaMultiplier; // Brightness multiplier
    float RingPower;       // Ring glow power
    
    // vec4 15: Geometry params
    float GeoSubdivisions;  // Tile density
    float GeoRoundTop;      // Top edge rounding
    float GeoRoundCorner;   // Corner rounding
    float GeoThickness;     // Shell thickness
    
    // vec4 16: Geometry params 2
    float GeoGap;              // Tile gap
    float GeoHeight;           // Extrusion height
    float GeoWaveResolution;   // Wave pattern resolution
    float GeoWaveAmplitude;    // Wave amplitude
    
    // vec4 17: Transform params
    float TransRotationX;   // Model rotation X (radians)
    float TransRotationY;   // Model rotation Y (radians)
    float TransScale;       // Model scale multiplier
    float TransReserved;    // Reserved
    
    // vec4 18: Lighting params
    float LightDiffuse;     // Diffuse light strength
    float LightAmbient;     // Ambient light strength
    float LightBackLight;   // Back rim light
    float LightFresnel;     // Fresnel rim strength
    
    // vec4 19: Timing params
    float TimingSceneDuration;    // Animation cycle length
    float TimingCrossfade;        // Transition blend time
    float TimingLoopMode;         // Loop mode (0-3)
    float TimingAnimFrequency;    // Pattern frequency
    
    // vec4 20: Screen effects
    float Blackout;        // 0-1 screen darkening
    float VignetteAmount;  // 0-1 edge darkening
    float VignetteRadius;  // 0.2-1.0 vignette size
    float TintAmount;      // 0-1 color tint strength
    
    // vec4 21: Distortion params
    float DistortionStrength;
    float DistortionRadius;
    float DistortionFrequency;
    float DistortionSpeed;
    
    // vec4 22: Blend params
    float BlendReserved;   // Reserved
    float BlendMode;       // 0=normal, 1=additive, 2=multiply, 3=screen
    float FadeIn;
    float FadeOut;
    
    // vec4 23: Reserved params (includes version/flags)
    float Version;           // Effect version (1.0, 2.0, 3.0, etc.)
    float RayCoronaFlags;    // bit 0 = showExternalRays, bit 1 = showCorona
    float ColorBlendMode;    // 0=multiply, 1=additive, 2=replace, 3=mix
    float EruptionContrast;  // V7: Ray discreteness (2.0=default, higher=discrete eruptions)
    
    // ═══════════════════════════════════════════════════════════════════════
    // V2/V3 DETAIL PARAMS (Slots 24-28)
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 24: V2 Corona Detail
    float V2CoronaStart;       // Where glow begins (0.15)
    float V2CoronaBrightness;  // Corona intensity multiplier (0.15)
    float V2CoreRadiusScale;   // Core size multiplier (0.1)
    float V2CoreMaskRadius;    // Core cutoff radius (0.35)
    
    // vec4 25: V2 Core Detail
    float V2CoreSpread;        // Core glow spread multiplier (1.0)
    float V2CoreGlow;          // Core glow intensity (1.0)
    float V2CoreMaskSoft;      // Core edge softness (0.05)
    float V2EdgeRadius;        // Edge ring position (0.3)
    
    // vec4 26: V2 Edge Detail
    float V2EdgeSpread;        // Ring spread multiplier (1.0)
    float V2EdgeGlow;          // Ring glow intensity (1.0)
    float V2SharpScale;        // Sharpness divisor (4.0)
    float V2LinesUVScale;      // Pattern UV scale (3.0)
    
    // vec4 27: V2 Lines Detail
    float V2LinesDensityMult;  // Layer 2 density multiplier (1.6)
    float V2LinesContrast1;    // Layer 1 power exponent (2.5)
    float V2LinesContrast2;    // Layer 2 power exponent (3.0)
    float V2LinesMaskRadius;   // Pattern cutoff radius (0.3)
    
    // vec4 28: V2 Alpha Detail
    float V2LinesMaskSoft;     // Pattern edge softness (0.02)
    float V2RayRotSpeed;       // Ray rotation speed (0.3)
    float V2RayStartRadius;    // Ray origin radius (0.32)
    float V2AlphaScale;        // Output alpha multiplier (0.5)
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEPRECATED - Camera data now in CameraDataUBO (Slots 29-40)
    // These slots are RESERVED to maintain layout compatibility.
    // DO NOT use for new params - they will be removed in a future version.
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 29: [DEPRECATED] Was Camera position + time - Now use CameraDataUBO
    float Reserved29_0;
    float Reserved29_1;
    float Reserved29_2;
    float Reserved29_3;
    
    // vec4 30: [DEPRECATED] Was Camera forward + aspect - Now use CameraDataUBO
    float Reserved30_0;
    float Reserved30_1;
    float Reserved30_2;
    float Reserved30_3;
    
    // vec4 31: [DEPRECATED] Was Camera up + fov - Now use CameraDataUBO
    float Reserved31_0;
    float Reserved31_1;
    float Reserved31_2;
    float Reserved31_3;
    
    // vec4 32: [DEPRECATED] Was Render params - Now use CameraDataUBO
    float Reserved32_0;
    float Reserved32_1;
    float Reserved32_2;
    float Reserved32_3;
    
    // mat4 (vec4 33-36): [DEPRECATED] Was InvViewProj - Now use InvViewProjUBO
    mat4 Reserved_InvViewProj;
    
    // mat4 (vec4 37-40): [DEPRECATED] Was ViewProj - Now use ViewProjUBO
    mat4 Reserved_ViewProj;
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG (Slot 41)
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 41: Debug params
    float CamMode;
    float DebugMode;
    float DebugReserved1;
    float DebugReserved2;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PULSAR V5+ PARAMS (Slots 42-43)
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 42: Flames params 1
    float FlamesEdge;          // Distance threshold (1.1)
    float FlamesPower;         // Falloff exponent (2.0)
    float FlamesMult;          // Brightness multiplier (50.0)
    float FlamesTimeScale;     // Time modulation (1.2)
    
    // vec4 43: Flames params 2
    float FlamesInsideFalloff; // Inside sphere falloff (24.0)
    float SurfaceNoiseScale;   // Surface procedural texture scale (5.0)
    float FlamesReserved1;     // Reserved
    float FlamesReserved2;     // Reserved
    
    // ═══════════════════════════════════════════════════════════════════════
    // GEODESIC ANIMATION PARAMS (Slot 44)
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 44: Geodesic Animation params
    float GeoAnimMode;      // Animation mode: 0.0=static, 0.1=wave, 0.2=Y-wave, 0.3=gap
    float GeoRotationSpeed; // Sphere rotation speed (rad/sec)
    float GeoDomeClip;      // Dome clip: 0=sphere, 0.5=hemisphere, 1=flat
    float GeoAnimReserved;  // Reserved
    
    // ═══════════════════════════════════════════════════════════════════════
    // V8 ELECTRIC AURA PARAMS (Slots 45-48)
    // Dedicated controls for plasma, rings, and corona - NOT shared with V7
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 45: Plasma Controls (electric noise texture)
    float V8PlasmaScale;      // Base pattern size (1-50, default 10)
    float V8PlasmaSpeed;      // Animation speed (0-10, default 1)
    float V8PlasmaTurbulence; // Ridged intensity (0-2, default 1)
    float V8PlasmaIntensity;  // Brightness (0-10, default 1)
    
    // vec4 46: Ring Controls 1 (pulsating logarithmic rings)
    float V8RingFrequency;    // Number of rings (1-20, default 4)
    float V8RingSpeed;        // Expansion rate (0-20, default 10)
    float V8RingSharpness;    // Edge sharpness (0.1-10, default 3)
    float V8RingCenterValue;  // Brightness target (0-0.5, default 0.1)
    
    // vec4 47: Ring Controls 2
    float V8RingModPower;     // Modulation strength (0-2, default 0.9)
    float V8RingIntensity;    // Ring brightness (0-10, default 1)
    float V8CoreType;         // Core pattern: 0=Default, 1=Electric, 2+future
    float V8RingReserved2;    // Reserved
    
    // vec4 48: Corona Controls (overall envelope)
    float V8CoronaExtent;     // Max extent multiplier (1-10, default 2)
    float V8CoronaFadeStart;  // Where fade begins (0-1, default 0.5)
    float V8CoronaFadePower;  // Fade curve (0.1-10, default 1)
    float V8CoronaIntensity;  // Overall brightness (0-10, default 1)
    
    // vec4 49: Electric Core Controls (dedicated to Electric core type)
    float V8ElectricFlash;         // Flash effect: 0=off, 1=on scene flash
    float V8ElectricFillIntensity; // Fill visibility: 0=minimal fill, 1=rich fill
    float V8ElectricFillDarken;    // Fill color: 0=white, 0.5=match lines, 1=black (default 0.60)
    float V8ElectricLineWidth;     // Line thickness: 0=thick, 1=thin (default 0.5)
    
    // ═══════════════════════════════════════════════════════════════════════
    // GOD RAY PARAMS (Slots 50-54)
    // Screen-space volumetric light scattering
    // ═══════════════════════════════════════════════════════════════════════
    
    // vec4 50: God Ray controls
    float GodRayEnabled;      // 0.0 = procedural rays, 1.0 = screen-space god rays
    float GodRayDecay;        // Range control: 0.94-0.99 (higher = longer rays)
    float GodRayExposure;     // Strength control: 0.005-0.1 (higher = brighter)
    float GodRaySamples;      // Sample count (default 96)
    
    // vec4 51: God Ray mask controls
    float GodRayThreshold;    // Brightness threshold: 0.0-1.0 (lower = more rays)
    float GodRaySkyEnabled;   // Sky rays toggle: 0.0 = off, 1.0 = on
    float GodRaySoftness;     // Transition softness (default 0.3)
    float GodRayMaskReserved; // Reserved
    
    // vec4 52: God Ray style controls
    float GodRayEnergyMode;      // 0=radiation (outward), 1=absorption (inward), 2=pulse
    float GodRayColorMode;       // 0=solid, 1=gradient, 2=temperature
    float GodRayDistributionMode;// 0=uniform, 1=weighted, 2=noise
    float GodRayArrangementMode; // 0=point, 1=ring, 2=sector
    
    // vec4 53: God Ray secondary color (for gradient mode)
    float GodRayColor2R;      // Red component (0-1, can exceed for HDR)
    float GodRayColor2G;      // Green component
    float GodRayColor2B;      // Blue component
    float GodRayGradientPower;// Blend curve power (1=linear, 2=quadratic)
    
    // vec4 54: God Ray noise controls (for distribution mode 2)
    float GodRayNoiseScale;   // Angular noise frequency (default 8.0)
    float GodRayNoiseSpeed;   // Animation speed (default 0.5)
    float GodRayNoiseIntensity;// Modulation strength 0-1 (default 0.5)
    float GodRayAngularBias;  // Directional bias (-1=vert, 0=none, 1=horiz)
    
    // vec4 55: God Ray curvature (vortex/spiral/pinwheel effects)
    float GodRayCurvatureMode;     // 0=radial, 1=vortex, 2=spiral, 3=tangential, 4=pinwheel
    float GodRayCurvatureStrength; // Curvature amount (0-2 typical)
    float GodRayCurvatureSpeed;    // Rotation speed multiplier
    float GodRayCurvatureReserved; // Future use
    
    // vec4 56: God Ray flicker (animation modes)
    float GodRayFlickerMode;       // 0=none, 1=scintillation, 2=strobe, 3=fadePulse, 4=heartbeat, 5=lightning
    float GodRayFlickerIntensity;  // Flicker strength (0-1)
    float GodRayFlickerFrequency;  // Animation frequency
    float GodRayWaveDistribution;  // 0=continuous, 1=sequential, 2=random, 3=goldenRatio
    
    // vec4 57: God Ray travel (chase/scroll effects)
    float GodRayTravelMode;        // 0=none, 1=scroll, 2=chase, 3=pulseWave, 4=comet
    float GodRayTravelSpeed;       // Travel speed
    float GodRayTravelCount;       // Number of particles (chase mode)
    float GodRayTravelWidth;       // Particle width (0-1)
};

// ═══════════════════════════════════════════════════════════════════════════
// EFFECT TYPE CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

#define EFFECT_NONE 0
#define EFFECT_ENERGY_ORB 1
#define EFFECT_GEODESIC 2
// Future: SHIELD=3, AURA=4, PORTAL=5

// ═══════════════════════════════════════════════════════════════════════════
// CORE LIBRARY INCLUDES
// Using the modular shader library (2026-01-03 refactor)
// ═══════════════════════════════════════════════════════════════════════════

// Core utilities
#include "constants.glsl"
#include "math_utils.glsl"
#include "noise_utils.glsl"

// Camera system
#include "../camera/types.glsl"
#include "../camera/basis.glsl"
#include "../camera/rays.glsl"
#include "../camera/projection.glsl"
#include "../camera/depth.glsl"

// SDF and rendering
#include "../sdf/primitives.glsl"
#include "../rendering/screen_effects.glsl"

// Effect types (provides FieldData struct - shared by all effects)
#include "../effects/energy_orb_types.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// LEGACY COMPATIBILITY ALIASES
// Map old variable names to new ones for gradual migration
// ═══════════════════════════════════════════════════════════════════════════

// SpiralParams are now NoiseConfig - create aliases for V1/V2 code
#define SpiralDensity NoiseResLow
#define SpiralTwist   NoiseResHigh

// ═══════════════════════════════════════════════════════════════════════════
// COMMON UTILITY FUNCTIONS
// These are used by all effect main() functions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Builds the common FieldData struct from UBO floats.
 * This ensures consistent data mapping across all effects.
 */
FieldData buildFieldDataFromUBO() {
    FieldData field;
    field.CenterAndRadius = vec4(CenterX, CenterY, CenterZ, Radius);
    field.PrimaryColor = vec4(PrimaryR, PrimaryG, PrimaryB, PrimaryA);
    field.SecondaryColor = vec4(SecondaryR, SecondaryG, SecondaryB, SecondaryA);
    field.TertiaryColor = vec4(TertiaryR, TertiaryG, TertiaryB, TertiaryA);
    field.HighlightColor = vec4(HighlightR, HighlightG, HighlightB, HighlightA);
    field.AnimParams = vec4(Phase, AnimSpeed, Intensity, EffectType);
    field.CoreEdgeParams = vec4(CoreSize, EdgeSharpness, ShapeType, CoreFalloff);
    field.SpiralParams = vec4(SpiralDensity, SpiralTwist, 0.0, 0.0);
    field.GlowLineParams = vec4(GlowLineCount, GlowLineIntensity, RayCoronaFlags, Version);
    field.V2Params = vec4(CoronaWidth, CoronaPower, CoronaMultiplier, RingPower);
    
    // V2 Detail parameters
    field.V2CoronaStart = V2CoronaStart;
    field.V2CoronaBrightness = V2CoronaBrightness;
    field.V2CoreRadiusScale = V2CoreRadiusScale;
    field.V2CoreMaskRadius = V2CoreMaskRadius;
    field.V2CoreSpread = V2CoreSpread;
    field.V2CoreGlow = V2CoreGlow;
    field.V2CoreMaskSoft = V2CoreMaskSoft;
    field.V2EdgeRadius = V2EdgeRadius;
    field.V2EdgeSpread = V2EdgeSpread;
    field.V2EdgeGlow = V2EdgeGlow;
    field.V2SharpScale = V2SharpScale;
    field.V2LinesUVScale = V2LinesUVScale;
    field.V2LinesDensityMult = V2LinesDensityMult;
    field.V2LinesContrast1 = V2LinesContrast1;
    field.V2LinesContrast2 = V2LinesContrast2;
    field.V2LinesMaskRadius = V2LinesMaskRadius;
    field.V2LinesMaskSoft = V2LinesMaskSoft;
    field.V2RayRotSpeed = V2RayRotSpeed;
    field.V2RayStartRadius = V2RayStartRadius;
    field.V2AlphaScale = V2AlphaScale;
    
    return field;
}

/**
 * Composites the field effect with the scene color.
 * Handles different blend modes consistently across all effects.
 * 
 * @param sceneColor Original scene color (RGB)
 * @param fieldEffect Effect color and alpha (RGBA)
 * @return Final composited color
 */
vec3 compositeFieldEffect(vec3 sceneColor, vec4 fieldEffect) {
    if (fieldEffect.a < 0.001) {
        return sceneColor;
    }
    
    vec3 effectColor = fieldEffect.rgb;
    float effectAlpha = fieldEffect.a;
    vec3 finalColor;
    
    // ColorBlendMode determines compositing method
    float blendModeVal = ColorBlendMode;
    
    if (blendModeVal > 4.5) {
        // SUBTRACT mode (5): Dark/shadow effects
        vec3 subtractAmount = effectColor * effectAlpha;
        finalColor = max(vec3(0.0), sceneColor - subtractAmount);
    } else if (blendModeVal > 1.5) {
        // REPLACE/MIX/DIRECT (2,3,4): MIX compositing
        finalColor = mix(sceneColor, effectColor, effectAlpha);
    } else {
        // MULTIPLY/ADDITIVE (0,1): Additive glow compositing
        vec3 effectToneMapped = 1.0 - exp(-effectColor * 1.5);
        finalColor = sceneColor + effectToneMapped * effectAlpha;
    }
    
    return finalColor;
}

/**
 * HDR version of compositeFieldEffect.
 * Does NOT apply tonemapping - values > 1.0 pass through.
 * Use with HDR framebuffers (RGBA16F) for proper HDR rendering.
 * 
 * @param sceneColor Original scene color (RGB)
 * @param fieldEffect Effect color and alpha (RGBA)
 * @return Final composited color (unbounded, can exceed 1.0)
 */
vec3 compositeFieldEffectHDR(vec3 sceneColor, vec4 fieldEffect) {
    if (fieldEffect.a < 0.001) {
        return sceneColor;
    }
    
    vec3 effectColor = fieldEffect.rgb;
    float effectAlpha = fieldEffect.a;
    vec3 finalColor;
    
    // ColorBlendMode determines compositing method
    float blendModeVal = ColorBlendMode;
    
    if (blendModeVal > 4.5) {
        // SUBTRACT mode (5): Dark/shadow effects
        vec3 subtractAmount = effectColor * effectAlpha;
        finalColor = max(vec3(0.0), sceneColor - subtractAmount);
    } else if (blendModeVal > 1.5) {
        // REPLACE/MIX/DIRECT (2,3,4): MIX compositing
        finalColor = mix(sceneColor, effectColor, effectAlpha);
    } else {
        // MULTIPLY/ADDITIVE (0,1): Additive glow compositing - NO TONEMAPPING
        // HDR: Raw additive blend, values > 1.0 flow through
        finalColor = sceneColor + effectColor * effectAlpha;
    }
    
    return finalColor;
}
