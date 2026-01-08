package net.cyberpunk042.client.field;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Renders a loading overlay during warmup phase.
 * Shows a progress bar while chunks and effects are loading.
 */
public final class WarmupOverlay {
    
    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_Y_OFFSET = 50;
    
    private WarmupOverlay() {}
    
    private static volatile boolean registered = false;
    
    /**
     * Registers the HUD render callback.
     */
    public static void init() {
        if (registered) return;
        registered = true;
        HudRenderCallback.EVENT.register(WarmupOverlay::render);
    }
    
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!JoinWarmupManager.isWarmingUp()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        float progress = JoinWarmupManager.getWarmupProgress();
        
        // Center of screen
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2 + BAR_Y_OFFSET;
        
        // Bar bounds
        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = centerY - BAR_HEIGHT / 2;
        int barRight = centerX + BAR_WIDTH / 2;
        int barBottom = centerY + BAR_HEIGHT / 2;
        
        // Background (dark)
        context.fill(barLeft - 2, barTop - 2, barRight + 2, barBottom + 2, 0xAA000000);
        
        // Bar background (gray)
        context.fill(barLeft, barTop, barRight, barBottom, 0xFF333333);
        
        // Progress fill (cyan gradient feel)
        int fillWidth = (int) (BAR_WIDTH * progress);
        if (fillWidth > 0) {
            // Cyan to white gradient effect
            int color = 0xFF00DDDD;
            context.fill(barLeft, barTop, barLeft + fillWidth, barBottom, color);
        }
        
        // Text - show current stage
        String loadingText = JoinWarmupManager.getCurrentStageLabel();
        int textWidth = client.textRenderer.getWidth(loadingText);
        context.drawText(
            client.textRenderer,
            Text.literal(loadingText),
            centerX - textWidth / 2,
            barTop - 15,
            0xFFFFFFFF,
            true
        );
        
        // Percentage
        String percentText = String.format("%.0f%%", progress * 100);
        int percentWidth = client.textRenderer.getWidth(percentText);
        context.drawText(
            client.textRenderer,
            Text.literal(percentText),
            centerX - percentWidth / 2,
            barBottom + 5,
            0xFFAAAAAA,
            true
        );
    }
}
