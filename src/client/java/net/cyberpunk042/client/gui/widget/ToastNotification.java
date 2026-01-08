package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiAnimations;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * G124-G126: Toast notification system with animations.
 * 
 * <p>Shows temporary messages that fade in/out smoothly.</p>
 */
public class ToastNotification {
    
    private static final List<Toast> toasts = new ArrayList<>();
    private static final int MAX_TOASTS = 5;
    private static final int DISPLAY_TICKS = 60; // 3 seconds
    private static final int FADE_TICKS = 10;
    
    private final String message;
    private final int color;
    private final long spawnTick;
    
    private ToastNotification(String message, int color) {
        this.message = message;
        this.color = color;
        this.spawnTick = System.currentTimeMillis();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G124: FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void info(String message) {
        add(new Toast(message, GuiConstants.TEXT_PRIMARY, ToastType.INFO));
    }
    
    public static void success(String message) {
        add(new Toast(message, GuiConstants.SUCCESS, ToastType.SUCCESS));
    }
    
    public static void warning(String message) {
        add(new Toast(message, GuiConstants.WARNING, ToastType.WARNING));
    }
    
    public static void error(String message) {
        add(new Toast(message, GuiConstants.ERROR, ToastType.ERROR));
    }
    
    private static void add(Toast toast) {
        synchronized (toasts) {
            toasts.add(toast);
            while (toasts.size() > MAX_TOASTS) {
                toasts.remove(0);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G125: RENDERING WITH ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void renderAll(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight) {
        synchronized (toasts) {
            long now = System.currentTimeMillis();
            int y = screenHeight - 40;
            
            Iterator<Toast> it = toasts.iterator();
            while (it.hasNext()) {
                Toast toast = it.next();
                long elapsed = now - toast.spawnTime;
                long totalDuration = (DISPLAY_TICKS + FADE_TICKS * 2) * 50L; // ticks to ms
                
                if (elapsed > totalDuration) {
                    it.remove();
                    continue;
                }
                
                // Calculate alpha for fade in/out
                float alpha = 1f;
                long fadeInEnd = FADE_TICKS * 50L;
                long fadeOutStart = (DISPLAY_TICKS + FADE_TICKS) * 50L;
                
                if (elapsed < fadeInEnd) {
                    alpha = GuiAnimations.easeInOut(elapsed / (float) fadeInEnd);
                } else if (elapsed > fadeOutStart) {
                    alpha = 1f - GuiAnimations.easeInOut((elapsed - fadeOutStart) / (float) (FADE_TICKS * 50L));
                }
                
                // Slide in from right
                int slideOffset = (int) ((1f - GuiAnimations.easeInOut(Math.min(1f, elapsed / 200f))) * 50);
                
                renderToast(context, textRenderer, toast, screenWidth - slideOffset, y, alpha);
                y -= 24;
            }
        }
    }
    
    private static void renderToast(DrawContext context, TextRenderer textRenderer, Toast toast, int x, int y, float alpha) {
        int textWidth = textRenderer.getWidth(toast.message);
        int padding = 8;
        int toastWidth = textWidth + padding * 2;
        int toastX = x - toastWidth - 10;
        
        // Background with alpha
        int bgAlpha = (int) (alpha * 220);
        int bgColor = (bgAlpha << 24) | 0x1E1E1E;
        context.fill(toastX, y, toastX + toastWidth, y + 20, bgColor);
        
        // Accent bar
        int accentAlpha = (int) (alpha * 255);
        int accentColor = (accentAlpha << 24) | (toast.color & 0x00FFFFFF);
        context.fill(toastX, y, toastX + 3, y + 20, accentColor);
        
        // Text with alpha
        int textAlpha = (int) (alpha * 255);
        int textColor = (textAlpha << 24) | (toast.color & 0x00FFFFFF);
        context.drawText(textRenderer, toast.message, toastX + padding, y + 6, textColor, false);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G126: TOAST DATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    private enum ToastType { INFO, SUCCESS, WARNING, ERROR }
    
    private static class Toast {
        final String message;
        final int color;
        final ToastType type;
        final long spawnTime;
        
        Toast(String message, int color, ToastType type) {
            this.message = message;
            this.color = color;
            this.type = type;
            this.spawnTime = System.currentTimeMillis();
        }
    }
    
    public static void clear() {
        synchronized (toasts) {
            toasts.clear();
        }
    }
}
