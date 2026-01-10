package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.ubo.Vec4Serializable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Type definitions for the Field Visual effect system.
 * 
 * <p>All records used by FieldVisualConfig and the UBO writer.
 * Organized by UBO slot order for clarity.</p>
 * 
 * <h3>UBO Layout (std140) - 37 vec4 slots = 592 bytes</h3>
 * <pre>
 * EFFECT PARAMS (Slots 0-23):
 *   0: PositionParams
 *   1-5: ColorParams (5 colors × 1 vec4 each)
 *   6-7: AnimParams (base + multi-speed)
 *   8: AnimTimingParams
 *   9: CoreEdgeParams
 *   10: FalloffParams
 *   11: NoiseConfigParams
 *   12: NoiseDetailParams
 *   13: GlowLineParams
 *   14: CoronaParams
 *   15: GeometryParams
 *   16: GeometryParams2
 *   17: TransformParams
 *   18: LightingParams
 *   19: TimingParams
 *   20: ScreenEffects
 *   21: DistortionParams
 *   22: BlendParams
 *   23: ReservedParams
 * 
 * CAMERA/RUNTIME (Slots 24-27):
 *   24: CameraPos + time
 *   25: CameraForward + aspect
 *   26: CameraUp + fov
 *   27: RenderParams
 * 
 * MATRICES (Slots 28-35):
 *   28-31: InvViewProj (mat4)
 *   32-35: ViewProj (mat4)
 * 
 * DEBUG (Slot 36):
 *   36: DebugParams
 * </pre>
 */
public final class FieldVisualTypes {
    
