package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;

/**
 * Configuration for a field's visual post-process effect.
 * 
 * <p>Immutable record composed of typed parameter groups.
 * Use the builder methods or preset factories to create configurations.</p>
 * 
 * <h3>UBO Layout (37 vec4 = 592 bytes)</h3>
 * <p>This record maps directly to the FieldVisualConfig UBO in the shader.
 * New parameters added in 2026-01 expansion for Shadertoy-style effects.</p>
 * 
 * <h3>Parameter Ranges</h3>
 * <ul>
 *   <li>intensity: 0.0 (invisible) to 2.0 (very bright)</li>
 *   <li>animationSpeed: 0.5 (slow) to 3.0 (fast)</li>
 *   <li>coreSize: 0.05 to 0.5 (relative to field radius)</li>
 *   <li>edgeSharpness: 1.0 (soft) to 10.0 (sharp)</li>
 * </ul>
 */
public record FieldVisualConfig(
    // Effect parameters (slots 0-23)
    ColorParams colors,              // 5 vec4 (primary, secondary, tertiary, highlight, ray)
    AnimParams anim,                 // 2 vec4 (base + multi-speed)
    AnimTimingParams animTiming,     // 1 vec4 (NEW)
    CoreEdgeParams coreEdge,         // 1 vec4 (expanded)
    FalloffParams falloff,           // 1 vec4 (NEW)
    NoiseConfigParams noiseConfig,   // 1 vec4 (replaces SpiralParams)
    NoiseDetailParams noiseDetail,   // 1 vec4 (NEW)
    GlowLineParams glowLine,         // 1 vec4 (expanded)
    CoronaParams corona,             // 1 vec4 (expanded)
    GeometryParams geometry,         // 1 vec4 (NEW)
    GeometryParams2 geometry2,       // 1 vec4 (NEW)
    TransformParams transform,       // 1 vec4 (NEW)
    LightingParams lighting,         // 1 vec4 (NEW)
    TimingParams timing,             // 1 vec4 (NEW)
    
    // Screen effects (slots 20-23)
    ScreenEffects screen,            // 1 vec4
    DistortionParams distortion,     // 1 vec4
    BlendParams blend,               // 1 vec4
    ReservedParams reserved,         // 1 vec4 (now includes version/flags)
    
    // V2 Detail parameters (slots 24-28 - NEW)
    V2CoronaDetail v2Corona,         // 1 vec4 (coronaStart, brightness, coreScale, maskRadius)
    V2CoreDetail v2Core,             // 1 vec4 (coreSpread, coreGlow, maskSoft, edgeRadius)
    V2EdgeDetail v2Edge,             // 1 vec4 (edgeSpread, edgeGlow, sharpScale, linesUVScale)
    V2LinesDetail v2Lines,           // 1 vec4 (densityMult, contrast1, contrast2, maskRadius)
    V2AlphaDetail v2Alpha,           // 1 vec4 (maskSoft, rayRotSpeed, rayStartRadius, alphaScale)
    
    // Pulsar V5 Flames (slots 42-43 - NEW)
    FlamesParams flames,             // 2 vec4 (edge, power, mult, timeScale, insideFalloff, surfaceNoise)
    
    // Geodesic Animation (slot 44 - NEW)
    GeoAnimParams geoAnim,           // 1 vec4 (animMode, rotationSpeed, domeClip, reserved)
    
    // V8 Electric Aura (slots 45-49 - NEW)
    V8PlasmaParams v8Plasma,         // 1 vec4 (scale, speed, turbulence, intensity)
    V8RingParams v8Ring,             // 2 vec4 (frequency, speed, sharpness, centerValue, modPower, intensity)
    V8CoronaParams v8Corona,         // 1 vec4 (extent, fadeStart, fadePower, intensity)
    V8ElectricParams v8Electric      // 1 vec4 (flash, fillIntensity, fillDarken, reserved)
) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS - Preset Configurations
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a default Energy Orb configuration (cyan/blue style).
     */
    public static FieldVisualConfig defaultEnergyOrb() {
        return new FieldVisualConfig(
            ColorParams.ofThree(0xFFFFFFFF, 0xFF00FFFF, 0xFF1A0528),
            new AnimParams(0f, 1.0f, 1.2f, EffectType.ENERGY_ORB, 2f, 2f, 5f, 2f),
            AnimTimingParams.DEFAULT,
            new CoreEdgeParams(0.15f, 4.0f, 0f, 4.0f),
            FalloffParams.DEFAULT,
            new NoiseConfigParams(5.0f, 45.0f, 2.0f, 0f),
            NoiseDetailParams.DEFAULT,
            new GlowLineParams(16.0f, 0.8f, 2.0f, 1.0f),
            new CoronaParams(0.5f, 2.0f, 50.0f, 1.0f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(1f, 3f, 0f, 0f),  // V1, rays+corona on
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a fire-colored Energy Orb configuration.
     */
    public static FieldVisualConfig fireEnergyOrb() {
        return new FieldVisualConfig(
            ColorParams.ofThree(0xFFFFFF00, 0xFFFF6600, 0xFF330000),
            new AnimParams(0f, 1.2f, 1.4f, EffectType.ENERGY_ORB, 3f, 2f, 6f, 3f),
            AnimTimingParams.DEFAULT,
            new CoreEdgeParams(0.12f, 5.0f, 0f, 5.0f),
            FalloffParams.DEFAULT,
            new NoiseConfigParams(6.0f, 36.0f, 2.0f, 0f),
            NoiseDetailParams.DEFAULT,
            new GlowLineParams(12.0f, 0.9f, 3.0f, 1.5f),
            new CoronaParams(0.6f, 2.0f, 60.0f, 1.5f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(1f, 3f, 0f, 0f),
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a void/dark Energy Orb configuration.
     */
    public static FieldVisualConfig voidEnergyOrb() {
        return new FieldVisualConfig(
            ColorParams.ofThree(0xFF8800FF, 0xFF4400AA, 0xFF000011),
            new AnimParams(0f, 0.7f, 1.0f, EffectType.ENERGY_ORB, 1.5f, 1f, 3f, 1f),
            AnimTimingParams.DEFAULT,
            new CoreEdgeParams(0.20f, 3.0f, 0f, 3.0f),
            FalloffParams.DEFAULT,
            new NoiseConfigParams(4.0f, 54.0f, 2.0f, 0f),
            NoiseDetailParams.DEFAULT,
            new GlowLineParams(8.0f, 0.5f, 1.5f, 0.8f),
            new CoronaParams(0.4f, 1.5f, 40.0f, 0.8f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(1f, 2f, 0f, 0f),  // corona on, rays off
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a healing/holy Energy Orb configuration.
     */
    public static FieldVisualConfig holyEnergyOrb() {
        return new FieldVisualConfig(
            ColorParams.ofThree(0xFFFFFFFF, 0xFFFFDD66, 0xFF221100),
            new AnimParams(0f, 0.8f, 1.3f, EffectType.ENERGY_ORB, 2f, 1.5f, 4f, 2f),
            AnimTimingParams.DEFAULT,
            new CoreEdgeParams(0.18f, 6.0f, 0f, 6.0f),
            FalloffParams.DEFAULT,
            new NoiseConfigParams(4.0f, 27.0f, 2.0f, 0f),
            NoiseDetailParams.DEFAULT,
            new GlowLineParams(12.0f, 0.6f, 2.5f, 1.2f),
            new CoronaParams(0.7f, 2.5f, 55.0f, 1.2f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(1f, 3f, 0f, 0f),
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a minimal/disabled configuration (NONE type).
     */
    public static FieldVisualConfig none() {
        return new FieldVisualConfig(
            ColorParams.ofThree(0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000),
            new AnimParams(0f, 1.0f, 0f, EffectType.NONE, 0f, 0f, 0f, 0f),
            AnimTimingParams.DEFAULT,
            new CoreEdgeParams(0.1f, 1.0f, 0f, 1.0f),
            FalloffParams.DEFAULT,
            NoiseConfigParams.DEFAULT,
            NoiseDetailParams.DEFAULT,
            new GlowLineParams(8.0f, 0f, 1.0f, 1.0f),
            new CoronaParams(0.5f, 2.0f, 0f, 0f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            ReservedParams.DEFAULT,
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a Volumetric Star configuration (Panteleymonov style).
     */
    public static FieldVisualConfig volumetricStar() {
        return new FieldVisualConfig(
            new ColorParams(0xFFFFFF00, 0xFFFF0000, 0xFFFF00FF, 0xFFFFFFFF, 0xFFFF9933),
            new AnimParams(0f, 1.0f, 1.5f, EffectType.ENERGY_ORB, 2f, 2f, 5f, 2f),
            new AnimTimingParams(0.1f, 0.35f, 0.15f, 0.015f),
            new CoreEdgeParams(0.2f, 5.0f, 0f, 4.0f),
            new FalloffParams(0.5f, 2.0f, 24.0f, 1.1f),
            new NoiseConfigParams(15.0f, 45.0f, 2.0f, 0f),
            new NoiseDetailParams(0.03125f, 4.0f, 3f, 0.4f),
            new GlowLineParams(16.0f, 0.8f, 2.0f, 1.0f),
            new CoronaParams(0.5f, 2.0f, 50.0f, 1.0f),
            GeometryParams.DEFAULT,
            GeometryParams2.DEFAULT,
            TransformParams.DEFAULT,
            LightingParams.DEFAULT,
            TimingParams.DEFAULT,
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(3f, 3f, 0f, 0f),  // V3 = Volumetric Star
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    /**
     * Creates a Geodesic Sphere configuration.
     */
    public static FieldVisualConfig geodesic() {
        return new FieldVisualConfig(
            new ColorParams(0xFFE6E6FF, 0xFF1A1A26, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFF9933),
            new AnimParams(0f, 1.0f, 1.2f, EffectType.ENERGY_ORB, 2f, 2f, 5f, 2f),
            AnimTimingParams.DEFAULT,
            CoreEdgeParams.DEFAULT,
            FalloffParams.DEFAULT,
            NoiseConfigParams.DEFAULT,
            NoiseDetailParams.DEFAULT,
            GlowLineParams.DEFAULT,
            CoronaParams.DEFAULT,
            new GeometryParams(3.0f, 0.05f, 0.1f, 2.0f),
            new GeometryParams2(0.005f, 2.0f, 0.1f, 0f),
            new TransformParams(0.3f, 0.25f, 1.0f, 0f),
            new LightingParams(1.2f, 0.8f, 0.3f, 0.2f),
            new TimingParams(6.0f, 2.0f, 0f, 10.0f),
            ScreenEffects.NONE,
            DistortionParams.NONE,
            BlendParams.DEFAULT,
            new ReservedParams(4f, 0f, 0f, 0f),  // V4 = Geodesic
            V2CoronaDetail.DEFAULT,
            V2CoreDetail.DEFAULT,
            V2EdgeDetail.DEFAULT,
            V2LinesDetail.DEFAULT,
            V2AlphaDetail.DEFAULT,
            FlamesParams.DEFAULT,
            GeoAnimParams.DEFAULT,
            V8PlasmaParams.DEFAULT,
            V8RingParams.DEFAULT,
            V8CoronaParams.DEFAULT,
            V8ElectricParams.DEFAULT
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE ACCESSORS (for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public EffectType effectType() { return anim.effectType(); }
    public float intensity() { return anim.intensity(); }
    public float animationSpeed() { return anim.speed(); }
    
    public int primaryColor() { return colors.primaryColor(); }
    public int secondaryColor() { return colors.secondaryColor(); }
    public int tertiaryColor() { return colors.tertiaryColor(); }
    public int highlightColor() { return colors.highlightColor(); }
    public int rayColor() { return colors.rayColor(); }
    
    public float coreSize() { return coreEdge.coreSize(); }
    public float edgeSharpness() { return coreEdge.edgeSharpness(); }
    
    // Legacy spiral accessors map to noise config
    public float spiralDensity() { return noiseConfig.density(); }
    public float spiralTwist() { return noiseConfig.twist(); }
    
    public float glowLineCount() { return glowLine.count(); }
    public float glowLineIntensity() { return glowLine.intensity(); }
    public boolean showExternalRays() { return reserved.showExternalRays(); }
    public boolean showCorona() { return reserved.showCorona(); }
    public int version() { return (int) reserved.version(); }
    
    public float coronaWidth() { return corona.width(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR EXTRACTION HELPERS (delegate to ColorParams)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public float primaryRed() { return colors.primaryR(); }
    public float primaryGreen() { return colors.primaryG(); }
    public float primaryBlue() { return colors.primaryB(); }
    public float primaryAlpha() { return colors.primaryA(); }
    
    public float secondaryRed() { return colors.secondaryR(); }
    public float secondaryGreen() { return colors.secondaryG(); }
    public float secondaryBlue() { return colors.secondaryB(); }
    public float secondaryAlpha() { return colors.secondaryA(); }
    
    public float tertiaryRed() { return colors.tertiaryR(); }
    public float tertiaryGreen() { return colors.tertiaryG(); }
    public float tertiaryBlue() { return colors.tertiaryB(); }
    public float tertiaryAlpha() { return colors.tertiaryA(); }
    
    public float highlightRed() { return colors.highlightR(); }
    public float highlightGreen() { return colors.highlightG(); }
    public float highlightBlue() { return colors.highlightB(); }
    public float highlightAlpha() { return colors.highlightA(); }
    
    public float rayRed() { return colors.rayR(); }
    public float rayGreen() { return colors.rayG(); }
    public float rayBlue() { return colors.rayB(); }
    public float rayAlpha() { return colors.rayA(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN (Immutable copy-on-write via sub-records)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Individual field builders
    public FieldVisualConfig withIntensity(float v) {
        return new FieldVisualConfig(colors, anim.withIntensity(v), animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withAnimationSpeed(float v) {
        return new FieldVisualConfig(colors, anim.withSpeed(v), animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withPrimaryColor(int v) {
        return new FieldVisualConfig(colors.withPrimary(v), anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withSecondaryColor(int v) {
        return new FieldVisualConfig(colors.withSecondary(v), anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withTertiaryColor(int v) {
        return new FieldVisualConfig(colors.withTertiary(v), anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withHighlightColor(int v) {
        return new FieldVisualConfig(colors.withHighlight(v), anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withRayColor(int v) {
        return new FieldVisualConfig(colors.withRay(v), anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withEffectType(EffectType v) {
        return new FieldVisualConfig(colors, anim.withEffectType(v), animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withCoreSize(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge.withCoreSize(v), falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withEdgeSharpness(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge.withEdgeSharpness(v), falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withSpiralDensity(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig.withDensity(v), noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withSpiralTwist(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig.withTwist(v), noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withGlowLineCount(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine.withCount(v), corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withGlowLineIntensity(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine.withIntensity(v), corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withShowExternalRays(boolean v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved.withShowExternalRays(v), v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withShowCorona(boolean v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved.withShowCorona(v), v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withVersion(int v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved.withVersion(v), v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withCoronaWidth(float v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona.withWidth(v), geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    // Record group builders
    public FieldVisualConfig withColors(ColorParams v) {
        return new FieldVisualConfig(v, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withAnim(AnimParams v) {
        return new FieldVisualConfig(colors, v, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withAnimTiming(AnimTimingParams v) {
        return new FieldVisualConfig(colors, anim, v, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withCoreEdge(CoreEdgeParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, v, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withFalloff(FalloffParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, v, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withNoiseConfig(NoiseConfigParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, v, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withNoiseDetail(NoiseDetailParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, v, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withGlowLine(GlowLineParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, v, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withCorona(CoronaParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, v, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withGeometry(GeometryParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, v, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withGeometry2(GeometryParams2 v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, v, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withTransform(TransformParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, v, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withLighting(LightingParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, v, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withTiming(TimingParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, v, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withScreen(ScreenEffects v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, v, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withDistortion(DistortionParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, v, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withBlend(BlendParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, v, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withReserved(ReservedParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, v, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    // V2 Detail group builders
    public FieldVisualConfig withV2Corona(V2CoronaDetail v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV2Core(V2CoreDetail v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV2Edge(V2EdgeDetail v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV2Lines(V2LinesDetail v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV2Alpha(V2AlphaDetail v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    // Flames group builder (Pulsar V5)
    public FieldVisualConfig withFlames(FlamesParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, v, geoAnim, v8Plasma, v8Ring, v8Corona, v8Electric);
    }
    
    // V8 Electric Aura group builders
    public FieldVisualConfig withV8Plasma(V8PlasmaParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v, v8Ring, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV8Ring(V8RingParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v, v8Corona, v8Electric);
    }
    
    public FieldVisualConfig withV8Corona(V8CoronaParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v, v8Electric);
    }
    
    public FieldVisualConfig withV8Electric(V8ElectricParams v) {
        return new FieldVisualConfig(colors, anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail, glowLine, corona, geometry, geometry2, transform, lighting, timing, screen, distortion, blend, reserved, v2Corona, v2Core, v2Edge, v2Lines, v2Alpha, flames, geoAnim, v8Plasma, v8Ring, v8Corona, v);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY COMPATIBILITY - spiral() accessor
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @deprecated Use noiseConfig() instead.
     */
    @Deprecated
    public SpiralParams spiral() {
        return new SpiralParams(noiseConfig.density(), noiseConfig.twist());
    }
    
    /**
     * @deprecated Use withNoiseConfig() instead.
     */
    @Deprecated  
    public FieldVisualConfig withSpiral(SpiralParams v) {
        return withNoiseConfig(v.toNoiseConfig());
    }
    
    /**
     * @deprecated Use reserved() instead.
     */
    @Deprecated
    public OtherParams other() {
        return new OtherParams(0f, 0f, noiseConfig.resLow(), noiseConfig.amplitude());
    }
    
    /**
     * @deprecated Legacy compatibility placeholder.
     */
    @Deprecated
    public FieldVisualConfig withOther(OtherParams v) {
        return this; // No-op for compatibility
    }
    
    /**
     * Legacy OtherParams for backward compatibility.
     */
    @Deprecated
    public record OtherParams(float pulseFrequency, float pulseAmplitude, float noiseScale, float noiseStrength) {
        public static final OtherParams DEFAULT = new OtherParams(1.0f, 0.1f, 1.0f, 0f);
    }
}
