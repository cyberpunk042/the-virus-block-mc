package net.cyberpunk042.client.gui.util;

import net.minecraft.client.MinecraftClient;

/**
 * Global GUI theming constants.
 * Change here → updates everywhere.
 * 
 * <p>Now supports responsive scaling based on screen size.</p>
 */
public final class GuiConstants {
    
    private GuiConstants() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSIVE SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Reference height for 1.0 scale */
    public static final int REFERENCE_HEIGHT = 720;
    /** Minimum supported height (below this, scrolling is required) */
    public static final int MIN_HEIGHT = 400;
    /** Minimum scale factor (don't go smaller than this) */
    public static final float MIN_SCALE = 0.65f;
    /** Maximum scale factor (don't go larger than this) */
    public static final float MAX_SCALE = 1.2f;
    
    /**
     * Gets the current scale factor based on screen height.
     * @return Scale factor (typically 0.65 - 1.2)
     */
    public static float getScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 1.0f;
        
        int height = client.getWindow().getScaledHeight();
        float scale = (float) height / REFERENCE_HEIGHT;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }
    
    /**
     * @return true if screen is too small and compact mode should be used
     */
    public static boolean isCompactMode() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return false;
        return client.getWindow().getScaledHeight() < 480;  // Lowered threshold
    }
    
    /**
     * Gets the scaled widget height.
     * In compact mode uses compact height, otherwise scales between 14-20px.
     */
    public static int widgetHeight() {
        if (isCompactMode()) {
            return COMPACT_HEIGHT;
        }
        // Scale widget height, but ensure minimum of 14, max of 20
        int scaled = (int)(WIDGET_HEIGHT * getScale());
        return Math.max(14, Math.min(WIDGET_HEIGHT, scaled));
    }
    
    /**
     * Gets the scaled padding.
     */
    public static int padding() {
        return isCompactMode() ? 2 : Math.max(2, (int)(PADDING * getScale()));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSIONS (base values at 1.0 scale)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int WIDGET_HEIGHT = 20;
    public static final int COMPACT_HEIGHT = 14;    // Compact widget height (for dense panels)
    public static final int COMPACT_GAP = 2;        // Gap between compact widgets
    public static final int BUTTON_WIDTH = 120;
    public static final int BUTTON_WIDTH_WIDE = 200;
    public static final int BUTTON_WIDTH_NARROW = 80;
    public static final int SLIDER_WIDTH = 150;
    public static final int PADDING = 4;
    public static final int SECTION_GAP = 12;
    public static final int SECTION_SPACING = 20;
    public static final int MARGIN = 10;
    public static final int TAB_HEIGHT = 24;
    public static final int BOTTOM_BAR_HEIGHT = 28;
    public static final int HEADER_HEIGHT = 16;
    public static final int LABEL_WIDTH = 80;
    
    // Scaled getters for common dimensions
    public static int sectionGap() { return isCompactMode() ? 6 : (int)(SECTION_GAP * getScale()); }
    public static int margin() { return Math.max(4, (int)(MARGIN * getScale())); }
    
    // Aliases for sub-panel consistency
    public static final int ELEMENT_HEIGHT = WIDGET_HEIGHT;
    public static final int ELEMENT_SPACING = SECTION_GAP;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (ARGB: 0xAARRGGBB)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Backgrounds
    public static final int BG_PRIMARY = 0xE0101010;
    public static final int BG_SECONDARY = 0xFF1A1A1A;
    public static final int BG_SCREEN = BG_PRIMARY;  // Alias
    public static final int BG_PANEL = 0xFF1E1E1E;
    public static final int BG_WIDGET = 0xFF2D2D2D;
    public static final int BG_WIDGET_HOVER = 0xFF3D3D3D;
    public static final int BG_WIDGET_FOCUS = 0xFF4D4D4D;
    public static final int BG_HEADER = 0xFF252525;
    
    // Text
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_DISABLED = 0xFF666666;
    public static final int TEXT_LABEL = 0xFFCCCCCC;
    
    // Accent
    public static final int ACCENT = 0xFF4488FF;
    public static final int ACCENT_HOVER = 0xFF66AAFF;
    
    // Status
    public static final int SUCCESS = 0xFF44DD44;
    public static final int WARNING = 0xFFDDAA00;
    public static final int ERROR = 0xFFFF4444;
    
    // Border
    public static final int BORDER = 0xFF404040;
    public static final int BORDER_FOCUS = 0xFF4488FF;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int FADE_DURATION_MS = 150;
    public static final float HOVER_SCALE = 1.02f;
}

