package net.cyberpunk042.client.gui.panel.layout;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.util.GuiConstants;

/**
 * Single-column compact layout for ProfilesPanel.
 * 
 * <p>All elements stacked vertically in one panel.
 * No details section - just filters, list, actions, name.</p>
 */
public class SingleColumnLayout implements ProfilesPanelLayout {
    
    private static final int FILTER_HEIGHT = 20;
    private static final int SECTION_GAP = 8;
    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 2;
    private static final int BTN_ROWS = 2;  // Compact: fewer button rows
    
    private final Bounds bounds;
    
    // Cached computed bounds
    private final Bounds filterArea;
    private final Bounds listArea;
    private final Bounds nameFieldArea;
    private final Bounds actionsArea;
    private final int buttonWidth;
    
    public SingleColumnLayout(Bounds bounds) {
        this.bounds = bounds;
        
        int pad = GuiConstants.PADDING;
        int x = bounds.x() + pad;
        int y = bounds.y() + pad;
        int w = bounds.width() - pad * 2;
        int h = bounds.height() - pad * 2;
        
        // Filter at top (single row, compact)
        this.filterArea = new Bounds(x, y, w, FILTER_HEIGHT);
        
        // Name field at very bottom
        int nameY = bounds.bottom() - pad - FILTER_HEIGHT;
        this.nameFieldArea = new Bounds(x, nameY, w, FILTER_HEIGHT);
        
        // Actions above name field (2 rows in compact mode)
        int actionsH = BTN_HEIGHT * BTN_ROWS + BTN_GAP * (BTN_ROWS - 1);
        int actionsY = nameY - SECTION_GAP - actionsH;
        this.actionsArea = new Bounds(x, actionsY, w, actionsH);
        
        // List fills the middle
        int listY = y + FILTER_HEIGHT + SECTION_GAP;
        int listH = actionsY - listY - SECTION_GAP;
        this.listArea = new Bounds(x, listY, w, Math.max(40, listH));
        
        // Button width: 3 buttons per row
        this.buttonWidth = (w - BTN_GAP * 2) / 3;
    }
    
    @Override
    public Bounds filterArea() {
        return filterArea;
    }
    
    @Override
    public Bounds listArea() {
        return listArea;
    }
    
    @Override
    public Bounds actionsArea() {
        return actionsArea;
    }
    
    @Override
    public Bounds nameFieldArea() {
        return nameFieldArea;
    }
    
    @Override
    public Bounds detailsArea() {
        return null;  // No details in single-column mode
    }
    
    @Override
    public int buttonWidth() {
        return buttonWidth;
    }
    
    @Override
    public int buttonsPerRow() {
        return 3;
    }
    
    @Override
    public int buttonGap() {
        return BTN_GAP;
    }
    
    @Override
    public boolean showServerButton() {
        return false;  // Skip in compact mode
    }
    
    @Override
    public boolean isDualMode() {
        return false;
    }
    
    @Override
    public Bounds fullBounds() {
        return bounds;
    }
}
