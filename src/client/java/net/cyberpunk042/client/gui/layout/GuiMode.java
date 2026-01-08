package net.cyberpunk042.client.gui.layout;

/**
 * GUI display mode determining layout strategy.
 */
public enum GuiMode {
    
    /**
     * Windowed mode: Split HUD with panels on sides.
     * - Game world visible in center
     * - Left panel: Primary controls (shape, fill, color)
     * - Right panel: Secondary controls (layers, actions)
     * - Status bar at bottom
     * - Player can see their character and debug field in real-time
     */
    WINDOWED("Windowed", "⬜", "Panels on sides, see game world"),
    
    /**
     * Fullscreen mode: Immersive editor.
     * - Fills screen with dark background
     * - 3D viewport at top for preview
     * - Tab bar + controls at bottom
     * - Full attention on editing
     */
    FULLSCREEN("Fullscreen", "⬛", "Full editor with 3D preview");
    
    private final String label;
    private final String icon;
    private final String description;
    
    GuiMode(String label, String icon, String description) {
        this.label = label;
        this.icon = icon;
        this.description = description;
    }
    
    public String label() { return label; }
    public String icon() { return icon; }
    public String description() { return description; }
    
    /**
     * Toggle between modes.
     */
    public GuiMode toggle() {
        return this == WINDOWED ? FULLSCREEN : WINDOWED;
    }
    
    @Override
    public String toString() {
        return icon + " " + label;
    }
}

