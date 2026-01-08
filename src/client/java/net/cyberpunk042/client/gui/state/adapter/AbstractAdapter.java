package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateAccessor;
import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.log.Logging;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for adapters, providing common reflection-based functionality.
 * 
 * <p>Uses {@link StateAccessor} for path-based get/set, and scans for 
 * {@link StateField} annotations to build the paths set.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * &#64;StateCategory("fill")
 * public class FillAdapter extends AbstractAdapter implements PrimitiveAdapter {
 *     &#64;StateField private FillConfig fill = FillConfig.SOLID;
 *     
 *     &#64;Override public String category() { return "fill"; }
 *     &#64;Override public void loadFrom(Primitive source) { ... }
 *     &#64;Override public void saveTo(SimplePrimitive.Builder builder) { ... }
 *     // get/set/paths/reset inherited from AbstractAdapter
 * }
 * </pre>
 */
public abstract class AbstractAdapter {
    
    // Cached paths for this adapter
    private Set<String> cachedPaths;
    
    /**
     * Get a value by path using reflection.
     */
    public Object get(String path) {
        try {
            return StateAccessor.get(this, path);
        } catch (Exception e) {
            Logging.GUI.topic("adapter").warn("Failed to get '{}' from {}: {}", 
                path, getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Set a value by path using reflection.
     */
    public void set(String path, Object value) {
        try {
            StateAccessor.set(this, path, value);
        } catch (Exception e) {
            Logging.GUI.topic("adapter").warn("Failed to set '{}' on {}: {}", 
                path, getClass().getSimpleName(), e.getMessage());
        }
    }
    
    /**
     * Get all paths this adapter handles by scanning @StateField annotations.
     */
    public Set<String> paths() {
        if (cachedPaths == null) {
            cachedPaths = new HashSet<>();
            collectPaths("", getClass());
        }
        return cachedPaths;
    }
    
    private void collectPaths(String prefix, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(StateField.class)) {
                String fieldName = field.getName();
                String path = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
                cachedPaths.add(path);
                
                // For record types, also add nested paths
                if (field.getType().isRecord()) {
                    addRecordPaths(path, field.getType());
                }
            }
        }
    }
    
    private void addRecordPaths(String prefix, Class<?> recordClass) {
        for (var component : recordClass.getRecordComponents()) {
            String path = prefix + "." + component.getName();
            cachedPaths.add(path);
            
            // Recurse for nested records
            if (component.getType().isRecord()) {
                addRecordPaths(path, component.getType());
            }
        }
    }
    
    /**
     * Reset all @StateField fields to their default values.
     * <p>Subclasses should override to provide proper defaults.</p>
     */
    public abstract void reset();
    
    /**
     * Helper to get a value with a default if null.
     */
    protected static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
