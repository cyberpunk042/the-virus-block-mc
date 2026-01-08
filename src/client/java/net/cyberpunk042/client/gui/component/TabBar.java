package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.Bounds;
// SimplifiedFieldRenderer removed - standard mode always used
import net.cyberpunk042.client.gui.screen.TabType;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.cyberpunk042.client.gui.widget.DropdownWidget;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tab bar component containing:
 * <ul>
 *   <li>Main tab buttons (Quick, Advanced, Debug, Profiles)</li>
 *   <li>Preset dropdown</li>
 *   <li>Renderer mode toggle (Simplified/Standard)</li>
 * </ul>
 * 
 * <p>Handles visibility of tabs based on renderer mode and permissions.</p>
 */
public class TabBar implements ScreenComponent {
    
    private Bounds bounds;
    private final TextRenderer textRenderer;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    
    // Callbacks
    private final Consumer<TabType> onTabSwitch;
    private final Consumer<String> onPresetSelected;
    private final Runnable onRendererModeChanged;
    
    // Visibility controller
    private final VisibilityController visibility;
    
    // State
    private TabType currentTab = TabType.QUICK;
    
    // Widgets
    private ButtonWidget quickTabBtn;
    private ButtonWidget advancedTabBtn;
    private ButtonWidget debugTabBtn;
    private ButtonWidget fxTabBtn;
    private ButtonWidget profilesTabBtn;
    private DropdownWidget<String> presetDropdown;
    private CyclingButtonWidget<Boolean> rendererModeToggle;
    
    // Layout constants - match old code
    private static final int TAB_WIDTH = 70;
    private static final int TAB_HEIGHT = 20;  // TAB_BAR_HEIGHT - 2
    private static final int TAB_GAP = 2;
    
    /**
     * Creates a tab bar.
     * 
     * @param textRenderer Font renderer
     * @param visibility Visibility controller for mode/permission checks
     * @param onTabSwitch Called when a tab is selected
     * @param onPresetSelected Called when a preset is selected from dropdown
     * @param onRendererModeChanged Called when renderer mode changes
     */
    public TabBar(
            TextRenderer textRenderer,
            VisibilityController visibility,
            Consumer<TabType> onTabSwitch,
            Consumer<String> onPresetSelected,
            Runnable onRendererModeChanged) {
        
        this.textRenderer = textRenderer;
        this.visibility = visibility;
        this.onTabSwitch = onTabSwitch;
        this.onPresetSelected = onPresetSelected;
        this.onRendererModeChanged = onRendererModeChanged;
    }
    
