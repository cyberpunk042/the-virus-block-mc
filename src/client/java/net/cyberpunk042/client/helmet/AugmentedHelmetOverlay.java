package net.cyberpunk042.client.helmet;

import net.cyberpunk042.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Renders the Augmented Helmet HUD overlay.
 * 
 * <h2>Phase 2: Polished Version</h2>
 * <ul>
 *   <li>Player-relative direction arrow</li>
 *   <li>Threat level coloring (dynamic)</li>
 *   <li>Smooth animations and fade transitions</li>
 *   <li>Critical threat blinking</li>
 *   <li>Filled arrow with glow effect</li>
 * </ul>
 * 
 * <p>Registered with {@link net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback}.</p>
 */
public final class AugmentedHelmetOverlay {
    
    private AugmentedHelmetOverlay() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final int BASE_PANEL_WIDTH = 100;
    private static final int BASE_PANEL_HEIGHT = 55;
    private static final int MARGIN = 6;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (Dark Sci-Fi Theme - Subtle)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final int COLOR_PANEL_BG = 0xA0101010;       // Dark gray, more transparent
    private static final int COLOR_HEADER_BG = 0xB0001818;      // Very dark teal
    private static final int COLOR_HEADER_TEXT = 0xDD00CCCC;    // Muted cyan
    private static final int COLOR_ACCENT = 0xCC00AAAA;         // Desaturated cyan
    private static final int COLOR_TEXT_PRIMARY = 0xDDCCCCCC;   // Light gray (not pure white)
    private static final int COLOR_TEXT_SECONDARY = 0xAA888888; // Dim gray
    private static final int COLOR_WARNING = 0xCCCC6600;        // Muted orange
    private static final int COLOR_CRITICAL = 0xCCCC3333;       // Darker red
    private static final int COLOR_BAR_BG = 0x80181818;         // Dark gray, semi-transparent
    private static final int COLOR_SAFE = 0xCC00AA66;           // Muted teal-green
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static long lastRenderTime = 0;
    private static float pulsePhase = 0f;
    private static float criticalPulse = 0f;
    private static float displayedArrowAngle = 0f;  // For smooth arrow rotation
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Main render method. Called by HudRenderCallback.
     */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        // Check if helmet HUD is enabled
        HelmetHudConfig config = HelmetHudConfig.get();
        if (!config.isHudEnabled()) return;
        
        // Check if wearing augmented helmet
        if (!hasAugmentedHelmet(client)) return;
        
        // Update state interpolation
        HelmetHudState state = HelmetHudState.get();
        state.tick();
        
        // HUD ALWAYS shows when helmet is worn - no fade out
        // Shows "NO SIGNAL" or actual data based on state.hasData()
        
        // Update animation phases
        long now = System.currentTimeMillis();
        float deltaMs = lastRenderTime > 0 ? Math.min(now - lastRenderTime, 100f) : 16f;
        lastRenderTime = now;
        
        // Slow pulse for border (0.003 = ~2 second cycle)
        pulsePhase += deltaMs * 0.003f;
        if (pulsePhase > Math.PI * 2) pulsePhase -= (float)(Math.PI * 2);
        
        // Fast pulse for critical threat (0.015 = ~0.4 second cycle)
        criticalPulse += deltaMs * 0.015f;
        if (criticalPulse > Math.PI * 2) criticalPulse -= (float)(Math.PI * 2);
        
        // Smooth arrow rotation (lerp toward target)
        float targetAngle = calculateRelativeAngle(client, state);
        displayedArrowAngle = lerpAngle(displayedArrowAngle, targetAngle, deltaMs * 0.008f);
        
        // Calculate panel dimensions with scaling
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        float scale = calculateScale(client, config);
        int panelWidth = (int)(BASE_PANEL_WIDTH * scale);
        int panelHeight = (int)(BASE_PANEL_HEIGHT * scale);
        
        // Position (top-right corner)
        int panelX = screenWidth - panelWidth - MARGIN;
        int panelY = MARGIN;
        
        // Always full opacity when helmet is worn
        float alpha = config.getHudOpacity();
        
