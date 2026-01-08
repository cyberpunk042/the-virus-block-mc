package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Nested tab container with horizontal tab buttons and content area.
 * 
 * <pre>
 * [Fill] [Appear] [Visibility]
 * ├─────────────────────────────┤
 * │  (active tab content)       │
 * │                             │
 * └─────────────────────────────┘
 * </pre>
 */
public class SubTabPane {
    
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_GAP = 2;
    
    private final TextRenderer textRenderer;
    private final List<TabEntry> tabs = new ArrayList<>();
    private int activeIndex = 0;
    
    private Bounds bounds = Bounds.EMPTY;
    private Bounds tabBarBounds = Bounds.EMPTY;
    private Bounds contentBounds = Bounds.EMPTY;
    
    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    private Consumer<Integer> onTabChange;
    
    public SubTabPane(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Adds a tab with label and content provider.
     */
    public SubTabPane addTab(String label, ContentProvider content) {
        tabs.add(new TabEntry(label, content));
        return this;
    }
    
    /**
     * Adds a tab with an AbstractPanel as content.
     */
    public SubTabPane addTab(String label, AbstractPanel panel) {
        tabs.add(new TabEntry(label, new PanelContentProvider(panel)));
        return this;
    }
    
    /**
     * Adds a tab with a LayoutPanel (v2) as content.
     */
    public SubTabPane addTab(String label, LayoutPanel panel) {
        tabs.add(new TabEntry(label, new LayoutPanelContentProvider(panel)));
        return this;
    }
    
    public SubTabPane onTabChange(Consumer<Integer> callback) {
        this.onTabChange = callback;
        return this;
    }
    
    public SubTabPane setActiveTab(int index) {
        if (index >= 0 && index < tabs.size() && index != activeIndex) {
            activeIndex = index;
            updateTabButtons();
            updateContentBounds();
            if (onTabChange != null) {
                onTabChange.accept(index);
            }
        }
        return this;
    }
    
    public int getActiveTab() {
        return activeIndex;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        
        // Tab bar at top
        tabBarBounds = bounds.sliceTop(TAB_HEIGHT);
        
        // Content below
        contentBounds = bounds.withoutTop(TAB_HEIGHT + 2);
        
        rebuildWidgets();
    }
    
    public Bounds getContentBounds() {
        return contentBounds;
    }
    
    /**
     * Refreshes tab states (e.g., after renderer mode change).
     * Call this when feature availability might have changed.
     */
    public void refreshTabs() {
        rebuildWidgets();
    }
    
    private void rebuildWidgets() {
        tabButtons.clear();
        
        if (bounds.isEmpty() || tabs.isEmpty()) return;
        
        // Count supported tabs for width calculation
        int supportedCount = 0;
        for (TabEntry tab : tabs) {
            if (tab.content.isFeatureSupported()) {
                supportedCount++;
            }
        }
        
        if (supportedCount == 0) return; // All tabs hidden
        
        // Calculate tab widths based on VISIBLE tabs only
        int totalGaps = (supportedCount - 1) * TAB_GAP;
        int availableWidth = tabBarBounds.width() - totalGaps;
        int tabWidth = availableWidth / supportedCount;
        
        int x = tabBarBounds.x();
        int y = tabBarBounds.y();
        
        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            TabEntry tab = tabs.get(i);
            
            // Skip unsupported tabs entirely (HIDE them)
            if (!tab.content.isFeatureSupported()) {
                // Add null placeholder to keep indices aligned
                tabButtons.add(null);
                continue;
            }
            
            ButtonWidget btn = ButtonWidget.builder(Text.literal(tab.label), b -> setActiveTab(tabIndex))
                .dimensions(x, y, tabWidth, TAB_HEIGHT)
                .build();
            
            tabButtons.add(btn);
            x += tabWidth + TAB_GAP;
        }
        
        // If active tab is now hidden, switch to first visible tab
        if (tabButtons.get(activeIndex) == null) {
            for (int i = 0; i < tabButtons.size(); i++) {
                if (tabButtons.get(i) != null) {
                    activeIndex = i;
                    break;
                }
            }
        }
        
        updateTabButtons();
        updateContentBounds();
    }
    
    private void updateTabButtons() {
        for (int i = 0; i < tabButtons.size(); i++) {
            ButtonWidget btn = tabButtons.get(i);
            if (btn != null) {
                btn.active = i != activeIndex;
            }
        }
    }
    
