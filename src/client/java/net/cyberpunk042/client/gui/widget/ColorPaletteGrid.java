package net.cyberpunk042.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A color palette grid widget showing predefined color swatches.
 * Click on a color to select it. Provides a quick visual color picker.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Theme colors row (primary, secondary, accent, etc.)</li>
 *   <li>Rainbow spectrum row</li>
 *   <li>Grayscale row</li>
 *   <li>Popular colors row (pastel, neon, etc.)</li>
 * </ul>
 */
public class ColorPaletteGrid implements Drawable, Element {
    
    // === THEME COLORS (match ColorButton) ===
    public static final int[] THEME_COLORS = {
        0xFF00FFFF,  // @primary (Cyan)
        0xFFFF00FF,  // @secondary (Magenta)
        0xFFFFFF00,  // @accent (Yellow)
        0xFF00FF00,  // @success (Green)
        0xFFFFAA00,  // @warning (Orange)
        0xFFFF0000,  // @error (Red)
        0xFF0080FF,  // @beam (Blue)
        0xFFFF80FF,  // Pink
    };
    public static final String[] THEME_NAMES = {
        "@primary", "@secondary", "@accent", "@success", 
        "@warning", "@error", "@beam", "Pink"
    };
    
    // === RAINBOW SPECTRUM ===
    private static final int[] RAINBOW = {
        0xFFFF0000,  // Red
        0xFFFF4000,  // Red-Orange
        0xFFFF8000,  // Orange
        0xFFFFBF00,  // Yellow-Orange
        0xFFFFFF00,  // Yellow
        0xFFBFFF00,  // Yellow-Green
        0xFF80FF00,  // Lime
        0xFF00FF00,  // Green
        0xFF00FF80,  // Green-Cyan
        0xFF00FFFF,  // Cyan
        0xFF00BFFF,  // Cyan-Blue
        0xFF0080FF,  // Light Blue
        0xFF0000FF,  // Blue
        0xFF4000FF,  // Blue-Purple
        0xFF8000FF,  // Purple
        0xFFBF00FF,  // Purple-Magenta
    };
    
    // === GRAYSCALE ===
    private static final int[] GRAYSCALE = {
        0xFFFFFFFF,  // White
        0xFFE0E0E0,  // Light Gray
        0xFFC0C0C0,  // Silver
        0xFFA0A0A0,  // Gray
        0xFF808080,  // Medium Gray
        0xFF606060,  // Dark Gray
        0xFF404040,  // Darker
        0xFF202020,  // Almost Black
    };
    
    // === NEON / BRIGHT ===
    private static final int[] NEON = {
        0xFFFF1493,  // Deep Pink
        0xFFFF69B4,  // Hot Pink
        0xFFDC143C,  // Crimson
        0xFFFF6347,  // Tomato
        0xFFFFA500,  // Orange
        0xFFFFD700,  // Gold
        0xFF7FFF00,  // Chartreuse
        0xFF00FA9A,  // Medium Spring Green
    };
    
    // === PASTELS ===
    private static final int[] PASTEL = {
        0xFFFFB6C1,  // Light Pink
        0xFFFFE4E1,  // Misty Rose
        0xFFFAFAD2,  // Light Goldenrod
        0xFFE0FFE0,  // Honeydew-ish
        0xFFE0FFFF,  // Light Cyan
        0xFFE6E6FA,  // Lavender
        0xFFDDA0DD,  // Plum
        0xFFFFC0CB,  // Pink
    };
    
    private final int x, y;
    private final int swatchSize;
    private final int gap;
    private final int columns;
    private final Consumer<Integer> onColorSelected;
    private final Consumer<String> onThemeSelected;
    
    private int hoveredColor = -1;
    private int height;
    
    /**
     * Creates a color palette grid.
     * 
     * @param x X position
     * @param y Y position
     * @param width Total width available
     * @param onColorSelected Callback when a regular color is selected (ARGB)
     * @param onThemeSelected Callback when a theme color is selected (@name)
     */
    public ColorPaletteGrid(int x, int y, int width, 
                           Consumer<Integer> onColorSelected,
                           Consumer<String> onThemeSelected) {
        this.x = x;
        this.y = y;
        this.swatchSize = 16;
        this.gap = 2;
        this.columns = width / (swatchSize + gap);
        this.onColorSelected = onColorSelected;
        this.onThemeSelected = onThemeSelected;
        
        // Calculate total height (5 rows: theme, rainbow(2), grayscale, neon, pastel)
        int rows = 6;
        this.height = rows * (swatchSize + gap) + (rows - 1) * 4; // Extra gap between sections
    }
    
