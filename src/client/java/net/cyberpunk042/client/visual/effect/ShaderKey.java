package net.cyberpunk042.client.visual.effect;

import net.minecraft.util.Identifier;

/**
 * Key for identifying which shader to use for a field effect.
 * 
 * <p>Combines effect type and version to select the appropriate
 * standalone shader. Used as cache key for processor lookup.</p>
 * 
 * <h3>Shader Mapping:</h3>
 * <ul>
 *   <li>ENERGY_ORB + V2 → field_visual_v2</li>
 *   <li>ENERGY_ORB + V3 → field_visual_v3</li>
 *   <li>ENERGY_ORB + V6 → field_visual_v6</li>
 *   <li>ENERGY_ORB + V7 → field_visual_v7</li>
 *   <li>ENERGY_ORB + V8 → field_visual_v8 (Electric Aura)</li>
 *   <li>GEODESIC → field_visual_geodesic</li>
 *   <li>Other → field_visual (monolith fallback)</li>
 * </ul>
 */
public record ShaderKey(EffectType effectType, int version) {
    
    /**
     * Creates a ShaderKey from a FieldVisualConfig.
     */
    public static ShaderKey fromConfig(FieldVisualConfig config) {
        return new ShaderKey(config.effectType(), config.version());
    }
    
    /**
     * Returns the shader identifier for this key.
     * Maps to the appropriate standalone shader based on effect type and version.
     */
    public Identifier toShaderId() {
        return switch (effectType) {
            case GEODESIC -> Identifier.of("the-virus-block", "field_visual_geodesic");
            case ENERGY_ORB -> energyOrbShaderId();
            case NONE -> null;  // No shader needed
        };
    }
    
    /**
     * Maps energy orb versions to their standalone shaders.
     */
    private Identifier energyOrbShaderId() {
        return switch (version) {
            case 1 -> Identifier.of("the-virus-block", "field_visual_v1");
            case 2 -> Identifier.of("the-virus-block", "field_visual_v2");
            case 3 -> Identifier.of("the-virus-block", "field_visual_v3");
            case 5 -> Identifier.of("the-virus-block", "field_visual_v5");
            case 6 -> Identifier.of("the-virus-block", "field_visual_v6");
            case 7 -> Identifier.of("the-virus-block", "field_visual_v7");
            case 8 -> Identifier.of("the-virus-block", "field_visual_v8");
            // Unknown versions fall back to V1 (simplest)
            default -> Identifier.of("the-virus-block", "field_visual_v1");
        };
    }
    
    /**
     * Returns a human-readable description for logging.
     */
    public String describe() {
        if (effectType == EffectType.GEODESIC) {
            return "Geodesic";
        }
        return String.format("%s V%d", effectType, version);
    }
    
    /**
     * Returns true if this key uses a standalone shader.
     * Now ALL versions have standalone shaders - no monolith fallback.
     */
    public boolean isStandalone() {
        return effectType == EffectType.GEODESIC || effectType == EffectType.ENERGY_ORB;
    }
}
