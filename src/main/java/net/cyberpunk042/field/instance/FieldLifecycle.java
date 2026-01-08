package net.cyberpunk042.field.instance;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.influence.DecayConfig;
import net.cyberpunk042.field.influence.FieldEvent;
import net.cyberpunk042.field.influence.LifecycleConfig;
import net.cyberpunk042.field.influence.TriggerEventDispatcher;
import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles field instance lifecycle events.
 * 
 * <h2>Lifecycle Phases</h2>
 * <ol>
 *   <li><b>Spawn</b> - Instance created and added to world</li>
 *   <li><b>Tick</b> - Per-tick update (effects, animation)</li>
 *   <li><b>Despawn</b> - Instance removed from world</li>
 * </ol>
 * 
 * <h2>Usage</h2>
 * <pre>
 * FieldLifecycle lifecycle = FieldLifecycle.get();
 * lifecycle.onSpawn(instance, world);
 * // Each tick:
 * lifecycle.onTick(instance, world);
 * // When removing:
 * lifecycle.onDespawn(instance, world);
 * </pre>
 */
public class FieldLifecycle {
    
    // Default values (used when no LifecycleConfig is present)
    private static final int DEFAULT_FADE_IN_TICKS = 10;
    private static final int DEFAULT_FADE_OUT_TICKS = 10;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle Events
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Called when a field instance is spawned.
     * 
     * @param instance the field instance
     * @param world the server world (may be null for client-side)
     */
    public void onSpawn(FieldInstance instance, ServerWorld world) {
        Logging.REGISTRY.topic("lifecycle").debug(
            "Field spawned: {} at {}", 
            instance.definitionId(), 
            instance.position());
        
        // F156: Dispatch FIELD_SPAWN event if this is a personal field
        if (instance.ownerUuid() != null && world != null) {
            PlayerEntity player = world.getPlayerByUuid(instance.ownerUuid());
            if (player instanceof ServerPlayerEntity serverPlayer) {
                TriggerEventDispatcher.dispatch(FieldEvent.FIELD_SPAWN, serverPlayer, instance);
            }
        }
        
        // Apply initial effects
        applySpawnEffects(instance, world);
    }
    
    /**
     * Called each tick to update the field instance.
     * 
     * @param instance the field instance
     * @param world the server world
     */
    public void onTick(FieldInstance instance, ServerWorld world) {
        int age = instance.age();
        
        // Update fade state based on age
        updateFadeState(instance, age);
        
        // Process effects (damage, healing, etc.)
        processEffects(instance, world);
        
        // Personal fields update position via their tick method
    }
    
    /**
     * Called when a field instance is despawned.
     * 
     * @param instance the field instance
     * @param world the server world
     */
    public void onDespawn(FieldInstance instance, ServerWorld world) {
        Logging.REGISTRY.topic("lifecycle").debug(
            "Field despawned: {} (age: {} ticks)", 
            instance.definitionId(),
            instance.age());
        
        // F156: Dispatch FIELD_DESPAWN event if this is a personal field
        if (instance.ownerUuid() != null && world != null) {
            PlayerEntity player = world.getPlayerByUuid(instance.ownerUuid());
            if (player instanceof ServerPlayerEntity serverPlayer) {
                TriggerEventDispatcher.dispatch(FieldEvent.FIELD_DESPAWN, serverPlayer, instance);
            }
        }
        
        // Clean up any effects
        cleanupEffects(instance, world);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Fade Handling
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * F161: Updates lifecycle state based on age and config.
     */
    private void updateFadeState(FieldInstance instance, int age) {
        LifecycleConfig config = getLifecycleConfig(instance);
        int fadeIn = config != null ? config.fadeIn() : DEFAULT_FADE_IN_TICKS;
        int fadeOut = config != null ? config.fadeOut() : DEFAULT_FADE_OUT_TICKS;
        
        switch (instance.lifecycleState()) {
            case SPAWNING -> {
                if (age < fadeIn) {
                    float progress = fadeIn > 0 ? (float) age / fadeIn : 1.0f;
                    instance.setFadeProgress(progress);
                    instance.setAlpha(progress);
                    // Apply scaleIn if configured
                    if (config != null && config.scaleIn() > 0) {
                        instance.setScale(progress);
                    }
                } else {
                    instance.activate();
                }
            }
            case ACTIVE -> {
                instance.setFadeProgress(1.0f);
                instance.setAlpha(1.0f);
                // Apply decay if configured
                if (config != null && config.decay() != null && config.decay().isActive()) {
                    DecayConfig decay = config.decay();
                    float decayed = decay.apply(instance.alpha());
                    instance.setAlpha(decayed);
                }
            }
            case DESPAWNING -> {
                // Calculate how long we've been despawning
                // (This assumes we track despawn start somewhere, simplified here)
                float progress = instance.fadeProgress() - (1.0f / Math.max(1, fadeOut));
                if (progress <= 0) {
                    instance.complete();
                } else {
                    instance.setFadeProgress(progress);
                    instance.setAlpha(progress);
                    if (config != null && config.scaleOut() > 0) {
                        instance.setScale(progress);
                    }
                }
            }
            case COMPLETE -> {
                instance.setAlpha(0);
            }
        }
    }
    
    /**
     * Gets the LifecycleConfig for an instance from its definition.
     */
    private LifecycleConfig getLifecycleConfig(FieldInstance instance) {
        FieldDefinition def = FieldRegistry.get(instance.definitionId());
        return def != null ? def.lifecycle() : null;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Effects
    // ─────────────────────────────────────────────────────────────────────────
    
    private void applySpawnEffects(FieldInstance instance, ServerWorld world) {
        // Spawn particles, sounds, etc.
        // Can be overridden in subclasses for specific behavior
    }
    
    private void processEffects(FieldInstance instance, ServerWorld world) {
        // Process damage, healing, knockback, etc.
        // Delegates to EffectProcessor if configured
        // Effects would be processed here via FieldEffects utility
    }
    
    private void cleanupEffects(FieldInstance instance, ServerWorld world) {
        // Remove any lingering status effects, particles, etc.
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────────
    
    private static final FieldLifecycle INSTANCE = new FieldLifecycle();
    
    public static FieldLifecycle get() {
        return INSTANCE;
    }
}