    @Override
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        rebuild();
    }
    
    @Override
    public void rebuild() {
        widgets.clear();
        if (bounds == null) return;
        
        int x = bounds.x();
        int y = bounds.y();
        int height = bounds.height() - 2;  // Slight visual padding
        int rightEdge = bounds.right();
        
        // In windowed mode, use short labels and smaller tabs (OLD: lines 307-332)
        boolean useShortLabels = !visibility.isFullscreen();
        int tabWidth = useShortLabels ? 30 : TAB_WIDTH;  // Narrower in windowed
        
        // Quick tab (always visible)
        String quickLabel = useShortLabels ? TabType.QUICK.shortLabel() : TabType.QUICK.label();
        quickTabBtn = ButtonWidget.builder(Text.literal(quickLabel), btn -> switchTab(TabType.QUICK))
            .dimensions(x, y, tabWidth, height)
            .tooltip(Tooltip.of(Text.literal(TabType.QUICK.tooltip())))
            .build();
        widgets.add(quickTabBtn);
        x += tabWidth + TAB_GAP;
        
        // Advanced tab (hidden in Simplified mode)
        String advLabel = useShortLabels ? TabType.ADVANCED.shortLabel() : TabType.ADVANCED.label();
        advancedTabBtn = ButtonWidget.builder(Text.literal(advLabel), btn -> switchTab(TabType.ADVANCED))
            .dimensions(x, y, tabWidth, height)
            .tooltip(Tooltip.of(Text.literal(TabType.ADVANCED.tooltip())))
            .build();
        advancedTabBtn.visible = visibility.isAdvancedTabVisible();
        widgets.add(advancedTabBtn);
        if (advancedTabBtn.visible) {
            x += tabWidth + TAB_GAP;
        }
        
        // Debug tab (hidden if not debug unlocked)
        String debugLabel = useShortLabels ? TabType.DEBUG.shortLabel() : TabType.DEBUG.label();
        debugTabBtn = ButtonWidget.builder(Text.literal(debugLabel), btn -> switchTab(TabType.DEBUG))
            .dimensions(x, y, tabWidth, height)
            .tooltip(Tooltip.of(Text.literal(TabType.DEBUG.tooltip())))
            .build();
        debugTabBtn.visible = visibility.isDebugTabVisible();
        widgets.add(debugTabBtn);
        if (debugTabBtn.visible) {
            x += tabWidth + TAB_GAP;
        }
        
        // FX tab (hidden if not debug unlocked - same as Debug)
        String fxLabel = useShortLabels ? TabType.FX.shortLabel() : TabType.FX.label();
        fxTabBtn = ButtonWidget.builder(Text.literal(fxLabel), btn -> switchTab(TabType.FX))
            .dimensions(x, y, tabWidth, height)
            .tooltip(Tooltip.of(Text.literal(TabType.FX.tooltip())))
            .build();
        fxTabBtn.visible = visibility.isFxTabVisible();
        widgets.add(fxTabBtn);
        if (fxTabBtn.visible) {
            x += tabWidth + TAB_GAP;
        }
        
        // Profiles tab (always visible)
        String profLabel = useShortLabels ? TabType.PROFILES.shortLabel() : TabType.PROFILES.label();
        profilesTabBtn = ButtonWidget.builder(Text.literal(profLabel), btn -> switchTab(TabType.PROFILES))
            .dimensions(x, y, tabWidth, height)
            .tooltip(Tooltip.of(Text.literal(TabType.PROFILES.tooltip())))
            .build();
        widgets.add(profilesTabBtn);
        
        // Preset dropdown and renderer toggle (FULLSCREEN only - in windowed these go to right panel)
        if (!useShortLabels) {
            int dropdownW = 120;  // Reduced from 160 to avoid overlapping tabs
            int toggleW = 70;
            int presetX = rightEdge - dropdownW;
            int toggleX = presetX - toggleW - 8;
            
            // Load presets
            PresetRegistry.loadAll();
            List<String> presetNames = new ArrayList<>(PresetRegistry.listPresets());
            final String placeholder = "Select Preset…";
            if (presetNames.isEmpty()) {
                presetNames.add("No presets found");
            } else {
                presetNames.add(0, placeholder);
            }
            
            presetDropdown = new DropdownWidget<>(
                textRenderer, presetX, y, dropdownW, height,
                presetNames,
                name -> {
                    if (name.equals(placeholder)) return Text.literal("Select Preset...");
                    if (name.equals("No presets found")) return Text.literal("No presets");
                    return Text.literal(PresetRegistry.getDisplayLabel(name));
                },
                name -> {
                    if (!name.equals(placeholder) && !name.equals("No presets found")) {
                        onPresetSelected.accept(name);
                    }
                }
            );
            widgets.add(presetDropdown);
            
            // Renderer mode toggle - DEPRECATED (SimplifiedFieldRenderer removed)
            // Standard mode is now always used
        }
        
        updateTabButtonStyles();
    }
    
    private void switchTab(TabType tab) {
        currentTab = tab;
        updateTabButtonStyles();
        onTabSwitch.accept(tab);
    }
    
    /**
     * Sets the active tab and updates button states.
     */
    public void setActiveTab(TabType tab) {
        currentTab = tab;
        updateTabButtonStyles();
    }
    
    /**
     * Returns the currently active tab.
     */
    public TabType getCurrentTab() {
        return currentTab;
    }
    
    /**
     * Updates tab button visual styles based on selection.
     * OLD: lines 897-902 - sets .active = false on current tab button
     */
    private void updateTabButtonStyles() {
        if (quickTabBtn != null) quickTabBtn.active = currentTab != TabType.QUICK;
        if (advancedTabBtn != null) advancedTabBtn.active = currentTab != TabType.ADVANCED;
        if (debugTabBtn != null) debugTabBtn.active = currentTab != TabType.DEBUG;
        if (fxTabBtn != null) fxTabBtn.active = currentTab != TabType.FX;
        if (profilesTabBtn != null) profilesTabBtn.active = currentTab != TabType.PROFILES;
    }
    
    /**
     * Refreshes visibility of tabs based on current mode and permissions.
     * Rebuilds the entire tab bar so hidden tabs don't leave gaps.
     */
    public void refreshVisibility() {
        // If current tab is now hidden, switch to Quick first
        if ((currentTab == TabType.ADVANCED && !visibility.isAdvancedTabVisible()) ||
            (currentTab == TabType.DEBUG && !visibility.isDebugTabVisible()) ||
            (currentTab == TabType.FX && !visibility.isFxTabVisible())) {
            currentTab = TabType.QUICK;
        }
        // Rebuild to remove hidden tabs completely (no gaps)
        rebuild();
    }
    
    @Override
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Tab bar background could be rendered here if desired
    }
    
    /**
     * Returns the dropdown widget (needed for z-order handling).
     */
    public DropdownWidget<String> getPresetDropdown() {
        return presetDropdown;
    }
}
