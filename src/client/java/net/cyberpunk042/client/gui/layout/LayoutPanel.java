package net.cyberpunk042.client.gui.layout;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * New-generation panel base class using Minecraft's native layout widgets.
 * 
 * <h3>Key Features</h3>
 * <ul>
 *   <li>Uses {@link DirectionalLayoutWidget} for automatic layout</li>
 *   <li>Bounds-aware: call {@link #setBounds(Bounds)} then {@link #build()}</li>
 *   <li>Widgets are positioned automatically - no manual offset needed</li>
 *   <li>Fluent API for adding widgets</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * class MyPanel extends LayoutPanel {
 *     @Override
 *     protected void buildContent(DirectionalLayoutWidget layout) {
 *         // Add widgets using native layout
 *         layout.add(ButtonWidget.builder(Text.literal("Click me"), btn -> {})
 *             .width(bounds.width() - 8)
 *             .build());
 *         
 *         // Or use helper methods
 *         addRow(layout, "Label:", slider(0, 100, 50, v -> {}));
 *     }
 * }
 * }</pre>
 * 
 * <h3>Migration Guide from AbstractPanel</h3>
 * <ol>
 *   <li>Extend LayoutPanel instead of AbstractPanel</li>
 *   <li>Move widget creation from init() to buildContent()</li>
 *   <li>Remove manual toScreenX/toScreenY calls</li>
 *   <li>Use layout.add() instead of addWidget()</li>
 * </ol>
 */
public abstract class LayoutPanel {
    
    protected final Screen parent;
    protected final FieldEditState state;
    protected final TextRenderer textRenderer;
    
    /** Current bounds - set before build() is called */
    protected Bounds bounds = Bounds.EMPTY;
    
    /** The root layout widget (vertical by default) */
    protected DirectionalLayoutWidget layout;
    
    /** All widgets created during build, for registration with Screen */
    protected final List<ClickableWidget> allWidgets = new ArrayList<>();
    
    /** Scroll state */
    protected int scrollOffset = 0;
    protected int contentHeight = 0;
    
    protected LayoutPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        this.parent = parent;
        this.state = state;
        this.textRenderer = textRenderer;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets bounds and triggers rebuild.
     * Call this before rendering.
     */
    public void setBounds(Bounds bounds) {
        if (this.bounds.equals(bounds)) return;
        this.bounds = bounds;
        rebuild();
    }
    
    /**
     * Rebuilds the layout from scratch.
     */
    public void rebuild() {
        allWidgets.clear();
        
        // Create vertical layout at bounds position
        layout = DirectionalLayoutWidget.vertical();
        layout.spacing(4);
        
        // Let subclass add content
        buildContent(layout);
        
        // Refresh positions based on content
        layout.refreshPositions();
        
        // Position at bounds
        layout.setPosition(bounds.x(), bounds.y());
        
        // Calculate content height
        contentHeight = layout.getHeight();
        
        // Collect all widgets
        collectWidgets(layout);
    }
    
    /**
     * Override this to add your content to the layout.
     * 
     * @param layout Vertical layout widget to add content to
     */
    protected abstract void buildContent(DirectionalLayoutWidget layout);
    
    /**
     * Collects all ClickableWidgets from a widget tree.
     */
    private void collectWidgets(Widget widget) {
        if (widget instanceof ClickableWidget clickable) {
            allWidgets.add(clickable);
        }
        widget.forEachChild(this::collectWidgets);
    }
    
    /**
     * @return All widgets for registration with the parent screen.
     */
    public List<ClickableWidget> getWidgets() {
        return allWidgets;
    }
    
    /**
     * @return Current bounds.
     */
    public Bounds getBounds() {
        return bounds;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK & RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called each tick for updates.
     */
    public void tick() {
        // Override if needed
    }
    
    /**
     * Renders the panel. Widgets render themselves via Screen.
     * Override to add custom rendering (backgrounds, labels, etc.)
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Default: just render the layout (which renders children)
        // Most panels just let Screen render the widgets
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCROLL
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isScrollable() {
        return contentHeight > bounds.height();
    }
    
    public void setScrollOffset(int offset) {
        int maxScroll = Math.max(0, contentHeight - bounds.height());
        this.scrollOffset = Math.max(0, Math.min(offset, maxScroll));
        
        // Reposition layout based on scroll
        if (layout != null) {
            layout.setPosition(bounds.x(), bounds.y() - scrollOffset);
        }
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        if (!isScrollable()) return false;
        
        setScrollOffset(scrollOffset - (int)(v * 10));
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS for building content
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a horizontal row layout.
     */
    protected DirectionalLayoutWidget row() {
        return DirectionalLayoutWidget.horizontal().spacing(4);
    }
    
    /**
     * Adds a widget with a positioner consumer.
     */
    protected <T extends Widget> T add(DirectionalLayoutWidget layout, T widget, Consumer<Positioner> positioner) {
        return layout.add(widget, positioner);
    }
    
    /**
     * Adds a widget aligned left with margins.
     */
    protected <T extends Widget> T addWithMargin(DirectionalLayoutWidget layout, T widget, int margin) {
        return layout.add(widget, p -> p.margin(margin));
    }
    
    /**
     * Gets the available width inside bounds (accounting for padding).
     */
    protected int contentWidth() {
        return bounds.width() - 8;  // 4px padding each side
    }
    
    /**
     * Gets the available width inside bounds for a widget row.
     */
    protected int rowWidth() {
        return bounds.width() - 16;  // More padding for rows
    }
}

