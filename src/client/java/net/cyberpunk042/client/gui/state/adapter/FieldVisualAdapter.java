package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.visual.effect.BlendEquation;
import net.cyberpunk042.client.visual.effect.ColorBlendMode;
import net.cyberpunk042.client.visual.effect.EffectType;
import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualRegistry;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Adapter for Field Visual effect parameters.
 * 
 * <p>Field Visual is a <b>field-level</b> effect that sits above the layer/primitive
 * hierarchy. It's not stored per-primitive but alongside the field definition.</p>
 * 
 * <p>Handles paths like {@code fieldVisual.enabled}, {@code fieldVisual.intensity},
 * {@code fieldVisual.primaryR}, etc.</p>
 * 
 * <h3>2026-01 Expansion</h3>
 * <p>Now uses 18 record groups matching the expanded 37-slot UBO layout.</p>
 */
@StateCategory("fieldVisual")
public class FieldVisualAdapter extends AbstractAdapter {
    
    private static final String LOG_TOPIC = "field_visual";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION STATE (using expanded records)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean enabled = false;
    
    // Effect parameter records (matching FieldVisualConfig)
    private ColorParams colors = ColorParams.DEFAULT;
    private AnimParams anim = AnimParams.DEFAULT;
    private AnimTimingParams animTiming = AnimTimingParams.DEFAULT;
    private CoreEdgeParams coreEdge = CoreEdgeParams.DEFAULT;
    private FalloffParams falloff = FalloffParams.DEFAULT;
    private NoiseConfigParams noiseConfig = NoiseConfigParams.DEFAULT;
    private NoiseDetailParams noiseDetail = NoiseDetailParams.DEFAULT;
    private GlowLineParams glowLine = GlowLineParams.DEFAULT;
    private CoronaParams corona = CoronaParams.DEFAULT;
    private GeometryParams geometry = GeometryParams.DEFAULT;
    private GeometryParams2 geometry2 = GeometryParams2.DEFAULT;
    private GeoAnimParams geoAnim = GeoAnimParams.DEFAULT;
    private TransformParams transform = TransformParams.DEFAULT;
    private LightingParams lighting = LightingParams.DEFAULT;
    private TimingParams timing = TimingParams.DEFAULT;
    private ScreenEffects screen = ScreenEffects.NONE;
    private DistortionParams distortion = DistortionParams.NONE;
    private BlendParams blend = BlendParams.DEFAULT;
    private ReservedParams reserved = ReservedParams.DEFAULT;
    private FlamesParams flames = FlamesParams.DEFAULT;  // Pulsar V5 flames
    
    // Color blend mode (how user colors combine with procedural base)
    private ColorBlendMode colorBlendMode = ColorBlendMode.MULTIPLY;
    
    // Hardware blend equation (controls OpenGL blending math)
    private BlendEquation blendEquation = BlendEquation.ADD;
    
    // V2 Detail records (expanded V2 shader control)
    private V2CoronaDetail v2Corona = V2CoronaDetail.DEFAULT;
    private V2CoreDetail v2Core = V2CoreDetail.DEFAULT;
    private V2EdgeDetail v2Edge = V2EdgeDetail.DEFAULT;
    private V2LinesDetail v2Lines = V2LinesDetail.DEFAULT;
    private V2AlphaDetail v2Alpha = V2AlphaDetail.DEFAULT;
    
    // V8 Electric Aura records (dedicated Plasma/Ring/Corona controls)
    private V8PlasmaParams v8Plasma = V8PlasmaParams.DEFAULT;
    private V8RingParams v8Ring = V8RingParams.DEFAULT;
    private V8CoronaParams v8Corona = V8CoronaParams.DEFAULT;
    private V8ElectricParams v8Electric = V8ElectricParams.DEFAULT;
    
    // Preview
    private float previewRadius = 1.0f;
    
    // Source primitive reference (format: "layerIndex.primitiveIndex" or null)
    private String sourceRef = null;
    
    // Preview field tracking
    private UUID previewFieldId = null;
    
    // Linked primitive state
    private Vec3d linkedPosition = null;
    private float linkedRadius = 3.0f;
    private String linkedShapeType = "sphere";
    
    // Follow mode - when true, effect follows player position each frame (like shockwave)
    private boolean followMode = true;
    
    // Currently selected preset name (for UI persistence across rebuilds)
    private String currentPresetName = "Default";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPAWN ANIMATION CONFIG
    // ═══════════════════════════════════════════════════════════════════════════
    
    private net.cyberpunk042.client.input.spawn.SpawnOriginMode spawnOriginMode = 
        net.cyberpunk042.client.input.spawn.SpawnOriginMode.FROM_ABOVE;
    private net.cyberpunk042.client.input.spawn.TargetMode spawnTargetMode = 
        net.cyberpunk042.client.input.spawn.TargetMode.RELATIVE;
    private float targetDistancePercent = 99f;  // 0-100%, how far toward player (50% = halfway)
    private double trueTargetX = 0;
    private double trueTargetY = 64;
    private double trueTargetZ = 0;
    private long spawnInterpolationDurationMs = 10000L;
    private net.cyberpunk042.client.input.spawn.EasingCurve spawnEasingCurve = 
        net.cyberpunk042.client.input.spawn.EasingCurve.EASE_OUT;
    private long spawnFadeInMs = 500L;
    private long spawnFadeOutMs = 500L;
    private long spawnLifetimeMs = 0L;  // 0 = infinite
    private boolean followModeAfterArrival = false;  // Stay fixed at target position after arrival
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THROW ANIMATION STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String orbStartPosition = "center";
    private float throwRange = 30.0f;
    private boolean throwActive = false;
    private float throwProgress = 0.0f;
    private float throwDuration = 1.5f;
    private Vec3d throwStartPos = null;
    private Vec3d throwTargetPos = null;
    private Vec3d throwDirection = null;
    private long throwStartTime = 0;
    
