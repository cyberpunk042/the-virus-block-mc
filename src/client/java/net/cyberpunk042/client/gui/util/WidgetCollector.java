package net.cyberpunk042.client.gui.util;

import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for collecting widgets from multiple providers (components, panels, etc.).
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In FieldCustomizerScreen.registerWidgets():
 * clearChildren();
 * List&lt;ClickableWidget&gt; widgets = WidgetCollector.collectVisible(
 *     headerBar,
 *     tabBar,
 *     currentTab != TabType.PROFILES ? selectorBar : null,
 *     currentTab == TabType.PROFILES ? profilesPanel : contentArea
 * );
 * widgets.forEach(this::addDrawableChild);
 * </pre>
 * 
 * @see WidgetProvider
 */
public final class WidgetCollector {
    
    private WidgetCollector() {}
    
    /**
     * Interface for components that provide widgets.
     * Implement this in panels, components, and other widget containers.
     */
    @FunctionalInterface
    public interface WidgetProvider {
        /**
         * Returns the list of widgets provided by this component.
         * @return List of clickable widgets (may be empty, never null)
         */
        List<ClickableWidget> getWidgets();
        
        /**
         * Returns whether this provider is currently visible.
         * Invisible providers' widgets will be skipped by collectVisible().
         * 
         * @return true if visible (default), false to hide all widgets
         */
        default boolean isVisible() {
            return true;
        }
    }
    
    /**
     * Collects widgets from all providers, regardless of visibility.
     * 
     * @param providers Widget providers (null providers are skipped)
     * @return Combined list of all widgets from all providers
     */
    public static List<ClickableWidget> collectAll(WidgetProvider... providers) {
        List<ClickableWidget> all = new ArrayList<>();
        for (WidgetProvider p : providers) {
            if (p != null) {
                List<ClickableWidget> widgets = p.getWidgets();
                if (widgets != null) {
                    all.addAll(widgets);
                }
            }
        }
        return all;
    }
    
    /**
     * Collects widgets from visible providers only.
     * 
     * <p>A provider is skipped if:
     * <ul>
     *   <li>It is null</li>
     *   <li>Its {@link WidgetProvider#isVisible()} returns false</li>
     * </ul>
     * 
     * <p>Additionally, individual widgets with {@code visible = false} are excluded.</p>
     * 
     * @param providers Widget providers
     * @return Combined list of visible widgets from visible providers
     */
    public static List<ClickableWidget> collectVisible(WidgetProvider... providers) {
        List<ClickableWidget> all = new ArrayList<>();
        for (WidgetProvider p : providers) {
            if (p != null && p.isVisible()) {
                List<ClickableWidget> widgets = p.getWidgets();
                if (widgets != null) {
                    for (ClickableWidget w : widgets) {
                        if (w != null && w.visible) {
                            all.add(w);
                        }
                    }
                }
            }
        }
        return all;
    }
    
    /**
     * Collects widgets from a list of providers.
     * 
     * @param providers List of widget providers
     * @return Combined list of all widgets
     */
    public static List<ClickableWidget> collectAll(List<? extends WidgetProvider> providers) {
        List<ClickableWidget> all = new ArrayList<>();
        for (WidgetProvider p : providers) {
            if (p != null) {
                List<ClickableWidget> widgets = p.getWidgets();
                if (widgets != null) {
                    all.addAll(widgets);
                }
            }
        }
        return all;
    }
    
    /**
     * Collects visible widgets from a list of providers.
     * 
     * @param providers List of widget providers
     * @return Combined list of visible widgets from visible providers
     */
    public static List<ClickableWidget> collectVisible(List<? extends WidgetProvider> providers) {
        List<ClickableWidget> all = new ArrayList<>();
        for (WidgetProvider p : providers) {
            if (p != null && p.isVisible()) {
                List<ClickableWidget> widgets = p.getWidgets();
                if (widgets != null) {
                    for (ClickableWidget w : widgets) {
                        if (w != null && w.visible) {
                            all.add(w);
                        }
                    }
                }
            }
        }
        return all;
    }
}
