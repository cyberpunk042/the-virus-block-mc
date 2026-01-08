package net.cyberpunk042.field.influence;

/**
 * Runtime state for an active trigger effect.
 * 
 * <p>Tracks:
 * <ul>
 *   <li>The trigger configuration</li>
 *   <li>Remaining ticks until effect ends</li>
 *   <li>Current effect progress (0-1)</li>
 * </ul>
 * 
 * <p>F154: While active, overrides any bindings that affect
 * the same property (e.g., a GLOW trigger overrides glow binding).
 */
public class ActiveTrigger {
    
    private final TriggerConfig config;
    private int remainingTicks;
    private final int totalTicks;
    
    public ActiveTrigger(TriggerConfig config) {
        this.config = config;
        this.totalTicks = config.duration();
        this.remainingTicks = config.duration();
    }
    
    /**
     * Ticks this trigger, returns true if still active.
     */
    public boolean tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            return true;
        }
        return false;
    }
    
    /**
     * Gets the progress of this trigger (0 = just started, 1 = ending).
     */
    public float getProgress() {
        if (totalTicks <= 0) return 1;
        return 1.0f - ((float) remainingTicks / totalTicks);
    }
    
    /**
     * Gets the current effect value based on progress.
     * 
     * <p>For effects that complete naturally (PULSE, SHAKE),
     * this returns a value that peaks at 0.5 and returns to 0.
     * For other effects, this returns the configured value
     * fading out towards the end.
     */
    public float getEffectValue() {
        float progress = getProgress();
        
        if (config.effect().completesNaturally()) {
            // Peak at middle, return to 0
            // Uses sine wave for smooth animation
            return (float) Math.sin(progress * Math.PI);
        } else {
            // Maintain value, fade out at end
            if (progress > 0.8f) {
                // Fade out in last 20%
                return 1.0f - ((progress - 0.8f) / 0.2f);
            }
            return 1.0f;
        }
    }
    
    /**
     * Gets the scaled effect value (e.g., scale * effectValue).
     */
    public float getScaledValue() {
        return switch (config.effect()) {
            case PULSE -> 1.0f + (config.scale() - 1.0f) * getEffectValue();
            case SHAKE -> config.amplitude() * getEffectValue();
            case GLOW -> config.intensity() * getEffectValue();
            case FLASH, COLOR_SHIFT -> getEffectValue();
        };
    }
    
    public TriggerConfig config() { return config; }
    public TriggerEffect effect() { return config.effect(); }
    public int remainingTicks() { return remainingTicks; }
    public boolean isActive() { return remainingTicks > 0; }
}
