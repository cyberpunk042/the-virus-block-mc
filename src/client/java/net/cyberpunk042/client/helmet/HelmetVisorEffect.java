package net.cyberpunk042.client.helmet;

import net.cyberpunk042.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Renders the Augmented Helmet visor overlay effect.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Corner bracket frame elements (close to edges)</li>
 *   <li>Subtle scan beam effect</li>
 *   <li>Minimal corner darkening only</li>
 * </ul>
 */
public final class HelmetVisorEffect {
    
    private HelmetVisorEffect() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (Subtle)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final int COLOR_SCANLINE = 0x00AAAA;  // Muted cyan
    private static final int COLOR_BRACKET = 0x00BBBB;   // Slightly brighter muted cyan
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static long lastRenderTime = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Main render method. Called by HudRenderCallback.
     */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        HelmetHudConfig config = HelmetHudConfig.get();
        
        // Check if visor effects are enabled
        if (!config.isVisorEnabled()) return;
        
        // Check if wearing augmented helmet
        if (!hasAugmentedHelmet(client)) return;
        
        // Visor effects ALWAYS show when helmet is worn (not dependent on server data)
        // The HUD panel needs server data, but the visor frame is purely cosmetic
        float alpha = config.getVisorIntensity();
        if (alpha < 0.01f) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Update animation timing
        long now = System.currentTimeMillis();
        lastRenderTime = now;
        
        // Render effects
        if (config.isScanlinesEnabled()) {
            renderScanBeam(context, screenWidth, screenHeight, config.getScanlinesOpacity() * alpha);
        }
        
        renderCornerBrackets(context, screenWidth, screenHeight, alpha * 0.8f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCAN BEAM (just the moving beam, no static scanlines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderScanBeam(DrawContext context, int w, int h, float opacity) {
        if (opacity < 0.01f) return;
        
        // Single brighter "scan beam" that moves down slowly
        float beamPos = (System.currentTimeMillis() % 6000) / 6000f;  // 6 second cycle
        int beamY = (int)(beamPos * h);
        int beamHeight = 2;
        int beamAlpha = (int)(opacity * 80);
        int beamColor = (beamAlpha << 24) | COLOR_SCANLINE;
        
        // Beam with soft edges
        context.fill(0, beamY - 1, w, beamY, (beamAlpha / 3 << 24) | COLOR_SCANLINE);
        context.fill(0, beamY, w, beamY + beamHeight, beamColor);
        context.fill(0, beamY + beamHeight, w, beamY + beamHeight + 1, (beamAlpha / 3 << 24) | COLOR_SCANLINE);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORNER BRACKETS (closer to edges now)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderCornerBrackets(DrawContext context, int w, int h, float alpha) {
        if (alpha < 0.01f) return;
        
        int bracketAlpha = (int)(alpha * 220);
        int color = (bracketAlpha << 24) | COLOR_BRACKET;
        int colorDim = ((bracketAlpha / 2) << 24) | COLOR_BRACKET;
        
        int length = 50;
        int thickness = 2;
        int margin = 5;  // Much closer to edge (was 15)
        
        // Top-left bracket ┌
        context.fill(margin, margin, margin + length, margin + thickness, color);  // Horizontal
        context.fill(margin, margin, margin + thickness, margin + length, color);  // Vertical
        // Inner accent line
        context.fill(margin + thickness + 2, margin + thickness + 2, margin + length - 8, margin + thickness + 3, colorDim);
        
        // Top-right bracket ┐
        context.fill(w - margin - length, margin, w - margin, margin + thickness, color);
        context.fill(w - margin - thickness, margin, w - margin, margin + length, color);
        context.fill(w - margin - length + 8, margin + thickness + 2, w - margin - thickness - 2, margin + thickness + 3, colorDim);
        
        // Bottom-left bracket └
        context.fill(margin, h - margin - thickness, margin + length, h - margin, color);
        context.fill(margin, h - margin - length, margin + thickness, h - margin, color);
        context.fill(margin + thickness + 2, h - margin - thickness - 3, margin + length - 8, h - margin - thickness - 2, colorDim);
        
        // Bottom-right bracket ┘
        context.fill(w - margin - length, h - margin - thickness, w - margin, h - margin, color);
        context.fill(w - margin - thickness, h - margin - length, w - margin, h - margin, color);
        context.fill(w - margin - length + 8, h - margin - thickness - 3, w - margin - thickness - 2, h - margin - thickness - 2, colorDim);
        
        // Small corner dots (accent) - adjacent to bracket ends
        int dotSize = 3;
        int dotAlpha = (int)(alpha * 180);
        int dotColor = (dotAlpha << 24) | COLOR_BRACKET;
        
        // Dots at end of horizontal brackets
        context.fill(margin + length + 3, margin, margin + length + 3 + dotSize, margin + dotSize, dotColor);
        context.fill(w - margin - length - 3 - dotSize, margin, w - margin - length - 3, margin + dotSize, dotColor);
        context.fill(margin + length + 3, h - margin - dotSize, margin + length + 3 + dotSize, h - margin, dotColor);
        context.fill(w - margin - length - 3 - dotSize, h - margin - dotSize, w - margin - length - 3, h - margin, dotColor);
        
        // Dots at end of vertical brackets  
        context.fill(margin, margin + length + 3, margin + dotSize, margin + length + 3 + dotSize, dotColor);
        context.fill(w - margin - dotSize, margin + length + 3, w - margin, margin + length + 3 + dotSize, dotColor);
        context.fill(margin, h - margin - length - 3 - dotSize, margin + dotSize, h - margin - length - 3, dotColor);
        context.fill(w - margin - dotSize, h - margin - length - 3 - dotSize, w - margin, h - margin - length - 3, dotColor);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if local player is wearing the augmented helmet.
     */
    private static boolean hasAugmentedHelmet(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack headSlot = client.player.getEquippedStack(EquipmentSlot.HEAD);
        return !headSlot.isEmpty() && headSlot.isOf(ModItems.AUGMENTED_HELMET);
    }
    
    /**
     * Registers the visor effect renderer.
     */
    public static void init() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
            HelmetVisorEffect::render);
    }
}
