package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/**
 * G34: Simple loading spinner for async operations.
 */
public class LoadingIndicator {
    
    private static final String[] SPINNER_FRAMES = {"◐", "◓", "◑", "◒"};
    private static final int FRAME_DURATION_MS = 150;
    
    private final int x, y;
    private final String label;
    private boolean visible = false;
    private long startTime;
    
    public LoadingIndicator(int x, int y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
    }
    
    /**
     * Shows the loading indicator.
     */
    public void show() {
        visible = true;
        startTime = System.currentTimeMillis();
    }
    
    /**
     * Hides the loading indicator.
     */
    public void hide() {
        visible = false;
    }
    
    /**
     * Returns whether the indicator is visible.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Renders the loading indicator.
     */
    public void render(DrawContext context, TextRenderer textRenderer) {
        if (!visible) return;
        
        long elapsed = System.currentTimeMillis() - startTime;
        int frameIndex = (int)((elapsed / FRAME_DURATION_MS) % SPINNER_FRAMES.length);
        String spinner = SPINNER_FRAMES[frameIndex];
        
        String text = spinner + " " + label + "...";
        context.drawTextWithShadow(textRenderer, text, x, y, GuiConstants.TEXT_SECONDARY);
    }
    
    /**
     * Creates a centered loading indicator.
     */
    public static LoadingIndicator centered(int screenWidth, int y, String label) {
        // Approximate center - actual centering needs font metrics
        return new LoadingIndicator(screenWidth / 2 - 50, y, label);
    }
}
