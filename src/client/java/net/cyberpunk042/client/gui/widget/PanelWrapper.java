package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

/**
 * Adapts an {@link AbstractPanel} to be used as a {@link SubTabPane.ContentProvider}.
 * 
 * <p>Handles initialization, bounds positioning, rendering with scissoring, and
 * scroll delegation to the wrapped panel.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * SubTabPane subTabs = new SubTabPane(...);
 * subTabs.addTab("Fill", new PanelWrapper(new FillSubPanel(screen, state)));
 * </pre>
 */
public class PanelWrapper implements SubTabPane.ContentProvider {
    
    private final AbstractPanel panel;
    private boolean initialized = false;
    private Bounds currentBounds;
    
    /**
     * Creates a wrapper for the given panel.
     * 
     * @param panel The panel to wrap
     */
    public PanelWrapper(AbstractPanel panel) {
        this.panel = panel;
    }
    
    @Override
    public void setBounds(Bounds bounds) {
        // Initialize the panel with dimensions first (creates widgets at 0,0)
        if (!initialized) {
            panel.init(bounds.width(), bounds.height());
            initialized = true;
        }
        
        // If bounds changed, we need to reposition widgets (labels are now widgets too)
        if (currentBounds == null || !currentBounds.equals(bounds)) {
            // Reset widgets to origin then apply new offset
            if (currentBounds != null) {
                panel.offsetWidgets(-currentBounds.x(), -currentBounds.y());
            }
            panel.offsetWidgets(bounds.x(), bounds.y());
            currentBounds = bounds;
        }
        
        // Use setBoundsQuiet to avoid triggering reflow() which would clearWidgets()
        panel.setBoundsQuiet(bounds);
    }
    
    @Override
    public List<ClickableWidget> getWidgets() {
        // Widgets are now at correct screen positions
        return initialized ? panel.getWidgets() : List.of();
    }
    
    @Override
    public void tick() {
        panel.tick();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized || currentBounds == null) return;
        
        // Scissor to bounds to prevent overflow
        context.enableScissor(
            currentBounds.x(), 
            currentBounds.y(), 
            currentBounds.right(), 
            currentBounds.bottom()
        );
        
        // Render panel with scroll handling - this is the ONLY render path for widgets
        // Widgets are registered with Screen as selectables (for input) but NOT drawables
        panel.render(context, mouseX, mouseY, delta);
        
        context.disableScissor();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (currentBounds != null && currentBounds.contains(mouseX, mouseY)) {
            return panel.mouseScrolled(mouseX, mouseY, h, v);
        }
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Widgets are at visual positions - Screen handles input via registered children
        return false;
    }
    
    @Override
    public boolean isFeatureSupported() {
        return panel.isFeatureSupported();
    }
    
    @Override
    public String getDisabledTooltip() {
        return panel.getDisabledTooltip();
    }
    
    /**
     * Returns the wrapped panel.
     */
    public AbstractPanel getPanel() {
        return panel;
    }
    
    /**
     * Returns whether the panel has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Returns the current bounds.
     */
    public Bounds getCurrentBounds() {
        return currentBounds;
    }
}
