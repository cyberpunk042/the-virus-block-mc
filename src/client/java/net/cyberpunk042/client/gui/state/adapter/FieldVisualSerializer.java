package net.cyberpunk042.client.gui.state.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;

/**
 * JSON serialization/deserialization for FieldVisual configuration.
 * 
 * <p>Extracted from FieldVisualAdapter to keep that class focused on
 * the adapter pattern. This class handles all the field mapping between
 * JSON profile format and the internal record types.</p>
 * 
 * <h3>Profile JSON Structure</h3>
 * <pre>
 * {
 *   "name": "...",
 *   "enabled": true,
 *   "effectType": "ENERGY_ORB",
 *   "version": 6,
 *   "primaryColor": [r, g, b],
 *   "secondaryColor": [r, g, b],
 *   "tertiaryColor": [r, g, b],
 *   "highlightColor": [r, g, b],
 *   "rayColor": [r, g, b],
 *   "params": { ... all params ... }
 * }
 * </pre>
 */
public final class FieldVisualSerializer {
    
    private FieldVisualSerializer() {} // Static utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DESERIALIZATION (loadFromJson)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Load ColorParams from JSON colors object.
     */
    public static ColorParams loadColors(JsonObject colorsJson, ColorParams current) {
        ColorParams colors = current;
        
        if (colorsJson.has("primary")) {
            var c = colorsJson.getAsJsonArray("primary");
            if (c.size() >= 3) {
                colors = colors.withPrimary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
            }
        }
        if (colorsJson.has("secondary")) {
            var c = colorsJson.getAsJsonArray("secondary");
            if (c.size() >= 3) {
                colors = colors.withSecondary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
            }
        }
        if (colorsJson.has("tertiary")) {
            var c = colorsJson.getAsJsonArray("tertiary");
            if (c.size() >= 3) {
                colors = colors.withTertiary(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
            }
        }
        if (colorsJson.has("highlight")) {
            var c = colorsJson.getAsJsonArray("highlight");
            if (c.size() >= 3) {
                colors = colors.withHighlight(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
            }
        }
        if (colorsJson.has("ray")) {
            var c = colorsJson.getAsJsonArray("ray");
            if (c.size() >= 3) {
                colors = colors.withRay(rgbToArgb(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()));
            }
        }
        
        return colors;
    }
    
    /**
     * Load AnimParams from JSON params object.
     */
    public static AnimParams loadAnim(JsonObject params, AnimParams current) {
        AnimParams anim = current;
        
        if (params.has("intensity")) anim = anim.withIntensity(params.get("intensity").getAsFloat());
        if (params.has("animationSpeed")) anim = anim.withSpeed(params.get("animationSpeed").getAsFloat());
        if (params.has("speedLow")) anim = anim.withSpeedLow(params.get("speedLow").getAsFloat());
        if (params.has("speedHigh")) anim = anim.withSpeedHigh(params.get("speedHigh").getAsFloat());
        if (params.has("speedRay")) anim = anim.withSpeedRay(params.get("speedRay").getAsFloat());
        if (params.has("speedRing")) anim = anim.withSpeedRing(params.get("speedRing").getAsFloat());
        
        return anim;
    }
    
    /**
     * Load AnimTimingParams from JSON params object.
     */
    public static AnimTimingParams loadAnimTiming(JsonObject params, AnimTimingParams current) {
        AnimTimingParams animTiming = current;
        
        if (params.has("timeScale")) animTiming = animTiming.withTimeScale(params.get("timeScale").getAsFloat());
        if (params.has("radialSpeed1")) animTiming = animTiming.withRadialSpeed1(params.get("radialSpeed1").getAsFloat());
        if (params.has("radialSpeed2")) animTiming = animTiming.withRadialSpeed2(params.get("radialSpeed2").getAsFloat());
        if (params.has("axialSpeed")) animTiming = animTiming.withAxialSpeed(params.get("axialSpeed").getAsFloat());
        
        return animTiming;
    }
    
    /**
     * Load CoreEdgeParams from JSON params object.
     */
    public static CoreEdgeParams loadCoreEdge(JsonObject params, CoreEdgeParams current) {
        CoreEdgeParams coreEdge = current;
        
        if (params.has("coreSize")) coreEdge = coreEdge.withCoreSize(params.get("coreSize").getAsFloat());
        if (params.has("edgeSharpness")) coreEdge = coreEdge.withEdgeSharpness(params.get("edgeSharpness").getAsFloat());
        if (params.has("shapeType")) coreEdge = coreEdge.withShapeType(params.get("shapeType").getAsFloat());
        if (params.has("coreFalloff")) coreEdge = coreEdge.withCoreFalloff(params.get("coreFalloff").getAsFloat());
        
        return coreEdge;
    }
    
    /**
     * Load FalloffParams from JSON params object.
     */
    public static FalloffParams loadFalloff(JsonObject params, FalloffParams current) {
        FalloffParams falloff = current;
        
        if (params.has("fadePower")) falloff = falloff.withFadePower(params.get("fadePower").getAsFloat());
        if (params.has("fadeScale")) falloff = falloff.withFadeScale(params.get("fadeScale").getAsFloat());
        if (params.has("insideFalloffPower")) falloff = falloff.withInsideFalloffPower(params.get("insideFalloffPower").getAsFloat());
        if (params.has("coronaEdge")) falloff = falloff.withCoronaEdge(params.get("coronaEdge").getAsFloat());
        
        return falloff;
    }
    
    /**
     * Load NoiseConfigParams from JSON params object.
     */
    public static NoiseConfigParams loadNoiseConfig(JsonObject params, NoiseConfigParams current) {
        NoiseConfigParams noiseConfig = current;
        
        if (params.has("noiseResLow")) noiseConfig = noiseConfig.withResLow(params.get("noiseResLow").getAsFloat());
        if (params.has("noiseResHigh")) noiseConfig = noiseConfig.withResHigh(params.get("noiseResHigh").getAsFloat());
        if (params.has("noiseAmplitude")) noiseConfig = noiseConfig.withAmplitude(params.get("noiseAmplitude").getAsFloat());
        if (params.has("noiseSeed")) noiseConfig = noiseConfig.withSeed(params.get("noiseSeed").getAsFloat());
        // Legacy aliases
        if (params.has("spiralDensity")) noiseConfig = noiseConfig.withResLow(params.get("spiralDensity").getAsFloat());
        if (params.has("spiralTwist")) noiseConfig = noiseConfig.withResHigh(params.get("spiralTwist").getAsFloat());
        
        return noiseConfig;
    }
    
    /**
     * Load NoiseDetailParams from JSON params object.
     */
    public static NoiseDetailParams loadNoiseDetail(JsonObject params, NoiseDetailParams current) {
        NoiseDetailParams noiseDetail = current;
        
        if (params.has("noiseBaseScale")) noiseDetail = noiseDetail.withBaseScale(params.get("noiseBaseScale").getAsFloat());
        if (params.has("noiseScaleMultiplier")) noiseDetail = noiseDetail.withScaleMultiplier(params.get("noiseScaleMultiplier").getAsFloat());
        if (params.has("noiseOctaves")) noiseDetail = noiseDetail.withOctaves(params.get("noiseOctaves").getAsFloat());
        if (params.has("noiseBaseLevel")) noiseDetail = noiseDetail.withBaseLevel(params.get("noiseBaseLevel").getAsFloat());
        
        return noiseDetail;
    }
    
    /**
     * Load GlowLineParams from JSON params object.
     */
    public static GlowLineParams loadGlowLine(JsonObject params, GlowLineParams current) {
        GlowLineParams glowLine = current;
        
        if (params.has("glowLineCount")) glowLine = glowLine.withCount(params.get("glowLineCount").getAsFloat());
        if (params.has("glowLineIntensity")) glowLine = glowLine.withIntensity(params.get("glowLineIntensity").getAsFloat());
        if (params.has("rayPower")) glowLine = glowLine.withRayPower(params.get("rayPower").getAsFloat());
        if (params.has("raySharpness")) glowLine = glowLine.withRaySharpness(params.get("raySharpness").getAsFloat());
        
        return glowLine;
    }
    
    /**
     * Load CoronaParams from JSON params object.
     */
    public static CoronaParams loadCorona(JsonObject params, CoronaParams current) {
        CoronaParams corona = current;
        
        if (params.has("coronaWidth")) corona = corona.withWidth(params.get("coronaWidth").getAsFloat());
        if (params.has("coronaPower")) corona = corona.withPower(params.get("coronaPower").getAsFloat());
        if (params.has("coronaMultiplier")) corona = corona.withMultiplier(params.get("coronaMultiplier").getAsFloat());
        if (params.has("ringPower")) corona = corona.withRingPower(params.get("ringPower").getAsFloat());
        
        return corona;
    }
    
    /**
     * Load LightingParams from JSON params object.
     */
    public static LightingParams loadLighting(JsonObject params, LightingParams current) {
        LightingParams lighting = current;
        
        if (params.has("lightAmbient")) lighting = lighting.withAmbientStrength(params.get("lightAmbient").getAsFloat());
        if (params.has("lightDiffuse")) lighting = lighting.withDiffuseStrength(params.get("lightDiffuse").getAsFloat());
        if (params.has("lightBackLight")) lighting = lighting.withBackLightStrength(params.get("lightBackLight").getAsFloat());
        if (params.has("lightFresnel")) lighting = lighting.withFresnelStrength(params.get("lightFresnel").getAsFloat());
        
        return lighting;
    }
    
    /**
     * Load TransformParams from JSON params object.
     */
    public static TransformParams loadTransform(JsonObject params, TransformParams current) {
        TransformParams transform = current;
        
        if (params.has("transScale")) transform = transform.withScale(params.get("transScale").getAsFloat());
        // Standard rotation keys
        if (params.has("rotationX")) transform = transform.withRotationX(params.get("rotationX").getAsFloat());
        if (params.has("rotationY")) transform = transform.withRotationY(params.get("rotationY").getAsFloat());
        // Geodesic-style keys (animMode, rotationSpeed)
        if (params.has("transRotationX")) transform = transform.withRotationX(params.get("transRotationX").getAsFloat());
        if (params.has("transRotationY")) transform = transform.withRotationY(params.get("transRotationY").getAsFloat());
        
        return transform;
    }
    
    /**
     * Load FlamesParams from JSON params object.
     */
    public static FlamesParams loadFlames(JsonObject params, FlamesParams current) {
        FlamesParams flames = current;
        
        if (params.has("flamesEdge")) flames = flames.withEdge(params.get("flamesEdge").getAsFloat());
        if (params.has("flamesPower")) flames = flames.withPower(params.get("flamesPower").getAsFloat());
        if (params.has("flamesMult")) flames = flames.withMultiplier(params.get("flamesMult").getAsFloat());
        if (params.has("flamesTimeScale")) flames = flames.withTimeScale(params.get("flamesTimeScale").getAsFloat());
        if (params.has("flamesInsideFalloff")) flames = flames.withInsideFalloff(params.get("flamesInsideFalloff").getAsFloat());
        if (params.has("surfaceNoiseScale")) flames = flames.withSurfaceNoiseScale(params.get("surfaceNoiseScale").getAsFloat());
        
        return flames;
    }
    
    /**
     * Load ReservedParams from JSON params object.
     */
    public static ReservedParams loadReserved(JsonObject params, ReservedParams current) {
        ReservedParams reserved = current;
        
        if (params.has("showExternalRays")) reserved = reserved.withShowExternalRays(params.get("showExternalRays").getAsBoolean());
        if (params.has("showCorona")) reserved = reserved.withShowCorona(params.get("showCorona").getAsBoolean());
        if (params.has("eruptionContrast")) reserved = reserved.withEruptionContrast(params.get("eruptionContrast").getAsFloat());
        
        return reserved;
    }
    
    /**
     * Load GeometryParams from JSON params object.
     * Used for Geodesic sphere parameters.
     */
    public static GeometryParams loadGeometry(JsonObject params, GeometryParams current) {
        GeometryParams geo = current;
        
        if (params.has("geoSubdivisions")) geo = geo.withSubdivisions(params.get("geoSubdivisions").getAsFloat());
        if (params.has("geoRoundTop")) geo = geo.withRoundTop(params.get("geoRoundTop").getAsFloat());
        if (params.has("geoRoundCorner")) geo = geo.withRoundCorner(params.get("geoRoundCorner").getAsFloat());
        if (params.has("geoThickness")) geo = geo.withThickness(params.get("geoThickness").getAsFloat());
        
        return geo;
    }
    
    /**
     * Load GeometryParams2 from JSON params object.
     * Used for Geodesic sphere parameters (gap, height, wave).
     */
    public static GeometryParams2 loadGeometry2(JsonObject params, GeometryParams2 current) {
        GeometryParams2 geo = current;
        
        if (params.has("geoGap")) geo = geo.withGap(params.get("geoGap").getAsFloat());
        if (params.has("geoHeight")) geo = geo.withHeight(params.get("geoHeight").getAsFloat());
        if (params.has("geoWaveResolution")) geo = geo.withWaveResolution(params.get("geoWaveResolution").getAsFloat());
        if (params.has("geoWaveAmplitude")) geo = geo.withWaveAmplitude(params.get("geoWaveAmplitude").getAsFloat());
        
        return geo;
    }
    
    /**
     * Load GeoAnimParams from JSON params object.
     * Used for Geodesic animation mode, rotation, dome clip.
     */
    public static GeoAnimParams loadGeoAnim(JsonObject params, GeoAnimParams current) {
        GeoAnimParams anim = current;
        
        if (params.has("geoAnimMode")) anim = anim.withAnimMode(params.get("geoAnimMode").getAsFloat());
        if (params.has("geoRotationSpeed")) anim = anim.withRotationSpeed(params.get("geoRotationSpeed").getAsFloat());
        if (params.has("geoDomeClip")) anim = anim.withDomeClip(params.get("geoDomeClip").getAsFloat());
        
        return anim;
    }
    
    /**
     * Load DistortionParams from JSON params object.
     * Used for Proximity Darken effect.
     */
    public static DistortionParams loadDistortion(JsonObject params, DistortionParams current) {
        DistortionParams distortion = current;
        
        if (params.has("distortionStrength")) distortion = distortion.withStrength(params.get("distortionStrength").getAsFloat());
        if (params.has("distortionRadius")) distortion = distortion.withRadius(params.get("distortionRadius").getAsFloat());
        if (params.has("distortionFrequency")) distortion = distortion.withFrequency(params.get("distortionFrequency").getAsFloat());
        if (params.has("distortionSpeed")) distortion = distortion.withSpeed(params.get("distortionSpeed").getAsFloat());
        
        return distortion;
    }
    
    /**
     * Load ScreenEffects from JSON params object.
     * Used for blackout/vignette effects.
     */
    public static ScreenEffects loadScreenEffects(JsonObject params, ScreenEffects current) {
        ScreenEffects screenEffects = current;
        
        if (params.has("blackout")) screenEffects = screenEffects.withBlackout(params.get("blackout").getAsFloat());
        if (params.has("vignetteAmount")) screenEffects = screenEffects.withVignetteAmount(params.get("vignetteAmount").getAsFloat());
        if (params.has("vignetteRadius")) screenEffects = screenEffects.withVignetteRadius(params.get("vignetteRadius").getAsFloat());
        if (params.has("tintAmount")) screenEffects = screenEffects.withTintAmount(params.get("tintAmount").getAsFloat());
        
        return screenEffects;
    }
    
    /**
     * Load V2CoronaDetail from JSON params object.
     */
    public static V2CoronaDetail loadV2Corona(JsonObject params, V2CoronaDetail current) {
        V2CoronaDetail v2 = current;
        
        if (params.has("v2CoronaStart")) v2 = v2.withCoronaStart(params.get("v2CoronaStart").getAsFloat());
        if (params.has("v2CoronaBrightness")) v2 = v2.withCoronaBrightness(params.get("v2CoronaBrightness").getAsFloat());
        if (params.has("v2CoreRadiusScale")) v2 = v2.withCoreRadiusScale(params.get("v2CoreRadiusScale").getAsFloat());
        if (params.has("v2CoreMaskRadius")) v2 = v2.withCoreMaskRadius(params.get("v2CoreMaskRadius").getAsFloat());
        
        return v2;
    }
    
    /**
     * Load V2CoreDetail from JSON params object.
     */
    public static V2CoreDetail loadV2Core(JsonObject params, V2CoreDetail current) {
        V2CoreDetail v2 = current;
        
        if (params.has("v2CoreSpread")) v2 = v2.withCoreSpread(params.get("v2CoreSpread").getAsFloat());
        if (params.has("v2CoreGlow")) v2 = v2.withCoreGlow(params.get("v2CoreGlow").getAsFloat());
        if (params.has("v2CoreMaskSoft")) v2 = v2.withCoreMaskSoft(params.get("v2CoreMaskSoft").getAsFloat());
        if (params.has("v2EdgeRadius")) v2 = v2.withEdgeRadius(params.get("v2EdgeRadius").getAsFloat());
        
        return v2;
    }
    
    /**
     * Load V2EdgeDetail from JSON params object.
     */
    public static V2EdgeDetail loadV2Edge(JsonObject params, V2EdgeDetail current) {
        V2EdgeDetail v2 = current;
        
        if (params.has("v2EdgeSpread")) v2 = v2.withEdgeSpread(params.get("v2EdgeSpread").getAsFloat());
        if (params.has("v2EdgeGlow")) v2 = v2.withEdgeGlow(params.get("v2EdgeGlow").getAsFloat());
        if (params.has("v2SharpScale")) v2 = v2.withSharpScale(params.get("v2SharpScale").getAsFloat());
        if (params.has("v2LinesUVScale")) v2 = v2.withLinesUVScale(params.get("v2LinesUVScale").getAsFloat());
        
        return v2;
    }
    
    /**
     * Load V2LinesDetail from JSON params object.
     */
    public static V2LinesDetail loadV2Lines(JsonObject params, V2LinesDetail current) {
        V2LinesDetail v2 = current;
        
        if (params.has("v2LinesDensityMult")) v2 = v2.withLinesDensityMult(params.get("v2LinesDensityMult").getAsFloat());
        if (params.has("v2LinesContrast1")) v2 = v2.withLinesContrast1(params.get("v2LinesContrast1").getAsFloat());
        if (params.has("v2LinesContrast2")) v2 = v2.withLinesContrast2(params.get("v2LinesContrast2").getAsFloat());
        if (params.has("v2LinesMaskRadius")) v2 = v2.withLinesMaskRadius(params.get("v2LinesMaskRadius").getAsFloat());
        
        return v2;
    }
    
    /**
     * Load V2AlphaDetail from JSON params object.
     */
    public static V2AlphaDetail loadV2Alpha(JsonObject params, V2AlphaDetail current) {
        V2AlphaDetail v2 = current;
        
        if (params.has("v2LinesMaskSoft")) v2 = v2.withLinesMaskSoft(params.get("v2LinesMaskSoft").getAsFloat());
        if (params.has("v2RayRotSpeed")) v2 = v2.withRayRotSpeed(params.get("v2RayRotSpeed").getAsFloat());
        if (params.has("v2RayStartRadius")) v2 = v2.withRayStartRadius(params.get("v2RayStartRadius").getAsFloat());
        if (params.has("v2AlphaScale")) v2 = v2.withAlphaScale(params.get("v2AlphaScale").getAsFloat());
        
        return v2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SERIALIZATION (toJson)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Save ColorParams to JSON colors object.
     */
    public static JsonObject saveColors(ColorParams colors) {
        var json = new JsonObject();
        
        var primary = new JsonArray();
        primary.add(colors.primaryR());
        primary.add(colors.primaryG());
        primary.add(colors.primaryB());
        json.add("primary", primary);
        
        var secondary = new JsonArray();
        secondary.add(colors.secondaryR());
        secondary.add(colors.secondaryG());
        secondary.add(colors.secondaryB());
        json.add("secondary", secondary);
        
        var tertiary = new JsonArray();
        tertiary.add(colors.tertiaryR());
        tertiary.add(colors.tertiaryG());
        tertiary.add(colors.tertiaryB());
        json.add("tertiary", tertiary);
        
        var highlight = new JsonArray();
        highlight.add(colors.highlightR());
        highlight.add(colors.highlightG());
        highlight.add(colors.highlightB());
        json.add("highlight", highlight);
        
        var ray = new JsonArray();
        ray.add(colors.rayR());
        ray.add(colors.rayG());
        ray.add(colors.rayB());
        json.add("ray", ray);
        
        return json;
    }
    
    /**
     * Save all params to JSON params object.
     */
    public static JsonObject saveParams(
            AnimParams anim,
            AnimTimingParams animTiming,
            CoreEdgeParams coreEdge,
            FalloffParams falloff,
            NoiseConfigParams noiseConfig,
            NoiseDetailParams noiseDetail,
            GlowLineParams glowLine,
            CoronaParams corona,
            LightingParams lighting,
            TransformParams transform,
            FlamesParams flames,
            ReservedParams reserved,
            V2CoronaDetail v2Corona,
            V2CoreDetail v2Core,
            V2EdgeDetail v2Edge,
            V2LinesDetail v2Lines,
            V2AlphaDetail v2Alpha,
            GeometryParams geometry,
            GeometryParams2 geometry2,
            GeoAnimParams geoAnim
    ) {
        var params = new JsonObject();
        
        // Core/Edge
        params.addProperty("coreSize", coreEdge.coreSize());
        params.addProperty("edgeSharpness", coreEdge.edgeSharpness());
        params.addProperty("coreFalloff", coreEdge.coreFalloff());
        
        // Falloff
        params.addProperty("fadePower", falloff.fadePower());
        params.addProperty("fadeScale", falloff.fadeScale());
        
        // Noise config
        params.addProperty("noiseResLow", noiseConfig.resLow());
        params.addProperty("noiseResHigh", noiseConfig.resHigh());
        params.addProperty("noiseAmplitude", noiseConfig.amplitude());
        params.addProperty("noiseSeed", noiseConfig.seed());
        
        // Noise detail
        params.addProperty("noiseBaseScale", noiseDetail.baseScale());
        params.addProperty("noiseScaleMultiplier", noiseDetail.scaleMultiplier());
        params.addProperty("noiseOctaves", noiseDetail.octaves());
        
        // Glow/Lines/Rays
        params.addProperty("glowLineCount", glowLine.count());
        params.addProperty("glowLineIntensity", glowLine.intensity());
        params.addProperty("rayPower", glowLine.rayPower());
        params.addProperty("raySharpness", glowLine.raySharpness());
        
        // Corona
        params.addProperty("coronaWidth", corona.width());
        params.addProperty("coronaPower", corona.power());
        params.addProperty("coronaMultiplier", corona.multiplier());
        params.addProperty("ringPower", corona.ringPower());
        
        // Animation speeds
        params.addProperty("speedLow", anim.speedLow());
        params.addProperty("speedHigh", anim.speedHigh());
        params.addProperty("speedRay", anim.speedRay());
        params.addProperty("speedRing", anim.speedRing());
        
        // Animation timing
        params.addProperty("radialSpeed1", animTiming.radialSpeed1());
        params.addProperty("radialSpeed2", animTiming.radialSpeed2());
        params.addProperty("axialSpeed", animTiming.axialSpeed());
        
        // Lighting
        params.addProperty("lightAmbient", lighting.ambientStrength());
        params.addProperty("lightDiffuse", lighting.diffuseStrength());
        
        // Transform (including animation mode and rotation speed for Geodesic)
        params.addProperty("transRotationX", transform.rotationX());
        params.addProperty("transRotationY", transform.rotationY());
        params.addProperty("transScale", transform.scale());
        
        // Reserved/Flags
        params.addProperty("showExternalRays", reserved.showExternalRays());
        params.addProperty("showCorona", reserved.showCorona());
        params.addProperty("eruptionContrast", reserved.eruptionContrast());
        
        // Flames (Pulsar V5)
        params.addProperty("flamesEdge", flames.edge());
        params.addProperty("flamesPower", flames.power());
        params.addProperty("flamesMult", flames.multiplier());
        params.addProperty("flamesTimeScale", flames.timeScale());
        params.addProperty("flamesInsideFalloff", flames.insideFalloff());
        params.addProperty("surfaceNoiseScale", flames.surfaceNoiseScale());
        
        // V2 Detail params
        params.addProperty("v2CoronaStart", v2Corona.coronaStart());
        params.addProperty("v2CoronaBrightness", v2Corona.coronaBrightness());
        params.addProperty("v2CoreMaskRadius", v2Corona.coreMaskRadius());
        params.addProperty("v2CoreSpread", v2Core.coreSpread());
        params.addProperty("v2CoreGlow", v2Core.coreGlow());
        params.addProperty("v2CoreMaskSoft", v2Core.coreMaskSoft());
        params.addProperty("v2EdgeRadius", v2Core.edgeRadius());
        params.addProperty("v2EdgeSpread", v2Edge.edgeSpread());
        params.addProperty("v2EdgeGlow", v2Edge.edgeGlow());
        params.addProperty("v2SharpScale", v2Edge.sharpScale());
        params.addProperty("v2LinesUVScale", v2Edge.linesUVScale());
        params.addProperty("v2LinesDensityMult", v2Lines.linesDensityMult());
        params.addProperty("v2LinesContrast1", v2Lines.linesContrast1());
        params.addProperty("v2LinesContrast2", v2Lines.linesContrast2());
        params.addProperty("v2LinesMaskRadius", v2Lines.linesMaskRadius());
        params.addProperty("v2LinesMaskSoft", v2Alpha.linesMaskSoft());
        params.addProperty("v2RayRotSpeed", v2Alpha.rayRotSpeed());
        params.addProperty("v2RayStartRadius", v2Alpha.rayStartRadius());
        params.addProperty("v2AlphaScale", v2Alpha.alphaScale());
        
        // Geodesic params
        saveGeometry(params, geometry, geometry2);
        saveGeoAnim(params, geoAnim);
        
        return params;
    }
    
    /**
     * Save geometry params to JSON params object.
     * Only saves non-default values to keep profiles clean.
     */
    public static void saveGeometry(JsonObject params, GeometryParams geo, GeometryParams2 geo2) {
        // GeometryParams
        if (geo.subdivisions() != GeometryParams.DEFAULT.subdivisions()) {
            params.addProperty("geoSubdivisions", geo.subdivisions());
        }
        if (geo.roundTop() != GeometryParams.DEFAULT.roundTop()) {
            params.addProperty("geoRoundTop", geo.roundTop());
        }
        if (geo.roundCorner() != GeometryParams.DEFAULT.roundCorner()) {
            params.addProperty("geoRoundCorner", geo.roundCorner());
        }
        if (geo.thickness() != GeometryParams.DEFAULT.thickness()) {
            params.addProperty("geoThickness", geo.thickness());
        }
        
        // GeometryParams2
        if (geo2.gap() != GeometryParams2.DEFAULT.gap()) {
            params.addProperty("geoGap", geo2.gap());
        }
        if (geo2.height() != GeometryParams2.DEFAULT.height()) {
            params.addProperty("geoHeight", geo2.height());
        }
        if (geo2.waveResolution() != GeometryParams2.DEFAULT.waveResolution()) {
            params.addProperty("geoWaveResolution", geo2.waveResolution());
        }
        if (geo2.waveAmplitude() != GeometryParams2.DEFAULT.waveAmplitude()) {
            params.addProperty("geoWaveAmplitude", geo2.waveAmplitude());
        }
    }
    
    /**
     * Save transform params to JSON params object.
     * Used for animation mode, rotation speed, dome clip.
     */
    public static void saveTransform(JsonObject params, TransformParams transform) {
        if (transform.rotationX() != TransformParams.DEFAULT.rotationX()) {
            params.addProperty("transRotationX", transform.rotationX());
        }
        if (transform.rotationY() != TransformParams.DEFAULT.rotationY()) {
            params.addProperty("transRotationY", transform.rotationY());
        }
        if (transform.scale() != TransformParams.DEFAULT.scale()) {
            params.addProperty("transScale", transform.scale());
        }
    }
    
    /**
     * Save geo animation params to JSON params object.
     * Used for Geodesic animation mode, rotation, dome clip.
     */
    public static void saveGeoAnim(JsonObject params, GeoAnimParams geoAnim) {
        if (geoAnim.animMode() != GeoAnimParams.DEFAULT.animMode()) {
            params.addProperty("geoAnimMode", geoAnim.animMode());
        }
        if (geoAnim.rotationSpeed() != GeoAnimParams.DEFAULT.rotationSpeed()) {
            params.addProperty("geoRotationSpeed", geoAnim.rotationSpeed());
        }
        if (geoAnim.domeClip() != GeoAnimParams.DEFAULT.domeClip()) {
            params.addProperty("geoDomeClip", geoAnim.domeClip());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int rgbToArgb(float r, float g, float b) {
        int ir = (int)(clamp(r) * 255);
        int ig = (int)(clamp(g) * 255);
        int ib = (int)(clamp(b) * 255);
        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }
    
    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