    public int getHeight() { return height; }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentY = y;
        int rowHeight = swatchSize + gap;
        
        // Row 1: Theme Colors (with labels on hover)
        renderColorRow(context, THEME_COLORS, currentY, mouseX, mouseY, true);
        currentY += rowHeight + 4;
        
        // Row 2-3: Rainbow (split into 2 rows of 8)
        renderColorRow(context, slice(RAINBOW, 0, 8), currentY, mouseX, mouseY, false);
        currentY += rowHeight;
        renderColorRow(context, slice(RAINBOW, 8, 16), currentY, mouseX, mouseY, false);
        currentY += rowHeight + 4;
        
        // Row 4: Grayscale
        renderColorRow(context, GRAYSCALE, currentY, mouseX, mouseY, false);
        currentY += rowHeight + 4;
        
        // Row 5: Neon
        renderColorRow(context, NEON, currentY, mouseX, mouseY, false);
        currentY += rowHeight;
        
        // Row 6: Pastel
        renderColorRow(context, PASTEL, currentY, mouseX, mouseY, false);
    }
    
    private void renderColorRow(DrawContext context, int[] colors, int rowY, 
                                int mouseX, int mouseY, boolean isTheme) {
        for (int i = 0; i < colors.length && i < columns; i++) {
            int sx = x + i * (swatchSize + gap);
            int sy = rowY;
            
            boolean hovered = mouseX >= sx && mouseX < sx + swatchSize &&
                             mouseY >= sy && mouseY < sy + swatchSize;
            
            // Border (highlight if hovered)
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF000000;
            context.fill(sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1, borderColor);
            
            // Color fill
            context.fill(sx, sy, sx + swatchSize, sy + swatchSize, colors[i]);
            
            if (hovered) {
                hoveredColor = colors[i];
            }
        }
    }
    
    /**
     * Handles mouse click and returns true if a color was selected.
     */
    public boolean handleClick(double mouseX, double mouseY) {
        int currentY = y;
        int rowHeight = swatchSize + gap;
        
        // Theme Colors row
        int themeIdx = getClickedIndex(mouseX, mouseY, THEME_COLORS.length, currentY);
        if (themeIdx >= 0) {
            if (onThemeSelected != null) onThemeSelected.accept(THEME_NAMES[themeIdx]);
            return true;
        }
        currentY += rowHeight + 4;
        
        // Rainbow row 1
        int rainbowIdx1 = getClickedIndex(mouseX, mouseY, 8, currentY);
        if (rainbowIdx1 >= 0) {
            if (onColorSelected != null) onColorSelected.accept(RAINBOW[rainbowIdx1]);
            return true;
        }
        currentY += rowHeight;
        
        // Rainbow row 2
        int rainbowIdx2 = getClickedIndex(mouseX, mouseY, 8, currentY);
        if (rainbowIdx2 >= 0) {
            if (onColorSelected != null) onColorSelected.accept(RAINBOW[rainbowIdx2 + 8]);
            return true;
        }
        currentY += rowHeight + 4;
        
        // Grayscale
        int grayIdx = getClickedIndex(mouseX, mouseY, GRAYSCALE.length, currentY);
        if (grayIdx >= 0) {
            if (onColorSelected != null) onColorSelected.accept(GRAYSCALE[grayIdx]);
            return true;
        }
        currentY += rowHeight + 4;
        
        // Neon
        int neonIdx = getClickedIndex(mouseX, mouseY, NEON.length, currentY);
        if (neonIdx >= 0) {
            if (onColorSelected != null) onColorSelected.accept(NEON[neonIdx]);
            return true;
        }
        currentY += rowHeight;
        
        // Pastel
        int pastelIdx = getClickedIndex(mouseX, mouseY, PASTEL.length, currentY);
        if (pastelIdx >= 0) {
            if (onColorSelected != null) onColorSelected.accept(PASTEL[pastelIdx]);
            return true;
        }
        
        return false;
    }
    
    private int getClickedIndex(double mouseX, double mouseY, int count, int rowY) {
        if (mouseY < rowY || mouseY >= rowY + swatchSize) return -1;
        
        for (int i = 0; i < count && i < columns; i++) {
            int sx = x + i * (swatchSize + gap);
            if (mouseX >= sx && mouseX < sx + swatchSize) {
                return i;
            }
        }
        return -1;
    }
    
    private static int[] slice(int[] arr, int from, int to) {
        int[] result = new int[to - from];
        System.arraycopy(arr, from, result, 0, to - from);
        return result;
    }
    
    // Element interface
    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean focused) {}
}
