package net.cyberpunk042.client.gui.layout;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Status bar component showing current context.
 * 
 * Displays:
 * - Current layer and primitive
 * - Dirty state indicator
 * - Quick hints/messages
 */
public class StatusBar {
    
    private final FieldEditState state;
    private final TextRenderer textRenderer;
    private Bounds bounds = Bounds.EMPTY;
    private String message = "";
    private long messageExpireTime = 0;
    
    // Warning/hint display
    private String warning = "";
    private int warningColor = 0xFFFFAA00;
    
    public StatusBar(FieldEditState state, TextRenderer textRenderer) {
        this.state = state;
        this.textRenderer = textRenderer;
    }
    
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }
    
    /**
     * Shows a temporary message in the status bar.
     */
    public void showMessage(String text, int durationMs) {
        this.message = text;
        this.messageExpireTime = System.currentTimeMillis() + durationMs;
    }
    
    /**
     * Sets a persistent warning/hint (like performance warnings).
     */
    public void setWarning(String text, int color) {
        this.warning = text != null ? text : "";
        this.warningColor = color;
    }
    
    /**
     * Clears the warning.
     */
    public void clearWarning() {
        this.warning = "";
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds.isEmpty()) return;
        
        // Background
        context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xCC222233);
        
        // Border at top
        context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, 0xFF333344);
        
        int y = bounds.y() + (bounds.height() - 8) / 2;
        int x = bounds.x() + 4;
        
        // Layer/Primitive info
        String layerInfo = String.format("Layer: %d", state.getSelectedLayerIndex());
        context.drawTextWithShadow(textRenderer, layerInfo, x, y, 0xFF888899);
        x += textRenderer.getWidth(layerInfo) + 8;
        
        String primInfo = String.format("Prim: %d", state.getSelectedPrimitiveIndex());
        context.drawTextWithShadow(textRenderer, primInfo, x, y, 0xFF888899);
        x += textRenderer.getWidth(primInfo) + 8;
        
        // Shape type is shown in preview area, skip here to avoid duplication
        
        // Warning (persistent, shows after shape info)
        if (!warning.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "│", x, y, 0xFF444455);
            x += 8;
            context.drawTextWithShadow(textRenderer, warning, x, y, warningColor);
        }
        
        // Temporary message (overrides right side)
        if (System.currentTimeMillis() < messageExpireTime && !message.isEmpty()) {
            int msgWidth = textRenderer.getWidth(message);
            int msgX = bounds.right() - msgWidth - 4;
            context.drawTextWithShadow(textRenderer, message, msgX, y, 0xFFFFAA00);
        } else {
            // Dirty indicator
            if (state.isDirty()) {
                int dirtyX = bounds.right() - 20;
                context.drawTextWithShadow(textRenderer, "●", dirtyX, y, 0xFFFFAA00);
            }
        }
    }
}

