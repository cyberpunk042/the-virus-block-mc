package net.cyberpunk042.field.effect;

import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.log.Logging;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Processes field effects on entities within range.
 * 
 * <h2>Usage</h2>
 * <pre>
 * EffectProcessor processor = new EffectProcessor();
 * processor.process(fieldInstance, world);
 * </pre>
 */
public class EffectProcessor {
    
    public EffectProcessor() {}
    
    /**
     * Processes all effects for a field instance.
     */
    public void process(FieldInstance instance, ServerWorld world, List<ActiveEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        
        Logging.REGISTRY.topic("effect").debug(
            "Processing {} effects for field {} at {}", 
            effects.size(), instance.id(), instance.position());
        
        Vec3d center = instance.position();
        float fieldRadius = instance.scale(); // Use scale as radius
        
        for (ActiveEffect effect : effects) {
            if (!effect.tick()) {
                continue; // On cooldown
            }
            
            float radius = effect.getRadius() > 0 ? effect.getRadius() : fieldRadius;
            processEffect(effect, center, radius, world);
        }
    }
    
    /**
     * Processes a single effect.
     */
    private void processEffect(ActiveEffect effect, Vec3d center, float radius, ServerWorld world) {
        Box box = new Box(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        List<Entity> entities = world.getOtherEntities(null, box, e -> e.isAlive());
        
        try (LogScope scope = Logging.REGISTRY.topic("effect").scope("process-entities", LogLevel.TRACE)) {
            for (Entity entity : entities) {
                double distance = entity.getPos().distanceTo(center);
                if (distance > radius) {
                    continue;
                }

                if (!effect.canApply(entity)) {
                    continue;
                }

                applyEffect(effect, entity, center, distance, radius);
                scope.branch("entity").kv("type", effect.getType()).kv("untranslatedName", entity.getType().getUntranslatedName()).kv("distance", distance);
            }
        }
        
        if (!entities.isEmpty()) {
            effect.onApplied();
            Logging.REGISTRY.topic("effect").debug(
                "Effect {} applied to {} entities", effect.getType(), entities.size());
        }
    }
    
    /**
     * Applies an effect to an entity.
     */
    private void applyEffect(ActiveEffect effect, Entity entity, Vec3d center, double distance, float radius) {
        float strength = effect.getStrength();
        float falloff = 1.0f - (float)(distance / radius); // Linear falloff
        
        switch (effect.getType()) {
            case PUSH -> applyPush(entity, center, strength * falloff);
            case PULL -> applyPull(entity, center, strength * falloff);
            case DAMAGE -> applyDamage(entity, strength);
            case HEAL -> applyHeal(entity, strength);
            case SLOW -> applySlow(entity, strength);
            case SPEED -> applySpeed(entity, strength);
            case SHIELD -> {} // Handled differently (blocks infection)
            case CUSTOM -> {} // External handling
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Effect implementations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public void applyPush(Entity entity, Vec3d center, float strength) {
        Vec3d direction = entity.getPos().subtract(center).normalize();
        Vec3d velocity = direction.multiply(strength * 0.5);
        entity.addVelocity(velocity.x, velocity.y * 0.5, velocity.z);
        entity.velocityModified = true;
    }
    
    public void applyPull(Entity entity, Vec3d center, float strength) {
        Vec3d direction = center.subtract(entity.getPos()).normalize();
        Vec3d velocity = direction.multiply(strength * 0.3);
        entity.addVelocity(velocity.x, velocity.y * 0.3, velocity.z);
        entity.velocityModified = true;
    }
    
    public void applyDamage(Entity entity, float amount) {
        if (entity instanceof LivingEntity living && entity.getWorld() instanceof ServerWorld serverWorld) {
            Logging.REGISTRY.topic("effect").debug(
                "Dealing {} magic damage to {}", amount, entity.getType().getUntranslatedName());
            living.damage(serverWorld, entity.getDamageSources().magic(), amount);
        }
    }
    
    public void applyHeal(Entity entity, float amount) {
        if (entity instanceof LivingEntity living) {
            Logging.REGISTRY.topic("effect").debug(
                "Healing {} for {} HP", entity.getType().getUntranslatedName(), amount);
            living.heal(amount);
        }
    }
    
    public void applySlow(Entity entity, float factor) {
        if (entity instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 
                40, // 2 seconds
                (int)(factor * 2),
                false, false, false
            ));
        }
    }
    
    public void applySpeed(Entity entity, float factor) {
        if (entity instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                40, // 2 seconds
                (int)(factor * 2),
                false, false, false
            ));
        }
    }
    
    /**
     * Checks if a position is shielded by any active shield field.
     * Used by infection system.
     */
    public boolean isShielded(Vec3d pos, List<FieldInstance> shieldFields) {
        for (FieldInstance field : shieldFields) {
            double distance = pos.distanceTo(field.position());
            if (distance <= field.scale()) {
                return true;
            }
        }
        return false;
    }
}
