package net.cyberpunk042.client.gui.layout;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.GridPane;
/**
 * Fullscreen layout: 2×2 grid layout matching OLD working code.
 * 
 * <p>Now responsive - adapts to screen size with scaling.</p>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ [⬜] Field Customizer                                            [×]    │ ← Title bar
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ [Quick] [Advanced] [Debug] [Profiles]      [Fast/Acc] [Select Preset…] │ ← Tab bar
 * ├─────────────────────────────┬───────────────────────────────────────────┤
 * │                             │ LAYER: [+] ◀ base ▶ [+]                   │
 * │       3D PREVIEW            │ PRIM:  [◀] sphere_main [+]                │
 * │      (top-left)             ├───────────────────────────────────────────┤
 * │                             │ [Fill] [Appear] [Visibility] [Xfm]        │
 * │                             │ (sub-tab content area - top-right)        │
 * ├─────────────────────────────┼───────────────────────────────────────────┤
 * │       SHAPE                 │                                           │
 * │   Type: [Sphere ▼]          │   (sub-tab content continues)             │
 * │   (bottom-left)             │   (bottom-right)                          │
 * └─────────────────────────────┴───────────────────────────────────────────┘
 * │ Layer: 0  Prim: 0  | Status bar                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class FullscreenLayout implements LayoutManager {
    
    // Configuration - base values (scaled dynamically)
    private static final int BASE_TITLE_BAR_HEIGHT = 20;
    private static final int BASE_TAB_BAR_HEIGHT = 22;
    private static final int BASE_STATUS_BAR_HEIGHT = 18;
    private static final int BASE_MARGIN = 8;
    private static final int BASE_SELECTOR_HEIGHT = 22;
    
    // Calculated bounds
    private Bounds panel = Bounds.EMPTY;
    private Bounds titleBar = Bounds.EMPTY;
    private Bounds preview = Bounds.EMPTY;
    private Bounds tabBar = Bounds.EMPTY;
    private Bounds content = Bounds.EMPTY;
    private Bounds statusBar = Bounds.EMPTY;
    private GridPane grid;  // 2x2 grid for content quadrants
    
    // Cached scaled values
    private int selectorHeight;
    
    @Override
    public GuiMode getMode() {
        return GuiMode.FULLSCREEN;
    }
    
    @Override
    public void calculate(int screenWidth, int screenHeight) {
        // Get scale factor
        float scale = GuiConstants.getScale();
        boolean compact = GuiConstants.isCompactMode();
        
        // Scale dimensions with minimums
        int titleHeight = Math.max(16, (int)(BASE_TITLE_BAR_HEIGHT * scale));
        int tabHeight = Math.max(18, (int)(BASE_TAB_BAR_HEIGHT * scale));
        int statusHeight = Math.max(14, (int)(BASE_STATUS_BAR_HEIGHT * scale));
        int margin = Math.max(4, (int)(BASE_MARGIN * scale));
        selectorHeight = Math.max(18, (int)(BASE_SELECTOR_HEIGHT * scale));
        
        // Full panel with margin
        panel = new Bounds(margin, margin, screenWidth - margin * 2, screenHeight - margin * 2);
        
        // Title bar at top
        titleBar = panel.sliceTop(titleHeight);
        Bounds remaining = panel.withoutTop(titleHeight);
        
        // Tab bar below title (NOT at bottom!)
        tabBar = remaining.sliceTop(tabHeight);
        remaining = remaining.withoutTop(tabHeight);
        
        // Status bar at bottom
        statusBar = remaining.sliceBottom(statusHeight);
        remaining = remaining.withoutBottom(statusHeight);
        
        // Content area uses 2x2 grid
        // In compact mode, give more space to content (less to preview)
        float leftRatio = compact ? 0.30f : 0.40f;
        float topRatio = compact ? 0.40f : 0.50f;
        
        content = remaining;
        grid = GridPane.grid2x2(leftRatio, topRatio);
        grid.setBounds(content);
        
        // Preview is top-left quadrant
        int inset = Math.max(2, (int)(4 * scale));
        preview = grid.topLeft().inset(inset);
    }
    
    @Override
    public Bounds getPanelBounds() {
        return panel;
    }
    
    @Override
    public Bounds getTitleBarBounds() {
        return titleBar;
    }
    
    @Override
    public Bounds getPreviewBounds() {
        return preview;
    }
    
    @Override
    public Bounds getTabBarBounds() {
        return tabBar;
    }
    
    @Override
    public Bounds getContentBounds() {
        // ContentArea spans FULL right column (both cells) below selectors
        int padding = GuiConstants.padding();
        return grid.rightColumn().withoutTop(selectorHeight * 2 + padding);
    }
    
    @Override
    public Bounds getStatusBarBounds() {
        return statusBar;
    }
    
    @Override
    public Bounds getSelectorBounds() {
        // Selectors at top of right column
        int padding = GuiConstants.padding();
        return grid.topRight().sliceTop(selectorHeight * 2 + padding);
    }
    
    @Override
    public Bounds getShapePanelBounds() {
        // Shape panel is bottom-left quadrant
        return grid.bottomLeft().inset(4);
    }
    
    /** Returns the sub-tab content bounds (right column below selectors) */
    public Bounds getSubTabBounds() {
        int padding = GuiConstants.padding();
        return grid.rightColumn().withoutTop(selectorHeight * 2 + padding);
    }
    
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {
        // Solid dark background - fullscreen blocks game world entirely
        context.fill(0, 0, screenWidth, screenHeight, 0xFF0a0a0a);
    }
    
    @Override
    public void renderFrame(net.minecraft.client.gui.DrawContext context) {
        // Main panel frame (border)
        context.fill(panel.x() - 1, panel.y() - 1, panel.right() + 1, panel.bottom() + 1, 0xFF333333);
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xDD1a1a1a);
        
        // Title bar background
        context.fill(titleBar.x(), titleBar.y(), titleBar.right(), titleBar.bottom(), 0xFF2a2a2a);
        
        // Tab bar background  
        context.fill(tabBar.x(), tabBar.y(), tabBar.right(), tabBar.bottom(), 0xFF222222);
        
        // Quadrant backgrounds
        renderQuadrantBackgrounds(context);
    }
    
    private void renderQuadrantBackgrounds(net.minecraft.client.gui.DrawContext context) {
        // Top-left: Preview area
        Bounds tl = grid.topLeft();
        context.fill(tl.x(), tl.y(), tl.right(), tl.bottom(), 0xFF0a0a0a);
        context.drawBorder(tl.x(), tl.y(), tl.width(), tl.height(), 0xFF333333);
        
        // Bottom-left: Shape area
        Bounds bl = grid.bottomLeft();
        context.fill(bl.x(), bl.y(), bl.right(), bl.bottom(), 0xFF151515);
        context.drawBorder(bl.x(), bl.y(), bl.width(), bl.height(), 0xFF333333);
        
        // Right column background (both cells)
        Bounds rightCol = grid.rightColumn();
        context.fill(rightCol.x(), rightCol.y(), rightCol.right(), rightCol.bottom(), 0xFF121212);
    }
    
    @Override
    public Bounds getProfilesLeftBounds() {
        // Fullscreen: left column
        return grid.getSpan(0, 0, 1, 2);
    }
    
    @Override
    public Bounds getProfilesRightBounds() {
        // Fullscreen: right column  
        return grid.getSpan(1, 0, 1, 2);
    }
    
    @Override
    public boolean shouldPauseGame() { return true; }
    
    @Override
    public boolean allowsPlayerMovement() { return false; }
    
    @Override
    public boolean showsGameWorld() { return false; }
    
    @Override
    public boolean hasPreviewWidget() { return true; }
    
    /** Returns the grid for direct quadrant access (needed for rendering). */
    public GridPane getGrid() {
        return grid;
    }
}

