package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Compact single-line item selector with smart navigation buttons.
 * 
 * <pre>
 * [+] ◀│ item_name │▶ [+]
 * 
 * Button logic:
 * - First item only:  [+] name [+]
 * - First of many:    [+] name [▶]
 * - Middle:           [◀] name [▶]
 * - Last of many:     [◀] name [+]
 * </pre>
 * 
 * @param <T> Type of items being selected
 */
public class CompactSelector<T> {
    
    private static final int BUTTON_SIZE = 16;
    private static final int NAME_MIN_WIDTH = 60;
    
    private final String label;
    private final TextRenderer textRenderer;
    private final List<T> items;
    private final Function<T, String> nameExtractor;
    
    private int selectedIndex = 0;
    private Bounds bounds = Bounds.EMPTY;
    
    // Callbacks
    private Consumer<T> onSelect;
    private Runnable onAdd;
    private Consumer<T> onItemClick;  // Center button click (opens modal)
    
    // Widgets
    private ButtonWidget leftBtn;
    private ButtonWidget centerBtn;
    private ButtonWidget rightBtn;
    
    private final List<ClickableWidget> widgets = new ArrayList<>();
    
    /**
     * Creates a compact selector.
     * 
     * @param label Label shown before the selector (e.g., "LAYER:")
     * @param textRenderer For text measurement
     * @param items List of items to select from
     * @param nameExtractor Function to get display name from item
     */
    public CompactSelector(String label, TextRenderer textRenderer, List<T> items, Function<T, String> nameExtractor) {
        this.label = label;
        this.textRenderer = textRenderer;
        this.items = items;
        this.nameExtractor = nameExtractor;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public CompactSelector<T> onSelect(Consumer<T> callback) {
        this.onSelect = callback;
        return this;
    }
    
    public CompactSelector<T> onAdd(Runnable callback) {
        this.onAdd = callback;
        return this;
    }
    
    public CompactSelector<T> onItemClick(Consumer<T> callback) {
        this.onItemClick = callback;
        return this;
    }
    
    public CompactSelector<T> selectIndex(int index) {
        if (index >= 0 && index < items.size()) {
            this.selectedIndex = index;
            updateButtons();
        }
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        rebuildWidgets();
    }
    
    private void rebuildWidgets() {
        widgets.clear();
        
        if (bounds.isEmpty()) return;
        
        int labelWidth = textRenderer.getWidth(label) + 4;
        int x = bounds.x() + labelWidth;
        int y = bounds.y();
        int h = Math.min(bounds.height(), 18);
        
        // Calculate button positions
        int availableWidth = bounds.width() - labelWidth;
        int nameWidth = Math.max(NAME_MIN_WIDTH, availableWidth - BUTTON_SIZE * 2 - 4);
        
        // Left button
        leftBtn = ButtonWidget.builder(Text.literal(getLeftButtonText()), btn -> onLeftClick())
            .dimensions(x, y, BUTTON_SIZE, h)
            .build();
        widgets.add(leftBtn);
        
        // Center button (name)
        String displayName = getCurrentDisplayName();
        centerBtn = ButtonWidget.builder(Text.literal(truncate(displayName, nameWidth - 8)), btn -> onCenterClick())
            .dimensions(x + BUTTON_SIZE + 2, y, nameWidth, h)
            .build();
        widgets.add(centerBtn);
        
        // Right button
        rightBtn = ButtonWidget.builder(Text.literal(getRightButtonText()), btn -> onRightClick())
            .dimensions(x + BUTTON_SIZE + nameWidth + 4, y, BUTTON_SIZE, h)
            .build();
        widgets.add(rightBtn);
        
        updateButtons();
    }
    
    private void updateButtons() {
        if (leftBtn != null) {
            leftBtn.setMessage(Text.literal(getLeftButtonText()));
        }
        if (rightBtn != null) {
            rightBtn.setMessage(Text.literal(getRightButtonText()));
        }
        if (centerBtn != null) {
            String displayName = getCurrentDisplayName();
            int nameWidth = centerBtn.getWidth();
            centerBtn.setMessage(Text.literal(truncate(displayName, nameWidth - 8)));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON LOGIC
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getLeftButtonText() {
        if (items.isEmpty() || selectedIndex == 0) {
            return "+";
        }
        return "◀";
    }
    
    private String getRightButtonText() {
        if (items.isEmpty() || selectedIndex >= items.size() - 1) {
            return "+";
        }
        return "▶";
    }
    
    private void onLeftClick() {
        if (items.isEmpty() || selectedIndex == 0) {
            // Add new item
            if (onAdd != null) onAdd.run();
        } else {
            // Go to previous
            selectedIndex--;
            updateButtons();
            notifySelect();
        }
    }
    
    private void onRightClick() {
        if (items.isEmpty() || selectedIndex >= items.size() - 1) {
            // Add new item
            if (onAdd != null) onAdd.run();
        } else {
            // Go to next
            selectedIndex++;
            updateButtons();
            notifySelect();
        }
    }
    
    private void onCenterClick() {
        if (!items.isEmpty() && onItemClick != null) {
            onItemClick.accept(items.get(selectedIndex));
        }
    }
    
    private void notifySelect() {
        if (onSelect != null && !items.isEmpty()) {
            onSelect.accept(items.get(selectedIndex));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public T getSelected() {
        if (items.isEmpty()) return null;
        return items.get(selectedIndex);
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }

    /**
     * Replace the item list and update button labels.
     * Does NOT rebuild widgets - just updates existing buttons to avoid
     * breaking screen widget registration.
     */
    public void setItems(List<T> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        if (selectedIndex >= items.size()) {
            selectedIndex = Math.max(0, items.size() - 1);
        }
        // Update existing buttons instead of rebuilding
        // This preserves screen widget registration
        updateButtons();
    }
    
    private String getCurrentDisplayName() {
        if (items.isEmpty()) return "(none)";
        return nameExtractor.apply(items.get(selectedIndex));
    }
    
    private String truncate(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        
        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        
        while (text.length() > 1 && textRenderer.getWidth(text) + ellipsisWidth > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds.isEmpty()) return;
        
        // Draw label
        int labelY = bounds.y() + (bounds.height() - 8) / 2;
        context.drawTextWithShadow(textRenderer, label, bounds.x(), labelY, 0xFF888899);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Call when the items list changes externally.
     */
    public void refresh() {
        // Clamp index
        if (selectedIndex >= items.size()) {
            selectedIndex = Math.max(0, items.size() - 1);
        }
        updateButtons();
    }
}

