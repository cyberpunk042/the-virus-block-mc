package net.cyberpunk042.client.gui.panel.layout;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.util.GuiConstants;

/**
 * Dual-column layout for ProfilesPanel.
 * 
 * <p>Left panel: Filters + Profile List + Name Field</p>
 * <p>Right panel: Details + Stats + Action Buttons</p>
 */
public class DualColumnLayout implements ProfilesPanelLayout {
    
    private static final int FILTER_HEIGHT = 20;
    private static final int FILTER_GAP = 4;
    private static final int SECTION_GAP = 8;
    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 4;
    private static final int BTN_ROWS = 3;
    
    private final Bounds left;
    private final Bounds right;
    
    // Cached computed bounds
    private final Bounds filterArea;
    private final Bounds listArea;
    private final Bounds nameFieldArea;
    private final Bounds detailsArea;
    private final Bounds actionsArea;
    private final int buttonWidth;
    
    public DualColumnLayout(Bounds left, Bounds right) {
        this.left = left;
        this.right = right;
        
        int pad = GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════
        // LEFT PANEL LAYOUT
        // ═══════════════════════════════════════════════════════════════════
        int lx = left.x() + pad;
        int ly = left.y() + pad;
        int lw = left.width() - pad * 2;
        
        // Filter row: two rows (source+category, then search)
        this.filterArea = new Bounds(lx, ly, lw, FILTER_HEIGHT * 2 + FILTER_GAP);
        
        // Name field at bottom of left panel
        int nameY = left.bottom() - pad - FILTER_HEIGHT;
        this.nameFieldArea = new Bounds(lx, nameY, lw, FILTER_HEIGHT);
        
        // List fills the middle
        int listY = ly + filterArea.height() + SECTION_GAP;
        int listH = nameY - listY - SECTION_GAP;
        this.listArea = new Bounds(lx, listY, lw, listH);
        
        // ═══════════════════════════════════════════════════════════════════
        // RIGHT PANEL LAYOUT
        // ═══════════════════════════════════════════════════════════════════
        int rx = right.x() + pad;
        int ry = right.y() + pad;
        int rw = right.width() - pad * 2;
        
        // Actions at bottom of right panel (3 rows of buttons)
        int actionsH = BTN_HEIGHT * BTN_ROWS + BTN_GAP * (BTN_ROWS - 1);
        int actionsY = right.bottom() - pad - actionsH;
        this.actionsArea = new Bounds(rx, actionsY, rw, actionsH);
        
        // Details fills the rest of right panel
        int detailsH = actionsY - ry - SECTION_GAP;
        this.detailsArea = new Bounds(rx, ry, rw, detailsH);
        
        // Button width: 3 buttons per row
        this.buttonWidth = (rw - BTN_GAP * 2) / 3;
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
        return detailsArea;
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
        return true;  // Full row available in dual mode
    }
    
    @Override
    public boolean isDualMode() {
        return true;
    }
    
    @Override
    public Bounds fullBounds() {
        // Union of left and right
        int x = Math.min(left.x(), right.x());
        int y = Math.min(left.y(), right.y());
        int right_ = Math.max(left.right(), right.right());
        int bottom = Math.max(left.bottom(), right.bottom());
        return new Bounds(x, y, right_ - x, bottom - y);
    }
    
    public Bounds leftPanel() {
        return left;
    }
    
    public Bounds rightPanel() {
        return right;
    }
}
