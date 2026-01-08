package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.client.gui.layout.Bounds;

/**
 * Helper for splitting a row into multiple columns.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Equal 3 columns
 * RowLayout row = RowLayout.of(bounds, 3);
 * Button btn1 = new Button(row.get(0).x(), row.get(0).y(), row.get(0).width(), ...);
 * Button btn2 = new Button(row.get(1).x(), row.get(1).y(), row.get(1).width(), ...);
 * Button btn3 = new Button(row.get(2).x(), row.get(2).y(), row.get(2).width(), ...);
 * 
 * // Weighted columns (50%, 25%, 25%)
 * RowLayout row = RowLayout.of(bounds, 3).weights(0.5f, 0.25f, 0.25f);
 * 
 * // With custom gap
 * RowLayout row = RowLayout.of(bounds, 3).gap(8);
 * </pre>
 */
public class RowLayout {
    
    private final Bounds bounds;
    private final int columns;
    private int gap = 4;
    private float[] weights;
    
    private RowLayout(Bounds bounds, int columns) {
        this.bounds = bounds;
        this.columns = columns;
        this.weights = null; // null = equal distribution
    }
    
    /**
     * Create a row layout that splits bounds into N equal columns.
     * 
     * @param bounds The bounds to split
     * @param columns Number of columns
     * @return A new RowLayout
     */
    public static RowLayout of(Bounds bounds, int columns) {
        return new RowLayout(bounds, Math.max(1, columns));
    }
    
    /**
     * Set the gap between columns.
     * 
     * @param gap Gap in pixels (default: 4)
     * @return This layout for chaining
     */
    public RowLayout gap(int gap) {
        this.gap = gap;
        return this;
    }
    
    /**
     * Set custom column weights for unequal distribution.
     * 
     * <p>Weights should sum to 1.0. If not, they will be normalized.</p>
     * 
     * @param weights Array of weights (one per column)
     * @return This layout for chaining
     */
    public RowLayout weights(float... weights) {
        if (weights != null && weights.length == columns) {
            this.weights = weights;
        }
        return this;
    }
    
    /**
     * Get the bounds for a specific column.
     * 
     * @param index Column index (0-based)
     * @return Bounds for that column
     */
    public Bounds get(int index) {
        if (index < 0 || index >= columns) {
            return Bounds.EMPTY;
        }
        
        int totalGap = gap * (columns - 1);
        int availableWidth = bounds.width() - totalGap;
        
        if (weights != null) {
            // Weighted distribution
            float totalWeight = 0;
            for (float w : weights) totalWeight += w;
            
            int x = bounds.x();
            for (int i = 0; i < index; i++) {
                int colWidth = (int) (availableWidth * (weights[i] / totalWeight));
                x += colWidth + gap;
            }
            
            int colWidth = (int) (availableWidth * (weights[index] / totalWeight));
            return new Bounds(x, bounds.y(), colWidth, bounds.height());
        } else {
            // Equal distribution
            int colWidth = availableWidth / columns;
            int x = bounds.x() + index * (colWidth + gap);
            return new Bounds(x, bounds.y(), colWidth, bounds.height());
        }
    }
    
    /**
     * Get bounds spanning multiple consecutive columns.
     * 
     * @param startIndex Starting column index
     * @param count Number of columns to span
     * @return Merged bounds for all columns
     */
    public Bounds span(int startIndex, int count) {
        if (startIndex < 0 || startIndex + count > columns || count <= 0) {
            return Bounds.EMPTY;
        }
        
        Bounds first = get(startIndex);
        Bounds last = get(startIndex + count - 1);
        
        return new Bounds(
            first.x(),
            first.y(),
            last.right() - first.x(),
            first.height()
        );
    }
    
    /**
     * Get the total number of columns.
     */
    public int getColumnCount() {
        return columns;
    }
    
    /**
     * Get the gap between columns.
     */
    public int getGap() {
        return gap;
    }
}
