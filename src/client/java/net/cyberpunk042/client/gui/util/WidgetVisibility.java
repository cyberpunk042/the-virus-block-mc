package net.cyberpunk042.client.gui.util;

import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * Tracks visibility conditions for individual widgets.
 * 
 * <p>Use this for control-level visibility based on renderer mode, permissions, etc.
 * Widgets registered with a visibility predicate can be refreshed when conditions change.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register a widget with a visibility condition
 * var slider = GuiWidgets.slider(...);
 * WidgetVisibility.register(slider, () -&gt; RendererCapabilities.isStandardMode());
 * 
 * // When renderer mode changes, refresh all widgets
 * WidgetVisibility.refreshAll();
 * </pre>
 * 
 * @see GuiWidgets#visibleWhen(ClickableWidget, BooleanSupplier)
 */
public final class WidgetVisibility {
    
    private WidgetVisibility() {}
    
    /**
     * Map of widgets to their visibility predicates.
     * Uses WeakHashMap so widgets can be garbage collected.
     */
    private static final Map<ClickableWidget, BooleanSupplier> registry = new WeakHashMap<>();
    
    /**
     * Register a widget with a visibility condition.
     * 
     * @param widget The widget to track
     * @param condition Predicate that returns true when widget should be visible
     */
    public static void register(ClickableWidget widget, BooleanSupplier condition) {
        if (widget != null && condition != null) {
            registry.put(widget, condition);
            // Apply immediately
            widget.visible = condition.getAsBoolean();
        }
    }
    
    /**
     * Unregister a widget.
     * 
     * @param widget The widget to stop tracking
     */
    public static void unregister(ClickableWidget widget) {
        if (widget != null) {
            registry.remove(widget);
        }
    }
    
    /**
     * Refresh a specific widget's visibility.
     * 
     * @param widget The widget to refresh
     */
    public static void refresh(ClickableWidget widget) {
        if (widget != null) {
            BooleanSupplier condition = registry.get(widget);
            if (condition != null) {
                widget.visible = condition.getAsBoolean();
            }
        }
    }
    
    /**
     * Refresh all registered widgets' visibility.
     * Call this when renderer mode changes, permissions change, etc.
     */
    public static void refreshAll() {
        for (Map.Entry<ClickableWidget, BooleanSupplier> entry : registry.entrySet()) {
            ClickableWidget widget = entry.getKey();
            BooleanSupplier condition = entry.getValue();
            if (widget != null && condition != null) {
                widget.visible = condition.getAsBoolean();
            }
        }
    }
    
    /**
     * Clear all registered widgets.
     * Call this when the screen is closed.
     */
    public static void clearAll() {
        registry.clear();
    }
    
    /**
     * Get the number of registered widgets.
     * Useful for debugging.
     */
    public static int size() {
        return registry.size();
    }
}
