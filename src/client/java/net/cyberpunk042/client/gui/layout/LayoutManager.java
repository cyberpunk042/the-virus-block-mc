package net.cyberpunk042.client.gui.layout;

/**
 * Strategy interface for GUI layout.
 * Implementations define where different UI regions are positioned.
 * 
 * <p>This enables the same panel code to work in both windowed and fullscreen modes
 * by providing different bounds for each region.</p>
 */
public interface LayoutManager {
    
    /**
     * @return The current GUI mode this layout represents.
     */
    GuiMode getMode();
    
    /**
     * Recalculates all bounds based on screen dimensions.
     * Should be called on init and resize.
     */
    void calculate(int screenWidth, int screenHeight);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGION BOUNDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Bounds for the entire GUI panel (for drawing frame/background).
     */
    Bounds getPanelBounds();
    
    /**
     * @return Bounds for the title bar (mode toggle, title text, close button).
     */
    Bounds getTitleBarBounds();
    
    /**
     * @return Bounds for the preview area.
     *         In WINDOWED mode: small box on left side.
     *         In FULLSCREEN mode: large viewport at top.
     */
    Bounds getPreviewBounds();
    
    /**
     * @return Bounds for the tab bar (Q, A, D, P buttons).
     */
    Bounds getTabBarBounds();
    
    /**
     * @return Bounds for the main content area (where panels render).
     */
    Bounds getContentBounds();
    
    /**
     * @return Bounds for the status bar at bottom.
     */
    Bounds getStatusBarBounds();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTOR AND SHAPE BOUNDS (for unified component placement)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Bounds for the layer/primitive selector area.
     */
    default Bounds getSelectorBounds() { return Bounds.EMPTY; }
    
    /**
     * @return Bounds for the shape panel.
     */
    default Bounds getShapePanelBounds() { return Bounds.EMPTY; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Renders the background for this layout.
     * Called first in render sequence.
     */
    default void renderBackground(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {}
    
    /**
     * Renders the frame/borders for this layout.
     * Called after background, before component rendering.
     */
    default void renderFrame(net.minecraft.client.gui.DrawContext context) {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WINDOWED-SPECIFIC (optional, may return EMPTY in fullscreen)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Bounds for left side panel in windowed mode.
     *         Returns EMPTY in fullscreen mode.
     */
    default Bounds getLeftPanelBounds() {
        return Bounds.EMPTY;
    }
    
    /**
     * @return Bounds for right side panel in windowed mode.
     *         Returns EMPTY in fullscreen mode.
     */
    default Bounds getRightPanelBounds() {
        return Bounds.EMPTY;
    }
    
    /**
     * @return Bounds for right panel title bar in windowed mode.
     *         Returns EMPTY in fullscreen mode.
     */
    default Bounds getRightTitleBarBounds() {
        return Bounds.EMPTY;
    }
    
    /**
     * @return Bounds for profiles list panel (left side).
     */
    default Bounds getProfilesLeftBounds() {
        return Bounds.EMPTY;
    }
    
    /**
     * @return Bounds for profiles details panel (right side).
     */
    default Bounds getProfilesRightBounds() {
        return Bounds.EMPTY;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Whether this layout should pause the game.
     */
    default boolean shouldPauseGame() {
        return getMode() == GuiMode.FULLSCREEN;
    }
    
    /**
     * @return Whether player can move while GUI is open.
     */
    default boolean allowsPlayerMovement() {
        return getMode() == GuiMode.WINDOWED;
    }
    
    /**
     * @return Whether to render the game world behind the GUI.
     */
    default boolean showsGameWorld() {
        return getMode() == GuiMode.WINDOWED;
    }
    
    /**
     * @return Whether to render a dedicated 3D preview widget.
     */
    default boolean hasPreviewWidget() {
        return getMode() == GuiMode.FULLSCREEN;
    }
}

