package net.cyberpunk042.client.gui.layout;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Side panel container for windowed mode.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Title bar with mode toggle and close button</li>
 *   <li>Tab bar for sub-panel switching</li>
 *   <li>Scrollable content area</li>
 *   <li>Collapsible sections</li>
 * </ul>
 */
public class SidePanel extends AbstractPanel {
    
    /**
     * Side of screen this panel is on.
     */
    public enum Side {
        LEFT, RIGHT
    }
    
    private final Side side;
    private final String title;
    private final TextRenderer textRenderer;
    private final List<TabEntry> tabs = new ArrayList<>();
    private int activeTabIndex = 0;
    
    // Sub-regions
    private Bounds titleBarBounds = Bounds.EMPTY;
    private Bounds tabBarBounds = Bounds.EMPTY;
    private Bounds contentBounds = Bounds.EMPTY;
    
    // Callbacks
    private Consumer<GuiMode> onModeToggle;
    private Runnable onClose;
    
    // Buttons
    private ButtonWidget modeToggleBtn;
    private ButtonWidget closeBtn;
    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    
    public SidePanel(Screen parent, FieldEditState state, Side side, String title, TextRenderer textRenderer) {
        super(parent, state);
        this.side = side;
        this.title = title;
        this.textRenderer = textRenderer;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TAB MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Adds a tab with an icon and associated panel.
     */
    public SidePanel addTab(String icon, String tooltip, AbstractPanel panel) {
        tabs.add(new TabEntry(icon, tooltip, panel));
        return this;
    }
    
    /**
     * Sets the active tab by index.
     */
    public void setActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeTabIndex = index;
            updateActivePanel();
        }
    }
    
    private void updateActivePanel() {
        // Update tab button states
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = i != activeTabIndex;
        }
        
        // Update content panel bounds
        if (activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).panel.setBounds(contentBounds);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setOnModeToggle(Consumer<GuiMode> callback) {
        this.onModeToggle = callback;
    }
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    protected void init() {
        clearWidgets();
        tabButtons.clear();
        
        int padding = 2;
        int titleHeight = 14;
        int tabHeight = 16;
        
        // Calculate sub-regions
        titleBarBounds = bounds.sliceTop(titleHeight);
        Bounds remaining = bounds.withoutTop(titleHeight);
        tabBarBounds = remaining.sliceTop(tabHeight);
        contentBounds = remaining.withoutTop(tabHeight).inset(padding);
        
        // Title bar buttons
        if (side == Side.LEFT) {
            // Mode toggle on left panel
            modeToggleBtn = addWidget(ButtonWidget.builder(Text.literal("⬜"), btn -> {
                if (onModeToggle != null) onModeToggle.accept(GuiMode.FULLSCREEN);
            })
                .dimensions(titleBarBounds.x() + 1, titleBarBounds.y() + 1, 12, 12)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Toggle fullscreen")))
                .build());
        } else {
            // Close button on right panel
            closeBtn = addWidget(ButtonWidget.builder(Text.literal("×"), btn -> {
                if (onClose != null) onClose.run();
            })
                .dimensions(titleBarBounds.right() - 13, titleBarBounds.y() + 1, 12, 12)
                .build());
        }
        
        // Tab buttons
        int tabWidth = Math.min(24, (tabBarBounds.width() - padding) / Math.max(1, tabs.size()));
        int x = tabBarBounds.x();
        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            TabEntry tab = tabs.get(i);
            ButtonWidget btn = addWidget(ButtonWidget.builder(Text.literal(tab.icon), b -> setActiveTab(tabIndex))
                .dimensions(x, tabBarBounds.y(), tabWidth, tabHeight)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(tab.tooltip)))
                .build());
            tabButtons.add(btn);
            x += tabWidth + 1;
        }
        
        // Initialize all tab panels with content bounds
        for (TabEntry tab : tabs) {
            tab.panel.setBounds(contentBounds);
        }
        
        updateActivePanel();
    }
    
    @Override
    public void tick() {
        if (activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).panel.tick();
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Panel background
        context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xDD1a1a24);
        
        // Border
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF444455);
        
        // Title bar
        context.fill(titleBarBounds.x(), titleBarBounds.y(), titleBarBounds.right(), titleBarBounds.bottom(), 0xFF2a2a3a);
        
        // Title text
        int titleX = side == Side.LEFT ? titleBarBounds.x() + 16 : titleBarBounds.x() + 2;
        context.drawTextWithShadow(textRenderer, title, titleX, titleBarBounds.y() + 3, 0xFF88CCFF);
        
        // Dirty indicator (on left panel)
        if (side == Side.LEFT && state.isDirty()) {
            context.drawText(textRenderer, "●", titleBarBounds.right() - 14, titleBarBounds.y() + 3, 0xFFFFAA00, false);
        }
        
        // Tab bar background
        context.fill(tabBarBounds.x(), tabBarBounds.y(), tabBarBounds.right(), tabBarBounds.bottom(), 0xFF222233);
        
        // Content background
        context.fill(contentBounds.x(), contentBounds.y(), contentBounds.right(), contentBounds.bottom(), 0xFF1a1a24);
        
        // Render active panel
        if (activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).panel.render(context, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        
        if (activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex).panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private record TabEntry(String icon, String tooltip, AbstractPanel panel) {}
}

