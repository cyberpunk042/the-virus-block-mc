package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.field.primitive.Primitive;

import java.util.Set;

/**
 * Adapter for a category of Primitive-level fields.
 * 
 * <p>Each adapter encapsulates related fields (like all shape records, 
 * or all animation configs) and knows how to sync with a Primitive.</p>
 * 
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li><b>loadFrom</b> - Extract this category's data from a Primitive</li>
 *   <li><b>saveTo</b> - Inject this category's data into a PrimitiveBuilder</li>
 *   <li><b>get/set</b> - Handle path-based access for GUI bindings</li>
 * </ul>
 * 
 * <h3>Example</h3>
 * <pre>
 * &#64;StateCategory("animation")
 * public class AnimationAdapter implements PrimitiveAdapter {
 *     private SpinConfig spin = SpinConfig.NONE;
 *     private PulseConfig pulse = PulseConfig.NONE;
 *     
 *     &#64;Override
 *     public void loadFrom(Primitive source) {
 *         Animation anim = source.animation();
 *         this.spin = anim != null ? anim.spin() : SpinConfig.NONE;
 *         // ...
 *     }
 *     
 *     &#64;Override
 *     public void saveTo(PrimitiveBuilder builder) {
 *         builder.animation(Animation.builder().spin(spin).build());
 *     }
 * }
 * </pre>
 * 
 * @see StateCategory
 * @see DefinitionAdapter
 * @see PrimitiveBuilder
 */
public interface PrimitiveAdapter {
    
    /**
     * The category name this adapter handles.
     * <p>Should match the @StateCategory value.</p>
     */
    String category();
    
    /**
     * Load this category's data from a Primitive.
     * 
     * @param source The primitive to load from (never null)
     */
    void loadFrom(Primitive source);
    
    /**
     * Save this category's data to a PrimitiveBuilder.
     * 
     * @param builder The builder to populate
     */
    void saveTo(PrimitiveBuilder builder);
    
    /**
     * Get a value by relative path.
     * <p>For example, for AnimationAdapter, path "spin.speed" returns the spin speed.</p>
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
     * <p>Used for introspection and debugging.</p>
     */
    Set<String> paths();
    
    /**
     * Reset all fields to their default values.
     */
    void reset();
}