        // Render panel
        renderPanel(context, client, state, panelX, panelY, panelWidth, panelHeight, scale, alpha);
    }
    
    /**
     * Calculates the arrow angle relative to player facing direction.
     */
    private static float calculateRelativeAngle(MinecraftClient client, HelmetHudState state) {
        if (client.player == null) return 0f;
        
        // World yaw to target (from server, already in MC convention)
        float worldYaw = state.getYaw();
        
        // Player's current looking direction
        float playerYaw = client.player.getYaw();
        
        // Normalize player yaw to 0-360
        while (playerYaw < 0) playerYaw += 360;
        while (playerYaw >= 360) playerYaw -= 360;
        
        // Relative angle: when looking directly at target, arrow points up (0°)
        // worldYaw and playerYaw are in same convention now
        float relativeAngle = worldYaw - playerYaw;
        
        // Normalize to 0-360
        while (relativeAngle < 0) relativeAngle += 360;
        while (relativeAngle >= 360) relativeAngle -= 360;
        
        return relativeAngle;
    }
    
    /**
     * Lerps between angles, handling wraparound correctly.
     */
    private static float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        float result = from + diff * Math.min(t, 1f);
        while (result < 0) result += 360;
        while (result >= 360) result -= 360;
        return result;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PANEL RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderPanel(DrawContext context, MinecraftClient client,
                                     HelmetHudState state, int x, int y, int w, int h,
                                     float scale, float alpha) {
        
        TextRenderer font = client.textRenderer;
        boolean hasData = state.hasData();
        HelmetHudState.ThreatLevel threatLevel = hasData ? state.getThreatLevel() : HelmetHudState.ThreatLevel.NONE;
        
        // Determine accent color based on threat level
        int accentColor = getThreatColor(threatLevel, true);
        
        // Background with border pulse
        float pulse = (float)(Math.sin(pulsePhase) * 0.5 + 0.5);
        
        // Critical threats have faster pulse
        if (threatLevel == HelmetHudState.ThreatLevel.CRITICAL) {
            pulse = (float)(Math.sin(criticalPulse) * 0.5 + 0.5);
        }
        
        int borderAlpha = (int)(alpha * (120 + pulse * 80));
        int borderColor = (borderAlpha << 24) | (accentColor & 0x00FFFFFF);
        
        // Outer glow (2px border)
        int glowAlpha = (int)(alpha * 40 * pulse);
        int glowColor = (glowAlpha << 24) | (accentColor & 0x00FFFFFF);
        context.fill(x - 3, y - 3, x + w + 3, y + h + 3, glowColor);
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);
        
        // Main border
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, borderColor);
        
        // Panel background
        int bgColor = applyAlpha(COLOR_PANEL_BG, alpha);
        context.fill(x, y, x + w, y + h, bgColor);
        
        // Header bar with threat color
        int headerHeight = (int)(14 * scale);
        int headerBg = blendColors(COLOR_HEADER_BG, accentColor, 0.2f);
        context.fill(x, y, x + w, y + headerHeight, applyAlpha(headerBg, alpha));
        
        // Header accent line
        int lineColor = applyAlpha(accentColor, alpha * 0.8f);
        context.fill(x, y + headerHeight - 1, x + w, y + headerHeight, lineColor);
        
        // Header text
        String headerText = "◈ THREAT SCANNER";
        int textColor = applyAlpha(COLOR_HEADER_TEXT, alpha);
        context.drawText(font, headerText, x + 4, y + 3, textColor, false);
        
        // Status indicator (pulsing dot)
        int dotX = x + w - 14;
        int dotY = y + 3;
        int dotSize = (int)(8 * scale);
        int dotColor = getThreatColor(threatLevel, threatLevel == HelmetHudState.ThreatLevel.CRITICAL && pulse > 0.5f);
        context.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, applyAlpha(dotColor, alpha));
        
        // Content area starts after header
        int contentY = y + headerHeight + 4;
        int contentX = x + 4;
        int contentWidth = w - 8;
        
        if (!hasData) {
            // NO SIGNAL state
            String noSignal = "NO SIGNAL";
            int nsWidth = font.getWidth(noSignal);
            context.drawText(font, noSignal, x + (w - nsWidth) / 2, contentY + 12, 
                applyAlpha(COLOR_TEXT_SECONDARY, alpha), true);
            return;
        }
        
        // Layout: [Arrow] [Distance + Direction]
        //                 [Signal bar]
        //                 [Count]
        
        // Small arrow on the left
        int arrowSize = 16;
        int arrowCenterX = contentX + arrowSize / 2 + 2;
        int arrowCenterY = contentY + arrowSize / 2 + 4;
        renderSmallArrow(context, arrowCenterX, arrowCenterY, displayedArrowAngle, 
            applyAlpha(accentColor, alpha), arrowSize);
        
        // Text info on the right of arrow
        int textX = contentX + arrowSize + 6;
        int textY = contentY + 2;
        
        // Line 1: Distance + direction (distance from virus block telemetry)
        int dist = (int)net.cyberpunk042.client.visual.shader.VirusBlockTelemetryState.get().getClosestDistance();
        String distText = dist + "m " + state.getCompassDirection();
        int distColor = dist < 20 ? getThreatColor(HelmetHudState.ThreatLevel.CRITICAL, true) : COLOR_TEXT_PRIMARY;
        context.drawText(font, distText, textX, textY, applyAlpha(distColor, alpha), false);
        
        // Line 2: Signal bar (mini)
        textY += 11;
        int barWidth = contentWidth - arrowSize - 10;
        int barHeight = 4;
        renderSignalBar(context, textX, textY, barWidth, barHeight, state.getSignalStrength(), alpha, threatLevel);
        
        // Line 3: Source count
        textY += 8;
        int count = state.getTotalCount();
        int countColor = count > 5 ? COLOR_WARNING : COLOR_TEXT_SECONDARY;
        String countText = count == 1 ? "1 source" : count + " sources";
        context.drawText(font, countText, textX, textY, applyAlpha(countColor, alpha), false);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SMALL ARROW (simple triangle)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderSmallArrow(DrawContext context, int cx, int cy, 
                                          float angle, int color, int size) {
        double rad = Math.toRadians(angle - 90);
        int half = size / 2;
        
        // Tip point
        int tipX = cx + (int)(Math.cos(rad) * half);
        int tipY = cy + (int)(Math.sin(rad) * half);
        
        // Base points (perpendicular to direction)
        double perpRad = rad + Math.PI / 2;
        int baseOffset = half / 2;
        int backOffset = half / 2;
        
        int base1X = cx - (int)(Math.cos(rad) * backOffset) + (int)(Math.cos(perpRad) * baseOffset);
        int base1Y = cy - (int)(Math.sin(rad) * backOffset) + (int)(Math.sin(perpRad) * baseOffset);
        int base2X = cx - (int)(Math.cos(rad) * backOffset) - (int)(Math.cos(perpRad) * baseOffset);
        int base2Y = cy - (int)(Math.sin(rad) * backOffset) - (int)(Math.sin(perpRad) * baseOffset);
        
        // Draw triangle
        drawFilledTriangle(context, tipX, tipY, base1X, base1Y, base2X, base2Y, color, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECTION ARROW (Filled with glow)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderDirectionArrow(DrawContext context, int cx, int cy, 
                                              float angle, float alpha, float scale,
                                              int accentColor, float pulse) {
        // Convert angle to radians (0 = up/north, 90 = right/east)
        double rad = Math.toRadians(angle - 90);  // -90 to make 0 point up
        
        int arrowLength = (int)(14 * scale);
        int arrowWidth = (int)(10 * scale);
        
        // Calculate arrow points
        double tipDx = Math.cos(rad) * arrowLength;
        double tipDy = Math.sin(rad) * arrowLength;
        
        double perpRad = rad + Math.PI / 2;
        double baseDx = Math.cos(perpRad) * arrowWidth / 2;
        double baseDy = Math.sin(perpRad) * arrowWidth / 2;
        
        double backDx = Math.cos(rad) * arrowLength * 0.4;
        double backDy = Math.sin(rad) * arrowLength * 0.4;
        
        // Arrow vertices
        int tipX = cx + (int)tipDx;
        int tipY = cy + (int)tipDy;
        int base1X = cx - (int)backDx + (int)baseDx;
        int base1Y = cy - (int)backDy + (int)baseDy;
        int base2X = cx - (int)backDx - (int)baseDx;
        int base2Y = cy - (int)backDy - (int)baseDy;
        
        // Glow effect (slightly larger, semi-transparent)
        int glowAlpha = (int)(alpha * 80 * (0.5f + pulse * 0.5f));
        int glowColor = (glowAlpha << 24) | (accentColor & 0x00FFFFFF);
        drawFilledTriangle(context, tipX, tipY, base1X, base1Y, base2X, base2Y, glowColor, 2);
        
        // Main arrow (filled)
        int mainColor = applyAlpha(accentColor, alpha);
        drawFilledTriangle(context, tipX, tipY, base1X, base1Y, base2X, base2Y, mainColor, 0);
        
        // Center dot
        int dotSize = (int)(3 * scale);
        context.fill(cx - dotSize/2, cy - dotSize/2, cx + dotSize/2 + 1, cy + dotSize/2 + 1, 
            applyAlpha(COLOR_TEXT_PRIMARY, alpha * 0.8f));
    }
    
    /**
     * Draws a filled triangle using horizontal scanlines.
     */
    private static void drawFilledTriangle(DrawContext context, int x1, int y1, int x2, int y2, 
                                            int x3, int y3, int color, int expand) {
        // Sort vertices by Y coordinate
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
        if (y1 > y3) { int t = y1; y1 = y3; y3 = t; t = x1; x1 = x3; x3 = t; }
        if (y2 > y3) { int t = y2; y2 = y3; y3 = t; t = x2; x2 = x3; x3 = t; }
        
        // Avoid division by zero
        if (y3 == y1) {
            int minX = Math.min(Math.min(x1, x2), x3) - expand;
            int maxX = Math.max(Math.max(x1, x2), x3) + expand;
            context.fill(minX, y1 - expand, maxX, y1 + 1 + expand, color);
            return;
        }
        
        // Scanline fill
        for (int y = y1 - expand; y <= y3 + expand; y++) {
            float t1, t2;
            int xa, xb;
            
            if (y < y1) {
                // Above triangle
                xa = x1; xb = x1;
            } else if (y <= y2) {
                // Upper half
                t1 = (y3 != y1) ? (float)(y - y1) / (y3 - y1) : 0;
                t2 = (y2 != y1) ? (float)(y - y1) / (y2 - y1) : 0;
                xa = x1 + (int)(t1 * (x3 - x1));
                xb = x1 + (int)(t2 * (x2 - x1));
            } else if (y <= y3) {
                // Lower half
                t1 = (y3 != y1) ? (float)(y - y1) / (y3 - y1) : 0;
                t2 = (y3 != y2) ? (float)(y - y2) / (y3 - y2) : 0;
                xa = x1 + (int)(t1 * (x3 - x1));
                xb = x2 + (int)(t2 * (x3 - x2));
            } else {
                // Below triangle
                xa = x3; xb = x3;
            }
            
            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            context.fill(xa - expand, y, xb + 1 + expand, y + 1, color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SIGNAL BAR
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderSignalBar(DrawContext context, int x, int y, int width, int height,
                                         float strength, float alpha, HelmetHudState.ThreatLevel level) {
        // Background
        context.fill(x, y, x + width, y + height, applyAlpha(COLOR_BAR_BG, alpha));
        
        // Fill based on strength (strength = 1 when close, 0 when far)
        int fillWidth = (int)(width * MathHelper.clamp(strength, 0f, 1f));
        if (fillWidth > 0) {
            // Color based on threat level (matches the overall theme)
            int fillColor = getThreatColor(level, true);
            context.fill(x, y, x + fillWidth, y + height, applyAlpha(fillColor, alpha));
        }
        
        // Tick marks for segments
        int segments = 10;
        int segWidth = width / segments;
        for (int i = 1; i < segments; i++) {
            int tickX = x + i * segWidth;
            context.fill(tickX, y, tickX + 1, y + height, applyAlpha(COLOR_PANEL_BG, alpha * 0.5f));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int getThreatColor(HelmetHudState.ThreatLevel level, boolean bright) {
        return switch (level) {
            case CRITICAL -> bright ? COLOR_CRITICAL : COLOR_WARNING;
            case HIGH -> COLOR_WARNING;
            case MEDIUM -> 0xFFDDDD00;  // Yellow
            case LOW -> COLOR_SAFE;
            case NONE -> COLOR_TEXT_SECONDARY;
        };
    }
    
    private static int blendColors(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        
        return (a1 << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static float calculateScale(MinecraftClient client, HelmetHudConfig config) {
        double guiScale = client.getWindow().getScaleFactor();
        
        // Apply slight adaptation (not 1:1, just a bit)
        float adaptFactor = 1.0f + (float)(guiScale - 2.0) * 0.1f;
        
        // Clamp to reasonable range and apply user scale
        adaptFactor = MathHelper.clamp(adaptFactor, 0.85f, 1.15f);
        return adaptFactor * config.getHudScale();
    }
    
    private static int applyAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * MathHelper.clamp(alpha, 0f, 1f));
        return (a << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * Checks if local player is wearing the augmented helmet.
     */
    private static boolean hasAugmentedHelmet(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack headSlot = client.player.getEquippedStack(EquipmentSlot.HEAD);
        return !headSlot.isEmpty() && headSlot.isOf(ModItems.AUGMENTED_HELMET);
    }
}
