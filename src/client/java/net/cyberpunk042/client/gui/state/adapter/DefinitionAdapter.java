package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.field.FieldDefinition;

import java.util.Set;

/**
 * Adapter for Definition-level fields.
 * 
 * <p>Handles fields that go directly on FieldDefinition rather than 
 * on individual Primitives (e.g., modifiers, beam, follow config).</p>
 * 
 * <h3>Example</h3>
 * <pre>
 * &#64;StateCategory(value = "modifiers", definitionLevel = true)
 * public class ModifiersAdapter implements DefinitionAdapter {
 *     private Modifiers modifiers = Modifiers.DEFAULT;
 *     
 *     &#64;Override
 *     public void loadFrom(FieldDefinition def) {
 *         this.modifiers = def.modifiers() != null ? def.modifiers() : Modifiers.DEFAULT;
 *     }
 *     
 *     &#64;Override
 *     public void applyTo(FieldDefinition.Builder builder) {
 *         builder.modifiers(modifiers);
 *     }
 * }
 * </pre>
 * 
 * @see StateCategory
 * @see PrimitiveAdapter
 */
public interface DefinitionAdapter {
    
    /**
     * The category name this adapter handles.
     * <p>Should match the @StateCategory value.</p>
     */
    String category();
    
    /**
     * Load this category's data from a FieldDefinition.
     * 
     * @param definition The definition to load from
     */
    void loadFrom(FieldDefinition definition);
    
    /**
     * Apply this category's data to a FieldDefinition builder.
     * 
     * @param builder The builder to populate
     */
    void applyTo(FieldDefinition.Builder builder);
    
    /**
     * Get a value by relative path.
     * 
     * @param path The relative path within this category
     * @return The value, or null if not found
     */
    Object get(String path);
    
    /**
     * Set a value by relative path.
     * 
     * @param path The relative path within this category
     * @param value The value to set
     */
    void set(String path, Object value);
    
    /**
     * Get all paths this adapter handles.
     */
    Set<String> paths();
    
    /**
     * Reset all fields to their default values.
     */
    void reset();
}
