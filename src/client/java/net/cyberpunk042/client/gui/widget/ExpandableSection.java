package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * G30: Expandable section with header and collapsible content.
 * Header shows ▸ (collapsed) or ▾ (expanded).
 */
public class ExpandableSection {
    
    private static final String COLLAPSED_ICON = "▸ ";
    private static final String EXPANDED_ICON = "▾ ";
    
    private final String title;
    private final ButtonWidget headerButton;
    private final List<Runnable> contentRenderers = new ArrayList<>();
    
    private boolean expanded = false;
    private int x, y, width;
    private int contentHeight = 0;
    private Consumer<Boolean> onToggle;
    
    /**
     * Creates an expandable section.
     * @param x X position
     * @param y Y position
     * @param width Section width
     * @param title Header title
     * @param initiallyExpanded Whether to start expanded
     */
    public ExpandableSection(int x, int y, int width, String title, boolean initiallyExpanded) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.expanded = initiallyExpanded;
        
        this.headerButton = ButtonWidget.builder(
            Text.literal(getHeaderText()),
            btn -> toggle()
        ).dimensions(x, y, width, GuiConstants.WIDGET_HEIGHT).build();
        
        Logging.GUI.topic("widget").trace("ExpandableSection created: {}", title);
    }
    
    private String getHeaderText() {
        return (expanded ? EXPANDED_ICON : COLLAPSED_ICON) + title;
    }
    
    /**
     * Toggles the expanded state.
     */
    public void toggle() {
        expanded = !expanded;
        headerButton.setMessage(Text.literal(getHeaderText()));
        Logging.GUI.topic("widget").trace("ExpandableSection {} toggled: {}", title, expanded);
        if (onToggle != null) {
            onToggle.accept(expanded);
        }
    }
    
    /**
     * Sets the toggle callback.
     */
    public void setOnToggle(Consumer<Boolean> callback) {
        this.onToggle = callback;
    }
    
    /**
     * Returns whether section is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }
    
    /**
     * Sets the content height (for layout calculations).
     */
    public void setContentHeight(int height) {
        this.contentHeight = height;
    }
    
    /**
     * Gets the total height of this section.
     */
    public int getTotalHeight() {
        return GuiConstants.WIDGET_HEIGHT + (expanded ? contentHeight + GuiConstants.PADDING : 0);
    }
    
    /**
     * Gets the Y position where content should start.
     */
    public int getContentY() {
        return y + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
    }
    
    /**
     * Renders the section header and background.
     */
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        // Header button
        headerButton.render(context, mouseX, mouseY, delta);
        
        // Content background if expanded
        if (expanded && contentHeight > 0) {
            int bgY = y + GuiConstants.WIDGET_HEIGHT;
            context.fill(x, bgY, x + width, bgY + contentHeight + GuiConstants.PADDING, GuiConstants.BG_WIDGET);
        }
    }
    
    public ButtonWidget getHeaderButton() {
        return headerButton;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    
    /**
     * Offsets the section's internal position by the given delta.
     * Also moves the headerButton to match.
     */
    public void offsetInternalPosition(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        // Also move the header button
        headerButton.setX(headerButton.getX() + dx);
        headerButton.setY(headerButton.getY() + dy);
    }
    
    public static ExpandableSection create(int x, int y, int width, String title) {
        return new ExpandableSection(x, y, width, title, false);
    }
}
