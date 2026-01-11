// ═══════════════════════════════════════════════════════════════════════════
// PULSAR V7 - Panteleymonov Sun Effect
// ═══════════════════════════════════════════════════════════════════════════
// Source: "SunShader" by Panteleymonov Aleksandr (2015-2016)
// Shadertoy: https://www.shadertoy.com/view/XdXGzH
// Unity: SunShader 1.0 for Unity3D 4-5
//
// This implementation follows the UNITY VERSION for full parametrization.
// ═══════════════════════════════════════════════════════════════════════════
//
// UNITY PROPERTIES → OUR UBO MAPPING:
// ────────────────────────────────────────────────────────────────────────────
// | Unity Property  | Default      | Range      | Our UBO Field           |
// |-----------------|--------------|------------|-------------------------|
// | _Radius         | 0.5          | -          | CenterAndRadius.w       |
// | _Light          | (1,1,1,1)    | Color      | HighlightColor          |
// | _Color          | (1,1,0,1)    | Color      | PrimaryColor            |
// | _Base           | (1,0,0,1)    | Color      | SecondaryColor          |
// | _Dark           | (1,0,1,1)    | Color      | TertiaryColor           |
// | _Ray            | (1,0.6,0.1)  | Color      | RayColor                |
// | _RayLight       | (1,0.95,1)   | Color      | Derived from Highlight  |
// | _RayString      | 1.0          | 0.02-10    | RaySharpness            |
// | _Detail         | 3            | 0-5        | NoiseOctaves (int)      |
// | _Rays           | 2.0          | 1-10       | RayPower                |
// | _RayRing        | 1.0          | 1-10       | CoronaPower             |
// | _RayGlow        | 2.0          | 1-10       | CoronaMultiplier*0.02   |
// | _Glow           | 4.0          | 1-100      | CoreFalloff             |
// | _Zoom           | 1.0          | -          | NoiseBaseScale          |
// | _SpeedHi        | 2.0          | 0-10       | SpeedHigh               |
// | _SpeedLow       | 2.0          | 0-10       | SpeedLow                |
// | _SpeedRay       | 5.0          | 0-10       | SpeedRay                |
// | _SpeedRing      | 2.0          | 0-20       | SpeedRing               |
// | _Seed           | 0            | -10 to 10  | NoiseSeed               |
// ────────────────────────────────────────────────────────────────────────────
//
// SECTIONS TO IMPLEMENT:
// 1. [X] Header & Parameters (this file)
// 2. [ ] noise4q - 4D animated noise function
// 3. [ ] noiseSpere - Body texture (multi-scale)
// 4. [ ] ring - Subtractive glow ring
// 5. [ ] Ray corona - ringRayNoise equivalent with full params
// 6. [ ] Main render function - Color composition
// 7. [ ] Dispatch integration in field_visual.fsh
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef PULSAR_V7_GLSL
#define PULSAR_V7_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2: noise4q - 4D Animated Noise (Now from shared library)
// ═══════════════════════════════════════════════════════════════════════════

#include "../core/noise_4d.glsl"
#include "../core/hdr_utils.glsl"

// Backwards compatibility aliases - V7 uses v7_ prefix internally
#define v7_noise4q noise4q
#define v7_noiseSpere noiseSpherical

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4: Corona Effects (Now from shared library)
// ═══════════════════════════════════════════════════════════════════════════

#include "../rendering/corona_effects.glsl"

// (Dark ring removed - v7_ring alias no longer needed)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5: Ray Corona (Now from shared library)
// ═══════════════════════════════════════════════════════════════════════════

// Backwards compatibility alias - same signature as animatedRayCorona
#define v7_rayCorona animatedRayCorona

// ═══════════════════════════════════════════════════════════════════════════
// RESULT STRUCT (uses shared EffectResult)
// ═══════════════════════════════════════════════════════════════════════════

#include "../core/effect_result.glsl"

// Backwards compatibility alias
#define PulsarV7Result EffectResult

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION SIGNATURE
// ═══════════════════════════════════════════════════════════════════════════
// Full parametrization matching Unity SunShader properties

