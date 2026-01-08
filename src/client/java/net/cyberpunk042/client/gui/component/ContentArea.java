package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.screen.TabType;
import net.cyberpunk042.client.gui.util.GuiConfigPersistence;
import net.cyberpunk042.client.gui.widget.SubTabPane;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Content area component that manages the main content display.
 * 
 * <p>Contains sub-tab panes for each main tab:</p>
 * <ul>
 *   <li><b>Quick:</b> Fill, Appearance, Visibility, Transform</li>
 *   <li><b>Advanced:</b> Animation, Prediction, Linking, Modifiers</li>
 *   <li><b>Debug:</b> Beam, Trigger, Lifecycle, Bindings, Trace</li>
 *   <li><b>FX:</b> Shockwave (visual effects)</li>
 * </ul>
 * 
 * <p>The Profiles tab uses a separate panel and is handled by the main screen.</p>
 */
public class ContentArea implements ScreenComponent {
    
    private Bounds bounds;
    private final TextRenderer textRenderer;
    private final VisibilityController visibility;
    private final Runnable onSubTabChange;
    
    // Sub-tab panes (one per main tab)
    private SubTabPane quickSubTabs;
    private SubTabPane advancedSubTabs;
    private SubTabPane debugSubTabs;
    private SubTabPane fxSubTabs;
    
    // Content provider factory
    private final ContentProviderFactory contentFactory;
    
    // Current state
    private TabType currentMainTab = TabType.QUICK;
    
    /**
     * Creates a content area.
     * 
     * @param textRenderer Font renderer
     * @param visibility Visibility controller
     * @param contentFactory Factory for creating sub-tab content
     * @param onSubTabChange Called when a sub-tab changes
     */
    public ContentArea(
            TextRenderer textRenderer,
            VisibilityController visibility,
            ContentProviderFactory contentFactory,
            Runnable onSubTabChange) {
        
        this.textRenderer = textRenderer;
        this.visibility = visibility;
        this.contentFactory = contentFactory;
        this.onSubTabChange = onSubTabChange;
    }
    
    @Override
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        rebuild();
    }
    
    @Override
    public void rebuild() {
        if (bounds == null || contentFactory == null) return;
        
        initQuickSubTabs();
        initAdvancedSubTabs();
        initDebugSubTabs();
        initFxSubTabs();
    }
    
    private void initQuickSubTabs() {
        int savedSubtab = GuiConfigPersistence.loadSavedSubtab(TabType.QUICK);
        quickSubTabs = new SubTabPane(textRenderer)
            .addTab("Fill", contentFactory.fill())
            .addTab("Appear", contentFactory.appearance())
            .addTab("Visibility", contentFactory.visibility())
            .addTab("Xfm", contentFactory.transform())
            .onTabChange(idx -> {
                GuiConfigPersistence.saveSubtab(TabType.QUICK, idx);
                onSubTabChange.run();
            });
        quickSubTabs.setBounds(bounds);
        quickSubTabs.setActiveTab(savedSubtab);
    }
    
    private void initAdvancedSubTabs() {
        int savedSubtab = GuiConfigPersistence.loadSavedSubtab(TabType.ADVANCED);
        advancedSubTabs = new SubTabPane(textRenderer)
            .addTab("Predict", contentFactory.prediction())
            .addTab("Linking", contentFactory.linking())
            .addTab("Mods", contentFactory.modifiers())
            .addTab("Trigger", contentFactory.trigger())
            .onTabChange(idx -> {
                GuiConfigPersistence.saveSubtab(TabType.ADVANCED, idx);
                onSubTabChange.run();
            });
        advancedSubTabs.setBounds(bounds);
        advancedSubTabs.setActiveTab(savedSubtab);
    }
    
    private void initDebugSubTabs() {
        int savedSubtab = GuiConfigPersistence.loadSavedSubtab(TabType.DEBUG);
        debugSubTabs = new SubTabPane(textRenderer)
            .addTab("Beam", contentFactory.beam())
            .addTab("Force", contentFactory.force())
            .addTab("Life", contentFactory.lifecycle())
            .addTab("Bindings", contentFactory.bindings())
            .addTab("Trace", contentFactory.trace())
            .onTabChange(idx -> {
                GuiConfigPersistence.saveSubtab(TabType.DEBUG, idx);
                onSubTabChange.run();
            });
        debugSubTabs.setBounds(bounds);
        debugSubTabs.setActiveTab(savedSubtab);
    }
    
    private void initFxSubTabs() {
        int savedSubtab = GuiConfigPersistence.loadSavedSubtab(TabType.FX);
        fxSubTabs = new SubTabPane(textRenderer)
            .addTab("Shockwave", contentFactory.shockwave())
            .addTab("Field Visual", contentFactory.fieldVisual())
            .addTab("Magic Circle", contentFactory.magicCircle())
            .onTabChange(idx -> {
                GuiConfigPersistence.saveSubtab(TabType.FX, idx);
                onSubTabChange.run();
            });
        fxSubTabs.setBounds(bounds);
        fxSubTabs.setActiveTab(savedSubtab);
    }
    
    /**
     * Sets the active main tab and switches content.
     */
    public void setActiveMainTab(TabType tab) {
        this.currentMainTab = tab;
    }
    
    /**
     * Returns the active sub-tab pane for the current main tab.
     */
    public SubTabPane getActiveSubTabPane() {
        return switch (currentMainTab) {
            case QUICK -> quickSubTabs;
            case ADVANCED -> advancedSubTabs;
            case DEBUG -> debugSubTabs;
            case FX -> fxSubTabs;
            case PROFILES -> null; // Profiles uses separate panel
        };
    }
    
    @Override
    public List<ClickableWidget> getWidgets() {
        List<ClickableWidget> all = new ArrayList<>();
        SubTabPane active = getActiveSubTabPane();
        if (active != null) {
            // Tab buttons (for tab switching)
            all.addAll(active.getWidgets());
            // Content panel widgets (sliders, toggles, etc.)
            all.addAll(active.getContentWidgets());
        }
        return all;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        SubTabPane active = getActiveSubTabPane();
        if (active != null) {
            active.render(context, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void tick() {
        SubTabPane active = getActiveSubTabPane();
        if (active != null) {
            active.tick();
        }
    }
    
    @Override
    public boolean isVisible() {
        // Not visible when on Profiles tab
        return currentMainTab != TabType.PROFILES;
    }
    
    /**
     * Handles mouse scroll for the active sub-tab.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        SubTabPane active = getActiveSubTabPane();
        if (active != null) {
            return active.mouseScrolled(mouseX, mouseY, h, v);
        }
        return false;
    }
    
    /**
     * Mouse clicks are handled by Screen since widgets are at visual positions.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    /**
     * Refreshes for renderer mode changes.
     * Rebuilds sub-tabs to update visibility of mode-dependent panels.
     */
    public void refreshForRendererMode() {
        // Re-set bounds triggers rebuild of the sub-tab pane
        if (bounds != null) {
            rebuild();
        }
    }
    
    /**
     * Returns the Quick sub-tabs pane.
     */
    public SubTabPane getQuickSubTabs() {
        return quickSubTabs;
    }
    
    /**
     * Returns the Advanced sub-tabs pane.
     */
    public SubTabPane getAdvancedSubTabs() {
        return advancedSubTabs;
    }
    
    /**
     * Returns the Debug sub-tabs pane.
     */
    public SubTabPane getDebugSubTabs() {
        return debugSubTabs;
    }
    
    /**
     * Returns the FX sub-tabs pane.
     */
    public SubTabPane getFxSubTabs() {
        return fxSubTabs;
    }
}
