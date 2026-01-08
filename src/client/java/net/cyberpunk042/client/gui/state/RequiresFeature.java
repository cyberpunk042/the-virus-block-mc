package net.cyberpunk042.client.gui.state;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a panel or sub-panel as requiring specific renderer features.
 * 
 * <p>When the current renderer mode doesn't support the required features,
 * the panel can be automatically hidden or shown with a "requires Accurate mode" 
 * indicator.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * &#64;RequiresFeature(RendererCapabilities.Feature.BINDINGS)
 * public class BindingsSubPanel extends AbstractPanel { ... }
 * 
 * &#64;RequiresFeature({Feature.WAVE, Feature.WOBBLE})
 * public class AdvancedAnimationPanel extends AbstractPanel { ... }
 * </pre>
 * 
 * @see RendererCapabilities
 * @see RendererCapabilities.Feature
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresFeature {
    /**
     * The feature(s) required by this panel.
     * Panel is hidden/disabled if ANY of these features is unsupported.
     */
    RendererCapabilities.Feature[] value();
    
    /**
     * If true, requires ALL features. If false, requires ANY feature.
     * Default is false (ANY).
     */
    boolean requireAll() default false;
}


