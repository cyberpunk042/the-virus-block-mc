package net.cyberpunk042.client.gui.layout;

import net.cyberpunk042.client.gui.util.GuiConstants;

/**
 * Windowed layout: Split HUD with game world visible in center.
 * 
 * <p>Now responsive - adapts to screen size with scaling.</p>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                                                             │
 * │  ┌──────────┐                              ┌──────────┐    │
 * │  │  LEFT    │                              │  RIGHT   │    │
 * │  │  PANEL   │      GAME WORLD              │  PANEL   │    │
 * │  │          │                              │          │    │
 * │  │ ▸ Shape  │         (player)             │ Layers   │    │
 * │  │ ▸ Fill   │      (debug field)           │ Actions  │    │
 * │  │ ▸ Color  │                              │          │    │
 * │  │          │                              │          │    │
 * │  └──────────┘                              └──────────┘    │
 * │                                                             │
 * │  ┌───────────────────────────────────────────────────────┐ │
 * │  │                    Status Bar                          │ │
 * │  └───────────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class WindowedLayout implements LayoutManager {
    
    // Configuration - base values (scaled dynamically)
    private static final int BASE_STATUS_BAR_HEIGHT = 18;
    private static final int BASE_TITLE_BAR_HEIGHT = 16;
    private static final int BASE_TAB_HEIGHT = 18;
    private static final int BASE_SELECTOR_HEIGHT = 22;
    private static final int BASE_MARGIN = 8;
    private static final int BASE_PADDING = 4;
    private static final int BASE_PANEL_WIDTH = 200;
    private static final int MIN_PANEL_WIDTH = 140;  // Reduced for small windows
    private static final int SMALL_SCREEN_THRESHOLD = 700;  // Width below which to shrink aggressively
    
    // Calculated bounds
    private Bounds leftPanel = Bounds.EMPTY;
    private Bounds rightPanel = Bounds.EMPTY;
    private Bounds statusBar = Bounds.EMPTY;
    private Bounds leftTitleBar = Bounds.EMPTY;
    private Bounds rightTitleBar = Bounds.EMPTY;
    private Bounds leftTabBar = Bounds.EMPTY;
    private Bounds rightTabBar = Bounds.EMPTY;
    private Bounds leftContent = Bounds.EMPTY;
    private Bounds rightContent = Bounds.EMPTY;
    
    private int screenWidth;
    private int screenHeight;
    
    // Cached scaled values
    private int selectorHeight;
    
    @Override
    public GuiMode getMode() {
        return GuiMode.WINDOWED;
    }
    
    @Override
    public void calculate(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Get scale factor
        float scale = GuiConstants.getScale();
        boolean compact = GuiConstants.isCompactMode();
        
        // Scale dimensions
        int statusHeight = Math.max(14, (int)(BASE_STATUS_BAR_HEIGHT * scale));
        int titleHeight = Math.max(12, (int)(BASE_TITLE_BAR_HEIGHT * scale));
        int tabHeight = Math.max(14, (int)(BASE_TAB_HEIGHT * scale));
        selectorHeight = Math.max(18, (int)(BASE_SELECTOR_HEIGHT * scale));
        int margin = Math.max(4, (int)(BASE_MARGIN * scale));
        int padding = Math.max(2, (int)(BASE_PADDING * scale));
        
        // Calculate panel width - scale with screen width, respecting min/max
        int maxPanelWidth = (screenWidth - margin * 4) / 3; // Max 1/3 of usable width per panel
        int targetWidth = (int)(BASE_PANEL_WIDTH * scale);
        int panelWidth;
        
        if (screenWidth < SMALL_SCREEN_THRESHOLD) {
            // Small window: use ~28% of screen width per panel, with minimum
            panelWidth = Math.max(MIN_PANEL_WIDTH, (int)(screenWidth * 0.28f));
        } else {
            // Normal: use scaled target, capped to max
            panelWidth = Math.max(MIN_PANEL_WIDTH, Math.min(maxPanelWidth, targetWidth));
        }
        
        int panelHeight = screenHeight - margin * 2 - statusHeight;
        
        // Left panel: anchored to left side, full height
        int leftX = margin;
        int panelY = margin;
        leftPanel = new Bounds(leftX, panelY, panelWidth, panelHeight);
        
        // Right panel: anchored to right side, full height
        int rightX = screenWidth - margin - panelWidth;
        rightPanel = new Bounds(rightX, panelY, panelWidth, panelHeight);
        
        // Status bar: spans bottom, between the panels
        int statusY = screenHeight - margin - statusHeight;
        statusBar = new Bounds(
            leftX + panelWidth + margin,  // After left panel + margin
            statusY,
            screenWidth - panelWidth * 2 - margin * 4,  // Width between panels
            statusHeight
        );
        
        // Sub-regions within left panel
        leftTitleBar = leftPanel.sliceTop(titleHeight);
        Bounds leftRemaining = leftPanel.withoutTop(titleHeight);
        leftTabBar = leftRemaining.sliceTop(tabHeight);
        leftContent = leftRemaining.withoutTop(tabHeight).inset(padding);
        
        // Sub-regions within right panel
        rightTitleBar = rightPanel.sliceTop(titleHeight);
        Bounds rightRemaining = rightPanel.withoutTop(titleHeight);
        rightContent = rightRemaining.inset(padding);
    }
    
    @Override
    public Bounds getPanelBounds() {
        // In windowed mode, the "panel" is conceptually the whole screen area we draw to
        // But we don't draw a single panel - we draw left + right + status
        return new Bounds(0, 0, screenWidth, screenHeight);
    }
    
    @Override
    public Bounds getTitleBarBounds() {
        // Return left panel's title bar as the primary (for mode toggle)
        return leftTitleBar;
    }
    
    @Override
    public Bounds getPreviewBounds() {
        // No dedicated preview in windowed mode - game world IS the preview
        return Bounds.EMPTY;
    }
    
    @Override
    public Bounds getTabBarBounds() {
        // Left panel's tab bar is the main one
        return leftTabBar;
    }
    
    @Override
    public Bounds getContentBounds() {
        // In windowed mode, sub-tabs/content are in the RIGHT panel, below selectors
        int padding = GuiConstants.padding();
        return rightContent.withoutTop(selectorHeight * 2 + padding);
    }
    
    @Override
    public Bounds getStatusBarBounds() {
        return statusBar;
    }
    
    @Override
    public Bounds getLeftPanelBounds() {
        return leftPanel;
    }
    
    @Override
    public Bounds getRightPanelBounds() {
        return rightPanel;
    }
    
    @Override
    public Bounds getRightTitleBarBounds() {
        return rightTitleBar;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WINDOWED-SPECIFIC ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Bounds getLeftTitleBar() { return leftTitleBar; }
    public Bounds getRightTitleBar() { return rightTitleBar; }
    public Bounds getLeftTabBar() { return leftTabBar; }
    public Bounds getRightTabBar() { return rightTabBar; }
    public Bounds getLeftContent() { return leftContent; }
    public Bounds getRightContent() { return rightContent; }
    
    @Override
    public Bounds getSelectorBounds() {
        // In windowed mode, selectors are in the right panel content area
        int padding = GuiConstants.padding();
        return rightContent.sliceTop(selectorHeight * 2 + padding);
    }
    
    @Override
    public Bounds getShapePanelBounds() {
        // Shape panel is in the left panel content area
        return leftContent;
    }
    
    @Override
    public Bounds getProfilesLeftBounds() {
        // Profiles list uses the left panel content area
        return leftContent.inset(0, 0);
    }
    
    @Override
    public Bounds getProfilesRightBounds() {
        // Profiles details uses the right panel content area
        return rightContent.inset(0, 0);
    }
    
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {
        // Windowed mode: transparent - game world is visible
        // No background fill
    }
    
    @Override
    public void renderFrame(net.minecraft.client.gui.DrawContext context) {
        // Left panel frame + title bar + tab bar
        renderPanelFrame(context, leftPanel);
        context.fill(leftTitleBar.x(), leftTitleBar.y(), leftTitleBar.right(), leftTitleBar.bottom(), 0xFF2a2a2a);
        context.fill(leftTabBar.x(), leftTabBar.y(), leftTabBar.right(), leftTabBar.bottom(), 0xFF222222);
        // Note: Title text "⬡ Field" is rendered by HeaderBar component
        
        // Right panel frame + title bar
        renderPanelFrame(context, rightPanel);
        context.fill(rightTitleBar.x(), rightTitleBar.y(), rightTitleBar.right(), rightTitleBar.bottom(), 0xFF2a2a2a);
        // Note: Title text "Context" should be rendered by a component or FieldCustomizerScreen
    }
    
    private void renderPanelFrame(net.minecraft.client.gui.DrawContext context, Bounds panel) {
        // Shadow
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() + 2, panel.bottom() + 2, 0x44000000);
        // Border
        context.fill(panel.x() - 1, panel.y() - 1, panel.right() + 1, panel.bottom() + 1, 0xFF444444);
        // Background
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xDD1a1a1a);
    }
    
    @Override
    public boolean shouldPauseGame() { return false; }
    
    @Override
    public boolean allowsPlayerMovement() { return true; }
    
    @Override
    public boolean showsGameWorld() { return true; }
    
    @Override
    public boolean hasPreviewWidget() { return false; }
}

