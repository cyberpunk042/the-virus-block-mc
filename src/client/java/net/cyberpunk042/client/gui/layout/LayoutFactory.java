package net.cyberpunk042.client.gui.layout;

/**
 * Factory for creating and caching layout managers.
 */
public final class LayoutFactory {
    
    private static final WindowedLayout WINDOWED = new WindowedLayout();
    private static final FullscreenLayout FULLSCREEN = new FullscreenLayout();
    
    private LayoutFactory() {}
    
    /**
     * Gets the layout manager for the specified mode.
     */
    public static LayoutManager get(GuiMode mode) {
        return switch (mode) {
            case WINDOWED -> WINDOWED;
            case FULLSCREEN -> FULLSCREEN;
        };
    }
    
    /**
     * Gets and recalculates the layout for the specified mode and screen size.
     */
    public static LayoutManager getAndCalculate(GuiMode mode, int screenWidth, int screenHeight) {
        LayoutManager layout = get(mode);
        layout.calculate(screenWidth, screenHeight);
        return layout;
    }
    
    /**
     * Gets windowed layout specifically (for windowed-specific methods).
     */
    public static WindowedLayout windowed() {
        return WINDOWED;
    }
    
    /**
     * Gets fullscreen layout specifically (for fullscreen-specific methods).
     */
    public static FullscreenLayout fullscreen() {
        return FULLSCREEN;
    }
}