    private FieldVisualTypes() {} // Namespace only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 0: POSITION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Field position and radius in world space.
     * Written as camera-relative coordinates in the UBO.
     */
    public record PositionParams(
        float centerX,
        float centerY,
        float centerZ,
        float radius
    ) implements Vec4Serializable {
        public static final PositionParams DEFAULT = new PositionParams(0f, 0f, 0f, 3.0f);
        
        @Override public float slot0() { return centerX; }
        @Override public float slot1() { return centerY; }
        @Override public float slot2() { return centerZ; }
        @Override public float slot3() { return radius; }
        
        public PositionParams withCenter(float x, float y, float z) {
            return new PositionParams(x, y, z, radius);
        }
        
        public PositionParams withRadius(float r) {
            return new PositionParams(centerX, centerY, centerZ, r);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOTS 1-5: COLORS (Expanded to 5 colors)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Five-color palette for field effects.
     * Colors stored as ARGB integers, extracted to floats for UBO.
     * 
     * <p>Color mapping by effect type:</p>
     * <ul>
     *   <li>Energy Orb V1/V2: primary=core, secondary=edge, tertiary=glow</li>
     *   <li>Volumetric Star: primary=body, secondary=base, tertiary=dark, 
     *                        highlight=light, ray=rayColor</li>
     *   <li>Geodesic: primary=face, secondary=back, tertiary=edgeGlow</li>
     * </ul>
     */
    public record ColorParams(
        int primaryColor,    // Core/main color (ARGB)
        int secondaryColor,  // Edge/accent color (ARGB)
        int tertiaryColor,   // Outer glow/background tint (ARGB)
        int highlightColor,  // Bright highlight/specular (ARGB) - NEW
        int rayColor         // Ray/corona tint (ARGB) - NEW
    ) {
        public static final ColorParams DEFAULT = new ColorParams(
            0xFFFFFFFF,  // White - primary (core)
            0xFFFFFFFF,  // White - secondary (edge) - original uses same glow for edge
            0xFF1A66CC,  // Blue - tertiary (inner lines) = vec3(0.1, 0.4, 0.8) from original
            0xFFFFFFFF,  // White - highlight
            0xFFFF9933   // Gold - ray
        );
        
        // Primary color extractors
        public float primaryR() { return ((primaryColor >> 16) & 0xFF) / 255f; }
        public float primaryG() { return ((primaryColor >> 8) & 0xFF) / 255f; }
        public float primaryB() { return (primaryColor & 0xFF) / 255f; }
        public float primaryA() { return ((primaryColor >> 24) & 0xFF) / 255f; }
        
        // Secondary color extractors
        public float secondaryR() { return ((secondaryColor >> 16) & 0xFF) / 255f; }
        public float secondaryG() { return ((secondaryColor >> 8) & 0xFF) / 255f; }
        public float secondaryB() { return (secondaryColor & 0xFF) / 255f; }
        public float secondaryA() { return ((secondaryColor >> 24) & 0xFF) / 255f; }
        
        // Tertiary color extractors
        public float tertiaryR() { return ((tertiaryColor >> 16) & 0xFF) / 255f; }
        public float tertiaryG() { return ((tertiaryColor >> 8) & 0xFF) / 255f; }
        public float tertiaryB() { return (tertiaryColor & 0xFF) / 255f; }
        public float tertiaryA() { return ((tertiaryColor >> 24) & 0xFF) / 255f; }
        
        // Highlight color extractors
        public float highlightR() { return ((highlightColor >> 16) & 0xFF) / 255f; }
        public float highlightG() { return ((highlightColor >> 8) & 0xFF) / 255f; }
        public float highlightB() { return (highlightColor & 0xFF) / 255f; }
        public float highlightA() { return ((highlightColor >> 24) & 0xFF) / 255f; }
        
        // Ray color extractors
        public float rayR() { return ((rayColor >> 16) & 0xFF) / 255f; }
        public float rayG() { return ((rayColor >> 8) & 0xFF) / 255f; }
        public float rayB() { return (rayColor & 0xFF) / 255f; }
        public float rayA() { return ((rayColor >> 24) & 0xFF) / 255f; }
        
        // Builders
        public ColorParams withPrimary(int c) { return new ColorParams(c, secondaryColor, tertiaryColor, highlightColor, rayColor); }
        public ColorParams withSecondary(int c) { return new ColorParams(primaryColor, c, tertiaryColor, highlightColor, rayColor); }
        public ColorParams withTertiary(int c) { return new ColorParams(primaryColor, secondaryColor, c, highlightColor, rayColor); }
        public ColorParams withHighlight(int c) { return new ColorParams(primaryColor, secondaryColor, tertiaryColor, c, rayColor); }
        public ColorParams withRay(int c) { return new ColorParams(primaryColor, secondaryColor, tertiaryColor, highlightColor, c); }
        
        /**
         * Creates a 3-color palette (for backward compatibility).
         */
        public static ColorParams ofThree(int primary, int secondary, int tertiary) {
            return new ColorParams(primary, secondary, tertiary, 0xFFFFFFFF, 0xFFFF9933);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOTS 6-7: ANIMATION (Base + Multi-Speed)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Animation and effect type parameters.
     * Includes multi-speed channels for complex effects.
     */
    public record AnimParams(
        // Base animation (vec4 slot 6)
        float phase,           // Animation phase offset
        float speed,           // Base animation speed multiplier (0.5-3.0)
        float intensity,       // Overall brightness (0-2)
        EffectType effectType, // Effect type enum
        
        // Multi-speed channels (vec4 slot 7)
        float speedHigh,       // Fast detail animation - "_SpeedHi" (0-10)
        float speedLow,        // Slow base movement - "_SpeedLow" (0-10)
        float speedRay,        // Ray/corona animation - "_SpeedRay" (0-10)
        float speedRing        // Ring/rotation speed - "_SpeedRing" (0-10)
    ) {
        public static final AnimParams DEFAULT = new AnimParams(
            0f, 1.0f, 1.2f, EffectType.ENERGY_ORB,
            2.0f, 2.0f, 5.0f, 2.0f
        );
        
        // Base builders
        public AnimParams withPhase(float v) { return new AnimParams(v, speed, intensity, effectType, speedHigh, speedLow, speedRay, speedRing); }
        public AnimParams withSpeed(float v) { return new AnimParams(phase, v, intensity, effectType, speedHigh, speedLow, speedRay, speedRing); }
        public AnimParams withIntensity(float v) { return new AnimParams(phase, speed, v, effectType, speedHigh, speedLow, speedRay, speedRing); }
        public AnimParams withEffectType(EffectType t) { return new AnimParams(phase, speed, intensity, t, speedHigh, speedLow, speedRay, speedRing); }
        
        // Multi-speed builders
        public AnimParams withSpeedHigh(float v) { return new AnimParams(phase, speed, intensity, effectType, v, speedLow, speedRay, speedRing); }
        public AnimParams withSpeedLow(float v) { return new AnimParams(phase, speed, intensity, effectType, speedHigh, v, speedRay, speedRing); }
        public AnimParams withSpeedRay(float v) { return new AnimParams(phase, speed, intensity, effectType, speedHigh, speedLow, v, speedRing); }
        public AnimParams withSpeedRing(float v) { return new AnimParams(phase, speed, intensity, effectType, speedHigh, speedLow, speedRay, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 8: ANIMATION TIMING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Animation timing modifiers for complex noise-based effects.
     * Controls how time affects different animation layers.
     */
    public record AnimTimingParams(
        float timeScale,      // Global time multiplier (0.1 = slow)
        float radialSpeed1,   // First radial noise speed (0.35)
        float radialSpeed2,   // Second radial noise speed (0.15)
        float axialSpeed      // Z/axial animation speed (0.015)
    ) implements Vec4Serializable {
        public static final AnimTimingParams DEFAULT = new AnimTimingParams(0.1f, 0.35f, 0.15f, 0.015f);
        
        @Override public float slot0() { return timeScale; }
        @Override public float slot1() { return radialSpeed1; }
        @Override public float slot2() { return radialSpeed2; }
        @Override public float slot3() { return axialSpeed; }
        
        public AnimTimingParams withTimeScale(float v) { return new AnimTimingParams(v, radialSpeed1, radialSpeed2, axialSpeed); }
        public AnimTimingParams withRadialSpeed1(float v) { return new AnimTimingParams(timeScale, v, radialSpeed2, axialSpeed); }
        public AnimTimingParams withRadialSpeed2(float v) { return new AnimTimingParams(timeScale, radialSpeed1, v, axialSpeed); }
        public AnimTimingParams withAxialSpeed(float v) { return new AnimTimingParams(timeScale, radialSpeed1, radialSpeed2, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 9: CORE/EDGE (Expanded with coreFalloff)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Core size and edge sharpness parameters.
     */
    public record CoreEdgeParams(
        float coreSize,       // Relative core size (0.05-0.5)
        float edgeSharpness,  // Edge ring sharpness (1-10)
        float shapeType,      // 0=sphere, 1=torus, 2=cylinder, 3=prism
        float coreFalloff     // Core glow falloff power - "_Glow" (1-100)
    ) implements Vec4Serializable {
        public static final CoreEdgeParams DEFAULT = new CoreEdgeParams(0.5f, 4.0f, 0f, 4.0f);
        
        @Override public float slot0() { return coreSize; }
        @Override public float slot1() { return edgeSharpness; }
        @Override public float slot2() { return shapeType; }
        @Override public float slot3() { return coreFalloff; }
        
        public CoreEdgeParams withCoreSize(float v) { return new CoreEdgeParams(v, edgeSharpness, shapeType, coreFalloff); }
        public CoreEdgeParams withEdgeSharpness(float v) { return new CoreEdgeParams(coreSize, v, shapeType, coreFalloff); }
        public CoreEdgeParams withShapeType(float v) { return new CoreEdgeParams(coreSize, edgeSharpness, v, coreFalloff); }
        public CoreEdgeParams withCoreFalloff(float v) { return new CoreEdgeParams(coreSize, edgeSharpness, shapeType, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 10: FALLOFF PARAMS (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Distance-based falloff parameters for corona and glow effects.
     */
    public record FalloffParams(
        float fadePower,           // Distance fade exponent (0.5)
        float fadeScale,           // Fade distance multiplier (2.0)
        float insideFalloffPower,  // Inside-core falloff power (24.0)
        float coronaEdge           // Corona edge threshold (1.1)
    ) implements Vec4Serializable {
        public static final FalloffParams DEFAULT = new FalloffParams(0.5f, 2.0f, 24.0f, 1.1f);
        
        @Override public float slot0() { return fadePower; }
        @Override public float slot1() { return fadeScale; }
        @Override public float slot2() { return insideFalloffPower; }
        @Override public float slot3() { return coronaEdge; }
        
        public FalloffParams withFadePower(float v) { return new FalloffParams(v, fadeScale, insideFalloffPower, coronaEdge); }
        public FalloffParams withFadeScale(float v) { return new FalloffParams(fadePower, v, insideFalloffPower, coronaEdge); }
        public FalloffParams withInsideFalloffPower(float v) { return new FalloffParams(fadePower, fadeScale, v, coronaEdge); }
        public FalloffParams withCoronaEdge(float v) { return new FalloffParams(fadePower, fadeScale, insideFalloffPower, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 11: NOISE CONFIG (Replaces SpiralParams)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Noise configuration parameters.
     * Controls the base noise used for procedural effects.
     */
    public record NoiseConfigParams(
        float resLow,      // Low frequency noise resolution (15.0)
        float resHigh,     // High frequency noise resolution (45.0)
        float amplitude,   // Noise amplitude multiplier (2.0)
        float seed         // Noise variation seed (-10 to 10)
    ) implements Vec4Serializable {
        public static final NoiseConfigParams DEFAULT = new NoiseConfigParams(5.0f, 5.0f, 2.0f, 0f);
        
        @Override public float slot0() { return resLow; }
        @Override public float slot1() { return resHigh; }
        @Override public float slot2() { return amplitude; }
        @Override public float slot3() { return seed; }
        
        // Legacy compatibility
        public float density() { return resLow; }
        public float twist() { return resHigh / 9f; }
        
        public NoiseConfigParams withResLow(float v) { return new NoiseConfigParams(v, resHigh, amplitude, seed); }
        public NoiseConfigParams withResHigh(float v) { return new NoiseConfigParams(resLow, v, amplitude, seed); }
        public NoiseConfigParams withAmplitude(float v) { return new NoiseConfigParams(resLow, resHigh, v, seed); }
        public NoiseConfigParams withSeed(float v) { return new NoiseConfigParams(resLow, resHigh, amplitude, v); }
        
        public NoiseConfigParams withDensity(float v) { return withResLow(v); }
        public NoiseConfigParams withTwist(float v) { return withResHigh(v * 9f); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 12: NOISE DETAIL (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Noise detail/FBM parameters.
     * Controls the iteration and layering of noise functions.
     */
    public record NoiseDetailParams(
        float baseScale,       // Base detail scale (0.03125)
        float scaleMultiplier, // Per-octave scale mult (4.0)
        float octaves,         // FBM octave count (1-10)
        float baseLevel        // Base noise floor (0.4)
    ) implements Vec4Serializable {
        public static final NoiseDetailParams DEFAULT = new NoiseDetailParams(0.03125f, 4.0f, 7f, 0.4f);
        
        @Override public float slot0() { return baseScale; }
        @Override public float slot1() { return scaleMultiplier; }
        @Override public float slot2() { return octaves; }
        @Override public float slot3() { return baseLevel; }
        
        public NoiseDetailParams withBaseScale(float v) { return new NoiseDetailParams(v, scaleMultiplier, octaves, baseLevel); }
        public NoiseDetailParams withScaleMultiplier(float v) { return new NoiseDetailParams(baseScale, v, octaves, baseLevel); }
        public NoiseDetailParams withOctaves(float v) { return new NoiseDetailParams(baseScale, scaleMultiplier, v, baseLevel); }
        public NoiseDetailParams withBaseLevel(float v) { return new NoiseDetailParams(baseScale, scaleMultiplier, octaves, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 13: GLOW LINES (Expanded with ray params)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Radial glow line and ray parameters.
     */
    public record GlowLineParams(
        float count,           // Radial line count (8-24)
        float intensity,       // Line brightness (0-1)
        float rayPower,        // Ray intensity exponent (1-10)
        float raySharpness     // Ray edge sharpness (0.02-10)
    ) implements Vec4Serializable {
        public static final GlowLineParams DEFAULT = new GlowLineParams(16.0f, 1.0f, 2.0f, 1.0f);
        
        @Override public float slot0() { return count; }
        @Override public float slot1() { return intensity; }
        @Override public float slot2() { return rayPower; }
        @Override public float slot3() { return raySharpness; }
        
        public GlowLineParams withCount(float v) { return new GlowLineParams(v, intensity, rayPower, raySharpness); }
        public GlowLineParams withIntensity(float v) { return new GlowLineParams(count, v, rayPower, raySharpness); }
        public GlowLineParams withRayPower(float v) { return new GlowLineParams(count, intensity, v, raySharpness); }
        public GlowLineParams withRaySharpness(float v) { return new GlowLineParams(count, intensity, rayPower, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 14: CORONA (Expanded)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Corona/outer glow parameters.
     */
    public record CoronaParams(
        float width,       // Corona width (0.2-1.5)
        float power,       // Falloff exponent (2.0)
        float multiplier,  // Brightness multiplier (50.0)
        float ringPower    // Ring glow power (1-10)
    ) implements Vec4Serializable {
        public static final CoronaParams DEFAULT = new CoronaParams(0.5f, 2.0f, 50.0f, 1.0f);
        
        @Override public float slot0() { return width; }
        @Override public float slot1() { return power; }
        @Override public float slot2() { return multiplier; }
        @Override public float slot3() { return ringPower; }
        
        public CoronaParams withWidth(float v) { return new CoronaParams(v, power, multiplier, ringPower); }
        public CoronaParams withPower(float v) { return new CoronaParams(width, v, multiplier, ringPower); }
        public CoronaParams withMultiplier(float v) { return new CoronaParams(width, power, v, ringPower); }
        public CoronaParams withRingPower(float v) { return new CoronaParams(width, power, multiplier, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PULSAR V5 FLAMES PARAMS (UBO Slots 42-43) - NEW
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Flames parameters for Pulsar V5 effect.
     * Controls the animated noise-based fire effect (original Shadertoy's "corona").
     */
    public record FlamesParams(
        // Slot 42
        float edge,            // Distance threshold (original 1.1)
        float power,           // Falloff exponent (original 2.0)
        float multiplier,      // Brightness multiplier (original 50.0)
        float timeScale,       // Time modulation (original 1.2)
        // Slot 43
        float insideFalloff,   // Inside sphere falloff (original 24.0)
        float surfaceNoiseScale // Surface texture scale (5.0)
    ) {
        public static final FlamesParams DEFAULT = new FlamesParams(1.1f, 2.0f, 50.0f, 1.2f, 24.0f, 5.0f);
        
        public FlamesParams withEdge(float v) { return new FlamesParams(v, power, multiplier, timeScale, insideFalloff, surfaceNoiseScale); }
        public FlamesParams withPower(float v) { return new FlamesParams(edge, v, multiplier, timeScale, insideFalloff, surfaceNoiseScale); }
        public FlamesParams withMultiplier(float v) { return new FlamesParams(edge, power, v, timeScale, insideFalloff, surfaceNoiseScale); }
        public FlamesParams withTimeScale(float v) { return new FlamesParams(edge, power, multiplier, v, insideFalloff, surfaceNoiseScale); }
        public FlamesParams withInsideFalloff(float v) { return new FlamesParams(edge, power, multiplier, timeScale, v, surfaceNoiseScale); }
        public FlamesParams withSurfaceNoiseScale(float v) { return new FlamesParams(edge, power, multiplier, timeScale, insideFalloff, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // V2 DETAIL SLOTS (NEW - Full V2 shader control)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * V2 Corona and Core detail parameters (slot 24 in expanded layout).
     */
    public record V2CoronaDetail(
        float coronaStart,       // Where glow begins (0.15)
        float coronaBrightness,  // Corona intensity multiplier
        float coreRadiusScale,   // Core size multiplier (0.1)
        float coreMaskRadius     // Core cutoff radius (0.35)
    ) implements Vec4Serializable {
        public static final V2CoronaDetail DEFAULT = new V2CoronaDetail(0.15f, 1.0f, 0.1f, 0.35f);
        
        @Override public float slot0() { return coronaStart; }
        @Override public float slot1() { return coronaBrightness; }
        @Override public float slot2() { return coreRadiusScale; }
        @Override public float slot3() { return coreMaskRadius; }
        
        public V2CoronaDetail withCoronaStart(float v) { return new V2CoronaDetail(v, coronaBrightness, coreRadiusScale, coreMaskRadius); }
        public V2CoronaDetail withCoronaBrightness(float v) { return new V2CoronaDetail(coronaStart, v, coreRadiusScale, coreMaskRadius); }
        public V2CoronaDetail withCoreRadiusScale(float v) { return new V2CoronaDetail(coronaStart, coronaBrightness, v, coreMaskRadius); }
        public V2CoronaDetail withCoreMaskRadius(float v) { return new V2CoronaDetail(coronaStart, coronaBrightness, coreRadiusScale, v); }
    }
    
    /**
     * V2 Core glow detail parameters (slot 25).
     */
    public record V2CoreDetail(
        float coreSpread,     // Core glow spread multiplier (1.0)
        float coreGlow,       // Core glow intensity (1.0)
        float coreMaskSoft,   // Core edge softness (0.05)
        float edgeRadius      // Edge ring position (0.3)
    ) implements Vec4Serializable {
        public static final V2CoreDetail DEFAULT = new V2CoreDetail(1.0f, 1.0f, 0.05f, 0.3f);
        
        @Override public float slot0() { return coreSpread; }
        @Override public float slot1() { return coreGlow; }
        @Override public float slot2() { return coreMaskSoft; }
        @Override public float slot3() { return edgeRadius; }
        
        public V2CoreDetail withCoreSpread(float v) { return new V2CoreDetail(v, coreGlow, coreMaskSoft, edgeRadius); }
        public V2CoreDetail withCoreGlow(float v) { return new V2CoreDetail(coreSpread, v, coreMaskSoft, edgeRadius); }
        public V2CoreDetail withCoreMaskSoft(float v) { return new V2CoreDetail(coreSpread, coreGlow, v, edgeRadius); }
        public V2CoreDetail withEdgeRadius(float v) { return new V2CoreDetail(coreSpread, coreGlow, coreMaskSoft, v); }
    }
    
    /**
     * V2 Edge ring detail parameters (slot 26).
     */
    public record V2EdgeDetail(
        float edgeSpread,    // Ring spread multiplier (1.0)
        float edgeGlow,      // Ring glow intensity (1.0)
        float sharpScale,    // Sharpness divisor (4.0)
        float linesUVScale   // Pattern UV scale (3.0)
    ) implements Vec4Serializable {
        public static final V2EdgeDetail DEFAULT = new V2EdgeDetail(1.0f, 1.0f, 4.0f, 3.0f);
        
        @Override public float slot0() { return edgeSpread; }
        @Override public float slot1() { return edgeGlow; }
        @Override public float slot2() { return sharpScale; }
        @Override public float slot3() { return linesUVScale; }
        
        public V2EdgeDetail withEdgeSpread(float v) { return new V2EdgeDetail(v, edgeGlow, sharpScale, linesUVScale); }
        public V2EdgeDetail withEdgeGlow(float v) { return new V2EdgeDetail(edgeSpread, v, sharpScale, linesUVScale); }
        public V2EdgeDetail withSharpScale(float v) { return new V2EdgeDetail(edgeSpread, edgeGlow, v, linesUVScale); }
        public V2EdgeDetail withLinesUVScale(float v) { return new V2EdgeDetail(edgeSpread, edgeGlow, sharpScale, v); }
    }
    
    /**
     * V2 Voronoi lines detail parameters (slot 27).
     */
    public record V2LinesDetail(
        float linesDensityMult,  // Layer 2 density multiplier (1.6)
        float linesContrast1,    // Layer 1 power exponent (2.5)
        float linesContrast2,    // Layer 2 power exponent (3.0)
        float linesMaskRadius    // Pattern cutoff radius (0.3)
    ) implements Vec4Serializable {
        public static final V2LinesDetail DEFAULT = new V2LinesDetail(1.6f, 2.5f, 3.0f, 0.3f);
        
        @Override public float slot0() { return linesDensityMult; }
        @Override public float slot1() { return linesContrast1; }
        @Override public float slot2() { return linesContrast2; }
        @Override public float slot3() { return linesMaskRadius; }
        
        public V2LinesDetail withLinesDensityMult(float v) { return new V2LinesDetail(v, linesContrast1, linesContrast2, linesMaskRadius); }
        public V2LinesDetail withLinesContrast1(float v) { return new V2LinesDetail(linesDensityMult, v, linesContrast2, linesMaskRadius); }
        public V2LinesDetail withLinesContrast2(float v) { return new V2LinesDetail(linesDensityMult, linesContrast1, v, linesMaskRadius); }
        public V2LinesDetail withLinesMaskRadius(float v) { return new V2LinesDetail(linesDensityMult, linesContrast1, linesContrast2, v); }
    }
    
    /**
     * V2 Alpha and ray detail parameters (slot 28).
     */
    public record V2AlphaDetail(
        float linesMaskSoft,   // Pattern edge softness (0.02)
        float rayRotSpeed,     // Ray rotation speed (0.3)
        float rayStartRadius,  // Ray origin radius (0.32)
        float alphaScale       // Output alpha multiplier
    ) implements Vec4Serializable {
        public static final V2AlphaDetail DEFAULT = new V2AlphaDetail(0.02f, 0.3f, 0.32f, 1.0f);
        
        @Override public float slot0() { return linesMaskSoft; }
        @Override public float slot1() { return rayRotSpeed; }
        @Override public float slot2() { return rayStartRadius; }
        @Override public float slot3() { return alphaScale; }
        
        public V2AlphaDetail withLinesMaskSoft(float v) { return new V2AlphaDetail(v, rayRotSpeed, rayStartRadius, alphaScale); }
        public V2AlphaDetail withRayRotSpeed(float v) { return new V2AlphaDetail(linesMaskSoft, v, rayStartRadius, alphaScale); }
        public V2AlphaDetail withRayStartRadius(float v) { return new V2AlphaDetail(linesMaskSoft, rayRotSpeed, v, alphaScale); }
        public V2AlphaDetail withAlphaScale(float v) { return new V2AlphaDetail(linesMaskSoft, rayRotSpeed, rayStartRadius, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 15: GEOMETRY (NEW - for SDF effects)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Geometry parameters for SDF-based effects (Geodesic, etc).
     */
    public record GeometryParams(
        float subdivisions,    // Tile density (1-10)
        float roundTop,        // Top edge rounding (0-0.5)
        float roundCorner,     // Corner rounding (0-0.5)
        float thickness        // Shell thickness (0.5-5.0)
    ) implements Vec4Serializable {
        public static final GeometryParams DEFAULT = new GeometryParams(3.0f, 0.05f, 0.1f, 2.0f);
        
        @Override public float slot0() { return subdivisions; }
        @Override public float slot1() { return roundTop; }
        @Override public float slot2() { return roundCorner; }
        @Override public float slot3() { return thickness; }
        
        public GeometryParams withSubdivisions(float v) { return new GeometryParams(v, roundTop, roundCorner, thickness); }
        public GeometryParams withRoundTop(float v) { return new GeometryParams(subdivisions, v, roundCorner, thickness); }
        public GeometryParams withRoundCorner(float v) { return new GeometryParams(subdivisions, roundTop, v, thickness); }
        public GeometryParams withThickness(float v) { return new GeometryParams(subdivisions, roundTop, roundCorner, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 16: GEOMETRY 2 (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Additional geometry parameters.
     */
    public record GeometryParams2(
        float gap,             // Tile gap (0-2)
        float height,          // Extrusion height (0.1-10)
        float waveResolution,  // Wave pattern resolution (1-200, was hardcoded 30)
        float waveAmplitude    // Wave amplitude (0-1)
    ) implements Vec4Serializable {
        public static final GeometryParams2 DEFAULT = new GeometryParams2(0.005f, 2.0f, 30f, 0.1f);
        
        @Override public float slot0() { return gap; }
        @Override public float slot1() { return height; }
        @Override public float slot2() { return waveResolution; }
        @Override public float slot3() { return waveAmplitude; }
        
        public GeometryParams2 withGap(float v) { return new GeometryParams2(v, height, waveResolution, waveAmplitude); }
        public GeometryParams2 withHeight(float v) { return new GeometryParams2(gap, v, waveResolution, waveAmplitude); }
        public GeometryParams2 withWaveResolution(float v) { return new GeometryParams2(gap, height, v, waveAmplitude); }
        public GeometryParams2 withWaveAmplitude(float v) { return new GeometryParams2(gap, height, waveResolution, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 17: GEO ANIMATION (GEODESIC SPECIFIC)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Geodesic animation parameters.
     * Dedicated parameters for geodesic sphere/dome animation control.
     */
    public record GeoAnimParams(
        float animMode,       // Animation mode: 0.0=static, 0.1=wave, 0.2=Y-wave, 0.3=gap breathing
        float rotationSpeed,  // Sphere rotation speed (radians/sec)
        float domeClip,       // Dome clipping: 0=full sphere, 0.5=hemisphere, 1=flat
        float reserved
    ) implements Vec4Serializable {
        public static final GeoAnimParams DEFAULT = new GeoAnimParams(0.1f, 0.2f, 0.0f, 0f);
        
        @Override public float slot0() { return animMode; }
        @Override public float slot1() { return rotationSpeed; }
        @Override public float slot2() { return domeClip; }
        @Override public float slot3() { return reserved; }
        
        public GeoAnimParams withAnimMode(float v) { return new GeoAnimParams(v, rotationSpeed, domeClip, reserved); }
        public GeoAnimParams withRotationSpeed(float v) { return new GeoAnimParams(animMode, v, domeClip, reserved); }
        public GeoAnimParams withDomeClip(float v) { return new GeoAnimParams(animMode, rotationSpeed, v, reserved); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 18: TRANSFORM (GENERIC)
    
    /**
     * Transform parameters for model rotation/scaling.
     */
    public record TransformParams(
        float rotationX,   // Model rotation X (radians)
        float rotationY,   // Model rotation Y (radians)
        float scale,       // Model scale multiplier
        float reserved
    ) implements Vec4Serializable {
        public static final TransformParams DEFAULT = new TransformParams(1.0f, 0.2f, 0.0f, 0f);
        
        @Override public float slot0() { return rotationX; }
        @Override public float slot1() { return rotationY; }
        @Override public float slot2() { return scale; }
        @Override public float slot3() { return reserved; }
        
        public TransformParams withRotationX(float v) { return new TransformParams(v, rotationY, scale, reserved); }
        public TransformParams withRotationY(float v) { return new TransformParams(rotationX, v, scale, reserved); }
        public TransformParams withScale(float v) { return new TransformParams(rotationX, rotationY, v, reserved); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 18: LIGHTING (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Lighting parameters for 3D-style effects.
     */
    public record LightingParams(
        float diffuseStrength,   // Diffuse light mult (1.20)
        float ambientStrength,   // Ambient light mult (0.80)
        float backLightStrength, // Back rim light (0.30)
        float fresnelStrength    // Fresnel rim mult (0.20)
    ) implements Vec4Serializable {
        public static final LightingParams DEFAULT = new LightingParams(1.2f, 0.8f, 0.3f, 0.2f);
        
        @Override public float slot0() { return diffuseStrength; }
        @Override public float slot1() { return ambientStrength; }
        @Override public float slot2() { return backLightStrength; }
        @Override public float slot3() { return fresnelStrength; }
        
        public LightingParams withDiffuseStrength(float v) { return new LightingParams(v, ambientStrength, backLightStrength, fresnelStrength); }
        public LightingParams withAmbientStrength(float v) { return new LightingParams(diffuseStrength, v, backLightStrength, fresnelStrength); }
        public LightingParams withBackLightStrength(float v) { return new LightingParams(diffuseStrength, ambientStrength, v, fresnelStrength); }
        public LightingParams withFresnelStrength(float v) { return new LightingParams(diffuseStrength, ambientStrength, backLightStrength, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 19: TIMING (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Animation timing/loop parameters.
     */
    public record TimingParams(
        float sceneDuration,       // Animation cycle length (6.0s)
        float crossfadeDuration,   // Transition blend time (2.0s)
        float loopMode,            // 0=none, 1/2/3=loop variants
        float animFrequency        // Pattern frequency (10-30)
    ) implements Vec4Serializable {
        public static final TimingParams DEFAULT = new TimingParams(6.0f, 2.0f, 0f, 10.0f);
        
        @Override public float slot0() { return sceneDuration; }
        @Override public float slot1() { return crossfadeDuration; }
        @Override public float slot2() { return loopMode; }
        @Override public float slot3() { return animFrequency; }
        
        public TimingParams withSceneDuration(float v) { return new TimingParams(v, crossfadeDuration, loopMode, animFrequency); }
        public TimingParams withCrossfadeDuration(float v) { return new TimingParams(sceneDuration, v, loopMode, animFrequency); }
        public TimingParams withLoopMode(float v) { return new TimingParams(sceneDuration, crossfadeDuration, v, animFrequency); }
        public TimingParams withAnimFrequency(float v) { return new TimingParams(sceneDuration, crossfadeDuration, loopMode, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 20: SCREEN EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Screen-wide post-process effects.
     */
    public record ScreenEffects(
        float blackout,        // 0-1 screen darkening
        float vignetteAmount,  // 0-1 edge darkening
        float vignetteRadius,  // 0.2-1.0 vignette size
        float tintAmount       // 0-1 color tint strength
    ) implements Vec4Serializable {
        // Default for Proximity Darken: enabled=OFF (strength=0), range=1000, fade=0.1, intensity via ScreenEffects
        public static final ScreenEffects NONE = new ScreenEffects(1.0f, 0f, 0.5f, 0f);
        
        @Override public float slot0() { return blackout; }
        @Override public float slot1() { return vignetteAmount; }
        @Override public float slot2() { return vignetteRadius; }
        @Override public float slot3() { return tintAmount; }
        
        public ScreenEffects withBlackout(float v) { return new ScreenEffects(v, vignetteAmount, vignetteRadius, tintAmount); }
        public ScreenEffects withVignetteAmount(float v) { return new ScreenEffects(blackout, v, vignetteRadius, tintAmount); }
        public ScreenEffects withVignetteRadius(float v) { return new ScreenEffects(blackout, vignetteAmount, v, tintAmount); }
        public ScreenEffects withTintAmount(float v) { return new ScreenEffects(blackout, vignetteAmount, vignetteRadius, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 21: DISTORTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Space distortion effects (lens, refraction, warp).
     */
    public record DistortionParams(
        float strength,    // Distortion intensity
        float radius,      // Distortion falloff
        float frequency,   // Ripple frequency
        float speed        // Animation speed
    ) implements Vec4Serializable {
        // Default for Proximity Darken: strength=1 (ON), radius=1000 blocks, frequency=0.1 (fade), speed=1
        public static final DistortionParams NONE = new DistortionParams(1f, 1000f, 0.1f, 1.0f);
        
        @Override public float slot0() { return strength; }
        @Override public float slot1() { return radius; }
        @Override public float slot2() { return frequency; }
        @Override public float slot3() { return speed; }
        
        public DistortionParams withStrength(float v) { return new DistortionParams(v, radius, frequency, speed); }
        public DistortionParams withRadius(float v) { return new DistortionParams(strength, v, frequency, speed); }
        public DistortionParams withFrequency(float v) { return new DistortionParams(strength, radius, v, speed); }
        public DistortionParams withSpeed(float v) { return new DistortionParams(strength, radius, frequency, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 22: BLEND
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Effect blending/compositing controls.
     */
    public record BlendParams(
        float reserved,   // Reserved (was opacity, unused)
        int blendMode,    // 0=normal, 1=additive, 2=multiply, 3=screen
        float fadeIn,     // Fade-in distance
        float fadeOut     // Fade-out distance
    ) implements Vec4Serializable {
        public static final BlendParams DEFAULT = new BlendParams(1.0f, 1, 0f, 0f);
        
        @Override public float slot0() { return reserved; }
        @Override public float slot1() { return (float) blendMode; }
        @Override public float slot2() { return fadeIn; }
        @Override public float slot3() { return fadeOut; }
        
        public BlendParams withReserved(float v) { return new BlendParams(v, blendMode, fadeIn, fadeOut); }
        public BlendParams withBlendMode(int v) { return new BlendParams(reserved, v, fadeIn, fadeOut); }
        public BlendParams withFadeIn(float v) { return new BlendParams(reserved, blendMode, v, fadeOut); }
        public BlendParams withFadeOut(float v) { return new BlendParams(reserved, blendMode, fadeIn, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UBO SLOT 23: RESERVED
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Reserved slots for rapid prototyping.
     * Includes version and flags that don't belong to a specific group.
     */
    public record ReservedParams(
        float version,           // Effect version (1.0 = V1, 2.0 = V2, etc)
        float rayCoronaFlags,    // bit 0 = showExternalRays, bit 1 = showCorona
        float slot3,             // Used for colorBlendMode injection at render time
        float eruptionContrast   // V7: Controls ray discreteness (2.0=default, higher=more discrete eruptions)
    ) implements Vec4Serializable {
        // Default: version=1, rayCoronaFlags=2 (showCorona=yes, showExternalRays=NO), eruptionContrast=2.0
        public static final ReservedParams DEFAULT = new ReservedParams(1f, 2f, 0f, 2f);
        
        @Override public float slot0() { return version; }
        @Override public float slot1() { return rayCoronaFlags; }
        @Override public float slot2() { return slot3; }
        @Override public float slot3() { return eruptionContrast; }
        
        public boolean showExternalRays() { return ((int) rayCoronaFlags & 1) != 0; }
        public boolean showCorona() { return ((int) rayCoronaFlags & 2) != 0; }
        
        public ReservedParams withVersion(float v) { return new ReservedParams(v, rayCoronaFlags, slot3, eruptionContrast); }
        public ReservedParams withRayCoronaFlags(float v) { return new ReservedParams(version, v, slot3, eruptionContrast); }
        public ReservedParams withShowExternalRays(boolean v) {
            float flags = (v ? 1f : 0f) + (showCorona() ? 2f : 0f);
            return new ReservedParams(version, flags, slot3, eruptionContrast);
        }
        public ReservedParams withShowCorona(boolean v) {
            float flags = (showExternalRays() ? 1f : 0f) + (v ? 2f : 0f);
            return new ReservedParams(version, flags, slot3, eruptionContrast);
        }
        public ReservedParams withSlot3(float v) { return new ReservedParams(version, rayCoronaFlags, v, eruptionContrast); }
        public ReservedParams withEruptionContrast(float v) { return new ReservedParams(version, rayCoronaFlags, slot3, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA/RUNTIME RECORDS (Set per frame, not from config)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Camera state for UBO writing.
     */
    public record CameraParams(
        float posX, float posY, float posZ,
        float time,
        float forwardX, float forwardY, float forwardZ,
        float aspect,
        float upX, float upY, float upZ,
        float fov
    ) {
        public static final CameraParams ORIGIN = new CameraParams(
            0f, 0f, 0f, 0f,
            0f, 0f, 1f, 1.0f,
            0f, 1f, 0f, 1.0f
        );
    }
    
    /**
     * Render parameters for UBO writing.
     */
    public record RenderParams(
        float nearPlane,
        float farPlane,
        float isFlying
    ) {
        public static final RenderParams DEFAULT = new RenderParams(0.05f, 1000f, 0f);
        
        public RenderParams withIsFlying(float v) { return new RenderParams(nearPlane, farPlane, v); }
    }
    
    /**
     * Debug/runtime flags for UBO writing.
     */
    public record DebugParams(
        float camMode,
        float debugMode
    ) {
        public static final DebugParams DEFAULT = new DebugParams(0f, 0f);
        
        public DebugParams withCamMode(float v) { return new DebugParams(v, debugMode); }
        public DebugParams withDebugMode(float v) { return new DebugParams(camMode, v); }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY COMPATIBILITY - SpiralParams alias
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @deprecated Use NoiseConfigParams instead. Kept for backward compatibility.
     */
    @Deprecated
    public record SpiralParams(float density, float twist) {
        public static final SpiralParams DEFAULT = new SpiralParams(5.0f, 5.0f);
        
        public SpiralParams withDensity(float v) { return new SpiralParams(v, twist); }
        public SpiralParams withTwist(float v) { return new SpiralParams(density, v); }
        
        /**
         * Convert to new NoiseConfigParams.
         */
        public NoiseConfigParams toNoiseConfig() {
            return new NoiseConfigParams(density, twist * 9f, 2.0f, 0f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // V8 ELECTRIC AURA PARAMS (UBO Slots 45-48) - NEW
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * V8 Electric Plasma parameters (slot 45).
     * Controls the electric plasma noise texture in the V8 Electric Aura effect.
     */
    public record V8PlasmaParams(
        float scale,       // Pattern size (1-50, default 10)
        float speed,       // Animation speed (0-10, default 1)
        float turbulence,  // Ridged intensity 0=smooth, 1=fully ridged
        float intensity    // Brightness multiplier (0-10, default 1)
    ) implements Vec4Serializable {
        public static final V8PlasmaParams DEFAULT = new V8PlasmaParams(6f, 1f, 1f, 2f);
        
        @Override public float slot0() { return scale; }
        @Override public float slot1() { return speed; }
        @Override public float slot2() { return turbulence; }
        @Override public float slot3() { return intensity; }
        
        public V8PlasmaParams withScale(float v) { return new V8PlasmaParams(v, speed, turbulence, intensity); }
        public V8PlasmaParams withSpeed(float v) { return new V8PlasmaParams(scale, v, turbulence, intensity); }
        public V8PlasmaParams withTurbulence(float v) { return new V8PlasmaParams(scale, speed, v, intensity); }
        public V8PlasmaParams withIntensity(float v) { return new V8PlasmaParams(scale, speed, turbulence, v); }
    }
    
    /**
     * V8 Electric Ring parameters (slots 46-47).
     * Controls the pulsating logarithmic rings in the V8 Electric Aura effect.
     * 
     * NOTE: coreType is stored here (slot 47.2) as a convenience - it controls
     * Core pattern mode (0=Default V7, 1=Electric FBM) but is grouped with ring
     * params in the UBO for slot efficiency.
     */
    public record V8RingParams(
        // Slot 46
        float frequency,     // Number of rings (1-20, default 4)
        float speed,         // Ring expansion rate (0-20, default 10)
        float sharpness,     // Ring edge sharpness (0.1-10, default 3)
        float centerValue,   // Ring brightness center (0-0.5, default 0.1)
        // Slot 47
        float modPower,      // Ring modulation curve (0-2, default 0.9)
        float intensity,     // Ring brightness multiplier (0-10, default 1)
        float coreType       // Core pattern: 0=Default, 1=Electric (uses slot 47.2)
    ) {
        public static final V8RingParams DEFAULT = new V8RingParams(4f, 5f, 28f, 0.1f, 0.9f, 1f, 1f);
        
        /** Boolean accessor for toggle UI - true if Electric core is enabled */
        public boolean isElectricCore() { return coreType > 0.5f; }
        
        public V8RingParams withFrequency(float v) { return new V8RingParams(v, speed, sharpness, centerValue, modPower, intensity, coreType); }
        public V8RingParams withSpeed(float v) { return new V8RingParams(frequency, v, sharpness, centerValue, modPower, intensity, coreType); }
        public V8RingParams withSharpness(float v) { return new V8RingParams(frequency, speed, v, centerValue, modPower, intensity, coreType); }
        public V8RingParams withCenterValue(float v) { return new V8RingParams(frequency, speed, sharpness, v, modPower, intensity, coreType); }
        public V8RingParams withModPower(float v) { return new V8RingParams(frequency, speed, sharpness, centerValue, v, intensity, coreType); }
        public V8RingParams withIntensity(float v) { return new V8RingParams(frequency, speed, sharpness, centerValue, modPower, v, coreType); }
        public V8RingParams withCoreType(float v) { return new V8RingParams(frequency, speed, sharpness, centerValue, modPower, intensity, v); }
        public V8RingParams withCoreType(boolean v) { return new V8RingParams(frequency, speed, sharpness, centerValue, modPower, intensity, v ? 1f : 0f); }
    }
    
    /**
     * V8 Corona envelope parameters (slot 48).
     * Controls the overall corona envelope in the V8 Electric Aura effect.
     */
    public record V8CoronaParams(
        float extent,      // Max corona reach as radius multiplier (1-10, default 2)
        float fadeStart,   // Where envelope fade begins 0-1 (0=edge, 1=center, default 0.5)
        float fadePower,   // Envelope fade curve power (0.1-10, default 1)
        float intensity    // Overall corona brightness (0-10, default 1)
    ) implements Vec4Serializable {
        public static final V8CoronaParams DEFAULT = new V8CoronaParams(2f, 0.5f, 1f, 1f);
        
        @Override public float slot0() { return extent; }
        @Override public float slot1() { return fadeStart; }
        @Override public float slot2() { return fadePower; }
        @Override public float slot3() { return intensity; }
        
        public V8CoronaParams withExtent(float v) { return new V8CoronaParams(v, fadeStart, fadePower, intensity); }
        public V8CoronaParams withFadeStart(float v) { return new V8CoronaParams(extent, v, fadePower, intensity); }
        public V8CoronaParams withFadePower(float v) { return new V8CoronaParams(extent, fadeStart, v, intensity); }
        public V8CoronaParams withIntensity(float v) { return new V8CoronaParams(extent, fadeStart, fadePower, v); }
    }
    
    /**
     * V8 Electric Core parameters (slot 49).
     * Controls the Electric core pattern's flash, fill, and line characteristics.
     * Flash: 0=off (no scene flash), 1=on (scene-wide flash effect)
     * Fill: controls visibility of background fill between lines
     * Darken: 0=white, 0.5=match lines, 1=black (fill color only)
     * LineWidth: controls thickness of electric lines (higher=thinner)
     */
    public record V8ElectricParams(
        float flash,         // Flash effect toggle: 0=off, 1=on scene flash
        float fillIntensity, // Fill visibility: 0=minimal fill, 1=rich fill
        float fillDarken,    // Fill color: 0=white, 0.5=match lines, 1=black
        float lineWidth      // Line thickness: 0=thick, 1=thin (default 0.5)
    ) implements Vec4Serializable {
        public static final V8ElectricParams DEFAULT = new V8ElectricParams(0f, 0.5f, 0.0f, -140f);
        
        @Override public float slot0() { return flash; }
        @Override public float slot1() { return fillIntensity; }
        @Override public float slot2() { return fillDarken; }
        @Override public float slot3() { return lineWidth; }
        
        /** Boolean accessor for toggle UI - true if flash is enabled */
        public boolean isFlashEnabled() { return flash > 0.5f; }
        
        public V8ElectricParams withFlash(float v) { return new V8ElectricParams(v, fillIntensity, fillDarken, lineWidth); }
        public V8ElectricParams withFlash(boolean v) { return new V8ElectricParams(v ? 1f : 0f, fillIntensity, fillDarken, lineWidth); }
        public V8ElectricParams withFillIntensity(float v) { return new V8ElectricParams(flash, v, fillDarken, lineWidth); }
        public V8ElectricParams withFillDarken(float v) { return new V8ElectricParams(flash, fillIntensity, v, lineWidth); }
        public V8ElectricParams withLineWidth(float v) { return new V8ElectricParams(flash, fillIntensity, fillDarken, v); }
    }
}
