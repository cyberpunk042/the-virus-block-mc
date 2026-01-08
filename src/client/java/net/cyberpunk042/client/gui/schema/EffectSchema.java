package net.cyberpunk042.client.gui.schema;

import net.cyberpunk042.client.visual.effect.EffectType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema defining visible parameters and their configuration for a specific effect type and version.
 * 
 * <p>Each effect type can have multiple versions (V1, V2, V3, V4) with different
 * visible parameters and custom labels. The schema determines:</p>
 * <ul>
 *   <li>Which parameters are shown in the GUI</li>
 *   <li>How they are labeled (can vary per version)</li>
 *   <li>How they are grouped and ordered</li>
 * </ul>
 * 
 * <h3>Example Usage</h3>
 * <pre>{@code
 * EffectSchema energyOrbV1 = EffectSchema.builder("Energy Orb V1", EffectType.ENERGY_ORB, 1)
 *     .group("Core", List.of(
 *         ParameterSpec.slider("fieldVisual.coreSize", "Core Size", 0.05f, 0.5f, 0.15f, "Core"),
 *         ParameterSpec.slider("fieldVisual.edgeSharpness", "Edge Sharpness", 1f, 10f, 4f, "Core")
 *     ))
 *     .group("Animation", List.of(
 *         ParameterSpec.slider("fieldVisual.intensity", "Intensity", 0f, 2f, 1.2f, "Animation"),
 *         ParameterSpec.slider("fieldVisual.animationSpeed", "Speed", 0.5f, 3f, 1f, "Animation")
 *     ))
 *     .build();
 * }</pre>
 */
public record EffectSchema(
    /**
     * Human-readable display name (e.g., "Energy Orb V2", "Volumetric Star").
     */
    String displayName,
    
    /**
     * The effect type this schema applies to.
     */
    EffectType effectType,
    
    /**
     * Version number (1, 2, 3, 4...).
     * Allows different parameter sets per version.
     */
    int version,
    
    /**
     * Parameters organized by group name.
     * LinkedHashMap preserves insertion order for consistent display.
     */
    Map<String, List<ParameterSpec>> groups
) {
    
    /**
     * Returns all parameter specs in a flat list, preserving group order.
     * 
     * <p>If the same path appears multiple times (e.g., defined in General then
     * overridden in a version-specific group), the LAST occurrence is kept.
     * This enables version-specific overrides of shared parameters.</p>
     */
    public List<ParameterSpec> allParameters() {
        // Use LinkedHashMap to deduplicate by path while preserving order
        // When same path appears twice, later one overwrites (enables overrides)
        var byPath = new LinkedHashMap<String, ParameterSpec>();
        groups.values().stream()
            .flatMap(List::stream)
            .forEach(spec -> byPath.put(spec.path(), spec));
        return List.copyOf(byPath.values());
    }
    
    /**
     * Returns the parameters for a specific group, or empty list if not found.
     */
    public List<ParameterSpec> getGroup(String groupName) {
        return groups.getOrDefault(groupName, Collections.emptyList());
    }
    
    /**
     * Returns true if this schema contains a parameter with the given path.
     */
    public boolean hasParameter(String path) {
        return groups.values().stream()
            .flatMap(List::stream)
            .anyMatch(spec -> spec.path().equals(path));
    }
    
    /**
     * Returns the ParameterSpec for a given path, or null if not found.
     * 
     * <p>If the path appears multiple times, returns the LAST occurrence
     * (the override, not the original).</p>
     */
    public ParameterSpec getParameter(String path) {
        ParameterSpec result = null;
        for (var group : groups.values()) {
            for (var spec : group) {
                if (spec.path().equals(path)) {
                    result = spec;  // Keep overwriting, last one wins
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the group names in order.
     */
    public List<String> groupNames() {
        return List.copyOf(groups.keySet());
    }
    
    /**
     * Returns total number of parameters across all groups.
     */
    public int parameterCount() {
        return groups.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a builder for constructing an EffectSchema.
     */
    public static Builder builder(String displayName, EffectType effectType, int version) {
        return new Builder(displayName, effectType, version);
    }
    
    /**
     * Fluent builder for EffectSchema.
     */
    public static class Builder {
        private final String displayName;
        private final EffectType effectType;
        private final int version;
        private final LinkedHashMap<String, List<ParameterSpec>> groups = new LinkedHashMap<>();
        
        private Builder(String displayName, EffectType effectType, int version) {
            this.displayName = displayName;
            this.effectType = effectType;
            this.version = version;
        }
        
        /**
         * Adds a group of parameters.
         * 
         * @param groupName Name of the group (e.g., "Colors", "Animation")
         * @param params    List of ParameterSpec for this group
         * @return this builder for chaining
         */
        public Builder group(String groupName, List<ParameterSpec> params) {
            groups.put(groupName, List.copyOf(params));
            return this;
        }
        
        /**
         * Adds a single parameter to an existing or new group.
         */
        public Builder param(String groupName, ParameterSpec param) {
            groups.compute(groupName, (k, existing) -> {
                if (existing == null) {
                    return List.of(param);
                }
                var list = new java.util.ArrayList<>(existing);
                list.add(param);
                return List.copyOf(list);
            });
            return this;
        }
        
        /**
         * Builds the immutable EffectSchema.
         */
        public EffectSchema build() {
            return new EffectSchema(displayName, effectType, version, Map.copyOf(groups));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCHEMA KEY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns a unique key for this schema (effectType + version).
     * Used for registry lookups.
     */
    public String key() {
        return effectType.name() + "_V" + version;
    }
    
    /**
     * Creates a key from effect type and version.
     */
    public static String key(EffectType effectType, int version) {
        return effectType.name() + "_V" + version;
    }
}
