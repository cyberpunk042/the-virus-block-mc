package net.cyberpunk042.client.gui.state;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in FieldEditState that maps directly to a FieldDefinition field.
 * 
 * <p>Used by {@link DefinitionBuilder} to automatically construct a FieldDefinition
 * from the current edit state using reflection.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * &#64;StateField &#64;DefinitionField("modifiers")
 * private Modifiers modifiers = Modifiers.DEFAULT;
 * 
 * &#64;StateField &#64;DefinitionField("beam")
 * private BeamConfig beam = BeamConfig.NONE;
 * </pre>
 * 
 * @see PrimitiveComponent
 * @see DefinitionBuilder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DefinitionField {
    /**
     * The path in FieldDefinition where this value goes.
     * 
     * <p>Examples: "modifiers", "beam", "prediction", "followMode"</p>
     */
    String value();
}


