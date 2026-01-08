package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;

/**
 * Grid layout helper for dividing bounds into cells.
 * 
 * <pre>
 * ┌─────────────┬─────────────┐
 * │  (0,0)      │  (1,0)      │
 * │  TOP-LEFT   │  TOP-RIGHT  │
 * ├─────────────┼─────────────┤
 * │  (0,1)      │  (1,1)      │
 * │  BOT-LEFT   │  BOT-RIGHT  │
 * └─────────────┴─────────────┘
 * </pre>
 */
public class GridPane {
    
    private final int columns;
    private final int rows;
    private final int hGap;
    private final int vGap;
    private Bounds bounds = Bounds.EMPTY;
    
    // Cached cell bounds
    private Bounds[][] cells;
    
    /**
     * Creates a grid with equal-sized cells.
     */
    public GridPane(int columns, int rows) {
        this(columns, rows, 4, 4);
    }
    
    /**
     * Creates a grid with gaps between cells.
     */
    public GridPane(int columns, int rows, int hGap, int vGap) {
        this.columns = columns;
        this.rows = rows;
        this.hGap = hGap;
        this.vGap = vGap;
        this.cells = new Bounds[columns][rows];
    }
    
    /**
     * Sets the outer bounds and recalculates cell bounds.
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        recalculate();
    }
    
    private void recalculate() {
        if (bounds.isEmpty()) {
            for (int c = 0; c < columns; c++) {
                for (int r = 0; r < rows; r++) {
                    cells[c][r] = Bounds.EMPTY;
                }
            }
            return;
        }
        
        int totalHGap = hGap * (columns - 1);
        int totalVGap = vGap * (rows - 1);
        
        int cellWidth = (bounds.width() - totalHGap) / columns;
        int cellHeight = (bounds.height() - totalVGap) / rows;
        
        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                int x = bounds.x() + c * (cellWidth + hGap);
                int y = bounds.y() + r * (cellHeight + vGap);
                cells[c][r] = new Bounds(x, y, cellWidth, cellHeight);
            }
        }
    }
    
    /**
     * Gets the bounds of a specific cell.
     * 
     * @param column 0-based column index
     * @param row 0-based row index
     */
    public Bounds getCell(int column, int row) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            return Bounds.EMPTY;
        }
        return cells[column][row];
    }
    
    /**
     * Gets bounds spanning multiple cells.
     * 
     * @param startCol Starting column
     * @param startRow Starting row
     * @param colSpan Number of columns to span
     * @param rowSpan Number of rows to span
     */
    public Bounds getSpan(int startCol, int startRow, int colSpan, int rowSpan) {
        Bounds topLeft = getCell(startCol, startRow);
        Bounds bottomRight = getCell(startCol + colSpan - 1, startRow + rowSpan - 1);
        
        if (topLeft.isEmpty() || bottomRight.isEmpty()) {
            return Bounds.EMPTY;
        }
        
        return new Bounds(
            topLeft.x(),
            topLeft.y(),
            bottomRight.right() - topLeft.x(),
            bottomRight.bottom() - topLeft.y()
        );
    }
    
    // Convenience methods for 2x2 grid
    
    public Bounds topLeft() { return getCell(0, 0); }
    public Bounds topRight() { return getCell(1, 0); }
    public Bounds bottomLeft() { return getCell(0, 1); }
    public Bounds bottomRight() { return getCell(1, 1); }
    
    // Row/column helpers
    
    public Bounds topRow() { return getSpan(0, 0, columns, 1); }
    public Bounds bottomRow() { return getSpan(0, rows - 1, columns, 1); }
    public Bounds leftColumn() { return getSpan(0, 0, 1, rows); }
    public Bounds rightColumn() { return getSpan(columns - 1, 0, 1, rows); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLEXIBLE SIZING (for unequal cells)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a 2-column layout with custom width ratio.
     * 
     * @param leftRatio Ratio for left column (0.0 to 1.0)
     */
    public static GridPane twoColumns(float leftRatio) {
        return new FlexGridPane(2, 1, new float[]{leftRatio, 1 - leftRatio}, new float[]{1});
    }
    
    /**
     * Creates a 2-row layout with custom height ratio.
     */
    public static GridPane twoRows(float topRatio) {
        return new FlexGridPane(1, 2, new float[]{1}, new float[]{topRatio, 1 - topRatio});
    }
    
    /**
     * Creates a 2x2 grid with custom ratios.
     */
    public static GridPane grid2x2(float leftRatio, float topRatio) {
        return new FlexGridPane(2, 2, 
            new float[]{leftRatio, 1 - leftRatio}, 
            new float[]{topRatio, 1 - topRatio});
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLEXIBLE GRID IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static class FlexGridPane extends GridPane {
        private final float[] colRatios;
        private final float[] rowRatios;
        
        FlexGridPane(int columns, int rows, float[] colRatios, float[] rowRatios) {
            super(columns, rows);
            this.colRatios = colRatios;
            this.rowRatios = rowRatios;
        }
        
        @Override
        public void setBounds(Bounds bounds) {
            // We need to override the parent's recalculate
            // Store bounds first, then do our own calculation
            super.bounds = bounds;
            recalculateFlex();
        }
        
        private void recalculateFlex() {
            if (super.bounds.isEmpty()) return;
            
            int hGap = super.hGap;
            int vGap = super.vGap;
            int columns = super.columns;
            int rows = super.rows;
            
            int totalHGap = hGap * (columns - 1);
            int totalVGap = vGap * (rows - 1);
            
            int availableWidth = super.bounds.width() - totalHGap;
            int availableHeight = super.bounds.height() - totalVGap;
            
            // Calculate column widths
            int[] colWidths = new int[columns];
            int x = super.bounds.x();
            for (int c = 0; c < columns; c++) {
                colWidths[c] = (int)(availableWidth * colRatios[c]);
            }
            
            // Calculate row heights
            int[] rowHeights = new int[rows];
            for (int r = 0; r < rows; r++) {
                rowHeights[r] = (int)(availableHeight * rowRatios[r]);
            }
            
            // Build cells
            int currentY = super.bounds.y();
            for (int r = 0; r < rows; r++) {
                int currentX = super.bounds.x();
                for (int c = 0; c < columns; c++) {
                    super.cells[c][r] = new Bounds(currentX, currentY, colWidths[c], rowHeights[r]);
                    currentX += colWidths[c] + hGap;
                }
                currentY += rowHeights[r] + vGap;
            }
        }
    }
}

