package net.cyberpunk042.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Compact text widget for descriptions with scaled text.
 */
public class ScaledTextWidget extends ClickableWidget {
    
    private final TextRenderer textRenderer;
    private final float scale;
    private int color = 0xFFFFFFFF;
    private boolean leftAligned = false;
    
    public ScaledTextWidget(int x, int y, int width, int height, Text text, TextRenderer textRenderer, float scale) {
        super(x, y, width, height, text);
        this.textRenderer = textRenderer;
        this.scale = scale;
    }
    
    public ScaledTextWidget setTextColor(int color) {
        this.color = color;
        return this;
    }
    
    public ScaledTextWidget alignLeft() {
        this.leftAligned = true;
        return this;
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        String textStr = getMessage().getString();
        
        // Get the 2D matrix stack
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to widget position, then scale
        matrices.translate(getX(), getY());
        matrices.scale(scale, scale);
        
        // Calculate X position in scaled space
        int textWidth = textRenderer.getWidth(textStr);
        int drawX;
        if (leftAligned) {
            drawX = 0;
        } else {
            int scaledWidth = (int)(width / scale);
            drawX = (scaledWidth - textWidth) / 2;
        }
        
        // Draw text at origin (we've translated already)
        context.drawText(textRenderer, textStr, drawX, 0, color, false);
        
        matrices.popMatrix();
    }
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // No narration for display-only text
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}

