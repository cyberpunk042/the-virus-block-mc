package net.cyberpunk042.client.gui.layout;

/**
 * Immutable rectangle bounds for GUI layout.
 * All panels receive bounds and render relative to them.
 */
public record Bounds(int x, int y, int width, int height) {
    
    public static final Bounds EMPTY = new Bounds(0, 0, 0, 0);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DERIVED VALUES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int right() { return x + width; }
    public int bottom() { return y + height; }
    public int centerX() { return x + width / 2; }
    public int centerY() { return y + height / 2; }
    
    public boolean isEmpty() { return width <= 0 || height <= 0; }
    
    public boolean contains(int px, int py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }
    
    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORMATIONS (return new Bounds, immutable)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Insets by same amount on all sides.
     */
    public Bounds inset(int amount) {
        return inset(amount, amount, amount, amount);
    }
    
    /**
     * Insets with different horizontal/vertical amounts.
     */
    public Bounds inset(int horizontal, int vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }
    
    /**
     * Insets with explicit amounts for each side.
     */
    public Bounds inset(int left, int top, int right, int bottom) {
        return new Bounds(
            x + left,
            y + top,
            Math.max(0, width - left - right),
            Math.max(0, height - top - bottom)
        );
    }
    
    /**
     * Returns bounds offset by dx, dy.
     */
    public Bounds offset(int dx, int dy) {
        return new Bounds(x + dx, y + dy, width, height);
    }
    
    /**
     * Returns bounds with new size, same position.
     */
    public Bounds withSize(int newWidth, int newHeight) {
        return new Bounds(x, y, newWidth, newHeight);
    }
    
    /**
     * Returns bounds with new position, same size.
     */
    public Bounds withPosition(int newX, int newY) {
        return new Bounds(newX, newY, width, height);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLICING (for layout)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns top portion with given height.
     */
    public Bounds sliceTop(int h) {
        return new Bounds(x, y, width, Math.min(h, height));
    }
    
    /**
     * Returns bottom portion with given height.
     */
    public Bounds sliceBottom(int h) {
        int sliceHeight = Math.min(h, height);
        return new Bounds(x, bottom() - sliceHeight, width, sliceHeight);
    }
    
    /**
     * Returns left portion with given width.
     */
    public Bounds sliceLeft(int w) {
        return new Bounds(x, y, Math.min(w, width), height);
    }
    
    /**
     * Returns right portion with given width.
     */
    public Bounds sliceRight(int w) {
        int sliceWidth = Math.min(w, width);
        return new Bounds(right() - sliceWidth, y, sliceWidth, height);
    }
    
    /**
     * Returns remaining bounds after slicing top.
     */
    public Bounds withoutTop(int h) {
        int cut = Math.min(h, height);
        return new Bounds(x, y + cut, width, height - cut);
    }
    
    /**
     * Returns remaining bounds after slicing bottom.
     */
    public Bounds withoutBottom(int h) {
        return new Bounds(x, y, width, Math.max(0, height - h));
    }
    
    /**
     * Returns remaining bounds after slicing left.
     */
    public Bounds withoutLeft(int w) {
        int cut = Math.min(w, width);
        return new Bounds(x + cut, y, width - cut, height);
    }
    
    /**
     * Returns remaining bounds after slicing right.
     */
    public Bounds withoutRight(int w) {
        return new Bounds(x, y, Math.max(0, width - w), height);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CENTERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns bounds of given size centered within this bounds.
     */
    public Bounds centerChild(int childWidth, int childHeight) {
        return new Bounds(
            x + (width - childWidth) / 2,
            y + (height - childHeight) / 2,
            childWidth,
            childHeight
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Bounds of(int x, int y, int width, int height) {
        return new Bounds(x, y, width, height);
    }
    
    public static Bounds ofSize(int width, int height) {
        return new Bounds(0, 0, width, height);
    }
    
    /**
     * Creates bounds centered in a screen of given size.
     */
    public static Bounds centeredIn(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        return new Bounds(
            (screenWidth - panelWidth) / 2,
            (screenHeight - panelHeight) / 2,
            panelWidth,
            panelHeight
        );
    }
    
    @Override
    public String toString() {
        return String.format("Bounds[%d,%d %dx%d]", x, y, width, height);
    }
}

