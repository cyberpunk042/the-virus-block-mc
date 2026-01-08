package net.cyberpunk042.client.gui.state;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as part of the editable GUI state.
 * Used by StateAccessor for reflection-based access and dirty tracking.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StateField {
    /** Optional name override for JSON/lookup. Defaults to field name. */
    String name() default "";
}

