package net.cyberpunk042.client.gui.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link ShowWhen} annotations.
 * 
 * <p>This is automatically used by Java when multiple @ShowWhen
 * annotations are placed on the same field.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ShowWhenConditions {
    ShowWhen[] value();
}