PulsarV7Result renderPulsarV7(
    // ─── Ray & Position (from our camera system) ───
    vec3 rayOrigin,           // Camera position
    vec3 rayDir,              // Normalized ray direction
    vec3 forward,             // Camera forward (for Z-depth comparison like V1)
    float maxDist,            // Scene depth for occlusion (Z-depth)
    vec3 sphereCenter,        // World position of sphere
    float sphereRadius,       // Base radius (before zoom: _Radius)
    
    // ─── Time ───
    float time,               // Animated time (pre-multiplied by AnimSpeed)
    
    // ─── Colors (6 total) ───
    vec3 lightColor,          // _Light: (1,1,1) highlight/white
    vec3 bodyColor,           // _Color: (1,1,0) yellow body
    vec3 baseColor,           // _Base: (1,0,0) red accent
    vec3 darkColor,           // _Dark: (1,0,1) magenta accent
    vec3 rayColor,            // _Ray: (1,0.6,0.1) orange ray base
    vec3 rayLightColor,       // _RayLight: (1,0.95,1) ray highlight
    
    // ─── Detail & Zoom ───
    int detail,               // _Detail: 0-5 LOD levels
    float zoom,               // _Zoom: detail zoom (default 1.0)
    
    // ─── Core Scale (NEW) ───
    float coreScale,          // Multiplier for core size (default 1.0)
    
    // ─── Intensity (overall brightness control) ───
    float intensity,          // Overall brightness multiplier (0-2, default 1.0)
    
    // ─── Ray/Glow Parameters ───
    float rayString,          // _RayString: ray thickness (0.02-10, default 1.0)
    float rayReach,           // NEW: Ray extent multiplier (1 = default, higher = rays extend further)
    float rayFade,            // NEW: Ray edge fade (1=linear, <1=smoother edge, >1=sharper core)
    float rays,               // _Rays: ray intensity exponent (1-10, default 2.0)
    float rayRing,            // _RayRing: ring noise power (1-10, default 1.0)
    float rayGlow,            // _RayGlow: ray glow power (1-10, default 2.0)
    float glow,               // _Glow: overall glow falloff (1-100, default 4.0)
    
    // ─── Dark Ring Controls (NEW) ───
    float ringRadius,         // Dark ring distance from core (default 1.03)
    float ringFalloff,        // Dark ring sharpness (default 11.0)
    float ringIntensity,      // Dark ring strength (default 2.0)
    
    // ─── Animation Speeds ───
    float speedHi,            // _SpeedHi: high frequency (0-10, default 2.0)
    float speedLow,           // _SpeedLow: low frequency (0-10, default 2.0)
    float speedRay,           // _SpeedRay: ray animation (0-10, default 5.0)
    float speedRing,          // _SpeedRing: ring rotation (0-20, default 2.0)
    
    // ─── Fade Threshold (NEW - for Replace blend mode) ───
    float fadeThreshold,      // Fade out low-intensity areas (default 0.0 = original behavior)
    
    // ─── Noise Seed ───
    float seed,               // _Seed: pattern variation (-10 to 10, default 0)
    
    // ─── Eruption Contrast (NEW) ───
    float eruptionContrast    // Controls ray discreteness (2.0=default, higher=discrete eruptions)
) {
    PulsarV7Result result;
    result.didHit = false;
    result.color = vec3(0.0);
    result.alpha = 0.0;
    result.distance = maxDist;
    
    // ═══════════════════════════════════════════════════════════════════════
    // SETUP (matching Unity frag() setup)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Unity: float invz = 1/_Zoom; _Radius *= invz;
    float invz = 1.0 / zoom;
    float radius = sphereRadius * invz * coreScale;  // Apply coreScale
    
    // Unity: fragTime = _Time.x * 10.0
    // We receive pre-scaled time, so just use it
    float fragTime = time;
    
    // Unity: posGlob = position relative to camera
    vec3 pos = sphereCenter - rayOrigin;
    
    // Unity uses the ray direction directly
    vec3 ray = rayDir;
    
    // Build a simple rotation matrix for surface sampling
    // Unity uses: m = transpose((float3x3)UNITY_MATRIX_V)
    // We approximate with an identity for now - the noise still animates via time
    // TODO: Could derive from camera orientation if needed
    mat3 viewRotation = mat3(1.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // RAY-SPHERE INTERSECTION (matching Unity frag())
    // ═══════════════════════════════════════════════════════════════════════
    
    // Unity: RayProj = dot(ray, posGlob)
    float rayProj = dot(ray, pos);
    
    // Unity: sqDist = dot(posGlob, posGlob)
    float sqDist = dot(pos, pos);
    
    // Unity: sphere = sqDist - RayProj*RayProj (perpendicular distance squared)
    float sphere = sqDist - rayProj * rayProj;
    
    // Unity: sqRadius = _Radius * _Radius
    float sqRadius = radius * radius;
    
    // Unity: if (RayProj <= 0.0) sphere = sqRadius; (behind camera)
    if (rayProj <= 0.0) sphere = sqRadius;
    
    // Unity: pr = ray * abs(RayProj) - posGlob
    vec3 pr = ray * abs(rayProj) - pos;
    
    // ═══════════════════════════════════════════════════════════════════════
    // COMPUTE SURFACE POINT (matching Unity frag())
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 surface = vec3(0.0);
    
    if (sqDist <= sqRadius) {
        // Inside sphere - don't render effect over player
        // Return empty result (didHit = false, alpha = 0)
        return result;
    } else if (sphere < sqRadius) {
        // Hits sphere surface
        float l1 = sqrt(sqRadius - sphere);
        // Unity: surfase = mul(m, pr - ray*l1)
        surface = viewRotation * (pr - ray * l1);
        result.didHit = true;
        result.distance = rayProj - l1;
    } else {
        // Miss - surface stays at origin
        surface = vec3(0.0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEPTH CHECK - Is sphere in front of scene geometry?
    // ═══════════════════════════════════════════════════════════════════════
    
    bool sphereInFront = true;  // Assume in front by default
    if (result.didHit && result.distance > maxDist) {
        // Sphere is BEHIND scene geometry - don't render body
        sphereInFront = false;
        result.didHit = false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OCCLUSION (V1-style: convert to Z-depth for proper depth buffer comparison)
    // ═══════════════════════════════════════════════════════════════════════
    // V1 formula: hitZDepth = hitDist * dot(rayDir, forward)
    // This projects the distance along camera's forward axis for depth comparison
    
    float distCamToSun = sqrt(sqDist);
    
    // Use result.distance for surface hits, distCamToSun for rays/glow
    float sunDist = result.didHit ? result.distance : distCamToSun;
    
    // Convert to Z-depth (project along camera forward) - THIS IS THE V1 FIX
    float sunZDepth = sunDist * dot(rayDir, forward);
    
    // Is sun behind scene geometry? (compare Z-depths, not raw distances)
    bool sunBehindScene = (sunZDepth > maxDist);
    
    // Calculate visibility with progressive bleed
    float effectVisibility = 1.0;
    if (sunBehindScene) {
        float distSceneToSun = sunZDepth - maxDist;

        float minDist = 5.0;
        float bleedRange = 400.0;

        // t=0 at minDist, t=1 at minDist+bleedRange
        float t = clamp((distSceneToSun - minDist) / bleedRange, 0.0, 1.0);

        // 0.5 -> 0.0 across the range (use smoothstep for nicer transition)
        float fade = 1.0 - smoothstep(0.0, 1.0, t);
        effectVisibility = mix(0.0, 0.5, fade);  // equivalent to 0.5 * fade
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BODY NOISE (matching Unity frag())
    // Now uses effectVisibility for smooth occlusion
    // ═══════════════════════════════════════════════════════════════════════
    
    vec4 col = vec4(0.0);
    
    if (effectVisibility > 0.01 && detail >= 1) {
        // Unity seed vectors:
        // s1: vec3(45.78, 113.04, 28.957) * _Seed
        // s2: vec3(83.23, 34.34, 67.453) * _Seed
        vec3 seedVec1 = vec3(45.78, 113.04, 28.957) * seed;
        vec3 seedVec2 = vec3(83.23, 34.34, 67.453) * seed;
        
        // Layer 1: Low frequency (0.5 * _Zoom)
        float s1 = v7_noiseSpere(surface, sphere, sqRadius, 0.5 * zoom, seedVec1, fragTime, speedHi, speedLow, detail);
        s1 = pow(s1 * 2.4, 2.0);
        
        // Layer 2: High frequency (4.0 * _Zoom)
        float s2 = v7_noiseSpere(surface, sphere, sqRadius, 4.0 * zoom, seedVec2, fragTime, speedHi, speedLow, detail);
        s2 = s2 * 2.2;
        
        // Unity color composition:
        // col.xyz = lerp(_Color, _Light, pow(s1, 60.0)) * s1
        col.rgb = mix(bodyColor, lightColor, pow(s1, 60.0)) * s1;
        
        // col.xyz += lerp(lerp(_Base, _Dark, s2*s2), _Light, pow(s2, 10.0)) * s2
        col.rgb += mix(mix(baseColor, darkColor, s2 * s2), lightColor, pow(s2, 10.0)) * s2;
        
        // Apply occlusion bleed to body
        col.rgb *= effectVisibility;
        
        // Apply intensity (overall brightness control)
        col.rgb *= intensity;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SUBTRACTIVE RING - DISABLED (not visually useful)
    // ═══════════════════════════════════════════════════════════════════════
    
    // ═══════════════════════════════════════════════════════════════════════
    // RAY CORONA (matching Unity frag())
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 seedVec = vec3(83.23, 34.34, 67.453) * seed;
    
    float s3 = v7_rayCorona(
        pr, viewRotation,
        radius, zoom,
        sphere, sqRadius,
        seedVec,
        fragTime,
        speedRing, speedRay,
        rayString, rayReach, rays, rayRing, rayGlow, glow, rayFade,
        detail,
        eruptionContrast
    );
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALPHA AND RAY COLOR (matching Unity frag())
    // ═══════════════════════════════════════════════════════════════════════
    
    // Unity: if (sphere < sqRadius) col.w = 1.0 - s3*dr
    // Apply body alpha with effectVisibility for smooth occlusion
    float dr = (sphere < sqRadius) ? sphere / sqRadius : 1.0;
    if (effectVisibility > 0.01 && sphere < sqRadius) {
        col.a = (1.0 - s3 * dr) * effectVisibility;
    }
    
    // Unity: if (sqDist > sqRadius) col.xyz += lerp(_Ray, _RayLight, s3*s3*s3) * s3
    // Rays use same effectVisibility as body/ring for smooth occlusion
    if (sqDist > sqRadius) {
        vec3 rayContrib = mix(rayColor, rayLightColor, s3 * s3 * s3) * s3;
        col.rgb += rayContrib * effectVisibility * intensity;  // Apply intensity to rays
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FINAL CLAMP
    // ═══════════════════════════════════════════════════════════════════════
    
    // HDR-aware clamp: LDR clamps to [0,1], HDR allows > 1.0
    col = hdrClamp(col);
    
    // ═══════════════════════════════════════════════════════════════════════
    // FADE THRESHOLD (for Replace blend mode)
    // ═══════════════════════════════════════════════════════════════════════
    // At fadeThreshold=0: no change (original behavior, may show dark glow in Replace mode)
    // At fadeThreshold>0: smoothly fade out low-intensity areas to remove dark glow
    
    if (fadeThreshold > 0.0) {
        float colorIntensity = max(col.r, max(col.g, col.b));
        if (colorIntensity < fadeThreshold) {
            // Smooth fade from 0 to threshold
            float fade = smoothstep(0.0, fadeThreshold, colorIntensity);
            col.rgb *= fade;
            col.a *= fade;
        }
    }
    
    result.color = col.rgb;
    result.alpha = max(col.a, length(col.rgb) > 0.01 ? 1.0 : 0.0);
    
    return result;
}

#endif // PULSAR_V7_GLSL
