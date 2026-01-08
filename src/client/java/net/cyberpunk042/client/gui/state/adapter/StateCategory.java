package net.cyberpunk042.client.gui.state.adapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a State Adapter for a specific category.
 * 
 * <p>The category name should match the {@code @PrimitiveComponent} or 
 * {@code @DefinitionField} value that this adapter handles.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * &#64;StateCategory("shape")
 * public class ShapeAdapter implements PrimitiveAdapter { ... }
 * 
 * &#64;StateCategory(value = "modifiers", definitionLevel = true)
 * public class ModifiersAdapter implements DefinitionAdapter { ... }
 * </pre>
 * 
 * @see PrimitiveAdapter
 * @see DefinitionAdapter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StateCategory {
    
    /**
     * The category name.
     * <p>Should match @PrimitiveComponent or @DefinitionField values.</p>
     */
    String value();
    
    /**
     * If true, this adapter handles @DefinitionField (definition-level).
     * If false (default), handles @PrimitiveComponent (primitive-level).
     */
    boolean definitionLevel() default false;
}
