package net.cyberpunk042.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A proper dropdown/select widget for Minecraft GUIs.
 * 
 * <p>Unlike CyclingButtonWidget, this shows a scrollable popup list
 * when clicked, allowing selection from many options.</p>
 */
public class DropdownWidget<T> extends ButtonWidget {
    
    private final TextRenderer textRenderer;
    private final List<T> options;
    private final Function<T, Text> labelProvider;
    private final Consumer<T> onSelect;
    
    private int selectedIndex = 0;
    private boolean expanded = false;
    private int scrollOffset = 0;
    private int maxVisibleItems = 6;
    private int hoveredIndex = -1;
    
    // Instance tracking for closing other dropdowns
    private static DropdownWidget<?> activeDropdown = null;
    
    public DropdownWidget(TextRenderer textRenderer, int x, int y, int width, int height,
                          List<T> options, Function<T, Text> labelProvider, Consumer<T> onSelect) {
        super(x, y, width, height, Text.empty(), (btn) -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.textRenderer = textRenderer;
        this.options = new ArrayList<>(options);
        this.labelProvider = labelProvider;
        this.onSelect = onSelect;
        
        updateMessage();
    }
    
    public T getSelected() {
        return selectedIndex >= 0 && selectedIndex < options.size() 
            ? options.get(selectedIndex) 
            : null;
    }
    
    public void setSelected(T value) {
        int idx = options.indexOf(value);
        if (idx >= 0) {
            selectedIndex = idx;
            updateMessage();
        }
    }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            selectedIndex = index;
            updateMessage();
        }
    }
    
    private void updateMessage() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            Text label = labelProvider.apply(options.get(selectedIndex));
            String text = label.getString();
            // Truncate if too long
            int maxChars = (getWidth() - 16) / 6;
            if (text.length() > maxChars && maxChars > 3) {
                text = text.substring(0, maxChars - 1) + "…";
            }
            setMessage(Text.literal(text + " ▼"));
        } else {
            setMessage(Text.literal("Select... ▼"));
        }
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (expanded) {
            expanded = false;
            activeDropdown = null;
        } else {
            // Close any other open dropdown
            if (activeDropdown != null && activeDropdown != this) {
                activeDropdown.expanded = false;
            }
            expanded = true;
            activeDropdown = this;
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleItems));
        }
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the main button only - list is rendered separately as overlay
        super.renderWidget(context, mouseX, mouseY, delta);
    }
    
    /**
     * Renders the dropdown list overlay. Call this AFTER all other widgets are rendered
     * to ensure the dropdown appears on top.
     */
    public void renderOverlay(DrawContext context, int mouseX, int mouseY) {
        if (expanded) {
            renderDropdownList(context, mouseX, mouseY);
        }
    }
    
    private void renderDropdownList(DrawContext context, int mouseX, int mouseY) {
        int listX = getX();
        int listY = getY() + getHeight();
        int listW = getWidth();
        int itemH = 14;
        int visibleCount = Math.min(maxVisibleItems, options.size());
        int listH = visibleCount * itemH + 4;
        
        // Draw list background
        context.fill(listX, listY, listX + listW, listY + listH, 0xF0101018);
        context.drawBorder(listX, listY, listW, listH, 0xFF444466);
        
        // Track hovered item
        hoveredIndex = -1;
        
        // Draw items
        for (int i = 0; i < visibleCount; i++) {
            int optIdx = scrollOffset + i;
            if (optIdx >= options.size()) break;
            
            int itemY = listY + 2 + i * itemH;
            boolean hovered = mouseX >= listX && mouseX < listX + listW 
                           && mouseY >= itemY && mouseY < itemY + itemH;
            boolean selected = optIdx == selectedIndex;
            
            if (hovered) {
                hoveredIndex = optIdx;
                context.fill(listX + 1, itemY, listX + listW - 1, itemY + itemH, 0x60FFFFFF);
            } else if (selected) {
                context.fill(listX + 1, itemY, listX + listW - 1, itemY + itemH, 0x4000FF88);
            }
            
            Text label = labelProvider.apply(options.get(optIdx));
            String text = label.getString();
            int maxChars = (listW - 8) / 6;
            if (text.length() > maxChars && maxChars > 3) {
                text = text.substring(0, maxChars - 1) + "…";
            }
            
            int textColor = selected ? 0xFF88FF88 : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            context.drawTextWithShadow(textRenderer, text, listX + 4, itemY + 3, textColor);
        }
        
        // Draw scroll indicators if needed
        if (options.size() > maxVisibleItems) {
            if (scrollOffset > 0) {
                context.drawTextWithShadow(textRenderer, "▲", listX + listW - 10, listY + 2, 0xFF888888);
            }
            if (scrollOffset + visibleCount < options.size()) {
                context.drawTextWithShadow(textRenderer, "▼", listX + listW - 10, listY + listH - 12, 0xFF888888);
            }
        }
    }
    
    /**
     * Called when mouse is clicked. Returns true if handled.
     * This should be called from the parent screen's mouseClicked.
     */
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!expanded) {
            return false;
        }
        
        // Check if click is in dropdown list
        int listX = getX();
        int listY = getY() + getHeight();
        int listW = getWidth();
        int itemH = 14;
        int visibleCount = Math.min(maxVisibleItems, options.size());
        int listH = visibleCount * itemH + 4;
        
        if (mouseX >= listX && mouseX < listX + listW 
            && mouseY >= listY && mouseY < listY + listH) {
            
            // Determine which item was clicked
            int relY = (int) mouseY - listY - 2;
            int clickedIdx = scrollOffset + relY / itemH;
            
            if (clickedIdx >= 0 && clickedIdx < options.size()) {
                selectedIndex = clickedIdx;
                updateMessage();
                expanded = false;
                activeDropdown = null;
                if (onSelect != null) {
                    onSelect.accept(options.get(selectedIndex));
                }
                return true;
            }
        }
        
        // Click outside - close dropdown
        expanded = false;
        activeDropdown = null;
        return false;
    }
    
    /**
     * Called when mouse is scrolled. Returns true if handled.
     */
    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        if (!expanded) {
            return false;
        }
        
        int listX = getX();
        int listY = getY() + getHeight();
        int listW = getWidth();
        int itemH = 14;
        int visibleCount = Math.min(maxVisibleItems, options.size());
        int listH = visibleCount * itemH + 4;
        
        if (mouseX >= listX && mouseX < listX + listW 
            && mouseY >= listY && mouseY < listY + listH) {
            
            scrollOffset -= (int) amount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleItems));
            return true;
        }
        
        return false;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void close() {
        expanded = false;
        if (activeDropdown == this) {
            activeDropdown = null;
        }
    }
    
    /**
     * Closes any active dropdown. Call this when the screen closes.
     */
    public static void closeAll() {
        if (activeDropdown != null) {
            activeDropdown.expanded = false;
            activeDropdown = null;
        }
    }
    
    /**
     * Returns the currently expanded dropdown, if any.
     */
    public static DropdownWidget<?> getActiveDropdown() {
        return activeDropdown;
    }
}

