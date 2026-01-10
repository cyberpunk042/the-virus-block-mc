package net.cyberpunk042.client.gui.schema;

import net.cyberpunk042.client.visual.effect.EffectType;

import java.util.*;

import static net.cyberpunk042.client.gui.schema.ParameterSpec.*;

/**
 * Centralized registry of effect schemas.
 * 
 * <p>Provides lookup by effect type and version, returning the appropriate
 * schema that defines which parameters are visible and how they are labeled.</p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * EffectSchema schema = EffectSchemaRegistry.get(EffectType.ENERGY_ORB, 2);
 * // Returns the Energy Orb V2 schema with its specific parameter configuration
 * }</pre>
 */
public final class EffectSchemaRegistry {
    
    private static final Map<String, EffectSchema> SCHEMAS = new LinkedHashMap<>();
    
    // Initialize all schemas on class load
    static {
        registerEnergyOrbV1();
        registerEnergyOrbV2();
        registerEnergyOrbV3();
        registerPulsar();
        registerElectricAura();  // V8
    }
    
    private EffectSchemaRegistry() {} // Static registry only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOOKUP METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets the schema for the given effect type and version.
     * 
     * @param effectType The effect type
     * @param version    The version number (1, 2, 3, etc.)
     * @return The schema, or null if not found
     */
    public static EffectSchema get(EffectType effectType, int version) {
        return SCHEMAS.get(EffectSchema.key(effectType, version));
    }
    
    /**
     * Gets the schema by key (e.g., "ENERGY_ORB_V2").
     */
    public static EffectSchema getByKey(String key) {
        return SCHEMAS.get(key);
    }
    
    /**
     * Returns all registered schemas.
     */
    public static Collection<EffectSchema> all() {
        return Collections.unmodifiableCollection(SCHEMAS.values());
    }
    
    /**
     * Returns all schemas for a specific effect type.
     */
    public static List<EffectSchema> forEffectType(EffectType effectType) {
        return SCHEMAS.values().stream()
            .filter(s -> s.effectType() == effectType)
            .toList();
    }
    
    /**
     * Returns available version numbers for an effect type.
     */
    public static List<Integer> versionsFor(EffectType effectType) {
        return SCHEMAS.values().stream()
            .filter(s -> s.effectType() == effectType)
            .map(EffectSchema::version)
            .sorted()
            .toList();
    }
    
