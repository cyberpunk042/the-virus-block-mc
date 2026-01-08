package net.cyberpunk042.client.gui.visibility;

import net.cyberpunk042.client.gui.annotation.ShowWhen;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.log.Logging;

import java.lang.reflect.Field;

/**
 * Evaluates {@link ShowWhen} annotations to determine widget visibility.
 * 
 * <p>Uses reflection to read annotations and match against current state.
 * This is called once per widget rebuild, not per frame, so reflection
 * overhead is negligible.</p>
 * 
 * <h3>Visibility Logic</h3>
 * <ul>
 *   <li>No annotations = always visible</li>
 *   <li>Multiple annotations = OR logic (visible if ANY matches)</li>
 *   <li>Multiple conditions in one annotation = AND logic (all must match)</li>
 *   <li>{@code not = true} = negate the result</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>
 * for (Field field : getClass().getDeclaredFields()) {
 *     if (WidgetVisibilityResolver.shouldShow(field, state)) {
 *         widgets.add((ClickableWidget) field.get(this));
 *     }
 * }
 * </pre>
 * 
 * @see ShowWhen
 */
public class WidgetVisibilityResolver {
    
    /**
     * Determines if a field's widget should be visible based on @ShowWhen annotations.
     * 
     * @param field The field with potential @ShowWhen annotations
     * @param state Current field edit state
     * @return true if widget should be shown
     */
    public static boolean shouldShow(Field field, FieldEditState state) {
        ShowWhen[] conditions = field.getAnnotationsByType(ShowWhen.class);
        
        // No annotations = always visible
        if (conditions.length == 0) {
            return true;
        }
        
        // OR logic: visible if ANY condition matches
        for (ShowWhen condition : conditions) {
            boolean matches = matchesAllConditions(condition, state);
            
            // Apply negation
            if (condition.not()) {
                matches = !matches;
            }
            
            if (matches) {
                Logging.GUI.topic("visibility").trace(
                    "Field {} visible: matched condition", field.getName());
                return true;
            }
        }
        
        Logging.GUI.topic("visibility").trace(
            "Field {} hidden: no conditions matched", field.getName());
        return false; // No conditions matched
    }
    
    /**
     * Checks if all non-empty conditions in a single @ShowWhen match (AND logic).
     */
    private static boolean matchesAllConditions(ShowWhen condition, FieldEditState state) {
        // Fill mode check
        if (!condition.fillMode().isEmpty()) {
            String current = state.fill().mode().name();
            if (!current.equalsIgnoreCase(condition.fillMode())) {
                return false;
            }
        }
        
        // Mask type check
        if (!condition.maskType().isEmpty()) {
            String current = "FULL";
            if (state.mask() != null && state.mask().mask() != null) {
                current = state.mask().mask().name();
            }
            if (!current.equalsIgnoreCase(condition.maskType())) {
                return false;
            }
        }
        
        // Shape type check
        if (!condition.shapeType().isEmpty()) {
            String current = state.getString("shapeType");
            if (!current.equalsIgnoreCase(condition.shapeType())) {
                return false;
            }
        }
        
        // Renderer mode check
        if (!condition.rendererMode().isEmpty()) {
            String current = state.getString("rendererMode");
            if (current == null) current = "BASIC";
            if (!current.equalsIgnoreCase(condition.rendererMode())) {
                return false;
            }
        }
        
        return true; // All specified conditions matched
    }
    
    /**
     * Convenience method to check visibility for a field by name.
     * 
     * @param clazz The class containing the field
     * @param fieldName The field name
     * @param state Current state
     * @return true if visible, or true if field not found (fail-open)
     */
    public static boolean shouldShow(Class<?> clazz, String fieldName, FieldEditState state) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return shouldShow(field, state);
        } catch (NoSuchFieldException e) {
            Logging.GUI.topic("visibility").warn(
                "Field {} not found in {}, defaulting to visible", fieldName, clazz.getSimpleName());
            return true; // Fail open - show if we can't find the field
        }
    }
}