    private void updateContentBounds() {
        // Initialize ALL tabs, not just the active one
        // This ensures all panels have fresh state values when selection changes
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).content.setBounds(contentBounds);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns ONLY tab button widgets (for Screen registration).
     * These are rendered by Screen and don't need scroll handling.
     */
    public List<ClickableWidget> getWidgets() {
        List<ClickableWidget> tabWidgets = new ArrayList<>();
        for (ButtonWidget btn : tabButtons) {
            if (btn != null) tabWidgets.add(btn);
        }
        return tabWidgets;
    }
    
    /**
     * Returns the active content panel's widgets.
     * These are NOT registered with Screen - the panel handles rendering and input.
     */
    public List<ClickableWidget> getContentWidgets() {
        if (activeIndex < tabs.size() && tabs.get(activeIndex).content.isFeatureSupported()) {
            return tabs.get(activeIndex).content.getWidgets();
        }
        return List.of();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void tick() {
        if (activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.tick();
        }
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds.isEmpty()) return;
        
        // Tab bar background
        context.fill(tabBarBounds.x(), tabBarBounds.y(), 
                     tabBarBounds.right(), tabBarBounds.bottom(), 0xFF222233);
        
        // Active tab indicator (only if active tab is visible)
        if (!tabButtons.isEmpty() && activeIndex < tabButtons.size()) {
            ButtonWidget activeBtn = tabButtons.get(activeIndex);
            if (activeBtn != null) {
                context.fill(activeBtn.getX(), activeBtn.getY() + activeBtn.getHeight() - 2,
                            activeBtn.getX() + activeBtn.getWidth(), activeBtn.getY() + activeBtn.getHeight(),
                            0xFF4488FF);
            }
        }
        
        // Content background (matches right column of grid)
        context.fill(contentBounds.x(), contentBounds.y(),
                     contentBounds.right(), contentBounds.bottom(), 0xFF12121a);
        
        // Render active content (only if tab is visible/supported)
        if (activeIndex < tabs.size() && tabs.get(activeIndex).content.isFeatureSupported()) {
            tabs.get(activeIndex).content.render(context, mouseX, mouseY, delta);
        }
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!contentBounds.contains(mouseX, mouseY)) return false;
        if (activeIndex < tabs.size() && tabs.get(activeIndex).content.isFeatureSupported()) {
            return tabs.get(activeIndex).content.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return false;
    }
    
    /**
 * Handles mouse click by forwarding to active content panel.
 * Content panels handle scroll-adjusted input.
 */
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (activeIndex < tabs.size()) {
        return tabs.get(activeIndex).content.mouseClicked(mouseX, mouseY, button);
    }
    return false;
}    
    // ═══════════════════════════════════════════════════════════════════════════
    // INNER TYPES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private record TabEntry(String label, ContentProvider content) {}
    
    /**
     * Interface for tab content providers.
     */
    public interface ContentProvider {
        void setBounds(Bounds bounds);
        List<ClickableWidget> getWidgets();
        void tick();
        void render(DrawContext context, int mouseX, int mouseY, float delta);
        default boolean mouseScrolled(double mouseX, double mouseY, double h, double v) { return false; }
        
        /** Handle mouse click with scroll-aware coordinate mapping. */
        default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
        
        /** Check if this content's features are supported by current renderer. */
        default boolean isFeatureSupported() { return true; }
        
        /** Get tooltip for when content is disabled due to renderer mode. */
        default String getDisabledTooltip() { return null; }
    }
    
    /**
     * Adapter for AbstractPanel as ContentProvider.
     */
    private static class PanelContentProvider implements ContentProvider {
        private final AbstractPanel panel;
        
        PanelContentProvider(AbstractPanel panel) {
            this.panel = panel;
        }
        
        @Override public void setBounds(Bounds bounds) { panel.setBounds(bounds); }
        @Override public List<ClickableWidget> getWidgets() { return panel.getWidgets(); }
        @Override public void tick() { panel.tick(); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            panel.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
            return panel.mouseScrolled(mouseX, mouseY, h, v);
        }
        @Override public boolean isFeatureSupported() { 
            return panel.isFeatureSupported(); 
        }
        @Override public String getDisabledTooltip() { 
            return panel.getDisabledTooltip(); 
        }
    }
    
    /**
     * Adapter for LayoutPanel (v2) as ContentProvider.
     */
    private static class LayoutPanelContentProvider implements ContentProvider {
        private final LayoutPanel panel;
        
        LayoutPanelContentProvider(LayoutPanel panel) {
            this.panel = panel;
        }
        
        @Override public void setBounds(Bounds bounds) { panel.setBounds(bounds); }
        @Override public List<ClickableWidget> getWidgets() { return panel.getWidgets(); }
        @Override public void tick() { panel.tick(); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            panel.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
            return panel.mouseScrolled(mouseX, mouseY, h, v);
        }
    }
}

