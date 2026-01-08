package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * G27-G29: Color button with swatch display, hex input, and theme colors.
 * Click to open a simple hex input popup or cycle theme colors.
 */
public class ColorButton extends ButtonWidget {
    
    // Theme color constants
    public static final int THEME_PRIMARY = 0xFF00FFFF;    // Cyan
    public static final int THEME_SECONDARY = 0xFFFF00FF;  // Magenta
    public static final int THEME_ACCENT = 0xFFFFFF00;     // Yellow
    public static final int THEME_SUCCESS = 0xFF00FF00;    // Green
    public static final int THEME_WARNING = 0xFFFFAA00;    // Orange
    public static final int THEME_ERROR = 0xFFFF0000;      // Red
    
    private static final int[] THEME_COLORS = {
        THEME_PRIMARY, THEME_SECONDARY, THEME_ACCENT, 
        THEME_SUCCESS, THEME_WARNING, THEME_ERROR
    };
    private static final String[] THEME_NAMES = {
        "@primary", "@secondary", "@accent",
        "@success", "@warning", "@error"
    };
    
    private int currentColor;
    private int themeIndex = -1; // -1 = custom color
    private final Consumer<Integer> onChange;
    private Runnable onRightClick; // Callback for right-click to open modal
    
    /**
     * Creates a color button.
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param label Display label
     * @param initialColor Initial color (ARGB)
     * @param onChange Callback when color changes
     */
    public ColorButton(int x, int y, int width, String label, int initialColor, Consumer<Integer> onChange) {
        super(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label), 
              btn -> ((ColorButton) btn).cycleColor(), DEFAULT_NARRATION_SUPPLIER);
        this.currentColor = initialColor;
        this.onChange = onChange;
        
        // Check if initial is a theme color
        for (int i = 0; i < THEME_COLORS.length; i++) {
            if (THEME_COLORS[i] == initialColor) {
                themeIndex = i;
                break;
            }
        }
        
        Logging.GUI.topic("widget").trace("ColorButton created: {}", label);
    }
    
    /**
     * Sets a callback for when the button is right-clicked.
     * This should show a modal dialog for entering color values.
     */
    public void setRightClickHandler(Runnable handler) {
        this.onRightClick = handler;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            if (button == 1) { // Right-click: open color input modal
                if (onRightClick != null) {
                    this.playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                    onRightClick.run();
                    return true;
                }
                // No handler set - do nothing on right-click
                return false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void cycleColor() {
        themeIndex = (themeIndex + 1) % THEME_COLORS.length;
        currentColor = THEME_COLORS[themeIndex];
        Logging.GUI.topic("widget").trace("ColorButton cycled to: {} ({})", 
            THEME_NAMES[themeIndex], Integer.toHexString(currentColor));
        if (onChange != null) {
            onChange.accept(currentColor);
        }
    }
    
    private void cycleColorBackward() {
        themeIndex = themeIndex <= 0 ? THEME_COLORS.length - 1 : themeIndex - 1;
        currentColor = THEME_COLORS[themeIndex];
        Logging.GUI.topic("widget").trace("ColorButton cycled backward to: {} ({})", 
            THEME_NAMES[themeIndex], Integer.toHexString(currentColor));
        if (onChange != null) {
            onChange.accept(currentColor);
        }
    }
    
    /**
     * Sets a color from a string value.
     * Supports theme references like @primary, @beam, or hex codes like #FF00FF.
     * 
     * @param colorString Color in format "#RRGGBB", "RRGGBB", or "@themeName"
     */
    public void setColorString(String colorString) {
        if (colorString == null || colorString.isEmpty()) return;
        
        String trimmed = colorString.trim();
        
        // Check for @theme reference
        if (trimmed.startsWith("@")) {
            for (int i = 0; i < THEME_NAMES.length; i++) {
                if (THEME_NAMES[i].equalsIgnoreCase(trimmed)) {
                    themeIndex = i;
                    currentColor = THEME_COLORS[i];
                    if (onChange != null) {
                        onChange.accept(currentColor);
                    }
                    Logging.GUI.topic("widget").trace("ColorButton set to theme: {}", trimmed);
                    return;
                }
            }
            Logging.GUI.topic("widget").warn("Unknown theme color: {}", trimmed);
            return;
        }
        
        // Try parsing as hex color
        try {
            String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
            currentColor = 0xFF000000 | Integer.parseInt(hex, 16);
            themeIndex = -1; // Custom color
            if (onChange != null) {
                onChange.accept(currentColor);
            }
            Logging.GUI.topic("widget").trace("ColorButton set to hex: #{}", hex);
        } catch (NumberFormatException e) {
            Logging.GUI.topic("widget").warn("Invalid color value: {}", colorString);
        }
    }
    
    /**
     * Sets a custom hex color.
     * @param hexColor Color in format "#RRGGBB" or "RRGGBB"
     * @deprecated Use {@link #setColorString(String)} instead for broader format support
     */
    @Deprecated
    public void setHexColor(String hexColor) {
        setColorString(hexColor);
    }
    
    /**
     * Gets the current color.
     */
    public int getColor() {
        return currentColor;
    }
    
    /**
     * Gets the current color as hex string.
     */
    public String getHexColor() {
        return String.format("#%06X", currentColor & 0xFFFFFF);
    }
    
    /**
     * Gets the theme name if using a theme color, or null.
     */
    public String getThemeName() {
        return themeIndex >= 0 ? THEME_NAMES[themeIndex] : null;
    }
    
    /**
     * Gets the current color as a string suitable for re-input.
     * Returns theme name (e.g., "@primary") if using a theme color, or hex code otherwise.
     */
    public String getColorString() {
        return themeIndex >= 0 ? THEME_NAMES[themeIndex] : getHexColor();
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        
        // Draw color swatch on the right side
        int swatchSize = height - 4;
        int swatchX = getX() + width - swatchSize - 2;
        int swatchY = getY() + 2;
        
        // Border
        context.fill(swatchX - 1, swatchY - 1, swatchX + swatchSize + 1, swatchY + swatchSize + 1, 0xFF000000);
        // Color fill
        context.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, currentColor);
    }
    
    public static ColorButton create(int x, int y, String label, int color, Consumer<Integer> onChange) {
        return new ColorButton(x, y, GuiConstants.BUTTON_WIDTH, label, color, onChange);
    }
}
