package net.cyberpunk042.client.gui.util;

import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

/**
 * G127-G130: Keyboard navigation utilities.
 * 
 * <p>Helps with Tab/arrow key navigation between widgets.</p>
 */
public final class GuiKeyboardNav {
    
    private GuiKeyboardNav() {}
    
    /**
     * G127: Find next focusable widget in list.
     * @param widgets list of widgets
     * @param current currently focused widget (or null)
     * @param forward true for next, false for previous
     * @return next widget to focus, or null
     */
    public static ClickableWidget findNext(List<? extends ClickableWidget> widgets, ClickableWidget current, boolean forward) {
        if (widgets.isEmpty()) return null;
        
        int currentIndex = current != null ? widgets.indexOf(current) : -1;
        int step = forward ? 1 : -1;
        int size = widgets.size();
        
        // Start from next/prev position
        int startIndex = currentIndex + step;
        if (startIndex < 0) startIndex = size - 1;
        if (startIndex >= size) startIndex = 0;
        
        // Search for active widget
        for (int i = 0; i < size; i++) {
            int index = (startIndex + i * step + size) % size;
            ClickableWidget widget = widgets.get(index);
            if (widget.active && widget.visible) {
                return widget;
            }
        }
        
        return null;
    }
    
    /**
     * G128: Find widget at position in grid layout.
     */
    public static ClickableWidget findAtGrid(List<? extends ClickableWidget> widgets, int columns, int row, int col) {
        int index = row * columns + col;
        if (index >= 0 && index < widgets.size()) {
            return widgets.get(index);
        }
        return null;
    }
    
    /**
     * G129: Navigate in direction (for arrow keys).
     * @param widgets list of widgets
     * @param current currently focused widget
     * @param dx horizontal direction (-1, 0, 1)
     * @param dy vertical direction (-1, 0, 1)
     * @param columns number of columns in grid (for 2D nav)
     */
    public static ClickableWidget navigateDirection(
            List<? extends ClickableWidget> widgets, 
            ClickableWidget current,
            int dx, int dy,
            int columns) {
        
        if (current == null || widgets.isEmpty()) {
            return widgets.isEmpty() ? null : widgets.get(0);
        }
        
        int currentIndex = widgets.indexOf(current);
        if (currentIndex < 0) return null;
        
        int rows = (widgets.size() + columns - 1) / columns;
        int currentRow = currentIndex / columns;
        int currentCol = currentIndex % columns;
        
        int newRow = Math.max(0, Math.min(rows - 1, currentRow + dy));
        int newCol = Math.max(0, Math.min(columns - 1, currentCol + dx));
        int newIndex = newRow * columns + newCol;
        
        if (newIndex >= 0 && newIndex < widgets.size()) {
            ClickableWidget target = widgets.get(newIndex);
            if (target.active && target.visible) {
                return target;
            }
        }
        
        return current;
    }
    
    /**
     * G130: Check if key is navigation key.
     */
    public static boolean isNavKey(int keyCode) {
        return keyCode == 258  // Tab
            || keyCode == 265  // Up
            || keyCode == 264  // Down
            || keyCode == 263  // Left
            || keyCode == 262; // Right
    }
    
    /**
     * Direction from key code.
     * @return int[2] = {dx, dy}
     */
    public static int[] directionFromKey(int keyCode) {
        return switch (keyCode) {
            case 265 -> new int[]{0, -1};  // Up
            case 264 -> new int[]{0, 1};   // Down
            case 263 -> new int[]{-1, 0};  // Left
            case 262 -> new int[]{1, 0};   // Right
            default -> new int[]{0, 0};
        };
    }
}
