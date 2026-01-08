package net.cyberpunk042.field.effect;

import net.minecraft.entity.Entity;

/**
 * Runtime state for an active effect on a field instance.
 * 
 * <p>Tracks cooldown and provides application logic.
 */
public class ActiveEffect {
    
    private final EffectConfig config;
    private int cooldownRemaining;
    
    public ActiveEffect(EffectConfig config) {
        this.config = config;
        this.cooldownRemaining = 0;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────────
    
    public EffectConfig getConfig() {
        return config;
    }
    
    public EffectType getType() {
        return config.type();
    }
    
    public float getStrength() {
        return config.strength();
    }
    
    public float getRadius() {
        return config.radius();
    }
    
    public boolean isOnCooldown() {
        return cooldownRemaining > 0;
    }
    
    public int getCooldownRemaining() {
        return cooldownRemaining;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Called each tick. Returns true if effect should be processed this tick.
     */
    public boolean tick() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return false;
        }
        return true;
    }
    
    /**
     * Starts the cooldown after effect application.
     */
    public void startCooldown() {
        cooldownRemaining = config.cooldown();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Application
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Checks if this effect can be applied to an entity.
     */
    public boolean canApply(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        return !isOnCooldown();
    }
    
    /**
     * Called after effect is applied to reset state.
     */
    public void onApplied() {
        if (config.cooldown() > 0) {
            startCooldown();
        }
    }
}
