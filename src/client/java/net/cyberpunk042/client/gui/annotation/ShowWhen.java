package net.cyberpunk042.client.gui.annotation;

import java.lang.annotation.*;

/**
 * Declares visibility conditions for a widget field.
 * 
 * <p>Multiple annotations are combined with OR logic - widget is visible
 * if ANY condition matches. Leave condition empty to ignore it.</p>
 * 
 * <p>Within a single annotation, all non-empty conditions must match (AND).</p>
 * 
 * <h3>Examples</h3>
 * <pre>
 * // Show when fill mode is CAGE or WIREFRAME (OR)
 * &#64;ShowWhen(fillMode = "CAGE")
 * &#64;ShowWhen(fillMode = "WIREFRAME")
 * private LabeledSlider wireThicknessSlider;
 * 
 * // Show when mask type is NOT "FULL"
 * &#64;ShowWhen(maskType = "FULL", not = true)
 * private LabeledSlider blendSlider;
 * 
 * // Show only for sphere shape in CAGE mode (AND within annotation)
 * &#64;ShowWhen(fillMode = "CAGE", shapeType = "sphere")
 * private LabeledSlider latitudeSlider;
 * </pre>
 * 
 * @see WidgetVisibilityResolver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ShowWhenConditions.class)
public @interface ShowWhen {
    
    /** Fill mode condition: SOLID, WIREFRAME, CAGE, POINTS */
    String fillMode() default "";
    
    /** Mask type condition: FULL, SPHERE, CYLINDER, etc. */
    String maskType() default "";
    
    /** Shape type condition: sphere, cylinder, torus, etc. */
    String shapeType() default "";
    
    /** Renderer mode condition: BASIC, INSTANCED */
    String rendererMode() default "";
    
    /** Negate the condition (show when NOT matching) */
    boolean not() default false;
}
