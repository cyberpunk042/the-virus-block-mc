package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.util.WidgetCollector;
import net.minecraft.client.gui.DrawContext;

/**
 * Interface for self-contained GUI components that manage their own widgets.
 * 
 * <p>Components encapsulate a group of related widgets and their rendering logic.
 * They participate in automatic widget collection via {@link WidgetCollector}.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * public class HeaderBar implements ScreenComponent {
 *     private List&lt;ClickableWidget&gt; widgets = new ArrayList&lt;&gt;();
 *     
 *     public HeaderBar(Bounds bounds, ...) {
 *         setBounds(bounds);
 *         // Create widgets...
 *     }
 *     
 *     &#64;Override
 *     public void setBounds(Bounds bounds) {
 *         this.bounds = bounds;
 *         // Reposition widgets...
 *     }
 *     
 *     &#64;Override
 *     public List&lt;ClickableWidget&gt; getWidgets() {
 *         return widgets;
 *     }
 *     
 *     &#64;Override
 *     public void render(DrawContext context, int mouseX, int mouseY, float delta) {
 *         // Custom rendering (labels, backgrounds, etc.)
 *     }
 * }
 * </pre>
 * 
 * @see WidgetCollector
 * @see VisibilityController
 */
public interface ScreenComponent extends WidgetCollector.WidgetProvider {
    
    /**
     * Sets the bounds for this component.
     * Components should reposition their widgets when bounds change.
     * 
     * @param bounds The new bounds
     */
    void setBounds(Bounds bounds);
    
    /**
     * Renders the component.
     * This is called after widgets are rendered and is for custom drawing
     * like labels, backgrounds, overlays, etc.
     * 
     * @param context Draw context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Frame delta time
     */
    void render(DrawContext context, int mouseX, int mouseY, float delta);
    
    /**
     * Called every tick (20 times per second).
     * Override for animations or periodic updates.
     */
    default void tick() {}
    
    /**
     * Returns whether this component is currently visible.
     * When false, the component's widgets will be excluded from registration.
     * 
     * @return true if visible, false to hide
     */
    @Override
    default boolean isVisible() {
        return true;
    }
    
    /**
     * Returns a tooltip explaining why this component is hidden.
     * Only applicable when {@link #isVisible()} returns false.
     * 
     * @return Tooltip text, or null if no explanation needed
     */
    default String getHiddenTooltip() {
        return null;
    }
    
    /**
     * Called when the component should reinitialize its widgets.
     * Used when layout changes, visibility changes, etc.
     */
    default void rebuild() {}
}
