package net.cyberpunk042.client.gui.util;

/**
 * G15: Layout positioning helper for GUI panels.
 */
public class GuiLayout {
    
    private int startX;
    private int startY;
    private int rowHeight;
    private int panelWidth;
    private int currentX;
    private int currentY;
    
    public GuiLayout(int startX, int startY, int rowHeight) {
        this.startX = startX;
        this.startY = startY;
        this.rowHeight = rowHeight;
        this.panelWidth = 200; // default width
        this.currentX = startX;
        this.currentY = startY;
    }
    
    /** Constructor with panel width for sub-panels. */
    public GuiLayout(int startX, int startY, int rowHeight, int panelWidth) {
        this.startX = startX;
        this.startY = startY;
        this.rowHeight = rowHeight;
        this.panelWidth = panelWidth;
        this.currentX = startX;
        this.currentY = startY;
    }
    
    public GuiLayout(int startX, int startY) {
        this(startX, startY, GuiConstants.widgetHeight() + GuiConstants.padding());
    }
    
    public void reset() {
        currentX = startX;
        currentY = startY;
    }
    
    public void reset(int newStartX, int newStartY) {
        this.startX = newStartX;
        this.startY = newStartY;
        reset();
    }
    
    public int getX() { return currentX; }
    public int getY() { return currentY; }
    public int getCurrentY() { return currentY; }
    
    // Aliases for sub-panel consistency
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getPanelWidth() { return panelWidth; }
    public void setPanelWidth(int width) { this.panelWidth = width; }
    
    public int nextRow() {
        int y = currentY;
        currentY += rowHeight;
        return y;
    }
    
    public int nextSection() {
        currentY += GuiConstants.SECTION_SPACING;
        return currentY;
    }
    
    public void setRowHeight(int height) {
        this.rowHeight = height;
    }
}
