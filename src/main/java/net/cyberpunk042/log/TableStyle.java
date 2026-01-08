package net.cyberpunk042.log;

/**
 * Table rendering styles for formatted output.
 */
public enum TableStyle {
    ASCII("+", "-", "+", "|"),
    UNICODE("┌", "─", "┐", "│"),
    MARKDOWN("|", "-", "|", "|"),
    COMPACT(" ", " ", " ", " ");
    
    public final String topLeft, horizontal, topRight, vertical;
    
    TableStyle(String topLeft, String horizontal, String topRight, String vertical) {
        this.topLeft = topLeft;
        this.horizontal = horizontal;
        this.topRight = topRight;
        this.vertical = vertical;
    }
}
