package net.cyberpunk042.field;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.LifecycleConfig;
import net.cyberpunk042.field.influence.TriggerConfig;
import net.cyberpunk042.field.instance.FollowConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Complete definition of a visual field.
 * 
 * <p>Per CLASS_DIAGRAM §1: FieldDefinition contains all configuration for a field.
 * 
 * <p>A field definition contains:
 * <ul>
 *   <li><b>id</b> - Unique identifier (e.g., "shield_default")</li>
 *   <li><b>type</b> - Field category (SHIELD, PERSONAL, etc.)</li>
 *   <li><b>baseRadius</b> - Base scale before modifiers</li>
 *   <li><b>themeId</b> - Color theme reference (e.g., "energy_blue")</li>
 *   <li><b>layers</b> - Ordered list of layers to render</li>
 *   <li><b>modifiers</b> - Visual modifiers (scale, tilt, swirl, etc.)</li>
 *   <li><b>follow</b> - How field follows player (lead/trail, responsiveness)</li>
 *   <li><b>beam</b> - Central beam effect config</li>
 *   <li><b>bindings</b> - Reactive bindings (property → external value)</li>
 *   <li><b>triggers</b> - Event-triggered effects</li>
 *   <li><b>lifecycle</b> - Spawn/despawn animation config</li>
 *   <li><b>forceConfig</b> - Force field physics (only for type=FORCE)</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "id": "shield_default",
 *   "type": "SHIELD",
 *   "baseRadius": 1.0,
 *   "themeId": "energy_blue",
 *   "layers": [...],
 *   "modifiers": { "visualScale": 1.0, "tilt": 0.0 },
 *   "follow": { "enabled": true, "leadOffset": 0.0, "responsiveness": 1.0 },
 *   "beam": { "enabled": false },
 *   "bindings": {
 *     "alpha": { "source": "player.health_percent", "outputRange": [0.3, 1.0] }
 *   },
 *   "triggers": [...],
 *   "lifecycle": { "fadeIn": 10, "fadeOut": 10 }
 * }
 * </pre>
 * 
 * @see FieldLayer
 * @see FieldType
 * @see BindingConfig
 * @see Modifiers
 * @see FollowConfig
 * @see BeamConfig
 * @see LifecycleConfig
 * @see ForceFieldConfig
 */
public record FieldDefinition(
    String id,
    FieldType type,
    float baseRadius,
    @Nullable @JsonField(skipIfNull = true) String themeId,
    List<FieldLayer> layers,
    @Nullable @JsonField(skipIfNull = true) Modifiers modifiers,
    @Nullable @JsonField(skipIfNull = true) FollowConfig follow,
    @Nullable @JsonField(skipIfNull = true) BeamConfig beam,
    @JsonField(skipIfEmpty = true) Map<String, BindingConfig> bindings,
    @JsonField(skipIfEmpty = true) List<TriggerConfig> triggers,
    @Nullable @JsonField(skipIfNull = true) LifecycleConfig lifecycle,
    @Nullable @JsonField(skipIfNull = true) ForceFieldConfig forceConfig,
    @Nullable @JsonField(skipIfNull = true) JsonObject shockwave,  // Field-level FX effect
    @Nullable @JsonField(skipIfNull = true) JsonObject fieldVisual  // Field visual effect (Energy Orb)
){
    
    /**
     * Parses a FieldDefinition from JSON.
     * Delegates to FieldLoader for full parsing with $ref resolution and defaults.
     * 
     * @param json the JSON object to parse
     * @return parsed FieldDefinition
     */
    public static FieldDefinition fromJson(JsonObject json) {
        return net.cyberpunk042.field.loader.FieldLoader.fromJson(json);
    }
    
    /**
     * Creates an empty definition.
     */
    public static FieldDefinition empty(String id) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, null, List.of(),
            null, null, null, Map.of(), List.of(), null, null, null, null);
    }
    
    /**
     * Creates a definition with layers.
     */
    public static FieldDefinition of(String id, List<FieldLayer> layers) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, null, layers,
            null, null, null, Map.of(), List.of(), null, null, null, null);
    }
    
    /**
     * Creates a definition with layers and theme.
     */
    public static FieldDefinition of(String id, List<FieldLayer> layers, String themeId) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, themeId, layers,
            null, null, null, Map.of(), List.of(), null, null, null, null);
    }
    
    /**
     * Whether this field has any bindings.
     */
    public boolean hasBindings() {
        return bindings != null && !bindings.isEmpty();
    }
    
    /**
     * Whether this field has a beam effect.
     */
    public boolean hasBeam() {
        return beam != null && beam.enabled();
    }
    
    /**
     * Whether this field has any triggers.
     */
    public boolean hasTriggers() {
        return triggers != null && !triggers.isEmpty();
    }
    
    /**
     * Whether this field has follow enabled.
     */
    public boolean hasFollow() {
        return follow != null && follow.enabled();
    }
    
    /**
     * Whether this field has force physics.
     */
    public boolean hasForceConfig() {
        return forceConfig != null;
    }
    
    /**
     * Whether this is a force-type field.
     */
    public boolean isForceField() {
        return type == FieldType.FORCE && forceConfig != null;
    }
    
    /**
     * Whether this field has a shockwave effect configured.
     */
    public boolean hasShockwave() {
        return shockwave != null && shockwave.size() > 0;
    }
    
    /**
     * Whether this field has a field visual effect configured.
     */
    public boolean hasFieldVisual() {
        return fieldVisual != null && fieldVisual.size() > 0;
    }
    
    /**
     * Serializes this field definition to JSON.
     * 
     * @return JSON representation of this definition
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    /**
     * Builder for complex definitions.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private FieldType type = FieldType.SHIELD;
        private float baseRadius = 1.0f;
        private String themeId = null;
        private List<FieldLayer> layers = List.of();
        private Modifiers modifiers = null;
        private FollowConfig follow = null;
        private BeamConfig beam = null;
        private Map<String, BindingConfig> bindings = Map.of();
        private List<TriggerConfig> triggers = List.of();
        private LifecycleConfig lifecycle = null;
        private ForceFieldConfig forceConfig = null;
        private JsonObject shockwave = null;
        private JsonObject fieldVisual = null;
        
        public Builder(String id) { this.id = id; }
        
        public Builder type(FieldType t) { this.type = t; return this; }
        public Builder baseRadius(float r) { this.baseRadius = r; return this; }
        public Builder themeId(String t) { this.themeId = t; return this; }
        public Builder layers(List<FieldLayer> l) { this.layers = l; return this; }
        public Builder layers(FieldLayer... l) { this.layers = List.of(l); return this; }
        public Builder modifiers(Modifiers m) { this.modifiers = m; return this; }
        public Builder follow(FollowConfig f) { this.follow = f; return this; }
        public Builder beam(BeamConfig b) { this.beam = b; return this; }
        public Builder bindings(Map<String, BindingConfig> b) { this.bindings = b; return this; }
        public Builder triggers(List<TriggerConfig> t) { this.triggers = t; return this; }
        public Builder lifecycle(LifecycleConfig l) { this.lifecycle = l; return this; }
        public Builder forceConfig(ForceFieldConfig f) { this.forceConfig = f; return this; }
        public Builder shockwave(JsonObject s) { this.shockwave = s; return this; }
        public Builder fieldVisual(JsonObject f) { this.fieldVisual = f; return this; }
        
        public FieldDefinition build() {
            return new FieldDefinition(
                id, type, baseRadius, themeId, layers,
                modifiers, follow, beam,
                bindings, triggers, lifecycle, forceConfig, shockwave, fieldVisual);
        }
    }
}
