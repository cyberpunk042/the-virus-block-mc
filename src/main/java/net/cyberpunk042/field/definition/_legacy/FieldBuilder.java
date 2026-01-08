package net.cyberpunk042.field.definition;

import net.cyberpunk042.field.*;
import net.cyberpunk042.field.instance.FieldEffect;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.cyberpunk042.field.effect.EffectType;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

/**
 * Convenience builder for field definitions with sensible defaults.
 * 
 * <p>This is a facade over {@link FieldDefinition.Builder} that provides
 * more convenient methods and preset configurations.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Simple shield field
 * FieldDefinition shield = FieldBuilder.shield("my_shield")
 *     .radius(3.0f)
 *     .theme("cyber_blue")
 *     .damageEnemies(2.0f)
 *     .build();
 * 
 * // Personal protective field
 * FieldDefinition personal = FieldBuilder.personal("my_aura")
 *     .radius(2.0f)
 *     .healing(0.5f)
 *     .build();
 * </pre>
 * 
 * @see FieldDefinition
 */
public final class FieldBuilder {
    
    private final FieldDefinition.Builder builder;
    
    private FieldBuilder(String id, FieldType type) {
        this.builder = FieldDefinition.builder(id, type);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates a shield-type field builder.
     */
    public static FieldBuilder shield(String id) {
        return new FieldBuilder(id, FieldType.SHIELD);
    }
    
    /**
     * Creates a personal-type field builder.
     */
    public static FieldBuilder personal(String id) {
        return new FieldBuilder(id, FieldType.PERSONAL);
    }
    
    /**
     * Creates a force-type field builder.
     */
    public static FieldBuilder force(String id) {
        return new FieldBuilder(id, FieldType.FORCE);
    }
    
    /**
     * Creates a growth-type field builder.
     */
    public static FieldBuilder growth(String id) {
        return new FieldBuilder(id, FieldType.GROWTH);
    }
    
    /**
     * Creates a singularity-type field builder.
     */
    public static FieldBuilder singularity(String id) {
        return new FieldBuilder(id, FieldType.SINGULARITY);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────
    
    public FieldBuilder radius(float radius) {
        builder.baseRadius(radius);
        return this;
    }
    
    public FieldBuilder theme(String themeId) {
        builder.theme(themeId);
        return this;
    }
    
    public FieldBuilder layer(FieldLayer layer) {
        builder.layer(layer);
        return this;
    }
    
    public FieldBuilder modifiers(Modifiers modifiers) {
        builder.modifiers(modifiers);
        return this;
    }
    
    public FieldBuilder effect(FieldEffect effect) {
        builder.effect(effect);
        return this;
    }
    
    public FieldBuilder prediction(PredictionConfig config) {
        builder.prediction(config);
        return this;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Effect Shortcuts
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Adds damage effect to enemies.
     */
    public FieldBuilder damageEnemies(float damage) {
        builder.effect(FieldEffect.damage(damage, 20)); // 1 second cooldown
        return this;
    }
    
    /**
     * Adds healing effect.
     */
    public FieldBuilder healing(float amount) {
        builder.effect(FieldEffect.heal(amount, 40));
        return this;
    }
    
    /**
     * Adds push/knockback effect.
     */
    public FieldBuilder pushBack(float strength) {
        builder.effect(FieldEffect.push(strength));
        return this;
    }
    
    /**
     * Adds pull effect (opposite of pushBack).
     */
    public FieldBuilder pullIn(float strength) {
        builder.effect(FieldEffect.pull(strength));
        return this;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Modifier Shortcuts
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Makes the field pulse.
     */
    public FieldBuilder pulsing() {
        builder.modifiers(Modifiers.builder().pulsing(true).build());
        return this;
    }
    
    /**
     * Inverts push/pull effects.
     */
    public FieldBuilder inverted() {
        builder.modifiers(Modifiers.builder().inverted(true).build());
        return this;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Build
    // ─────────────────────────────────────────────────────────────────────────
    
    public FieldDefinition build() {
        Logging.REGISTRY.topic("field-builder").debug("Building field via FieldBuilder");
        return builder.build();
    }
}
