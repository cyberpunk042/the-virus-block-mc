package net.cyberpunk042.field.influence;

import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;

/**
 * Resolves binding values at runtime.
 * 
 * <p>Per ARCHITECTURE §12.1:
 * <ol>
 *   <li>Gets raw value from BindingSource</li>
 *   <li>Normalizes to inputRange</li>
 *   <li>Applies interpolation curve</li>
 *   <li>Maps to outputRange</li>
 * </ol>
 * 
 * @see BindingConfig
 * @see BindingSource
 */
public final class BindingResolver {
    
    private BindingResolver() {}
    
    /**
     * Evaluates a single binding.
     * 
     * @param config Binding configuration
     * @param player Player to read source from
     * @param fieldAge Field age in ticks (for field.age source)
     * @return Resolved output value
     */
    public static float evaluate(BindingConfig config, PlayerEntity player, int fieldAge) {
        // 1. Get source
        BindingSource source = BindingSources.getOrWarn(config.source());
        if (source == null) {
            return config.outputMin(); // Default to min on unknown source
        }
        
        // Special handling for field.age
        float rawValue;
        if ("field.age".equals(config.source())) {
            rawValue = fieldAge;
        } else {
            rawValue = source.getValue(player);
        }
        
        // 2. Normalize to 0-1 based on input range
        float normalized;
        if (config.inputMax() == config.inputMin()) {
            normalized = 0;
        } else {
            normalized = (rawValue - config.inputMin()) / (config.inputMax() - config.inputMin());
        }
        normalized = Math.max(0, Math.min(1, normalized)); // Clamp
        
        // 3. Apply curve
        float curved = config.curve().apply(normalized);
        
        // 4. Map to output range
        float output = config.outputMin() + curved * (config.outputMax() - config.outputMin());
        
        Logging.FIELD.topic("binding").trace(
            "Evaluated {}: raw={}, normalized={}, curved={}, output={}",
            config.source(), rawValue, normalized, curved, output);
        
        return output;
    }
    
    /**
     * Evaluates all bindings in a map.
     * 
     * @param bindings Map of property path → BindingConfig
     * @param player Player to read from
     * @param fieldAge Field age in ticks
     * @return Map of property path → resolved value
     */
    public static Map<String, Float> evaluateAll(
            Map<String, BindingConfig> bindings, 
            PlayerEntity player, 
            int fieldAge) {
        
        Map<String, Float> results = new java.util.HashMap<>();
        
        for (Map.Entry<String, BindingConfig> entry : bindings.entrySet()) {
            float value = evaluate(entry.getValue(), player, fieldAge);
            results.put(entry.getKey(), value);
        }
        
        return results;
    }
    
    /**
     * Gets a specific bound value, or default if not bound.
     * 
     * @param bindings All bindings
     * @param propertyPath Property path (e.g., "alpha")
     * @param player Player
     * @param fieldAge Field age
     * @param defaultValue Value if not bound
     * @return Bound value or default
     */
    public static float getOrDefault(
            Map<String, BindingConfig> bindings,
            String propertyPath,
            PlayerEntity player,
            int fieldAge,
            float defaultValue) {
        
        BindingConfig config = bindings.get(propertyPath);
        if (config == null) {
            return defaultValue;
        }
        return evaluate(config, player, fieldAge);
    }
}
