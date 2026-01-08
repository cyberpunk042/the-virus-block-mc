package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.GuiMode;
// SimplifiedFieldRenderer removed - standard mode is always used
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities;
import net.cyberpunk042.client.gui.util.WidgetVisibility;
import net.minecraft.client.MinecraftClient;

/**
 * Centralized controller for visibility of UI elements based on:
 * <ul>
 *   <li>Renderer mode (Simplified vs Standard)</li>
 *   <li>GUI mode (Fullscreen vs Windowed)</li>
 *   <li>Player permissions (Operator level)</li>
 *   <li>State flags (debugUnlocked)</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * VisibilityController visibility = new VisibilityController(state, mode);
 * 
 * // Check visibility
 * if (visibility.isAdvancedTabVisible()) {
 *     advancedTab.visible = true;
 * }
 * 
 * // When renderer mode changes:
 * visibility.notifyRendererModeChanged();
 * </pre>
 * 
 * @see RendererCapabilities
 * @see WidgetVisibility
 */
public class VisibilityController {
    
    private final FieldEditState state;
    private GuiMode guiMode;
    
    /**
     * Creates a visibility controller.
     * 
     * @param state The field edit state
     * @param guiMode Current GUI mode (fullscreen/windowed)
     */
    public VisibilityController(FieldEditState state, GuiMode guiMode) {
        this.state = state;
        this.guiMode = guiMode;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERER MODE CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if Standard renderer mode is enabled.
     * Standard mode supports all features (bindings, triggers, lifecycle, etc.)
     */
    public boolean isStandardModeEnabled() {
        return true; // Standard mode is always enabled (SimplifiedFieldRenderer was removed)
    }
    
    /**
     * Returns true if Simplified renderer mode is enabled.
     * Simplified mode has reduced features for better performance.
     */
    public boolean isSimplifiedMode() {
        return false; // Simplified mode removed - standard mode is always used
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if the current player is an operator (permission level 2+).
     */
    public boolean isOperator() {
        var player = MinecraftClient.getInstance().player;
        return player != null && player.hasPermissionLevel(2);
    }
    
    /**
     * Returns true if debug mode is unlocked for this session.
     */
    public boolean isDebugUnlocked() {
        return state.getBool("debugUnlocked");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GUI MODE CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if currently in fullscreen GUI mode.
     */
    public boolean isFullscreen() {
        return guiMode == GuiMode.FULLSCREEN;
    }
    
    /**
     * Returns true if currently in windowed GUI mode.
     */
    public boolean isWindowed() {
        return guiMode == GuiMode.WINDOWED;
    }
    
    /**
     * Updates the GUI mode.
     */
    public void setGuiMode(GuiMode mode) {
        this.guiMode = mode;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TAB VISIBILITY PREDICATES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if the Advanced tab should be visible.
     * Hidden in Simplified mode (features not supported).
     */
    public boolean isAdvancedTabVisible() {
        return isStandardModeEnabled();
    }
    
    /**
     * Returns true if the Debug tab should be visible.
     * Only visible to operators who have unlocked debug mode.
     */
    public boolean isDebugTabVisible() {
        return isDebugUnlocked();
    }
    
    /**
     * Returns true if the FX (visual effects) tab should be visible.
     * Only visible to operators who have unlocked debug mode.
     */
    public boolean isFxTabVisible() {
        return isDebugUnlocked();
    }
    
    /**
     * Returns true if the Profiles tab should be visible.
     * Currently always visible.
     */
    public boolean isProfilesTabVisible() {
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET VISIBILITY PREDICATES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if the 3D preview mode toggle should be visible.
     * Only visible in Standard mode AND Fullscreen layout.
     */
    public boolean is3DPreviewToggleVisible() {
        return isStandardModeEnabled() && isFullscreen();
    }
    
    /**
     * Returns true if the "Save to Server" button should be visible.
     * Only visible to operators.
     */
    public boolean isSaveToServerVisible() {
        return isOperator();
    }
    
    /**
     * Returns true if wave animation controls should be visible.
     * Hidden in Simplified mode (wave not supported).
     */
    public boolean isWaveControlsVisible() {
        return isStandardModeEnabled();
    }
    
    /**
     * Returns true if wobble animation controls should be visible.
     * Hidden in Simplified mode (wobble not supported).
     */
    public boolean isWobbleControlsVisible() {
        return isStandardModeEnabled();
    }
    
    /**
     * Returns true if bindings controls should be visible.
     * Hidden in Simplified mode (bindings not supported).
     */
    public boolean isBindingsControlsVisible() {
        return isStandardModeEnabled();
    }
    
    /**
     * Returns true if lifecycle controls should be visible.
     * Hidden in Simplified mode (lifecycle not supported).
     */
    public boolean isLifecycleControlsVisible() {
        return isStandardModeEnabled();
    }
    
    /**
     * Returns true if linking controls should be visible.
     * Hidden in Simplified mode (linking not supported).
     */
    public boolean isLinkingControlsVisible() {
        return isStandardModeEnabled();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE CHANGE NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Call this when the renderer mode changes (Simplified ↔ Standard).
     * Updates all widgets registered with WidgetVisibility.
     */
    public void notifyRendererModeChanged() {
        WidgetVisibility.refreshAll();
    }
    
    /**
     * Call this when the GUI mode changes (Fullscreen ↔ Windowed).
     * Updates all widgets registered with WidgetVisibility.
     */
    public void notifyGuiModeChanged(GuiMode newMode) {
        this.guiMode = newMode;
        WidgetVisibility.refreshAll();
    }
}
