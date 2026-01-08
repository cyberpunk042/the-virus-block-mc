package net.cyberpunk042.client.gui.state;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in FieldEditState that belongs to a Primitive within a Layer.
 * 
 * <p>Used by {@link DefinitionBuilder} to construct Primitive objects that go
 * into FieldLayers within the FieldDefinition.</p>
 * 
 * <h3>Mapping</h3>
 * <pre>
 * FieldDefinition
 *   └── layers[selectedLayer]
 *         └── primitives[selectedPrimitive]
 *               ├── shape      ← &#64;PrimitiveComponent("shape")
 *               ├── fill       ← &#64;PrimitiveComponent("fill")
 *               ├── transform  ← &#64;PrimitiveComponent("transform")
 *               ├── appearance ← &#64;PrimitiveComponent("appearance")
 *               └── animation  ← &#64;PrimitiveComponent("animation")
 * </pre>
 * 
 * <h3>Usage</h3>
 * <pre>
 * &#64;StateField &#64;PrimitiveComponent("fill")
 * private FillConfig fill = FillConfig.SOLID;
 * 
 * &#64;StateField &#64;PrimitiveComponent("transform")
 * private Transform transform = Transform.IDENTITY;
 * </pre>
 * 
 * @see DefinitionField
 * @see DefinitionBuilder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrimitiveComponent {
    /**
     * The field name within the Primitive where this value goes.
     * 
     * <p>Valid values: "shape", "fill", "transform", "appearance", 
     * "animation", "mask", "visibility"</p>
     */
    String value();
    
    /**
     * If true, this component is shape-type-specific.
     * Only include if shapeType matches the field name prefix.
     * 
     * <p>Example: sphereShape is only included if shapeType == "sphere"</p>
     */
    boolean shapeSpecific() default false;
}