    /**
     * Registers a custom schema (for runtime additions).
     */
    public static void register(EffectSchema schema) {
        SCHEMAS.put(schema.key(), schema);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCHEMA DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final String FV = "fieldVisual."; // Path prefix
    
    // ─────────────────────────────────────────────────────────────────────────
    // ENERGY ORB V1 - Basic raymarched rendering
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerEnergyOrbV1() {
        SCHEMAS.put("ENERGY_ORB_V1", EffectSchema.builder("Energy Orb V1", EffectType.ENERGY_ORB, 1)
            .group("Core", List.of(
                slider(FV + "coreSize", "Core Size", 0.01f, 3f, 0.15f, "Core"),
                slider(FV + "edgeSharpness", "Edge Sharpness", 0.5f, 20f, 4f, "Core"),
                slider(FV + "coronaWidth", "Corona Width", 0f, 3f, 0.5f, "Core")
            ))
            .group("Colors", List.of(
                normalized(FV + "primaryR", "Core R", 1f, "Colors"),
                normalized(FV + "primaryG", "Core G", 1f, "Colors"),
                normalized(FV + "primaryB", "Core B", 1f, "Colors"),
                normalized(FV + "secondaryR", "Edge R", 0f, "Colors"),
                normalized(FV + "secondaryG", "Edge G", 1f, "Colors"),
                normalized(FV + "secondaryB", "Edge B", 1f, "Colors"),
                normalized(FV + "tertiaryR", "Outer R", 0.1f, "Colors"),
                normalized(FV + "tertiaryG", "Outer G", 0.02f, "Colors"),
                normalized(FV + "tertiaryB", "Outer B", 0.16f, "Colors")
            ))
            .group("Pattern", List.of(
                slider(FV + "spiralDensity", "Spiral Density", 1f, 50f, 5f, "Pattern"),
                slider(FV + "spiralTwist", "Spiral Twist", 1f, 50f, 5f, "Pattern"),
                slider(FV + "glowLineCount", "Glow Lines", 4f, 64f, 16f, "Pattern"),
                slider(FV + "glowLineIntensity", "Line Intensity", 0f, 5f, 0.8f, "Pattern")
            ))
            .build());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // ENERGY ORB V2 - Shadertoy-style with corona/rays + full detail control
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerEnergyOrbV2() {
        SCHEMAS.put("ENERGY_ORB_V2", EffectSchema.builder("Energy Orb V2", EffectType.ENERGY_ORB, 2)
            .group("Core", List.of(
                slider(FV + "coreSize", "Core Size", 0.01f, 3f, 0.5f, "Core"),
                slider(FV + "v2CoreSpread", "Core Spread", 0.1f, 10f, 1f, "Core"),
                slider(FV + "v2CoreGlow", "Core Glow", 0f, 5f, 1f, "Core"),
                slider(FV + "v2CoreMaskRadius", "Core Mask", 0.1f, 1f, 0.35f, "Core"),
                slider(FV + "v2CoreMaskSoft", "Mask Soft", 0.001f, 0.5f, 0.05f, "Core")
            ))
            .group("Corona", List.of(
                toggle(FV + "showCorona", "Show Corona", true, "Corona"),
                slider(FV + "coronaWidth", "Width", 0.01f, 3f, 0.5f, "Corona"),
                slider(FV + "coronaPower", "Falloff", 0.1f, 10f, 2f, "Corona"),
                slider(FV + "v2CoronaStart", "Start Dist", 0f, 1f, 0.15f, "Corona"),
                slider(FV + "v2CoronaBrightness", "Brightness", 0f, 3f, 1f, "Corona")
            ))
            .group("Edge Ring", List.of(
                slider(FV + "edgeSharpness", "Sharpness", 0.5f, 20f, 4f, "Edge Ring"),
                slider(FV + "ringPower", "Ring Power", 0f, 20f, 1f, "Edge Ring"),
                slider(FV + "v2EdgeRadius", "Ring Radius", 0.05f, 1f, 0.3f, "Edge Ring"),
                slider(FV + "v2EdgeSpread", "Ring Spread", 0.1f, 5f, 1f, "Edge Ring"),
                slider(FV + "v2EdgeGlow", "Ring Glow", 0f, 5f, 1f, "Edge Ring"),
                slider(FV + "v2SharpScale", "Sharp Scale", 0.5f, 20f, 4f, "Edge Ring")
            ))
            .group("Inner Lines", List.of(
                slider(FV + "glowLineIntensity", "Lines Opacity", 0f, 3f, 1f, "Inner Lines"),
                slider(FV + "spiralDensity", "Voronoi Density", 1f, 50f, 5f, "Inner Lines"),
                slider(FV + "spiralTwist", "Twist Amount", 0f, 20f, 5f, "Inner Lines"),
                slider(FV + "v2LinesUVScale", "UV Scale", 0.5f, 10f, 3f, "Inner Lines"),
                slider(FV + "v2LinesDensityMult", "Layer2 Mult", 0.5f, 5f, 1.6f, "Inner Lines"),
                slider(FV + "v2LinesContrast1", "Contrast 1", 0.5f, 10f, 2.5f, "Inner Lines"),
                slider(FV + "v2LinesContrast2", "Contrast 2", 0.5f, 10f, 3f, "Inner Lines"),
                slider(FV + "v2LinesMaskRadius", "Mask Radius", 0.1f, 1f, 0.3f, "Inner Lines"),
                slider(FV + "v2LinesMaskSoft", "Mask Soft", 0.001f, 0.2f, 0.02f, "Inner Lines")
            ))
            .group("External Rays", List.of(
                toggle(FV + "showExternalRays", "Show Rays", true, "External Rays"),
                slider(FV + "rayPower", "Ray Power", 0f, 20f, 2f, "External Rays"),
                slider(FV + "raySharpness", "Ray Sharp", 0.01f, 20f, 1f, "External Rays"),
                slider(FV + "glowLineCount", "Ray Count", 4f, 64f, 16f, "External Rays"),
                slider(FV + "v2RayRotSpeed", "Ray Rotation", 0f, 2f, 0.3f, "External Rays"),
                slider(FV + "v2RayStartRadius", "Ray Start", 0.1f, 1f, 0.32f, "External Rays")
            ))
            .group("Output", List.of(
                slider(FV + "v2AlphaScale", "Alpha", 0f, 1f, 1f, "Output")
            ))
            .group("Colors", List.of(
                normalized(FV + "primaryR", "Core R", 1f, "Colors"),
                normalized(FV + "primaryG", "Core G", 1f, "Colors"),
                normalized(FV + "primaryB", "Core B", 1f, "Colors"),
                normalized(FV + "secondaryR", "Edge R", 0f, "Colors"),
                normalized(FV + "secondaryG", "Edge G", 1f, "Colors"),
                normalized(FV + "secondaryB", "Edge B", 1f, "Colors"),
                normalized(FV + "tertiaryR", "Lines R", 0.1f, "Colors"),
                normalized(FV + "tertiaryG", "Lines G", 0.4f, "Colors"),
                normalized(FV + "tertiaryB", "Lines B", 0.8f, "Colors")
            ))
            .build());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // ENERGY ORB V3 - Raymarched 3D Sphere (V2 visual + V7 architecture)
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerEnergyOrbV3() {
        SCHEMAS.put("ENERGY_ORB_V3", EffectSchema.builder("Energy Orb 3D", EffectType.ENERGY_ORB, 3)
            .group("Core", List.of(
                slider(FV + "coreSize", "Core Size", 0.01f, 3f, 0.5f, "Core"),
                slider(FV + "v2CoreSpread", "Core Spread", 0.1f, 10f, 1f, "Core"),
                slider(FV + "v2CoreGlow", "Core Glow", 0f, 5f, 1f, "Core"),
                slider(FV + "v2CoreRadiusScale", "Radius Scale", 0.1f, 2f, 0.35f, "Core"),
                slider(FV + "v2CoreMaskRadius", "Core Mask", 0.1f, 1f, 0.35f, "Core"),
                slider(FV + "v2CoreMaskSoft", "Mask Soft", 0.001f, 0.5f, 0.05f, "Core")
            ))
            .group("Corona", List.of(
                toggle(FV + "showCorona", "Show Corona", true, "Corona"),
                slider(FV + "coronaWidth", "Width", 0.01f, 3f, 0.5f, "Corona"),
                slider(FV + "coronaPower", "Falloff", 0.1f, 10f, 2f, "Corona"),
                slider(FV + "v2CoronaStart", "Start Dist", 0f, 1f, 0.15f, "Corona"),
                slider(FV + "v2CoronaBrightness", "Brightness", 0f, 3f, 1f, "Corona")
            ))
            .group("Edge Ring", List.of(
                slider(FV + "edgeSharpness", "Sharpness", 0.5f, 20f, 4f, "Edge Ring"),
                slider(FV + "ringPower", "Ring Power", 0f, 20f, 1f, "Edge Ring"),
                slider(FV + "v2EdgeRadius", "Ring Radius", 0.05f, 1f, 0.3f, "Edge Ring"),
                slider(FV + "v2EdgeSpread", "Ring Spread", 0.1f, 5f, 1f, "Edge Ring"),
                slider(FV + "v2EdgeGlow", "Ring Glow", 0f, 5f, 1f, "Edge Ring"),
                slider(FV + "v2SharpScale", "Sharp Scale", 0.5f, 20f, 4f, "Edge Ring")
            ))
            .group("Surface Pattern", List.of(
                slider(FV + "glowLineIntensity", "Pattern Opacity", 0f, 3f, 1f, "Surface Pattern"),
                slider(FV + "spiralDensity", "Voronoi Density", 1f, 50f, 5f, "Surface Pattern"),
                slider(FV + "spiralTwist", "Twist Amount", 0f, 20f, 5f, "Surface Pattern"),
                slider(FV + "v2LinesUVScale", "UV Scale", 0.5f, 10f, 3f, "Surface Pattern"),
                slider(FV + "v2LinesDensityMult", "Layer2 Mult", 0.5f, 5f, 1.6f, "Surface Pattern"),
                slider(FV + "v2LinesContrast1", "Contrast 1", 0.5f, 10f, 2.5f, "Surface Pattern"),
                slider(FV + "v2LinesContrast2", "Contrast 2", 0.5f, 10f, 3f, "Surface Pattern"),
                slider(FV + "v2LinesMaskRadius", "Mask Radius", 0.1f, 1f, 0.3f, "Surface Pattern"),
                slider(FV + "v2LinesMaskSoft", "Mask Soft", 0.001f, 0.2f, 0.02f, "Surface Pattern")
            ))
            .group("External Rays", List.of(
                toggle(FV + "showExternalRays", "Show Rays", true, "External Rays"),
                slider(FV + "rayPower", "Ray Power", 0f, 20f, 2f, "External Rays"),
                slider(FV + "raySharpness", "Ray Sharp", 0.01f, 20f, 1f, "External Rays"),
                slider(FV + "glowLineCount", "Ray Count", 4f, 64f, 16f, "External Rays"),
                slider(FV + "v2RayRotSpeed", "Ray Rotation", 0f, 2f, 0.3f, "External Rays"),
                slider(FV + "v2RayStartRadius", "Ray Start", 0.1f, 1f, 0.32f, "External Rays")
            ))
            .group("Output", List.of(
                slider(FV + "v2AlphaScale", "Alpha", 0f, 1f, 1f, "Output")
            ))
            .group("Colors", List.of(
                normalized(FV + "primaryR", "Core R", 1f, "Colors"),
                normalized(FV + "primaryG", "Core G", 1f, "Colors"),
                normalized(FV + "primaryB", "Core B", 1f, "Colors"),
                normalized(FV + "secondaryR", "Edge R", 0f, "Colors"),
                normalized(FV + "secondaryG", "Edge G", 1f, "Colors"),
                normalized(FV + "secondaryB", "Edge B", 1f, "Colors"),
                normalized(FV + "tertiaryR", "Pattern R", 0.1f, "Colors"),
                normalized(FV + "tertiaryG", "Pattern G", 0.4f, "Colors"),
                normalized(FV + "tertiaryB", "Pattern B", 0.8f, "Colors")
            ))
            .build());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // PULSAR (V5) - Star/sun effect with procedural noise corona
    // Based on Shadertoy trisomie21
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerPulsar() {
        SCHEMAS.put("ENERGY_ORB_V5", EffectSchema.builder("Pulsar", EffectType.ENERGY_ORB, 5)
            .group("Core", List.of(
                slider(FV + "coreSize", "Core Size", 0.01f, 1f, 0.24f, "Core", "Original: 0.24"),
                slider(FV + "lightAmbient", "Surface Brightness", 0f, 2f, 0.75f, "Core", "Original: 0.75"),
                slider(FV + "lightDiffuse", "Brightness Scale", 0f, 1f, 0.3f, "Core", "Original: 0.3"),
                slider(FV + "transScale", "Surface Scale", 0.1f, 5f, 2f, "Core", "Original: 2.0"),
                slider(FV + "surfaceNoiseScale", "Surface Noise", 1f, 20f, 5f, "Core", "Texture detail")
            ))
            .group("Fade", List.of(
                slider(FV + "fadePower", "Fade Power", 0f, 3f, 0.5f, "Fade", "Original: 0.5"),
                slider(FV + "fadeScale", "Fade Scale", 0.1f, 5f, 2f, "Fade", "Original: 2.0")
            ))
            .group("Noise", List.of(
                slider(FV + "noiseResLow", "Noise Res Low", 1f, 50f, 15f, "Noise", "Original: 15"),
                slider(FV + "noiseResHigh", "Noise Res High", 1f, 100f, 45f, "Noise", "Original: 45"),
                slider(FV + "noiseAmplitude", "Noise Amplitude", 0f, 3f, 0.5f, "Noise", "Original: 0.5"),
                intSlider(FV + "noiseOctaves", "Noise Octaves", 1, 10, 7, "Noise"),
                slider(FV + "noiseBaseScale", "Base Scale", 0.01f, 50f, 10f, "Noise", "Original: 10"),
                slider(FV + "noiseScaleMultiplier", "Scale Mult", 1f, 50f, 25f, "Noise", "Original: 25")
            ))
            .group("Animation", List.of(
                slider(FV + "speedLow", "Loop Speed", 0f, 1f, 0.2f, "Animation", "Original: 0.2"),
                slider(FV + "radialSpeed1", "Radial 1", 0f, 1f, 0.35f, "Animation", "Original: 0.35"),
                slider(FV + "radialSpeed2", "Radial 2", 0f, 1f, 0.15f, "Animation", "Original: 0.15"),
                slider(FV + "axialSpeed", "Axial", 0f, 0.2f, 0.015f, "Animation", "Original: 0.015")
            ))
            .group("Flames", List.of(
                // NEW flame params - animated noise effect
                slider(FV + "flamesEdge", "Flames Edge", 0f, 3f, 1.1f, "Flames", "Original: 1.1"),
                slider(FV + "flamesPower", "Flames Power", 0f, 5f, 2f, "Flames", "Original: 2.0"),
                slider(FV + "flamesMult", "Flames Bright", 0f, 200f, 50f, "Flames", "Original: 50"),
                slider(FV + "flamesTimeScale", "Flames Time", 0.5f, 2f, 1.2f, "Flames", "Original: 1.2"),
                slider(FV + "flamesInsideFalloff", "Inside Falloff", 1f, 50f, 24f, "Flames", "Original: 24"),
                slider(FV + "surfaceNoiseScale", "Surface Noise", 1f, 20f, 5f, "Flames", "Surface texture detail")
            ))
            .group("Corona", List.of(
                // Corona params - NEW smooth glow halo
                slider(FV + "coronaWidth", "Corona Width", 0f, 2f, 0.5f, "Corona", "Glow spread"),
                slider(FV + "coronaPower", "Corona Power", 0f, 5f, 2f, "Corona", "Falloff exponent"),
                slider(FV + "coronaMultiplier", "Corona Bright", 0f, 200f, 50f, "Corona", "Glow intensity")
            ))
            .group("Colors", List.of(
                normalized(FV + "primaryR", "Surface R", 1f, "Colors"),
                normalized(FV + "primaryG", "Surface G", 0.9f, "Colors"),
                normalized(FV + "primaryB", "Surface B", 0.7f, "Colors"),
                normalized(FV + "secondaryR", "Flames R", 1f, "Colors"),
                normalized(FV + "secondaryG", "Flames G", 0.5f, "Colors"),
                normalized(FV + "secondaryB", "Flames B", 0.2f, "Colors"),
                normalized(FV + "tertiaryR", "Glow R", 1f, "Colors"),
                normalized(FV + "tertiaryG", "Glow G", 0.3f, "Colors"),
                normalized(FV + "tertiaryB", "Glow B", 0.1f, "Colors"),
                normalized(FV + "highlightR", "Corona R", 1f, "Colors"),
                normalized(FV + "highlightG", "Corona G", 0.8f, "Colors"),
                normalized(FV + "highlightB", "Corona B", 0.5f, "Colors")
            ))
            .group("Output", List.of(
                slider(FV + "v2AlphaScale", "Alpha", 0f, 1f, 1f, "Output", "Output alpha multiplier")
            ))
            .build());
        
        // V6: Raymarched Pulsar - Same params as V5, rendered as true 3D geometry
        SCHEMAS.put("ENERGY_ORB_V6", EffectSchema.builder("Pulsar 3D", EffectType.ENERGY_ORB, 6)
            .group("Core", List.of(
                slider(FV + "coreSize", "Core Size", 0.01f, 1f, 0.5f, "Core", "Java default: 0.5"),
                slider(FV + "lightAmbient", "Surface Brightness", 0f, 2f, 0.8f, "Core", "Java default: 0.8"),
                slider(FV + "lightDiffuse", "Brightness Scale", 0f, 1f, 0.3f, "Core", "Original: 0.3")
            ))
            .group("Noise", List.of(
                slider(FV + "noiseResLow", "Noise Res Low", 1f, 50f, 15f, "Noise", "Original: 15"),
                slider(FV + "noiseResHigh", "Noise Res High", 1f, 100f, 45f, "Noise", "Original: 45"),
                slider(FV + "noiseAmplitude", "Noise Amplitude", 0f, 3f, 0.5f, "Noise", "Original: 0.5"),
                intSlider(FV + "noiseOctaves", "Noise Octaves", 1, 10, 7, "Noise"),
                slider(FV + "noiseBaseScale", "Base Scale", 0.01f, 50f, 0.1f, "Noise", "Original: 10"),
                steppedSlider(FV + "noiseScaleMultiplier", "Scale Mult", 1f, 50f, 0.25f, 1f, "Noise")
            ))
            .group("Animation", List.of(
                slider(FV + "speedLow", "Loop Speed", 0f, 1f, 0.2f, "Animation", "Original: 0.2"),
                slider(FV + "radialSpeed1", "Radial 1", 0f, 1f, 0.35f, "Animation", "Original: 0.35"),
                slider(FV + "radialSpeed2", "Radial 2", 0f, 1f, 0.15f, "Animation", "Original: 0.15"),
                slider(FV + "axialSpeed", "Axial", 0f, 0.2f, 0.015f, "Animation", "Original: 0.015")
            ))
            .group("Flames", List.of(
                slider(FV + "flamesEdge", "Flames Edge", 0f, 3f, 1.1f, "Flames", "Original: 1.1"),
                slider(FV + "flamesPower", "Flames Power", 0f, 5f, 2f, "Flames", "Original: 2.0"),
                slider(FV + "flamesMult", "Flames Bright", 0f, 200f, 50f, "Flames", "Original: 50")
            ))
            .group("Corona", List.of(
                slider(FV + "coronaWidth", "Corona Width", 0f, 2f, 0.5f, "Corona", "Glow spread"),
                slider(FV + "coronaPower", "Corona Power", 0f, 5f, 2f, "Corona", "Falloff exponent"),
                slider(FV + "coronaMultiplier", "Corona Bright", 0f, 200f, 50f, "Corona", "Glow intensity")
            ))
            .group("Rays", List.of(
                // showExternalRays = toggle (false by default)
                toggle(FV + "showExternalRays", "Enable Rays", false, "Rays"),
                slider(FV + "rayPower", "Ray Power", 0.5f, 5f, 2f, "Rays", "Java default: 2.0"),
                slider(FV + "raySharpness", "Ray Sharpness", 0.5f, 3f, 1f, "Rays", "Edge definition"),
                slider(FV + "speedRay", "Ray Speed", 0f, 10f, 5f, "Rays", "Java default: 5.0"),
                // Use rayR/rayG/rayB which map to colors.rayColor()
                normalized(FV + "rayR", "Ray R", 1f, "Rays"),
                normalized(FV + "rayG", "Ray G", 0.8f, "Rays"),
                normalized(FV + "rayB", "Ray B", 0.5f, "Rays")
            ))
            .group("Colors", List.of(
                normalized(FV + "primaryR", "Surface R", 1f, "Colors"),
                normalized(FV + "primaryG", "Surface G", 0.9f, "Colors"),
                normalized(FV + "primaryB", "Surface B", 0.7f, "Colors"),
                normalized(FV + "secondaryR", "Flames R", 1f, "Colors"),
                normalized(FV + "secondaryG", "Flames G", 0.5f, "Colors"),
                normalized(FV + "secondaryB", "Flames B", 0.2f, "Colors"),
                normalized(FV + "tertiaryR", "Glow R", 1f, "Colors"),
                normalized(FV + "tertiaryG", "Glow G", 0.3f, "Colors"),
                normalized(FV + "tertiaryB", "Glow B", 0.1f, "Colors"),
                normalized(FV + "highlightR", "Corona R", 1f, "Colors"),
                normalized(FV + "highlightG", "Corona G", 0.8f, "Colors"),
                normalized(FV + "highlightB", "Corona B", 0.5f, "Colors")
            ))
            .build());
        
        // V7: Panteleymonov Sun - Full Unity SunShader parametrization
        // Source: SunShader 1.0 for Unity3D by Panteleymonov Aleksandr (2015-2016)
        SCHEMAS.put("ENERGY_ORB_V7", EffectSchema.builder("Panteleymonov Sun", EffectType.ENERGY_ORB, 7)
            .group("Body", List.of(
                // _Zoom: Detail zoom level
                slider(FV + "noiseBaseScale", "Zoom", 0.1f, 10f, 1f, "Body", "_Zoom: Detail zoom level"),
                // _Detail: LOD levels 0-10 (int) - type default is 7
                intSlider(FV + "noiseOctaves", "Detail", 0, 10, 7, "Body")
            ))
            .group("Rays", List.of(
                // _RayString: Ray thickness/falloff width (controls ray reach)
                slider(FV + "raySharpness", "Ray Thickness", 0.01f, 20f, 1f, "Rays", "_RayString: Higher = longer rays"),
                // Ray extent multiplier
                slider(FV + "fadeScale", "Ray Reach", 1f, 1000f, 50f, "Rays", "Java default: 2.0"),
                // Ray edge fade curve - type default is 24.0
                slider(FV + "insideFalloffPower", "Ray Fade", 0.1f, 100f, 24f, "Rays", "Edge transition: lower=smoother, higher=sharper"),
                // _Rays: Ray intensity exponent
                slider(FV + "rayPower", "Rays Power", 0.1f, 20f, 2f, "Rays", "_Rays: Ray intensity exponent"),
                // _RayRing: Ring noise power
                slider(FV + "coronaPower", "Ring Power", 0.1f, 20f, 2f, "Rays", "Java default: 2.0"),
                // _RayGlow: Ray glow power - maps to coronaMultiplier scaled by 0.02
                slider(FV + "coronaMultiplier", "Ray Glow", 1f, 500f, 50f, "Rays", "Java default: 50.0"),
                // _Glow: Overall glow falloff
                slider(FV + "coreFalloff", "Glow Falloff", 0.1f, 100f, 4f, "Rays", "_Glow: Overall glow falloff"),
                // Eruption Contrast: Controls ray discreteness (higher = more discrete eruptions with gaps)
                slider(FV + "eruptionContrast", "Eruption Contrast", 0.1f, 20f, 2f, "Rays", "Higher = discrete eruptions, lower = continuous radiation")
            ))
            .group("Animation", List.of(
                // _SpeedHi: High frequency noise animation
                slider(FV + "speedHigh", "Speed High", 0f, 50f, 2f, "Animation", "_SpeedHi: High freq noise"),
                // _SpeedLow: Low frequency noise animation
                slider(FV + "speedLow", "Speed Low", 0f, 50f, 2f, "Animation", "_SpeedLow: Low freq noise"),
                // _SpeedRay: Ray animation speed
                slider(FV + "speedRay", "Speed Ray", 0f, 50f, 5f, "Animation", "_SpeedRay: Ray animation"),
                // _SpeedRing: Ring rotation speed
                slider(FV + "speedRing", "Speed Ring", 0f, 50f, 2f, "Animation", "_SpeedRing: Ring rotation")
            ))
            .group("Core", List.of(
                // Core size multiplier
                slider(FV + "coreSize", "Core Scale", 0.01f, 10f, 0.5f, "Core", "Java default: 0.5")
            ))
            // Dark Ring: REMOVED - legacy feature not visually useful
            .group("Fade", List.of(
                // Fade threshold for Replace blend mode (removes dark glow behind rays)
                slider(FV + "fadePower", "Fade Threshold", 0f, 100f, 0.5f, "Fade", "Java default: 0.5")
            ))
            .group("Proximity Darken", List.of(
                // Screen darkening as you approach the sun (like shockwave blackout)
                toggle(FV + "distortionStrength", "Enable", false, "Proximity Darken", "Toggle proximity darkening on/off"),
                slider(FV + "blackout", "Intensity", 0f, 1f, 1f, "Proximity Darken", "How dark at center (0-1)"),
                slider(FV + "distortionRadius", "Range", 10f, 10000f, 1000f, "Proximity Darken", "Distance in blocks where effect fades to 0"),
                slider(FV + "distortionFrequency", "Fade", 0.01f, 5f, 0.1f, "Proximity Darken", "0.1=fast fade, 1=linear, 2=slow fade")
            ))
            .group("Noise", List.of(
                // _Seed: Pattern variation
                slider(FV + "noiseSeed", "Seed", -10f, 10f, 1f, "Noise", "_Seed: Pattern variation (avoid 0)")
            ))
            .group("Colors", List.of(
                // _Light: Highlight white (1,1,1)
                normalized(FV + "highlightR", "Light R", 1f, "Colors"),
                normalized(FV + "highlightG", "Light G", 1f, "Colors"),
                normalized(FV + "highlightB", "Light B", 1f, "Colors"),
                // _Color: Yellow body (1,1,0)
                normalized(FV + "primaryR", "Body R", 1f, "Colors"),
                normalized(FV + "primaryG", "Body G", 1f, "Colors"),
                normalized(FV + "primaryB", "Body B", 0f, "Colors"),
                // _Base: Red accent (1,0,0)
                normalized(FV + "secondaryR", "Base R", 1f, "Colors"),
                normalized(FV + "secondaryG", "Base G", 0f, "Colors"),
                normalized(FV + "secondaryB", "Base B", 0f, "Colors"),
                // _Dark: Magenta accent (1,0,1)
                normalized(FV + "tertiaryR", "Dark R", 1f, "Colors"),
                normalized(FV + "tertiaryG", "Dark G", 0f, "Colors"),
                normalized(FV + "tertiaryB", "Dark B", 1f, "Colors"),
                // _Ray: Orange ray base (1,0.6,0.1)
                normalized(FV + "rayR", "Ray R", 1f, "Colors"),
                normalized(FV + "rayG", "Ray G", 0.6f, "Colors"),
                normalized(FV + "rayB", "Ray B", 0.1f, "Colors")
                // Note: _RayLight is derived from mix(RayColor, HighlightColor, 0.7) in shader
            ))
            .build());
        
        // ═══════════════════════════════════════════════════════════════════════
        // GEODESIC V1: Animated Geodesic Sphere
        // ═══════════════════════════════════════════════════════════════════════
        // Ported from Shadertoy "Geodesic Tiling" by tdhooper
        // Uses icosahedral symmetry with hexagonal cells
        
        SCHEMAS.put("GEODESIC_V1", EffectSchema.builder("Geodesic Sphere", EffectType.GEODESIC, 1)
            .group("General", List.of(
                toggle(FV + "enabled", "Enabled", true, "General"),
                // slider(path, label, MIN, MAX, default, group)
                slider(FV + "intensity", "Intensity", 0f, 10f, 1f, "General"),
                slider(FV + "animationSpeed", "Animation Speed", 0.01f, 5f, 1f, "General"),
                slider(FV + "previewRadius", "Preview Radius", 0.1f, 100f, 5f, "General")
            ))
            .group("Geodesic Structure", List.of(
                // Original: subdivisions animated 2.4-3.4
                slider(FV + "geoSubdivisions", "Subdivisions", 1f, 100f, 3f, "Geodesic Structure"),
                // Original: height = 2.0
                slider(FV + "geoHeight", "Cell Height", 0.5f, 5f, 2f, "Geodesic Structure"),
                // Original: thickness varies, default 2.0
                slider(FV + "geoThickness", "Cell Thickness", 0.01f, 5f, 2f, "Geodesic Structure"),
                // Original: gap = 0.005
                slider(FV + "geoGap", "Cell Gap", 0f, 0.5f, 0.005f, "Geodesic Structure"),
                slider(FV + "geoDomeClip", "Dome Clip", 0f, 1f, 0f, "Geodesic Structure")
            ))
            .group("Rounding", List.of(
                // Original: roundTop = 0.05/subdiv, roundCorner = 0.1/subdiv
                slider(FV + "geoRoundTop", "Top Rounding", 0f, 0.9f, 0.05f, "Rounding"),
                slider(FV + "geoRoundCorner", "Corner Rounding", 0f, 0.9f, 0.1f, "Rounding")
            ))
            .group("Animation", List.of(
                // 0.0=static, 0.1=wave, 0.2=Y-wave, 0.3=gap breathing
                slider(FV + "geoAnimMode", "Animation Mode", 0f, 0.3f, 0.1f, "Animation"),
                // Original: pR(p.xz, time * PI/16) = ~0.2 rad/sec
                slider(FV + "geoRotationSpeed", "Rotation Speed", 0f, 2f, 0.2f, "Animation"),
                // Original: hardcoded 30, controls wave frequency
                slider(FV + "geoWaveResolution", "Wave Frequency", 1f, 100f, 30f, "Animation"),
                slider(FV + "geoWaveAmplitude", "Wave Intensity", 0f, 2f, 0.1f, "Animation")
            ))
            .group("Edge Colors", List.of(
                // Original: spectrum(...) * 5
                slider(FV + "lightFresnel", "Spectrum Offset", 0f, 20f, 5f, "Edge Colors")
            ))
            .group("Colors", List.of(
                // Face color (outer surface)
                normalized(FV + "primaryR", "Face R", 0.9f, "Colors"),
                normalized(FV + "primaryG", "Face G", 0.9f, "Colors"),
                normalized(FV + "primaryB", "Face B", 1f, "Colors"),
                // Back color (inner surface)
                normalized(FV + "secondaryR", "Back R", 0.1f, "Colors"),
                normalized(FV + "secondaryG", "Back G", 0.1f, "Colors"),
                normalized(FV + "secondaryB", "Back B", 0.15f, "Colors"),
                // Background color
                normalized(FV + "tertiaryR", "Background R", 0f, "Colors"),
                normalized(FV + "tertiaryG", "Background G", 0.005f, "Colors"),
                normalized(FV + "tertiaryB", "Background B", 0.03f, "Colors")
            ))
            .build());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // V8: Electric Aura - Pulsating electric plasma rays with ground transposition
    // Forked from V7, replaces corona rays with turbulent FBM electric plasma
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerElectricAura() {
        SCHEMAS.put("ENERGY_ORB_V8", EffectSchema.builder("Electric Aura", EffectType.ENERGY_ORB, 8)
            .group("General", List.of(
                slider(FV + "previewRadius", "Preview Radius", 0.01f, 100f, 10f, "General")
            ))
            .group("Body", List.of(
                // _Zoom: Detail zoom level
                slider(FV + "noiseBaseScale", "Zoom", 0.01f, 100f, 1f, "Body", "_Zoom: Detail zoom level"),
                // _Detail: LOD levels 0-10 (int)
                intSlider(FV + "noiseOctaves", "Detail", 0, 50, 5, "Body")
            ))
            .group("Core", List.of(
                // Core type: false=Default V7, true=Electric FBM
                toggle(FV + "v8CoreType", "Electric Core", true, "Core", "Enable Electric FBM tendril pattern"),
                // Electric pattern scale (only affects Electric mode)
                slider(FV + "v2CoreSpread", "Electric Scale", 0.1f, 10f, 1f, "Core", "Electric pattern scale (Type 1 only)"),
                // Core size multiplier
                slider(FV + "coreSize", "Core Scale", 0.001f, 100f, 0.1f, "Core", "Multiplier for visible core size")
            ))
            
            // ═══════════════════════════════════════════════════════════════════════
            // V8 DEDICATED CONTROLS - Separate Plasma, Ring, and Corona
            // ═══════════════════════════════════════════════════════════════════════
            
            .group("Plasma", List.of(
                slider(FV + "v8PlasmaScale", "Scale", 1f, 50f, 6f, "Plasma", "Pattern size - larger = bigger features"),
                slider(FV + "v8PlasmaSpeed", "Speed", 0f, 10f, 1f, "Plasma", "Animation speed"),
                slider(FV + "v8PlasmaTurbulence", "Turbulence", 0f, 2f, 1f, "Plasma", "0=smooth, 1=ridged electric"),
                slider(FV + "v8PlasmaIntensity", "Intensity", 0f, 10f, 2f, "Plasma", "Brightness")
            ))
            .group("Rings", List.of(
                slider(FV + "v8RingFrequency", "Frequency", 1f, 20f, 4f, "Rings", "Number of rings"),
                slider(FV + "v8RingSpeed", "Speed", 0f, 20f, 5f, "Rings", "Expansion rate"),
                slider(FV + "v8RingSharpness", "Sharpness", 0.1f, 100f, 28f, "Rings", "Edge crispness"),
                slider(FV + "v8RingCenterValue", "Center Value", 0f, 0.5f, 0.1f, "Rings", "Brightness center"),
                slider(FV + "v8RingModPower", "Mod Power", 0f, 2f, 0.9f, "Rings", "Modulation curve"),
                slider(FV + "v8RingIntensity", "Intensity", 0f, 10f, 1f, "Rings", "Ring brightness")
            ))
            .group("Corona", List.of(
                slider(FV + "v8CoronaExtent", "Extent", 1f, 10f, 2f, "Corona", "Max reach from sphere"),
                slider(FV + "v8CoronaFadeStart", "Fade Start", 0f, 1f, 0.5f, "Corona", "Where fade begins"),
                slider(FV + "v8CoronaFadePower", "Fade Power", 0.1f, 10f, 1f, "Corona", "Fade curve"),
                slider(FV + "v8CoronaIntensity", "Intensity", 0f, 10f, 1f, "Corona", "Overall brightness")
            ))
            
            // ═══════════════════════════════════════════════════════════════════════
            // V7 RAY CONTROLS - All 9 legacy params now working for corona
            // ═══════════════════════════════════════════════════════════════════════
            
            .group("V7 Ray Controls", List.of(
                slider(FV + "raySharpness", "Ray String", 0.02f, 10f, 1f, "V7 Ray Controls", "Ring width/spread"),
                slider(FV + "fadeScale", "Ray Reach", 1f, 1000f, 200f, "V7 Ray Controls", "Ring extent multiplier"),
                slider(FV + "insideFalloffPower", "Ray Fade", 0.01f, 100f, 1f, "V7 Ray Controls", "Edge fade curve"),
                slider(FV + "rayPower", "Rays", 0.1f, 10f, 2f, "V7 Ray Controls", "Ray noise intensity exponent"),
                slider(FV + "coronaPower", "Ray Ring", 0.1f, 10f, 1f, "V7 Ray Controls", "Ring noise exponent"),
                slider(FV + "coronaMultiplier", "Ray Glow", 1f, 100f, 2f, "V7 Ray Controls", "Ray-modulated glow power"),
                slider(FV + "coreFalloff", "Glow", 1f, 100f, 4f, "V7 Ray Controls", "Base glow falloff power")
            ))
            .group("V7 Animation", List.of(
                slider(FV + "speedRay", "Speed Ray", 0f, 20f, 5f, "V7 Animation", "Ray noise animation"),
                slider(FV + "speedRing", "Speed Ring", 0f, 20f, 2f, "V7 Animation", "Ring noise animation")
            ))
            
            // ═══════════════════════════════════════════════════════════════════════
            // BODY/CORE CONTROLS (still from V7)
            // ═══════════════════════════════════════════════════════════════════════
            
            .group("Body Animation", List.of(
                slider(FV + "speedHigh", "Speed High", 0f, 50f, 2f, "Body Animation", "Body noise (high freq)"),
                slider(FV + "speedLow", "Speed Low", 0f, 50f, 2f, "Body Animation", "Body noise (low freq)")
            ))
            .group("Electric Core", List.of(
                // Electric Core controls: flash toggle, fill, and line width (only active when Electric core type is on)
                // Fill COLOR is controlled by Accent1 (Secondary Color) in the Colors group
                toggle(FV + "v8ElectricFlash", "Flash Effect", false, "Electric Core", "Enable scene-wide flash (OFF = no flash)"),
                slider(FV + "v8ElectricFillIntensity", "Fill Opacity", 0f, 1f, 0.5f, "Electric Core", "Fill visibility (0=transparent, 1=opaque)"),
                slider(FV + "v8ElectricFillDarken", "Fill Darken", 0f, 1f, 0.0f, "Electric Core", "Darken fill color (0=Accent1, 1=black)"),
                slider(FV + "v8ElectricLineWidth", "Line Width", -400f, 400f, -140f, "Electric Core", "Line thickness (-1000=thickest, 0=medium, +1000=thinnest)")
            ))
            // Dark Ring: REMOVED - V7 legacy feature disabled in V8
            .group("Proximity Darken", List.of(
                toggle(FV + "distortionStrength", "Enable", true, "Proximity Darken"),
                slider(FV + "blackout", "Intensity", 0f, 1f, 0.5f, "Proximity Darken", "Darkening amount"),
                slider(FV + "distortionRadius", "Range", 10f, 10000f, 500f, "Proximity Darken", "Fade distance"),
                slider(FV + "distortionFrequency", "Fade", 0.01f, 5f, 0.1f, "Proximity Darken", "Fade curve")
            ))
            .group("Noise", List.of(
                slider(FV + "noiseSeed", "Seed", -10f, 10f, 1f, "Noise", "Pattern variation")
            ))
            .group("Colors", List.of(
                // Electric effect colors - cyan/purple plasma by default
                normalized(FV + "highlightR", "Light R", 1f, "Colors"),
                normalized(FV + "highlightG", "Light G", 1f, "Colors"),
                normalized(FV + "highlightB", "Light B", 1f, "Colors"),
                normalized(FV + "primaryR", "Body R", 0.34f, "Colors"),
                normalized(FV + "primaryG", "Body G", 0.27f, "Colors"),
                normalized(FV + "primaryB", "Body B", 1f, "Colors"),
                normalized(FV + "secondaryR", "Accent1 R", 0.1f, "Colors"),
                normalized(FV + "secondaryG", "Accent1 G", 0.3f, "Colors"),
                normalized(FV + "secondaryB", "Accent1 B", 0.8f, "Colors"),
                normalized(FV + "tertiaryR", "Accent2 R", 0.5f, "Colors"),
                normalized(FV + "tertiaryG", "Accent2 G", 0f, "Colors"),
                normalized(FV + "tertiaryB", "Accent2 B", 0.8f, "Colors"),
                normalized(FV + "rayR", "Ray R", 0.20f, "Colors"),
                normalized(FV + "rayG", "Ray G", 0.06f, "Colors"),
                normalized(FV + "rayB", "Ray B", 1f, "Colors")
            ))
            .build());
    }
}
