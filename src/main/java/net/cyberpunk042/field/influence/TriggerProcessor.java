package net.cyberpunk042.field.influence;

import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Processes trigger events and manages active triggers.
 * 
 * <p>Per ARCHITECTURE §12.2:
 * <ul>
 *   <li>Listens for game events</li>
 *   <li>Creates ActiveTriggers when events match</li>
 *   <li>Ticks active triggers each frame</li>
 *   <li>Provides current effect values to renderer</li>
 * </ul>
 * 
 * <p>F154: Active triggers override bindings for the same property.
 */
public class TriggerProcessor {
    
    private final List<TriggerConfig> triggers;
    private final List<ActiveTrigger> activeTriggers = new ArrayList<>();
    
    public TriggerProcessor(List<TriggerConfig> triggers) {
        this.triggers = triggers != null ? triggers : List.of();
    }
    
    /**
     * Fires an event, creating active triggers for any matching configs.
     */
    public void fireEvent(FieldEvent event) {
        try (LogScope scope = Logging.FIELD.topic("trigger").scope("process-triggers", LogLevel.DEBUG)) {
            for (TriggerConfig config : triggers) {
                if (config.event() == event) {
                    activeTriggers.add(new ActiveTrigger(config));
                    scope.branch("config").kv("event", event).kv("effect", config.effect());
                }
            }
        }
    }
    
    /**
     * Ticks all active triggers, removing completed ones.
     */
    public void tick() {
        activeTriggers.removeIf(t -> !t.tick());
    }
    
    /**
     * Checks if any triggers are currently active.
     */
    public boolean hasActiveTriggers() {
        return !activeTriggers.isEmpty();
    }
    
    /**
     * Gets all currently active triggers.
     */
    public List<ActiveTrigger> getActiveTriggers() {
        return Collections.unmodifiableList(activeTriggers);
    }
    
    /**
     * Gets active triggers of a specific effect type.
     */
    public List<ActiveTrigger> getActiveTriggers(TriggerEffect effect) {
        return activeTriggers.stream()
            .filter(t -> t.effect() == effect)
            .toList();
    }
    
    /**
     * F154: Gets the combined effect value for an effect type.
     * Multiple triggers of same type are combined (max value).
     */
    public float getEffectValue(TriggerEffect effect) {
        return activeTriggers.stream()
            .filter(t -> t.effect() == effect)
            .map(ActiveTrigger::getScaledValue)
            .max(Float::compare)
            .orElse(0f);
    }
    
    /**
     * F154: Whether a trigger is currently overriding a property.
     */
    public boolean isOverriding(String property) {
        // Map properties to effects that override them
        TriggerEffect overridingEffect = switch (property) {
            case "scale", "transform.scale" -> TriggerEffect.PULSE;
            case "glow", "appearance.glow" -> TriggerEffect.GLOW;
            case "color", "appearance.color" -> TriggerEffect.COLOR_SHIFT;
            default -> null;
        };
        
        if (overridingEffect == null) return false;
        
        return activeTriggers.stream()
            .anyMatch(t -> t.effect() == overridingEffect && t.isActive());
    }
    
    /**
     * Gets the shake offset for this tick (random jitter).
     */
    public float[] getShakeOffset() {
        float shakeAmount = getEffectValue(TriggerEffect.SHAKE);
        if (shakeAmount <= 0) return new float[]{0, 0, 0};
        
        // Random offset within amplitude
        return new float[]{
            (float) (Math.random() - 0.5) * 2 * shakeAmount,
            (float) (Math.random() - 0.5) * 2 * shakeAmount,
            (float) (Math.random() - 0.5) * 2 * shakeAmount
        };
    }
    
    /**
     * Gets the flash color if FLASH is active.
     */
    public String getFlashColor() {
        for (ActiveTrigger trigger : activeTriggers) {
            if (trigger.effect() == TriggerEffect.FLASH && trigger.config().color() != null) {
                return trigger.config().color();
            }
        }
        return null;
    }
    
    /**
     * Gets flash intensity (0-1).
     */
    public float getFlashIntensity() {
        return getEffectValue(TriggerEffect.FLASH);
    }
}