    public String category() { return "fieldVisual"; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED GET (required by AbstractAdapter)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public Object get(String path) {
        String prop = path.startsWith("fieldVisual.") ? path.substring(12) : path;
        
        return switch (prop) {
            case "enabled" -> enabled;
            case "effectType" -> anim.effectType();
            case "intensity" -> anim.intensity();
            case "animationSpeed", "speed" -> anim.speed();
            
            // Colors - primary/secondary/tertiary/highlight/ray (RGB only - alpha handled by shader)
            case "primaryR" -> colors.primaryR();
            case "primaryG" -> colors.primaryG();
            case "primaryB" -> colors.primaryB();
            case "secondaryR" -> colors.secondaryR();
            case "secondaryG" -> colors.secondaryG();
            case "secondaryB" -> colors.secondaryB();
            case "tertiaryR" -> colors.tertiaryR();
            case "tertiaryG" -> colors.tertiaryG();
            case "tertiaryB" -> colors.tertiaryB();
            case "highlightR" -> colors.highlightR();
            case "highlightG" -> colors.highlightG();
            case "highlightB" -> colors.highlightB();
            case "rayR" -> colors.rayR();
            case "rayG" -> colors.rayG();
            case "rayB" -> colors.rayB();
            case "colorBlendMode" -> colorBlendMode;
            
            // AnimParams multi-speed
            case "speedHigh" -> anim.speedHigh();
            case "speedLow" -> anim.speedLow();
            case "speedRay" -> anim.speedRay();
            case "speedRing" -> anim.speedRing();
            
            // AnimTimingParams
            case "timeScale" -> animTiming.timeScale();
            case "radialSpeed1" -> animTiming.radialSpeed1();
            case "radialSpeed2" -> animTiming.radialSpeed2();
            case "axialSpeed" -> animTiming.axialSpeed();
            
            // Core/Edge
            case "coreSize" -> coreEdge.coreSize();
            case "edgeSharpness" -> coreEdge.edgeSharpness();
            case "shapeType" -> coreEdge.shapeType();
            case "coreFalloff" -> coreEdge.coreFalloff();
            
            // Falloff
            case "fadePower" -> falloff.fadePower();
            case "fadeScale" -> falloff.fadeScale();
            case "insideFalloffPower" -> falloff.insideFalloffPower();
            case "coronaEdge" -> falloff.coronaEdge();
            
            // NoiseConfig (legacy spiral aliases)
            case "spiralDensity", "noiseResLow" -> noiseConfig.resLow();
            case "spiralTwist", "noiseResHigh" -> noiseConfig.resHigh();
            case "noiseAmplitude" -> noiseConfig.amplitude();
            case "noiseSeed" -> noiseConfig.seed();
            
            // NoiseDetail
            case "noiseBaseScale" -> noiseDetail.baseScale();
            case "noiseScaleMultiplier" -> noiseDetail.scaleMultiplier();
            case "noiseOctaves" -> noiseDetail.octaves();
            case "noiseBaseLevel" -> noiseDetail.baseLevel();
            
            // GlowLine
            case "glowLineCount" -> glowLine.count();
            case "glowLineIntensity" -> glowLine.intensity();
            case "rayPower" -> glowLine.rayPower();
            case "raySharpness" -> glowLine.raySharpness();
            
            // Corona
            case "coronaWidth" -> corona.width();
            case "coronaPower" -> corona.power();
            case "coronaMultiplier" -> corona.multiplier();
            case "ringPower" -> corona.ringPower();
            
            // Flames (Pulsar V5)
            case "flamesEdge" -> flames.edge();
            case "flamesPower" -> flames.power();
            case "flamesMult" -> flames.multiplier();
            case "flamesTimeScale" -> flames.timeScale();
            case "flamesInsideFalloff" -> flames.insideFalloff();
            case "surfaceNoiseScale" -> flames.surfaceNoiseScale();
            
            // Geometry
            case "geoSubdivisions" -> geometry.subdivisions();
            case "geoRoundTop" -> geometry.roundTop();
            case "geoRoundCorner" -> geometry.roundCorner();
            case "geoThickness" -> geometry.thickness();
            
            // Geometry2
            case "geoGap" -> geometry2.gap();
            case "geoHeight" -> geometry2.height();
            case "geoWaveResolution" -> geometry2.waveResolution();
            case "geoWaveAmplitude" -> geometry2.waveAmplitude();
            
            // Geodesic Animation (dedicated params)
            case "geoAnimMode" -> geoAnim.animMode();
            case "geoRotationSpeed" -> geoAnim.rotationSpeed();
            case "geoDomeClip" -> geoAnim.domeClip();
            
            // Transform (generic, not used for Geodesic)
            case "transRotationX" -> transform.rotationX();
            case "transRotationY" -> transform.rotationY();
            case "transScale" -> transform.scale();
            
            // Lighting
            case "lightDiffuse" -> lighting.diffuseStrength();
            case "lightAmbient" -> lighting.ambientStrength();
            case "lightBackLight" -> lighting.backLightStrength();
            case "lightFresnel" -> lighting.fresnelStrength();
            
            // Timing
            case "timingSceneDuration" -> timing.sceneDuration();
            case "timingCrossfade" -> timing.crossfadeDuration();
            case "timingLoopMode" -> timing.loopMode();
            case "timingAnimFrequency" -> timing.animFrequency();
            
            // ScreenEffects
            case "blackout" -> screen.blackout();
            case "vignetteAmount" -> screen.vignetteAmount();
            case "vignetteRadius" -> screen.vignetteRadius();
            case "tintAmount" -> screen.tintAmount();
            
            // DistortionParams
            case "distortionStrength" -> distortion.strength();
            case "distortionRadius" -> distortion.radius();
            case "distortionFrequency" -> distortion.frequency();
            case "distortionSpeed" -> distortion.speed();
            
            // BlendParams
            case "blendReserved" -> blend.reserved();
            case "blendMode" -> blend.blendMode();
            case "fadeIn" -> blend.fadeIn();
            case "fadeOut" -> blend.fadeOut();
            
            // Reserved (version/flags/eruptionContrast)
            case "version" -> (int) reserved.version();
            case "showExternalRays" -> reserved.showExternalRays() ? 1f : 0f;
            case "showCorona" -> reserved.showCorona() ? 1f : 0f;
            case "eruptionContrast" -> reserved.eruptionContrast();
            
            // V2 Corona Detail
            case "v2CoronaStart" -> v2Corona.coronaStart();
            case "v2CoronaBrightness" -> v2Corona.coronaBrightness();
            case "v2CoreRadiusScale" -> v2Corona.coreRadiusScale();
            case "v2CoreMaskRadius" -> v2Corona.coreMaskRadius();
            
            // V2 Core Detail
            case "v2CoreSpread" -> v2Core.coreSpread();
            case "v2CoreGlow" -> v2Core.coreGlow();
            case "v2CoreMaskSoft" -> v2Core.coreMaskSoft();
            case "v2EdgeRadius" -> v2Core.edgeRadius();
            
            // V2 Edge Detail
            case "v2EdgeSpread" -> v2Edge.edgeSpread();
            case "v2EdgeGlow" -> v2Edge.edgeGlow();
            case "v2SharpScale" -> v2Edge.sharpScale();
            case "v2LinesUVScale" -> v2Edge.linesUVScale();
            
            // V2 Lines Detail
            case "v2LinesDensityMult" -> v2Lines.linesDensityMult();
            case "v2LinesContrast1" -> v2Lines.linesContrast1();
            case "v2LinesContrast2" -> v2Lines.linesContrast2();
            case "v2LinesMaskRadius" -> v2Lines.linesMaskRadius();
            
            // V2 Alpha Detail
            case "v2LinesMaskSoft" -> v2Alpha.linesMaskSoft();
            case "v2RayRotSpeed" -> v2Alpha.rayRotSpeed();
            case "v2RayStartRadius" -> v2Alpha.rayStartRadius();
            case "v2AlphaScale" -> v2Alpha.alphaScale();
            
            // V8 Plasma
            case "v8PlasmaScale" -> v8Plasma.scale();
            case "v8PlasmaSpeed" -> v8Plasma.speed();
            case "v8PlasmaTurbulence" -> v8Plasma.turbulence();
            case "v8PlasmaIntensity" -> v8Plasma.intensity();
            
            // V8 Ring
            case "v8RingFrequency" -> v8Ring.frequency();
            case "v8RingSpeed" -> v8Ring.speed();
            case "v8RingSharpness" -> v8Ring.sharpness();
            case "v8RingCenterValue" -> v8Ring.centerValue();
            case "v8RingModPower" -> v8Ring.modPower();
            case "v8RingIntensity" -> v8Ring.intensity();
            case "v8CoreType" -> v8Ring.coreType();  // Return float (0 or 1) for toggle
            
            // V8 Corona
            case "v8CoronaExtent" -> v8Corona.extent();
            case "v8CoronaFadeStart" -> v8Corona.fadeStart();
            case "v8CoronaFadePower" -> v8Corona.fadePower();
            case "v8CoronaIntensity" -> v8Corona.intensity();
            
            // V8 Electric Core
            case "v8ElectricFlash" -> v8Electric.flash();
            case "v8ElectricFillIntensity" -> v8Electric.fillIntensity();
            case "v8ElectricFillDarken" -> v8Electric.fillDarken();
            case "v8ElectricLineWidth" -> v8Electric.lineWidth();
            
            // Preview/linking
            case "previewRadius" -> previewRadius;
            case "sourceRef" -> sourceRef;
            case "currentPresetName" -> currentPresetName;
            case "followMode" -> followMode;
            case "blendEquation" -> blendEquation;
            
            // Throw animation
            case "orbStartPosition" -> orbStartPosition;
            case "throwRange" -> throwRange;
            
            // Spawn animation
            case "targetDistancePercent" -> targetDistancePercent;
            
            default -> {
                Logging.GUI.topic(LOG_TOPIC).warn("Unknown fieldVisual property: {}", prop);
                yield null;
            }
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED SET (required by AbstractAdapter)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void set(String path, Object value) {
        String prop = path.startsWith("fieldVisual.") ? path.substring(12) : path;
        
        switch (prop) {
            case "enabled" -> {
                enabled = toBool(value);
                // DO NOT call setEnabled(enabled) here - it would affect ALL fields including spawn orbs
                // Preview orb is managed through syncToEffect which registers/unregisters it
            }
            case "effectType" -> {
                EffectType et = EffectType.ENERGY_ORB;
                if (value instanceof EffectType t) {
                    et = t;
                } else if (value != null) {
                    try { et = EffectType.valueOf(value.toString()); } 
                    catch (IllegalArgumentException e) { /* keep default */ }
                }
                anim = anim.withEffectType(et);
            }
            case "intensity" -> anim = anim.withIntensity(toFloat(value));
            case "animationSpeed", "speed" -> anim = anim.withSpeed(toFloat(value));
            
            // Colors (RGB only)
            case "primaryR" -> colors = colors.withPrimary(colorWithR(colors.primaryColor(), toFloat(value)));
            case "primaryG" -> colors = colors.withPrimary(colorWithG(colors.primaryColor(), toFloat(value)));
            case "primaryB" -> colors = colors.withPrimary(colorWithB(colors.primaryColor(), toFloat(value)));
            case "secondaryR" -> colors = colors.withSecondary(colorWithR(colors.secondaryColor(), toFloat(value)));
            case "secondaryG" -> colors = colors.withSecondary(colorWithG(colors.secondaryColor(), toFloat(value)));
            case "secondaryB" -> colors = colors.withSecondary(colorWithB(colors.secondaryColor(), toFloat(value)));
            case "tertiaryR" -> colors = colors.withTertiary(colorWithR(colors.tertiaryColor(), toFloat(value)));
            case "tertiaryG" -> colors = colors.withTertiary(colorWithG(colors.tertiaryColor(), toFloat(value)));
            case "tertiaryB" -> colors = colors.withTertiary(colorWithB(colors.tertiaryColor(), toFloat(value)));
            case "highlightR" -> colors = colors.withHighlight(colorWithR(colors.highlightColor(), toFloat(value)));
            case "highlightG" -> colors = colors.withHighlight(colorWithG(colors.highlightColor(), toFloat(value)));
            case "highlightB" -> colors = colors.withHighlight(colorWithB(colors.highlightColor(), toFloat(value)));
            case "rayR" -> colors = colors.withRay(colorWithR(colors.rayColor(), toFloat(value)));
            case "rayG" -> colors = colors.withRay(colorWithG(colors.rayColor(), toFloat(value)));
            case "rayB" -> colors = colors.withRay(colorWithB(colors.rayColor(), toFloat(value)));
            case "colorBlendMode" -> {
                if (value instanceof ColorBlendMode m) {
                    colorBlendMode = m;
                } else if (value != null) {
                    try { colorBlendMode = ColorBlendMode.valueOf(value.toString()); }
                    catch (IllegalArgumentException e) { /* keep current */ }
                }
            }
            case "blendEquation" -> {
                if (value instanceof BlendEquation e) {
                    blendEquation = e;
                } else if (value != null) {
                    try { blendEquation = BlendEquation.valueOf(value.toString()); }
                    catch (IllegalArgumentException e) { /* keep current */ }
                }
            }
            
            // AnimParams multi-speed
            case "speedHigh" -> anim = anim.withSpeedHigh(toFloat(value));
            case "speedLow" -> anim = anim.withSpeedLow(toFloat(value));
            case "speedRay" -> anim = anim.withSpeedRay(toFloat(value));
            case "speedRing" -> anim = anim.withSpeedRing(toFloat(value));
            
            // AnimTimingParams
            case "timeScale" -> animTiming = animTiming.withTimeScale(toFloat(value));
            case "radialSpeed1" -> animTiming = animTiming.withRadialSpeed1(toFloat(value));
            case "radialSpeed2" -> animTiming = animTiming.withRadialSpeed2(toFloat(value));
            case "axialSpeed" -> animTiming = animTiming.withAxialSpeed(toFloat(value));
            
            // Core/Edge
            case "coreSize" -> coreEdge = coreEdge.withCoreSize(toFloat(value));
            case "edgeSharpness" -> coreEdge = coreEdge.withEdgeSharpness(toFloat(value));
            case "shapeType" -> coreEdge = coreEdge.withShapeType(toFloat(value));
            case "coreFalloff" -> coreEdge = coreEdge.withCoreFalloff(toFloat(value));
            
            // Falloff
            case "fadePower" -> falloff = falloff.withFadePower(toFloat(value));
            case "fadeScale" -> falloff = falloff.withFadeScale(toFloat(value));
            case "insideFalloffPower" -> falloff = falloff.withInsideFalloffPower(toFloat(value));
            case "coronaEdge" -> falloff = falloff.withCoronaEdge(toFloat(value));
            
            // NoiseConfig (legacy spiral aliases)
            case "spiralDensity", "noiseResLow" -> noiseConfig = noiseConfig.withResLow(toFloat(value));
            case "spiralTwist", "noiseResHigh" -> noiseConfig = noiseConfig.withResHigh(toFloat(value));
            case "noiseAmplitude" -> noiseConfig = noiseConfig.withAmplitude(toFloat(value));
            case "noiseSeed" -> noiseConfig = noiseConfig.withSeed(toFloat(value));
            
            // NoiseDetail
            case "noiseBaseScale" -> noiseDetail = noiseDetail.withBaseScale(toFloat(value));
            case "noiseScaleMultiplier" -> noiseDetail = noiseDetail.withScaleMultiplier(toFloat(value));
            case "noiseOctaves" -> noiseDetail = noiseDetail.withOctaves(toFloat(value));
            case "noiseBaseLevel" -> noiseDetail = noiseDetail.withBaseLevel(toFloat(value));
            
            // GlowLine
            case "glowLineCount" -> glowLine = glowLine.withCount(toFloat(value));
            case "glowLineIntensity" -> glowLine = glowLine.withIntensity(toFloat(value));
            case "rayPower" -> glowLine = glowLine.withRayPower(toFloat(value));
            case "raySharpness" -> glowLine = glowLine.withRaySharpness(toFloat(value));
            
            // Corona
            case "coronaWidth" -> corona = corona.withWidth(toFloat(value));
            case "coronaPower" -> corona = corona.withPower(toFloat(value));
            case "coronaMultiplier" -> corona = corona.withMultiplier(toFloat(value));
            case "ringPower" -> corona = corona.withRingPower(toFloat(value));
            
            // Flames (Pulsar V5)
            case "flamesEdge" -> flames = flames.withEdge(toFloat(value));
            case "flamesPower" -> flames = flames.withPower(toFloat(value));
            case "flamesMult" -> flames = flames.withMultiplier(toFloat(value));
            case "flamesTimeScale" -> flames = flames.withTimeScale(toFloat(value));
            case "flamesInsideFalloff" -> flames = flames.withInsideFalloff(toFloat(value));
            case "surfaceNoiseScale" -> flames = flames.withSurfaceNoiseScale(toFloat(value));
            
            // Geometry
            case "geoSubdivisions" -> geometry = geometry.withSubdivisions(toFloat(value));
            case "geoRoundTop" -> geometry = geometry.withRoundTop(toFloat(value));
            case "geoRoundCorner" -> geometry = geometry.withRoundCorner(toFloat(value));
            case "geoThickness" -> geometry = geometry.withThickness(toFloat(value));
            
            // Geometry2
            case "geoGap" -> geometry2 = geometry2.withGap(toFloat(value));
            case "geoHeight" -> geometry2 = geometry2.withHeight(toFloat(value));
            case "geoWaveResolution" -> geometry2 = geometry2.withWaveResolution(toFloat(value));
            case "geoWaveAmplitude" -> geometry2 = geometry2.withWaveAmplitude(toFloat(value));
            
            // Geodesic Animation (dedicated)
            case "geoAnimMode" -> geoAnim = geoAnim.withAnimMode(toFloat(value));
            case "geoRotationSpeed" -> geoAnim = geoAnim.withRotationSpeed(toFloat(value));
            case "geoDomeClip" -> geoAnim = geoAnim.withDomeClip(toFloat(value));
            
            // Transform (generic)
            case "transRotationX" -> transform = transform.withRotationX(toFloat(value));
            case "transRotationY" -> transform = transform.withRotationY(toFloat(value));
            case "transScale" -> transform = transform.withScale(toFloat(value));
            
            // Lighting
            case "lightDiffuse" -> lighting = lighting.withDiffuseStrength(toFloat(value));
            case "lightAmbient" -> lighting = lighting.withAmbientStrength(toFloat(value));
            case "lightBackLight" -> lighting = lighting.withBackLightStrength(toFloat(value));
            case "lightFresnel" -> lighting = lighting.withFresnelStrength(toFloat(value));
            
            // Timing
            case "timingSceneDuration" -> timing = timing.withSceneDuration(toFloat(value));
            case "timingCrossfade" -> timing = timing.withCrossfadeDuration(toFloat(value));
            case "timingLoopMode" -> timing = timing.withLoopMode(toFloat(value));
            case "timingAnimFrequency" -> timing = timing.withAnimFrequency(toFloat(value));
            
            // ScreenEffects
            case "blackout" -> screen = screen.withBlackout(toFloat(value));
            case "vignetteAmount" -> screen = screen.withVignetteAmount(toFloat(value));
            case "vignetteRadius" -> screen = screen.withVignetteRadius(toFloat(value));
            case "tintAmount" -> screen = screen.withTintAmount(toFloat(value));
            
            // DistortionParams
            case "distortionStrength" -> distortion = distortion.withStrength(toFloat(value));
            case "distortionRadius" -> distortion = distortion.withRadius(toFloat(value));
            case "distortionFrequency" -> distortion = distortion.withFrequency(toFloat(value));
            case "distortionSpeed" -> distortion = distortion.withSpeed(toFloat(value));
            
            // BlendParams
            case "blendReserved" -> blend = blend.withReserved(toFloat(value));
            case "blendMode" -> blend = blend.withBlendMode(toInt(value));
            case "fadeIn" -> blend = blend.withFadeIn(toFloat(value));
            case "fadeOut" -> blend = blend.withFadeOut(toFloat(value));
            
            // Reserved (version/flags/eruptionContrast)
            case "version" -> reserved = reserved.withVersion(toFloat(value));
            case "showExternalRays" -> reserved = reserved.withShowExternalRays(toFloat(value) > 0.5f);
            case "showCorona" -> reserved = reserved.withShowCorona(toFloat(value) > 0.5f);
            case "eruptionContrast" -> reserved = reserved.withEruptionContrast(toFloat(value));
            
            // V2 Corona Detail
            case "v2CoronaStart" -> v2Corona = v2Corona.withCoronaStart(toFloat(value));
            case "v2CoronaBrightness" -> v2Corona = v2Corona.withCoronaBrightness(toFloat(value));
            case "v2CoreRadiusScale" -> v2Corona = v2Corona.withCoreRadiusScale(toFloat(value));
            case "v2CoreMaskRadius" -> v2Corona = v2Corona.withCoreMaskRadius(toFloat(value));
            
            // V2 Core Detail
            case "v2CoreSpread" -> v2Core = v2Core.withCoreSpread(toFloat(value));
            case "v2CoreGlow" -> v2Core = v2Core.withCoreGlow(toFloat(value));
            case "v2CoreMaskSoft" -> v2Core = v2Core.withCoreMaskSoft(toFloat(value));
            case "v2EdgeRadius" -> v2Core = v2Core.withEdgeRadius(toFloat(value));
            
            // V2 Edge Detail
            case "v2EdgeSpread" -> v2Edge = v2Edge.withEdgeSpread(toFloat(value));
            case "v2EdgeGlow" -> v2Edge = v2Edge.withEdgeGlow(toFloat(value));
            case "v2SharpScale" -> v2Edge = v2Edge.withSharpScale(toFloat(value));
            case "v2LinesUVScale" -> v2Edge = v2Edge.withLinesUVScale(toFloat(value));
            
            // V2 Lines Detail
            case "v2LinesDensityMult" -> v2Lines = v2Lines.withLinesDensityMult(toFloat(value));
            case "v2LinesContrast1" -> v2Lines = v2Lines.withLinesContrast1(toFloat(value));
            case "v2LinesContrast2" -> v2Lines = v2Lines.withLinesContrast2(toFloat(value));
            case "v2LinesMaskRadius" -> v2Lines = v2Lines.withLinesMaskRadius(toFloat(value));
            
            // V2 Alpha Detail
            case "v2LinesMaskSoft" -> v2Alpha = v2Alpha.withLinesMaskSoft(toFloat(value));
            case "v2RayRotSpeed" -> v2Alpha = v2Alpha.withRayRotSpeed(toFloat(value));
            case "v2RayStartRadius" -> v2Alpha = v2Alpha.withRayStartRadius(toFloat(value));
            case "v2AlphaScale" -> v2Alpha = v2Alpha.withAlphaScale(toFloat(value));
            
            // V8 Plasma
            case "v8PlasmaScale" -> v8Plasma = v8Plasma.withScale(toFloat(value));
            case "v8PlasmaSpeed" -> v8Plasma = v8Plasma.withSpeed(toFloat(value));
            case "v8PlasmaTurbulence" -> v8Plasma = v8Plasma.withTurbulence(toFloat(value));
            case "v8PlasmaIntensity" -> v8Plasma = v8Plasma.withIntensity(toFloat(value));
            
            // V8 Ring
            case "v8RingFrequency" -> v8Ring = v8Ring.withFrequency(toFloat(value));
            case "v8RingSpeed" -> v8Ring = v8Ring.withSpeed(toFloat(value));
            case "v8RingSharpness" -> v8Ring = v8Ring.withSharpness(toFloat(value));
            case "v8RingCenterValue" -> v8Ring = v8Ring.withCenterValue(toFloat(value));
            case "v8RingModPower" -> v8Ring = v8Ring.withModPower(toFloat(value));
            case "v8RingIntensity" -> v8Ring = v8Ring.withIntensity(toFloat(value));
            case "v8CoreType" -> v8Ring = v8Ring.withCoreType(toFloat(value));
            
            // V8 Corona
            case "v8CoronaExtent" -> v8Corona = v8Corona.withExtent(toFloat(value));
            case "v8CoronaFadeStart" -> v8Corona = v8Corona.withFadeStart(toFloat(value));
            case "v8CoronaFadePower" -> v8Corona = v8Corona.withFadePower(toFloat(value));
            case "v8CoronaIntensity" -> v8Corona = v8Corona.withIntensity(toFloat(value));
            
            // V8 Electric Core
            case "v8ElectricFlash" -> v8Electric = v8Electric.withFlash(toFloat(value));
            case "v8ElectricFillIntensity" -> v8Electric = v8Electric.withFillIntensity(toFloat(value));
            case "v8ElectricFillDarken" -> v8Electric = v8Electric.withFillDarken(toFloat(value));
            case "v8ElectricLineWidth" -> v8Electric = v8Electric.withLineWidth(toFloat(value));
            
            // Preview/linking
            case "previewRadius" -> previewRadius = toFloat(value);
            case "sourceRef" -> sourceRef = value != null ? value.toString() : null;
            case "currentPresetName" -> currentPresetName = value != null ? value.toString() : "Default";
            case "followMode" -> followMode = toBool(value);
            
            // Throw animation
            case "orbStartPosition" -> orbStartPosition = value != null ? value.toString() : "front";
            case "throwRange" -> throwRange = toFloat(value);
            
            // Spawn animation config
            case "spawnOriginMode" -> {
                if (value instanceof net.cyberpunk042.client.input.spawn.SpawnOriginMode m) {
                    spawnOriginMode = m;
                    System.out.println("[FVA] spawnOriginMode set to: " + m.name() + " (direct)");
                } else if (value != null) {
                    try { 
                        spawnOriginMode = net.cyberpunk042.client.input.spawn.SpawnOriginMode.valueOf(value.toString());
                        System.out.println("[FVA] spawnOriginMode set to: " + spawnOriginMode.name() + " (parsed)");
                    }
                    catch (IllegalArgumentException e) { 
                        System.out.println("[FVA] Failed to parse spawnOriginMode: " + value);
                    }
                }
            }
            case "spawnTargetMode" -> {
                if (value instanceof net.cyberpunk042.client.input.spawn.TargetMode m) {
                    spawnTargetMode = m;
                } else if (value != null) {
                    try { spawnTargetMode = net.cyberpunk042.client.input.spawn.TargetMode.valueOf(value.toString()); }
                    catch (IllegalArgumentException e) { /* keep current */ }
                }
            }
            // spawnDistance and targetDistance are computed from proximityDarken, not user input
            case "targetDistancePercent" -> {
                targetDistancePercent = toFloat(value);
                System.out.println("[FVA] SET targetDistancePercent=" + targetDistancePercent + " from value=" + value);
            }
            case "trueTargetX" -> trueTargetX = toDouble(value);
            case "trueTargetY" -> trueTargetY = toDouble(value);
            case "trueTargetZ" -> trueTargetZ = toDouble(value);
            case "spawnInterpolationDurationMs" -> spawnInterpolationDurationMs = toLong(value);
            case "spawnEasingCurve" -> {
                if (value instanceof net.cyberpunk042.client.input.spawn.EasingCurve c) {
                    spawnEasingCurve = c;
                } else if (value != null) {
                    try { spawnEasingCurve = net.cyberpunk042.client.input.spawn.EasingCurve.valueOf(value.toString()); }
                    catch (IllegalArgumentException e) { /* keep current */ }
                }
            }
            case "spawnFadeInMs" -> spawnFadeInMs = toLong(value);
            case "spawnFadeOutMs" -> spawnFadeOutMs = toLong(value);
            case "spawnLifetimeMs" -> spawnLifetimeMs = toLong(value);
            case "followModeAfterArrival" -> followModeAfterArrival = toBool(value);
            
            default -> Logging.GUI.topic(LOG_TOPIC).warn("Unknown fieldVisual property: {}", prop);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private float toFloat(Object v) { 
        if (v instanceof Number n) return n.floatValue();
        if (v instanceof Boolean b) return b ? 1f : 0f;
        return 0f;
    }
    
    private int toInt(Object v) { 
        return v instanceof Number n ? n.intValue() : 0; 
    }
    
    private boolean toBool(Object v) { 
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); 
    }
    
    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
    
    private long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }
    
    /** Modify just the R component of an ARGB color */
    private int colorWithR(int argb, float r) {
        int ir = (int)(clamp(r) * 255);
        return (argb & 0xFF00FFFF) | (ir << 16);
    }
    
    /** Modify just the G component of an ARGB color */
    private int colorWithG(int argb, float g) {
        int ig = (int)(clamp(g) * 255);
        return (argb & 0xFFFF00FF) | (ig << 8);
    }
    
    /** Modify just the B component of an ARGB color */
    private int colorWithB(int argb, float b) {
        int ib = (int)(clamp(b) * 255);
        return (argb & 0xFFFFFF00) | ib;
    }
    
    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isEnabled() { return enabled; }
    public String sourceRef() { return sourceRef; }
    public EffectType effectType() { return anim.effectType(); }
    public ColorBlendMode colorBlendMode() { return colorBlendMode; }
    public BlendEquation blendEquation() { return blendEquation; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIG BUILDING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Builds a FieldVisualConfig from current record state.
     */
    public FieldVisualConfig buildConfig() {
        // Inject colorBlendMode into reserved.slot3 for shader access
        ReservedParams reservedWithMode = reserved.withSlot3(colorBlendMode.getShaderValue());
        
        return new FieldVisualConfig(
            colors,
            anim,
            animTiming,
            coreEdge,
            falloff,
            noiseConfig,
            noiseDetail,
            glowLine,
            corona,
            geometry,
            geometry2,
            transform,
            lighting,
            timing,
            screen,
            distortion,
            blend,
            reservedWithMode,
            v2Corona,
            v2Core,
            v2Edge,
            v2Lines,
            v2Alpha,
            flames,
            geoAnim,
            v8Plasma,
            v8Ring,
            v8Corona,
            v8Electric
        );
    }
    
    /**
     * Builds an OrbSpawnConfig from current spawn animation settings.
     * 
     * <p>Spawn distance = proximityDarken (distortion.radius()).
     * Target distance = spawnDistance * (1 - targetDistancePercent/100).
     * At 0%: orb stays at spawn. At 100%: orb reaches player.</p>
     */
    public net.cyberpunk042.client.input.spawn.OrbSpawnConfig buildSpawnConfig() {
        net.minecraft.util.math.Vec3d trueTarget = 
            spawnTargetMode == net.cyberpunk042.client.input.spawn.TargetMode.TRUE_TARGET
                ? new net.minecraft.util.math.Vec3d(trueTargetX, trueTargetY, trueTargetZ)
                : null;
        
        // Spawn distance = proximityDarken, with fallback of 1000 if not configured
        float spawnDistance = Math.max(distortion.radius(), 1000f);
        
        // Target distance = where orb stops
        // 0% = stays at spawnDistance, 50% = halfway, 100% = at player
        float targetDistance = spawnDistance * (1f - targetDistancePercent / 100f);
        System.out.println("[FVA] buildSpawnConfig: originMode=" + spawnOriginMode.name() + " spawnDist=" + spawnDistance + " pct=" + targetDistancePercent + " targetDist=" + targetDistance);
        
        return new net.cyberpunk042.client.input.spawn.OrbSpawnConfig(
            spawnOriginMode,
            spawnTargetMode,
            spawnDistance,
            targetDistance,
            trueTarget,
            spawnInterpolationDurationMs,
            spawnEasingCurve,
            spawnFadeInMs,
            spawnFadeOutMs,
            spawnLifetimeMs,
            followModeAfterArrival
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC TO EFFECT SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Syncs current config to the visual effect system.
     * Called by panel after changes.
     */
    public void syncToEffect(Vec3d position) {
        Logging.GUI.topic(LOG_TOPIC).info("syncToEffect called, enabled={}, position={}", enabled, position);
        
        if (!enabled) {
            if (previewFieldId != null) {
                FieldVisualRegistry.unregister(previewFieldId);
                previewFieldId = null;
            }
            // DO NOT call setEnabled(false) here - spawn orbs must continue running independently
            // The effect will auto-disable when no fields exist at all
            return;
        }
        
        FieldVisualConfig config = buildConfig();
        
        float radius = (sourceRef != null && linkedPosition != null) ? linkedRadius : previewRadius;
        String shapeType = (sourceRef != null) ? linkedShapeType : "sphere";
        
        Vec3d pos = position;
        if (sourceRef != null && linkedPosition != null) {
            pos = pos.add(linkedPosition);
        }
        
        if (previewFieldId == null) {
            previewFieldId = UUID.randomUUID();
            FieldVisualInstance preview = new FieldVisualInstance(
                previewFieldId,
                UUID.randomUUID(),
                pos,
                radius,
                shapeType,
                config
            );
            if (linkedPosition != null) {
                preview.setAnchorOffset(linkedPosition);
            }
            FieldVisualRegistry.register(preview);
            
            // Position updates handled by tickPreviewPosition()
            Logging.GUI.topic(LOG_TOPIC).info("Created preview field at pos={}, followMode={}", pos, followMode);
        } else {
            if (!followMode) {
                // Position stays fixed
            }
            FieldVisualRegistry.updateRadius(previewFieldId, radius);
            FieldVisualInstance existing = FieldVisualRegistry.get(previewFieldId);
            if (existing != null) {
                existing.updateConfig(config);
                existing.updateShapeType(shapeType);
                if (linkedPosition != null) {
                    existing.setAnchorOffset(linkedPosition);
                }
            }
            // Position updates handled by tickPreviewPosition()
        }
        
        FieldVisualPostEffect.setEnabled(true);
    }
    
    public void clearPreview() {
        if (previewFieldId != null) {
            FieldVisualRegistry.unregister(previewFieldId);
            previewFieldId = null;
        }
    }
    
    /**
     * Updates preview orb position each frame when followMode is enabled.
     * Called from render mixin - replaces the global tickFollowPosition system.
     */
    public void tickPreviewPosition() {
        if (!enabled || !followMode || previewFieldId == null) {
            return;
        }
        
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        FieldVisualInstance field = FieldVisualRegistry.get(previewFieldId);
        if (field == null) return;
        
        // Use bounding box center
        Vec3d playerCenter = client.player.getBoundingBox().getCenter();
        
        // Apply anchor offset from the primitive
        Vec3d anchorOffset = field.getAnchorOffset();
        Vec3d finalPos = playerCenter.add(anchorOffset);
        
        // Apply orb position offset if no linked primitive
        if (field.getAnchorOffset().equals(Vec3d.ZERO)) {
            String orbPos = orbStartPosition;
            if (orbPos != null && !orbPos.equals("center")) {
                // Get camera for forward/right calculation
                var camera = client.gameRenderer.getCamera();
                float yaw = (float) Math.toRadians(camera.getYaw());
                float pitch = (float) Math.toRadians(camera.getPitch());
                
                float fwdX = (float) (-Math.sin(yaw) * Math.cos(pitch));
                float fwdY = (float) (-Math.sin(pitch));
                float fwdZ = (float) (Math.cos(yaw) * Math.cos(pitch));
                
                Vec3d forward = new Vec3d(fwdX, fwdY, fwdZ).normalize();
                Vec3d worldUp = new Vec3d(0, 1, 0);
                Vec3d right = forward.crossProduct(worldUp).normalize();
                if (right.lengthSquared() < 0.01) {
                    right = new Vec3d(1, 0, 0);
                }
                
                float dist = 2.0f;
                Vec3d orbOffset = switch (orbPos) {
                    case "front" -> forward.multiply(dist);
                    case "behind" -> forward.multiply(-dist);
                    case "left" -> right.multiply(-dist);
                    case "right" -> right.multiply(dist);
                    case "above" -> new Vec3d(0, dist, 0);
                    default -> Vec3d.ZERO;
                };
                finalPos = finalPos.add(orbOffset);
            }
        }
        
        FieldVisualRegistry.updatePosition(previewFieldId, finalPos);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKED PRIMITIVE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setLinkedPosition(Vec3d position) { this.linkedPosition = position; }
    public void setLinkedRadius(float radius) { this.linkedRadius = radius; }
    public void setLinkedShapeType(String shapeType) { this.linkedShapeType = shapeType; }
    public Vec3d getLinkedPosition() { return linkedPosition; }
    
    public boolean isFollowMode() { return followMode; }
    public void setFollowMode(boolean follow) { this.followMode = follow; }
    
    public void tickPosition(Vec3d playerPosition) {
        if (!enabled || !followMode || previewFieldId == null) {
            return;
        }
        
        Vec3d pos = playerPosition;
        if (sourceRef != null && linkedPosition != null) {
            pos = pos.add(linkedPosition);
        }
        
        FieldVisualRegistry.updatePosition(previewFieldId, pos);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THROW ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void triggerThrow(Vec3d playerPos, Vec3d lookDir, Vec3d rightDir) {
        if (!enabled || throwActive) {
            return;
        }
        
        Vec3d startOffset = getOrbStartOffset(lookDir, rightDir);
        throwStartPos = playerPos.add(startOffset);
        
        // ALWAYS throw toward where player is looking (crosshair), NOT based on orb position
        throwDirection = lookDir;
        
        // Target is where crosshair points at throwRange distance from PLAYER
        throwTargetPos = playerPos.add(lookDir.multiply(throwRange));
        
        throwActive = true;
        throwProgress = 0.0f;
        throwStartTime = System.currentTimeMillis();
        
        followMode = false;
        FieldVisualPostEffect.setFollowMode(false);
        
        if (previewFieldId != null) {
            FieldVisualRegistry.updatePosition(previewFieldId, throwStartPos);
            FieldVisualInstance field = FieldVisualRegistry.get(previewFieldId);
            if (field != null) {
                field.resetInterpolation();
            }
        }
        
        Logging.GUI.topic(LOG_TOPIC).info("Throw triggered: start={}, target={}, range={}",
            throwStartPos, throwTargetPos, throwRange);
    }
    
    private Vec3d getOrbStartOffset(Vec3d lookDir, Vec3d rightDir) {
        float offsetDist = 2.0f;
        
        // Up vector for hand positioning
        Vec3d upDir = new Vec3d(0, 1, 0);
        
        return switch (orbStartPosition) {
            case "center" -> Vec3d.ZERO;
            case "front" -> lookDir.multiply(offsetDist);
            case "behind" -> lookDir.multiply(-offsetDist);
            case "left" -> rightDir.multiply(-offsetDist);
            case "right" -> rightDir.multiply(offsetDist);
            case "above" -> upDir.multiply(offsetDist);
            
            // New positions: Hand-like (visible in first person, like holding a block)
            case "left-hand" -> rightDir.multiply(-0.6).add(lookDir.multiply(0.8)).add(upDir.multiply(-0.3));
            case "right-hand" -> rightDir.multiply(0.6).add(lookDir.multiply(0.8)).add(upDir.multiply(-0.3));
            
            // Front corners (visible but not centered)
            case "left-front" -> lookDir.multiply(offsetDist).add(rightDir.multiply(-offsetDist * 0.7));
            case "right-front" -> lookDir.multiply(offsetDist).add(rightDir.multiply(offsetDist * 0.7));
            
            default -> Vec3d.ZERO;
        };
    }
    
    public boolean tickThrow() {
        if (!throwActive) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - throwStartTime;
        float durationMs = throwDuration * 1000.0f;
        throwProgress = Math.min(1.0f, elapsed / durationMs);
        
        float easedProgress = easeOutCubic(throwProgress);
        
        Vec3d currentPos = throwStartPos.lerp(throwTargetPos, easedProgress);
        
        if (previewFieldId != null) {
            FieldVisualRegistry.updatePosition(previewFieldId, currentPos);
            
            // Fade out near the end
            if (throwProgress > 0.8f) {
                float fadeProgress = (throwProgress - 0.8f) / 0.2f;
                float newIntensity = anim.intensity() * (1.0f - fadeProgress);
                FieldVisualInstance field = FieldVisualRegistry.get(previewFieldId);
                if (field != null) {
                    FieldVisualConfig fadedConfig = buildConfig().withIntensity(newIntensity);
                    field.updateConfig(fadedConfig);
                }
            }
        }
        
        if (throwProgress >= 1.0f) {
            finishThrow();
            return false;
        }
        
        return true;
    }
    
    private void finishThrow() {
        throwActive = false;
        throwProgress = 0.0f;
        throwStartPos = null;
        throwTargetPos = null;
        throwDirection = null;
        
        followMode = true;
        FieldVisualPostEffect.setFollowMode(true);
        
        if (previewFieldId != null) {
            FieldVisualInstance field = FieldVisualRegistry.get(previewFieldId);
            if (field != null) {
                field.updateConfig(buildConfig());
                field.resetInterpolation();
            }
        }
        
        Logging.GUI.topic(LOG_TOPIC).info("Throw finished, returning to follow mode");
    }
    
    private float easeOutCubic(float t) {
        return 1.0f - (float)Math.pow(1.0 - t, 3);
    }
    
    public boolean isThrowActive() { return throwActive; }
    public float getThrowProgress() { return throwProgress; }
    
    public void clearLinkedPrimitive() {
        linkedPosition = null;
        linkedRadius = 3.0f;
        linkedShapeType = "sphere";
    }
    
    /**
     * Syncs geometry from a source primitive.
     */
    public void syncFromPrimitive(net.cyberpunk042.field.primitive.Primitive primitive) {
        if (primitive == null) return;
        
        var transform = primitive.transform();
        
        if (transform != null) {
            var effectiveOffset = transform.getEffectiveOffset();
            linkedPosition = new Vec3d(effectiveOffset.x, effectiveOffset.y, effectiveOffset.z);
            Logging.GUI.topic(LOG_TOPIC).info("Captured primitive effective offset: {} (anchor={}, additional={})", 
                linkedPosition, transform.anchor(), transform.offset());
            
            linkedRadius = transform.scale();
        } else {
            linkedPosition = Vec3d.ZERO;
            linkedRadius = 1.0f;
            Logging.GUI.topic(LOG_TOPIC).info("Primitive has no transform, using defaults");
        }
        
        if (primitive.type() != null) {
            linkedShapeType = primitive.type().toString().toLowerCase();
        }
        
        Logging.GUI.topic(LOG_TOPIC).debug("Synced from primitive: type={} radius={}", 
            linkedShapeType, linkedRadius);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void reset() {
        // NOTE: We do NOT reset 'enabled' here - that's a separate toggle
        // 
        // IDENTITY PARAMETERS (preserved across reset):
        // - effectType: What kind of effect (ENERGY_ORB, GEODESIC, etc)
        // - version: Which shader version (V1-V8)
        // - currentPresetName: UI state
        // - followMode: UI preference for orb following player
        // These describe WHAT effect is being used or UI state, not HOW it looks.
        
        // Save identity parameters before reset
        EffectType savedEffectType = anim.effectType();
        float savedVersion = reserved.version();
        boolean savedFollowMode = followMode;  // Preserve UI state
        
        // Reset shader params to their defaults
        colors = ColorParams.DEFAULT;
        anim = AnimParams.DEFAULT.withEffectType(savedEffectType);  // Preserve effectType
        animTiming = AnimTimingParams.DEFAULT;
        coreEdge = CoreEdgeParams.DEFAULT;
        falloff = FalloffParams.DEFAULT;
        noiseConfig = NoiseConfigParams.DEFAULT;
        noiseDetail = NoiseDetailParams.DEFAULT;
        glowLine = GlowLineParams.DEFAULT;
        corona = CoronaParams.DEFAULT;
        geometry = GeometryParams.DEFAULT;
        geometry2 = GeometryParams2.DEFAULT;
        geoAnim = GeoAnimParams.DEFAULT;
        transform = TransformParams.DEFAULT;
        lighting = LightingParams.DEFAULT;
        timing = TimingParams.DEFAULT;
        screen = ScreenEffects.NONE;
        distortion = DistortionParams.NONE;
        blend = BlendParams.DEFAULT;
        reserved = ReservedParams.DEFAULT.withVersion(savedVersion);  // Preserve version
        
        // V2 detail params
        v2Corona = V2CoronaDetail.DEFAULT;
        v2Core = V2CoreDetail.DEFAULT;
        v2Edge = V2EdgeDetail.DEFAULT;
        v2Lines = V2LinesDetail.DEFAULT;
        v2Alpha = V2AlphaDetail.DEFAULT;
        
        // Pulsar flames params
        flames = FlamesParams.DEFAULT;
        
        // V8 specific params
        v8Plasma = V8PlasmaParams.DEFAULT;
        v8Ring = V8RingParams.DEFAULT;
        v8Corona = V8CoronaParams.DEFAULT;
        v8Electric = V8ElectricParams.DEFAULT;
        
        followMode = savedFollowMode;  // Restore UI state
        previewRadius = 1.0f;
        sourceRef = null;
        // NOTE: currentPresetName is UI state, managed by preset selector - NOT reset here
        
        clearPreview();
        clearLinkedPrimitive();
        
        // DO NOT call setEnabled(false) - spawn orbs must continue running
        // Preview is already cleared by clearPreview()
        
        // NOTE: We do NOT apply schema defaults here. The caller should apply them
        // after the version is known (e.g., after loadFromJson sets the version from preset).
        // Applying schema defaults here would use the preserved version which may be wrong.
    }
    
    /**
     * Applies default values from an EffectSchema.
     * 
     * <p>This makes the schema the single source of truth for defaults.
     * First resets all shader params to Java defaults to clear any version-specific
     * values (e.g., V8 params when switching to V1), then applies the schema defaults.</p>
     * 
     * @param schema The effect schema containing parameter defaults
     */
    public void applySchemaDefaults(net.cyberpunk042.client.gui.schema.EffectSchema schema) {
        if (schema == null) {
            Logging.GUI.topic("fieldVisual").warn("Cannot apply null schema defaults");
            return;
        }
        
        // FIRST: Reset all shader params to Java defaults
        // This clears version-specific values (e.g., V8 params when switching to V1)
        resetShaderParamsToDefaults();
        
        // THEN: Apply schema defaults on top
        int applied = 0;
        for (var param : schema.allParameters()) {
            try {
                // Extract the path after "fieldVisual." prefix
                String fullPath = param.path();
                if (fullPath.startsWith("fieldVisual.")) {
                    String key = fullPath.substring("fieldVisual.".length());
                    
                    // Apply the default value
                    if (param.isBoolean()) {
                        set(fullPath, param.defaultValue() > 0.5f);
                    } else {
                        set(fullPath, param.defaultValue());
                    }
                    applied++;
                }
            } catch (Exception e) {
                Logging.GUI.topic("fieldVisual").debug("Could not apply default for {}: {}", 
                    param.path(), e.getMessage());
            }
        }
        
        Logging.GUI.topic("fieldVisual").info("Applied {} schema defaults from {}", 
            applied, schema.displayName());
    }
    
    /**
     * Resets all shader parameters to their Java defaults.
     * Called before applying schema defaults to ensure clean state.
     */
    private void resetShaderParamsToDefaults() {
        // Core shader params
        colors = ColorParams.DEFAULT;
        anim = anim.withEffectType(anim.effectType()); // Preserve effect type
        animTiming = AnimTimingParams.DEFAULT;
        coreEdge = CoreEdgeParams.DEFAULT;
        falloff = FalloffParams.DEFAULT;
        noiseConfig = NoiseConfigParams.DEFAULT;
        noiseDetail = NoiseDetailParams.DEFAULT;
        glowLine = GlowLineParams.DEFAULT;
        corona = CoronaParams.DEFAULT;
        geometry = GeometryParams.DEFAULT;
        geometry2 = GeometryParams2.DEFAULT;
        geoAnim = GeoAnimParams.DEFAULT;
        transform = TransformParams.DEFAULT;
        lighting = LightingParams.DEFAULT;
        timing = TimingParams.DEFAULT;
        screen = ScreenEffects.NONE;
        distortion = DistortionParams.NONE;
        blend = BlendParams.DEFAULT;
        flames = FlamesParams.DEFAULT;
        
        // V2 detail params
        v2Corona = V2CoronaDetail.DEFAULT;
        v2Core = V2CoreDetail.DEFAULT;
        v2Edge = V2EdgeDetail.DEFAULT;
        v2Lines = V2LinesDetail.DEFAULT;
        v2Alpha = V2AlphaDetail.DEFAULT;
        
        // V8 specific params - critical for V8→V1 transition!
        v8Plasma = V8PlasmaParams.DEFAULT;
        v8Ring = V8RingParams.DEFAULT;
        v8Corona = V8CoronaParams.DEFAULT;
        v8Electric = V8ElectricParams.DEFAULT;
        
        // Note: reserved.version is preserved (set externally before this is called)
        // Note: anim.intensity and anim.speed could be preserved, but schema will override
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON SERIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        
        json.addProperty("enabled", enabled);
        json.addProperty("effectType", anim.effectType().name());
        json.addProperty("version", (int) reserved.version());
        
        if (sourceRef != null) {
            json.addProperty("sourceRef", sourceRef);
        }
        
        // General
        var general = new com.google.gson.JsonObject();
        general.addProperty("intensity", anim.intensity());
        general.addProperty("animationSpeed", anim.speed());
        general.addProperty("previewRadius", previewRadius);
        json.add("general", general);
        
        // Colors - use serializer
        json.add("colors", FieldVisualSerializer.saveColors(colors));
        
        // Params - use serializer (includes geodesic params)
        json.add("params", FieldVisualSerializer.saveParams(
            anim, animTiming, coreEdge, falloff, noiseConfig, noiseDetail,
            glowLine, corona, lighting, transform, flames, reserved,
            v2Corona, v2Core, v2Edge, v2Lines, v2Alpha,
            geometry, geometry2, geoAnim
        ));
        
        return json;
    }
    
    /**
     * Loads configuration from JSON, applying all fields including version.
     */
    public void loadFromJson(com.google.gson.JsonObject json) {
        loadFromJson(json, false);
    }
    
    /**
     * Loads configuration from JSON with optional version skipping.
     * 
     * @param json The JSON configuration
     * @param skipVersion If true, the version field in the JSON is ignored (useful for version switching)
     */
    public void loadFromJson(com.google.gson.JsonObject json, boolean skipVersion) {
        if (json == null) return;

        
        // Track keys from general section
        if (json.has("general")) {
            var general = json.getAsJsonObject("general");
        }

        // Track nested colors section too
        if (json.has("colors")) {
            var colors = json.getAsJsonObject("colors");
        }
        
        if (json.has("enabled")) {
            enabled = json.get("enabled").getAsBoolean();
        }
        if (json.has("effectType")) {
            try {
                anim = anim.withEffectType(EffectType.valueOf(json.get("effectType").getAsString()));
            } catch (IllegalArgumentException e) {
                anim = anim.withEffectType(EffectType.ENERGY_ORB);
            }
        }
        if (!skipVersion && json.has("version")) {
            reserved = reserved.withVersion(json.get("version").getAsFloat());
        }
        if (json.has("sourceRef")) {
            sourceRef = json.get("sourceRef").getAsString();
        }
        
        // General
        if (json.has("general")) {
            var general = json.getAsJsonObject("general");
            if (general.has("intensity")) anim = anim.withIntensity(general.get("intensity").getAsFloat());
            if (general.has("animationSpeed")) anim = anim.withSpeed(general.get("animationSpeed").getAsFloat());
            if (general.has("previewRadius")) previewRadius = general.get("previewRadius").getAsFloat();
        }
        
        // Colors - try nested "colors" section first (new format)
        if (json.has("colors")) {
            colors = FieldVisualSerializer.loadColors(json.getAsJsonObject("colors"), colors);
        }
        // Fallback: load top-level color arrays (legacy/existing profile format)
        if (json.has("primaryColor")) {
            var c = json.getAsJsonArray("primaryColor");
            if (c.size() >= 3) colors = colors.withPrimary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
        }
        if (json.has("secondaryColor")) {
            var c = json.getAsJsonArray("secondaryColor");
            if (c.size() >= 3) colors = colors.withSecondary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
        }
        if (json.has("tertiaryColor")) {
            var c = json.getAsJsonArray("tertiaryColor");
            if (c.size() >= 3) colors = colors.withTertiary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
        }
        if (json.has("highlightColor")) {
            var c = json.getAsJsonArray("highlightColor");
            if (c.size() >= 3) colors = colors.withHighlight(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
        }
        if (json.has("rayColor")) {
            var c = json.getAsJsonArray("rayColor");
            if (c.size() >= 3) colors = colors.withRay(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
        }
        
        // Effect params - use serializer for all record types
        if (json.has("params")) {
            var params = json.getAsJsonObject("params");
            
            // Load all param groups via serializer
            anim = FieldVisualSerializer.loadAnim(params, anim);
            animTiming = FieldVisualSerializer.loadAnimTiming(params, animTiming);
            coreEdge = FieldVisualSerializer.loadCoreEdge(params, coreEdge);
            falloff = FieldVisualSerializer.loadFalloff(params, falloff);
            noiseConfig = FieldVisualSerializer.loadNoiseConfig(params, noiseConfig);
            noiseDetail = FieldVisualSerializer.loadNoiseDetail(params, noiseDetail);
            glowLine = FieldVisualSerializer.loadGlowLine(params, glowLine);
            corona = FieldVisualSerializer.loadCorona(params, corona);
            lighting = FieldVisualSerializer.loadLighting(params, lighting);
            transform = FieldVisualSerializer.loadTransform(params, transform);
            flames = FieldVisualSerializer.loadFlames(params, flames);
            reserved = FieldVisualSerializer.loadReserved(params, reserved);
            geometry = FieldVisualSerializer.loadGeometry(params, geometry);
            geometry2 = FieldVisualSerializer.loadGeometry2(params, geometry2);
            geoAnim = FieldVisualSerializer.loadGeoAnim(params, geoAnim);
            distortion = FieldVisualSerializer.loadDistortion(params, distortion);
            screen = FieldVisualSerializer.loadScreenEffects(params, screen);
            
            // V2 detail params
            v2Corona = FieldVisualSerializer.loadV2Corona(params, v2Corona);
            v2Core = FieldVisualSerializer.loadV2Core(params, v2Core);
            v2Edge = FieldVisualSerializer.loadV2Edge(params, v2Edge);
            v2Lines = FieldVisualSerializer.loadV2Lines(params, v2Lines);
            v2Alpha = FieldVisualSerializer.loadV2Alpha(params, v2Alpha);
            
            // Special non-record fields
            if (params.has("previewRadius")) previewRadius = params.get("previewRadius").getAsFloat();
            if (params.has("followMode")) followMode = params.get("followMode").getAsBoolean();
        }
        
        Logging.GUI.topic(LOG_TOPIC).debug("Loaded field visual config from JSON");
    }
    
    private int rgbToArgb(float r, float g, float b) {
        int ir = (int)(clamp(r) * 255);
        int ig = (int)(clamp(g) * 255);
        int ib = (int)(clamp(b) * 255);
        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }
    
    public boolean hasPreview() { return previewFieldId != null; }
    public FieldVisualConfig getConfig() { return buildConfig(); }
}
